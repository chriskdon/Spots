package ca.brocku.dotscanvas.app;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;

import ca.brocku.dotscanvas.app.core.Callback;
import ca.brocku.dotscanvas.app.core.GameOverListener;
import ca.brocku.dotscanvas.app.views.PauseDialog;

public class MainActivity extends ActionBarActivity implements GameOverListener {
  private GameSurfaceView mGameSurfaceView;
  private TextView mScoreTextView, mMissedTextView;
  private ImageButton mPauseButton;
  private PauseDialog dialog;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    Log.e("MainActivity", "#onCreate()");

    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main);

    mGameSurfaceView = (GameSurfaceView) findViewById(R.id.game_surfaceView);
    mScoreTextView = (TextView) findViewById(R.id.score_textView);
    mMissedTextView = (TextView) findViewById(R.id.missed_textView);
    mPauseButton = (ImageButton) findViewById(R.id.btn_Pause);

    mGameSurfaceView.setScoreView(mScoreTextView);
    mGameSurfaceView.setMissedView(mMissedTextView);

    dialog = new PauseDialog(MainActivity.this);

    mPauseButton.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View view) {
        showDialogAndPauseGame();
      }
    });

    dialog.setOnResumeClickHandler(new Callback() {
      @Override
      public void call() {
        hideDialogAndResumeGame();
      }
    });

    dialog.setOnRestartClickHandler(new Callback() {
      @Override
      public void call() {
        // TODO: Restart
      }
    });

    dialog.setOnQuitClickHandler(new Callback() {
      @Override
      public void call() {
        // TODO: Go home
      }
    });

    dialog.setOnHighscoresClickHandler(new Callback() {
      @Override
      public void call() {
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
    if (mGameSurfaceView.isGamePaused()) {
      hideDialogAndResumeGame();
    } else {
      showDialogAndPauseGame();
    }
  }

  @Override
  public void onGameOver(int score) {
    //TODO: Show Game Over menu
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
}

