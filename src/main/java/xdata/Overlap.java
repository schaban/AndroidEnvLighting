// Author: Sergey Chaban <sergey.chaban@gmail.com>

package xdata;

public final class Overlap {

	public static boolean spheres(float[] cA, float rA, float[] cB, float rB) {
		float cds = Calc.distSq(cA, cB);
		float rs = rA + rB;
		return cds <= Calc.sq(rs);
	}

	public static boolean spheres(float[] a, float[] b) {
		float ra = a.length < 4 ? 0.0f : a[3];
		float rb = b.length < 4 ? 0.0f : b[3];
		return spheres(a, ra, b, rb);
	}

	public static boolean boxes(float[] minmaxA, float[] minmaxB) {
		for (int i = 0; i < 3; ++i) {
			/* minA > maxB */
			if (minmaxA[i] > minmaxB[3 + i]) return false;
		}
		for (int i = 0; i < 3; ++i) {
			/* maxA < minB */
			if (minmaxA[3 + i] < minmaxB[i]) return false;
		}
		return true;
	}

	public static boolean boxes(float[] minmaxA, int offsA, float[] minmaxB, int offsB) {
		for (int i = 0; i < 3; ++i) {
			/* minA > maxB */
			if (minmaxA[offsA + i] > minmaxB[offsB + 3 + i]) return false;
		}
		for (int i = 0; i < 3; ++i) {
			/* maxA < minB */
			if (minmaxA[offsA + 3 + i] < minmaxB[offsB + i]) return false;
		}
		return true;
	}

	public static boolean sphereBox(float[] sphC, float sphR, float[] minmax) {
		float[] pos = new float[3];
		Calc.vclamp(pos, sphC, minmax);
		float ds = Calc.distSq(pos, sphC);
		return ds <= Calc.sq(sphR);
	}

	public static boolean sphereBox(float[] sph, float[] minmax) {
		float r = 0.0f;
		if (sph.length >= 4) {
			r = sph[3];
		}
		return sphereBox(sph, r, minmax);
	}

	public static boolean pointSphere(float[] pos, float[] c, float r) {
		float ds = Calc.distSq(pos, c);
		float rs = Calc.sq(r);
		return ds <= rs;
	}

	public static boolean pointSphere(float[] pos, float[] sph) {
		float r = sph.length < 4 ? 0.0f : sph[3];
		return pointSphere(pos, sph, r);
	}

	public static boolean pointBox(float x, float y, float z, float[] minmax, int offs) {
		if (x < minmax[offs + 0] || x > minmax[offs + 3 + 0]) return false;
		if (y < minmax[offs + 1] || y > minmax[offs + 3 + 1]) return false;
		if (z < minmax[offs + 2] || z > minmax[offs + 3 + 2]) return false;
		return true;
	}

	public static boolean segmentBox(
			float p0x, float p0y, float p0z,
			float p1x, float p1y, float p1z,
			float[] minmax, int offs
	) {
		float dirx = p1x - p0x;
		float diry = p1y - p0y;
		float dirz = p1z - p0z;
		float len = Calc.vecLen(dirx, diry, dirz);
		if (len < 1.0e-7f) {
			return pointBox(p0x, p0y, p0z, minmax, offs);
		}

		float tmin = 0.0f;
		float tmax = len;

		float bbminx = minmax[offs];
		float bbmaxx = minmax[offs + 3];
		if (dirx != 0) {
			float dx = len / dirx;
			float x1 = (bbminx - p0x) * dx;
			float x2 = (bbmaxx - p0x) * dx;
			float xmin = Math.min(x1, x2);
			float xmax = Math.max(x1, x2);
			tmin = Math.max(tmin, xmin);
			tmax = Math.min(tmax, xmax);
			if (tmin > tmax) {
				return false;
			}
		} else {
			if (p0x < bbminx || p0x > bbmaxx) return false;
		}

		float bbminy = minmax[offs + 1];
		float bbmaxy = minmax[offs + 3 + 1];
		if (diry != 0) {
			float dy = len / diry;
			float y1 = (bbminy - p0y) * dy;
			float y2 = (bbmaxy - p0y) * dy;
			float ymin = Math.min(y1, y2);
			float ymax = Math.max(y1, y2);
			tmin = Math.max(tmin, ymin);
			tmax = Math.min(tmax, ymax);
			if (tmin > tmax) {
				return false;
			}
		} else {
			if (p0y < bbminy || p0y > bbmaxy) return false;
		}

		float bbminz = minmax[offs + 2];
		float bbmaxz = minmax[offs + 3 + 2];
		if (dirz != 0) {
			float dz = len / dirz;
			float z1 = (bbminz - p0z) * dz;
			float z2 = (bbmaxz - p0z) * dz;
			float zmin = Math.min(z1, z2);
			float zmax = Math.max(z1, z2);
			tmin = Math.max(tmin, zmin);
			tmax = Math.min(tmax, zmax);
			if (tmin > tmax) {
				return false;
			}
		} else {
			if (p0z < bbminz || p0z > bbmaxz) return false;
		}

		if (tmax > len) return false;
		return true;
	}

	public static boolean segmentBox(Vec p0, Vec p1, float[] minmax, int offs) {
		return segmentBox(p0.x(), p0.y(), p0.z(), p1.x(), p1.y(), p1.z(), minmax, offs);
	}

}
