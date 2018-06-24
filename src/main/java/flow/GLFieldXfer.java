// Author: Sergey Chaban <sergey.chaban@gmail.com>

package flow;

import gley.*;

import static android.opengl.GLES20.*;

public class GLFieldXfer extends FieldXfer {

	protected int mProgId;

	public void init(int progId, GParam gp, int[] vtxUsage, int[] pixUsage) {
		mProgId = progId;
		int[] usage = gp.allocUsageTbl();
		for (int i = 0; i < usage.length; ++i) {
			usage[i] = vtxUsage[i] | pixUsage[i];
		}
		super.init(gp, usage);
	}

	protected int getFieldLocation(String name) {
		return glGetUniformLocation(mProgId, name);
	}

	protected void sendField(int idx) {
		int dstLoc = getNoCkFldDstLoc(idx);
		if (dstLoc < 0) return;
		int type = getFldType(idx);
		float[] srcF = mGP.getFloats();
		int srcOffs = getNoCkFldSrcOffs(idx);
		int arySize = getNoCkFldArySize(idx);
		int cnt = arySize > 1 ? arySize : 1;
		switch (type) {
			case GParam.T_FLOAT:
				if (cnt > 1) {
					glUniform1fv(dstLoc, cnt, srcF, srcOffs);
				} else {
					glUniform1f(dstLoc, srcF[srcOffs]);
				}
				break;
			case GParam.T_VEC2:
				glUniform2fv(dstLoc, cnt, srcF, srcOffs);
				break;
			case GParam.T_VEC3:
				glUniform3fv(dstLoc, cnt, srcF, srcOffs);
				break;
			case GParam.T_VEC4:
				glUniform4fv(dstLoc, cnt, srcF, srcOffs);
				break;
			case GParam.T_MAT4:
				glUniformMatrix4fv(dstLoc, cnt, false, srcF, srcOffs);
				break;
			case GParam.T_MAT3x4:
				glUniform4fv(dstLoc, cnt*3, srcF, srcOffs);
				break;
		}
	}

}
