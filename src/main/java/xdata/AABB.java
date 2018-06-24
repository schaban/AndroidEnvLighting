// Author: Sergey Chaban <sergey.chaban@gmail.com>

package xdata;

public class AABB {
	public float[] mMinMax;

	public AABB() {
		mMinMax = new float[6];
	}

	public AABB(AABB box) {
		this();
		Calc.vcpy(mMinMax, box.mMinMax);
	}

	public void read(Binary bin, int org) {
		for (int i = 0; i < 6; ++i) {
			mMinMax[i] = bin.getF32(org + i*4);
		}
	}

	public void reset() {
		float f = Float.MAX_VALUE;
		mMinMax[0] = f;
		mMinMax[1] = f;
		mMinMax[2] = f;
		mMinMax[0] = -f;
		mMinMax[1] = -f;
		mMinMax[2] = -f;
	}

	public void getCenter(float[] dst, int offs) {
		Calc.vadd(dst, offs, mMinMax, 0, mMinMax, 3, 3);
		Calc.vscl(dst, 0.5f, offs, 3);
	}

	public Vec getCenter() {
		Vec c = new Vec();
		getCenter(c.el, 0);
		return c;
	}

	public void getSize(float[] dst, int offs) {
		getMax(dst, offs);
		Calc.vsub(dst, offs, mMinMax, 0);
	}

	public Vec getSize() {
		Vec v = new Vec();
		getSize(v.el, 0);
		return v;
	}

	public void getMin(float[] dst, int offs) {
		Calc.vcpy(offs, dst, 0, mMinMax);
	}

	public Vec getMin() {
		Vec v = new Vec();
		getMin(v.el, 0);
		return v;
	}

	public void getMax(float[] dst, int offs) {
		Calc.vcpy(offs, dst, 3, mMinMax);
	}

	public Vec getMax() {
		Vec v = new Vec();
		getMax(v.el, 0);
		return v;
	}

	public boolean overlaps(AABB box) {
		return Overlap.boxes(mMinMax, box.mMinMax);
	}

	public boolean overlaps(Sphere sph) {
		return Overlap.sphereBox(sph.mCenterRadius, mMinMax);
	}
}
