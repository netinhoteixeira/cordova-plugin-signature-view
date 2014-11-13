/**
 * A plugin for Apache Cordova (Phonegap) which will ask the user to
 * write his or her a signature, which gets captured into an image.
 *
 * Copyright (c) 2013-2014, Code Yellow B.V.
 *
 * Heavily based on Holly Schinsky's tutorial:
 * http://devgirl.org/2013/09/17/how-to-write-a-phonegap-3-0-plugin-for-android/
 */
package nl.codeyellow.plugin;

import android.app.Activity;
import android.app.FragmentManager;
import android.content.Context;
import nl.codeyellow.app.SignatureDialogFragment;
import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaPlugin;
import org.json.JSONObject;
import org.json.JSONArray;
import org.json.JSONException;

public class SignaturePlugin extends CordovaPlugin {
	@Override
	public boolean execute(String action, JSONArray args, CallbackContext callbackContext)
		throws JSONException
	{
		if (action.equals("new")) {
			// TODO: Make default title translatable
			String title = "Please sign below", htmlFile = null;
			if (args.length() >= 2) htmlFile = args.getString(1);
			if (args.length() >= 1) title = args.getString(0);

			Activity act = this.cordova.getActivity();
			FragmentManager fragmentManager = act.getFragmentManager();
			SignatureDialogFragment frag = new SignatureDialogFragment(title, htmlFile, callbackContext);
			frag.show(fragmentManager, "dialog");
			return true;
		} else {
			callbackContext.error("Unknown action: "+action);
			return false;
		}
	}
}