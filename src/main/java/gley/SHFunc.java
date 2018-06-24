// GLSL ES codegen
// Author: Sergey Chaban <sergey.chaban@gmail.com>

package gley;

import java.util.Locale;

import static xdata.SH.*;

public final class SHFunc {
	private SHFunc() {}

	private static void casgn(StringBuilder sb, int idx) {
		sb.append("\tsh[");
		sb.append(idx);
		sb.append("] = ");
	}

	private static void casgn(StringBuilder sb, int l, int m) {
		casgn(sb, calcAryIdx(l, m));
	}

	private static void cval(StringBuilder sb, float c) {
		sb.append(String.format(Locale.US, "%.16f", c));
		//sb.append("f");
	}

	private static void updsin(StringBuilder sb, int isc) {
		sb.append("\ts");
		sb.append(isc^1);
		sb.append(" = x*s");
		sb.append(isc);
		sb.append(" + y*c");
		sb.append(isc);
		sb.append(";\n");
	}

	private static void updcos(StringBuilder sb, int isc) {
		sb.append("\tc");
		sb.append(isc^1);
		sb.append(" = x*c");
		sb.append(isc);
		sb.append(" - y*s");
		sb.append(isc);
		sb.append(";\n");
	}

	public static void gen(StringBuilder sb, int order, float[] consts, int coffs) {
		if (order < 1) return;
		if (consts == null) return;
		sb.append("void sh");
		sb.append(order);
		sb.append("(out float sh[");
		sb.append(order*order);
		sb.append("], vec3 v) {\n");
		sb.append("\tfloat x = v.x;\n");
		sb.append("\tfloat y = v.y;\n");
		sb.append("\tfloat z = v.z;\n");
		sb.append("\tfloat zz = z*z;\n");
		sb.append("\tfloat tmp, prev0, prev1, prev2, s0 = y, s1, c0 = x, c1;\n");
		int cidx = coffs;
		casgn(sb, 0);
		cval(sb, consts[cidx++]);
		sb.append(";\n");
		if (order > 1) {
			casgn(sb, 1, 0);
			sb.append("z*");
			cval(sb, consts[cidx++]);
			sb.append(";\n");
		}
		if (order > 2) {
			casgn(sb, 2, 0);
			sb.append("zz*");
			cval(sb, consts[cidx++]);
			sb.append(" + ");
			cval(sb, consts[cidx++]);
			sb.append(";\n");
		}
		if (order > 3) {
			casgn(sb, 3, 0);
			sb.append("(zz*");
			cval(sb, consts[cidx++]);
			sb.append(" + ");
			cval(sb, consts[cidx++]);
			sb.append(")*z;\n");
		}
		for (int l = 4; l < order; ++l) {
			casgn(sb, l, 0);
			sb.append("z*");
			cval(sb, consts[cidx++]);
			sb.append("*sh[");
			sb.append(calcAryIdx(l-1, 0));
			sb.append("] + sh[");
			sb.append(calcAryIdx(l-2, 0));
			sb.append("]*");
			cval(sb, consts[cidx++]);
			sb.append(";\n");
		}
		int isc = 0;
		for (int m = 1; m < order-1; ++m) {
			int l = m;
			String s = isc == 0 ? "s0" : "s1";
			String c = isc == 0 ? "c0" : "c1";
			sb.append("\ttmp = ");
			cval(sb, consts[cidx++]);
			sb.append(";\n");
			casgn(sb, l*l + l - m);
			sb.append("tmp * ");
			sb.append(s);
			sb.append(";\n");
			casgn(sb, l*l + l + m);
			sb.append("tmp * ");
			sb.append(c);
			sb.append(";\n");
			if (m+1 < order) {
				/* (m+1, -m), (m+1, m) */
				l = m+1;
				sb.append("\tprev1 = z*");
				cval(sb, consts[cidx++]);
				sb.append(";\n");
				casgn(sb, l*l + l - m);
				sb.append("prev1 * ");
				sb.append(s);
				sb.append(";\n");
				casgn(sb, l*l + l + m);
				sb.append("prev1 * ");
				sb.append(c);
				sb.append(";\n");
			}
			if (m+2 < order) {
				/* (m+2, -m), (m+2, m) */
				l = m+2;
				sb.append("\tprev2 = zz*");
				cval(sb, consts[cidx++]);
				sb.append(" + ");
				cval(sb, consts[cidx++]);
				sb.append(";\n");
				casgn(sb, l*l + l - m);
				sb.append("prev2 * ");
				sb.append(s);
				sb.append(";\n");
				casgn(sb, l*l + l + m);
				sb.append("prev2 * ");
				sb.append(c);
				sb.append(";\n");
			}
			if (m+3 < order) {
				/* (m+3, -m), (m+3, m) */
				l = m+3;
				sb.append("\tprev0 = (zz*");
				cval(sb, consts[cidx++]);
				sb.append(" + ");
				cval(sb, consts[cidx++]);
				sb.append(") * z;\n");
				casgn(sb, l*l + l - m);
				sb.append("prev0 * ");
				sb.append(s);
				sb.append(";\n");
				casgn(sb, l*l + l + m);
				sb.append("prev0 * ");
				sb.append(c);
				sb.append(";\n");
			}
			final int prevMask = 1 | (0 << 2) | (2 << 4) | (1 << 6) | (0 << 8) | (2 << 10) | (1 << 12);
			int mask = prevMask | ((prevMask >>> 2) << 14);
			int maskCnt = 0;
			for (l = m+4; l < order; ++l) {
				int iprev = mask & 3;
				sb.append("\tprev");
				sb.append(iprev);
				sb.append(" = prev");
				sb.append((mask >>> 2) & 3);
				sb.append("*z*");
				cval(sb, consts[cidx++]);
				sb.append(" + prev");
				sb.append((mask >>> 4) & 3);
				sb.append("*");
				cval(sb, consts[cidx++]);
				sb.append(";\n");

				casgn(sb, l*l + l - m);
				sb.append("prev");
				sb.append(iprev);
				sb.append(" * ");
				sb.append(s);
				sb.append(";\n");

				casgn(sb, l*l + l + m);
				sb.append("prev");
				sb.append(iprev);
				sb.append(" * ");
				sb.append(c);
				sb.append(";\n");

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
			updsin(sb, isc);
			updcos(sb, isc);
			isc ^= 1;
		}
		if (order > 1) {
			String s = isc == 0 ? "s0" : "s1";
			String c = isc == 0 ? "c0" : "c1";
			sb.append("\ttmp = ");
			cval(sb, consts[cidx]);
			sb.append(";\n");
			casgn(sb, order-1, -(order-1));
			sb.append("tmp * ");
			sb.append(s);
			sb.append(";\n");
			casgn(sb, order-1, order-1);
			sb.append("tmp * ");
			sb.append(c);
			sb.append(";\n");
		}
		sb.append("}\n");
	}

	public static void gen(StringBuilder sb, int order) {
		if (order < 1) return;
		float[] c = calcConsts(order);
		gen(sb, order, c, 0);
	}

}
