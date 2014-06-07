package ca.brocku.dotscanvas.app.gameboard;

public class Dot {
    private final int ID;

    private int row;
    private int col;

    private float centerX;
    private float centerY;

    private DotState state;
    private long stateStartTime;

    private boolean isVisible;


    public Dot(int id, int row, int col) {
        this.ID = id;

        this.row = row;
        this.col = col;

        this.centerX = 0;
        this.centerY = 0;

        this.state = DotState.INVISIBLE;
        this.stateStartTime = System.currentTimeMillis();

        this.isVisible = false;
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

    public void setState(DotState state) {
        this.state = state;
    }

    public long getStateStartTime() {
        return stateStartTime;
    }

    public void setStateStartTime(long stateStartTime) {
        this.stateStartTime = stateStartTime;
    }

    public boolean isVisible() {
        return (state == DotState.VISIBLE);
    }
}
