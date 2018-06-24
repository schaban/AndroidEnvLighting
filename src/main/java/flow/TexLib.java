// Author: Sergey Chaban <sergey.chaban@gmail.com>

package flow;

import xdata.*;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.HashMap;

import static android.opengl.GLES20.*;

public class TexLib {

	public enum Mode {
		STANDARD,
		LOWQ,
		HRGB
	}

	protected int[] mTexHandles;
	protected String[] mTexNames;
	protected HashMap<String, Integer> mNameToIdx;
	protected boolean[] mTexSemiFlgs;

	public void reset() {
		if (mTexHandles != null) {
			int n = mTexHandles.length;
			glDeleteTextures(n, mTexHandles, 0);
		}
		mTexHandles = null;
	}

	public void init(XTex[] texs) {
		init(texs, Mode.STANDARD);
	}

	public void init(XTex[] texs, Mode mode) {
		reset();
		int n = texs.length;
		int maxw = 0;
		int maxh = 0;
		for (int i = 0; i < n; ++i) {
			maxw = Math.max(maxw, texs[i].getWidth());
			maxh = Math.max(maxh, texs[i].getHeight());
		}
		int[] htbl = new int[XTex.HTBL_SIZE];
		float[] ftmp = new float[maxw * maxh * 4];
		float[] fclr = new float[4];
		ByteBuffer buf = ByteBuffer.allocateDirect(maxw * maxh * 4);
		buf.order(ByteOrder.nativeOrder());
		boolean flg16 = mode == Mode.LOWQ;
		ByteBuffer pkbuf = null;
		if (flg16) {
			pkbuf = ByteBuffer.allocateDirect(maxw * maxh * 2);
			pkbuf.order(ByteOrder.nativeOrder());
		}
		mTexNames = new String[n];
		mTexHandles = new int[n];
		mTexSemiFlgs = new boolean[n];
		mNameToIdx = new HashMap<String, Integer>();
		glGenTextures(n, mTexHandles, 0);
		glPixelStorei(GL_UNPACK_ALIGNMENT, 1);
		TexParam defTexParam = new TexParam();
		defTexParam.setFiltLinear();
		for (int i = 0; i < n; ++i) {
			XTex xt = texs[i];
			if (xt == null) continue;
			int w = xt.getWidth();
			int h = xt.getHeight();
			mTexNames[i] = xt.mName;
			mNameToIdx.put(mTexNames[i], i);
			if (mTexHandles[i] > 0) {
				XTex.ImgPlane apln = xt.getPlane("a");
				boolean aflg = apln != null;
				if (aflg) {
					if (apln.isConst() && apln.mMaxVal == 1.0f) {
						aflg = false;
					}
				}
				xt.getRGBA(ftmp, 0, htbl);
				glBindTexture(GL_TEXTURE_2D, mTexHandles[i]);
				buf.position(0);
				boolean semi = false;
				if (mode == Mode.HRGB) {
					for (int j = 0; j < w*h; ++j) {
						for (int k = 0; k < 3; ++k) {
							fclr[k] = ftmp[j*4 + k];
						}
						float hscl = Math.max(fclr[0], Math.max(fclr[1], fclr[2]));
						if (hscl > 1.0f) {
							hscl = Calc.rcp0(hscl);
							hscl = Math.max(hscl, 0.01f);
							for (int k = 0; k < 3; ++k) {
								fclr[k] *= hscl;
							}
						} else {
							hscl = 1.0f;
						}
						fclr[3] = hscl;
						Calc.vsaturate(fclr);
						Calc.vscl(fclr, 255.0f);
						for (int k = 0; k < 4; ++k) {
							buf.put((byte)fclr[k]);
						}
					}
				} else {
					for (int j = 0; j < w*h; ++j) {
						for (int k = 0; k < 4; ++k) {
							fclr[k] = ftmp[j*4 + k];
						}
						Calc.vsaturate(fclr);
						if (aflg && !semi) {
							semi |= fclr[3] < 1.0f;
						}
						Calc.vscl(fclr, 255.0f);
						for (int k = 0; k < 4; ++k) {
							buf.put((byte)fclr[k]);
						}
					}
				}
				mTexSemiFlgs[i] = semi;
				if (pkbuf != null) {
					buf.position(0);
					pkbuf.position(0);
					if (semi) {
						for (int j = 0; j < w*h; ++j) {
							int ipix = buf.getInt();
							int ir = ipix & 0xFF;
							int ig = (ipix >> 8) & 0xFF;
							int ib = (ipix >>> 16) & 0xFF;
							int ia = (ipix >>> 24) & 0xFF;
							int sr = (ir >> 4) & 0xF;
							int sg = (ig >> 4) & 0xF;
							int sb = (ib >> 4) & 0xF;
							int sa = (ia >> 4) & 0xF;
							int spix = sa | (sb << 4) | (sg << 8) | (sr << 12);
							pkbuf.putShort((short)spix);
						}
						pkbuf.position(0);
						glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA, w, h, 0, GL_RGBA, GL_UNSIGNED_SHORT_4_4_4_4, pkbuf);
					} else {
						for (int j = 0; j < w * h; ++j) {
							int ipix = buf.getInt();
							int ir = ipix & 0xFF;
							int ig = (ipix >> 8) & 0xFF;
							int ib = (ipix >>> 16) & 0xFF;
							int sr = (ir >> 3) & 0x1F;
							int sg = (ig >> 3) & 0x1F;
							int sb = (ib >> 3) & 0x1F;
							int spix = 1 | (sb << 1) | (sg << 6) | (sr << 11);
							pkbuf.putShort((short)spix);
						}
						pkbuf.position(0);
						glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA, w, h, 0, GL_RGBA, GL_UNSIGNED_SHORT_5_5_5_1, pkbuf);
					}
				} else {
					buf.position(0);
					glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA, w, h, 0, GL_RGBA, GL_UNSIGNED_BYTE, buf);
				}
				glGenerateMipmap(GL_TEXTURE_2D);
				defTexParam.apply();
			}
		}
		glBindTexture(GL_TEXTURE_2D, 0);
	}

	public int getTexNum() {
		return mTexHandles != null ? mTexHandles.length : 0;
	}

	public boolean ckTexId(int id) {
		return id >= 0 && id < getTexNum();
	}

	public int findTex(String name) {
		int idx = -1;
		if (mNameToIdx != null) {
			Integer i = mNameToIdx.get(name);
			if (i != null) {
				idx = i;
			}
		}
		return idx;
	}

	public int getTexHandle(int id) {
		int h = 0;
		if (ckTexId(id)) {
			h = mTexHandles[id];
		}
		return h;
	}

	public int getTexHandle(String name) {
		return getTexHandle(findTex(name));
	}

	public boolean isTexSemi(int id) {
		return ckTexId(id) ? mTexSemiFlgs[id] : false;
	}

	public boolean isTexSemi(String name) {
		return isTexSemi(findTex(name));
	}

	protected void setTexParamNoCk(int id, TexParam param) {
		int h = mTexHandles[id];
		glBindTexture(GL_TEXTURE_2D, h);
		param.apply();
	}

	public void setTexParam(int id, TexParam param) {
		if (param != null && ckTexId(id)) {
			setTexParamNoCk(id, param);
			glBindTexture(GL_TEXTURE_2D, 0);
		}
	}

	public void setAllTexParams(TexParam param) {
		int n = getTexNum();
		if (n > 0 && param != null) {
			for (int i = 0; i < n; ++i) {
				setTexParamNoCk(i, param);
			}
		}
		glBindTexture(GL_TEXTURE_2D, 0);
	}

}
