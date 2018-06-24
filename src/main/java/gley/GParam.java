// GLSL ES codegen
// Author: Sergey Chaban <sergey.chaban@gmail.com>

package gley;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.reflect.Field;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Vector;

public class GParam {

	public static final int MAX_SKIN_NODES = 32*2;
	public static final int MAX_SH_ORDER = 7;

	public static final int INT_BIT = 6;

	public static final byte T_NONE = 0;
	public static final byte T_INT = (byte)((1 << INT_BIT) | 1);
	public static final byte T_FLOAT = 1;
	public static final byte T_VEC2 = 2;
	public static final byte T_VEC3 = 3;
	public static final byte T_VEC4 = 4;
	public static final byte T_MAT4 = 4*4;
	public static final byte T_MAT3x4 = 3*4;

	@Retention(RetentionPolicy.RUNTIME)
	public @interface ArySize {
		public int value() default 0;
	}

	public class vec2 {}
	public class vec3 {}
	public class vec4 {}
	public class mat4 {}
	public class mat3x4 {}


	public class GP_CAMERA {
		mat4 view;
		mat4 proj;
		mat4 viewProj;
		mat4 invView;
		mat4 invProj;
		mat4 invViewProj;
		vec3 viewPos;
		int param;
	}

	public class GP_WORLD {
		mat3x4 mtx;
	}

	public class GP_VERTEX {
		vec4 texShift;
		vec4 attrScl;
		vec3 posBase;
		vec3 posScl;
	}

	public class GP_SKIN {
		@ArySize(MAX_SKIN_NODES) mat3x4 mtx;
	}

	public class GP_AMBIENT {
		vec3 color;
		vec3 hemiSky;
		vec3 hemiGround;
		vec3 hemiUp;
	}

	public class GP_ENVSH {
		@ArySize(3*3) vec3 diff;
		@ArySize(3*3) vec3 refl;
		vec3 diffClr;
		vec3 reflClr;
	}

	public class GP_FOG {
		vec4 color; // RGB, density
		vec4 params; // start, invRange, crvP1, crvP2
	}

	public class GP_LIGHT {
		vec3 dir;
		vec3 pos;
		vec3 distClr;
		vec3 omniClr;
		vec2 omniAttn; // start, falloff
		vec4 clrFactors; // distDiff, distSpec, omniDiff, omniSpec
	}

	public class GP_MATERIAL {
		vec4 vclrGain;
		vec4 vclrBias;
		vec4 viewFresnel; // spec{Gain, Bias}, refl{Gain, Bias}
		vec3 baseColor;
		vec3 specColor;
		vec3 diffRoughness;
		vec3 specRoughness;
		vec3 diffSHAdj;
		vec3 IOR;
		vec3 reflColor;
		vec2 flipTngFrame;
		float bumpFactor;
		float reflLvl;
	}

	public class GP_COLOR {
		vec4 invGamma;
		vec3 exposure;
		vec3 linWhite;
		vec3 linGain;
		vec3 linBias;
		vec3 inBlack;
		vec3 rangeRatio; // outRange / inRange
		vec3 outBlack;
	}

	private static byte getFldType(Field fld) {
		byte ft = T_NONE;
		Type typ = fld.getType();
		String tname = typ.toString();
		if (tname.endsWith("$vec2")) {
			ft = T_VEC2;
		} else if (tname.endsWith("$vec3")) {
			ft = T_VEC3;
		} else if (tname.endsWith("$vec4")) {
			ft = T_VEC4;
		} else if (tname.endsWith("$mat4")) {
			ft = T_MAT4;
		} else if (tname.endsWith("$mat3x4")) {
			ft = T_MAT3x4;
		} else if (tname.equals("int")) {
			ft = T_INT;
		} else if (tname.equals("float")) {
			ft = T_FLOAT;
		}
		return ft;
	}

	private static boolean isAry(Field fld) {
		return fld.getAnnotation(ArySize.class) != null;
	}

	private static int getArySize(Field fld) {
		int n = 1;
		if (isAry(fld)) {
			n = ((ArySize)fld.getAnnotation(ArySize.class)).value();
		}
		return n;
	}

	public static int getTypeSizeSel(int ft) {
		int size = 0;
		switch (ft) {
			case T_FLOAT:
				size = 1;
				break;
			case T_VEC2:
				size = 2;
				break;
			case T_VEC3:
				size = 3;
				break;
			case T_VEC4:
				size = 4;
				break;
			case T_MAT4:
				size = 4*4;
				break;
			case T_MAT3x4:
				size = 3*4;
				break;
			case T_INT:
				size = 1;
				break;
		}
		return size;
	}

	public static int getTypeSize(int ft) {
		return ft & ((1 << INT_BIT) - 1);
	}

	public class BlockInfo {
		public String mName;
		public int mFltNum;
		public int mIntNum;
		public int mAryNum;
		public String[] mFldNames;
		public byte[] mFldTypes;
		public int[] mArySizes;
		public int[] mOffs;
		public int mFltTop = -1;
		public int mIntTop = -1;

		BlockInfo(Class cls) {
			mName = cls.getName().substring(cls.getName().lastIndexOf("$GP_") + 4);
			Field[] flst = cls.getDeclaredFields();
			mFltNum = 0;
			mIntNum = 0;
			mAryNum = 0;
			int nfld = 0;
			for (Field fld : flst) {
				byte ft = getFldType(fld);
				if (ft != T_NONE) {
					if (isAry(fld)) {
						++mAryNum;
					}
					switch (ft) {
						case T_FLOAT:
						case T_VEC2:
						case T_VEC3:
						case T_VEC4:
						case T_MAT4:
						case T_MAT3x4:
							mFltNum += getTypeSize(ft) * getArySize(fld);
							break;
						case T_INT:
							mIntNum += getTypeSize(ft) * getArySize(fld);
							break;
					}
					++nfld;
				}
			}
			if (nfld > 0) {
				mFldNames = new String[nfld];
				mFldTypes = new byte[nfld];
				mArySizes = new int[nfld];
				int ifld = 0;
				for (Field fld : flst) {
					byte ft = getFldType(fld);
					if (ft != T_NONE) {
						mFldNames[ifld] = fld.getName();
						mFldTypes[ifld] = ft;
						if (isAry(fld)) {
							mArySizes[ifld] = getArySize(fld);
						} else {
							mArySizes[ifld] = 0;
						}
						++ifld;
					}
				}
				mOffs = new int[nfld];
				int fptr = 0;
				int iptr = 0;
				for (int i = 0; i < nfld; ++i) {
					byte ft = mFldTypes[i];
					int add = getTypeSize(ft);
					if (mArySizes[i] > 0) {
						add *= mArySizes[i];
					}
					if (isFloatType(ft)) {
						mOffs[i] = fptr;
						fptr += add;
					} else {
						mOffs[i] = iptr;
						iptr += add;
					}
				}
			}
		}

		public int getFldNum() {
			return mFldNames != null ? mFldNames.length : 0;
		}

		public boolean ckFldId(int id) {
			int nfld = getFldNum();
			return id >= 0 && id < nfld;
		}

		public int findFld(String name) {
			int idx = -1;
			int n = getFldNum();
			for (int i = 0; i < n; ++i) {
				if (mFldNames[i].equals(name)) {
					idx = i;
					break;
				}
			}
			return idx;
		}

		public boolean isIntFld(int id) {
			return isIntType(mFldTypes[id]);
		}
	}

	public int mTgtVersion = 200;
	protected BlockInfo[] mBlkInfos;
	protected HashMap<String, Integer> mMapNameToId;
	protected HashMap<Integer, String> mMapIdToName;
	protected float[] mFlts;
	protected int[] mInts;
	protected int mTotalFlds;
	protected int mMaxFldPerBlk;

	public static int makeGPID(int blk, int fld) {
		return (blk << 16) | fld;
	}

	public static int blkIdFromGPID(int gpid) {
		return (gpid >> 16) & 0xFFFF;
	}

	public static int fldIdFromGPID(int gpid) {
		return gpid & 0xFFFF;
	}

	public static boolean isFloatTypeSel(int ft) {
		boolean res = false;
		switch (ft) {
			case T_FLOAT:
			case T_VEC2:
			case T_VEC3:
			case T_VEC4:
			case T_MAT4:
			case T_MAT3x4:
				res = true;
				break;
		}
		return res;
	}

	public static boolean isFloatType(int ft) {
		return ft != T_NONE && (ft & (1 << INT_BIT)) == 0;
	}

	public static boolean isIntType(int ft) {
		return ft != T_NONE && (ft & (1 << INT_BIT)) != 0;
	}

	public void emitVersion(StringBuilder sb) {
		if (mTgtVersion >= 300) {
			sb.append("#version ");
			sb.append(mTgtVersion);
			sb.append(" es\n");
		}
	}

	public void emitInDecl(StringBuilder sb, String decl) {
		if (mTgtVersion >= 300) {
			sb.append("in ");
		} else {
			sb.append("varying ");
		}
		sb.append(decl);
		sb.append(";\n");
	}

	public void emitAttrDecl(StringBuilder sb, String decl, boolean partial) {
		if (mTgtVersion >= 300) {
			sb.append("in ");
		} else {
			sb.append("attribute ");
		}
		sb.append(decl);
		if (!partial) {
			sb.append(";\n");
		}
	}

	public void emitAttrDecl(StringBuilder sb, String decl) {
		emitAttrDecl(sb, decl, false);
	}

	public void emitOutDecl(StringBuilder sb, String decl) {
		if (mTgtVersion >= 300) {
			sb.append("out ");
		} else {
			sb.append("varying ");
		}
		sb.append(decl);
		sb.append(";\n");
	}

	public void init() {
		Vector<BlockInfo> infos = new Vector<BlockInfo>();
		Class[] clsLst = GParam.class.getDeclaredClasses();
		for (Class cls : clsLst) {
			String cname = cls.getCanonicalName();
			int idx = cname.lastIndexOf('.');
			if (idx > 0) {
				cname = cname.substring(idx + 1);
				if (cname.startsWith("GP_")) {
					BlockInfo info = new BlockInfo(cls);
					infos.add(info);
				}
			}
		}
		mBlkInfos = new BlockInfo[infos.size()];
		infos.toArray(mBlkInfos);
		mMapNameToId = new HashMap<String, Integer>();
		mMapIdToName = new HashMap<Integer, String>();
		mTotalFlds = 0;
		int nblk = getBlkNum();
		for (int i = 0; i < nblk; ++i) {
			int nfld = mBlkInfos[i].getFldNum();
			mTotalFlds += nfld;
			for (int j = 0; j < nfld; ++j) {
				int gpid = makeGPID(i, j);
				String gpname = mBlkInfos[i].mName + "." + mBlkInfos[i].mFldNames[j];
				mMapNameToId.put(gpname, gpid);
				mMapIdToName.put(gpid, gpname);
			}
		}
		int nflt = 0;
		int nint = 0;
		for (int i = 0; i < nblk; ++i) {
			nflt += mBlkInfos[i].mFltNum;
			nint += mBlkInfos[i].mIntNum;
		}
		if (nflt > 0) {
			mFlts = new float[nflt];
			int fptr = 0;
			for (int i = 0; i < nblk; ++i) {
				if (mBlkInfos[i].mFltNum > 0) {
					mBlkInfos[i].mFltTop = fptr;
					fptr += mBlkInfos[i].mFltNum;
				}
			}
		}
		if (nint > 0) {
			mInts = new int[nint];
			int iptr = 0;
			for (int i = 0; i < nblk; ++i) {
				if (mBlkInfos[i].mIntNum > 0) {
					mBlkInfos[i].mIntTop = iptr;
					iptr += mBlkInfos[i].mIntNum;
				}
			}
		}
		mMaxFldPerBlk = 0;
		for (int i = 0; i < nblk; ++i) {
			mMaxFldPerBlk = Math.max(mMaxFldPerBlk, mBlkInfos[i].getFldNum());
		}
	}

	public int getBlkNum() {
		return mBlkInfos != null ? mBlkInfos.length : 0;
	}

	public boolean ckBlkId(int id) {
		int nblk = getBlkNum();
		return id >= 0 && id < nblk;
	}

	public boolean ckIds(int blkId, int fldId) {
		if (!ckBlkId(blkId)) return false;
		return mBlkInfos[blkId].ckFldId(fldId);
	}

	public boolean ckGPID(int gpid) {
		if (gpid < 0) return false;
		return ckIds(blkIdFromGPID(gpid), fldIdFromGPID(gpid));
	}

	public boolean ckGPIDs(int... ids) {
		for (int id : ids) {
			if (!ckGPID(id)) return false;
		}
		return true;
	}

	public int findBlk(String name) {
		int idx = -1;
		int n = getBlkNum();
		for (int i = 0; i < n; ++i) {
			if (mBlkInfos[i].mName.equals(name)) {
				idx = i;
				break;
			}
		}
		return idx;
	}

	public BlockInfo getBlk(int idx) {
		BlockInfo blk = null;
		if (ckBlkId(idx)) {
			blk = mBlkInfos[idx];
		}
		return blk;
	}

	public BlockInfo getBlk(String name) {
		return getBlk(findBlk(name));
	}

	public void emitName(StringBuilder sb, int blkId, int fldId) {
		if (ckIds(blkId, fldId)) {
			emitNameNoCk(sb, mBlkInfos[blkId], fldId);
		}
	}

	public void emitName(StringBuilder sb, int gpid) {
		if (gpid >= 0) {
			emitName(sb, blkIdFromGPID(gpid), fldIdFromGPID(gpid));
		}
	}

	public String getNameFromGPID(int gpid) {
		StringBuilder sb = new StringBuilder();
		emitName(sb, gpid);
		return sb.toString();
	}

	protected void emitNameNoCk(StringBuilder sb, BlockInfo blk, int fldId) {
		sb.append("GP_");
		sb.append(blk.mName);
		sb.append("_");
		sb.append(blk.mFldNames[fldId]);
	}

	public static void emitFldType(StringBuilder sb, int ft) {
		switch (ft) {
			case T_FLOAT: sb.append("float"); break;
			case T_VEC2: sb.append("vec2"); break;
			case T_VEC3: sb.append("vec3"); break;
			case T_VEC4: sb.append("vec4"); break;
			case T_MAT4: sb.append("mat4"); break;
			case T_MAT3x4: sb.append("mat3x4"); break;
			case T_INT: sb.append("int"); break;
		}
	}

	public static void emitFldDeclType(StringBuilder sb, int ft) {
		switch (ft) {
			case T_FLOAT: sb.append("float"); break;
			case T_VEC2: sb.append("vec2"); break;
			case T_VEC3: sb.append("vec3"); break;
			case T_VEC4: sb.append("vec4"); break;
			case T_MAT4: sb.append("mat4"); break;
			case T_MAT3x4: sb.append("vec4"); break;
			case T_INT: sb.append("int"); break;
		}
	}

	public void emitDecl(StringBuilder sb, int blkId, int fldId, StringBuilder err) {
		if (!ckBlkId(blkId)) {
			if (err != null) {
				err.append("emitDecl: invalid block id: 0x");
				err.append(Integer.toHexString(blkId));
				err.append("\n");
			}
			return;
		}
		BlockInfo blk = mBlkInfos[blkId];
		if (!blk.ckFldId(fldId)) {
			if (err != null) {
				err.append("emitDecl: invalid field id for block ");
				err.append(blk.mName);
				err.append(": 0x");
				err.append(Integer.toHexString(fldId));
				err.append("\n");
			}
			return;
		}
		byte ft = blk.mFldTypes[fldId];
		sb.append("uniform ");
		emitFldDeclType(sb, ft);
		sb.append(" ");
		emitNameNoCk(sb, blk, fldId);
		if (ft == T_MAT3x4) {
			int asize = blk.mArySizes[fldId];
			if (asize > 0) {
				sb.append("[3*");
				sb.append(asize);
				sb.append("]");
			} else {
				sb.append("[3]");
			}
		} else {
			int asize = blk.mArySizes[fldId];
			if (asize > 0) {
				sb.append("[");
				sb.append(asize);
				sb.append("]");
			}
		}
		sb.append(";\n");
	}

	public void emitDecl(StringBuilder sb, int blkId, int fldId) {
		emitDecl(sb, blkId, fldId, null);
	}

	public void emitDecl(StringBuilder sb, int gpid, StringBuilder err) {
		if (gpid >= 0) {
			emitDecl(sb, blkIdFromGPID(gpid), fldIdFromGPID(gpid), err);
		}
	}

	public void emitDecl(StringBuilder sb, int gpid) {
		emitDecl(sb, gpid, null);
	}

	public void emitDecl(StringBuilder sb, int[] usage, StringBuilder err) {
		int nblk = getBlkNum();
		for (int i = 0; i < nblk; ++i) {
			int nfld = mBlkInfos[i].getFldNum();
			for (int j = 0; j < nfld; ++j) {
				if (usageCk(usage, i, j)) {
					emitDecl(sb, i, j, err);
				}
			}
		}
	}

	public void emitAllDecls(StringBuilder sb) {
		int nblk = getBlkNum();
		for (int i = 0; i < nblk; ++i) {
			int nfld = mBlkInfos[i].getFldNum();
			for (int j = 0; j < nfld; ++j) {
				int gpid = makeGPID(i, j);
				sb.append("/* ");
				sb.append(Integer.toHexString(gpid).toUpperCase());
				sb.append(" @ ");
				sb.append(Integer.toHexString(mBlkInfos[i].mOffs[j]).toUpperCase());
				sb.append(" */ ");
				emitDecl(sb, i, j);
			}
		}
	}

	public int getGPID(String name) {
		int gpid = -1;
		if (mMapNameToId != null && mMapNameToId.containsKey(name)) {
			gpid = mMapNameToId.get(name);
		}
		return gpid;
	}

	public int calcUsageTblSize() {
		int n = mMaxFldPerBlk * getBlkNum();
		if (n > 0) {
			n = ((n - 1) / 32) + 1;
		}
		return n;
	}

	public int[] allocUsageTbl() {
		int[] tbl = null;
		int n = calcUsageTblSize();
		if (n > 0) {
			tbl = new int[n];
		}
		return tbl;
	}

	public void clearUsageTbl(int[] tbl) {
		Arrays.fill(tbl, 0);
	}

	public boolean usageCk(int[] tbl, int blkId, int fldId) {
		int nblk = getBlkNum();
		if (nblk <= 0) return false;
		if (!ckBlkId(blkId)) return false;
		int idx = blkId*mMaxFldPerBlk + fldId;
		return (tbl[idx/32] & (1 << (idx & 31))) != 0;
	}

	public boolean usageCk(int[] tbl, int gpid) {
		if (gpid < 0) return false;
		return usageCk(tbl, blkIdFromGPID(gpid), fldIdFromGPID(gpid));
	}

	public void usageSt(int[] tbl, int blkId, int fldId) {
		int nblk = getBlkNum();
		if (nblk <= 0) return;
		if (!ckBlkId(blkId)) return;
		int idx = blkId*mMaxFldPerBlk + fldId;
		tbl[idx/32] |= 1 << (idx & 31);
	}

	public void usageSt(int[] tbl, int gpid) {
		if (gpid < 0) return;
		usageSt(tbl, blkIdFromGPID(gpid), fldIdFromGPID(gpid));
	}

	public int usageFldCount(int[] tbl) {
		int n = 0;
		for (int mask : tbl) {
			n += Integer.bitCount(mask);
		}
		return n;
	}

	public int[] makeFieldXferList(int[] usage) {
		int nblk = getBlkNum();
		if (nblk <= 0) return null;
		int n = usageFldCount(usage);
		if (n == 0) return null;
		int esize = FieldXfer.ENTRY_SIZE;
		int[] tbl = new int[n*esize]; // {gpid, info, offs, loc}[n]
		int idx = 0;
		for (int i = 0; i < nblk; ++i) {
			int nfld = mBlkInfos[i].getFldNum();
			for (int j = 0; j < nfld; ++j) {
				if (usageCk(usage, i, j)) {
					BlockInfo blk = mBlkInfos[i];
					tbl[idx*esize] = makeGPID(i, j);
					int ftyp = blk.mFldTypes[j] & 0xFF;
					int asiz = blk.mArySizes[j] & 0xFFFFFF;
					tbl[idx*esize + 1] = ftyp | (asiz << 8);
					if (blk.isIntFld(j)) {
						tbl[idx*esize + 2] = blk.mIntTop + blk.mOffs[j];
					} else {
						tbl[idx*esize + 2] = blk.mFltTop + blk.mOffs[j];
					}
					tbl[idx*esize + 3] = -1;
					++idx;
				}
			}
		}
		return tbl;
	}

	public float[] getFloats() {
		return mFlts;
	}

	public int[] getInts() {
		return mInts;
	}

	public void setF(int gpid, int dstOffs, float[] src, int srcOffs, int num) {
		if (gpid < 0) {
			return;
		}
		int blkId = blkIdFromGPID(gpid);
		if (!ckBlkId(blkId)) return;
		int fldId = fldIdFromGPID(gpid);
		BlockInfo blk = mBlkInfos[blkId];
		if (!blk.ckFldId(fldId)) return;
		if (blk.isIntFld(fldId)) return;
		int idst = blk.mFltTop + blk.mOffs[fldId];
		/*
		for (int i = 0; i < num; ++i) {
			mFlts[idst + dstOffs + i] = src[srcOffs + i];
		}*/
		System.arraycopy(src, srcOffs, mFlts, idst + dstOffs, num);
	}

	public void setF(int gpid, int dstOffs, float... src) {
		setF(gpid, dstOffs, src, 0, src.length);
	}

	public void setF(int gpid, float... src) {
		setF(gpid, 0, src, 0, src.length);
	}

	public void setF(String name, int dstOffs, float[] src, int srcOffs, int num) {
		setF(getGPID(name), dstOffs, src, srcOffs, num);
	}

	public void setF(String name, int dstOffs, float... src) {
		setF(name, dstOffs, src, 0, src.length);
	}

	public void setF(String name, float... src) {
		setF(name, 0, src, 0, src.length);
	}

}
