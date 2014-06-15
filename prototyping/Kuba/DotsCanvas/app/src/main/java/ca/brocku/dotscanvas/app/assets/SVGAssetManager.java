package ca.brocku.dotscanvas.app.assets;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.PictureDrawable;

import com.caverock.androidsvg.SVG;

import java.util.HashMap;
import java.util.Map;

/**
 * Author: Chris Kellendonk
 * Student #: 4810800
 * Date: 2014-06-15
 */
public class SVGAssetManager {
    private static Map<String, Drawable> SVG_DRAWABLES = new HashMap<String, Drawable>();

    public static Drawable Load(Context context, String assetFileName) {
        if (SVG_DRAWABLES.containsKey(assetFileName)) {
            return SVG_DRAWABLES.get(assetFileName);
        }

        try {
            SVG svg = SVG.getFromAsset(context.getAssets(), assetFileName);
            Drawable drawable = new PictureDrawable(svg.renderToPicture());

            SVG_DRAWABLES.put(assetFileName, drawable);

            return drawable;
        } catch (Exception ex) {
            throw new RuntimeException(ex); // TODO: handle?
        }
    }
}
