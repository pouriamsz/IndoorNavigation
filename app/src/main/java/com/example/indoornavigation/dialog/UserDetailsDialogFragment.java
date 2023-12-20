package com.example.indoornavigation.dialog;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.example.indoornavigation.R;

import com.example.indoornavigation.interfaces.OnUserUpdateListener;

public class UserDetailsDialogFragment extends DialogFragment {

    public static final String USER_TAG = "USER";
    public static final String STRIDE_LENGTH_TAG = "STRIDE_LENGTH_TAG";

    private OnUserUpdateListener onUserUpdateListener;
    private boolean addingUser;
    private String userName;

    public UserDetailsDialogFragment() {}

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {

        final Context context = getActivity();

        //create dialog view
        View dialogBox = View.inflate(context, R.layout.dialog_user_details, null);

        //set dialog view
        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(context);
        alertDialogBuilder.setView(dialogBox);

        //defining views
        final TextView textName = dialogBox.findViewById(R.id.nameEdt);
        final EditText textStride = dialogBox.findViewById(R.id.strideEdt);
        final EditText textMosaic = dialogBox.findViewById(R.id.mosaicEdt);

        //if not adding a new user, disable the name EditText
        if (!addingUser) {
            textName.setEnabled(false);
            textName.setText(userName);
        }

        alertDialogBuilder
                .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dismiss();
                    }
                })
                .setPositiveButton("Okay", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {

                        try {


                            String userName = textName.getText().toString();
                            double mosaicNumber = Double.parseDouble(textMosaic.getText().toString());
                            double stepNumber = Double.parseDouble(textStride.getText().toString());
                            double strideLength = mosaicNumber * 0.3 / stepNumber;

                            String strideLengthString = "" + strideLength;

                            Bundle bundle = new Bundle();

                            bundle.putString(UserDetailsDialogFragment.USER_TAG, userName);
                            bundle.putString(UserDetailsDialogFragment.STRIDE_LENGTH_TAG, strideLengthString);

                            onUserUpdateListener.onUserUpdateListener(bundle);

                            dismiss();
                        }catch (Exception ex){
                            Toast.makeText(getActivity(), "Empty inputs, fill and try again", Toast.LENGTH_SHORT).show();
                        }

                    }
                });
        return alertDialogBuilder.create();
    }

    public void setOnUserUpdateListener(OnUserUpdateListener onUserUpdateListener) {
        this.onUserUpdateListener = onUserUpdateListener;
    }

    public void setAddingUser(boolean addingUser) {
        this.addingUser = addingUser;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    private boolean checkInvalidStrideLength(String strideLength) {
        return strideLength.length() == 0;
    }

    private boolean checkInvalidUserName(String userName) {
        return userName.length() == 0;
    }

}
