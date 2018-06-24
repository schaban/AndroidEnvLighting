// Author: Sergey Chaban <sergey.chaban@gmail.com>

package xdata;


public class XData {

	public final static String KIND_GEO = "XGEO";
	public final static String KIND_TEX = "XTEX";
	public final static String KIND_VAL = "XVAL";
	public final static String KIND_RIG = "XRIG";
	public final static String KIND_KFR = "XKFR";
	public final static String KIND_CEL = "XCEL";
	public final static String KIND_CAT = "FCAT";

	public class Head {
		public String mKind;
		public int mFlags;
		public int mFileSize;
		public int mHeadSize;
		public int mOffsStr;
		public int mNameId;
		public int mPathId;

		public void init(Binary bin) {
			mKind = bin.getKind();
			mFlags = bin.getFlags();
			mFileSize = bin.getFileSize();
			mHeadSize = bin.getHeadSize();
			mOffsStr = bin.getOffsStr();
			mNameId = bin.getNameId();
			mPathId = bin.getPathId();
		}

		public boolean ckFlgMask(int mask) { return (mFlags & mask) != 0; }

		public boolean isGeometry() { return mKind.equals(KIND_GEO); }
		public boolean isTexture() { return mKind.equals(KIND_TEX); }
		public boolean isValues() { return mKind.equals(KIND_VAL); }
		public boolean isRig() { return mKind.equals(KIND_RIG); }
		public boolean isKeyframes() { return mKind.equals(KIND_KFR); }
		public boolean isCompiledExprLib() { return mKind.equals(KIND_CEL); }
		public boolean isFileCat() { return mKind.equals(KIND_CAT); }
	}

	public String mName;
	public Head mHead;
	public StrList mStrs;

	public void init(Binary bin) {
		mHead = new Head();
		mHead.init(bin);
		if (mHead.mOffsStr > 0) {
			mStrs = new StrList();
			mStrs.read(bin, mHead.mOffsStr);
			mName = getStr(mHead.mNameId);
		}
	}

	void reset() {
		mHead = null;
		mStrs = null;
	}

	public String getStr(int idx) {
		String s = null;
		if (mStrs != null) {
			s = mStrs.get(idx);
		}
		return s;
	}

	public int findStrIdx(String s) {
		int idx = -1;
		if (s != null && mStrs != null) {
			idx = mStrs.find(s);
		}
		return idx;
	}

}
