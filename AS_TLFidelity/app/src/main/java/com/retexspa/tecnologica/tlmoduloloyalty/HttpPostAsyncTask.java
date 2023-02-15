package com.retexspa.tecnologica.tlmoduloloyalty;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.AsyncTask;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Map;

public class HttpPostAsyncTask extends AsyncTask<String, Void, Void>  {
    // This is the JSON body of the post
    JSONObject postData;// This is a constructor that allows you to pass in the JSON body

    ArrayList<Object> result = new ArrayList<Object>();
    private Context context;


    public HttpPostAsyncTask(Map<String, String> postData, Context context) {
        this.context = context;
        if (postData != null) {
            this.postData = new JSONObject(postData);
        }
    }

    // This is a function that we are overriding from AsyncTask. It takes Strings as parameters because that is what we defined for the parameters of our async task
    @Override
    protected Void doInBackground(String... params) {

        try {
            // This is getting the url from the string we passed in
            URL url = new URL(params[0]);
            String otp = params[1];
            result.add(otp);
            // Create the urlConnection
            HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();


            urlConnection.setDoInput(true);
            urlConnection.setDoOutput(true);

            urlConnection.setRequestProperty("Content-Type", "application/json");

            urlConnection.setRequestMethod("POST");


            // OPTIONAL - Sets an authorization header
            urlConnection.setRequestProperty("Authorization", "92156a34f732898f59ae1dd7c36f9fb37db77caf");

            // Send the post body
            if (this.postData != null) {
                OutputStreamWriter writer = new OutputStreamWriter(urlConnection.getOutputStream());
                writer.write(postData.toString());
                writer.flush();
            }

            int statusCode = urlConnection.getResponseCode();
            InputStream inputStream;
            String response;

            switch (statusCode) {

                case 200:
                    // Your code to build your JSON object
                    inputStream = new BufferedInputStream(urlConnection.getInputStream());
                    response = convertInputStreamToString(inputStream);
                    result.add("Ok");
                    result.add(response);
                    break;
                case 400:
                    //inputStream = new BufferedInputStream(urlConnection.getErrorStream());
                    //response = convertInputStreamToString(inputStream);
                    result.add("Durante l'invio del messaggio contenente il codice di verifica è stato riscontrato il seguente errore '400 - Bad request: rivedere i dati inseriti'.");
                    result.add(null);
                    break;
                case 401:
                    //inputStream = new BufferedInputStream(urlConnection.getErrorStream());
                    //response = convertInputStreamToString(inputStream);
                    result.add("Durante l'invio del messaggio contenente il codice di verifica è stato riscontrato il seguente errore '401 - Unauthorized request: contattare il call center'.");
                    result.add(null);
                    break;
            }



        } catch (Exception e) {
            e.printStackTrace();
            Log.d("TAG",e.getLocalizedMessage());
        }
        return null;
    }

    @Override
    protected void onPostExecute(Void r) {
        String message = (String) result.get(1);
        if (message=="Ok") {
            String otp = (String) result.get(0);
            LayoutInflater inflater = LayoutInflater.from(context);
            AlertDialog.Builder builder = new AlertDialog.Builder(context);
            View view = inflater.inflate(R.layout.otp_popup, null);
            AlertDialog dialog = builder.setView(R.layout.otp_popup).create();
                    // Add action buttons
            builder.setPositiveButton("OK", new DialogInterface.OnClickListener()
                    {
                        @Override public void onClick(DialogInterface dialog, int id)
                        {
                            TextView otpTxt = (TextView) view.findViewById(R.id.otpText);
                            String otp2 = (String)otpTxt.getText().toString();
                            if (otp == otp2) {
                                // gli otp sono corretti possiamo proseguire
                                //salvataggio dati memorizzando anche data ora dei consensi inseriti ed il punto vendita
                                AlertDialog.Builder builderMsg = new AlertDialog.Builder(context);
                                builderMsg.setTitle("TLCustomerApp")
                                        .setMessage("I dati sono stati salvati correttamente. Leggi il codice a barre della tessera.")
                                        .setPositiveButton("OK", null)
                                        .show().setCanceledOnTouchOutside(false);
                            } else {
                                AlertDialog.Builder builderMsg = new AlertDialog.Builder(context);
                                builderMsg.setTitle("Errore")
                                        .setMessage("Il codice di verifica inserito non è corretto")
                                        .setPositiveButton("OK", null)
                                        .show().setCanceledOnTouchOutside(false);
                            }
                        }
                    });
            builder.setNegativeButton("Annulla", null);
            dialog.show();

        } else {
           //visualizzo un messaggio di errore'
            AlertDialog.Builder builderMsg = new AlertDialog.Builder(context);
            builderMsg.setTitle("Errore")
                    .setMessage(message)
                    .setPositiveButton("OK", null)
                    .show().setCanceledOnTouchOutside(false);
        }
    }



    private static String convertInputStreamToString(InputStream inputStream)
            throws Exception {
        BufferedReader bufferedReader = new BufferedReader(

                new InputStreamReader(inputStream));
        String line = "";
        String result = "";
        while ((line = bufferedReader.readLine()) != null) {
            result += line;
        }

        inputStream.close();
        return result;

    }
}
