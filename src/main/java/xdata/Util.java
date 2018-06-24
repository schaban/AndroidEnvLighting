// Author: Sergey Chaban <sergey.chaban@gmail.com>

package xdata;

public final class Util {

	public static short floatToHalf(float x) {
		int b = Float.floatToIntBits(x);
		int s = (b >> 16) & (1 << 15);
		b &= ~(1 << 31);
		if (b > 0x477FE000) {
			b = 0x7C00; /* infinity */
		} else {
			if (b < 0x38800000) {
				int r = 0x70 + 1 - (b >>> 23);
				b &= (1 << 23) - 1;
				b |= (1 << 23);
				b >>>= r;
			} else {
				b += 0xC8000000;
			}
			int a = (b >>> 13) & 1;
			b += (1 << 12) - 1;
			b += a;
			b >>>= 13;
			b &= (1 << 15) - 1;
		}
		return (short)(b | s);
	}

	public static float halfToFloat(short sh) {
		int h = (int)sh & 0xFFFF;
		float f = 0.0f;
		if ( (h & ((1 << 16) - 1)) != 0 ) {
			int e = (((h >> 10) & 0x1F) + 0x70) << 23;
			int m = (h & ((1 << 10) - 1)) << (23 - 10);
			int s = (h >>> 15) << 31;
			f = Float.intBitsToFloat(e | m | s);
		}
		return f;
	}

	public static int calcBitAryIntSize(int n) {
		return ((n - 1) / 32) + 1;
	}

	public static boolean bitCk(int[] ary, int idx) {
		return (ary[idx/32] & (1 << (idx & 31))) != 0;
	}

	public static void bitSt(int[] ary, int idx) {
		ary[idx/32] |= 1 << (idx & 31);
	}

	public static void bitCl(int[] ary, int idx) {
		ary[idx/32] &= ~(1 << (idx & 31));
	}

	public static void bitSw(int[] ary, int idx) {
		ary[idx/32] ^= 1 << (idx & 31);
	}

	public static int align(int x, int a) {
		return ((x + (a - 1)) / a) * a;
	}

	public static int fetchBits32(byte[] bits, int org, int len) {
		int idx = org >>> 3;
		long wk = bits[idx] & 0xFF;
		for (int i = 1; i < 5; ++i) {
			long b = bits[idx + i] & 0xFF;
			wk |= b << (i << 3);
		}
		wk >>>= org & 7;
		wk &= (1L << len) - 1;
		return (int)wk;
	}

}
