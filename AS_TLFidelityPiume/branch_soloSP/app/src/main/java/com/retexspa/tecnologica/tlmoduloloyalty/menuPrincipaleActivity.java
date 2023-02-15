package com.retexspa.tecnologica.tlmoduloloyalty;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.Uri;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.util.TypedValue;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.FileProvider;
import androidx.preference.PreferenceManager;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.OutputStream;
import java.net.NetworkInterface;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import static com.retexspa.tecnologica.tlmoduloloyalty.TLFidelityWS.CostantiAutorizzazione.GiaChiesto;
import static com.retexspa.tecnologica.tlmoduloloyalty.TLFidelityWS.CostantiAutorizzazione.MaiChiesto;

public class menuPrincipaleActivity extends AppCompatActivity {

   private Dialog saveDialogs;
   private ActivityResultLauncher<Intent> InstallaVersioneResultLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        InstallaVersioneResultLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK) {
                        Intent data = result.getData();
                        if(data!=null && data.getData()!=null) {
                            LanciaInstallazione(data.getData());
                        }
                    }
                });
        setContentView(R.layout.activity_menu_principale);
        TextView scritta1 = findViewById(R.id.scritta1);
        TextView scritta2 = findViewById(R.id.scritta2);
        TextView versione = findViewById(R.id.versione);
        PackageManager manager = this.getPackageManager();
        PackageInfo info = null;
        try {
            info = manager.getPackageInfo(this.getPackageName(), PackageManager.GET_ACTIVITIES);
            long ver;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                ver = info.getLongVersionCode() & 0xFFFFFFFF;
            } else
                ver = (long)info.versionCode;
            versione.setText("TLCustomerApp vers. " + String.valueOf(ver));
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
            versione.setText("");
        }

        ViewTreeObserver.OnDrawListener setText = new ViewTreeObserver.OnDrawListener() {
            private float size=0;
            @Override
            public void onDraw() {
                if (size == 0) {
                    float s1 = scritta1.getTextSize();
                    float s2 = scritta2.getTextSize();
                    if (s1 != 0 && s2 != 0) {
                        size = (Math.min(s1, s2));
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                            scritta1.setAutoSizeTextTypeWithDefaults(TextView.AUTO_SIZE_TEXT_TYPE_NONE);
                            scritta2.setAutoSizeTextTypeWithDefaults(TextView.AUTO_SIZE_TEXT_TYPE_NONE);
                        }
                        scritta1.setTextSize(TypedValue.COMPLEX_UNIT_PX,size);
                        scritta2.setTextSize(TypedValue.COMPLEX_UNIT_PX,size);
                    }
                }
            }
        };
        scritta1.getViewTreeObserver().addOnDrawListener(setText);
        scritta2.getViewTreeObserver().addOnDrawListener(setText);

        Toolbar toolbar = findViewById(R.id.toolbar);
        toolbar.inflateMenu(R.menu.main_menu);
        toolbar.setSubtitleTextColor(0xFFFFFFFF);
        toolbar.setOnMenuItemClickListener(menuPrincipaleActivity.this::onOptionsItemSelected);

        if(savedInstanceState==null ||  idPV==-1) {
            PreferenceManager.setDefaultValues(this, R.xml.root_preferences, false);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                ControllaCollegamento();
            }
        }
        /*
        PopupMenu popup = new PopupMenu(this, );
        MenuInflater findMenuItems =popup.getMenuInflater();
        findMenuItems.inflate(R.menu.main_menu, popup.getMenu());
        popup.show();*/
    }

    static int idPV=-1;
    public static int getIDPV() {
        return idPV;
    }

    public String getMacAddr() {
         //return "02-15-b2-00-00-00";/* //Prove con emulatore locale
        SharedPreferences sharedPreferences= PreferenceManager.getDefaultSharedPreferences(this.getApplicationContext());
        String address=sharedPreferences.getString("MAC","");
        if (!address.isEmpty()) {
            return address;
        }
        address = "02-00-00-00-00-00";
        try {
            List<NetworkInterface> all = Collections.list(NetworkInterface.getNetworkInterfaces());
            for (NetworkInterface nif : all) {

                if (!nif.getName().equalsIgnoreCase("wlan0")) continue;

                byte[] macBytes = nif.getHardwareAddress();
                if (macBytes == null) {
                    return "";
                }

                StringBuilder res1 = new StringBuilder();
                for (byte b : macBytes) {
                    if (Integer.toHexString(b & 0xFF).length() == 1) {
                        res1.append("0").append(Integer.toHexString(b & 0xFF)).append(":");
                    } else {
                        res1.append(Integer.toHexString(b & 0xFF)).append(":");
                    }
                }

                if (res1.length() > 0) {
                    res1.deleteCharAt(res1.length() - 1);
                }
                address=res1.toString();
                break;
            }

        } catch (Exception ignored) { }
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString("MAC", address);
        editor.apply();

        return address; //*/
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    @SuppressLint("StaticFieldLeak")
    private void ControllaCollegamento() {
        idPV=-1;
        // mi collego al web service
        TLFidelityWS web = new TLFidelityWS(this);
        AlertDialog.Builder builder = new AlertDialog.Builder(menuPrincipaleActivity.this);
        final AlertDialog  dlg = builder.setView(R.layout.downloading_dialog).show();
        final ProgressBar bar=dlg.findViewById(R.id.progressBar);
        TextView msg = dlg.findViewById(R.id.messaggio);
        msg.setText(R.string.collegamento);
        dlg.setCanceledOnTouchOutside(false);
        bar.setIndeterminate(true);
        web.connect(false,(Boolean connected)->{
            // è riuscito a collegarsi
            if(!connected) {
                DialogInterface.OnClickListener listener = (DialogInterface dialog, int which) -> {
                    if (which == DialogInterface.BUTTON_POSITIVE) {
                        menuPrincipaleActivity.this.StartSetup();
                    } else
                        System.exit(1);
                    dlg.hide();
                };
                AlertDialog.Builder builder2 = new AlertDialog.Builder(this);
                builder2.setMessage(R.string.webserviceNotAvailable)
                        .setPositiveButton(R.string.si, listener)
                        .setNegativeButton(R.string.no, listener)
                        .show().setCanceledOnTouchOutside(false);
                return;
            }
            ControllaAutorizzazione(web, dlg);
        });
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    private void ControllaAutorizzazione(TLFidelityWS web,AlertDialog  dlg) {
        final TextView msg = dlg.findViewById(R.id.messaggio);
        msg.setText(R.string.autorizzazione);

        // controllo se l'app può partire
        web.checkAutorizzazione( getMacAddr(), (Integer idPV)-> {
            if(idPV>0) {
                menuPrincipaleActivity.idPV =idPV;
                TLFidelityWS.setPV(idPV.toString());
                // sembrerebbe un idPV valido, vado avanti e
                // controllo se è necessario un'aggiornamento del DB città
                ControllaApp(web, dlg);
            } else {
                // non è un idPV valido, quindi può valere MaiChiesto, GiaChiesto o Errore.
                // l'ultimo caso non dovrebbe succedere in produzione.
                dlg.hide();
                TLFidelityWS.CostantiAutorizzazione risultato = TLFidelityWS.CostantiAutorizzazione.valueOf(idPV);
                if(risultato== MaiChiesto) {
                    Intent i = new Intent(this, InfoPVActivity.class);
                    i.putExtra("macAddress",getMacAddr());
                    startActivity(i);
                } else {
                    String exitMess = "Errore, l'applicazione non può essere avviata."; // non dovrebbe succedere mai in produzione.
                    if (risultato == GiaChiesto)
                        exitMess = getString(R.string.richiestaInviata);
                    AlertDialog.Builder builder2 = new AlertDialog.Builder(this);
                    builder2.setMessage(exitMess)
                            .setPositiveButton("OK", (DialogInterface dialog, int which) ->
                                    System.exit(1))
                            .show().setCanceledOnTouchOutside(false);
                }
            }
        });

        Timer tmr = new Timer(120000, new Runnable() {
            final boolean[] inProgress = {false};
            @Override
            public void run() {
                CheckCoords();
                if (!inProgress[0] && !isFinishing())
                  CheckConnection(inProgress);
            }
        });

        tmr.start();
    }

    private void CheckConnection(boolean[] inProgress) {

        TLFidelityWS web = new TLFidelityWS(this);
                web.connect(true,(Boolean connected)->{
                    // è riuscito a collegarsi
            if (!connected) {
                inProgress[0] = true;
                        AlertDialog.Builder builder = new AlertDialog.Builder(menuPrincipaleActivity.this);
                        final AlertDialog  dlg = builder.setView(R.layout.downloading_dialog).show();
                final ProgressBar bar = dlg.findViewById(R.id.progressBar);
                        dlg.setCanceledOnTouchOutside(false);
                        bar.setIndeterminate(true);
                CheckConnectionInterno(web, dlg, 1, inProgress);
            }
        });
    }

    private void CheckConnectionInterno(final TLFidelityWS web, final AlertDialog dlg, Integer id, boolean[] inProgress) {
        if(id==6) {
                                                                AlertDialog.Builder builder2 = new AlertDialog.Builder(menuPrincipaleActivity.this);
                                                                builder2.setMessage("E' mancata la connessione l'applicazione verrà chiusa.")
                                                                        .setPositiveButton("OK", (DialogInterface dialog, int which) ->
                                                                                System.exit(1))
                                                                        .show().setCanceledOnTouchOutside(false);
                                                                return;
                                                            }
        TextView msg = dlg.findViewById(R.id.messaggio);
        msg.setText(String.format(Locale.ITALY, "Problemi di connessione: provo a ristabilire la connettività (tentativo %d)", id));
        web.connect(true,(Boolean connected)->{
            if(!connected) {
                CheckConnectionInterno(web,dlg,id+1,inProgress);
                                                    } else {
                                                        dlg.cancel();
                inProgress[0] = false;
                                                    }
                });
            }

    private static final int SCRIVI_DOWNLOAD = 1304;
    private static final int INSTALLA_PACCHETTI = 1305;
    private static final int ID_RICHIESTA_PERMISSION_LOCALIZZAZIONE = 1306;

    @RequiresApi(api = Build.VERSION_CODES.M)
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == ID_RICHIESTA_PERMISSION_LOCALIZZAZIONE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                CheckCoords();
            }
            if(saveDialogs != null && !isFinishing()) {
                saveDialogs.dismiss();
                saveDialogs = null;
            }
        } else {
        //if (requestCode != SCRIVI_DOWNLOAD && requestCode != INSTALLA_PACCHETTI) {
          if (requestCode != INSTALLA_PACCHETTI) {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
            return;
        }
        if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            InstallaNuovaVersione(null);
        } else {
            AlertDialog.Builder builder2 = new AlertDialog.Builder(this);
            builder2.setMessage("L'applicazione non aggiornata non verrà avviata, abilitare il salvataggio e l'avvio dell'installazione di una nuova versione.")
                    .setPositiveButton("OK", (DialogInterface dialog, int which) -> System.exit(1))
                    .show().setCanceledOnTouchOutside(false);
        }
    }
    }
    static AlertDialog tmpDlg = null;
    // https://stackoverflow.com/a/4969421/854279

    private void InstallaNuovaVersione(AlertDialog dlg) {
        if(dlg!=null && tmpDlg==null) tmpDlg = dlg;
        //Delete update file if exists
        if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.R) {
            // se non è android 11, niente dialog
            String destination = this.getFilesDir() + "/TLCustomerApp.apk";
            File file = new File(destination);
            if (file.exists())
                //file.delete() - test this, I think sometimes it doesn't work
                file.delete();
            Uri apkURI = FileProvider.getUriForFile(menuPrincipaleActivity.this,
                    getApplicationContext().getPackageName() + ".provider", file);
            LanciaInstallazione(apkURI);
            return;
        }
        // https://developer.android.com/training/data-storage/shared/documents-files#create-file
        Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("application/vnd.android.package-archive");
        intent.putExtra(Intent.EXTRA_TITLE, "TLCustomerApp.apk");
        InstallaVersioneResultLauncher.launch(intent);

    }

    private void LanciaInstallazione(Uri uri) {
        TLFidelityWS web = new TLFidelityWS(this);
        final AlertDialog finalDlg = tmpDlg;
        final ProgressBar bar = finalDlg.findViewById(R.id.progressBar);
        tmpDlg=null;
        OutputStream destination = null;
        try {
            destination = getContentResolver().openOutputStream(uri);
        } catch (FileNotFoundException ex) {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setMessage("Si è verificato un problema con il download della nuova versione. Contattare l'assistenza. Vuoi continuare a lavorare con la vecchia versione?")
                    .setPositiveButton("Sì", (DialogInterface dialog, int which) ->
                            finalDlg.hide())
                    .setNegativeButton("No",(DialogInterface dialog, int which) ->
                            finish())
                    .show();
        }
        if(destination!=null)
            web.scaricaNuovaVersione(destination, (Boolean downloaded) -> {
            if (!downloaded) {
                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setMessage("Si è verificato un problema con il download della nuova versione. Contattare l'assistenza. Vuoi continuare a lavorare con la vecchia versione?")
                        .setPositiveButton("Sì", (DialogInterface dialog, int which) ->
                                finalDlg.hide())
                        .setNegativeButton("No",(DialogInterface dialog, int which) ->
                                finish())
                        .show();
            } else {
                //long size = file.length();
                Intent install = new Intent(Intent.ACTION_VIEW);
                install.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                install.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                //install.setDataAndType(uri, "application/vnd.android.package-archive");
                try {
                install.setDataAndType(uri, "application/vnd.android.package-archive");
                install.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                startActivity(install);
                }catch (Exception e) {
                    e.printStackTrace();
                    web.insertLog(getMacAddr(), 0, menuPrincipaleActivity.class .getName(),"Errore download nuova versione ","Eccezione: "+e.getMessage(), (Integer done)->{
                        if(done==-1) {
                            Log.e(menuPrincipaleActivity.class.getName(),"Errore download nuova versione");
                        }

                    });
                }
            }
            finalDlg.hide();
            finish();
        }, (Integer[] values) -> {
            bar.setIndeterminate(false);
            bar.setMax(values[1]);
            bar.setProgress(values[0]);
        });
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    private void ControllaApp(TLFidelityWS web, AlertDialog dlg) {
        final TextView msg = dlg.findViewById(R.id.messaggio);
        PackageManager manager = this.getPackageManager();
        PackageInfo info = null;
        try {
            info = manager.getPackageInfo(this.getPackageName(), PackageManager.GET_ACTIVITIES);
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
            ControllaDB(web, dlg);
            return;
        }
        msg.setText(R.string.versione);
        long ver;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            ver = info.getLongVersionCode();
        } else
            ver = (long)info.versionCode;

        web.insertLog(getMacAddr(), 0, menuPrincipaleActivity.class .getName(),"TLCustomerApp versione: "+ String.valueOf(ver),"", (Integer done)->{
            if(done==-1) {
                Log.e(menuPrincipaleActivity.class.getName(),"Errore inserimento log versione");
            }

        });

        web.checkVersione(ver,(Boolean chiudi)-> {
            if(chiudi) {
                msg.setText(R.string.download);
                InstallaNuovaVersione(dlg);
            } else {
                ControllaDB(web, dlg);
            }
        });

        WifiManager wifiMan = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        WifiInfo wifiInf = wifiMan.getConnectionInfo();
        int ipAddress = wifiInf.getIpAddress();
        String ip = String.format("%d.%d.%d.%d", (ipAddress & 0xff),(ipAddress >> 8 & 0xff),(ipAddress >> 16 & 0xff),(ipAddress >> 24 & 0xff));

        web.insertLog(getMacAddr(), 0, menuPrincipaleActivity.class .getName(),"IP: "+ ip,"", (Integer done)->{
            if(done==-1) {
                Log.e(menuPrincipaleActivity.class.getName(),"Errore inserimento log ip");
            }

        });

    }

    private void ControllaDB(TLFidelityWS web,AlertDialog  dlg) {
        LocalDB db = new LocalDB(this);
        final ProgressBar bar=dlg.findViewById(R.id.progressBar);
        final TextView msg = dlg.findViewById(R.id.messaggio);
        db.necessarioAggiornamentoGeografia(web,(Boolean needed)->{
            if(needed) {
                // come testo messaggio
                msg.setText(R.string.aggiornamentoDB);
                LocalDB.aggiornaGeografia updater = new LocalDB.aggiornaGeografia(db,dlg,bar);
                updater.execute(web);
            } else {
                // non è necessario l'aggiornamento, finalmente posso chiudere la
                // finestra.
                dlg.hide();
            }

            menuPrincipaleActivity ctx = menuPrincipaleActivity.this;
            if(ctx instanceof menuPrincipaleActivity) {
                ctx.saveDialogs = dlg;
            }

            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, ID_RICHIESTA_PERMISSION_LOCALIZZAZIONE);

        });

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater findMenuItems = getMenuInflater();
        findMenuItems.inflate(R.menu.main_menu, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.impostazioniItem) {
            StartSetup();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }


    void StartSetup() {
        Intent setupIntent = new Intent(menuPrincipaleActivity.this, WebServiceSetupActivity.class);
        startActivity(setupIntent);
    }

    static private int idCliente;
    static private String codiceTessera;

    static public int getIdCliente() {
        return idCliente;
    }

    public static String getCodiceTessera() {
        return codiceTessera;
    }

    static public void setIdCliente(int idCliente,String codiceTessera) {
        menuPrincipaleActivity.idCliente = idCliente;
        menuPrincipaleActivity.codiceTessera = codiceTessera;
    }

    public void OnNuovoClick(View view) {
        idCliente=-1;
        Intent i = new Intent(this, ModuloActivity.class);
        startActivity(i);
    }

    public void OnModificaClick(View btn) {
        Intent i = new Intent(this, ModificaActivity.class);
        startActivity(i);
    }

    /// GEO LOCALIZZAZIONE
    private Location bestLocation = null;
    private void updateLocation() {
        LocationManager locationManager = (LocationManager) getApplicationContext().getSystemService(LOCATION_SERVICE);
        List<String> providers = locationManager.getProviders(true);
        bestLocation = null;
        if(providers.size()==0) // non è autorizzato
            return;
        int nTest = providers.size();
        LocationListener listener = new LocationListener() {
            int nUpdated = 0;

            @Override
            public void onLocationChanged(Location location) {
                nUpdated++;
                if (bestLocation == null ||
                        location.getAccuracy() < bestLocation.getAccuracy()) {
                    // Found best last known location: %s", l);
                    bestLocation = location;
                }
                if(nUpdated>=nTest) {
                    nUpdated = 0;
                    InviaPosizione();
                    locationManager.removeUpdates(this);
                }
            }

            @Override
            public void onStatusChanged(String provider, int status, Bundle extras) {

            }

            @Override
            public void onProviderEnabled(String provider) {

            }

            @Override
            public void onProviderDisabled(String provider) {

            }
        };
        boolean requested = false;
        for (String provider : providers) {
            try{
                long currentMillis = System.currentTimeMillis();
                Location l = locationManager.getLastKnownLocation(provider);
                if (l == null || currentMillis-l.getTime()>60e3) {
                    // è più vecchio di un minuto
                    try {
                        locationManager.requestLocationUpdates(provider, 0, 0, listener);
                        requested = true;
                    } catch (SecurityException e) {
                        e.printStackTrace();
                    }

                    continue;
                }
                if (bestLocation == null || l.getAccuracy() < bestLocation.getAccuracy()) {
                    // Found best last known location: %s", l);
                    bestLocation = l;
                }
            } catch(SecurityException e) {
                e.printStackTrace();
            }
        }
        if (!requested) {
            InviaPosizione();
        }
        //*/
    }

    private static final long deltaInvioGPS = 1000*60*60; //ogni ora
    private static final long deltaForzaInvioGPS = 1000*60*60*24; //ogni giorno
    private static final long minDeltaPosMt = 10; //invia solo se il delta è maggiore di 10mt
    private void CheckCoords() {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this.getApplicationContext());
        long lastTime = sharedPreferences.getLong("lastTimeCoords", 0);
        long timeFromLastSend = System.currentTimeMillis() - lastTime;
        if(timeFromLastSend<deltaInvioGPS)
            return;
        updateLocation();
    }

    private void InviaPosizione() {
        if(bestLocation==null) return;
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this.getApplicationContext());
        float lastLat = sharedPreferences.getFloat("lastLat",0);
        float lastLon = sharedPreferences.getFloat("lastLon",0);
        long lastTime = sharedPreferences.getLong("lastTimeCoords", 0);
        long timeFromLastSend = System.currentTimeMillis() - lastTime;
        if(lastLat!=0 && lastLon!=0 && timeFromLastSend<deltaForzaInvioGPS) {
            // se è passato meno di deltaForzaInvioGPS, controllo la distanza
            float[] dist = {0};
            Location.distanceBetween(lastLat,lastLon,bestLocation.getLatitude(),bestLocation.getLongitude(),dist);
            if(dist[0]<minDeltaPosMt) {
                // troppo vicino
                return;
            }
        }
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putFloat("lastLat",(float)bestLocation.getLatitude());
        editor.putFloat("lastLon",(float)bestLocation.getLongitude());
        editor.putLong("lastTimeCoords",System.currentTimeMillis());
        editor.apply();
        TLFidelityWS web = new TLFidelityWS(this);
        web.sendPos(getMacAddr(),(float)bestLocation.getLatitude(),(float)bestLocation.getLongitude(), (Boolean v) -> {});
    }

}
