package com.ejjan.commondaywatch;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.wearable.watchface.CanvasWatchFaceService;
import android.support.wearable.watchface.WatchFaceStyle;
import android.view.SurfaceHolder;

import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

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

        private static final int MSG_UPDATE_TIME = 42;
        private static final long DEFAULT_UPDATE_RATE_MS = 1000*60;

        private boolean hasTimeZoneReceiverBeenRegistered = false;

        Calendar calendar;

        final Handler updateTimeHandler = new Handler() {
            @Override
            public void handleMessage(Message message) {
                switch (message.what) {
                    case MSG_UPDATE_TIME:
                        invalidate();
                        System.out.println("Invalidated due to timer message");
                        if (shouldTimerBeRunning()) {
                            long timeMs = System.currentTimeMillis();
                            long delayMs = DEFAULT_UPDATE_RATE_MS
                                    - (timeMs % DEFAULT_UPDATE_RATE_MS);
                            updateTimeHandler.sendEmptyMessageDelayed(MSG_UPDATE_TIME, delayMs);
                        }
                        break;
                }
            }
        };

        final BroadcastReceiver timeZoneReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                calendar.setTimeZone(TimeZone.getDefault());
                invalidate();
            }
        };

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

            calendar = Calendar.getInstance();
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
            System.out.println("Time tick...");
            invalidate();
        }

        @Override
        public void onAmbientModeChanged(boolean inAmbientMode) {
            super.onAmbientModeChanged(inAmbientMode);
            /* the wearable switched between modes */
        }

        @Override
        public void onDraw(Canvas canvas, Rect bounds) {
            /* draw your watch face */
            canvas.drawColor(Color.TRANSPARENT , PorterDuff.Mode.CLEAR);

            System.out.println("Drawing...");
            //Draw background
            BitmapFactory factory = new BitmapFactory();
            Bitmap bitmap = factory.decodeResource(getResources(), R.drawable.background);
            Bitmap scaled = Bitmap.createScaledBitmap(bitmap, bounds.width(), bounds.height(), false);
            canvas.drawBitmap(scaled, 0, 0, new Paint(Paint.ANTI_ALIAS_FLAG));

            int height = canvas.getHeight();
            int width = canvas.getWidth();
            int centerx = width/2;
            int centery = height/2;

            calendar.setTime(new Date());
            int mins = calendar.get(Calendar.MINUTE);
            int hours = calendar.get(Calendar.HOUR_OF_DAY);

            drawTickets(canvas, centerx, centery);
            drawHourHand(canvas, centerx, centery, hours, mins);
            drawMinuteHand(canvas, centerx, centery, mins);
            drawNumbers(canvas, centerx, centery);
            drawMiddle(canvas, centerx, centery);
        }

        @Override
        public void onVisibilityChanged(boolean visible) {
            super.onVisibilityChanged(visible);
            /* the watch face became visible or invisible */
            if (visible) {
                registerReceiver();

                // Update time zone in case it changed while we weren't visible.
                calendar.setTimeZone(TimeZone.getDefault());
            } else {
                unregisterReceiver();
            }

            // Whether the timer should be running depends on whether we're visible and
            // whether we're in ambient mode, so we may need to start or stop the timer
            updateTimer();
        }

        private void updateTimer() {
            updateTimeHandler.removeMessages(MSG_UPDATE_TIME);
            if (shouldTimerBeRunning()) {
                updateTimeHandler.sendEmptyMessage(MSG_UPDATE_TIME);
            }
        }

        private void registerReceiver() {
            if (hasTimeZoneReceiverBeenRegistered) {
                return;
            }
            hasTimeZoneReceiverBeenRegistered = true;
            IntentFilter filter = new IntentFilter(Intent.ACTION_TIMEZONE_CHANGED);
            WatchFaceService.this.registerReceiver(timeZoneReceiver, filter);
        }

        private void unregisterReceiver() {
            if (!hasTimeZoneReceiverBeenRegistered) {
                return;
            }
            hasTimeZoneReceiverBeenRegistered = false;
            WatchFaceService.this.unregisterReceiver(timeZoneReceiver);
        }

        private boolean shouldTimerBeRunning() {
            return isVisible() && !isInAmbientMode();
        }

        private void drawHourHand(Canvas canvas, int centerx, int centery, int hours, int minutes) {
            int backLength = 15;
            int length = 115 + backLength;

            System.out.println("Hours: " + hours);
            double angle = ((hours + (minutes / 60f)) / 6f ) * (float) Math.PI;

            double xVal = Math.sin(angle) * length;
            double yVal = -Math.cos(angle) * length;
            double backx = Math.sin(angle) * backLength;
            double backy = -Math.cos(angle) * backLength;

            Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
            p.setStrokeWidth(3);
            p.setColor(Color.WHITE);
            p.setShadowLayer(2, 1, 0, Color.BLACK);
            canvas.drawLine((float)(centerx-backx), (float)(centery-backy), (float)(centerx+xVal), (float)(centery+yVal), p);
        }

        private void drawMinuteHand(Canvas canvas, int centerx, int centery, int minutes) {
            int backLength = 20;
            int length = 135 + backLength;

            System.out.println("Minutes: " + minutes);
            double angle = minutes / 30f * (float) Math.PI;

            double xVal = Math.sin(angle) * length;
            double yVal = -Math.cos(angle) * length;
            double backx = Math.sin(angle) * backLength;
            double backy = -Math.cos(angle) * backLength;

            Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
            p.setStrokeWidth(3);
            p.setColor(Color.WHITE);
            p.setShadowLayer(2, 1, 0, Color.BLACK);
            canvas.drawLine((float)(centerx - backx), (float)(centery - backy), (float)(centerx+xVal), (float)(centery+yVal), p);
        }

        private void drawTickets(Canvas canvas, int centerx, int centery) {
            double sinVal, cosVal, angle;
            float length1, length2;
            float x1, y1, x2, y2;

            // draw ticks
            length1 = centerx - 25;
            length2 = centerx;
            for (int i = 0; i < 60; i++) {
                angle = (i * Math.PI * 2 / 60);
                sinVal = Math.sin(angle);
                cosVal = Math.cos(angle);
                float len;
                if (i % 5 == 0) { //larger tickets
                    len = length1 + 15;
                    x1 = (float)(sinVal * len);
                    y1 = (float)(-cosVal * len);

                    Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
                    p.setColor(Color.parseColor("#aaaaaa"));
                    p.setTextSize(16);
                    int xoffset, yoffset; //adjust number placement since they're origin is the left/top corner.
                    xoffset = (i < 10) ? -4 : -8; //number with one digit should not be adjusted as much.
                    yoffset = 6;
//                    canvas.drawText(String.valueOf(i), centerx + x1 + xoffset, centery + y1 + yoffset, p);

                    canvas.drawCircle(centerx + x1, centery + y1, 6, p);

                } else { // smaller tickets
                    len = length1 + 15;
                    x1 = (float)(sinVal * len);
                    y1 = (float)(-cosVal * len);
                    x2 = (float)(sinVal * length2);
                    y2 = (float)(-cosVal * length2);

                    Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
                    p.setColor(Color.parseColor("#5b5b5b"));
                    p.setStrokeWidth(2);
                    canvas.drawLine(centerx + x1, centery + y1, centerx + x2, centery + y2, p);
                }


            }
        }

        private void drawNumbers(Canvas canvas, int centerx, int centery) {
            int len = centerx - 45;
            double[] angles = {0, 0.25*Math.PI*2, 0.5*Math.PI*2, 0.75*Math.PI*2};
            double[] sinVals = {Math.sin(angles[0]), Math.sin(angles[1]), Math.sin(angles[2]), Math.sin(angles[3])};
            double[] cosVals = {Math.cos(angles[0]), Math.cos(angles[1]), Math.cos(angles[2]), Math.cos(angles[3])};

            int textSize = 30;
            Paint p = new Paint();
            p.setTextSize(textSize);
            p.setColor(Color.WHITE);
            p.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
            float x, y;
            x = (float)(sinVals[0] * len);
            y = (float)(-cosVals[0] * len);
            canvas.drawText("12", centerx + x - (textSize/2), centery + y , p);

            x = (float)(sinVals[1] * len);
            y = (float)(-cosVals[1] * len);
            canvas.drawText("3", centerx + x + 8, centery + y + 8, p);

            x = (float)(sinVals[2] * len);
            y = (float)(-cosVals[2] * len);
            canvas.drawText("6", centerx + x - (textSize/4), centery + y + 20, p);

            x = (float)(sinVals[3] * len);
            y = (float)(-cosVals[3] * len);
            canvas.drawText("9", centerx + x - 20, centery + y + 8, p);
        }

        private void drawMiddle(Canvas canvas, int centerx, int centery) {
            Paint p = new Paint();
            p.setColor(Color.WHITE);
            canvas.drawCircle(centerx, centery, 6, p);

            p.setColor(Color.BLACK);
            canvas.drawCircle(centerx, centery, 2, p);
        }
    }
}
