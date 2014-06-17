package ca.brocku.dotscanvas.app.views;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.util.AttributeSet;
import android.view.MotionEvent;

import ca.brocku.dotscanvas.app.R;

/**
 * Author: Chris Kellendonk
 * Student #: 4810800
 * Date: 2014-06-15
 */
public class SVGImageButton extends SVGImageView {
    private String normalAssetName, touchAssetName;

    /**
     * @param context
     */
    public SVGImageButton(Context context) {
        super(context);
    }

    /**
     * @param context
     * @param attrs
     */
    public SVGImageButton(Context context, AttributeSet attrs)
    {
        super(context, attrs);


    }

    @Override
    protected void initWithSvg(Context context, AttributeSet attrs) {
        if(isInEditMode()) {
            setBackgroundColor(Color.RED);
            return; // Do Nothing
        }

        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.SVGImageButton);

        normalAssetName = a.getString(R.styleable.SVGImageButton_normal_asset_filename);
        touchAssetName = a.getString(R.styleable.SVGImageButton_touch_asset_filename);

        setSVGImage(normalAssetName);
    }

    /**
     * Implement this method to handle touch screen motion events.
     * <p/>
     * If this method is used to detect click actions, it is recommended that
     * the actions be performed by implementing and calling
     * {@link #performClick()}. This will ensure consistent system behavior,
     * including:
     * <ul>
     * <li>obeying click sound preferences
     * <li>dispatching OnClickListener calls
     * accessibility features are enabled
     * </ul>
     *
     * @param event The motion event.
     * @return True if the event was handled, false otherwise.
     */
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if(event.getAction() == MotionEvent.ACTION_DOWN) {
            setSVGImage(touchAssetName);
        } else if(event.getAction() == MotionEvent.ACTION_UP) {
            setSVGImage(normalAssetName);
        }

        super.onTouchEvent(event);

        return true;
    }
}
