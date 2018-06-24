// Author: Sergey Chaban <sergey.chaban@gmail.com>

package xdata;

public enum RotOrd {
	XYZ,
	XZY,
	YXZ,
	YZX,
	ZXY,
	ZYX;

	public static RotOrd fromInt(int i) {
		RotOrd ord = XYZ;
		switch (i) {
			case 0: ord = XYZ; break;
			case 1: ord = XZY; break;
			case 2: ord = YXZ; break;
			case 3: ord = YZX; break;
			case 4: ord = ZXY; break;
			case 5: ord = ZYX; break;
		}
		return ord;
	}

	public static RotOrd fromString(String s) {
		RotOrd ord = XYZ;
		String us = s.toUpperCase();
		String[] lst = {"XYZ", "XZY", "YXZ", "YZX", "ZXY", "ZYX"};
		for (int i = 0; i < lst.length; ++i) {
			if (us.equals(lst[i])) {
				ord = fromInt(i);
				break;
			}
		}
		return ord;
	}
}
