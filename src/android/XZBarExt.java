package com.polyvi.xface.extension.zbar;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.PluginResult;
import org.json.JSONArray;
import org.json.JSONException;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;

public class XZBarExt extends CordovaPlugin {

    private static boolean mLock = false;        //不能同时调用多次这个扩展，需要锁。

     /*
      *Camera.setDisplayOrientation函数在android 2.1上不支持，故暂不支持android 2.1
     */
    private static final int apiLevel = android.os.Build.VERSION.SDK_INT;
    private CallbackContext mCallbackCtx = null;



    @Override
    public boolean execute(String action, JSONArray args,
            CallbackContext callbackContext) throws JSONException {
        if("start".equals(action) && !mLock && apiLevel >= 8) {
            mCallbackCtx = callbackContext;
            mLock = true;
            Intent intent = new Intent();
            intent.setClass(cordova.getActivity(), XCameraActivity.class);
            cordova.startActivityForResult(this, intent, 1);
            PluginResult result = new PluginResult(PluginResult.Status.NO_RESULT);
            result.setKeepCallback(true);
            callbackContext.sendPluginResult(result);
            return true;
        }
        return false;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent intent) {
        mLock = false;
        if(requestCode == 1 && mCallbackCtx !=null && this.webView.getContext() !=null) {
            if (resultCode ==  Activity.RESULT_OK && intent != null) {
                //返回的条形码数据
                String code = intent.getStringExtra("Code");
                mCallbackCtx.success(code);
                PluginResult result = new PluginResult(PluginResult.Status.OK);
                mCallbackCtx.sendPluginResult(result);
            } else {
                mCallbackCtx.error("Error");
            }
        }
    }
}
