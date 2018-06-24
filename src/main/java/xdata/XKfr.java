// Author: Sergey Chaban <sergey.chaban@gmail.com>

package xdata;

public class XKfr extends XData {

	public enum Func {
		CONSTANT,
		LINEAR,
		CUBIC;

		public static Func fromInt(int i) {
			Func func = LINEAR;
			switch (i) {
				case 0: func = CONSTANT;
				case 2: func = CUBIC;
			}
			return func;
		}
	}

	protected float mFPS;
	protected int mMinFrame;
	protected int mMaxFrame;
	protected int mFCurveNum;
	protected int mFCurveOffs;
	protected int mNodeInfoNum;
	protected int mNodeInfoOffs;

	public class NodeInfo {
		public String mPath;
		public String mName;
		public String mType;
		public short mPathId;
		public short mNameId;
		public short mTypeId;
		public RotOrd mRotOrd;
		public XformOrd mXfmOrd;

		public void init(Binary bin, int top) {
			int pos = top;
			mPathId = (short)bin.getI16(pos); pos += 2;
			mNameId = (short)bin.getI16(pos); pos += 2;
			mTypeId = (short)bin.getI16(pos); pos += 2;
			mRotOrd = RotOrd.fromInt(bin.getU8(pos)); pos++;
			mXfmOrd = XformOrd.fromInt(bin.getU8(pos));
			mPath = getStr(mPathId);
			mName = getStr(mNameId);
			mType = getStr(mTypeId);
		}
	}

	public class FCurve {
		public String mNodePath;
		public String mNodeName;
		public String mChanName;
		public short mNodePathId;
		public short mNodeNameId;
		public short mChanNameId;
		public int mKeyNum;
		public float mMinVal;
		public float mMaxVal;
		public Func mCmnFunc;

		public float[] mVals;
		public float[] mLSlopes;
		public float[] mRSlopes;
		public Func[] mFuncs;

		public void init(Binary bin, int top) {
			int pos = top;
			mNodePathId = (short)bin.getI16(pos); pos += 2;
			mNodeNameId = (short)bin.getI16(pos); pos += 2;
			mChanNameId = (short)bin.getI16(pos); pos += 2;
			mKeyNum = bin.getU16(pos); pos += 2;
			mMinVal = bin.getF32(pos); pos += 4;
			mMaxVal = bin.getF32(pos); pos += 4;
			int offsVal = bin.getI32(pos); pos += 4;
			int offsLSlope = bin.getI32(pos); pos += 4;
			int offsRSlope = bin.getI32(pos); pos += 4;
			int offsFNo = bin.getI32(pos); pos += 4;
			int offsFunc = bin.getI32(pos); pos += 4;
			mCmnFunc = Func.fromInt(bin.getU8(pos));
			mNodePath = getStr(mNodePathId);
			mNodeName = getStr(mNodeNameId);
			mChanName = getStr(mChanNameId);
			if (mKeyNum > 0) {
				if (offsVal > 0) {
					mVals = new float[mKeyNum];
					bin.getFloats(mVals, offsVal);
				}
				if (offsLSlope > 0) {
					mLSlopes = new float[mKeyNum];
					bin.getFloats(mLSlopes, offsVal);
				}
				if (offsRSlope > 0) {
					mRSlopes = new float[mKeyNum];
					bin.getFloats(mRSlopes, offsVal);
				}
				if (offsFunc > 0) {
					mFuncs = new Func[mKeyNum];
					for (int i = 0; i < mKeyNum; ++i) {
						mFuncs[i] = Func.fromInt(bin.getU8(offsFunc + i));
					}
				}
			}
		}
	}

	protected NodeInfo[] mNodes;
	protected FCurve[] mFCurves;

	public void init(Binary bin) {
		super.init(bin);
		if (!mHead.isKeyframes()) return;
		int pos = 0x20;
		mFPS = bin.getF32(pos); pos += 4;
		mMinFrame = bin.getI32(pos); pos += 4;
		mMaxFrame = bin.getI32(pos); pos += 4;
		mFCurveNum = bin.getI32(pos); pos += 4;
		mFCurveOffs = bin.getI32(pos); pos += 4;
		if (mHead.mHeadSize > 0x34) {
			mNodeInfoNum = bin.getI32(pos); pos += 4;
			mNodeInfoOffs = bin.getI32(pos);
		} else {
			mNodeInfoNum = 0;
			mNodeInfoOffs = 0;
		}

		if (mNodeInfoNum > 0 && mNodeInfoOffs > 0) {
			mNodes = new NodeInfo[mNodeInfoNum];
			for (int i = 0; i < mNodeInfoNum; ++i) {
				mNodes[i] = new NodeInfo();
				mNodes[i].init(bin, mNodeInfoOffs + i*8);
			}
		}

		if (mFCurveNum > 0 && mFCurveOffs > 0) {
			mFCurves = new FCurve[mFCurveNum];
			for (int i = 0; i < mFCurveNum; ++i) {
				mFCurves[i] = new FCurve();
				mFCurves[i].init(bin, mFCurveOffs + i*0x30);
			}
		}
	}

}
