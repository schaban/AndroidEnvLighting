// Author: Sergey Chaban <sergey.chaban@gmail.com>

package flow;

import static android.opengl.GLES20.*;
import java.nio.*;
import xdata.*;

public class DbgText {

	protected int mTexHandle;
	protected int mShaderIdVtx;
	protected int mShaderIdPix;
	protected int mProgId;
	protected int mAttrLocPos;
	protected int mAttrLocTex;
	protected int mPrmLocClr;
	protected int mSmpLocFont;
	protected int mFntWidth;
	protected int mFntHeight;
	protected int mScrWidth;
	protected int mScrHeight;
	protected int mTexWidth;
	protected int mTexHeight;
	protected float mFontScl = 1.0f;
	protected ByteBuffer mVB;

	protected final static String s_vtx =
			"attribute vec2 vtxPos;\n" +
			"attribute vec2 vtxTex;\n" +
			"\n" +
			"varying vec2 pixTex;\n" +
			"\n" +
			"\n" +
			"void main() {\n" +
			"\tpixTex = vtxTex;\n" +
			"\tgl_Position = vec4(vtxPos, 0.0, 1.0);\n" +
			"}\n";

	protected final static String s_pix = "precision mediump float;\n" +
			"\n" +
			"varying vec2 pixTex;\n" +
			"\n" +
			"uniform sampler2D smpFont;\n" +
			"\n" +
			"uniform vec4 prmClr;\n" +
			"\n" +
			"void main() {\n" +
			"\tvec4 tex = texture2D(smpFont, pixTex);\n" +
			"\tgl_FragColor = tex * prmClr;\n" +
			"}\n";

	public boolean init(XTex fontTex, int fontW, int fontH) {
		mFntWidth = fontW;
		mFntHeight = fontH;

		if (fontTex == null) return false;
		mTexWidth = fontTex.getWidth();
		mTexHeight = fontTex.getHeight();

		mTexHandle = GLUtil.uploadTex32NoMip(fontTex);
		if (mTexHandle == 0) return false;

		StringBuilder err = new StringBuilder();
		boolean progOk = GLUtil.makeProg(this, s_vtx, s_pix, err);
		if (!progOk) {
			return false;
		}
		GLUtil.getProgLocs(this);

		int vbSize = 8 * 1024;
		ByteBuffer vb = ByteBuffer.allocateDirect(vbSize);
		vb.order(ByteOrder.nativeOrder());
		vb.position(0);
		mVB = vb;

		return true;
	}

	public boolean init(XTex fontTex) {
		return init(fontTex, 10, 16);
	}

	public void setFontScale(float s) {
		mFontScl = s;
	}

	public void setScrSize(int w, int h) {
		mScrWidth = w;
		mScrHeight = h;
	}

	public int getFontWidth() {
		return mFntWidth;
	}

	public int getFontHeight() {
		return mFntHeight;
	}

	public void print(String s, float x, float y, Color c) {
		print(s, x, y, c.r(), c.g(), c.b(), c.a());
	}

	public void print(String s, float x, float y, float r, float g, float b, float a) {
		if (mScrWidth == 0 || mScrHeight == 0) return;
		mVB.position(0);
		float sw = Calc.rcp0((float)mScrWidth);
		float sh = Calc.rcp0((float)mScrHeight);
		float sx = -1.0f + (x * sw);
		float sy = 1.0f - (y * sh);
		float fw = (float)mFntWidth;
		float fh = (float)mFntHeight;
		float rx = (fw - 0.0f) * sw * mFontScl;
		float ry = (fh - 0.0f) * sh * mFontScl;
		float dx = fw * sw * mFontScl;
		float su = Calc.rcp0((float)mTexWidth);
		float sv = Calc.rcp0((float)mTexHeight - 1);
		float du = (fw - 0.25f) * su;
		float dv = (fh - 0.25f) * sv;
		int n = s.length();
		int nvtx = 0;
		for (int i = 0; i < n; ++i) {
			int c = s.charAt(i) & 0xFF;
			if (c > 0x20) {
				c -= 0x20;
				int row = c >>> 4;
				int col = c & 0xF;
				float u = (float)col * fw * su;
				float v = (float)row * fh * sv;

				mVB.putFloat(sx);
				mVB.putFloat(sy);
				mVB.putFloat(u);
				mVB.putFloat(v);

				mVB.putFloat(sx + rx);
				mVB.putFloat(sy);
				mVB.putFloat(u + du);
				mVB.putFloat(v);

				mVB.putFloat(sx + rx);
				mVB.putFloat(sy - ry);
				mVB.putFloat(u + du);
				mVB.putFloat(v + dv);

				mVB.putFloat(sx);
				mVB.putFloat(sy);
				mVB.putFloat(u);
				mVB.putFloat(v);

				mVB.putFloat(sx + rx);
				mVB.putFloat(sy - ry);
				mVB.putFloat(u + du);
				mVB.putFloat(v + dv);

				mVB.putFloat(sx);
				mVB.putFloat(sy - ry);
				mVB.putFloat(u);
				mVB.putFloat(v + dv);

				nvtx += 3*2;
			}

			sx += dx;
		}

		int vtxBytes = 4 * 4;
		glUseProgram(mProgId);
		glBindBuffer(GL_ARRAY_BUFFER, 0);
		glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, 0);

		glEnableVertexAttribArray(mAttrLocPos);
		mVB.position(0);
		glVertexAttribPointer(mAttrLocPos, 2, GL_FLOAT, false, vtxBytes, mVB);

		glEnableVertexAttribArray(mAttrLocTex);
		mVB.position(8);
		glVertexAttribPointer(mAttrLocTex, 2, GL_FLOAT, false, vtxBytes, mVB);

		glActiveTexture(GL_TEXTURE0);
		glBindTexture(GL_TEXTURE_2D, mTexHandle);
		glUniform1i(mSmpLocFont, 0);

		glUniform4f(mPrmLocClr, r, g, b, a);

		glDepthMask(false);
		BlendState.semi();
		glDisable(GL_CULL_FACE);
		glDisable(GL_DEPTH_TEST);

		glDrawArrays(GL_TRIANGLES, 0, nvtx);

		glDisableVertexAttribArray(mAttrLocPos);
		glDisableVertexAttribArray(mAttrLocTex);

		//glDepthMask(true);
	}

}
