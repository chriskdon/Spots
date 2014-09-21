package ca.brocku.dotscanvas.app;

import android.app.ActionBar;
import android.app.Dialog;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import java.util.concurrent.Callable;

import ca.brocku.dotscanvas.app.core.Callback;
import ca.brocku.dotscanvas.app.views.PauseDialog;

public class MainActivity extends ActionBarActivity {
    private GameSurfaceView mGameSurfaceView;
    private TextView mScoreTextView, mMissedTextView;
    private ImageButton mPauseButton;
    private PauseDialog dialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.e("MainActivity", "onCreate");

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mGameSurfaceView = (GameSurfaceView) findViewById(R.id.game_surfaceView);
        mScoreTextView = (TextView) findViewById(R.id.score_textView);
        mMissedTextView = (TextView) findViewById(R.id.missed_textView);
        mPauseButton = (ImageButton)findViewById(R.id.btn_Pause);

        mGameSurfaceView.setScoreView(mScoreTextView);
        mGameSurfaceView.setMissedView(mMissedTextView);

        dialog = new PauseDialog(MainActivity.this);

        dialog.setOnResumeClickHandler(new Callback() {
            @Override
            public void call() {
                dialog.hide();
                onResume();
            }
        });

        mPauseButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mGameSurfaceView.onPauseGame();
                dialog.show();
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
        Log.e("MainActivity", "onPause");
        super.onPause();

        mGameSurfaceView.onPauseGame(); // pause game when Activity pauses
        //TODO display pause menu
    }

    @Override
    protected void onResume() {
        Log.e("MainActivity", "onResume");
        super.onResume();
        //TODO remove the following onResumeGame() as pause menu will be present
        mGameSurfaceView.onResumeGame(); //resume game when Activity resumes
    }

    @Override
    public void onBackPressed() {
        if(mGameSurfaceView.isGamePaused()) {
            //TODO hide pause menu
            Toast.makeText(MainActivity.this, "Resumed", Toast.LENGTH_SHORT).show();
            mGameSurfaceView.onResumeGame();
        } else {
            //TODO display pause menu
            mGameSurfaceView.onPauseGame();
            Toast.makeText(MainActivity.this, "Paused", Toast.LENGTH_SHORT).show();
        }
    }
}

