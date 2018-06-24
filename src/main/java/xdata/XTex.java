// Author: Sergey Chaban <sergey.chaban@gmail.com>

package xdata;

import java.util.Arrays;

public class XTex extends XData {

	public static final int HTBL_SIZE = 1 << 8;

	protected int mWidth;
	protected int mHeight;
	protected int mPlaneNum;
	protected int mPlaneOffs;

	protected ImgPlane[] mPlanes;

	public class Pyramid {
		public int mBaseWidth;
		public int mBaseHeight;
		public int[] mLvlOffs;
		public float[] mLvlData;

		public int getLvlNum() {
			return mLvlOffs != null ? mLvlOffs.length : 0;
		}


		void build() {
			int w0 = mWidth;
			int h0 = mHeight;
			int w = w0;
			int h = h0;
			boolean flgW = Calc.isPow2(w);
			boolean flgH = Calc.isPow2(h);
			int baseW = flgW ? w : (1 << (1 + Calc.log2i(w)));
			int baseH = flgH ? h : (1 << (1 + Calc.log2i(h)));
			w = baseW;
			h = baseH;
			int nlvl = 1;
			int npix = 1;
			while (w > 1 || h > 1) {
				npix += w*h;
				if (w > 1) w >>= 1;
				if (h > 1) h >>= 1;
				++nlvl;
			}
		}
	}

	public class ImgPlane {
		public String mName;
		public int mDataOffs;
		public int mNameId;
		public int mTrailingZeroes;
		public int mFormat;
		public float mMinVal;
		public float mMaxVal;
		public float mValOffs;
		public int mBitCount;
		public byte[] mData;

		public boolean isConst() { return mFormat == 0; }
		public boolean isCompressed() { return mFormat == 1; }
		public boolean isHDR() { return mMinVal < 0.0f || mMaxVal > 1.0f; }

		public void expand(float[] dst, int dstOrg, int dstStride, int[] wkTbl) {
			int w = XTex.this.mWidth;
			int h = XTex.this.mHeight;
			int n = w * h;
			if (isConst()) {
				int bits = mData[0] & 0xFF;
				for (int i = 1; i < 4; ++i) {
					bits |= (mData[i] & 0xFF) << (i << 3);
				}
				float val = Float.intBitsToFloat(bits);
				int idst = dstOrg;
				for (int i = 0; i < n; ++i) {
					dst[idst] = val;
					idst += dstStride;
				}
			} else if (isCompressed()) {
				XTex.expand(
				            dst, dstOrg, dstStride,
				            mData, 0, mBitCount,
				            mTrailingZeroes, mValOffs, wkTbl);
			} else {
				int idst = dstOrg;
				int isrc = 0;
				for (int i = 0; i < n; ++i) {
					int bits = mData[isrc];
					for (int j = 1; j < 4; ++j) {
						bits |= (mData[isrc + j] & 0xFF) << (j << 3);
					}
					isrc += 4;
					float val = Float.intBitsToFloat(bits);
					dst[idst] = val;
					idst += dstStride;
				}
			}
		}
	}

	public void init(Binary bin) {
		super.init(bin);
		if (!mHead.isTexture()) return;
		int pos = 0x20;
		mWidth = bin.getI32(pos); pos += 4;
		mHeight = bin.getI32(pos); pos += 4;
		mPlaneNum = bin.getI32(pos); pos += 4;
		mPlaneOffs = bin.getI32(pos);
		int npix = mWidth * mHeight;

		mPlanes = new ImgPlane[mPlaneNum];
		for (int i = 0; i < mPlaneNum; ++i) {
			pos = mPlaneOffs + (i * 0x20);
			ImgPlane plane = new ImgPlane();
			plane.mDataOffs = bin.getI32(pos); pos += 4;
			plane.mNameId = bin.getI16(pos); pos += 2;
			plane.mTrailingZeroes = bin.getU8(pos); pos += 1;
			plane.mFormat = bin.getI8(pos); pos += 1;
			plane.mMinVal = bin.getF32(pos); pos += 4;
			plane.mMaxVal = bin.getF32(pos); pos += 4;
			plane.mValOffs = bin.getF32(pos); pos += 4;
			plane.mBitCount = bin.getI32(pos);
			if (plane.isConst()) {
				plane.mData = new byte[4];
			} else if (plane.isCompressed()){
				plane.mData = new byte[(plane.mBitCount >>> 3) + 5];
			} else {
				plane.mData = new byte[npix * 4];
			}
			bin.getBytes(plane.mData, plane.mDataOffs);
			plane.mName = getStr(plane.mNameId);
			mPlanes[i] = plane;
		}
	}

	public int getWidth() {
		return mWidth;
	}

	public int getHeight() {
		return mHeight;
	}

	public boolean ckPlaneIdx(int idx) { return idx >= 0 && idx < mPlaneNum; }

	public int getPlanesNum() {
		return mPlanes == null ? 0 : mPlanes.length;
	}

	public int findPlane(String name) {
		int idx = -1;
		int n = getPlanesNum();
		for (int i = 0; i < n; ++i) {
			if (mPlanes[i].mName.equals(name)) {
				idx = i;
				break;
			}
		}
		return idx;
	}

	public ImgPlane getPlane(String name) {
		int idx = findPlane(name);
		if (ckPlaneIdx(idx)) {
			return mPlanes[idx];
		}
		return null;
	}

	public void getRGBA(float[] dst) {
		getRGBA(dst, 0, null);
	}

	public void getRGBA(float[] dst, int dstOrg, int[] wkTbl) {
		ImgPlane plnR = getPlane("r");
		ImgPlane plnG = getPlane("g");
		ImgPlane plnB = getPlane("b");
		ImgPlane plnA = getPlane("a");
		int[] tbl = wkTbl;
		if (tbl == null) {
			boolean cmpFlg = false;
			cmpFlg |= plnR != null && plnR.isCompressed();
			cmpFlg |= plnG != null && plnG.isCompressed();
			cmpFlg |= plnB != null && plnB.isCompressed();
			cmpFlg |= plnA != null && plnA.isCompressed();
			if (cmpFlg) {
				tbl = new int[HTBL_SIZE];
			}
		}
		int n = mWidth * mHeight;
		if (plnR != null) {
			plnR.expand(dst, dstOrg, 4, tbl);
		} else {
			int idst = dstOrg;
			for (int i = 0; i < n; ++i) {
				dst[idst] = 0.0f;
				idst += 4;
			}
		}
		if (plnG != null) {
			plnG.expand(dst, dstOrg + 1, 4, tbl);
		} else {
			int idst = dstOrg + 1;
			for (int i = 0; i < n; ++i) {
				dst[idst] = 0.0f;
				idst += 4;
			}
		}
		if (plnB != null) {
			plnB.expand(dst, dstOrg + 2, 4, tbl);
		} else {
			int idst = dstOrg + 2;
			for (int i = 0; i < n; ++i) {
				dst[idst] = 0.0f;
				idst += 4;
			}
		}
		if (plnA != null) {
			plnA.expand(dst, dstOrg + 3, 4, tbl);
		} else {
			int idst = dstOrg + 3;
			for (int i = 0; i < n; ++i) {
				dst[idst] = 1.0f;
				idst += 4;
			}
		}
	}

	public void getRGB(float[] dst, int dstOrg, int[] wkTbl, int pixelStride) {
		ImgPlane plnR = getPlane("r");
		ImgPlane plnG = getPlane("g");
		ImgPlane plnB = getPlane("b");
		int[] tbl = wkTbl;
		if (tbl == null) {
			boolean cmpFlg = false;
			cmpFlg |= plnR != null && plnR.isCompressed();
			cmpFlg |= plnG != null && plnG.isCompressed();
			cmpFlg |= plnB != null && plnB.isCompressed();
			if (cmpFlg) {
				tbl = new int[HTBL_SIZE];
			}
		}
		int n = mWidth * mHeight;
		if (plnR != null) {
			plnR.expand(dst, dstOrg, pixelStride, tbl);
		} else {
			int idst = dstOrg;
			for (int i = 0; i < n; ++i) {
				dst[idst] = 0.0f;
				idst += pixelStride;
			}
		}
		if (plnG != null) {
			plnG.expand(dst, dstOrg + 1, pixelStride, tbl);
		} else {
			int idst = dstOrg + 1;
			for (int i = 0; i < n; ++i) {
				dst[idst] = 0.0f;
				idst += pixelStride;
			}
		}
		if (plnB != null) {
			plnB.expand(dst, dstOrg + 2, pixelStride, tbl);
		} else {
			int idst = dstOrg + 2;
			for (int i = 0; i < n; ++i) {
				dst[idst] = 0.0f;
				idst += pixelStride;
			}
		}
	}

	public void getRGB(float[] dst, int dstOrg, int[] wkTbl) {
		getRGB(dst, dstOrg, wkTbl, 3);
	}

	public boolean isHDR() {
		final String[] rgbNames = new String[] { "r", "g", "b" };
		for (String plnName : rgbNames) {
			ImgPlane pln = getPlane(plnName);
			if (pln != null && pln.isHDR()) return true;
		}
		return false;
	}

	public static void expand(
	        float[] dst, int dstOrg, int dstStride,
	        byte[] src, int srcOrg, int bitCount,
	        int trailingZeroes, float valOffs,
			int[] extTbl
	) {
		int[] tbl = extTbl;
		if (tbl == null) {
			tbl = new int[HTBL_SIZE];
		}
		Arrays.fill(tbl, 0);
		int tblMask = HTBL_SIZE - 1;
		int bitCnt = bitCount;
		int bitPtr = 0;
		int pred = 0;
		int hash = 0;
		int idst = dstOrg;
		while (bitCnt > 0) {
			int bitLen = Util.fetchBits32(src, bitPtr, 5);
			bitPtr += 5;
			int bxor = 0;
			if (bitLen > 0) {
				bxor = Util.fetchBits32(src, bitPtr, bitLen);
				bitPtr += bitLen;
			}
			bitCnt -= 5 + bitLen;
			int ival = bxor ^ pred;
			tbl[hash] = ival;
			hash = (ival >>> 21) & tblMask;
			pred = tbl[hash];
			int fbits = ival << trailingZeroes;
			float fval = Float.intBitsToFloat(fbits);
			fval += valOffs;
			dst[idst] = fval;
			idst += dstStride;
		}
	}

	protected static void calcResampleWgts(float[] wgts, short[] orgs, int oldRes, int newRes) {
		float rt = (float)oldRes / (float)newRes;
		float fw = 2.0f;
		for (int i = 0; i < newRes; ++i) {
			float c = ((float)i + 0.5f) * rt;
			float org = (float)Math.floor((c - fw) + 0.5f);
			orgs[i] = (short)org;
			int iwgt = i * 4;
			float s = 0.0f;
			for (int j = 0; j < 4; ++j) {
				float pos = org + (float)j + 0.5f;
				float x = Math.abs((pos - c) / fw);
				float w;
				if (x < 1.0e-5f) {
					w = 1.0f;
				} else if (x > 1.0f) {
					w = 1.0f;
				} else {
					x *= (float)Math.PI;
					w = Calc.sinc(x*2.0f) * Calc.sinc(x);
				}
				wgts[iwgt + j] = w;
				s += w;
			}
			s = Calc.rcp0(s);
			for (int j = 0; j < 4; ++j) {
				wgts[iwgt + j] *= s;
			}
		}
	}

}
