package com.retexspa.tecnologica.tlmoduloloyalty;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.SoundEffectConstants;
import android.webkit.JavascriptInterface;
import android.webkit.WebSettings;
import android.webkit.WebView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import static com.retexspa.tecnologica.tlmoduloloyalty.TLFidelityWS.statoCodiceTessera.Valida;

public class ModuloActivity extends AppCompatActivity {

    private WebView myWebView;
    static String baseTessera;
    static String baseTesseraSenior;

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_modulo);

        if(myWebView==null) myWebView = findViewById(R.id.webView);
        if (savedInstanceState == null) {
            myWebView.setWebViewClient(new TLWebClient(this));
            myWebView.addJavascriptInterface(this, "tecno");
            // chrome://inspect/#devices
            //myWebView.setWebContentsDebuggingEnabled(true);

            WebSettings settings = myWebView.getSettings();
            settings.setUserAgentString("it_IT");
            settings.setAllowFileAccessFromFileURLs(true);
            settings.setAllowUniversalAccessFromFileURLs(true);
            settings.setJavaScriptEnabled(true);
            myWebView.loadUrl("file:///android_asset/index1.html");
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if(item.getItemId()==android.R.id.home) {
            onBackPressed();
            return true;
        }
        return false;
    }


    @JavascriptInterface
    public void vaiAvanti(String codiceTessera,boolean cartaSenior) {
        if(codiceTessera.equals(baseTessera)) baseTessera = null;
        if(codiceTessera.equals(baseTesseraSenior)) baseTesseraSenior = null;
        if(!controllaCodiceTessera(codiceTessera)) {
            AlertDialog.Builder builder = new AlertDialog.Builder(ModuloActivity.this);
            builder.setMessage(getString(R.string.tessera_invalida))
                    .setPositiveButton("Ok",null)
                    .show().setCanceledOnTouchOutside(false);
            return;
        }
        TLFidelityWS ws = new TLFidelityWS(this);
        try {
            ws.controllaTessera(codiceTessera,cartaSenior,(TLFidelityWS.statoCodiceTessera stato)->{
                if(stato==Valida) {
                    runOnUiThread(() -> {
                        menuPrincipaleActivity.setIdCliente(-1,codiceTessera,cartaSenior);
                        Intent i = new Intent(this, Modulo2Activity.class);
                        i.putExtra("tipoCarta","senior");
                        startActivity(i);
                    });
                    return;
                }
                String msg = "";
                switch (stato) {
                    case TipoErrato:
                        /*if(cartaSenior)
                            msg="La tessera indicata non è di tipo Carta Più Senior";
                        else
                            msg="La tessera indicata non è di tipo Carta Più";
                        break;*/
                        msg="Il tipo di tessera selezionata non è corretto";
                        break;
                    case Inesistente:
                        msg=getString(R.string.tessera_invalida);
                        break;
                    case DiAltroPV:
                        msg=getString(R.string.tessera_altroPV);
                        break;
                    case InUso:
                        msg=getString(R.string.tessera_inUso);
                        break;
                    case Errore:
                        msg="Si è verificato un'errore";
                        break;
                }
                AlertDialog.Builder builder = new AlertDialog.Builder(ModuloActivity.this);
                builder.setMessage(msg)
                        .setPositiveButton("Ok",null)
                        .show().setCanceledOnTouchOutside(false);
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // copiata da Visual Studio 2010\Projects\Librerie\CommonCode\BarCode.vb GetEANCheckDigit
    public static boolean controllaCodiceTessera(@Nullable String codiceTessera) {
        if(codiceTessera==null) return false;
        if(codiceTessera.length()!=13) return false;

        //determino la somma delle cifre con posizione dispari
        int sommaDispari = 0, sommaPari = 0;
        for(int i=0;i<12;i+=2) {
            final int cifraDispari = codiceTessera.charAt(i) - 48;
            final int cifraPari = codiceTessera.charAt(i + 1) - 48;
            if(cifraDispari<0 || cifraDispari>9) return false;
            if(cifraPari<0 || cifraPari>9) return false;
            sommaDispari += cifraDispari;
            sommaPari += cifraPari;
        }
        int somma = (sommaPari * 3) + sommaDispari;
        //determino il check digit
        int checkDigit = somma % 10;
        if(checkDigit!=0) checkDigit=10-checkDigit;

        int checkDigitDato = codiceTessera.charAt(12)-48;
        return checkDigit==checkDigitDato;
    }

    @JavascriptInterface
    public boolean checkTessera(String codiceTessera) {
        return controllaCodiceTessera(codiceTessera);
    }

    void playClick() {
        myWebView.playSoundEffect(SoundEffectConstants.CLICK);
    }

    @JavascriptInterface
    public void vaiIndietro() {
        runOnUiThread(()->{
            playClick();
            onBackPressed();
        });
    }

    @JavascriptInterface
    public void vaiCamera() {
        runOnUiThread(this::playClick);
        Intent i = new Intent(this, ScanActivity.class);
        startActivityForResult(i,1);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(requestCode==1 && resultCode==RESULT_OK && data!=null) {
            WebView webView = findViewById(R.id.webView);
            final String codice = data.getStringExtra("codice");
            //if(controllaCodiceTessera(codice))
                webView.loadUrl("javascript:(SetTessera('"+ codice +"'))");
        }
    }

    @JavascriptInterface
    public void mostraRegolamento(boolean cartaSenior) {
        Intent i = new Intent(this, RegolamentoActivity.class);
        if(cartaSenior)
            i.putExtra("tipoCarta","senior");
        else
            i.putExtra("tipoCarta","normale");
        startActivity(i);
    }



    @JavascriptInterface
    public String getBaseTessera(boolean cartaSenior,String valoreAttuale) {
        if((cartaSenior && baseTesseraSenior==null)||(!cartaSenior && baseTessera==null)) {
            TLFidelityWS web = new TLFidelityWS(this);
            String ret = web.getBaseTesseraSync(cartaSenior);
            if(cartaSenior) baseTesseraSenior=ret;
                      else baseTessera=ret;
        }
        if(baseTessera!=null && !cartaSenior && (valoreAttuale.equals(baseTesseraSenior) || valoreAttuale.length()<4))
                return baseTessera;
        if(baseTesseraSenior!=null && cartaSenior && (valoreAttuale.equals(baseTessera) || valoreAttuale.length()<4))
            return baseTesseraSenior;
        return valoreAttuale;
    }


    @Override
    public void onBackPressed() {
        if(userInput) {
            DialogInterface.OnClickListener listener = (DialogInterface dialog, int which) -> {
                if(which==DialogInterface.BUTTON_POSITIVE) {
                    ModuloActivity.super.onBackPressed();
                }
            };
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setMessage("L'attuale modifica non è stata salvata, uscire?")
                    .setPositiveButton("Sì",listener)
                    .setNegativeButton("No",listener)
                    .show().setCanceledOnTouchOutside(false);
        } else
            super.onBackPressed();
    }

    private boolean userInput = false;

    @JavascriptInterface
    public void DataWritten() {
        userInput = true;
    }
}
