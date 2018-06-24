// Author: Sergey Chaban <sergey.chaban@gmail.com>

package xdata;

public class Mtx {
	public final float[] el;

	public Mtx() {
		el = new float[4*4];
	}

	public void set(float... data) {
		Calc.vcpy(0.0f, 0, el, 0, data);
	}

	public void cpy(Mtx m) {
		if (this != m) {
			set(m.el);
		}
	}

	public void identity() {
		Calc.identity(el, 4);
	}

	public void identitySR() {
		Calc.vfill(el, 0.0f, 3*4);
		el[0] = 1.0f;
		el[4 + 1] = 1.0f;
		el[8 + 2] = 1.0f;
	}

	public void zero() {
		Calc.vfill(el, 0.0f);
	}

	public void read(Binary bin, int org) {
		bin.getFloats(el, org);
	}

	public void invert(Mtx m, int[] iwk) {
		if (this != m) {
			cpy(m);
		}
		Calc.invGJ(el, 4, iwk);
	}

	public void invert(Mtx m) {
		final int[] iwk = new int[4*3];
		invert(m, iwk);
	}

	public void invert(int[] iwk) {
		boolean ok = Calc.invGJ(el, 4, iwk);
		if (!ok) {
			zero();
		}
	}

	public void invert() {
		final int[] iwk = new int[4*3];
		invert(iwk);
	}

	public void transpose() {
		Calc.transpose(el, 4);
	}

	public void transpose(Mtx m) {
		set(m.el);
		transpose();
	}

	public void mul(Mtx m) {
		float[] tm = new float[4*4];
		Calc.mulMM(tm, el, m.el, 4, 4, 4);
		set(tm);
	}

	public void mul(Mtx a, Mtx b) {
		if (this != a && this != b) {
			Calc.mulMM(el, a.el, b.el, 4, 4, 4);
		} else {
			float[] tm = new float[4*4];
			Calc.mulMM(tm, a.el, b.el, 4, 4, 4);
			set(tm);
		}
	}

	public void setTranslation(float x, float y, float z) {
		el[12] = x;
		el[12 + 1] = y;
		el[12 + 2] = z;
		el[12 + 3] = 1.0f;
	}

	public void setTranslation(Vec v) {
		setTranslation(v.x(), v.y(), v.z());
	}

	public void getTranslation(float[] pos, int posOffs) {
		for (int i = 0; i < 3; ++i) {
			pos[posOffs + i] = el[12 + i];
		}
	}

	public void getTranslation(float[] pos) {
		getTranslation(pos, 0);
	}

	public Vec getTranslation() {
		Vec pos = new Vec();
		getTranslation(pos.el);
		return pos;
	}

	public void setAxisAngle(Vec axis, float ang) {
		float x = axis.x();
		float y = axis.y();
		float z = axis.z();
		float xx = x*x;
		float yy = y*y;
		float zz = z*z;
		float xy = x*y;
		float xz = x*z;
		float yz = y*z;
		float s = Calc.sinf(ang);
		float c = Calc.cosf(ang);
		float t = 1.0f - c;
		el[0] = t*xx + c;
		el[1] = t*xy + s*z;
		el[2] = t*xz - s*y;
		el[3] = 0.0f;
		el[4] = t*xy - s*z;
		el[4 + 1] = t*yy + c;
		el[4 + 2] = t*yz + s*x;
		el[4 + 3] = 0.0f;
		el[8] = t*xz + s*y;
		el[8 + 1] = t*yz - s*x;
		el[8 + 2] = t*zz + c;
		el[8 + 3] = 0.0f;
		setTranslation(0.0f, 0.0f, 0.0f);
	}

	public void setRot(Vec axis, float ang) {
		setAxisAngle(axis.getNormalized(), ang);
	}

	public void setRotN(Vec axis, float ang) {
		setAxisAngle(axis, ang);
	}

	public void setRadians(float rx, float ry, float rz, RotOrd ord) {
		final float[] m0 = new float[3*3];
		final float[] m1 = new float[3*3];
		final float[] m2 = new float[3*3];
		final float[] m3 = new float[3*3];
		switch (ord) {
			case XYZ:
				Calc.setRowRX(m1, 3, rx);
				Calc.setRowRY(m2, 3, ry);
				Calc.setRowRZ(m3, 3, rz);
				break;
			case XZY:
				Calc.setRowRX(m1, 3, rx);
				Calc.setRowRZ(m2, 3, rz);
				Calc.setRowRY(m3, 3, ry);
				break;
			case YXZ:
				Calc.setRowRY(m1, 3, ry);
				Calc.setRowRX(m2, 3, rx);
				Calc.setRowRZ(m3, 3, rz);
				break;
			case YZX:
				Calc.setRowRY(m1, 3, ry);
				Calc.setRowRZ(m2, 3, rz);
				Calc.setRowRX(m3, 3, rx);
				break;
			case ZXY:
				Calc.setRowRZ(m1, 3, rz);
				Calc.setRowRX(m2, 3, rx);
				Calc.setRowRY(m3, 3, ry);
				break;
			case ZYX:
				Calc.setRowRZ(m1, 3, rz);
				Calc.setRowRY(m2, 3, ry);
				Calc.setRowRX(m3, 3, rx);
				break;
			default: identity(); return;
		}
		Calc.mulMM(m0, m1, m2, 3, 3, 3);
		Calc.mulMM(m1, m0, m3, 3, 3, 3);
		for (int i = 0; i < 3; ++i) {
			for (int j = 0; j < 3; ++j) {
				el[i*4 + j] = m1[i*3 + j];
			}
		}
		el[3] = 0.0f;
		el[4 + 3] = 0.0f;
		el[8 + 3] = 0.0f;
		setTranslation(0.0f, 0.0f, 0.0f);
	}

	public void setDegrees(float dx, float dy, float dz, RotOrd ord) {
		setRadians(Calc.radians(dx), Calc.radians(dy), Calc.radians(dz), ord);
	}

	public void setDegrees(Vec deg, RotOrd ord) {
		setDegrees(deg.x(), deg.y(), deg.z(), ord);
	}

	public void setDegrees(float dx, float dy, float dz) {
		setDegrees(dx, dy, dz, RotOrd.XYZ);
	}

	public void setDegrees(Vec deg) {
		setDegrees(deg.x(), deg.y(), deg.z());
	}

	public void setRadiansXYZ(float rx, float ry, float rz) {
		float sx = Calc.sinf(rx);
		float cx = Calc.cosf(rx);
		float sy = Calc.sinf(ry);
		float cy = Calc.cosf(ry);

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

		float sz = Calc.sinf(rz);
		float cz = Calc.cosf(rz);
		float z00 =   cz; float z01 =  sz; float z02 =  0.0f;
		float z10 =  -sz; float z11 =  cz; float z12 =  0.0f;
		float z20 = 0.0f; float z21 = 0.0f; float z22 = 1.0f;

		el[0] = xy00*z00 /* + xy01*z10 + xy02*z20 */;
		el[1] = xy00*z01 /* + xy01*z11 + xy02*z21 */;
		el[2] = /* xy00*z02 + xy01*z12 + */ xy02*z22;

		el[4] = xy10*z00 + xy11*z10 /* + xy12*z20 */;
		el[4 + 1] = xy10*z01 + xy11*z11 /* + xy12*z21 */;
		el[4 + 2] = /* xy10*z02 + xy11*z12 + */ xy12*z22;

		el[8] = xy20*z00 + xy21*z10 /* + xy22*z20 */;
		el[8 + 1] = xy20*z01 + xy21*z11 /* + xy22*z21 */;
		el[8 + 2] = /* xy20*z02 + xy21*z12 + */ xy22*z22;

		el[3] = 0.0f;
		el[4 + 3] = 0.0f;
		el[8 + 3] = 0.0f;
		setTranslation(0.0f, 0.0f, 0.0f);
	}

	public void setDegreesXYZ(float dx, float dy, float dz) {
		setRadiansXYZ(Calc.radians(dx), Calc.radians(dy), Calc.radians(dz));
	}

	public void setDegreesXYZ(Vec deg) {
		setDegreesXYZ(deg.x(), deg.y(), deg.z());
	}

	public void setScaling(float sx, float sy, float sz) {
		zero();
		el[0] = sx;
		el[4 + 1] = sy;
		el[8 + 2] = sz;
		el[12 + 3] = 1.0f;
	}

	public void setScaling(float s) {
		setScaling(s, s, s);
	}

	public void calcVec(float[] dst, int dstOffs, float[] src, int srcOffs) {
		float x = src[srcOffs + 0];
		float y = src[srcOffs + 1];
		float z = src[srcOffs + 2];
		float tx = x*el[0] + y*el[4] + z*el[8];
		float ty = x*el[1] + y*el[4 + 1] + z*el[8 + 1];
		float tz = x*el[2] + y*el[4 + 2] + z*el[8 + 2];
		dst[dstOffs] = tx;
		dst[dstOffs + 1] = ty;
		dst[dstOffs + 2] = tz;
	}

	public Vec calcVec(Vec v) {
		Vec res = new Vec();
		calcVec(res.el, 0, v.el, 0);
		return res;
	}

	public void calcPnt(float[] dst, int dstOffs, float[] src, int srcOffs) {
		float x = src[srcOffs + 0];
		float y = src[srcOffs + 1];
		float z = src[srcOffs + 2];
		float tx = x*el[0] + y*el[4] + z*el[8];
		float ty = x*el[1] + y*el[4 + 1] + z*el[8 + 1];
		float tz = x*el[2] + y*el[4 + 2] + z*el[8 + 2];
		dst[dstOffs] = tx;
		dst[dstOffs + 1] = ty;
		dst[dstOffs + 2] = tz;
		for (int i = 0; i < 3; ++i) {
			dst[dstOffs + i] += el[12 + i];
		}
	}

	public Vec calcPnt(Vec v) {
		Vec res = new Vec();
		calcPnt(res.el, 0, v.el, 0);
		return res;
	}

	public void makeView(Vec pos, Vec tgt, Vec upvec) {
		final float[] dir = new float[3];
		Calc.vsub(dir, tgt.el, pos.el);
		Calc.normalize(dir);
		final float[] side = new float[3];
		Calc.cross(side, upvec.el, dir);
		Calc.normalize(side);
		final float[] up = new float[3];
		Calc.cross(up, side, dir);
		for (int i = 0; i < 3; ++i) {
			el[i*4] = -side[i];
		}
		for (int i = 0; i < 3; ++i) {
			el[i*4 + 1] = -up[i];
		}
		for (int i = 0; i < 3; ++i) {
			el[i*4 + 2] = -dir[i];
		}
		el[3] = 0.0f;
		el[4 + 3] = 0.0f;
		el[8 + 3] = 0.0f;
		setTranslation(-pos.x(), -pos.y(), -pos.z());
		calcVec(el, 3*4, el, 3*4);
	}

	public void makeProj(float fovy, float aspect, float znear, float zfar) {
		float h = fovy*0.5f;
		float s = Calc.sinf(h);
		float c = Calc.cosf(h);
		float cot = c / s;
		float q = zfar / (zfar - znear);
		zero();
		el[0] = cot / aspect;
		el[4 + 1] = cot;
		el[8 + 2] = -q;
		el[8 + 3] = -1.0f;
		el[12 + 2] = -q * znear;
	}

	public Vec getRowVec(int row) {
		Vec v = new Vec();
		if (row >= 0 && row <= 3) {
			for (int i = 0; i < 3; ++i) {
				v.el[i] = el[row*4 + i];
			}
		} else {
			v.fill(Float.NaN);
		}
		return v;
	}

	public Vec getColumnVec(int col) {
		Vec v = new Vec();
		if (col >= 0 && col <= 3) {
			for (int i = 0; i < 3; ++i) {
				v.el[i] = el[i*4 + col];
			}
		} else {
			v.fill(Float.NaN);
		}
		return v;
	}

}
