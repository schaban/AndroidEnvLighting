// Author: Sergey Chaban <sergey.chaban@gmail.com>

package xdata;

public class Color {
	public final float[] ch;

	public Color() {
		ch = new float[4];
	}

	public Color(float... data) {
		this();
		set(data);
	}

	public Color(Color c) {
		this(c.ch);
	}

	public float r() { return ch[0]; }
	public float g() { return ch[1]; }
	public float b() { return ch[2]; }
	public float a() { return ch[3]; }
	public void r(float val) { ch[0] = val; }
	public void g(float val) { ch[1] = val; }
	public void b(float val) { ch[2] = val; }
	public void a(float val) { ch[3] = val; }

	public void read(Binary bin, int org) {
		bin.getFloats(ch, org);
	}

	public void set(float... data) {
		int n = Calc.vcpy(0.0f, 0, ch, 0, data);
		if (n < 4) a(1.0f);
	}

	public void fill(float val) {
		Calc.vfill(ch, val);
	}

	public void fillRGB(float val) {
		r(val);
		g(val);
		b(val);
	}

	public float luma() {
		return Calc.luma(r(), g(), b());
	}

	public float luminance() {
		return Calc.luminance(r(), g(), b());
	}

	public float average() {
		return (r() + g() + b()) / 3.0f;
	}

	public float max() {
		return Math.max(r(), Math.max(g(), b()));
	}

	public void scl(float s) {
		Calc.vscl(ch, s);
	}

	public void sclRGB(float s) {
		Calc.vscl(ch, s, 3);
	}

}
