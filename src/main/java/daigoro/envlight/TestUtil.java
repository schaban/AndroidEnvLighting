package daigoro.envlight;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.FileOutputStream;
import java.io.FileNotFoundException;
import java.io.InputStreamReader;
import java.lang.reflect.*;
import java.nio.*;
import java.nio.channels.*;

import android.content.Context;
import android.content.res.Resources;

import flow.DbgText;
import flow.MotClip;
import xdata.*;

public final class TestUtil {

	public static Binary getBin(Resources rsrc, int rid) {
		InputStream is = null;
		Binary bin = null;
		try {
			is = rsrc.openRawResource(rid);
			bin = new Binary();
			bin.init(is);
			is.close();
		} catch (IOException ioe) {
			bin = null;
		} catch (Resources.NotFoundException err) {
			bin = null;
		}
		return bin;
	}

	public static XGeo loadGeo(Resources rsrc, int rid) {
		XGeo geo = null;
		Binary bin = TestUtil.getBin(rsrc, rid);
		if (bin != null) {
			if (bin.getKind().equals(XData.KIND_GEO)) {
				geo = new XGeo();
				geo.init(bin);
			}
			bin.reset();
		}
		return geo;
	}

	public static XGeo loadGeo(Context ctx, int rid) {
		return loadGeo(ctx.getResources(), rid);
	}

	public static XRig loadRig(Resources rsrc, int rid) {
		XRig rig = null;
		Binary bin = TestUtil.getBin(rsrc, rid);
		if (bin != null) {
			if (bin.getKind().equals(XData.KIND_RIG)) {
				rig = new XRig();
				rig.init(bin);
			}
			bin.reset();
		}
		return rig;
	}

	public static XRig loadRig(Context ctx, int rid) {
		return loadRig(ctx.getResources(), rid);
	}

	public static XTex loadTex(Resources rsrc, int rid) {
		XTex tex = null;
		Binary bin = TestUtil.getBin(rsrc, rid);
		if (bin != null) {
			if (bin.getKind().equals(XData.KIND_TEX)) {
				tex = new XTex();
				tex.init(bin);
			}
			bin.reset();
		}
		return tex;
	}

	public static XTex loadTex(Context ctx, int rid) {
		return loadTex(ctx.getResources(), rid);
	}

	public static MotClip loadMotClip(Resources rsrc, int rid) {
		MotClip clip = null;
		Binary bin = TestUtil.getBin(rsrc, rid);
		if (bin != null) {
			if (bin.getKind().equals(MotClip.SIG)) {
				clip = new MotClip();
				clip.init(bin);
			}
			bin.reset();
		}
		return clip;
	}

	public static MotClip loadMotClip(Context ctx, int rid) {
		return loadMotClip(ctx.getResources(), rid);
	}

	public static MotClip[] loadMotClips(Resources rsrc, int[] rids) {
		int n = rids.length;
		MotClip[] clips = new MotClip[n];
		for (int i = 0; i < n; ++i) {
			clips[i] = loadMotClip(rsrc, rids[i]);
		}
		return clips;
	}

	public static MotClip[] loadMotClips(Context ctx, int[] rids) {
		return loadMotClips(ctx.getResources(), rids);
	}

	public static void dumpGeo(XGeo geo, Context ctx, String fname) {
		String geoStr = geo.getHouGeoStr();
		try {
			FileOutputStream fos = ctx.openFileOutput(fname, Context.MODE_PRIVATE);
			fos.write(geoStr.getBytes());
			fos.close();
		} catch (FileNotFoundException fe) {
		} catch (IOException ioe) {
		}
	}

	public static String readText(Context ctx, int rid) {
		String text = null;
		try {
			InputStream is = ctx.getResources().openRawResource(rid);
			BufferedReader reader = new BufferedReader(new InputStreamReader(is));
			StringBuilder sb = new StringBuilder();
			String eol = "\n";
			while (true) {
				String ln = reader.readLine();
				if (ln == null) break;
				sb.append(ln);
				sb.append(eol);
			}
			text = sb.toString();
		} catch (IOException ioe ){
		} catch (Resources.NotFoundException nfe) {
		}
		return text;
	}

	public static ByteBuffer readBytes(Context ctx, int rid) {
		ByteBuffer b = null;
		try {
			InputStream is = ctx.getResources().openRawResource(rid);
			int size = is.available();
			byte[] tmp = new byte[size];
			is.read(tmp);
			b = ByteBuffer.allocateDirect(size);
			b.put(tmp);
			tmp = null;
			b.position(0);
		} catch (IOException ioe ){
		} catch (Resources.NotFoundException nfe) {
		}
		return b;
	}

	public static void writeBytes(Context ctx, ByteBuffer b, String fname) {
		try {
			FileOutputStream fos = ctx.openFileOutput(fname, Context.MODE_PRIVATE);
			b.position(0);
			fos.getChannel().write(b);
			b.position(0);
			fos.close();
		} catch (FileNotFoundException fe) {
		} catch (IOException ioe) {
		}
	}

	public static int getRsrcId(String name) {
		int id = 0;
		if (name != null) {
			try {
				Class cls = R.raw.class;
				Field fld = cls.getField(name);
				id = fld.getInt(cls);
			} catch (NoSuchFieldException e) {
			} catch (IllegalAccessException e) {
			}
		}
		return id;
	}

	public static int[] getTexLibIds(Resources rsrc, int rid) {
		int[] tids = null;
		Binary bin = TestUtil.getBin(rsrc, rid);
		if (bin != null) {
			if (bin.getKind().equals(XData.KIND_CAT)) {
				FileCat cat = new FileCat();
				cat.init(bin);
				int n = cat.getFilesNum();
				if (n > 0) {
					tids = new int[n];
					for (int i = 0; i < n; ++i) {
						String tname = cat.getShortName(i);
						tids[i] = getRsrcId(tname);
					}
				}
			}
			bin.reset();
		}
		return tids;
	}

	public static int[] getTexLibIds(Context ctx, int rid) {
		return getTexLibIds(ctx.getResources(), rid);
	}

	public static XTex[] loadTexAry(Resources rsrc, int catRID) {
		int[] texRIDs = getTexLibIds(rsrc, catRID);
		if (texRIDs == null) return null;
		int n = texRIDs.length;
		XTex[] texList = new XTex[n];
		for (int i = 0; i < n; ++i) {
			texList[i] = loadTex(rsrc, texRIDs[i]);
		}
		return texList;
	}

	public static XTex[] loadTexAry(Context ctx, int catRID) {
		return loadTexAry(ctx.getResources(), catRID);
	}

	public static DbgText makeDbgText(Context ctx) {
		DbgText t = null;
		XTex fontTex = TestUtil.loadTex(ctx, R.raw.def_font);
		if (fontTex != null) {
			t = new DbgText();
			t.init(fontTex);
		}
		return t;
	}

}
