package ca.brocku.dotscanvas.app;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;

import java.util.Stack;

public class GameSurfaceView extends SurfaceView implements SurfaceHolder.Callback, View.OnTouchListener {
    private GameThread thread; //Handles drawing; initialized in surfaceCreated() callback
    private Context mContext;

    public GameSurfaceView(Context context, AttributeSet attrs) {
        super(context, attrs);

        //Register this SurfaceHolder to listen for changes to the Surface
        SurfaceHolder surfaceHolder = getHolder();
        surfaceHolder.addCallback(this);

        //Register this to listen for click events
        this.setOnTouchListener(this);

        mContext =  context;

        //Clear any saved game state
        SharedPreferences.Editor editor =
                mContext.getSharedPreferences(GameThread.GAME_STATE_FILENAME, Context.MODE_PRIVATE).edit();
        editor.clear().commit();
    }

    /**
     * Callback for the SurfaceHolder once the surface is created.
     *
     * Create a new thread here for every new surface to tie the thread's lifecycle to that of the
     * surface.
     *
     * Start the thread here so we don't busy-wait in run() waiting for the surface to be created
     *
     * @param surfaceHolder the SurfaceHolder whose surface has been created.
     */
    @Override
    public void surfaceCreated(SurfaceHolder surfaceHolder) {
        Log.e("Thread", "surfaceCreated()");
        thread = new GameThread(surfaceHolder, mContext);
        thread.restoreState();
        thread.setRunning(true);
        thread.start();
    }

    /**
     * Callback for the SurfaceHolder when the surface changes.
     *
     * @param holder the SurfaceHolder whose surface has changed.
     * @param format the new PixelFormat of the surface.
     * @param width the new width of the surface.
     * @param height the new height of the surface.
     */
    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        Log.e("Thread", "surfaceChanged()");
        thread.setSurfaceSize(width, height);
    }

    /**
     * Callback for the SurfaceHolder once the surface is destroyed.
     *
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
        if(thread != null) {
            thread.onPause();
        }
    }

    public void onResumeGame() {
        if(thread != null) {
            thread.onResume();
        }
    }

    public GameThread getGameThread() {
        return thread;
    }

    /**
     * This is the Thread which draws to the Canvas.
     */
    class GameThread extends Thread {
        //Strings used for storing the game state
        private static final String GAME_STATE_FILENAME = "GAME_STATE";

        private static final int GRID_LENGTH = 6;
        private static final int NUMBER_OF_DOTS = 36;
        private static final int DOTS_TO_MISS = 15;

        private Context mContext;
        private SurfaceHolder mSurfaceHolder;
        private int mCanvasHeight = 1;
        private int mCanvasWidth = 1;
        private float mCanvasLength = 1; //the smaller of the height and width
        float mPixelsPerDotRegion = 1;
        float mDotRadius = 1;

        private boolean mRun;  //whether the surface has been created & is ready to draw
        private boolean mBlock; //whether the surface has lost focus

        private boolean isGameOver; //has the game completed
        private boolean isQuitRequested; //is the user quitting the game

        private Dot[][] mDotGrid;
        private Stack<Dot> mDotChain;
        private boolean isInteracting; //interaction = actions from touch down to touch up

        public GameThread(SurfaceHolder surfaceHolder, Context context) {
            mSurfaceHolder = surfaceHolder;
            mContext = context;

            mRun = false;
            mBlock = false;

            isGameOver = false;
            isQuitRequested = false;

            initializeGrid();
            mDotChain = new Stack<Dot>();
            isInteracting = false;
        }

        private void initializeGrid() {
            mDotGrid = new Dot[GRID_LENGTH][GRID_LENGTH];

            for(int row=0; row<GRID_LENGTH; row++) {
                for(int col=0; col<GRID_LENGTH; col++) {
                    mDotGrid[row][col] = new Dot(row*GRID_LENGTH + (col+1));
                    mDotGrid[row][col].setVisible(true);
                }
            }
        }

        @Override
        public void run() {
            while(mRun) {
                Canvas c = null;
                try {
                    c = mSurfaceHolder.lockCanvas();
                    synchronized (mSurfaceHolder) {
                        /** UPDATE STATES HERE **/
                        if(c != null) {
                            doDraw(c);
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
                    while(mBlock) {
                        try {
                            mSurfaceHolder.wait();
                        } catch (InterruptedException e) {}
                    }
                }
            }
        }

        /**
         * Whether or not this thread should finish. It is set based on the state of the Surface.
         *  surfaceCreated --> run thread
         *  surfaceDestroyed --> stop thread
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
                if(!isGameOver && !isQuitRequested) {
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
                return isGameOver;
            }
        }

        public void setQuitRequested(boolean isQuitRequested) {
            synchronized (mSurfaceHolder) {
                this.isQuitRequested = isQuitRequested;
            }
        }

        /**
         * Called when the Surface size changes to store the updated canvas dimensions and related
         * measurements
         *
         * @see ca.brocku.dotscanvas.app.GameSurfaceView#surfaceChanged
         *
         * @param width the new width of the the new width of the surface.
         * @param height the new height of the surface.
         */
        public void setSurfaceSize(int width, int height) {
            synchronized (mSurfaceHolder) {
                mCanvasHeight = height;
                mCanvasWidth = width;
                mCanvasLength = height < width ? height : width;

                mPixelsPerDotRegion = mCanvasLength/GRID_LENGTH;
                mDotRadius = mPixelsPerDotRegion*2.0f/3.0f /2;

                for(int row=0; row<GRID_LENGTH; row++) {
                    for(int col=0; col<GRID_LENGTH; col++) {
                        mDotGrid[row][col].setCenterX(
                                (float) ((float)row*mPixelsPerDotRegion + mPixelsPerDotRegion/2.0));
                        mDotGrid[row][col].setCenterY(
                                (float) ((float)col*mPixelsPerDotRegion + mPixelsPerDotRegion/2.0));
                    }
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
            for(int row=0; row<GRID_LENGTH; row++) {
                for(int col=0; col<GRID_LENGTH; col++) {
                    if(mDotGrid[row][col].isVisible()) {
                        float diffX = Math.abs(x - mDotGrid[row][col].getCenterX());
                        float diffY = Math.abs(y - mDotGrid[row][col].getCenterY());

                        if(diffX <= mDotRadius && diffY <= mDotRadius) {
                            mDotChain.push(mDotGrid[row][col]);
                            //TODO animate dot
                        }
                    }
                }
            }
            isInteracting = true;
        }

        private void onTouchUp(float x, float y, MotionEvent motionEvent) {
            Log.i("Thread", "onTouchUp()");

            for(Dot dot: mDotChain) {
                dot.setVisible(false);
            }
            isInteracting = false;
        }

        private void onTouchMove(float x, float y, MotionEvent motionEvent) {
            Log.i("Thread", "onTouchMove()");

        }

        private void onTouchOutside(float x, float y, MotionEvent motionEvent) {
            Log.i("Thread", "onTouchOutside()");
        }

        private void doDraw(Canvas canvas) {
            canvas.drawColor(Color.WHITE); //clear the screen

            //Draw dots
            Paint paint = new Paint();
            paint.setColor(Color.RED);
            for(int row=0; row<GRID_LENGTH; row++) {
                for(int col=0; col<GRID_LENGTH; col++) {
                    if(mDotGrid[row][col].isVisible()) {
                        canvas.drawCircle(
                                mDotGrid[row][col].getCenterX(),
                                mDotGrid[row][col].getCenterY(),
                                mDotRadius, paint);
                    }
                }
            }
        }
    }
}