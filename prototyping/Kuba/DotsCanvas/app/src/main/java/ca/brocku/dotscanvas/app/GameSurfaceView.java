package ca.brocku.dotscanvas.app;

import android.content.Context;
import android.graphics.PixelFormat;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.TextView;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.ObjectInputStream;

import ca.brocku.dotscanvas.app.engine.GameThread;
import ca.brocku.dotscanvas.app.engine.Handlers.MissedViewHandler;
import ca.brocku.dotscanvas.app.engine.Handlers.ScoreViewHandler;


public class GameSurfaceView extends SurfaceView implements SurfaceHolder.Callback, View.OnTouchListener {
  //Strings used for storing the game state
  public static final String GAME_STATE_FILENAME = "game-state.ser";
  public static String gameStateFilepath;

  private GameThread thread; //Handles drawing; initialized in surfaceCreated() callback
  private Context mContext;
  private TextView mScoreView;
  private TextView mMissedView;

  public GameSurfaceView(Context context, AttributeSet attrs) {
    super(context, attrs);

    if (!isInEditMode()) {
      //Register this SurfaceHolder to listen for changes to the Surface
      SurfaceHolder surfaceHolder = getHolder();
      surfaceHolder.addCallback(this);

      //Register this to listen for click events
      this.setOnTouchListener(this);

      mContext = context;

      gameStateFilepath = mContext.getFilesDir().getPath() + GAME_STATE_FILENAME;

      //Clear any saved game state
      clearSavedState();
    }
  }

  /**
   * Callback for the SurfaceHolder once the surface is created.
   * <p/>
   * Create a new thread here for every new surface to tie the thread's lifecycle to that of the
   * surface.
   * <p/>
   * Start the thread here so we don't busy-wait in run() waiting for the surface to be created
   *
   * @param surfaceHolder the SurfaceHolder whose surface has been created.
   */
  @Override
  public void surfaceCreated(SurfaceHolder surfaceHolder) {
    Log.e("GameSurfaceView", "#surfaceCreated()");

    if (new File(gameStateFilepath).exists()) {
      restoreGameState();
      if (!((MainActivity) mContext).isDialogVisible()) {
        thread.onResume();
      }
    } else {
      initGameThread();
    }

    thread.start();
  }

  public void restoreGameState() {
    synchronized (getHolder()) {
      FileInputStream fileIn;
      try {
        fileIn = new FileInputStream(gameStateFilepath);
        ObjectInputStream in = new ObjectInputStream(fileIn);
        thread = (GameThread) in.readObject();
        in.close();
        fileIn.close();
      } catch (FileNotFoundException e) {
        e.printStackTrace();
      } catch (IOException e) {
        e.printStackTrace();
      } catch (ClassNotFoundException e) {
        e.printStackTrace();
      }

      if (thread != null) {
        thread.restoreState(getHolder(),
                            mContext,
                            new ScoreViewHandler(mScoreView),
                            new MissedViewHandler(mMissedView));
      }

      clearSavedState();
    }
  }

  /**
   * Callback for the SurfaceHolder when the surface changes.
   *
   * @param holder the SurfaceHolder whose surface has changed.
   * @param format the new PixelFormat of the surface.
   * @param width  the new width of the surface.
   * @param height the new height of the surface.
   */
  @Override
  public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
    Log.e("GameSurfaceView", "#surfaceChanged()");
    holder.setFormat(PixelFormat.RGBA_8888); //sets 32-bit color mode to match the views' colors
    thread.onSurfaceChange(holder);
  }

  /**
   * Callback for the SurfaceHolder once the surface is destroyed.
   * <p/>
   * Tell the thread to stop and wait for it to finish so that it does not touch the Surface after
   * we return and explode.
   *
   * @param surfaceHolder the SurfaceHolder whose surface has been destroyed.
   */
  @Override
  public void surfaceDestroyed(SurfaceHolder surfaceHolder) {
    Log.e("GameSurfaceView", "#surfaceDestroyed()");
    stopGameThread();
    thread.saveState();
  }

  @Override
  public boolean onTouch(View view, MotionEvent motionEvent) {
    return thread.onTouch(motionEvent);
  }

  public void onPauseGame() {
    if (thread != null) {
      thread.onPause();
    }
  }

  /**
   * Start the game thread if it has not been started yet.
   */
  public void onResumeGame() {
    if (thread != null) {
      thread.onResume();
    }
  }

  public void onQuitGame() {
    thread.requestGameQuit();
    stopGameThread();
  }

  public void onRestartGame() {
    thread.requestGameQuit();
    stopGameThread();
    mScoreView.setText("0");
    mMissedView.setText(String.valueOf(GameThread.DOTS_TO_MISS));
    initGameThread();
    thread.start();
  }

  public GameThread getGameThread() {
    return thread;
  }

  public boolean isGamePaused() {
    return thread.isGamePaused();
  }

  /**
   * Determines whether there is a game that is existing. An existing game is one that can be
   * continued. Specifically, a game is considered existing if there is a saved game-state file or
   * if a game thread is running.
   *
   * @return whether or not there is a game that may be continued
   */
  public boolean isExistingGame() {
    boolean threadIsRunning = thread != null && thread.isAlive();
    boolean savedFileExists = new File(gameStateFilepath).exists();

    return threadIsRunning || savedFileExists;
  }

  public void setScoreView(TextView scoreView) {
    this.mScoreView = scoreView;
  }

  public void setMissedView(TextView missedView) {
    this.mMissedView = missedView;
  }

  /**
   * Helper method to initialize a new game thread object. Does not start the game thread.
   */
  private void initGameThread() {
    thread = new GameThread(getHolder(),
                            mContext,
                            new ScoreViewHandler(mScoreView),
                            new MissedViewHandler(mMissedView));
  }

  /**
   * Kills the game thread. Blocks until the thread finishes.
   */
  private void stopGameThread() {
    boolean retry = true;
    thread.setRunning(false); //tell the thread to shutdown
    thread.onResume(); //in case the thread is waiting
    while (retry) {
      try {
        thread.join(); //block until it finishes
        retry = false;
      } catch (InterruptedException e) {
        e.printStackTrace();
      }
    }
  }

  /**
   * Tries to delete a game-state file.
   */
  private void clearSavedState() {
    new File(gameStateFilepath).delete();
  }
}