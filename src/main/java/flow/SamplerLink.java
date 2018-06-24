// Author: Sergey Chaban <sergey.chaban@gmail.com>

package flow;

import java.lang.reflect.Field;
import static android.opengl.GLES20.*;
import geas.*;

public class SamplerLink {

	public static final int TEX_UNIT_BASE = 0;
	public static final int TEX_UNIT_SPEC = 1;
	public static final int TEX_UNIT_BUMP = 2;
	public static final int TEX_UNIT_PANO = 3;
	public static final int TEX_UNIT_CUBE = 4;

	public int mProgId = 0;
	public int mSmpLocBase;
	public int mSmpLocSpec;
	public int mSmpLocBump;
	public int mSmpLocPano;
	public int mSmpLocCube;

	private static final String SMP_LOC_PREFIX = "mSmpLoc";

	public SamplerLink() {
		reset();
	}

	public void reset() {
		mProgId = 0;
		Field[] flst = getClass().getDeclaredFields();
		for (Field fld : flst) {
			String fldName = fld.getName();
			try {
				if (fldName.startsWith(SMP_LOC_PREFIX)) {
					fld.setInt(this, -1);
				}
			} catch (IllegalAccessException ae) {}
		}
	}

	public void make(int progId) {
		reset();
		mProgId = progId;
		Field[] flst = getClass().getDeclaredFields();
		for (Field fld : flst) {
			String fldName = fld.getName();
			try {
				if (fldName.startsWith(SMP_LOC_PREFIX)) {
					String attrName = "smp" + fldName.substring(SMP_LOC_PREFIX.length());
					fld.setInt(this, glGetUniformLocation(mProgId, attrName));
				}
			} catch (IllegalAccessException ae) {}
		}
	}

}
