package com.retexspa.tecnologica.tlmoduloloyalty;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.SoundEffectConstants;
import android.view.View;
import android.webkit.JavascriptInterface;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import static com.retexspa.tecnologica.tlmoduloloyalty.ModuloActivity.controllaCodiceTessera;

public class ModificaActivity extends AppCompatActivity {

    private WebView myWebView;

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
            myWebView.loadUrl("file:///android_asset/index_m.html");
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
    public boolean checkTessera(String codiceTessera) {
        return controllaCodiceTessera(codiceTessera);
    }

    @JavascriptInterface
    public void vaiAvanti(String cognome,String nome,String codiceTessera) {
        TLFidelityWS ws = new TLFidelityWS(this);
        try {
            ws.controllaTripletta(codiceTessera,cognome,nome,(Integer idCliente)->{
                if(idCliente>0) {
                    menuPrincipaleActivity.setIdCliente( idCliente, codiceTessera,false);
                    Intent i = new Intent(this, Modulo2Activity.class);
                    startActivity(i);
                } else {
                    String msg="Si è verificato un'errore. Vuole inviare una segnalazione al customer care?";
                    TLFidelityWS.StatoCliente stato =TLFidelityWS.StatoCliente.valueOf(idCliente);
                    if (stato != null) {
                        switch (stato) {
                            case DiAltroCedi:
                                msg = getString(R.string.modifica_altroCedi);
                                break;
                            case NonTrovato:
                                msg = getString(R.string.modifica_nonTrovatoMess);
                                break;
                        }
                    }
                    DialogInterface.OnClickListener listener = (DialogInterface dialog, int which) -> {
                        if (which == DialogInterface.BUTTON_POSITIVE) {



                           // LayoutInflater inflater = LayoutInflater.from(ModificaActivity.this);
                            AlertDialog.Builder builder = new AlertDialog.Builder(ModificaActivity.this);
                            //View view = inflater.inflate(R.layout.customer_care_popup, null);
                            builder.setView(R.layout.customer_care_popup);
                            final AlertDialog dialogInvia = builder
                                    .setNegativeButton("Annulla", null)
                                    .setPositiveButton("OK",null).show();
                            dialogInvia.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(new View.OnClickListener()
                            {
                                @Override public void onClick(View btn)
                                {
                                    EditText emailTxt = (EditText) (dialogInvia).findViewById(R.id.emailText);
                                    TextView msgError = (TextView) (dialogInvia).findViewById(R.id.errorMsg);

                                    String email = (String)emailTxt.getText().toString().trim();
                                    //String emailPattern = "[a-zA-Z0-9._-]+@[a-z]+\\.+[a-z]+";
                                    String emailPattern = "^([a-zA-Z0-9_\\-\\.]+)@([a-zA-Z0-9_\\-\\.]+)\\.([a-zA-Z]{2,5})$";
                                    if (email.matches(emailPattern)) {
                                        msgError.setVisibility(View.INVISIBLE);
                                        //invio la mail
                                        ws.inviaSegnalazione(email,codiceTessera,cognome,nome,(Boolean mailInviata)-> {
                                            AlertDialog.Builder builder = new AlertDialog.Builder(ModificaActivity.this);

                                            if(mailInviata) {
                                                builder.setMessage("Mail inviata correttamente al customer care.")
                                                        .setPositiveButton("Ok",null)
                                                        .show().setCanceledOnTouchOutside(false);
                                            } else {
                                                builder.setMessage("Errore nell'invio della mail al customer care.")
                                                        .setPositiveButton("Ok",null)
                                                        .show().setCanceledOnTouchOutside(false);
                                            }
                                        });
                                        dialogInvia.dismiss();
                                    }
                                    else {
                                        msgError.setVisibility(View.VISIBLE);
                                    }
                                }
                            });
                        }
                    };
                    AlertDialog.Builder builder = new AlertDialog.Builder(ModificaActivity.this);
                    builder.setMessage(msg)
                            .setPositiveButton("Sì",listener)
                            .setNegativeButton("No",null)
                            .show().setCanceledOnTouchOutside(false);
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }

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
        if(requestCode==1 && resultCode==RESULT_OK && data != null) {
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

    static String baseTessera;
    @JavascriptInterface
    public String getBaseTessera() {
        if(baseTessera==null) {
            TLFidelityWS web = new TLFidelityWS(this);
            baseTessera= web.getBaseTesseraSync();
        }
        return baseTessera;
    }

    //private boolean userInput = false;

    @JavascriptInterface
    public void DataWritten() {
      //  userInput = true;
    }
}
