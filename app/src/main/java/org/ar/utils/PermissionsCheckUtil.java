package org.ar.utils;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;

public class PermissionsCheckUtil {

    public static final int SETTING_APP = 0x123;
    private static String[] PHONE_MTYB = new String[]{"huawei", "sanxing", "xiaomi"};

    /**
     * @param activity activity
     * @param message  hint message
     */
    public static void showMissingPermissionDialog(final Activity activity, String message) {
        boolean canSetting = false;
        String mtyb = Build.BRAND;
        for (int i = 0; i < PHONE_MTYB.length; i++) {
            if (PHONE_MTYB[i].equalsIgnoreCase(mtyb)) {
                canSetting = true;
                break;
            } else {
                canSetting = false;
            }
        }
        final AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        builder.setTitle("Help");
        builder.setMessage(message);
        if (canSetting) {
            builder.setPositiveButton("Settings", new DialogInterface.OnClickListener() {

                @Override
                public void onClick(DialogInterface dialog, int which) {
                    startAppSettings(activity);
                    dialog.dismiss();
                }
            });
        }
        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });
        builder.show();
    }

    public static void startAppSettings(Activity activity) {
        Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
        intent.setData(Uri.parse("package:" + activity.getPackageName()));
        activity.startActivityForResult(intent, SETTING_APP);
    }
}