// Author: Sergey Chaban <sergey.chaban@gmail.com>

package flow;

import gley.GParam;
import xdata.*;

public class Camera {

	protected Vec mPos;
	protected Vec mTgt;
	protected Vec mUp;
	protected Vec mDir;
	protected Vec mPrevPos;
	protected float mFOVY;
	protected float mNear;
	protected float mFar;
	protected int mWidth;
	protected int mHeight;
	protected float mAspect;
	protected Mtx mViewMtx;
	protected Mtx mProjMtx;
	protected Mtx mViewProjMtx;
	protected Mtx mInvViewMtx;
	protected Mtx mInvProjMtx;
	protected Mtx mInvViewProjMtx;
	protected Frustum mFrustum;
	protected Mtx mTmpMtx = new Mtx();
	protected int[] mMtxIWk = new int[4*3];

	public Camera() {
		mPos = new Vec(0.0f, 1.0f, 1.5f);
		mTgt = new Vec(0.0f, 1.0f, 0.0f);
		mUp = new Vec (0.0f, 1.0f, 0.0f);
		mDir = new Vec();
		mPrevPos = new Vec(mPos);
		mViewMtx = new Mtx();
		mProjMtx = new Mtx();
		mViewProjMtx = new Mtx();
		mInvViewMtx = new Mtx();
		mInvProjMtx = new Mtx();
		mInvViewProjMtx = new Mtx();
		mFrustum = new Frustum();
		update(mPos, mTgt, mUp, Calc.radians(40.0f), 0.1f, 1000.0f, 800, 600);
	}

	protected void updateMatrices() {
		mViewMtx.makeView(mPos, mTgt, mUp);
		mProjMtx.makeProj(mFOVY, mAspect, mNear, mFar);
		mViewProjMtx.mul(mViewMtx, mProjMtx);
		mInvViewMtx.invert(mViewMtx, mMtxIWk);
		mInvProjMtx.invert(mProjMtx, mMtxIWk);
		mInvViewProjMtx.invert(mViewProjMtx, mMtxIWk);
	}

	public void update(Vec pos, Vec tgt, Vec up, float fovy, float znear, float zfar, int w, int h) {
		mPrevPos.set(mPos);
		mPos.set(pos);
		mTgt.set(tgt);
		mUp.set(up);
		mDir.set(tgt);
		mDir.sub(pos);
		mDir.normalize();
		mFOVY = fovy;
		mNear = znear;
		mFar = zfar;
		mWidth = w;
		mHeight = h;
		mAspect = (float)mWidth / mHeight;
		updateMatrices();
		mFrustum.init(mInvViewMtx, mFOVY, mAspect, mNear, mFar);
	}

	public Vec getPos() {
		return mPos;
	}

	public Vec getTgt() {
		return mTgt;
	}

	public Vec getUp() {
		return mUp;
	}

	public Vec getDir() {
		return mDir;
	}

	public Vec getPrevPos() {
		return mPrevPos;
	}

	public Mtx getView() {
		return mViewMtx;
	}

	public Mtx getViewProj() {
		return mViewProjMtx;
	}

	public float calcViewDist(float x, float y, float z) {
		float dx = x - mPos.el[0];
		float dy = y - mPos.el[1];
		float dz = z - mPos.el[2];
		return dx*mDir.el[0] + dy*mDir.el[1] + dz*mDir.el[2];
	}

	public float calcViewDist(float[] pos, int posOffs) {
		return calcViewDist(pos[posOffs], pos[posOffs + 1], pos[posOffs + 2]);
	}

	public float calcViewDist(Vec pos) {
		return calcViewDist(pos.el, 0);
	}

	public float calcViewDepth(float x, float y, float z, float bias) {
		float dist;
		if (bias != 0.0f) {
			float bx = x + mDir.el[0]*bias;
			float by = y + mDir.el[1]*bias;
			float bz = z + mDir.el[2]*bias;
			dist = calcViewDist(bx, by, bz);
		} else {
			dist = calcViewDist(x, y, z);
		}
		float d = Calc.div0(dist - mNear, mFar - mNear);
		return Calc.saturate(d);
	}

	public float calcViewDepth(float[] pos, int posOffs, float bias) {
		return calcViewDepth(pos[posOffs], pos[posOffs + 1], pos[posOffs + 2], bias);
	}

	public float calcViewDepth(float[] pos, int posOffs) {
		return calcViewDepth(pos, posOffs, 0.0f);
	}

	public float calcViewDepth(Vec pos, float bias) {
		return calcViewDepth(pos.el, 0, bias);
	}

	public float calcViewDepth(Vec pos) {
		return calcViewDepth(pos, 0.0f);
	}

	public Frustum getFrustum() {
		return mFrustum;
	}

	public boolean cullBox(float[] minmax, int offs) {
		return mFrustum.cullBox(minmax, offs);
	}

	public boolean cullSphere(float[] sph, int sphOffs) {
		return mFrustum.cullSphere(sph, sphOffs);
	}

	protected void putMtxGP(CmdList cmd, String name, Mtx mtx) {
		mTmpMtx.transpose(mtx);
		cmd.putSETF(name, mTmpMtx.el);
	}

	public void putAllGP(CmdList cmd) {
		putMtxGP(cmd,"CAMERA.view", mViewMtx);
		putMtxGP(cmd, "CAMERA.proj", mProjMtx);
		putMtxGP(cmd, "CAMERA.viewProj", mViewProjMtx);
		putMtxGP(cmd, "CAMERA.invView", mInvViewMtx);
		putMtxGP(cmd, "CAMERA.invProj", mInvProjMtx);
		putMtxGP(cmd, "CAMERA.invViewProj", mInvViewProjMtx);
		cmd.putSETF("CAMERA.viewPos", mPos.el);
	}
}
