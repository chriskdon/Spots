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

      //Clear any saved game state
      clearState();
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

    if (new File(mContext.getFilesDir().getPath().toString() + GameThread.GAME_STATE_FILENAME).exists()) {
      restoreGameState();
      if (!((MainActivity) mContext).isDialogVisible()) {
        thread.onResume();
      }
    } else {
      thread = new GameThread(surfaceHolder, mContext,
          new ScoreViewHandler(mScoreView), new MissedViewHandler(mMissedView));
    }

    thread.start();
  }

  public void restoreGameState() {
    synchronized (getHolder()) {
      FileInputStream fileIn = null;
      try {
        fileIn = new FileInputStream(mContext.getFilesDir().getPath().toString() + GameThread.GAME_STATE_FILENAME);
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
        thread.restoreState(mContext,
            getHolder(),
            new ScoreViewHandler(mScoreView),
            new MissedViewHandler(mMissedView));
      }


      clearState();
    }
  }

  public void clearState() {
    //Clear the loaded state
    new File(mContext.getFilesDir().getPath().toString() + GameThread.GAME_STATE_FILENAME).delete();
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
    thread.onSurfaceChange(holder, width, height);
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

  public GameThread getGameThread() {
    return thread;
  }

  public boolean isGamePaused() {
    return thread.isGamePaused();
  }

  public void setScoreView(TextView scoreView) {
    this.mScoreView = scoreView;
  }

  public void setMissedView(TextView missedView) {
    this.mMissedView = missedView;
  }
}