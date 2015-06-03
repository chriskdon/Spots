package dev.spots.app.views;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;

import dev.spots.app.R;
import dev.spots.app.core.Callback;

/**
 * Author: Chris Kellendonk
 * Student #: 4810800
 * Date: 2014-08-26
 */
public class PauseDialog extends Dialog {
  private Callback onResumeClickHandler, onQuitClickHandler,
      onRestartClickHandler, onHighscoresClickHandler;

  private Context mContext;
  private TextView lblFinalScore, lblGameOverTitle;
  private ImageButton btnResume;

  /**
   * Create a Dialog window that uses the default dialog frame style.
   *
   * @param context The Context the Dialog is to run it.  In particular, it
   *                uses the window manager and theme in this context to
   *                present its UI.
   */
  public PauseDialog(Context context) {
    super(context, R.style.PauseScreen);

    mContext = context;

    View view = LayoutInflater.from(context).inflate(R.layout.pause_menu, null);
    setContentView(view);

    lblFinalScore = (TextView) view.findViewById(R.id.lblFinalScore);
    lblGameOverTitle = (TextView) view.findViewById(R.id.lblGameOverTitle);
    btnResume = (ImageButton) view.findViewById(R.id.btn_play);

    setEndGameMode(false);

    btnResume.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View v) {
        if (onResumeClickHandler != null) {
          onResumeClickHandler.call();
        }
      }
    });

    view.findViewById(R.id.button_pause_menu_restart).setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View v) {
        if (onRestartClickHandler != null) {
          onRestartClickHandler.call();
        }
      }
    });

    view.findViewById(R.id.button_pause_menu_home).setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View v) {
        if (onQuitClickHandler != null) {
          onQuitClickHandler.call();
        }
      }
    });

    view.findViewById(R.id.button_pause_menu_highscores).setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View v) {
        if (onHighscoresClickHandler != null) {
          onHighscoresClickHandler.call();
        }
      }
    });
  }

  public void setScore(int score) {
    lblFinalScore.setText(Integer.toString(score));
  }

  public void setEndGameMode(boolean isEndGame) {
    int endGameControlsState = isEndGame ? View.VISIBLE : View.INVISIBLE;
    int resumeButtonState = isEndGame ? View.INVISIBLE : View.VISIBLE;

    lblFinalScore.setVisibility(endGameControlsState);
    lblGameOverTitle.setVisibility(endGameControlsState);
    btnResume.setVisibility(resumeButtonState);
  }

  public void setOnResumeClickHandler(Callback onResumeClickHandler) {
    this.onResumeClickHandler = onResumeClickHandler;
  }

  public void setOnRestartClickHandler(Callback onRestartClickHandler) {
    this.onRestartClickHandler = onRestartClickHandler;
  }

  public void setOnQuitClickHandler(Callback onQuitClickHandler) {
    this.onQuitClickHandler = onQuitClickHandler;
  }

  public void setOnHighscoresClickHandler(Callback onHighscoresClickHandler) {
    this.onHighscoresClickHandler = onHighscoresClickHandler;
  }

  /**
   * Make the activity that opened this dialog handle the back press.
   */
  @Override
  public void onBackPressed() {
    ((Activity) mContext).onBackPressed();
  }
}
