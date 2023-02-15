package com.retexspa.tecnologica.tlmoduloloyalty;

class UtilsParole {
    /**
     * Tutte le vocali.
     */
    @SuppressWarnings("SpellCheckingInspection")
    private static final String VOCALI = "AEIOU";

    /**
     * Calcola il numero delle consonanti presenti nella stringa.
     * @param string la stringa sulla quale calcolare le consonanti.
     * @return Restituisce il numero delle consonanti.
     */
    static int getNumeroConsonanti(String string){
        int consonanti = 0;
        for(int i = 0; i < string.length(); i++){
            if(!isVocale(string.charAt(i))){
                consonanti++;
            }
        }
        return consonanti;
    }


    /**
     * Restituisce le prime numero consonanti presenti nella stringa.
     * @param string la stringa sulla quale prelevare le consonanti.
     * @param numero il numero di consonanti da prelevare.
     * @return Restituisce le prime numero consonanti.
     */
    static String getPrimeConsonanti(String string, int numero){
        StringBuilder consonanti = new StringBuilder();
        for(int i = 0; i < string.length(); i++){
            if(!isVocale(string.charAt(i))){
                if(consonanti.length() < numero){
                    consonanti.append(string.charAt(i));
                }
            }
        }
        return consonanti.toString();
    }

    /**
     * Restituisce la i-esima consonante.
     * @param string la stringa sulla quale cercare.
     * @param i l'indice della consonante.
     * @return Restituisce la i-esima consonante di string.
     */
    static String getConsonanteI(String string, int i){
        int contatoreConsonanti = 0;
        for(int j = 0; j < string.length(); j++){
            if(!isVocale(string.charAt(j))){
                contatoreConsonanti++;
                if(contatoreConsonanti == i){
                    return Character.toString(string.charAt(j));
                }
            }
        }
        return null;
    }



    /**
     * Restituisce le prime numero vocali presenti nella stringa.
     * @param string la stringa sulla quale prelevare le vocali.
     * @param numero il numero di vocali da prelevare.
     * @return Restituisce le prime numero vocali.
     */
    static String getPrimeVocali(String string, int numero){
        StringBuilder vocali = new StringBuilder();
        for(int i = 0; i < string.length(); i++){
            if(isVocale(string.charAt(i))){
                if(vocali.length() < numero){
                    vocali.append(string.charAt(i));
                }
            }
        }
        return vocali.toString();
    }

    /**
     * Restituisce una stringa di n X.
     * @param n il numero di X.
     * @return Restituisce la stringa con n X.
     */
    static String nXChar(int n){
        StringBuilder risultato = new StringBuilder();
        for(int i = 0; i < n; i++){
            risultato.append("X");
        }
        return risultato.toString();
    }


    /**
     * Elimina tutti gli spazi bianchi presenti nella stringa.
     * @param string la stringa sulla quale eliminare gli spazi bianchi.
     * @return Restituisce la stringa senza gli spazi bianchi.
     * @implNote http://stackoverflow.com/a/5455809
     */
    static String eliminaSpaziBianchi(String string){
        return string.replaceAll("\\s+","");
    }


    /**
     * Controlla se un carattere è una vocale.
     * @param character il carattere.
     * @return Restituisce true se il carattere è una vocale e false in caso contrario.
     */
    private static boolean isVocale(char character){
        return VOCALI.contains(Character.toString(character));
    }

    /**
     * Restituisce la stringa con tutti i caratteri di posizione pari di string.
     * @param string la stringa dal quale calcolare la stringa con tutti i caratteri di posizione pari.
     * @return Restituisce la stringa con tutti i caratteri di posizione pari di string.
     */
    static String getStringaPari(String string){
        StringBuilder risultato = new StringBuilder();
        for(int i = 0; i < string.length(); i++){
            if((i+1) % 2 == 0){
                risultato.append(string.charAt(i));
            }
        }
        return risultato.toString();
    }


    /**
     * Restituisce la stringa con tutti i caratteri di posizione dispari di string.
     * @param string la stringa dal quale calcolare la stringa con tutti i caratteri di posizione dispari.
     * @return Restituisce la stringa con tutti i caratteri di posizione dispari di string.
     */
    static String getStringaDispari(String string){
        StringBuilder risultato = new StringBuilder();
        for(int i = 0; i < string.length(); i++){
            if((i+1) % 2 == 1){
                risultato.append(string.charAt(i));
            }
        }
        return risultato.toString();
    }
}
