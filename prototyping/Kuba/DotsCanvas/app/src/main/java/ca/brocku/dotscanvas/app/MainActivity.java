package ca.brocku.dotscanvas.app;

import android.content.Intent;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.io.File;

import ca.brocku.dotscanvas.app.core.Callback;
import ca.brocku.dotscanvas.app.core.GameOverListener;
import ca.brocku.dotscanvas.app.models.HighscoreManager;
import ca.brocku.dotscanvas.app.views.PauseDialog;

public class MainActivity extends ActionBarActivity implements GameOverListener {
  private GameSurfaceView mGameSurfaceView;
  private TextView mScoreTextView, mMissedTextView;
  private ImageButton mPauseButton;
  private LinearLayout scnLives;
  private PauseDialog dialog;
  private MediaPlayer mButtonSoundPlayer;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    Log.e("MainActivity", "#onCreate()");

    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main);

    mGameSurfaceView = (GameSurfaceView) findViewById(R.id.game_surfaceView);
    mScoreTextView = (TextView) findViewById(R.id.score_textView);
    mMissedTextView = (TextView) findViewById(R.id.missed_textView);
    mPauseButton = (ImageButton) findViewById(R.id.btn_Pause);
    scnLives = (LinearLayout) findViewById(R.id.scnLives);

    mGameSurfaceView.setScoreView(mScoreTextView);
    mGameSurfaceView.setMissedView(mMissedTextView);

    mButtonSoundPlayer = MediaPlayer.create(this, R.raw.button_press);
    mButtonSoundPlayer.setVolume(.15f, .15f);

    dialog = new PauseDialog(MainActivity.this);

    mPauseButton.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View view) {
        mButtonSoundPlayer.start();
        showDialogAndPauseGame();
      }
    });

    dialog.setOnResumeClickHandler(new Callback() {
      @Override
      public void call() {
        mButtonSoundPlayer.start();
        hideDialogAndResumeGame();
      }
    });

    dialog.setOnRestartClickHandler(new Callback() {
      @Override
      public void call() {
        mButtonSoundPlayer.start();
        hideDialogAndRestartGame();
      }
    });

    dialog.setOnQuitClickHandler(new Callback() {
      @Override
      public void call() {
        mButtonSoundPlayer.start();
        mGameSurfaceView.onQuitGame();
        finish();
      }
    });

    dialog.setOnHighscoresClickHandler(new Callback() {
      @Override
      public void call() {
        mButtonSoundPlayer.start();
        MainActivity.this.startActivity(new Intent(MainActivity.this, HighscoresActivity.class));
      }
    });
  }

  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
    getMenuInflater().inflate(R.menu.main, menu);
    return true;
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    int id = item.getItemId();
    if (id == R.id.action_settings) {
      return true;
    }
    return super.onOptionsItemSelected(item);
  }

  @Override
  protected void onPause() {
    Log.e("MainActivity", "#onPause()");
    super.onPause();
    showDialogAndPauseGame();
  }

  @Override
  protected void onResume() {
    Log.e("MainActivity", "#onResume()");
    super.onResume();
  }

  @Override
  public void onBackPressed() {
    if (mGameSurfaceView.isGameOver()) return;

    if (mGameSurfaceView.isGamePaused()) {
      hideDialogAndResumeGame();
    } else {
      showDialogAndPauseGame();
    }
  }

  /**
   * Opens the pause menu in case there is an existing game that can be continued.
   */
  @Override
  protected void onStart() {
    Log.e("MainActivity", "#onStart()");
    super.onStart();
    if (mGameSurfaceView.isExistingGame()) {
      dialog.show();
    }
  }

  @Override
  protected void onStop() {
    Log.e("MainActivity", "#onStop()");
    dialog.dismiss();
    super.onStop();
  }

  @Override
  public void onGameOver(final int score) {
    HighscoreManager manager = new HighscoreManager(this);
    manager.updateScore(score);

    runOnUiThread(new Runnable() {
      @Override
      public void run() {
        dialog.setScore(score);
        dialog.setEndGameMode(true);
        mPauseButton.setVisibility(View.INVISIBLE);
        hideScoreAndLives();
        dialog.show();
      }
    });
  }

  private void hideScoreAndLives() {
    mScoreTextView.setVisibility(View.INVISIBLE);
    scnLives.setVisibility(View.INVISIBLE);
  }

  private void showScoreAndLives() {
    mScoreTextView.setVisibility(View.VISIBLE);
    scnLives.setVisibility(View.VISIBLE);
  }

  public boolean isDialogVisible() {
    return dialog.isShowing();
  }

  private void hideDialogAndResumeGame() {
    mPauseButton.setVisibility(View.VISIBLE);
    dialog.hide();
    mGameSurfaceView.onResumeGame();
  }

  private void showDialogAndPauseGame() {
    mPauseButton.setVisibility(View.INVISIBLE);
    dialog.show();
    mGameSurfaceView.onPauseGame();
  }

  private void hideDialogAndRestartGame() {
    showScoreAndLives();
    mPauseButton.setVisibility(View.VISIBLE);
    dialog.setEndGameMode(false);
    dialog.hide();
    mGameSurfaceView.onRestartGame();
  }
}

