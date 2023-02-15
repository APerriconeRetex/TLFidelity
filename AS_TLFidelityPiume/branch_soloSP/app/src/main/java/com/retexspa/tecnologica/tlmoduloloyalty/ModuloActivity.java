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
import android.util.Log;
import android.view.MenuItem;
import android.view.SoundEffectConstants;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.Transformation;
import android.webkit.JavascriptInterface;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.widget.Button;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Random;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.PreferenceManager;

import static com.retexspa.tecnologica.tlmoduloloyalty.TLFidelityWS.statoCodiceTessera.Valida;

public class ModuloActivity extends AppCompatActivity implements TLWebClient.connection {

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
    static String baseTessera;
    static String baseTesseraSenior;
    private TLWebClient webCl;
    private SharedPreferences sharedPreferences;

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        try {
            sharedPreferences= PreferenceManager.getDefaultSharedPreferences(this);
            setContentView(R.layout.activity_modulo);

            if(myWebView==null) myWebView = findViewById(R.id.webView);
            if (savedInstanceState == null) {
                webCl = new TLWebClient(this);
                webCl.activityInterface = this;
                myWebView.setWebViewClient(webCl);
                myWebView.addJavascriptInterface(this, "tecno");
            // chrome://inspect/#devices
           myWebView.setWebContentsDebuggingEnabled(true);

            WebSettings settings = myWebView.getSettings();
            settings.setUserAgentString("it_IT");
            settings.setAllowFileAccessFromFileURLs(true);
            settings.setAllowUniversalAccessFromFileURLs(true);
            settings.setJavaScriptEnabled(true);
            myWebView.loadUrl("file:///android_asset/index1.html");

                if(menuPrincipaleActivity.getIdCliente()>0) {
                    TLFidelityWS web = new TLFidelityWS(this);
                    web.loadCliente(menuPrincipaleActivity.getIdCliente(),this::clienteCaricato);
                }
            }
        } catch (Exception ex) {
            ex.printStackTrace();
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
    public void vaiAvanti(String codiceTessera, String json) {
        if(codiceTessera.equals(baseTessera)) baseTessera = null;
        if(codiceTessera.equals(baseTesseraSenior)) baseTesseraSenior = null;
        if(!controllaCodiceTessera(codiceTessera)) {
            AlertDialog.Builder builder = new AlertDialog.Builder(ModuloActivity.this);
            builder.setMessage(getString(R.string.tessera_invalida))
                    .setPositiveButton("Ok",null)
                    .show();
            return;
        }
        TLFidelityWS ws = new TLFidelityWS(this);

        Bundle b = getIntent().getExtras();
        if (b!= null  ) {
            //Modifica
            salvaCliente(json);
            return;
        }
        else {
            try {
                ws.controllaTessera(codiceTessera, (TLFidelityWS.statoCodiceTessera stato) -> {
                    if (stato == Valida) {
                        runOnUiThread(() -> {
                            menuPrincipaleActivity.setIdCliente(-1, codiceTessera);

                        });
                        salvaCliente(json);
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
                            msg = "Il tipo di tessera selezionata non è corretto";
                            break;
                        case Inesistente:
                            msg = getString(R.string.tessera_invalida);
                            break;
                        case DiAltroPV:
                            msg = getString(R.string.tessera_altroPV);
                            break;
                        case InUso:
                            msg = getString(R.string.tessera_inUso);
                            break;
                        case Errore:
                            msg = "Si è verificato un'errore";
                            break;
                    }
                    AlertDialog.Builder builder = new AlertDialog.Builder(ModuloActivity.this);
                    builder.setMessage(msg)
                            .setPositiveButton("Ok", null)
                            .show();
                });

            } catch (Exception e) {
                e.printStackTrace();
            }
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
                    .show();
        } else
            super.onBackPressed();
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
        myWebView.loadUrl("javascript:(()=> {$('#firma"+idStr+"')[0].src='tl:/firma"+idStr+".png?id="+tmp+"';})()");
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
        }
        return null;
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

            firma.updateSize(size);
        }
    }

    @JavascriptInterface
    public String  getCodiceFiscale(String cognome,String nome,String dataNascita,boolean maschio,String citta, String prov) throws Exception {
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


        if (citta.equals("ESTERO")){
            return  "";
        }

        CodiceFiscale cf = new CodiceFiscale(nome,cognome, giorno, mese, anno, maschio,citta,prov,db);
        return cf.calcola();
    }

    @JavascriptInterface
    public boolean checkCitta(String citta,String prov) {
        if(citta.length()<2) return false;

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

    @JavascriptInterface
    public String getUrlWS() {
        SharedPreferences preference= PreferenceManager.getDefaultSharedPreferences(this);
        String baseUrl = preference.getString("WSURL",null);
        if(baseUrl!=null && baseUrl.endsWith("/"))
            baseUrl=baseUrl.substring(0,baseUrl.length()-1);
        return baseUrl;
    }

    public void salvaCliente(String json) {
        if(!getFirma(1).hasData())  { //||clienteid!=-1
            runOnUiThread(()-> {
                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setMessage("La firma e' obbligatoria")
                        .setPositiveButton("OK",null)
                        .show();
            });
            return;
        }

        ClienteInfo info = new ClienteInfo();
        try {
            info.FromForm(json);

            //inserisco richiesta OTP

            int min = 100000;
            int max = 999999;
            Random rand = new Random();
            int otpNum = rand.nextInt((max-min)+1)+min;
            String otp = String.valueOf(otpNum);
            Map<String,String> postData = new HashMap<>();
            String cellulare = info.cellulare.trim();
            if(cellulare.startsWith("00")) cellulare=cellulare.substring(2); else
            if(cellulare.startsWith("+")) cellulare=cellulare.substring(1); else
                cellulare="39"+cellulare;
            postData.put("phone", cellulare);
            postData.put("sender_id","TLFidelity");
            String sms = otp +" è il tuo codice di verifica *PiùCard PiùMe*";
            postData.put("body",sms);
            postData.put("receive_dlr","off");
            //inserimento storicizzazione otp:    macaddress, data, ora, cedi, pv, cellulare, tessera, stato, cliente, sms
            StoricizzazioneOTPInfo storOtp = new StoricizzazioneOTPInfo();

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
                            .show();
                } else {
                    OTPAsyncTask task = new OTPAsyncTask(postData, ModuloActivity.this, new AsyncResponse() {

                        @Override
                        public void processFinish(ArrayList<Object> result) {
                            String message = (String) (result!=null? result.get(1) : "Errore chiamata OTP.");
                            Integer codeMessage =  (Integer) (result!=null? result.get(3) : -1);
                            if (message=="Ok") {
                                storOtp.idStato = 2;
                                runOnUiThread(()-> {
                                    String otp = (String) result.get(0);

                                    AlertDialog.Builder builder = new AlertDialog.Builder(ModuloActivity.this);
                                    builder.setTitle("Inserimento OTP");
                                    final AlertDialog ad = builder.setView(R.layout.otp_popup).show();

                                    Button closeButton = (Button) ad.findViewById(R.id.cancel);
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

                                                    AlertDialog.Builder builderMsg = new AlertDialog.Builder(ModuloActivity.this);
                                                    builderMsg.setTitle("Errore")
                                                            .setMessage("Il codice di verifica inserito non è corretto")
                                                            .setPositiveButton("OK", (DialogInterface dialog, int which) ->
                                                                    dialog.dismiss())
                                                            .show();
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
                                    AlertDialog.Builder builderMsg = new AlertDialog.Builder(ModuloActivity.this);
                                    builderMsg.setTitle("Errore")
                                            .setMessage(message)
                                            .setPositiveButton("OK", null )
                                            .show();
                                });
                            }
//inserisco

                            web.salvaOTP(storOtp,(Boolean done)->{
                                if(!done) {
                                    AlertDialog.Builder builder = new AlertDialog.Builder(ModuloActivity.this);
                                    builder.setMessage("Errore salvando la storicizzazione OTP")
                                            .setPositiveButton("OK", null)
                                            .show();
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
                        .show();
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
                        .show();
            } else {
                storOtp.idStato = 3;
                SalvataggioRiuscito(info, web, storOtp);


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

                web.SalvaFirme(firme,(Boolean done)-> {
                    if (!done) {
                        AlertDialog.Builder builder = new AlertDialog.Builder(this);
                        builder.setMessage("Errore salvando il cliente")
                                .setPositiveButton("OK", null)
                                .show();
                    } else {
                        if(menuPrincipaleActivity.getIdCliente()!=idCliente && info.email.length()>0) {
                            web.insertLog(sharedPreferences.getString("MAC",""), 0, ModuloActivity.class .getName(),"Sto per inviare la mail ","", (Integer done2)->{
                                if(done2==-1) {
                                    Log.e(ModuloActivity.class.getName(),"Errore inserimento log email cliente");
                                }

                            });
                            web.insertLog(sharedPreferences.getString("MAC",""), 0, ModuloActivity.class .getName(),"URL centralizzato "+ web.getBaseUrlCentralizzato(),"", (Integer done2)->{
                                if(done2==-1) {
                                    Log.e(ModuloActivity.class.getName(),"Errore inserimento log email cliente");
                                }

                            });
                            web.insertLog(sharedPreferences.getString("MAC",""), 0, ModuloActivity.class .getName(),"URL: "+web.getBaseUrl(),"", (Integer done2)->{
                                if(done2==-1) {
                                    Log.e(ModuloActivity.class.getName(),"Errore inserimento log email cliente");
                                }

                            });
                            web.insertLog(sharedPreferences.getString("MAC",""), 0, ModuloActivity.class .getName(),"Valore useCentralizzato: "+Boolean.toString(web.getUseCentralizzato()),"", (Integer done2)->{
                                if(done2==-1) {
                                    Log.e(ModuloActivity.class.getName(),"Errore inserimento log email cliente");
                                }

                            });
                          try {
                                web.emailCliente(idCliente, info, (Void) -> {});
                                web.insertLog(sharedPreferences.getString("MAC",""), 0, ModuloActivity.class .getName(),"Mail inviata correttamente","", (Integer done2)->{
                                  if(done2==-1) {
                                      Log.e(ModuloActivity.class.getName(),"Errore inserimento log email cliente");
                                  }

                                });
                          } catch (Exception ex) {
                              web.insertLog(sharedPreferences.getString("MAC",""), 0, ModuloActivity.class .getName(),"Errore invio mail cliente: "+ex.getMessage().toString(),"", (Integer done2)->{
                                  if(done2==-1) {
                                      Log.e(ModuloActivity.class.getName(),"Errore inserimento log email cliente");
                                  }

                              });
                              ex.printStackTrace();
                          }
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
                                AlertDialog.Builder builder2 = new AlertDialog.Builder(ModuloActivity.this);
                                builder2.setMessage("Cliente salvato con successo. Rilevato errore durante la storicizzazione OTP.")
                                        .setPositiveButton("OK", listener)
                                        .show();
                            } else {
                                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                                builder.setMessage("Cliente salvato con successo.")
                                        .setPositiveButton("OK", listener)
                                        .show();
                            }});
                    }
                });
            }  // else non dovrebbe mai succedere
        });
    }

    private void clienteCaricato(ClienteInfo clienteInfo) {
        webCl.setCliente(clienteInfo.tojQuery());
        menuPrincipaleActivity.setIdCliente(clienteInfo.id, clienteInfo.codiceTessera );
        myWebView.loadUrl("javascript:richiedCliente()");
    }

    @JavascriptInterface
    public void mostraInformativa() {
        Intent i = new Intent(this, RegolamentoActivity.class);

        startActivity(i);
    }




}
