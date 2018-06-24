// Author: Sergey Chaban <sergey.chaban@gmail.com>

package flow;

import xdata.*;

import java.util.Locale;
import java.util.HashMap;
import java.util.Formatter;

public class MotClip {
	public enum TrackKind {
		POS,
		ROT,
		SCL
	}

	public final static String SIG = "MCLP";

	public class Track {
		public Node mNode;
		public TrackKind mKind;
		public float[] mMinMax;
		public byte mSrcMask;
		public byte mDataMask;
		public byte mStride;
		public float[] mData;
	}

	public class Node {
		public String mName;
		public XformOrd mXfmOrd;
		public RotOrd mRotOrd;
		public Track mPosTrk;
		public Track mRotTrk;
		public Track mSclTrk;

		public boolean hasPos() {
			return mPosTrk != null;
		}

		public boolean hasRot() {
			return mRotTrk != null;
		}

		public boolean hasScl() {
			return mSclTrk != null;
		}
	}

	protected String mName;
	protected float mRate;
	protected int mFramesNum;
	protected Node[] mNodes;
	protected HashMap<String, Integer> mNameToIdx;

	public String getName() {
		return mName;
	}

	public float getFPS() {
		return mRate;
	}

	public int getFramesNum() {
		return mFramesNum;
	}

	public int getNodesNum() {
		return mNodes != null ? mNodes.length : 0;
	}

	private Track readTrkInfo(Binary bin, int at, TrackKind kind) {
		Track trk = new Track();
		trk.mKind = kind;
		trk.mMinMax = new float[3 + 3];
		bin.getFloats(trk.mMinMax, at);
		trk.mSrcMask = (byte)bin.getI8(at + 0x18);
		trk.mDataMask = (byte)bin.getI8(at + 0x19);
		trk.mStride = (byte)bin.getI8(at + 0x1A);
		return trk;
	}

	private void readTrkData(Track trk, Binary bin, int at) {
		int stride = trk.mStride;
		if (stride > 0) {
			trk.mData = new float[stride * mFramesNum];
			bin.getFloats(trk.mData, at);
		}
	}

	public void init(Binary bin) {
		String kind = bin.getKind();
		if (!kind.equals(SIG)) return;
		mRate = bin.getF32(8);
		mFramesNum = bin.getI32(0xC);
		int nnodes = bin.getI32(0x10);
		int slen = bin.getU8(0x20);
		mName = bin.getStrLen(0x21, slen);
		mNodes = new Node[nnodes];
		mNameToIdx = new HashMap<String, Integer>();
		for (int i = 0; i < nnodes; ++i) {
			int top = 0x60 + i*0xB0;
			slen = bin.getU8(top);
			Node node = new Node();
			node.mName = bin.getStrLen(top + 1, slen);;
			int offsPos = bin.getI32(top + 0x40);
			int offsRot = bin.getI32(top + 0x44);
			int offsScl = bin.getI32(top + 0x48);
			node.mXfmOrd = XformOrd.fromInt(bin.getU8(top + 0x4C));
			node.mRotOrd = RotOrd.fromInt(bin.getU8(top + 0x4D));
			if (offsPos > 0) {
				node.mPosTrk = readTrkInfo(bin, top + 0x50, TrackKind.POS);
			}
			if (offsRot > 0) {
				node.mRotTrk = readTrkInfo(bin, top + 0x50 + 0x20, TrackKind.ROT);
			}
			if (offsScl > 0) {
				node.mSclTrk = readTrkInfo(bin, top + 0x50 + 0x20*2, TrackKind.SCL);
			}
			if (node.mPosTrk != null) {
				readTrkData(node.mPosTrk, bin, offsPos);
			}
			if (node.mRotTrk != null) {
				readTrkData(node.mRotTrk, bin, offsRot);
			}
			if (node.mSclTrk != null) {
				readTrkData(node.mSclTrk, bin, offsScl);
			}
			mNodes[i] = node;
			mNameToIdx.put(node.mName, i);
		}
	}

	public boolean ckNodeIdx(int idx) {
		return mNodes != null && idx >= 0 && idx < mNodes.length;
	}

	public Node getNode(int idx) {
		Node node = null;
		if (ckNodeIdx(idx)) {
			node = mNodes[idx];
		}
		return node;
	}

	public Node getNode(String name) {
		return getNode(findNode(name));
	}

	public int findNode(String name) {
		int idx = -1;
		if (mNameToIdx != null) {
			Integer i = mNameToIdx.get(name);
			if (i != null) {
				idx = i;
			}
		}
		return idx;
	}

	public int[] getRigMap(XRig rig) {
		int[] map = null;
		int n = getNodesNum();
		if (n > 0) {
			map = new int[n];
			for (int i = 0; i < n; ++i) {
				map[i] = rig.findNode(mNodes[i].mName);
			}
		}
		return map;
	}

	public int frameMod(int frame) {
		if (frame < 0) {
			frame = mFramesNum - (-frame % mFramesNum);
		} else {
			frame = frame % mFramesNum;
		}
		return frame;
	}

	public void getPos(float[] pos, int posOffs, int nodeIdx, int frame) {
		if (!ckNodeIdx(nodeIdx)) return;
		frame = frameMod(frame);
		Node node = mNodes[nodeIdx];
		float tx = 0.0f;
		float ty = 0.0f;
		float tz = 0.0f;
		if (node.hasPos()) {
			Track trk = node.mPosTrk;
			float[] data = trk.mData;
			int ptr = frame * trk.mStride;

			if ((trk.mDataMask & 1) != 0) {
				tx = data[ptr++];
			} else if ((trk.mSrcMask & 1) != 0) {
				tx = trk.mMinMax[0];
			}

			if ((trk.mDataMask & 2) != 0) {
				ty = data[ptr++];
			} else if ((trk.mSrcMask & 2) != 0) {
				ty = trk.mMinMax[1];
			}

			if ((trk.mDataMask & 4) != 0) {
				tz = data[ptr];
			} else if ((trk.mSrcMask & 4) != 0) {
				tz = trk.mMinMax[2];
			}
		}
		pos[posOffs] = tx;
		pos[posOffs + 1] = ty;
		pos[posOffs + 2] = tz;
	}

	public void getXformPosRot(float[] xform, int xformOffs, int nodeIdx, int frame) {
		if (!ckNodeIdx(nodeIdx)) return;
		frame = frameMod(frame);
		Node node = mNodes[nodeIdx];
		if (node.hasRot()) {
			Track trk = node.mRotTrk;
			float[] data = trk.mData;
			int ptr = frame * trk.mStride;

			float lx = 0.0f;
			if ((trk.mDataMask & 1) != 0) {
				lx = data[ptr++];
			} else if ((trk.mSrcMask & 1) != 0) {
				lx = trk.mMinMax[0];
			}

			float ly = 0.0f;
			if ((trk.mDataMask & 2) != 0) {
				ly = data[ptr++];
			} else if ((trk.mSrcMask & 2) != 0) {
				ly = trk.mMinMax[1];
			}

			float lz = 0.0f;
			if ((trk.mDataMask & 4) != 0) {
				lz = data[ptr];
			} else if ((trk.mSrcMask & 4) != 0) {
				lz = trk.mMinMax[2];
			}
			Calc.xform3x4ExpMap(xform, xformOffs, lx, ly, lz);
		}
		if (node.hasPos()) {
			Track trk = node.mPosTrk;
			float[] data = trk.mData;
			int ptr = frame * trk.mStride;

			float tx = 0.0f;
			if ((trk.mDataMask & 1) != 0) {
				tx = data[ptr++];
			} else if ((trk.mSrcMask & 1) != 0) {
				tx = trk.mMinMax[0];
			}

			float ty = 0.0f;
			if ((trk.mDataMask & 2) != 0) {
				ty = data[ptr++];
			} else if ((trk.mSrcMask & 2) != 0) {
				ty = trk.mMinMax[1];
			}

			float tz = 0.0f;
			if ((trk.mDataMask & 4) != 0) {
				tz = data[ptr];
			} else if ((trk.mSrcMask & 4) != 0) {
				tz = trk.mMinMax[2];
			}
			Calc.xform3x4Pos(xform, xformOffs, tx, ty, tz);
		}
	}

	public void dumpPosRotClip(StringBuilder sb) {
		dumpPosRotClip(sb, "/obj");
	}

	public void dumpPosRotClip(StringBuilder sb, String basePath) {
		Formatter f = new Formatter(sb, Locale.US);
		int nn = getNodesNum();
		int nch = 0;
		for (int i = 0; i < nn; ++i) {
			if (mNodes[i].hasPos()) {
				nch += 3;
			}
			if (mNodes[i].hasRot()) {
				nch += 3;
			}
		}
		f.format("{\n\trate = %f\n\tstart = -1\n\ttracklength = %d\n\ttracks = %d\n", mRate, mFramesNum, nch);
		float[] xform = new float[3*4 * mFramesNum];
		float[] radians = new float[3 * mFramesNum];
		float[] quat = new float[4];
		for (int i = 0; i < nn; ++i) {
			Node node = mNodes[i];
			for (int fno = 0; fno < mFramesNum; ++fno) {
				getXformPosRot(xform, fno * 3*4, i, fno);
			}
			if (node.hasRot()) {
				for (int fno = 0; fno < mFramesNum; ++fno) {
					Calc.xform3x4GetQuat(quat, 0, xform, fno * 3*4);
					Calc.radiansFromQuat(radians, fno * 3, quat[0], quat[1], quat[2], quat[3], node.mRotOrd);
				}
			}
			if (node.hasPos()) {
				f.format("\t{\n\t\tname = %s/%s:tx\n\t\tdata =", basePath, node.mName);
				for (int fno = 0; fno < mFramesNum; ++fno) {
					f.format(" %f", Calc.xform3x4GetPosX(xform, fno*3*4));
				}
				f.format("\n\t}\n");

				f.format("\t{\n\t\tname = %s/%s:ty\n\t\tdata =", basePath, node.mName);
				for (int fno = 0; fno < mFramesNum; ++fno) {
					f.format(" %f", Calc.xform3x4GetPosY(xform, fno*3*4));
				}
				f.format("\n\t}\n");

				f.format("\t{\n\t\tname = %s/%s:tz\n\t\tdata =", basePath, node.mName);
				for (int fno = 0; fno < mFramesNum; ++fno) {
					f.format(" %f", Calc.xform3x4GetPosZ(xform, fno*3*4));
				}
				f.format("\n\t}\n");
			}
			if (node.hasRot()) {
				f.format("\t{\n\t\tname = %s/%s:rx\n\t\tdata =", basePath, node.mName);
				for (int fno = 0; fno < mFramesNum; ++fno) {
					f.format(" %f", Calc.degrees(radians[fno*3]));
				}
				f.format("\n\t}\n");

				f.format("\t{\n\t\tname = %s/%s:ry\n\t\tdata =", basePath, node.mName);
				for (int fno = 0; fno < mFramesNum; ++fno) {
					f.format(" %f", Calc.degrees(radians[fno*3 + 1]));
				}
				f.format("\n\t}\n");

				f.format("\t{\n\t\tname = %s/%s:rz\n\t\tdata =", basePath, node.mName);
				for (int fno = 0; fno < mFramesNum; ++fno) {
					f.format(" %f", Calc.degrees(radians[fno*3 + 2]));
				}
				f.format("\n\t}\n");
			}
		}
		f.format("}\n");
	}
}
