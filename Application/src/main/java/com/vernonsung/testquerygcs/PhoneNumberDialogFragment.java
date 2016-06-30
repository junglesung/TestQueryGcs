package com.vernonsung.testquerygcs;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

/**
 * A interactive dialog for users to input their phone numbers when creating an item in CreateItemFragment
 */
public class PhoneNumberDialogFragment extends DialogFragment {

    /**
     * The activity that creates an instance of this dialog fragment must
     * implement this interface in order to receive event callbacks.
     * Each method passes the DialogFragment in case the host needs to query it.
     */
    public interface PhoneNumberDialogListener {
        void onDialogPositiveClick(DialogFragment dialog);
        void onDialogNegativeClick(DialogFragment dialog);
    }

    private static final String LOG_TAG = "TestGood";

    // The activity host
    private Activity mActivityHost;
    // Use this instance of the interface to deliver action events
    private PhoneNumberDialogListener mListener;
    // UI
    private EditText mEditTextPhone;
    // The phone number user input
    private String phoneNumber;

    public String getPhoneNumber() {
        return phoneNumber;
    }

    // Override the Fragment.onAttach() method to instantiate the PhoneNumberDialogListener
    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        mActivityHost = activity;
        // Verify that the host activity implements the callback interface
        try {
            // Instantiate the PhoneNumberDialogListener so we can send events to the host
            mListener = (PhoneNumberDialogListener) activity;
        } catch (ClassCastException e) {
            // The activity doesn't implement the interface, throw exception
            throw new ClassCastException(activity.toString()
                    + " must implement PhoneNumberDialogListener");
        }
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        // Get phone number from shared preferences
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(mActivityHost);
        phoneNumber = sharedPreferences.getString(MyConstants.PHONE_NUMBER, "");
        if (phoneNumber.equals("")) {
            TelephonyManager tMgr = (TelephonyManager)mActivityHost.getSystemService(Context.TELEPHONY_SERVICE);
            phoneNumber = tMgr.getLine1Number();
        }

        // Use the Builder class for convenient dialog construction
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        // Get the layout inflater
        LayoutInflater inflater = getActivity().getLayoutInflater();
        // Pass null as the parent view because its going in the dialog layout
        View view = inflater.inflate(R.layout.phone_number_dialog, null);

        // Initial UI
        mEditTextPhone = (EditText) view.findViewById(R.id.editTextPhone);
        if (phoneNumber != null && !phoneNumber.isEmpty()) {
            mEditTextPhone.setText(phoneNumber);
            Log.d(LOG_TAG, "SIM1 " + phoneNumber + " detected");
        }
        mEditTextPhone.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                PhoneNumberDialogFragment.this.dismiss();
                okClicked();
                return true;
            }
        });

        // Inflate and set the layout for the dialog
        builder.setView(view)
                .setTitle(R.string.phone_number_for_others_to_contact)
                .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        okClicked();
                    }
                })
                .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        Log.d(LOG_TAG, "PhoneNumberDialog canceled");
                        mListener.onDialogNegativeClick(PhoneNumberDialogFragment.this);
                    }
                });

        // Create the AlertDialog object and return it
        return builder.create();
    }

    private void okClicked() {
        // Get user input phone number
        phoneNumber = mEditTextPhone.getText().toString();
        // Store to shared preference
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(mActivityHost);
        sharedPreferences.edit().putString(MyConstants.PHONE_NUMBER, phoneNumber).apply();

        Log.d(LOG_TAG, "PhoneNumberDialog returns phone number " + phoneNumber);
        mListener.onDialogPositiveClick(PhoneNumberDialogFragment.this);
    }
}
