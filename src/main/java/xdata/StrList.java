// Author: Sergey Chaban <sergey.chaban@gmail.com>

package xdata;

import java.util.HashMap;
import java.util.ArrayList;

public class StrList {

	protected String[] mAry;
	protected ArrayList<String> mLst;
	protected HashMap<String, Integer> mMap;

	public int getStrNum() {
		if (mAry != null) return mAry.length;
		if (mLst != null) return mLst.size();
		return 0;
	}

	public int getStrDataSize() {
		int n = getStrNum();
		int size = 0;
		for (int i = 0; i < n; ++i) {
			size += get(i).length() + 1;
		}
		return size;
	}

	public void read(Binary bin, int org) {
		if (mLst != null) return;
		if (org > 0) {
			int size = bin.getI32(org);
			int num = bin.getI32(org + 4);
			if (num > 0) {
				int[] offs = new int[num];
				for (int i = 0; i < num; ++i) {
					offs[i] = bin.getI32(org + 8 + i*4);
				}
				mAry = new String[num];
				for (int i = 0; i < num; ++i) {
					mAry[i] = bin.getStr(org + offs[i]);
				}
				mMap = new HashMap<String, Integer>();
				for (int i = 0; i < num; ++i) {
					mMap.put(mAry[i], i);
				}
			}
		}
	}

	public String get(int idx) {
		String s = null;
		if (mAry != null) {
			if (idx >= 0 && idx < mAry.length) {
				s = mAry[idx];
			}
		} else if (mLst != null){
			if (idx >= 0 && idx < mLst.size()) {
				s = mLst.get(idx);
			}
		}
		return s;
	}

	public int add(String s) {
		if (mAry != null) {
			return -1;
		}
		if (mLst == null) {
			mMap = new HashMap<String, Integer>();
			mLst = new ArrayList<String>();
			mLst.add(s);
			mMap.put(s, 0);
			return 0;
		}
		if (mMap.containsKey(s)) {
			return mMap.get(s);
		}
		int idx = mLst.size();
		mLst.add(s);
		mMap.put(s, idx);
		return idx;
	}

	public int find(String s) {
		int idx = -1;
		if (mMap != null && mMap.containsKey(s)) {
			idx = mMap.get(s);
		}
		return idx;
	}

}
