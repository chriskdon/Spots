package ca.brocku.dotscanvas.app;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import java.util.UUID;

/**
 * @author Jakub Subczynski
 * @date May 23, 2014
 */
public class GameSurfaceView extends SurfaceView implements SurfaceHolder.Callback {
    private GameThread thread; //Handles drawing; initialized in surfaceCreated() callback
    private Context mContext;

    public GameSurfaceView(Context context, AttributeSet attrs) {
        super(context, attrs);

        //Register this SurfaceHolder to listen for changes to the Surface
        SurfaceHolder surfaceHolder = getHolder();
        surfaceHolder.addCallback(this);

        mContext =  context;
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
        Log.e("surfaceCreated", "started");
        thread = new GameThread(surfaceHolder, mContext);
        thread.setRunning(true);
        thread.start();
        Log.e("surfaceCreated", "finished");
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
        Log.e("surfaceChanged", "ran");
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
        Log.e("surfaceDestroyed", "started");
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
        Log.e("surfaceDestroyed", "finished");
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


    /**
     * This is the Thread which draws to the Canvas.
     */
    class GameThread extends Thread {
        private SurfaceHolder mSurfaceHolder;
        private Context mContext;

        private boolean mRun;  //whether the surface has been created & is ready to draw
        private boolean mBlock; //whether the surface has lost focus

        private String TEMP_UUID = UUID.randomUUID().toString();

        private int counter = 0;

        public GameThread(SurfaceHolder surfaceHolder, Context context) {
            mSurfaceHolder = surfaceHolder;
            mContext = context;

            mRun = false;
            mBlock = false;
        }

        @Override
        public void run() {
            while(mRun) {
                Log.e("UUID", TEMP_UUID);
                Canvas c = null;
                try {
                    c = mSurfaceHolder.lockCanvas();
                    synchronized (mSurfaceHolder) {
                        /** UPDATE STATES HERE */
                    if(c != null) doDraw(c);
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

        private void doDraw(Canvas canvas) {
            canvas.drawColor(Color.WHITE); //clear the screen
            Paint paint = new Paint();
            paint.setColor(Color.RED);
            paint.setStrokeWidth(5);
            canvas.drawPoint(counter, counter, paint);
            counter++;
        }
    }
}
