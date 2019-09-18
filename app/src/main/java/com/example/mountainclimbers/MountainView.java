package com.example.mountainclimbers;

import android.content.Context;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.support.v4.content.ContextCompat;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import java.lang.Math;

public class MountainView extends View {

    public static int STEP_NUMBER = 1000;
    public static int PADDING = 150;
    private static int TEXT_SIZE = 1000;

    protected Mountain mountain;
    private Paint mountainPaint, skyPaint;
    protected Paint victoryTextPaint;
    private ColorFilter arrowFilter, highlightedArrowFilter;
    protected List<MountainClimber> climbers;
    protected Map<MountainClimber, Paint> climberPaints;
    protected Context context;
    protected MountainClimber selectedClimber;
    protected Moving moving;
    protected boolean victory;
    protected Rect r = new Rect();
    protected OnVictoryListener victoryListener;

    public MountainView(Context context, AttributeSet attrs){
        super(context, attrs);
        this.context = context;
        Resources r = getResources();
        this.mountainPaint = new Paint();
        this.mountainPaint.setColor(r.getColor(R.color.mountainGrey));
        this.skyPaint = new Paint();
        this.skyPaint.setColor(r.getColor(R.color.skyBlue));
        this.victoryTextPaint = new Paint();
        this.victoryTextPaint.setColor(r.getColor(R.color.victoryGold));
        this.victoryTextPaint.setTextSize(TEXT_SIZE);
        this.arrowFilter = new PorterDuffColorFilter(r.getColor(R.color.guideArrows), PorterDuff.Mode.SRC_ATOP);
        this.highlightedArrowFilter = new PorterDuffColorFilter(r.getColor(R.color.highlightedArrow), PorterDuff.Mode.SRC_ATOP);
        this.climbers = new ArrayList<>();
        this.climberPaints = new HashMap<>();
        this.selectedClimber = null;
        this.moving = Moving.NONE;
        this.victoryListener = new OnVictoryListener() {
            @Override
            public void onVictory() {
                return;
            }
        };
    }

    public void addClimber(MountainClimber climber, int colorId){
        this.climbers.add(climber);
        Paint paint = new Paint();
        paint.setColor(getResources().getColor(colorId));
        this.climberPaints.put(climber, paint);
    }

    public void setMountain(Mountain mountain){
        this.mountain = mountain;
        this.victory = false;
        this.moving = Moving.NONE;
        this.climbers = new ArrayList<>();
        this.selectedClimber = null;
        this.climberPaints = new HashMap<>();
        invalidate();
    }

    public Mountain getMountain(){
        return mountain;
    }

    public void setOnVictoryListener(OnVictoryListener v){
        this.victoryListener = v;
    }

    protected boolean removeClimbers(){
        if (climbers.size() == 1){
            victory = true;
            return false;
        }
        for (MountainClimber climber : climbers){
            for (MountainClimber c2 : climbers) {
                if (c2 != climber && Math.abs(c2.getPosition() - climber.getPosition()) < 1.5) {
                    this.climbers.remove(c2);
                    climber.setDirection(null);
                    return true;
                }
            }
        }
        return false;
    }

    private void drawClimbers(Canvas canvas){
        int width = getWidth() - 2 * PADDING;
        int height = getHeight() - 2 * PADDING;
        for (MountainClimber climber : climbers){
            canvas.drawCircle(climber.getPosition() * width / mountain.getWidth() + PADDING,
                    getHeight() - PADDING - mountain.getHeightAt(climber.getPosition()) * height / mountain.getMaxHeight(),
                    30, climberPaints.get(climber));
        }
    }

    protected void drawDirections(Canvas canvas){
        if (moving == Moving.UP || moving == Moving.DOWN || victory){
            return;
        }
        int width = getWidth() - 2 * PADDING;
        int height = getHeight() - 2 * PADDING;
        for (MountainClimber climber : climbers){
            int cx = climber.getPosition() * width / mountain.getWidth() + PADDING;
            int cy = getHeight() - PADDING - this.mountain.getHeightAt(climber.getPosition())  * height / mountain.getMaxHeight();
            Drawable d = ContextCompat.getDrawable(this.context, R.drawable.arrow_left);
            d.setColorFilter(arrowFilter);

            MountainClimber.Direction direction = climber.getDirection();

            d.setBounds(cx - 80, cy - 30, cx - 35, cy + 30);
            if (direction == MountainClimber.Direction.LEFT){
                d.setColorFilter(highlightedArrowFilter);
                d.draw(canvas);
                d.setColorFilter(arrowFilter);
            } else if (climber == selectedClimber){
                d.draw(canvas);
            }
            d.setBounds(cx + 80, cy - 30, cx + 35, cy + 30);
            if (direction == MountainClimber.Direction.RIGHT){
                d.setColorFilter(highlightedArrowFilter);
                d.draw(canvas);
                d.setColorFilter(arrowFilter);
            } else if (climber == selectedClimber){
                d.draw(canvas);
            }
        }
    }

    protected void drawCenter(Canvas canvas, Paint paint, String text) {
        TEXT_SIZE = 1000;
        canvas.getClipBounds(r);
        int cHeight = r.height();
        int cWidth = r.width();
        paint.setTextAlign(Paint.Align.LEFT);
        paint.getTextBounds(text, 0, text.length(), r);
        float x = cWidth / 2f - r.width() / 2f - r.left;
        float y = cHeight / 2f + r.height() / 2f - r.bottom;
        while (x < PADDING){
            TEXT_SIZE = TEXT_SIZE - 50;
            paint.setTextSize(TEXT_SIZE);
            canvas.getClipBounds(r);
            cHeight = r.height();
            cWidth = r.width();
            paint.setTextAlign(Paint.Align.LEFT);
            paint.getTextBounds(text, 0, text.length(), r);
            x = cWidth / 2f - r.width() / 2f - r.left;
            y = cHeight / 2f + r.height() / 2f - r.bottom;
        }
        canvas.drawText(text, x, y, paint);
    }

    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        while (removeClimbers()){};

        int height = getHeight() - 2 * PADDING;
        int width = getWidth() - 2 * PADDING;
        canvas.drawRect(0,  getHeight(), getWidth(), 0, skyPaint);
        canvas.drawRect(0, getHeight(), getWidth(), getHeight() - PADDING, mountainPaint);
        for (int i = 0; i < STEP_NUMBER; i++){
            int y = -PADDING + mountain.getHeightAt(
                    mountain.getWidth() * i / STEP_NUMBER) * height / mountain.getMaxHeight();
            canvas.drawRect(i * width / STEP_NUMBER + PADDING,
                    height + PADDING,
                    (i + 1) * width / STEP_NUMBER + PADDING,
                    height - y, mountainPaint);
        }
        drawClimbers(canvas);
        drawDirections(canvas);

        if (victory && moving == Moving.NONE){
            drawCenter(canvas, victoryTextPaint, "YOU WIN!");
        }

        if (moving != Moving.NONE){
            boolean canGoUp = (moving == Moving.UP);
            for (MountainClimber climber : climbers){
                canGoUp = canGoUp && climber.canMoveUp(mountain);
            }
            boolean canGoDown = (moving == Moving.DOWN);
            for (MountainClimber climber : climbers){
                canGoDown = canGoDown && climber.canMoveDown(mountain);
            }
            if (canGoUp || canGoDown){
                for (MountainClimber climber : climbers){
                    climber.move();
                }
                postInvalidateDelayed(2);
                return;
            } else {
                moving = Moving.NONE;
                if (victory) {
                    victoryListener.onVictory();
                }
                invalidate();
            }
        }
    }

    public void go(){
        if (victory){
            return;
        }

        boolean allSelected = true;
        for (MountainClimber climber : climbers){
            if (climber.getDirection() == null){
                allSelected = false;
            }
        }
        if (allSelected == false){
            return;
        }
        boolean canGoUp = true;
        for (MountainClimber climber : climbers){
            canGoUp = canGoUp && climber.canMoveUp(mountain);
        }
        if (canGoUp){
            Log.d("MVIEW", "Going up");
            moving = Moving.UP;
            invalidate();
        }
        boolean canGoDown = true;
        for (MountainClimber climber : climbers){
            canGoDown = canGoDown && climber.canMoveDown(mountain);
        }
        if (canGoDown){
            Log.d("MVIEW", "Going down");
            moving = Moving.DOWN;
            invalidate();
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent e){
        if (victory){
            return true;
        }

        float x = e.getX();
        float y = e.getY();

        switch (e.getAction()) {
            case MotionEvent.ACTION_DOWN:
                if (selectedClimber == null){
                    int width = getWidth() - 2 * PADDING;
                    int height = getHeight() - 2 * PADDING;

                    MountainClimber bestClimber = null;
                    int bestDistance = 50;
                    for (MountainClimber climber : climbers){
                        int cx = climber.getPosition() * width / mountain.getWidth() + PADDING;
                        int cy = getHeight() - PADDING - mountain.getHeightAt(climber.getPosition()) * height / mountain.getMaxHeight();
                        if (Math.abs(cx - x) + Math.abs(cy - y) < bestDistance){
                            bestClimber = climber;
                            bestDistance = (int) (Math.abs(cx - x) + Math.abs(cy - y));
                        }
                    }
                    selectedClimber = bestClimber;
                    invalidate();
                    return true;
                } else {
                    return true;
                }
            case MotionEvent.ACTION_MOVE:
                if (selectedClimber == null){
                    return false;
                }
                int cx = selectedClimber.getPosition() * getWidth() / mountain.getWidth();
                int cy = getHeight() - mountain.getHeightAt(selectedClimber.getPosition()) * getHeight() / mountain.getMaxHeight();
                if (x > cx){
                    selectedClimber.setDirection(MountainClimber.Direction.RIGHT);
                } else {
                    selectedClimber.setDirection(MountainClimber.Direction.LEFT);
                }
                invalidate();
                return true;
            case MotionEvent.ACTION_UP:
                if (selectedClimber != null){
                    selectedClimber = null;
                    invalidate();
                    return true;
                }
                return false;
        }
        return false;
    }

    public enum Moving{
        UP, DOWN, NONE
    }

    public interface OnVictoryListener {
        public void onVictory();
    }
}
