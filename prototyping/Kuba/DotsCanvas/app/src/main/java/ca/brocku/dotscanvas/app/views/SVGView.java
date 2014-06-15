package ca.brocku.dotscanvas.app.views;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.PictureDrawable;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;

import com.caverock.androidsvg.SVG;

import java.lang.reflect.Method;

import ca.brocku.dotscanvas.app.R;

/**
 * Author: Chris Kellendonk
 * Student #: 4810800
 * Date: 2014-06-15
 */
public class SVGView extends ImageView {
    private static Method setLayerTypeMethod = null;

    {
        try
        {
            setLayerTypeMethod = View.class.getMethod("setLayerType", Integer.TYPE, Paint.class);
        }
        catch (NoSuchMethodException e) { /* do nothing */ }
    }

    /**
     * @param context
     */
    public SVGView(Context context) {
        super(context);
    }

    /**
     * @param context
     * @param attrs
     */
    public SVGView(Context context, AttributeSet attrs)
    {
        super(context, attrs);
        initWithSvg(context, attrs);
    }

    private void initWithSvg(Context context, AttributeSet attrs) {
        if(isInEditMode()) {
            setBackgroundColor(Color.RED);
            return; // Do Nothing
        }

        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.SVGView);

        String assetFileName = a.getString(R.styleable.SVGView_asset_filename);

        try {
            SVG svg = SVG.getFromAsset(context.getAssets(), assetFileName);
            Drawable drawable = new PictureDrawable(svg.renderToPicture());

            setSoftwareLayerType();
            setImageDrawable(drawable);
        } catch(Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    /**
     * Try to make it a software layer type
     */
    private void  setSoftwareLayerType()
    {
        if (setLayerTypeMethod == null)
            return;

        try
        {
            int  LAYER_TYPE_SOFTWARE = View.class.getField("LAYER_TYPE_SOFTWARE").getInt(new View(getContext()));
            setLayerTypeMethod.invoke(this, LAYER_TYPE_SOFTWARE, null);
        }
        catch (Exception e)
        {
            throw new RuntimeException(e);
        }
    }


}
