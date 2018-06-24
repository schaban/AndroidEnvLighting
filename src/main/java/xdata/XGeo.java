// Author: Sergey Chaban <sergey.chaban@gmail.com>

package xdata;

import java.util.Locale;

public class XGeo extends XData {

	public enum AttrClass {
		GLOBAL,
		POINT,
		POLYGON
	}

	public enum AttrType {
		UNKNOWN,
		INT,
		FLOAT,
		STRING
	}

	public class AttrInfo {
		public String mName;
		public AttrClass mClass;
		public AttrType mType;
		public int mTop;
		public int mElemNum;
		public int mNameId;
	}

	public enum GrpClass {
		MATERIAL,
		POINT,
		POLYGON
	}

	public class GrpInfo {
		public AABB mBBox;
		public String mName;
		public String mPath;
		public int mMinIdx;
		public int mMaxIdx;
		public int mMaxWgtNum;
		public int mSkinNodeNum;
		public int mIdxNum;
		public int mNameId;
		public int mPathId;
		public byte[] mIdxData;
		public float[] mSkinSphData;
		public int[] mSkinIds;

		public int getIdxElemSize() {
			int idxSpan = mMaxIdx - mMinIdx;
			int res = 0;
			if (idxSpan < (1 << 8)) {
				res = 1;
			} else if (idxSpan < (1 << 16)) {
				res = 2;
			} else {
				res = 3;
			}
			return res;
		}

		public boolean ckIdx(int at) {
			return mIdxData != null && at >= 0 && at < mIdxNum;
		}

		public int getRelIdxNoCk(int at) {
			int idx = -1;
			int esize = getIdxElemSize();
			if (esize == 1) {
				idx = mIdxData[at] & 0xFF;
			} else if (esize == 2) {
				idx = mIdxData[at*2] & 0xFF;
				idx |= (mIdxData[at*2 + 1] & 0xFF) << 8;
			} else if (esize == 3) {
				idx = mIdxData[at*3] & 0xFF;
				idx |= (mIdxData[at*3 + 1] & 0xFF) << 8;
				idx |= (mIdxData[at*3 + 2] & 0xFF) << 16;
			}
			return idx;
		}

		public int getRelIdx(int at) {
			int idx = -1;
			if (ckIdx(at)) {
				idx = getRelIdxNoCk(at);
			}
			return idx;
		}

		public int getIdx(int at) {
			int idx = -1;
			if (ckIdx(at)) {
				idx = getRelIdxNoCk(at) + mMinIdx;
			}
			return idx;
		}

	}

	public AABB mBBox;
	protected int mPntNum;
	protected int mPolNum;
	protected int mMtlNum;
	protected int mGlbAttrNum;
	protected int mPntAttrNum;
	protected int mPolAttrNum;
	protected int mPntGrpNum;
	protected int mPolGrpNum;
	protected int mSkinNodeNum;

	protected int mMaxSkinWgtNum;
	protected int mMaxVtxPerPol;

	protected int mPntOffs;
	protected int mPolOffs;
	protected int mMtlOffs;
	protected int mGlbAttrOffs;
	protected int mPntAttrOffs;
	protected int mPolAttrOffs;
	protected int mPntGrpOffs;
	protected int mPolGrpOffs;
	protected int mSkinNodesOffs;
	protected int mSkinOffs;
	protected int mBVHOffs;

	protected float[] mPntData;

	protected byte[] mPolMtlIds;
	protected int[] mPolPtrs;
	protected byte[] mPolVtxCounts;
	protected byte[] mPolData;

	protected int[] mAttrIntData;
	protected float[] mAttrFloatData;
	protected AttrInfo[] mAttrsGlb;
	protected AttrInfo[] mAttrsPnt;
	protected AttrInfo[] mAttrsPol;
	protected int mPntClrAttrId = -1;
	protected int mPntAlfAttrId = -1;
	protected int mPntNrmAttrId = -1;
	protected int mPntTngAttrId = -1;
	protected int mPntTexAttrId = -1;
	protected int mPntTex2AttrId = -1;

	protected GrpInfo[] mMtlGrps;
	protected GrpInfo[] mPntGrps;
	protected GrpInfo[] mPolGrps;

	protected byte[] mSkinWgtNums;
	protected int[] mSkinPtrs;
	protected float[] mSkinWgts;
	protected short[] mSkinJnts;
	protected float[] mSkinNodeSpheres;
	protected int[] mSkinNodeNameIds;

	protected int mBVHNodesNum;
	protected float[] mBVHBoxes;
	protected int[] mBVHTree; // {left, right}


	public void init(Binary bin) {
		super.init(bin);
		if (!mHead.isGeometry()) return;
		mBBox = new AABB();
		int pos = 32;
		mBBox.read(bin, pos);
		pos += 0xC * 2;
		mPntNum = bin.getI32(pos); pos += 4;
		mPolNum = bin.getI32(pos); pos += 4;
		mMtlNum = bin.getI32(pos); pos += 4;
		mGlbAttrNum = bin.getI32(pos); pos += 4;
		mPntAttrNum = bin.getI32(pos); pos += 4;
		mPolAttrNum = bin.getI32(pos); pos += 4;
		mPntGrpNum = bin.getI32(pos); pos += 4;
		mPolGrpNum = bin.getI32(pos); pos += 4;
		mSkinNodeNum = bin.getI32(pos); pos += 4;

		mMaxSkinWgtNum = bin.getI16(pos); pos += 2;
		mMaxVtxPerPol = bin.getI16(pos); pos += 2;

		mPntOffs = bin.getI32(pos); pos += 4;
		mPolOffs = bin.getI32(pos); pos += 4;
		mMtlOffs = bin.getI32(pos); pos += 4;
		mGlbAttrOffs = bin.getI32(pos); pos += 4;
		mPntAttrOffs = bin.getI32(pos); pos += 4;
		mPolAttrOffs = bin.getI32(pos); pos += 4;
		mPntGrpOffs = bin.getI32(pos); pos += 4;
		mPolGrpOffs = bin.getI32(pos); pos += 4;
		mSkinNodesOffs = bin.getI32(pos); pos += 4;
		mSkinOffs = bin.getI32(pos); pos += 4;
		mBVHOffs = bin.getI32(pos);

		if (mPntNum > 0 && mPntOffs > 0) {
			int n = mPntNum * 3;
			mPntData = new float[n];
			for (int i = 0; i < n; ++i) {
				mPntData[i] = bin.getF32(mPntOffs + i*4);
			}
		}

		if (mPolNum > 0 && mPolOffs > 0) {
			int[] polTops = null;
			int polDataSize = 0;
			int polDataOffs = 0;
			if (!isSamePolSize()) {
				int offs = mPolOffs;
				polTops = new int[mPolNum];
				for (int i = 0; i < mPolNum; ++i) {
					polTops[i] = bin.getI32(offs + i*4);
				}
				int ntop = offs + mPolNum*4;
				if (!isSamePolMtl()) {
					ntop += getMtlIdSize() * mPolNum;
				}
				int nsize = mPolNum;
				if (mMaxVtxPerPol >= (1<<8)) {
					nsize *= 2;
				}
				mPolVtxCounts = new byte[nsize];
				bin.getBytes(mPolVtxCounts, ntop);
				for (int i = 0; i < mPolNum; ++i) {
					polDataSize += getPolVtxNum(i);
				}
				polDataSize *= getVtxIdxSize();
				polDataOffs = offs;
			} else {
				polDataSize = mMaxVtxPerPol * mPolNum * getVtxIdxSize();
				polDataOffs = mPolOffs;
				if (!isSamePolMtl()) {
					polDataOffs += mPolNum * getMtlIdSize();
				}
			}
			mPolData = new byte[polDataSize];
			if (isSamePolSize()) {
				bin.getBytes(mPolData, polDataOffs);
			} else if (polTops != null){
				mPolPtrs = new int[mPolNum];
				int ptr = 0;
				for (int i = 0; i < mPolNum; ++i) {
					int nvtx = getPolVtxNum(i);
					int nbytes = nvtx * getVtxIdxSize();
					bin.getBytes(mPolData, ptr, polTops[i], nbytes);
					mPolPtrs[i] = ptr;
					ptr += nbytes;
				}
			}
			if (!isSamePolMtl()) {
				int offs = mPolOffs;
				if (!isSamePolSize()) {
					offs += mPolNum * 4;
				}
				int size = getMtlIdSize();
				if (size > 0) {
					size *= mPolNum;
					mPolMtlIds = new byte[size];
					bin.getBytes(mPolMtlIds, offs);
				}
			}
		}

		int attrIntCnt = 0;
		int attrFloatCnt = 0;
		AttrClass[] attrCls = AttrClass.values(); //{ AttrClass.GLOBAL, AttrClass.POINT, AttrClass.POLYGON };
		for (AttrClass cls : attrCls) {
			int ninfo = getAttrInfoNum(cls);
			int nitem = getAttrItemNum(cls);
			for (int i = 0; i < ninfo; ++i) {
				AttrType t = peekAttrType(bin, cls, i);
				int nelem = peekAttrElemCount(bin, cls, i);
				switch (t) {
					case INT:
					case STRING:
						attrIntCnt += nelem * nitem;
						break;
					case FLOAT:
						attrFloatCnt += nelem * nitem;
						break;
				}
			}
		}
		if (attrIntCnt > 0) {
			mAttrIntData = new int[attrIntCnt];
		}
		if (attrFloatCnt > 0) {
			mAttrFloatData = new float[attrFloatCnt];
		}
		int attrIntPtr = 0;
		int attrFloatPtr = 0;
		for (AttrClass cls : attrCls) {
			int ninfo = getAttrInfoNum(cls);
			int nitem = getAttrItemNum(cls);
			if (ninfo > 0 && nitem > 0) {
				AttrInfo[] attrs = new AttrInfo[ninfo];
				for (int i = 0; i < ninfo; ++i) {
					int nelem = peekAttrElemCount(bin, cls, i);
					attrs[i] = new AttrInfo();
					AttrType t = peekAttrType(bin, cls, i);
					switch (t) {
						case INT:
						case STRING:
							attrs[i].mTop = attrIntPtr;
							break;
						case FLOAT:
							attrs[i].mTop = attrFloatPtr;
							break;
					}
					attrs[i].mNameId = peekAttrNameId(bin, cls, i);
					attrs[i].mClass = cls;
					attrs[i].mType = t;
					attrs[i].mElemNum = nelem;
					attrs[i].mName = getStr(attrs[i].mNameId);
					int offs = peekAttrDataOffs(bin, cls, i);
					for (int j = 0; j < nitem; ++j) {
						switch (t) {
							case INT:
							case STRING:
								for (int k = 0; k < nelem; ++k) {
									mAttrIntData[attrIntPtr++] = bin.getI32(offs);
									offs += 4;
								}
								break;
							case FLOAT:
								for (int k = 0; k < nelem; ++k) {
									mAttrFloatData[attrFloatPtr++] = bin.getF32(offs);
									offs += 4;
								}
								break;
						}
					}
				}
				switch (cls) {
					case GLOBAL:
						mAttrsGlb = attrs;
						break;
					case POINT:
						mAttrsPnt = attrs;
						break;
					case POLYGON:
						mAttrsPol = attrs;
						break;
				}
			}
		}
		mPntClrAttrId = findPntAttrIdx("Cd");
		mPntAlfAttrId = findPntAttrIdx("Alpha");
		mPntNrmAttrId = findPntAttrIdx("N");
		mPntTngAttrId = findPntAttrIdx("tangentu");
		mPntTexAttrId = findPntAttrIdx("uv");
		mPntTex2AttrId = findPntAttrIdx("uv2");

		if (hasSkin()) {
			mSkinWgtNums = new byte[mPntNum];
			bin.getBytes(mSkinWgtNums, mSkinOffs + mPntNum*4);
			mSkinPtrs = new int[mPntNum];
			int skinDataCnt = 0;
			for (int i = 0; i < mPntNum; ++i) {
				mSkinPtrs[i] = skinDataCnt;
				skinDataCnt += mSkinWgtNums[i] & 0xFF;
			}
			mSkinWgts = new float[skinDataCnt];
			mSkinJnts = new short[skinDataCnt];
			int[] skinTbl = new int[mPntNum];
			bin.getInts(skinTbl, mSkinOffs);
			int nn = getSkinNodesNum();
			for (int i = 0; i < mPntNum; ++i) {
				int nwgt = mSkinWgtNums[i] & 0xFF;
				int ptr = mSkinPtrs[i];
				bin.getFloats(mSkinWgts, ptr, skinTbl[i], nwgt);
				int jntIdxTop = skinTbl[i] + nwgt*4;
				for (int j = 0; j < nwgt; ++j) {
					if (nn <= (1 << 8)) {
						mSkinJnts[ptr + j] = (short)bin.getU8(jntIdxTop + j);
					} else {
						mSkinJnts[ptr + j] = (short)bin.getI16(jntIdxTop + j*2);
					}
				}
			}
		}

		if (hasSkinNodes()) {
			int nn = mSkinNodeNum;
			mSkinNodeSpheres = new float[nn*4];
			bin.getFloats(mSkinNodeSpheres, mSkinNodesOffs);
			mSkinNodeNameIds = new int[nn];
			bin.getInts(mSkinNodeNameIds, mSkinNodesOffs + nn*0x10);
		}

		for (GrpClass grpCls : GrpClass.values()) {
			int ngrp = getGrpNum(grpCls);
			int grpTblOffs = getGrpTblOffs(grpCls);
			if (ngrp > 0 && grpTblOffs > 0) {
				GrpInfo[] grpInfos = new GrpInfo[ngrp];
				for (int i = 0; i < ngrp; ++i) {
					grpInfos[i] = new GrpInfo();
					int grpInfoTop = bin.getI32(grpTblOffs + i*4);
					if (grpInfoTop > 0) {
						grpInfos[i].mNameId = bin.getI16(grpInfoTop);
						grpInfos[i].mPathId = bin.getI16(grpInfoTop + 2);
						grpInfos[i].mBBox = new AABB();
						grpInfos[i].mBBox.read(bin, grpInfoTop + 4);
						grpInfos[i].mMinIdx = bin.getI32(grpInfoTop + 0x1C);
						grpInfos[i].mMaxIdx = bin.getI32(grpInfoTop + 0x20);
						grpInfos[i].mMaxWgtNum = bin.getU16(grpInfoTop + 0x24);
						grpInfos[i].mSkinNodeNum = bin.getU16(grpInfoTop + 0x26);
						grpInfos[i].mIdxNum = bin.getI32(grpInfoTop + 0x28);
						grpInfos[i].mName = getStr(grpInfos[i].mNameId);
						grpInfos[i].mPath = getStr(grpInfos[i].mPathId);
					}
				}
				for (int i = 0; i < ngrp; ++i) {
					GrpInfo ginf = grpInfos[i];
					int grpInfoTop = bin.getI32(grpTblOffs + i*4);
					int idxDataSize = ginf.getIdxElemSize() * ginf.mIdxNum;
					int idxDataTop = grpInfoTop + 0x2C;
					int nskn = ginf.mSkinNodeNum;
					if (nskn > 0) {
						if (hasSkinSpheres()) {
							ginf.mSkinSphData = new float[nskn * 4];
							int sphDataTop = idxDataTop;
							bin.getFloats(ginf.mSkinSphData, sphDataTop);
							idxDataTop += 0x10 * nskn;
						}
						int skinIdsDataTop = idxDataTop;
						ginf.mSkinIds = new int[nskn];
						for (int j = 0; j < nskn; ++j) {
							ginf.mSkinIds[j] = bin.getU16(skinIdsDataTop + j*2);
						}
						idxDataTop += 2 * nskn;
					}
					ginf.mIdxData = new byte[idxDataSize];
					bin.getBytes(ginf.mIdxData, idxDataTop);
				}
				switch (grpCls) {
					case MATERIAL:
						mMtlGrps = grpInfos;
						break;
					case POINT:
						mPntGrps = grpInfos;
						break;
					case POLYGON:
						mPolGrps = grpInfos;
						break;
				}
			}
		}

		if (hasBVH()) {
			int nbvh = bin.getI32(mBVHOffs);
			mBVHNodesNum = nbvh;
			mBVHBoxes = new float[nbvh * 6];
			mBVHTree = new int[nbvh * 2];
			int nodesTop = mBVHOffs + 0x10;
			for (int i = 0; i < nbvh; ++i) {
				int nodeOffs = nodesTop + i*8*4;
				bin.getFloats(mBVHBoxes, i*6, nodeOffs, 6);
				bin.getInts(mBVHTree, i*2, nodeOffs + 6*4, 2);
			}
		} // BVH
	}

	public void reset() {
		mPntData = null;

		mPolMtlIds = null;
		mPolPtrs = null;
		mPolVtxCounts = null;
		mPolData = null;

		mBBox.reset();

		super.reset();
	}

	public boolean isSamePolSize() { return mHead.ckFlgMask(1); }
	public boolean isSamePolMtl() { return mHead.ckFlgMask(2); }
	public boolean hasSkinSpheres() { return mHead.ckFlgMask(4); }
	public boolean isAllTris() { return isSamePolSize() && mMaxVtxPerPol == 3; }
	public boolean allQuadsPlanarConvex() { return mHead.ckFlgMask(8); }
	public boolean hasSkin() { return mSkinOffs != 0; }
	public boolean hasSkinNodes() { return mSkinNodesOffs != 0; }
	public boolean hasBVH() { return mBVHOffs != 0; }

	public int getSkinNodesNum() {
		return mSkinNodeNum;
	}

	public int getMaxSkinWgtNum() {
		return mMaxSkinWgtNum;
	}

	public float[] getSkinNodeSpheres() {
		return mSkinNodeSpheres;
	}

	public String[] getSkinNodeNames() {
		String[] names = null;
		if (hasSkinNodes()) {
			int n = getSkinNodesNum();
			names = new String[n];
			for (int i = 0; i < n; ++i) {
				names[i] = getStr(mSkinNodeNameIds[i]);
			}
		}
		return names;
	}

	public int getPntNum() { return mPntNum; }
	public int getPolNum() { return mPolNum; }
	public int getMtlNum() { return mMtlNum; }

	public boolean ckPntIdx(int idx) {
		return idx >= 0 && idx < mPntNum;
	}

	public boolean ckPolIdx(int idx) {
		return idx >= 0 && idx < mPolNum;
	}

	public boolean ckMtlIdx(int idx) {
		return idx >= 0 && idx < mMtlNum;
	}

	public int getVtxIdxSize() {
		int size = 0;
		if (mHead != null) {
			if (mPntNum <= (1 << 8)) size = 1;
			else if (mPntNum <= (1 << 16)) size = 2;
			else size = 3;
		}
		return size;
	}

	public int getMtlIdSize() {
		int size = 0;
		if (mHead != null) {
			size = (mMtlNum < (1<<7)) ? 1 : 2;
		}
		return size;
	}

	public int getPolMtlId(int idx) {
		int mtlId = -1;
		if (mMtlNum > 0) {
			if (isSamePolMtl()) {
				mtlId = 0;
			} else {
				if (mPolMtlIds != null && ckPolIdx(idx)) {
					int size = getMtlIdSize();
					if (size == 1) {
						mtlId = mPolMtlIds[idx] & 0xFF;
					} else {
						mtlId = mPolMtlIds[idx*2] & 0xFF;
						mtlId |= (mPolMtlIds[idx*2 + 1] & 0xFF) << 8;
					}
				}
			}
		}
		return mtlId;
	}

	public boolean hasPntNormals() {
		return mPntNrmAttrId >= 0;
	}

	public boolean hasPntTangents() {
		return mPntTngAttrId >= 0;
	}

	public boolean hasPntColors() {
		return mPntClrAttrId >= 0;
	}

	public boolean hasPntAlphas() {
		return mPntAlfAttrId >= 0;
	}

	public boolean hasPntTex() {
		return mPntTexAttrId >= 0;
	}

	public boolean hasPntTex2() {
		return mPntTex2AttrId >= 0;
	}

	public boolean hasUV() {
		return hasPntTex();
	}

	public boolean hasUV2() {
		return hasPntTex2();
	}

	public void getPnt(Vec dst, int idx) {
		if (mPntData != null && ckPntIdx(idx)) {
			int org = idx*3;
			dst.set(mPntData[org], mPntData[org + 1], mPntData[org + 2]);
		} else {
			dst.fill(0.0f);
		}
	}

	public Vec getPnt(int idx) {
		Vec pnt = new Vec();
		getPnt(pnt, idx);
		return pnt;
	}

	public void getPnt(float[] dst, int dstOffs, int idx) {
		float x = 0.0f;
		float y = 0.0f;
		float z = 0.0f;
		if (mPntData != null && ckPntIdx(idx)) {
			int org = idx * 3;
			x = mPntData[org];
			y = mPntData[org + 1];
			z = mPntData[org + 2];
		}
		dst[dstOffs] = x;
		dst[dstOffs + 1] = y;
		dst[dstOffs + 2] = z;
	}

	public void getPntNormal(float[] dst, int dstOffs, int idx) {
		float x = 0.0f;
		float y = 1.0f;
		float z = 0.0f;
		if (mPntNrmAttrId >= 0 && ckPntIdx(idx)) {
			AttrInfo info = mAttrsPnt[mPntNrmAttrId];
			x = getAttrValF(info, idx, 0, x);
			y = getAttrValF(info, idx, 1, y);
			z = getAttrValF(info, idx, 2, z);
		}
		dst[dstOffs] = x;
		dst[dstOffs + 1] = y;
		dst[dstOffs + 2] = z;
	}

	public void getPntNormal(Vec dst, int idx) {
		getPntNormal(dst.el, 0, idx);
	}

	public Vec getPntNormal(int idx) {
		Vec n = new Vec();
		getPntNormal(n, idx);
		return n;
	}

	public void getPntNormalOcta(float[] dst, int dstOffs, int idx) {
		float x = 0.0f;
		float y = 1.0f;
		float z = 0.0f;
		if (mPntNrmAttrId >= 0 && ckPntIdx(idx)) {
			AttrInfo info = mAttrsPnt[mPntNrmAttrId];
			x = getAttrValF(info, idx, 0, x);
			y = getAttrValF(info, idx, 1, y);
			z = getAttrValF(info, idx, 2, z);
		}
		Calc.encodeOcta(dst, dstOffs, x, y, z);
	}

	public void getPntTangent(float[] dst, int dstOffs, int idx) {
		float x = 0.0f;
		float y = 0.0f;
		float z = 1.0f;
		if (mPntTngAttrId >= 0 && ckPntIdx(idx)) {
			AttrInfo info = mAttrsPnt[mPntTngAttrId];
			x = getAttrValF(info, idx, 0, x);
			y = getAttrValF(info, idx, 1, y);
			z = getAttrValF(info, idx, 2, z);
		}
		dst[dstOffs] = x;
		dst[dstOffs + 1] = y;
		dst[dstOffs + 2] = z;
	}

	public void getPntTangent(Vec dst, int idx) {
		getPntTangent(dst.el, 0, idx);
	}

	public Vec getPntTangent(int idx) {
		Vec n = new Vec();
		getPntTangent(n, idx);
		return n;
	}

	public void getPntTangentOcta(float[] dst, int dstOffs, int idx) {
		float x = 0.0f;
		float y = 0.0f;
		float z = 1.0f;
		if (mPntTngAttrId >= 0 && ckPntIdx(idx)) {
			AttrInfo info = mAttrsPnt[mPntTngAttrId];
			x = getAttrValF(info, idx, 0, x);
			y = getAttrValF(info, idx, 1, y);
			z = getAttrValF(info, idx, 2, z);
		}
		Calc.encodeOcta(dst, dstOffs, x, y, z);
	}

	public void getPntColor(float[] dst, int dstOffs, int idx) {
		float r = 1.0f;
		float g = 1.0f;
		float b = 1.0f;
		if (mPntClrAttrId >= 0 && ckPntIdx(idx)) {
			AttrInfo info = mAttrsPnt[mPntClrAttrId];
			r = getAttrValF(info, idx, 0, r);
			g = getAttrValF(info, idx, 1, g);
			b = getAttrValF(info, idx, 2, b);
		}
		dst[dstOffs] = r;
		dst[dstOffs + 1] = g;
		dst[dstOffs + 2] = b;
	}

	public void getPntColor(Color clr, int idx) {
		getPntColor(clr.ch, 0, idx);
	}

	public Color getPntColor(int idx) {
		Color c = new Color();
		getPntColor(c, idx);
		return c;
	}

	public float getPntAlpha(int idx) {
		float alpha = 1.0f;
		if (mPntAlfAttrId >= 0 && ckPntIdx(idx)) {
			AttrInfo info = mAttrsPnt[mPntAlfAttrId];
			alpha = getAttrValF(info, idx, 0, 1.0f);
		}
		return alpha;
	}

	public void getPntTex(float[] dst, int dstOffs, int idx) {
		float u = 0.0f;
		float v = 0.0f;
		if (mPntTexAttrId >= 0 && ckPntIdx(idx)) {
			AttrInfo info = mAttrsPnt[mPntTexAttrId];
			u = getAttrValF(info, idx, 0, 0.0f);
			v = getAttrValF(info, idx, 1, 0.0f);
		}
		dst[dstOffs] = u;
		dst[dstOffs + 1] = v;
	}

	public void getPntTex2(float[] dst, int dstOffs, int idx) {
		if (hasUV2()) {
			float u = 0.0f;
			float v = 0.0f;
			if (ckPntIdx(idx)) {
				AttrInfo info = mAttrsPnt[mPntTex2AttrId];
				u = getAttrValF(info, idx, 0, 0.0f);
				v = getAttrValF(info, idx, 1, 0.0f);
			}
			dst[dstOffs] = u;
			dst[dstOffs + 1] = v;
		} else {
			getPntTex(dst, dstOffs, idx);
		}
	}

	public int getPntSkinWgtNum(int idx) {
		int n = 0;
		if (hasSkin() && ckPntIdx(idx)) {
			n = mSkinWgtNums[idx];
		}
		return n;
	}

	public int getPntSkinWgtNoCk(int idx, float[] wgt, int offs) {
		int n = mSkinWgtNums[idx];
		int ptr = mSkinPtrs[idx];
		for (int i = 0; i < n; ++i) {
			wgt[offs + i] = mSkinWgts[ptr + i];
		}
		return n;
	}

	public int getPntSkinWgt(int idx, float[] wgt, int offs) {
		int n = 0;
		if (hasSkin() && ckPntIdx(idx)) {
			n = getPntSkinWgtNoCk(idx, wgt, offs);
		}
		return n;
	}

	public int getPntSkinJntNoCk(int idx, int[] jnt, int offs) {
		int n = mSkinWgtNums[idx];
		int ptr = mSkinPtrs[idx];
		for (int i = 0; i < n; ++i) {
			jnt[offs + i] = mSkinJnts[ptr + i] & 0xFFFF;
		}
		return n;
	}

	public int getPntSkinJnt(int idx, int[] jnt, int offs) {
		int n = 0;
		if (hasSkin() && ckPntIdx(idx)) {
			n = getPntSkinJntNoCk(idx, jnt, offs);
		}
		return n;
	}

	public int getPntSkinNoCk(int idx, int[] jnt, int jntOffs, float[] wgt, int wgtOffs) {
		int n = mSkinWgtNums[idx];
		int ptr = mSkinPtrs[idx];
		for (int i = 0; i < n; ++i) {
			jnt[jntOffs + i] = mSkinJnts[ptr + i];
		}
		for (int i = 0; i < n; ++i) {
			wgt[wgtOffs + i] = mSkinWgts[ptr + i];
		}
		return n;
	}

	public int getPntSkin(int idx, int[] jnt, int jntOffs, float[] wgt, int wgtOffs) {
		int n = 0;
		if (hasSkin() && ckPntIdx(idx)) {
			n = getPntSkinNoCk(idx, jnt, jntOffs, wgt, wgtOffs);
		}
		return n;
	}

	public int getPolVtxNum(int idx) {
		if (idx < 0 || idx >= mPolNum) return 0;
		if (isSamePolSize()) {
			return mMaxVtxPerPol;
		}
		int n = 0;
		if (mPolVtxCounts != null) {
			if (mMaxVtxPerPol < (1 << 8)) {
				n = mPolVtxCounts[idx] & 0xFF;
			} else {
				int b0 = mPolVtxCounts[idx*2] & 0xFF;
				int b1 = mPolVtxCounts[idx*2 + 1] & 0xFF;
				n = b0 | (b1 << 8);
			}
		}
		return n;
	}

	protected int getVtxPntId(int ptr) {
		int id = 0;
		int vsize = getVtxIdxSize();
		switch (vsize) {
			case 1:
				id = mPolData[ptr] & 0xFF;
				break;
			case 2:
				id = mPolData[ptr] & 0xFF;
				id |= (mPolData[ptr + 1] & 0xFF) << 8;
				break;
			case 3:
				id = mPolData[ptr] & 0xFF;
				id |= (mPolData[ptr + 1] & 0xFF) << 8;
				id |= (mPolData[ptr + 2] & 0xFF) << 16;
				break;
		}
		return id;
	}

	public int getPolVtxLst(int[] vlst, int idx) {
		if (mPolData == null) return 0;
		int n = getPolVtxNum(idx);
		if (n < 1 || n > vlst.length) return 0;
		int ptr = 0;
		int vsize = getVtxIdxSize();
		if (isSamePolSize()) {
			ptr = idx * vsize * mMaxVtxPerPol;
		} else if (mPolPtrs != null) {
			ptr = mPolPtrs[idx];
		} else {
			return 0;
		}
		for (int i = 0; i < n; ++i) {
			vlst[i] = getVtxPntId(ptr);
			ptr += vsize;
		}
		return n;
	}

	public int[] getPolVtxLst(int idx) {
		int n = getPolVtxNum(idx);
		if (n < 1) return null;
		int[] vlst = new int[n];
		getPolVtxLst(vlst, idx);
		return vlst;
	}

	public int getAttrItemNum(AttrClass c) {
		int n = 0;
		switch (c) {
			case GLOBAL: n = 1; break;
			case POINT: n = mPntNum; break;
			case POLYGON: n = mPolNum; break;
		}
		return n;
	}

	public int getAttrInfoNum(AttrClass cls) {
		int n = 0;
		switch (cls) {
			case GLOBAL: n = mGlbAttrNum; break;
			case POINT: n = mPntAttrNum; break;
			case POLYGON: n = mPolAttrNum; break;
		}
		return n;
	}

	public int getGlbAttrNum() { return mAttrsGlb != null ? mAttrsGlb.length : 0; }
	public int getPntAttrNum() { return mAttrsPnt != null ? mAttrsPnt.length : 0; }
	public int getPolAttrNum() { return mAttrsPol != null ? mAttrsPol.length : 0; }

	private int getAttrInfoOffs(AttrClass cls) {
		int offs = 0;
		switch (cls) {
			case GLOBAL: offs = mGlbAttrOffs; break;
			case POINT: offs = mPntAttrOffs; break;
			case POLYGON: offs = mPolAttrOffs; break;
		}
		return offs;
	}

	private int getAttrInfoOffs(AttrClass cls, int idx) {
		int offs = getAttrInfoOffs(cls);
		if (offs > 0) {
			int n = getAttrInfoNum(cls);
			if (idx >= 0 && idx < n) {
				offs += idx * 0xC;
			} else {
				offs = 0;
			}
		}
		return offs;
	}

	private AttrType peekAttrType(Binary bin, AttrClass cls, int idx) {
		AttrType t = AttrType.UNKNOWN;
		int offs = getAttrInfoOffs(cls, idx);
		if (offs > 0) {
			int traw = bin.getU8(offs + 0xA);
			switch (traw) {
				case 0: t = AttrType.INT; break;
				case 1: t = AttrType.FLOAT; break;
				case 2: t = AttrType.STRING; break;
			}
		}
		return t;
	}

	private int peekAttrElemCount(Binary bin, AttrClass cls, int idx) {
		int n = 0;
		int offs = getAttrInfoOffs(cls, idx);
		if (offs > 0) {
			n = bin.getU16(offs + 8);
		}
		return n;
	}

	private int peekAttrDataOffs(Binary bin, AttrClass cls, int idx) {
		int data = 0;
		int offs = getAttrInfoOffs(cls, idx);
		if (offs > 0) {
			data = bin.getI32(offs);
		}
		return data;
	}

	private int peekAttrNameId(Binary bin, AttrClass cls, int idx) {
		int id = -1;
		int offs = getAttrInfoOffs(cls, idx);
		if (offs > 0) {
			id = bin.getI32(offs + 4);
		}
		return id;
	}

	public AttrInfo[] getClassAttrs(AttrClass cls) {
		AttrInfo[] infos = null;
		switch (cls) {
			case GLOBAL: infos = mAttrsGlb; break;
			case POINT: infos = mAttrsPnt; break;
			case POLYGON: infos = mAttrsPol; break;
		}
		return infos;
	}

	public int findAttrIdx(String name, AttrClass cls) {
		int idx = -1;
		AttrInfo[] infos = getClassAttrs(cls);
		if (infos != null) {
			int n = infos.length;
			int nameIdx = findStrIdx(name);
			for (int i = 0; i < n; ++i) {
				if (infos[i].mNameId == nameIdx) {
					idx = i;
					break;
				}
			}
		}
		return idx;
	}

	public AttrInfo findAttr(String name, AttrClass cls) {
		AttrInfo info = null;
		int idx = findAttrIdx(name, cls);
		if (idx >= 0) {
			AttrInfo[] infos = getClassAttrs(cls);
			if (infos != null) {
				info = infos[idx];
			}
		}
		return info;
	}

	public int findGlbAttrIdx(String name) {
		return findAttrIdx(name, AttrClass.GLOBAL);
	}

	public AttrInfo findGlbAttr(String name) {
		return findAttr(name, AttrClass.GLOBAL);
	}

	public int findPntAttrIdx(String name) {
		return findAttrIdx(name, AttrClass.POINT);
	}

	public AttrInfo findPntAttr(String name) {
		return findAttr(name, AttrClass.POINT);
	}

	public int findPolAttrIdx(String name) {
		return findAttrIdx(name, AttrClass.POLYGON);
	}

	public AttrInfo findPolAttr(String name) {
		return findAttr(name, AttrClass.POLYGON);
	}

	public AttrInfo getGlbAttrInfo(int idx) {
		AttrInfo info = null;
		if (mAttrsGlb != null && idx >= 0 && idx < mAttrsGlb.length) {
			info = mAttrsGlb[idx];
		}
		return info;
	}

	public AttrInfo getPntAttrInfo(int idx) {
		AttrInfo info = null;
		if (mAttrsPnt != null && idx >= 0 && idx < mAttrsPnt.length) {
			info = mAttrsPnt[idx];
		}
		return info;
	}

	public AttrInfo getPolAttrInfo(int idx) {
		AttrInfo info = null;
		if (mAttrsPol != null && idx >= 0 && idx < mAttrsPol.length) {
			info = mAttrsPol[idx];
		}
		return info;
	}

	public float getAttrValF(AttrInfo info, int item, int elem, float def) {
		float val = def;
		if (info != null) {
			int nitems = getAttrItemNum(info.mClass);
			if (item >= 0 && item < nitems) {
				if (elem >= 0 && elem < info.mElemNum) {
					int ptr =  info.mTop + item*info.mElemNum + elem;
					switch (info.mType) {
						case FLOAT:
							val = mAttrFloatData[ptr];
							break;
						case INT:
							val = (float)mAttrIntData[ptr];
							break;
					}
				}
			}
		}
		return val;
	}

	public float getAttrValF(AttrInfo info, int item, int elem) {
		return getAttrValF(info, item, elem, Float.NaN);
	}

	public String getAttrValS(AttrInfo info, int item, int elem, String def) {
		String str = def;
		if (info != null) {
			int nitems = getAttrItemNum(info.mClass);
			if (item >= 0 && item < nitems) {
				if (elem >= 0 && elem < info.mElemNum) {
					int ptr =  info.mTop + item*info.mElemNum + elem;
					switch (info.mType) {
						case FLOAT:
							str = String.format(Locale.US, "%f", mAttrFloatData[ptr]);
							break;
						case INT:
							str = String.format(Locale.US, "%d", mAttrIntData[ptr]);
							break;
						case STRING:
							str = getStr(mAttrIntData[ptr]);
							break;
					}
				}
			}
		}
		return str;
	}

	public String getAttrValS(AttrInfo info, int item, int elem) {
		return getAttrValS(info, item, elem, null);
	}

	public int getGrpNum(GrpClass cls) {
		int num = 0;
		switch (cls) {
			case MATERIAL:
				num = mMtlNum;
				break;
			case POINT:
				num = mPntGrpNum;
				break;
			case POLYGON:
				num = mPolGrpNum;
				break;
		}
		return num;
	}

	protected int getGrpTblOffs(GrpClass cls) {
		int offs = 0;
		switch (cls) {
			case MATERIAL:
				offs = mMtlOffs;
				break;
			case POINT:
				offs = mPntGrpOffs;
				break;
			case POLYGON:
				offs = mPolGrpOffs;
				break;
		}
		return offs;
	}

	public void calcTangentsFromUVs(float[] tngs) {
		calcTangentsFromUVs(tngs, false);
	}

	public void calcTangentsFromUVs(float[] tngs, boolean flip) {
		if (tngs == null) return;
		if (!isAllTris()) return;
		if (!hasPntNormals()) return;
		if (!hasUV()) return;
		int npol = getPolNum();
		int npnt = getPntNum();
		Calc.vfill(tngs, 0.0f);
		float[] dp1 = new float[3];
		float[] dt1 = new float[2];
		float[] dp2 = new float[3];
		float[] dt2 = new float[2];
		int[] tri = new int[3];
		float[] triPts = new float[3 * 3];
		float[] triUVs = new float[2 * 3];
		float[] tu = new float[3];
		for (int i = 0; i < npol; ++i) {
			getPolVtxLst(tri, i);
			for (int j = 0; j < 3; ++j) {
				int pntId = tri[j];
				getPnt(triPts, j*3, pntId);
				getPntTex(triUVs, j*2, pntId);
			}
			for (int j = 0; j < 3; ++j) {
				dp1[j] = triPts[3 + j] - triPts[j]; // pt1 - pt0
			}
			for (int j = 0; j < 2; ++j) {
				dt1[j] = triUVs[2 + j] - triUVs[j]; // tex1 - tex0
			}
			for (int j = 0; j < 3; ++j) {
				dp2[j] = triPts[6 + j] - triPts[j]; // pt2 - pt0
			}
			for (int j = 0; j < 2; ++j) {
				dt2[j] = triUVs[4 + j] - triUVs[j]; // tex2 - tex0
			}
			float d = Calc.rcp0(dt1[0]*dt2[1] - dt1[1]*dt2[0]);
			for (int j = 0; j < 3; ++j) {
				tu[j] = dt2[1]*dp1[j] - dt1[1]*dp2[j];
			}
			for (int j = 0; j < 3; ++j) {
				tu[j] *= d;
			}
			for (int j = 0; j < 3; ++j) {
				int pntId = tri[j];
				int torg = pntId * 3;
				for (int k = 0; k < 3; ++k) {
					tngs[torg + k] += tu[k];
				}
			}
		}
		float[] nrm = new float[3];
		for (int i = 0; i < npnt; ++i) {
			int torg = i * 3;
			getPntNormal(nrm, 0, i);
			float d = Calc.inner(nrm, 0, tngs, torg, 3);
			Calc.vscl(nrm, d);
			if (flip) {
				for (int j = 0; j < 3; ++j) {
					tngs[torg + j] = nrm[j] - tngs[torg + j];
				}
			} else {
				for (int j = 0; j < 3; ++j) {
					tngs[torg + j] = tngs[torg + j] - nrm[j];
				}
			}
			Calc.normalize(tngs, torg, 3);
		}
	}

	public GrpInfo[] getGrpClassInfos(GrpClass cls) {
		GrpInfo[] infos = null;
		switch (cls) {
			case MATERIAL:
				infos = mMtlGrps;
				break;
			case POINT:
				infos = mPntGrps;
				break;
			case POLYGON:
				infos = mPolGrps;
				break;
		}
		return infos;
	}

	public GrpInfo[] getMtlGrpInfos() {
		return getGrpClassInfos(GrpClass.MATERIAL);
	}

	public GrpInfo[] getPntGrpInfos() {
		return getGrpClassInfos(GrpClass.POINT);
	}

	public GrpInfo[] getPolGrpInfos() {
		return getGrpClassInfos(GrpClass.POLYGON);
	}

	public int getGrpInfoNum(GrpClass cls) {
		int n = 0;
		GrpInfo[] infos = getGrpClassInfos(cls);
		if (infos != null) {
			n = infos.length;
		}
		return n;
	}

	public int getMtlGrpNum() {
		return getGrpInfoNum(GrpClass.MATERIAL);
	}

	public int getPntGrpNum() {
		return getGrpInfoNum(GrpClass.POINT);
	}

	public int getPolGrpNum() {
		return getGrpInfoNum(GrpClass.POLYGON);
	}

	public GrpInfo getGrpInfo(GrpClass cls, int idx) {
		GrpInfo info = null;
		GrpInfo[] infos = null;
		if (idx >= 0) {
			infos = getGrpClassInfos(cls);
			if (infos != null && idx < infos.length) {
				info = infos[idx];
			}
		}
		return info;
	}

	public GrpInfo getMtlGrpInfo(int idx) {
		return getGrpInfo(GrpClass.MATERIAL, idx);
	}

	public GrpInfo getPntGrpInfo(int idx) {
		return getGrpInfo(GrpClass.POINT, idx);
	}

	public GrpInfo getPolGrpInfo(int idx) {
		return getGrpInfo(GrpClass.POLYGON, idx);
	}

	public boolean ckSkinNodeId(int id) {
		return mSkinNodeNameIds != null && id >= 0 && id < mSkinNodeNameIds.length;
	}

	public String getSkinNodeName(int id) {
		String name = null;
		if (ckSkinNodeId(id)) {
			name = getStr(mSkinNodeNameIds[id]);
		}
		return name;
	}

	public boolean getSkinNodeSphere(int id, float[] dst, int dstOffs) {
		boolean res = dst != null && mSkinNodeSpheres != null && ckSkinNodeId(id);
		if (res) {
			int srcOffs = id * 4;
			for (int i = 0; i < 4; ++i) {
				dst[dstOffs + i] = mSkinNodeSpheres[srcOffs + i];
			}
		}
		return res;
	}

	public class HitQuery {
		protected float[] mSegMinMax = new float[6];
		protected Intersect.SegPolyWk mSegPolyWk = new Intersect.SegPolyWk();
		protected int[] mPolVtxLst = new int[4];
		protected Vec mPolV0 = new Vec();
		protected Vec mPolV1 = new Vec();
		protected Vec mPolV2 = new Vec();
		protected Vec mPolV3 = new Vec();
		protected float mHitDistSq = -1.0f;
		protected Vec mHitPos = new Vec();
		protected Vec mHitNrm = new Vec();
		protected int mHitIdx = -1;
		protected boolean mHitFlg = false;

		protected void updateNearest(int polIdx) {
			float dsq = mSegPolyWk.getHitDistSq();
			if (mHitDistSq < 0.0f || dsq < mHitDistSq) {
				mHitDistSq = dsq;
				mHitIdx = polIdx;
				mSegPolyWk.getHitPos(mHitPos);
				mSegPolyWk.getHitNrm(mHitNrm);
			}
		}

		protected void ckTri(int polIdx) {
			getPolVtxLst(mPolVtxLst, polIdx);
			getPnt(mPolV0, mPolVtxLst[0]);
			getPnt(mPolV1, mPolVtxLst[1]);
			getPnt(mPolV2, mPolVtxLst[2]);
			mSegPolyWk.setTri(mPolV0, mPolV2, mPolV1); // CW
			boolean hitFlg = mSegPolyWk.calcIntersection(true, false);
			if (hitFlg) {
				updateNearest(polIdx);
				mHitFlg = true;
			}
		}

		protected void ckQuad(int polIdx) {
			getPolVtxLst(mPolVtxLst, polIdx);
			getPnt(mPolV0, mPolVtxLst[0]);
			getPnt(mPolV1, mPolVtxLst[1]);
			getPnt(mPolV2, mPolVtxLst[2]);
			getPnt(mPolV3, mPolVtxLst[3]);
			mSegPolyWk.setQuad(mPolV3, mPolV2, mPolV1, mPolV0); // CW
			boolean hitFlg = mSegPolyWk.calcIntersection(false, false);
			if (hitFlg) {
				updateNearest(polIdx);
				mHitFlg = true;
			}
		}

		protected void execNoBVH() {
			int npol = getPolNum();
			if (isAllTris()) {
				for (int i = 0; i < npol; ++i) {
					ckTri(i);
				}
			} else {
				boolean qflg = allQuadsPlanarConvex();
				for (int i = 0; i < npol; ++i) {
					int nvtx = getPolVtxNum(i);
					if (nvtx == 3) {
						ckTri(i);
					} else if (qflg && nvtx == 4) {
						ckQuad(i);
					}
				}
			}
		}

		protected void execBVH(int nodeId) {
			int boxOffs = nodeId * 6;
			if (Overlap.boxes(mSegMinMax, 0, mBVHBoxes, boxOffs)) {
				float p0x = mSegPolyWk.getSegP0X();
				float p0y = mSegPolyWk.getSegP0Y();
				float p0z = mSegPolyWk.getSegP0Z();
				float p1x = mSegPolyWk.getSegP1X();
				float p1y = mSegPolyWk.getSegP1Y();
				float p1z = mSegPolyWk.getSegP1Z();
				boolean segFlg = Overlap.segmentBox(p0x, p0y, p0z, p1x, p1y, p1z, mBVHBoxes, boxOffs);
				if (segFlg) {
					int treeOffs = nodeId * 2;
					int left = mBVHTree[treeOffs];
					int right = mBVHTree[treeOffs + 1];
					boolean isLeaf = right < 0;
					if (isLeaf) {
						int polIdx = left;
						if (isAllTris()) {
							ckTri(polIdx);
						} else {
							boolean qflg = allQuadsPlanarConvex();
							int nvtx = getPolVtxNum(polIdx);
							if (nvtx == 3) {
								ckTri(polIdx);
							} else if (qflg && nvtx == 4) {
								ckQuad(polIdx);
							}
						}
					} else {
						execBVH(left);
						execBVH(right);
					}
				}
			}
		}

		public boolean exec(float p0x, float p0y, float p0z, float p1x, float p1y, float p1z, boolean useBVH) {
			mSegPolyWk.setSeg(p0x, p0y, p0z, p1x, p1y, p1z);
			mHitDistSq = -1.0f;
			mHitIdx = -1;
			mHitFlg = false;
			if (useBVH && hasBVH()) {
				mSegMinMax[0] = Math.min(p0x, p1x);
				mSegMinMax[1] = Math.min(p0y, p1y);
				mSegMinMax[2] = Math.min(p0z, p1z);
				mSegMinMax[3] = Math.max(p0x, p1x);
				mSegMinMax[4] = Math.max(p0y, p1y);
				mSegMinMax[5] = Math.max(p0z, p1z);
				execBVH(0);
			} else {
				execNoBVH();
			}
			return mHitFlg;
		}

		public boolean exec(float p0x, float p0y, float p0z, float p1x, float p1y, float p1z) {
			return exec(p0x, p0y, p0z, p1x, p1y, p1z, true);
		}

		public boolean exec(Vec p0, Vec p1) {
			return exec(p0.x(), p0.y(), p0.z(), p1.x(), p1.y(), p1.z());
		}

		public boolean execWithoutBVH(float p0x, float p0y, float p0z, float p1x, float p1y, float p1z) {
			return exec(p0x, p0y, p0z, p1x, p1y, p1z, false);
		}

		public float getHitDistSq() {
			return mHitDistSq;
		}

		public float getHitDist() {
			return mHitDistSq >= 0.0f ? Calc.sqrtf(mHitDistSq) : -1.0f;
		}

		public void getHitPos(Vec pos) {
			pos.set(mHitPos);
		}

		public float getHitPosX() {
			return mHitPos.x();
		}

		public float getHitPosY() {
			return mHitPos.y();
		}

		public float getHitPosZ() {
			return mHitPos.z();
		}

		public void getHitNrm(Vec nrm) {
			nrm.set(mHitNrm);
		}

		public float getHitNrmX() {
			return mHitNrm.x();
		}

		public float getHitNrmY() {
			return mHitNrm.y();
		}

		public float getHitNrmZ() {
			return mHitNrm.z();
		}
	}

	public class RangeQuery {
		protected float[] mRange = new float[6];
		protected float[] mPolMinMax = new float[6];
		protected Vec mTmpPnt = new Vec();
		protected int[] mPolVtxLst;
		protected int[] mPolLst;
		protected int mPolPtr;

		protected void ckPol(int polIdx) {
			int nvtx = getPolVtxLst(mPolVtxLst, polIdx);
			getPnt(mTmpPnt, mPolVtxLst[0]);
			for (int i = 0; i < 3; ++i) {
				float el = mTmpPnt.el[i];
				mPolMinMax[i] = el;
				mPolMinMax[3 + i] = el;
			}
			for (int ivtx = 1; ivtx < nvtx; ++ivtx) {
				getPnt(mTmpPnt, mPolVtxLst[ivtx]);
				for (int i = 0; i < 3; ++i) {
					float el = mTmpPnt.el[i];
					mPolMinMax[i] = Math.min(mPolMinMax[i], el);
					mPolMinMax[3 + i] = Math.max(mPolMinMax[3 + i], el);
				}
			}
			boolean inRange = Overlap.boxes(mRange, mPolMinMax);
			if (inRange) {
				if (mPolPtr < mPolLst.length) {
					mPolLst[mPolPtr++] = polIdx;
				}
			}
		}

		protected void execBVH(int nodeId) {
			int boxOffs = nodeId * 6;
			if (Overlap.boxes(mRange, 0, mBVHBoxes, boxOffs)) {
				int treeOffs = nodeId * 2;
				int left = mBVHTree[treeOffs];
				int right = mBVHTree[treeOffs + 1];
				boolean isLeaf = right < 0;
				if (isLeaf) {
					int polIdx = left;
					ckPol(polIdx);
				} else {
					execBVH(left);
					execBVH(right);
				}
			}
		}

		public int exec(float xmin, float ymin, float zmin, float xmax, float ymax, float zmax) {
			if (mPolVtxLst == null) {
				mPolVtxLst = new int[mMaxVtxPerPol];
			}
			int npol = getPolNum();
			if (mPolLst == null) {
				mPolLst = new int[npol];
			}
			mRange[0] = xmin;
			mRange[1] = ymin;
			mRange[2] = zmin;
			mRange[3] = xmax;
			mRange[4] = ymax;
			mRange[5] = zmax;
			mPolPtr = 0;
			if (hasBVH()) {
				execBVH(0);
			} else {
				for (int i = 0; i < npol; ++i) {
					ckPol(i);
				}
			}
			return mPolPtr;
		}

		public void allocPolList(int n) {
			int npol = getPolNum();
			if (n <= 0 || n > npol) {
				n = npol;
			}
			mPolLst = new int[n];
		}

		public void allocPolList() {
			allocPolList(0);
		}

		public int[] getPolList() {
			return mPolLst;
		}

		public int getPolListSize() {
			return mPolPtr;
		}

		public XGeo getGeo() {
			return XGeo.this;
		}
	}

	public HitQuery allocHitQuery() {
		return new HitQuery();
	}

	public RangeQuery allocRangeQuery() {
		return new RangeQuery();
	}

	public String getHouGeoStr() {
		StringBuilder sb = new StringBuilder();
		dumpHouGeo(sb);
		return sb.toString();
	}

	public void dumpHouGeo(StringBuilder sb) {
		String eol = "\n";
		sb.append("PGEOMETRY V5" + eol);
		int npnt = getPntNum();
		int npol = getPolNum();
		sb.append("NPoints " + npnt + " NPrims " + npol + eol);
		int ngrpPnt = 0;
		int ngrpPol = 0;
		sb.append("NPointGroups " + ngrpPnt + " NPrimGroups " + ngrpPol + eol);
		int npntAttr = getPntAttrNum();
		int npolAttr = 0;
		sb.append("NPointAttrib " + npntAttr + " NVertexAttrib 0 NPrimAttrib " + npolAttr + " NAttrib 0" + eol);
		if (npntAttr > 0) {
			sb.append("PointAttrib" + eol);
			for (int i = 0; i < npntAttr; ++i) {
				AttrInfo info = getPntAttrInfo(i);
				if (info != null) {
					String name = info.mName;
					int nelem = info.mElemNum;
					sb.append(name);
					sb.append(" " + nelem + " ");
					if (name.equals("N") || name.equals("tangentu")) {
						sb.append("vector");
					} else {
						if (info.mType == AttrType.FLOAT) {
							sb.append("float");
						} else {
							sb.append("int");
						}
					}
					for (int j = 0; j < nelem; ++j) {
						sb.append(" 0");
					}
					sb.append(eol);
				}
			}
		}
		Vec pnt = new Vec();
		for (int i = 0; i < npnt; ++i) {
			getPnt(pnt, i);
			sb.append("" + pnt.x() + " " + pnt.y() + " " + pnt.z() + " 1");
			if (npntAttr > 0) {
				sb.append(" (");
				for (int j = 0; j < npntAttr; ++j) {
					if (j != 0) sb.append(" ");
					AttrInfo info = getPntAttrInfo(j);
					if (info != null) {
						int nelem = info.mElemNum;
						for (int k = 0; k < nelem; ++k) {
							sb.append(" " + getAttrValF(info, i, k));
						}
					}
				}
				sb.append(")");
			}
			sb.append(eol);
		}
		sb.append("Run " + npol + " Poly" + eol);
		int[] vlst = new int[mMaxVtxPerPol];
		for (int i = 0; i < npol; ++i) {
			int nvtx = getPolVtxLst(vlst, i);
			sb.append(" " + nvtx + " <");
			for (int j = 0; j < nvtx; ++j) {
				sb.append(" " + vlst[j]);
			}
			sb.append(eol);
		}
		sb.append("beginExtra" + eol);
		sb.append("endExtra" + eol);
	}

	public void dumpHouSkin(StringBuilder sb, String basePath) {
		if (!hasSkin()) return;
		boolean sepFlg = !basePath.endsWith("/");
		sb.append("1\n\n");
		String[] nodeNames = getSkinNodeNames();
		int npnt = mPntNum;
		float[] wgt = new float[mMaxSkinWgtNum];
		int[] jnt = new int[mMaxSkinWgtNum];
		for (int i = 0; i < npnt; ++i) {
			int nwgt = getPntSkin(i, jnt, 0, wgt, 0);
			for (int j = 0; j < nwgt; ++j) {
				sb.append(i);
				sb.append(" ");
				sb.append(basePath);
				if (sepFlg) sb.append("/");
				sb.append(nodeNames[jnt[j]]);
				sb.append("/cregion 0 ");
				sb.append(wgt[j]);
				sb.append("\n");
			}
		}
	}

	public void dumpHouSkin(StringBuilder sb) {
		dumpHouSkin(sb, "/obj/");
	}

	public String getHouSkinStr(String basePath) {
		StringBuilder sb = new StringBuilder();
		dumpHouSkin(sb, basePath);
		return sb.toString();
	}

	public String getHouSkinStr() {
		StringBuilder sb = new StringBuilder();
		dumpHouSkin(sb);
		return sb.toString();
	}

}
