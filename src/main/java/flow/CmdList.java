// Author: Sergey Chaban <sergey.chaban@gmail.com>

package flow;

import java.util.Arrays;
import java.util.Locale;

import gley.*;

import static android.opengl.GLES20.*;

public class CmdList {

	protected final static int PKT_PTR_BITS = 31;
	protected final static int PKT_PTR_MASK = (1 << PKT_PTR_BITS) - 1;

	protected final static int CMD_NOP = 0;
	protected final static int CMD_PROG = 1;
	protected final static int CMD_SETF = 2;
	protected final static int CMD_SEND = 3;
	protected final static int CMD_DRWST = 4;
	protected final static int CMD_BLEND = 5;
	protected final static int CMD_OPAQ = 6;
	protected final static int CMD_SEMI = 7;
	protected final static int CMD_TEX = 8;
	protected final static int CMD_CALL = 9;
	protected final static int CMD_END = 0xFF;

	protected long[] mKeys;
	protected int mKeyPtr;

	protected int[] mPkt;
	protected int mPktPtr;
	protected int mCurPktTop = -1;

	protected float[] mFltData;
	protected int mFltPtr;

	protected Object[] mObjs;
	protected int mObjPtr;

	protected GParam mGP;

	public void init(GParam gp, int maxPkt) {
		mGP = gp;
		mKeys = new long[maxPkt];
		mPkt = new int[maxPkt * 16];
		mFltData = new float[maxPkt * 24];
		mObjs = new Object[maxPkt * 4];
		reset();
	}

	public void init(GParam gp) {
		init(gp, 1000);
	}

	public void reset() {
		mKeyPtr = 0;
		mPktPtr = 0;
		mFltPtr = 0;
		mObjPtr = 0;
		if (mObjs != null) {
			Arrays.fill(mObjs, null);
		}
	}

	public void startPacket(int key) {
		long ki = key;
		ki <<= PKT_PTR_BITS;
		ki |= mPktPtr & PKT_PTR_MASK;
		mKeys[mKeyPtr] = ki;
		mCurPktTop = mPktPtr;
	}

	public void putPROG(int progId) {
		mPkt[mPktPtr++] = CMD_PROG;
		mPkt[mPktPtr++] = progId;
	}

	public void putSETF(int gpid, int dstOffs, float[] src, int srcOffs, int num) {
		if (gpid < 0) return;
		mPkt[mPktPtr++] = CMD_SETF;
		mPkt[mPktPtr++] = gpid;
		mPkt[mPktPtr++] = dstOffs;
		mPkt[mPktPtr++] = mFltPtr;
		mPkt[mPktPtr++] = num;
		/*
		for (int i = 0; i < num; ++i) {
			mFltData[mFltPtr++] = src[srcOffs + i];
		}*/
		System.arraycopy(src, srcOffs, mFltData, mFltPtr, num);
		mFltPtr += num;
	}

	public void putSETF(int gpid, float... src) {
		putSETF(gpid, 0, src, 0, src.length);
	}

	public void putSETF(String name, int dstOffs, float[] src, int srcOffs, int num) {
		int gpid = mGP.getGPID(name);
		putSETF(gpid, dstOffs, src, srcOffs, num);
	}

	public void putSETF(String name, float... src) {
		putSETF(name, 0, src, 0, src.length);
	}

	public void putSEND(FieldXfer xfer) {
		mPkt[mPktPtr++] = CMD_SEND;
		mPkt[mPktPtr++] = mObjPtr;
		mObjs[mObjPtr++] = xfer;
	}

	public void putOPAQ() {
		mPkt[mPktPtr++] = CMD_OPAQ;
	}

	public void putSEMI() {
		mPkt[mPktPtr++] = CMD_SEMI;
	}

	public void putBLEND(int bits) {
		mPkt[mPktPtr++] = CMD_BLEND;
		mPkt[mPktPtr++] = bits;
	}

	public void putBLEND(BlendState st) {
		putBLEND(st.getBits());
	}

	public void putDRWST(int bits) {
		mPkt[mPktPtr++] = CMD_DRWST;
		mPkt[mPktPtr++] = bits;
	}

	public void putDRWST(DrawState st) {
		putDRWST(st.getBits());
	}

	public void putTEX(int texUnit, int texHandle, int smpLoc) {
		mPkt[mPktPtr++] = CMD_TEX;
		mPkt[mPktPtr++] = texUnit;
		mPkt[mPktPtr++] = texHandle;
		mPkt[mPktPtr++] = smpLoc;
	}

	public void putCALL(CallIfc call, int param) {
		mPkt[mPktPtr++] = CMD_CALL;
		mPkt[mPktPtr++] = mObjPtr;
		mPkt[mPktPtr++] = param;
		mObjs[mObjPtr++] = call;
	}

	public void endPacket() {
		mPkt[mPktPtr++] = CMD_END;
		mCurPktTop = -1;
		++mKeyPtr;
	}

	protected int execPROG(int p) {
		int progId = mPkt[p++];
		glUseProgram(progId);
		return p;
	}

	protected int execSETF(int p) {
		int gpid = mPkt[p++];
		int dstOffs = mPkt[p++];
		int fltPtr = mPkt[p++];
		int num = mPkt[p++];
		mGP.setF(gpid, dstOffs, mFltData, fltPtr, num);
		return p;
	}

	protected int execSEND(int p) {
		int objPtr = mPkt[p++];
		Object obj = mObjs[objPtr];
		if (obj instanceof FieldXfer) {
			((FieldXfer)obj).send();
		}
		return p;
	}

	protected int execDRWST(int p) {
		int bits = mPkt[p++];
		DrawState.apply(bits);
		return p;
	}

	protected int execBLEND(int p) {
		int bits = mPkt[p++];
		BlendState.apply(bits);
		return p;
	}

	protected int execTEX(int p) {
		int texUnit = mPkt[p++];
		int texHandle = mPkt[p++];
		int smpLoc = mPkt[p++];
		glActiveTexture(GL_TEXTURE0 + texUnit);
		glBindTexture(GL_TEXTURE_2D, texHandle);
		glUniform1i(smpLoc, texUnit);
		return p;
	}

	protected int execCALL(int p) {
		int objPtr = mPkt[p++];
		int param = mPkt[p++];
		Object obj = mObjs[objPtr];
		if (obj instanceof CallIfc) {
			((CallIfc)obj).cmdListExec(param);
		}
		return p;
	}

	protected void execPacket(int ptr) {
		while (true) {
			int cmd = mPkt[ptr++];
			if (cmd == CMD_END) {
				glUseProgram(0);
				break;
			}
			switch (cmd) {
				case CMD_PROG:
					ptr = execPROG(ptr);
					break;
				case CMD_SETF:
					ptr = execSETF(ptr);
					break;
				case CMD_SEND:
					ptr = execSEND(ptr);
					break;
				case CMD_DRWST:
					ptr = execDRWST(ptr);
					break;
				case CMD_BLEND:
					ptr = execBLEND(ptr);
					break;
				case CMD_OPAQ:
					BlendState.opaq();
					break;
				case CMD_SEMI:
					BlendState.semi();
					break;
				case CMD_TEX:
					ptr = execTEX(ptr);
					break;
				case CMD_CALL:
					ptr = execCALL(ptr);
					break;
			}
		}
	}

	public void exec() {
		int npkt = mKeyPtr;
		for (int i = 0; i < npkt; ++i) {
			int ipkt = (int)(mKeys[i] & PKT_PTR_MASK);
			execPacket(ipkt);
		}
	}

	public void sort() {
		int npkt = mKeyPtr;
		Arrays.sort(mKeys, 0, npkt);
	}


	private static void dumpFlt(StringBuilder sb, float val) {
		sb.append(String.format(Locale.US, "%.8f", val));
	}

	protected void dumpPtr(StringBuilder sb, int ptr) {
		sb.append(String.format("%06X:", ptr));
	}

	protected void dumpHex(StringBuilder sb, int val) {
		sb.append(String.format("%08X", val));
	}

	protected int dumpPROG(StringBuilder sb, int p) {
		int progId = mPkt[p++];
		sb.append("PROG ");
		sb.append(progId);
		return p;
	}

	protected int dumpSETF(StringBuilder sb, int p) {
		int gpid = mPkt[p++];
		int dstOffs = mPkt[p++];
		int fltPtr = mPkt[p++];
		int num = mPkt[p++];
		sb.append("SETF ");
		sb.append(mGP.getNameFromGPID(gpid));
		sb.append(" dst:");
		sb.append(dstOffs);
		sb.append(", flt:");
		sb.append(fltPtr);
		sb.append(", num:");
		sb.append(num);
		return p;
	}

	protected int dumpSEND(StringBuilder sb, int p) {
		int objPtr = mPkt[p++];
		Object obj = mObjs[objPtr];
		sb.append("SEND ");
		if (obj instanceof FieldXfer) {
			sb.append(obj);
		} else {
			sb.append("???");
		}
		return p;
	}

	protected int dumpDRWST(StringBuilder sb, int p) {
		int bits = mPkt[p++];
		sb.append("DRWST ");
		dumpHex(sb, bits);
		return p;
	}

	protected int dumpBLEND(StringBuilder sb, int p) {
		int bits = mPkt[p++];
		sb.append("BLEND ");
		dumpHex(sb, bits);
		return p;
	}

	protected int dumpTEX(StringBuilder sb, int p) {
		int texUnit = mPkt[p++];
		int texHandle = mPkt[p++];
		int smpLoc = mPkt[p++];
		sb.append("TEX ");
		sb.append(texUnit);
		sb.append(", ");
		sb.append(texHandle);
		sb.append(", ");
		sb.append(smpLoc);
		return p;
	}

	protected int dumpCALL(StringBuilder sb, int p) {
		int objPtr = mPkt[p++];
		int param = mPkt[p++];
		Object obj = mObjs[objPtr];
		if (obj instanceof CallIfc) {
			sb.append(obj);
			sb.append(" param:");
			sb.append(param);
		} else {
			sb.append("???");
		}
		return p;
	}

	protected void dumpPacket(StringBuilder sb, int ptr) {
		while (true) {
			dumpPtr(sb, ptr);
			sb.append(" ");
			int cmd = mPkt[ptr++];
			if (cmd == CMD_END) {
				sb.append("END\n");
				break;
			}
			switch (cmd) {
				case CMD_PROG:
					ptr = dumpPROG(sb, ptr);
					break;
				case CMD_SETF:
					ptr = dumpSETF(sb, ptr);
					break;
				case CMD_SEND:
					ptr = dumpSEND(sb, ptr);
					break;
				case CMD_DRWST:
					ptr = dumpDRWST(sb, ptr);
					break;
				case CMD_BLEND:
					ptr = dumpBLEND(sb, ptr);
					break;
				case CMD_OPAQ:
					sb.append("OPAQ");
					break;
				case CMD_SEMI:
					sb.append("SEMI");
					break;
				case CMD_TEX:
					ptr = dumpTEX(sb, ptr);
					break;
				case CMD_CALL:
					ptr = dumpCALL(sb, ptr);
					break;
			}
			sb.append("\n");
		}
	}

	public void dump(StringBuilder sb) {
		int npkt = mKeyPtr;
		for (int i = 0; i < npkt; ++i) {
			int ipkt = (int)(mKeys[i] & PKT_PTR_MASK);
			dumpPacket(sb, ipkt);
		}
	}

}
