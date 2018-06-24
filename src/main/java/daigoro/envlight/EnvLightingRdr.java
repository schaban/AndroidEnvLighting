package daigoro.envlight;

import java.util.Arrays;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

import android.content.Context;

import javax.microedition.khronos.opengles.GL10;
import javax.microedition.khronos.egl.EGLConfig;

import static android.opengl.GLES20.*;
import android.opengl.GLSurfaceView.Renderer;


import xdata.*;
import gley.*;
import geas.*;
import flow.*;

public class EnvLightingRdr implements Renderer {

	private boolean mUseChr = !true;

	private boolean mUseDistLight = !false;
	private boolean mUseDistLightClr = true;

	private boolean mUseTonemap = true;

	private boolean mUseExposure = !false;
	private boolean mAutoExposure = true;

	private boolean mUseSpecVclr = true;

	private boolean mDispFPS = true;

	enum CamMode {
		STATIC,
		ORBIT
	}
	private CamMode mCamMode = CamMode.ORBIT;

	// 72, 98, 253, 166
	private int mCharPoseFrame = -1; // < 0 == animate

	enum ChrDrawMode {
		FULL,
		RAW,
		RAW_DIFF,
		RAW_REFL,
		TEX_DIFF,
		TEX_REFL
	}
	private ChrDrawMode mChrDrawMode = ChrDrawMode.FULL;

	private Context mCtx;
	private int mFrameCount;
	private int mWidth;
	private int mHeight;
	protected long mSleepNanos = 0;

	protected GParam mGP;
	private CmdList mCmdList;

	private DbgText mDbgText;

	protected GLProg mProgEnv;
	protected GLProg mProgObj;
	protected GLProg mProgChr;

	protected Camera mCam;

	private float[] mSHConsts;

	private XTex mPanoTex;

	private float[] mPanoSHR;
	private float[] mPanoSHG;
	private float[] mPanoSHB;
	private float[] mPanoSHIrrad;
	private float[] mPanoSHRefl;
	private Vec mPanoLightDir;
	private Color mPanoLightClr;
	private float mPanoLightLum;
	private Color mPanoAmbient;
	private Color mPanoRawAmbient;
	private float mPanoAmbientLum;

	private TexLib mGlbTexLib;

	private XGeo mEnvGeo;
	private GLGeoData mEnvMdl;
	private Mtx mEnvWorld;
	private DrawState mEnvDrawState;
	private AttrLink mEnvAttrLink;
	private EnvCmdCall mEnvCmdCall;
	protected float[] mEnvCullingBoxes;
	protected int[] mEnvCullingBits;

	private XGeo mObjGeo;
	private GLGeoData mObjMdl;
	private Mtx mObjWorld;
	private DrawState mObjDrawState;
	private AttrLink mObjAttrLink;
	private ObjCmdCall mObjCmdCall;

	private TexLib mChrTexLib;
	private GLGeoData mChrMdl;
	protected AnimRig mChrRig;
	protected AnimRig.MotList mChrMotList;
	private int mChrFrame = 0;
	protected float[] mChrSkinXforms;
	private DrawState mChrDrawState;
	private AttrLink mChrAttrLink;
	private ChrCmdCall mChrCmdCall;
	protected int[] mChrBatTexMap;

	private FPSInfo mFPSInfo = new FPSInfo();

	public EnvLightingRdr(Context ctx) {
		mCtx = ctx;
	}

	public void start() {
		int projOrder = 3;
		mSHConsts = SH.calcConsts(projOrder);

		int[] panos = new int[] {
				R.raw.pano_test1,
				R.raw.pano_test2,
				R.raw.pano_test3,
				R.raw.pano_test4,
				R.raw.pano_test5,
				R.raw.pano_test6,
				R.raw.pano_test7,
				R.raw.pano_test8
		};
		int panoSel = (int)((System.currentTimeMillis() >>> 4) & 0xFF);
		//panoSel = 7;//////////////
		int panoIdx = panoSel % panos.length;
		int panoRID = panos[panoIdx];
		mPanoTex = TestUtil.loadTex(mCtx, panoRID);
		mPanoSHR = SH.allocCoefs(projOrder);
		mPanoSHG = SH.allocCoefs(projOrder);
		mPanoSHB = SH.allocCoefs(projOrder);
		SH.pano(projOrder, mSHConsts, mPanoSHR, 0, 1, mPanoSHG, 0, 1, mPanoSHB, 0, 1, mPanoTex);

		mPanoSHIrrad = new float[3*3 * 3];
		float[] wgt = SH.allocWeights(3);
		SH.diffWeights(wgt, 3);
		//SH.calcWeights(wgt, 3, 12.0f, 1.0f);
		float[] tmpCoefs = SH.allocCoefs(3);
		SH.applyWeights(tmpCoefs, 3, mPanoSHR, wgt);
		for (int i = 0; i < 3*3; ++i) {
			mPanoSHIrrad[i*3] = tmpCoefs[i];
		}
		SH.applyWeights(tmpCoefs, 3, mPanoSHG, wgt);
		for (int i = 0; i < 3*3; ++i) {
			mPanoSHIrrad[i*3 + 1] = tmpCoefs[i];
		}
		SH.applyWeights(tmpCoefs, 3, mPanoSHB, wgt);
		for (int i = 0; i < 3*3; ++i) {
			mPanoSHIrrad[i*3 + 2] = tmpCoefs[i];
		}

		mPanoSHRefl = new float[3*3 * 3];
		SH.calcWeights(wgt, 3, mUseChr ? 15.0f : 16.0f, 1.0f);
		SH.applyWeights(tmpCoefs, 3, mPanoSHR, wgt);
		for (int i = 0; i < 3*3; ++i) {
			mPanoSHRefl[i*3] = tmpCoefs[i];
		}
		SH.applyWeights(tmpCoefs, 3, mPanoSHG, wgt);
		for (int i = 0; i < 3*3; ++i) {
			mPanoSHRefl[i*3 + 1] = tmpCoefs[i];
		}
		SH.applyWeights(tmpCoefs, 3, mPanoSHB, wgt);
		for (int i = 0; i < 3*3; ++i) {
			mPanoSHRefl[i*3 + 2] = tmpCoefs[i];
		}

		mPanoLightDir = SH.extractDominantDir(mPanoSHR, mPanoSHG, mPanoSHB);
		float[] dirCoefs = SH.allocCoefs(projOrder);
		SH.eval(projOrder, dirCoefs, mSHConsts, mPanoLightDir);
		mPanoLightClr = new Color();
		mPanoLightClr.fill(0.0f);
		for (int i = 0; i < 3*3; ++i) {
			for (int j = 0; j < 3; ++j) {
				mPanoLightClr.ch[j] += mPanoSHRefl[i*3 + j] * dirCoefs[i];
			}
		}
		mPanoLightClr.a(2.0f);
		mPanoLightLum = mPanoLightClr.luminance();
		float cmax = mPanoLightClr.max();
		mPanoLightClr.scl(Calc.rcp0(cmax));
		mPanoLightDir.neg();

		mPanoAmbient = new Color();
		for (int i = 0; i < 3; ++i) {
			mPanoAmbient.ch[i] = mPanoSHIrrad[i];// * 5.317363f;
		}
		mPanoAmbient.a(2.0f);
		mPanoRawAmbient = new Color(mPanoAmbient);
		mPanoAmbientLum = mPanoAmbient.luminance();
		cmax = mPanoAmbient.max();
		mPanoAmbient.scl(Calc.rcp0(cmax));

		mEnvGeo = TestUtil.loadGeo(mCtx, R.raw.sphere);
		mObjGeo = TestUtil.loadGeo(mCtx, R.raw.teapot);
	}

	public class EnvCmdCall implements CallIfc {
		public void cmdListExec(int param) {
			int batId = param;
			if (!mEnvMdl.ckBatchId(batId)) return;
			mEnvMdl.drawBatch(batId, mEnvAttrLink);
		}
	}

	private void initEnvMdl() {
		if (mEnvGeo == null) return;
		if (!mEnvGeo.isAllTris()) return;
		GLGeoData gd = new GLGeoData();
		GeoCfg cfg = new GeoCfg();
		cfg.mPackLevel = 2;
		cfg.mBatchGrpPrefix = "xbat_";
		cfg.mIsSharedVB = false;
		gd.fromXGeo(mEnvGeo, cfg);
		gd.initGLBuffers();
		mEnvMdl = gd;
		mEnvWorld = new Mtx();
		mEnvWorld.identity();
		mEnvWorld.setScaling(5.0f -2);
		if (mUseChr) {
			mEnvWorld.setTranslation(0.0f, 1.0f, 0.0f);
		}
		mEnvDrawState = new DrawState();
		mEnvDrawState.setCullCW();
		mEnvCmdCall = new EnvCmdCall();
		mEnvAttrLink = new AttrLink();
		mEnvAttrLink.make(mEnvMdl, mProgEnv.getProgId());
		int nbat = mEnvMdl.getBatchesNum();
		mEnvCullingBoxes = new float[nbat * 6];
		mEnvCullingBits = new int[Util.calcBitAryIntSize(nbat)];
		mEnvMdl.getXformedBatchBoxes(mEnvWorld, mEnvCullingBoxes);
	}

	protected void setPixCC(PixConfig psCfg) {
		int ncc = 1;
		if (mUseTonemap) ++ncc;
		if (mUseExposure) ++ncc;
		psCfg.mCCFuncs = new PixConfig.ColorCorrectFunc[ncc];
		int icc = 0;
		if (mUseTonemap) {
			psCfg.mCCFuncs[icc++] = PixConfig.ColorCorrectFunc.TONEMAP;
		}
		if (mUseExposure) {
			psCfg.mCCFuncs[icc++] = PixConfig.ColorCorrectFunc.EXPOSURE;
		}
		psCfg.mCCFuncs[icc] = PixConfig.ColorCorrectFunc.GAMMA;
	}

	protected boolean initEnvProg(StringBuilder errsb) {
		VtxConfig vsCfg = new VtxConfig();
		vsCfg.mWorldMode = VtxConfig.WorldMode.SOLID;
		vsCfg.mQuantPos = false;
		vsCfg.mNrmMode = VtxConfig.NrmMode.OCT;
		vsCfg.mUseTangent = false;
		vsCfg.mWriteBitangent = true;
		vsCfg.mClrMode = VtxConfig.ClrMode.NONE;
		vsCfg.mClrScl = false;
		vsCfg.mTexMode = VtxConfig.TexMode.NONE;
		vsCfg.mTexScl = false;

		StringBuilder vsCode = new StringBuilder();
		VtxShader vs = new VtxShader(mGP);
		vs.gen(vsCode, vsCfg, errsb);

		PixConfig psCfg = new PixConfig();
		psCfg.mHighPrecision = false;
		psCfg.mSrcBase = PixConfig.BaseMapSource.WHITE;
		psCfg.mSrcSpec = PixConfig.SpecMapSource.BASE_AVG;
		psCfg.mBumpMode = PixConfig.BumpMode.NONE;
		psCfg.mTngMode = PixConfig.TangentMode.GEOM;
		psCfg.mVtxClrMode = PixConfig.VtxClrMode.NONE;
		psCfg.mAmbMode = PixConfig.AmbientMode.NONE;
		psCfg.mAmbScaling = false;
		psCfg.mReflMode = PixConfig.ReflMode.NONE;
		psCfg.mDistLightMode = PixConfig.LightMode.NONE;
		psCfg.mOmniLightMode = PixConfig.LightMode.NONE;
		psCfg.mDiffFunc = PixConfig.DiffFunc.LAMBERT;
		psCfg.mSpecFunc = PixConfig.SpecFunc.GGX;
		psCfg.mUseReflViewFresnel = false;
		psCfg.mUseSpecViewFresnel = false;
		psCfg.mUseSpecFresnel = false;
		psCfg.mFogMode = PixConfig.FogMode.NONE;
		psCfg.mEnvDiffMode = PixConfig.EnvDiffMode.PANO;
		setPixCC(psCfg);

		PixShader ps = new PixShader(mGP);
		StringBuilder psCode = new StringBuilder();
		ps.gen(psCode, psCfg, errsb);

		mProgEnv = new GLProg();
		return mProgEnv.makeProg(mGP, vsCfg, psCfg, errsb);
	}

	public class ObjCmdCall implements CallIfc {
		public void cmdListExec(int param) {
			int batId = param;
			if (!mObjMdl.ckBatchId(batId)) return;
			mObjMdl.drawBatch(batId, mObjAttrLink);
		}
	}

	private void initObjMdl() {
		if (mObjGeo == null) return;
		if (!mObjGeo.isAllTris()) return;

		GLGeoData gd = new GLGeoData();
		GeoCfg cfg = new GeoCfg();
		cfg.mPackLevel = 2;
		cfg.mBatchGrpPrefix = "xbat_";
		cfg.mIsSharedVB = false;
		gd.fromXGeo(mObjGeo, cfg);
		gd.initGLBuffers();
		mObjMdl = gd;
		mObjWorld = new Mtx();
		mObjWorld.identity();
		mObjDrawState = new DrawState();
		mObjCmdCall = new ObjCmdCall();
		mObjAttrLink = new AttrLink();
		mObjAttrLink.make(mObjMdl, mProgObj.getProgId());
	}

	protected boolean initObjProg(StringBuilder errsb) {
		VtxConfig vsCfg = new VtxConfig();
		vsCfg.mWorldMode = VtxConfig.WorldMode.SOLID;
		vsCfg.mQuantPos = false;
		vsCfg.mNrmMode = VtxConfig.NrmMode.OCT;
		vsCfg.mUseTangent = false;
		vsCfg.mWriteBitangent = true;
		vsCfg.mClrMode = VtxConfig.ClrMode.NONE;
		vsCfg.mClrScl = false;
		vsCfg.mTexMode = VtxConfig.TexMode.NONE;
		vsCfg.mTexScl = false;

		StringBuilder vsCode = new StringBuilder();
		VtxShader vs = new VtxShader(mGP);
		vs.gen(vsCode, vsCfg, errsb);

		PixConfig psCfg = new PixConfig();
		psCfg.mHighPrecision = false;
		psCfg.mSrcBase = PixConfig.BaseMapSource.WHITE;
		psCfg.mSrcSpec = PixConfig.SpecMapSource.BASE_AVG;
		psCfg.mBumpMode = PixConfig.BumpMode.NONE;
		psCfg.mTngMode = PixConfig.TangentMode.GEOM;
		psCfg.mVtxClrMode = PixConfig.VtxClrMode.NONE;
		psCfg.mAmbMode = PixConfig.AmbientMode.NONE;
		psCfg.mAmbScaling = false;
		psCfg.mReflMode = PixConfig.ReflMode.NONE;
		psCfg.mDistLightMode = mUseDistLight ? PixConfig.LightMode.SPEC : PixConfig.LightMode.NONE;
		psCfg.mOmniLightMode = PixConfig.LightMode.NONE;
		psCfg.mDiffFunc = PixConfig.DiffFunc.LAMBERT;
		psCfg.mSpecFunc = PixConfig.SpecFunc.GGX;
		psCfg.mUseReflViewFresnel = true;
		psCfg.mUseSpecViewFresnel = false;
		psCfg.mUseSpecFresnel = true;
		psCfg.mFogMode = PixConfig.FogMode.NONE;
		switch (mChrDrawMode) {
			case RAW_DIFF:
			case TEX_DIFF:
				psCfg.mEnvDiffMode = PixConfig.EnvDiffMode.SH3;
				psCfg.mEnvReflMode = PixConfig.EnvReflMode.NONE;
				break;
			case RAW_REFL:
			case TEX_REFL:
				psCfg.mEnvDiffMode = PixConfig.EnvDiffMode.NONE;
				psCfg.mEnvReflMode = PixConfig.EnvReflMode.SH3;
				break;
			default:
				psCfg.mEnvDiffMode = PixConfig.EnvDiffMode.SH3;
				psCfg.mEnvReflMode = PixConfig.EnvReflMode.SH3;
				break;
		}
		setPixCC(psCfg);

		PixShader ps = new PixShader(mGP);
		StringBuilder psCode = new StringBuilder();
		ps.gen(psCode, psCfg, errsb);

		mProgObj = new GLProg();
		return mProgObj.makeProg(mGP, vsCfg, psCfg, errsb);
	}

	protected void initChrMdl() {
		XRig rig = TestUtil.loadRig(mCtx, R.raw.chr_rig);
		XGeo geo = TestUtil.loadGeo(mCtx, R.raw.chr_mdl);
		XTex[] texs = TestUtil.loadTexAry(mCtx, R.raw.chr_texlib);
		mChrTexLib = new TexLib();
		mChrTexLib.init(texs);
		mChrRig = new AnimRig();
		mChrRig.init(rig);
		GeoCfg cfg = new GeoCfg();
		cfg.mBatchGrpPrefix = "xbat_";
		cfg.mIsSharedVB = false;
		cfg.mPackLevel = 3;
		cfg.mNrmMode = GeoCfg.AttrMode.FORCE;
		cfg.mClrMode = mUseSpecVclr ? GeoCfg.AttrMode.FORCE : GeoCfg.AttrMode.NONE;
		cfg.mTngMode = GeoCfg.AttrMode.NONE;
		cfg.mTexMode = GeoCfg.AttrMode.FORCE;
		cfg.mUseSkin = true;
		cfg.mUseLocalSkin = false;
		GLGeoData gd = new GLGeoData();
		gd.fromXGeo(geo, cfg, rig);
		gd.initGLBuffers();
		mChrMdl = gd;
		mChrSkinXforms = mChrMdl.allocSkinXforms();
		mChrDrawState = new DrawState();
		mChrCmdCall = new ChrCmdCall();
		mChrAttrLink = new AttrLink();
		mChrAttrLink.make(mChrMdl, mProgChr.getProgId());

		mChrBatTexMap = GeoUtil.getBatTexMapFromGlbAttr(mChrMdl, geo, mChrTexLib, "texmap");

		int[] motRIDs = new int[]{R.raw.chr_stand};
		MotClip[] clips = TestUtil.loadMotClips(mCtx, motRIDs);
		mChrMotList = mChrRig.createMotList(clips);
		int fnum = mChrMotList.getClipFramesNum(0);
		//mChrFrame = (int)(System.currentTimeMillis() % (fnum/2));

		mChrRig.setWorldPos(0.0f, 0.0f, 0.0f);
		mChrRig.setWorldRotY(0.0f);
		mChrMotList.applyClip(0, 0);
		mChrRig.updateCoord();
		mChrRig.calcWorld();
		mChrRig.blendInit(0);
	}

	protected boolean initChrProg(StringBuilder errsb) {
		VtxConfig vsCfg = new VtxConfig();
		vsCfg.mWorldMode = VtxConfig.WorldMode.SKIN4;
		vsCfg.mQuantPos = true;
		vsCfg.mNrmMode = VtxConfig.NrmMode.OCT;
		vsCfg.mUseTangent = false;
		vsCfg.mWriteBitangent = true;
		vsCfg.mClrMode = mUseSpecVclr ? VtxConfig.ClrMode.RGB : VtxConfig.ClrMode.NONE;
		vsCfg.mClrScl = true;
		vsCfg.mTexMode = VtxConfig.TexMode.TEX;
		vsCfg.mTexScl = true;

		StringBuilder vsCode = new StringBuilder();
		VtxShader vs = new VtxShader(mGP);
		vs.gen(vsCode, vsCfg, errsb);

		PixConfig psCfg = new PixConfig();
		psCfg.mHighPrecision = false;
		switch (mChrDrawMode) {
			case RAW:
			case RAW_DIFF:
			case RAW_REFL:
				psCfg.mSrcBase = PixConfig.BaseMapSource.WHITE;
				break;
			default:
				psCfg.mSrcBase = PixConfig.BaseMapSource.TEX;
				break;
		}
		psCfg.mSrcSpec = PixConfig.SpecMapSource.BASE;
		psCfg.mBumpMode = PixConfig.BumpMode.NONE;
		psCfg.mTngMode = PixConfig.TangentMode.GEOM;
		psCfg.mVtxClrMode = mUseSpecVclr ? PixConfig.VtxClrMode.MASK : PixConfig.VtxClrMode.NONE;
		psCfg.mAmbMode = PixConfig.AmbientMode.NONE;
		psCfg.mAmbScaling = false;
		psCfg.mReflMode = PixConfig.ReflMode.NONE;
		psCfg.mDistLightMode = mUseDistLight ? PixConfig.LightMode.SPEC : PixConfig.LightMode.NONE;
		psCfg.mOmniLightMode = PixConfig.LightMode.NONE;
		psCfg.mDiffFunc = PixConfig.DiffFunc.LAMBERT;
		psCfg.mSpecFunc = PixConfig.SpecFunc.GGX;
		psCfg.mUseReflViewFresnel = true;
		psCfg.mUseSpecViewFresnel = false;
		psCfg.mUseSpecFresnel = true;
		psCfg.mFogMode = PixConfig.FogMode.NONE;
		switch (mChrDrawMode) {
			case RAW_DIFF:
			case TEX_DIFF:
				psCfg.mEnvDiffMode = PixConfig.EnvDiffMode.SH3;
				psCfg.mEnvReflMode = PixConfig.EnvReflMode.NONE;
				break;
			case RAW_REFL:
			case TEX_REFL:
				psCfg.mEnvDiffMode = PixConfig.EnvDiffMode.NONE;
				psCfg.mEnvReflMode = PixConfig.EnvReflMode.SH3;
				break;
			default:
				psCfg.mEnvDiffMode = PixConfig.EnvDiffMode.SH3;
				psCfg.mEnvReflMode = PixConfig.EnvReflMode.SH3;
				break;
		}
		psCfg.mSpecFadeDown = true;
		setPixCC(psCfg);

		PixShader ps = new PixShader(mGP);
		StringBuilder psCode = new StringBuilder();
		ps.gen(psCode, psCfg, errsb);

		mProgChr = new GLProg();
		return mProgChr.makeProg(mGP, vsCfg, psCfg, errsb);
	}

	protected boolean initProgs(StringBuilder errsb) {
		boolean ok = initEnvProg(errsb);
		if (!ok) return false;
		ok = initObjProg(errsb);
		if (!ok) return false;
		ok = initChrProg(errsb);
		if (!ok) return false;
		return true;
	}

	private boolean initGPU() {
		mGP = new GParam();
		mGP.init();
		mCmdList = new CmdList();
		mCmdList.init(mGP);

		StringBuilder errsb = new StringBuilder();
		boolean progOk = initProgs(errsb);
		if (!progOk) {
			return false;
		}
		return false;
	}

	private void ctrlCamOrbit() {
		Vec tgt = mUseChr ? new Vec(0.0f, 1.3f, 0.0f) : new Vec(0.0f, 0.0f, 0.0f);

		int frm = mFrameCount;
		float dy = frm;
		Mtx vr = new Mtx();
		vr.setDegrees(0.0f, dy, 0.0f);
		float yp = 500;
		float yt = (frm % yp) / (yp - 1);
		yt = (yt < 0.5f ? yt : 1.0f - yt) * 2;
		yt = Calc.ease(yt);
		yt = Calc.fit(yt, 0, 1, 1, -1);
		yt *= 0.25f;
		yt -= 0.1f;
		Vec pos = vr.calcVec(new Vec(0.0f, yt, mUseChr ? 1.75f : 0.5f));
		pos.add(tgt);

		Vec up = new Vec(0.0f, 1.0f, 0.0f);
		mCam.update(pos, tgt, up, Calc.radians(30.0f), 0.1f, 500.0f, mWidth, mHeight);
	}

	private void ctrlCamStatic() {
		Vec tgt = new Vec(0.0f, 0.0f, 0.0f);
		Vec pos = new Vec(0.0f, 0.0f, 2.0f);
		if (mUseChr) {
			tgt = new Vec(0.0f, 1.3f, 0.0f);

			//pos = new Vec(0.6f, 1.44f, 1.64f);
			//pos = new Vec(0.0f, 1.23f, 1.5f);

			//pos = new Vec(0.75f, 1.1f, 1.25f);

			pos = new Vec(1.02f, 1.25f, 1.27f);
			//pos = new Vec(1.5f, 1.24f, 0.52f);
			//pos = new Vec(1.42f, 1.3f, -1.18f);
		}
		Vec up = new Vec(0.0f, 1.0f, 0.0f);
		mCam.update(pos, tgt, up, Calc.radians(30.0f), 0.1f, 500.0f, mWidth, mHeight);
	}

	private void ctrlCam() {
		switch (mCamMode) {
			case STATIC:
				ctrlCamStatic();
				break;
			case ORBIT:
			default:
				ctrlCamOrbit();
				break;
		}
	}

	private void ctrl() {
		if (mUseChr) {
			AnimRig rig = mChrRig;
			int motId = 0;
			int fnum = mChrMotList.getClipFramesNum(motId);
			int fadd = 1;
			if (mCharPoseFrame >= 0) {
				mChrFrame = mCharPoseFrame % fnum;
				fadd = 0;
			}
			mChrMotList.applyClip(motId, mChrFrame);
			rig.calcSupportJoints();
			//rig.updateCoord();
			rig.blendExec();
			rig.calcWorld();
			mChrFrame += fadd;
			if (mChrFrame > fnum - 1) {
				mChrFrame = 0;
			}
		}
		ctrlCam();
	}

	private void putEnv(CmdList cmd) {
		int nbat = mEnvMdl.getBatchesNum();
		int ncull = 0;
		Arrays.fill(mEnvCullingBits, 0);
		for (int i = 0; i < nbat; ++i) {
			boolean cullFlg = mCam.cullBox(mEnvCullingBoxes, i * 6);
			if (cullFlg) {
				Util.bitSt(mEnvCullingBits, i);
				++ncull;
			}
		}
		cmd.startPacket(0);
		cmd.putOPAQ();
		cmd.putDRWST(mEnvDrawState);
		Mtx tm = new Mtx();
		tm.transpose(mEnvWorld);
		cmd.putSETF("WORLD.mtx", 0, tm.el, 0, 3*4);
		cmd.putSETF("MATERIAL.baseColor", 1.0f, 1.0f, 1.0f);
		cmd.putSETF("MATERIAL.specColor", 1.0f, 1.0f, 1.0f);
		cmd.putPROG(mProgEnv.getProgId());
		cmd.putSEND(mProgEnv.getFieldXfer());
		cmd.putTEX(SamplerLink.TEX_UNIT_PANO, mGlbTexLib.getTexHandle(0), mProgEnv.getSamplerLink().mSmpLocPano);
		for (int i = 0; i < nbat; ++i) {
			if (!Util.bitCk(mEnvCullingBits, i)) {
				cmd.putCALL(mEnvCmdCall, i);
			}
		}
		cmd.endPacket();
	}

	private void putObj(CmdList cmd) {
		cmd.startPacket(0);
		cmd.putOPAQ();
		cmd.putDRWST(mObjDrawState);
		Mtx tm = new Mtx();
		tm.transpose(mObjWorld);
		cmd.putSETF("WORLD.mtx", 0, tm.el, 0, 3*4);
		cmd.putSETF("MATERIAL.viewFresnel", 0.02f, 0.0f, 0.2f, 0.0f);
		float diffVal = 0.95f;
		cmd.putSETF("MATERIAL.baseColor", 1.0f*diffVal, 1.0f*diffVal, 1.0f*diffVal);
		float specVal = 2.0f;
		cmd.putSETF("MATERIAL.specColor", 1.0f*specVal, 1.0f*specVal, 1.0f*specVal);
		cmd.putPROG(mProgObj.getProgId());
		cmd.putSEND(mProgObj.getFieldXfer());
		int nbat = mObjMdl.getBatchesNum();
		for (int i = 0; i < nbat; ++i) {
			cmd.putCALL(mObjCmdCall, i);
		}
		cmd.endPacket();
	}

	public class ChrCmdCall implements CallIfc {
		public void cmdListExec(int param) {
			int batId = param;
			if (!mChrMdl.ckBatchId(batId)) return;
			mChrMdl.drawBatch(batId, mChrAttrLink);
		}
	}

	private void putChr(CmdList cmd) {
		mChrMdl.calcSkinXforms(mChrSkinXforms, mChrRig.getWorldXforms());
		int nbat = mChrMdl.getBatchesNum();
		int njnt = mChrMdl.getSkinNodesNum();
		cmd.startPacket(0);
		GeoData.VtxInfo vi = mChrMdl.getVtxInfo();
		if (vi.isQuantPos()) {
			cmd.putSETF("VERTEX.posBase", mChrMdl.getQuantBase().el);
			cmd.putSETF("VERTEX.posScl", mChrMdl.getQuantScale().el);
		}
		float cscl = Calc.rcp0(vi.getClrScl());
		float tscl = Calc.rcp0(vi.getTexScl());
		if (cscl != 1.0f || tscl != 1.0f) {
			cmd.putSETF("VERTEX.attrScl", cscl, tscl, 1.0f, 1.0f);
		}
		cmd.putOPAQ();
		cmd.putDRWST(mChrDrawState);
		cmd.putSETF("SKIN.mtx", 0, mChrSkinXforms, 0, njnt * 3*4);
		cmd.putSETF("MATERIAL.viewFresnel", 0.02f, 0.0f, 0.1f, 0.0f);
		float diffVal = 3.0f;
		float specVal = 2.0f -0.5f;
		switch (mChrDrawMode) {
			case RAW:
				diffVal = 0.95f;
				specVal = 1.0f;
				break;
			case RAW_DIFF:
				diffVal = 0.95f;
				break;
			case RAW_REFL:
				specVal = 1.0f;
				break;
		}
		cmd.putSETF("MATERIAL.baseColor", 1.0f*diffVal, 1.0f*diffVal, 1.0f*diffVal);
		cmd.putSETF("MATERIAL.specColor", 1.0f*specVal, 1.0f*specVal, 1.0f*specVal);
		float[] diffRough = new float[] {0.6f, 0.6f, 0.6f};
		//for (int i = 0; i < 3; ++i) { diffRough[i] = mPanoLightClr.ch[i] * 0.6f; }//////////
		float[] diffSHAdj = new float[3];
		for (int i = 0; i < 3; ++i) {
			float r = Math.max(diffRough[i], 0.1f);
			diffSHAdj[i] = Calc.rcp0(Calc.sq(r) / 2.0f);
		}
		cmd.putSETF("MATERIAL.diffSHAdj", diffSHAdj);
		cmd.putPROG(mProgChr.getProgId());
		cmd.putSEND(mProgChr.getFieldXfer());
		boolean singleTex = mChrTexLib.getTexNum() == 1;
		if (singleTex) {
			cmd.putTEX(SamplerLink.TEX_UNIT_BASE, mChrTexLib.getTexHandle(0), mProgChr.getSamplerLink().mSmpLocBase);
		}
		for (int i = 0; i < nbat; ++i) {
			if (!singleTex) {
				int texId = mChrBatTexMap[i];
				int htex = mChrTexLib.getTexHandle(texId);
				cmd.putTEX(SamplerLink.TEX_UNIT_BASE, htex, mProgChr.getSamplerLink().mSmpLocBase);
			}
			cmd.putCALL(mChrCmdCall, i);
		}
		cmd.endPacket();
	}

	private void drawInfo() {
		float fontScl = 5.0f;
		boolean reducedRes = mWidth*mHeight < 1e6;
		if (reducedRes) {
			fontScl /= 2;
		}
		mDbgText.setFontScale(fontScl);
		float x = 8 * fontScl;
		float y = 8 * fontScl;
		float dy = 20 * fontScl;
		String str = "";
		if (mDispFPS) {
			str = String.format(Locale.US, "FPS: %.2f", mFPSInfo.mFPS);
			mDbgText.print(str, x + fontScl, y + fontScl, 0.0f, 0.0f, 0.0f, 2.0f);
			mDbgText.print(str, x, y, 0.5f, 1.0f, 0.5f, 2.0f);
			y += dy;
		}
		switch (mChrDrawMode) {
			case FULL:
				str = "Full Lighting";
				break;
			case RAW:
				str = "Raw Lighting (Diff + Refl)";
				break;
			case RAW_DIFF:
				str = "Raw Diffuse";
				break;
			case RAW_REFL:
				str = "Raw Reflection";
				break;
			case TEX_DIFF:
				str = "Diffuse";
				break;
			case TEX_REFL:
				str = "Reflection";
				break;
		}
		str = String.format(Locale.US, "ambL: %.2f, litL: %.2f", mPanoAmbientLum, mPanoLightLum);
		mDbgText.print(str, x + fontScl, y + fontScl, 0.0f, 0.0f, 0.0f, 2.0f);
		mDbgText.print(str, x, y, 0.5f, 0.75f, 0.99f, 2.0f);

		y += dy;
		str = String.format(Locale.US, "amb: %.2f, %.2f, %.2f", mPanoAmbient.r(), mPanoAmbient.g(), mPanoAmbient.b());
		mDbgText.print(str, x + fontScl, y + fontScl, 0.0f, 0.0f, 0.0f, 2.0f);
		mDbgText.print(str, x, y, mPanoAmbient);

		y += dy;
		str = String.format(Locale.US, "lit: %.2f, %.2f, %.2f", mPanoLightClr.r(), mPanoLightClr.g(), mPanoLightClr.b());
		mDbgText.print(str, x + fontScl, y + fontScl, 0.0f, 0.0f, 0.0f, 2.0f);
		mDbgText.print(str, x, y, mPanoLightClr);

		y += dy;
		str = String.format(Locale.US, "exp: %s", mUseExposure ? "on" : "off");
		mDbgText.print(str, x + fontScl, y + fontScl, 0.0f, 0.0f, 0.0f, 2.0f);
		mDbgText.print(str, x, y, 0.55f, 0.75f, 0.6f, 2.0f);
	}

	private void draw() {
		mDbgText.setScrSize(mWidth, mHeight);
		GLUtil.clearFrame(0.33f, 0.44f, 0.55f);
		CmdList cmd = mCmdList;
		cmd.reset();

		cmd.startPacket(0);
		mCam.putAllGP(cmd);
		if (mUseTonemap) {
			//cmd.putSETF("COLOR.linWhite", 0.95f, 0.95f, 0.95f);
			cmd.putSETF("COLOR.linWhite", mPanoAmbient.ch);
			cmd.putSETF("COLOR.linGain", 1.0f + mPanoAmbient.r()*0.1f, 1.0f + mPanoAmbient.g()*0.1f, 1.0f + mPanoAmbient.b()*0.1f);
			//cmd.putSETF("COLOR.linBias", -0.01f, -0.01f, -0.01f);
			cmd.putSETF("COLOR.linBias", -mPanoAmbient.r()*0.01f, -mPanoAmbient.g()*0.01f, -mPanoAmbient.b()*0.01f);
		}
		if (mUseExposure) {
			if (mAutoExposure) {
				float expScale = Calc.fit(Calc.clamp(mPanoAmbientLum, 0.5f, 2.0f), 0.5f, 2.0f, 2.0f, 0.5f);
				//cmd.putSETF("COLOR.exposure", mPanoRawAmbient.r()*expScale, mPanoRawAmbient.g()*expScale, mPanoRawAmbient.b()*expScale);

				float expo = mPanoAmbientLum * expScale;
				//expo = Calc.clamp(expo, 0.5f, 1.5f);
				//expo = Calc.fit(expo, 0.5f, 1.5f, 1.5f, 1.95f);
				cmd.putSETF("COLOR.exposure", expo, expo, expo);
			} else {
				cmd.putSETF("COLOR.exposure", 1.83343f, 2.0f, 1.474f);
				//cmd.putSETF("COLOR.exposure", 2.0f, 1.6055f, 1.474f);
				//cmd.putSETF("COLOR.exposure", 1.474f, 1.88603f, 2.0f);
			}
		}
		float gamma = 2.2f;
		float invGamma = Calc.rcp0(gamma);
		cmd.putSETF("COLOR.invGamma", invGamma, invGamma, invGamma);
		cmd.putSETF("ENVSH.diff", mPanoSHIrrad);
		cmd.putSETF("ENVSH.refl", mPanoSHRefl);
		if (mUseDistLight) {
			cmd.putSETF("LIGHT.dir", mPanoLightDir.el);
			if (mUseDistLightClr) {
				cmd.putSETF("LIGHT.distClr", mPanoLightClr.r(), mPanoLightClr.g(), mPanoLightClr.b());
			} else {
				cmd.putSETF("LIGHT.distClr", 1.0f, 1.0f, 1.0f);
			}
			boolean litIOR = true;
			if (litIOR) {
				float minIOR = 1.4f;
				float maxIOR = 1.8f;
				float[] r = new float[3];
				for (int i = 0; i < 3; ++i) {
					r[i] = Calc.fit(mPanoLightClr.ch[i], 0.0f, 1.0f, minIOR, maxIOR);
				}
				cmd.putSETF("MATERIAL.IOR", r);
			} else {
				final float defIOR = 1.8f;//1.4f;
				cmd.putSETF("MATERIAL.IOR", defIOR, defIOR, defIOR);
			}
			boolean litRough = true;
			if (litRough) {
				float[] r = new float[3];
				float roffs = mPanoAmbientLum > 0.5f ? 0.0f : 0.1f;
				for (int i = 0; i < 3; ++i) {
					r[i] = Calc.sqrtf(2.0f / (mPanoLightClr.ch[i]*4.0f + 2.0f)) + roffs;
					r[i] = Calc.clamp(r[i], 0.01f, 1.0f);
				}
				cmd.putSETF("MATERIAL.specRoughness", r);
			} else {
				final float defRough = 0.6f;
				cmd.putSETF("MATERIAL.specRoughness", defRough, defRough, defRough);
			}
		}
		cmd.endPacket();

		if (mUseChr) {
			putChr(cmd);
		} else {
			putObj(cmd);
		}
		putEnv(cmd);

		//StringBuilder cmdDump = new StringBuilder();
		//cmd.dump(cmdDump);

		cmd.exec();

		drawInfo();
	}

	@Override public void onSurfaceCreated(GL10 gl, EGLConfig cfg) {
		mCam = new Camera();
		mGlbTexLib = new TexLib();
		mGlbTexLib.init(new XTex[] {mPanoTex}, TexLib.Mode.HRGB);
		initGPU();
		initEnvMdl();
		initObjMdl();
		if (mUseChr) {
			initChrMdl();
		}
		mDbgText = TestUtil.makeDbgText(mCtx);
	}

	@Override public void onSurfaceChanged(GL10 gl, int w, int h) {
		mWidth = w;
		mHeight = h;
		glViewport(0, 0, w, h);
	}

	@Override public void onDrawFrame(GL10 gl) {
		mFPSInfo.update();
		long t0 = System.nanoTime();
		ctrl();
		draw();
		double dt = (double)(System.nanoTime() - t0) / 1.0e9f;
		final double mt = 1.0f / 33.0f;
		if (dt < mt) {
			long st = (long)((mt - dt) * 1.0e9f);
			mSleepNanos = st;
			if (mUseChr) {
				try {
					TimeUnit.NANOSECONDS.sleep(st);
				} catch (InterruptedException err) {
					mSleepNanos = 0;
				}
			}
		}
		++mFrameCount;
	}

}
