/**
 * Dialog to present the user with a "pad" upon which to place their
 * signature and an OK/Cancel button to commit or discard their
 * signature.  Embeds SignatureView as the signature area.
 */
package nl.codeyellow.app;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;
import android.widget.PopupWindow;
import android.widget.TextView;
import java.lang.CharSequence;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import nl.codeyellow.view.SignatureView;
import org.apache.cordova.CallbackContext; // Ugh, but the alternatives are probably worse
import org.apache.cordova.PluginResult;

public class SignatureDialogFragment extends DialogFragment {
	protected CallbackContext callbackContext;
	protected CharSequence dialogTitle;
	
	public SignatureDialogFragment(CharSequence title, CallbackContext ctx) {
		dialogTitle = title;
		callbackContext = ctx;
	}

	// Closures are hard, so we jump through a few hoops and do it the Java way... The moronic way
	// (if there's a way to get at the dialog view from the title view I'd love to hear it:
	//  so far it didn't work because getParent keeps returning the Layout even if invoked
	//  on the parent etc)
	class DialogCloseListener implements View.OnClickListener {
		public AlertDialog dialog;
		public CallbackContext ctx;
		
		public DialogCloseListener(CallbackContext c) {
			ctx = c;
		}

		public void setDialog(AlertDialog d) {
			dialog = d;
		}
		
		@Override
		public void onClick(View view) {
			// Signal that the user has exited, just in
			// case we want to perform some sort of action
			// on the JS side.
			ctx.success((String)null);
			dialog.cancel();
		}
	}
	
	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState) {
		Activity act = getActivity();
		final SignatureView view = new SignatureView(act.getApplicationContext(), null);
		final CallbackContext ctx = callbackContext; // Silly Java

		// More silliness because the order of OK / Cancel keeps tripping people up,
		// so we present a "close" button at the top right and only use OK
		TextView titleLabelView = new TextView(act);
		titleLabelView.setText(dialogTitle);
		titleLabelView.setTextSize(TypedValue.COMPLEX_UNIT_MM, 5);
		titleLabelView.setPadding(15, 0, 0, 0);
		TextView titleCloseView = new TextView(act);
		titleCloseView.setText("╳");
		titleCloseView.setTextSize(TypedValue.COMPLEX_UNIT_MM, 5);
		RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
		params.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
		titleCloseView.setLayoutParams(params);
		titleCloseView.setPadding(0, 0, 15, 0);
		DialogCloseListener listener = new DialogCloseListener(ctx);
		titleCloseView.setOnClickListener(listener);
		RelativeLayout titleView = new RelativeLayout(act);
		titleView.setGravity(Gravity.FILL_HORIZONTAL | Gravity.CENTER_VERTICAL);
		titleView.setLayoutParams(new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.FILL_PARENT));
		titleView.addView(titleLabelView);
		titleView.addView(titleCloseView);
		
		AlertDialog dialog = new AlertDialog.Builder(act)
			.setView(view)
			.setCustomTitle(titleView)
			.setPositiveButton(
				android.R.string.ok,
				new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						Bitmap bmp = view.getBitmap();
						// Drawing nothing is the same as canceling (for now?)
						if (bmp == null) {
							ctx.success((String)null);
						} else {
							// Maybe use getAllocationByteCount()+8?  It
							// was added in API level 19.
							int size = bmp.getWidth() * bmp.getHeight() * 4 + 8;
							ByteBuffer buf = ByteBuffer.allocate(size); // BIG_ENDIAN
							bmp.copyPixelsToBuffer(buf);
							
							// We can't put the metadata at the start because
							// copyPixelsToBuffer() ignores buf's position...
							buf.putInt(bmp.getWidth());
							buf.putInt(bmp.getHeight());
							ctx.success(buf.array());
						}
							dialog.dismiss();
					}
				})
			.create();
		listener.setDialog(dialog);
		return dialog;
	}
}