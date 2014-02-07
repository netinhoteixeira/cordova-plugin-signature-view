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
import android.util.AttributeSet;
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
	private RectF usedRect = null;

	public SignatureView(Context context, AttributeSet attrs) {
		super(context, attrs);

		setBackgroundColor(Color.WHITE);
		paint.setAntiAlias(true);
		paint.setColor(Color.BLACK); // TODO: Make this configurable
		paint.setStyle(Paint.Style.STROKE);
		paint.setStrokeJoin(Paint.Join.ROUND);
		paint.setStrokeWidth(STROKE_WIDTH);
	}

	/**
	 * Erases the signature.
	 */
	public void clear() {
		path.reset();
		usedRect = null;
		// Repaints the entire view.
		invalidate();
	}

	/**
	 * Extract the current bitmap state of the display.  Returns an
	 * immutable ARGB_8888 bitmap or null if nothing has been drawn.
	 */
	public Bitmap getBitmap() {
		if (usedRect == null)
			return null;
		
		if (!isDrawingCacheEnabled())
			buildDrawingCache();
		
		Bitmap bmp = getDrawingCache().copy(Bitmap.Config.ARGB_8888, false);
		
		if (!isDrawingCacheEnabled())
			destroyDrawingCache();

		// Calculate the edges of the drawing, accommodating
		// for the width of the stroke.
		int x = (int)Math.floor(usedRect.left-HALF_STROKE_WIDTH);
		int y = (int)Math.floor(usedRect.top-HALF_STROKE_WIDTH);
		int width = (int)Math.ceil(usedRect.width()+HALF_STROKE_WIDTH);
		int height = (int)Math.ceil(usedRect.height()+HALF_STROKE_WIDTH);

		// Clip on image size (order matters here!)
		x = (int)Math.max(0, x);
		y = (int)Math.max(0, y);
		width = (int)Math.min(width, bmp.getWidth() - x);
		height = (int)Math.min(height, bmp.getHeight() - y);

		return Bitmap.createBitmap(bmp, x, y, width, height);
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
			if (usedRect == null)
				usedRect = new RectF(eventX, eventY, eventX, eventY);
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

			// Augment the used region with the newly dirtied region
			usedRect.union(dirtyRect);
			
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
