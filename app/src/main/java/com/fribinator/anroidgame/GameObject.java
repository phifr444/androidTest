package com.fribinator.gamemki;

import android.graphics.Rect;

/**
 * Created by Philip on 2015-12-09.
 */
public abstract class GameObject {
    protected int x;
    protected int y;
    protected int dy;
    protected int dx;
    protected int height;
    protected int width;

    public Rect getRectangle(){
        return new Rect(x, y, x+width, y+height);
    }

    public int getX() {
        return x;
    }

    public void setX(int x) {
        this.x = x;
    }

    public int getY() {
        return y;
    }

    public void setY(int y) {
        this.y = y;
    }

    public int getHeight() {
        return height;
    }

    public int getWidth() {
        return width;
    }
    
}
