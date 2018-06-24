// Author: Sergey Chaban <sergey.chaban@gmail.com>

package flow;

import xdata.*;

public final class Obstacle {

	public static class PolyWk {
		public static final int PLANE = 0;
		public static final int NRM = PLANE;
		public static final int PLANE_A = PLANE;
		public static final int PLANE_B = PLANE + 1;
		public static final int PLANE_C = PLANE + 2;
		public static final int PLANE_D = PLANE + 3;
		public static final int BBOX = 4;
		public static final int BBOX_MIN = BBOX;
		public static final int BBOX_MAX = BBOX + 3;
		public static final int V0 = BBOX + 6;
		public static final int V1 = V0 + 3;
		public static final int V2 = V1 + 3;
		public static final int V3 = V2 + 3;
		public static final int TMP_VEC_A = V3 + 3;
		public static final int TMP_VEC_B = TMP_VEC_A + 3;
		public static final int TMP_VEC_C = TMP_VEC_B + 3;
		public static final int FSIZE = TMP_VEC_C + 3;

		public final float[] f = new float[FSIZE];
		public final int[] mPolVtxLst = new int[4];
		public int mLstIdx = -1;
		public int mGeoIdx = -1;
		public boolean mTriFlg = false;

		public void init(XGeo geo, int lstIdx, int geoIdx) {
			mLstIdx = lstIdx;
			mGeoIdx = geoIdx;
			int nvtx = geo.getPolVtxNum(geoIdx);
			if (nvtx == 3) {
				mTriFlg = true;
				geo.getPolVtxLst(mPolVtxLst, geoIdx);
				geo.getPnt(f, V0, mPolVtxLst[2]);
				geo.getPnt(f, V1, mPolVtxLst[1]);
				geo.getPnt(f, V2, mPolVtxLst[0]);
				geo.getPnt(f, V3, mPolVtxLst[0]);
			} else if (nvtx == 4 && geo.allQuadsPlanarConvex()) {
				mTriFlg = false;
				geo.getPolVtxLst(mPolVtxLst, geoIdx);
				geo.getPnt(f, V0, mPolVtxLst[3]);
				geo.getPnt(f, V1, mPolVtxLst[2]);
				geo.getPnt(f, V2, mPolVtxLst[1]);
				geo.getPnt(f, V3, mPolVtxLst[0]);
			} else {
				mLstIdx = -1;
				mGeoIdx = -1;
				return;
			}
			Calc.triNormalCCW(f, NRM, f, V0, V1, V2);
			f[PLANE_D] = Calc.inner(f, V0, f, NRM, 3);
			System.arraycopy(f, V0, f, BBOX_MIN, 3);
			System.arraycopy(f, V0, f, BBOX_MAX, 3);
			for (int ivtx = 1; ivtx < nvtx; ++ivtx) {
				for (int i = 0; i < 3; ++i) {
					float el = f[V0 + ivtx*3 + i];
					f[BBOX_MIN + i] = Math.min(f[BBOX_MIN + i], el);
					f[BBOX_MAX + i] = Math.max(f[BBOX_MAX + i], el);
				}
			}
		}

		public boolean valid() {
			return mLstIdx >= 0;
		}

		public int getVtxNum() {
			if (mLstIdx < 0) return 0;
			return mTriFlg ? 3 : 4;
		}

		public int getVtxOffs(int i) {
			return V0 + i*3;
		}

		public int getNextLst() {
			return mTriFlg ? 0x1021 : 0x0321;
		}

		public float getMinY() {
			return f[BBOX_MIN + 1];
		}

		public float getMaxY() {
			return f[BBOX_MAX + 1];
		}

		public boolean isInYRange(float y) {
			return y >= getMinY() && y <= getMaxY();
		}

		public boolean isPntInside(float[] pnt, int pntOffs) {
			int nvtx = getVtxNum();
			if (nvtx <= 0) return false;
			int next = getNextLst();
			for (int ivtx = 0; ivtx < nvtx; ++ivtx) {
				int offs0 = V0 + ivtx*3;
				int offs1 = V0 + (next & 3)*3;
				Calc.vsub(f, TMP_VEC_A, f, offs1, f, offs0, 3);
				Calc.vsub(f, TMP_VEC_B, pnt, pntOffs, f, offs0, 3);
				Calc.cross(f, TMP_VEC_C, f, TMP_VEC_A, f, TMP_VEC_B);
				float d = Calc.inner(f, TMP_VEC_C, f, NRM, 3);
				if (d < 0.0f) return false;
				next >>>= 4;
			}
			return true;
		}

		public float getPlaneSignedDist(float[] pos, int posOffs) {
			return Calc.planeSignedDist(pos, posOffs, f, PLANE);
		}

		public boolean intersectSegPlane(float[] p0, int p0Offs, float[] p1, int p1Offs, float[] isect, int isectOffs) {
			float p0x = p0[p0Offs];
			float p0y = p0[p0Offs + 1];
			float p0z = p0[p0Offs + 2];
			float p1x = p1[p1Offs];
			float p1y = p1[p1Offs + 1];
			float p1z = p1[p1Offs + 2];
			float A = f[PLANE_A];
			float B = f[PLANE_B];
			float C = f[PLANE_C];
			float D = f[PLANE_D];
			float t = Intersect.linePlane(p0x, p0y, p0z, p1x, p1y, p1z, A, B, C, D);
			boolean hitFlg = t >= 0.0f && t <= 1.0f;
			if (hitFlg) {
				isect[isectOffs] = Calc.lerp(p0x, p1x, t);
				isect[isectOffs + 1] = Calc.lerp(p0y, p1y, t);
				isect[isectOffs + 2] = Calc.lerp(p0z, p1z, t);
			}
			return hitFlg;
		}

	}

	public static class BallWk {
		public static final int NEW_POS = 0;
		public static final int OLD_POS = 3;
		public static final int ADJ_POS = 6;
		public static final int ISECT = 9;
		public static final int ADJ_VEC = 12;
		public static final int TMP = 15;
		public static final int TMP_VEC_A = TMP;
		public static final int TMP_VEC_B = TMP_VEC_A + 3;
		public static final int TMP_VEC_C = TMP_VEC_B + 3;
		public static final int FSIZE = TMP_VEC_C + 3;

		public final float[] f = new float[FSIZE];
		public int mAdjGeoIdx = -1;
		public int mState = 0;
		public int mObstState = 0;
		public int mAdjCount = 0;
		public float mRadius;
		public float mRange;
		public float mSqDist;

		public void getNewPos(Vec v) {
			System.arraycopy(f, NEW_POS, v.el, 0, 3);
		}

		public float getNewPosX() {
			return f[NEW_POS];
		}

		public float getNewPosY() {
			return f[NEW_POS + 1];
		}

		public float getNewPosZ() {
			return f[NEW_POS + 2];
		}

		public void init(Vec newPos, Vec oldPos, float radius, float range) {
			System.arraycopy(newPos.el, 0, f, NEW_POS, 3);
			System.arraycopy(oldPos.el, 0, f, OLD_POS, 3);
			System.arraycopy(newPos.el, 0, f, ADJ_POS, 3);
			mRadius = radius;
			mRange = range;
			mSqDist = Float.MAX_VALUE;
			mAdjGeoIdx = -1;
			mState = 0;
			mObstState = 0;
		}

		public void adjust(PolyWk poly, XGeo.RangeQuery rangeQry, float margin) {
			if (!poly.valid()) return;
			float sdist = poly.getPlaneSignedDist(f, NEW_POS);
			float adist = Math.abs(sdist);
			int[] polLst = rangeQry.getPolList();
			if (sdist > 0.0f && adist > mRadius) {
				if (adist > mRange) {
					polLst[poly.mLstIdx] = -polLst[poly.mLstIdx];
				}
				return;
			}
			if (adist > mRadius) {
				if (adist > mRange) {
					polLst[poly.mLstIdx] = -polLst[poly.mLstIdx];
				}
				if (!poly.intersectSegPlane(f, NEW_POS, f, OLD_POS, f, ISECT)) {
					return;
				}
			} else {
				System.arraycopy(f, NEW_POS, f, ISECT, 3);
				if (sdist > 0.0f) {
					for (int i = 0; i < 3; ++i) {
						f[ISECT + i] -= poly.f[PolyWk.NRM + i] * adist;
					}
				} else {
					for (int i = 0; i < 3; ++i) {
						f[ISECT + i] += poly.f[PolyWk.NRM + i] * adist;
					}
				}
			}
			float dist2;
			float r = mRadius;
			final float eps = 1e-6f;
			if (poly.isPntInside(f, ISECT)) {
				mState = 1;
				adist = (r - adist) + margin;
				System.arraycopy(poly.f, PolyWk.NRM, f, ADJ_VEC, 3);
				if (sdist <= 0.0f) {
					for (int i = 0; i < 3; ++i) {
						f[ADJ_VEC + i] = -f[ADJ_VEC + i];
					}
				}
				for (int i = 0; i < 3; ++i) {
					f[ADJ_VEC + i] *= adist;
				}
			} else {
				if (mState == 1) {
					return;
				}
				for (int i = 0; i < 3; ++i) {
					f[TMP_VEC_A + i] = 0.0f;
				}
				float mov = 0.0f;
				float rr = Calc.sq(r);
				adist = Float.MAX_VALUE;
				boolean flg = false;
				int nvtx = poly.getVtxNum();
				int next = poly.getNextLst();
				for (int ivtx = 0; ivtx < nvtx; ++ivtx) {
					Calc.vsub(f, TMP_VEC_B, f, NEW_POS, poly.f, poly.getVtxOffs(ivtx), 3);
					dist2 = Calc.lenSq(f, TMP_VEC_B, 3);
					if (dist2 <= rr && dist2 < adist) {
						System.arraycopy(f, TMP_VEC_B, f, TMP_VEC_A, 3);
						Calc.normalize(f, TMP_VEC_A, 3);
						mov = Calc.sqrtf(dist2);
						Calc.vsub(f, TMP_VEC_C, f, OLD_POS, poly.f, poly.getVtxOffs(ivtx), 3);
						if (Calc.inner(f, TMP_VEC_A, f, TMP_VEC_C, 3) < 0.0f) {
							for (int i = 0; i < 3; ++i) {
								f[TMP_VEC_A + i] = -f[TMP_VEC_A + i];
							}
						} else {
							mov = -mov;
						}
						flg = true;
					}
					Calc.vsub(f, TMP_VEC_C, poly.f, poly.getVtxOffs(next & 3), poly.f, poly.getVtxOffs(ivtx), 3);
					dist2 = Calc.lenSq(f, TMP_VEC_C, 3);
					if (dist2 > eps) {
						float nd = Calc.inner(f, TMP_VEC_B, f, TMP_VEC_C, 3);
						if (nd >= eps && dist2 >= nd) {
							System.arraycopy(f, ISECT, poly.f, poly.getVtxOffs(ivtx), 3);
							float s = nd / dist2;
							for (int i = 0; i < 3; ++i) {
								f[ISECT + i] += f[TMP_VEC_C + i] * s;
							}
							Calc.vsub(f, TMP_VEC_C, f, NEW_POS, f, ISECT, 3);
							dist2 = Calc.lenSq(f, TMP_VEC_C, 3);
							if (dist2 <= rr && dist2 < adist) {
								mov = Calc.sqrtf(dist2);
								System.arraycopy(f, TMP_VEC_C, f, TMP_VEC_A, 3);
								Calc.normalize(f, TMP_VEC_A, 3);
								Calc.vsub(f, TMP_VEC_C, f, OLD_POS, f, ISECT, 3);
								if (Calc.inner(f, TMP_VEC_A, f, TMP_VEC_C, 3) < 0.0f) {
									for (int i = 0; i < 3; ++i) {
										f[TMP_VEC_A + i] = -f[TMP_VEC_A + i];
									}
								} else {
									mov = -mov;
								}
								adist = dist2;
								flg = true;
							}
						}
					}
					next >>>= 4;
				}
				if (flg) {
					mState = 2;
					float s = r + mov + margin;
					for (int i = 0; i < 3; ++i) {
						f[ADJ_VEC + i] = f[TMP_VEC_A + i] * s;
					}
				} else {
					return;
				}
			}
			Calc.vadd(f, ADJ_VEC, f, NEW_POS, 3);
			dist2 = Calc.distSq(f, ADJ_VEC, f, OLD_POS, 3);
			if (dist2 > mSqDist) {
				if (mObstState == 1) return;
				if (mState != 1) return;
			}
			mObstState = mState;
			mSqDist = dist2;
			System.arraycopy(f, ADJ_VEC, f, ADJ_POS, 3);
			mAdjGeoIdx = poly.mGeoIdx;
		}

		public boolean adjust(Vec newPos, Vec oldPos, float radius, XGeo.RangeQuery rangeQry, PolyWk poly, float margin) {
			mAdjCount = 0;
			float maxRange = radius + Calc.sqrtf(Vec.distSq(oldPos, newPos))*2.0f;
			float xmin = newPos.x() - maxRange;
			float ymin = newPos.y() - maxRange;
			float zmin = newPos.z() - maxRange;
			float xmax = newPos.x() + maxRange;
			float ymax = newPos.y() + maxRange;
			float zmax = newPos.z() + maxRange;
			rangeQry.exec(xmin, ymin, zmin, xmax, ymax, zmax);
			int npol = rangeQry.getPolListSize();
			if (npol <= 0) return false;
			int[] polLst = rangeQry.getPolList();
			init(newPos, oldPos, radius, maxRange);
			int adjCount = 0;
			final int itrMax = 15;
			int itrCnt = -1;
			do {
				++itrCnt;
				for (int i = 0; i < npol; ++i) {
					int wkPolIdx = Math.abs(polLst[i]);
					if (mAdjGeoIdx != wkPolIdx) {
						poly.init(rangeQry.getGeo(), i, wkPolIdx);
						boolean calcFlg = true;
						if (itrCnt > 0) {
							calcFlg = polLst[i] >= 0;
						} else {
							float ny = newPos.y();
							if (!poly.isInYRange(ny)) {
								polLst[i] = -polLst[i];
								calcFlg = false;
							}
						}
						if (calcFlg) {
							adjust(poly, rangeQry, margin);
						}
					}
				}
				if (mState != 0) {
					System.arraycopy(f, ADJ_POS, f, NEW_POS, 3);
					++adjCount;
				}
			} while (mState != 0 && itrCnt < itrMax);
			mAdjCount = adjCount;
			return adjCount > 0;
		}

		public boolean adjust(Vec newPos, Vec oldPos, float radius, XGeo.RangeQuery rangeQry, PolyWk poly) {
			return adjust(newPos, oldPos, radius, rangeQry, poly, 0.005f);
		}
	}

}
