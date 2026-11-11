package com.virhuman.testsdk.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;
import androidx.annotation.Nullable;

import java.util.Random;
import android.view.MotionEvent;

/**
 * Simple waveform-like view: draws vertical bars whose heights are generated
 * deterministically from a seed (so same audioUrl gives same pattern).
 * Supports progress highlighting and a subtle animation when playing.
 */
public class WaveformView extends View {
    private int barCount = 28;
    private int barWidth = 6;
    private int gap = 4;
    
    private int[] heights;
    private Paint bgPaint;
    private Paint fgPaint;
    private Paint playedPaint;
    private int max = 1000;
    private int progress = 0;
    private boolean isPlaying = false;
    private long phase = 0;

    public WaveformView(Context context) {
        super(context);
        init(null);
    }

    public WaveformView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(attrs);
    }

    private void init(@Nullable AttributeSet attrs) {
        bgPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        bgPaint.setColor(0xFFECECEC); // light grey
        fgPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        fgPaint.setColor(0xFFBDBDBD); // medium grey
        playedPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        playedPaint.setColor(0xFFFF6B6B); // accent red
        generateHeights(12345); // default seed
    }

    /**
     * Generate deterministic pseudo-random heights based on seed.
     */
    public void generateHeights(long seed) {
        heights = new int[barCount];
        Random r = new Random(seed);
        for (int i = 0; i < barCount; i++) {
            // value between 30% and 100% height
            heights[i] = 30 + r.nextInt(70);
        }
        invalidate();
    }

    public void setBarCount(int count) {
        this.barCount = Math.max(4, count);
        generateHeights(System.nanoTime());
    }

    public void setSeedFromString(@Nullable String s) {
        long seed = 0;
        if (s != null) {
            for (int i = 0; i < s.length(); i++) seed = seed * 31 + s.charAt(i);
        } else seed = System.nanoTime();
        generateHeights(seed);
    }

    public void setMax(int max) {
        this.max = Math.max(1, max);
        invalidate();
    }

    public void setProgress(int progress) {
        this.progress = Math.max(0, Math.min(max, progress));
        invalidate();
    }

    public void setPlaying(boolean playing) {
        if (this.isPlaying != playing) {
            this.isPlaying = playing;
            if (playing) postInvalidateOnAnimation();
            else invalidate();
        }
    }

    // Listener for user-initiated seek/drag on the waveform
    public interface OnProgressChangeListener {
        void onProgressChanged(float progress);
    }

    private OnProgressChangeListener progressListener;

    public void setOnProgressChangeListener(OnProgressChangeListener l) {
        this.progressListener = l;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (progressListener == null || getWidth() == 0) return false;
        int action = event.getActionMasked();
        if (action == MotionEvent.ACTION_DOWN || action == MotionEvent.ACTION_MOVE || action == MotionEvent.ACTION_UP) {
            float x = event.getX();
            float frac = Math.max(0f, Math.min(1f, x / (float) getWidth()));
            try {
                progressListener.onProgressChanged(frac);
            } catch (Exception ignored) {}
            return true;
        }
        return super.onTouchEvent(event);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        int w = getWidth();
        int h = getHeight();
        if (heights == null || heights.length == 0) return;

        int totalBarWidth = barWidth + gap;
        int needed = totalBarWidth * barCount - gap;
        int startX = Math.max(0, (w - needed) / 2);

        // compute playedThreshold index
        float fraction = max > 0 ? (float) progress / (float) max : 0f;
        int playedBars = Math.round(fraction * barCount);

        // animate phase to give slight movement
        if (isPlaying) phase++;

        for (int i = 0; i < barCount; i++) {
            int left = startX + i * totalBarWidth;
            int seedVal = heights[i];
            // vary height slightly with phase
            float anim = 1f;
            if (isPlaying) anim = 0.85f + 0.15f * (float) Math.abs(Math.sin((phase + i) * 0.15));
            float hRatio = (seedVal / 100f) * anim;
            int barH = Math.max(4, (int) (h * 0.6f * hRatio));
            int top = (h - barH) / 2;

            // draw background bar
            canvas.drawRect(left, top, left + barWidth, top + barH, fgPaint);

            // overlay played color for played bars
            if (i < playedBars) {
                canvas.drawRect(left, top, left + barWidth, top + barH, playedPaint);
            }
        }

        // if playing, continue animating
        if (isPlaying) postInvalidateOnAnimation();
    }
}


