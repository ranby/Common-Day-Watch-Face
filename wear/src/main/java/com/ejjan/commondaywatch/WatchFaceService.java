package com.ejjan.commondaywatch;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.os.Bundle;
import android.support.wearable.watchface.CanvasWatchFaceService;
import android.support.wearable.watchface.WatchFaceStyle;
import android.view.SurfaceHolder;

import java.util.Date;

/**
 * Created by Erik on 12/09/15.
 */
public class WatchFaceService extends CanvasWatchFaceService {
    @Override
    public Engine onCreateEngine() {
        /* provide your watch face implementation */
        System.out.println("Creating Engine...");
        return new Engine();
    }

    /* implement service callback methods */
    private class Engine extends CanvasWatchFaceService.Engine {

        @Override
        public void onCreate(SurfaceHolder holder) {
            super.onCreate(holder);
            /* initialize your watch face */
            setWatchFaceStyle( new WatchFaceStyle.Builder( WatchFaceService.this )
                            .setBackgroundVisibility( WatchFaceStyle.BACKGROUND_VISIBILITY_INTERRUPTIVE )
                            .setCardPeekMode( WatchFaceStyle.PEEK_MODE_VARIABLE )
                            .setShowSystemUiTime( false )
                            .build()
            );
        }

        @Override
        public void onPropertiesChanged(Bundle properties) {
            super.onPropertiesChanged(properties);
            /* get device features (burn-in, low-bit ambient) */
        }

        @Override
        public void onTimeTick() {
            super.onTimeTick();
            /* the time changed */
        }

        @Override
        public void onAmbientModeChanged(boolean inAmbientMode) {
            super.onAmbientModeChanged(inAmbientMode);
            /* the wearable switched between modes */
        }

        @Override
        public void onDraw(Canvas canvas, Rect bounds) {
            /* draw your watch face */
            System.out.println("Drawing...");

            int height = canvas.getHeight();
            int width = canvas.getWidth();
            int centerx = width/2;
            int centery = height/2;

            Date date = new Date();
            int mins = date.getMinutes();
            int hours = date.getHours();

            drawTickets(canvas, centerx, centery);
            drawHourHand(canvas, centerx, centery, hours, mins);
            drawMinuteHand(canvas, centerx, centery, mins);
        }

        @Override
        public void onVisibilityChanged(boolean visible) {
            super.onVisibilityChanged(visible);
            /* the watch face became visible or invisible */
        }

        private void drawHourHand(Canvas canvas, int centerx, int centery, int hours, int minutes) {
            int length = 115;

            System.out.println("Hours: " + hours);
            double angle = ((hours + (minutes / 60f)) / 6f ) * (float) Math.PI;

            double xVal = Math.sin(angle) * length;
            double yVal = -Math.cos(angle) * length;

            Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
            p.setStrokeWidth(3);
            p.setColor(Color.WHITE);
            p.setShadowLayer(2, 1, 0, Color.BLACK);
            canvas.drawLine(centerx, centery, (float)(centerx+xVal), (float)(centery+yVal), p);
        }

        private void drawMinuteHand(Canvas canvas, int centerx, int centery, int minutes) {
            int length = 135;

            System.out.println("Minutes: " + minutes);
            double angle = minutes / 30f * (float) Math.PI;

            double xVal = Math.sin(angle) * length;
            double yVal = -Math.cos(angle) * length;

            Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
            p.setStrokeWidth(3);
            p.setColor(Color.WHITE);
            p.setShadowLayer(2, 1, 0, Color.BLACK);
            canvas.drawLine(centerx, centery, (float)(centerx+xVal), (float)(centery+yVal), p);
        }

        private void drawTickets(Canvas canvas, int centerx, int centery) {
            double sinVal = 0, cosVal = 0, angle = 0;
            float length1 = 0, length2 = 0;
            float x1 = 0, y1 = 0;

            // draw ticks
            length1 = centerx - 25;
            length2 = centerx;
            for (int i = 0; i < 60; i++) {
                angle = (i * Math.PI * 2 / 60);
                sinVal = Math.sin(angle);
                cosVal = Math.cos(angle);
                float len;
                if (i % 5 == 0) { //larger tickets
                    len = length1 + 10;
                    x1 = (float)(sinVal * len);
                    y1 = (float)(-cosVal * len);

                    Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
                    p.setColor(Color.parseColor("#ffffff"));
                    p.setTextSize(16);
                    int xoffset, yoffset; //adjust number placement since they're origin is the left/top corner.
                    xoffset = (i < 10) ? -4 : -8; //number with one digit should not be adjusted as much.
                    yoffset = 6;

                    canvas.drawText(String.valueOf(i), centerx + x1 + xoffset, centery + y1 + yoffset, p);
                } else { // smaller tickets
                    len = length1 + 15;
                    x1 = (float)(sinVal * len);
                    y1 = (float)(-cosVal * len);

                    Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
                    p.setColor(Color.parseColor("#ffffff"));
                    canvas.drawCircle(centerx + x1, centery + y1, 2, p);
                }


            }
        }
    }
}
