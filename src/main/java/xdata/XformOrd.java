// Author: Sergey Chaban <sergey.chaban@gmail.com>

package xdata;

public enum XformOrd {
	SRT,
	STR,
	RST,
	RTS,
	TSR,
	TRS;

	private String s;

	public static XformOrd fromInt(int i) {
		XformOrd ord = SRT;
		switch (i) {
			case 0: ord = SRT; break;
			case 1: ord = STR; break;
			case 2: ord = RST; break;
			case 3: ord = RTS; break;
			case 4: ord = TSR; break;
			case 5: ord = TRS; break;
		}
		return ord;
	}

	public static XformOrd fromString(String s) {
		XformOrd ord = SRT;
		String us = s.toUpperCase();
		String[] lst = {"SRT", "STR", "RST", "RTS", "TSR", "TRS"};
		for (int i = 0; i < lst.length; ++i) {
			if (us.equals(lst[i])) {
				ord = fromInt(i);
				break;
			}
		}
		return ord;
	}
}
