// Author: Sergey Chaban <sergey.chaban@gmail.com>

package flow;

import java.nio.*;
import java.lang.reflect.*;

import xdata.*;

import static android.opengl.GLES20.*;

public final class GLUtil {

	public static int compileShader(int type, String src, StringBuilder err) {
		int id = 0;
		if (src != null && src.length() > 1) {
			id = glCreateShader(type);
			if (id != 0) {
				glShaderSource(id, src);
				glCompileShader(id);
				int[] status = new int[1];
				glGetShaderiv(id, GL_COMPILE_STATUS, status, 0);
				if (status[0] == 0) {
					if (err != null) {
						err.append(glGetShaderInfoLog(id));
					}
					glDeleteShader(id);
					id = 0;
				}
			}
		}
		return id;
	}

	public static int compileVtxShader(String src, StringBuilder err) {
		return compileShader(GL_VERTEX_SHADER, src, err);
	}

	public static int compilePixShader(String src, StringBuilder err) {
		return compileShader(GL_FRAGMENT_SHADER, src, err);
	}

	public static void getProgLocs(Object obj, int progId, String attrLocPrefix, String prmLocPrefix, String smpLocPrefix) {
		Field[] flst = obj.getClass().getDeclaredFields();
		for (Field fld : flst) {
			String fldName = fld.getName();
			try {
				if (attrLocPrefix != null && fldName.startsWith(attrLocPrefix)) {
					String attrName = "vtx" + fldName.substring(attrLocPrefix.length());
					fld.setInt(obj, glGetAttribLocation(progId, attrName));
				} else if (prmLocPrefix != null && fldName.startsWith(prmLocPrefix)) {
					String prmName = "prm" + fldName.substring(prmLocPrefix.length());
					fld.setInt(obj, glGetUniformLocation(progId, prmName));
				} else if (smpLocPrefix != null && fldName.startsWith(smpLocPrefix)) {
					String smpName = "smp" + fldName.substring(smpLocPrefix.length());
					fld.setInt(obj, glGetUniformLocation(progId, smpName));
				}
			} catch (IllegalAccessException ae) {}
		}
	}

	public static void getProgLocs(Object obj, int progId) {
		getProgLocs(obj, progId, "mAttrLoc", "mPrmLoc", "mSmpLoc");
	}

	public static void getProgLocs(Object obj) {
		try {
			Field fldProgId = obj.getClass().getDeclaredField("mProgId");
			try {
				int progId = fldProgId.getInt(obj);
				getProgLocs(obj, progId);
			} catch (IllegalAccessException e) {}
		} catch (NoSuchFieldException e) {}
	}

	public static boolean makeProg(Object obj,
								   String vtxCode, String pixCode, StringBuilder err,
								   String fldNameProgId, String fldNameShaderIdVtx, String fldNameShaderIdPix
	) {
		Class cls = obj.getClass();
		return makeProg(obj, cls, vtxCode, pixCode, err, fldNameProgId, fldNameShaderIdVtx, fldNameShaderIdPix);
	}

	public static boolean makeProg(Object obj, Class cls,
	                      String vtxCode, String pixCode, StringBuilder err,
	                      String fldNameProgId, String fldNameShaderIdVtx, String fldNameShaderIdPix
	) {
		try {
			Field fldProgId = cls.getDeclaredField(fldNameProgId);
			Field fldShaderIdVtx = cls.getDeclaredField(fldNameShaderIdVtx);
			Field fldShaderIdPix = cls.getDeclaredField(fldNameShaderIdPix);
			fldProgId.setInt(obj, 0);
			fldShaderIdVtx.setInt(obj, 0);
			fldShaderIdPix.setInt(obj, 0);
			int vtxId = compileVtxShader(vtxCode, err);
			if (vtxId == 0) {
				return false;
			}
			int pixId = compilePixShader(pixCode, err);
			if (pixId == 0) {
				glDeleteShader(vtxId);
				return false;
			}
			int progId = glCreateProgram();
			if (progId == 0) {
				glDeleteShader(vtxId);
				glDeleteShader(pixId);
				return false;
			}
			glAttachShader(progId, vtxId);
			glAttachShader(progId, pixId);
			glLinkProgram(progId);
			int[] status = new int[1];
			glGetProgramiv(progId, GL_LINK_STATUS, status, 0);
			if (status[0] == 0) {
				if (err != null) {
					err.append(glGetProgramInfoLog(progId));
				}
				glDeleteProgram(progId);
				glDeleteShader(vtxId);
				glDeleteShader(pixId);
				return false;
			}
			fldShaderIdVtx.setInt(obj, vtxId);
			fldShaderIdPix.setInt(obj, pixId);
			fldProgId.setInt(obj, progId);
			return true;
		} catch (IllegalAccessException e) {
			if (err != null) {
				err.append(e.toString());
			}
		} catch (NoSuchFieldException e) {
			if (err != null) {
				err.append(e.toString());
			}
		}
		return false;
	}

	public static boolean makeProg(Object obj, Class cls, String vtxCode, String pixCode, StringBuilder err) {
		return makeProg(obj, cls, vtxCode, pixCode, err,
				"mProgId", "mShaderIdVtx", "mShaderIdPix");
	}

	public static boolean makeProg(Object obj, String vtxCode, String pixCode, StringBuilder err) {
		Class cls = obj.getClass();
		return makeProg(obj, cls, vtxCode, pixCode, err);
	}

	public static int uploadTex32NoMip(XTex tex) {
		return uploadTex32(tex, false);
	}

	public static int uploadTex32(XTex tex, boolean mipFlg) {
		int w = tex.getWidth();
		int h = tex.getHeight();
		ByteBuffer buf = ByteBuffer.allocateDirect(w * h * 4);
		buf.order(ByteOrder.nativeOrder());
		final int[] htex = new int[1];
		glGenTextures(1, htex, 0);
		if (htex[0] == 0) {
			return 0;
		}
		glPixelStorei(GL_UNPACK_ALIGNMENT, 1);
		TexParam texParam = new TexParam();
		texParam.setFiltLinear();
		float[] ftmp = new float[w * h * 4];
		float[] fclr = new float[4];
		glBindTexture(GL_TEXTURE_2D, htex[0]);
		tex.getRGBA(ftmp);
		buf.position(0);
		for (int j = 0; j < w*h; ++j) {
			for (int k = 0; k < 4; ++k) {
				fclr[k] = ftmp[j*4 + k];
			}
			Calc.vsaturate(fclr);
			Calc.vscl(fclr, 255.0f);
			for (int k = 0; k < 4; ++k) {
				buf.put((byte)fclr[k]);
			}
		}
		buf.position(0);
		glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA, w, h, 0, GL_RGBA, GL_UNSIGNED_BYTE, buf);
		if (mipFlg) {
			glGenerateMipmap(GL_TEXTURE_2D);
		}
		texParam.apply();
		glBindTexture(GL_TEXTURE_2D, 0);
		return htex[0];
	}

	public static void clearFrame(float r, float g, float b, float a) {
		glColorMask(true, true, true, true);
		glDepthMask(true);
		glClearColor(r, g, b, a);
		glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT | GL_STENCIL_BUFFER_BIT);
	}

	public static void clearFrame(float r, float g, float b) {
		clearFrame(r, g, b, 1.0f);
	}

}
