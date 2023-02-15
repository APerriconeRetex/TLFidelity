package com.retexspa.tecnologica.tlmoduloloyalty;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

@SuppressWarnings("WeakerAccess")
public class ClienteInfo {
    public int id;
    public String cognome;
    public String nome;
    public String cellulare;
    public String email;
    public String dataNascita;
    public String sesso;  //M o F diventano 1 o 2 (in teoria anche Ditta che diventa 3)
    public String cittaNascita;
    public String provNascita;
    public String codFiscale;
    public String indirizzo;
    public String civico;
    public String citta;
    public String cap;
    public String prov;
    public String auth1; //S o N
    public String auth2; //S o N
    public String auth3; //S o N
    public String dataFirma;
    public String dataFirma2;
    public String codiceTessera;
    public boolean cartaSenior;

    ClienteInfo() {
        id = menuPrincipaleActivity.getIdCliente();
        //TODO: usare reflection
        cognome = "";
        nome = "";
        cellulare = "";
        email = "";
        dataNascita = "";
        sesso = "";
        cittaNascita = "";
        provNascita = "";
        codFiscale = "";
        indirizzo = "";
        civico = "";
        citta = "";
        cap = "";
        prov = "";
        auth1 = "";
        auth2 = "";
        auth3 = "";
        dataFirma = "";
        dataFirma2 = "";
        codiceTessera = "";
        cartaSenior = false;
    }

    /**
     * Inizializza da un json arrivato dall'html
     * @param json il json arrivato
     * @throws JSONException in caso json non valido
     */
    void FromForm(String json) throws JSONException {
        id = menuPrincipaleActivity.getIdCliente();
        //TODO: usare reflection
        JSONArray parsed =new JSONArray(json);
        for (int i = 0; i < parsed.length(); i++) {
            JSONObject curr = parsed.getJSONObject(i);
            switch (curr.getString("name")) {
                case "cognome": cognome = curr.getString("value"); break;
                case "nome": nome = curr.getString("value"); break;
                case "cellulare": cellulare = curr.getString("value"); break;
                case "email": email = curr.getString("value"); break;
                case "dataNascita": dataNascita = curr.getString("value"); break;
                case "sesso": sesso = curr.getString("value"); break;
                case "cittaNascita": cittaNascita = curr.getString("value"); break;
                case "provNascita": provNascita = curr.getString("value"); break;
                case "codFiscale": codFiscale = curr.getString("value"); break;
                case "indirizzo": indirizzo = curr.getString("value"); break;
                case "civico": civico = curr.getString("value"); break;
                case "citta": citta = curr.getString("value"); break;
                case "cap": cap = curr.getString("value"); break;
                case "prov": prov = curr.getString("value"); break;
                case "auth1": auth1 = curr.getString("value"); break;
                case "auth2": auth2 = curr.getString("value"); break;
                case "auth3": auth3 = curr.getString("value"); break;
                case "dataFirma": dataFirma = curr.getString("value"); break;
                case "dataFirma2": dataFirma2 = curr.getString("value"); break;
            }
        }
        codiceTessera = menuPrincipaleActivity.getCodiceTessera();
        SimpleDateFormat formatoDataFirme = new SimpleDateFormat("yyyy-MM-dd", Locale.ITALY);
        if(dataNascita.length()>0)
            try {
                Date test = (new SimpleDateFormat("dd/MM/yyyy", Locale.ITALY)).parse(dataNascita);
                if(test!=null) dataNascita =formatoDataFirme.format(test);
            } catch (ParseException e) {
                dataNascita ="";
            }
        if(dataFirma.length()>0)
            try {
                formatoDataFirme.parse(dataFirma);
            } catch (ParseException e) {
                dataFirma ="";
            }
        if(dataFirma2.length()>0)
            try {
                formatoDataFirme.parse(dataFirma2);
            } catch (ParseException e) {
                dataFirma2="";
            }
        int pos = cittaNascita.indexOf(" (");
        if(pos>0) {
            try {
                provNascita = cittaNascita.substring(pos + 2, pos + 4);
                cittaNascita = cittaNascita.substring(0, pos);
            } catch (Exception ignored) { }
        }
    }

    /**
     * Inizializza da un json arrivato dal WebService
     * @param json il json arrivato
     * @throws JSONException se il json non è valido
     */
    void FromWS(String json) throws JSONException {
        //TODO: usare reflection
        JSONObject parsed = new JSONObject(json);
        id = parsed.getInt("id");
        cognome = parsed.getString("cognome");
        nome = parsed.getString("nome");
        cellulare = parsed.getString("cellulare");
        email = parsed.getString("email");
        dataNascita = parsed.getString("dataNascita");
        sesso = parsed.getInt("sesso")==1? "M" : "F";
        cittaNascita = parsed.getString("cittaNascita");
        provNascita = parsed.getString("provNascita");
        codFiscale = parsed.getString("codFiscale");
        indirizzo = parsed.getString("indirizzo");
        civico = parsed.getString("civico");
        citta = parsed.getString("citta");
        cap = parsed.getString("cap");
        prov = parsed.getString("provincia");
        dataFirma = parsed.getString("dataFirma");
        dataFirma2 = parsed.getString("dataFirma2");
        codiceTessera  = parsed.getString("codiceTessera");
        cartaSenior  = parsed.getBoolean("cartaSenior");
        JSONArray autorizzazioni = parsed.getJSONArray("autorizzazioni");
        for (int i = 0; i < autorizzazioni.length(); i++) {
            JSONObject auth = autorizzazioni.getJSONObject(i);
            int idAut = auth.getInt("id");
            boolean consenso = auth.getBoolean("consenso");
            switch(idAut) {
                case 7: auth1 =  consenso? "S" : "N"; break;
                case 8: auth2 =  consenso? "S" : "N"; break;
                case 9: auth3 =  consenso? "S" : "N"; break;
            }
        }

        //https://docs.oracle.com/javase/10/docs/api/java/text/SimpleDateFormat.html
        // esempio: 2020-03-16T08:10:10.0139359+01:00
        // esempio2: 2020-03-16T08:10:10
        //noinspection SpellCheckingInspection
        SimpleDateFormat formatoDataNET = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSSSSSXXX", Locale.ITALY);
        SimpleDateFormat formatoDataNET2 = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.ITALY);
        SimpleDateFormat formatoDataFirme = new SimpleDateFormat("yyyy-MM-dd", Locale.ITALY);
        if(dataNascita.length()>0)
            try {
                Date test;
                if(dataNascita.length()>19)
                    test = formatoDataNET.parse(dataNascita);
                else
                    test = formatoDataNET2.parse(dataNascita);
                if(test!=null)
                    dataNascita = formatoDataFirme.format(test);
            } catch (ParseException e) {
                dataNascita ="";
            }
        if(dataFirma.length()>0)
            try {
                Date test;
                if(dataNascita.length()>19)
                    test = formatoDataNET.parse(dataFirma);
                else
                    test = formatoDataNET2.parse(dataFirma);
                if(test!=null)
                    dataFirma = formatoDataFirme.format(test);
            } catch (ParseException e) {
                dataFirma ="";
            }
        if(dataFirma2.length()>0)
            try {
                Date test;
                if(dataNascita.length()>19)
                    test = formatoDataNET.parse(dataFirma2);
                else
                    test = formatoDataNET2.parse(dataFirma2);
                if(test!=null)
                    dataFirma2 = formatoDataFirme.format(test);
            } catch (ParseException e) {
                dataFirma2="";
            }
    }

    /**
     * Converte i dati in JSON per essere inviati a TLFidelityWS
     * @return il json creato
     */
    String toWS() {
        //TODO: usare reflection
        return "{\"id\": " + id + ", " +
                "\"cognome\": \"" + cognome + "\", " +
                "\"nome\": \"" + nome + "\", " +
                "\"cellulare\": \"" + cellulare + "\", " +
                "\"email\": \"" + email + "\", " +
                "\"dataNascita\": \"" + dataNascita + "\", " +
                "\"sesso\": \"" + (sesso.equals("M") ? 1 : 2) + "\", " +
                "\"cittaNascita\": \"" + cittaNascita + "\", " +
                "\"provNascita\": \"" + provNascita + "\", " +
                "\"codFiscale\": \"" + codFiscale + "\", " +
                "\"indirizzo\": \"" + indirizzo.replace("\\","/") + "\", " +
                "\"civico\": \"" + civico.replace("\\","/") + "\", " +
                "\"citta\": \"" + citta + "\", " +
                "\"cap\": \"" + cap + "\", " +
                "\"provincia\": \"" + prov + "\", " +
                "\"autorizzazioni\": [" +
                "{\"id\": 7, \"consenso\": " + (auth1.equals("S") ? "true" : "false") + " }, " +
                "{\"id\": 8, \"consenso\": " + (auth2.equals("S") ? "true" : "false") + " }, " +
                "{\"id\": 9, \"consenso\": " + (auth3.equals("S") ? "true" : "false") + " }, " +
                "{\"id\": 10, \"consenso\": " + "false" + " }], " +
                "\"dataFirma\": \"" + dataFirma + "\", " +
                "\"dataFirma2\": \"" + dataFirma2 + "\", " +
                "\"codiceTessera\": \"" + codiceTessera + "\"}";
    }

    public String tojQuery() {
        StringBuilder dest =new StringBuilder();
        dest.append("{");
        //TODO: usare reflection
        dest.append("\"cognome\": \"").append(cognome).append("\", ");
        dest.append("\"nome\": \"").append(nome).append("\", ");
        if (cellulare.startsWith("00")) {
            dest.append("\"cellulare\": \"").append(cellulare).append("\", ");
        } else if (cellulare.startsWith("+")) {
            dest.append("\"cellulare\": \"").append("00").append(cellulare.substring(1)).append("\", ");
        } else {
            dest.append("\"cellulare\": \"").append("0039").append(cellulare).append("\", ");
        }

        dest.append("\"email\": \"").append(email).append("\", ");
        if(dataNascita.length()==10) {
            dest.append("\"dataNascita\": \"").append(dataNascita.substring(8, 10)).append("/")
                    .append(dataNascita.substring(5, 7)).append("/").append(dataNascita.substring(0, 4)).append("\", ");
        }
        dest.append("\"sesso\": \"").append(sesso).append("\", ");
        /*
        dest.append("\"cittaNascita\": \"").append(cittaNascita).append("\", ");
        dest.append("\"provNascita\": \"").append(provNascita).append("\", ");*/
        dest.append("\"cittaNascita\": \"").append(cittaNascita).append(" (").append(provNascita).append(")\", ");;
        dest.append("\"codFiscale\": \"").append(codFiscale).append("\", ");
        dest.append("\"indirizzo\": \"").append(indirizzo).append("\", ");
        dest.append("\"civico\": \"").append(civico).append("\", ");
        dest.append("\"citta\": \"").append(citta).append("\", ");
        dest.append("\"cap\": \"").append(cap).append("\", ");
        dest.append("\"prov\": \"").append(prov).append("\", ");
        dest.append("\"auth1\": \"").append(auth1).append("\", ");
        dest.append("\"auth2\": \"").append(auth2).append("\", ");
        dest.append("\"auth3\": \"").append(auth3).append("\", ");
        dest.append("\"dataFirma\": \"").append(dataFirma).append("\", ");
        dest.append("\"dataFirma2\": \"").append(dataFirma2).append("\"} ");
        return dest.toString();
    }
}
