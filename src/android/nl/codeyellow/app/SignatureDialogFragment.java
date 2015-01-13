/**
 * Dialog to present the user with a "pad" upon which to place their signature
 * and an OK/Cancel button to commit or discard their signature. Embeds
 * SignatureView as the signature area.
 */
package nl.codeyellow.app;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.content.pm.ActivityInfo;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.widget.Button;
import android.widget.LinearLayout.LayoutParams;
import android.widget.RelativeLayout;
import android.widget.TextView;
import java.nio.ByteBuffer;
import com.github.gcacace.signaturepad.views.SignaturePad;
import org.apache.cordova.CallbackContext;

public class SignatureDialogFragment extends DialogFragment {

    protected CallbackContext callbackContext;
    protected CharSequence dialogTitle;
    protected CharSequence saveLabel;
    protected CharSequence clearLabel;
    protected CharSequence htmlString;

    public SignatureDialogFragment(CharSequence title, CharSequence save, CharSequence clear, CharSequence html, CallbackContext ctx) {
        dialogTitle = title;
        callbackContext = ctx;
        htmlString = html;
        saveLabel = save;
        clearLabel = clear;
    }

    // Closures are hard, so we jump through a few hoops and do it the Java way... The moronic way
    // (if there's a way to get at the dialog view from the title view I'd love to hear it:
    // so far it didn't work because getParent keeps returning the Layout even if invoked
    // on the parent etc)
    class DialogCloseListener implements View.OnClickListener {

        public AlertDialog alertDialog;
        public CallbackContext callbackContext;

        public DialogCloseListener(CallbackContext callbackContext) {
            this.callbackContext = callbackContext;
        }

        public void setDialog(AlertDialog alertDialog) {
            this.alertDialog = alertDialog;
        }

        @Override
        public void onClick(View view) {
            // Signal that the user has exited, just in
            // case we want to perform some sort of action
            // on the JS side.
            callbackContext.success((String) null);
            alertDialog.cancel();
        }

    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        final Activity activity = getActivity();
        final SignaturePad signaturePad = new SignaturePad(activity.getApplicationContext(), null);
        final CallbackContext ctx = callbackContext; // Silly Java

        // More silliness because the order of OK / Cancel keeps tripping people up,
        // so we present a "close" button at the top right and only use OK
        TextView titleLabelView = new TextView(activity);
        titleLabelView.setText(dialogTitle);
        titleLabelView.setTextSize(TypedValue.COMPLEX_UNIT_MM, 5);
        titleLabelView.setPadding(15, 0, 0, 0);

        TextView titleCloseView = new TextView(activity);
        titleCloseView.setText("╳");
        titleCloseView.setTextSize(TypedValue.COMPLEX_UNIT_MM, 5);
        RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
        params.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
        titleCloseView.setLayoutParams(params);
        titleCloseView.setPadding(0, 0, 15, 0);
        DialogCloseListener listener = new DialogCloseListener(ctx);
        titleCloseView.setOnClickListener(listener);

        RelativeLayout titleView = new RelativeLayout(activity);
        titleView.setGravity(Gravity.FILL_HORIZONTAL | Gravity.CENTER_VERTICAL);
        titleView.setLayoutParams(new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.FILL_PARENT));
        titleView.addView(titleLabelView);
        titleView.addView(titleCloseView);

        RelativeLayout mainView = new RelativeLayout(activity);
        mainView.setGravity(Gravity.FILL_HORIZONTAL | Gravity.CENTER_VERTICAL);
        mainView.setLayoutParams(new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.FILL_PARENT));
        mainView.addView(signaturePad);

        if (htmlString != null) {
            // XXX TODO: Find a way to use the same class as the
            // current Cordova Webview.  If we're using Crosswalk it
            // should automatically pick up that class.
            WebView htmlView = new WebView(activity);
            WebSettings setting = htmlView.getSettings();

            setting.setJavaScriptEnabled(true);
            setting.setDefaultTextEncodingName("utf-8");
            htmlView.setLayerType(WebView.LAYER_TYPE_SOFTWARE, null); // FAST RENDERING, PLEASE (but will be slower when using fancy effects)

            // Nobody knows exactly how this works...
            htmlView.loadDataWithBaseURL("file:///android_asset/www/", htmlString.toString(), "text/html", null, null);
            mainView.addView(htmlView);
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        builder.setView(mainView);
        builder.setCustomTitle(titleView);
        builder.setPositiveButton(saveLabel, null);
        builder.setNegativeButton(clearLabel, null);

        AlertDialog dialog = builder.create();

        dialog.setOnShowListener(new DialogInterface.OnShowListener() {

            @Override
            public void onShow(final DialogInterface dialog) {

                activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);

                ((AlertDialog) dialog).getWindow().setLayout(LayoutParams.FILL_PARENT, LayoutParams.FILL_PARENT);

                Button p = ((AlertDialog) dialog).getButton(AlertDialog.BUTTON_POSITIVE);
                p.setOnClickListener(new View.OnClickListener() {

                    @Override
                    public void onClick(View view) {
                        Bitmap bmp = signaturePad.getSignatureBitmap();

                        // Drawing nothing is the same as canceling (for now?)
                        if (bmp == null) {
                            ctx.success((String) null);
                        } else {
                            // Maybe use getAllocationByteCount()+8? It
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

                        ((AlertDialog) dialog).dismiss();
                    }

                });

                Button n = ((AlertDialog) dialog).getButton(AlertDialog.BUTTON_NEGATIVE);
                n.setOnClickListener(new View.OnClickListener() {

                    @Override
                    public void onClick(View view) {
                        signaturePad.clear();
                    }

                });
            }
        });

        listener.setDialog(dialog);

        return dialog;
    }

    @Override
    public void onCancel(DialogInterface dialog) {
        super.onCancel(dialog);

        getActivity().setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
    }

    @Override
    public void onDismiss(DialogInterface dialog) {
        super.onDismiss(dialog);

        getActivity().setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
    }

}
