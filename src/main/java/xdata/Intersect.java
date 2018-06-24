// Author: Sergey Chaban <sergey.chaban@gmail.com>

package xdata;

public final class Intersect {

	public static class SegPolyWk {
		/*
			 0: p0
			 3: p1
			 6: v0
			 9: v1
			12: v2
			15: v3
			18: vec0
			21: vec1
			24: vec2
			27: vec3
			30: edge0
			33: edge1
			36: edge2
			39: edge3
			42: dir
			45: nrm
			48: pos
			51: tmp
			54: t
		 */
		public static final int P0 = 0;
		public static final int P1 = 3;
		public static final int V0 = 6;
		public static final int V1 = 9;
		public static final int V2 = 12;
		public static final int V3 = 15;
		public static final int VEC0 = 18;
		public static final int VEC1 = 21;
		public static final int VEC2 = 24;
		public static final int VEC3 = 27;
		public static final int EDGE0 = 30;
		public static final int EDGE1 = 33;
		public static final int EDGE2 = 36;
		public static final int EDGE3 = 39;
		public static final int DIR = 42;
		public static final int NRM = 45;
		public static final int POS = 48;
		public static final int TMP = 51;
		public static final int T = 54;

		public final float[] f;

		public SegPolyWk() {
			f = new float[T + 1];
		}

		public void setSeg(float x0, float y0, float z0, float x1, float y1, float z1) {
			f[P0] = x0; f[P0 + 1] = y0; f[P0 + 2] = z0;
			f[P1] = x1; f[P1 + 1] = y1; f[P1 + 2] = z1;
		}

		public void setSeg(Vec p0, Vec p1) {
			setSeg(p0.x(), p0.y(), p0.z(), p1.x(), p1.y(), p1.z());
		}

		public void setVtx(int idx, float x, float y, float z) {
			int offs = V0 + idx*3;
			f[offs] = x;
			f[offs + 1] = y;
			f[offs + 2] = z;
		}

		public void setVtx(int idx, Vec v) {
			setVtx(idx, v.x(), v.y(), v.z());
		}

		public void setQuad(Vec v0, Vec v1, Vec v2, Vec v3) {
			setVtx(0, v0);
			setVtx(1, v1);
			setVtx(2, v2);
			setVtx(3, v3);
		}

		public void setTri(Vec v0, Vec v1, Vec v2) {
			setVtx(0, v0);
			setVtx(1, v1);
			setVtx(2, v2);
			setVtx(3, v0);
		}

		public void setNrm(float x, float y, float z) {
			f[NRM] = x;
			f[NRM + 1] = y;
			f[NRM + 2] = z;
		}

		public void setNrm(Vec nrm) {
			setNrm(nrm.x(), nrm.y(), nrm.z());
		}

		public void calcVec(int idx) {
			int offs = idx * 3;
			int ivec = VEC0 + offs;
			int ivtx = V0 + offs;
			for (int i = 0; i < 3; ++i) {
				f[ivec + i] = f[i] - f[ivtx + i];
			}
		}

		public void calcEdge(int idx) {
			int offs = idx * 3;
			int iedge = EDGE0 + offs;
			int iv = V0 + offs;
			int ivnext = idx < 3 ? iv + 3 : V0;
			for (int i = 0; i < 3; ++i) {
				f[iedge + i] = f[ivnext + i] - f[iv + i];
			}
		}

		public void calcDir() {
			for (int i = 0; i < 3; ++i) {
				f[DIR + i] = f[P1 + i] - f[P0 + i]; // dir = p1 - p0
			}
		}

		public void calcNrm() {
			for (int i = 0; i < 3; ++i) {
				f[TMP + i] = f[V2 + i] - f[V0 + i]; // tmp = v2 - v0
			}
			// req: edge0
			Calc.cross(f, NRM, f, EDGE0, f, TMP); // N = edge0 x (v2 - v0)
			Calc.normalize(f, NRM, 3);
		}

		public float calcTriple(int idx) {
			Calc.cross(f, TMP, f,EDGE0 + idx*3, f, DIR);
			return Calc.inner(f, TMP, f, VEC0 + idx*3, 3);
		}

		public boolean calcIntersection(boolean isTri, boolean extNrm) {
			f[T] = -1.0f;
			calcEdge(0);
			if (!extNrm) {
				calcNrm();
			}
			calcVec(0);
			float d0 = Calc.inner(f, NRM, f, VEC0, 3);
			for (int i = 0; i < 3; ++i) {
				f[TMP + i] = f[P1 + i] - f[V0 + i]; // tmp = p1 - v0
			}
			float d1 = Calc.inner(f, NRM, f, TMP, 3);
			if (d0*d1 > 0.0f || (d0 == 0.0f && d1 == 0.0f)) return false;
			int n = isTri ? 3 : 4;
			for (int i = 1; i < n; ++i) {
				calcEdge(i);
			}
			for (int i = 1; i < n; ++i) {
				calcVec(i);
			}
			calcDir();
			for (int i = 0; i < n; ++i) {
				if (calcTriple(i) < 0.0f) return false;
			}
			float d = Calc.inner(f, NRM, f, DIR, 3);
			float t;
			if (d == 0.0f || d0 == 0.0f) {
				t = 0.0f;
			} else {
				t = -d0 / d;
			}
			f[T] = t;
			if (t > 1.0f || t < 0.0f) return false;
			for (int i = 0; i < 3; ++i) {
				f[POS + i] = f[P0 + i] + f[DIR + i]*t;
			}
			return true;
		}

		public float getSegP0X() {
			return f[P0];
		}

		public float getSegP0Y() {
			return f[P0 + 1];
		}

		public float getSegP0Z() {
			return f[P0 + 2];
		}

		public float getSegP1X() {
			return f[P1];
		}

		public float getSegP1Y() {
			return f[P1 + 1];
		}

		public float getSegP1Z() {
			return f[P1 + 2];
		}

		public void getHitPos(Vec pos) {
			for (int i = 0; i < 3; ++i) {
				pos.el[i] = f[POS + i];
			}
		}

		public void getHitNrm(Vec nrm) {
			for (int i = 0; i < 3; ++i) {
				nrm.el[i] = f[NRM + i];
			}
		}

		public float getHitDistSq() {
			return Calc.distSq(f, P0, f, POS, 3);
		}
	}


	public static float segPlane(
			float x0, float y0, float z0,
			float x1, float y1, float z1,
			float A, float B, float C, float D
	) {
		float dx = x1 - x0;
		float dy = y1 - y0;
		float dz = z1 - z0;
		float dn = dx*A + dy*B + dz*C;
		return Calc.div0(D - (x0*A + y0*B + z0*C), dn);
	}

	public static boolean segQuadCCW(Vec p0, Vec p1, Vec v0, Vec v1, Vec v2, Vec v3, Vec hitPos, Vec hitNrm) {
		SegPolyWk wk = new SegPolyWk();
		wk.setSeg(p0, p1);
		wk.setQuad(v0, v1, v2, v3);
		boolean hit = wk.calcIntersection(false, false);
		if (hit) {
			if (hitPos != null) {
				wk.getHitPos(hitPos);
			}
			if (hitNrm != null) {
				wk.getHitNrm(hitNrm);
			}
		}
		return hit;
	}

	public static boolean segQuadNrmCCW(Vec p0, Vec p1, Vec v0, Vec v1, Vec v2, Vec v3, Vec n, Vec hitPos) {
		SegPolyWk wk = new SegPolyWk();
		wk.setSeg(p0, p1);
		wk.setQuad(v0, v1, v2, v3);
		wk.setNrm(n);
		boolean hit = wk.calcIntersection(false, true);
		if (hit) {
			if (hitPos != null) {
				wk.getHitPos(hitPos);
			}
		}
		return hit;
	}

	public static boolean segQuadCW(Vec p0, Vec p1, Vec v0, Vec v1, Vec v2, Vec v3, Vec hitPos, Vec hitNrm) {
		return segQuadCCW(p0, p1, v3, v2, v1, v0, hitPos, hitNrm);
	}

	public static boolean segQuadNrmCW(Vec p0, Vec p1, Vec v0, Vec v1, Vec v2, Vec v3, Vec n, Vec hitPos) {
		return segQuadNrmCCW(p0, p1, v3, v2, v1, v0, n, hitPos);
	}

	public static boolean segTriCCW(Vec p0, Vec p1, Vec v0, Vec v1, Vec v2, Vec hitPos, Vec hitNrm) {
		SegPolyWk wk = new SegPolyWk();
		wk.setSeg(p0, p1);
		wk.setTri(v0, v1, v2);
		boolean hit = wk.calcIntersection(true, false);
		if (hit) {
			if (hitPos != null) {
				wk.getHitPos(hitPos);
			}
			if (hitNrm != null) {
				wk.getHitNrm(hitNrm);
			}
		}
		return hit;
	}

	public static boolean segTriCW(Vec p0, Vec p1, Vec v0, Vec v1, Vec v2, Vec hitPos, Vec hitNrm) {
		return segTriCCW(p0, p1, v2, v1, v0, hitPos, hitNrm);
	}

	public static float linePlane(
			float p0x, float p0y, float p0z,
			float p1x, float p1y, float p1z,
			float plnA, float plnB, float plnC, float plnD
	) {
		float dx = p1x - p0x;
		float dy = p1y - p0y;
		float dz = p1z - p0z;
		float dn = dx*plnA + dy*plnB + dz*plnC;
		float np = plnA*p0x + plnB*p0y + plnC*p0z;
		float t = Calc.div0(plnD - np, dn);
		return t;
	}

}
