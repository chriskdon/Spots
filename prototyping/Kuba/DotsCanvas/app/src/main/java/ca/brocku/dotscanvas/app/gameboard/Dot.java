package ca.brocku.dotscanvas.app.gameboard;

public class Dot {
    private final int ID;

    private int row;
    private int col;

    private float centerX;
    private float centerY;
    private boolean isVisible;


    public Dot(int id, int row, int col) {
        this.ID = id;

        this.row = row;
        this.col = col;

        this.centerX = 0;
        this.centerY = 0;
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

    public boolean isVisible() {
        return isVisible;
    }

    public void setVisible(boolean isVisible) {
        this.isVisible = isVisible;
    }
}
