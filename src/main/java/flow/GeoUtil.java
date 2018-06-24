// Author: Sergey Chaban <sergey.chaban@gmail.com>

package flow;

import java.util.*;

import xdata.*;
import geas.*;

public final class GeoUtil {

	public static int[] getBatTexMapFromGlbAttr(GeoData geo, XGeo srcGeo, TexLib texLib, String attrName) {
		XGeo.AttrInfo texmapAttr = srcGeo.findGlbAttr(attrName);
		String texMap = srcGeo.getAttrValS(texmapAttr, 0, 0);
		if (texMap == null || texMap.length() < 3) return null;
		String[] texAsgn = texMap.split(";");
		int batTexNum = texAsgn.length;
		int[] batTexMap = new int[batTexNum];
		Arrays.fill(batTexMap, -1);
		for (int i = 0; i < batTexNum; ++i) {
			String[] batTex = texAsgn[i].split("=");
			String batName = batTex[0];
			String texName = batTex[1];
			int batIdx = geo.findBatch(batName);
			if (batIdx >= 0) {
				int texIdx = texLib.findTex(texName);
				batTexMap[batIdx] = texIdx;
			}
		}
		return batTexMap;
	}

}
