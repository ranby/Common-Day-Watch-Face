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
import android.os.BatteryManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.wearable.watchface.CanvasWatchFaceService;
import android.support.wearable.watchface.WatchFaceStyle;
import android.view.SurfaceHolder;
import android.view.WindowInsets;

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
        private static final long DEFAULT_UPDATE_RATE_MS = 1000;
        private int batteryLevel = 80;

        private boolean hasTimeZoneReceiverBeenRegistered = false;
        private Paint smallTicketsColor;
        private Paint largeTicketsColor;
        private Paint specialLargeTicketsColor;
        private Paint hourHandColor;
        private Paint minuteHandColor;
        private Paint secondHandColor;
        private Paint numbersColor;
        private Paint middleColor;
        private Paint middleDotColor;
        private Paint dateRectColor;
        private Paint dateTextColor;
        private Paint batteryFullColor;
        private Paint batteryEmptyColor;
        private int numbersTextSize = 30;

        private Bitmap backgroundBitmap;

        private int chinSize = 0;

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

        private BroadcastReceiver batteryInfoReceiver = new BroadcastReceiver(){
            @Override
            public void onReceive(Context arg0, Intent intent) {
                batteryLevel = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, 0);
                System.out.println("battery changed to : " + batteryLevel + "%");
                invalidate();
            }
        };

        @Override
        public void onCreate(SurfaceHolder holder) {
            super.onCreate(holder);
            /* initialize your watch face */
            setWatchFaceStyle( new WatchFaceStyle.Builder( WatchFaceService.this )
                            .setBackgroundVisibility( WatchFaceStyle.BACKGROUND_VISIBILITY_INTERRUPTIVE )
                            .setCardPeekMode( WatchFaceStyle.PEEK_MODE_SHORT )
                            .setShowSystemUiTime( false )
                            .build()
            );
            WatchFaceService.this.registerReceiver(batteryInfoReceiver, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));

            calendar = Calendar.getInstance();
            setupPaints();
        }

        @Override
        public void onSurfaceChanged (SurfaceHolder holder, int format, int width, int height) {
            BitmapFactory factory = new BitmapFactory();
            Bitmap bitmap = factory.decodeResource(getResources(), R.drawable.background);
            backgroundBitmap = Bitmap.createScaledBitmap(bitmap, width, height, false);

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

            invalidate();
            updateTimer();

        }

        @Override
        public void onApplyWindowInsets(WindowInsets insets) {
            super.onApplyWindowInsets(insets);
            boolean isRound = insets.isRound();
            chinSize = insets.getSystemWindowInsetBottom();

            if (isRound) {
                System.out.println("Chin size: " + chinSize);

            }
        }

        @Override
        public void onDraw(Canvas canvas, Rect bounds) {
            /* draw your watch face */
            canvas.drawColor(Color.TRANSPARENT , PorterDuff.Mode.CLEAR);

            //Draw background
            canvas.drawBitmap(backgroundBitmap, 0, 0, new Paint(Paint.ANTI_ALIAS_FLAG));

            int height = canvas.getHeight();
            int width = canvas.getWidth();
            int centerx = width/2;
            int centery = height/2;

            calendar.setTime(new Date());
            int mins = calendar.get(Calendar.MINUTE);
            int hours = calendar.get(Calendar.HOUR_OF_DAY);
            int day = calendar.get(Calendar.DAY_OF_MONTH);
            int month = calendar.get(Calendar.MONTH);
            int secs = calendar.get(Calendar.SECOND);

            drawTickets(canvas, centerx, centery);
            drawNumbers(canvas, centerx, centery);
            drawDate(canvas, centerx, centery, day, month);
            drawBattery(canvas, centerx, centery, batteryLevel);

            drawHourHand(canvas, centerx, centery, hours, mins);
            drawMinuteHand(canvas, centerx, centery, mins);
            if (!isInAmbientMode()) {
                drawSecondHand(canvas, centerx, centery, secs);
            }
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
            int length = 100 + backLength;

            System.out.println("Hours: " + hours);
            double angle = ((hours + (minutes / 60f)) / 6f ) * (float) Math.PI;

            double xVal = Math.sin(angle) * length;
            double yVal = -Math.cos(angle) * length;
            double backx = Math.sin(angle) * backLength;
            double backy = -Math.cos(angle) * backLength;

            canvas.drawLine((float)(centerx-backx), (float)(centery-backy), (float)(centerx+xVal), (float)(centery+yVal), hourHandColor);
        }

        private void drawMinuteHand(Canvas canvas, int centerx, int centery, int minutes) {
            int backLength = 20;
            int length = 120 + backLength;

            System.out.println("Minutes: " + minutes);
            double angle = minutes / 30f * (float) Math.PI;

            double xVal = Math.sin(angle) * length;
            double yVal = -Math.cos(angle) * length;
            double backx = Math.sin(angle) * backLength;
            double backy = -Math.cos(angle) * backLength;

            canvas.drawLine((float)(centerx - backx), (float)(centery - backy), (float)(centerx+xVal), (float)(centery+yVal), minuteHandColor);
        }

        private void drawSecondHand(Canvas canvas, int centerx, int centery, int seconds) {
            int backLength = 25;
            int length = 120 + backLength;

            System.out.println("Seconds: " + seconds);
            double angle = seconds / 30f * (float) Math.PI;

            double xVal = Math.sin(angle) * length;
            double yVal = -Math.cos(angle) * length;
            double backx = Math.sin(angle) * backLength;
            double backy = -Math.cos(angle) * backLength;

            canvas.drawLine((float)(centerx - backx), (float)(centery - backy), (float)(centerx + xVal), (float)(centery + yVal), secondHandColor);
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
                    if (i == 0)
                        canvas.drawCircle(centerx + x1, centery + y1, 6, specialLargeTicketsColor);
                    else if (chinSize > 0 && i >= 25 && i <= 35) {
                        canvas.drawCircle(centerx + x1, (2*centery) - chinSize - 10, 6, largeTicketsColor);
                    }
                    else
                        canvas.drawCircle(centerx + x1, centery + y1, 6, largeTicketsColor);

                } else { // smaller tickets
                    len = length1 + 15;
                    x1 = (float)(sinVal * len);
                    y1 = (float)(-cosVal * len);
                    x2 = (float)(sinVal * length2);
                    y2 = (float)(-cosVal * length2);

                    canvas.drawLine(centerx + x1, centery + y1, centerx + x2, centery + y2, smallTicketsColor);
                }


            }
        }

        private void drawNumbers(Canvas canvas, int centerx, int centery) {
            int len = centerx - 45;
            double[] angles = {0, 0.25*Math.PI*2, 0.5*Math.PI*2, 0.75*Math.PI*2};
            double[] sinVals = {Math.sin(angles[0]), Math.sin(angles[1]), Math.sin(angles[2]), Math.sin(angles[3])};
            double[] cosVals = {Math.cos(angles[0]), Math.cos(angles[1]), Math.cos(angles[2]), Math.cos(angles[3])};

            float x, y;
            x = (float)(sinVals[0] * len);
            y = (float)(-cosVals[0] * len);
            canvas.drawText("12", centerx + x - (numbersTextSize/2), centery + y , numbersColor);

            x = (float)(sinVals[1] * len);
            y = (float)(-cosVals[1] * len);
            canvas.drawText("3", centerx + x + 6, centery + y + 10, numbersColor);

            x = (float)(sinVals[2] * len);
            y = (float)(-cosVals[2] * len);
            canvas.drawText("6", centerx + x - (numbersTextSize/4), centery + y + 20 - chinSize, numbersColor);

            x = (float)(sinVals[3] * len);
            y = (float)(-cosVals[3] * len);
            canvas.drawText("9", centerx + x - 20, centery + y + 10, numbersColor);
        }

        private void drawMiddle(Canvas canvas, int centerx, int centery) {
            canvas.drawCircle(centerx, centery, 6, middleColor);
            canvas.drawCircle(centerx, centery, 2, middleDotColor);
        }

        private void drawDate(Canvas canvas, int centerx, int centery, int dayOfMonth, int month) {
            int height = 25;
            int width = 75;
            int left = centerx + (centerx/2) - (width/2) - 10;
            int right = centerx + (centerx/2) + (width/2) - 10;
            int top = centery - (height/2);
            int bot = centery + (height/2);
            Rect rect = new Rect(left, top, right, bot);
            canvas.drawRect(rect, dateRectColor);

            String[] monthNames = {"JAN", "FEB", "MAR", "APR", "MAY", "JUN", "JUL", "AUG", "SEP", "OCT", "NOV", "DEC"};
            canvas.drawText(String.valueOf(dayOfMonth) +" "+ monthNames[month], (float)(centerx + (centerx/2) - (width/2) - 5), (float)(centery + 7), dateTextColor);
        }

        private void drawBattery(Canvas canvas, int centerx, int centery, int batteryLevel) {
            int width = 75;
            float batteryPart = batteryLevel/100f;
            int leftEnd = centerx - (centerx/2) - (width/2) + 10;
            int rightEnd = centerx - (centerx/2) + (width/2) + 10;
            float batteryEnd = rightEnd - (width * batteryPart);

            canvas.drawLine(leftEnd, centery - 4, leftEnd, centerx + 4, batteryEmptyColor);
            canvas.drawLine(batteryEnd, centery, leftEnd, centery, batteryEmptyColor);

            canvas.drawLine(batteryEnd, centery - 4, batteryEnd, centery + 4, batteryFullColor);
            canvas.drawLine(rightEnd, centery, batteryEnd, centery, batteryFullColor);
            canvas.drawLine(rightEnd, centery - 4, rightEnd, centery + 4, batteryFullColor);
        }

        private void setupPaints() {
            hourHandColor = new Paint(Paint.ANTI_ALIAS_FLAG);
            hourHandColor.setStrokeWidth(3);
            hourHandColor.setColor(Color.WHITE);
            hourHandColor.setShadowLayer(2, 1, 0, Color.BLACK);

            minuteHandColor = new Paint(Paint.ANTI_ALIAS_FLAG);
            minuteHandColor.setStrokeWidth(3);
            minuteHandColor.setColor(Color.WHITE);
            minuteHandColor.setShadowLayer(1.5f, 0.5f, 0.5f, Color.BLACK);

            secondHandColor = new Paint(Paint.ANTI_ALIAS_FLAG);
            secondHandColor.setStrokeWidth(2);
            secondHandColor.setColor(Color.parseColor("#DA0008"));
            secondHandColor.setShadowLayer(2, 1, 0, Color.BLACK);

            largeTicketsColor = new Paint(Paint.ANTI_ALIAS_FLAG);
            largeTicketsColor.setColor(Color.parseColor("#cbcbcb"));
            specialLargeTicketsColor = new Paint(Paint.ANTI_ALIAS_FLAG);
            specialLargeTicketsColor.setColor(Color.parseColor("#A30006"));

            smallTicketsColor = new Paint(Paint.ANTI_ALIAS_FLAG);
            smallTicketsColor.setColor(Color.parseColor("#929292"));
            smallTicketsColor.setStrokeWidth(2);

            numbersColor = new Paint();
            numbersColor.setTextSize(numbersTextSize);
            numbersColor.setColor(Color.parseColor("#cbcbcb"));
            numbersColor.setShadowLayer(2, 2, 0, Color.BLACK);
            numbersColor.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));

            middleColor = new Paint();
            middleColor.setColor(Color.WHITE);
            middleDotColor = new Paint();
            middleDotColor.setColor(Color.BLACK);

            dateRectColor = new Paint();
            dateRectColor.setStyle(Paint.Style.STROKE);
            dateRectColor.setColor(Color.parseColor("#A30006"));
            dateRectColor.setShadowLayer(2, .25f, .25f, Color.BLACK);
            dateRectColor.setStrokeWidth(1.75f);

            dateTextColor = new Paint(Paint.ANTI_ALIAS_FLAG);
            dateTextColor.setColor(Color.parseColor("#cbcbcb"));
            dateTextColor.setShadowLayer(2, .75f, .75f, Color.BLACK);
            dateTextColor.setTextSize(20);

            batteryFullColor = new Paint();
            batteryFullColor.setColor(Color.parseColor("#A30006"));
            batteryFullColor.setStrokeWidth(2);
            batteryFullColor.setShadowLayer(2, -.5f, .5f, Color.BLACK);

            batteryEmptyColor = new Paint();
            batteryEmptyColor.setColor(Color.parseColor("#cbcbcb"));
            batteryEmptyColor.setStrokeWidth(1.5f);

        }
    }
}
