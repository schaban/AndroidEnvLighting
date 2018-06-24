package daigoro.envlight;

import android.support.v7.app.AppCompatActivity;
import android.content.Context;
import android.os.Bundle;
import android.view.Display;
import android.view.MotionEvent;
import android.view.View;
import android.view.Menu;
import android.opengl.GLSurfaceView;
import android.opengl.GLSurfaceView.Renderer;
import android.view.WindowManager;

import xdata.*;
import gley.*;

public class MainActivity extends AppCompatActivity {

	private GLSurfaceView mGLView;
	private boolean mGLFlg = false;
	private Renderer mRdr;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		mGLView = new GLSurfaceView(this);
		mGLView.setEGLContextClientVersion(2);

		boolean reducedRes = !true;
		if (reducedRes) {
			WindowManager wm = (WindowManager) getSystemService(Context.WINDOW_SERVICE);
			Display disp = wm != null ? wm.getDefaultDisplay() : null;
			if (disp != null) {
				int dw = disp.getWidth();
				int dh = disp.getHeight();
				float div = 2.0f;
				int rhw = (int) ((float)dw / div);
				int rhh = (int) ((float)dh / div);
				mGLView.getHolder().setFixedSize(rhw, rhh);
			}
		}
		setContentView(mGLView);
		EnvLightingRdr rdr = new EnvLightingRdr(this);
		rdr.start();
		mGLView.setRenderer(rdr);
		mGLFlg = true;
	}

	@Override
	protected void onPause() {
		super.onPause();
		if (mGLFlg) {
			mGLView.onPause();
		}
	}

	@Override
	protected void onResume() {
		super.onResume();
		if (mGLFlg) {
			mGLView.onResume();
		}
	}

}
