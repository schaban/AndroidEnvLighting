// Author: Sergey Chaban <sergey.chaban@gmail.com>

package flow;

import static android.opengl.GLES20.*;

public class TexParam {

	public final static int FMAG_NEAREST = 0;
	public final static int FMAG_LINEAR = 1;
	public final static int _FMAG_BITS = 1;
	public final static int _FMAG_MASK = (1 << _FMAG_BITS) - 1;

	public final static int FMIN_NEAREST = 0;
	public final static int FMIN_LINEAR = 1;
	public final static int FMIN_MIP_NEAREST = 2;
	public final static int FMIN_MIP_LINEAR = 3;
	public final static int FMIN_MIP_BILINEAR = 4;
	public final static int FMIN_MIP_TRILINEAR = 5;
	public final static int _FMIN_BITS = 3;
	public final static int _FMIN_MASK = (1 << _FMIN_BITS) - 1;

	public final static int TWRAP_REPEAT = 0;
	public final static int TWRAP_CLAMP = 1;
	public final static int TWRAP_MIRROR = 2;
	public final static int _TWRAP_BITS = 3;
	public final static int _TWRAP_MASK = (1 << _TWRAP_BITS) - 1;

	/*
		00: fmag
		01: fmin
		04: wrapu
		07: wrapv
	 */
	protected int mBits;

	public TexParam() {
		mBits = 0;
		setFiltLinear();
		setWrapU(TWRAP_REPEAT);
		setWrapV(TWRAP_REPEAT);
	}

	public int getBits() {
		return mBits;
	}

	public void setMagFilt(int filt) {
		mBits = setMagFilt(mBits, filt);
	}

	public void setMinFilt(int filt) {
		mBits = setMinFilt(mBits, filt);
	}

	public void setFiltLow() {
		mBits = setFiltLow(mBits);
	}

	public void setFiltMedium() {
		mBits = setFiltMedium(mBits);
	}

	public void setFiltHigh() {
		mBits = setFiltHigh(mBits);
	}

	public void setFiltLinear() {
		mBits = setFiltLinear(mBits);
	}

	public void setWrapU(int wrap) {
		mBits = setWrapU(mBits, wrap);
	}

	public void setWrapV(int wrap) {
		mBits = setWrapV(mBits, wrap);
	}

	public void apply() {
		apply(mBits);
	}

	public static int xlatWrap(int wrap) {
		int glval;
		switch (wrap) {
			case TWRAP_REPEAT:
				glval = GL_REPEAT;
				break;
			case TWRAP_CLAMP:
			default:
				glval = GL_CLAMP_TO_EDGE;
				break;
			case TWRAP_MIRROR:
				glval = GL_MIRRORED_REPEAT;
				break;
		}
		return glval;
	}

	public static int xlatMagFilt(int filt) {
		int glval;
		switch (filt) {
			case FMAG_NEAREST:
				glval = GL_NEAREST;
				break;
			case FMAG_LINEAR:
			default:
				glval = GL_LINEAR;
				break;
		}
		return glval;
	}

	public static int xlatMinFilt(int filt) {
		int glval;
		switch (filt) {
			case FMIN_NEAREST:
				glval = GL_NEAREST;
				break;
			case FMIN_LINEAR:
			default:
				glval = GL_LINEAR;
				break;
			case FMIN_MIP_NEAREST:
				glval = GL_NEAREST_MIPMAP_NEAREST;
				break;
			case FMIN_MIP_LINEAR:
				glval = GL_NEAREST_MIPMAP_LINEAR;
				break;
			case FMIN_MIP_BILINEAR:
				glval = GL_LINEAR_MIPMAP_NEAREST;
				break;
			case FMIN_MIP_TRILINEAR:
				glval = GL_LINEAR_MIPMAP_LINEAR;
				break;
		}
		return glval;
	}

	public static int setMagFilt(int bits, int filt) {
		bits &= ~_FMAG_MASK;
		bits |= filt & _FMAG_MASK;
		return bits;
	}

	public static int setMinFilt(int bits, int filt) {
		bits &= ~(_FMIN_MASK << 1);
		bits |= (filt & _FMIN_MASK) << 1;
		return bits;
	}

	public static int setFiltLow(int bits) {
		bits = setMagFilt(bits, FMAG_NEAREST);
		bits = setMinFilt(bits, FMIN_NEAREST);
		return bits;
	}

	public static int setFiltMedium(int bits) {
		bits = setMagFilt(bits, FMAG_LINEAR);
		bits = setMinFilt(bits, FMIN_MIP_BILINEAR);
		return bits;
	}

	public static int setFiltHigh(int bits) {
		bits = setMagFilt(bits, FMAG_LINEAR);
		bits = setMinFilt(bits, FMIN_MIP_TRILINEAR);
		return bits;
	}

	public static int setFiltLinear(int bits) {
		bits = setMagFilt(bits, FMAG_LINEAR);
		bits = setMinFilt(bits, FMIN_LINEAR);
		return bits;
	}

	public static int setWrapU(int bits, int wrap) {
		bits &= ~(_TWRAP_MASK << 4);
		bits |= (wrap & _TWRAP_MASK) << 4;
		return bits;
	}

	public static int setWrapV(int bits, int wrap) {
		bits &= ~(_TWRAP_MASK << 7);
		bits |= (wrap & _TWRAP_MASK) << 7;
		return bits;
	}

	public static void apply(int bits) {
		int fmag = bits & _FMAG_MASK;
		fmag = xlatMagFilt(fmag);
		glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, fmag);
		int fmin = (bits >>> 1) & _FMIN_MASK;
		fmin = xlatMinFilt(fmin);
		glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, fmin);
		int wu = (bits >>> 4) & _TWRAP_MASK;
		wu = xlatWrap(wu);
		glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, wu);
		int wv = (bits >>> 7) & _TWRAP_MASK;
		wv = xlatWrap(wv);
		glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, wv);
	}

}
