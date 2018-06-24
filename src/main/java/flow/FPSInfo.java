package flow;

import java.util.Arrays;
import xdata.Calc;

public class FPSInfo {
	public long mFramePrevT;
	public long mFrameDT;
	public double mFPS;
	public double mAvgFPS;
	public double mMinFPS;
	public double mMaxFPS;
	public double[] mFPSSamples = new double[10*3];
	protected int mFPSSmpIdx = -1;

	public void update() {
		if (mFPSSmpIdx < 0) {
			mFramePrevT = System.nanoTime();
		} else {
			long t = System.nanoTime();
			mFrameDT = t - mFramePrevT;
			mFramePrevT = t;
			double fps = 1.0 / (mFrameDT * 1.0e-9);
			if (mFPSSmpIdx == 0) {
				mFPS = fps;
				mAvgFPS = fps;
				mMinFPS = fps;
				mMaxFPS = fps;
			}
			int nsmp = mFPSSamples.length;
			if (mFPSSmpIdx >= nsmp) {
				Arrays.sort(mFPSSamples);
				mMinFPS = mFPSSamples[0];
				mMaxFPS = mFPSSamples[mFPSSamples.length - 1];
				if (Calc.isOdd(nsmp)) {
					mFPS = mFPSSamples[nsmp/2];
				} else {
					mFPS = (mFPSSamples[nsmp/2 - 1] + mFPSSamples[nsmp/2]) / 2;
				}
				mAvgFPS = 0;
				for (int i = 0; i < nsmp; ++i) {
					mAvgFPS += mFPSSamples[i];
				}
				mAvgFPS /= nsmp;
				mFPSSmpIdx = -1;
			} else {
				mFPSSamples[mFPSSmpIdx] = fps;
			}
		}
		++mFPSSmpIdx;
	}

}
