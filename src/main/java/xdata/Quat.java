// Author: Sergey Chaban <sergey.chaban@gmail.com>

package xdata;

public class Quat {
	public final float[] el;

	public Quat() {
		el = new float[4];
	}

	public float x() { return el[0]; }
	public float y() { return el[1]; }
	public float z() { return el[2]; }
	public float w() { return el[3]; }
	public void x(float val) { el[0] = val; }
	public void y(float val) { el[1] = val; }
	public void z(float val) { el[2] = val; }
	public void w(float val) { el[3] = val; }

	public void read(Binary bin, int org) {
		bin.getFloats(el, org);
	}

	public void set(float... data) {
		Calc.vcpy(0.0f, 0, el, 0, data);
	}

	public void identity() {
		set(0, 0, 0, 1);
	}

	public Vec getV() {
		return new Vec(el);
	}

	public float getS() {
		return el[3];
	}

	public void setS(float s) {
		el[3] = s;
	}

	public void setRX(float rad) {
		float h = rad / 2;
		set(Calc.sinf(h), 0, 0, Calc.cosf(h));
	}

	public void setRY(float rad) {
		float h = rad / 2;
		set(0, Calc.sinf(h), 0, Calc.cosf(h));
	}

	public void setRZ(float rad) {
		float h = rad / 2;
		set(0, 0, Calc.sinf(h), Calc.cosf(h));
	}

	public void mulRX(float rad) {
		float h = rad / 2;
		mul(Calc.sinf(h), 0, 0, Calc.cosf(h));
	}

	public void mulRY(float rad) {
		float h = rad / 2;
		mul(0, Calc.sinf(h), 0, Calc.cosf(h));
	}

	public void mulRZ(float rad) {
		float h = rad / 2;
		mul(0, 0, Calc.sinf(h), Calc.cosf(h));
	}

	public void setDX(float deg) {
		setRX(Calc.radians(deg));;
	}

	public void setDY(float deg) {
		setRY(Calc.radians(deg));;
	}

	public void setDZ(float deg) {
		setRZ(Calc.radians(deg));;
	}

	public void mul(Quat q, Quat p) {
		set(q.el);
		mul(p);
	}

	public void mul(Quat p) {
		mul(p.el[0], p.el[1], p.el[2], p.el[3]);
	}

	public void mul(float x, float y, float z, float w) {
		float qx = el[0];
		float qy = el[1];
		float qz = el[2];
		float sq = getS();
		float sp = w;
		Calc.cross(el, el, x, y, z);
		el[0] += qx*sp + x*sq;
		el[1] += qy*sp + y*sq;
		el[2] += qz*sp + z*sq;
		float d = qx*x + qy*y + qz*z;
		float s = sq*sp - d;
		setS(s);
	}

	public void setRadians(float rx, float ry, float rz, RotOrd ord) {
		switch (ord) {
			case XYZ: setRZ(rz); mulRY(ry); mulRX(rx); break;
			case XZY: setRY(ry); mulRZ(rz); mulRX(rx); break;
			case YXZ: setRZ(rz); mulRX(rx); mulRY(ry); break;
			case YZX: setRX(rx); mulRZ(rz); mulRY(ry); break;
			case ZXY: setRY(ry); mulRX(rx); mulRZ(rz); break;
			case ZYX: setRX(rx); mulRY(ry); mulRZ(rz); break;
			default: identity(); break;
		}
	}

	public void setDegrees(float dx, float dy, float dz, RotOrd ord) {
		setRadians(Calc.radians(dx), Calc.radians(dy), Calc.radians(dz), ord);
	}

	public float dot(Quat q) {
		return Calc.inner(el, q.el);
	}

}
