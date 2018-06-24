// Author: Sergey Chaban <sergey.chaban@gmail.com>

package xdata;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class Binary {
	private ByteBuffer mBytes;

	public boolean init(InputStream is) {
		try {
			int len = is.available();
			byte[] data = new byte[len];
			is.read(data);
			mBytes = ByteBuffer.allocateDirect(len);
			mBytes.order(ByteOrder.LITTLE_ENDIAN);
			mBytes.put(data);
		} catch (IOException ioe) {
			return false;
		}
		return true;
	}

	public void reset() {
		mBytes = null;
	}

	public int getI8(int at) {
		int res = 0;
		if (mBytes != null && at < mBytes.capacity()) {
			res = mBytes.get(at);
		}
		return res;
	}

	public int getU8(int at) {
		int res = 0;
		if (mBytes != null && at < mBytes.capacity()) {
			res = mBytes.get(at) & 0xFF;
		}
		return res;
	}

	public int getI32(int at) {
		int res = 0;
		if (mBytes != null && at + 4 <= mBytes.capacity()) {
			res = mBytes.getInt(at);
		}
		return res;
	}

	public int getI16(int at) {
		int res = 0;
		if (mBytes != null && at + 2 <= mBytes.capacity()) {
			for (int i = 0; i < 2; ++i) {
				res |= (mBytes.get(at + i) & 0xFF) << (16 + i*8);
			}
			res >>= 16;
		}
		return res;
	}

	public int getU16(int at) {
		int res = 0;
		if (mBytes != null && at + 2 <= mBytes.capacity()) {
			for (int i = 0; i < 2; ++i) {
				res |= (mBytes.get(at + i) & 0xFF) << (i*8);
			}
		}
		return res;
	}

	public float getF32(int at) {
		float res = Float.NaN;
		if (mBytes != null && at + 4 <= mBytes.capacity()) {
			res = mBytes.getFloat(at);
		}
		return res;
	}

	public String getStr(int at) {
		String res = null;
		if (mBytes != null) {
			res = "";
			int i = at;
			int max = mBytes.capacity();
			while (true) {
				if (i >= max) break;
				byte b = mBytes.get(i++);
				if (b == 0) break;
				res += (char)b;
			}
		}
		return res;
	}

	public String getStrLen(int at, int len) {
		StringBuilder sb = new StringBuilder(len);
		for (int i = 0; i < len; ++i) {
			byte b = mBytes.get(at + i);
			sb.append((char)b);
		}
		return sb.toString();
	}

	public String getKind() {
		String kind = null;
		if (mBytes != null && mBytes.capacity() >= 4) {
			kind = "";
			for (int i = 0; i < 4; ++i) {
				kind += (char)mBytes.get(i);
			}
		}
		return kind;
	}

	public int getBytes(byte[] ary, int at) {
		if (mBytes == null) return 0;
		int n = at + ary.length <= mBytes.capacity() ? ary.length : mBytes.capacity() - at;
		mBytes.position(at);
		mBytes.get(ary, 0, n);
		return n;
	}

	public int getBytes(byte[] ary, int dst, int at, int num) {
		if (mBytes == null) return 0;
		mBytes.position(at);
		mBytes.get(ary, dst, num);
		return num;
	}

	public void getInts(int[] ary, int at) {
		for (int i = 0; i < ary.length; ++i) {
			ary[i] = getI32(at + i*4);
		}
	}

	public void getInts(int[] ary, int dst, int at, int num) {
		for (int i = 0; i < num; ++i) {
			ary[dst + i] = getI32(at + i*4);
		}
	}

	public void getFloats(float[] ary, int at) {
		for (int i = 0; i < ary.length; ++i) {
			ary[i] = getF32(at + i*4);
		}
	}

	public void getFloats(float[] ary, int dst, int at, int num) {
		for (int i = 0; i < num; ++i) {
			ary[dst + i] = getF32(at + i*4);
		}
	}

	public int getFlags() { return getI32(4); }
	public int getFileSize() { return getI32(8); }
	public int getHeadSize() { return getI32(0xC); }
	public int getOffsStr() { return getI32(0x10); }
	public int getNameId() { return getI16(0x14); }
	public int getPathId() { return getI16(0x16); }
}
