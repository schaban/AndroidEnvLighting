// Author: Sergey Chaban <sergey.chaban@gmail.com>

package xdata;

import java.lang.reflect.Array;
import java.util.Arrays;

public final class Calc {

	private Calc() {}

	public static float inner(float[] a, float[] b) {
		int n = Math.min(a.length, b.length);
		float d = a[0] * b[0];
		for (int i = 1; i < n; ++i) {
			d += a[i] * b[i];
		}
		return d;
	}

	public static float inner(float[] a, float[] b, int n) {
		n = Math.min(Math.min(a.length, b.length), n);
		float d = a[0] * b[0];
		for (int i = 1; i < n; ++i) {
			d += a[i] * b[i];
		}
		return d;
	}

	public static float inner(float[] a, int aoffs, float[] b, int boffs, int n) {
		float d = a[aoffs] * b[boffs];
		for (int i = 1; i < n; ++i) {
			d += a[aoffs + i] * b[boffs + i];
		}
		return d;
	}

	public static float maxAbsElem(float[] v) {
		int n = v.length;
		float res = Math.abs(v[0]);
		for (int i = 1; i < n; ++i) {
			res = Math.max(res, Math.abs(v[i]));
		}
		return res;
	}

	public static float maxAbsElem(float[] v, int offs, int n) {
		float res = Math.abs(v[offs]);
		for (int i = 1; i < n; ++i) {
			res = Math.max(res, Math.abs(v[offs + i]));
		}
		return res;
	}

	public static float magnitude(float[] v) {
		float mag = 0.0f;
		float m = maxAbsElem(v);
		if (m > 0.0f) {
			int n = v.length;
			float r = 1.0f / m;
			float d = sq(v[0] * r);
			for (int i = 1; i < n; ++i) {
				d += sq(v[i] * r);
			}
			mag = sqrtf(d) * m;
		}
		return mag;
	}

	public static float norm(float[] v, int p) {
		float mag = 0.0f;
		float m = 0.0f;
		int n = v.length;
		if (p == 0) {
			for (int i = 0; i < n; ++i) {
				if (v[i] != 0) ++m;
			}
		} else if (p == 1) {
			for (int i = 0; i < n; ++i) {
				m += Math.abs(v[i]);
			}
		} else {
			m = maxAbsElem(v);
		}
		if (p <= 1) {
			mag = m;
		} else if (m > 0.0f) {
			float r = 1.0f / m;
			float d = ipowf(Math.abs(v[0]) * r, p);
			for (int i = 1; i < n; ++i) {
				d += ipowf(Math.abs(v[i]) * r, p);
			}
			mag = powf(d, 1.0f / (float)p) * m;
		}
		return mag;
	}

	public static void normalize(float[] v) {
		float m = maxAbsElem(v);
		if (m > 0) {
			vscl(v, 1.0f / m);;
			vscl(v, 1.0f / sqrtf(inner(v, v)));
		}
	}

	public static void normalize(float[] v, int offs, int n) {
		float m = maxAbsElem(v, offs, n);
		if (m > 0) {
			vscl(v, 1.0f / m, offs, n);
			vscl(v, 1.0f / sqrtf(inner(v, offs, v, offs, n)), offs, n);
		}
	}

	public static float distSq(float[] a, float[] b) {
		int n = Math.min(a.length, b.length);
		float d = 0.0f;
		for (int i = 0; i < n; ++i) {
			d += sq(a[i] - b[i]);
		}
		return d;
	}

	public static float distSq(float[] a, int aoffs, float[] b, int boffs, int n) {
		float d = 0.0f;
		for (int i = 0; i < n; ++i) {
			d += sq(a[aoffs + i] - b[boffs + i]);
		}
		return d;
	}

	public static float lenSq(float[] v, int offs, int n) {
		float d = 0.0f;
		for (int i = 0; i < n; ++i) {
			d += sq(v[offs + i]);
		}
		return d;
	}

	public static void mulMM(float[] dst, float[] src1, float[] src2, int M, int N, int P) {
		for (int i = 0; i < M; ++i) {
			int ra = i * N;
			int rr = i * P;
			float s = src1[ra];
			for (int k = 0; k < P; ++k) {
				dst[rr + k] = src2[k] * s;
			}
		}
		for (int i = 0; i < M; ++i) {
			int ra = i * N;
			int rr = i * P;
			for (int j = 1; j < N; ++j) {
				int rb = j * P;
				float s = src1[ra + j];
				for (int k = 0; k < P; ++k) {
					dst[rr + k] += src2[rb + k] * s;
				}
			}
		}
	}

	public static boolean invGJ(float[] mtx, int N, int[] iwk) {
		int opiv = 0;
		int ocol = N;
		int orow = N + N;
		int ir = 0;
		int ic = 0;
		if (iwk == null) {
			iwk = new int[N*3];
		}
		for (int i = 0; i < N; ++i) {
			iwk[opiv + i] = 0;
		}
		for (int i = 0; i < N; ++i) {
			float amax = 0;
			for (int j = 0; j < N; ++j) {
				if (iwk[opiv + j] != 1) {
					int rj = j * N;
					for (int k = 0; k < N; ++k) {
						if (iwk[opiv + k] == 0) {
							float a = Math.abs(mtx[rj + k]);
							if (a >= amax) {
								amax = a;
								ir = j;
								ic = k;
							}
						}
					}
				}
			}
			++iwk[opiv + ic];
			if (ir != ic) {
				int rr = ir * N;
				int rc = ic * N;
				for (int j = 0; j < N; ++j) {
					float t = mtx[rr + j];
					mtx[rr + j] = mtx[rc + j];
					mtx[rc + j] = t;
				}
			}
			iwk[orow + i] = ir;
			iwk[ocol + i] = ic;
			int rc = ic * N;
			float piv = mtx[rc + ic];
			if (piv == 0) return false; /* singular */
			float ipiv = 1.0f / piv;
			mtx[rc + ic] = 1.0f;
			for (int j = 0; j < N; ++j) {
				mtx[rc + j] *= ipiv;
			}
			for (int j = 0; j < N; ++j) {
				if (j != ic) {
					int rj = j * N;
					float d = mtx[rj + ic];
					mtx[rj + ic] = 0;
					for (int k = 0; k < N; ++k) {
						mtx[rj + k] -= mtx[rc + k] * d;
					}
				}
			}
		}
		for (int i = N; --i >= 0;) {
			ir = iwk[orow + i];
			ic = iwk[ocol + i];
			if (ir != ic) {
				for (int j = 0; j < N; ++j) {
					int rj = j * N;
					float t = mtx[rj + ir];
					mtx[rj + ir] = mtx[rj + ic];
					mtx[rj + ic] = t;
				}
			}
		}
		return true;
	}

	public static void identity(float[] mtx, int N) {
		Arrays.fill(mtx, 0.0f);
		for (int i = 0; i < N; ++i) {
			mtx[(i * N) + i] = 1.0f;
		}
	}

	public static void identity34(float[] mtx, int offs) {
		Arrays.fill(mtx, offs, offs + 3*4, 0.0f);
		mtx[offs] = 1.0f;
		mtx[offs + 4 + 1] = 1.0f;
		mtx[offs + 4*2 + 2] = 1.0f;
	}

	public static void identity34(float[] mtx) {
		identity34(mtx, 0);
	}

	public static void transpose(float[] mtx, int N) {
		for (int i = 0; i < N - 1; ++i) {
			for (int j = i + 1; j < N; ++j) {
				int ij = i*N + j;
				int ji = j*N + i;
				float t = mtx[ij];
				mtx[ij] = mtx[ji];
				mtx[ji] = t;
			}
		}
	}

	public static void transposeM44toM34(float[] dstM34, int dstOffs, float[] srcM44, int srcOffs) {
		dstM34[dstOffs] = srcM44[srcOffs];
		dstM34[dstOffs + 1] = srcM44[srcOffs + 4];
		dstM34[dstOffs + 2] = srcM44[srcOffs + 4*2];
		dstM34[dstOffs + 3] = srcM44[srcOffs + 4*3];

		dstM34[dstOffs + 4] = srcM44[srcOffs + 1];
		dstM34[dstOffs + 4 + 1] = srcM44[srcOffs + 4 + 1];
		dstM34[dstOffs + 4 + 2] = srcM44[srcOffs + 4*2 + 1];
		dstM34[dstOffs + 4 + 3] = srcM44[srcOffs + 4*3 + 1];

		dstM34[dstOffs + 4*2] = srcM44[srcOffs + 2];
		dstM34[dstOffs + 4*2 + 1] = srcM44[srcOffs + 4 + 2];
		dstM34[dstOffs + 4*2 + 2] = srcM44[srcOffs + 4*2 + 2];
		dstM34[dstOffs + 4*2 + 3] = srcM44[srcOffs + 4*3 + 2];
	}

	public static void transpose(float[] mtx, int M, int N, int[] flgWk) {
		if (M == N) {
			transpose(mtx, N);
		} else {
			int flgSize = ((M*N - 1) / 32) + 1;
			int[] flg = flgWk;
			if (flgWk == null || flgWk.length < flgSize) {
				flg = new int[flgSize];
			}
			Arrays.fill(flg, 0);
			int q = M*N - 1;
			for (int i = 1; i < q;) {
				int i0 = i;
				int org = i;
				float t = mtx[org];
				do {
					int i1 = (i*M) % q;
					float t1 = mtx[i1];
					mtx[i1] = t;
					t = t1;
					flg[i/32] |= 1 << (i & 31);
					i = i1;
				} while (i != org);
				for (i = i0 + 1; i < q && (flg[i/32] & (1 << (i & 31))) != 0; ++i) {}
			}
		}
	}

	public static void setRowRX(float[] mtx, int N, float rad) {
		if (N < 3) return;
		float s = sinf(rad);
		float c = cosf(rad);
		for (int i = 1; i < N*3; ++i) {
			mtx[i] = 0.0f;
		}
		mtx[0] = 1.0f;
		mtx[N + 1] = c;
		mtx[N + 2] = s;
		mtx[N*2 + 1] = -s;
		mtx[N*2 + 2] = c;
	}

	public static void setRowRY(float[] mtx, int N, float rad) {
		if (N < 3) return;
		float s = sinf(rad);
		float c = cosf(rad);
		for (int i = 1; i < N*3; ++i) {
			mtx[i] = 0.0f;
		}
		mtx[0] = c;
		mtx[2] = -s;
		mtx[N + 1] = 1.0f;
		mtx[N*2 + 0] = s;
		mtx[N*2 + 2] = c;
	}

	public static void setRowRZ(float[] mtx, int N, float rad) {
		if (N < 3) return;
		float s = sinf(rad);
		float c = cosf(rad);
		for (int i = 1; i < N*3; ++i) {
			mtx[i] = 0.0f;
		}
		mtx[0] = c;
		mtx[1] = s;
		mtx[N + 0] = -s;
		mtx[N + 1] = c;
		mtx[N*2 + 2] = 1.0f;
	}

	public static int vcpy(int dstOffs, float[] dst, int srcOffs, float... src) {
		int slen = src.length;
		int nsrc = slen;
		int dlen = dst.length;
		int ndst = dlen;
		if (srcOffs >= nsrc) return 0;
		if (dstOffs >= ndst) return 0;
		nsrc -= srcOffs;
		ndst -= dstOffs;
		int n = Math.min(nsrc, ndst);
		if (srcOffs + n > slen) n = slen - srcOffs;
		if (dstOffs + n > dlen) n = dlen - dstOffs;
		for (int i = 0; i < n; ++i) {
			dst[dstOffs + i] = src[srcOffs + i];
		}
		return n;
	}

	public static int vcpy(float[] dst, float... src) {
		return vcpy(0, dst, 0, src);
	}

	public static int vcpy(float pad, int dstOffs, float[] dst, int srcOffs, float... src) {
		int n = vcpy(dstOffs, dst, srcOffs, src);
		int rn = dst.length - n;
		for (int i = 0; i < rn; ++i) {
			dst[n + i] = pad;
		}
		return n;
	}

	public static void vfill(float[] dst, float val) {
		Arrays.fill(dst, val);
	}

	public static void vfill(float[] dst, float val, int n) {
		Arrays.fill(dst, 0, n-1, val);
	}

	public static void vclamp(float[] vdst, float[] vsrc, float[] vmin, float[] vmax, int maxOffs) {
		int n = Math.min(vdst.length, Math.min(vsrc.length, Math.min(vmin.length, vmax.length - maxOffs)));
		for (int i = 0; i < n; ++i) {
			vdst[i] = clamp(vsrc[i], vmin[i], vmax[maxOffs + i]);
		}
	}

	public static void vclamp(float[] vdst, float[] vsrc, float[] vmin, float[] vmax) {
		if (vmin == vmax) {
			vclamp(vdst, vsrc, vmin, vmax, vmin.length / 2);
		} else {
			vclamp(vdst, vsrc, vmin, vmax);
		}
	}

	public static void vclamp(float[] vdst, float[] vsrc, float[] vminmax) {
		vclamp(vdst, vsrc, vminmax, vminmax, vminmax.length / 2);
	}

	public static void vsaturate(float[] vdst, int dstOffs, float[] vsrc, int srcOffs, int n) {
		for (int i = 0; i < n; ++i) {
			vdst[dstOffs + i] = saturate(vsrc[srcOffs + i]);
		}
	}

	public static void vsaturate(float[] vdst) {
		vsaturate(vdst, 0, vdst, 0, vdst.length);
	}

	public static void vscl(float[] v, float s) {
		int n = v.length;
		for (int i = 0; i < n; ++i) {
			v[i] *= s;
		}
	}

	public static void vscl(float[] v, float s, int offs, int n) {
		int vlen = v.length;
		if (offs + n > vlen) {
			n = vlen - offs;
		}
		for (int i = 0; i < n; ++i) {
			v[offs + i] *= s;
		}
	}

	public static void vscl(float[] v, float s, int n) {
		n = Math.min(v.length, n);
		for (int i = 0; i < n; ++i) {
			v[i] *= s;
		}
	}

	public static void vadds(float[] vdst, float[] vsrc, float s) {
		int n = Math.min(vdst.length, vsrc.length);
		for (int i = 0; i < n; ++i) {
			vdst[i] += vsrc[i] * s;
		}
	}

	public static void vadds(float[] vdst, float[] vsrc, float s, int n) {
		n = Math.min(Math.min(vdst.length, vsrc.length), n);
		for (int i = 0; i < n; ++i) {
			vdst[i] += vsrc[i] * s;
		}
	}

	public static void vadd(float[] vdst, float[] vsrc) {
		int n = Math.min(vdst.length, vsrc.length);
		for (int i = 0; i < n; ++i) {
			vdst[i] += vsrc[i];
		}
	}

	public static void vadd(float[] vdst, float[] vsrc, int n) {
		n = Math.min(Math.min(vdst.length, vsrc.length), n);
		for (int i = 0; i < n; ++i) {
			vdst[i] += vsrc[i];
		}
	}

	public static void vadd(float[] vdst, float[] vsrc1, float[] vsrc2) {
		int n = Math.min(Math.min(vdst.length, vsrc1.length), vsrc2.length);
		for (int i = 0; i < n; ++i) {
			vdst[i] = vsrc1[i] + vsrc2[i];
		}
	}

	public static void vadd(float[] vdst, int dstOffs, float[] vsrc, int srcOffs) {
		int n = Math.min(vdst.length - dstOffs, vsrc.length - srcOffs);
		for (int i = 0; i < n; ++i) {
			vdst[dstOffs + i] += vsrc[srcOffs + i];
		}
	}

	public static void vadd(float[] vdst, int dstOffs, float[] vsrc, int srcOffs, int n) {
		for (int i = 0; i < n; ++i) {
			vdst[dstOffs + i] += vsrc[srcOffs + i];
		}
	}

	public static void vadd(float[] vdst, int offs, float[] vsrc1, int offs1, float[] vsrc2, int offs2, int n) {
		for (int i = 0; i < n; ++i) {
			vdst[offs + i] = vsrc1[offs1 + i] + vsrc2[offs2 + i];
		}
	}

	public static void vsub(float[] vdst, float[] vsrc) {
		int n = Math.min(vdst.length, vsrc.length);
		for (int i = 0; i < n; ++i) {
			vdst[i] -= vsrc[i];
		}
	}

	public static void vsub(float[] vdst, float[] vsrc, int n) {
		n = Math.min(Math.min(vdst.length, vsrc.length), n);
		for (int i = 0; i < n; ++i) {
			vdst[i] -= vsrc[i];
		}
	}

	public static void vsub(float[] vdst, float[] vsrc1, float[] vsrc2) {
		int n = Math.min(Math.min(vdst.length, vsrc1.length), vsrc2.length);
		for (int i = 0; i < n; ++i) {
			vdst[i] = vsrc1[i] - vsrc2[i];
		}
	}

	public static void vsub(float[] vdst, int dstOffs, float[] vsrc, int srcOffs) {
		int n = Math.min(vdst.length - dstOffs, vsrc.length - srcOffs);
		for (int i = 0; i < n; ++i) {
			vdst[dstOffs + i] -= vsrc[srcOffs + i];
		}
	}

	public static void vsub(float[] vdst, int dstOffs, float[] vsrc, int srcOffs, int n) {
		for (int i = 0; i < n; ++i) {
			vdst[dstOffs + i] -= vsrc[srcOffs + i];
		}
	}

	public static void vsub(float[] vdst, int offs, float[] vsrc1, int offs1, float[] vsrc2, int offs2, int n) {
		for (int i = 0; i < n; ++i) {
			vdst[offs + i] = vsrc1[offs1 + i] - vsrc2[offs2 + i];
		}
	}

	public static void vmul(float[] vdst, float[] vsrc) {
		int n = Math.min(vdst.length, vsrc.length);
		for (int i = 0; i < n; ++i) {
			vdst[i] *= vsrc[i];
		}
	}

	public static void vmul(float[] vdst, float[] vsrc, int n) {
		n = Math.min(Math.min(vdst.length, vsrc.length), n);
		for (int i = 0; i < n; ++i) {
			vdst[i] *= vsrc[i];
		}
	}

	public static void vmul(float[] vdst, float[] vsrc1, float[] vsrc2) {
		int n = Math.min(Math.min(vdst.length, vsrc1.length), vsrc2.length);
		for (int i = 0; i < n; ++i) {
			vdst[i] = vsrc1[i] * vsrc2[i];
		}
	}

	public static void vmul(float[] vdst, int dstOffs, float[] vsrc, int srcOffs) {
		int n = Math.min(vdst.length - dstOffs, vsrc.length - srcOffs);
		for (int i = 0; i < n; ++i) {
			vdst[dstOffs + i] *= vsrc[srcOffs + i];
		}
	}

	public static void vmul(float[] vdst, int offs, float[] vsrc1, int offs1, float[] vsrc2, int offs2, int n) {
		for (int i = 0; i < n; ++i) {
			vdst[offs + i] = vsrc1[offs1 + i] * vsrc2[offs2 + i];
		}
	}

	public static void vdiv(float[] vdst, float[] vsrc) {
		int n = Math.min(vdst.length, vsrc.length);
		for (int i = 0; i < n; ++i) {
			vdst[i] /= vsrc[i];
		}
	}

	public static void vdiv(float[] vdst, float[] vsrc, int n) {
		n = Math.min(Math.min(vdst.length, vsrc.length), n);
		for (int i = 0; i < n; ++i) {
			vdst[i] /= vsrc[i];
		}
	}

	public static void vdiv(float[] vdst, float[] vsrc1, float[] vsrc2) {
		int n = Math.min(Math.min(vdst.length, vsrc1.length), vsrc2.length);
		for (int i = 0; i < n; ++i) {
			vdst[i] = vsrc1[i] / vsrc2[i];
		}
	}

	public static void vdiv(float[] vdst, int dstOffs, float[] vsrc, int srcOffs) {
		int n = Math.min(vdst.length - dstOffs, vsrc.length - srcOffs);
		for (int i = 0; i < n; ++i) {
			vdst[dstOffs + i] /= vsrc[srcOffs + i];
		}
	}

	public static void vdiv(float[] vdst, int offs, float[] vsrc1, int offs1, float[] vsrc2, int offs2, int n) {
		for (int i = 0; i < n; ++i) {
			vdst[offs + i] = vsrc1[offs1 + i] / vsrc2[offs2 + i];
		}
	}

	public static void vlerp(float[] vdst, int offs, float[] vsrc1, int offs1, float[] vsrc2, int offs2, int n, float t) {
		for (int i = 0; i < n; ++i) {
			vdst[offs + i] = lerp(vsrc1[offs1 + i], vsrc2[offs2 + i], t);
		}
	}

	public static void cross(float[] v0, float[] v1, float[] v2) {
		cross(v0, v1, v2[0], v2[1], v2[2]);
	}

	public static void cross(float[] v0, float[] v1, float v20, float v21, float v22) {
		float v10 = v1[0];
		float v11 = v1[1];
		float v12 = v1[2];
		v0[0] = v11*v22 - v12*v21;
		v0[1] = v12*v20 - v10*v22;
		v0[2] = v10*v21 - v11*v20;
	}

	public static void cross(float[] dst, int offs, float v20, float v21, float v22) {
		float v10 = dst[offs];
		float v11 = dst[offs + 1];
		float v12 = dst[offs + 2];
		dst[offs] = v11*v22 - v12*v21;
		dst[offs + 1] = v12*v20 - v10*v22;
		dst[offs + 2] = v10*v21 - v11*v20;
	}

	public static void cross(float[] v0, int offs0, float[] v1, int offs1, float[] v2, int offs2) {
		float v10 = v1[offs1];
		float v11 = v1[offs1 + 1];
		float v12 = v1[offs1 + 2];
		float v20 = v2[offs2];
		float v21 = v2[offs2 + 1];
		float v22 = v2[offs2 + 2];
		v0[offs0] = v11*v22 - v12*v21;
		v0[offs0 + 1] = v12*v20 - v10*v22;
		v0[offs0 + 2] = v10*v21 - v11*v20;
	}

	public static float triple(float[] v0, float[] v1, float[] v2) {
		final float[] v = new float[3];
		cross(v, v0, v1);
		return inner(v, v2);
	}

	public static boolean isOdd(int n) {
		return (n & 1) != 0;
	}

	public static boolean isEven(int n) {
		return (n & 1) == 0;
	}

	public static boolean isPow2(int val) {
		return (val & (val - 1)) == 0;
	}

	public static int log2i(int val) {
		if (val <= 0) return 0;
		return 31 - Integer.numberOfLeadingZeros(val);
	}

	public static int clamp(int x, int lo, int hi) {
		return Math.max(Math.min(x, hi), lo);
	}

	public static float clamp(float x, float lo, float hi) {
		return Math.max(Math.min(x, hi), lo);
	}

	public static float saturate(float x) {
		return clamp(x, 0.0f, 1.0f);
	}

	public static float div0(float x, float y) {
		return y != 0.0f ? x / y : 0.0f;
	}

	public static float rcp0(float x) {
		return div0(1.0f, x);
	}

	public static float sq(float x) {
		return x*x;
	}

	public static float cb(float x) {
		return x*x*x;
	}

	public static float ipowf(float x, int n) {
		float wx = x;
		int wn = n;
		if (n < 0) {
			wx = rcp0(x);
			wn = -n;
		}
		float res = 1.0f;
		do {
			if (isOdd(wn)) res *= wx;
			wx *= wx;
			wn >>>= 1;
		} while (wn > 0);
		return res;
	}

	public static float powf(float x, float e) {
		return (float)Math.pow(x, e);
	}

	public static float expf(float x) {
		return (float)Math.exp(x);
	}

	public static float radians(float deg) {
		return (float)Math.toRadians(deg);
	}

	public static float degrees(float rad) {
		return (float)Math.toDegrees(rad);
	}

	public static float sqrtf(float x) {
		return (float)Math.sqrt(x);
	}

	public static float sinf(float a) {
		return (float)Math.sin(a);
	}

	public static float cosf(float a) {
		return (float)Math.cos(a);
	}

	public static float tanf(float a) {
		return (float)Math.tan(a);
	}

	public static float asinf(float a) {
		return (float)Math.asin(a);
	}

	public static float acosf(float a) {
		return (float)Math.acos(a);
	}

	public static float atan2f(float y, float x) {
		return (float)Math.atan2(y, x);
	}

	public static float hypotf(float x, float y) {
		float m = Math.max(Math.abs(x), Math.abs(y));
		float im = rcp0(m);
		return sqrtf(sq(x*im) + sq(y*im)) * m;
	}

	public static float sinc(float x) {
		if (Math.abs(x) < 1.0e-4f) {
			return 1.0f;
		}
		return (sinf(x) / x);
	}

	public static float lerp(float a, float b, float t) {
		return a + (b - a)*t;
	}

	public static float ease(float t, float e) {
		float x = 0.5f - 0.5f*cosf(t * (float)Math.PI);
		if (e != 1.0f) {
			x = powf(x, e);
		}
		return x;
	}

	public static float ease(float t) {
		return ease(t, 1.0f);
	}

	public static float hermite(float p0, float m0, float p1, float m1, float t) {
		float tt = t*t;
		float ttt = tt*t;
		float tt3 = tt*3.0f;
		float tt2 = tt + tt;
		float ttt2 = tt2 * t;
		float h00 = ttt2 - tt3 + 1.0f;
		float h10 = ttt - tt2 + t;
		float h01 = -ttt2 + tt3;
		float h11 = ttt - tt;
		return (h00*p0 + h10*m0 + h01*p1 + h11*m1);
	}

	public static float fit(float val, float oldMin, float oldMax, float newMin, float newMax) {
		float rel = div0(val - oldMin, oldMax - oldMin);
		rel = saturate(rel);
		return lerp(newMin, newMax, rel);
	}

	public static float calcFOVY(float focal, float aperture, float aspect) {
		float zoom = ((2.0f * focal) / aperture) * aspect;
		float fovy = 2.0f * atan2f(1.0f, zoom);
		return fovy;
	}

	public static void encodeOcta(float[] dst, int offs, float x, float y, float z) {
		float ax = Math.abs(x);
		float ay = Math.abs(y);
		float az = Math.abs(z);
		float d = rcp0(ax + ay + az);
		float ox = x * d;
		float oy = y * d;
		if (z < 0.0f) {
			float tx = (1.0f - Math.abs(oy)) * (ox < 0.0f ? -1.0f : 1.0f);
			float ty = (1.0f - Math.abs(ox)) * (oy < 0.0f ? -1.0f : 1.0f);
			ox = tx;
			oy = ty;
		}
		dst[offs + 0] = ox;
		dst[offs + 1] = oy;
	}

	public static void encodeOcta(float[] oct, float x, float y, float z) {
		encodeOcta(oct, 0, x, y, z);
	}

	public static void encodeOcta(float[] oct, float... xyz) {
		encodeOcta(oct, 0, xyz[0], xyz[1], xyz[2]);
	}

	public static void decodeOcta(float[] vec, float ox, float oy) {
		float ax = Math.abs(ox);
		float ay = Math.abs(oy);
		float z = 1.0f - ax - ay;
		float x = ox;
		float y = oy;
		if (z < 0.0f) {
			x = (1.0f - ay) * (ox < 0.0f ? -1.0f : 1.0f);
			y = (1.0f - ax) * (oy < 0.0f ? -1.0f : 1.0f);
		}
		vec[0] = x;
		vec[1] = y;
		vec[2] = z;
		normalize(vec);
	}

	public static float limitPI(float rad) {
		float pi = (float)Math.PI;
		float pi2 = pi * 2.0f;
		rad = rad % pi2;
		if (Math.abs(rad) > pi) {
			if (rad < 0.0f) {
				rad = pi2 + rad;
			} else {
				rad = rad - pi2;
			}
		}
		return rad;
	}

	public static void radiansFromAxes(
			float[] r, int roffs,
			float x0, float y0, float z0,
			float x1, float y1, float z1,
			float x2, float y2, float z2
	) {
		r[roffs + 0] = atan2f(z1, z2);
		r[roffs + 1] = atan2f(-z0, sqrtf(sq(x0) + sq(y0)));
		float s = sinf(r[roffs + 0]);
		float c = cosf(r[roffs + 0]);
		r[roffs + 2] = atan2f(s*x2 - c*x1, c*y1 - s*y2);
	}

	public static void radiansFromQuat(float[] r, int roffs, float qx, float qy, float qz, float qw, RotOrd ord) {
		final float eps = 1.0e-6f;
		int axisMask = Math.abs(qx) < eps ? 1 : 0;
		axisMask |= Math.abs(qy) < eps ? 2 : 0;
		axisMask |= Math.abs(qz) < eps ? 4 : 0;
		axisMask |= Math.abs(qw) < eps ? 8 : 0;
		boolean singleAxis = false;
		qw = clamp(qw, -1.0f, 1.0f);
		float rx = 0.0f;
		float ry = 0.0f;
		float rz = 0.0f;
		switch (axisMask) {
			case 6: /* 0110 -> X */
				rx = acosf(qw) * 2.0f;
				if (qx < 0.0f) rx = -rx;
				rx = limitPI(rx);
				singleAxis = true;
				break;
			case 5: /* 0101 -> Y */
				ry = acosf(qw) * 2.0f;
				if (qy < 0.0f) ry = -ry;
				ry = limitPI(ry);
				singleAxis = true;
				break;
			case 3: /* 0011 -> Z */
				rz = acosf(qw) * 2.0f;
				if (qz < 0.0f) rz = -rz;
				rz = limitPI(rz);
				singleAxis = true;
				break;
			case 7: /* 0111 -> identity */
				singleAxis = true;
				break;
		}
		if (singleAxis) {
			r[roffs + 0] = rx;
			r[roffs + 1] = ry;
			r[roffs + 2] = rz;
			return;
		}
		float x0, y0, z0;
		float x1, y1, z1;
		float x2, y2, z2;
		int ridx = 0;
		boolean neg;
		switch (ord) {
			default:
			case XYZ:
				// X
				x0 = 1.0f - (2.0f*qy*qy) - (2.0f*qz*qz);
				y0 = (2.0f*qx*qy) + (2.0f*qw*qz);
				z0 = (2.0f*qx*qz) - (2.0f*qw*qy);
				// Y
				x1 = (2.0f*qx*qy) - (2.0f*qw*qz);
				y1 = 1.0f - (2.0f*qx*qx) - (2.0f*qz*qz);
				z1 = (2.0f*qy*qz) + (2.0f*qw*qx);
				// Z
				x2 = (2.0f*qx*qz) + (2.0f*qw*qy);
				y2 = (2.0f*qy*qz) - (2.0f*qw*qx);
				z2 = 1.0f - (2.0f*qx*qx) - (2.0f*qy*qy);
				// +
				neg = false;
				break;
			case XZY:
				ridx = 0x021;
				// X
				x0 = 1.0f - (2.0f*qy*qy) - (2.0f*qz*qz);
				y0 = (2.0f*qx*qy) + (2.0f*qw*qz);
				z0 = (2.0f*qx*qz) - (2.0f*qw*qy);
				// Z
				x1 = (2.0f*qx*qz) + (2.0f*qw*qy);
				y1 = (2.0f*qy*qz) - (2.0f*qw*qx);
				z1 = 1.0f - (2.0f*qx*qx) - (2.0f*qy*qy);
				// Y
				x2 = (2.0f*qx*qy) - (2.0f*qw*qz);
				y2 = 1.0f - (2.0f*qx*qx) - (2.0f*qz*qz);
				z2 = (2.0f*qy*qz) + (2.0f*qw*qx);
				// -
				neg = true;
				break;
			case YXZ:
				ridx = 0x102;
				// Y
				x0 = (2.0f*qx*qy) - (2.0f*qw*qz);
				y0 = 1.0f - (2.0f*qx*qx) - (2.0f*qz*qz);
				z0 = (2.0f*qy*qz) + (2.0f*qw*qx);
				// X
				x1 = 1.0f - (2.0f*qy*qy) - (2.0f*qz*qz);
				y1 = (2.0f*qx*qy) + (2.0f*qw*qz);
				z1 = (2.0f*qx*qz) - (2.0f*qw*qy);
				// Z
				x2 = (2.0f*qx*qz) + (2.0f*qw*qy);
				y2 = (2.0f*qy*qz) - (2.0f*qw*qx);
				z2 = 1.0f - (2.0f*qx*qx) - (2.0f*qy*qy);
				// -
				neg = true;
				break;
			case YZX:
				ridx = 0x120;
				// Y
				x0 = (2.0f*qx*qy) - (2.0f*qw*qz);
				y0 = 1.0f - (2.0f*qx*qx) - (2.0f*qz*qz);
				z0 = (2.0f*qy*qz) + (2.0f*qw*qx);
				// Z
				x1 = (2.0f*qx*qz) + (2.0f*qw*qy);
				y1 = (2.0f*qy*qz) - (2.0f*qw*qx);
				z1 = 1.0f - (2.0f*qx*qx) - (2.0f*qy*qy);
				// X
				x2 = 1.0f - (2.0f*qy*qy) - (2.0f*qz*qz);
				y2 = (2.0f*qx*qy) + (2.0f*qw*qz);
				z2 = (2.0f*qx*qz) - (2.0f*qw*qy);
				// +
				neg = false;
				break;
			case ZXY:
				ridx = 0x201;
				// Z
				x0 = (2.0f*qx*qz) + (2.0f*qw*qy);
				y0 = (2.0f*qy*qz) - (2.0f*qw*qx);
				z0 = 1.0f - (2.0f*qx*qx) - (2.0f*qy*qy);
				// X
				x1 = 1.0f - (2.0f*qy*qy) - (2.0f*qz*qz);
				y1 = (2.0f*qx*qy) + (2.0f*qw*qz);
				z1 = (2.0f*qx*qz) - (2.0f*qw*qy);
				// Y
				x2 = (2.0f*qx*qy) - (2.0f*qw*qz);
				y2 = 1.0f - (2.0f*qx*qx) - (2.0f*qz*qz);
				z2 = (2.0f*qy*qz) + (2.0f*qw*qx);
				// +
				neg = false;
				break;
			case ZYX:
				ridx = 0x210;
				// Z
				x0 = (2.0f*qx*qz) + (2.0f*qw*qy);
				y0 = (2.0f*qy*qz) - (2.0f*qw*qx);
				z0 = 1.0f - (2.0f*qx*qx) - (2.0f*qy*qy);
				// Y
				x1 = (2.0f*qx*qy) - (2.0f*qw*qz);
				y1 = 1.0f - (2.0f*qx*qx) - (2.0f*qz*qz);
				z1 = (2.0f*qy*qz) + (2.0f*qw*qx);
				// X
				x2 = 1.0f - (2.0f*qy*qy) - (2.0f*qz*qz);
				y2 = (2.0f*qx*qy) + (2.0f*qw*qz);
				z2 = (2.0f*qx*qz) - (2.0f*qw*qy);
				// -
				neg = true;
				break;
		}
		radiansFromAxes(r, roffs, x0, y0, z0, x1, y1, z1, x2, y2, z2);
		if (ridx != 0) {
			float r0 = r[roffs];
			float r1 = r[roffs + 1];
			float r2 = r[roffs + 2];
			r[roffs + ((ridx >> 8) & 0xF)] = r0;
			r[roffs + ((ridx >> 4) & 0xF)] = r1;
			r[roffs + (ridx & 0xF)] = r2;
		}
		if (neg) {
			for (int i = 0; i < 3; ++i) {
				r[roffs + i] = -r[roffs + i];
			}
		}
		for (int i = 0; i < 3; ++i) {
			r[roffs + i] = limitPI(r[roffs + i]);
		}
	}

	public static float luma(float r, float g, float b) {
		return r*0.299f + g*0.587f + b*0.114f;
	}

	public static float luminance(float r, float g, float b) {
		return r*0.212671f + g*0.71516f + b*0.072169f;
	}

	public static float vecLen(float x, float y, float z) {
		return sqrtf(x*x + y*y + z*z);
	}

	public static float vecInvLen(float x, float y, float z) {
		return rcp0(vecLen(x, y, z));
	}

	public static void triNormalCW(
	                   float[] nrm, int nrmOffs,
	                   float[] v0, int v0Offs,
	                   float[] v1, int v1Offs,
	                   float[] v2, int v2Offs
	) {
		float x1 = v1[v1Offs];
		float y1 = v1[v1Offs + 1];
		float z1 = v1[v1Offs + 2];
		float v10 = v0[v0Offs] - x1;
		float v11 = v0[v0Offs + 1] - y1;
		float v12 = v0[v0Offs + 2] - z1;
		float v20 = v2[v2Offs] - x1;
		float v21 = v2[v2Offs + 1] - y1;
		float v22 = v2[v2Offs + 2] - z1;
		nrm[nrmOffs] = v11*v22 - v12*v21;
		nrm[nrmOffs + 1] = v12*v20 - v10*v22;
		nrm[nrmOffs + 2] = v10*v21 - v11*v20;
		normalize(nrm, nrmOffs, 3);
	}

	public static void triNormalCW(
			float[] nrm, int nrmOffs,
			float[] vtx, int v0Offs, int v1Offs, int v2Offs
	) {
		triNormalCW(nrm, nrmOffs, vtx, v0Offs, vtx, v1Offs, vtx, v2Offs);
	}

	public static void triNormalCCW(
	                   float[] nrm, int nrmOffs,
	                   float[] v0, int v0Offs,
	                   float[] v1, int v1Offs,
	                   float[] v2, int v2Offs
	) {
		float x0 = v0[v0Offs];
		float y0 = v0[v0Offs + 1];
		float z0 = v0[v0Offs + 2];
		float v10 = v1[v1Offs] - x0;
		float v11 = v1[v1Offs + 1] - y0;
		float v12 = v1[v1Offs + 2] - z0;
		float v20 = v2[v2Offs] - x0;
		float v21 = v2[v2Offs + 1] - y0;
		float v22 = v2[v2Offs + 2] - z0;
		nrm[nrmOffs] = v11*v22 - v12*v21;
		nrm[nrmOffs + 1] = v12*v20 - v10*v22;
		nrm[nrmOffs + 2] = v10*v21 - v11*v20;
		normalize(nrm, nrmOffs, 3);
	}

	public static void triNormalCCW(
			float[] nrm, int nrmOffs,
			float[] vtx, int v0Offs, int v1Offs, int v2Offs
	) {
		triNormalCCW(nrm, nrmOffs, vtx, v0Offs, vtx, v1Offs, vtx, v2Offs);
	}

	public static void planeFromPosNrm(
	                   float[] planeABCD, int planeOffs,
	                   float[] pos, int posOffs,
	                   float[] nrm, int nrmOffs
	) {
		for (int i = 0; i < 3; ++i) {
			planeABCD[planeOffs + i] = nrm[nrmOffs + i];
		}
		planeABCD[planeOffs + 3] = inner(pos, posOffs, nrm, nrmOffs, 3);
	}

	public static float planeSignedDist(
			float x, float y, float z,
			float plnA, float plnB, float plnC, float plnD
	) {
		return x*plnA + y*plnB + z*plnC - plnD;
	}

	public static float planeSignedDist(float[] pos, int posOffs, float[] pln, int plnOffs) {
		return planeSignedDist(
			pos[posOffs], pos[posOffs + 1], pos[posOffs + 2],
			pln[plnOffs], pln[plnOffs + 1], pln[plnOffs + 2], pln[plnOffs + 3]
		);
	}

	public static void xformAABB(
	                   float[] minmaxDst, int dstOffs,
	                   float[] minmaxSrc, int srcOffs,
	                   float[] mtx, int mtxOffs
	) {
		float ominx = minmaxSrc[srcOffs + 0];
		float ominy = minmaxSrc[srcOffs + 1];
		float ominz = minmaxSrc[srcOffs + 2];
		float omaxx = minmaxSrc[srcOffs + 3];
		float omaxy = minmaxSrc[srcOffs + 4];
		float omaxz = minmaxSrc[srcOffs + 5];
		float nminx = mtx[mtxOffs + 3*4 + 0];
		float nminy = mtx[mtxOffs + 3*4 + 1];
		float nminz = mtx[mtxOffs + 3*4 + 2];
		float nmaxx = nminx;
		float nmaxy = nminy;
		float nmaxz = nminz;
		for (int i = 0; i < 3; ++i) {
			float ax = mtx[mtxOffs + i*4 + 0];
			float ay = mtx[mtxOffs + i*4 + 1];
			float az = mtx[mtxOffs + i*4 + 2];
			float se = i == 0 ? ominx : i == 1 ? ominy : ominz;
			float ex = ax * se;
			float ey = ay * se;
			float ez = az * se;
			float sf = i == 0 ? omaxx : i == 1 ? omaxy : omaxz;
			float fx = ax * sf;
			float fy = ay * sf;
			float fz = az * sf;
			nminx += Math.min(ex, fx);
			nminy += Math.min(ey, fy);
			nminz += Math.min(ez, fz);
			nmaxx += Math.max(ex, fx);
			nmaxy += Math.max(ey, fy);
			nmaxz += Math.max(ez, fz);
		}
		minmaxDst[dstOffs + 0] = nminx;
		minmaxDst[dstOffs + 1] = nminy;
		minmaxDst[dstOffs + 2] = nminz;
		minmaxDst[dstOffs + 3] = nmaxx;
		minmaxDst[dstOffs + 4] = nmaxy;
		minmaxDst[dstOffs + 5] = nmaxz;
	}

	public static void xform3x4Mul(float[] dst, int dstOffs, float[] src1, int src1Offs, float[] src2, int src2Offs) {
		float a00 = src1[src1Offs];
		float a01 = src1[src1Offs + 4];
		float a02 = src1[src1Offs + 4*2];
		float a10 = src1[src1Offs + 1];
		float a11 = src1[src1Offs + 4 + 1];
		float a12 = src1[src1Offs + 4*2 + 1];
		float a20 = src1[src1Offs + 2];
		float a21 = src1[src1Offs + 4 + 2];
		float a22 = src1[src1Offs + 4*2 + 2];
		float a30 = src1[src1Offs + 3];
		float a31 = src1[src1Offs + 4 + 3];
		float a32 = src1[src1Offs + 4*2 + 3];

		float b00 = src2[src2Offs];
		float b01 = src2[src2Offs + 4];
		float b02 = src2[src2Offs + 4*2];
		float b10 = src2[src2Offs + 1];
		float b11 = src2[src2Offs + 4 + 1];
		float b12 = src2[src2Offs + 4*2 + 1];
		float b20 = src2[src2Offs + 2];
		float b21 = src2[src2Offs + 4 + 2];
		float b22 = src2[src2Offs + 4*2 + 2];
		float b30 = src2[src2Offs + 3];
		float b31 = src2[src2Offs + 4 + 3];
		float b32 = src2[src2Offs + 4*2 + 3];

		float d00 = a00*b00 + a01*b10 + a02*b20;
		float d01 = a00*b01 + a01*b11 + a02*b21;
		float d02 = a00*b02 + a01*b12 + a02*b22;

		float d10 = a10*b00 + a11*b10 + a12*b20;
		float d11 = a10*b01 + a11*b11 + a12*b21;
		float d12 = a10*b02 + a11*b12 + a12*b22;

		float d20 = a20*b00 + a21*b10 + a22*b20;
		float d21 = a20*b01 + a21*b11 + a22*b21;
		float d22 = a20*b02 + a21*b12 + a22*b22;

		float d30 = a30*b00 + a31*b10 + a32*b20 + b30;
		float d31 = a30*b01 + a31*b11 + a32*b21 + b31;
		float d32 = a30*b02 + a31*b12 + a32*b22 + b32;

		dst[dstOffs] = d00;
		dst[dstOffs + 4] = d01;
		dst[dstOffs + 4*2] = d02;

		dst[dstOffs + 1] = d10;
		dst[dstOffs + 4 + 1] = d11;
		dst[dstOffs + 4*2 + 1] = d12;

		dst[dstOffs + 2] = d20;
		dst[dstOffs + 4 + 2] = d21;
		dst[dstOffs + 4*2 + 2] = d22;

		dst[dstOffs + 3] = d30;
		dst[dstOffs + 4 + 3] = d31;
		dst[dstOffs + 4*2 + 3] = d32;
	}

	public static void xform3x4CalcVec(float[] dst, int dstOffs, float[] xform, int xformOffs, float x, float y, float z) {
		dst[dstOffs] = x*xform[xformOffs] + y*xform[xformOffs + 1] + z*xform[xformOffs + 2];
		dst[dstOffs + 1] = x*xform[xformOffs + 4] + y*xform[xformOffs + 4 + 1] + z*xform[xformOffs + 4 + 2];
		dst[dstOffs + 2] = x*xform[xformOffs + 4*2] + y*xform[xformOffs + 4*2 + 1] + z*xform[xformOffs + 4*2 + 2];
	}

	public static void xform3x4CalcPnt(float[] dst, int dstOffs, float[] xform, int xformOffs, float x, float y, float z) {
		dst[dstOffs] = x*xform[xformOffs] + y*xform[xformOffs + 1] + z*xform[xformOffs + 2] + xform[xformOffs + 3];
		dst[dstOffs + 1] = x*xform[xformOffs + 4] + y*xform[xformOffs + 4 + 1] + z*xform[xformOffs + 4 + 2] + xform[xformOffs + 4 + 3];
		dst[dstOffs + 2] = x*xform[xformOffs + 4*2] + y*xform[xformOffs + 4*2 + 1] + z*xform[xformOffs + 4*2 + 2] + xform[xformOffs + 4*2 + 3];
	}

	public static void xform3x4RadiansXYZ(float[] dst, int dstOffs, float rx, float ry, float rz) {
		float sx = sinf(rx);
		float cx = cosf(rx);
		float sy = sinf(ry);
		float cy = cosf(ry);

		float x00 = 1.0f; float x01 = 0.0f; float x02 = 0.0f;
		float x10 = 0.0f; float x11 =   cx; float x12 =   sx;
		float x20 = 0.0f; float x21 =  -sx; float x22 =   cx;

		float y00 =   cy; float y01 = 0.0f; float y02 =  -sy;
		float y10 = 0.0f; float y11 = 1.0f; float y12 = 0.0f;
		float y20 =   sy; float y21 = 0.0f; float y22 =   cy;

		float xy00 = x00*y00 /* + x01*y10 + x02*y20 */;
		//float xy01 = x00*y01 + x01*y11 + x02*y21;
		float xy02 = x00*y02 /* + x01*y12 + x02*y22 */;

		float xy10 = /* x10*y00 + x11*y10 + */ x12*y20;
		float xy11 = /* x10*y01 + */ x11*y11 /* + x12*y21 */;
		float xy12 = /* x10*y02 + x11*y12 + */ x12*y22;

		float xy20 = /* x20*y00 + x21*y10 + */ x22*y20;
		float xy21 = /* x20*y01 + */ x21*y11 /* + x22*y21 */;
		float xy22 = /* x20*y02 + x21*y12 + */ x22*y22;

		float sz = sinf(rz);
		float cz = cosf(rz);
		float z00 =   cz; float z01 =  sz; float z02 =  0.0f;
		float z10 =  -sz; float z11 =  cz; float z12 =  0.0f;
		float z20 = 0.0f; float z21 = 0.0f; float z22 = 1.0f;

		float d00 = xy00*z00 /* + xy01*z10 + xy02*z20 */;
		float d01 = xy00*z01 /* + xy01*z11 + xy02*z21 */;
		float d02 = /* xy00*z02 + xy01*z12 + */ xy02*z22;

		float d10 = xy10*z00 + xy11*z10 /* + xy12*z20 */;
		float d11 = xy10*z01 + xy11*z11 /* + xy12*z21 */;
		float d12 = /* xy10*z02 + xy11*z12 + */ xy12*z22;

		float d20 = xy20*z00 + xy21*z10 /* + xy22*z20 */;
		float d21 = xy20*z01 + xy21*z11 /* + xy22*z21 */;
		float d22 = /* xy20*z02 + xy21*z12 + */ xy22*z22;

		dst[dstOffs] = d00;
		dst[dstOffs + 4] = d01;
		dst[dstOffs + 4*2] = d02;

		dst[dstOffs + 1] = d10;
		dst[dstOffs + 4 + 1] = d11;
		dst[dstOffs + 4*2 + 1] = d12;

		dst[dstOffs + 2] = d20;
		dst[dstOffs + 4 + 2] = d21;
		dst[dstOffs + 4*2 + 2] = d22;
	}

	public static void xform3x4DegreesXYZ(float[] dst, int dstOffs, float dx, float dy, float dz) {
		xform3x4RadiansXYZ(dst, dstOffs, radians(dx), radians(dy), radians(dz));
	}

	public static void xform3x4Pos(float[] dst, int dstOffs, float x, float y, float z) {
		dst[dstOffs + 3] = x;
		dst[dstOffs + 4 + 3] = y;
		dst[dstOffs + 4*2 + 3] = z;
	}

	public static float xform3x4GetPosX(float[] xform, int xformOffs) {
		return xform[xformOffs + 3];
	}

	public static float xform3x4GetPosY(float[] xform, int xformOffs) {
		return xform[xformOffs + 4 + 3];
	}

	public static float xform3x4GetPosZ(float[] xform, int xformOffs) {
		return xform[xformOffs + 4*2 + 3];
	}

	public static void xform3x4GetPos(float[] pos, int posOffs, float[] xform, int xformOffs) {
		pos[posOffs] = xform[xformOffs + 3];
		pos[posOffs + 1] = xform[xformOffs + 4 + 3];
		pos[posOffs + 2] = xform[xformOffs + 4*2 + 3];
	}

	public static void xform3x4SetAxis(float[] xform, int offs, int axis, float x, float y, float z) {
		xform[offs + axis] = x;
		xform[offs + 4 + axis] = y;
		xform[offs + 4*2 + axis] = z;
	}

	public static void xform3x4InterpolateAxes(float[] dst, int dstOffs,
	                   float[] src1, int src1Offs, float[] src2, int src2Offs, float t
	) {
		float x0 = lerp(src1[src1Offs], src2[src2Offs], t);
		float x1 = lerp(src1[src1Offs + 4], src2[src2Offs + 4], t);
		float x2 = lerp(src1[src1Offs + 4*2], src2[src2Offs + 4*2], t);

		float z0 = lerp(src1[src1Offs + 2], src2[src2Offs + 2], t);
		float z1 = lerp(src1[src1Offs + 4 + 2], src2[src2Offs + 4 + 2], t);
		float z2 = lerp(src1[src1Offs + 4*2 + 2], src2[src2Offs + 4*2 + 2], t);

		float s = vecInvLen(x0, x1, x2);
		dst[dstOffs] = x0 * s;
		dst[dstOffs + 4] = x1 * s;
		dst[dstOffs + 4*2] = x2 * s;

		float y0 = z1*x2 - z2*x1;
		float y1 = z2*x0 - z0*x2;
		float y2 = z0*x1 - z1*x0;
		s = vecInvLen(y0, y1, y2);
		dst[dstOffs + 1] = y0 * s;
		dst[dstOffs + 4 + 1] = y1 * s;
		dst[dstOffs + 4*2 + 1] = y2 * s;

		z0 = x1*y2 - x2*y1;
		z1 = x2*y0 - x0*y2;
		z2 = x0*y1 - x1*y0;
		s = vecInvLen(z0, z1, z2);
		dst[dstOffs + 2] = z0 * s;
		dst[dstOffs + 4 + 2] = z1 * s;
		dst[dstOffs + 4*2 + 2] = z2 * s;
	}

	public static void xform3x4InterpolatePos(float[] dst, int dstOffs,
											   float[] src1, int src1Offs, float[] src2, int src2Offs, float t
	) {
		dst[dstOffs + 3] = lerp(src1[src1Offs + 3], src2[src2Offs + 3], t);
		dst[dstOffs + 4 + 3] = lerp(src1[src1Offs + 4 + 3], src2[src2Offs + 4 + 3], t);
		dst[dstOffs + 4*2 + 3] = lerp(src1[src1Offs + 4*2 + 3], src2[src2Offs + 4*2 + 3], t);
	}

	public static void xform3x4ExpMap(float[] dst, int dstOffs, float lx, float ly, float lz) {
		float ha = sqrtf(lx*lx + ly*ly + lz*lz);
		/*
		float w = cosf(ha);
		float s = sinc(ha);
		*/
		float ha2 = ha * ha;
		float ha4 = ha2 * ha2;
		float ha6 = ha2 * ha4;
		float ha8 = ha4 * ha4;
		float w = 1.0f + (-1.0f/2)*ha2 + (1.0f/24)*ha4 + (-1.0f/720)*ha6 + (1.0f/40320)*ha8;
		float s = 1.0f + (-1.0f/6)*ha2 + (1.0f/120)*ha4 + (-1.0f/5040)*ha6 + (1.0f/362880)*ha8;

		float x = lx * s;
		float y = ly * s;
		float z = lz * s;

		s = 1.0f / sqrtf(x*x + y*y + z*z + w*w);
		x *= s;
		y *= s;
		z *= s;
		w *= s;

		float m00 = 1.0f - 2.0f*y*y - 2.0f*z*z;
		float m01 = 2.0f*x*y + 2.0f*w*z;
		float m02 = 2.0f*x*z - 2.0f*w*y;
		float m10 = 2.0f*x*y - 2.0f*w*z;
		float m11 = 1.0f - 2.0f*x*x - 2.0f*z*z;
		float m12 = 2.0f*y*z + 2.0f*w*x;
		float m20 = 2.0f*x*z + 2.0f*w*y;
		float m21 = 2.0f*y*z - 2.0f*w*x;
		float m22 = 1.0f - 2.0f*x*x - 2.0f*y*y;

		dst[dstOffs] = m00;
		dst[dstOffs + 4] = m01;
		dst[dstOffs + 4*2] = m02;

		dst[dstOffs + 1] = m10;
		dst[dstOffs + 4 + 1] = m11;
		dst[dstOffs + 4*2 + 1] = m12;

		dst[dstOffs + 2] = m20;
		dst[dstOffs + 4 + 2] = m21;
		dst[dstOffs + 4*2 + 2] = m22;
	}

	public static void xform3x4GetQuat(float[] quat, int quatOffs, float[] xform, int xformOffs) {
		float m00 = xform[xformOffs];
		float m01 = xform[xformOffs + 4];
		float m02 = xform[xformOffs + 4*2];

		float m10 = xform[xformOffs + 1];
		float m11 = xform[xformOffs + 4 + 1];
		float m12 = xform[xformOffs + 4*2 + 1];

		float m20 = xform[xformOffs + 2];
		float m21 = xform[xformOffs + 4 + 2];
		float m22 = xform[xformOffs + 4*2 + 2];

		float s;
		float x = 0.0f;
		float y = 0.0f;
		float z = 0.0f;
		float w = 1.0f;
		float trace = m00 + m11 + m22;
		if (trace > 0.0f) {
			s = sqrtf(trace + 1.0f);
			w = s * 0.5f;
			s = 0.5f / s;
			x = (m12 - m21) * s;
			y = (m20 - m02) * s;
			z = (m01 - m10) * s;
		} else {
			if (m11 > m00) {
				if (m22 > m11) {
					s = m22 - m11 - m00;
					s = sqrtf(s + 1.0f);
					z = s * 0.5f;
					if (s != 0.0f) {
						s = 0.5f / s;
					}
					w = (m01 - m10) * s;
					x = (m20 + m02) * s;
					y = (m21 + m12) * s;
				} else {
					s = m11 - m22 - m00;
					s = sqrtf(s + 1.0f);
					y = s * 0.5f;
					if (s != 0.0f) {
						s = 0.5f / s;
					}
					w = (m20 - m02) * s;
					z = (m12 + m21) * s;
					x = (m10 + m01) * s;
				}
			} else if (m22 > m00) {
				s = m22 - m11 - m00;
				s = sqrtf(s + 1.0f);
				z = s * 0.5f;
				if (s != 0.0f) {
					s = 0.5f / s;
				}
				w = (m01 - m10) * s;
				x = (m20 + m02) * s;
				y = (m21 + m12) * s;
			} else {
				s = m00 - m11 - m22;
				s = sqrtf(s + 1.0f);
				x = s * 0.5f;
				if (s != 0.0f) {
					s = 0.5f / s;
				}
				w = (m12 - m21) * s;
				y = (m01 + m10) * s;
				z = (m02 + m20) * s;
			}
		}

		quat[quatOffs] = x;
		quat[quatOffs + 1] = y;
		quat[quatOffs + 2] = z;
		quat[quatOffs + 3] = w;
	}

	public static void xform3x4Quat(float[] xform, int xformOffs, float[] quat, int quatOffs) {
		float x = quat[quatOffs];
		float y = quat[quatOffs + 1];
		float z = quat[quatOffs + 2];
		float w = quat[quatOffs + 3];
		xform3x4SetAxis(xform, xformOffs, 0,
				1.0f - (2.0f*y*y) - (2.0f*z*z),
				(2.0f*x*y) + (2.0f*w*z),
				(2.0f*x*z) - (2.0f*w*y)
		);
		xform3x4SetAxis(xform, xformOffs, 1,
				(2.0f*x*y) - (2.0f*w*z),
				1.0f - (2.0f*x*x) - (2.0f*z*z),
				(2.0f*y*z) + (2.0f*w*x)
		);
		xform3x4SetAxis(xform, xformOffs, 2,
				(2.0f*x*z) + (2.0f*w*y),
				(2.0f*y*z) - (2.0f*w*x),
				1.0f - (2.0f*x*x) - (2.0f*y*y)
		);
	}

	public static void xform3x4Inv(float[] dst, int dstOffs, float[] src, int srcOffs) {
		float m00 = src[srcOffs];
		float m01 = src[srcOffs + 4];
		float m02 = src[srcOffs + 4*2];
		float m10 = src[srcOffs + 1];
		float m11 = src[srcOffs + 4 + 1];
		float m12 = src[srcOffs + 4*2 + 1];
		float m20 = src[srcOffs + 2];
		float m21 = src[srcOffs + 4 + 2];
		float m22 = src[srcOffs + 4*2 + 2];
		float m30 = src[srcOffs + 3];
		float m31 = src[srcOffs + 4 + 3];
		float m32 = src[srcOffs + 4*2 + 3];

		float a0 = m00*m11 - m01*m10;
		float a1 = m00*m12 - m02*m10;
		float a3 = m01*m12 - m02*m11;

		float b0 = m20*m31 - m21*m30;
		float b1 = m20*m32 - m22*m30;
		float b2 = m20;
		float b3 = m21*m32 - m22*m31;
		float b4 = m21;
		float b5 = m22;

		float det = a0*b5 - a1*b4 + a3*b2;
		float idet = 0.0f;
		if (det != 0.0f) {
			float im00 =  m11*b5 - m12*b4;
			float im10 = -m10*b5 + m12*b2;
			float im20 =  m10*b4 - m11*b2;
			float im30 = -m10*b3 + m11*b1 - m12*b0;

			float im01 = -m01*b5 + m02*b4;
			float im11 =  m00*b5 - m02*b2;
			float im21 = -m00*b4 + m01*b2;
			float im31 =  m00*b3 - m01*b1 + m02*b0;

			float im02 =  a3;
			float im12 = -a1;
			float im22 =  a0;
			float im32 = -m30*a3 + m31*a1 - m32*a0;

			dst[dstOffs] = im00;
			dst[dstOffs + 4] = im01;
			dst[dstOffs + 4*2] = im02;
			dst[dstOffs + 1] = im10;
			dst[dstOffs + 4 + 1] = im11;
			dst[dstOffs + 4*2 + 1] = im12;
			dst[dstOffs + 2] = im20;
			dst[dstOffs + 4 + 2] = im21;
			dst[dstOffs + 4*2 + 2] = im22;
			dst[dstOffs + 3] = im30;
			dst[dstOffs + 4 + 3] = im31;
			dst[dstOffs + 4*2 + 3] = im32;

			idet = 1.0f / det;
		}
		for (int i = dstOffs; i < dstOffs + 4*3; ++i) {
			dst[i] *= idet;
		}
	}

	private static void quatClosestAxis(float[] dst, int dstOffs, float[] src, int srcOffs, int axis) {
		float e = src[srcOffs + axis];
		float w = src[srcOffs + 3];
		dst[dstOffs] = 0.0f;
		dst[dstOffs + 1] = 0.0f;
		dst[dstOffs + 2] = 0.0f;
		dst[dstOffs + 3] = 1.0f;
		float sqmag = e*e + w*w;
		if (sqmag > 1.0e-5f) {
			float s = rcp0(sqrtf(sqmag));
			dst[dstOffs + axis] = e*s;
			dst[dstOffs + 3] = w*s;
		}
	}

	private static void quatClosestX(float[] dst, int dstOffs, float[] src, int srcOffs) {
		quatClosestAxis(dst, dstOffs, src, srcOffs, 0);
	}

	private static void quatClosestY(float[] dst, int dstOffs, float[] src, int srcOffs) {
		quatClosestAxis(dst, dstOffs, src, srcOffs, 1);
	}

	private static void quatClosestZ(float[] dst, int dstOffs, float[] src, int srcOffs) {
		quatClosestAxis(dst, dstOffs, src, srcOffs, 2);
	}

	public static void quatClosestXY(float[] dst, int dstOffs, float x, float y, float z, float w) {
		float det = Math.abs(-x*y - z*w);
		float s;
		if (det < 0.5f) {
			float d = sqrtf(Math.abs(1.0f - 4.0f*sq(det)));
			float a = x*w - y*z;
			float b = sq(w) - sq(x) + sq(y) - sq(z);
			float s0, c0;
			if (b >= 0.0f) {
				s0 = a;
				c0 = (d + b)*0.5f;
			} else {
				s0 = (d - b)*0.5f;
				c0 = a;
			}
			s = rcp0(hypotf(s0, c0));
			s0 *= s;
			c0 *= s;

			float s1 = y*c0 - z*s0;
			float c1 = w*c0 + x*s0;
			s = rcp0(hypotf(s1, c1));
			s1 *= s;
			c1 *= s;

			x = s0*c1;
			y = c0*s1;
			z = -s0*s1;
			w = c0*c1;
		} else {
			s = rcp0(sqrtf(det));
			x *= s;
			y = 0.0f;
			z = 0.0f;
			w *= s;
		}
		dst[dstOffs] = x;
		dst[dstOffs + 1] = y;
		dst[dstOffs + 2] = z;
		dst[dstOffs + 3] = w;
	}

	public static void quatClosestXY(float[] dst, int dstOffs, float[] src, int srcOffs) {
		float x = src[srcOffs];
		float y = src[srcOffs + 1];
		float z = src[srcOffs + 2];
		float w = src[srcOffs + 3];
		quatClosestXY(dst, dstOffs, x, y, z, w);
	}

	public static void quatClosestYX(float[] dst, int dstOffs, float x, float y, float z, float w) {
		quatClosestXY(dst, dstOffs, x, y, -z, w);
		dst[dstOffs + 2] = -dst[dstOffs + 2];
	}

	public static void quatClosestYX(float[] dst, int dstOffs, float[] src, int srcOffs) {
		float x = src[srcOffs];
		float y = src[srcOffs + 1];
		float z = src[srcOffs + 2];
		float w = src[srcOffs + 3];
		quatClosestYX(dst, dstOffs, x, y, z, w);
	}

	public static void quatClosestXZ(float[] dst, int dstOffs, float x, float y, float z, float w) {
		quatClosestYX(dst, dstOffs, x, z, y, w);
		float t = dst[dstOffs + 1];
		dst[dstOffs + 1] = dst[dstOffs + 2];
		dst[dstOffs + 2] = t;
	}

	public static void quatClosestXZ(float[] dst, int dstOffs, float[] src, int srcOffs) {
		float x = src[srcOffs];
		float y = src[srcOffs + 1];
		float z = src[srcOffs + 2];
		float w = src[srcOffs + 3];
		quatClosestXZ(dst, dstOffs, x, y, z, w);
	}

	public static void quatClosestZX(float[] dst, int dstOffs, float x, float y, float z, float w) {
		quatClosestXY(dst, dstOffs, x, z, y, w);
		float t = dst[dstOffs + 1];
		dst[dstOffs + 1] = dst[dstOffs + 2];
		dst[dstOffs + 2] = t;
	}

	public static void quatClosestZX(float[] dst, int dstOffs, float[] src, int srcOffs) {
		float x = src[srcOffs];
		float y = src[srcOffs + 1];
		float z = src[srcOffs + 2];
		float w = src[srcOffs + 3];
		quatClosestZX(dst, dstOffs, x, y, z, w);
	}

	public static void quatClosestYZ(float[] dst, int dstOffs, float x, float y, float z, float w) {
		quatClosestXY(dst, dstOffs, y, z, x, w);
		float tx = dst[dstOffs];
		float ty = dst[dstOffs + 1];
		float tz = dst[dstOffs + 2];
		dst[dstOffs] = tz;
		dst[dstOffs + 1] = tx;
		dst[dstOffs + 2] = ty;
	}

	public static void quatClosestYZ(float[] dst, int dstOffs, float[] src, int srcOffs) {
		float x = src[srcOffs];
		float y = src[srcOffs + 1];
		float z = src[srcOffs + 2];
		float w = src[srcOffs + 3];
		quatClosestYZ(dst, dstOffs, x, y, z, w);
	}

	public static void quatClosestZY(float[] dst, int dstOffs, float x, float y, float z, float w) {
		quatClosestYX(dst, dstOffs, y, z, x, w);
		float tx = dst[dstOffs];
		float ty = dst[dstOffs + 1];
		float tz = dst[dstOffs + 2];
		dst[dstOffs] = tz;
		dst[dstOffs + 1] = tx;
		dst[dstOffs + 2] = ty;
	}

	public static void slerp(float[] dst, int dstOffs, float[] src1, int src1Offs, float[] src2, int src2Offs, float t) {
		float x1 = src1[src1Offs];
		float y1 = src1[src1Offs + 1];
		float z1 = src1[src1Offs + 2];
		float w1 = src1[src1Offs + 3];
		float x2 = src2[src2Offs];
		float y2 = src2[src2Offs + 1];
		float z2 = src2[src2Offs + 2];
		float w2 = src2[src2Offs + 3];
		float c = x1*x2 + y1*y2 + z1*z2 + w1*w2;
		if (c < 0.0d) {
			x2 = -x2;
			y2 = -y2;
			z2 = -z2;
			w2 = -w2;
		}
		float u = sq(x1 - x2) + sq(y1 - y2) + sq(z1 - z2) + sq(w1 - w2);
		float v = sq(x1 + x2) + sq(y1 + y2) + sq(z1 + z2) + sq(w1 + w2);
		float ang = 2.0f * atan2f(sqrtf(u), sqrtf(v));
		float s = 1.0f - t;
		float d = 1.0f / sinc(ang);
		s = sinc(ang*s) * d * s;
		t = sinc(ang*t) * d * t;
		dst[dstOffs] = x1*s + x2*t;
		dst[dstOffs + 1] = y1*s + y2*t;
		dst[dstOffs + 2] = z1*s + z2*t;
		dst[dstOffs + 3] = w1*s + w2*t;
		normalize(dst, dstOffs, 4);
	}

	public static void quatMul(float[] q, int offs, float x, float y, float z, float w) {
		float qx = q[offs];
		float qy = q[offs + 1];
		float qz = q[offs + 2];
		float sq = q[offs + 3];
		float sp = w;
		cross(q, offs, x, y, z);
		q[offs] += qx*sp + x*sq;
		q[offs + 1] += qy*sp + y*sq;
		q[offs + 2] += qz*sp + z*sq;
		float d = qx*x + qy*y + qz*z;
		q[offs + 3] = sq*sp - d;
	}

	public static void quatRadiansX(float[] q, int offs, float rad) {
		float h = rad * 0.5f;
		q[offs] = sinf(h);
		q[offs + 1] = 0.0f;
		q[offs + 2] = 0.0f;
		q[offs + 3] = cosf(h);
	}

	public static void quatDegreesX(float[] q, int offs, float deg) {
		quatRadiansX(q, offs, radians(deg));
	}

	public static void quatMulRadiansX(float[] q, int offs, float rad) {
		float h = rad * 0.5f;
		quatMul(q, offs, sinf(h), 0.0f, 0.0f, cosf(h));
	}

	public static void quatMulDegreesX(float[] q, int offs, float deg) {
		quatMulRadiansX(q, offs, radians(deg));
	}

	public static void quatRadiansY(float[] q, int offs, float rad) {
		float h = rad * 0.5f;
		q[offs] = 0.0f;
		q[offs + 1] = sinf(h);
		q[offs + 2] = 0.0f;
		q[offs + 3] = cosf(h);
	}

	public static void quatDegreesY(float[] q, int offs, float deg) {
		quatRadiansY(q, offs, radians(deg));
	}

	public static void quatMulRadiansY(float[] q, int offs, float rad) {
		float h = rad * 0.5f;
		quatMul(q, offs, 0.0f, sinf(h), 0.0f, cosf(h));
	}

	public static void quatMulDegreesY(float[] q, int offs, float deg) {
		quatMulRadiansY(q, offs, radians(deg));
	}

	public static void quatRadiansZ(float[] q, int offs, float rad) {
		float h = rad * 0.5f;
		q[offs] = 0.0f;
		q[offs + 1] = 0.0f;
		q[offs + 2] = sinf(h);
		q[offs + 3] = cosf(h);
	}

	public static void quatDegreesZ(float[] q, int offs, float deg) {
		quatRadiansZ(q, offs, radians(deg));
	}

	public static void quatMulRadiansZ(float[] q, int offs, float rad) {
		float h = rad * 0.5f;
		quatMul(q, offs, 0.0f, 0.0f, sinf(h), cosf(h));
	}

	public static void quatMulDegreesZ(float[] q, int offs, float deg) {
		quatMulRadiansZ(q, offs, radians(deg));
	}

	public static void quatRadiansXYZ(float[] q, int offs, float rx, float ry, float rz) {
		quatRadiansZ(q, offs, rz);
		quatMulRadiansY(q, offs, ry);
		quatMulRadiansX(q, offs, rx);
	}

	public static void quatDegreesXYZ(float[] q, int offs, float dx, float dy, float dz) {
		quatRadiansXYZ(q, offs, radians(dx), radians(dy), radians(dz));
	}

}

