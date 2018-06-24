// Author: Sergey Chaban <sergey.chaban@gmail.com>

package xdata;

public class Vec {
	public final float[] el;

	public Vec() {
		el = new float[3];
	}

	public Vec(Vec v) {
		this();
		set(v.el);
	}

	public Vec(float... data) {
		this();
		set(data);
	}

	public float x() { return el[0]; }
	public float y() { return el[1]; }
	public float z() { return el[2]; }
	public void x(float val) { el[0] = val; }
	public void y(float val) { el[1] = val; }
	public void z(float val) { el[2] = val; }

	public void read(Binary bin, int org) {
		bin.getFloats(el, org);
	}

	public void fill(float s) {
		Calc.vfill(el, s);
	}

	public void zero() {
		fill(0.0f);
	}

	public void set(float... data) {
		Calc.vcpy(0.0f, 0, el, 0, data);
	}

	public void set(Vec v) {
		set(v.el);
	}

	public void scl(float s) {
		Calc.vscl(el, s);
	}

	public float length() {
		return Calc.magnitude(el);
	}

	public float lengthSq()	{
		return Calc.inner(el, el);
	}

	public void normalize() {
		Calc.normalize(el);
	}

	public void normalize(Vec v) {
		set(v.el);
		Calc.normalize(el);
	}

	public Vec getNormalized() {
		Vec v = new Vec(this);
		v.normalize();
		return v;
	}

	public void neg() {
		for (int i = 0; i < 3; ++i) {
			el[i] = -el[i];
		}
	}

	public void neg(Vec v) {
		for (int i = 0; i < 3; ++i) {
			el[i] = -v.el[i];
		}
	}

	public void addX(float x) {
		el[0] += x;
	}

	public void addY(float y) {
		el[1] += y;
	}

	public void addZ(float z) {
		el[2] += z;
	}

	public void add(int offs, float[] vec) {
		Calc.vadd(el, 0, vec, offs);
	}

	public void add(Vec v) {
		add(0, v.el);
	}

	public void add(float... vals) {
		add(0, vals);
	}

	public void sub(int offs, float[] vec) {
		Calc.vsub(el, 0, vec, offs);
	}

	public void sub(Vec v) {
		sub(0, v.el);
	}

	public void sub(float... vals) {
		sub(0, vals);
	}

	public void div0(Vec a, Vec b) {
		for (int i = 0; i < 3; ++i) {
			el[i] = Calc.div0(a.el[i], b.el[i]);
		}
	}

	public float dot(Vec v) {
		return Calc.inner(el, v.el);
	}

	public void cross(Vec a, Vec b) {
		Calc.cross(el, a.el, b.el);
	}

	public void cross(Vec b) {
		Calc.cross(el, el, b.el);
	}


	public static float distSq(Vec a, Vec b) {
		float dx = b.el[0] - a.el[0];
		float dy = b.el[1] - a.el[1];
		float dz = b.el[2] - a.el[2];
		return dx*dx + dy*dy + dz*dz;
	}
}
