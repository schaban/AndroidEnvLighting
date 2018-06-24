// Author: Sergey Chaban <sergey.chaban@gmail.com>

package xdata;

import java.util.HashMap;

public class XRig extends XData {

	public class Node {
		public String mName;
		public String mPath;
		public String mType;
		public short mSelfIdx;
		public short mParentIdx;
		public short mNameId;
		public short mPathId;
		public short mTypeId;
		public short mLvl;
		public int mAttr;
		public RotOrd mRotOrd;
		public XformOrd mXfmOrd;

		public boolean isHrcTop() {
			return mParentIdx < 0;
		}
	}

	protected Node[] mNodes;
	protected float[] mWMtx;
	protected float[] mIMtx;
	protected float[] mLMtx;
	protected float[] mLPos;
	protected float[] mLRot;
	protected float[] mLScl;
	protected HashMap<String, Integer> mNameToIdx;

	public void init(Binary bin) {
		super.init(bin);
		if (!mHead.isRig()) return;
		int pos = 0x20;
		int nodeNum = bin.getI32(pos); pos += 4;
		int lvlNum = bin.getI32(pos); pos += 4;
		int offsNode = bin.getI32(pos); pos += 4;
		int offsWMtx = bin.getI32(pos); pos += 4;
		int offsIMtx = bin.getI32(pos); pos += 4;
		int offsLMtx = bin.getI32(pos); pos += 4;
		int offsLPos = bin.getI32(pos); pos += 4;
		int offsLRot = bin.getI32(pos); pos += 4;
		int offsLScl = bin.getI32(pos); pos += 4;
		int offsInfo = bin.getI32(pos);

		mNodes = new Node[nodeNum];
		for (int i = 0; i < nodeNum; ++i) {
			Node node = new Node();
			pos = offsNode + (i * 0x10);
			node.mSelfIdx = (short)bin.getI16(pos); pos += 2;
			node.mParentIdx = (short)bin.getI16(pos); pos += 2;
			node.mNameId = (short)bin.getI16(pos); pos += 2;
			node.mPathId = (short)bin.getI16(pos); pos += 2;
			node.mTypeId = (short)bin.getI16(pos); pos += 2;
			node.mLvl = (short)bin.getI16(pos); pos += 2;
			node.mAttr = bin.getU16(pos); pos += 2;
			node.mRotOrd = RotOrd.fromInt(bin.getU8(pos)); ++pos;
			node.mXfmOrd = XformOrd.fromInt(bin.getU8(pos));
			node.mName = getStr(node.mNameId);
			node.mPath = getStr(node.mPathId);
			node.mType = getStr(node.mTypeId);
			mNodes[i] = node;
		}

		mNameToIdx = new HashMap<String, Integer>();
		for (int i = 0; i < nodeNum; ++i) {
			mNameToIdx.put(mNodes[i].mName, i);
		}

		int mtxDataSize = nodeNum * 4*4;
		if (offsWMtx > 0) {
			mWMtx = new float[mtxDataSize];
			bin.getFloats(mWMtx, offsWMtx);
		}
		if (offsIMtx > 0) {
			mIMtx = new float[mtxDataSize];
			bin.getFloats(mIMtx, offsIMtx);
		}
		if (offsLMtx > 0) {
			mLMtx = new float[mtxDataSize];
			bin.getFloats(mLMtx, offsLMtx);
		}

		int vecDataSize = nodeNum * 3;
		if (offsLPos > 0) {
			mLPos = new float[vecDataSize];
			bin.getFloats(mLPos, offsLPos);
		}
		if (offsLRot > 0) {
			mLRot = new float[vecDataSize];
			bin.getFloats(mLRot, offsLRot);
		}
		if (offsLScl > 0) {
			mLScl = new float[vecDataSize];
			bin.getFloats(mLScl, offsLScl);
		}
	}

	public int getNodesNum() {
		return mNodes != null ? mNodes.length : 0;
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

	public int[] getSkinToRigMap(XGeo geo) {
		int[] map = null;
		if (geo != null && geo.hasSkinNodes()) {
			String[] names = geo.getSkinNodeNames();
			if (names != null) {
				int n = names.length;
				map = new int[n];
				for (int i = 0; i < n; ++i) {
					map[i] = findNode(names[i]);
				}
			}
		}
		return map;
	}

	public float[] getSkinRestXforms(XGeo geo, int[] geoToRigMap) {
		float[] mtxData = null;
		if (geoToRigMap != null && mIMtx != null) {
			int n = geoToRigMap.length;
			mtxData = new float[n * 3*4];
			for (int i = 0; i < n; ++i) {
				int mtxTop = i*3*4;
				int nodeIdx = geoToRigMap[i];
				if (ckNodeIdx(nodeIdx)) {
					Calc.transposeM44toM34(mtxData, mtxTop, mIMtx, nodeIdx*4*4);
				} else {
					Calc.identity34(mtxData, mtxTop);
				}
			}
		}
		return mtxData;
	}

	public float[] getSkinRestXforms(XGeo geo) {
		return getSkinRestXforms(geo, getSkinToRigMap(geo));
	}

	public float[] getLocalPosData() {
		return mLPos;
	}

	public float[] getLocalRotData() {
		return mLRot;
	}

	public float[] getLocalSclData() {
		return mLScl;
	}

	public int getLocalDataOffs(int nodeIdx) {
		int offs = -1;
		if (ckNodeIdx(nodeIdx)) {
			offs = nodeIdx * 3;
		}
		return offs;
	}

	public void getLocalXforms(float[] dst, int dstOffs) {
		int n = getNodesNum();
		for (int i = 0; i < n; ++i) {
			Calc.transposeM44toM34(dst, dstOffs + i*3*4, mLMtx, i*4*4);
		}
	}

	public float[] getLocalXforms() {
		float[] xforms = null;
		int n = getNodesNum();
		if (n > 0) {
			xforms = new float[n * 3*4];
			getLocalXforms(xforms, 0);
		}
		return xforms;
	}

	public void getWorldXforms(float[] dst, int dstOffs) {
		int n = getNodesNum();
		for (int i = 0; i < n; ++i) {
			Calc.transposeM44toM34(dst, dstOffs + i*3*4, mWMtx, i*4*4);
		}
	}

	public float[] getWorldXforms() {
		float[] xforms = null;
		int n = getNodesNum();
		if (n > 0) {
			xforms = new float[n * 3*4];
			getWorldXforms(xforms, 0);
		}
		return xforms;
	}

	public void getParentList(int[] dst) {
		int n = getNodesNum();
		for (int i = 0; i < n; ++i) {
			dst[i] = mNodes[i].mParentIdx;
		}
	}

	public int[] getParentList() {
		int[] lst = null;
		int n = getNodesNum();
		if (n > 0) {
			lst = new int[n];
			getParentList(lst);
		}
		return lst;
	}

}
