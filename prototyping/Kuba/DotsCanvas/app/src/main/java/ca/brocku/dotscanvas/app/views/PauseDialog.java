package ca.brocku.dotscanvas.app.views;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.view.Window;
import android.view.WindowManager;

import ca.brocku.dotscanvas.app.R;

/**
 * Author: Chris Kellendonk
 * Student #: 4810800
 * Date: 2014-08-26
 */
public class PauseDialog extends Dialog {
    /**
     * Create a Dialog window that uses the default dialog frame style.
     *
     * @param context The Context the Dialog is to run it.  In particular, it
     *                uses the window manager and theme in this context to
     *                present its UI.
     */
    public PauseDialog(Context context) {
        super(context);

        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.pause_menu);

        initViewLocation();
    }

    /**
     * Set the location of the dialog window.
     *
     * The resume button must line up with the pause button in the game activity.
     */
    private void initViewLocation() {
        WindowManager.LayoutParams params = getWindow().getAttributes();
        DisplayMetrics displayMetrics = getContext().getResources().getDisplayMetrics();

        params.x = 300/2;
        params.y = 300/2;
        params.width = displayMetrics.widthPixels - 300;
        params.height = displayMetrics.heightPixels - 300;
        params.gravity = Gravity.TOP | Gravity.LEFT;

        getWindow().setBackgroundDrawable(new ColorDrawable(android.R.color.transparent));
        getWindow().setWindowAnimations(R.style.DialogNoAnimation);
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
        getWindow().setAttributes(params);
    }
}

