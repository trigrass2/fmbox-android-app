package org.apache.cordova.xxx;

import org.apache.cordova.*;
import org.ionic.bluetooth.BlueTooth;
import org.json.JSONArray;
import org.json.JSONException;

import com.ionic.todo.FmBox;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.IntentFilter;
import android.content.Intent;
import android.util.Log;

public class CustomPlugin extends CordovaPlugin {

	private static final String TAG = "CustomPlugin";
	private static CallbackContext msgCallback;

	@Override
	public void initialize(CordovaInterface cordova, CordovaWebView webView) {
		super.initialize(cordova, webView);
	}

	public synchronized boolean execute(String action, JSONArray args,
			CallbackContext callback) throws JSONException {

		Log.d(TAG, "execute: " + action);

		if (action.equals("start")) {
			CustomPlugin.msgCallback = callback;
			PluginResult result = new PluginResult(PluginResult.Status.NO_RESULT);
			result.setKeepCallback(true);
			callback.sendPluginResult(result);
		}
		
		if (action.equals("send")) {
			String arg = args.getString(0);
			if (arg.equals("bluetoothScan")) {
				BlueTooth.scan();
			} else {
				BlueTooth.send(arg);
			}
		}

		return true;
	}
	
	public static synchronized void send(String msg) {
		Log.d(TAG, "send: " + msg);
		if (msgCallback == null)
			return;
		PluginResult result = new PluginResult(PluginResult.Status.OK, msg);
		result.setKeepCallback(true);
		CustomPlugin.msgCallback.sendPluginResult(result);
	}

}
