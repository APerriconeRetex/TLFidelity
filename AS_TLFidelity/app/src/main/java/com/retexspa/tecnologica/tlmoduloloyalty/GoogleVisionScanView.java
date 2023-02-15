package com.retexspa.tecnologica.tlmoduloloyalty;

import android.app.Activity;
import android.content.Context;
import android.media.Image;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.camera.core.Camera;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ExperimentalAnalyzer;
import androidx.camera.core.ExperimentalGetImage;
import androidx.camera.core.FocusMeteringAction;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.MeteringPoint;
import androidx.camera.core.Preview;
import androidx.camera.core.SurfaceOrientedMeteringPointFactory;
import androidx.camera.core.UseCaseGroup;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.LifecycleRegistry;

import com.google.android.gms.tasks.OnSuccessListener;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.mlkit.vision.barcode.BarcodeScanner;
import com.google.mlkit.vision.barcode.BarcodeScannerOptions;
import com.google.mlkit.vision.barcode.BarcodeScanning;
import com.google.mlkit.vision.barcode.common.Barcode;
import com.google.mlkit.vision.common.InputImage;

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;

@ExperimentalAnalyzer
public class GoogleVisionScanView
        implements View.OnTouchListener, ImageAnalysis.Analyzer, LifecycleOwner, Runnable {
    private boolean torchOn = false;
    private boolean isClosed = false;
    private String sResult = "";


    private final LifecycleRegistry lifecycleRegistry;
    private Camera camera = null;
    private final PreviewView previewView;
    private BarcodeScanner scanner = null;
    private final ImageView lightButton;

    public GoogleVisionScanView(PreviewView _previewView,ImageView _lightButton)  {
        lightButton = _lightButton;
        lightButton.setOnClickListener(v -> manageTorch());
        previewView = _previewView;
        previewView.setOnTouchListener(this);
        lifecycleRegistry = new LifecycleRegistry(this);
        Context context = previewView.getContext();
        if(context==null)
            return;
    }

    @Override
    protected void finalize() throws Throwable {
        super.finalize();
        lifecycleRegistry.setCurrentState(Lifecycle.State.DESTROYED);
        isClosed = true;
        camera = null;
        mScanChangedListeners.clear();
    }


    private void manageTorch() {
        if(camera==null) return;
        torchOn = !torchOn;
        camera.getCameraControl().enableTorch(torchOn);
        if (torchOn)
            lightButton.setImageResource(R.drawable.ic_flash_on_black_24dp);
        else
            lightButton.setImageResource(R.drawable.ic_flash_off_black_24dp);
    }


    @Override
    public boolean onTouch(View v, MotionEvent event) {
        switch(event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                SurfaceOrientedMeteringPointFactory factory =
                        new SurfaceOrientedMeteringPointFactory(
                            v.getWidth(), v.getHeight()
                );
                MeteringPoint point = factory.createPoint(event.getX(), event.getY());
                FocusMeteringAction action = new FocusMeteringAction.Builder(point, FocusMeteringAction.FLAG_AF)
                        .build();
                camera.getCameraControl().startFocusAndMetering(action);
                return true;
            case MotionEvent.ACTION_UP:
                v.performClick();
                break;
        }
        return false;
    }

    @Override
    public void run() {
        Context context = previewView.getContext();
        if(context==null)
            return;
        ListenableFuture<ProcessCameraProvider> cameraProviderFuture=
                ProcessCameraProvider.getInstance(context);
        ProcessCameraProvider cameraProvider;
        try {
            cameraProvider = cameraProviderFuture.get();
        } catch (ExecutionException | InterruptedException e) {
            e.printStackTrace();
            return;
        }
        CameraSelector cameraSelector = new CameraSelector.Builder()
                .requireLensFacing(CameraSelector.LENS_FACING_BACK)
                .build();

        Preview preview = new Preview.Builder().build();
        preview.setSurfaceProvider(previewView.getSurfaceProvider());

        if (scanner == null) {
            scanner = BarcodeScanning.getClient(
                    new BarcodeScannerOptions.Builder()
                            .setBarcodeFormats(Barcode.FORMAT_EAN_13)
                            .build()
            );
        }

        ImageAnalysis imageAnalysis = new ImageAnalysis.Builder()
                //.setMaxResolution(Size(1980,1080))
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build();
        imageAnalysis.setAnalyzer(Executors.newSingleThreadExecutor(), this);
        UseCaseGroup useCaseGroup = new UseCaseGroup.Builder()
                .addUseCase(preview)
                .addUseCase(imageAnalysis)
                .build();

        camera = cameraProvider.bindToLifecycle(
                this,
                cameraSelector,
                useCaseGroup
        );
        if(!camera.getCameraInfo().hasFlashUnit()) {
            lightButton.setVisibility(View.GONE);
        } else {
            manageTorch();
        }
        lifecycleRegistry.setCurrentState(Lifecycle.State.RESUMED);
    }

    public void creaCamera(Context context) {
        ListenableFuture<ProcessCameraProvider> cameraProviderFuture=
                ProcessCameraProvider.getInstance(context);
        cameraProviderFuture.addListener(this, ContextCompat.getMainExecutor(context));

    }

    public interface ScanResultChangedListener  {
        void OnScanResult(String newResult);
    }
    private final List<ScanResultChangedListener> mScanChangedListeners = new LinkedList<>();
    public void addOnScanResultChangedListener(ScanResultChangedListener v) {
        mScanChangedListeners.add(v);
    }

    @Override
    @ExperimentalGetImage
    public void analyze(@NonNull ImageProxy imageProxy) {
        if(isClosed) {
            imageProxy.close();
            return;
        }
        // https://developers.google.com/ml-kit/vision/barcode-scanning/android
        Image mediaImage = imageProxy.getImage();
        if(mediaImage==null)
            return;

        InputImage image = InputImage.fromMediaImage(mediaImage, imageProxy.getImageInfo().getRotationDegrees());
        OnSuccessListener<List<Barcode>> tmp = (OnSuccessListener<List<Barcode>>) detections -> {
            if (detections.size() > 0) {
                String newRead = "";
                for(Barcode bb : detections) {
                    String value = bb.getRawValue();
                    if(value==null) continue;
                    if (value.length() < 13)
                        value = String.format("%13s",value).replace(' ','0');
                    if(value.equals(sResult)) {
                        newRead=value;
                        break;
                    }
                    if(newRead.isEmpty())
                        newRead = value;
                }
                if (!newRead.isEmpty() && !newRead.equals(sResult)) {
                    sResult = newRead;
                    ((Activity) previewView.getContext()).runOnUiThread(() -> {
                        for (ScanResultChangedListener ll : mScanChangedListeners) {
                            ll.OnScanResult(sResult);
                        }
                    });
                }
            }
            imageProxy.close();
        };
        scanner.process(image).addOnSuccessListener(tmp);
    }

    @NonNull
    @Override
    public Lifecycle getLifecycle() {
        return lifecycleRegistry;
    }
}
