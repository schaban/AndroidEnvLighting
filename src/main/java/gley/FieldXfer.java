// GLSL ES codegen
// Author: Sergey Chaban <sergey.chaban@gmail.com>

package gley;

import java.util.Arrays;

public abstract class FieldXfer {
	protected GParam mGP;
	protected int[] mXferLst;
	protected int[] mSwitches;

	protected final static int ENTRY_SIZE = 4;

	public FieldXfer() {
	}

	protected void init(GParam gp, int[] usage) {
		mGP = gp;
		mXferLst = mGP.makeFieldXferList(usage);
		int n = getSize();
		for (int i = 0; i < n; ++i) {
			mXferLst[i*ENTRY_SIZE + 3] = getFieldLocation(getFldName(i));
		}
	}

	public int getSize() {
		return mXferLst != null ? mXferLst.length / ENTRY_SIZE : 0;
	}

	public boolean ckIdx(int idx) {
		return idx >= 0 && idx < getSize();
	}

	protected int getNoCkFldGPID(int idx) {
		return mXferLst[idx*ENTRY_SIZE];
	}

	public int getFldGPID(int idx) {
		int gpid = -1;
		if (ckIdx(idx)) {
			gpid = getNoCkFldGPID(idx);
		}
		return gpid;
	}

	public String getFldName(int idx) {
		String name = null;
		if (mGP != null) {
			name = mGP.getNameFromGPID(getFldGPID(idx));
		}
		return name;
	}

	protected byte getNoCkFldType(int idx) {
		return (byte)(mXferLst[idx*ENTRY_SIZE + 1] & 0xFF);
	}

	public byte getFldType(int idx) {
		byte ft = GParam.T_NONE;
		if (ckIdx(idx)) {
			ft = getNoCkFldType(idx);
		}
		return ft;
	}

	protected int getNoCkFldArySize(int idx) {
		return mXferLst[idx*ENTRY_SIZE + 1] >>> 8;
	}

	public int getFldArySize(int idx) {
		int size = 0;
		if (ckIdx(idx)) {
			size = getNoCkFldArySize(idx);
		}
		return size;
	}

	protected int getNoCkFldSrcOffs(int idx) {
		return mXferLst[idx*ENTRY_SIZE + 2];
	}

	public int getFldSrcOffs(int idx) {
		int offs = -1;
		if (ckIdx(idx)) {
			offs = getNoCkFldSrcOffs(idx);
		}
		return offs;
	}

	protected int getNoCkFldDstLoc(int idx) {
		return mXferLst[idx*ENTRY_SIZE + 3];
	}

	public int getFldDstLoc(int idx) {
		int loc = -1;
		if (ckIdx(idx)) {
			loc = getNoCkFldDstLoc(idx);
		}
		return loc;
	}

	public int findFieldByGPID(int gpid) {
		int idx = -1;
		if (gpid >= 0) {
			int n = getSize();
			for (int i = 0; i < n; ++i) {
				if (getNoCkFldGPID(i) == gpid) {
					idx = i;
					break;
				}
			}
		}
		return idx;
	}

	public int findFieldByName(String name) {
		return mGP != null ? findFieldByGPID(mGP.getGPID(name)) : -1;
	}

	protected void allocSwitchBits() {
		if (mSwitches == null) {
			int n = getSize();
			if (n > 0) {
				int isize = ((n - 1) / 32) + 1;
				mSwitches = new int[isize];
				Arrays.fill(mSwitches, -1);
			}
		}
	}

	public void enableAll() {
		allocSwitchBits();
		Arrays.fill(mSwitches, -1);
	}

	public void disableAll() {
		allocSwitchBits();
		Arrays.fill(mSwitches, 0);
	}

	public void fldDisable(int idx) {
		if (ckIdx(idx)) {
			allocSwitchBits();
			mSwitches[idx/32] &= ~(1 << (idx & 31));
		}
	}

	public void fldDisable(String name) {
		fldDisable(mGP.getGPID(name));
	}

	public void fldEnable(int idx) {
		if (ckIdx(idx)) {
			allocSwitchBits();
			mSwitches[idx/32] |= 1 << (idx & 31);
		}
	}

	public void fldEnable(String name) {
		fldEnable(mGP.getGPID(name));
	}

	public void sendOne(int idx) {
		if (ckIdx(idx)) {
			sendField(idx);
		}
	}

	public void sendMasked(int[] mask) {
		int n = getSize();
		for (int i = 0; i < n; ++i) {
			boolean on = (mask[i >>> 5] & (1 << (i & 31))) != 0;
			if (on) {
				sendField(i);
			}
		}
	}

	public void send() {
		int n = getSize();
		if (mSwitches != null) {
			sendMasked(mSwitches);
		} else {
			for (int i = 0; i < n; ++i) {
				sendField(i);
			}
		}
	}

	public void dump(StringBuilder sb) {
		int n = getSize();
		for (int i = 0; i < n; ++i) {
			sb.append(i);
			sb.append(": ");
			GParam.emitFldType(sb, getFldType(i));
			sb.append("  ");
			sb.append(getFldName(i));
			int asize = getFldArySize(i);
			if (asize > 0) {
				sb.append("[");
				sb.append(asize);
				sb.append("]");
			}
			sb.append(" ");
			sb.append(getFldSrcOffs(i));
			sb.append(" -> ");
			sb.append(getFldDstLoc(i));
			sb.append("\n");
		}
	}

	protected abstract int getFieldLocation(String name);
	protected abstract void sendField(int idx);

}
