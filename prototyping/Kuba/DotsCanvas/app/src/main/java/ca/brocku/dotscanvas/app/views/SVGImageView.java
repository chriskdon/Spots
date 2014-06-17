package ca.brocku.dotscanvas.app.views;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;
import android.widget.ImageView;

import java.lang.reflect.Method;

import ca.brocku.dotscanvas.app.R;
import ca.brocku.dotscanvas.app.assets.SVGAssetManager;

/**
 * Author: Chris Kellendonk
 * Student #: 4810800
 * Date: 2014-06-15
 */
public class SVGImageView extends ImageView {
    private static Method setLayerTypeMethod = null;

    static {
        try {
            setLayerTypeMethod = View.class.getMethod("setLayerType", Integer.TYPE, Paint.class);
        } catch (NoSuchMethodException e) { /* do nothing */ }
    }

    /**
     * @param context
     */
    public SVGImageView(Context context) {
        super(context);
    }

    /**
     * @param context
     * @param attrs
     */
    public SVGImageView(Context context, AttributeSet attrs) {
        super(context, attrs);
        initWithSvg(context, attrs);
    }

    {
        setSoftwareLayerType();
    }

    protected void setSVGImage(String assetFileName) {
        setImageDrawable(SVGAssetManager.Load(getContext(), assetFileName));
    }

    protected void initWithSvg(Context context, AttributeSet attrs) {
        if (isInEditMode()) {
            setBackgroundColor(Color.RED);
            return; // Do Nothing
        }

        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.SVGImageView);

        String assetFileName = a.getString(R.styleable.SVGImageView_asset_filename);

        setSVGImage(assetFileName);
    }

    /**
     * Try to make it a software layer type
     */
    private final void setSoftwareLayerType() {
        if (setLayerTypeMethod == null) {
            return;
        }

        try {
            int LAYER_TYPE_SOFTWARE = View.class.getField("LAYER_TYPE_SOFTWARE").getInt(new View(getContext()));
            setLayerTypeMethod.invoke(this, LAYER_TYPE_SOFTWARE, null);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
