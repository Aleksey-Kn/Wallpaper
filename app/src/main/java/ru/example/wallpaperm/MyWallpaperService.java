package ru.example.wallpaperm;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Rect;
import android.os.BatteryManager;
import android.os.Build;
import android.os.Handler;
import android.service.wallpaper.WallpaperService;
import android.view.SurfaceHolder;

import androidx.annotation.RequiresApi;

import de.vogella.android.wallpaper.R;

public class MyWallpaperService extends WallpaperService {

    @Override
    public Engine onCreateEngine() {
        return new MyWallpaperEngine();
    }

    private class MyWallpaperEngine extends Engine {
        private final Handler handler = new Handler();
        private final Runnable drawRunner = new Runnable() {
            @RequiresApi(api = Build.VERSION_CODES.N)
            @Override
            public void run() {
                draw();
            }
        };
        private final List<MyPoint> circles;
        private int width;
        private int height;
        private boolean visible = true;
        private final Random random;
        private final Bitmap fish = BitmapFactory.decodeResource(getResources(), R.drawable.fish);
        private final Bitmap back = BitmapFactory.decodeResource(getResources(), R.drawable.back);
        private final Bitmap sky = BitmapFactory.decodeResource(getResources(), R.drawable.sky);
        private final Rect src = new Rect(0, 0, 160 * 2, 106 * 2);
        private int status;
        private Rect srcSky = new Rect(0, 0, 1200, (int)(1800f / 100 * (100 - status)));
        private Rect dstSky = new Rect(0, 0, width, (int)(height * (float)(100 - status) / 100));
        private Rect forBack = new Rect(0, (int)(height * (float)(100 - status) / 100), width, height);
        private final Rect srcBack = new Rect(0, 0, 1252, 1878);
        private boolean full = true;

        public MyWallpaperEngine() {
            random = new Random();
            circles = new ArrayList<>();
            Paint paint = new Paint();
            paint.setAntiAlias(true);
            paint.setColor(Color.WHITE);
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeJoin(Paint.Join.ROUND);
            paint.setStrokeWidth(10f);
            handler.post(drawRunner);
            BroadcastReceiver receiver = new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    status = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, 100);
                    srcSky = new Rect(0, 0, 1200, (int)(1800f / 100 * (100 - status)));
                    dstSky = new Rect(0, 0, width, (int)(height * (float)(100 - status) / 100));
                    forBack = new Rect(0, (int)(height * (float)(100 - status) / 100), width, height);
                    if(status > 15 && !full){
                        full = true;
                        handler.post(drawRunner);
                    } else if(status < 16){
                        full = false;
                    }
                }
            };
            registerReceiver(receiver, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
        }

        @Override
        public void onVisibilityChanged(boolean visible) {
            this.visible = visible;
            if (visible) {
                handler.post(drawRunner);
            } else {
                handler.removeCallbacks(drawRunner);
            }
        }

        @Override
        public void onSurfaceDestroyed(SurfaceHolder holder) {
            super.onSurfaceDestroyed(holder);
            this.visible = false;
            handler.removeCallbacks(drawRunner);
        }

        @Override
        public void onSurfaceChanged(SurfaceHolder holder, int format, int width, int height) {
            this.width = width;
            this.height = height;
            super.onSurfaceChanged(holder, format, width, height);
        }

        @RequiresApi(api = Build.VERSION_CODES.N)
        private void draw() {
            SurfaceHolder holder = getSurfaceHolder();
            Canvas canvas = null;
            try {
                canvas = holder.lockCanvas();
                if (canvas != null) {
                    circles.forEach(MyPoint::step);
                    circles.removeIf(o -> o.getX() > width);
                    if((random.nextInt() & 31) == 0){
                        circles.add(new MyPoint(0,
                                (int)(height - Math.abs(random.nextInt()) % status * (float)height / 100)));
                    }
                    drawCircles(canvas, circles);
                }
            } finally {
                if (canvas != null)
                    holder.unlockCanvasAndPost(canvas);
            }
            handler.removeCallbacks(drawRunner);
            if (visible && full) {
                handler.postDelayed(drawRunner, 33);
            }
        }

        // Surface view requires that all elements are drawn completely
        private void drawCircles(Canvas canvas, List<MyPoint> circles) {
            canvas.drawBitmap(sky, srcSky, dstSky, null);
            canvas.drawBitmap(back, srcBack, forBack, null);
            Rect dst;
            for (MyPoint point : circles) {
                dst = new Rect(point.getX(), point.getY(), point.getX() + 160, point.getY() + 106);
                canvas.drawBitmap((full? fish: rotateBitmap(fish, 180)), src, dst, null);
           }
        }

        public Bitmap rotateBitmap(Bitmap source, float angle)
        {
            Matrix matrix = new Matrix();
            matrix.postRotate(angle);
            return Bitmap.createBitmap(source, 0, 0, source.getWidth(), source.getHeight(), matrix, true);
        }
    }
}
