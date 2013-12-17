/**
 * A plugin for Apache Cordova (Phonegap) which will ask the user to
 * write his or her a signature, which gets captured into an image.
 *
 * Copyright (c) 2013, Code Yellow B.V.
 *
 * Heavily based on Holly Schinsky's tutorial:
 * http://devgirl.org/2013/09/17/how-to-write-a-phonegap-3-0-plugin-for-android/
 */
package nl.codeyellow.plugin;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaPlugin;
import org.json.JSONObject;
import org.json.JSONArray;
import org.json.JSONException;
import android.widget.PopupWindow;
import android.content.Context;
import nl.codeyellow.view.SignatureView;

public class SignaturePlugin extends CordovaPlugin {
	@Override
	public boolean execute(String action, JSONArray args, CallbackContext callbackContext)
		throws JSONException
	{
		if (action.equals("new")) {
			Context ctx = this.cordova.getActivity().getApplicationContext();
			new PopupWindow(new SignatureView(ctx, null));
			return true;
		}
		return false;
	}
}