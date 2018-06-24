// Author: Sergey Chaban <sergey.chaban@gmail.com>

package xdata;

public class Frustum {
	protected static final int O_POS = 0;
	protected static final int O_NRM = 8*3;
	protected static final int O_PLN = (8 + 6) * 3;

	protected float[] mData = new float[8*3 + 6*3 + 6*4];

	protected final static int[] s_ntbl = new int [] {
		O_POS + 0*3, O_POS + 1*3, O_POS + 3*3, /* near */
		O_POS + 0*3, O_POS + 3*3, O_POS + 4*3, /* left */
		O_POS + 0*3, O_POS + 4*3, O_POS + 5*3, /* top */
		O_POS + 6*3, O_POS + 2*3, O_POS + 1*3, /* right */
		O_POS + 6*3, O_POS + 7*3, O_POS + 3*3, /* bottom */
		O_POS + 6*3, O_POS + 5*3, O_POS + 4*3  /* far */
	};

	protected void calcNormals() {
		for (int i = 0; i < 6; ++i) {
			int idx = i * 3;
			int p0 = s_ntbl[idx];
			int p1 = s_ntbl[idx + 1];
			int p2 = s_ntbl[idx + 2];
			Calc.triNormalCW(mData, O_NRM + i*3, mData, p0, p1, p2);
		}
	}

	protected void calcPlanes() {
		for (int i = 0; i < 3; ++i) {
			Calc.planeFromPosNrm(mData, O_PLN + i*4, mData, O_POS, mData, O_NRM + i*3);
		}
		for (int i = 3; i < 6; ++i) {
			Calc.planeFromPosNrm(mData, O_PLN + i*4, mData, O_POS + 6*3, mData, O_NRM + i*3);
		}
	}

	protected void setPnt(int idx, float x, float y, float z) {
		int org = O_POS + idx*3;
		mData[org] = x;
		mData[org + 1] = y;
		mData[org + 2] = z;
	}

	public void init(Mtx mtx, float fovy, float aspect, float znear, float zfar) {
		float t = Calc.tanf(fovy * 0.5f);
		float z = znear;
		float y = t * z;
		float x = y * aspect;
		setPnt(0, -x, y, -z);
		setPnt(1, x, y, -z);
		setPnt(2, x, -y, -z);
		setPnt(3, -x, -y, -z);
		z = zfar;
		y = t * z;
		x = y * aspect;
		setPnt(4, -x, y, -z);
		setPnt(5, x, y, -z);
		setPnt(6, x, -y, -z);
		setPnt(7, -x, -y, -z);
		calcNormals();
		for (int i = 0; i < 8; ++i) {
			int opnt = O_POS + i*3;
			mtx.calcPnt(mData, opnt, mData, opnt);
		}
		for (int i = 0; i < 6; ++i) {
			int onrm = O_NRM + i*3;
			mtx.calcVec(mData, onrm, mData, onrm);
		}
		calcPlanes();
	}

	public boolean cullSphere(float[] sph, int sphOffs) {
		float cx = sph[sphOffs];
		float cy = sph[sphOffs + 1];
		float cz = sph[sphOffs + 2];
		float r = sph[sphOffs + 3];
		float vx = cx - mData[O_POS];
		float vy = cy - mData[O_POS + 1];
		float vz = cz - mData[O_POS + 2];
		for (int i = 0; i < 3; ++i) {
			int onrm = O_NRM + i*3;
			float d = vx*mData[onrm] + vy*mData[onrm+1] + vz*mData[onrm+2];
			if (r < d) return true;
		}
		vx = cx - mData[O_POS + 6*3];
		vy = cy - mData[O_POS + 6*3 + 1];
		vz = cz - mData[O_POS + 6*3 + 2];
		for (int i = 3; i < 6; ++i) {
			int onrm = O_NRM + i*3;
			float d = vx*mData[onrm] + vy*mData[onrm+1] + vz*mData[onrm+2];
			if (r < d) return true;
		}
		return false;
	}

	public boolean cullBox(float[] minmax, int offs) {
		float maxx = minmax[offs + 3];
		float maxy = minmax[offs + 3 + 1];
		float maxz = minmax[offs + 3 + 2];
		float cx = (minmax[offs] + maxx) * 0.5f;
		float cy = (minmax[offs + 1] + maxy) * 0.5f;
		float cz = (minmax[offs + 2] + maxz) * 0.5f;
		float rx = maxx - cx;
		float ry = maxy - cy;
		float rz = maxz - cz;
		float vx = cx - mData[O_POS];
		float vy = cy - mData[O_POS + 1];
		float vz = cz - mData[O_POS + 2];
		for (int i = 0; i < 3; ++i) {
			int onrm = O_NRM + i*3;
			float nx = mData[onrm];
			float ny = mData[onrm + 1];
			float nz = mData[onrm + 2];
			float anx = Math.abs(nx);
			float any = Math.abs(ny);
			float anz = Math.abs(nz);
			float ran = rx*anx + ry*any + rz*anz;
			float vn = vx*nx + vy*ny + vz*nz;
			if (ran < vn) return true;
		}
		vx = cx - mData[O_POS + 6*3];
		vy = cy - mData[O_POS + 6*3 + 1];
		vz = cz - mData[O_POS + 6*3 + 2];
		for (int i = 3; i < 6; ++i) {
			int onrm = O_NRM + i*3;
			float nx = mData[onrm];
			float ny = mData[onrm + 1];
			float nz = mData[onrm + 2];
			float anx = Math.abs(nx);
			float any = Math.abs(ny);
			float anz = Math.abs(nz);
			float ran = rx*anx + ry*any + rz*anz;
			float vn = vx*nx + vy*ny + vz*nz;
			if (ran < vn) return true;
		}
		return false;
	}

}
