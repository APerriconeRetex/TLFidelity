package com.retexspa.tecnologica.tlmoduloloyalty;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.graphics.Point;
import android.util.AttributeSet;
import android.view.Display;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.ViewGroup;

import com.google.android.gms.vision.CameraSource;
import com.google.android.gms.vision.Detector;
import com.google.android.gms.vision.barcode.Barcode;
import com.google.android.gms.vision.barcode.BarcodeDetector;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

/**
 * Created by Antonino Perricone on 10/01/2017.
 */

public class GoogleVisionScanView
        extends SurfaceView implements Detector.Processor<Barcode>, SurfaceHolder.Callback
{
    private BarcodeDetector detector = null;
    private CameraSource mCameraSource = null;
    private String sResult = "";

    public interface ScanResultChangedListener  {
        void OnScanResult(String newResult);
    }
    private List<ScanResultChangedListener> mScanChangedListeners = new LinkedList<>();
    public void addOnScanResultChangedListener(ScanResultChangedListener v) {
        mScanChangedListeners.add(v);
    }

    public GoogleVisionScanView(Context context, AttributeSet attrs)  {
        super(context, attrs);
        CommonConstructor(context);
    }
    public GoogleVisionScanView(Context context)  {
        super(context);
        CommonConstructor(context);
    }

    private void CommonConstructor(Context context) {
        if (isInEditMode()) return;

        detector = new BarcodeDetector.Builder(context).build();
        detector.setProcessor(this);
    }

    @SuppressLint("MissingPermission")
    public void creaCamera(int chosenCamera) {
        if(mCameraSource!=null) {
            mCameraSource.stop();
            mCameraSource.release();
        }
        CameraSource.Builder builder = new CameraSource.Builder(getContext(), detector);
        builder.setFacing(chosenCamera);
        builder.setAutoFocusEnabled(true);

        mCameraSource = builder.build();
        SurfaceHolder holder = getHolder();
        holder.addCallback(this);
        if(holder.getSurface().isValid()) {
            try {
                mCameraSource.start(holder);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    @Override public void release()  {
    }

    @Override public void receiveDetections(final Detector.Detections<Barcode> detections) {
        if (detections.getDetectedItems().size() > 0) {
            final String value = detections.getDetectedItems().valueAt(0).rawValue;
            if (!value.equals(sResult)) {
                ((Activity)getContext()).runOnUiThread(() -> {
                    sResult = value;
                    for (ScanResultChangedListener ll : mScanChangedListeners) {
                        ll.OnScanResult(value);
                    }
                });
            }
        }
    }

    @Override public void surfaceCreated(SurfaceHolder holder) {
        try {
            //noinspection MissingPermission
            mCameraSource.start(holder);

            //Size size = mCameraSource.getPreviewSize();

            ViewGroup.LayoutParams ll = getLayoutParams();
            if(ll.height!=wantHeight) {
                ll.height = wantHeight;
                setLayoutParams(ll);
                requestLayout();
                //for (ResizeListener rl : mResizeListeners) rl.OnResize(wantHeight);
            }

        } catch (Exception e) {
            if (mCameraSource != null) {
                mCameraSource.stop();
                mCameraSource.release();
                mCameraSource = null;
            }
        }
    }

    @Override public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        /*mCameraSource.stop();
        surfaceCreated(holder);*/
    }

    @Override public void surfaceDestroyed(SurfaceHolder holder) {
        if (mCameraSource != null) {
            mCameraSource.stop();
        }
    }
/*
    public interface ResizeListener {
        void OnResize(int newH);
    }
    private List<ResizeListener> mResizeListeners = new LinkedList<>();

    public void addOnResizeListener(ResizeListener v) {
        mResizeListeners.add(v);
    }*/

    private int wantHeight = 0;

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        super.onLayout(changed, l, t, r, b);
        if (isInEditMode()) return;

        final Display defaultDisplay = ((Activity) getContext()).getWindowManager().getDefaultDisplay();
        @SuppressLint("DrawAllocation") Point size = new Point();
        defaultDisplay.getSize(size);
        int w = r - l;
        wantHeight = w * size.y / size.x;
        getHolder().setFixedSize(w, wantHeight);
    }

    @Override public void setVisibility(int visibility) {
        super.setVisibility(visibility);
        if (visibility == VISIBLE)
            StartCamera();
        else
            StopCamera();
    }

    public void StopCamera() {
        if(mCameraSource!=null) {
            mCameraSource.stop();
        }
    }

    @SuppressLint("MissingPermission")
    public void StartCamera() {
        try {
            mCameraSource.start(getHolder());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public int getCameraFacing() {
        if(mCameraSource!=null) {
            return mCameraSource.getCameraFacing();
        }
        return -1;
    }
}
