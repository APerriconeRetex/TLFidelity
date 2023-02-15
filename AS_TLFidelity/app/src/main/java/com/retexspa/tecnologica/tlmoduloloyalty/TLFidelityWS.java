package com.retexspa.tecnologica.tlmoduloloyalty;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.AsyncTask;

import androidx.preference.PreferenceManager;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

/**
 * Classe di interfacciamento con il web service TLFidelityWS
 * per sviluppare: https://stackoverflow.com/a/36645473/854279
 * chrome://inspect/#devices
 */
class TLFidelityWS {

    /** l'url configurato */
    private static String baseUrl;
    /** il timeout configurato */
    private static int timeout;

    static String getBaseUrl() {
        return baseUrl;
    }

    TLFidelityWS(Context cnt) {
        SharedPreferences preference= PreferenceManager.getDefaultSharedPreferences(cnt);
        baseUrl = preference.getString("WSURL",null);
        if(baseUrl!=null && baseUrl.endsWith("/"))
            baseUrl=baseUrl.substring(0,baseUrl.length()-1);
        timeout = Integer.parseInt(preference.getString("timeout","30"));
    }

    private static HttpURLConnection GetConnection(String page) throws Exception {
        URL url = new URL(baseUrl+page);
        if(url.getProtocol().equals("http") && url.getProtocol().equals("https")) {
            throw new Exception("Invalid url");
        }
        HttpURLConnection retValue = (HttpURLConnection)url.openConnection();
        retValue.setRequestProperty("Accept","application/json");
        retValue.setConnectTimeout(timeout*1000);
        retValue.setReadTimeout(timeout*1000);
        return retValue;
    }

    static abstract class MyCaller<Params, Result> extends AsyncTask<Params, Void,Result> {
        AsyncCallback<Result> onDone;
        @SafeVarargs
        final void execute(AsyncCallback<Result> onDone, Params... params) {
            this.onDone=onDone;
            super.execute(params);
        }

        protected abstract String getPage(Params... params);
        protected Result EvaluateConnection(HttpURLConnection conn) throws Exception {
            if (conn.getResponseCode() != 200) {
                throw new Exception(getErrorString(conn));
            }
            return EvaluateResponse(getResponseString(conn));
        }
        protected Result EvaluateResponse(String response) throws Exception {
            return null;
        }

        protected Result onError(Exception ex) {
            return null;
        }

        @Override
        protected Result doInBackground(Params... params) {
            Result retValue = null;
            HttpURLConnection conn = null;
            if(baseUrl==null) {
                return onError(null);
            }
            try {
                // provo la configurazione
                conn = GetConnection(getPage(params));
                retValue = EvaluateConnection(conn);
            } catch (Exception e) {
                e.printStackTrace();
                retValue = onError(e);
            } finally {
                if(conn!=null)
                    conn.disconnect();
            }
            return retValue;
        }

        @Override
        protected void onPostExecute(Result result) {
            onDone.call(result);
        }
    }

    static class getConteggioComuni extends MyCaller<Void,Integer> {
        @Override
        protected String getPage(Void... voids) {
            return "/api/geografia/conteggio";
        }

        @Override
        protected Integer EvaluateResponse(String response) {
            return Integer.parseInt(response);
        }

        @Override
        protected Integer onError(Exception ex) {
            return 0;
        }
    }
    /**
     * Chiama il webService per il numero di comuni
     * @return il numero di comuni nel db remoto
     */
    void getConteggioComuni(AsyncCallback<Integer> callback) {
        getConteggioComuni worker = new getConteggioComuni();
        worker.execute(callback);
    }

    private static class connect extends MyCaller<Boolean,Boolean> {
        private Boolean isFromTimer;

        @Override
        protected String getPage(Boolean... booleans) {
            isFromTimer = booleans[0];
            return "";
        }

        @Override
        protected Boolean EvaluateConnection(HttpURLConnection conn) throws Exception {
            if (isFromTimer) {
                conn.setConnectTimeout(10000);
                conn.setReadTimeout(10000);
            }


            return super.EvaluateConnection(conn);
        }

        @Override
        protected Boolean EvaluateResponse(String response) throws Exception {
            return true;
        }

        @Override
        protected Boolean onError(Exception ex) {
            return false;
        }

    }
    /**
     * Prova a leggere la configurazione ed a scaricare la pagina principale,
     */
    void connect(Boolean isFromTimer, AsyncCallback<Boolean> onDone ) {
        connect worker = new connect();
        worker.execute(onDone,isFromTimer);
    }

    /**
     * Chiama il webService per il DB totale.
     * @return un'array di nazioni
     * @implNote è sincrono perché viene chiamato in un AsyncTask.
     * @throws Exception
     */
    JSONArray getGeografiaSync() throws Exception {
        HttpURLConnection conn = null;
        JSONArray retValue = null;
        try {
            conn = GetConnection("/api/geografia");
            if (conn.getResponseCode() != 200) {
                throw new Exception("error HTTP: " + conn.getResponseMessage());
            }
            retValue = new JSONArray(TLFidelityWS.getResponseString(conn));
        } catch (Exception e) {
            e.printStackTrace();
            throw  e;
        } finally {
            if (conn != null)
                conn.disconnect();
        }
        return retValue;
    }

    /**
     * usato internamento per leggere il valore tornato dal webService
     * @param conn HttpURLConnection da cui scaricare
     * @return tutto il contenuto
     * @throws Exception
     */
    private static String getResponseString(HttpURLConnection conn) throws Exception {
        String contentType = conn.getHeaderField("Content-Type");
        String charset = "UTF-8";
        if(contentType!=null && contentType.contains("charset=")) {
            charset=contentType.substring(contentType.indexOf("charset=")+8);
        }
        BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream(), charset));
        StringBuilder result = new StringBuilder();
        String line;
        while((line = reader.readLine()) != null) {
            result.append(line);
        }
        return result.toString();
    }

    private static String getErrorString(HttpURLConnection conn) throws Exception {
        String contentType = conn.getHeaderField("Content-Type");
        String charset = "UTF-8";
        if(contentType!=null && contentType.contains("charset=")) {
            charset=contentType.substring(contentType.indexOf("charset=")+8);
        }
        BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getErrorStream(), charset));
        StringBuilder result = new StringBuilder();
        String line;
        while((line = reader.readLine()) != null) {
            result.append(line);
        }
        return result.toString();
    }

    public String getBaseTesseraSync() {
        HttpURLConnection conn = null;
        String retValue = null;
        try {
            conn = GetConnection("/api/Tessere/"+ menuPrincipaleActivity.getIDPV());
            if (conn.getResponseCode() != 200) {
                String errValue = getErrorString(conn);
                throw new Exception("error HTTP: " + conn.getResponseMessage());
            }
            retValue = getResponseString(conn);
            retValue = retValue.substring(1,retValue.length()-1);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (conn != null)
                conn.disconnect();
        }
        return retValue;
    }

    public String getBaseTesseraSync(boolean cartaSenior) {
        HttpURLConnection conn = null;
        String retValue = null;
        try {
            conn = GetConnection("/api/Tessere?idpv="+ menuPrincipaleActivity.getIDPV() +"&senior="+ cartaSenior);
            if (conn.getResponseCode() != 200) {
                String errValue = getErrorString(conn);
                throw new Exception("error HTTP: " + conn.getResponseMessage());
            }
            retValue = getResponseString(conn);
            retValue = retValue.substring(1,retValue.length()-1);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (conn != null)
                conn.disconnect();
        }
        return retValue;
    }

    public enum CostantiAutorizzazione {
        Errore(0),
        MaiChiesto(-1),
        GiaChiesto(-2);

        private final int value;
        CostantiAutorizzazione(int value) {
            this.value = value;
        }

        static CostantiAutorizzazione valueOf(int x) {
            for(CostantiAutorizzazione val : CostantiAutorizzazione.values()) {
                if(x==val.value)
                    return val;
            }
            return null;
        }

        int getValue() {
            return this.value;
        }
    }

    static class checkAutorizzazione extends MyCaller<String,Integer> {

        @Override
        protected String getPage(String... strings) {
            return "/api/autorizzazione/"+strings[0].replaceAll(":","-");

        }

        @Override
        protected Integer EvaluateConnection(HttpURLConnection conn) throws Exception {
            switch (conn.getResponseCode())
            {
                case 200:
                    return Integer.parseInt(getResponseString(conn));
                case 403:
                    return CostantiAutorizzazione.MaiChiesto.getValue();
                case 401:
                    return CostantiAutorizzazione.GiaChiesto.getValue();
            }
            return super.EvaluateConnection(conn);
        }

        @Override
        protected Integer onError(Exception ex) {
            return CostantiAutorizzazione.Errore.getValue();
        }
    }

    void checkAutorizzazione(String macAddress, AsyncCallback<Integer> onDone) {
        new checkAutorizzazione().execute(onDone,macAddress);
    }

    static class checkInvioMail extends MyCaller<String,Boolean> {

        @Override
        protected String getPage(String... strings) {
            return "/api/autorizzazione/mail/"+strings[0].replaceAll(":","-");

        }

        @Override
        protected Boolean EvaluateConnection(HttpURLConnection conn) throws Exception {

            return super.EvaluateConnection(conn);
        }

        @Override
        protected Boolean EvaluateResponse(String msg) throws Exception {
            return Boolean.parseBoolean(msg);
        }

        @Override
        protected Boolean onError(Exception ex) {
            return false;
        }
    }

    void checkInvioMail(String macAddress, AsyncCallback<Boolean> onDone) {
        new checkInvioMail().execute(onDone,macAddress);
    }

    static class richiediAutorizzazione extends MyCaller<String,Integer> {

        private String codiceCedi;
        private String descrCedi;
        private String codicePV;
        private String descrPV;
        private String email;
        private String tel;
        private String psw;
        @Override
        protected String getPage(String... strings) {
            codiceCedi = strings[1];
            descrCedi = strings[2];
            codicePV = strings[3];
            descrPV = strings[4];
            email = strings[5];
            tel = strings[6];
            psw = strings[7];
            return "/api/autorizzazione/"+strings[0].replaceAll(":","-");
        }

        @Override
        protected Integer EvaluateConnection(HttpURLConnection conn) throws Exception {
            conn.setDoOutput(true);
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json; utf-8");
            conn.setRequestProperty("Accept", "application/json");
            OutputStream os = conn.getOutputStream();
            String jsonAutorizzazione="{\"psw\": \"" + psw + "\", " +
                    "\"codiceCedi\": \"" + codiceCedi + "\", " +
                    "\"descrizioneCedi\": \"" + descrCedi + "\", " +
                    "\"codicePV\": \"" + codicePV + "\", " +
                    "\"ragSocPV\": \"" + descrPV + "\", " +
                    "\"email\": \"" + email + "\", " +
                    "\"tel\": \"" + tel + "\"}";
            os.write(jsonAutorizzazione.getBytes(StandardCharsets.UTF_8));
            os.close();
            return super.EvaluateConnection(conn);
        }

        @Override
        protected Integer EvaluateResponse(String msg) throws Exception {
            return Integer.parseInt(msg);
        }

        @Override
        protected Integer onError(Exception ex) {
            return -3;
        }
    }

    public void richiediAutorizzazione(String macAddress, String codiceCedi, String descrizioneCedi, String codicePV, String ragSocPV, String email, String tel, String psw, AsyncCallback<Integer> onDone) {
        new richiediAutorizzazione().execute(onDone,macAddress,codiceCedi,descrizioneCedi,codicePV,ragSocPV,email,tel,psw);
    }


    public enum statoCodiceTessera {
        Valida,
        Errore,
        Inesistente,
        DiAltroPV,
        TipoErrato,
        InUso
    }
    static class controllaTessera extends MyCaller<String, statoCodiceTessera> {
        @Override
        protected String getPage(String... strings) {
            String ret ="/api/Tessere/check/"+strings[0]+"?idpv="+ menuPrincipaleActivity.getIDPV();
            ret += "&senior="+strings[1];
            return ret;
        }

        @Override
        protected statoCodiceTessera EvaluateConnection(HttpURLConnection conn) throws Exception {
            switch (conn.getResponseCode())
            {
                case 400:
                    return statoCodiceTessera.TipoErrato;
                case 403:
                    return statoCodiceTessera.DiAltroPV;
                case 404:
                    return statoCodiceTessera.Inesistente;
            }
            return super.EvaluateConnection(conn);
        }

        @Override
        protected statoCodiceTessera EvaluateResponse(String response) throws Exception {
            if(response.equals("true"))
                return statoCodiceTessera.Valida;
            else
                return statoCodiceTessera.InUso;
        }

        @Override
        protected statoCodiceTessera onError(Exception ex) {
            return statoCodiceTessera.Errore;
        }
    }

    void controllaTessera(String codiceTessera,boolean senior,AsyncCallback<statoCodiceTessera> onDone)  {
        TLFidelityWS.controllaTessera controllaTessera = new controllaTessera(); //in questo modo no warning
        controllaTessera.execute(onDone, codiceTessera,Boolean.toString(senior));
    }

    String getTessereSync(String senior,String emesse, String startValue, int limit) {
        HttpURLConnection conn = null;
        String retValue = null;
        try {

            String page = "/api/Tessere";
            page += "?idpv=" + menuPrincipaleActivity.getIDPV();
            if(senior!=null && senior.length()>0) page += "&senior=" + senior;
            if(emesse!=null && emesse.length()>0) page += "&emesse=" + emesse;
            page += "&base=" + startValue;
            page += "&limit=" + limit;
            conn = GetConnection(page);
            if (conn.getResponseCode() != 200) {
                String errValue = getErrorString(conn);
                throw new Exception("error HTTP: " + conn.getResponseMessage());
            }
            retValue = getResponseString(conn);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (conn != null)
                conn.disconnect();
        }
        return retValue;
    }

    public enum StatoCliente {
        Errore(0),
        NonTrovato(-1),
        DiAltroCedi(-2);

        private final int value;
        StatoCliente(int value) {
            this.value = value;
        }

        static StatoCliente valueOf(int x) {
            for(StatoCliente val : StatoCliente.values()) {
                if(x==val.value)
                    return val;
            }
            return null;
        }

        int getValue() {
            return this.value;
        }
    }

    static class controllaTripletta extends MyCaller<String, Integer> {
        @Override
        protected String getPage(String... strings) {
            String page = "/api/Clienti";
            page += "?tessera="+strings[0];
            page += "&cognome="+strings[1];
            page += "&nome="+strings[2];
            page += "&idpv="+ menuPrincipaleActivity.getIDPV();
            return page;
        }

        @Override
        protected Integer EvaluateConnection(HttpURLConnection conn) throws Exception {
            switch (conn.getResponseCode())
            {
                case 200:
                    return super.EvaluateConnection(conn);
                case 404:
                    return StatoCliente.NonTrovato.getValue();
                case 403:
                    return StatoCliente.DiAltroCedi.getValue();
            }
            return StatoCliente.Errore.getValue();
        }

        @Override
        protected Integer EvaluateResponse(String response) throws Exception {
            return Integer.parseInt(response);
        }

        @Override
        protected Integer onError(Exception ex) {
            return StatoCliente.Errore.getValue();
        }
    }

    void controllaTripletta(String tessera, String cognome, String nome, AsyncCallback<Integer> onDone) {
        TLFidelityWS.controllaTripletta controllaTripletta = new controllaTripletta(); //in questo modo no warning
        controllaTripletta.execute(onDone,tessera,cognome,nome);
    }

    static class getPVInfo extends MyCaller<String, String> {

        @Override
        protected String getPage(String... strings) {
            return "/api/PuntoVendita/"+strings[0];
        }

        @Override
        protected String EvaluateResponse(String response) throws Exception {
            return response;
        }

    }

    static String infoPV;
    public static String getInfoPV() {
        return infoPV;
    }

    static void setPV(String id) {
        getPVInfo pv = new getPVInfo();
        pv.execute((String info)-> infoPV=info,id);
    }

    static class salvaCliente extends MyCaller<ClienteInfo, Boolean> {
        boolean nuovo;
        String infoCliente;

        @Override
        protected String getPage(ClienteInfo... clienteInfos) {
            String extra = "";
            nuovo = clienteInfos[0].id==-1;
            if(!nuovo) {
                extra = "/"+Integer.toString(clienteInfos[0].id);
            }
            infoCliente = clienteInfos[0].toWS();
            return "/api/Clienti"+extra;
        }

        @Override
        protected Boolean EvaluateConnection(HttpURLConnection conn) throws Exception {
            conn.setDoOutput(true);
            if(nuovo)
                conn.setRequestMethod("POST");
            else
                conn.setRequestMethod("PUT");
            conn.setRequestProperty("Content-Type", "application/json; utf-8");
            conn.setRequestProperty("Accept", "application/json");
            OutputStream os = conn.getOutputStream();
            os.write(infoCliente.getBytes(StandardCharsets.UTF_8));
            os.close();
            return super.EvaluateConnection(conn);
        }

        @Override
        protected Boolean EvaluateResponse(String response) throws Exception {
            return true;
        }

        @Override
        protected Boolean onError(Exception ex) {
            return false;
        }
    }
    static salvaCliente salvaCliente;
    public void salvaCliente(ClienteInfo info,AsyncCallback<Boolean> onDone) {
        salvaCliente salvaCliente = new salvaCliente();
        salvaCliente.execute(onDone,info);
    }

    static class firme {
        public int id;
        public byte[] firma1;
        public byte[] firma2;
    }

    static class SalvaFirme extends MyCaller<firme, Boolean> {
        byte[] firma1;
        byte[] firma2;
        @Override
        protected String getPage(firme... info) {
            firma1=info[0].firma1;
            firma2=info[0].firma2;
            return "/api/modulo/"+Integer.toString(info[0].id);
        }

        @Override
        protected Boolean EvaluateConnection(HttpURLConnection conn) throws Exception {
            //https://stackoverflow.com/a/11826317/854279
            String boundary = "**0123456789**";
            conn.setDoOutput(true);
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Accept", "application/json");
            /* prova singola
            conn.setRequestProperty("Content-Type", "image/png");
            conn.setRequestProperty("Content-Disposition", "attachment; filename=\"firma1.png\"");
            OutputStream outputStream = conn.getOutputStream();
            outputStream.write(firma1);
            /*/
            conn.setRequestProperty("Content-Type", "multipart/form-data;boundary=\"" + boundary+"\"");
            OutputStream outputStream = conn.getOutputStream();
            boundary="--"+boundary;
            outputStream.write((boundary+ "\r\nContent-Disposition: form-data; name=\"firma1.png\";filename=\"firma1.png\"\r\n\r\n").getBytes(StandardCharsets.US_ASCII));
            outputStream.write(firma1);
            outputStream.write(("\r\n"+boundary+ "\r\nContent-Disposition: form-data; name=\"firma2.png\";filename=\"firma2.png\"\r\n\r\n").getBytes(StandardCharsets.US_ASCII));
            outputStream.write(firma2);
            outputStream.write(("\r\n"+boundary+"--\r\n").getBytes(StandardCharsets.US_ASCII));//*/
            outputStream.close();
            return super.EvaluateConnection(conn);
        }

        @Override
        protected Boolean EvaluateResponse(String response) throws Exception {
            return true;
        }

        @Override
        protected Boolean onError(Exception ex) {
            return false;
        }
    }
    static SalvaFirme SalvaFirme;
    public void SalvaFirme(firme info,AsyncCallback<Boolean> onDone) {
        SalvaFirme salvaCliente = new SalvaFirme();
        salvaCliente.execute(onDone,info);
    }

    static class loadCliente extends MyCaller<Integer, ClienteInfo> {
        @Override
        protected String getPage(Integer... integers) {
            return "/api/Clienti/"+integers[0].toString();
        }

        @Override
        protected ClienteInfo EvaluateResponse(String response) throws Exception {
            ClienteInfo ret = new ClienteInfo();
            ret.FromWS(response);
            return ret;
        }
    }
    static SalvaFirme loadCliente;
    public void loadCliente(int id,AsyncCallback<ClienteInfo> onDone) {
        loadCliente loadCliente = new loadCliente();
        loadCliente.execute(onDone,id);
    }
/*
    static class checkVersione extends MyCaller<Long,Boolean> {
        private Long currVer;
        @Override
        protected String getPage(Long... longs) {
            currVer = longs[0];
            //return "/api/app/ver/release43";
            return "/api/app/ver";
        }

        @Override
        protected Boolean EvaluateResponse(String response) throws Exception {
            long remoteVer = Long.parseLong(response);
            return remoteVer>currVer;
        }

        @Override
        protected Boolean onError(Exception ex) {
            return false;
        }
    }

    void checkVersione(Long versioneAttuale,AsyncCallback<Boolean> onDone) {
        new checkVersione().execute(onDone,versioneAttuale);
    }
*/
static class checkVersioneAndUpdate extends MyCaller<Long,Boolean> {
    private Long currVer;
    @Override
    protected String getPage(Long... longs) {
        currVer = longs[0];
        //return "/api/app/ver/22/release43";
        return "/api/app/verUpd/"+String.valueOf(currVer);
        //return "/api/app/ver";
    }

    @Override
    protected Boolean EvaluateResponse(String response) throws Exception {
        long remoteVer = Long.parseLong(response);
        return remoteVer>currVer;
    }

    @Override
    protected Boolean onError(Exception ex) {
        return false;
    }
}

    void checkVersioneAndUpdate(Long versioneAttuale,AsyncCallback<Boolean> onDone) {
        new checkVersioneAndUpdate().execute(onDone,versioneAttuale);
    }

    static class scaricaNuovaVersione extends AsyncTask<OutputStream,Integer,Boolean> {
        AsyncCallback<Boolean> onDone;
        AsyncCallback<Integer[]> onProgress;
        @SafeVarargs
        final void execute(AsyncCallback<Boolean> onDone,AsyncCallback<Integer[]> onProgress, OutputStream... params) {
            this.onDone=onDone;
            this.onProgress=onProgress;
            super.execute(params);
        }

        @Override
        protected Boolean doInBackground(OutputStream... destPath) {
            Boolean retValue = null;
            HttpURLConnection conn = null;
            if(baseUrl==null) {
                return false;
            }
            try {
                // provo la configurazione
                // conn = GetConnection("/api/app/apk/release43");
                conn = GetConnection("/api/app/apk");
                int contentLength = conn.getContentLength();
                InputStream inputStream = conn.getInputStream();

                // opens an output stream to save into file
                OutputStream outputStream = destPath[0];

                int bytesRead = -1,totalRead=0;
                byte[] buffer = new byte[1024];
                while ((bytesRead = inputStream.read(buffer)) != -1) {
                    outputStream.write(buffer, 0, bytesRead);
                    totalRead+=bytesRead;
                    publishProgress(totalRead,contentLength);
                }
                outputStream.flush();
                outputStream.close();
                retValue = true;
            } catch (Exception e) {
                e.printStackTrace();
                retValue = false;
            } finally {
                if(conn!=null)
                    conn.disconnect();
            }
            return retValue;
        }

        @Override
        protected void onProgressUpdate(Integer... values) {
            onProgress.call(values);
        }

        @Override
        protected void onPostExecute(Boolean result) {
            onDone.call(result);
        }
    }

    void scaricaNuovaVersione(OutputStream outputStream, AsyncCallback<Boolean> onDone,AsyncCallback<Integer[]> onProgress) {
        new scaricaNuovaVersione().execute(onDone,onProgress, outputStream);
    }

    static class inviaSegnalazione extends MyCaller<String, Boolean> {
        @Override
        protected String getPage(String... strings) {
            String page = "/api/Clienti/segnalazioni";
            page += "?email="+strings[0];
            page += "&codiceTessera="+strings[1];
            page += "&cognome="+strings[2];
            page += "&nome="+ strings[3];
            return page;
        }

        @Override
        protected Boolean EvaluateResponse(String msg) throws Exception {
            return Boolean.parseBoolean(msg);
        }
        @Override
        protected Boolean onError(Exception ex) {
            return false;
        }
    }

      void inviaSegnalazione(String email,String codiceTessera, String cognome, String nome, AsyncCallback<Boolean> onDone) {
        TLFidelityWS.inviaSegnalazione inviaSegnalazione = new inviaSegnalazione();
        inviaSegnalazione.execute(onDone,email,codiceTessera,cognome,nome);
    }

    static class emailCliente extends MyCaller<String, Void> {
        String cliente, email;
        @Override
        protected String getPage(String... strings) {
            String page = "/api/modulo/email/"+strings[0]+"/"+strings[1];
            cliente = strings[2];
            email = strings[3];
            return page;
        }

        @Override
        protected Void EvaluateConnection(HttpURLConnection conn) throws Exception {
            conn.setDoOutput(true);
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json; utf-8");
            conn.setRequestProperty("Accept", "application/json");
            OutputStream os = conn.getOutputStream();
            String jsonEmail="{\n" +
                    "  \"From\": {\n" +
                    "    \"name\": \"Crai supermercati\",\n" +
                    "    \"address\": \"cartapiucrai@crai.org\"\n" +
                    "  },\n" +
                    "  \"To\": [{\n" +
                    "      \"name\": \""+cliente+"\",\n" +
                    "      \"address\": \""+email+"\"\n" +
                    "    }],\n" +
                    "  \"CC\": [],\n" +
                    "  \"Bcc\": [],\n" +
                    "  \"Subject\": \"Benvenuto/a nel programma fedeltà CRAI Carta Più\",\n" +
                    "  \"Body\": {\n" +
                    "    \"html\": true,\n" +
                    "    \"text\": \"<!DOCTYPE html PUBLIC \\\"-//W3C//DTD XHTML 1.0 Transitional //EN\\\" \\\"http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd\\\">\\n<html xmlns=\\\"http://www.w3.org/1999/xhtml\\\" xmlns:v=\\\"urn:schemas-microsoft-com:vml\\\" xmlns:o=\\\"urn:schemas-microsoft-com:office:office\\\">\\n\\n<head>\\n    <!--[if gte mso 9]><xml><o:OfficeDocumentSettings><o:AllowPNG/><o:PixelsPerInch>96</o:PixelsPerInch></o:OfficeDocumentSettings></xml><![endif]-->\\n    <meta http-equiv=\\\"Content-Type\\\" content=\\\"text/html; charset=utf-8\\\">\\n    <meta name=\\\"viewport\\\" content=\\\"width=device-width\\\">\\n    <!--[if !mso]><!-->\\n    <meta http-equiv=\\\"X-UA-Compatible\\\" content=\\\"IE=edge\\\">\\n    <!--<![endif]-->\\n    <title></title>\\n    <!--[if !mso]><!-->\\n    <link href=\\\"https://fonts.googleapis.com/css?family=Montserrat\\\" rel=\\\"stylesheet\\\" type=\\\"text/css\\\">\\n    <!--<![endif]-->\\n    <style type=\\\"text/css\\\">\\n\\t\\tbody {\\n\\t\\t\\tmargin: 0;\\n\\t\\t\\tpadding: 0;\\n\\t\\t}\\n\\n\\t\\ttable,\\n\\t\\ttd,\\n\\t\\ttr {\\n\\t\\t\\tvertical-align: top;\\n\\t\\t\\tborder-collapse: collapse;\\n\\t\\t}\\n\\n\\t\\t* {\\n\\t\\t\\tline-height: inherit;\\n\\t\\t}\\n\\n\\t\\ta[x-apple-data-detectors=true] {\\n\\t\\t\\tcolor: inherit !important;\\n\\t\\t\\ttext-decoration: none !important;\\n\\t\\t}\\n\\t</style>\\n    <style type=\\\"text/css\\\" id=\\\"media-query\\\">\\n\\t\\t@media (max-width: 670px) {\\n\\n\\t\\t\\t.block-grid,\\n\\t\\t\\t.col {\\n\\t\\t\\t\\tmin-width: 320px !important;\\n\\t\\t\\t\\tmax-width: 100% !important;\\n\\t\\t\\t\\tdisplay: block !important;\\n\\t\\t\\t}\\n\\n\\t\\t\\t.block-grid {\\n\\t\\t\\t\\twidth: 100% !important;\\n\\t\\t\\t}\\n\\n\\t\\t\\t.col {\\n\\t\\t\\t\\twidth: 100% !important;\\n\\t\\t\\t}\\n\\n\\t\\t\\t.col>div {\\n\\t\\t\\t\\tmargin: 0 auto;\\n\\t\\t\\t}\\n\\n\\t\\t\\timg.fullwidth,\\n\\t\\t\\timg.fullwidthOnMobile {\\n\\t\\t\\t\\tmax-width: 100% !important;\\n\\t\\t\\t}\\n\\n\\t\\t\\t.no-stack .col {\\n\\t\\t\\t\\tmin-width: 0 !important;\\n\\t\\t\\t\\tdisplay: table-cell !important;\\n\\t\\t\\t}\\n\\n\\t\\t\\t.no-stack.two-up .col {\\n\\t\\t\\t\\twidth: 50% !important;\\n\\t\\t\\t}\\n\\n\\t\\t\\t.no-stack .col.num4 {\\n\\t\\t\\t\\twidth: 33% !important;\\n\\t\\t\\t}\\n\\n\\t\\t\\t.no-stack .col.num8 {\\n\\t\\t\\t\\twidth: 66% !important;\\n\\t\\t\\t}\\n\\n\\t\\t\\t.no-stack .col.num4 {\\n\\t\\t\\t\\twidth: 33% !important;\\n\\t\\t\\t}\\n\\n\\t\\t\\t.no-stack .col.num3 {\\n\\t\\t\\t\\twidth: 25% !important;\\n\\t\\t\\t}\\n\\n\\t\\t\\t.no-stack .col.num6 {\\n\\t\\t\\t\\twidth: 50% !important;\\n\\t\\t\\t}\\n\\n\\t\\t\\t.no-stack .col.num9 {\\n\\t\\t\\t\\twidth: 75% !important;\\n\\t\\t\\t}\\n\\n\\t\\t\\t.video-block {\\n\\t\\t\\t\\tmax-width: none !important;\\n\\t\\t\\t}\\n\\n\\t\\t\\t.mobile_hide {\\n\\t\\t\\t\\tmin-height: 0px;\\n\\t\\t\\t\\tmax-height: 0px;\\n\\t\\t\\t\\tmax-width: 0px;\\n\\t\\t\\t\\tdisplay: none;\\n\\t\\t\\t\\toverflow: hidden;\\n\\t\\t\\t\\tfont-size: 0px;\\n\\t\\t\\t}\\n\\n\\t\\t\\t.desktop_hide {\\n\\t\\t\\t\\tdisplay: block !important;\\n\\t\\t\\t\\tmax-height: none !important;\\n\\t\\t\\t}\\n\\t\\t}\\n\\t</style>\\n</head>\\n\\n<body class=\\\"clean-body\\\" style=\\\"margin: 0; padding: 0; -webkit-text-size-adjust: 100%; background-color: #FFFFFF;\\\">\\n<!--[if IE]><div class=\\\"ie-browser\\\"><![endif]-->\\n<table class=\\\"nl-container\\\" style=\\\"table-layout: fixed; vertical-align: top; min-width: 320px; Margin: 0 auto; border-spacing: 0; border-collapse: collapse; mso-table-lspace: 0pt; mso-table-rspace: 0pt; background-color: #FFFFFF; width: 100%;\\\" cellpadding=\\\"0\\\" cellspacing=\\\"0\\\" role=\\\"presentation\\\" width=\\\"100%\\\" bgcolor=\\\"#FFFFFF\\\" valign=\\\"top\\\">\\n    <tbody>\\n    <tr style=\\\"vertical-align: top;\\\" valign=\\\"top\\\">\\n        <td style=\\\"word-break: break-word; vertical-align: top;\\\" valign=\\\"top\\\">\\n            <!--[if (mso)|(IE)]><table width=\\\"100%\\\" cellpadding=\\\"0\\\" cellspacing=\\\"0\\\" border=\\\"0\\\"><tr><td align=\\\"center\\\" style=\\\"background-color:#FFFFFF\\\"><![endif]-->\\n            <div style=\\\"background-color:transparent;\\\">\\n                <div class=\\\"block-grid \\\" style=\\\"Margin: 0 auto; min-width: 320px; max-width: 650px; overflow-wrap: break-word; word-wrap: break-word; word-break: break-word; background-color: #FFFFFF;\\\">\\n                    <div style=\\\"border-collapse: collapse;display: table;width: 100%;background-color:#FFFFFF;\\\">\\n                        <!--[if (mso)|(IE)]><table width=\\\"100%\\\" cellpadding=\\\"0\\\" cellspacing=\\\"0\\\" border=\\\"0\\\" style=\\\"background-color:transparent;\\\"><tr><td align=\\\"center\\\"><table cellpadding=\\\"0\\\" cellspacing=\\\"0\\\" border=\\\"0\\\" style=\\\"width:650px\\\"><tr class=\\\"layout-full-width\\\" style=\\\"background-color:#FFFFFF\\\"><![endif]-->\\n                        <!--[if (mso)|(IE)]><td align=\\\"center\\\" width=\\\"650\\\" style=\\\"background-color:#FFFFFF;width:650px; border-top: 0px solid transparent; border-left: 0px solid transparent; border-bottom: 0px solid transparent; border-right: 0px solid transparent;\\\" valign=\\\"top\\\"><table width=\\\"100%\\\" cellpadding=\\\"0\\\" cellspacing=\\\"0\\\" border=\\\"0\\\"><tr><td style=\\\"padding-right: 0px; padding-left: 0px; padding-top:0px; padding-bottom:0px;\\\"><![endif]-->\\n                        <div class=\\\"col num12\\\" style=\\\"min-width: 320px; max-width: 650px; display: table-cell; vertical-align: top; width: 650px;\\\">\\n                            <div style=\\\"width:100% !important;\\\">\\n                                <!--[if (!mso)&(!IE)]><!-->\\n                                <div style=\\\"border-top:0px solid transparent; border-left:0px solid transparent; border-bottom:0px solid transparent; border-right:0px solid transparent; padding-top:0px; padding-bottom:0px; padding-right: 0px; padding-left: 0px;\\\">\\n                                    <!--<![endif]-->\\n                                    <div class=\\\"img-container center  autowidth \\\" align=\\\"center\\\" style=\\\"padding-right: 0px;padding-left: 0px;\\\">\\n                                        <!--[if mso]><table width=\\\"100%\\\" cellpadding=\\\"0\\\" cellspacing=\\\"0\\\" border=\\\"0\\\"><tr style=\\\"line-height:0px\\\"><td style=\\\"padding-right: 0px;padding-left: 0px;\\\" align=\\\"center\\\"><![endif]--><a href=\\\"https://www.crai-supermercati.it/loyalty/index.php\\\" target=\\\"_blank\\\" style=\\\"outline:none\\\" tabindex=\\\"-1\\\"> <img class=\\\"center  autowidth \\\" align=\\\"center\\\" border=\\\"0\\\" src=\\\"http://tloyalty.crai.org/AppServer01/Statico/TLCustomerApp/images/Nuovi_iscritti_01.jpg\\\" alt=\\\"CRAI: nel cuore dell'italia\\\" title=\\\"CRAI: nel cuore dell'italia\\\" style=\\\"text-decoration: none; -ms-interpolation-mode: bicubic; height: auto; border: none; width: 100%; max-width: 649px; display: block;\\\" width=\\\"649\\\"></a>\\n                                        <!--[if mso]></td></tr></table><![endif]-->\\n                                    </div>\\n                                    <div class=\\\"img-container center  autowidth \\\" align=\\\"center\\\" style=\\\"padding-right: 0px;padding-left: 0px;\\\">\\n                                        <!--[if mso]><table width=\\\"100%\\\" cellpadding=\\\"0\\\" cellspacing=\\\"0\\\" border=\\\"0\\\"><tr style=\\\"line-height:0px\\\"><td style=\\\"padding-right: 0px;padding-left: 0px;\\\" align=\\\"center\\\"><![endif]--><a href=\\\"https://www.crai-supermercati.it/loyalty/index.php\\\" target=\\\"_blank\\\" style=\\\"outline:none\\\" tabindex=\\\"-1\\\"> <img class=\\\"center  autowidth \\\" align=\\\"center\\\" border=\\\"0\\\" src=\\\"http://tloyalty.crai.org/AppServer01/Statico/TLCustomerApp/images/Nuovi_iscritti_02.jpg\\\" alt=\\\"Benvenuto nel programma fedeltà\\\" title=\\\"Benvenuto nel programma fedeltà\\\" style=\\\"text-decoration: none; -ms-interpolation-mode: bicubic; height: auto; border: none; width: 100%; max-width: 649px; display: block;\\\" width=\\\"649\\\"></a>\\n                                        <!--[if mso]></td></tr></table><![endif]-->\\n                                    </div>\\n                                    <!--[if mso]><table width=\\\"100%\\\" cellpadding=\\\"0\\\" cellspacing=\\\"0\\\" border=\\\"0\\\"><tr><td style=\\\"padding-right: 10px; padding-left: 10px; padding-top: 30px; padding-bottom: 30px; font-family: Arial, sans-serif\\\"><![endif]-->\\n                                    <div style=\\\"color:#00673a;font-family:'Helvetica Neue', Helvetica, Arial, sans-serif;line-height:1.2;padding-top:30px;padding-right:10px;padding-bottom:30px;padding-left:10px;\\\">\\n                                        <div style=\\\"font-family: 'Helvetica Neue', Helvetica, Arial, sans-serif; font-size: 12px; line-height: 1.2; color: #00673a; mso-line-height-alt: 14px;\\\">\\n                                            <p style=\\\"font-size: 22px; line-height: 1.2; text-align: center; mso-line-height-alt: 26px; margin: 0;\\\"><span style=\\\"font-size: 22px;\\\"><strong>USI <span style=\\\"color: #dc281f; font-size: 22px;\\\">CARTA PIÙ</span> IN OGNI SUA SPESA... </strong></span></p>\\n                                            <p style=\\\"font-size: 22px; line-height: 1.2; text-align: center; mso-line-height-alt: 26px; margin: 0;\\\"><span style=\\\"font-size: 22px;\\\">ACCUMULERÀ <strong>1 PUNTO</strong></span></p>\\n                                            <p style=\\\"font-size: 22px; line-height: 1.2; text-align: center; mso-line-height-alt: 26px; margin: 0;\\\"><span style=\\\"font-size: 22px;\\\">OGNI EURO DI SPESA EFFETTUATO.</span></p>\\n                                        </div>\\n                                    </div>\\n                                    <!--[if mso]></td></tr></table><![endif]-->\\n                                    <div class=\\\"img-container center  autowidth  fullwidth\\\" align=\\\"center\\\" style=\\\"padding-right: 0px;padding-left: 0px;\\\">\\n                                        <!--[if mso]><table width=\\\"100%\\\" cellpadding=\\\"0\\\" cellspacing=\\\"0\\\" border=\\\"0\\\"><tr style=\\\"line-height:0px\\\"><td style=\\\"padding-right: 0px;padding-left: 0px;\\\" align=\\\"center\\\"><![endif]--><a href=\\\"https://www.crai-supermercati.it/loyalty/index.php\\\" target=\\\"_blank\\\" style=\\\"outline:none\\\" tabindex=\\\"-1\\\"> <img class=\\\"center  autowidth  fullwidth\\\" align=\\\"center\\\" border=\\\"0\\\" src=\\\"http://tloyalty.crai.org/AppServer01/Statico/TLCustomerApp/images/Nuovi_iscritti_07.jpg\\\" alt=\\\"SCOPRI DI PIÙ\\\" title=\\\"SCOPRI DI PIÙ\\\" style=\\\"text-decoration: none; -ms-interpolation-mode: bicubic; height: auto; border: none; width: 100%; max-width: 650px; display: block;\\\" width=\\\"650\\\"></a>\\n                                        <!--[if mso]></td></tr></table><![endif]-->\\n                                    </div>\\n                                    <!--[if (!mso)&(!IE)]><!-->\\n                                </div>\\n                                <!--<![endif]-->\\n                            </div>\\n                        </div>\\n                        <!--[if (mso)|(IE)]></td></tr></table><![endif]-->\\n                        <!--[if (mso)|(IE)]></td></tr></table></td></tr></table><![endif]-->\\n                    </div>\\n                </div>\\n            </div>\\n            <div style=\\\"background-color:transparent;\\\">\\n                <div class=\\\"block-grid two-up\\\" style=\\\"Margin: 0 auto; min-width: 320px; max-width: 650px; overflow-wrap: break-word; word-wrap: break-word; word-break: break-word; background-color: #a7ba5d;\\\">\\n                    <div style=\\\"border-collapse: collapse;display: table;width: 100%;background-color:#a7ba5d;\\\">\\n                        <!--[if (mso)|(IE)]><table width=\\\"100%\\\" cellpadding=\\\"0\\\" cellspacing=\\\"0\\\" border=\\\"0\\\" style=\\\"background-color:transparent;\\\"><tr><td align=\\\"center\\\"><table cellpadding=\\\"0\\\" cellspacing=\\\"0\\\" border=\\\"0\\\" style=\\\"width:650px\\\"><tr class=\\\"layout-full-width\\\" style=\\\"background-color:#a7ba5d\\\"><![endif]-->\\n                        <!--[if (mso)|(IE)]><td align=\\\"center\\\" width=\\\"325\\\" style=\\\"background-color:#a7ba5d;width:325px; border-top: 0px solid transparent; border-left: 0px solid transparent; border-bottom: 0px solid transparent; border-right: 0px solid transparent;\\\" valign=\\\"top\\\"><table width=\\\"100%\\\" cellpadding=\\\"0\\\" cellspacing=\\\"0\\\" border=\\\"0\\\"><tr><td style=\\\"padding-right: 0px; padding-left: 0px; padding-top:0px; padding-bottom:0px;\\\"><![endif]-->\\n                        <div class=\\\"col num6\\\" style=\\\"min-width: 320px; max-width: 325px; display: table-cell; vertical-align: top; width: 325px;\\\">\\n                            <div style=\\\"width:100% !important;\\\">\\n                                <!--[if (!mso)&(!IE)]><!-->\\n                                <div style=\\\"border-top:0px solid transparent; border-left:0px solid transparent; border-bottom:0px solid transparent; border-right:0px solid transparent; padding-top:0px; padding-bottom:0px; padding-right: 0px; padding-left: 0px;\\\">\\n                                    <!--<![endif]-->\\n                                    <div class=\\\"button-container\\\" align=\\\"center\\\" style=\\\"padding-top:10px;padding-right:10px;padding-bottom:10px;padding-left:10px;\\\">\\n                                        <!--[if mso]><table width=\\\"100%\\\" cellpadding=\\\"0\\\" cellspacing=\\\"0\\\" border=\\\"0\\\" style=\\\"border-spacing: 0; border-collapse: collapse; mso-table-lspace:0pt; mso-table-rspace:0pt;\\\"><tr><td style=\\\"padding-top: 10px; padding-right: 10px; padding-bottom: 10px; padding-left: 10px\\\" align=\\\"center\\\"><v:roundrect xmlns:v=\\\"urn:schemas-microsoft-com:vml\\\" xmlns:w=\\\"urn:schemas-microsoft-com:office:word\\\" href=\\\"https://www.crai-supermercati.it/loyalty/index.php\\\" style=\\\"height:40.5pt; width:155.25pt; v-text-anchor:middle;\\\" arcsize=\\\"19%\\\" strokeweight=\\\"1.5pt\\\" strokecolor=\\\"#FFFFFF\\\" fillcolor=\\\"#e40324\\\"><w:anchorlock/><v:textbox inset=\\\"0,0,0,0\\\"><center style=\\\"color:#ffffff; font-family:Arial, sans-serif; font-size:16px\\\"><![endif]--><a href=\\\"https://www.crai-supermercati.it/loyalty/index.php\\\" target=\\\"_blank\\\" style=\\\"-webkit-text-size-adjust: none; text-decoration: none; display: inline-block; color: #ffffff; background-color: #e40324; border-radius: 10px; -webkit-border-radius: 10px; -moz-border-radius: 10px; width: auto; width: auto; border-top: 2px solid #FFFFFF; border-right: 2px solid #FFFFFF; border-bottom: 2px solid #FFFFFF; border-left: 2px solid #FFFFFF; padding-top: 5px; padding-bottom: 5px; font-family: 'Helvetica Neue', Helvetica, Arial, sans-serif; text-align: center; mso-border-alt: none; word-break: keep-all;\\\"><span style=\\\"padding-left:20px;padding-right:20px;font-size:16px;display:inline-block;\\\">\\n\\t\\t\\t\\t\\t\\t\\t\\t\\t\\t\\t\\t\\t\\t<span style=\\\"font-size: 16px; line-height: 1.2; mso-line-height-alt: 19px;\\\"><strong>REGISTRAZIONE AL </strong></span>\\n\\t\\t\\t\\t\\t\\t\\t\\t\\t\\t\\t\\t\\t\\t<span style=\\\"font-size: 16px; line-height: 1.2; mso-line-height-alt: 19px;\\\"><br><strong>NOSTRO SITO WEB</strong></span>\\n\\t\\t\\t\\t\\t\\t\\t\\t\\t\\t\\t\\t\\t</span></a>\\n                                        <!--[if mso]></center></v:textbox></v:roundrect></td></tr></table><![endif]-->\\n                                    </div>\\n                                    <!--[if (!mso)&(!IE)]><!-->\\n                                </div>\\n                                <!--<![endif]-->\\n                            </div>\\n                        </div>\\n                        <!--[if (mso)|(IE)]></td></tr></table><![endif]-->\\n                        <!--[if (mso)|(IE)]></td><td align=\\\"center\\\" width=\\\"325\\\" style=\\\"background-color:#a7ba5d;width:325px; border-top: 0px solid transparent; border-left: 0px solid transparent; border-bottom: 0px solid transparent; border-right: 0px solid transparent;\\\" valign=\\\"top\\\"><table width=\\\"100%\\\" cellpadding=\\\"0\\\" cellspacing=\\\"0\\\" border=\\\"0\\\"><tr><td style=\\\"padding-right: 0px; padding-left: 0px; padding-top:0px; padding-bottom:0px;\\\"><![endif]-->\\n                        <!--[if (mso)|(IE)]></td></tr></table><![endif]-->\\n                        <!--[if (mso)|(IE)]></td></tr></table></td></tr></table><![endif]-->\\n                    </div>\\n                </div>\\n            </div>\\n            <div style=\\\"background-color:transparent;\\\">\\n                <div class=\\\"block-grid \\\" style=\\\"Margin: 0 auto; min-width: 320px; max-width: 650px; overflow-wrap: break-word; word-wrap: break-word; word-break: break-word; background-color: #a7ba5d;\\\">\\n                    <div style=\\\"border-collapse: collapse;display: table;width: 100%;background-color:#a7ba5d;\\\">\\n                        <!--[if (mso)|(IE)]><table width=\\\"100%\\\" cellpadding=\\\"0\\\" cellspacing=\\\"0\\\" border=\\\"0\\\" style=\\\"background-color:transparent;\\\"><tr><td align=\\\"center\\\"><table cellpadding=\\\"0\\\" cellspacing=\\\"0\\\" border=\\\"0\\\" style=\\\"width:650px\\\"><tr class=\\\"layout-full-width\\\" style=\\\"background-color:#a7ba5d\\\"><![endif]-->\\n                        <!--[if (mso)|(IE)]><td align=\\\"center\\\" width=\\\"650\\\" style=\\\"background-color:#a7ba5d;width:650px; border-top: 0px solid transparent; border-left: 0px solid transparent; border-bottom: 0px solid transparent; border-right: 0px solid transparent;\\\" valign=\\\"top\\\"><table width=\\\"100%\\\" cellpadding=\\\"0\\\" cellspacing=\\\"0\\\" border=\\\"0\\\"><tr><td style=\\\"padding-right: 0px; padding-left: 0px; padding-top:0px; padding-bottom:0px;\\\"><![endif]-->\\n                        <div class=\\\"col num12\\\" style=\\\"min-width: 320px; max-width: 650px; display: table-cell; vertical-align: top; width: 650px;\\\">\\n                            <div style=\\\"width:100% !important;\\\">\\n                                <!--[if (!mso)&(!IE)]><!-->\\n                                <div style=\\\"border-top:0px solid transparent; border-left:0px solid transparent; border-bottom:0px solid transparent; border-right:0px solid transparent; padding-top:0px; padding-bottom:0px; padding-right: 0px; padding-left: 0px;\\\">\\n                                    <!--<![endif]-->\\n                                    <!--[if mso]><table width=\\\"100%\\\" cellpadding=\\\"0\\\" cellspacing=\\\"0\\\" border=\\\"0\\\"><tr><td style=\\\"padding-right: 10px; padding-left: 10px; padding-top: 0px; padding-bottom: 0px; font-family: Arial, sans-serif\\\"><![endif]-->\\n                                    <div style=\\\"color:#00673a;font-family:'Helvetica Neue', Helvetica, Arial, sans-serif;line-height:1.5;padding-top:0px;padding-right:10px;padding-bottom:0px;padding-left:10px;\\\">\\n                                        <div style=\\\"font-family: 'Helvetica Neue', Helvetica, Arial, sans-serif; font-size: 12px; line-height: 1.5; color: #00673a; mso-line-height-alt: 18px;\\\">\\n                                            <p style=\\\"font-size: 14px; line-height: 1.5; text-align: center; mso-line-height-alt: 21px; margin: 0;\\\"><strong><span style=\\\"font-size: 20px;\\\">PER VISUALIZZARE IL SUO SALDO PUNTI</span></strong></p>\\n                                            <p style=\\\"font-size: 14px; line-height: 1.5; text-align: center; mso-line-height-alt: 21px; margin: 0;\\\"><strong><span style=\\\"font-size: 20px;\\\">E SCOPRIRE TUTTI I NOSTRI SERVIZI.</span></strong></p>\\n                                        </div>\\n                                    </div>\\n                                    <!--[if mso]></td></tr></table><![endif]-->\\n                                    <!--[if (!mso)&(!IE)]><!-->\\n                                </div>\\n                                <!--<![endif]-->\\n                            </div>\\n                        </div>\\n                        <!--[if (mso)|(IE)]></td></tr></table><![endif]-->\\n                        <!--[if (mso)|(IE)]></td></tr></table></td></tr></table><![endif]-->\\n                    </div>\\n                </div>\\n            </div>\\n            <div style=\\\"background-color:transparent;\\\">\\n                <div class=\\\"block-grid \\\" style=\\\"Margin: 0 auto; min-width: 320px; max-width: 650px; overflow-wrap: break-word; word-wrap: break-word; word-break: break-word; background-color: #FFFFFF;\\\">\\n                    <div style=\\\"border-collapse: collapse;display: table;width: 100%;background-color:#FFFFFF;\\\">\\n                        <!--[if (mso)|(IE)]><table width=\\\"100%\\\" cellpadding=\\\"0\\\" cellspacing=\\\"0\\\" border=\\\"0\\\" style=\\\"background-color:transparent;\\\"><tr><td align=\\\"center\\\"><table cellpadding=\\\"0\\\" cellspacing=\\\"0\\\" border=\\\"0\\\" style=\\\"width:650px\\\"><tr class=\\\"layout-full-width\\\" style=\\\"background-color:#FFFFFF\\\"><![endif]-->\\n                        <!--[if (mso)|(IE)]><td align=\\\"center\\\" width=\\\"650\\\" style=\\\"background-color:#FFFFFF;width:650px; border-top: 0px solid transparent; border-left: 0px solid transparent; border-bottom: 0px solid transparent; border-right: 0px solid transparent;\\\" valign=\\\"top\\\"><table width=\\\"100%\\\" cellpadding=\\\"0\\\" cellspacing=\\\"0\\\" border=\\\"0\\\"><tr><td style=\\\"padding-right: 0px; padding-left: 0px; padding-top:0px; padding-bottom:5px;\\\"><![endif]-->\\n                        <div class=\\\"col num12\\\" style=\\\"min-width: 320px; max-width: 650px; display: table-cell; vertical-align: top; width: 650px;\\\">\\n                            <div style=\\\"width:100% !important;\\\">\\n                                <!--[if (!mso)&(!IE)]><!-->\\n                                <div style=\\\"border-top:0px solid transparent; border-left:0px solid transparent; border-bottom:0px solid transparent; border-right:0px solid transparent; padding-top:0px; padding-bottom:5px; padding-right: 0px; padding-left: 0px;\\\">\\n                                    <!--<![endif]-->\\n                                    <div class=\\\"img-container center  autowidth  fullwidth\\\" align=\\\"center\\\" style=\\\"padding-right: 0px;padding-left: 0px;\\\">\\n                                        <!--[if mso]><table width=\\\"100%\\\" cellpadding=\\\"0\\\" cellspacing=\\\"0\\\" border=\\\"0\\\"><tr style=\\\"line-height:0px\\\"><td style=\\\"padding-right: 0px;padding-left: 0px;\\\" align=\\\"center\\\"><![endif]--><img class=\\\"center  autowidth  fullwidth\\\" align=\\\"center\\\" border=\\\"0\\\" src=\\\"http://tloyalty.crai.org/AppServer01/Statico/TLCustomerApp/images/Nuovi_iscritti_08.jpg\\\" alt=\\\"Image\\\" title=\\\"Image\\\" style=\\\"text-decoration: none; -ms-interpolation-mode: bicubic; border: 0; height: auto; width: 100%; max-width: 650px; display: block;\\\" width=\\\"650\\\">\\n                                        <!--[if mso]></td></tr></table><![endif]-->\\n                                    </div>\\n                                    <!--[if mso]><table width=\\\"100%\\\" cellpadding=\\\"0\\\" cellspacing=\\\"0\\\" border=\\\"0\\\"><tr><td style=\\\"padding-right: 10px; padding-left: 10px; padding-top: 25px; padding-bottom: 25px; font-family: Arial, sans-serif\\\"><![endif]-->\\n                                    <div style=\\\"color:#000000;font-family:'Helvetica Neue', Helvetica, Arial, sans-serif;line-height:1.2;padding-top:25px;padding-right:10px;padding-bottom:25px;padding-left:10px;\\\">\\n                                        <div style=\\\"font-family: 'Helvetica Neue', Helvetica, Arial, sans-serif; font-size: 12px; line-height: 1.2; color: #000000; mso-line-height-alt: 14px;\\\">\\n                                            <p style=\\\"font-size: 22px; line-height: 1.2; text-align: center; mso-line-height-alt: 26px; margin: 0;\\\"><span style=\\\"font-size: 22px;\\\">In allegato trova il modulo <strong>Carta Più</strong></span></p>\\n                                            <p style=\\\"font-size: 22px; line-height: 1.2; text-align: center; mso-line-height-alt: 26px; margin: 0;\\\"><span style=\\\"font-size: 22px;\\\">da lei sottoscritto in negozio. </span></p>\\n                                            <p style=\\\"font-size: 14px; line-height: 1.2; text-align: center; mso-line-height-alt: 17px; margin: 0;\\\">&nbsp;</p>\\n                                            <p style=\\\"font-size: 14px; line-height: 1.2; text-align: center; mso-line-height-alt: 17px; margin: 0;\\\"><strong><span style=\\\"font-size: 22px;\\\">Buona spesa da CRAI</span></strong></p>\\n                                        </div>\\n                                    </div>\\n                                    <!--[if mso]></td></tr></table><![endif]-->\\n                                    <!--[if (!mso)&(!IE)]><!-->\\n                                </div>\\n                                <!--<![endif]-->\\n                            </div>\\n                        </div>\\n                        <!--[if (mso)|(IE)]></td></tr></table><![endif]-->\\n                        <!--[if (mso)|(IE)]></td></tr></table></td></tr></table><![endif]-->\\n                    </div>\\n                </div>\\n            </div>\\n            <div style=\\\"background-color:transparent;\\\">\\n                <div class=\\\"block-grid \\\" style=\\\"Margin: 0 auto; min-width: 320px; max-width: 650px; overflow-wrap: break-word; word-wrap: break-word; word-break: break-word; background-color: #FFFFFF;\\\">\\n                    <div style=\\\"border-collapse: collapse;display: table;width: 100%;background-color:#FFFFFF;\\\">\\n                        <!--[if (mso)|(IE)]><table width=\\\"100%\\\" cellpadding=\\\"0\\\" cellspacing=\\\"0\\\" border=\\\"0\\\" style=\\\"background-color:transparent;\\\"><tr><td align=\\\"center\\\"><table cellpadding=\\\"0\\\" cellspacing=\\\"0\\\" border=\\\"0\\\" style=\\\"width:650px\\\"><tr class=\\\"layout-full-width\\\" style=\\\"background-color:#FFFFFF\\\"><![endif]-->\\n                        <!--[if (mso)|(IE)]><td align=\\\"center\\\" width=\\\"650\\\" style=\\\"background-color:#FFFFFF;width:650px; border-top: 0px solid transparent; border-left: 0px solid transparent; border-bottom: 0px solid transparent; border-right: 0px solid transparent;\\\" valign=\\\"top\\\"><table width=\\\"100%\\\" cellpadding=\\\"0\\\" cellspacing=\\\"0\\\" border=\\\"0\\\"><tr><td style=\\\"padding-right: 0px; padding-left: 0px; padding-top:5px; padding-bottom:0px;\\\"><![endif]-->\\n                        <div class=\\\"col num12\\\" style=\\\"min-width: 320px; max-width: 650px; display: table-cell; vertical-align: top; width: 650px;\\\">\\n                            <div style=\\\"width:100% !important;\\\">\\n                                <!--[if (!mso)&(!IE)]><!-->\\n                                <div style=\\\"border-top:0px solid transparent; border-left:0px solid transparent; border-bottom:0px solid transparent; border-right:0px solid transparent; padding-top:5px; padding-bottom:0px; padding-right: 0px; padding-left: 0px;\\\">\\n                                    <!--<![endif]-->\\n                                    <div class=\\\"img-container center  autowidth \\\" align=\\\"center\\\" style=\\\"padding-right: 0px;padding-left: 0px;\\\">\\n                                        <!--[if mso]><table width=\\\"100%\\\" cellpadding=\\\"0\\\" cellspacing=\\\"0\\\" border=\\\"0\\\"><tr style=\\\"line-height:0px\\\"><td style=\\\"padding-right: 0px;padding-left: 0px;\\\" align=\\\"center\\\"><![endif]--><img class=\\\"center  autowidth \\\" align=\\\"center\\\" border=\\\"0\\\" src=\\\"http://tloyalty.crai.org/AppServer01/Statico/TLCustomerApp/images/Nuovi_iscritti_05.jpg\\\" alt=\\\"Image\\\" title=\\\"Image\\\" style=\\\"text-decoration: none; -ms-interpolation-mode: bicubic; border: 0; height: auto; width: 100%; max-width: 649px; display: block;\\\" width=\\\"649\\\">\\n                                        <!--[if mso]></td></tr></table><![endif]-->\\n                                    </div>\\n                                    <!--[if (!mso)&(!IE)]><!-->\\n                                </div>\\n                                <!--<![endif]-->\\n                            </div>\\n                        </div>\\n                        <!--[if (mso)|(IE)]></td></tr></table><![endif]-->\\n                        <!--[if (mso)|(IE)]></td></tr></table></td></tr></table><![endif]-->\\n                    </div>\\n                </div>\\n            </div>\\n            <div style=\\\"background-color:transparent;\\\">\\n                <div class=\\\"block-grid two-up\\\" style=\\\"Margin: 0 auto; min-width: 320px; max-width: 650px; overflow-wrap: break-word; word-wrap: break-word; word-break: break-word; background-color: #176832;\\\">\\n                    <div style=\\\"border-collapse: collapse;display: table;width: 100%;background-color:#176832;\\\">\\n                        <!--[if (mso)|(IE)]><table width=\\\"100%\\\" cellpadding=\\\"0\\\" cellspacing=\\\"0\\\" border=\\\"0\\\" style=\\\"background-color:transparent;\\\"><tr><td align=\\\"center\\\"><table cellpadding=\\\"0\\\" cellspacing=\\\"0\\\" border=\\\"0\\\" style=\\\"width:650px\\\"><tr class=\\\"layout-full-width\\\" style=\\\"background-color:#176832\\\"><![endif]-->\\n                        <!--[if (mso)|(IE)]><td align=\\\"center\\\" width=\\\"325\\\" style=\\\"background-color:#176832;width:325px; border-top: 0px solid transparent; border-left: 0px solid transparent; border-bottom: 0px solid transparent; border-right: 0px solid transparent;\\\" valign=\\\"top\\\"><table width=\\\"100%\\\" cellpadding=\\\"0\\\" cellspacing=\\\"0\\\" border=\\\"0\\\"><tr><td style=\\\"padding-right: 5px; padding-left: 5px; padding-top:5px; padding-bottom:5px;\\\"><![endif]-->\\n                        <div class=\\\"col num6\\\" style=\\\"min-width: 320px; max-width: 325px; display: table-cell; vertical-align: top; width: 325px;\\\">\\n                            <div style=\\\"width:100% !important;\\\">\\n                                <!--[if (!mso)&(!IE)]><!-->\\n                                <div style=\\\"border-top:0px solid transparent; border-left:0px solid transparent; border-bottom:0px solid transparent; border-right:0px solid transparent; padding-top:5px; padding-bottom:5px; padding-right: 5px; padding-left: 5px;\\\">\\n                                    <!--<![endif]-->\\n                                    <!--[if mso]><table width=\\\"100%\\\" cellpadding=\\\"0\\\" cellspacing=\\\"0\\\" border=\\\"0\\\"><tr><td style=\\\"padding-right: 10px; padding-left: 10px; padding-top: 10px; padding-bottom: 10px; font-family: 'Trebuchet MS', Tahoma, sans-serif\\\"><![endif]-->\\n                                    <div style=\\\"color:#FFFFFF;font-family:'Trebuchet MS', 'Lucida Grande', 'Lucida Sans Unicode', 'Lucida Sans', Tahoma, sans-serif;line-height:1.8;padding-top:10px;padding-right:10px;padding-bottom:10px;padding-left:10px;\\\">\\n                                        <div style=\\\"font-family: 'Trebuchet MS', 'Lucida Grande', 'Lucida Sans Unicode', 'Lucida Sans', Tahoma, sans-serif; font-size: 12px; line-height: 1.8; color: #FFFFFF; mso-line-height-alt: 22px;\\\">\\n                                            <p style=\\\"font-size: 15px; line-height: 1.8; text-align: center; mso-line-height-alt: 27px; margin: 0;\\\"><span style=\\\"font-size: 15px;\\\"><strong><span style=\\\"font-size: 15px;\\\"><a href=\\\"https://www.crai-supermercati.it/\\\" target=\\\"_blank\\\" rel=\\\"noopener\\\" style=\\\"color: #FFFFFF;\\\">craiweb.it</a></span></strong></span></p>\\n                                        </div>\\n                                    </div>\\n                                    <!--[if mso]></td></tr></table><![endif]-->\\n                                    <!--[if (!mso)&(!IE)]><!-->\\n                                </div>\\n                                <!--<![endif]-->\\n                            </div>\\n                        </div>\\n                        <!--[if (mso)|(IE)]></td></tr></table><![endif]-->\\n                        <!--[if (mso)|(IE)]></td><td align=\\\"center\\\" width=\\\"325\\\" style=\\\"background-color:#176832;width:325px; border-top: 0px solid transparent; border-left: 0px solid transparent; border-bottom: 0px solid transparent; border-right: 0px solid transparent;\\\" valign=\\\"top\\\"><table width=\\\"100%\\\" cellpadding=\\\"0\\\" cellspacing=\\\"0\\\" border=\\\"0\\\"><tr><td style=\\\"padding-right: 0px; padding-left: 0px; padding-top:5px; padding-bottom:5px;\\\"><![endif]-->\\n                        <!--[if (mso)|(IE)]></td></tr></table><![endif]-->\\n                        <!--[if (mso)|(IE)]></td></tr></table></td></tr></table><![endif]-->\\n                    </div>\\n                </div>\\n            </div>\\n            <div style=\\\"background-color:transparent;\\\">\\n                <div class=\\\"block-grid \\\" style=\\\"Margin: 0 auto; min-width: 320px; max-width: 650px; overflow-wrap: break-word; word-wrap: break-word; word-break: break-word; background-color: #FFFFFF;\\\">\\n                    <div style=\\\"border-collapse: collapse;display: table;width: 100%;background-color:#FFFFFF;\\\">\\n                        <!--[if (mso)|(IE)]><table width=\\\"100%\\\" cellpadding=\\\"0\\\" cellspacing=\\\"0\\\" border=\\\"0\\\" style=\\\"background-color:transparent;\\\"><tr><td align=\\\"center\\\"><table cellpadding=\\\"0\\\" cellspacing=\\\"0\\\" border=\\\"0\\\" style=\\\"width:650px\\\"><tr class=\\\"layout-full-width\\\" style=\\\"background-color:#FFFFFF\\\"><![endif]-->\\n                        <!--[if (mso)|(IE)]><td align=\\\"center\\\" width=\\\"650\\\" style=\\\"background-color:#FFFFFF;width:650px; border-top: 0px solid transparent; border-left: 0px solid transparent; border-bottom: 0px solid transparent; border-right: 0px solid transparent;\\\" valign=\\\"top\\\"><table width=\\\"100%\\\" cellpadding=\\\"0\\\" cellspacing=\\\"0\\\" border=\\\"0\\\"><tr><td style=\\\"padding-right: 0px; padding-left: 0px; padding-top:5px; padding-bottom:5px;\\\"><![endif]-->\\n                        <div class=\\\"col num12\\\" style=\\\"min-width: 320px; max-width: 650px; display: table-cell; vertical-align: top; width: 650px;\\\">\\n                            <div style=\\\"width:100% !important;\\\">\\n                                <!--[if (!mso)&(!IE)]><!-->\\n                                <div style=\\\"border-top:0px solid transparent; border-left:0px solid transparent; border-bottom:0px solid transparent; border-right:0px solid transparent; padding-top:5px; padding-bottom:5px; padding-right: 0px; padding-left: 0px;\\\">\\n                                    <!--<![endif]-->\\n                                    <!--[if mso]><table width=\\\"100%\\\" cellpadding=\\\"0\\\" cellspacing=\\\"0\\\" border=\\\"0\\\"><tr><td style=\\\"padding-right: 25px; padding-left: 25px; padding-top: 0px; padding-bottom: 25px; font-family: 'Trebuchet MS', Tahoma, sans-serif\\\"><![endif]-->\\n                                    <div style=\\\"color:#555555;font-family:'Montserrat', 'Trebuchet MS', 'Lucida Grande', 'Lucida Sans Unicode', 'Lucida Sans', Tahoma, sans-serif;line-height:1.2;padding-top:0px;padding-right:25px;padding-bottom:25px;padding-left:25px;\\\">\\n                                        <div style=\\\"font-size: 12px; line-height: 1.2; font-family: 'Montserrat', 'Trebuchet MS', 'Lucida Grande', 'Lucida Sans Unicode', 'Lucida Sans', Tahoma, sans-serif; color: #555555; mso-line-height-alt: 14px;\\\">\\n                                            <p style=\\\"font-size: 12px; line-height: 1.2; text-align: center; mso-line-height-alt: 14px; margin: 0;\\\"><span style=\\\"font-size: 12px;\\\">© 2020 CRAI Secom S.p.A. P.IVA 12641600155</span><br><span style=\\\"font-size: 12px;\\\">Centro Direzionale Milano 2 - Strada di Olgia Vecchia - 20090 Segrate Milano</span></p>\\n                                        </div>\\n                                    </div>\\n                                    <!--[if mso]></td></tr></table><![endif]-->\\n                                    <!--[if (!mso)&(!IE)]><!-->\\n                                </div>\\n                                <!--<![endif]-->\\n                            </div>\\n                        </div>\\n                        <!--[if (mso)|(IE)]></td></tr></table><![endif]-->\\n                        <!--[if (mso)|(IE)]></td></tr></table></td></tr></table><![endif]-->\\n                    </div>\\n                </div>\\n            </div>\\n            <!--[if (mso)|(IE)]></td></tr></table><![endif]-->\\n        </td>\\n    </tr>\\n    </tbody>\\n</table>\\n<!--[if (IE)]></div><![endif]-->\\n</body>\\n\\n</html>\",\n" +
                    "  },\n" +
                    "  \"Priority\": \"Normal\"\n" +
                    "}";
            os.write(jsonEmail.getBytes(StandardCharsets.UTF_8));
            os.close();
            return super.EvaluateConnection(conn);
        }

        @Override
        protected Void EvaluateResponse(String msg) throws Exception {
            return null;
        }

        @Override
        protected Void onError(Exception ex) {
            return null;
        }
    }

    void emailCliente(int idCliente, ClienteInfo info, AsyncCallback<Void> onDone) {
        boolean senior = false;
        try {
            JSONObject parsed = new JSONObject(infoPV);
            if(parsed.has("haCartaSenior")) {
                senior = parsed.getBoolean("haCartaSenior");
            } else if(parsed.has("tipiTessere")) {
                JSONArray tipi =parsed.getJSONArray("tipiTessere");
                for(int i=0;i<tipi.length();i++)
                    if(tipi.optInt(i,0)==11) {
                        senior =true;
                        break;
                    }
            }
        } catch (Exception ex) {

        }
        TLFidelityWS.emailCliente emailCliente = new emailCliente();
        emailCliente.execute(onDone,senior? "android2" : "android", Integer.toString(idCliente), info.cognome+" "+info.nome, info.email);
    }

    static class getCedi extends MyCaller<String, String[]> {

        @Override
        protected String getPage(String... strings) {
            return "/api/Cedi";
        }

        @Override
        protected String[] EvaluateResponse(String response) throws Exception {
            JSONArray jsonarray = new JSONArray(response);
            String[] ret = new String[jsonarray.length()];
            for (int i = 0; i < jsonarray.length(); i++) {
                JSONObject jsonobject = jsonarray.getJSONObject(i);
                String cedi = jsonobject.getString("cedi");
                ret[i] = cedi;
            }


            //ret.FromWS(response);
            return ret;
        }

    }

    public void getCedi(AsyncCallback<String[]> onDone) {
        getCedi getCedi = new getCedi();
        getCedi.execute(onDone);
    }

    static class getPuntiVendita extends MyCaller<String, String[]> {

        @Override
        protected String getPage(String... strings) {
            return "/api/PuntiVendita?cedi="+strings[0];
        }

        @Override
        protected String[] EvaluateResponse(String response) throws Exception {
            JSONArray jsonarray = new JSONArray(response);
            String[] ret = new String[jsonarray.length()];
            for (int i = 0; i < jsonarray.length(); i++) {
                JSONObject jsonobject = jsonarray.getJSONObject(i);
                String pv = jsonobject.getString("PV");
                ret[i] = pv;
            }


            //ret.FromWS(response);
            return ret;
        }

    }

    public void getPuntiVendita(String cedi,AsyncCallback<String[]> onDone) {
        getPuntiVendita puntiVendita = new getPuntiVendita();
        puntiVendita.execute(onDone,cedi);
    }

    static class salvaOTP extends MyCaller<StoricizzazioneOTPInfo, Boolean> {
        boolean nuovo;
        String infoOTP;

        @Override
        protected String getPage(StoricizzazioneOTPInfo... otpInfos) {
            String extra = "";
            nuovo = otpInfos[0].id==-1;
            if(!nuovo) {
                extra = "/"+Integer.toString(otpInfos[0].id);
            }
            infoOTP = otpInfos[0].toWS();
            return "/api/PuntoVendita"+extra;
        }

        @Override
        protected Boolean EvaluateConnection(HttpURLConnection conn) throws Exception {
            conn.setDoOutput(true);
            if(nuovo)
                conn.setRequestMethod("POST");
            else
                conn.setRequestMethod("PUT");
            conn.setRequestProperty("Content-Type", "application/json; utf-8");
            conn.setRequestProperty("Accept", "application/json");
            OutputStream os = conn.getOutputStream();
            os.write(infoOTP.getBytes(StandardCharsets.UTF_8));
            os.close();
            return super.EvaluateConnection(conn);
        }

        @Override
        protected Boolean EvaluateResponse(String response) throws Exception {
            return true;
        }

        @Override
        protected Boolean onError(Exception ex) { return false;
        }
    }
    static salvaOTP salvaOTP;
    public void salvaOTP(StoricizzazioneOTPInfo info,AsyncCallback<Boolean> onDone) {
        salvaOTP salvaOTP = new salvaOTP();
        salvaOTP.execute(onDone,info);
    }

    static class emailOTP extends MyCaller<String, Void> {
        String cliente, email,codiceTessera,otp;
        @Override
        protected String getPage(String... strings) {
            String page = "/api/autorizzazione/emailOTP/";
            cliente = strings[0];
            email = strings[1];
            codiceTessera = strings[2];
            otp = strings[3];
            return page;
        }

        @Override
        protected Void EvaluateConnection(HttpURLConnection conn) throws Exception {
            conn.setDoOutput(true);
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json; utf-8");
            conn.setRequestProperty("Accept", "application/json");
            OutputStream os = conn.getOutputStream();
            String jsonEmail;
            jsonEmail = "{\n" +
                    "  \"From\": {\n" +
                    "    \"name\": \"Crai supermercati\",\n" +
                    "    \"address\": \"cartapiucrai@crai.org\"\n" +
                    "  },\n" +
                    "  \"To\": [{\n" +
                    "      \"name\": \""+cliente+"\",\n" +
                    "      \"address\": \""+email+"\"\n" +
                    "    }],\n" +
                    "  \"CC\": [],\n" +
                    "  \"Bcc\": [],\n" +
                    "  \"Subject\": \"OTP Carta Più "+codiceTessera+ "\",\n" +
                    "  \"Body\": {\n" +
                    "    \"html\": false,\n" +
                    "    \"text\": \"Gentile cliente, \\n  di seguito il codice OTP per confermare la registrazione a Carta Più CRAI: "+otp+ "\"\n" +
                    "  },\n" +
                    "  \"Priority\": \"Normal\"\n" +
                    "}";
            os.write(jsonEmail.getBytes(StandardCharsets.UTF_8));
            os.close();
            return super.EvaluateConnection(conn);
        }

        @Override
        protected Void EvaluateResponse(String msg) throws Exception {
            return null;
        }

        @Override
        protected Void onError(Exception ex) {
            return null;
        }
    }

    void emailOTP(String otp, ClienteInfo info, AsyncCallback<Void> onDone) {
        TLFidelityWS.emailOTP emailOTP = new emailOTP();
        emailOTP.execute(onDone,  info.cognome+" "+info.nome, info.email, info.codiceTessera, otp);
    }


    static class SendPos extends MyCaller<Object,Boolean> {

        @Override
        protected String getPage(Object... objects) {
            String page="/api/autorizzazione/GPS/";
            page += ((String)objects[0]).replaceAll(":","-");
            page += "?lat="+Float.toString((float) objects[1]);
            page += "&lon="+Float.toString((float) objects[2]);
            return page;
        }

        @Override
        protected Boolean EvaluateResponse(String response) throws Exception {
            return Boolean.parseBoolean(response);
        }
    }
    public void sendPos(String macAddr, float latitude, float longitude,AsyncCallback<Boolean> onDone) {
        SendPos sendPos = new SendPos();
        sendPos.execute(onDone,macAddr,latitude,longitude);
    }

    static class insertLog extends MyCaller<String,Integer> {

        private String mac;
        private String tipo;
        private String modulo;
        private String messaggio;
        private String eccezione;

        @Override
        protected String getPage(String... strings) {
            mac = strings[0].replaceAll(":","-");
            tipo = strings[1];
            modulo = strings[2];
            messaggio = strings[3];
            eccezione = strings[4];

            return "/api/app/log";
        }

        @Override
        protected Integer EvaluateConnection(HttpURLConnection conn) throws Exception {
            conn.setDoOutput(true);
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json; utf-8");
            conn.setRequestProperty("Accept", "application/json");
            OutputStream os = conn.getOutputStream();
            String jsonAutorizzazione="{\"mac\": \"" + mac + "\", " +
                    "\"tipo\": \"" + tipo + "\", " +
                    "\"modulo\": \"" + modulo + "\", " +
                    "\"messaggio\": \"" + messaggio + "\", " +
                    "\"eccezione\": \"" + eccezione  + "\"}";
            os.write(jsonAutorizzazione.getBytes(StandardCharsets.UTF_8));
            os.close();
            return super.EvaluateConnection(conn);
        }

        @Override
        protected Integer EvaluateResponse(String msg) throws Exception {
            return Integer.parseInt(msg);
        }

        @Override
        protected Integer onError(Exception ex) {
            return -1;
        }
    }

    public enum TipoLog {
        Info(0),
        Debug(1),
        Errore(5);

        private final int value;
        TipoLog(int value) {
            this.value = value;
        }

        int getValue() {
            return this.value;
        }

        String getName() {
            return this.name();
        }

        static TipoLog valueOf(int x) {
            for(TipoLog val : TipoLog.values()) {
                if(x==val.value)
                    return val;
            }
            return null;
        }
    }


    public void insertLog(String macAddress, Integer tipo, String modulo, String messaggio, String eccezione, AsyncCallback<Integer> onDone) {

        String tipoLabel = TipoLog.valueOf(tipo).getName();
        new insertLog().execute(onDone,macAddress,tipoLabel,modulo,messaggio,eccezione);
    }

    static class checkVersioneTabletAndUpdate extends MyCaller<String,Integer> {

        @Override
        protected String getPage(String... strings) {
            return "/api/app/verMac/"+strings[0].replaceAll(":","-")+"/"+strings[1];
        }

        @Override
        protected Integer EvaluateResponse(String msg) throws Exception {
            return Integer.parseInt(msg);
        }

        @Override
        protected Integer onError(Exception ex) {
            return 0;
        }

    }

    public void checkVersioneTabletAndUpdate(String macAddress, String versione, AsyncCallback<Integer> onDone) {
        new checkVersioneTabletAndUpdate().execute(onDone,macAddress,versione);
    }


    static class checkScarico extends MyCaller<String,Boolean> {

        @Override
        protected String getPage(String... strings) {
            return "/api/autorizzazione/scarico/"+strings[0].replaceAll(":","-")+"/";

        }

        @Override
        protected Boolean EvaluateConnection(HttpURLConnection conn) throws Exception {

            return super.EvaluateConnection(conn);
        }

        @Override
        protected Boolean EvaluateResponse(String msg) throws Exception {
            return Boolean.parseBoolean(msg);
        }

        @Override
        protected Boolean onError(Exception ex) {
            return false;
        }
    }

    void checkScarico(String macAddress, AsyncCallback<Boolean> onDone) {
        new checkScarico().execute(onDone,macAddress);
    }

    static class autorizzazioneScarico extends MyCaller<String,Boolean> {

        @Override
        protected String getPage(String... strings) {
            return "/api/autorizzazione/scarico/"+strings[0].replaceAll(":","-")+"/"+strings[1];

        }

        @Override
        protected Boolean EvaluateConnection(HttpURLConnection conn) throws Exception {

            return super.EvaluateConnection(conn);
        }

        @Override
        protected Boolean EvaluateResponse(String msg) throws Exception {
            return Boolean.parseBoolean(msg);
        }

        @Override
        protected Boolean onError(Exception ex) {
            return false;
        }
    }

    void autorizzazioneScarico(String macAddress, String versione, AsyncCallback<Boolean> onDone) {
        new autorizzazioneScarico().execute(onDone,macAddress, versione);
    }

}


