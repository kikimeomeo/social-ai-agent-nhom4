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
        axisPaint.setColor(Color.parseColor("#E2E8F0"));
        axisPaint.setStrokeWidth(2f);

        gridPaint.setColor(Color.parseColor("#F1F5F9"));
        gridPaint.setStrokeWidth(1.5f);

        labelPaint.setColor(Color.parseColor("#64748B"));
        labelPaint.setTextSize(spToPx(11));
        labelPaint.setTextAlign(Paint.Align.CENTER);

        fbBarPaint.setColor(Color.parseColor("#1877F2"));
        liBarPaint.setColor(Color.parseColor("#0A66C2"));

        linePaint.setColor(Color.parseColor("#4F46E5"));
        linePaint.setStrokeWidth(4f);
        linePaint.setStyle(Paint.Style.STROKE);

        lineFillPaint.setStyle(Paint.Style.FILL);

        pointPaint.setColor(Color.parseColor("#4F46E5"));
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

        float paddingLeft = dpToPx(36);
        float paddingRight = dpToPx(16);
        float paddingTop = dpToPx(24);
        float paddingBottom = dpToPx(36);

        float chartWidth = width - paddingLeft - paddingRight;
        float chartHeight = height - paddingTop - paddingBottom;

        // Draw horizontal grid lines (3 lines: 0%, 50%, 100%)
        for (int i = 0; i <= 3; i++) {
            float y = paddingTop + chartHeight * (1f - (float) i / 3f);
            canvas.drawLine(paddingLeft, y, width - paddingRight, y, gridPaint);

            int valLabel = (int) (maxVal * (float) i / 3f);
            labelPaint.setTextAlign(Paint.Align.RIGHT);
            canvas.drawText(String.valueOf(valLabel), paddingLeft - dpToPx(6), y + dpToPx(4), labelPaint);
        }

        // Base Axis
        canvas.drawLine(paddingLeft, paddingTop + chartHeight, width - paddingRight, paddingTop + chartHeight, axisPaint);

        // Draw bars and points for each day
        int n = data.size();
        float slotWidth = chartWidth / n;
        float barWidth = dpToPx(10);

        Path linePath = new Path();
        Path fillPath = new Path();
        fillPath.moveTo(paddingLeft, paddingTop + chartHeight);

        for (int i = 0; i < n; i++) {
            AnalyticsData.DayMetric m = data.get(i);
            float centerX = paddingLeft + slotWidth * i + slotWidth / 2f;

            // Facebook bar
            float fbRatio = (float) m.getFacebookEngagement() / maxVal;
            float fbH = chartHeight * fbRatio;
            float fbLeft = centerX - barWidth - dpToPx(2);
            float fbTop = paddingTop + chartHeight - fbH;
            RectF fbRect = new RectF(fbLeft, fbTop, fbLeft + barWidth, paddingTop + chartHeight);
            canvas.drawRoundRect(fbRect, dpToPx(4), dpToPx(4), fbBarPaint);

            // LinkedIn bar
            float liRatio = (float) m.getLinkedinEngagement() / maxVal;
            float liH = chartHeight * liRatio;
            float liLeft = centerX + dpToPx(2);
            float liTop = paddingTop + chartHeight - liH;
            RectF liRect = new RectF(liLeft, liTop, liLeft + barWidth, paddingTop + chartHeight);
            canvas.drawRoundRect(liRect, dpToPx(4), dpToPx(4), liBarPaint);

            // Day label
            labelPaint.setTextAlign(Paint.Align.CENTER);
            canvas.drawText(m.getDayLabel(), centerX, height - dpToPx(12), labelPaint);

            // Total indicator point on top of bars
            float totalRatio = (float) (m.getFacebookEngagement() + m.getLinkedinEngagement()) / (maxVal * 1.8f);
            float totalY = paddingTop + chartHeight - (chartHeight * Math.min(1.0f, totalRatio));
            if (i == 0) {
                linePath.moveTo(centerX, totalY);
                fillPath.lineTo(centerX, totalY);
            } else {
                linePath.lineTo(centerX, totalY);
                fillPath.lineTo(centerX, totalY);
            }

            // Draw point
            canvas.drawCircle(centerX, totalY, dpToPx(4), pointPaint);
        }

        // Close fill path
        fillPath.lineTo(paddingLeft + chartWidth, paddingTop + chartHeight);
        fillPath.close();

        // Gradient for fill
        Shader gradient = new LinearGradient(0, paddingTop, 0, paddingTop + chartHeight,
                Color.parseColor("#334F46E5"), Color.parseColor("#054F46E5"), Shader.TileMode.CLAMP);
        lineFillPaint.setShader(gradient);
        canvas.drawPath(fillPath, lineFillPaint);
        canvas.drawPath(linePath, linePaint);
    }

    private float dpToPx(float dp) {
        return dp * getResources().getDisplayMetrics().density;
    }

    private float spToPx(float sp) {
        return sp * getResources().getDisplayMetrics().scaledDensity;
    }
}

