package ca.brocku.dotscanvas.app;

import android.graphics.drawable.Drawable;
import android.graphics.drawable.PictureDrawable;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.caverock.androidsvg.SVG;
import com.caverock.androidsvg.SVGParseException;

import java.io.FileInputStream;

import ca.brocku.dotscanvas.app.views.SVGImageButton;

public class MainActivity extends ActionBarActivity {
    private GameSurfaceView mGameSurfaceView;
    private TextView mScoreTextView, mMissedTextView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.e("MainActivity", "onCreate");

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mGameSurfaceView = (GameSurfaceView) findViewById(R.id.game_surfaceView);
        mScoreTextView = (TextView) findViewById(R.id.score_textView);
        mMissedTextView = (TextView) findViewById(R.id.missed_textView);

        mGameSurfaceView.setScoreView(mScoreTextView);
        mGameSurfaceView.setMissedView(mMissedTextView);

        SVGImageButton button = (SVGImageButton)findViewById(R.id.btn_Pause);
        button.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                Toast.makeText(MainActivity.this, "Pause", Toast.LENGTH_SHORT).show();

                return false;
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
    }

    @Override
    protected void onResume() {
        Log.e("MainActivity", "onResume");
        super.onResume();
        mGameSurfaceView.onResumeGame(); //resume game when Activity resumes
    }

    @Override
    public void onBackPressed() {
        //TODO: display pause menu here...call mGameSurfaceView.onPauseGame() or possibly this.onPause() to wait thread

        //super.onBackPressed();
    }
}
