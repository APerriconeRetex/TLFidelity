package com.retexspa.tecnologica.tlmoduloloyalty;

public class StoricizzazioneOTPInfo {
    public int id;
    public String mac;
    public int idCliente;
    public String cognome;
    public String nome;
    public String cellulare;
    public String tessera;
    public int idStato;
    public String sms;

    StoricizzazioneOTPInfo() {
        id = -1;
        //TODO: usare reflection
        mac = "";
        idCliente = menuPrincipaleActivity.getIdCliente();
        cognome = "";
        nome = "";
        cellulare = "";
        tessera = "";
        idStato = -1;
        sms = "";


    }

    String toWS() {
        //TODO: usare reflection
        return "{\"id\": " + id + ", " +
                "\"mac\": \"" + mac + "\", " +
                "\"idCliente\": " + idCliente + ", " +
                "\"cognome\": \"" + cognome + "\", " +
                "\"nome\": \"" + nome + "\", " +
                "\"cellulare\": \"" + cellulare + "\", " +
                "\"sms\": \"" + sms + "\", " +
                "\"idStato\": " + idStato + ", " +
                "\"tessera\": \"" + tessera + "\"}";
    }

}
