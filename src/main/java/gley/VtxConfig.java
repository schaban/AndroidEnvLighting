// GLSL ES codegen
// Author: Sergey Chaban <sergey.chaban@gmail.com>

package gley;

public class VtxConfig extends ShaderConfig {

	public enum NrmMode {
		NONE,
		VEC,
		OCT
	}

	public enum WorldMode {
		STATIC,
		SOLID,
		SKIN1,
		SKIN2,
		SKIN4,
		SKIN8
	}

	public enum ClrMode {
		NONE,
		WHITE,
		RGB,
		RGBA
	}

	public enum TexMode {
		NONE,
		TEX,
		TEX2
	}

	public WorldMode mWorldMode;
	public NrmMode mNrmMode;
	public ClrMode mClrMode;
	public TexMode mTexMode = TexMode.NONE;
	public boolean mQuantPos = false;
	public boolean mUseTangent = false;
	public boolean mWriteBitangent = true;
	public boolean mPremultIdx = true;
	public boolean mFullWgt = true;
	public boolean mNormalize = true;
	public boolean mUseTexShift = false;
	public boolean mClrScl = false;
	public boolean mTexScl = false;

	public boolean hasVtxColor() {
		return mClrMode != ClrMode.NONE && mClrMode != ClrMode.WHITE;
	}

	public boolean hasSkinning() {
		return mWorldMode != WorldMode.STATIC && mWorldMode != WorldMode.SOLID;
	}

}
