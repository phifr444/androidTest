package com.fribinator.anroidgame;
import android.graphics.Bitmap;
import android.graphics.Canvas;

/**
 * Created by Philip on 2015-12-09.
 */
public class Animation {
    private Bitmap[] frames;
    private int currentFrame;
    private long startTime;
    private long delay;
    private boolean playedOnce;

    public void setFrames(Bitmap[] frames){
       this.frames = frames;
        currentFrame = 0;
        startTime = System.nanoTime();
    }

    public void setDelay(long delay){
        this.delay = delay;
    }

    public void setFrame(int i){
        currentFrame = i;
    }

    public void update(){
        long elapsed = (System.nanoTime() - startTime)/1000000;

        if (elapsed > delay){
            currentFrame ++;
            startTime = System.nanoTime();
        }
        if (currentFrame == frames.length){
            currentFrame = 0;
            playedOnce = true;
        }
    }



    public Bitmap getImage(){
        return frames[currentFrame];
    }

    public int getCurrentFrame(){
        return currentFrame;
    }

    public boolean isPlayedOnce(){
        return playedOnce;
    }


}
