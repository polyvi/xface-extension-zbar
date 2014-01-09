
/*
 Copyright 2012-2013, Polyvi Inc. (http://polyvi.github.io/openxface)
 This program is distributed under the terms of the GNU General Public License.

 This file is part of xFace.

 xFace is free software: you can redistribute it and/or modify
 it under the terms of the GNU General Public License as published by
 the Free Software Foundation, either version 3 of the License, or
 (at your option) any later version.

 xFace is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with xFace.  If not, see <http://www.gnu.org/licenses/>.
*/

package com.polyvi.xface.extension.zbar;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.PluginResult;
import org.json.JSONArray;
import org.json.JSONException;

import com.polyvi.xface.util.XUtils;

import android.app.Activity;
import android.content.Intent;

public class XZBarExt extends CordovaPlugin {

    private static final String COMMAND_START = "start";
    //ZBar startActivityForResult的标志位
    private static final int ZBAR_REQUEST_CODE = XUtils.genActivityRequestCode();
    //不能同时调用多次这个扩展，需要锁。
    private static boolean mLock = false;

    /*
     * TODO:在XExtension中抽象一个函数出来 用于根据API level判断是否可以执行
     * 该扩展的action isSupportedInCurApiLevel（String action），
     * 类似于isSync的设计，然后统一到ExtensionManager中调用
     */
    //Camera.setDisplayOrientation函数在android 2.1上不支持，故暂不支持android 2.1
    private static final int apiLevel = android.os.Build.VERSION.SDK_INT;
    private CallbackContext mCallbackCtx = null;



    @Override
    public boolean execute(String action, JSONArray args,
            CallbackContext callbackContext) throws JSONException {
        if(COMMAND_START.equals(action) && !mLock && apiLevel >= 8) {
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
        if(requestCode == ZBAR_REQUEST_CODE && mCallbackCtx !=null && this.webView.getContext() !=null) {
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
