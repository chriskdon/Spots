package ca.brocku.dotscanvas.app;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.TextView;

import java.util.Iterator;
import java.util.Stack;

import ca.brocku.dotscanvas.app.gameboard.Dot;
import ca.brocku.dotscanvas.app.gameboard.DotGrid;
import ca.brocku.dotscanvas.app.gameboard.DotState;

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
            SharedPreferences.Editor editor =
                    mContext.getSharedPreferences(GameThread.GAME_STATE_FILENAME, Context.MODE_PRIVATE).edit();
            editor.clear().commit();
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
        Log.e("Thread", "surfaceCreated()");
        thread = new GameThread(surfaceHolder, mContext, new ScoreViewHandler(), new MissedViewHandler());
        thread.restoreState();
        thread.setRunning(true);
        thread.start();
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
        Log.e("Thread", "surfaceChanged()");
        thread.setSurfaceSize(width, height);
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
        Log.e("Thread", "surfaceDestroyed()");
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

    public void onResumeGame() {
        if (thread != null) {
            thread.onResume();
        }
    }

    public GameThread getGameThread() {
        return thread;
    }

    public void setScoreView(TextView scoreView) {
        this.mScoreView = scoreView;
    }

    public void setMissedView(TextView missedView) {
        this.mMissedView = missedView;
    }

    /**
     * This is the Thread which draws to the Canvas.
     */
    class GameThread extends Thread {
        //Strings used for storing the game state
        public static final String GAME_STATE_FILENAME = "GAME_STATE";
        public static final String GAME_SCORE = "GAME_SCORE";
        public static final String GAME_MISSED = "GAME_MISSED";

        private static final int GRID_LENGTH = 6;
        private static final int NUMBER_OF_DOTS = 36;
        private static final int DOTS_TO_MISS = 15;

        private static final long DURATION_ANIMATION = 100;
        private static final long DURATION_VISIBLE = 2000;

        private SurfaceHolder mSurfaceHolder;
        private Context mContext;
        private ScoreViewHandler mScoreViewHandler;
        private MissedViewHandler mMissedViewHandler;

        private int mCanvasHeight = 1;
        private int mCanvasWidth = 1;
        private float mCanvasLength = 1; //the smaller of the height and width
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

        // Game Loop
        private final static int MAX_FPS = 60;                  // Desired fps
        private final static int MAX_FRAME_SKIPS = 5;           // Maximum number of frames to be skipped
        private final static int FRAME_PERIOD = 1000 / MAX_FPS; // The frame period

        public GameThread(SurfaceHolder surfaceHolder, Context context, ScoreViewHandler scoreViewHandler, MissedViewHandler missedViewHandler) {
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
                            // we need to catch up
                            // update without rendering
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
        private void setRunning(boolean b) {
            synchronized (mSurfaceHolder) {
                mRun = b;
            }
        }

        /**
         * Requests this thread to wait.
         */
        public void onPause() {
            synchronized (mSurfaceHolder) {
                mBlock = true;
            }
        }

        /**
         * Requests this thread to continue.
         */
        public void onResume() {
            synchronized (mSurfaceHolder) {
                mBlock = false;
                mSurfaceHolder.notifyAll();
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
        public void setSurfaceSize(int width, int height) {
            synchronized (mSurfaceHolder) {
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
//            Log.i("SurfaceSize", "L: " + String.valueOf(mCanvasLength) + "; H: " + String.valueOf(mCanvasHeight) + "; W: " + String.valueOf(mCanvasWidth));
        }

        public boolean onTouch(MotionEvent motionEvent) {
            synchronized (mSurfaceHolder) {
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

                return true;
            }
        }

        private void onTouchDown(float x, float y, MotionEvent motionEvent) {
            Log.i("Thread", "onTouchDown()");
            mChainingLineX = x;
            mChainingLineY = y;

            for (Dot dot : mDotGrid) {
                if (dot.isVisible() && isTouchWithinDot(x, y, dot, 1.4f)) {
                    mDotChain.push(dot);
                    //TODO animate dot
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
                            mDotChain.push(dot);
                            //TODO animate dot

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

        private void updateScore() {
            if (!mDotChain.isEmpty()) {
                mScore += Math.pow(mDotChain.size(), 2);
            }

            Message message = mScoreViewHandler.obtainMessage();
            Bundle bundle = new Bundle();
            bundle.putInt(GAME_SCORE, mScore);
            message.setData(bundle);
            mScoreViewHandler.sendMessage(message);
        }

        private void updateMissedByOne() {
            mMissedDots++;

            Message message = mMissedViewHandler.obtainMessage();
            Bundle bundle = new Bundle();
            bundle.putInt(GAME_MISSED, DOTS_TO_MISS - mMissedDots);
            message.setData(bundle);
            mMissedViewHandler.sendMessage(message);
        }

        private void updateState() {
            for (Dot dot : mDotGrid) {
                long stateDuration = dot.getStateDuration();

                switch (dot.getState()) {
                    case VISIBLE:
                        if (stateDuration > DURATION_VISIBLE) {
                            dot.setState(DotState.DISAPPEARING);
                            updateMissedByOne();
                            if (mMissedDots >= DOTS_TO_MISS) {
                                mRun = false;
                            }
                        }
                        break;
                    case DISAPPEARING:
                        if (stateDuration > DURATION_ANIMATION) {
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
                        if (stateDuration > DURATION_ANIMATION) {
                            dot.setState(DotState.VISIBLE);
                        }
                        break;
                }
            }
        }

        private void doDraw(Canvas canvas) {
            canvas.drawColor(Color.rgb(51, 51, 51)); //clear the screen

            Paint paint = new Paint();

            paint.setAntiAlias(true);
            paint.setColor(Color.rgb(237, 17, 100));
            paint.setStrokeWidth(15);

            //Draw dots
            for (Dot dot : mDotGrid) {
                switch (dot.getState()) {
                    case VISIBLE:
                        canvas.drawCircle(dot.getCenterX(), dot.getCenterY(), mDotRadius, paint);
                        break;
                    case DISAPPEARING:
                        //Determine how far into the animation we are
                        float dFactor = 1 - ((float) dot.getStateDuration() / DURATION_ANIMATION);
                        //Set Radius
                        float dRadius = mDotRadius * dFactor;
                        //Set Paint
                        Paint dPaint = new Paint();
                        dPaint.set(paint);
                        dPaint.setAlpha((int) (255 * dFactor));

                        canvas.drawCircle(dot.getCenterX(), dot.getCenterY(), dRadius, dPaint);
                        break;
                    case APPEARING:
                        //Determine how far into the animation we are
                        float aFactor = (float) dot.getStateDuration() / DURATION_ANIMATION;
                        //Set Radius
                        float aRadius = mDotRadius * aFactor;
                        //Set Paint
                        Paint aPaint = new Paint();
                        aPaint.set(paint);
                        aPaint.setAlpha((int) (255 * aFactor));

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

    private class ScoreViewHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            String score = String.valueOf(msg.getData().getInt(GameThread.GAME_SCORE));
            mScoreView.setText(score);
        }
    }

    private class MissedViewHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            String missed = String.valueOf(msg.getData().getInt(GameThread.GAME_MISSED));
            mMissedView.setText(missed);
        }
    }
}