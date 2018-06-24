// Author: Sergey Chaban <sergey.chaban@gmail.com>

package xdata;

import java.util.Arrays;

public final class SH {

	private SH() {}

	public static int calcCoefsNum(int order) {
		return order < 1 ? 0 : order*order;
	}

	public static int calcAryIdx(int l, int m) {
		return l*(l+1) + m;
	}

	public static int bandIdxFromAryIdx(int idx) {
		return (int)Math.sqrt((float)idx);
	}

	public static int funcIdxFromAryBand(int idx, int l) {
		return idx - l*(l + 1);
	}

	public static double calcK(int l, int m) {
		int am = Math.abs(m);
		double v = 1.0;
		for (int k = l + am; k > l - am; --k) {
			v *= k;
		}
		return Math.sqrt((2.0*l + 1.0) / (4.0*Math.PI*v));
	}

	public static double calcPmm(int m) {
		double v = 1.0;
		for (int k = 0; k <= m; ++k) {
			v *= 1.0 - 2.0*k;
		}
		return v;
	}

	public static double calcConstA(int m) {
		return calcPmm(m) * calcK(m, m) * Math.sqrt(2.0);
	}

	public static double calcConstB(int m) {
		return (2*m + 1) * calcPmm(m) * calcK(m+1, m);
	}

	public static double calcConstC1(int l, int m) {
		return calcK(l, m) / calcK(l-1, m) * (2*l-1) / (l-m);
	}

	public static double calcConstC2(int l, int m) {
		return -calcK(l, m) / calcK(l-2, m) * (l+m-1) / (l-m);
	}

	public static double calcConstD1(int m) {
		return ((2*m+3)*(2*m+1)) * calcPmm(m) / 2.0 * calcK(m+2, m);
	}

	public static double calcConstD2(int m) {
		return (-(2*m + 1)) * calcPmm(m) / 2.0 * calcK(m+2, m);
	}

	public static double calcConstE1(int m) {
		return ((2*m+5)*(2*m+3)*(2*m+1)) * calcPmm(m) / 6.0 * calcK(m+3,m);
	}

	public static double calcConstE2(int m) {
		double pmm = calcPmm(m);
		return ((2*m+5)*(2*m+1)*pmm/6.0 + ((2*m+2)*(2*m+1))*pmm/3.0) * -calcK(m+3, m);
	}

	public static void calcConsts(int order, float[] c, int offs) {
		if (order < 1) return;
		if (c == null) return;
		int idx = offs;
		// 0, 0
		c[idx++] = (float)calcK(0, 0);
		if (order > 1) {
			// 1, 0
			c[idx++] = (float) (calcPmm(0) * calcK(1, 0));
		}
		if (order > 2) {
			// 2, 0
			c[idx++] = (float)calcConstD1(0);
			c[idx++] = (float)calcConstD2(0);
		}
		if (order > 3) {
			// 2, 0
			c[idx++] = (float)calcConstE1(0);
			c[idx++] = (float)calcConstE2(0);
		}
		for (int l = 4; l < order; ++l) {
			c[idx++] = (float)calcConstC1(l, 0);
			c[idx++] = (float)calcConstC2(l, 0);
		}
		final double scl = Math.sqrt(2.0);
		for (int m = 1; m < order-1; ++m) {
			c[idx++] = (float)calcConstA(m);
			if (m+1 < order) {
				c[idx++] = (float)(calcConstB(m) * scl);
			}
			if (m+2 < order) {
				c[idx++] = (float)(calcConstD1(m) * scl);
				c[idx++] = (float)(calcConstD2(m) * scl);
			}
			if (m+3 < order) {
				c[idx++] = (float)(calcConstE1(m) * scl);
				c[idx++] = (float)(calcConstE2(m) * scl);
			}
			for (int l = m+4; l < order; ++l) {
				c[idx++] = (float)calcConstC1(l, m);
				c[idx++] = (float)calcConstC2(l, m);
			}
		}
		if (order > 1) {
			c[idx] = (float)calcConstA(order-1);
		}
	}

	public static float[] calcConsts(int order) {
		float[] c = allocConsts(order);
		calcConsts(order, c, 0);
		return c;
	}

	public static void eval(int order, float[] coefs, int coefsOffs, float[] consts, int constsOffs, float x, float y, float z) {
		if (order < 1) return;
		int idx = constsOffs;
		float zz = z*z;
		float tmp = 0.0f;
		coefs[0] = (float)consts[idx++];
		if (order > 1) {
			coefs[calcAryIdx(1, 0)] = consts[idx++] * z;
		}
		if (order > 2) {
			tmp = consts[idx++] * zz;
			tmp += consts[idx++];
			coefs[calcAryIdx(2, 0)] = tmp;
		}
		if (order > 3) {
			tmp = consts[idx++] * zz;
			tmp += consts[idx++];
			coefs[calcAryIdx(3, 0)] = tmp * z;
		}
		for (int l = 4; l < order; ++l) {
			tmp = consts[idx++] * z * coefs[calcAryIdx(l-1, 0)];
			tmp += consts[idx++] * coefs[calcAryIdx(l-2, 0)];
			coefs[calcAryIdx(l, 0)] = tmp;
		}
		float prev0 = 0.0f;
		float prev1 = 0.0f;
		float prev2 = 0.0f;
		float s0 = y;
		float s1 = 0.0f;
		float c0 = x;
		float c1 = 0.0f;
		int isc = 0;
		for (int m = 1; m < order-1; ++m) {
			int l = m;
			float s = isc == 0 ? s0 : s1;
			float c = isc == 0 ? c0 : c1;
			tmp = consts[idx++];
			coefs[l*l + l - m] = tmp * s;
			coefs[l*l + l + m] = tmp * c;
			if (m+1 < order) {
				/* (m+1, -m), (m+1, m) */
				l = m+1;
				prev1 = consts[idx++] * z;
				coefs[l*l + l - m] = prev1 * s;
				coefs[l*l + l + m] = prev1 * c;
			}
			if (m+2 < order) {
				/* (m+2, -m), (m+2, m) */
				l = m+2;
				tmp = consts[idx++] * zz;
				tmp += consts[idx++];
				prev2 = tmp;
				coefs[l*l + l - m] = prev2 * s;
				coefs[l*l + l + m] = prev2 * c;
			}
			if (m+3 < order) {
				/* (m+3, -m), (m+3, m) */
				l = m+3;
				tmp = consts[idx++] * zz;
				tmp += consts[idx++];
				prev0 = tmp * z;
				coefs[l*l + l - m] = prev0 * s;
				coefs[l*l + l + m] = prev0 * c;
			}
			final int prevMask = 1 | (0 << 2) | (2 << 4) | (1 << 6) | (0 << 8) | (2 << 10) | (1 << 12);
			int mask = prevMask | ((prevMask >>> 2) << 14);
			int maskCnt = 0;
			for (l = m+4; l < order; ++l) {
				tmp = consts[idx++] * z;
				int ip = (mask >>> 2) & 3;
				tmp *= ip == 0 ? prev0 : ip == 1 ? prev1 : prev2;
				int prevIdx = mask & 3;
				float prev = tmp;
				ip = (mask >>> 4) & 3;
				tmp = consts[idx++];
				tmp *= ip == 0 ? prev0 : ip == 1 ? prev1 : prev2;
				prev += tmp;
				if (prevIdx == 0) {
					prev0 = prev;
				} else if (prevIdx == 1){
					prev1 = prev;
				} else {
					prev2 = prev;
				}
				coefs[l*l + l - m] = prev * s;
				coefs[l*l + l + m] = prev * c;
				if (order < 11) {
					mask >>>= 4;
				} else {
					++maskCnt;
					if (maskCnt < 3) {
						mask >>>= 4;
					} else {
						mask = prevMask;
						maskCnt = 0;
					}
				}
			}
			isc ^= 1;
			if (isc == 0) {
				s0 = x*s1 + y*c1;
				c0 = x*c1 - y*s1;
			} else {
				s1 = x*s0 + y*c0;
				c1 = x*c0 - y*s0;
			}
		}
		if (order > 1) {
			float s = isc == 0 ? s0 : s1;
			float c = isc == 0 ? c0 : c1;
			tmp = consts[idx];
			coefs[calcAryIdx(order-1, -(order-1))] = tmp * s;
			coefs[calcAryIdx(order-1, (order-1))] = tmp * c;
		}
	}

	public static void eval(int order, float[] coefs, float[] consts, float... vec) {
		if (vec == null || vec.length < 1) {
			return;
		}
		float x = vec[0];
		float y = vec.length > 1 ? vec[1] : 0.0f;
		float z = vec.length > 2 ? vec[2] : 0.0f;
		eval(order, coefs, 0, consts, 0, x, y, z);
	}

	public static void eval(int order, float[] coefs, float[] consts, Vec vec) {
		eval(order, coefs, consts, vec.el);
	}

	public static float[] allocConsts(int order) {
		if (order < 1) return null;
		return new float[order*order - (order-1)];
	}

	public static float[] allocCoefs(int order) {
		if (order < 1) return null;
		return new float[order*order];
	}


	public static void pano(
			/* SH */
			int order,
			float[] consts,
			float[] rdst, int rdstOffs, int rdstStep,
			float[] gdst, int gdstOffs, int gdstStep,
			float[] bdst, int bdstOffs, int bdstStep,
			/* img */
			int w, int h,
			float[] rsrc, int roffs, int rstep,
			float[] gsrc, int goffs, int gstep,
			float[] bsrc, int boffs, int bstep
	) {
		int ncoef = calcCoefsNum(order);
		if (ncoef < 1) return;
		for (int i = 0; i < ncoef; ++i) {
			rdst[rdstOffs + i*rdstStep] = 0.0f;
			gdst[gdstOffs + i*gdstStep] = 0.0f;
			bdst[bdstOffs + i*bdstStep] = 0.0f;
		}
		float[] ctmp = new float[ncoef];
		int rstride = rstep * w;
		int gstride = gstep * w;
		int bstride = bstep * w;
		int ir = rdstOffs;
		int ig = gdstOffs;
		int ib = bdstOffs;
		float da = (float)((2.0*Math.PI / w) * (Math.PI / h));
		float sum = 0.0f;
		float iw = 1.0f / w;
		float ih = 1.0f / h;
		for (int y = 0; y < h; ++y) {
			float v = 1.0f - (y + 0.5f)*ih;
			float dw = da * (float)Math.sin(Math.PI * v);
			for (int x = 0; x < w; ++x) {
				float u = (x + 0.5f) * iw;
				int idx = y*w + x;
				float r = rsrc[roffs + rstep*idx];
				float g = gsrc[goffs + gstep*idx];
				float b = bsrc[boffs + rstep*idx];
				float azi = u * 2.0f * (float)Math.PI;
				float ele = (v - 1.0f) * (float)Math.PI;
				float sinAzi = Calc.sinf(azi);
				float cosAzi = Calc.cosf(azi);
				float sinEle = Calc.sinf(ele);
				float cosEle = Calc.cosf(ele);
				float nx = cosAzi*sinEle;
				float ny = cosEle;
				float nz = sinAzi*sinEle;
				eval(order, ctmp, 0, consts, 0, nx, ny, nz);
				for (int i = 0; i < ncoef; ++i) {
					float sw = ctmp[i] * dw;
					rdst[rdstOffs + i*rdstStep] += r * sw;
					gdst[gdstOffs + i*gdstStep] += g * sw;
					bdst[bdstOffs + i*bdstStep] += b * sw;
				}
				sum += dw;
			}
		}
		float s = (float)(Math.PI*4.0f) / sum;
		for (int i = 0; i < ncoef; ++i) {
			rdst[rdstOffs + i*rdstStep] *= s;
			gdst[gdstOffs + i*gdstStep] *= s;
			bdst[bdstOffs + i*bdstStep] *= s;
		}
	}

	public static void pano(
			/* SH */
			int order,
			float[] consts,
			float[] rdst, int rdstOffs, int rdstStep,
			float[] gdst, int gdstOffs, int gdstStep,
			float[] bdst, int bdstOffs, int bdstStep,
			/* img */
			XTex tex
	) {
		int w = tex.getWidth();
		int h = tex.getHeight();
		float[] rgb = new float[w*h*3];
		tex.getRGB(rgb, 0, null);
		pano(order, consts,
		     rdst, rdstOffs, rdstStep,
		     gdst, gdstOffs, gdstStep,
		     bdst, bdstOffs, bdstStep,
		     w, h,
		     rgb, 0, 3,
		     rgb, 1, 3,
		     rgb, 2, 3
		);
	}

	public static float[] allocWeights(int order) {
		return new float[order];
	}

	public static void calcWeights(float[] wgt, int order, float s, float scl) {
		for (int i = 0; i < order; ++i) {
			float ii = (float)(-i*i);
			wgt[i] = Calc.expf(ii / (2.0f*s)) * scl;
		}
	}

	public static float[] calcWeights(int order, float s, float scl) {
		float[] wgt = allocWeights(order);
		calcWeights(wgt, order, s, scl);
		return wgt;
	}

	public static void diffWeights(float[] wgt, int order) {
		Arrays.fill(wgt, 0.0f);
		if (order >= 1) wgt[0] = 1.0f;
		if (order >= 2) wgt[1] = 1.0f / 1.5f;
		if (order >= 3) wgt[2] = 1.0f / 4.0f;
	}

	public static void applyWeights(float[] dst, int order, float[] src, float[] wgt) {
		for (int l = 0; l < order; ++l) {
			int i0 = calcCoefsNum(l);
			int i1 = calcCoefsNum(l+1);
			float w = wgt[l];
			for (int i = i0; i < i1; ++i) {
				dst[i] = src[i] * w;
			}
		}
	}

	public static void premultDC(float[] coefs) {
		coefs[0] *= 0.282094806432724f;
	}

	public static void extractDominantDir(
	        float[] dst, int dstOffs,
	        float[] coefsR, int offsR,
	        float[] coefsG, int offsG,
	        float[] coefsB, int offsB
	) {
		int idx = calcAryIdx(1, 1);
		float lx = Calc.luminance(coefsR[offsR + idx], coefsG[offsG + idx], coefsB[offsB + idx]);
		idx = calcAryIdx(1, -1);
		float ly = Calc.luminance(coefsR[offsR + idx], coefsG[offsG + idx], coefsB[offsB + idx]);
		idx = calcAryIdx(1, 0);
		float lz = Calc.luminance(coefsR[offsR + idx], coefsG[offsG + idx], coefsB[offsB + idx]);
		dst[dstOffs] = -lx;
		dst[dstOffs + 1] = -ly;
		dst[dstOffs + 2] = lz;
		Calc.normalize(dst, dstOffs, 3);
	}

	public static Vec extractDominantDir(
			float[] coefsR, int offsR,
			float[] coefsG, int offsG,
			float[] coefsB, int offsB
	) {
		Vec dir = new Vec();
		extractDominantDir(dir.el, 0, coefsR, offsR, coefsG, offsG, coefsB, offsB);
		return dir;
	}

	public static Vec extractDominantDir(float[] coefsR, float[] coefsG, float[] coefsB) {
		return extractDominantDir(coefsR, 0, coefsG, 0, coefsB, 0);
	}

}
