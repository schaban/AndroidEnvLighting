// Author: Sergey Chaban <sergey.chaban@gmail.com>

package geas;

import java.nio.*;
import java.util.Arrays;
import java.util.HashMap;

import xdata.*;

public class GeoData {

	public enum ElemType {
		NONE,
		FLOAT,
		HALF,
		SHORT,
		USHORT,
		BYTE,
		UBYTE
	}

	public class VtxInfo {
		private String mTag;

		private ElemType mPosType = ElemType.NONE;
		private int mPosOffs = -1;

		private boolean mUseOcta;
		private ElemType mNrmType = ElemType.NONE;
		private int mNrmOffs = -1;
		private int mTngOffs = -1;

		private boolean mHasTex2;
		private ElemType mTexType = ElemType.NONE;
		private int mTexOffs = -1;

		private boolean mHasAlpha;
		private ElemType mClrType = ElemType.NONE;
		private int mClrOffs = -1;

		private boolean mIsLocalSkin;
		private int mSkinOffs = -1;
		private int mSkinMode;

		private float mTexScl;
		private float mClrScl;

		private int mByteSize;

		public String getTag() {
			return mTag;
		}

		public int getPosOffs() {
			return mPosOffs;
		}

		public boolean hasPos() {
			return mPosOffs >= 0;
		}

		public ElemType getPosType() {
			return mPosType;
		}

		public int getPosElemsNum() {
			if (!hasPos()) return 0;
			return getPosType() == ElemType.USHORT ? 4 : 3;
		}

		public boolean isQuantPos() {
			return mPosType == ElemType.USHORT;
		}

		public int getNrmOffs() {
			return mNrmOffs;
		}

		public boolean hasNrm() {
			return mNrmOffs >= 0;
		}

		public ElemType getNrmType() {
			return mNrmType;
		}

		public int getNrmElemsNum() {
			if (!hasNrm()) return 0;
			return mUseOcta ? 2 : 3;
		}

		public int getTngOffs() {
			return mTngOffs;
		}

		public boolean hasTng() {
			return mTngOffs >= 0;
		}

		public ElemType getTngType() {
			return mNrmType; // always same type as normal
		}

		public int getTngElemsNum() {
			if (!hasTng()) return 0;
			return mUseOcta ? 2 : 3;
		}

		public int getTexOffs() {
			return mTexOffs;
		}

		public boolean hasTex() {
			return mTexOffs >= 0;
		}

		public boolean hasTex2() {
			return hasTex() && mHasTex2;
		}

		public ElemType getTexType() {
			return mTexType;
		}

		public int getTexElemsNum() {
			if (!hasTex()) return 0;
			return mHasTex2 ? 4 : 2;
		}

		public int getClrOffs() {
			return mClrOffs;
		}

		public boolean hasClr() {
			return mClrOffs >= 0;
		}

		public ElemType getClrType() {
			return mClrType;
		}

		public int getClrElemsNum() {
			if (!hasClr()) return 0;
			return mHasAlpha ? 4 : 3;
		}

		public int getJntOffs() {
			return mSkinOffs;
		}

		public int getWgtOffs() {
			if (mSkinOffs < 0) return -1;
			return mSkinOffs + mSkinMode*2;
		}

		public ElemType getJntType() {
			return mSkinOffs >= 0 ? ElemType.USHORT : ElemType.NONE;
		}

		public ElemType getWgtType() {
			return mSkinOffs >= 0 ? ElemType.USHORT : ElemType.NONE;
		}

		public boolean hasAlpha() {
			return hasClr() && mHasAlpha;
		}

		public boolean isOcta() {
			return mUseOcta;
		}

		public float getTexScl() {
			return mTexScl;
		}

		public float getClrScl() {
			return mClrScl;
		}

		public int getByteSize() {
			return mByteSize;
		}

	}

	public enum BatchKind {
		GEO,
		MTL,
		GRP
	}

	public class BatchInfo {
		protected AABB mBBox;
		protected String mName;
		protected BatchKind mKind;
		protected int mMtlId;
		protected int mGrpId;
		protected int mTriNum;
		protected int mMinIdx;
		protected int mMaxIdx;
		protected int mMaxWgtNum;
		protected int mSkinNodeNum;
		protected int[] mSkinNodeIds;
		protected float[] mSkinSphData;
		protected ByteBuffer mVB;
		protected ByteBuffer mIB;

		public boolean isIdx16() {
			return (mMaxIdx - mMinIdx) < (1 << 16);
		}

		public int getIBByteSize() {
			return mTriNum * 3 * (isIdx16() ? 2 : 4);
		}

		public String getName() {
			return mName;
		}

		public BatchKind getKind() {
			return mKind;
		}

		public int getMtlId() {
			return mMtlId;
		}

		public int getGrpId() {
			return mGrpId;
		}

		public int getTriNum() {
			return mTriNum;
		}

		public ByteBuffer getVB() {
			if (mVB != null) {
				mVB.position(0);
			}
			return mVB;
		}

		public ByteBuffer getIB() {
			if (mIB != null) {
				mIB.position(0);
			}
			return mIB;
		}

		public int getMinIdx() {
			return mMinIdx;
		}

		public int getMaxIdx() {
			return mMaxIdx;
		}

		public int getPrivateVBVtxNum() {
			if (GeoData.this.isSharedVB()) return 0;
			return (mMaxIdx - mMinIdx) + 1;
		}

		public int getPrivateVBByteSize() {
			int nvtx = getPrivateVBVtxNum();
			return nvtx * GeoData.this.mVtxInfo.mByteSize;
		}

		public String getMtlName() {
			return GeoData.this.getMtlName(mMtlId);
		}
	}

	protected AABB mBBox;
	protected VtxInfo mVtxInfo;
	protected Vec mQuantScale;
	protected Vec mQuantBase;

	protected ByteBuffer mSharedVB;
	protected int mTotalVtxNum;
	protected int mNumIB16;
	protected int mNumIB32;

	protected BatchInfo[] mBatches;
	protected HashMap<String, Integer> mBatNameToIdx;

	protected int mMtlNum;
	protected String[] mMtlNames;

	protected int mSkinNodesNum;
	protected String[] mSkinNodeNames;
	protected float[] mSkinNodeSpheres;
	protected int[] mSkinToRigMap;
	protected float[] mSkinRestXforms;

	public boolean isSharedVB() {
		return mSharedVB != null;
	}

	public ByteBuffer getSharedVB() {
		if (mSharedVB != null) {
			mSharedVB.position(0);
		}
		return mSharedVB;
	}

	public int getSharedVBByteSize() {
		int size = 0;
		if (isSharedVB()) {
			size = mTotalVtxNum * mVtxInfo.mByteSize;
		}
		return size;
	}

	public int getIB16Num() {
		return mNumIB16;
	}

	public int getIB32Num() {
		return mNumIB32;
	}

	public boolean ckMtlId(int id) {
		return id >= 0 && id < mMtlNum;
	}

	public String getMtlName(int mtlId) {
		String name = null;
		if (ckMtlId(mtlId)) {
			name = mMtlNames[mtlId];
		}
		return name;
	}

	public VtxInfo getVtxInfo() {
		return mVtxInfo;
	}

	public int getBatchesNum() {
		return mBatches != null ? mBatches.length : 0;
	}

	public boolean ckBatchId(int id) {
		return id >= 0 && id < getBatchesNum();
	}

	public BatchInfo getBatchInfo(int id) {
		return ckBatchId(id) ? mBatches[id] : null;
	}

	public BatchInfo getBatchInfo(String name) {
		return getBatchInfo(findBatch(name));
	}

	public int findBatch(String name) {
		int id = -1;
		if (mBatNameToIdx != null) {
			Integer i = mBatNameToIdx.get(name);
			if (i != null) {
				id = i;
			}
		}
		return id;
	}

	public int getBatchMtlId(int batId) {
		int mtlId = -1;
		BatchInfo bat = getBatchInfo(batId);
		if (bat != null) {
			mtlId = bat.mMtlId;
		}
		return mtlId;
	}

	public int getBatchTriNum(int batId) {
		int n = 0;
		BatchInfo bat = getBatchInfo(batId);
		if (bat != null) {
			n = bat.getTriNum();
		}
		return n;
	}

	public void getBatchBoxes(float[] minmax, int offs) {
		int n = getBatchesNum();
		if (n < 1) return;
		for (int i = 0; i < n; ++i) {
			BatchInfo bat = mBatches[i];
			int idst = offs + i*6;
			for (int j = 0; j < 6; ++j) {
				minmax[idst + j] = bat.mBBox.mMinMax[j];
			}
		}
	}

	public void getBatchBoxes(float[] minmax) {
		getBatchBoxes(minmax, 0);
	}

	public void getXformedBatchBoxes(Mtx mtx, float[] minmax, int offs) {
		int n = getBatchesNum();
		if (n < 1) return;
		getBatchBoxes(minmax, offs);
		for (int i = 0; i < n; ++i) {
			int idst = offs + i*6;
			Calc.xformAABB(minmax, idst, minmax, idst, mtx.el, 0);
		}
	}

	public void getXformedBatchBoxes(Mtx mtx, float[] minmax) {
		getXformedBatchBoxes(mtx, minmax, 0);
	}

	public int getMtlsNum() {
		return mMtlNum;
	}

	public Vec getQuantBase() {
		if (mQuantBase == null) {
			return new Vec(0.0f, 0.0f, 0.0f);
		}
		return mQuantBase;
	}

	public Vec getQuantScale() {
		if (mQuantScale == null) {
			return new Vec(1.0f, 1.0f, 1.0f);
		}
		return mQuantScale;
	}

	protected static void putf(ByteBuffer bb, float[] ary) {
		for (float val : ary) {
			bb.putFloat(val);
		}
	}

	public void fromXGeo(XGeo geo, GeoCfg cfg) {
		fromXGeo(geo, cfg, null);
	}

	public void fromXGeo(XGeo geo, GeoCfg cfg, XRig rig) {
		if (geo == null) return;
		if (!geo.isAllTris()) return;
		mBBox = new AABB(geo.mBBox);
		mQuantScale = mBBox.getSize();
		mQuantBase = mBBox.getMin();
		mVtxInfo = new VtxInfo();
		int pkLvl = cfg.mPackLevel;
		if (pkLvl > 3) pkLvl = 3;

		StringBuilder vtag = new StringBuilder();

		mVtxInfo.mTexScl = 1.0f;
		mVtxInfo.mClrScl = 1.0f;
		if (pkLvl >= 2) {
			if (!cfg.mUseHalf) {
				mVtxInfo.mTexScl = (float)(1 << 12);
				mVtxInfo.mClrScl = (float)(1 << 10);
			}
		}

		int vtxByteSize = 0;
		ByteOrder bord = ByteOrder.nativeOrder();

		mVtxInfo.mPosOffs = 0;
		boolean useRelPos = pkLvl >= 3;
		if (useRelPos) {
			vtxByteSize += 2 * 4; // ushort4
			mVtxInfo.mPosType = ElemType.USHORT;
			vtag.append("Pq");
		} else {
			vtxByteSize += 4 * 3; // float3
			mVtxInfo.mPosType = ElemType.FLOAT;
			vtag.append("PV");
		}

		boolean useOcta = pkLvl >= 1;
		mVtxInfo.mUseOcta = useOcta;
		boolean useNrm = cfg.mNrmMode == GeoCfg.AttrMode.FORCE;
		if (cfg.mNrmMode == GeoCfg.AttrMode.AUTO) {
			useNrm = geo.hasPntNormals();
		}
		boolean useTng = false;
		if (useNrm) {
			mVtxInfo.mNrmOffs = vtxByteSize;
			if (useOcta) {
				if (pkLvl >= 2) {
					mVtxInfo.mNrmType = ElemType.SHORT;
					vtxByteSize += 2 * 2; // short2
					vtag.append("No");
				} else {
					mVtxInfo.mNrmType = ElemType.FLOAT;
					vtxByteSize += 4 * 2; // float2
					vtag.append("NO");
				}
			} else {
				mVtxInfo.mNrmType = ElemType.FLOAT;
				vtxByteSize += 4 * 3; // float3
				vtag.append("NV");
			}
			useTng = cfg.mTngMode == GeoCfg.AttrMode.FORCE;
			if (cfg.mTngMode == GeoCfg.AttrMode.AUTO) {
				useTng = geo.hasPntTangents();
			}
			if (useTng) {
				mVtxInfo.mTngOffs = vtxByteSize;
				if (useOcta) {
					if (pkLvl >= 2) {
						vtxByteSize += 2 * 2; // short2
						vtag.append("To");
					} else {
						vtxByteSize += 4 * 2; // float2
						vtag.append("TO");
					}
				} else {
					vtxByteSize += 4 * 3; // float3
					vtag.append("TV");
				}
			}
		}

		boolean useTex = cfg.mTexMode == GeoCfg.AttrMode.FORCE;
		if (cfg.mTexMode == GeoCfg.AttrMode.AUTO) {
			useTex = geo.hasUV();
		}
		if (useTex) {
			mVtxInfo.mTexOffs = vtxByteSize;
			mVtxInfo.mHasTex2 = (cfg.mTex2Mode == GeoCfg.AttrMode.AUTO && geo.hasUV2())
			                     || cfg.mTex2Mode == GeoCfg.AttrMode.FORCE;
			int texElemsNum = mVtxInfo.mHasTex2 ? 4 : 2;
			if (pkLvl >= 2) {
				if (cfg.mUseHalf) {
					mVtxInfo.mTexType = ElemType.HALF;
				} else {
					mVtxInfo.mTexType = ElemType.SHORT;
				}
				vtxByteSize += 2 * texElemsNum; // half|short2|4
			} else {
				mVtxInfo.mTexType = ElemType.FLOAT;
				vtxByteSize += 4 * texElemsNum; // float2|4
			}
			if (texElemsNum > 2) {
				vtag.append("T");
			} else {
				vtag.append("t");
			}
			switch (mVtxInfo.mTexType) {
				case HALF:
					vtag.append("h");
					break;
				case SHORT:
					vtag.append("s");
					break;
				default:
					vtag.append("f");
					break;
			}
		}

		boolean useClr = cfg.mClrMode == GeoCfg.AttrMode.FORCE;
		if (cfg.mClrMode == GeoCfg.AttrMode.AUTO) {
			useClr = geo.hasPntColors();
		}
		if (useClr) {
			mVtxInfo.mClrOffs = vtxByteSize;
			if (pkLvl >= 2) {
				if (cfg.mUseHalf) {
					mVtxInfo.mClrType = ElemType.HALF;
					vtag.append("Ch");
				} else {
					mVtxInfo.mClrType = ElemType.SHORT;
					vtag.append("Cs");
				}
				mVtxInfo.mHasAlpha = true;
				vtxByteSize += 2 * 4; // half|short4
			} else {
				mVtxInfo.mClrType = ElemType.FLOAT;
				mVtxInfo.mHasAlpha = geo.hasPntAlphas();
				if (mVtxInfo.mHasAlpha) {
					vtxByteSize += 4 * 4; // float4
					vtag.append("C4");
				} else {
					vtxByteSize += 4 * 3; // float3
					vtag.append("C3");
				}
			}
		}

		boolean skinFlg = rig != null && cfg.mUseSkin && geo.hasSkin();
		int skinMode = 0;
		if (skinFlg) {
			mVtxInfo.mSkinOffs = vtxByteSize;
			if (geo.getMaxSkinWgtNum() > 4) {
				skinMode = 8;
			} else {
				skinMode = 4;
			}
			vtxByteSize += skinMode * 2 * 2; // ushort4 jnt[], ushort4 wgt[]
			vtag.append("S");
			vtag.append(skinMode);
		}
		mVtxInfo.mSkinMode = skinMode;

		mVtxInfo.mTag = vtag.toString();

		mVtxInfo.mByteSize = vtxByteSize;
		int npnt = geo.getPntNum();
		float[] tngs = null;
		if (useTng && !geo.hasPntTangents()) {
			tngs = new float[npnt * 3];
			geo.calcTangentsFromUVs(tngs);
		}
		int vbSize = mVtxInfo.mByteSize * npnt;
		ByteBuffer vb = ByteBuffer.allocateDirect(vbSize);
		vb.order(bord);
		vb.position(0);
		float[] tf3 = new float[3];
		float[] tf2 = new float[2];
		float[] wgt = null;
		int[] jnt = null;
		if (skinFlg) {
			wgt = new float[skinMode];
			jnt = new int[skinMode];
		}
		for (int i = 0; i < npnt; ++i) {
			geo.getPnt(tf3, 0, i);
			if (useRelPos) {
				Calc.vsub(tf3, mQuantBase.el);
				Calc.vdiv(tf3, mQuantScale.el);
				Calc.vscl(tf3, 0xFFFF);
				for (int j = 0; j < 3; ++j) {
					vb.putShort((short)((int)tf3[j] & 0xFFFF));
				}
				vb.putShort((short)i);
			} else {
				putf(vb, tf3);
			}

			if (useNrm) {
				if (useOcta) {
					geo.getPntNormalOcta(tf2, 0, i);
					if (pkLvl >= 2) {
						for (int j = 0; j < 2; ++j) {
							vb.putShort((short)(tf2[j] * Short.MAX_VALUE));
						}
					} else {
						putf(vb, tf2);
					}
				} else {
					geo.getPntNormal(tf3, 0, i);
					putf(vb, tf3);
				}

				if (useTng) {
					if (useOcta) {
						if (tngs != null) {
							for (int j = 0; j < 3; ++j) {
								tf3[j] = tngs[i*3 + j];
							}
							Calc.encodeOcta(tf2, tf3);
						} else {
							geo.getPntTangentOcta(tf2, 0, i);
						}
						if (pkLvl >= 2) {
							for (int j = 0; j < 2; ++j) {
								vb.putShort((short)(tf2[j] * Short.MAX_VALUE));
							}
						} else {
							putf(vb, tf2);
						}
					} else {
						if (tngs != null) {
							for (int j = 0; j < 3; ++j) {
								tf3[j] = tngs[i * 3 + j];
							}
						} else {
							geo.getPntTangent(tf3, 0, i);
						}
						putf(vb, tf3);
					}
				}
			}

			if (useTex) {
				for (int k = 0; k < 2; ++k) {
					if (k > 0) {
						if (mVtxInfo.mHasTex2) {
							geo.getPntTex2(tf2, 0, i);
						} else {
							break;
						}
					} else {
						geo.getPntTex(tf2, 0, i);
					}
					tf2[1] = 1.0f - tf2[1];
					if (pkLvl >= 2) {
						if (cfg.mUseHalf) {
							for (int j = 0; j < 2; ++j) {
								vb.putShort(Util.floatToHalf(tf2[j]));
							}
						} else {
							for (int j = 0; j < 2; ++j) {
								vb.putShort((short)(tf2[j] * mVtxInfo.mTexScl));
							}
						}
					} else {
						putf(vb, tf2);
					}
				}
			}

			if (useClr) {
				geo.getPntColor(tf3, 0, i);
				if (pkLvl >= 2) {
					float alpha = geo.getPntAlpha(i);
					if (cfg.mUseHalf) {
						for (int j = 0; j < 3; ++j) {
							vb.putShort(Util.floatToHalf(tf3[j]));
						}
						vb.putShort(Util.floatToHalf(alpha));
					} else {
						for (int j = 0; j < 3; ++j) {
							vb.putShort((short)(tf3[j] * mVtxInfo.mClrScl));
						}
						vb.putShort((short)(alpha * mVtxInfo.mClrScl));
					}
				} else {
					putf(vb, tf3);
					if (mVtxInfo.mHasAlpha) {
						vb.putFloat(geo.getPntAlpha(i));
					}
				}
			}

			if (skinFlg) {
				int nwgt = geo.getPntSkin(i, jnt, 0, wgt, 0);
				for (int j = 0; j < nwgt; ++j) {
					vb.putShort((short)(jnt[j] * 3));
				}
				short padJnt = (short)(jnt[0] * 3);
				for (int j = 0; j < skinMode - nwgt; ++j) {
					vb.putShort(padJnt);
				}
				for (int j = 0; j < nwgt; ++j) {
					int usw = Calc.clamp((int)(wgt[j] * 0xFFFF), 0, 0xFFFF);
					vb.putShort((short)usw);
				}
				for (int j = 0; j < skinMode - nwgt; ++j) {
					vb.putShort((short)0);
				}
			}
		} // pts

		int nmtl = geo.getMtlNum();
		int nbat = nmtl;
		if (nbat == 0) nbat = 1;
		int batGrpCnt = 0;
		int polGrpNum = geo.getPolGrpNum();
		boolean batGrpFlg = cfg.mBatchGrpPrefix != null && cfg.mBatchGrpPrefix.length() > 0 && polGrpNum > 0;
		if (batGrpFlg) {
			for (int i = 0; i < polGrpNum; ++i) {
				XGeo.GrpInfo polGrp = geo.getPolGrpInfo(i);
				if (polGrp != null && polGrp.mName.startsWith(cfg.mBatchGrpPrefix)) {
					++batGrpCnt;
				}
			}
		}
		batGrpFlg = batGrpCnt > 0;
		if (batGrpFlg) {
			nbat = batGrpCnt;
		}
		mBatches = new BatchInfo[nbat];
		if (nbat == 1 && nmtl == 0) {
			mBatches[0] = new BatchInfo();
			mBatches[0].mName = "$all";
			mBatches[0].mKind = BatchKind.GEO;
			mBatches[0].mTriNum = geo.getPolNum();
			mBatches[0].mMinIdx = 0;
			mBatches[0].mMaxIdx = geo.getPntNum() - 1;
			mBatches[0].mMtlId = -1;
			mBatches[0].mGrpId = -1;
			mBatches[0].mSkinNodeNum = geo.getSkinNodesNum();
			mBatches[0].mMaxWgtNum = geo.getMaxSkinWgtNum();
			mBatches[0].mBBox = new AABB(geo.mBBox);
		} else {
			if (batGrpFlg) {
				int ibat = 0;
				for (int i = 0; i < polGrpNum; ++i) {
					XGeo.GrpInfo polGrp = geo.getPolGrpInfo(i);
					if (polGrp != null && polGrp.mName.startsWith(cfg.mBatchGrpPrefix)) {
						mBatches[ibat] = new BatchInfo();
						mBatches[ibat].mName = polGrp.mName;
						mBatches[ibat].mKind = BatchKind.GRP;
						mBatches[ibat].mTriNum = polGrp.mIdxNum;
						mBatches[ibat].mGrpId = i;
						mBatches[ibat].mMtlId = geo.getPolMtlId(polGrp.getIdx(0));
						mBatches[ibat].mSkinNodeNum = polGrp.mSkinNodeNum;
						mBatches[ibat].mMaxWgtNum = polGrp.mMaxWgtNum;
						mBatches[ibat].mBBox = new AABB(polGrp.mBBox);
						++ibat;
					}
				}
			} else {
				for (int i = 0; i < nmtl; ++i) {
					XGeo.GrpInfo mtlGrp = geo.getMtlGrpInfo(i);
					mBatches[i] = new BatchInfo();
					mBatches[i].mName = mtlGrp.mName;
					mBatches[i].mKind = BatchKind.MTL;
					mBatches[i].mTriNum = mtlGrp.mIdxNum;
					mBatches[i].mMtlId = i;
					mBatches[i].mGrpId = -1;
					mBatches[i].mSkinNodeNum = mtlGrp.mSkinNodeNum;
					mBatches[i].mMaxWgtNum = mtlGrp.mMaxWgtNum;
					mBatches[i].mBBox = new AABB(mtlGrp.mBBox);
				}
			}
		}

		int maxBatTris = 0;
		for (int i = 0; i < nbat; ++i) {
			maxBatTris = Math.max(maxBatTris, mBatches[i].mTriNum);
		}

		boolean useRelIdx = true;

		int[] tri = new int[3];
		if (!cfg.mIsSharedVB && nbat > 1) {
			mSharedVB = null;
			mTotalVtxNum = 0;
			int[] usedVtxMap = new int[npnt];
			byte[] vtx = new byte[vtxByteSize];
			for (int i = 0; i < nbat; ++i) {
				Arrays.fill(usedVtxMap, -1);
				BatchInfo bat = mBatches[i];
				boolean isMtl = bat.mKind == BatchKind.MTL;
				XGeo.GrpInfo ginf = isMtl ? geo.getMtlGrpInfo(bat.mMtlId) : geo.getPolGrpInfo(bat.mGrpId);
				int nvtx = 0;
				int ntri = bat.mTriNum;
				int minPntIdx = npnt;
				int maxPntIdx = -1;
				for (int j = 0; j < ntri; ++j) {
					int triIdx = ginf.getIdx(j);
					geo.getPolVtxLst(tri, triIdx);
					for (int k = 0; k < 3; ++k) {
						int vtxId = tri[k];
						if (usedVtxMap[vtxId] < 0) {
							minPntIdx = Math.min(minPntIdx, vtxId);
							maxPntIdx = Math.max(maxPntIdx, vtxId);
							++nvtx;
							usedVtxMap[vtxId] = triIdx;
						}
					}
				}
				mTotalVtxNum += nvtx;
				int vcnt = 0;
				for (int j = minPntIdx; j <= maxPntIdx; ++j) {
					if (usedVtxMap[j] >= 0) {
						usedVtxMap[j] = vcnt;
						++vcnt;
					}
				}
				bat.mMinIdx = 0;
				bat.mMaxIdx = nvtx - 1;
				if (bat.isIdx16()) {
					bat.mIB = ByteBuffer.allocateDirect(ntri * 3 * 2);
				} else {
					bat.mIB = ByteBuffer.allocateDirect(ntri * 3 * 4);
				}
				bat.mIB.order(bord);
				bat.mIB.position(0);
				for (int j = 0; j < ntri; ++j) {
					int triIdx = ginf.getIdx(j);
					geo.getPolVtxLst(tri, triIdx);
					for (int k = 0; k < 3; ++k) {
						int vtxId = tri[k];
						vtxId = usedVtxMap[vtxId];
						if (bat.isIdx16()) {
							bat.mIB.putShort((short)vtxId);
						} else {
							bat.mIB.putInt(vtxId);
						}
					}
				}
				bat.mVB = ByteBuffer.allocateDirect(nvtx * vtxByteSize);
				bat.mVB.order(bord);
				bat.mVB.position(0);
				for (int j = minPntIdx; j <= maxPntIdx; ++j) {
					if (usedVtxMap[j] >= 0) {
						vb.position(j*vtxByteSize);
						vb.get(vtx);
						bat.mVB.put(vtx);
					}
				}
			}
		} else {
			mSharedVB = vb;
			mTotalVtxNum = npnt;
			for (int i = 0; i < nbat; ++i) {
				mBatches[i].mVB = vb;
			}
			if (nbat > 1) {
				for (int i = 0; i < nbat; ++i) {
					BatchInfo bat = mBatches[i];
					int ntri = bat.mTriNum;
					boolean isMtl = bat.mKind == BatchKind.MTL;
					XGeo.GrpInfo ginf = isMtl ? geo.getMtlGrpInfo(bat.mMtlId) : geo.getPolGrpInfo(bat.mGrpId);
					int minPntIdx = npnt - 1;
					int maxPntIdx = -1;
					for (int j = 0; j < ntri; ++j) {
						int triIdx = ginf.getIdx(j);
						geo.getPolVtxLst(tri, triIdx);
						for (int k = 0; k < 3; ++k) {
							int vtxId = tri[k];
							minPntIdx = Math.min(minPntIdx, vtxId);
							maxPntIdx = Math.max(maxPntIdx, vtxId);
						}
					}
					bat.mMinIdx = useRelIdx ? minPntIdx : 0;
					bat.mMaxIdx = maxPntIdx;
					if (bat.isIdx16()) {
						bat.mIB = ByteBuffer.allocateDirect(ntri * 3 * 2);
					} else {
						bat.mIB = ByteBuffer.allocateDirect(ntri * 3 * 4);
					}
					bat.mIB.order(bord);
					bat.mIB.position(0);
					for (int j = 0; j < ntri; ++j) {
						int triIdx = ginf.getIdx(j);
						geo.getPolVtxLst(tri, triIdx);
						for (int k = 0; k < 3; ++k) {
							int vtxId = tri[k] - bat.mMinIdx;
							if (bat.isIdx16()) {
								bat.mIB.putShort((short)vtxId);
							} else {
								bat.mIB.putInt(vtxId);
							}
						}
					}
				}
			} else {
				BatchInfo bat = mBatches[0];
				int ntri = geo.getPolNum();
				if (bat.isIdx16()) {
					bat.mIB = ByteBuffer.allocateDirect(ntri * 3 * 2);
				} else {
					bat.mIB = ByteBuffer.allocateDirect(ntri * 3 * 4);
				}
				bat.mIB.order(bord);
				bat.mIB.position(0);
				for (int i = 0; i < ntri; ++i) {
					geo.getPolVtxLst(tri, i);
					for (int k = 0; k < 3; ++k) {
						int vtxId = tri[k] - bat.mMinIdx;
						if (bat.isIdx16()) {
							bat.mIB.putShort((short)vtxId);
						} else {
							bat.mIB.putInt(vtxId);
						}
					}
				}
			}
		}

		mNumIB16 = 0;
		mNumIB32 = 0;
		for (int i = 0; i < nbat; ++i) {
			if (mBatches[i].isIdx16()) {
				++mNumIB16;
			} else {
				++mNumIB32;
			}
		}

		if (skinFlg) {
			mSkinNodesNum = geo.getSkinNodesNum();
			boolean copySkinData = false;
			mSkinNodeNames = geo.getSkinNodeNames();
			mSkinNodeSpheres = geo.getSkinNodeSpheres();
			for (int i = 0; i < nbat; ++i) {
				BatchInfo bat = mBatches[i];
				if (bat.mKind != BatchKind.GEO) {
					boolean isMtl = bat.mKind == BatchKind.MTL;
					XGeo.GrpInfo ginf = isMtl ? geo.getMtlGrpInfo(bat.mMtlId) : geo.getPolGrpInfo(bat.mGrpId);
					int nn = ginf.mSkinNodeNum;
					if (copySkinData) {
						bat.mSkinNodeIds = new int[nn];
						for (int j = 0; j < nn; ++j) {
							bat.mSkinNodeIds[j] = ginf.mSkinIds[j];
						}
						if (ginf.mSkinSphData != null) {
							int sphSize = ginf.mSkinSphData.length;
							bat.mSkinSphData = new float[sphSize];
							System.arraycopy(ginf.mSkinSphData, 0, bat.mSkinSphData, 0, sphSize);
						}
					} else {
						bat.mSkinNodeIds = ginf.mSkinIds;
						bat.mSkinSphData = ginf.mSkinSphData;
					}
				}
			}
			mSkinToRigMap = rig.getSkinToRigMap(geo);
			mSkinRestXforms = rig.getSkinRestXforms(geo, mSkinToRigMap);
			mVtxInfo.mIsLocalSkin = cfg.mUseLocalSkin && nbat > 1 && mSharedVB == null;
			if (mVtxInfo.mIsLocalSkin) {
				int[] jntMap = new int[mSkinNodesNum * 3];
				for (int i = 0; i < nbat; ++i) {
					Arrays.fill(jntMap, -1);
					BatchInfo bat = mBatches[i];
					int batJntNum = bat.mSkinNodeNum;
					for (int j = 0; j < batJntNum; ++j) {
						jntMap[bat.mSkinNodeIds[j] * 3] = j * 3;
					}
					int nvtx = bat.getPrivateVBVtxNum();
					for (int k = 0; k < nvtx; ++k) {
						int jntOffs = mVtxInfo.mByteSize*k + mVtxInfo.mSkinOffs;
						for (int j = 0; j < skinMode; ++j) {
							int jptr = jntOffs + j*2;
							int jglb = bat.mVB.getShort(jptr) & 0xFFFF;
							int jloc = jntMap[jglb];
							bat.mVB.putShort(jptr, (short)(jloc & 0xFFFF));
						}
					}
				}
			}
		}

		mMtlNum = nmtl;
		if (nmtl > 0) {
			mMtlNames = new String[nmtl];
			for (int i = 0; i < nmtl; ++i) {
				XGeo.GrpInfo mtlGrp = geo.getMtlGrpInfo(i);
				mMtlNames[i] = mtlGrp.mName;
			}
		}

		mBatNameToIdx = new HashMap<String, Integer>();
		for (int i = 0; i < nbat; ++i) {
			mBatNameToIdx.put(mBatches[i].mName, i);
		}
	}

	public int getSkinNodesNum() {
		return mSkinNodesNum;
	}

	public int[] getSkinToRigMap() {
		return mSkinToRigMap;
	}

	public float[] allocSkinXforms() {
		float[] xforms = null;
		int n = getSkinNodesNum();
		if (n > 0) {
			xforms = new float[n * 3*4];
			for (int i = 0; i < n; ++i) {
				Calc.identity34(xforms, i * 3*4);
			}
		}
		return xforms;
	}

	public void calcSkinXforms(float[] xformsJ, float[] xformsW, int[] map) {
		int n = getSkinNodesNum();
		for (int i = 0; i < n; ++i) {
			int idxW = map[i];
			int offsJ = i * 3*4;
			if (idxW < 0) {
				Calc.identity34(xformsJ, offsJ);
			} else {
				Calc.xform3x4Mul(xformsJ, offsJ, mSkinRestXforms, offsJ, xformsW, idxW * 3*4);
			}
		}
	}

	public void calcSkinXforms(float[] xformsJ, float[] xformsW) {
		calcSkinXforms(xformsJ, xformsW, mSkinToRigMap);
	}

}
