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

import android.app.Activity;
import android.content.Context;
import android.view.Gravity;
import android.view.View;
import android.widget.PopupWindow;
import android.widget.RelativeLayout;
import nl.codeyellow.view.SignatureView;
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
			Activity act = this.cordova.getActivity();
			SignatureView view = new SignatureView(act.getApplicationContext(), null);
			
			/* Create layout programmatically because we
			   can't obtain a reference to 'R', which
			   lives in a project-specific package, the
			   name of which varies across projects. */
			RelativeLayout layout = new RelativeLayout(act);
			RelativeLayout.LayoutParams params =
				new RelativeLayout.LayoutParams(
					RelativeLayout.LayoutParams.MATCH_PARENT,
					RelativeLayout.LayoutParams.MATCH_PARENT);
			layout.addView(view, params);

			final PopupWindow popup = new PopupWindow(act);
			popup.setContentView(layout);
			popup.setHeight(1000);
			popup.setWidth(1000);
			popup.showAtLocation(layout, Gravity.NO_GRAVITY, 100, 100);

			// Button accept = layout.findViewById(R.id.accept);
			// accept.setOnClickListener(new OnClickListener() {
			// 	@Override
			// 	public void onClick(View v) {
			// 		popup.dismiss();
			// 		callbackContext.success("HAI");
			// 	}
			// });
			return true;
		} else {
			callbackContext.error("Unknown action: "+action);
			return false;
		}
	}
}