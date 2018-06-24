// Author: Sergey Chaban <sergey.chaban@gmail.com>

package flow;

import static android.opengl.GLES20.*;

public class DrawState {

	public final static int CMP_NEVER        = 0;
	public final static int CMP_GREATEREQUAL = 1;
	public final static int CMP_EQUAL        = 2;
	public final static int CMP_GREATER      = 3;
	public final static int CMP_LESSEQUAL    = 4;
	public final static int CMP_NOTEQUAL     = 5;
	public final static int CMP_LESS         = 6;
	public final static int CMP_ALWAYS       = 7;
	public final static int _CMP_BITS = 3;
	public final static int _CMP_MASK = (1 << _CMP_BITS) - 1;

	public final static int CULL_NONE = 0;
	public final static int CULL_CCW = 1;
	public final static int CULL_CW = 2;
	public final static int _CULL_BITS = 2;
	public final static int _CULL_MASK = (1 << _CULL_BITS) - 1;

	/*
		0: cull : 2
		2: ztest : 1
		3: zwrite : 1
		4: zfunc : 3
	 */
	protected int mBits;

	public DrawState() {
		setCullCCW();
		setZTest(true);
		setZWrite(true);
		setZFunc(CMP_LESSEQUAL);
	}

	public int getBits() {
		return mBits;
	}

	public void setCull(int cull) {
		mBits &= ~_CULL_MASK;
		mBits |= cull & _CULL_MASK;
	}

	public void setCullNONE() {
		setCull(CULL_NONE);
	}

	public void setCullCCW() {
		setCull(CULL_CCW);
	}

	public void setCullCW() {
		setCull(CULL_CW);
	}

	public void setZTest(boolean enable) {
		if (enable) {
			mBits |= 1 << 2;
		} else {
			mBits &= ~(1 << 2);
		}
	}

	public void setZWrite(boolean enable) {
		if (enable) {
			mBits |= 1 << 3;
		} else {
			mBits &= ~(1 << 3);
		}
	}

	public void setZFunc(int func) {
		mBits &= ~(_CMP_MASK << 4);
		mBits |= (func & _CMP_MASK) << 4;
	}

	public static int xlatCmpFunc(int cmp) {
		int glval = GL_NEVER;
		switch (cmp) {
			case CMP_GREATEREQUAL:
				glval = GL_GEQUAL;
				break;
			case CMP_EQUAL:
				glval = GL_EQUAL;
				break;
			case CMP_GREATER:
				glval = GL_GREATER;
				break;
			case CMP_LESSEQUAL:
				glval = GL_LEQUAL;
				break;
			case CMP_NOTEQUAL:
				glval = GL_NOTEQUAL;
				break;
			case CMP_LESS:
				glval = GL_LESS;
				break;
			case CMP_ALWAYS:
				glval = GL_ALWAYS;
				break;
			default:
				break;
		}
		return glval;
	}

	public static void apply(int bits) {
		int cull = bits & _CULL_MASK;
		if (cull == CULL_NONE) {
			glDisable(GL_CULL_FACE);
		} else {
			glEnable(GL_CULL_FACE);
			switch (cull) {
				case CULL_CCW:
				default:
					glFrontFace(GL_CW);
					break;
				case CULL_CW:
					glFrontFace(GL_CCW);
					break;
			}
			glCullFace(GL_BACK);
		}

		boolean ztest = ((bits >>> 2) & 1) != 0;
		if (ztest) {
			glEnable(GL_DEPTH_TEST);
		} else {
			glDisable(GL_DEPTH_TEST);
		}

		boolean zwrite = ((bits >>> 3) & 1) != 0;
		glDepthMask(zwrite);

		int cmp = (bits >>> 4) & _CMP_MASK;
		glDepthFunc(xlatCmpFunc(cmp));
	}
}
