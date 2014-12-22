package ca.brocku.dotscanvas.app.gameboard;

import java.io.Serializable;

public class Dot implements Serializable {
    private final int ID;

    private int row;
    private int col;

    private float centerX;
    private float centerY;

    private DotState state;
    private long stateStartTime;


    public Dot(int id, int row, int col) {
        this.ID = id;

        this.row = row;
        this.col = col;

        this.centerX = 0;
        this.centerY = 0;

        this.state = DotState.INVISIBLE;
        this.stateStartTime = System.currentTimeMillis();
    }

    public int getID() {
        return ID;
    }

    public int getRow() {
        return row;
    }

    public int getCol() {
        return col;
    }

    public float getCenterX() {
        return centerX;
    }

    public void setCenterX(float centerX) {
        this.centerX = centerX;
    }

    public float getCenterY() {
        return centerY;
    }

    public void setCenterY(float centerY) {
        this.centerY = centerY;
    }

    public DotState getState() {
        return state;
    }

    /**
     * Sets the state. Also resets the stateStartTime if we are changing states.
     *
     * @param state the new state of the Dot
     */
    public void setState(DotState state) {
        if(this.state != state) {
            this.state = state;
            this.stateStartTime = System.currentTimeMillis();
        }
    }

    public long getStateStartTime() {
        return stateStartTime;
    }

    public void increaseStateStartTime(long time) {
        stateStartTime += time;
    }

    public long getStateDuration() {
        return System.currentTimeMillis() - stateStartTime;
    }

    public boolean isVisible() {
        return state == DotState.VISIBLE;
    }
}
