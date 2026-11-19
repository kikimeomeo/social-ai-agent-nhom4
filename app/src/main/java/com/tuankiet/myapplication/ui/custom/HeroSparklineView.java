package com.tuankiet.myapplication.ui.custom;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.Nullable;

/**
 * Draws the minimalist white sparkline curve with circular nodes inside the Home Hero Card.
 */
public class HeroSparklineView extends View {

    private final Paint linePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint dotPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path linePath = new Path();

    // Normalized Y values (0 = bottom, 1 = top) matching T2..CN trend
    private final float[] points = {0.20f, 0.38f, 0.30f, 0.56f, 0.48f, 0.68f, 0.78f};

    public HeroSparklineView(Context context) {
        super(context);
        init();
    }

    public HeroSparklineView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public HeroSparklineView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        float density = getResources().getDisplayMetrics().density;
        linePaint.setColor(Color.argb(235, 255, 255, 255));
        linePaint.setStyle(Paint.Style.STROKE);
        linePaint.setStrokeWidth(2.4f * density);
        linePaint.setStrokeCap(Paint.Cap.ROUND);
        linePaint.setStrokeJoin(Paint.Join.ROUND);

        dotPaint.setColor(Color.WHITE);
        dotPaint.setStyle(Paint.Style.FILL);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        float w = getWidth();
        float h = getHeight();
        if (w <= 0 || h <= 0) return;

        float density = getResources().getDisplayMetrics().density;
        float padX = 8f * density;
        float padY = 6f * density;
        float usableW = w - padX * 2f;
        float usableH = h - padY * 2f;

        linePath.reset();
        float[] xs = new float[points.length];
        float[] ys = new float[points.length];

        for (int i = 0; i < points.length; i++) {
            xs[i] = padX + (usableW * i) / (points.length - 1);
            ys[i] = padY + usableH * (1f - points[i]);
            if (i == 0) {
                linePath.moveTo(xs[i], ys[i]);
            } else {
                linePath.lineTo(xs[i], ys[i]);
            }
        }

        canvas.drawPath(linePath, linePaint);

        // Draw circular nodes at T2, T4, T6, and larger highlighted node at CN
        int[] dotIndices = {0, 2, 4, 6};
        for (int idx : dotIndices) {
            float radius = (idx == 6 ? 4.2f : 3.0f) * density;
            canvas.drawCircle(xs[idx], ys[idx], radius, dotPaint);
        }
    }
}
