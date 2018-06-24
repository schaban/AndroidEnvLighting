// GLSL ES codegen
// Author: Sergey Chaban <sergey.chaban@gmail.com>

package gley;

import gley.VtxConfig.*;

public class VtxShader {

	protected GParam mGP;
	protected int[] mUsage;

	public VtxShader(GParam gp) {
		mGP = gp;
		mUsage = mGP.allocUsageTbl();
	}

	public int[] getUsage() {
		return mUsage;
	}

	public boolean ckGPID(int gpid) {
		return mGP.ckGPID(gpid);
	}

	protected void genWorldXformVec(StringBuilder sb, int gpidWM, String srcName) {
		sb.append("vec3(dot(");
		sb.append(srcName);
		sb.append(", ");
		mGP.emitName(sb, gpidWM);
		sb.append("[0].xyz), dot(");
		sb.append(srcName);
		sb.append(", ");
		mGP.emitName(sb, gpidWM);
		sb.append("[1].xyz), dot(");
		sb.append(srcName);
		sb.append(", ");
		mGP.emitName(sb, gpidWM);
		sb.append("[2].xyz))");
	}

	protected void genWorldXformPnt(StringBuilder sb, int gpidWM, String srcName) {
		sb.append("vec3(dot(");
		sb.append(srcName);
		sb.append(", ");
		mGP.emitName(sb, gpidWM);
		sb.append("[0]), dot(");
		sb.append(srcName);
		sb.append(", ");
		mGP.emitName(sb, gpidWM);
		sb.append("[1]), dot(");
		sb.append(srcName);
		sb.append(", ");
		mGP.emitName(sb, gpidWM);
		sb.append("[2]))");
	}

	protected void genWorldXformVec(StringBuilder sb, String wmName, String srcName) {
		sb.append("vec3(dot(");
		sb.append(srcName);
		sb.append(", ");
		sb.append(wmName);
		sb.append("[0].xyz), dot(");
		sb.append(srcName);
		sb.append(", ");
		sb.append(wmName);
		sb.append("[1].xyz), dot(");
		sb.append(srcName);
		sb.append(", ");
		sb.append(wmName);
		sb.append("[2].xyz))");
	}

	protected void genWorldXformPnt(StringBuilder sb, String wmName, String srcName) {
		sb.append("vec3(dot(");
		sb.append(srcName);
		sb.append(", ");
		sb.append(wmName);
		sb.append("[0]), dot(");
		sb.append(srcName);
		sb.append(", ");
		sb.append(wmName);
		sb.append("[1]), dot(");
		sb.append(srcName);
		sb.append(", ");
		sb.append(wmName);
		sb.append("[2]))");
	}

	protected void genSkinMtxOp(StringBuilder sb, int gpidSM, String op) {
		for (int i = 0; i < 3; ++i) {
			sb.append("\twmtx[");
			sb.append(i);
			sb.append("] ");
			sb.append(op);
			sb.append(" ");
			mGP.emitName(sb, gpidSM);
			sb.append("[jidx + ");
			sb.append(i);
			sb.append("] * jwgt;\n");
		}
	}

	protected final static String s_octaFunc = "vec3 octa(vec2 oct) {\n" +
			"\tvec2 xy = oct;\n" +
			"\tvec2 a = abs(xy);\n" +
			"\tfloat z = 1.0 - a.x - a.y;\n" +
			"\tvec2 s = vec2(xy.x < 0.0 ? -1.0 : 1.0, xy.y < 0.0 ? -1.0 : 1.0);\n" +
			"\txy = mix(xy, (vec2(1.0) - a.yx) * s, vec2(z < 0.0));\n" +
			"\tvec3 n = vec3(xy, z);\n" +
			"\treturn normalize(n);\n" +
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

	public void gen(StringBuilder sb, VtxConfig cfg, StringBuilder err) {
		mGP.clearUsageTbl(mUsage);

		int gpidWM = -1;
		int gpidSM = -1;
		if (cfg.mWorldMode != WorldMode.STATIC) {
			if (cfg.mWorldMode == WorldMode.SOLID) {
				gpidWM = mGP.getGPID("WORLD.mtx");
				if (!ckGPID(gpidWM)) {
					return;
				}
				mGP.usageSt(mUsage, gpidWM);
			} else {
				gpidSM = mGP.getGPID("SKIN.mtx");
				if (!ckGPID(gpidSM)) {
					return;
				}
				mGP.usageSt(mUsage, gpidSM);
			}
		}

		int gpidVP = mGP.getGPID("CAMERA.viewProj");
		if (!ckGPID(gpidVP)) {
			return;
		}
		mGP.usageSt(mUsage, gpidVP);

		int gpidTexShift = -1;
		boolean texShiftFlg = cfg.mTexMode != TexMode.NONE && cfg.mUseTexShift;
		if (texShiftFlg) {
			gpidTexShift = getGPID("VERTEX.texShift");
		}

		int gpidAttrScl = -1;
		if (cfg.mClrScl || cfg.mTexScl) {
			gpidAttrScl = getGPID("VERTEX.attrScl");
		}

		int gpidPosBase = -1;
		int gpidPosScl = -1;
		if (cfg.mQuantPos) {
			gpidPosBase = getGPID("VERTEX.posBase");
			gpidPosScl = getGPID("VERTEX.posScl");
		}

		mGP.emitVersion(sb);
		sb.append("precision highp float;\n");
		sb.append("\n");

		// vtx ->
		mGP.emitAttrDecl(sb, "vec3 vtxPos");
		if (cfg.mNrmMode != NrmMode.NONE) {
			mGP.emitAttrDecl(sb, "vec", true);
			switch (cfg.mNrmMode) {
				case VEC:
					sb.append(3);
					break;
				case OCT:
					sb.append(2);
					break;
			}
			sb.append(" vtxNrm;\n");
			if (cfg.mUseTangent) {
				mGP.emitAttrDecl(sb, "vec", true);
				switch (cfg.mNrmMode) {
					case VEC:
						sb.append(3);
						break;
					case OCT:
						sb.append(2);
						break;
				}
				sb.append(" vtxTng;\n");
			}
		}
		if (cfg.hasVtxColor()) {
			mGP.emitAttrDecl(sb, "vec", true);
			switch (cfg.mClrMode) {
				case RGB:
					sb.append(3);
					break;
				case RGBA:
					sb.append(4);
					break;
			}
			sb.append(" vtxClr;\n");
		}
		if (cfg.mTexMode != TexMode.NONE) {
			mGP.emitAttrDecl(sb, "vec", true);
			switch (cfg.mTexMode) {
				case TEX:
					sb.append(2);
					break;
				case TEX2:
					sb.append(4);
					break;
			}
			sb.append(" vtxTex;\n");
		}
		boolean fullWgt = cfg.mFullWgt;
		if (cfg.hasSkinning()) {
			switch (cfg.mWorldMode) {
				case SKIN1:
					mGP.emitAttrDecl(sb, "float vtxJnt");
					break;
				case SKIN2:
					mGP.emitAttrDecl(sb, "vec2 vtxJnt");
					if (fullWgt) {
						mGP.emitAttrDecl(sb, "vec2 vtxWgt");
					} else {
						mGP.emitAttrDecl(sb, "float vtxWgt");
					}
					break;
				case SKIN4:
					mGP.emitAttrDecl(sb, "vec4 vtxJnt");
					if (fullWgt) {
						mGP.emitAttrDecl(sb, "vec4 vtxWgt");
					} else {
						mGP.emitAttrDecl(sb, "vec3 vtxWgt");
					}
					break;
				case SKIN8:
					mGP.emitAttrDecl(sb, "vec4 vtxJnt");
					mGP.emitAttrDecl(sb, "vec4 vtxJnt2");
					mGP.emitAttrDecl(sb, "vec4 vtxWgt;");
					if (fullWgt) {
						mGP.emitAttrDecl(sb, "vec4 vtxWgt2");
					} else {
						mGP.emitAttrDecl(sb, "vec3 vtxWgt2");
					}
					break;
			}
		}
		sb.append("\n");

		// gp
		mGP.emitDecl(sb, mUsage, err);
		sb.append("\n");

		// -> pix
		mGP.emitOutDecl(sb,"vec3 pixWPos");
		if (cfg.mNrmMode != NrmMode.NONE) {
			mGP.emitOutDecl(sb, "vec3 pixWNrm");
			if (cfg.mUseTangent) {
				mGP.emitOutDecl(sb, "vec3 pixWTng");
				if (cfg.mWriteBitangent) {
					mGP.emitOutDecl(sb, "vec3 pixWBtg");
				}
			}
		}
		if (cfg.mClrMode != ClrMode.NONE) {
			mGP.emitOutDecl(sb, "vec4 pixRGBA");
		}
		if (cfg.mTexMode != TexMode.NONE) {
			mGP.emitOutDecl(sb, "vec4 pixTex");
		}
		sb.append("\n");

		if (cfg.mNrmMode == NrmMode.OCT) {
			sb.append(s_octaFunc);
			sb.append("\n");
		}

		sb.append("void main() {\n");
		if (cfg.mWorldMode != WorldMode.STATIC) {
			sb.append("\tvec4 wmtx[3];\n");
			if (cfg.mWorldMode == WorldMode.SOLID) {
				for (int i = 0; i < 3; ++i) {
					sb.append("\twmtx[");
					sb.append(i);
					sb.append("] = ");
					mGP.emitName(sb, gpidWM);
					sb.append("[");
					sb.append(i);
					sb.append("];\n");
				}
			} else {
				boolean premultIdx = cfg.mPremultIdx;
				sb.append("\tint jidx;\n");
				switch (cfg.mWorldMode) {
					case SKIN1:
						if (premultIdx) {
							sb.append("\tjidx = int(vtxJnt);\n");
						} else {
							sb.append("\tjidx = int(vtxJnt) * 3;\n");
						}
						for (int i = 0; i < 3; ++i) {
							sb.append("\twmtx[");
							sb.append(i);
							sb.append("] = ");
							mGP.emitName(sb, gpidSM);
							sb.append("[jidx + ");
							sb.append(i);
							sb.append("];\n");
						}
						break;
					case SKIN2:
					case SKIN4:
					case SKIN8:
						sb.append("\tfloat jwgt;\n");
						if (premultIdx) {
							sb.append("\tjidx = int(vtxJnt.x);\n");
						} else {
							sb.append("\tjidx = int(vtxJnt.x) * 3;\n");
						}
						if (cfg.mWorldMode == WorldMode.SKIN2 && !fullWgt) {
							sb.append("\tjwgt = vtxWgt;\n");
						} else {
							sb.append("\tjwgt = vtxWgt.x;\n");
						}
						genSkinMtxOp(sb, gpidSM, "=");
						if (premultIdx) {
							sb.append("\tjidx = int(vtxJnt.y);\n");
						} else {
							sb.append("\tjidx = int(vtxJnt.y) * 3;\n");
						}
						if (cfg.mWorldMode == WorldMode.SKIN2 && !fullWgt) {
							sb.append("\tjwgt = 1.0 - vtxWgt;\n");
						} else {
							sb.append("\tjwgt = vtxWgt.y;\n");
						}
						genSkinMtxOp(sb, gpidSM, "+=");
						if (cfg.mWorldMode != WorldMode.SKIN2) {
							if (premultIdx) {
								sb.append("\tjidx = int(vtxJnt.z);\n");
							} else {
								sb.append("\tjidx = int(vtxJnt.z) * 3;\n");
							}
							sb.append("\tjwgt = vtxWgt.z;\n");
							genSkinMtxOp(sb, gpidSM, "+=");
							if (premultIdx) {
								sb.append("\tjidx = int(vtxJnt.w);\n");
							} else {
								sb.append("\tjidx = int(vtxJnt.w) * 3;\n");
							}
							if (cfg.mWorldMode == WorldMode.SKIN4 && !fullWgt) {
								sb.append("\tjwgt = 1.0 - (vtxWgt.x + vtxWgt.y + vtxWgt.z);\n");
							} else {
								sb.append("\tjwgt = vtxWgt.w;\n");
							}
							genSkinMtxOp(sb, gpidSM, "+=");
							if (cfg.mWorldMode == WorldMode.SKIN8) {
								if (premultIdx) {
									sb.append("\tjidx = int(vtxJnt2.x);\n");
								} else {
									sb.append("\tjidx = int(vtxJnt2.x) * 3;\n");
								}
								sb.append("\tjwgt = vtxWgt2.x;\n");
								genSkinMtxOp(sb, gpidSM, "+=");
								if (premultIdx) {
									sb.append("\tjidx = int(vtxJnt2.y);\n");
								} else {
									sb.append("\tjidx = int(vtxJnt2.y) * 3;\n");
								}
								sb.append("\tjwgt = vtxWgt2.y;\n");
								genSkinMtxOp(sb, gpidSM, "+=");
								if (premultIdx) {
									sb.append("\tjidx = int(vtxJnt2.z);\n");
								} else {
									sb.append("\tjidx = int(vtxJnt2.z) * 3;\n");
								}
								sb.append("\tjwgt = vtxWgt2.z;\n");
								genSkinMtxOp(sb, gpidSM, "+=");
								if (premultIdx) {
									sb.append("\tjidx = int(vtxJnt2.w);\n");
								} else {
									sb.append("\tjidx = int(vtxJnt2.w) * 3;\n");
								}
								if (!fullWgt) {
									sb.append("\tjwgt = 1.0 - (vtxWgt.x + vtxWgt.y + vtxWgt.z + vtxWgt.w + vtxWgt2.x + vtxWgt2.y + vtxWgt2.z);\n");
								} else {
									sb.append("\tjwgt = vtxWgt2.w;\n");
								}
								genSkinMtxOp(sb, gpidSM, "+=");
							}
						}
						break;
				}
			}
		}
		if (cfg.mQuantPos) {
			sb.append("\tvec4 vpos = vec4(");
			if (ckGPID(gpidPosBase)) {
				emitName(sb, gpidPosBase);
			} else {
				sb.append("vec3(0.0)");
			}
			sb.append(" + vtxPos*");
			if (ckGPID(gpidPosScl)) {
				emitName(sb, gpidPosScl);
			} else {
				sb.append("vec3(1.0)");
			}
			sb.append(", 1.0);\n");
		} else {
			sb.append("\tvec4 vpos = vec4(vtxPos, 1.0);\n");
		}
		if (cfg.mNrmMode != NrmMode.NONE) {
			sb.append("\tvec3 vnrm = ");
			switch (cfg.mNrmMode) {
				case VEC:
					sb.append("vtxNrm");
					break;
				case OCT:
					sb.append("octa(vtxNrm)");
					break;
			}
			sb.append(";\n");
			if (cfg.mUseTangent) {
				sb.append("\tvec3 vtng = ");
				switch (cfg.mNrmMode) {
					case VEC:
						sb.append("vtxTng");
						break;
					case OCT:
						sb.append("octa(vtxTng)");
						break;
				}
				sb.append(";\n");
			}
		}

		if (cfg.mClrMode != ClrMode.NONE) {
			boolean clrSclFlg = true;
			sb.append("\tvec4 vclr = ");
			switch (cfg.mClrMode) {
				case WHITE:
					sb.append("vec4(1.0)");
					clrSclFlg = false;
					break;
				case RGB:
					sb.append("vec4(vtxClr");
					if (clrSclFlg && cfg.mClrScl && ckGPID(gpidAttrScl)) {
						sb.append(" * ");
						mGP.emitName(sb, gpidAttrScl);
						sb.append(".x");
					}
					sb.append(", 1.0)");
					break;
				case RGBA:
					sb.append("vtxClr");
					if (clrSclFlg && cfg.mClrScl && ckGPID(gpidAttrScl)) {
						sb.append(" * ");
						mGP.emitName(sb, gpidAttrScl);
						sb.append(".x");
					}
					break;
			}
			sb.append(";\n");
		}

		if (cfg.mTexMode != TexMode.NONE) {
			sb.append("\tvec4 vtex = ");
			switch (cfg.mTexMode) {
				case TEX:
					sb.append("vec4(vtxTex, vtxTex)");
					break;
				case TEX2:
					sb.append("vtxTex");
					break;
			}
			if (cfg.mTexScl && ckGPID(gpidAttrScl)) {
				sb.append(" * ");
				mGP.emitName(sb, gpidAttrScl);
				sb.append(".y");
			}
			sb.append(";\n");
		}

		boolean nrmFlg = cfg.mNormalize;

		if (cfg.mWorldMode == WorldMode.STATIC) {
			sb.append("\tvec3 wpos = vpos.xyz;\n");
			if (cfg.mNrmMode != NrmMode.NONE) {
				sb.append("\tvec3 wnrm = vnrm;\n");
				if (cfg.mUseTangent) {
					sb.append("\tvec3 wtng = vtng;\n");
				}
			}
		} else {
			sb.append("\tvec3 wpos = ");
			genWorldXformPnt(sb, "wmtx", "vpos");
			sb.append(";\n");
			if (cfg.mNrmMode != NrmMode.NONE) {
				sb.append("\tvec3 wnrm = ");
				if (nrmFlg) sb.append("normalize(");
				genWorldXformVec(sb, "wmtx", "vnrm");
				if (nrmFlg) sb.append(")");
				sb.append(";\n");
				if (cfg.mUseTangent) {
					sb.append("\tvec3 wtng = ");
					if (nrmFlg) sb.append("normalize(");
					genWorldXformVec(sb, "wmtx", "vtng");
					if (nrmFlg) sb.append(")");
					sb.append(";\n");
				}
			}
		}
		if (cfg.mNrmMode != NrmMode.NONE) {
			if (cfg.mUseTangent && cfg.mWriteBitangent) {
				sb.append("\tvec3 wbtg = ");
				if (nrmFlg) sb.append("normalize(");
				sb.append("cross(wtng, wnrm)");
				if (nrmFlg) sb.append(")");
				sb.append(";\n");
			}
		}
		sb.append("\tvec4 cpos = vec4(wpos, 1.0) * ");
		mGP.emitName(sb, gpidVP);
		sb.append(";\n");
		sb.append("\tpixWPos = wpos;\n");
		if (cfg.mNrmMode != NrmMode.NONE) {
			sb.append("\tpixWNrm = wnrm;\n");
			if (cfg.mUseTangent) {
				sb.append("\tpixWTng = wtng;\n");
				if (cfg.mWriteBitangent) {
					sb.append("\tpixWBtg = wbtg;\n");
				}
			}
		}
		if (cfg.mClrMode != ClrMode.NONE) {
			sb.append("\tpixRGBA = vclr;\n");
		}
		if (cfg.mTexMode != TexMode.NONE) {
			if (texShiftFlg) {
				sb.append("\tpixTex = vtex + ");
				mGP.emitName(sb, gpidTexShift);
				sb.append(";\n");
			} else {
				sb.append("\tpixTex = vtex;\n");
			}
		}
		sb.append("\tgl_Position = cpos;\n");

		sb.append("}\n");
	}

}
