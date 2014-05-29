package ca.brocku.dotscanvas.app;

import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;

public class MainActivity extends ActionBarActivity {
    private GameSurfaceView mGameSurfaceView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.e("MainActivity", "onCreate");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mGameSurfaceView = (GameSurfaceView) findViewById(R.id.game_surfaceView);

        if (savedInstanceState == null) {
            // we were just launched: set up a new game
            //mLunarThread.setState(LunarThread.STATE_READY);
            Log.w(this.getClass().getName(), "SIS is null");
        } else {
            // we are being restored: resume a previous game
            //mLunarThread.restoreState(savedInstanceState);
            Log.w(this.getClass().getName(), "SIS is non-null");
        }

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
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

    /**
     * Notification that something is about to happen, to give the Activity a
     * chance to save state.
     *
     * @param outState a Bundle into which this Activity should save its state
     */
//    @Override
//    protected void onSaveInstanceState(Bundle outState) {
//        // just have the View's thread save its state into our Bundle
//        super.onSaveInstanceState(outState);
//        //gameThread.saveState(outState);
//        Log.w(this.getClass().getName(), "SIS called");
//    }

}
