// Author: Sergey Chaban <sergey.chaban@gmail.com>

package flow;

import static android.opengl.GLES20.*;

public class BlendState {

	public final static int OP_ADD = 0;
	public final static int OP_SUB = 1;
	public final static int OP_RSUB = 2;
	public final static int _OP_BITS = 2;
	public final static int _OP_MASK = (1 << _OP_BITS) - 1;

	public final static int MODE_ZERO     = 0;
	public final static int MODE_ONE      = 1;
	public final static int MODE_SRC      = 2;
	public final static int MODE_INVSRC   = 3;
	public final static int MODE_DST      = 4;
	public final static int MODE_INVDST   = 5;
	public final static int MODE_SRCA     = 6;
	public final static int MODE_INVSRCA  = 7;
	public final static int MODE_DSTA     = 8;
	public final static int MODE_INVDSTA  = 9;
	public final static int _MODE_BITS = 4;
	public final static int _MODE_MASK = (1 << _MODE_BITS) - 1;

	/*
		00: op
		02: src
		06: dst
		0A: opA
		0C: srcA
		10: dstA
		14: enable
	 */
	protected int mBits;

	public int getBits() {
		return mBits;
	}

	public static int xlatOp(int op) {
		int glval;
		switch (op) {
			case OP_ADD:
			default:
				glval = GL_FUNC_ADD;
				break;
			case OP_SUB:
				glval = GL_FUNC_SUBTRACT;
				break;
			case OP_RSUB:
				glval = GL_FUNC_REVERSE_SUBTRACT;
				break;
		}
		return glval;
	}

	public static int xlatMode(int mode) {
		int glval;
		switch (mode) {
			case MODE_ZERO:
				glval = GL_ZERO;
				break;
			case MODE_ONE:
				glval = GL_ONE;
				break;
			case MODE_SRC:
				glval = GL_SRC_COLOR;
				break;
			case MODE_INVSRC:
				glval = GL_ONE_MINUS_SRC_COLOR;
				break;
			case MODE_DST:
				glval = GL_DST_COLOR;
				break;
			case MODE_INVDST:
				glval = GL_ONE_MINUS_DST_COLOR;
				break;
			case MODE_SRCA:
				glval = GL_SRC_ALPHA;
				break;
			case MODE_INVSRCA:
				glval = GL_ONE_MINUS_SRC_ALPHA;
				break;
			case MODE_DSTA:
				glval = GL_DST_ALPHA;
				break;
			case MODE_INVDSTA:
				glval = GL_ONE_MINUS_DST_ALPHA;
				break;
			default:
				glval = GL_ONE;
				break;
		}
		return glval;
	}

	public static void apply(int bits) {
		boolean enable = ((bits >>> 0x14) & 1) != 0;
		if (enable) {
			glEnable(GL_BLEND);
			int op = xlatOp(bits & _OP_MASK);
			int src = xlatMode((bits >>> 2) & _MODE_MASK);
			int dst = xlatMode((bits >>> 6) & _MODE_MASK);
			int opA = xlatOp((bits >>> 0xA) & _OP_MASK);
			int srcA = xlatMode((bits >>> 0xC) & _MODE_MASK);
			int dstA = xlatMode((bits >>> 0x10) & _MODE_MASK);
			glBlendEquationSeparate(op, opA);
			glBlendFuncSeparate(src, dst, srcA, dstA);
		} else {
			glDisable(GL_BLEND);
		}
	}

	public static void opaq() {
		glDisable(GL_BLEND);
	}

	public static void semi() {
		glEnable(GL_BLEND);
		glBlendEquation(GL_FUNC_ADD);
		glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
	}

}
