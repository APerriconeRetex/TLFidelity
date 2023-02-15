package com.retexspa.tecnologica.tlmoduloloyalty;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.graphics.Point;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.Transformation;
import android.webkit.JavascriptInterface;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.PreferenceManager;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.TimeUnit;

public class Modulo2Activity extends AppCompatActivity implements TLWebClient.connection {

    @Override
    protected void attachBaseContext(Context base) {
        // questo pezzo di codice serve nel caso in cui la lingua del dispositivo non sia l'Italiano
        super.attachBaseContext(base);
        Configuration overrideConfiguration = base.getResources().getConfiguration();
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
            if(overrideConfiguration.getLocales().get(0).equals(Locale.ITALY))
                return;
        } else {
            if(overrideConfiguration.locale.equals(Locale.ITALY))
                return;
        }
        final Configuration conf = new Configuration(overrideConfiguration);
        conf.setLocale(Locale.ITALY);
        Locale.setDefault(Locale.ITALY);
        base.getResources().updateConfiguration(conf, base.getResources().getDisplayMetrics());
        base.createConfigurationContext(conf);
    }

    private WebView myWebView;
    private TLWebClient webCl;
    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        try {
            setContentView(R.layout.activity_modulo);
            if (myWebView == null) myWebView = findViewById(R.id.webView);
            if (savedInstanceState == null) {
                webCl = new TLWebClient(this);
                webCl.activityInterface = this;
                myWebView.setWebViewClient(webCl);
                myWebView.addJavascriptInterface(this, "tecno");
                // chrome://inspect/#devices
                //myWebView.setWebContentsDebuggingEnabled(true);

                WebSettings settings = myWebView.getSettings();
                settings.setUserAgentString("it_IT");
                settings.setAllowFileAccessFromFileURLs(true);
                settings.setAllowUniversalAccessFromFileURLs(true);
                settings.setJavaScriptEnabled(true);
                myWebView.loadUrl("file:///android_asset/index3.html");

                if(menuPrincipaleActivity.getIdCliente()>0) {
                    TLFidelityWS web = new TLFidelityWS(this);
                    web.loadCliente(menuPrincipaleActivity.getIdCliente(),this::clienteCaricato);
                }
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private void clienteCaricato(ClienteInfo clienteInfo) {
        webCl.setCliente(clienteInfo.tojQuery());
        menuPrincipaleActivity.setIdCliente(clienteInfo.id, clienteInfo.codiceTessera, clienteInfo.cartaSenior );
        myWebView.loadUrl("javascript:richiedCliente()");
    }

    @Override
    public void onConfigurationChanged(@NonNull Configuration newConfig) {
        super.onConfigurationChanged(newConfig);

        DisplayMetrics displayMetrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
        final Point size = new Point(displayMetrics.widthPixels,displayMetrics.heightPixels);

        View black = findViewById(R.id.nero);
        if(black.getVisibility()==View.VISIBLE) {
            firmaView firma = findViewById(R.id.firma1);
            if(firma.getVisibility()!=View.VISIBLE)
                firma = findViewById(R.id.firma2);
            firma.updateSize(size);
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
    public void mostraInformativa() {
        Intent i = new Intent(this, RegolamentoActivity.class);
        if(menuPrincipaleActivity.getCartaSenior())
            i.putExtra("tipoCarta","senior");
        else
            i.putExtra("tipoCarta","normale");
        startActivity(i);
    }

    @JavascriptInterface
    public void salvaCliente(String json) {
        if(!getFirma(1).hasData() || !getFirma(2).hasData())  { //||clienteid!=-1
            runOnUiThread(()-> {
                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setMessage("le firme sono anch'esse obbligatorie")
                        .setPositiveButton("OK",null)
                        .show().setCanceledOnTouchOutside(false);
            });
            return;
        }

        ClienteInfo info = new ClienteInfo();
        try {
            info.FromForm(json);
            //inserisco blocco su invio sms
            AlertDialog.Builder builderStop = new AlertDialog.Builder(Modulo2Activity.this);
            final AlertDialog  dlgStop = builderStop.setView(R.layout.downloading_dialog).show();
            final ProgressBar bar=dlgStop.findViewById(R.id.progressBar);
            TextView msg = dlgStop.findViewById(R.id.messaggio);
            msg.setText("Invio sms in corso");
            dlgStop.setCanceledOnTouchOutside(false);
            bar.setIndeterminate(true);

            //inserisco richiesta OTP
            /*
              int min = 100000;
                int max = 999999;
                Random rand = new Random();
                int otpNum = rand.nextInt((max-min)+1)+min;

             */
            Date current = Calendar.getInstance().getTime();
            SimpleDateFormat dfYear = new SimpleDateFormat("yyyy");
            SimpleDateFormat dfFirst = new SimpleDateFormat("MM/dd/yyyy");
            String year = dfYear.format(current);
            Date first = dfFirst.parse("01/01/"+year);

            String cellulare = info.cellulare.trim();

            int otpNum = (int) (Math.round(Long.parseLong(cellulare)/999999) - 365 + TimeUnit.MILLISECONDS.toDays(current.getTime() - first.getTime()));

            String otp = String.valueOf(otpNum);
                Map<String,String> postData = new HashMap<>();
                if(cellulare.startsWith("00")) cellulare=cellulare.substring(2); else
                if(cellulare.startsWith("+")) cellulare=cellulare.substring(1); else
                    cellulare="39"+cellulare;
                postData.put("phone", cellulare);
                postData.put("sender_id","TLFidelity");
                String sms = otp +" è il tuo codice di verifica *Crai Carta Più*";
                postData.put("body",sms);
                postData.put("receive_dlr","off");
                //inserimento storicizzazione otp:    macaddress, data, ora, cedi, pv, cellulare, tessera, stato, cliente, sms
                StoricizzazioneOTPInfo storOtp = new StoricizzazioneOTPInfo();

                SharedPreferences sharedPreferences= PreferenceManager.getDefaultSharedPreferences(this.getApplicationContext());
                storOtp.mac = sharedPreferences.getString("MAC","");
                storOtp.cognome = info.cognome;
                storOtp.nome = info.nome;
                storOtp.cellulare = cellulare;
                storOtp.tessera = info.codiceTessera;
                storOtp.idStato = 1; //inviato
                storOtp.sms = sms;

            TLFidelityWS web = new TLFidelityWS(this);
                web.salvaOTP(storOtp,(Boolean done)->{
                if(!done) {
                    AlertDialog.Builder builder = new AlertDialog.Builder(this);
                    builder.setMessage("Errore salvando la storicizzazione OTP")
                            .setPositiveButton("OK", null)
                            .show().setCanceledOnTouchOutside(false);
                    dlgStop.cancel();
                } else {

                    //controllo se il mac è autorizzato all'invio della mail
                   web.checkInvioMail(storOtp.mac, (Boolean invia) -> {

                            //controllo che l'utente abbia inserito la mail
                            //invio mail
                            if(invia && info.email.length()>0) {
                                web.emailOTP(otp, info, (Void) -> {});
                            }


                    });




                    //invio sms
                OTPAsyncTask task = new OTPAsyncTask(postData, Modulo2Activity.this, new AsyncResponse() {

                    @Override
                    public void processFinish(ArrayList<Object> result) {
                        dlgStop.cancel();
                         String message = (String) (result!=null? result.get(1) : "Errore chiamata OTP.");
                            Integer codeMessage =  (Integer) (result!=null? result.get(3) : -1);



                        if (message=="Ok") {
                                storOtp.idStato = 2;
                            runOnUiThread(()-> {
                                String otp = (String) result.get(0);

                                AlertDialog.Builder builder = new AlertDialog.Builder(Modulo2Activity.this);
                                //builder.setTitle("Inserimento OTP");
                                final AlertDialog ad = builder.setView(R.layout.otp_popup).show();
                                TextView otpCell = (TextView) ad.findViewById(R.id.otpCell);
                                String cellOTP;

                                otpCell.setText("Gentile cliente il messaggio è stato inviato al numero "+ storOtp.cellulare.substring(2));

                                ad.setCanceledOnTouchOutside(false);
                                Button closeButton = (Button) ad.findViewById(R.id.backOTP);
                                closeButton.postDelayed(new Runnable() {
                                    @Override
                                    public void run() {
                                        if (!isDestroyed() && !isFinishing())
                                            closeButton.setEnabled(Boolean.TRUE);
                                    }
                                },9000);
                                Button okButton = (Button) ad.findViewById(R.id.okOTP);

                                // Add action buttons
                                okButton.setOnClickListener(new View.OnClickListener() {
                                    @Override
                                    public void onClick(View view) {

                                        TextView otpTxt = (TextView) ad.findViewById(R.id.otpText);
                                        String otp2 = (String) otpTxt.getText().toString();
                                        if (otp.equals(otp2) ) {
                                            // gli otp sono corretti possiamo proseguire
                                            //salvataggio dati memorizzando anche data ora dei consensi inseriti ed il punto vendita
                                                runOnUiThread(() -> salvaClienteOnUi(info,ad, storOtp));

                                        } else {
                                            runOnUiThread(()-> {
                                                AlertDialog.Builder builder = new AlertDialog.Builder(Modulo2Activity.this);
                                                final AlertDialog adError = builder.setView(R.layout.otp_error_popup).show();
                                                TextView otpCellError = (TextView) adError.findViewById(R.id.otpCell);
                                                String error = "Gentile cliente a causa di un errore non è stato possibile inviare il messaggio al numero "+ storOtp.cellulare.substring(2);
                                                otpCellError.setText(error + " (Il codice di verifica inserito non è corretto)");
                                                adError.setCanceledOnTouchOutside(false);
                                                Button closeButtonError = (Button) adError.findViewById(R.id.backOTP);
                                                closeButtonError.setOnClickListener(new View.OnClickListener() {
                                                    @Override
                                                    public void onClick(View view) {
                                                        adError.dismiss();
                                                    }
                                                });

                                               /*
                                                AlertDialog.Builder builderMsg = new AlertDialog.Builder(Modulo2Activity.this);
                                                builderMsg.setTitle("Errore")
                                                        .setMessage("Il codice di verifica inserito non è corretto")
                                                        .setPositiveButton("OK", (DialogInterface dialog, int which) ->
                                                                dialog.dismiss())
                                                        .show();

                                                */
                                                });
                                        }
                                    }
                                });

                                closeButton.setOnClickListener(new View.OnClickListener() {
                                    @Override
                                    public void onClick(View view) {
                                        ad.dismiss();
                                    }
                                });



                            });

                        } else {
                                switch (codeMessage){
                                    case 400:
                                        storOtp.idStato = 4;
                                        break;
                                    case 401:
                                        storOtp.idStato = 5;
                                        break;
                                    default:
                                        storOtp.idStato = 6;
                                        break;
                                }

                            //visualizzo un messaggio di errore'
                            runOnUiThread(()-> {
                                AlertDialog.Builder builder = new AlertDialog.Builder(Modulo2Activity.this);
                                final AlertDialog adError = builder.setView(R.layout.otp_error_popup).show();
                                TextView otpCellError = (TextView) adError.findViewById(R.id.otpCell);
                                String error = "Gentile cliente a causa di un errore non è stato possibile inviare il messaggio al numero "+ storOtp.cellulare.substring(2);
                                otpCellError.setText(error + " ("+message+")");
                                adError.setCanceledOnTouchOutside(false);
                                Button closeButtonError = (Button) adError.findViewById(R.id.backOTP);
                                closeButtonError.setOnClickListener(new View.OnClickListener() {
                                    @Override
                                    public void onClick(View view) {
                                        adError.dismiss();
                                    }
                                });

                                /*
                            AlertDialog.Builder builderMsg = new AlertDialog.Builder(Modulo2Activity.this);
                            builderMsg.setTitle("Errore")
                                    .setMessage(message)
                                    .setPositiveButton("OK", null )
                                    .show();

                                 */
                            });
                        }

//inserisco

                            web.salvaOTP(storOtp,(Boolean done)->{
                                if(!done) {
                                    /*
                                    builder.setMessage("Errore salvando la storicizzazione OTP")
                                            .setPositiveButton("OK", null)
                                            .show();

                                     */
                                    AlertDialog.Builder builder = new AlertDialog.Builder(Modulo2Activity.this);
                                    final AlertDialog adError = builder.setView(R.layout.otp_error_popup).show();
                                    TextView otpCellError = (TextView) adError.findViewById(R.id.otpCell);
                                    String error = "Gentile cliente a causa di un errore non è stato possibile inviare il messaggio al numero "+ storOtp.cellulare.substring(2);
                                    otpCellError.setText(error + " (Errore salvando la storicizzazione OTP)");
                                    adError.setCanceledOnTouchOutside(false);
                                    Button closeButtonError = (Button) adError.findViewById(R.id.backOTP);
                                    closeButtonError.setOnClickListener(new View.OnClickListener() {
                                        @Override
                                        public void onClick(View view) {
                                            adError.dismiss();
                                        }
                                    });

                                } });




                    }
                });
                task.execute("https://api.transactionale.com/platform/api/sms/send",otp);
            }
            });


            }
            catch (Exception e) {
                e.printStackTrace();
                runOnUiThread(()->{
                    AlertDialog.Builder builder = new AlertDialog.Builder(this);
                    builder.setMessage("Errore nella richiesta OTP")
                            .setPositiveButton("OK",null)
                            .show().setCanceledOnTouchOutside(false);
                });
            }
    }

    private void salvaClienteOnUi(ClienteInfo info, AlertDialog ad, StoricizzazioneOTPInfo storOtp) {
        TLFidelityWS web = new TLFidelityWS(this);
        web.salvaCliente(info,(Boolean done)->{
            if(!done) {
                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setMessage("Errore salvando il cliente")
                        .setPositiveButton("OK", null)
                        .show().setCanceledOnTouchOutside(false);
            } else {
                storOtp.idStato = 3;
                        SalvataggioRiuscito(info, web,storOtp);


                ad.dismiss();

            }
        });
    }


    private void SalvataggioRiuscito(ClienteInfo info, TLFidelityWS web, StoricizzazioneOTPInfo storOTP) {
        web.controllaTripletta(info.codiceTessera,info.cognome,info.nome,(Integer idCliente)->{
            if(idCliente>0) {
                TLFidelityWS.firme firme = new TLFidelityWS.firme();
                firme.id = idCliente;
                firme.firma1 = getFirma(1).getImage(900,300);
                firme.firma2 = getFirma(2).getImage(900,300);
                web.SalvaFirme(firme,(Boolean done)-> {
                    if (!done) {
                        AlertDialog.Builder builder = new AlertDialog.Builder(this);
                        builder.setMessage("Errore salvando il cliente")
                                .setPositiveButton("OK", null)
                                .show().setCanceledOnTouchOutside(false);
                    } else {
                        if(menuPrincipaleActivity.getIdCliente()!=idCliente && info.email.length()>0) {
                            web.emailCliente(idCliente, info, (Void) -> {});
                        }
                        // de-commentare nel caso si voglia mantenere questa schermata dopo il salvataggio
                        //menuPrincipaleActivity.setIdCliente( idCliente, info.codiceTessera, menuPrincipaleActivity.getCartaSenior());
                        DialogInterface.OnClickListener listener = (DialogInterface dialog, int which) -> {
                            Intent intent = new Intent(getApplicationContext(), menuPrincipaleActivity.class);
                            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                            startActivity(intent);
                        };
                        userInput = false;

                        storOTP.idCliente = idCliente;
                        web.salvaOTP(storOTP,(Boolean done2)->{
                            if(!done2) {
                                AlertDialog.Builder builder2 = new AlertDialog.Builder(Modulo2Activity.this);
                                builder2.setMessage("Cliente salvato con successo. Rilevato errore durante la storicizzazione OTP.")
                                        .setPositiveButton("OK", listener)
                                        .show().setCanceledOnTouchOutside(false);
                            } else {
                        AlertDialog.Builder builder = new AlertDialog.Builder(this);
                        builder.setMessage("Cliente salvato con successo.")
                                .setPositiveButton("OK", listener)
                                .show().setCanceledOnTouchOutside(false);
                            }});
                    }
                });
            }  // else non dovrebbe mai succedere
        });
    }

    @JavascriptInterface
    public void vaiIndietro() {
        runOnUiThread(this::onBackPressed);
    }

    @Override
    public void onBackPressed() {
        if(userInput) {
            DialogInterface.OnClickListener listener = (DialogInterface dialog, int which) -> {
                if(which==DialogInterface.BUTTON_POSITIVE) {
                    finish();
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

    @JavascriptInterface
    public String getUrlWS() {
        SharedPreferences preference= PreferenceManager.getDefaultSharedPreferences(this);
        String baseUrl = preference.getString("WSURL",null);
        if(baseUrl!=null && baseUrl.endsWith("/"))
            baseUrl=baseUrl.substring(0,baseUrl.length()-1);
        return baseUrl;
    }

    private boolean userInput = false;

    @JavascriptInterface
    public void DataWritten() {
        userInput = true;
    }

    @JavascriptInterface
    public void faiFirma(int id, int x,int y,int w,int h) {
        final View okBtn = findViewById(R.id.ok);
        final View cancelBtn=findViewById(R.id.cancel);
        runOnUiThread(() -> {
            DisplayMetrics displayMetrics = new DisplayMetrics();
            getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
            final int realY = y+displayMetrics.widthPixels/15;
            firmaView view = getFirma(id);
            final Point size = new Point(displayMetrics.widthPixels,displayMetrics.heightPixels);
            view.VaiPienoSchermoDa(x, realY, w, h, size);
            View black = findViewById(R.id.nero);
            black.setVisibility(View.VISIBLE);
            black.setAlpha(0.5f);
            view.okBtn = okBtn;
            view.cancelBtn = cancelBtn;
        } );
        okBtn.setOnClickListener((View v)-> nascondiFirma(id));
        cancelBtn.setOnClickListener((View v)-> getFirma(id).Cancella());
    }

    private void nascondiFirma(int id) {
        String idStr = Integer.toString(id);
        String tmp = Long.valueOf(System.currentTimeMillis()).toString();
        myWebView.loadUrl("javascript:(()=> {$('#firma"+idStr+"')[0].src='tl:/firma"+idStr+".png?"+tmp+"';})()");
        runOnUiThread(() -> {
            final firmaView firma = getFirma(id);
            final View black = findViewById(R.id.nero);
            Animation a = new Animation() {
                @Override
                protected void applyTransformation(float interpolatedTime, Transformation t) {
                        firma.setAlpha(1-interpolatedTime);
                        firma.okBtn.setAlpha(1-interpolatedTime);
                        firma.cancelBtn.setAlpha(1-interpolatedTime);
                        black.setAlpha((1-interpolatedTime)/2);
                        if(interpolatedTime==1) {
                            firma.setVisibility(View.INVISIBLE);
                            firma.okBtn.setVisibility(View.INVISIBLE);
                            firma.cancelBtn.setVisibility(View.INVISIBLE);
                            black.setVisibility(View.INVISIBLE);
                            firma.setAlpha(1);
                            firma.okBtn.setAlpha(1);
                            firma.cancelBtn.setAlpha(1);
                            black.setAlpha(0.5f);
                        }
                    }
            };
            a.setDuration(500); // in ms
            firma.startAnimation(a);
        });
    }

    @Override
    public firmaView getFirma(int id) {
        switch(id) {
            case 1: return findViewById(R.id.firma1);
            case 2: return findViewById(R.id.firma2);
        }
        return null;
    }

    @JavascriptInterface
    public String  getCodiceFiscale(String cognome,String nome,String dataNascita,boolean maschio,String citta) throws Exception {
        LocalDB db = new LocalDB(this);
        String[] data = dataNascita.split("/");
        int giorno, mese, anno;
        try {
            giorno = data.length>0? Integer.parseInt(data[0]) : 1;
        } catch(Exception ex) { giorno=1; }
        try {
            mese = data.length>1? Integer.parseInt(data[1]) : 1;
        } catch(Exception ex) { mese=1; }
        try {
            anno = data.length>2? Integer.parseInt(data[2]) : 1920;
        } catch(Exception ex) { anno=1920; }
        String prov="";
        int pos = citta.indexOf(" (");
        if(pos>0) {
            try {
                prov = citta.substring(pos + 2, pos + 4);
                citta = citta.substring(0, pos);
            } catch (Exception ignored) { }
        }

        if (citta.equals("ESTERO")){
            return  "";
        }

        CodiceFiscale cf = new CodiceFiscale(nome,cognome, giorno, mese, anno, maschio,citta,prov,db);
        return cf.calcola();
    }

    @JavascriptInterface
    public boolean checkCitta(String citta,String prov) {
        if(citta.length()<2) return false;
        int pos = citta.indexOf(" (");
        if(pos>0 && prov.length() == 0) {
            try {
                prov = citta.substring(pos + 2, pos + 4);
                citta = citta.substring(0, pos);
            } catch (Exception ignored) { }
        }
        LocalDB db = new LocalDB(this);
        return db.checkCitta(citta, prov);
    }

    @JavascriptInterface
    public boolean checkProv(String prov,String citta) {
        if(prov.length()!=2) return false;
        if(citta.length()<2) return false;
        LocalDB db = new LocalDB(this);
        return db.checkCitta(citta, prov);
    }

    @JavascriptInterface
    public boolean checkCap(String cap,String citta,String prov) {
        if(prov.length()!=2) return false;
        if(citta.length()<2) return false;
        if(cap.length()!=5) return false;
        LocalDB db = new LocalDB(this);
        return db.checkCap(cap, citta, prov);
    }
}
