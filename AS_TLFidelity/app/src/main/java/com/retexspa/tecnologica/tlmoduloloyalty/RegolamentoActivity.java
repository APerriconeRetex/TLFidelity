package com.retexspa.tecnologica.tlmoduloloyalty;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.webkit.JavascriptInterface;
import android.webkit.WebSettings;
import android.webkit.WebView;

import androidx.appcompat.app.AppCompatActivity;

public class RegolamentoActivity extends AppCompatActivity {

    private WebView myWebView;
    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_regolamento);
        if(myWebView==null) myWebView = findViewById(R.id.webView);

        if(getSupportActionBar()!=null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }
        if (savedInstanceState == null) {
            //myWebView.setWebViewClient(new TLWebClient());
            myWebView.addJavascriptInterface(this, "tecno");
            // chrome://inspect/#devices
            //myWebView.setWebContentsDebuggingEnabled(true);

            WebSettings settings = myWebView.getSettings();
            settings.setUserAgentString("it_IT");
            settings.setAllowFileAccessFromFileURLs(true);
            settings.setAllowUniversalAccessFromFileURLs(true);
            settings.setJavaScriptEnabled(true);
            Bundle b = getIntent().getExtras();
            if (b != null) {
                String tipoCarta = b.getString("tipoCarta");
                if (tipoCarta == null || tipoCarta.equals("normale")) {
                    myWebView.loadUrl("file:///android_asset/Regolamento CARTA PIU CRAI.html");
                } else if ("senior".equals(tipoCarta)) {
                    myWebView.loadUrl("file:///android_asset/Regolamento CARTA SENIOR CRAI.html");
                }
            }
        }
    }

    @JavascriptInterface
    public void vaiAvanti() {
        runOnUiThread(this::onBackPressed);
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }
}
