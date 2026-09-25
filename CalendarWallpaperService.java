package com.garmentsquality.livecalendar;

import android.graphics.*;
import android.graphics.drawable.*;
import android.os.*;
import android.service.wallpaper.WallpaperService;
import android.view.SurfaceHolder;
import java.text.SimpleDateFormat;
import java.util.*;

public class CalendarWallpaperService extends WallpaperService {
    @Override public Engine onCreateEngine() { return new CalendarEngine(); }

    class CalendarEngine extends Engine {
        private final Handler handler = new Handler(Looper.getMainLooper());
        private final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        private Bitmap photo, logo;
        private boolean visible = true;

        private final Runnable drawTask = new Runnable() {
            @Override public void run() {
                draw();
                if (visible) handler.postDelayed(this, 30_000);
            }
        };

        CalendarEngine() {
            photo = load(com.garmentsquality.livecalendar.R.drawable.user_photo);
            logo = load(com.garmentsquality.livecalendar.R.drawable.garments_quality_logo);
        }

        Bitmap load(int id) {
            return BitmapFactory.decodeResource(getResources(), id);
        }

        @Override public void onVisibilityChanged(boolean v) {
            visible = v;
            if (v) handler.post(drawTask); else handler.removeCallbacks(drawTask);
        }

        @Override public void onSurfaceChanged(SurfaceHolder h, int f, int w, int he) {
            super.onSurfaceChanged(h,f,w,he);
            draw();
        }

        @Override public void onSurfaceDestroyed(SurfaceHolder h) {
            visible = false;
            handler.removeCallbacks(drawTask);
            super.onSurfaceDestroyed(h);
        }

        private void draw() {
            SurfaceHolder h = getSurfaceHolder();
            Canvas c = null;
            try {
                c = h.lockCanvas();
                if (c == null) return;
                int w = c.getWidth(), he = c.getHeight();
                p.setStyle(Paint.Style.FILL);

                // Background photo, cropped to screen.
                Rect src = centerCrop(photo, w, he);
                c.drawBitmap(photo, src, new Rect(0,0,w,he), p);

                // Dark translucent overlay for readability.
                p.setColor(Color.argb(125, 0, 15, 35));
                c.drawRect(0,0,w,he,p);

                // Logo in top-right corner.
                int logoSize = Math.max(120, w / 5);
                RectF lr = new RectF(w-logoSize-28, 28, w-28, 28+logoSize);
                c.drawBitmap(logo, null, lr, p);

                Calendar now = Calendar.getInstance();
                String time = new SimpleDateFormat("hh:mm:ss a", Locale.ENGLISH).format(now.getTime());
                String enDate = new SimpleDateFormat("dd MMMM yyyy", Locale.ENGLISH).format(now.getTime());
                String enDay = new SimpleDateFormat("EEEE", Locale.ENGLISH).format(now.getTime());

                // Header
                p.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
                text(c, "GARMENTS QUALITY", 28, 42, 48, Color.WHITE);
                text(c, "LIVE CALENDAR", 28, 42, 83, Color.rgb(150, 235, 90));

                // Live time
                p.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
                text(c, time, w/2f, 34, 130, Color.WHITE, Paint.Align.CENTER);

                // English
                roundPanel(c, 22, 170, w-22, 255, Color.argb(210, 7, 26, 51));
                text(c, "English", 42, 30, 201, Color.rgb(255,210,70));
                text(c, enDate + " — " + enDay, 42, 24, 235, Color.WHITE);

                // Bangla date (Bangladesh calendar approximation)
                int[] bd = banglaDate(now);
                String[] banglaMonths = {"বৈশাখ","জ্যৈষ্ঠ","আষাঢ়","শ্রাবণ","ভাদ্র","আশ্বিন","কার্তিক","অগ্রহায়ণ","পৌষ","মাঘ","ফাল্গুন","চৈত্র"};
                String bnDate = bn(bd[0]) + " " + banglaMonths[bd[2]-1] + " " + bn(bd[1]);
                String bnDay = banglaDay(now.get(Calendar.DAY_OF_WEEK));
                roundPanel(c, 22, 272, w-22, 365, Color.argb(210, 7, 26, 51));
                text(c, "বাংলা", 42, 30, 304, Color.rgb(255,210,70));
                text(c, bnDate + " — " + bnDay, 42, 24, 342, Color.WHITE);

                // Hijri (civil calculation)
                int[] hij = hijri(now.get(Calendar.YEAR), now.get(Calendar.MONTH)+1, now.get(Calendar.DAY_OF_MONTH));
                String hijDate = bn(hij[0]) + " " + hijriMonth(hij[1]) + " " + bn(hij[2]) + " হিজরি";
                String hijDay = arabicDayBangla(now.get(Calendar.DAY_OF_WEEK));
                roundPanel(c, 22, 382, w-22, 485, Color.argb(210, 7, 26, 51));
                text(c, "🌙 হিজরি", 42, 30, 414, Color.rgb(255,210,70));
                text(c, hijDate, 42, 23, 451, Color.WHITE);
                text(c, hijDay, 42, 21, 477, Color.rgb(150, 235, 90));

                // Footer
                roundPanel(c, 22, he-125, w-22, he-25, Color.argb(220, 7, 26, 51));
                text(c, "শৃঙ্খলা • সততা • গুণগত মান", w/2f, 25, he-92, Color.WHITE, Paint.Align.CENTER);
                text(c, "Learn • Inspect • Improve", w/2f, 23, he-55, Color.rgb(255,210,70), Paint.Align.CENTER);

            } finally {
                if (c != null) h.unlockCanvasAndPost(c);
            }
        }

        private void roundPanel(Canvas c, float l,float t,float r,float b,int color) {
            p.setColor(color); p.setStyle(Paint.Style.FILL);
            c.drawRoundRect(new RectF(l,t,r,b), 24,24,p);
        }

        private void text(Canvas c,String s,float x,float size,float y,int color) {
            text(c,s,x,size,y,color,Paint.Align.LEFT);
        }
        private void text(Canvas c,String s,float x,float size,float y,int color,Paint.Align align) {
            p.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.NORMAL));
            p.setTextSize(size); p.setColor(color); p.setTextAlign(align); p.setStyle(Paint.Style.FILL);
            c.drawText(s,x,y,p);
        }

        private Rect centerCrop(Bitmap b, int w, int h) {
            float scale = Math.max((float)w/b.getWidth(), (float)h/b.getHeight());
            int sw = Math.round(w/scale), sh = Math.round(h/scale);
            int left=(b.getWidth()-sw)/2, top=(b.getHeight()-sh)/2;
            return new Rect(left,top,left+sw,top+sh);
        }

        private String bn(int n) {
            String s = String.valueOf(n);
            return s.replace('0','০').replace('1','১').replace('2','২').replace('3','৩')
                    .replace('4','৪').replace('5','৫').replace('6','৬').replace('7','৭')
                    .replace('8','৮').replace('9','৯');
        }

        private String banglaDay(int d) {
            String[] a={"","রবিবার","সোমবার","মঙ্গলবার","বুধবার","বৃহস্পতিবার","শুক্রবার","শনিবার"};
            return a[d];
        }

        private String arabicDayBangla(int d) {
            String[] a={"","ইয়াওমুল আহাদ","ইয়াওমুল ইসনাইন","ইয়াওমুস সালাসা","ইয়াওমুল আরবিআ","ইয়াওমুল খামিস","ইয়াওমুল জুমু‘আহ","ইয়াওমুস সাবত"};
            return a[d];
        }

        private String hijriMonth(int m) {
            String[] a={"","মুহররম","সফর","রবিউল আউয়াল","রবিউস সানি","জমাদিউল আউয়াল","জমাদিউস সানি","রজব","শাবান","রমজান","শাওয়াল","জিলকদ","জিলহজ"};
            return a[m];
        }

        // Bangladesh Bangla calendar (modern fixed-month approximation).
        private int[] banglaDate(Calendar g) {
            int y=g.get(Calendar.YEAR), m=g.get(Calendar.MONTH)+1, d=g.get(Calendar.DAY_OF_MONTH);
            int by = y - 593;
            int doy=g.get(Calendar.DAY_OF_YEAR);
            boolean leap = new GregorianCalendar(y, Calendar.FEBRUARY, 29).get(Calendar.DAY_OF_MONTH)==29;
            int start = new GregorianCalendar(y, Calendar.APRIL, 14).get(Calendar.DAY_OF_YEAR);
            if (doy < start) { by--; start = new GregorianCalendar(y-1, Calendar.APRIL, 14).get(Calendar.DAY_OF_YEAR); doy += (new GregorianCalendar(y-1, Calendar.DECEMBER,31).get(Calendar.DAY_OF_YEAR)); }
            int delta=doy-start;
            int month=1, day;
            int[] lens={31,31,31,31,31,30,30,30,30,30,30,30};
            if (leap) lens[10]=31;
            while (month<=12 && delta>=lens[month-1]) { delta-=lens[month-1]; month++; }
            day=delta+1;
            String[] names={"বৈশাখ","জ্যৈষ্ঠ","আষাঢ়","শ্রাবণ","ভাদ্র","আশ্বিন","কার্তিক","অগ্রহায়ণ","পৌষ","মাঘ","ফাল্গুন","চৈত্র"};
            return new int[]{day, by, month};
        }

        private int[] hijri(int gy,int gm,int gd) {
            long jd = julianDay(gy,gm,gd);
            long l = jd - 1948440 + 10632;
            long n = (l - 1) / 10631;
            l = l - 10631*n + 354;
            long j = ((10985-l)/5316)*((50*l)/17719) + (l/5670)*((43*l)/15238);
            l = l - ((30-j)/15)*((17719*j)/50) - (j/16)*((15238*j)/43) + 29;
            int m = (int)((24*l)/709);
            int d = (int)(l - (709*m)/24);
            int y = (int)(30*n + j - 30);
            return new int[]{d,m,y};
        }

        private long julianDay(int y,int m,int d) {
            int a=(14-m)/12, yy=y+4800-a, mm=m+12*a-3;
            return d + (153*mm+2)/5 + 365L*yy + yy/4 - yy/100 + yy/400 - 32045;
        }
    }
}
