package ca.brocku.dotscanvas.app.engine;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RadialGradient;
import android.graphics.Shader;
import android.util.Log;
import android.view.MotionEvent;
import android.view.SurfaceHolder;

import java.util.Iterator;
import java.util.Stack;

import ca.brocku.dotscanvas.app.R;
import ca.brocku.dotscanvas.app.engine.Handlers.MissedViewHandler;
import ca.brocku.dotscanvas.app.engine.Handlers.ScoreViewHandler;
import ca.brocku.dotscanvas.app.gameboard.Dot;
import ca.brocku.dotscanvas.app.gameboard.DotGrid;
import ca.brocku.dotscanvas.app.gameboard.DotState;

/**
 * This is the Thread which draws to the Canvas.
 */
public class GameThread extends Thread {
    //Strings used for storing the game state
    public static final String GAME_STATE_FILENAME = "GAME_STATE";

    private static final int GRID_LENGTH = 6;
    private static final int NUMBER_OF_DOTS = 36;
    private static final int DOTS_TO_MISS = 15;

    private static final long DURATION_VISIBILITY_ANIMATION = 100; //time to appear/disappear
    private static final long DURATION_VISIBLE = 2000; //time for which dot stays visible

    private SurfaceHolder mSurfaceHolder;
    private Context mContext;
    private ScoreViewHandler mScoreViewHandler;
    private MissedViewHandler mMissedViewHandler;

    private int mCanvasHeight = 1;
    private int mCanvasWidth = 1;
    private float mCanvasLength = (mCanvasHeight > mCanvasWidth ? mCanvasWidth : mCanvasHeight); //the smaller of the height and width
    private float mPixelsPerDotRegion = 1;
    private float mDotRadius = 1;
    private float mMaxLineLength = 1;

    private boolean mRun;  //whether the surface has been created & is ready to draw
    private boolean mBlock; //whether the surface has lost focus

    private boolean mGameOver; //has the game completed
    private boolean mQuitRequested; //is the user quitting the game

    private DotGrid mDotGrid;
    private Stack<Dot> mDotChain;
    private boolean mInteracting; //interaction = actions from touch down to touch up
    private float mChainingLineX;
    private float mChainingLineY;

    private int mScore;
    private int mMissedDots;

    private long mTimePausedLast;

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

        mRun = false;
        mBlock = false;

        mGameOver = false;
        mQuitRequested = false;

        mDotGrid = new DotGrid(GRID_LENGTH);

        //TODO remove when dots appear randomly
//            for(Dot dot: mDotGrid) {
//                int random = (int) (Math.random()*3);
//                //if(random == 1) dot.setState(DotState.APPEARING);
//                dot.setState(DotState.VISIBLE);
//            }

        mDotChain = new Stack<Dot>();
        mInteracting = false;
        mChainingLineX = 0;
        mChainingLineY = 0;

        mScore = 0;
        mMissedDots = 0;
    }

    @Override
    public void run() {
        long beginTime;        // the time when the cycle begun
        long timeDiff;        // the time it took for the cycle to execute
        int sleepTime;        // ms to sleep (<0 if we're behind)
        int framesSkipped;    // number of frames being skipped

        while (mRun) {

            Canvas c = null;
            try {
                c = mSurfaceHolder.lockCanvas();
                synchronized (mSurfaceHolder) {
                    Log.d("MAIN LOOP", "TICK");
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
            synchronized (mSurfaceHolder) {
                while (mBlock) {
                    try {
                        mSurfaceHolder.wait();
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
        synchronized (mSurfaceHolder) {
            mRun = b;
        }
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
        synchronized (mSurfaceHolder) {
            mBlock = true;
            mTimePausedLast = System.currentTimeMillis();
        }
    }

    /**
     * Requests this thread to continue.
     */
    public void onResume() {
        synchronized (mSurfaceHolder) {
            mBlock = false;
            mSurfaceHolder.notifyAll();

            //Update each visible dot with the time we were paused for
            long timePaused = System.currentTimeMillis() - mTimePausedLast;
            for(Dot dot: mDotGrid) {
                if (dot.isVisible()) dot.increaseStateStartTime(timePaused);
            }
        }
    }

    public void saveState() {
        Log.e("THREAD", "saveState");
        synchronized (mSurfaceHolder) {
            if (!mGameOver && !mQuitRequested) {
                SharedPreferences.Editor editor =
                        mContext.getSharedPreferences(GAME_STATE_FILENAME, Context.MODE_PRIVATE).edit();

                //List of variables to store
                //TODO: save all values
//                    editor
//                            .putInt(GAME_COUNTER, counter)
//                            .commit();
            }
        }
    }

    public void restoreState() {
        Log.e("THREAD", "restoreState");
        synchronized (mSurfaceHolder) {
            SharedPreferences sharedPreferences =
                    mContext.getSharedPreferences(GAME_STATE_FILENAME, Context.MODE_PRIVATE);

            //List of variables to restore
//                counter = sharedPreferences.getInt(GAME_COUNTER, 0);
            //TODO: restore all values

            //Clear the loaded state
            clearState();
        }
    }

    public void clearState() {
        Log.e("THREAD", "clearState");
        synchronized (mSurfaceHolder) {
            SharedPreferences.Editor editor =
                    mContext.getSharedPreferences(GAME_STATE_FILENAME, Context.MODE_PRIVATE).edit();

            //Clear the saved game state
            editor.clear().commit();
        }
    }

    public boolean isGameOver() {
        synchronized (mSurfaceHolder) {
            return mGameOver;
        }
    }

    public boolean isGamePaused() {
        synchronized (mSurfaceHolder) {
            return mBlock;
        }
    }

    public void setQuitRequested(boolean isQuitRequested) {
        synchronized (mSurfaceHolder) {
            this.mQuitRequested = isQuitRequested;
        }
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
        synchronized (mSurfaceHolder) {
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
        synchronized (mSurfaceHolder) {
            if (!mBlock) {
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
        }
        return true;
    }

    private void onTouchDown(float x, float y, MotionEvent motionEvent) {
        Log.i("Thread", "onTouchDown()");
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
        Log.i("Thread", "onTouchUp()");

        updateScore();

        //Hide all of the dots in the dot chain
        for (Dot dot : mDotChain) {
            dot.setState(DotState.DISAPPEARING);
        }
        mDotChain.clear();
        mInteracting = false;
    }

    private void onTouchMove(float x, float y, MotionEvent motionEvent) {
        Log.i("Thread", "onTouchMove()");

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
        Log.i("Thread", "onTouchOutside()");
        mDotChain.clear();
        mInteracting = false;
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
    }

    private void updateState() {
        for (Dot dot : mDotGrid) {
            long stateDuration = dot.getStateDuration();

            switch (dot.getState()) {
                case SELECTED:
                    // Hold this state while the dot is selected
                    break;
                case VISIBLE:
                    if (stateDuration > DURATION_VISIBLE) {
                        dot.setState(DotState.DISAPPEARING);
                        updateMissedByOne();
                        if (mMissedDots >= DOTS_TO_MISS) {
                            mRun = false;
                            mGameOver = true;
                        }
                    }
                    break;
                case DISAPPEARING:
                    if (stateDuration > DURATION_VISIBILITY_ANIMATION) {
                        dot.setState(DotState.INVISIBLE);
                    }
                    break;
                case INVISIBLE: //TODO make insane algorithm to determine when a dot should appear
                    if (stateDuration > 2000) {
                        if ((int) (Math.random() * 800) == 1) {
                            dot.setState(DotState.APPEARING);
                        }
                    }
                    break;
                case APPEARING:
                    if (stateDuration > DURATION_VISIBILITY_ANIMATION) {
                        dot.setState(DotState.VISIBLE);
                    }
                    break;
            }
        }
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
                    shadowPaint.setShader(new RadialGradient(dot.getCenterX(), dot.getCenterY()+mDotRadius*2f*0.15f, mDotRadius, mContext.getResources().getColor(R.color.black), mContext.getResources().getColor(R.color.background), Shader.TileMode.MIRROR));
                    canvas.drawCircle(dot.getCenterX(), dot.getCenterY()+mDotRadius*2f*0.15f, mDotRadius, shadowPaint);
                    canvas.drawCircle(dot.getCenterX(), dot.getCenterY(), mDotRadius, paint);
                    break;
                case SELECTED:
                    canvas.drawCircle(dot.getCenterX(), dot.getCenterY(), mDotRadius*1.15f, paint);
                    break;
                case DISAPPEARING:
                    //Determine how far into the animation we are
                    float dFactor = 1 - ((float) dot.getStateDuration() / DURATION_VISIBILITY_ANIMATION);
                    //Set Radius
                    float dRadius = mDotRadius * dFactor;
                    //Set Paint
                    Paint dPaint = new Paint(paint);
                    dPaint.setAlpha((int) (255 * dFactor));

                    shadowPaint.setShader(new RadialGradient(dot.getCenterX(), dot.getCenterY()+dRadius*2f*0.15f, dRadius, mContext.getResources().getColor(R.color.black), mContext.getResources().getColor(R.color.background), Shader.TileMode.MIRROR));
                    canvas.drawCircle(dot.getCenterX(), dot.getCenterY()+dRadius*2f*0.15f, dRadius, shadowPaint);
                    canvas.drawCircle(dot.getCenterX(), dot.getCenterY(), dRadius, dPaint);
                    break;
                case APPEARING:
                    //Determine how far into the animation we are
                    float aFactor = (float) dot.getStateDuration() / DURATION_VISIBILITY_ANIMATION;
                    //Set Radius
                    float aRadius = mDotRadius * aFactor;
                    //Set Paint
                    Paint aPaint = new Paint();
                    aPaint.set(paint);
                    aPaint.setAlpha((int) (255 * aFactor));

                    shadowPaint.setShader(new RadialGradient(dot.getCenterX(), dot.getCenterY()+aRadius*2f*0.15f, aRadius, mContext.getResources().getColor(R.color.black), mContext.getResources().getColor(R.color.background), Shader.TileMode.MIRROR));
                    canvas.drawCircle(dot.getCenterX(), dot.getCenterY()+aRadius*2f*0.15f, aRadius, shadowPaint);
                    canvas.drawCircle(dot.getCenterX(), dot.getCenterY(), aRadius, aPaint);
                    break;
            }
        }

        //Draw lines
        if (mInteracting) {
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
}
