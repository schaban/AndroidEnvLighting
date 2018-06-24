// Author: Sergey Chaban <sergey.chaban@gmail.com>

package flow;

import gley.*;

public class GLProg {

	protected GParam mGP;
	protected int mShaderIdVtx;
	protected int mShaderIdPix;
	protected int mProgId;
	protected GLFieldXfer mFieldXfer;
	protected SamplerLink mSmpLink;

	public boolean makeProg(GParam gp, VtxConfig vsCfg, PixConfig psCfg, StringBuilder err) {
		mGP = gp;

		StringBuilder vsCode = new StringBuilder();
		VtxShader vs = new VtxShader(mGP);
		vs.gen(vsCode, vsCfg, err);

		PixShader ps = new PixShader(mGP);
		StringBuilder psCode = new StringBuilder();
		ps.gen(psCode, psCfg, err);

		boolean ok = GLUtil.makeProg(this, GLProg.class, vsCode.toString(), psCode.toString(), err);
		if (!ok) {
			return false;
		}

		mFieldXfer = new GLFieldXfer();
		mFieldXfer.init(mProgId, mGP, vs.getUsage(), ps.getUsage());

		mSmpLink = new SamplerLink();
		mSmpLink.make(mProgId);

		return true;
	}

	public int getProgId() {
		return mProgId;
	}

	public GParam getGParam() {
		return mGP;
	}

	public FieldXfer getFieldXfer() {
		return mFieldXfer;
	}

	public SamplerLink getSamplerLink() {
		return mSmpLink;
	}

}
