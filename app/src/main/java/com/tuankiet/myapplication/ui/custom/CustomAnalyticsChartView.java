package com.tuankiet.myapplication.ui.custom;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.Shader;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.Nullable;

import com.tuankiet.myapplication.model.AnalyticsData;

import java.util.ArrayList;
import java.util.List;

public class CustomAnalyticsChartView extends View {

    private final Paint axisPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint gridPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint labelPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint fbBarPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint liBarPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint linePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint lineFillPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint pointPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    private List<AnalyticsData.DayMetric> data = new ArrayList<>();
    private int maxVal = 1000;

    public CustomAnalyticsChartView(Context context) {
        super(context);
        init();
    }

    public CustomAnalyticsChartView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public CustomAnalyticsChartView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        axisPaint.setColor(Color.parseColor("#EAE5DC"));
        axisPaint.setStrokeWidth(2f);

        gridPaint.setColor(Color.parseColor("#F3EFE7"));
        gridPaint.setStrokeWidth(1.5f);

        labelPaint.setColor(Color.parseColor("#78716C"));
        labelPaint.setTextSize(spToPx(11));
        labelPaint.setTextAlign(Paint.Align.CENTER);

        fbBarPaint.setColor(Color.parseColor("#1877F2"));
        liBarPaint.setColor(Color.parseColor("#1C1917"));

        linePaint.setColor(Color.parseColor("#0284C7"));
        linePaint.setStrokeWidth(4f);
        linePaint.setStyle(Paint.Style.STROKE);

        lineFillPaint.setStyle(Paint.Style.FILL);

        pointPaint.setColor(Color.parseColor("#0284C7"));
        pointPaint.setStyle(Paint.Style.FILL);
    }

    public void setData(List<AnalyticsData.DayMetric> metrics) {
        if (metrics == null || metrics.isEmpty()) return;
        this.data = metrics;
        maxVal = 100;
        for (AnalyticsData.DayMetric m : metrics) {
            int top = Math.max(m.getFacebookEngagement(), m.getLinkedinEngagement());
            if (top > maxVal) {
                maxVal = top;
            }
        }
        maxVal = (int) (maxVal * 1.25); // headroom for clean top
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        int width = getWidth();
        int height = getHeight();
        if (width <= 0 || height <= 0 || data.isEmpty()) return;

        float paddingLeft = dpToPx(14);
        float paddingRight = dpToPx(14);
        float paddingTop = dpToPx(12);
        float paddingBottom = dpToPx(28);

        float chartWidth = width - paddingLeft - paddingRight;
        float chartHeight = height - paddingTop - paddingBottom;

        // Draw clean trend line with soft Ocean Blue fill (no noisy Y-axis labels/grid, matching reference)
        int n = data.size();
        float stepX = n > 1 ? chartWidth / (n - 1) : chartWidth;

        Path linePath = new Path();
        Path fillPath = new Path();

        float lastX = paddingLeft;
        float lastY = paddingTop + chartHeight;

        for (int i = 0; i < n; i++) {
            AnalyticsData.DayMetric m = data.get(i);
            float centerX = paddingLeft + i * stepX;

            float totalRatio = (float) (m.getFacebookEngagement() + m.getLinkedinEngagement()) / (maxVal * 1.8f);
            float totalY = paddingTop + chartHeight - (chartHeight * Math.min(1.0f, totalRatio));

            if (i == 0) {
                linePath.moveTo(centerX, totalY);
                fillPath.moveTo(centerX, paddingTop + chartHeight);
                fillPath.lineTo(centerX, totalY);
            } else {
                linePath.lineTo(centerX, totalY);
                fillPath.lineTo(centerX, totalY);
            }
            lastX = centerX;
            lastY = totalY;

            // Day label (highlight last day CN with primary pill)
            labelPaint.setTextAlign(Paint.Align.CENTER);
            if (i == n - 1) {
                RectF pillRect = new RectF(centerX - dpToPx(14), height - dpToPx(22), centerX + dpToPx(14), height - dpToPx(6));
                canvas.drawRoundRect(pillRect, dpToPx(8), dpToPx(8), pointPaint);
                Paint pillTextPaint = new Paint(labelPaint);
                pillTextPaint.setColor(Color.WHITE);
                pillTextPaint.setFakeBoldText(true);
                canvas.drawText(m.getDayLabel(), centerX, height - dpToPx(10), pillTextPaint);
            } else {
                canvas.drawText(m.getDayLabel(), centerX, height - dpToPx(10), labelPaint);
            }
        }

        fillPath.lineTo(lastX, paddingTop + chartHeight);
        fillPath.close();

        Shader gradient = new LinearGradient(0, paddingTop, 0, paddingTop + chartHeight,
                Color.parseColor("#260284C7"), Color.parseColor("#000284C7"), Shader.TileMode.CLAMP);
        lineFillPaint.setShader(gradient);
        canvas.drawPath(fillPath, lineFillPaint);
        canvas.drawPath(linePath, linePaint);

        // White-bordered end dot
        Paint haloPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        haloPaint.setColor(Color.WHITE);
        canvas.drawCircle(lastX, lastY, dpToPx(6), haloPaint);
        canvas.drawCircle(lastX, lastY, dpToPx(4.2f), pointPaint);
    }

    private float dpToPx(float dp) {
        return dp * getResources().getDisplayMetrics().density;
    }

    private float spToPx(float sp) {
        return sp * getResources().getDisplayMetrics().scaledDensity;
    }
}

