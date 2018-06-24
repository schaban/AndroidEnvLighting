// Author: Sergey Chaban <sergey.chaban@gmail.com>

package flow;

import java.util.*;
import xdata.*;

public class AnimRig {

	public class MotList {

		public class Entry {
			public MotClip mClip;
			public int[] mRigMap;
		}

		protected ArrayList<Entry> mLst = new ArrayList<Entry>();
		protected HashMap<String, Integer> mMap = new HashMap<String, Integer>();

		public void init(MotClip[] clips) {
			XRig rigData = AnimRig.this.mData;
			if (rigData == null) return;
			int n = clips.length;
			for (int i = 0; i < n; ++i) {
				MotClip clip = clips[i];
				if (clip != null) {
					int id = mLst.size();
					Entry mot = new Entry();
					mot.mClip = clip;
					mot.mRigMap = clip.getRigMap(rigData);
					mLst.add(mot);
					mMap.put(clip.getName(), id);
				}
			}
		}

		public int getNum() {
			return mLst.size();
		}

		public boolean ckIdx(int idx) {
			return idx >= 0 && idx < getNum();
		}

		public int findClip(String name) {
			int idx = -1;
			Integer i = mMap.get(name);
			if (i != null) {
				idx = i;
			}
			return idx;
		}

		public Entry get(int idx) {
			return ckIdx(idx) ? mLst.get(idx) : null;
		}

		public MotClip getClip(int idx) {
			Entry mot = get(idx);
			return mot != null ? mot.mClip : null;
		}

		public int[] getRigMap(int idx) {
			Entry mot = get(idx);
			return mot != null ? mot.mRigMap : null;
		}

		public int getClipFramesNum(int idx) {
			int n = 0;
			Entry mot = get(idx);
			if (mot != null && mot.mClip != null) {
				n = mot.mClip.getFramesNum();
			}
			return n;
		}

		public void applyClip(int motIdx, int frame) {
			AnimRig rig = AnimRig.this;
			Entry mot = get(motIdx);
			if (mot != null) {
				MotClip clip = mot.mClip;
				int[] map = mot.mRigMap;
				rig.applyMotion(clip, frame, map);
			}
		}

	}

	protected XRig mData;
	protected float[] mXformsRestL;
	protected float[] mXformsL;
	protected float[] mXformsW;
	protected int[] mParentList;
	protected Vec mWorldPos = new Vec();
	protected Vec mPrevWorldPos = new Vec();
	protected Vec mPrevMovePos = new Vec();
	protected Vec mMoveVel = new Vec();
	protected Vec mVelVec = new Vec();
	protected Vec mWorldRot = new Vec();
	protected Mtx mWorldMtx = new Mtx();
	protected int mRootNodeId = -1;
	protected int mMovementNodeId = -1;
	protected int mCenterNodeId = -1;
	protected int mWristIdL = -1;
	protected int mWristIdR = -1;
	protected int mForearmIdL = -1;
	protected int mForearmIdR = -1;
	protected float[] mXformsBlendL;
	protected float mBlendDuration;
	protected float mBlendCount;
	protected boolean mBlendSlerpEnabled = true;
	protected float[] mFltWk = new float[16];
	protected boolean mEnableSupJoints = true;

	protected int getNodesNum() {
		return mData != null ? mData.getNodesNum() : 0;
	}

	public void init(XRig data) {
		if (data == null) return;
		mData = data;
		mXformsRestL = mData.getLocalXforms();
		mXformsL = mData.getLocalXforms();
		mXformsW = mData.getWorldXforms();
		mParentList = data.getParentList();
		mRootNodeId = data.findNode("root");
		mMovementNodeId = data.findNode("n_Move");
		mCenterNodeId = data.findNode("n_Center");
		mWristIdL = data.findNode("j_Wrist_L");
		mWristIdR = data.findNode("j_Wrist_R");
		mForearmIdL = data.findNode("s_Forearm_L");
		mForearmIdR = data.findNode("s_Forearm_R");
		mWorldPos.fill(0.0f);
		mPrevWorldPos.fill(0.0f);
		mMoveVel.fill(0.0f);
		mWorldRot.fill(0.0f);
		mWorldMtx.identity();
		mXformsBlendL = mData.getLocalXforms();
	}

	public Vec getWorldPos() {
		return mWorldPos;
	}

	public void setWorldPos(float x, float y, float z) {
		mWorldPos.set(x, y, z);
	}

	public void setWorldPos(Vec pos) {
		mWorldPos.set(pos);
	}

	public void setWorldPosXZ(float x, float z) {
		mWorldPos.x(x);
		mWorldPos.z(z);
	}

	public Vec getPrevWorldPos() {
		return mPrevWorldPos;
	}

	public Vec getWorldRot() {
		return mWorldRot;
	}

	public void setWorldRot(float dx, float dy, float dz) {
		mWorldRot.set(dx, dy, dz);
	}

	public void setWorldRotY(float dy) {
		mWorldRot.set(0.0f, dy, 0.0f);
	}

	public void addWorldRotY(float yadd) {
		mWorldRot.el[1] += yadd;
	}

	public void updateCoord() {
		mPrevWorldPos.set(mWorldPos);
		mWorldMtx.setDegreesXYZ(mWorldRot);
		mVelVec = mWorldMtx.calcVec(mMoveVel);
		mWorldPos.add(mVelVec);
		mWorldMtx.setTranslation(mWorldPos);
		if (mData.ckNodeIdx(mMovementNodeId)) {
			Calc.identity34(mXformsL, mMovementNodeId * 3*4);
		}
		if (mData.ckNodeIdx(mRootNodeId)) {
			Calc.transposeM44toM34(mXformsL, mRootNodeId * 3*4, mWorldMtx.el, 0);
		}
	}

	public void updateRoot() {
		mWorldMtx.setDegreesXYZ(mWorldRot);
		mWorldMtx.setTranslation(mWorldPos);
		if (mData.ckNodeIdx(mRootNodeId)) {
			Calc.transposeM44toM34(mXformsL, mRootNodeId * 3*4, mWorldMtx.el, 0);
		}
	}

	public float[] getWorldXforms() {
		return mXformsW;
	}

	public void calcWorld() {
		updateRoot();
		int n = getNodesNum();
		final int xsize = 3*4;
		for (int i = 0; i < n; ++i) {
			int iparent = mParentList[i];
			int iptr = i * xsize;
			if (iparent < 0) {
				System.arraycopy(mXformsL, iptr, mXformsW, iptr, xsize);
			} else {
				Calc.xform3x4Mul(mXformsW, iptr, mXformsL, iptr, mXformsW, iparent * xsize);
			}
		}
	}

	public Mtx getNodeWorldMtx(int idx) {
		Mtx m = null;
		if (mData != null && mData.ckNodeIdx(idx)) {
			m = new Mtx();
			m.identity();
			System.arraycopy(mXformsW, idx * 3*4, m.el, 0, 3*4);
			m.transpose();
		}
		return m;
	}

	public Mtx getNodeWorldMtx(String name) {
		if (mData == null) return null;
		int idx = mData.findNode(name);
		return getNodeWorldMtx(idx);
	}

	public void blendInit(int duration) {
		int n = getNodesNum();
		if (n > 0) {
			System.arraycopy(mXformsL, 0, mXformsBlendL, 0, n * 3*4);
		}
		mBlendDuration = (float)duration;
		mBlendCount = mBlendDuration;
	}

	public void blendSlerpEnable(boolean enable) {
		mBlendSlerpEnabled = enable;
	}

	public void blendExec() {
		if (mBlendCount <= 0.0f) return;
		int n = getNodesNum();
		float t = Calc.div0(mBlendDuration - mBlendCount, mBlendDuration);
		if (mBlendSlerpEnabled) {
			for (int i = 0; i < n; ++i) {
				int offs = i * 3*4;
				final int q0 = 0;
				final int q1 = 4;
				final int q2 = 8;
				Calc.xform3x4GetQuat(mFltWk, q1, mXformsBlendL, offs);
				Calc.xform3x4GetQuat(mFltWk, q2, mXformsL, offs);
				Calc.slerp(mFltWk, q0, mFltWk, q1, mFltWk, q2, t);
				Calc.xform3x4Quat(mXformsL, offs, mFltWk, q0);
			}
		} else {
			for (int i = 0; i < n; ++i) {
				int offs = i * 3*4;
				Calc.xform3x4InterpolateAxes(mXformsL, offs, mXformsBlendL, offs, mXformsL, offs, t);
			}
		}
		if (mData.ckNodeIdx(mCenterNodeId)) {
			int offs = mCenterNodeId * 3*4;
			Calc.xform3x4InterpolatePos(mXformsL, offs, mXformsBlendL, offs, mXformsL, offs, t);
		}
		--mBlendCount;
		mBlendCount = Math.max(0.0f, mBlendCount);
	}

	public void applyMotion(MotClip clip, int frame) {
		applyMotion(clip, frame, null);
	}

	public void applyMotion(MotClip clip, int frame, int[] map) {
		if (mData == null || clip == null) return;
		System.arraycopy(mXformsRestL, 0, mXformsL, 0, mXformsL.length);
		int motNodesNum = clip.getNodesNum();
		for (int motNodeIdx = 0; motNodeIdx < motNodesNum; ++motNodeIdx) {
			int rigNodeIdx = -1;
			if (map != null) {
				rigNodeIdx = map[motNodeIdx];
			} else {
				MotClip.Node motNode = clip.getNode(motNodeIdx);
				rigNodeIdx = mData.findNode(motNode.mName);
			}
			if (mData.ckNodeIdx(rigNodeIdx)) {
				clip.getXformPosRot(mXformsL, rigNodeIdx * 3*4, motNodeIdx, frame);
				if (rigNodeIdx == mMovementNodeId) {
					int fno = clip.frameMod(frame);
					Calc.xform3x4GetPos(mMoveVel.el, 0, mXformsL, rigNodeIdx * 3*4);
					if (fno > 0) {
						int prevFrame = Math.max(fno - 1, 0);
						clip.getPos(mPrevMovePos.el, 0, motNodeIdx, prevFrame);
						mMoveVel.sub(mPrevMovePos);
					}
				}
			}
		}
	}

	public MotList createMotList(MotClip[] clips) {
		MotList ml = new MotList();
		ml.init(clips);
		return ml;
	}

	public void enableSupportJoints(boolean enabled) {
		mEnableSupJoints = enabled;
	}

	protected void localSupInterR(int idst, int isrc, int axis, float rate) {
		final int offsQ0 = 0;
		final int offsQ1 = 4;
		int dstOffs = idst * 3*4;
		int srcOffs = isrc * 3*4;
		Calc.xform3x4GetQuat(mFltWk, offsQ0, mXformsL, dstOffs);
		Calc.xform3x4GetQuat(mFltWk, offsQ1, mXformsL, srcOffs);
		float x = axis == 0 ? mFltWk[offsQ1] : 0.0f;
		float y = axis == 1 ? mFltWk[offsQ1 + 1] : 0.0f;
		float z = axis == 2 ? mFltWk[offsQ1 + 2] : 0.0f;
		float w = mFltWk[offsQ1 + 3];
		float sqmag = x*x + y*y + z*z + w*w;
		if (sqmag > 1.0e-5f) {
			float s = 1.0f / Calc.sqrtf(sqmag);
			x *= s;
			y *= s;
			z *= s;
			w *= s;
			mFltWk[offsQ0] = Calc.lerp(mFltWk[offsQ0], x, rate);
			mFltWk[offsQ0 + 1] = Calc.lerp(mFltWk[offsQ0 + 1], y, rate);
			mFltWk[offsQ0 + 2] = Calc.lerp(mFltWk[offsQ0 + 2], z, rate);
			mFltWk[offsQ0 + 3] = Calc.lerp(mFltWk[offsQ0 + 3], w, rate);
			Calc.normalize(mFltWk, offsQ0, 4);
			Calc.xform3x4Quat(mXformsL, dstOffs, mFltWk, offsQ0);
		}
	}

	protected void localSupInterRXZ(int idst, int isrc, float rate) {
		final int offsQ0 = 0;
		final int offsQ1 = 4;
		final int offsQ2 = 8;
		int dstOffs = idst * 3*4;
		int srcOffs = isrc * 3*4;
		Calc.xform3x4GetQuat(mFltWk, offsQ0, mXformsL, dstOffs);
		Calc.xform3x4GetQuat(mFltWk, offsQ2, mXformsL, srcOffs);
		Calc.quatClosestZX(mFltWk, offsQ1, mFltWk, offsQ2);
		Calc.vlerp(mFltWk, offsQ0, mFltWk, offsQ0, mFltWk, offsQ1, 4, rate);
		Calc.normalize(mFltWk, offsQ0, 4);
		Calc.xform3x4Quat(mXformsL, dstOffs, mFltWk, offsQ0);
	}

	public void calcSupportJoints() {
		if (!mEnableSupJoints) return;
		// FIXME
		final float forearmRate = 0.25f;
		final float forearmRateX = 0.5f;
		final float forearmRateZ = 0.25f;
		boolean forearmXZ = true;
		if (mWristIdL >= 0 && mForearmIdL >= 0) {
			if (forearmXZ) {
				if (forearmRateX == forearmRateZ) {
					localSupInterRXZ(mForearmIdL, mWristIdL, forearmRateX);
				} else {
					localSupInterR(mForearmIdL, mWristIdL, 0, forearmRateX);
					localSupInterR(mForearmIdL, mWristIdL, 2, forearmRateZ);
				}
			} else {
				int dstOffs = mForearmIdL * 3*4;
				int srcOffs = mWristIdL * 3*4;
				Calc.xform3x4InterpolateAxes(mXformsL, dstOffs, mXformsL, dstOffs, mXformsL, srcOffs, forearmRate);
			}
		}
		if (mWristIdR >= 0 && mForearmIdR >= 0) {
			if (forearmXZ) {
				if (forearmRateX == forearmRateZ) {
					localSupInterRXZ(mForearmIdR, mWristIdR, forearmRateX);
				} else {
					localSupInterR(mForearmIdR, mWristIdR, 0, forearmRateX);
					localSupInterR(mForearmIdR, mWristIdR, 2, forearmRateZ);
				}
			} else {
				int dstOffs = mForearmIdR * 3*4;
				int srcOffs = mWristIdR * 3*4;
				Calc.xform3x4InterpolateAxes(mXformsL, dstOffs, mXformsL, dstOffs, mXformsL, srcOffs, forearmRate);
			}
		}
	}
}
