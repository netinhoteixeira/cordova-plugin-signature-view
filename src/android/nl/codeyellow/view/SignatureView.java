/**
 * Custom View for capturing the user's signature
 *
 * Code taken from a post by Eric Burke on Square's Engineering Blog:
 * http://corner.squareup.com/2010/07/smooth-signatures.html
 *
 * This falls under Apache License 2.0
 */
package nl.codeyellow.view;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import java.io.ByteArrayOutputStream;
import android.util.AttributeSet;
import android.util.Base64;
import android.util.Base64OutputStream;
import android.view.MotionEvent;
import android.view.View;

public class SignatureView extends View {

	/** Need to track this so the dirty region can accommodate the stroke. **/
	private float STROKE_WIDTH = 1.5f; // TODO: Make this dependent on pressure
	private float HALF_STROKE_WIDTH = .75f;

	private Paint paint = new Paint();
	private Path path = new Path();

	/**
	 * Optimizes painting by invalidating the smallest possible area.
	 */
	private float lastTouchX;
	private float lastTouchY;
	private final RectF dirtyRect = new RectF();

	public SignatureView(Context context, AttributeSet attrs) {
		super(context, attrs);

		paint.setAntiAlias(true);
		paint.setColor(Color.WHITE); // TODO: Make this configurable
		paint.setStyle(Paint.Style.STROKE);
		paint.setStrokeJoin(Paint.Join.ROUND);
		paint.setStrokeWidth(STROKE_WIDTH);
	}

	/**
	 * Erases the signature.
	 */
	public void clear() {
		path.reset();

		// Repaints the entire view.
		invalidate();
	}

	/**
	 * Extract the current bitmap state of the display, encoded in
	 * a data URI.
	 */
	public String getBitmapDataURI() {
		if (!isDrawingCacheEnabled())
			buildDrawingCache();
		
		Bitmap bmp = getDrawingCache();
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		
		// TODO: Is JPEG the best we can use here?
		// TODO: Provide a way to determine image size.
		byte[] header = "data:image/jpeg;base64,".getBytes();
		out.write(header, 0, header.length);

		Base64OutputStream b64out = new Base64OutputStream(out, Base64.DEFAULT);
		bmp.compress(Bitmap.CompressFormat.JPEG, 100, b64out);
		
		if (!isDrawingCacheEnabled())
			destroyDrawingCache();
		
		return out.toString();
	}

	@Override
	protected void onDraw(Canvas canvas) {
		canvas.drawPath(path, paint);
	}

	@Override
	public boolean onTouchEvent(MotionEvent event) {
		float eventX = event.getX();
		float eventY = event.getY();

		// XXX TODO: Some kind of handling of just touching
		// the pen on the display.  Currently it does not draw
		// any "dot" or similar.
		switch (event.getAction()) {
		case MotionEvent.ACTION_DOWN:
			path.moveTo(eventX, eventY);
			lastTouchX = eventX;
			lastTouchY = eventY;
			// There is no end point yet, so don't waste cycles invalidating.
			return true;

		case MotionEvent.ACTION_MOVE:
		case MotionEvent.ACTION_UP:
			// Start tracking the dirty region.
			resetDirtyRect(eventX, eventY);

			// When the hardware tracks events faster than they are delivered, the
			// event will contain a history of those skipped points.
			int historySize = event.getHistorySize();
			for (int i = 0; i < historySize; i++) {
				float historicalX = event.getHistoricalX(i);
				float historicalY = event.getHistoricalY(i);
				expandDirtyRect(historicalX, historicalY);
				path.lineTo(historicalX, historicalY);
			}

			// After replaying history, connect the line to the touch point.
			path.lineTo(eventX, eventY);
			break;

		default:
			return false;
		}

		// Include half the stroke width to avoid clipping.
		invalidate(
			(int) (dirtyRect.left - HALF_STROKE_WIDTH),
			(int) (dirtyRect.top - HALF_STROKE_WIDTH),
			(int) (dirtyRect.right + HALF_STROKE_WIDTH),
			(int) (dirtyRect.bottom + HALF_STROKE_WIDTH));
    
		lastTouchX = eventX;
		lastTouchY = eventY;

		return true;
	}

	/**
	 * Called when replaying history to ensure the dirty region includes all
	 * points.
	 */
	private void expandDirtyRect(float historicalX, float historicalY) {
		if (historicalX < dirtyRect.left) {
			dirtyRect.left = historicalX;
		} else if (historicalX > dirtyRect.right) {
			dirtyRect.right = historicalX;
		}
		if (historicalY < dirtyRect.top) {
			dirtyRect.top = historicalY;
		} else if (historicalY > dirtyRect.bottom) {
			dirtyRect.bottom = historicalY;
		}
	}

	/**
	 * Resets the dirty region when the motion event occurs.
	 */
	private void resetDirtyRect(float eventX, float eventY) {

		// The lastTouchX and lastTouchY were set when the ACTION_DOWN
		// motion event occurred.
		dirtyRect.left = Math.min(lastTouchX, eventX);
		dirtyRect.right = Math.max(lastTouchX, eventX);
		dirtyRect.top = Math.min(lastTouchY, eventY);
		dirtyRect.bottom = Math.max(lastTouchY, eventY);
	}
}
