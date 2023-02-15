package com.retexspa.tecnologica.tlmoduloloyalty;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.DashPathEffect;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Point;
import android.os.Bundle;
import android.os.Parcelable;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.Transformation;
import android.widget.RelativeLayout;

import androidx.annotation.*;
import java.io.ByteArrayOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

class firmaView extends View {
    public View okBtn, cancelBtn;

    public firmaView(Context context) {
        super(context);
    }
    public firmaView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }
    @Override protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec)  {
        // copiato da SignatureCatcher di TLoyaltyDataEntry android
        // setta le dimensioni della view come voglio
        // o scalato mantenendo l'aspect ratio
        int myWidth = 900;
        int myHeight = 300;

        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        // dimensioni richieste
        int width=-1;
        int height=-1;
        // dimensioni massime
        int maxW = -1;
        int maxH = -1;
        switch (MeasureSpec.getMode(widthMeasureSpec)) {
            case MeasureSpec.EXACTLY:
                width = MeasureSpec.getSize(widthMeasureSpec);
                break;
            case MeasureSpec.AT_MOST:
                maxW = MeasureSpec.getSize(widthMeasureSpec);
                break;
            case MeasureSpec.UNSPECIFIED:
                break;
        }
        switch (MeasureSpec.getMode(heightMeasureSpec)) {
            case MeasureSpec.EXACTLY:
                height = MeasureSpec.getSize(heightMeasureSpec);
                break;
            case MeasureSpec.AT_MOST:
                maxH = MeasureSpec.getSize(heightMeasureSpec);
                break;
            case MeasureSpec.UNSPECIFIED:
                break;
        }

        if(width==-1 && height==-1) {
            // nessuna misura specificata, metto il max dove c'è
            if(maxW>0) width = maxW;
            if(maxH>0) height = maxH;
        }
        if(width==-1 && height==-1) {
            //  nessuna misura specificata, uso le mie
            width = myWidth;
            height = myHeight;
        } else
        if( height == -1) {
            // specificata larghezza
            height = width * myHeight / myWidth;
            if(maxH>0 && height>maxH) height = maxH;
        } else
        if(width == -1) {
            // specificata altezza
            width = height * myWidth / myHeight;
            if(maxW>0 && width>maxW) width = maxW;
        }
        // le applico
        setMeasuredDimension(width, height);
    }

    public void VaiPienoSchermoDa(int x, int y, int w, int h, Point size) {
        RelativeLayout.LayoutParams p = (RelativeLayout.LayoutParams) getLayoutParams();
        p.leftMargin = x;
        p.topMargin = y;
        p.width = w;
        p.height=h;
        setLayoutParams(p);
        setVisibility(View.VISIBLE);
        final int finalW = (int)(size.x*0.8);
        final int finalH = finalW/3;
        //if(size.x>size.y*3) {
        //    finalH=(int)(size.y*0.8);;
        //    finalW=finalH*3;
        //}
        final int finalX = (size.x-finalW)/2;
        final int finalY = (size.y-finalH)/2;
        final int okCenter = size.x-finalX/2;
        final int cancelCenter = finalX/2;
        final int verCenter = size.y/2;
        Animation a = new Animation() {
            @Override
            protected void applyTransformation(float interpolatedTime, Transformation t) {
                RelativeLayout.LayoutParams p = (RelativeLayout.LayoutParams) getLayoutParams();
                p.leftMargin = (int)(x*(1-interpolatedTime)+finalX*(interpolatedTime));
                p.topMargin = (int)(y*(1-interpolatedTime)+finalY*(interpolatedTime));
                p.width = (int)(w*(1-interpolatedTime)+finalW*(interpolatedTime));
                p.height = (int)(h*(1-interpolatedTime)+finalH*(interpolatedTime));
                setLayoutParams(p);
                int dim = (int) (finalX*interpolatedTime);
                if(okBtn!=null) {
                    p = (RelativeLayout.LayoutParams) okBtn.getLayoutParams();
                    p.leftMargin = okCenter-dim/2;
                    p.topMargin = verCenter-dim/2;
                    p.width = dim;
                    p.height = dim;
                    okBtn.setLayoutParams(p);
                    okBtn.setVisibility(View.VISIBLE);
                }
                if(cancelBtn!=null) {
                    p = (RelativeLayout.LayoutParams) cancelBtn.getLayoutParams();
                    p.leftMargin = cancelCenter-dim/2;
                    p.topMargin = verCenter-dim/2;
                    p.width = dim;
                    p.height = dim;
                    cancelBtn.setLayoutParams(p);
                    cancelBtn.setVisibility(View.VISIBLE);
                }
            }
        };
        a.setDuration(500); // in ms
        startAnimation(a);
    }

    public void Cancella() {
        current=null;
        invalidate();
    }

    public void updateSize(@NonNull Point size) {
        final int finalW = (int)(size.x*0.8);
        final int finalH = finalW/3;
        final int finalX = (size.x-finalW)/2;
        final int finalY = (size.y-finalH)/2;
        final int okCenter = size.x-finalX/2;
        final int cancelCenter = finalX/2;
        final int verCenter = size.y/2;
        RelativeLayout.LayoutParams p = (RelativeLayout.LayoutParams) getLayoutParams();
        p.leftMargin = finalX;
        p.topMargin = finalY;
        p.width = finalW;
        p.height = finalH;
        setLayoutParams(p);
        if(okBtn!=null) {
            p = (RelativeLayout.LayoutParams) okBtn.getLayoutParams();
            p.leftMargin = okCenter-finalX/2;
            p.topMargin = verCenter-finalX/2;
            //noinspection SuspiciousNameCombination
            p.width = p.height = finalX;
            okBtn.setLayoutParams(p);
        }
        if(cancelBtn!=null) {
            p = (RelativeLayout.LayoutParams) cancelBtn.getLayoutParams();
            p.leftMargin = cancelCenter-finalX/2;
            p.topMargin = verCenter-finalX/2;
            //noinspection SuspiciousNameCombination
            p.width = p.height = finalX;
            cancelBtn.setLayoutParams(p);
        }
    }

    static class CustomPath extends Path implements Serializable {
        private final List<Float> points;
        private float width, height;

        CustomPath(float w, float h) {
            super();
            width = w;
            height = h;
            points = new ArrayList<>();
        }

        void AddPoint(float x, float y) {
            if (points.size() == 0) {
                moveTo(x * width, y * height);
            } else {
                lineTo(x * width, y * height);
            }
            points.add(x);
            points.add(y);
        }

        void Loaded() /*float w, float h) */ {
            reset();
        }

        void Draw(Canvas c, Paint p, float w, float h) {
            if(isEmpty() || width!=w || height!=h) {
                reset();
                width=w;
                height=h;
                for (int i = 0; i < points.size(); i+=2) {
                    float x = points.get( i ) * width;
                    float y = points.get( i + 1 ) * height;
                    if(i==0)
                        moveTo(x,y);
                    else
                        lineTo(x,y);
                }
            }
            c.drawPath(this,p);
        }
    }

    private List<CustomPath> current = null;

    public boolean hasData() {
        return current!=null && current.size()>0;
    }

    @Override protected Parcelable onSaveInstanceState()  {
        Bundle ret = new Bundle();
        ret.putParcelable("super",super.onSaveInstanceState());
        if(current!=null) {
            ret.putInt("nCurrent",current.size());
            for(int i=0;i<current.size();i++)
                ret.putSerializable("path" + i, current.get(i));
        } else
            ret.putInt("nCurrent",0);
        return ret;
    }

    @Override protected void onRestoreInstanceState(Parcelable state) {
        if (state instanceof Bundle) { // implicit null check
            Bundle bundle = (Bundle) state;
            state = bundle.getParcelable("superState");
            super.onRestoreInstanceState(state);

            int len = bundle.getInt("nCurrent");
            current = new ArrayList<>(len);
            for(int i=0;i<len;i++)
            {
                CustomPath pp =(CustomPath) bundle.getSerializable("path" + i);
                if (pp != null) {
                    pp.Loaded();
                    current.add(pp);
                }
            }
        } else
            super.onRestoreInstanceState(state);
    }

    @Override protected void onDraw(Canvas canvas)
    {
        float width = getWidth();
        float height = getHeight();
        Draw(width,height,canvas, false);
    }

    private void Draw(float width,float height,Canvas canvas, boolean transparent)  {
        if(!transparent) {
            Paint whiteBack = new Paint();
            whiteBack.setColor(Color.WHITE);
            whiteBack.setStyle(Paint.Style.FILL);
            canvas.drawRect(0, 0, width, height, whiteBack);

            Paint linePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            linePaint.setColor(Color.BLACK);
            linePaint.setStyle(Paint.Style.STROKE);
            linePaint.setPathEffect(new DashPathEffect(new float[]{10, 10},0));
            float margineX = width/18;
            float altezzaLinea = height*2/3;
            canvas.drawLine(margineX,altezzaLinea,width-margineX,altezzaLinea,linePaint);
        }

        if(current == null)
            return;

        Paint signaturePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        signaturePaint.setColor(Color.BLACK);
        signaturePaint.setStyle(Paint.Style.STROKE);
        signaturePaint.setStrokeWidth(height/80); //empirico
        for(CustomPath tratto : current )
        {
            tratto.Draw(canvas,signaturePaint,width,height);
            //canvas.drawPath(tratto,signaturePaint);
        }

    }

    @SuppressLint("WrongThread")
    public byte[] getImage(int w, int h) {
        Bitmap b = Bitmap.createBitmap(w,h, Bitmap.Config.ARGB_8888);
        Canvas c = new Canvas(b);
        Draw(w,h,c,true);
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        b.compress(Bitmap.CompressFormat.PNG,80,stream);
        return stream.toByteArray();
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override public boolean onTouchEvent(MotionEvent event) {
        switch(event.getAction())
        {
            case MotionEvent.ACTION_DOWN:
                if(current==null)
                    current = new ArrayList<>();
                current.add(new CustomPath(getWidth(), getHeight()));
                getParent().requestDisallowInterceptTouchEvent(true);
                // no break
            case MotionEvent.ACTION_MOVE:
                current.get(current.size()-1).AddPoint(event.getX() / (float)getWidth(), event.getY() / (float)getHeight());
                invalidate();
                break;
            case MotionEvent.ACTION_UP:
                getParent().requestDisallowInterceptTouchEvent(false);
                break;

        }
        return true; //super.onTouchEvent(event);
    }

}
