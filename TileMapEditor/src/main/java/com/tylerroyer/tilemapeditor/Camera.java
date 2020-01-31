package com.tylerroyer.tilemapeditor;

import org.apache.commons.lang3.mutable.MutableDouble;

public class Camera {
    private MutableDouble offsetX, offsetY;
    public Camera(MutableDouble offsetX, MutableDouble offsetY) {
        this.offsetX = offsetX;
        this.offsetY = offsetY;
    }

    public double getOffsetX() {
        return offsetX.getValue();
    }

    public void setOffsetX(double offsetX) {
        this.offsetX.setValue(offsetX);
    }

    public double getOffsetY() {
        return offsetY.getValue();
    }

    public void setOffsetY(double offsetY) {
        this.offsetY.setValue(offsetY);
    }
}
