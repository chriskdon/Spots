package ca.brocku.dotscanvas.app;

public class Dot {
    private final int ID;

    private float centerX;
    private float centerY;
    private boolean isVisible;


    public Dot(int id) {
        this.ID = id;
        this.isVisible = false;
    }

    public int getID() {
        return ID;
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
