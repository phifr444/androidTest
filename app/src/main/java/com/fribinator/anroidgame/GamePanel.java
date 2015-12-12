package com.fribinator.gamemki;

import android.content.Context;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.ViewStructure;

import java.util.ArrayList;
import java.util.Random;

/**
 * Created by Philip on 2015-12-09.
 */
public class GamePanel extends SurfaceView implements SurfaceHolder.Callback {
    public static final int MOVESPEED = -5;
    public static final int WIDTH = 856;
    public static final int HEIGHT = 480;

    private long smokePuffTimer;
    private long missileStartTime;
    private long missileElapsed;

    private MainThread thread;
    private Background background;
    private Player player;
    private ArrayList<Smokepuff> smokepuffs;
    private ArrayList<Missile> missiles;
    private ArrayList<TopBorder> topBorders;
    private ArrayList<BotBorder> botBorders;

    private int maxBorderHeight;
    private int minBorderHeight;
    private boolean topDown = true;
    private boolean botDown = true;

    private boolean newGameCreated;

    //Increase to slow down difficulty progression, decrease to speed up
    private int progressDenom = 20;

    private Random rand = new Random();

    public GamePanel(Context context) {
        super(context);
        //Add callback to surface holder
        getHolder().addCallback(this);

        //make gamePanel focusable to handle events
        setFocusable(true);
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {

    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        boolean retry = true;
        int counter = 0;
        while (retry && counter < 1000) {
            counter++;
            try {
                thread.setRunning(false);
                thread.join();
                retry = false;
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        background = new Background(BitmapFactory.decodeResource(getResources(), R.drawable.grassbg1));
        player = new Player(BitmapFactory.decodeResource(getResources(), R.drawable.helicopter), 65, 25, 3);
        smokepuffs = new ArrayList<Smokepuff>();
        missiles = new ArrayList<Missile>();
        topBorders = new ArrayList<TopBorder>();
        botBorders = new ArrayList<BotBorder>();

        missileStartTime = System.nanoTime();
        smokePuffTimer = System.nanoTime();

        thread = new MainThread(getHolder(), this);

        thread.setRunning(true);
        thread.start();
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            if (!player.isPlaying()) {
                player.setPlaying(true);
            } else {
                player.setUp(true);
            }
            return true;
        }
        if (event.getAction() == MotionEvent.ACTION_UP) {
            player.setUp(false);
            return true;
        }

        return super.onTouchEvent(event);
    }

    public void update() {
        if (player.isPlaying()) {
            background.update();
            player.update();


            //Calculate the threshold of the height of the border
            //max/min height are updated and border switches direction when reaching either
            //Update top border

            maxBorderHeight = 30 + player.getScore() / progressDenom;
            //Cap max border height to half the screen
            if (maxBorderHeight > HEIGHT / 4) maxBorderHeight = HEIGHT / 4;

            minBorderHeight = 5 + player.getScore() / progressDenom;

            //Check for top border collision
            for (int i = 0; i<topBorders.size(); i++){
                if (collision(topBorders.get(i), player)){
                    player.setPlaying(false);
                }
            }

            //Check for bottom border collision
            for (int i = 0; i<botBorders.size(); i++){
                if (collision(botBorders.get(i), player)){
                    player.setPlaying(false);
                }
            }


            //Update top border
            this.updateTopBorder();

            //Update bottom border
            this.updateBottomBorder();



            //Add missiles on timer
            missileElapsed = (System.nanoTime() - missileStartTime) / 1000000;
            if (missileElapsed > (2000 - player.getScore() / 4)) {
                //First missile always in the middle
                if (missiles.size() == 0) {
                    missiles.add(new Missile(BitmapFactory.decodeResource(getResources(), R.drawable.missile), WIDTH + 10, HEIGHT / 2, 45, 15, player.getScore(), 13));
                } else {
                    missiles.add(new Missile(BitmapFactory.decodeResource(getResources(), R.drawable.missile), WIDTH + 10,
                            (int) (rand.nextDouble() * (HEIGHT - (maxBorderHeight * 2))+maxBorderHeight), 45, 15, player.getScore(), 13));
                }
                //Reset timer
                missileStartTime = System.nanoTime();
            }

            //loop through every missile
            for (int i = 0; i < missiles.size(); i++) {
                missiles.get(i).update();
                if (collision(missiles.get(i), player)) {
                    missiles.remove(i);
                    player.setPlaying(false);
                    break;
                }
                //Remove missiles that are out of the screen
                if (missiles.get(i).getX() < -100) {
                    missiles.remove(i);
                    break;
                }
            }

            //Add smoke puffs
            long elapsed = (System.nanoTime() - smokePuffTimer) / 1000000;
            if (elapsed > 120) {
                smokepuffs.add(new Smokepuff(player.getX(), player.getY() + 10));
                smokePuffTimer = System.nanoTime();
            }

            for (int i = 0; i < smokepuffs.size(); i++) {
                smokepuffs.get(i).update();
                if (smokepuffs.get(i).getX() < -10) {
                    smokepuffs.remove(i);
                }
            }
        }
        else {
            if (!newGameCreated){
                newGame();
            }
        }

    }

    public boolean collision(GameObject a, GameObject b) {
        if (Rect.intersects(a.getRectangle(), b.getRectangle())) {
            return true;
        }
        return false;
    }

    @Override
    public void draw(Canvas canvas) {
        final float scaleFactorX = getWidth() / (WIDTH * 1.f);
        final float scaleFactorY = getHeight() / (HEIGHT * 1.f);

        if (canvas != null) {
            final int savedState = canvas.save();
            canvas.scale(scaleFactorX, scaleFactorY);
            background.draw(canvas);
            player.draw(canvas);
            for (Smokepuff sp : smokepuffs) {
                sp.draw(canvas);
            }
            for (Missile m : missiles) {
                m.draw(canvas);
            }
            //Draw top border
            for (TopBorder tp : topBorders){
                tp.draw(canvas);
            }
            //Draw bot border
            for (BotBorder bb : botBorders){
                bb.draw(canvas);
            }
            canvas.restoreToCount(savedState);


        }
    }


    public void updateTopBorder() {
        if (player.getScore() % 50 == 0) {
            topBorders.add(new TopBorder(BitmapFactory.decodeResource(getResources(), R.drawable.brick),
                    topBorders.get(topBorders.size()-1).getX()+20, 0, (int)((rand.nextDouble()*(maxBorderHeight))+1)));
        }
        for (int i = 0; i < topBorders.size(); i++){
            topBorders.get(i).update();
            if (topBorders.get(i).getX() < -20){
                topBorders.remove(i);
                //Remove borders outside of the screen

                if (topBorders.get(topBorders.size() -1).getHeight() >= maxBorderHeight){
                    topDown = false;
                }
                else if (topBorders.get(topBorders.size()-1).getHeight() <= minBorderHeight){
                    topDown = true;
                }
                //new border will have larger height
                if (topDown){
                    topBorders.add(new TopBorder(BitmapFactory.decodeResource(getResources(), R.drawable.brick), topBorders.get(topBorders.size()-1).getX()+20, 0,
                            topBorders.get(topBorders.size()-1).getHeight()+1));
                }
                //new border will have smaller height
                else {
                    topBorders.add(new TopBorder(BitmapFactory.decodeResource(getResources(), R.drawable.brick), topBorders.get(topBorders.size()-1).getX()+20, 0,
                            topBorders.get(topBorders.size()-1).getHeight()-1));
                }
            }
        }

    }

    public void updateBottomBorder() {
        //every 50 points insert randomly placed top blocks
        if (player.getScore() % 40 == 0) {
            botBorders.add(new BotBorder(BitmapFactory.decodeResource(getResources(), R.drawable.brick),
                    botBorders.get(botBorders.size()-1).getX()+20, (int)((rand.nextDouble()*maxBorderHeight)+(HEIGHT-maxBorderHeight))));
        }
        for (int i = 0; i < botBorders.size(); i++) {
            botBorders.get(i).update();

            //Remove borders off screen
            if (botBorders.get(i).getX() < -20) {
                botBorders.remove(i);
                if (botBorders.get(botBorders.size() - 1).getY() <= HEIGHT- maxBorderHeight) {
                    botDown = true;
                } else if (botBorders.get(botBorders.size() - 1).getY() >= HEIGHT - minBorderHeight) {
                    botDown = false;
                }
                //new border will have larger height
                if (botDown) {
                    botBorders.add(new BotBorder(BitmapFactory.decodeResource(getResources(), R.drawable.brick),
                            botBorders.get(botBorders.size() - 1).getX() + 20, botBorders.get(botBorders.size() - 1).getY() - 1));
                }
                //new border will have smaller height
                else {
                    botBorders.add(new BotBorder(BitmapFactory.decodeResource(getResources(), R.drawable.brick),
                            botBorders.get(botBorders.size() - 1).getX() + 20, botBorders.get(botBorders.size() - 1).getY() - 1));
                }
            }
        }
    }

    public void newGame(){
        botBorders.clear();
        topBorders.clear();
        missiles.clear();
        smokepuffs.clear();


        minBorderHeight = 5;
        maxBorderHeight = 30;

        player.resetDY();
        player.resetScore();
        player.setY(HEIGHT/2);


        //Create initial top borders
        for (int i = 0; i*20 < WIDTH+40; i++){
            //First top border created
            if (i == 0){
                topBorders.add(new TopBorder(BitmapFactory.decodeResource(getResources(), R.drawable.brick),
                        i*20, 0 , 10));
            }
            else {
                topBorders.add(new TopBorder(BitmapFactory.decodeResource(getResources(), R.drawable.brick),
                        i*20, 0, topBorders.get(i-1).getHeight()+1));
            }
        }

        //Create initial bottom borders
        for (int i = 0; i*20 < WIDTH + 40; i++){
            //First bottom border created
            if (i == 0){
                botBorders.add(new BotBorder(BitmapFactory.decodeResource(getResources(), R.drawable.brick),
                        i*20, HEIGHT-minBorderHeight));
            }
            else {
                botBorders.add(new BotBorder(BitmapFactory.decodeResource(getResources(), R.drawable.brick),
                        i*20, botBorders.get(i-1).getY() - 1));
            }
        }

        newGameCreated = true;
    }
}