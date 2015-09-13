package com.ejjan.commondaywatch;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.Rect;
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

            System.out.println("Drawing...");

            int height = canvas.getHeight();
            int width = canvas.getWidth();
            int centerx = width/2;
            int centery = height/2;

//            currentTime.setToNow();
            calendar.setTime(new Date());
            int mins = calendar.get(Calendar.MINUTE);
            int hours = calendar.get(Calendar.HOUR_OF_DAY);

            canvas.drawColor(Color.TRANSPARENT , PorterDuff.Mode.CLEAR);
            drawTickets(canvas, centerx, centery);
            drawHourHand(canvas, centerx, centery, hours, mins);
            drawMinuteHand(canvas, centerx, centery, mins);
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
