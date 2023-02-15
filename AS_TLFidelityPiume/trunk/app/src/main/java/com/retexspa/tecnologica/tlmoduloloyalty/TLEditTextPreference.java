package com.retexspa.tecnologica.tlmoduloloyalty;

import android.content.Context;
import android.util.AttributeSet;

import androidx.preference.EditTextPreference;

//https://enzam.wordpress.com/2013/09/29/android-preference-show-current-value-in-summary/
public class TLEditTextPreference extends EditTextPreference {
    public TLEditTextPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    public CharSequence getSummary() {
        return this.getText();

    }
}
