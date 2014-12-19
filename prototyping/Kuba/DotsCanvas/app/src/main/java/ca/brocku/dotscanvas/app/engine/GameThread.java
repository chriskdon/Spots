package ca.brocku.dotscanvas.app.engine;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RadialGradient;
import android.graphics.Shader;
import android.media.AudioManager;
import android.media.SoundPool;
import android.util.Log;
import android.view.MotionEvent;
import android.view.SurfaceHolder;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.Iterator;
import java.util.Stack;
import java.util.concurrent.atomic.AtomicBoolean;

import ca.brocku.dotscanvas.app.R;
import ca.brocku.dotscanvas.app.engine.Handlers.MissedViewHandler;
import ca.brocku.dotscanvas.app.engine.Handlers.ScoreViewHandler;
import ca.brocku.dotscanvas.app.gameboard.Dot;
import ca.brocku.dotscanvas.app.gameboard.DotGrid;
import ca.brocku.dotscanvas.app.gameboard.DotState;

/**
 * This is the Thread which draws to the Canvas.
 */
public class GameThread extends Thread implements Serializable {
  //Strings used for storing the game state
  public static final String GAME_STATE_FILENAME = "game-state.ser";

  private static final Object mutex = new Object();

  private static final int GRID_LENGTH = 6;
  private static final int NUMBER_OF_DOTS = 36;
  private static final int DOTS_TO_MISS = 15;

  private static final long DURATION_VISIBILITY_ANIMATION = 100; //time to appear/disappear
  private static final long DURATION_VISIBLE = 2000; //time for which dot stays visible

  // Probability-calculation constants for dots to appear
  private static final long SEED_PROB_TIME_LIMIT = 10000;
  private static final long ADJACENT_PROB_CLUSTER_LIMIT = 9;
  private static final double SEED_PROB_WEIGHT = 0.5;
  private static final double ADJACENT_PROB_WEIGHT = 1 - SEED_PROB_WEIGHT;

  private transient SurfaceHolder mSurfaceHolder;
  private transient Context mContext;
  private transient ScoreViewHandler mScoreViewHandler;
  private transient MissedViewHandler mMissedViewHandler;

  private int mCanvasHeight = 1;
  private int mCanvasWidth = 1;
  private float mCanvasLength = (mCanvasHeight > mCanvasWidth ? mCanvasWidth : mCanvasHeight); //the smaller of the height and width
  private float mPixelsPerDotRegion = 1;
  private float mDotRadius = 1;
  private float mMaxLineLength = 1;

  private AtomicBoolean mRun;  //whether the surface has been created & is ready to draw
  private AtomicBoolean mBlock; //whether the surface has lost focus

  private AtomicBoolean mGameOver; //has the game completed
  private AtomicBoolean mQuitRequested; //is the user quitting the game

  private DotGrid mDotGrid;
  private Stack<Dot> mDotChain;
  private boolean mInteracting; //interaction = actions from touch down to touch up
  private float mChainingLineX;
  private float mChainingLineY;

  private int mScore;
  private int mMissedDots;

  private long mTimePausedLast;

  private long mLastSecond; //the last second that calculations for dots to appear were done

  private transient SoundPool mSoundPool;
  private transient int mMissedSoundId;
  private transient int mReleaseSoundId;
  private transient int[] mSelectSoundIds;

  // Game Loop
  private final static int MAX_FPS = 30;                  // Desired fps
  private final static int MAX_FRAME_SKIPS = 5;           // Maximum number of frames to be skipped
  private final static int FRAME_PERIOD = 1000 / MAX_FPS; // The frame period

  public GameThread(SurfaceHolder surfaceHolder, Context context,
                    ScoreViewHandler scoreViewHandler, MissedViewHandler missedViewHandler) {
    mSurfaceHolder = surfaceHolder;
    mContext = context;
    mScoreViewHandler = scoreViewHandler;
    mMissedViewHandler = missedViewHandler;

    mRun = new AtomicBoolean(false);
    mBlock = new AtomicBoolean(false);

    mGameOver = new AtomicBoolean(false);
    mQuitRequested = new AtomicBoolean(false);

    mDotGrid = new DotGrid(GRID_LENGTH);

    mDotChain = new Stack<Dot>();
    mInteracting = false;
    mChainingLineX = 0;
    mChainingLineY = 0;

    mScore = 0;
    mMissedDots = 0;

    mTimePausedLast = System.currentTimeMillis();

    mLastSecond = System.currentTimeMillis() / 1000;

    initializeSoundPool();

    //TODO extract to initialize board method
//        for(int i=0; i<4; i++) {
//            int location = (int) (Math.random()*36);
//            mDotGrid.dotAt(location).setState(DotState.APPEARING);
//        }
  }

  @Override
  public void run() {
    long beginTime;        // the time when the cycle begun
    long timeDiff;        // the time it took for the cycle to execute
    int sleepTime;        // ms to sleep (<0 if we're behind)
    int framesSkipped;    // number of frames being skipped

    while (mRun.get()) {

      Canvas c = null;
      try {
        c = mSurfaceHolder.lockCanvas();
        synchronized (mutex) {
          beginTime = System.currentTimeMillis();
          framesSkipped = 0;    // resetting the frames skipped

          updateState();

          if (c != null) {
            doDraw(c);
          }

          timeDiff = System.currentTimeMillis() - beginTime;
          // calculate sleep time
          sleepTime = (int) (FRAME_PERIOD - timeDiff);

          if (sleepTime > 0) {
            // if sleepTime > 0 we're OK
            try {
              // send the thread to sleep for a short period
              // very useful for battery saving
              Thread.sleep(sleepTime);
            } catch (InterruptedException e) {
            }
          }

          while (sleepTime < 0 && framesSkipped < MAX_FRAME_SKIPS) {
            // we need to catch up, update without rendering
            updateState();

            // add frame period to check if in next frame
            sleepTime += FRAME_PERIOD;
            framesSkipped++;
          }
        }

      } catch (Exception e) {
        e.printStackTrace();
      } finally {
        // Prevents the surface from being left in an inconsistent state in case of an
        // exception
        if (c != null) {
          mSurfaceHolder.unlockCanvasAndPost(c);
        }
      }

      //Wait this thread when the Activity onPauses
      synchronized (mutex) {
        while (mBlock.get()) {
          try {
            mutex.wait();
          } catch (InterruptedException e) {
          }

        }
      }
    }
  }

  /**
   * Whether or not this thread should finish. It is set based on the state of the Surface.
   * surfaceCreated --> run thread
   * surfaceDestroyed --> stop thread
   *
   * @param b whether the thread should continue running
   */
  public void setRunning(boolean b) {
    mRun.getAndSet(b);
  }

  /**
   * Starts the new Thread of execution. The <code>run()</code> method of
   * the receiver will be called by the receiver Thread itself (and not the
   * Thread calling <code>start()</code>).
   *
   * @throws IllegalThreadStateException - if this thread has already started.
   * @see Thread#run
   */
  @Override
  public synchronized void start() {
    setRunning(true);
    super.start();
  }

  /**
   * Requests this thread to wait.
   */
  public void onPause() {
    mBlock.getAndSet(true);
    mTimePausedLast = System.currentTimeMillis();
  }

  /**
   * Requests this thread to continue.
   */
  public void onResume() {
    mBlock.getAndSet(false);
    synchronized (mutex) {
      mutex.notifyAll();
    }

    //Update each visible dot with the time we were paused for
    long timePaused = System.currentTimeMillis() - mTimePausedLast;
    for (Dot dot : mDotGrid) {
      if (dot.isVisible()) dot.increaseStateStartTime(timePaused);
    }
  }

  public void saveState() {
    Log.e("GameThread", "#saveState()");

    if (!mGameOver.get() && !mQuitRequested.get()) {
      mBlock.getAndSet(true);

      try {
        FileOutputStream fileOut = new FileOutputStream(mContext.getFilesDir().getPath().toString() + GAME_STATE_FILENAME);
        ObjectOutputStream out = new ObjectOutputStream(fileOut);
        out.writeObject(this);
        out.close();
        fileOut.close();
      } catch (FileNotFoundException e) {
        e.printStackTrace();
      } catch (IOException e) {
        e.printStackTrace();
      }
    }
  }

  public void restoreState(Context mContext, SurfaceHolder holder, ScoreViewHandler scoreViewHandler, MissedViewHandler missedViewHandler) {
    Log.e("GameThread", "#restoreState()");
    this.mContext = mContext;
    this.mSurfaceHolder = holder;
    this.mScoreViewHandler = scoreViewHandler;
    this.mMissedViewHandler = missedViewHandler;
    initializeSoundPool();
  }

  public boolean isGameOver() {
      return mGameOver.get();
  }

  public boolean isGamePaused() {
    return mBlock.get();
  }

  public void setQuitRequested(boolean b) {
    mQuitRequested.getAndSet(b);
  }

  /**
   * Called when the Surface size changes to store the updated canvas dimensions and related
   * measurements.
   *
   * @param width  the new width of the the new width of the surface.
   * @param height the new height of the surface.
   * @see ca.brocku.dotscanvas.app.GameSurfaceView#surfaceChanged
   */
  public void onSurfaceChange(SurfaceHolder surfaceHolder, int width, int height) {
    synchronized (mutex) {
      mSurfaceHolder = surfaceHolder;
      mCanvasHeight = height;
      mCanvasWidth = width;
      mCanvasLength = height < width ? height : width;

      mPixelsPerDotRegion = mCanvasLength / GRID_LENGTH;
      mDotRadius = mPixelsPerDotRegion * 2.0f / 3.0f / 2;
      mMaxLineLength = (float) (1.5 * mPixelsPerDotRegion);

      for (Dot dot : mDotGrid) {
        dot.setCenterX(
            (float) ((float) dot.getRow() * mPixelsPerDotRegion + mPixelsPerDotRegion / 2.0));
        dot.setCenterY(
            (float) ((float) dot.getCol() * mPixelsPerDotRegion + mPixelsPerDotRegion / 2.0));
      }
    }
  }

  public boolean onTouch(MotionEvent motionEvent) {
    if (!mBlock.get()) {
      float x = motionEvent.getX();
      float y = motionEvent.getY();

      switch (motionEvent.getAction()) {
        case MotionEvent.ACTION_DOWN:
          onTouchDown(x, y, motionEvent);
          break;
        case MotionEvent.ACTION_UP:
          onTouchUp(x, y, motionEvent);
          break;
        case MotionEvent.ACTION_MOVE:
          onTouchMove(x, y, motionEvent);
          break;
        case MotionEvent.ACTION_OUTSIDE:
          onTouchOutside(x, y, motionEvent);
          break;
      }
    }
    return true;
  }

  private void onTouchDown(float x, float y, MotionEvent motionEvent) {
    mChainingLineX = x;
    mChainingLineY = y;

    for (Dot dot : mDotGrid) {
      if (dot.isVisible() && isTouchWithinDot(x, y, dot, 1.4f)) {
        addDotToChain(dot);
        break;
      }
    }

    mInteracting = true;
  }

  private void onTouchUp(float x, float y, MotionEvent motionEvent) {
    updateScore();

    //Hide all of the dots in the dot chain
    for (Dot dot : mDotChain) {
      dot.setState(DotState.DISAPPEARING);
    }
    clearChain();
  }

  private void onTouchMove(float x, float y, MotionEvent motionEvent) {
    setInteractingCoordinates(x, y);

    if (!mDotChain.isEmpty()) {
      for (Dot dot : mDotGrid) {
        if (dot.isVisible() && isTouchWithinDot(x, y, dot, 1)) {
          if (!mDotChain.contains(dot) && isDotAdjacent(dot)) {
            addDotToChain(dot);
          }
          break;
        }
      }
    }
  }

  private void onTouchOutside(float x, float y, MotionEvent motionEvent) {
    clearChain();
  }


  private void setInteractingCoordinates(float endX, float endY) {
    if (!mDotChain.isEmpty()) {
      Dot lastDot = mDotChain.peek();
      float startX = lastDot.getCenterX();
      float startY = lastDot.getCenterY();

      //Lengths from the start to the end coordinates
      float diffX = Math.abs(startX - endX);
      float diffY = Math.abs(startY - endY);

      if (diffX > mMaxLineLength || diffY > mMaxLineLength) { //if a length exceeds the max allowed

        //The factor represents how much larger the larger distance of the two is than the max length allowed
        float factor = (diffX >= diffY ? diffX / mMaxLineLength : diffY / mMaxLineLength);

        //Trim each axis' distance by the factor
        diffX /= factor;
        diffY /= factor;

        //Adjust ending coordinates
        if (startX - endX > 0) {
          mChainingLineX = startX - diffX;
        } else if (startX - endX < 0) {
          mChainingLineX = startX + diffX;
        }
        if (startY - endY > 0) {
          mChainingLineY = startY - diffY;
        } else if (startY - endY < 0) {
          mChainingLineY = startY + diffY;
        }

      } else { //line length within limit, set values
        mChainingLineX = endX;
        mChainingLineY = endY;
      }
    }
  }

  private boolean isTouchWithinDot(float x, float y, Dot dot, float radiusFactor) {
    float diffX = Math.abs(x - dot.getCenterX());
    float diffY = Math.abs(y - dot.getCenterY());

    return (diffX <= mDotRadius * radiusFactor && diffY <= mDotRadius * radiusFactor);
  }

  private boolean isDotAdjacent(Dot dot) {
    if (!mDotChain.isEmpty()) {
      Dot lastDot = mDotChain.peek();

      //Check if dot is adjacent to the last selected dot
      if (Math.abs(dot.getRow() - lastDot.getRow()) <= 1 && Math.abs(dot.getCol() - lastDot.getCol()) <= 1) {
        return true;
      }
    }

    return false;
  }

  private void addDotToChain(Dot dot) {
    mDotChain.push(dot);
    dot.setState(DotState.SELECTED);
    playSound(mSelectSoundIds[mDotChain.size()-1]);
  }

  private void clearChain() {
    if (!mDotChain.isEmpty()) {
      mDotChain.clear();
      playSound(mReleaseSoundId);
    }
    mInteracting = false;
  }

  private void updateScore() {
    if (!mDotChain.isEmpty()) {
      mScore += Math.pow(mDotChain.size(), 2);
    }

    mScoreViewHandler.updateScore(mScore);
  }

  private void updateMissedByOne() {
    mMissedDots++;

    mMissedViewHandler.updateMissedCount(DOTS_TO_MISS - mMissedDots);

    playSound(mMissedSoundId);
  }

  private void playSound(int soundId) {
    mSoundPool.play(soundId, 1, 1, 0, 0, 1);
  }

  private void updateState() {
    long currentSecond = System.currentTimeMillis() / 1000;

    for (Dot dot : mDotGrid) {
      if (mMissedDots >= DOTS_TO_MISS) {
        mRun.getAndSet(false);
        mGameOver.getAndSet(true);
        break;
      }
      long stateDuration = dot.getStateDuration();

      switch (dot.getState()) {
        case SELECTED:
          // Do nothing. Hold this state while the dot is selected.
          break;
        case VISIBLE:
          if (stateDuration > DURATION_VISIBLE) {
            dot.setState(DotState.DISAPPEARING);
            updateMissedByOne();
          }
          break;
        case DISAPPEARING:
          if (stateDuration > DURATION_VISIBILITY_ANIMATION) {
            dot.setState(DotState.INVISIBLE);
          }
          break;
        case INVISIBLE:
          if (currentSecond != mLastSecond) { //ensures probability is calculated once/sec
            handleInvisibleState(dot);
          }
          break;
        case APPEARING:
          if (stateDuration > DURATION_VISIBILITY_ANIMATION) {
            dot.setState(DotState.VISIBLE);
          }
          break;
      }
    }
    if (mGameOver.get()) {
      for (Dot dot : mDotGrid) dot.setState(DotState.INVISIBLE);
    }
    mLastSecond = currentSecond;
  }

  /**
   * Determines whether or not to make this dot begin appearing by running some probability
   * calculations.
   * <p/>
   * Note: Both of the probability calculations have a maximum probability of 10%. The need to be equal
   * for the way the current total probability calculation is set up. If they can produce
   * different probabilities, then they will need to be normalized.
   *
   * @param dot the invisible dot under consideration
   */
  private void handleInvisibleState(Dot dot) {
    double seedProb = calculateSeedProbability(dot);
    double adjacentProb = calculateAdjacentProbability(dot);

    double totalProb = seedProb * SEED_PROB_WEIGHT + adjacentProb * ADJACENT_PROB_WEIGHT;

    if (Math.random() < totalProb) {
      dot.setState(DotState.APPEARING);
    }
  }

  /**
   * Calculates the probability that a dot should appear based on how long it has been invisible
   * for. The probability the dot will appear increases as time invisible increases.
   *
   * @param dot the dot the probability is being calculated for
   * @return the probability that the dot should appear
   */
  private double calculateSeedProbability(Dot dot) {
    double probability = 0;
    long invisibleDuration = dot.getStateDuration();
    if (invisibleDuration > SEED_PROB_TIME_LIMIT) {
      probability = 0.1;
    } else {
      probability = 0.00001 * invisibleDuration;
    }
    return probability;
  }

  /**
   * Calculates the probability that a dot should appear based on how large of a dot-cluster it is
   * in. The probability the dot will appear increases as the number of dots in the cluster
   * increases.
   *
   * @param dot the dot the probability is being calculated for
   * @return the probability that the dot should appear
   */
  private double calculateAdjacentProbability(Dot dot) {
    double probability = 0;
    int clusterSize = mDotGrid.clusterSizeFor(dot);
    if (clusterSize > ADJACENT_PROB_CLUSTER_LIMIT || clusterSize < 1) {
      probability = 0.0;
    } else {
      probability = -0.002040816 * Math.pow(clusterSize - 1, 2) + 0.1;
    }
    return probability;
  }

  private void doDraw(Canvas canvas) {
    canvas.drawColor(mContext.getResources().getColor(R.color.background)); //clear the screen

    Paint paint = new Paint();

    paint.setAntiAlias(true);
    paint.setColor(Color.rgb(237, 17, 100));
    paint.setStrokeWidth(15);

    //Draw dots
    for (Dot dot : mDotGrid) {
      //Draw dot placeholders
      Paint invisibleDotPaint = new Paint(paint);
      invisibleDotPaint.setColor(Color.rgb(77, 77, 77));
      canvas.drawCircle(dot.getCenterX(), dot.getCenterY(), mDotRadius * 0.75f, invisibleDotPaint);

      //Paint for the Dots' shadows
      Paint shadowPaint = new Paint(paint);
      shadowPaint.setColor(Color.rgb(77, 77, 77));
      //shadowPaint.setAlpha(255/2);

      switch (dot.getState()) {
        case VISIBLE:
          shadowPaint.setShader(new RadialGradient(dot.getCenterX(), dot.getCenterY() + mDotRadius * 2f * 0.15f, mDotRadius, mContext.getResources().getColor(R.color.black), mContext.getResources().getColor(R.color.background), Shader.TileMode.MIRROR));
          canvas.drawCircle(dot.getCenterX(), dot.getCenterY() + mDotRadius * 2f * 0.15f, mDotRadius, shadowPaint);
          canvas.drawCircle(dot.getCenterX(), dot.getCenterY(), mDotRadius, paint);
          break;
        case SELECTED:
          canvas.drawCircle(dot.getCenterX(), dot.getCenterY(), mDotRadius * 1.15f, paint);
          break;
        case DISAPPEARING:
          //Determine how far into the animation we are
          float dFactor = 1 - ((float) dot.getStateDuration() / DURATION_VISIBILITY_ANIMATION);
          if (dFactor > 0) {
            //Set Radius
            float dRadius = mDotRadius * dFactor;
            //Set Paint
            Paint dPaint = new Paint(paint);
            dPaint.setAlpha((int) (255 * dFactor));

            shadowPaint.setShader(new RadialGradient(dot.getCenterX(), dot.getCenterY() + dRadius * 2f * 0.15f, dRadius, mContext.getResources().getColor(R.color.black), mContext.getResources().getColor(R.color.background), Shader.TileMode.MIRROR));
            canvas.drawCircle(dot.getCenterX(), dot.getCenterY() + dRadius * 2f * 0.15f, dRadius, shadowPaint);
            canvas.drawCircle(dot.getCenterX(), dot.getCenterY(), dRadius, dPaint);
          }
          break;
        case APPEARING:
          //Determine how far into the animation we are
          float aFactor = (float) dot.getStateDuration() / DURATION_VISIBILITY_ANIMATION;
          if (aFactor > 0) {
            //Set Radius
            float aRadius = mDotRadius * aFactor;
            //Set Paint
            Paint aPaint = new Paint();
            aPaint.set(paint);
            aPaint.setAlpha((int) (255 * aFactor));

            shadowPaint.setShader(new RadialGradient(dot.getCenterX(), dot.getCenterY() + aRadius * 2f * 0.15f, aRadius, mContext.getResources().getColor(R.color.black), mContext.getResources().getColor(R.color.background), Shader.TileMode.MIRROR));
            canvas.drawCircle(dot.getCenterX(), dot.getCenterY() + aRadius * 2f * 0.15f, aRadius, shadowPaint);
            canvas.drawCircle(dot.getCenterX(), dot.getCenterY(), aRadius, aPaint);
          }
          break;
      }
    }

    //Draw lines
    if (mInteracting && !mDotChain.isEmpty()) {
      //Draw lines between chained dots
      {
        Iterator<Dot> iterator = mDotChain.iterator();
        if (iterator.hasNext()) {
          Dot startDot = iterator.next();
          float startX = startDot.getCenterX();
          float startY = startDot.getCenterY();

          while (iterator.hasNext()) {
            Dot endDot = iterator.next();

            canvas.drawLine(startX, startY, endDot.getCenterX(), endDot.getCenterY(), paint);
            startX = endDot.getCenterX();
            startY = endDot.getCenterY();
          }
        }
      }

      //Draw unconnected line
      Dot lastDot = mDotChain.peek();
      float startX = lastDot.getCenterX();
      float startY = lastDot.getCenterY();
      canvas.drawLine(startX, startY, mChainingLineX, mChainingLineY, paint);
    }
  }

  private void initializeSoundPool() {
    mSoundPool = new SoundPool(4, AudioManager.STREAM_MUSIC, 0);
    mMissedSoundId = mSoundPool.load(this.mContext, R.raw.miss, 1);
    mReleaseSoundId = mSoundPool.load(this.mContext, R.raw.release, 2);

    mSelectSoundIds = new int[12];
    mSelectSoundIds[0] = mSoundPool.load(this.mContext, R.raw.select, 3);
    mSelectSoundIds[1] = mSoundPool.load(this.mContext, R.raw.select1u, 3);
    mSelectSoundIds[2] = mSoundPool.load(this.mContext, R.raw.select2u, 3);
    mSelectSoundIds[3] = mSoundPool.load(this.mContext, R.raw.select3u, 3);
    mSelectSoundIds[4] = mSoundPool.load(this.mContext, R.raw.select4u, 3);
    mSelectSoundIds[5] = mSoundPool.load(this.mContext, R.raw.select5u, 3);
    mSelectSoundIds[6] = mSoundPool.load(this.mContext, R.raw.select6u, 3);
    mSelectSoundIds[7] = mSoundPool.load(this.mContext, R.raw.select7u, 3);
    mSelectSoundIds[8] = mSoundPool.load(this.mContext, R.raw.select8u, 3);
    mSelectSoundIds[9] = mSoundPool.load(this.mContext, R.raw.select9u, 3);
    mSelectSoundIds[10] = mSoundPool.load(this.mContext, R.raw.select10u, 3);
    mSelectSoundIds[11] = mSoundPool.load(this.mContext, R.raw.select11u, 3);
  }
}
