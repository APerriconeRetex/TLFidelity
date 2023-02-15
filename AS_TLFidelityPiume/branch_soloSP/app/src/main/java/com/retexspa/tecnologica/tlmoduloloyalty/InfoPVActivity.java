package com.retexspa.tecnologica.tlmoduloloyalty;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.EditText;
import android.widget.ListAdapter;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

public class InfoPVActivity extends AppCompatActivity {


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_info_p_v);
        TLFidelityWS web = new TLFidelityWS(this);
        web.getCedi(this::cedi);
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
            Intent setupIntent = new Intent(InfoPVActivity.this, WebServiceSetupActivity.class);
            startActivity(setupIntent);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void cedi(String[] strings) {
        AutoCompleteTextView tvPV =  (AutoCompleteTextView)findViewById(R.id.tvPV);
        ArrayAdapter<String> aCedi = new ArrayAdapter<String>(this,android.R.layout.select_dialog_item,strings);
        AutoCompleteTextView tvCedi =  (AutoCompleteTextView)findViewById(R.id.tvCedi);
        tvCedi.setThreshold(1);
        tvCedi.setAdapter(aCedi);
        tvCedi.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View view, boolean b) {
                if(!b) {
                    // on focus off
                    String str = tvCedi.getText().toString();

                    ListAdapter listAdapter = tvCedi.getAdapter();
                    for(int i = 0; i < listAdapter.getCount(); i++) {
                        String temp = listAdapter.getItem(i).toString();
                        if(str.compareTo(temp) == 0) {
                            return;
                        }
                    }

                    tvCedi.setText("");

                }
            }
        });

        tvCedi.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Object item = parent.getItemAtPosition(position);
                String cedi = ((String) item).split("-")[0].trim();
                tvPV.setEnabled(true);
                TLFidelityWS web = new TLFidelityWS(getApplicationContext());
                web.getPuntiVendita(cedi,this::puntiVendita);
            }

            private void puntiVendita(String[] strings) {

                ArrayAdapter<String> aPV = new ArrayAdapter<String> (getApplicationContext(),android.R.layout.select_dialog_item,strings);
                AutoCompleteTextView tvPV =  (AutoCompleteTextView)findViewById(R.id.tvPV);
                tvPV.setThreshold(1);//will start working from first character
                tvPV.setAdapter(aPV);//setting the adapter data into the AutoCompleteTextView

                tvPV.setOnFocusChangeListener(new View.OnFocusChangeListener() {
                    @Override
                    public void onFocusChange(View view, boolean b) {
                        if(!b) {
                            // on focus off
                            String str = tvPV.getText().toString();

                            ListAdapter listAdapter = tvPV.getAdapter();
                            for(int i = 0; i < listAdapter.getCount(); i++) {
                                String temp = listAdapter.getItem(i).toString();
                                if(str.compareTo(temp) == 0) {
                                    return;
                                }
                            }

                            tvPV.setText("");

                        }
                    }
                });
            }
        });
    }

    public void OnAnnullaClick(View view) {
        moveTaskToBack(true);
        System.exit(1);
    }

    public void OnInviaClick(View view) {
        AutoCompleteTextView tvCedi =  (AutoCompleteTextView)findViewById(R.id.tvCedi);
        AutoCompleteTextView tvPV =  (AutoCompleteTextView)findViewById(R.id.tvPV);
        EditText email = (EditText) findViewById(R.id.txtEmail);
        EditText tel = (EditText) findViewById(R.id.txtTel);
        EditText psw = (EditText) findViewById(R.id.txtPsw);
        if (validaModulo(tvCedi,tvPV,email,tel)) {
            String codiceCedi = tvCedi.getText().toString().split("-")[0].trim();
            String descrCedi = tvCedi.getText().toString().split("-")[1].trim();
            String codicePV = tvPV.getText().toString().split("-")[0].trim();
            String descrPV = tvPV.getText().toString().split("-")[1].trim();
            String mail = email.getText().toString().trim();
            String telefono = tel.getText().toString().trim();
            String password = psw.getText().toString().trim();

            Bundle b = getIntent().getExtras();
            if (b != null) {
                String address = b.getString("macAddress");
                TLFidelityWS web = new TLFidelityWS(view.getContext());
                web.richiediAutorizzazione(address, codiceCedi, descrCedi, codicePV,descrPV, mail,telefono, password, (Integer done)->{
                    String msg;
                    if(done==-1) {
                        msg = getString(R.string.richiediInviata);
                        msg = msg+"\nRiprovare più tardi.";
                    } else if (done==-2) {
                        //è già stato assegnato il pv
                        msg = "Il punto vendita è stato autorizzato. Riavviare l'applicazione.";
                    }
                    else {
                        msg = getString(R.string.richiediErrore);
                        msg = msg+"\nRiprovare più tardi.";
                    }
                    AlertDialog.Builder builder2 = new AlertDialog.Builder(view.getContext());
                    builder2.setMessage(msg)
                            .setPositiveButton("OK", (DialogInterface dialog, int which) -> {
                                    moveTaskToBack(true);
                                    System.exit(1);
                            })
                            .show().setCanceledOnTouchOutside(false);
                });
            }
        }


    }

    private boolean validaModulo(AutoCompleteTextView tvCedi, AutoCompleteTextView tvPV, EditText email, EditText tel) {
        boolean isvalid = true;
        String msg = "Per poter proseguire con l'invio della richiesta inserire:\n";

        if (tvCedi.getText().toString().equals("")) {
            isvalid = false;
            msg = msg + "- cedi\n";
        }


        if (tvPV.getText().toString().equals("")) {
            isvalid = false;
            msg = msg + "- punto vendita\n";
        }


        String getEmail = email.getText().toString();
        if (getEmail.equals("")) {
            isvalid = false;
            msg = msg + "- email\n";
        } else if (!isEmailValid(getEmail)){
            isvalid = false;
            msg = msg + "- indirizzo email valido\n";
        }


        if (tel.getText().toString().equals("")) {
            isvalid = false;
            msg = msg + "- telefono\n";
        }

        if (!isvalid) {
            AlertDialog.Builder builder2 = new AlertDialog.Builder(this);
            builder2.setMessage(msg)
                    .setPositiveButton(R.string.ok, null)
                    .show().setCanceledOnTouchOutside(false);
        }


        return isvalid;
    }

    boolean isEmailValid(CharSequence email) {
        return android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches();
    }



    @Override
    public void onBackPressed() {
    }


}
