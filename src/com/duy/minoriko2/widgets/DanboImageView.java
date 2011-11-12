/**
 * Copyright (c) 2011 Duy Truong <hduyudh@gmail.com>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.duy.minoriko2.widgets;

import java.util.ArrayList;

import com.duy.minoriko2.ImgViewActivity;
import com.duy.minoriko2.model.Note;
import com.duy.minoriko2.model.Post;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Picture;
import android.graphics.PointF;
import android.graphics.RectF;
import android.graphics.Paint.Style;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.util.FloatMath;
import android.view.GestureDetector;
import android.view.GestureDetector.OnGestureListener;
import android.view.GestureDetector.OnDoubleTapListener;
import android.view.MotionEvent;

public class DanboImageView extends NotifyImageView implements
        OnGestureListener, OnDoubleTapListener,
        NotifyImageView.ImageReadyListener {
    //Matrix matrix = new Matrix();
    Matrix savedMatrix = new Matrix();
    Post post = null;
    public ArrayList<Note> notes = null;
    boolean hideAllNotes;
    Note currentNote;
    Bitmap currentNoteBmp;
    float noteViewX, noteViewY;
    ImgViewActivity context;
    float noteRight = 0;
    float noteBottom = 0;
    GestureDetector gestureDetector;
    public boolean notesDone;
    public NotifyImageView previewImgView;


    // Stuffs for touch
    // 4 states for the FSM. Refer to draft paper for the machine.
    enum Mode {
        UP, DOWN, ZOOM, PAN
    }
    Mode mode = Mode.UP;
    // Remember some things for zooming
    PointF start = new PointF();
    PointF mid = new PointF();
    float oldDist = 1f;
    boolean zoomLimitedX = true;
    private PrevNextHandler prevNextHandler = null;

    // Paints for drawings
    Paint bmpPaint = new Paint();
    Paint noteFillPaint = new Paint();
    Paint noteStrokePaint = new Paint();
    Paint noteViewPaint = new Paint();
    Paint loadingNotesTextPaint = new Paint();
    Paint loadingNotesBoxPaint = new Paint();

    // Constructors
    public DanboImageView(Context context) {
        super(context);
        init((ImgViewActivity) context);
    }
    public DanboImageView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init((ImgViewActivity) context);
    }
    public DanboImageView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init((ImgViewActivity) context);
    }

    /** Initialize stuffs
     */
    private void init(ImgViewActivity context) {
        this.context = context;
        gestureDetector = new GestureDetector(context, this);
        gestureDetector.setOnDoubleTapListener(this);
        previewImgView = new NotifyImageView(context);
        previewImgView.setImageReadyListener(this);

        notesDone = false;

        Matrix m = new Matrix();
        m.setTranslate(1f, 1f);
        setImageMatrix(m);

        bmpPaint.setFilterBitmap(true);
        noteFillPaint.setARGB(127, 255, 255, 238);
        noteFillPaint.setStyle(Style.FILL);
        noteStrokePaint.setARGB(127, 0, 0, 0);
        noteStrokePaint.setStyle(Style.STROKE);
        noteStrokePaint.setStrokeWidth(1);
        noteViewPaint.setFilterBitmap(true);
        noteViewPaint.setAlpha(245);
        loadingNotesTextPaint.setARGB(255, 255, 255, 255);
        loadingNotesTextPaint.setAntiAlias(true);
        loadingNotesTextPaint.setTextAlign(Paint.Align.LEFT);
        loadingNotesTextPaint.setTextSize(22f);
        loadingNotesBoxPaint.setARGB(220, 0, 0, 0);
    }

    // Functions
    /** Set the post that this View should be displaying
     *
     * @param p the Post object containing the post info
     */
    public void setPost(Post p) {
        this.post = p;
        centerAndZoomOut(getWidth(), getHeight());
        requestLayout();
        invalidate();
    }

    public void setNotes(ArrayList<Note> notes) {
        if (this.notes != null) {
            for (Note note : this.notes) {
                if (note != null) {
                    note.destroy();
                }
            }
        }
        this.notes = notes;
        currentNote = null;
        currentNoteBmp = null;
        notesDone = true;
        hideAllNotes = false;
        invalidate();
    }

    /**
     * Draw the currently selected note (currentNote)
     * into currentNoteBmp (a Bitmap), ready to be rendered
     * This method utilizes a hacky method using a WebView to render
     * notes that have HTML
     */
    public void drawNote(Note note) {
        if (post == null || notes == null ||
                (note.wv == null && note.pic == null) ||
                !(getDrawable() instanceof BitmapDrawable))
            return;

        if (note.pic == null) { // meaning note.wv != null
            currentNote = null;
            Picture pic = note.wv.capturePicture();
            if (pic.getWidth() == 0 || pic.getHeight() == 0) {
                currentNoteBmp = null;
                return;
            }
            note.pic = pic;
            note.wv = null;
        }

        currentNote = note;
        Bitmap tmpBmp = Bitmap.createBitmap(
                note.pic.getWidth(),
                note.pic.getHeight(),
                Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(tmpBmp);
        canvas.drawPicture(note.pic);

        Bitmap bitmap = ((BitmapDrawable) getDrawable()).getBitmap();
        float scale = (post.w > post.h) ?
                (((float)bitmap.getWidth()) / post.w) :
                (((float)bitmap.getHeight()) / post.h);
        //}

        // crop the bitmap
        int i;
        for (i=tmpBmp.getWidth()-1; i>=0; i--)
            if ((tmpBmp.getPixel(i, 0) & 0x00FFFFFF) != 0x00FFFFFF)
                break;
        currentNoteBmp = Bitmap.createBitmap(tmpBmp,
                0, 0, i+1, tmpBmp.getHeight());
        tmpBmp.recycle();

        noteViewX = note.x - 2;
        noteViewY = note.y + note.h + 2;
        noteViewX *= scale;
        noteViewY *= scale;
        noteRight = noteViewX + currentNoteBmp.getWidth();
        noteBottom = noteViewY + currentNoteBmp.getHeight();

        zoomLimit();
        panningBound();
        invalidate();
    }

    /** This method does only 1 thing: Scale and translate the image
     *  so that it fits on the screen, after size changed (inc. on load)
     */
    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        centerAndZoomOut(w, h);
        zoomLimitedX = true;
    }

    /**
     * Aside from notifying the ImageReadyListener by super, also set the
     * new matrix to match the new image
     */
    @Override
    public void setImageDrawable(Drawable drawable) {
        super.setImageDrawable(drawable);
        centerAndZoomOut(getWidth(), getHeight());
    }

    /**
     * This is the main drawing method of the view.
     * It renders the image, all the note boxes,
     * and the currently viewed note
     */
    @Override
    public void onDraw(Canvas canvas) {

        canvas.save();
        Bitmap bitmap = null;

        //draw the "Loading notes..." text
        if (post == null) {
            canvas.drawRect(0, 0, getWidth(), 25, loadingNotesBoxPaint);
            canvas.drawText("Loading post...", 3, 20, loadingNotesTextPaint);
            canvas.restore();
            return;
        } else if (!(getDrawable() instanceof BitmapDrawable)) {
            if (previewImgView.getDrawable() instanceof BitmapDrawable) {
                bitmap = ((BitmapDrawable) previewImgView.getDrawable())
                        .getBitmap();
            } else {
                canvas.drawRect(0, 0, getWidth(), 25, loadingNotesBoxPaint);
                canvas.drawText("Loading preview...",
                        3, 20, loadingNotesTextPaint);
                canvas.restore();
                return;
            }
        } else {
            bitmap = ((BitmapDrawable) getDrawable()).getBitmap();
            //previewImgView.setImageBitmap(null);
            //previewImgView.setImageDrawable(null);
        }

        // draw the main image
        Matrix matrix = this.getImageMatrix();
        canvas.drawBitmap(bitmap, matrix, bmpPaint);

        if (!notesDone) {
            canvas.drawRect(0, 0, getWidth(), 25, loadingNotesBoxPaint);
            canvas.drawText("Loading notes...", 3, 20, loadingNotesTextPaint);
        }


        if (!hideAllNotes && notes != null) {
            float scale = 1;
            //if (post.bitmapIsSample) {
                scale = (post.w > post.h) ?
                        (((float)bitmap.getWidth()) / post.w) :
                        (((float)bitmap.getHeight()) / post.h);
            //}
            // Draw the note boxes
            for (Note note : notes) {
                RectF rect = new RectF(
                        note.x*scale, note.y*scale,
                        (note.x + note.w)*scale,
                        (note.y + note.h)*scale);
                matrix.mapRect(rect);
                canvas.drawRect(rect, noteFillPaint);
                canvas.drawRect(rect, noteStrokePaint);
            }

            // draw the currently viewed note
            if (currentNote != null && currentNoteBmp != null) {
                Matrix m2 = new Matrix(matrix);
                m2.preTranslate(noteViewX, noteViewY);
                canvas.drawBitmap(currentNoteBmp, m2, noteViewPaint);
            }
        }

        canvas.restore();
    }

    /**
     * This is the method called on touch events
     * This method constructs the matrix reflecting the last touch event
     * that the image can be drawn against.
     */
    @Override
    public boolean onTouchEvent(MotionEvent event) {

        // escalate to upper level first (only for fling)
        if (gestureDetector.onTouchEvent(event))
            return true;

        if (post == null || !(getDrawable() instanceof BitmapDrawable))
            return true;

        Matrix matrix = this.getImageMatrix();

        // Handle touch events here...
        switch (event.getAction() & MotionEvent.ACTION_MASK) {
        case MotionEvent.ACTION_DOWN:
            savedMatrix.set(matrix);
            start.set(event.getX(), event.getY());
            mode = Mode.DOWN;
            break;

        case MotionEvent.ACTION_POINTER_DOWN:
            oldDist = spacing(event);
            if (oldDist > 10f) {
                savedMatrix.set(matrix);
                midPoint(mid, event);
                mode = Mode.ZOOM;
            }
            break;

        case MotionEvent.ACTION_UP:
            if (mode == Mode.DOWN) {
                onClick(event.getX(), event.getY());
            }
            mode = Mode.UP;

        case MotionEvent.ACTION_POINTER_UP:
            mode = Mode.UP;
            break;

        case MotionEvent.ACTION_MOVE:
            if (mode == Mode.DOWN) {
                float dX = event.getX() - start.x;
                float dY = event.getY() - start.y;
                float dist = FloatMath.sqrt(dX*dX + dY*dY);
                if (dist > 15f) {
                    matrix.set(savedMatrix);
                    matrix.postTranslate(event.getX() - start.x,
                                    event.getY() - start.y);
                    if (panningBound()) {
                        savedMatrix.set(matrix);
                        start.set(event.getX(), event.getY());
                        mode = Mode.PAN;
                    }
                    invalidate();
                    mode = Mode.PAN;
                }
            }
            else if (mode == Mode.PAN) {
                matrix.set(savedMatrix);
                matrix.postTranslate(event.getX() - start.x,
                                event.getY() - start.y);
                if (panningBound()) {
                    savedMatrix.set(matrix);
                    start.set(event.getX(), event.getY());
                    mode = Mode.PAN;
                }

                invalidate();
                mode = Mode.PAN;
            }
            else if (mode == Mode.ZOOM) {
                float newDist = spacing(event);
                if (newDist > 10f) {
                    matrix.set(savedMatrix);
                    float scale = newDist / oldDist;
                    matrix.postScale(scale, scale, mid.x, mid.y);
                    zoomLimit();
                    panningBound();
                    invalidate();
                }
            }
            break;

        default:
            return false;
        }

        return true; // indicate event was handled
    }

    @Override
    public boolean onFling(MotionEvent e1, MotionEvent e2,
            float vX, float vY) {
        if ((vX < -1200 || vX > 1200) && zoomLimitedX) {
            if (vX < 0) {
                if (prevNextHandler != null)
                    prevNextHandler.next();
            } else {
                if (prevNextHandler != null)
                    prevNextHandler.prev();
            }
            return true;
        } else {
            return false;
        }
    }
    @Override
    public boolean onDown(MotionEvent event) {
        return false;
    }
    @Override
    public void onLongPress(MotionEvent arg0) {
    }
    @Override
    public boolean onScroll(MotionEvent e1, MotionEvent e2,
            float dX, float dY) {
        return false;
    }
    @Override
    public void onShowPress(MotionEvent arg0) {
    }
    @Override
    public boolean onSingleTapUp(MotionEvent arg0) {
        return false;
    }
    @Override
    public boolean onDoubleTap(MotionEvent e) {
        // TODO Auto-generated method stub
        return false;
    }
    @Override
    public boolean onDoubleTapEvent(MotionEvent e) {
        // TODO Auto-generated method stub
        return false;
    }
    @Override
    public boolean onSingleTapConfirmed(MotionEvent e) {
        // TODO Auto-generated method stub
        return false;
    }

    /**
     * This method handles clicking on the image.
     * If hideAllNotes is true, set it to false and return
     * If clicking inside a note, display its content
     * If clicking outside a note and a noteview is displayed, hide noteview
     * If clicking outside a note and no noteview displayed, hide all notes
     * @param x horizontal position of the click
     * @param y vertical position of the click
     */
    private void onClick(float x, float y) {
        if (prevNextHandler != null)
            prevNextHandler.click();

        if (post == null || notes == null || notes.size() == 0)
            return;

        if (hideAllNotes) {
            hideAllNotes = false;
            invalidate();
            return;
        }

        // only display notes when current bitmap is the main image
        if (!(getDrawable() instanceof BitmapDrawable))
            return;
        Bitmap bitmap = ((BitmapDrawable) getDrawable()).getBitmap();

        // transform the note box into screen coordinate, then check bounds
        float scale = (post.w > post.h) ?
                (((float)bitmap.getWidth()) / post.w) :
                (((float)bitmap.getHeight()) / post.h);
        //}

        // if we clicked inside a note, display it and return
        for (Note note : notes) {
            RectF rect = new RectF(
                    note.x*scale, note.y*scale,
                    (note.x + note.w)*scale,
                    (note.y + note.h)*scale);
            getImageMatrix().mapRect(rect);

            // Give users a 3px margin to click on
            if (y > (rect.top - 3) &&
                y < (rect.bottom + 3) &&
                x > (rect.left - 3) &&
                x < (rect.right + 3)) {
                drawNote(note);
                return;
            }
        }

        // now we know that we clicked outside.
        if (currentNote != null) {
            currentNote = null;
            zoomLimit();
            panningBound();
            invalidate();
        } else {
            hideAllNotes = true;
            currentNote = null;
            zoomLimit();
            panningBound();
            invalidate();
        }
    }

    /**
     * Determine the space between the first two fingers
     * @param event the MotionEvent passed down from onTouch()
     * @return distance between the first 2 fingers
     */
    private float spacing(MotionEvent event) {
       // ...
       float x = event.getX(0) - event.getX(1);
       float y = event.getY(0) - event.getY(1);
       return FloatMath.sqrt(x * x + y * y);
    }

    /**
     * Calculate the mid point of the first two fingers.
     * @param point The Point object to return the result in
     * @param event The MotionEvent passed down from onTouch()
     */
    private void midPoint(PointF point, MotionEvent event) {
       // ...
       float x = event.getX(0) + event.getX(1);
       float y = event.getY(0) + event.getY(1);
       point.set(x / 2, y / 2);
    }

    /**
     * Limit zoom minimum to fullview
     * Unintuitive side effect: set isZoomedOut to true if zoom was limited.
     * false otherwise
     * @return true if zoom was limited
     */
    private boolean zoomLimit() {
        Bitmap bitmap = getCurrentBitmap();
        if (bitmap == null)
            return true;

        boolean ret = false;
        int viewHeight = getHeight();
        int viewWidth = getWidth();
        // = ((BitmapDrawable) getDrawable()).getBitmap();
        RectF rect = new RectF(0, 0, bitmap.getWidth(), bitmap.getHeight());
        Matrix matrix = this.getImageMatrix();

        // check to see if any note is displayed
        if (!hideAllNotes && currentNote != null) {
            if (rect.bottom < noteBottom)
                rect.bottom = noteBottom;
            if (rect.right < noteRight)
                rect.right = noteRight;
        }
        matrix.mapRect(rect);

        float height = rect.height();
        float width = rect.width();

        // Zoom limiting
        float scale = 1, scaleX = 1, scaleY = 1;

        if (height <= viewHeight) {
            scaleY = (float) ((float)viewHeight )/ ((float)height);
        }
        if (width <= viewWidth) {
            scaleX = (float) ((float)viewWidth)/ ((float)width);
        }

        if (width <= viewWidth*1.001) {
            zoomLimitedX = true;
        } else {
            zoomLimitedX = false;
        }

        scale = Math.min(scaleX, scaleY);
        matrix.postScale(scale, scale);

        if (scale > 1)
            ret = true;
        //zoomLimited = ret; // set zoomLimited if limited on both axes
        return ret;
    }

    /**
     * Center the image if smaller than screen; translate to eliminate
     * black bars
     * @return true if panning/zooming was limited
     */
    private boolean panningBound() {
        Bitmap bitmap = getCurrentBitmap();
        if (bitmap == null)
            return true;

        int viewHeight = getHeight();
        int viewWidth = getWidth();
        RectF rect = new RectF(0, 0, bitmap.getWidth(), bitmap.getHeight());
        Matrix matrix = this.getImageMatrix();

        // check to see if any note is displayed
        if (!hideAllNotes && currentNote != null) {
            if (rect.bottom < noteBottom)
                rect.bottom = noteBottom;
            if (rect.right < noteRight)
                rect.right = noteRight;
        }
        matrix.mapRect(rect);
        float height = rect.height();
        float width = rect.width();

        // Panning bounds
        float deltaX = 0, deltaY = 0;
        boolean ret = false;

        if (height < viewHeight) { // if height < ImgView's height, center
            deltaY = (viewHeight - height) / 2 - rect.top;
            ret = true;
        } else if (rect.top > 0) { // if top is below window top, move up
            deltaY = -rect.top;
            ret = true;
        } else if (rect.bottom < viewHeight) { // similarly, move down
            deltaY = viewHeight - rect.bottom;
            ret = true;
        }

        if (width < viewWidth) {
            deltaX = (viewWidth - width) / 2 - rect.left;
            ret = true;
        } else if (rect.left > 0) {
            deltaX = -rect.left;
            ret = true;
        } else if (rect.right < viewWidth) {
            deltaX = viewWidth - rect.right;
            ret = true;
        }

        matrix.postTranslate(deltaX, deltaY);
        return ret;
    }

    public void centerAndZoomOut(int w, int h) {
        Bitmap bitmap = getCurrentBitmap();
        if (bitmap == null)
            return;

        Matrix matrix = this.getImageMatrix();
        float viewHeight = h;
        float viewWidth = w;
        float bmpHeight = bitmap.getHeight();
        float bmpWidth = bitmap.getWidth();

        float scaleY = viewHeight / bmpHeight;
        float scaleX = viewWidth / bmpWidth;
        float scale = Math.min(scaleX, scaleY);
        matrix.setScale(scale, scale);

        RectF rect = new RectF(0, 0, bmpWidth, bmpHeight);
        matrix.mapRect(rect);
        float rHeight = rect.height();
        float rWidth = rect.width();
        float transY = (viewHeight - rHeight) / 2 - rect.top;
        float transX = (viewWidth - rWidth) / 2 - rect.left;
        matrix.postTranslate(transX, transY);

        requestLayout();
        invalidate();
    }

    /**
     * Get the currently displayed bitmap. It can be either the main image,
     * the preview image, or nothing.
     * @return The currently displayed bitmap, or null.
     */
    private Bitmap getCurrentBitmap() {
        if (getDrawable() instanceof BitmapDrawable) {
            return ((BitmapDrawable) getDrawable()).getBitmap();
        } else if (previewImgView.getDrawable() instanceof BitmapDrawable) {
            return ((BitmapDrawable) previewImgView.getDrawable()).getBitmap();
        } else {
            return null;
        }
    }

    /**
     * Set the handler for the prev/next swipe events
     * @param handler The handler
     */
    public void setPrevNextHandler(PrevNextHandler handler) {
        this.prevNextHandler = handler;
    }

    /**
     * Handles the swiping back and forth of this View
     * @author duy
     *
     */
    public interface PrevNextHandler {
        public void prev();
        public void click();
        public void next();
    }

    /**
     * Callback called by a NotifyImageView when the main image is finished.
     */
    @Override
    public void onImageLoaded(NotifyImageView imgView) {
        // only center and zoom out if main image is not yet loaded
        // else, just ignore the preview image
        if (!(getDrawable() instanceof BitmapDrawable)) {
            centerAndZoomOut(getWidth(), getHeight());
            zoomLimitedX = true;
            invalidate();
        }
    }
}
