// Author: Sergey Chaban <sergey.chaban@gmail.com>

package xdata;

public class FileCat extends XData {

	protected int mFilesNum;
	protected int[] mFileInfo; // {nameId, fnameId}

	public void init(Binary bin) {
		super.init(bin);
		if (!mHead.isFileCat()) return;
		int n = bin.getI32(0x20);
		mFilesNum = n;
		int offs = bin.getI32(0x24);
		mFileInfo = new int[n*2];
		for (int i = 0; i < n; ++i) {
			mFileInfo[i*2] = bin.getI32(offs + i*8);
			mFileInfo[i*2 + 1] = bin.getI32(offs + i*8 + 4);
		}
	}

	public int getFilesNum() {
		return mFileInfo != null ? mFilesNum : 0;
	}

	public boolean ckFileId(int id) {
		return id >= 0 && id < getFilesNum();
	}

	public String getShortName(int id) {
		String name = null;
		if (ckFileId(id)) {
			name = getStr(mFileInfo[id*2]);
		}
		return name;
	}

	public String getFullName(int id) {
		String name = null;
		if (ckFileId(id)) {
			name = getStr(mFileInfo[id*2 + 1]);
		}
		return name;
	}

}
