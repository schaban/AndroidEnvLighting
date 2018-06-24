// GLSL ES codegen
// Author: Sergey Chaban <sergey.chaban@gmail.com>

package gley;

public class PixConfig extends ShaderConfig {

	public enum BaseMapSource {
		WHITE,
		TEX
	}

	public enum SpecMapSource {
		WHITE,
		BASE,
		BASE_AVG,
		BASE_LUM,
		BASE_LUMA,
		BASE_INV_AVG,
		BASE_INV_LUM,
		BASE_INV_LUMA,
		BASE_TEX_A,
		TEX
	}

	public enum AmbientMode {
		NONE,
		CONST,
		HEMI
	}

	public enum EnvDiffMode {
		NONE,
		PANO,
		SH3,
		SH3ADJ,
		SH3XLU
	}

	public enum EnvReflMode {
		NONE,
		SH3
	}

	public enum TangentMode {
		AUTO,
		GEOM
	}

	public enum BumpMode {
		NONE,
		HMAP,
		NMAP
	}

	public enum VtxClrMode {
		NONE,
		BASE,
		DIFF,
		MASK
	}

	public enum LightMode {
		NONE,
		DIFF,
		SPEC,
		COMBO
	}

	public enum ReflMode {
		NONE,
		PANO,
		CUBE
	}

	public enum DiffFunc {
		LAMBERT,
		OREN_NAYAR
	}

	public enum SpecFunc {
		BLINN,
		PHONG,
		GGX
	}

	public enum FogMode {
		NONE,
		LINEAR,
		CURVE
	}

	public enum ColorCorrectFunc {
		TONEMAP,
		EXPOSURE,
		GAMMA,
		GAMMA2,
		LEVELS
	}

	public boolean mHighPrecision = true;
	public TangentMode mTngMode = TangentMode.GEOM;
	public boolean mCalcBitangent = false;
	public BaseMapSource mSrcBase = BaseMapSource.WHITE;
	public SpecMapSource mSrcSpec = SpecMapSource.BASE_AVG;
	public BumpMode mBumpMode = BumpMode.NONE;
	public VtxClrMode mVtxClrMode = VtxClrMode.BASE;
	public boolean mUseVtxClrParams = false;
	public boolean mVtxClrClampNeg = false;
	public AmbientMode mAmbMode = AmbientMode.HEMI;
	public boolean mAmbScaling = false;
	public EnvDiffMode mEnvDiffMode = EnvDiffMode.NONE;
	public EnvReflMode mEnvReflMode = EnvReflMode.NONE;
	public boolean mEnvDiffScaling = false;
	public boolean mEnvReflScaling = false;
	public ReflMode mReflMode = ReflMode.NONE;
	public LightMode mDistLightMode = LightMode.COMBO;
	public LightMode mOmniLightMode = LightMode.COMBO;
	public DiffFunc mDiffFunc = DiffFunc.LAMBERT;
	public SpecFunc mSpecFunc = SpecFunc.BLINN;
	public boolean mUseReflViewFresnel = true;
	public boolean mUseSpecViewFresnel = true;
	public boolean mUseSpecFresnel = true;
	public boolean mDiffFadeDown = false;
	public boolean mSpecFadeDown = false;
	public ColorCorrectFunc[] mCCFuncs = new ColorCorrectFunc[] {
			ColorCorrectFunc.TONEMAP,
			ColorCorrectFunc.GAMMA,
			ColorCorrectFunc.LEVELS
	};
	public FogMode mFogMode = FogMode.NONE;

	public boolean needNormal() {
		return hasDynLighting() || hasEnvLighting() || hasReflection() || hasBump() || mAmbMode == AmbientMode.HEMI;
	}

	public boolean isAutoTangent() {
		return mTngMode == TangentMode.AUTO;
	}

	public boolean needUV() {
		return mSrcBase == BaseMapSource.TEX
		    || mSrcSpec == SpecMapSource.TEX
		    || mBumpMode != BumpMode.NONE;
	}

	public boolean needTextures() {
		return needUV() || hasReflection() || mEnvDiffMode == EnvDiffMode.PANO;
	}

	public boolean needTangent() {
		return mBumpMode != BumpMode.NONE;
	}

	public boolean needBaseTex() {
		return mSrcBase == BaseMapSource.TEX;
	}

	public boolean needSpecTex() {
		return mSrcSpec == SpecMapSource.TEX;
	}

	public boolean needBumpTex() {
		return mBumpMode != BumpMode.NONE;
	}

	public boolean hasBump() {
		return mBumpMode != BumpMode.NONE;
	}

	public boolean hasReflection() {
		return mReflMode != ReflMode.NONE;
	}

	public boolean needViewFresnel() {
		return (hasReflection() || hasSpecLighting() || mEnvReflMode != EnvReflMode.NONE)
		    && (mUseReflViewFresnel == true || mUseSpecViewFresnel == true);
	}

	public boolean needAmbientColor() {
		return mAmbMode != AmbientMode.NONE &&
		       (mAmbScaling || mAmbMode == AmbientMode.CONST);
	}

	public boolean hasEnvLighting() {
		return mEnvDiffMode != EnvDiffMode.NONE || mEnvReflMode != EnvReflMode.NONE;
	}

	public boolean isDistLightEnabled() {
		return mDistLightMode != LightMode.NONE;
	}

	public boolean isOmniLightEnabled() {
		return mOmniLightMode != LightMode.NONE;
	}

	public boolean hasDynLighting() {
		return isDistLightEnabled() || isOmniLightEnabled();
	}

	public boolean hasDistDiff() {
		return mDistLightMode == LightMode.DIFF || mDistLightMode == LightMode.COMBO;
	}

	public boolean hasOmniDiff() {
		return mOmniLightMode == LightMode.DIFF || mOmniLightMode == LightMode.COMBO;
	}

	public boolean hasDiffLighting() {
		return hasDistDiff() || hasOmniDiff();
	}

	public boolean hasDistSpec() {
		return mDistLightMode == LightMode.SPEC || mDistLightMode == LightMode.COMBO;
	}

	public boolean hasOmniSpec() {
		return mOmniLightMode == LightMode.SPEC || mOmniLightMode == LightMode.COMBO;
	}

	public boolean hasSpecLighting() {
		return hasDistSpec() || hasOmniSpec();
	}

	public boolean hasLightBalance() {
		return mDistLightMode == LightMode.COMBO || mOmniLightMode == LightMode.COMBO;
	}

	public boolean hasFog() {
		return mFogMode != FogMode.NONE;
	}

	public boolean hasCC() {
		return mCCFuncs != null && mCCFuncs.length > 0;
	}

	public boolean hasColorCorrection() {
		return hasCC();
	}

	public boolean hasTonemapCC() {
		if (hasCC()) {
			for (ColorCorrectFunc fn : mCCFuncs) {
				if (fn == ColorCorrectFunc.TONEMAP) {
					return true;
				}
			}
		}
		return false;
	}

	public boolean hasExposureCC() {
		if (hasCC()) {
			for (ColorCorrectFunc fn : mCCFuncs) {
				if (fn == ColorCorrectFunc.EXPOSURE) {
					return true;
				}
			}
		}
		return false;
	}

	public boolean hasVariableGammaCC() {
		if (hasCC()) {
			for (ColorCorrectFunc fn : mCCFuncs) {
				if (fn == ColorCorrectFunc.GAMMA) {
					return true;
				}
			}
		}
		return false;
	}

	public boolean hasGammaCC() {
		if (hasCC()) {
			for (ColorCorrectFunc fn : mCCFuncs) {
				if (fn == ColorCorrectFunc.GAMMA || fn == ColorCorrectFunc.GAMMA2) {
					return true;
				}
			}
		}
		return false;
	}

	public boolean hasLevelsCC() {
		if (hasCC()) {
			for (ColorCorrectFunc fn : mCCFuncs) {
				if (fn == ColorCorrectFunc.LEVELS) {
					return true;
				}
			}
		}
		return false;
	}

	public void setCCTonemapGamma() {
		mCCFuncs = new ColorCorrectFunc[] {
				ColorCorrectFunc.TONEMAP,
				ColorCorrectFunc.GAMMA
		};
	}

	public void setCCTonemapGammaLevels() {
		mCCFuncs = new ColorCorrectFunc[] {
				ColorCorrectFunc.TONEMAP,
				ColorCorrectFunc.GAMMA,
				ColorCorrectFunc.LEVELS
		};
	}

	public void setCCGamma() {
		mCCFuncs = new ColorCorrectFunc[] { ColorCorrectFunc.GAMMA };
	}

	public void setCCGamma2() {
		mCCFuncs = new ColorCorrectFunc[] { ColorCorrectFunc.GAMMA2 };
	}

	public void disableCC() {
		mCCFuncs = null;
	}

}
