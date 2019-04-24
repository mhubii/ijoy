package com.example.max.ifp;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.support.design.widget.Snackbar;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.AlertDialog;
import android.view.View;
import android.widget.Toast;


 class Common {

    private Common(){ }

    static void showToast(final Context activity, String msg){
        Toast.makeText(activity, msg, Toast.LENGTH_LONG).show();
    }

    static void showSnackbar(final View activity, String msg){
        Snackbar.make(activity, "Replace with your own action", Snackbar.LENGTH_LONG)
                .setAction("Action", null)
                .setActionTextColor(Color.GREEN).show(); //TODO green not working
    }

    //get code from message
    static int parseMessage(String msg){
        switch (msg){
            case "(RobotStatus 0)": return 0;
            case "(RobotStatus 1)": return 1;
            case "(RobotStatus 2)": return 2;
            case "(RobotStatus 3)": return 3;
            case "(Warning 0)": return 4;
            case "(Warning 1)": return 5;
            case "(ERROR 0)": return 6;
            case "(ERROR 1)": return 7;
            case "(ERROR 2)": return 8;
            default: return -1;
        }
    }

    static String getMessageTextFromId(int id){
        String messages[] = {"Not Connected",
                            "Not Initialized",
                            "Initializing",
                            "Initialization Done",
                            "No Warning",
                            "Inverse kinematics did not converge",
                            "No Error",
                            "Quadratic problem infeasible",
                            "Hardware Limits"};
        return messages[id];
    }

    static void showAlertDialog(final Context activity, final String title, final String msg){
        try {
            AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(activity);

            // set title
            alertDialogBuilder.setTitle(title);



            // set dialog message
            alertDialogBuilder
                    .setMessage(msg)
                    .setCancelable(false)
                    .setNegativeButton("Dismiss",new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            // if this button is clicked, just close
                            // the dialog box and do nothing
                            dialog.cancel();
                        }
                    });

            if(!(activity instanceof ConnectActivity)){
                alertDialogBuilder.setPositiveButton("go to connect",new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog,int id) {
                        // if this button is clicked, close
                        // current activity
                        //activity.finish();
                        Intent m = new Intent(activity, JoystickActivity.class);
                        activity.startActivity(m);
                    }
                });
            }

            // create alert dialog
            AlertDialog alertDialog = alertDialogBuilder.create();

            // show it
            alertDialog.show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
