package com.umrefill.app.ui.floorguide;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.DashPathEffect;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.os.SystemClock;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.animation.LinearInterpolator;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatImageView;
import androidx.core.content.ContextCompat;

import com.umrefill.app.R;
import com.umrefill.app.data.entity.EntryPoint;
import com.umrefill.app.data.entity.PointStatus;
import com.umrefill.app.data.entity.RefillPoint;
import com.umrefill.app.util.pathfinding.GraphNode;

import java.util.ArrayList;
import java.util.List;

/**
 * Interactive 2D Floor Plan view with animated directional pathway, pulsing target ring,
 * Okabe-Ito status pins, and throttled touch/drag starting-point repositioning (FR3.1, FR3.2).
 */
public class DynamicMapView extends AppCompatImageView {

    public interface OnPointPinClickListener {
        void onPointPinClicked(@NonNull RefillPoint point);
    }

    public interface OnCustomEntryMoveListener {
        void onCustomEntryMoved(float xPct, float yPct);
    }

    private static final float MIN_ZOOM = 1.0f;
    private static final float MAX_ZOOM = 4.0f;
    private static final long DRAG_ROUTE_THROTTLE_MS = 90L;

    private int colorUmNavy;
    private int colorUmGold;
    private int colorStatusAvailable;
    private int colorStatusEmpty;
    private int colorStatusFaulty;
    private int colorStatusUnknownOutline;
    private int colorSurfaceGrid;
    private int colorOutlineVariant;

    private final List<RefillPoint> points = new ArrayList<>();
    private final List<GraphNode> currentPath = new ArrayList<>();
    @Nullable
    private EntryPoint selectedEntry;
    private long highlightedPointId = -1L;

    @Nullable
    private OnPointPinClickListener pinClickListener;
    @Nullable
    private OnCustomEntryMoveListener customEntryMoveListener;

    private final Paint pathOutlinePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint pathStrokePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint chevronPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint pulsePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint pinFillPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint pinStrokePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint iconPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint labelPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint labelBgPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint fallbackGridPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    private final Path routePath = new Path();
    private final RectF baseImageBounds = new RectF();

    private float pinVisualRadiusPx;
    private float pinSelectedRadiusPx;
    private float pinTouchTargetRadiusPx;
    private float dashLengthPx;
    private float dashGapPx;
    private float animationProgress = 0f;
    @Nullable
    private ValueAnimator flowAnimator;

    // Zoom & Pan state
    private float zoomScale = 1.0f;
    private float panX = 0f;
    private float panY = 0f;
    private float lastTouchX;
    private float lastTouchY;
    private boolean isPanningOrScaling = false;
    private boolean hasPendingDragRoute = false;
    private long lastDragRouteCalcUptimeMs = 0L;

    private ScaleGestureDetector scaleDetector;
    private GestureDetector gestureDetector;

    public DynamicMapView(@NonNull Context context) {
        super(context);
        init(context);
    }

    public DynamicMapView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public DynamicMapView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    private void init(@NonNull Context context) {
        setScaleType(ScaleType.FIT_CENTER);

        colorUmNavy = ContextCompat.getColor(context, R.color.umr_navy);
        colorUmGold = ContextCompat.getColor(context, R.color.umr_gold);
        colorStatusAvailable = ContextCompat.getColor(context, R.color.umr_status_available_fill);
        colorStatusEmpty = ContextCompat.getColor(context, R.color.umr_status_empty_fill);
        colorStatusFaulty = ContextCompat.getColor(context, R.color.umr_status_faulty_fill);
        colorStatusUnknownOutline = ContextCompat.getColor(context, R.color.umr_status_unknown_outline);
        colorSurfaceGrid = ContextCompat.getColor(context, R.color.umr_surface_grid);
        colorOutlineVariant = ContextCompat.getColor(context, R.color.umr_outline_variant);

        pinVisualRadiusPx = dpToPx(context, 14f);
        pinSelectedRadiusPx = dpToPx(context, 18f);
        pinTouchTargetRadiusPx = dpToPx(context, 24f);
        dashLengthPx = dpToPx(context, 9f);
        dashGapPx = dpToPx(context, 6f);

        pathOutlinePaint.setStyle(Paint.Style.STROKE);
        pathOutlinePaint.setColor(colorUmNavy);
        pathOutlinePaint.setStrokeWidth(dpToPx(context, 6.5f));
        pathOutlinePaint.setStrokeCap(Paint.Cap.ROUND);
        pathOutlinePaint.setStrokeJoin(Paint.Join.ROUND);

        pathStrokePaint.setStyle(Paint.Style.STROKE);
        pathStrokePaint.setColor(colorUmGold);
        pathStrokePaint.setStrokeWidth(dpToPx(context, 3.5f));
        pathStrokePaint.setStrokeCap(Paint.Cap.ROUND);
        pathStrokePaint.setStrokeJoin(Paint.Join.ROUND);

        chevronPaint.setStyle(Paint.Style.STROKE);
        chevronPaint.setColor(Color.WHITE);
        chevronPaint.setStrokeWidth(dpToPx(context, 2.0f));
        chevronPaint.setStrokeCap(Paint.Cap.ROUND);
        chevronPaint.setStrokeJoin(Paint.Join.ROUND);

        pulsePaint.setStyle(Paint.Style.FILL);
        pulsePaint.setColor(colorStatusAvailable);

        pinStrokePaint.setStyle(Paint.Style.STROKE);
        pinStrokePaint.setStrokeWidth(dpToPx(context, 2.2f));

        iconPaint.setStyle(Paint.Style.STROKE);
        iconPaint.setColor(Color.WHITE);
        iconPaint.setStrokeWidth(dpToPx(context, 2.2f));
        iconPaint.setStrokeCap(Paint.Cap.ROUND);
        iconPaint.setStrokeJoin(Paint.Join.ROUND);

        labelPaint.setColor(Color.WHITE);
        labelPaint.setTextSize(dpToPx(context, 10.5f));
        labelPaint.setFakeBoldText(true);
        labelPaint.setTextAlign(Paint.Align.CENTER);

        labelBgPaint.setStyle(Paint.Style.FILL);
        labelBgPaint.setColor(colorUmNavy);

        fallbackGridPaint.setStyle(Paint.Style.STROKE);
        fallbackGridPaint.setColor(colorSurfaceGrid);
        fallbackGridPaint.setStrokeWidth(dpToPx(context, 1.5f));

        scaleDetector = new ScaleGestureDetector(context, new ScaleGestureDetector.SimpleOnScaleGestureListener() {
            @Override
            public boolean onScale(@NonNull ScaleGestureDetector detector) {
                float factor = detector.getScaleFactor();
                float prevScale = zoomScale;
                zoomScale = Math.max(MIN_ZOOM, Math.min(MAX_ZOOM, zoomScale * factor));
                float focusX = detector.getFocusX();
                float focusY = detector.getFocusY();
                panX = focusX - ((focusX - panX) * (zoomScale / prevScale));
                panY = focusY - ((focusY - panY) * (zoomScale / prevScale));
                clampPan();
                invalidate();
                return true;
            }
        });

        gestureDetector = new GestureDetector(context, new GestureDetector.SimpleOnGestureListener() {
            @Override
            public boolean onDoubleTap(@NonNull MotionEvent e) {
                if (zoomScale < 1.8f) {
                    float prevScale = zoomScale;
                    zoomScale = 2.5f;
                    panX = e.getX() - ((e.getX() - panX) * (zoomScale / prevScale));
                    panY = e.getY() - ((e.getY() - panY) * (zoomScale / prevScale));
                } else {
                    resetZoom();
                }
                clampPan();
                invalidate();
                return true;
            }

            @Override
            public boolean onSingleTapConfirmed(@NonNull MotionEvent e) {
                computeBaseImageBounds();
                float canvasX = viewToCanvasX(e.getX());
                float canvasY = viewToCanvasY(e.getY());

                RefillPoint tapped = findTappedPoint(canvasX, canvasY);
                if (tapped != null && pinClickListener != null) {
                    performClick();
                    pinClickListener.onPointPinClicked(tapped);
                    return true;
                }
                if (baseImageBounds.contains(canvasX, canvasY)) {
                    notifyCustomEntryMoved(canvasX, canvasY);
                    return true;
                }
                return false;
            }
        });
    }

    public void resetZoom() {
        zoomScale = 1.0f;
        panX = 0f;
        panY = 0f;
        invalidate();
    }

    private void clampPan() {
        if (zoomScale <= 1.01f) {
            panX = 0f;
            panY = 0f;
            return;
        }
        float maxPanX = 0f;
        float minPanX = getWidth() * (1f - zoomScale);
        float maxPanY = 0f;
        float minPanY = getHeight() * (1f - zoomScale);
        panX = Math.max(minPanX, Math.min(maxPanX, panX));
        panY = Math.max(minPanY, Math.min(maxPanY, panY));
    }

    public void setOnPointPinClickListener(@Nullable OnPointPinClickListener listener) {
        this.pinClickListener = listener;
    }

    public void setOnCustomEntryMoveListener(@Nullable OnCustomEntryMoveListener listener) {
        this.customEntryMoveListener = listener;
    }

    public void setFloorData(
            @NonNull List<RefillPoint> floorPoints,
            @Nullable EntryPoint entryPoint,
            @NonNull List<GraphNode> pathNodes,
            long nearestWorkingPointId) {
        this.points.clear();
        this.points.addAll(floorPoints);
        this.selectedEntry = entryPoint;
        this.currentPath.clear();
        this.currentPath.addAll(pathNodes);
        this.highlightedPointId = nearestWorkingPointId;
        startFlowAnimationIfNeeded();
        invalidate();
    }

    private void startFlowAnimationIfNeeded() {
        if (flowAnimator != null && flowAnimator.isRunning()) return;
        flowAnimator = ValueAnimator.ofFloat(0f, 1f);
        flowAnimator.setDuration(1100L);
        flowAnimator.setRepeatCount(ValueAnimator.INFINITE);
        flowAnimator.setInterpolator(new LinearInterpolator());
        flowAnimator.addUpdateListener(anim -> {
            animationProgress = (float) anim.getAnimatedValue();
            if (!currentPath.isEmpty()) {
                invalidate();
            }
        });
        flowAnimator.start();
    }

    @Override
    protected void onDetachedFromWindow() {
        if (flowAnimator != null) {
            flowAnimator.cancel();
            flowAnimator = null;
        }
        super.onDetachedFromWindow();
    }

    @Override
    protected void onDraw(@NonNull Canvas canvas) {
        computeBaseImageBounds();

        canvas.save();
        canvas.translate(panX, panY);
        canvas.scale(zoomScale, zoomScale);

        super.onDraw(canvas);

        if (getDrawable() == null) {
            drawFallbackArchitecturalGrid(canvas);
        }

        drawComputedPathway(canvas);
        drawEntryMarker(canvas);
        drawRefillPins(canvas);

        canvas.restore();
    }

    private void computeBaseImageBounds() {
        Drawable drawable = getDrawable();
        int viewWidth = getWidth() - getPaddingLeft() - getPaddingRight();
        int viewHeight = getHeight() - getPaddingTop() - getPaddingBottom();

        if (drawable == null || drawable.getIntrinsicWidth() <= 0 || drawable.getIntrinsicHeight() <= 0) {
            float margin = dpToPx(getContext(), 8f);
            baseImageBounds.set(
                    getPaddingLeft() + margin,
                    getPaddingTop() + margin,
                    getPaddingLeft() + viewWidth - margin,
                    getPaddingTop() + viewHeight - margin
            );
            return;
        }

        Matrix matrix = getImageMatrix();
        baseImageBounds.set(0, 0, drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight());
        matrix.mapRect(baseImageBounds);
        baseImageBounds.offset(getPaddingLeft(), getPaddingTop());
    }

    private void drawFallbackArchitecturalGrid(@NonNull Canvas canvas) {
        pinFillPaint.setStyle(Paint.Style.FILL);
        pinFillPaint.setColor(Color.WHITE);
        canvas.drawRoundRect(baseImageBounds, dpToPx(getContext(), 12f), dpToPx(getContext(), 12f), pinFillPaint);

        int steps = 10;
        for (int i = 1; i < steps; i++) {
            float x = baseImageBounds.left + (baseImageBounds.width() * i / steps);
            float y = baseImageBounds.top + (baseImageBounds.height() * i / steps);
            canvas.drawLine(x, baseImageBounds.top, x, baseImageBounds.bottom, fallbackGridPaint);
            canvas.drawLine(baseImageBounds.left, y, baseImageBounds.right, y, fallbackGridPaint);
        }
        pinStrokePaint.setColor(colorOutlineVariant);
        canvas.drawRoundRect(baseImageBounds, dpToPx(getContext(), 12f), dpToPx(getContext(), 12f), pinStrokePaint);
    }

    private void drawComputedPathway(@NonNull Canvas canvas) {
        if (currentPath.size() < 2) return;
        routePath.reset();
        GraphNode first = currentPath.get(0);
        routePath.moveTo(pctToX(first.xPct), pctToY(first.yPct));

        for (int i = 1; i < currentPath.size(); i++) {
            GraphNode node = currentPath.get(i);
            routePath.lineTo(pctToX(node.xPct), pctToY(node.yPct));
        }

        float invScale = 1.0f / Math.max(1.0f, (float) Math.sqrt(zoomScale));
        pathOutlinePaint.setStrokeWidth(dpToPx(getContext(), 6.5f) * invScale);
        pathStrokePaint.setStrokeWidth(dpToPx(getContext(), 3.5f) * invScale);

        float period = (dashLengthPx + dashGapPx) * invScale;
        float phase = -(animationProgress * period);
        pathStrokePaint.setPathEffect(new DashPathEffect(
                new float[]{dashLengthPx * invScale, dashGapPx * invScale}, phase));

        canvas.drawPath(routePath, pathOutlinePaint);
        canvas.drawPath(routePath, pathStrokePaint);

        float arrowSize = dpToPx(getContext(), 4.2f) * invScale;
        chevronPaint.setStrokeWidth(dpToPx(getContext(), 2.0f) * invScale);
        for (int i = 0; i < currentPath.size() - 1; i++) {
            GraphNode a = currentPath.get(i);
            GraphNode b = currentPath.get(i + 1);
            float x1 = pctToX(a.xPct);
            float y1 = pctToY(a.yPct);
            float x2 = pctToX(b.xPct);
            float y2 = pctToY(b.yPct);
            float segLen = (float) Math.hypot(x2 - x1, y2 - y1);
            if (segLen < dpToPx(getContext(), 22f) * invScale) continue;

            float mx = x1 + ((x2 - x1) * 0.5f);
            float my = y1 + ((y2 - y1) * 0.5f);
            double angle = Math.atan2(y2 - y1, x2 - x1);
            float lx = (float) (mx - (arrowSize * Math.cos(angle - (Math.PI / 6.0))));
            float ly = (float) (my - (arrowSize * Math.sin(angle - (Math.PI / 6.0))));
            float rx = (float) (mx - (arrowSize * Math.cos(angle + (Math.PI / 6.0))));
            float ry = (float) (my - (arrowSize * Math.sin(angle + (Math.PI / 6.0))));
            canvas.drawLine(lx, ly, mx, my, chevronPaint);
            canvas.drawLine(rx, ry, mx, my, chevronPaint);
        }
    }

    private void drawEntryMarker(@NonNull Canvas canvas) {
        if (selectedEntry == null) return;
        float invScale = 1.0f / Math.max(1.0f, (float) Math.sqrt(zoomScale));
        float cx = pctToX(selectedEntry.xPct);
        float cy = pctToY(selectedEntry.yPct);
        float radius = dpToPx(getContext(), 9.5f) * invScale;

        pinFillPaint.setStyle(Paint.Style.FILL);
        pinFillPaint.setColor(colorUmNavy);
        canvas.drawCircle(cx, cy, radius, pinFillPaint);

        pinStrokePaint.setColor(Color.WHITE);
        pinStrokePaint.setStrokeWidth(dpToPx(getContext(), 2.2f) * invScale);
        canvas.drawCircle(cx, cy, radius, pinStrokePaint);

        pinFillPaint.setColor(colorUmGold);
        canvas.drawCircle(cx, cy, dpToPx(getContext(), 3.8f) * invScale, pinFillPaint);

        drawCalloutPill(canvas, cx, cy + radius + (dpToPx(getContext(), 11f) * invScale),
                selectedEntry.name, invScale);
    }

    private void drawRefillPins(@NonNull Canvas canvas) {
        float invScale = 1.0f / Math.max(1.0f, (float) Math.sqrt(zoomScale));
        for (RefillPoint point : points) {
            float cx = pctToX(point.xPct);
            float cy = pctToY(point.yPct);
            boolean isHighlighted = (point.pointId == highlightedPointId);
            float radius = (isHighlighted ? pinSelectedRadiusPx : pinVisualRadiusPx) * invScale;

            if (isHighlighted) {
                float pulseRadius = radius + (dpToPx(getContext(), 10f) * invScale * animationProgress);
                int alpha = (int) ((1f - animationProgress) * 110f);
                pulsePaint.setAlpha(alpha);
                canvas.drawCircle(cx, cy, pulseRadius, pulsePaint);

                pinStrokePaint.setColor(colorUmNavy);
                pinStrokePaint.setStrokeWidth(dpToPx(getContext(), 3.0f) * invScale);
                canvas.drawCircle(cx, cy, radius + (dpToPx(getContext(), 2.5f) * invScale), pinStrokePaint);
            }

            pinFillPaint.setStyle(Paint.Style.FILL);
            pinFillPaint.setColor(resolveStatusColor(point.status));
            canvas.drawCircle(cx, cy, radius, pinFillPaint);

            pinStrokePaint.setColor(Color.WHITE);
            pinStrokePaint.setStrokeWidth(dpToPx(getContext(), 2.0f) * invScale);
            canvas.drawCircle(cx, cy, radius, pinStrokePaint);

            drawStatusSymbol(canvas, cx, cy, point.status, invScale);

            if (isHighlighted) {
                String codeLabel = (point.qrCode != null && !point.qrCode.isEmpty())
                        ? point.qrCode : point.name;
                drawCalloutPill(canvas, cx, cy - radius - (dpToPx(getContext(), 13f) * invScale),
                        codeLabel, invScale);
            }
        }
    }

    private void drawCalloutPill(@NonNull Canvas canvas, float cx, float centerY,
                                 @NonNull String rawText, float invScale) {
        String text = rawText.length() > 20 ? rawText.substring(0, 18) + "…" : rawText;
        labelPaint.setTextSize(dpToPx(getContext(), 10f) * invScale);
        float textWidth = labelPaint.measureText(text);
        float padH = dpToPx(getContext(), 6f) * invScale;
        float halfH = dpToPx(getContext(), 8.5f) * invScale;

        float left = cx - (textWidth / 2f) - padH;
        float right = cx + (textWidth / 2f) + padH;
        if (left < baseImageBounds.left + 4f) {
            float shift = (baseImageBounds.left + 4f) - left;
            left += shift;
            right += shift;
        } else if (right > baseImageBounds.right - 4f) {
            float shift = right - (baseImageBounds.right - 4f);
            left -= shift;
            right -= shift;
        }

        RectF pill = new RectF(left, centerY - halfH, right, centerY + halfH);
        float r = dpToPx(getContext(), 5f) * invScale;
        canvas.drawRoundRect(pill, r, r, labelBgPaint);
        canvas.drawText(text, pill.centerX(), pill.bottom - (dpToPx(getContext(), 4.8f) * invScale), labelPaint);
    }

    private void drawStatusSymbol(@NonNull Canvas canvas, float cx, float cy,
                                  @NonNull String status, float invScale) {
        float s = dpToPx(getContext(), 4.8f) * invScale;
        iconPaint.setStrokeWidth(dpToPx(getContext(), 2.2f) * invScale);
        if (PointStatus.WORKING.equals(status)) {
            canvas.drawLine(cx - s, cy, cx - (s * 0.25f), cy + (s * 0.75f), iconPaint);
            canvas.drawLine(cx - (s * 0.25f), cy + (s * 0.75f), cx + s, cy - (s * 0.75f), iconPaint);
        } else if (PointStatus.EMPTY.equals(status)) {
            canvas.drawLine(cx - s, cy, cx + s, cy, iconPaint);
        } else {
            canvas.drawLine(cx - (s * 0.8f), cy - (s * 0.8f), cx + (s * 0.8f), cy + (s * 0.8f), iconPaint);
            canvas.drawLine(cx - (s * 0.8f), cy + (s * 0.8f), cx + (s * 0.8f), cy - (s * 0.8f), iconPaint);
        }
    }

    private int resolveStatusColor(@NonNull String status) {
        switch (status) {
            case PointStatus.WORKING:
                return colorStatusAvailable;
            case PointStatus.EMPTY:
                return colorStatusEmpty;
            case PointStatus.BROKEN:
            case PointStatus.DIRTY:
            case PointStatus.OUT_OF_SERVICE:
                return colorStatusFaulty;
            default:
                return colorStatusUnknownOutline;
        }
    }

    @Override
    public boolean onTouchEvent(@NonNull MotionEvent event) {
        if (getParent() != null) {
            getParent().requestDisallowInterceptTouchEvent(true);
        }
        scaleDetector.onTouchEvent(event);
        gestureDetector.onTouchEvent(event);

        float x = event.getX();
        float y = event.getY();

        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                lastTouchX = x;
                lastTouchY = y;
                isPanningOrScaling = false;
                hasPendingDragRoute = false;
                return true;

            case MotionEvent.ACTION_POINTER_DOWN:
                isPanningOrScaling = true;
                hasPendingDragRoute = false;
                return true;

            case MotionEvent.ACTION_MOVE:
                if (scaleDetector.isInProgress()) {
                    isPanningOrScaling = true;
                    hasPendingDragRoute = false;
                    lastTouchX = x;
                    lastTouchY = y;
                    return true;
                }
                float dx = x - lastTouchX;
                float dy = y - lastTouchY;
                if (zoomScale > 1.05f) {
                    // When zoomed in, 1-finger drag pans around the map smoothly
                    if (Math.hypot(dx, dy) > 2f) {
                        isPanningOrScaling = true;
                        panX += dx;
                        panY += dy;
                        clampPan();
                        invalidate();
                        lastTouchX = x;
                        lastTouchY = y;
                    }
                } else if (!isPanningOrScaling && Math.hypot(dx, dy) > dpToPx(getContext(), 6f)) {
                    // At 1x zoom, throttle route recalculation during drag to prevent main-thread stutter
                    computeBaseImageBounds();
                    float cx = viewToCanvasX(x);
                    float cy = viewToCanvasY(y);
                    if (baseImageBounds.contains(cx, cy)) {
                        hasPendingDragRoute = true;
                        long nowMs = SystemClock.uptimeMillis();
                        if (nowMs - lastDragRouteCalcUptimeMs >= DRAG_ROUTE_THROTTLE_MS) {
                            lastDragRouteCalcUptimeMs = nowMs;
                            hasPendingDragRoute = false;
                            notifyCustomEntryMoved(cx, cy);
                        }
                    }
                }
                return true;

            case MotionEvent.ACTION_UP:
                if (!isPanningOrScaling && hasPendingDragRoute) {
                    computeBaseImageBounds();
                    float cx = viewToCanvasX(x);
                    float cy = viewToCanvasY(y);
                    if (baseImageBounds.contains(cx, cy)) {
                        notifyCustomEntryMoved(cx, cy);
                    }
                }
                hasPendingDragRoute = false;
                isPanningOrScaling = false;
                break;

            case MotionEvent.ACTION_CANCEL:
                hasPendingDragRoute = false;
                isPanningOrScaling = false;
                break;
        }
        return super.onTouchEvent(event);
    }

    private float viewToCanvasX(float viewX) {
        return (viewX - panX) / zoomScale;
    }

    private float viewToCanvasY(float viewY) {
        return (viewY - panY) / zoomScale;
    }

    private void notifyCustomEntryMoved(float canvasX, float canvasY) {
        if (customEntryMoveListener == null || baseImageBounds.width() <= 0 || baseImageBounds.height() <= 0) {
            return;
        }
        float xPct = Math.max(0.02f, Math.min(0.98f, (canvasX - baseImageBounds.left) / baseImageBounds.width()));
        float yPct = Math.max(0.02f, Math.min(0.98f, (canvasY - baseImageBounds.top) / baseImageBounds.height()));
        customEntryMoveListener.onCustomEntryMoved(xPct, yPct);
    }

    @Override
    public boolean performClick() {
        return super.performClick();
    }

    @Nullable
    private RefillPoint findTappedPoint(float canvasX, float canvasY) {
        RefillPoint closest = null;
        float minDistance = pinTouchTargetRadiusPx / Math.max(1f, (float) Math.sqrt(zoomScale));
        for (RefillPoint point : points) {
            float px = pctToX(point.xPct);
            float py = pctToY(point.yPct);
            float dist = (float) Math.hypot(canvasX - px, canvasY - py);
            if (dist <= minDistance) {
                minDistance = dist;
                closest = point;
            }
        }
        return closest;
    }

    private float pctToX(float xPct) {
        return baseImageBounds.left + (xPct * baseImageBounds.width());
    }

    private float pctToY(float yPct) {
        return baseImageBounds.top + (yPct * baseImageBounds.height());
    }

    private static float dpToPx(@NonNull Context context, float dp) {
        return TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, dp, context.getResources().getDisplayMetrics());
    }
}