package geas;

public class GeoCfg {

	public enum AttrMode {
		NONE,
		AUTO,
		FORCE
	}

	public boolean mIsSharedVB = true;

	/*
	 	0: all floats
	 	1: all floats, octa n/t
	 	2: float pos, octa n/t, 16-bit attrs
	 	3: 16-bit rel pos, octa n/t, 16-bit attrs
	 */
	public int mPackLevel = 1;
	public boolean mUseHalf = false;
	public boolean mUseSkin = true;
	public boolean mUseLocalSkin = true;

	public AttrMode mNrmMode = AttrMode.AUTO;
	public AttrMode mTngMode = AttrMode.AUTO;

	public AttrMode mClrMode = AttrMode.AUTO;

	public AttrMode mTexMode = AttrMode.AUTO;
	public AttrMode mTex2Mode = AttrMode.NONE;

	public String mBatchGrpPrefix;

}
