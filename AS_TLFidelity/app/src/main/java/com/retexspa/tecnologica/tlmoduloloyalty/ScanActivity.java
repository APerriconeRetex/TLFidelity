package com.retexspa.tecnologica.tlmoduloloyalty;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.media.AudioManager;
import android.media.ToneGenerator;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.ExperimentalAnalyzer;
import androidx.core.app.ActivityCompat;
import androidx.core.widget.ImageViewCompat;

@ExperimentalAnalyzer
public class ScanActivity extends AppCompatActivity implements GoogleVisionScanView.ScanResultChangedListener {

    private static final int CAMERA_REQUEST_PERMISSION = 1337;
    private GoogleVisionScanView scan;

    @Override
    @ExperimentalAnalyzer
    protected void  onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_scan);
        scan = new GoogleVisionScanView(findViewById(R.id.scanView),findViewById(R.id.flash));
        scan.addOnScanResultChangedListener(this);
        if(ActivityCompat.checkSelfPermission(this,Manifest.permission.CAMERA)==PackageManager.PERMISSION_GRANTED) {
            scan.creaCamera(getApplicationContext());
        } else {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, CAMERA_REQUEST_PERMISSION);
        }
    }

    @ExperimentalAnalyzer
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode != CAMERA_REQUEST_PERMISSION) {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
            return;
        }
        if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            scan.creaCamera(getApplicationContext());
        } else {
            onBackPressed();
        }
    }

    public void vaiIndietro(View view) {
        Intent intent = new Intent();
        setResult(RESULT_CANCELED, intent);
        finish();
    }

    public void tornaCodice(View view) {
        ColorStateList vc= ImageViewCompat.getImageTintList((ImageView) view);
        if(vc!=null) {
            return;
        }
        Intent intent = new Intent();
        TextView txt = findViewById(R.id.currResult);
        intent.putExtra("codice",txt.getText());
        setResult(RESULT_OK, intent);
        finish();
    }

    /*public void cambiaCamera(View view) {
        int facing = creaCamera(-1);
        ImageButton btn = (ImageButton)view;
        if(facing==CameraSource.CAMERA_FACING_BACK) {
            btn.setImageResource(R.drawable.ic_camera_front_black_24dp);
        } else
            btn.setImageResource(R.drawable.ic_camera_rear_black_24dp);
    }*/

    @Override
    public void OnScanResult(String newResult) {
        TextView txt = findViewById(R.id.currResult);
        String oldTxt = txt.getText().toString();
        if (newResult.length() == 12 && newResult.startsWith("4")) {
            newResult = "0" + newResult;
        }

        txt.setText(newResult);
        if(!oldTxt.equals(newResult)) {
            ToneGenerator toneG = new ToneGenerator(AudioManager.STREAM_ALARM, 100);
            toneG.startTone(ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD, 200);
        }
        ImageButton btn = findViewById(R.id.ok);
        ImageViewCompat.setImageTintList(btn,null);
    }


}
