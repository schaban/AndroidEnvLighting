// Author: Sergey Chaban <sergey.chaban@gmail.com>

package xdata;

public class Sphere {
	public float[] mCenterRadius;

	public Sphere() {
		mCenterRadius = new float[4];
	}

	public Sphere(float x, float y, float z, float r) {
		this();
		set(x, y, z, r);
	}

	public void set(float... data) {
		Calc.vcpy(mCenterRadius, data);
	}

	public void read(Binary bin, int org) {
		for (int i = 0; i < 4; ++i) {
			mCenterRadius[i] = bin.getF32(org + i*4);
		}
	}

	public Vec getCenter() {
		Vec c = new Vec();
		Calc.vcpy(c.el, mCenterRadius);
		return c;
	}

	public float cx() { return mCenterRadius[0]; }
	public float cy() { return mCenterRadius[1]; }
	public float cz() { return mCenterRadius[2]; }
	public float r() { return mCenterRadius[3]; }

	public boolean contains(Vec pos) {
		return Overlap.pointSphere(pos.el, mCenterRadius);
	}

	public boolean overlaps(Sphere sph) {
		return Overlap.spheres(mCenterRadius, sph.mCenterRadius);
	}

	public boolean overlaps(AABB box) {
		return Overlap.sphereBox(mCenterRadius, box.mMinMax);
	}
}
