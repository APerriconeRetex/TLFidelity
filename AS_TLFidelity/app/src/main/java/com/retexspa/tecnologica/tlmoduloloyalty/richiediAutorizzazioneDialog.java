package com.retexspa.tecnologica.tlmoduloloyalty;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

public class richiediAutorizzazioneDialog extends DialogFragment {

    private String address;
    private menuPrincipaleActivity activity;

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setView(R.layout.richiedi_autorizzazione_dialog);
        Dialog ret = builder.create();
        ret.setCanceledOnTouchOutside(false);
        return ret;
    }

    @Override
    public void onStart() {
        super.onStart();
        Dialog view = getDialog();
        if (view != null) {
            Button btnAnnulla = view.findViewById(R.id.annulla);
            btnAnnulla.setOnClickListener(this::OnAnnullaClick);
            final Button btnInvia = view.findViewById(R.id.invia);
            btnInvia.setOnClickListener(this::OnInviaClick);
            final TextView msg = view.findViewById(R.id.inviaMsg);
            final EditText edtMsg = view.findViewById(R.id.messaggio);
            edtMsg.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) { }

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) { }

                @Override
                public void afterTextChanged(Editable s) {
                    if (s.length() >= 10 || s.equals("tecno")) {
                        btnInvia.setEnabled(true);
                        msg.setVisibility(View.INVISIBLE);
                    } else {
                        btnInvia.setEnabled(false);
                        msg.setVisibility(View.VISIBLE);
                    }
                }
            });
        }

    }

    private void OnAnnullaClick(View view) {
        System.exit(1);
    }

    private void OnInviaClick(View view) {

    }

    void setAddress(String address) {
        this.address = address;
    }

    void setActivity(menuPrincipaleActivity activity) {
        this.activity = activity;
    }
}
