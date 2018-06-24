// GLSL ES codegen
// Author: Sergey Chaban <sergey.chaban@gmail.com>

package gley;

import java.util.Locale;

public abstract class ShaderConfig {

	protected static void emitTagOrdinal(StringBuilder sb, String name, int val) {
		if (val > 0xFF) {
			sb.append(String.format(Locale.US, "%s%X", name, val));
		} else {
			sb.append(String.format(Locale.US, "%s%02X", name, val));
		}
	}

	protected static void emitTagBool(StringBuilder sb, String name, boolean val) {
		sb.append(name);
		sb.append(val ? "+" : "-");
	}

	//public abstract String getTag();

}
