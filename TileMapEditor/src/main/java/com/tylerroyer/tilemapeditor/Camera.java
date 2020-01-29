package com.tylerroyer.tilemapeditor;

public class Camera {
    private double offsetX = 0.0, offsetY = 0.0;
    public Camera() {}

    public double getOffsetX() {
        return offsetX;
    }

    public void setOffsetX(double offsetX) {
        this.offsetX = offsetX;
    }

    public double getOffsetY() {
        return offsetY;
    }

    public void setOffsetY(double offsetY) {
        this.offsetY = offsetY;
    }
}
