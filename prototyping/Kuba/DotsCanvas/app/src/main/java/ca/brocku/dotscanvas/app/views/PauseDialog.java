package ca.brocku.dotscanvas.app.views;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;

import ca.brocku.dotscanvas.app.R;
import ca.brocku.dotscanvas.app.core.Callback;

/**
 * Author: Chris Kellendonk
 * Student #: 4810800
 * Date: 2014-08-26
 */
public class PauseDialog extends Dialog {
  private Callback onResumeClickHandler, onQuitClickHandler, onRestartClickHandler;
  private Context mContext;

  /**
   * Create a Dialog window that uses the default dialog frame style.
   *
   * @param context The Context the Dialog is to run it.  In particular, it
   *                uses the window manager and theme in this context to
   *                present its UI.
   */
  public PauseDialog(Context context) {
    super(context, android.R.style.Theme_Black_NoTitleBar_Fullscreen);

    mContext = context;

    initViewLocation();

    View view = LayoutInflater.from(context).inflate(R.layout.pause_menu, null);
    setContentView(view);

    view.findViewById(R.id.btn_play).setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View v) {
        try {
          onResumeClickHandler.call();
        } catch (Exception ex) {
          throw new RuntimeException(ex);
        }
      }
    });
  }

  public void setOnResumeClickHandler(Callback onResumeClickHandler) {
    this.onResumeClickHandler = onResumeClickHandler;
  }

  /**
   * Set the location of the dialog window.
   * <p/>
   * The resume button must line up with the pause button in the game activity.
   */
  private void initViewLocation() {
    getWindow().setBackgroundDrawableResource(android.R.color.transparent);
    getWindow().setWindowAnimations(R.style.DialogNoAnimation);
    getWindow().setLayout(WindowManager.LayoutParams.FILL_PARENT, WindowManager.LayoutParams.FILL_PARENT);
    getWindow().clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
  }

  /**
   * Make the activity that opened this dialog handle the back press.
   */
  @Override
  public void onBackPressed() {
    ((Activity) mContext).onBackPressed();
  }
}

