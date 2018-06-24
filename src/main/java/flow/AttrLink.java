// Author: Sergey Chaban <sergey.chaban@gmail.com>

package flow;

import java.lang.reflect.Field;
import static android.opengl.GLES20.*;
import geas.*;

public class AttrLink {

	public int mProgId = 0;
	public int mAttrLocPos = -1;
	public int mAttrLocNrm = -1;
	public int mAttrLocTng = -1;
	public int mAttrLocTex = -1;
	public int mAttrLocClr = -1;
	public int mAttrLocJnt = -1;
	public int mAttrLocWgt = -1;

	private static final String ATTR_LOC_PREFIX = "mAttrLoc";

	public AttrLink() {
		reset();
	}

	public void reset() {
		mProgId = 0;
		Field[] flst = getClass().getDeclaredFields();
		for (Field fld : flst) {
			String fldName = fld.getName();
			try {
				if (fldName.startsWith(ATTR_LOC_PREFIX)) {
					fld.setInt(this, -1);
				}
			} catch (IllegalAccessException ae) {}
		}
	}

	public void make(GeoData gd, int progId) {
		reset();
		GeoData.VtxInfo vi = gd.getVtxInfo();
		if (!vi.hasPos()) return;
		mProgId = progId;
		Field[] flst = getClass().getDeclaredFields();
		for (Field fld : flst) {
			String fldName = fld.getName();
			try {
				if (fldName.startsWith(ATTR_LOC_PREFIX)) {
					String attrName = "vtx" + fldName.substring(ATTR_LOC_PREFIX.length());
					fld.setInt(this, glGetAttribLocation(mProgId, attrName));
				}
			} catch (IllegalAccessException ae) {}
		}
	}

}
