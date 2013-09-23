package com.polyvi.xface.extension.zbar;

import net.sourceforge.zbar.Config;
import net.sourceforge.zbar.Image;
import net.sourceforge.zbar.ImageScanner;
import net.sourceforge.zbar.Symbol;
import net.sourceforge.zbar.SymbolSet;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.hardware.Camera;
import android.hardware.Camera.AutoFocusCallback;
import android.hardware.Camera.PreviewCallback;
import android.hardware.Camera.Size;
import android.os.Bundle;
import android.os.Handler;
import android.os.Vibrator;
import android.util.Log;
import android.view.View;
import android.widget.LinearLayout;

public class XCameraActivity extends Activity {
	private static final String CLASS_NAME = XCameraActivity.class.getSimpleName();
    private static final long VIBRATE_DURATION = 200L;

    private Camera mCamera;
    private XCameraPreview mPreview;
    private Handler mAutoFocusHandler;
    private ImageScanner mScanner;
    private boolean mPreviewing = true;

    static {
        System.loadLibrary("iconv");
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        try {
            LinearLayout layout = new LinearLayout(this);
            setContentView(layout);

            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

            mCamera = getCameraInstance();
            if(null == mCamera) {
                Intent intent = new Intent();
                setResult(RESULT_CANCELED, intent);
                finish();
                return;
            }
            mAutoFocusHandler = new Handler();
            /* Instance barcode scanner */
            mScanner = new ImageScanner();
            mScanner.setConfig(0, Config.X_DENSITY, 3);
            mScanner.setConfig(0, Config.Y_DENSITY, 3);
            mPreview = new XCameraPreview(this, mCamera, previewCb, autoFocusCB);
            layout.addView(mPreview);

            mCamera.setPreviewCallback(previewCb);
            mCamera.startPreview();
            mPreviewing = true;
            mCamera.autoFocus(autoFocusCB);
        } catch (Exception e) {
            Log.e(CLASS_NAME, "Error in onCreate:" + e.getMessage());
        }
    }

    public void onPause() {
        super.onPause();
        releaseCamera();
    }

    /** A safe way to get an instance of the Camera object. */
    private Camera getCameraInstance() {
        Camera c = null;
        //TODO:api level 9可选择摄像头，比如前置摄像头
        try {
            c = Camera.open();
        } catch (Exception e) {
            Log.e(CLASS_NAME, "open camera fail");
        }
        return c;
    }

    private void releaseCamera() {
        if (mCamera != null) {
            mPreviewing = false;
            mCamera.setPreviewCallback(null);
            mCamera.release();
            mCamera = null;
        }
    }

    private Runnable doAutoFocus = new Runnable() {
        public void run() {
            if (mPreviewing) {
                try {
                    mCamera.autoFocus(autoFocusCB);
                } catch (Exception e) {
                   Log.e(CLASS_NAME, "Error when running autoFocusCB"+ e.getMessage());
                }
            }
        }
    };

    private PreviewCallback previewCb = new PreviewCallback() {
        public void onPreviewFrame(byte[] data, Camera camera) {
            Camera.Parameters parameters = camera.getParameters();
            Size size = parameters.getPreviewSize();

            Image barcode = new Image(size.width, size.height, "Y800");
            barcode.setData(data);

            int result = mScanner.scanImage(barcode);

            if (result != 0) {
                mPreviewing = false;
                mCamera.setPreviewCallback(null);
                mCamera.stopPreview();

                SymbolSet syms = mScanner.getResults();

                playVibrate();// 振动代表成功获取二维码

                for (Symbol sym : syms) {
                    // 将扫描后的信息返回
                    Intent intent = new Intent();
                    intent.putExtra("Code", sym.getData());
                    setResult(RESULT_OK, intent);
                    finish();
                }
            }
        }
    };

    // Mimic continuous auto-focusing
    private AutoFocusCallback autoFocusCB = new AutoFocusCallback() {
        public void onAutoFocus(boolean success, Camera camera) {
            mAutoFocusHandler.postDelayed(doAutoFocus, 1000);
        }
    };

    /**
     * 震动
     */
    private void playVibrate() {
        // 打开震动
        Vibrator vibrator = (Vibrator) getSystemService(VIBRATOR_SERVICE);
        vibrator.vibrate(VIBRATE_DURATION);
    }
}
