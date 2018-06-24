// Author: Sergey Chaban <sergey.chaban@gmail.com>

package xdata;

public class XVal extends XData {

	protected final static byte VAL_TYPE_UNKNOWN = 0;
	protected final static byte VAL_TYPE_FLOAT = 1;
	protected final static byte VAL_TYPE_VEC2 = 2;
	protected final static byte VAL_TYPE_VEC3 = 3;
	protected final static byte VAL_TYPE_VEC4 = 4;
	protected final static byte VAL_TYPE_INT = 5;
	protected final static byte VAL_TYPE_STRING = 6;

	public void init(Binary bin) {
		super.init(bin);
		if (!mHead.isValues()) return;
	}

}
