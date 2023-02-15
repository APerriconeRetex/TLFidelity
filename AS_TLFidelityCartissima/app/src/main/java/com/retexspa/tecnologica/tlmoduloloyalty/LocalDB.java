package com.retexspa.tecnologica.tlmoduloloyalty;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.AsyncTask;
import android.widget.ProgressBar;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

public class LocalDB extends SQLiteOpenHelper {

    LocalDB(Context c)
    {
        super(c,"TLFidelity",null,2);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE Nazione (\n" +
                "    id integer  PRIMARY KEY ,\n" +
                "    descrizione TEXT not null);");
        db.execSQL("CREATE TABLE Regione (\n" +
                "    id integer  PRIMARY KEY,\n" +
                "    idNazione integer, \n" +
                "    descrizione TEXT not null,\n" +
                "    FOREIGN KEY(idNazione) REFERENCES Nazione(id));");
        db.execSQL("CREATE TABLE Provincia (\n" +
                "    id integer  PRIMARY KEY,\n" +
                "    idRegione integer, \n" +
                "    descrizione TEXT not null,\n" +
                "    sigla TEXT not null,\n" +
                "    FOREIGN KEY(idRegione) REFERENCES Regione(id));");
        db.execSQL("CREATE TABLE Citta (\n" +
                "    id integer  PRIMARY KEY,\n" +
                "    idProvincia integer, \n" +
                "    descrizione TEXT not null,\n" +
                "    codIstat TEXT not null,\n" +
                "    codCatasto TEXT not null,\n" +
                "    FOREIGN KEY(idProvincia) REFERENCES Provincia(id));");
        db.execSQL("CREATE TABLE CAP (\n" +
                "    id integer PRIMARY KEY AUTOINCREMENT,\n" +
                "    idCitta integer, \n" +
                "    descrizione TEXT not null,\n" +
                "    FOREIGN KEY(idCitta) REFERENCES Citta(id));");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if(oldVersion==1) {
            // svuoto le citta perché non hanno il codice belfiore
            db.execSQL("delete from CAP;"); // svuoto anche il cap per la chiave esterna
            db.execSQL("delete from Citta;");
            db.execSQL("ALTER TABLE Citta ADD codCatasto TEXT not null default '';");
        }
    }

    String getCodiceCatastale(String comune, String prov) {
        String codice = "X000";
        if(comune.length()>0) {
            SQLiteDatabase db = getReadableDatabase();
            Cursor c = db.rawQuery("select codCatasto, sigla from citta inner join provincia on citta.idProvincia=provincia.id where citta.Descrizione = ?", new String[]{comune});
            while (c.moveToNext()) {
                codice = c.getString(0);
                if (prov.length() > 0 && c.getString(1).equals(prov))
                    break;
            }
            c.close();
        }
        return codice;
    }


    private interface extraGeoValori {
        void addExtra(JSONObject obj, ContentValues values) throws Exception;
    }

    void necessarioAggiornamentoGeografia(TLFidelityWS web, AsyncCallback<Boolean> callback) {
        web.getConteggioComuni((Integer nRemoto) -> {
            int nLocale = 0;
            SQLiteDatabase db = getReadableDatabase();
            Cursor c = db.rawQuery("select count(*) from citta", null);
            if (c.moveToNext()) {
                nLocale = c.getInt(0);
            }
            c.close();
            callback.call(nLocale != nRemoto);
        });
    }

    @SuppressLint("StaticFieldLeak") //accade 1 sola volta
    public static class aggiornaGeografia extends AsyncTask<TLFidelityWS,Integer,Void> {
        public ProgressBar bar;
        public AlertDialog dlg;
        public SQLiteDatabase db;

        public aggiornaGeografia(LocalDB src, AlertDialog dlg, ProgressBar bar) {
            this.db = src.getWritableDatabase();
            this.dlg = dlg;
            this.bar = bar;
    }

        @Override
        protected Void doInBackground(TLFidelityWS... web) {
            db.execSQL("delete from CAP;");
            db.execSQL("delete from Citta;");
            db.execSQL("delete from Provincia;");
            db.execSQL("delete from Regione;");
            db.execSQL("delete from Nazione;");

            try {
                JSONArray info = web[0].getGeografiaSync();
                int nCitta = 0;
                for (int idxNazione = 0; idxNazione < info.length(); ++idxNazione) {
                    JSONObject nazione = info.getJSONObject(idxNazione);
                    JSONArray regioni = nazione.getJSONArray("regioni");
                    for (int idxRegione = 0; idxRegione < regioni.length(); ++idxRegione) {
                        JSONObject regione = regioni.getJSONObject(idxRegione);
                        JSONArray provincie = regione.getJSONArray("provincie");
                        for (int idxProvincia = 0; idxProvincia < provincie.length(); ++idxProvincia) {
                            JSONObject provincia = provincie.getJSONObject(idxProvincia);
                            JSONArray citta = provincia.getJSONArray("citta");
                            nCitta += citta.length();
                        }
                    }
                }
                int nDone=0;
                for (int idxNazione = 0; idxNazione < info.length(); ++idxNazione) {
                    JSONObject nazione = info.getJSONObject(idxNazione);
                    int idNazione = geografia_FaiPezzo(db, nazione, "Nazione", null, -1, null);
                    JSONArray regioni = nazione.getJSONArray("regioni");
                    for (int idxRegione = 0; idxRegione < regioni.length(); ++idxRegione) {
                        JSONObject regione = regioni.getJSONObject(idxRegione);
                        int idRegione = geografia_FaiPezzo(db, regione, "Regione", "idNazione", idNazione, null);
                        JSONArray provincie = regione.getJSONArray("provincie");
                        db.beginTransaction();
                        for (int idxProvincia = 0; idxProvincia < provincie.length(); ++idxProvincia) {
                            JSONObject provincia = provincie.getJSONObject(idxProvincia);
                            int idProvincia = geografia_FaiPezzo(db, provincia, "Provincia", "idRegione", idRegione,
                                    (JSONObject obj, ContentValues values) -> values.put("sigla", obj.getString("sigla")));
                            JSONArray citta = provincia.getJSONArray("citta");
                            for (int idxCitta = 0; idxCitta < citta.length(); ++idxCitta) {
                                JSONObject cc = citta.getJSONObject(idxCitta);
                                int idCitta = geografia_FaiPezzo(db, cc, "Citta", "idProvincia", idProvincia, (JSONObject obj, ContentValues values) -> {
                                    values.put("codIstat", obj.getString("codIstat"));
                                    values.put("codCatasto", obj.getString("codCatasto"));
                                });
                                JSONArray caps = cc.getJSONArray("caps");
                                publishProgress(nDone++,nCitta);
                                geografia_SalvaCaps(db, idCitta, caps);
                            }
                        }
                        db.setTransactionSuccessful();
                        db.endTransaction();
                    }

                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            return null;
        }

        @Override
        protected void onProgressUpdate(Integer... value) {
            bar.setIndeterminate(false);
            bar.setMax(value[1]);
            bar.setProgress(value[0]);
        }
        @Override
        protected void onPostExecute(Void voids) {
            dlg.hide();
    }

    private void geografia_SalvaCaps(SQLiteDatabase db, int idCitta, JSONArray caps) throws JSONException {
        ArrayList<String> capCorrenti = new ArrayList<>();
        Cursor c = db.rawQuery("select descrizione from cap where idCitta="+ idCitta, null);
        while(c.moveToNext()) {
            capCorrenti.add(c.getString(0));
            //test.add(c.getInt(1));
        }
        c.close();

        for (int idxCap = 0; idxCap < caps.length(); ++idxCap) {
            String cap = caps.getString(idxCap);
            if(!capCorrenti.contains(cap)) {
                ContentValues values = new ContentValues();
                values.put("Descrizione", cap);
                values.put("idCitta", idCitta);
                db.insert("cap", null, values);
            }
        }
    }

        private int geografia_FaiPezzo(SQLiteDatabase db, JSONObject obj, String nomeTabella, String parentCol, int parentId, extraGeoValori extraValori) throws Exception {
            int id = obj.getInt("id");
            Cursor c = db.rawQuery("select count(*) from "+nomeTabella+" where id=?",new String[] {Integer.toString(id)});
            if(c.moveToNext() && c.getInt(0)==0) {
                ContentValues values= new ContentValues();
                values.put("ID",id);
                values.put("Descrizione",obj.getString("descrizione"));
                if(parentCol!=null) {
                    values.put(parentCol, parentId);
                }
                if(extraValori!=null)
                    extraValori.addExtra(obj, values);
                db.insert(nomeTabella, null, values);
            }
            c.close();
            return id;
        }
    }


    String[] getComuni(String startValue, boolean provIncluded) {
        try {
            ArrayList<String> risultato = new ArrayList<>();
            //ArrayList<Integer> test = new  ArrayList<Integer>();
            SQLiteDatabase db = getReadableDatabase();
            Cursor c = db.rawQuery("select citta.Descrizione, provincia.Sigla \n" +
                            "from citta inner join Provincia on provincia.id=citta.idProvincia \n" +
                            "where citta.Descrizione like ? order by case when citta.Descrizione = ? then ? when citta.Descrizione like ? then ? || citta.Descrizione else ? || citta.Descrizione  end limit 10",
                    new String[]{ "%" + startValue + "%", startValue, "0",startValue + "%","1","2"});
            while (c.moveToNext()) {
                String nomeComune = c.getString(0);
                if (provIncluded) {
                    if(nomeComune.contains("("))
                        continue;
                    nomeComune += " (" + c.getString(1) + ")";
                    risultato.add(nomeComune);
                } else if (!risultato.contains(nomeComune))
                    risultato.add(nomeComune);
                //test.add(c.getInt(1));
            }
            c.close();
            return risultato.toArray(new String[]{});
        } catch (Exception ex ) {
            ex.printStackTrace();
            return null;
        }
    }

    String[] getProvincie(String citta, String base) {
        ArrayList<String> risultato = new ArrayList<>();
        //ArrayList<Integer> test = new  ArrayList<Integer>();
        SQLiteDatabase db = getReadableDatabase();
        String sql = "select distinct sigla from Provincia inner join citta on citta.idProvincia=provincia.id where citta.Descrizione = ? ";
        if(base.length()!=0) {
            sql += " and sigla like '"+base.toUpperCase()+"%'";
        }
        Cursor c = db.rawQuery(sql, new String[]{citta});
        while(c.moveToNext()) {
            risultato.add(c.getString(0));
            //test.add(c.getInt(1));
        }
        c.close();
        return risultato.toArray(new String[] {});
    }

    String[] getCAP(String citta, String base) {
        ArrayList<String> risultato = new ArrayList<>();
        //ArrayList<Integer> test = new  ArrayList<Integer>();
        SQLiteDatabase db = getReadableDatabase();

        String sql = "select distinct CAP.descrizione from CAP inner join citta on CAP.idCitta=citta.id where citta.Descrizione = ? ";
        if(base.length()!=0) {
            sql += " and CAP.descrizione like '%"+base.toUpperCase()+"%'";
        }
        Cursor c = db.rawQuery(sql, new String[] {citta});
        while(c.moveToNext()) {
            risultato.add(c.getString(0));
            //test.add(c.getInt(1));
        }
        c.close();
        return risultato.toArray(new String[] {});
    }

    public boolean checkCitta(String citta, String prov) {
        SQLiteDatabase db = getReadableDatabase();
        String sql = "select citta.Descrizione,sigla from citta inner join Provincia on provincia.id=citta.idProvincia where citta.Descrizione = ? ";
        if(prov.length()!=0) {
            sql += " and sigla like '"+prov.toUpperCase()+"%'";
        }
        Cursor c = db.rawQuery(sql, new String[] {citta});
        boolean ret = (c.moveToNext());
        c.close();
        return ret;
    }

    public boolean checkCap(String cap,String citta,String prov) {
        SQLiteDatabase db = getReadableDatabase();
        String sql = "select distinct CAP.descrizione from CAP inner join citta on CAP.idCitta=citta.id  inner join Provincia on provincia.id=citta.idProvincia where cap.descrizione = ? and citta.Descrizione = ? and sigla=?";
        Cursor c = db.rawQuery(sql, new String[] {cap, citta, prov});
        boolean ret = (c.moveToNext());
        c.close();
        return ret;

    }
}
