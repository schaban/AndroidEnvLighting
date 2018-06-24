// GLSL ES codegen
// Author: Sergey Chaban <sergey.chaban@gmail.com>

package gley;

import gley.PixConfig.*;
import xdata.SH;

import java.util.Locale;

public class PixShader {

	protected GParam mGP;
	protected int[] mUsage;

	public PixShader(GParam gp) {
		mGP = gp;
		mUsage = mGP.allocUsageTbl();
	}

	public int[] getUsage() {
		return mUsage;
	}

	protected final static String s_tngFunc = "void calcTng(vec3 pos, vec3 nrm, vec2 uv, out vec3 tng, out vec3 btg) {\n" +
			"\tvec3 npos = normalize(pos);\n" +
			"\tvec3 s = dFdx(npos);\n" +
			"\tvec3 t = dFdy(npos);\n" +
			"\tvec3 v1 = cross(t, nrm);\n" +
			"\tvec3 v2 = cross(nrm, s);\n" +
			"\tvec2 uvdx = dFdx(uv);\n" +
			"\tvec2 uvdy = dFdy(uv);\n" +
			"\ttng = v1*uvdx.x + v2*uvdy.x;\n" +
			"\tbtg = v1*uvdx.y + v2*uvdy.y;\n" +
			"\tfloat tscl = 1.0 / sqrt(max(dot(tng, tng), dot(btg, btg)));\n" +
			"\ttng *= tscl;\n" +
			"\tbtg *= tscl;\n" +
			"}\n";

	protected final static String s_panoFunc = "vec2 panoUV(vec3 dir) {\n" +
			"\tvec2 uv = vec2(0.0);\n" +
			"\tfloat lxz = sqrt(dir.x*dir.x + dir.z*dir.z);\n" +
			"\tif (lxz > 1.0e-5) uv.x = -dir.x / lxz;\n" +
			"\tuv.y = dir.y;\n" +
			"\tuv = clamp(uv, -1.0, 1.0);\n" +
			"\tuv = acos(uv) / 3.141592653;\n" +
			"\tuv.x *= 0.5;\n" +
			"\tif (dir.z >= 0.0) uv.x = 1.0 - uv.x;\n" +
			"\treturn uv;\n" +
			"}\n";

	protected final static String s_diffLambert = "float ldiff(vec3 N, vec3 L) {\n" +
			"\treturn max(0.0, min(dot(N, L), 1.0));\n" +
			"}\n";

	protected final static String s_diffOrenNayar = "vec3 ldiff(vec3 N, vec3 L, vec3 V, vec3 rough) {\n" +
			"\tfloat NL = dot(N, L);\n" +
			"\tfloat NV = dot(N, V);\n" +
			"\tvec3 s = rough;\n" +
			"\tvec3 ss = s*s;\n" +
			"\tvec3 a = vec3(1.0) - 0.5*(ss/(ss+vec3(0.57)));\n" +
			"\tvec3 b = 0.45*(ss / (ss + vec3(0.09)));\n" +
			"\tvec3 al = normalize(L - NL*N);\n" +
			"\tvec3 av = normalize(V - NV*N);\n" +
			"\tfloat c = max(0.0, dot(al, av));\n" +
			"\tfloat ci = clamp(NL, -1.0, 1.0);\n" +
			"\tfloat cr = clamp(NV, -1.0, 1.0);\n" +
			"\tfloat ti = acos(ci);\n" +
			"\tfloat tr = acos(cr);\n" +
			"\tfloat aa = max(ti, tr);\n" +
			"\tfloat bb = min(ti, tr);\n" +
			"\tfloat d = sin(aa)*tan(bb);\n" +
			"\treturn max(0.0, min(NL, 1.0)) * (a + b*c*d);\n" +
			"}\n";

	protected final static String s_specFresnelSchlick = "vec3 specFresnel(float tcos, vec3 ior) {\n" +
			"\tvec3 r = (ior - vec3(1.0)) / (ior + vec3(1.0));\n" +
			"\tvec3 f0 = r*r;\n" +
			"\treturn f0 + (vec3(1.0) - f0) * pow(1.0 - tcos, 5.0);\n" +
			"}\n";

	protected final static String s_ggx = "vec3 ggx(vec3 srr, float nl, float nh, float nv) {\n" +
			"\tvec3 srr2 = srr*srr;\n" +
			"\tvec3 dv = nh*nh*(srr2 - vec3(1.0)) + vec3(1.0);\n" +
			"\tvec3 dstr = srr2 / (dv*dv*3.141592653);\n" +
			"\tvec3 hsrr = srr / 2.0;\n" +
			"\tvec3 tnl = nl*(vec3(1.0) - hsrr) + hsrr;\n" +
			"\tvec3 tnv = nv*(vec3(1.0) - hsrr) + hsrr;\n" +
			"\treturn (nl*dstr) / (tnl*tnv);\n" +
			"}\n";

	protected final static String s_ease = "float ease(vec2 cp, float t) {\n" +
			"\tfloat tt = t*t;\n" +
			"\tfloat ttt = tt*t;\n" +
			"\tfloat b1 = dot(vec3(ttt, tt, t), vec3(3.0, -6.0, 3.0));\n" +
			"\tfloat b2 = dot(vec2(ttt, tt), vec2(-3.0, 3.0));\n" +
			"\treturn dot(vec3(cp, 1.0), vec3(b1, b2, ttt));\n" +
			"}\n";

	protected int getGPID(String name) {
		int gpid = mGP.getGPID(name);
		if (mGP.ckGPID(gpid)) {
			mGP.usageSt(mUsage, gpid);
		}
		return gpid;
	}

	protected void emitName(StringBuilder sb, int gpid) {
		mGP.emitName(sb, gpid);
	}

	public boolean ckGPID(int gpid) {
		return mGP.ckGPID(gpid);
	}

	protected void emitBlinn(StringBuilder sb, String nh) {
		sb.append("pow(vec3(");
		sb.append(nh);
		sb.append("), max(vec3(0.001), vec3(2.0)/(srr*srr) - vec3(2.0)))");
	}

	protected void emitPhong(StringBuilder sb, String vr) {
		sb.append("pow(vec3(");
		sb.append(vr);
		sb.append("), max(vec3(0.001), vec3(2.0)/srr - vec3(2.0))) / vec3(2.0)");
	}

	private static void emitf(StringBuilder sb, float val) {
		sb.append(String.format(Locale.US, "%.16f", val));
	}

	private static float[] s_constsSH3 = null;

	private static /*synchronized*/ float[] getSH3Consts() {
		if (s_constsSH3 == null) {
			s_constsSH3 = SH.allocConsts(3);
			SH.calcConsts(3, s_constsSH3, 0);
		}
		return s_constsSH3;
	}

	public void gen(StringBuilder sb, PixConfig cfg, StringBuilder err) {
		mGP.clearUsageTbl(mUsage);
		int gpidBaseColor = -1;
		int gpidSpecColor = -1;
		int gpidCamViewPos = -1;
		int gpidAmbColor = -1;
		int gpidHemiSky = -1;
		int gpidHemiGnd = -1;
		int gpidHemiUp = -1;
		int gpidVClrGain = -1;
		int gpidVClrBias = -1;
		int gpidBumpFactor = -1;
		int gpidFlipTng = -1;
		int gpidReflClr = -1;
		int gpidReflLvl = -1;
		int gpidLightDir = -1;
		int gpidLightPos = -1;
		int gpidDistClr = -1;
		int gpidOmniClr = -1;
		int gpidOmniAttn = -1;
		int gpidLightClrFactors = -1;
		int gpidDiffRoughness = -1;
		int gpidSpecRoughness = -1;
		int gpidDiffSHAdj = -1;
		int gpidIOR = -1;
		int gpidViewFr = -1;
		int gpidFogColor = -1;
		int gpidFogParams = -1;
		int gpidCCLinWhite = -1;
		int gpidCCLinGain = -1;
		int gpidCCLinBias = -1;
		int gpidCCExposure = -1;
		int gpidCCInvGamma = -1;
		int gpidCCInBlack = -1;
		int gpidCCRangeRatio = -1;
		int gpidCCOutBlack = -1;
		int gpidEnvSHDiff = -1;
		int gpidEnvSHDiffClr = -1;
		int gpidEnvSHRefl = -1;
		int gpidEnvSHReflClr = -1;

		int shOrd = 0;
		float[] shConsts = null;

		gpidBaseColor = getGPID("MATERIAL.baseColor");
		gpidSpecColor = getGPID("MATERIAL.specColor");
		gpidCamViewPos = getGPID("CAMERA.viewPos");
		if (cfg.needAmbientColor()) {
			gpidAmbColor = getGPID("AMBIENT.color");
		}
		if (cfg.mAmbMode == AmbientMode.HEMI) {
			gpidHemiSky = getGPID("AMBIENT.hemiSky");
			gpidHemiGnd = getGPID("AMBIENT.hemiGround");
			gpidHemiUp = getGPID("AMBIENT.hemiUp");
		}
		if (cfg.mEnvDiffMode != EnvDiffMode.NONE) {
			if (cfg.mEnvDiffMode == EnvDiffMode.SH3
			    || cfg.mEnvDiffMode == EnvDiffMode.SH3ADJ
			    || cfg.mEnvDiffMode == EnvDiffMode.SH3XLU
			) {
				gpidEnvSHDiff = getGPID("ENVSH.diff");
				shOrd = 3;
				shConsts = getSH3Consts();
			}
			if (cfg.mEnvDiffMode == EnvDiffMode.SH3ADJ) {
				gpidDiffSHAdj = getGPID("MATERIAL.diffSHAdj");
			}
			if (cfg.mEnvDiffScaling) {
				gpidEnvSHDiffClr = getGPID("ENVSH.diffClr");
			}
		}
		if (cfg.mEnvReflMode != EnvReflMode.NONE) {
			if (cfg.mEnvReflMode == EnvReflMode.SH3) {
				gpidEnvSHRefl = getGPID("ENVSH.refl");
				shOrd = 3;
				shConsts = getSH3Consts();
			}
			if (cfg.mEnvReflScaling) {
				gpidEnvSHReflClr = getGPID("ENVSH.reflClr");
			}
		}
		if (cfg.mVtxClrMode != VtxClrMode.NONE) {
			if (cfg.mUseVtxClrParams) {
				gpidVClrGain = getGPID("MATERIAL.vclrGain");
				gpidVClrBias = getGPID("MATERIAL.vclrBias");
			}
		}
		if (cfg.mBumpMode == BumpMode.NMAP) {
			gpidBumpFactor = getGPID("MATERIAL.bumpFactor");
			gpidFlipTng = getGPID("MATERIAL.flipTngFrame");
		}
		if (cfg.hasReflection()) {
			gpidReflClr = getGPID("MATERIAL.reflColor");
			gpidReflLvl = getGPID("MATERIAL.reflLvl");
		}
		if (cfg.needViewFresnel()) {
			gpidViewFr = getGPID("MATERIAL.viewFresnel");
		}
		if (cfg.hasDynLighting()) {
			if (cfg.isDistLightEnabled()) {
				gpidLightDir = getGPID("LIGHT.dir");
				gpidDistClr = getGPID("LIGHT.distClr");
			}
			if (cfg.isOmniLightEnabled()) {
				gpidLightPos = getGPID("LIGHT.pos");
				gpidOmniClr = getGPID("LIGHT.omniClr");
				gpidOmniAttn = getGPID("LIGHT.omniAttn");
			}
			if (cfg.hasLightBalance()) {
				gpidLightClrFactors = getGPID("LIGHT.clrFactors");
			}
			if (cfg.mDiffFunc == DiffFunc.OREN_NAYAR) {
				gpidDiffRoughness = getGPID("MATERIAL.diffRoughness");
			}
			gpidSpecRoughness = getGPID("MATERIAL.specRoughness");
			if (cfg.mUseSpecFresnel) {
				gpidIOR = getGPID("MATERIAL.IOR");
			}
		}
		if (cfg.hasFog()) {
			gpidFogColor = getGPID("FOG.color");
			gpidFogParams = getGPID("FOG.params");
		}
		if (cfg.hasCC()) {
			if (cfg.hasTonemapCC()) {
				gpidCCLinWhite = getGPID("COLOR.linWhite");
				gpidCCLinGain = getGPID("COLOR.linGain");
				gpidCCLinBias = getGPID("COLOR.linBias");
			}

			if (cfg.hasExposureCC()) {
				gpidCCExposure = getGPID("COLOR.exposure");
			}

			if (cfg.hasVariableGammaCC()) {
				gpidCCInvGamma = getGPID("COLOR.invGamma");
			}

			if (cfg.hasLevelsCC()) {
				gpidCCInBlack = getGPID("COLOR.inBlack");
				gpidCCRangeRatio = getGPID("COLOR.rangeRatio");
				gpidCCOutBlack = getGPID("COLOR.outBlack");
			}
		}

		mGP.emitVersion(sb);
		if ((cfg.needTangent() && cfg.isAutoTangent()) || (cfg.mBumpMode == BumpMode.HMAP)) {
			sb.append("#extension GL_OES_standard_derivatives : enable\n");
		}

		if (cfg.mHighPrecision) {
			sb.append("precision highp float;\n");
		} else {
			sb.append("precision mediump float;\n");
		}
		sb.append("\n");

		mGP.emitInDecl(sb, "vec3 pixWPos");
		if (cfg.needNormal()) {
			mGP.emitInDecl(sb, "vec3 pixWNrm");
			if (cfg.needTangent() && !cfg.isAutoTangent()) {
				mGP.emitInDecl(sb, "vec3 pixWTng");
				if (!cfg.mCalcBitangent) {
					mGP.emitInDecl(sb, "vec3 pixWBtg");
				}
			}
		}
		if (cfg.needUV()) {
			mGP.emitInDecl(sb, "vec4 pixTex");
		}
		if (cfg.mVtxClrMode != VtxClrMode.NONE) {
			mGP.emitInDecl(sb, "vec4 pixRGBA");
		}
		sb.append("\n");

		if (cfg.needTextures()) {
			if (cfg.needBaseTex()) {
				sb.append("uniform sampler2D smpBase;\n");
			}
			if (cfg.needSpecTex()) {
				sb.append("uniform sampler2D smpSpec;\n");
			}
			if (cfg.needBumpTex()) {
				sb.append("uniform sampler2D smpBump;\n");
			}
			if (cfg.hasReflection() || cfg.mEnvDiffMode == EnvDiffMode.PANO) {
				if (cfg.mReflMode == ReflMode.PANO || cfg.mEnvDiffMode == EnvDiffMode.PANO) {
					sb.append("uniform sampler2D smpPano;\n");
				} else if (cfg.mReflMode == ReflMode.CUBE) {
					sb.append("uniform samplerCube smpCube;\n");
				}
			}
			sb.append("\n");
		}

		// gp
		mGP.emitDecl(sb, mUsage, err);
		sb.append("\n");

		if (mGP.mTgtVersion >= 300) {
			sb.append("out vec4 outColor;\n\n");
		}

		if (cfg.needTangent() && cfg.isAutoTangent()) {
			sb.append(s_tngFunc);
			sb.append("\n");
		}

		if (cfg.mReflMode == ReflMode.PANO || cfg.mEnvDiffMode == EnvDiffMode.PANO) {
			sb.append(s_panoFunc);
			sb.append("\n");
		}

		if (cfg.hasDynLighting()) {
			if (cfg.mDiffFunc == DiffFunc.OREN_NAYAR) {
				sb.append(s_diffOrenNayar);
			} else {
				sb.append(s_diffLambert);
			}
			sb.append("\n");

			if (cfg.mSpecFunc == SpecFunc.GGX) {
				sb.append(s_ggx);
				sb.append("\n");
			}
		}

		if (cfg.hasSpecLighting() && cfg.mUseSpecFresnel) {
			sb.append(s_specFresnelSchlick);
			sb.append("\n");
		}

		if (cfg.mFogMode == FogMode.CURVE) {
			sb.append(s_ease);
			sb.append("\n");
		}

		if (ckGPID(gpidViewFr)) {
			sb.append("float viewFresnel(float tcos, float gain, float bias) {\n" +
					"\tfloat t = max(1.0 - tcos, 1.0e-9);\n" +
					"\tfloat s = pow(t, 5.0)*gain + bias;\n" +
					"\ts = max(0.0, min(s, 1.0));\n" +
					"\treturn 1.0 - s;\n" +
					"}\n\n");
		}

		if (shOrd > 0) {
			SHFunc.gen(sb, shOrd, shConsts, 0);
			sb.append("\n");
		}

		sb.append("void main() {\n");
		sb.append("\tvec3 wpos = pixWPos;\n");
		if (cfg.needNormal()) {
			sb.append("\tvec3 wnrm = normalize(pixWNrm);\n");
			sb.append("\tvec3 gnrm = wnrm;\n");
		}
		if (cfg.needUV()) {
			sb.append("\tvec2 uvPrimary = pixTex.xy;\n");
			sb.append("\tvec2 uvSecondary = pixTex.zw;\n");
			sb.append("\tvec2 uvBase = uvPrimary;\n");
			sb.append("\tvec2 uvSpec = uvPrimary;\n");
			sb.append("\tvec2 uvBump = uvPrimary;\n");
			sb.append("\tvec2 uvLMap = uvSecondary;\n");
		}
		if (cfg.needNormal() && cfg.needTangent()) {
			if (cfg.isAutoTangent()) {
				sb.append("\tvec3 wtng;\n");
				sb.append("\tvec3 wbtg;\n");
				sb.append("\tcalcTng(wpos, wnrm, uvBump, wtng, wbtg);\n");
			} else {
				sb.append("\tvec3 wtng = normalize(pixWTng);\n");
				if (cfg.mCalcBitangent) {
					sb.append("\tvec3 wbtg = normalize(cross(wtng, wnrm));\n");
				} else {
					sb.append("\tvec3 wbtg = normalize(pixWBtg);\n");
				}
			}
		}
		if (cfg.mVtxClrMode != VtxClrMode.NONE) {
			sb.append("\tvec4 vrgba = pixRGBA.rgba;\n");
			if (ckGPID(gpidVClrGain)) {
				sb.append("\tvrgba *= ");
				emitName(sb, gpidVClrGain);
				sb.append(";\n");
			}
			if (ckGPID(gpidVClrBias)) {
				sb.append("\tvrgba += ");
				emitName(sb, gpidVClrBias);
				sb.append(";\n");
			}
			if (cfg.mVtxClrClampNeg) {
				sb.append("\tvrgba = max(vrgba, vec4(0.0));\n");
			}
			sb.append("\tvec3 vclr = vrgba.rgb;\n");
			sb.append("\tfloat alpha = vrgba.a;\n");
		} else {
			sb.append("\tfloat alpha = 1.0;\n");
		}

		if (cfg.hasBump()) {
			sb.append("\tvec4 bumpTex = ");
			if (mGP.mTgtVersion >= 300) {
				sb.append("texture");
			} else {
				sb.append("texture2D");
			}
			sb.append("(smpBump, uvBump);\n");
			if (cfg.mBumpMode == BumpMode.NMAP) {
				sb.append("\tvec2 nxy = (bumpTex.xy - 0.5) * 2.0;\n");
				if (ckGPID(gpidBumpFactor)) {
					sb.append("\tnxy *= ");
					emitName(sb, gpidBumpFactor);
					sb.append(";\n");
				}
				sb.append("\tvec2 sqxy = nxy * nxy;\n");
				sb.append("\tfloat nz = sqrt(max(0.0, 1.0 - sqxy.x - sqxy.y));\n");
				if (ckGPID(gpidFlipTng)) {
					sb.append("\tfloat uflip = ");
					emitName(sb, gpidFlipTng);
					sb.append(".x;\n");
					sb.append("\tfloat vflip = ");
					emitName(sb, gpidFlipTng);
					sb.append(".y;\n");
					sb.append("\tvec3 nnrm = normalize(nxy.x*wtng*uflip + nxy.y*wbtg*vflip + nz*wnrm);\n");
				} else {
					sb.append("\tvec3 nnrm = normalize(nxy.x*wtng + nxy.y*wbtg + nz*wnrm);\n");
				}
			} else if (cfg.mBumpMode == BumpMode.HMAP) {
				sb.append("\tvec2 uvdx = dFdx(uvBump);\n");
				sb.append("\tvec2 uvdy = dFdy(uvBump);\n");
				float bscl = 4.0f;
				sb.append("\tfloat xbump = ");
				if (mGP.mTgtVersion >= 300) {
					sb.append("texture");
				} else {
					sb.append("texture2D");
				}
				sb.append("(smpBump, uvBump + uvdx).x;\n");
				sb.append("\txbump -= bumpTex.x;\n");
				sb.append("\txbump *= ");
				emitf(sb, bscl);
				sb.append(";\n");
				sb.append("\tfloat ybump = ");
				if (mGP.mTgtVersion >= 300) {
					sb.append("texture");
				} else {
					sb.append("texture2D");
				}
				sb.append("(smpBump, uvBump + uvdy).x;\n");
				sb.append("\tybump -= bumpTex.x;\n");
				sb.append("\tybump *= ");
				emitf(sb, bscl);
				sb.append(";\n");
				sb.append("\tvec3 nnrm = normalize(xbump*wtng + ybump*wbtg + wnrm);\n");
			}
			sb.append("\twnrm = nnrm;\n");
		}

		sb.append("\tvec3 viewPos = ");
		if (ckGPID(gpidCamViewPos)) {
			emitName(sb, gpidCamViewPos);
		} else {
			sb.append("vec3(0.0)");
		}
		sb.append(";\n");
		sb.append("\tvec3 viewDir = normalize(wpos - viewPos);\n");
		if (cfg.hasReflection() || cfg.mEnvReflMode != EnvReflMode.NONE) {
			sb.append("\tvec3 viewRefl = reflect(viewDir, wnrm);\n");
		}

		sb.append("\tvec4 baseTex = ");
		if (cfg.mSrcBase == BaseMapSource.WHITE) {
			sb.append("vec4(1.0)");
		} else {
			if (mGP.mTgtVersion >= 300) {
				sb.append("texture");
			} else {
				sb.append("texture2D");
			}
			sb.append("(smpBase, uvBase)");
		}
		sb.append(";\n");
		if (cfg.mVtxClrMode == VtxClrMode.BASE) {
			sb.append("\tbaseTex.rgb *= vclr;\n");
		}

		sb.append("\tvec4 specTex = ");
		switch (cfg.mSrcSpec) {
			case WHITE:
				sb.append("vec4(1.0)");
				break;
			case BASE:
				sb.append("baseTex");
				break;
			case BASE_AVG:
				sb.append("vec4((baseTex.r + baseTex.g + baseTex.b)/3.0)");
				break;
			case BASE_INV_AVG:
				sb.append("vec4(1.0 - (baseTex.r + baseTex.g + baseTex.b)/3.0)");
				break;
			case BASE_LUM:
				sb.append("vec4(dot(baseTex.rgb, vec3(0.212671, 0.71516, 0.072169)))");
				break;
			case BASE_LUMA:
				sb.append("vec4(dot(baseTex.rgb, vec3(0.299, 0.587, 0.114)))");
				break;
			case BASE_INV_LUM:
				sb.append("vec4(1.0 - dot(baseTex.rgb, vec3(0.212671, 0.71516, 0.072169)))");
				break;
			case BASE_INV_LUMA:
				sb.append("vec4(1.0 - dot(baseTex.rgb, vec3(0.299, 0.587, 0.114)))");
				break;
			case BASE_TEX_A:
				sb.append("vec4(baseTex.a)");
				break;
			case TEX:
				if (mGP.mTgtVersion >= 300) {
					sb.append("texture");
				} else {
					sb.append("texture2D");
				}
				sb.append("(smpSpec, uvSpec)");
				break;
		}
		sb.append(";\n");
		if (cfg.mVtxClrMode == VtxClrMode.MASK) {
			sb.append("\tspecTex.rgb *= vclr;\n");
		}
		if (ckGPID(gpidSpecColor)) {
			sb.append("\tspecTex.rgb *= ");
			emitName(sb, gpidSpecColor);
			sb.append(";\n");
		}

		if (ckGPID(gpidBaseColor)) {
			sb.append("\tbaseTex.rgb *= ");
			emitName(sb, gpidBaseColor);
			sb.append(";\n");
		}
		if (cfg.mSrcBase != BaseMapSource.WHITE && cfg.mSrcSpec != SpecMapSource.BASE_TEX_A) {
			sb.append("\talpha *= baseTex.a;\n");
		}

		boolean hemiCmp = false;

		sb.append("\tvec3 diff = ");
		switch (cfg.mAmbMode) {
			case CONST:
				if (ckGPID(gpidAmbColor)) {
					emitName(sb, gpidAmbColor);
				} else {
					sb.append("vec3(1.0)");
				}
				break;
			case HEMI:
				if (mGP.ckGPIDs(gpidHemiSky, gpidHemiGnd, gpidHemiUp)) {
					if (hemiCmp) {
						sb.append("(");
						emitName(sb, gpidHemiSky);
						sb.append(" == ");
						emitName(sb, gpidHemiGnd);
						sb.append(" ? ");
						emitName(sb, gpidHemiSky);
						sb.append(" : ");
					}
					sb.append("mix(");
					emitName(sb, gpidHemiGnd);
					sb.append(", ");
					emitName(sb, gpidHemiSky);
					sb.append(", (dot(wnrm, ");
					emitName(sb, gpidHemiUp);
					sb.append(") + 1.0) * 0.5)");
					if (hemiCmp) {
						sb.append(")");
					}
				} else if (mGP.ckGPIDs(gpidHemiSky, gpidHemiGnd) && !ckGPID(gpidHemiUp)) {
					if (hemiCmp) {
						sb.append("(");
						emitName(sb, gpidHemiSky);
						sb.append(" == ");
						emitName(sb, gpidHemiGnd);
						sb.append(" ? ");
						emitName(sb, gpidHemiSky);
						sb.append(" : ");
					}
					sb.append("mix(");
					emitName(sb, gpidHemiGnd);
					sb.append(", ");
					emitName(sb, gpidHemiSky);
					sb.append(", (wnrm.y + 1.0) * 0.5)");
					if (hemiCmp) {
						sb.append(")");
					}
				} else {
					if (ckGPID(gpidHemiSky)) {
						emitName(sb, gpidHemiSky);
					} else if (ckGPID(gpidHemiGnd)) {
						emitName(sb, gpidHemiGnd);
					} else {
						sb.append("vec3(1.0)");
					}
				}
				if (ckGPID(gpidAmbColor)) {
					sb.append(" * ");
					emitName(sb, gpidAmbColor);
				}
				break;
			default:
				sb.append("vec3(0.0)");
				break;
		}
		sb.append(";\n");
		if (cfg.mVtxClrMode == VtxClrMode.DIFF) {
			sb.append("\tdiff += vclr;\n");
		}

		switch (cfg.mEnvDiffMode) {
			case PANO:
				sb.append("\tvec2 envDiffUV = panoUV(wnrm);\n");
				sb.append("\tvec4 envDiffTex = texture2D(smpPano, envDiffUV);\n");
				sb.append("\tvec3 envDiff = envDiffTex.rgb / envDiffTex.a;\n");
				if (ckGPID(gpidEnvSHDiffClr)) {
					sb.append("\tenvDiff *= ");
					emitName(sb, gpidEnvSHDiffClr);
					sb.append(";\n");
				}
				sb.append("\tdiff += envDiff;\n");
				break;
			case SH3:
			case SH3XLU:
				if (ckGPID(gpidEnvSHDiff)) {
					sb.append("\tfloat diffSH[9];\n");
					sb.append("\tsh3(diffSH, wnrm);\n");
					for (int i = 0; i < 3*3; ++i) {
						if (i == 0) {
							sb.append("\tvec3 envSHDiff = ");
						} else {
							sb.append("\tenvSHDiff += ");
						}
						emitName(sb, gpidEnvSHDiff);
						sb.append("[");
						sb.append(i);
						sb.append("]*diffSH[");
						sb.append(i);
						sb.append("];\n");
					}
					if (ckGPID(gpidEnvSHDiffClr)) {
						sb.append("\tenvSHDiff *= ");
						emitName(sb, gpidEnvSHDiffClr);
						sb.append(";\n");
					}
					sb.append("\tdiff += envSHDiff;\n");

					if (cfg.mEnvDiffMode == EnvDiffMode.SH3XLU) {
						sb.append("\tsh3(diffSH, -wnrm);\n");
						for (int i = 0; i < 3*3; ++i) {
							if (i == 0) {
								sb.append("\tvec3 envSHXlu = ");
							} else {
								sb.append("\tenvSHXlu += ");
							}
							emitName(sb, gpidEnvSHDiff);
							sb.append("[");
							sb.append(i);
							sb.append("]*diffSH[");
							sb.append(i);
							sb.append("];\n");
						}
						sb.append("\tdiff += envSHXlu * baseTex.r * 0.1;\n"); // FIXME
					}
				}
				break;
			case SH3ADJ:
				if (ckGPID(gpidEnvSHDiff) && ckGPID(gpidDiffSHAdj)) {
					sb.append("\tfloat diffSH[9];\n");
					sb.append("\tsh3(diffSH, wnrm);\n");
					sb.append("\tvec3 diffSHAdjRGB = ");
					emitName(sb, gpidDiffSHAdj);
					sb.append(";\n");
					sb.append("\tfloat adjNE = dot(wnrm, -viewDir);\n");
					sb.append("\tvec3 fSHAdj = vec3(1.0) - vec3(1.0)/(vec3(2.0) + diffSHAdjRGB*0.65);\n");
					sb.append("\tvec3 gSHAdj = vec3(1.0)/(vec3(2.22222) + diffSHAdjRGB*0.1);\n");
					sb.append("\tvec3 adjDC = fSHAdj + gSHAdj*0.5*(1.0 + 2.0*acos(adjNE)/3.14159265 - adjNE);\n");
					sb.append("\tvec3 adjLin = fSHAdj + (fSHAdj - vec3(1.0)) * (1.0 - adjNE);\n");
					sb.append("\tvec3 envSHDiff = ");
					emitName(sb, gpidEnvSHDiff);
					sb.append("[0]*diffSH[0]*adjDC;\n");
					for (int i = 1; i < 4; ++i) {
						sb.append("\tenvSHDiff += ");
						emitName(sb, gpidEnvSHDiff);
						sb.append("[");
						sb.append(i);
						sb.append("]*diffSH[");
						sb.append(i);
						sb.append("]*adjLin;\n");
					}
					for (int i = 4; i < 3*3; ++i) {
						sb.append("\tenvSHDiff += ");
						emitName(sb, gpidEnvSHDiff);
						sb.append("[");
						sb.append(i);
						sb.append("]*diffSH[");
						sb.append(i);
						sb.append("];\n");
					}
					if (ckGPID(gpidEnvSHDiffClr)) {
						sb.append("\tenvSHDiff *= ");
						emitName(sb, gpidEnvSHDiffClr);
						sb.append(";\n");
					}
					sb.append("\tdiff += envSHDiff;\n");
				}
				break;
		}

		sb.append("\tvec3 spec = vec3(0.0);\n");
		sb.append("\tvec3 refl = vec3(0.0);\n");
		if (ckGPID(gpidViewFr)) {
			sb.append("\tfloat tcosView = clamp(dot(viewDir, wnrm), -1.0, 1.0);\n");
		}

		if (cfg.hasReflection()) {
			sb.append("\tvec3 reflDir = viewRefl;\n");
			sb.append("\tvec3 reflClr = ");
			if (ckGPID(gpidReflClr)) {
				emitName(sb, gpidReflClr);
			} else {
				sb.append("vec3(1.0)");
			}
			sb.append(";\n");
			if (mGP.mTgtVersion >= 300) {
				sb.append("\tfloat reflLvl = ");
				if (ckGPID(gpidReflLvl)) {
					sb.append("max(0.0, ");
					emitName(sb, gpidReflLvl);
					sb.append(")");
				} else {
					sb.append("0.0");
				}
				sb.append(";\n");
			}
			if (cfg.mReflMode == ReflMode.PANO) {
				sb.append("\tvec2 reflUV = panoUV(reflDir);\n");
				if (mGP.mTgtVersion >= 300) {
					sb.append("\tvec4 reflTex = textureLod(smpPano, reflUV, reflLvl);\n");
				} else {
					sb.append("\tvec4 reflTex = texture2D(smpPano, reflUV);\n");
				}
			} else {
				sb.append("\tvec4 reflTex = textureCube(smpCube, reflDir);\n");
			}
			sb.append("\trefl += (reflTex.rgb / reflTex.a) * reflClr;\n");
		}

		switch (cfg.mEnvReflMode) {
			case SH3:
				if (ckGPID(gpidEnvSHRefl)) {
					sb.append("\tfloat reflSH[9];\n");
					sb.append("\tsh3(reflSH, viewRefl);\n");
					for (int i = 0; i < 3*3; ++i) {
						if (i == 0) {
							sb.append("\tvec3 envSHRefl = ");
						} else {
							sb.append("\tenvSHRefl += ");
						}
						emitName(sb, gpidEnvSHRefl);
						sb.append("[");
						sb.append(i);
						sb.append("]*reflSH[");
						sb.append(i);
						sb.append("];\n");
					}
					if (ckGPID(gpidEnvSHReflClr)) {
						sb.append("\tenvSHRefl *= ");
						emitName(sb, gpidEnvSHReflClr);
						sb.append(";\n");
					}
					sb.append("\tenvSHRefl = max(envSHRefl, vec3(0.0));\n");
					sb.append("\trefl += envSHRefl;\n");
				}
				break;
		}

		if (cfg.mUseReflViewFresnel && ckGPID(gpidViewFr)) {
			sb.append("\trefl *= viewFresnel(tcosView, ");
			emitName(sb, gpidViewFr);
			sb.append(".z, ");
			emitName(sb, gpidViewFr);
			sb.append(".w);\n");
		}

		if (cfg.hasDynLighting()) {
			if (cfg.hasOmniDiff() || cfg.hasOmniSpec()) {
				sb.append("\tvec3 omniL = normalize(wpos");
				if (ckGPID(gpidLightPos)) {
					sb.append(" - ");
					emitName(sb, gpidLightPos);
				}
				sb.append(");\n");
				if (ckGPID(gpidOmniAttn)) {
					sb.append("\tfloat omniAttn = length(wpos");
					if (ckGPID(gpidLightPos)) {
						sb.append(" - ");
						emitName(sb, gpidLightPos);
					}
					sb.append(");\n");
					sb.append("\tomniAttn = 1.0 - min(max((omniAttn - ");
					emitName(sb, gpidOmniAttn);
					sb.append(".x) * ");
					emitName(sb, gpidOmniAttn);
					sb.append(".y, 0.0), 1.0);\n");
					sb.append("\tomniAttn *= omniAttn;\n");
				} else {
					sb.append("\tfloat omniAttn = 1.0;\n");
				}
			}
			if (cfg.hasDistDiff() && ckGPID(gpidLightDir)) {
				sb.append("\tdiff += ldiff(wnrm, -");
				emitName(sb, gpidLightDir);
				switch (cfg.mDiffFunc) {
					case OREN_NAYAR:
						sb.append(", -viewDir, ");
						if (ckGPID(gpidDiffRoughness)) {
							emitName(sb, gpidDiffRoughness);
						} else {
							sb.append("vec3(0.7)");
						}
						break;
				}
				sb.append(")");
				if (ckGPID(gpidDistClr)) {
					sb.append(" * ");
					emitName(sb, gpidDistClr);
				}
				if (cfg.mDistLightMode == LightMode.COMBO) {
					if (ckGPID(gpidLightClrFactors)) {
						sb.append(" * ");
						emitName(sb, gpidLightClrFactors);
						sb.append(".x");
					}
				}
				sb.append(";\n");
			}
			if (cfg.hasOmniDiff()) {
				sb.append("\tdiff += ldiff(wnrm, -omniL");
				switch (cfg.mDiffFunc) {
					case OREN_NAYAR:
						sb.append(", -viewDir, ");
						if (ckGPID(gpidDiffRoughness)) {
							emitName(sb, gpidDiffRoughness);
						} else {
							sb.append("vec3(0.7)");
						}
						break;
				}
				sb.append(")");
				if (ckGPID(gpidOmniClr)) {
					sb.append(" * ");
					emitName(sb, gpidOmniClr);
				}
				if (cfg.mOmniLightMode == LightMode.COMBO) {
					if (ckGPID(gpidLightClrFactors)) {
						sb.append(" * ");
						emitName(sb, gpidLightClrFactors);
						sb.append(".z");
					}
				}
				sb.append(" * omniAttn;\n");
			}

			if (cfg.hasSpecLighting()) {
				sb.append("\tvec3 rspec = ");
				if (ckGPID(gpidSpecRoughness)) {
					emitName(sb, gpidSpecRoughness);
				} else {
					sb.append("vec3(0.7)");
				}
				sb.append(";\n");
				sb.append("\tvec3 srr = rspec * rspec;\n");
				if (cfg.mSpecFunc == SpecFunc.GGX) {
					sb.append("\tfloat specNV = max(0.0, dot(wnrm, -viewDir));\n");
				}
				if (cfg.hasDistSpec() && ckGPID(gpidLightDir)) {
					sb.append("\tvec3 distH = normalize(-");
					emitName(sb, gpidLightDir);
					sb.append(" + -viewDir);\n");
					switch (cfg.mSpecFunc) {
						case BLINN:
						case GGX:
							sb.append("\tfloat distNH = max(0.0, dot(wnrm, distH));\n");
							if (cfg.mSpecFunc == SpecFunc.GGX) {
								sb.append("\tfloat distNL = max(0.0, dot(wnrm, -");
								emitName(sb, gpidLightDir);
								sb.append("));\n");
							}
							break;
						case PHONG:
							sb.append("\tfloat distVR = max(0.0, dot(-viewDir, reflect(");
							emitName(sb, gpidLightDir);
							sb.append(", wnrm)));\n");
							break;
					}
					sb.append("\tspec += ");
					switch (cfg.mSpecFunc) {
						case BLINN:
							emitBlinn(sb, "distNH");
							break;
						case PHONG:
							emitPhong(sb, "distVR");
							break;
						case GGX:
							sb.append("ggx(srr, distNL, distNH, specNV)");
							break;
					}
					if (ckGPID(gpidDistClr)) {
						sb.append(" * ");
						emitName(sb, gpidDistClr);
					}
					if (cfg.mDistLightMode == LightMode.COMBO) {
						if (ckGPID(gpidLightClrFactors)) {
							sb.append(" * ");
							emitName(sb, gpidLightClrFactors);
							sb.append(".y");
						}
					}
					if (cfg.mUseSpecFresnel) {
						sb.append(" * specFresnel(max(0.0, dot(-");
						emitName(sb, gpidLightDir);
						sb.append(", distH)), ");
						if (ckGPID(gpidIOR)) {
							emitName(sb, gpidIOR);
						} else {
							sb.append("vec3(1.4)");
						}
						sb.append(")");
					}
					sb.append(";\n");
				}
				if (cfg.hasOmniSpec()) {
					sb.append("\tvec3 omniH = normalize(-omniL + -viewDir);\n");
					switch (cfg.mSpecFunc) {
						case BLINN:
						case GGX:
							sb.append("\tfloat omniNH = max(0.0, dot(wnrm, omniH));\n");
							if (cfg.mSpecFunc == SpecFunc.GGX) {
								sb.append("\tfloat omniNL = max(0.0, dot(wnrm, -omniL));\n");
							}
							break;
						case PHONG:
							sb.append("\tfloat omniVR = max(0.0, dot(-viewDir, reflect(omniL, wnrm)));\n");
							break;
					}
					sb.append("\tspec += ");
					switch (cfg.mSpecFunc) {
						case BLINN:
							emitBlinn(sb, "omniNH");
							break;
						case PHONG:
							emitPhong(sb, "omniVR");
							break;
						case GGX:
							sb.append("ggx(srr, omniNL, omniNH, specNV)");
							break;
					}
					if (ckGPID(gpidOmniClr)) {
						sb.append(" * ");
						emitName(sb, gpidOmniClr);
					}
					if (cfg.mOmniLightMode == LightMode.COMBO) {
						if (ckGPID(gpidLightClrFactors)) {
							sb.append(" * ");
							emitName(sb, gpidLightClrFactors);
							sb.append(".w");
						}
					}
					if (cfg.mUseSpecFresnel) {
						sb.append(" * specFresnel(max(0.0, dot(-omniL, omniH)), ");
						if (ckGPID(gpidIOR)) {
							emitName(sb, gpidIOR);
						} else {
							sb.append("vec3(1.4)");
						}
						sb.append(")");
					}
					sb.append(" * omniAttn;\n");
				}
			}
		}

		sb.append("\tdiff *= baseTex.rgb;\n");
		if (cfg.mDiffFadeDown) {
			sb.append("\tdiff *= 1.0 + min(wnrm.y, 0.0)*0.5;\n");
		}
		sb.append("\tspec += refl;\n");
		if (cfg.mUseSpecViewFresnel && ckGPID(gpidViewFr)) {
			sb.append("\tspec *= viewFresnel(tcosView, ");
			emitName(sb, gpidViewFr);
			sb.append(".x, ");
			emitName(sb, gpidViewFr);
			sb.append(".y);\n");
		}
		sb.append("\tspec *= specTex.rgb;\n");
		if (cfg.mSpecFadeDown) {
			sb.append("\tspec *= 1.0 + min(wnrm.y, 0.0);\n");
		}

		sb.append("\tvec4 tgtClr = vec4(diff + spec, alpha);\n");

		if (cfg.hasFog() && mGP.ckGPIDs(gpidFogColor, gpidFogParams)) {
			sb.append("\tfloat dfog = distance(wpos, viewPos);\n");
			sb.append("\tvec4 pfog = ");
			emitName(sb, gpidFogParams);
			sb.append(";\n");
			sb.append("\tvec4 cfog = ");
			emitName(sb, gpidFogColor);
			sb.append(";\n");
			sb.append("\tfloat tfog = max(0.0, min((dfog - pfog.x) * pfog.y, 1.0));\n");
			if (cfg.mFogMode == FogMode.CURVE) {
				sb.append("\ttfog = ease(pfog.zw, tfog);\n");
			}
			sb.append("\ttfog *= cfog.a;\n");
			sb.append("\ttgtClr.rgb = tgtClr.rgb*(1.0 - tfog) + cfog.rgb*tfog;\n");
		}

		if (cfg.hasCC()) {
			for (ColorCorrectFunc ccfn : cfg.mCCFuncs) {
				if (ccfn == ColorCorrectFunc.TONEMAP) {
					if (mGP.ckGPIDs(gpidCCLinWhite, gpidCCLinGain, gpidCCLinBias)) {
						sb.append("\ttgtClr.rgb = (tgtClr.rgb * (vec3(1.0) + tgtClr.rgb/");
						emitName(sb, gpidCCLinWhite);
						sb.append(")) / (vec3(1.0) + tgtClr.rgb);\n");
						sb.append("\ttgtClr.rgb *= ");
						emitName(sb, gpidCCLinGain);
						sb.append(";\n");
						sb.append("\ttgtClr.rgb += ");
						emitName(sb, gpidCCLinBias);
						sb.append(";\n");
					}
				} else if (ccfn == ColorCorrectFunc.EXPOSURE) {
					if (mGP.ckGPID(gpidCCExposure)) {
						sb.append("\ttgtClr.rgb = vec3(1.0) - exp(tgtClr.rgb * -");
						emitName(sb, gpidCCExposure);
						sb.append(");\n");
					}
				} else if (ccfn == ColorCorrectFunc.GAMMA || ccfn == ColorCorrectFunc.GAMMA2) {
					sb.append("\ttgtClr = max(tgtClr, vec4(0.0));\n");
					if (ccfn == ColorCorrectFunc.GAMMA) {
						if (ckGPID(gpidCCInvGamma)) {
							sb.append("\ttgtClr = pow(tgtClr, ");
							emitName(sb, gpidCCInvGamma);
							sb.append(");\n");
						} else {
							sb.append("\ttgtClr = pow(tgtClr, vec4(1.0/2.2));\n");
						}
					} else {
						sb.append("\ttgtClr = sqrt(tgtClr);\n");
					}
				} else if (ccfn == ColorCorrectFunc.LEVELS) {
					if (mGP.ckGPIDs(gpidCCInBlack, gpidCCRangeRatio, gpidCCOutBlack)) {
						sb.append("\ttgtClr.rgb = (tgtClr.rgb - ");
						emitName(sb, gpidCCInBlack);
						sb.append(") * ");
						emitName(sb, gpidCCRangeRatio);
						sb.append(" + ");
						emitName(sb, gpidCCOutBlack);
						sb.append(";\n");
					}
				}
			} // CC funcs
		} // CC
		if (mGP.mTgtVersion >= 300) {
			sb.append("\toutColor = tgtClr;\n");
		} else {
			sb.append("\tgl_FragColor = tgtClr;\n");
		}

		sb.append("}\n");
	}

}
