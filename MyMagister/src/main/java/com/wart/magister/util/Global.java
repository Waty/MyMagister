package com.wart.magister.util;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.provider.Settings;
import android.telephony.TelephonyManager;
import android.util.Log;

import com.loopj.android.http.AsyncHttpClient;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectOutputStream;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class Global {
    private static final String TAG = "Global";
    public static AsyncHttpClient AsyncHttpClient = new AsyncHttpClient();
    public static Context appContext;
    public static boolean doMediusCallToServer;
    public static String MD5Hash = "";
    public static Date NODATE;
    public static List<HashMap<String, String>> profiles;
    public static boolean bAuthenticate = false;
    private static Map<String, Object> sharedDictionary = new HashMap<String, Object>();

    public static String getMD5Hash() {
        if (isNullOrEmpty(Global.MD5Hash) && Global.appContext != null) {
            try {
                File file = new File(Global.appContext.getPackageCodePath());
                FileInputStream fis = new FileInputStream(file);
                byte[] array = new byte[(int) file.length()];
                int read;
                for (int i = 0; i < array.length; i += read) {
                    read = fis.read(array, i, array.length - i);
                    if (read < 0) break;
                }
                fis.close();
                MessageDigest digester = MessageDigest.getInstance("MD5");
                digester.update(array, 0, array.length);
                Global.MD5Hash = new BigInteger(1, digester.digest()).toString(16);
                Log.i(TAG, "MD5: " + Global.MD5Hash);

            } catch (Exception ex) {
                Log.e(TAG, "Exception in getMD5Hash", ex);
            }
        }
        return Global.MD5Hash;
    }

    public static SharedPreferences getSharedPreferences() {
        if (appContext == null) return null;
        return appContext.getSharedPreferences(Data.APPNAME, Context.MODE_PRIVATE);
    }

    // Fake the version like the latest version of Meta
    public static String getVersionFromPackageInfo() {
        return "1.0.21";
    }

    public static boolean isNullOrEmpty(String paramString) {
        return paramString == null || paramString.equals("") || paramString.equalsIgnoreCase("null") || paramString.length() <= 0;
    }

    public static boolean isOnline() {
        if (appContext == null) return false;
        ConnectivityManager cm = (ConnectivityManager) appContext.getSystemService(Context.CONNECTIVITY_SERVICE);
        return cm.getActiveNetworkInfo() != null && cm.getActiveNetworkInfo().isConnected();
    }

    public static DataTable[] processDataTableResponse(final Serializer serializer, int tableCount) {
        try {
            final ArrayList<DataTable> list = new ArrayList<DataTable>();
            for (int i = 0; serializer.pos < serializer.getBufferLength() && (tableCount == -1 || i < tableCount); i++)
                list.add(serializer.readDataTable());

            final DataTable[] array = new DataTable[list.size()];
            list.toArray(array);
            return array;
        } catch (Exception ex) {
            Log.e(TAG, "processDataTableResponse Error", ex);
        }
        return null;
    }

    public static void saveProfile() {
        try {
            File file = new File(appContext.getDir("profile", Context.MODE_PRIVATE), "map");
            ObjectOutputStream outputStream = new ObjectOutputStream(new FileOutputStream(file));
            outputStream.writeObject(profiles);
            outputStream.flush();
            outputStream.close();
        } catch (Exception e) {
            Log.e(TAG, "Error in SaveProfile", e);
        }
        return;
    }

    public static void setSharedPreferenceValues(final String key, final Object value) {
        if (Global.appContext != null) {
            final SharedPreferences getSharedPreferenceValue = getSharedPreferences();
            if (getSharedPreferenceValue != null) {
                final SharedPreferences.Editor edit = getSharedPreferenceValue.edit();
                if (value instanceof String) edit.putString(key, toDBString(value));
                else if (value instanceof Integer) edit.putInt(key, toDBInt(value));
                else if (value instanceof Boolean) edit.putBoolean(key, toDBBool(value));
                else if (value instanceof Date)
                    edit.putString(key, new SimpleDateFormat("dd MMM yyyy HH:mm:ss", Locale.ENGLISH).format((Date) value) + " GMT");

                if (edit.commit()) Log.v("Global", String.format("Succesfully saved %s", key));
                else Log.w("Global", String.format("Saving %s failed!", key));
            }
        }
    }

    public static void setSharedValue(final String s, final Object o) {
        Global.sharedDictionary.put(s.toLowerCase(Locale.ENGLISH), o);
    }

    public static boolean toDBBool(Object obj) {
        if (obj != null && obj != DBNull.Value) return Boolean.parseBoolean(String.valueOf(obj));
        return false;
    }

    public static int toDBInt(Object obj) {
        if (obj != null) {
            String value = String.valueOf(obj);
            if (value.length() > 0) return Integer.parseInt(value);
        }
        return 0;
    }

    public static String toDBString(Object obj) {
        if (obj != null) {
            if (obj.toString().contains("\r\n")) return obj.toString().replaceAll("\r\n", "\n");
            return obj.toString();
        }
        return "";
    }

    public static void updateCurrentProfile() {
        if (profiles != null) {
            int n = 0;
            int n2 = -1;
            for (int n3 = 0; n3 < Global.profiles.size() && n == 0; ++n3) {
                if (toDBString(Global.profiles.get(n3).get("naam")).equalsIgnoreCase(Data.getString(Data.FULLNAME))) {
                    n2 = n3;
                    n = 1;
                }
            }
            if (n2 > -1) {
                final HashMap<String, String> hashMap = Global.profiles.get(n2);
                hashMap.remove(Data.MAGISTER_SUITE);
                hashMap.put(Data.MAGISTER_SUITE, Data.getString(Data.MAGISTER_SUITE));
                saveProfile();
            }
        }
    }

    public static String getHWID() {
        String s;
        if (Device.HardwareID != null && Device.HardwareID.length() > 4) return Device.HardwareID;

        s = "";
        try {
            final TelephonyManager telephonyManager = (TelephonyManager) Global.appContext.getSystemService("phone");
            if (telephonyManager != null) {
                s = telephonyManager.getDeviceId();
            }
            if (s == null || s.length() < 5) {
                s = Settings.Secure.getString(Global.appContext.getContentResolver(), "android_id");
                if (s != null && s.equalsIgnoreCase("9774d56d682e549c")) s = null;
            }
            if (s == null || s.length() < 5) {
                final WifiInfo connectionInfo = ((WifiManager) Global.appContext.getSystemService("wifi")).getConnectionInfo();
                if (connectionInfo != null) {
                    s = connectionInfo.getMacAddress();
                    if (s != null && s.equalsIgnoreCase("-1")) s = null;
                }
            }
            if (s == null || s.length() < 5) {
                return "9774d56d682e549c_emulator";
            }
        } catch (Exception ex) {
            Log.e(TAG, "Exception in determineHardwareID: ", ex);
            return "unknown";
        }
        Log.i(TAG, "determined HWID=" + s);
        return s;
    }

    public static class Device {
        public static String HardwareID;
        public static String Model;
        public static String OSVersion;
        public static String Version;
    }

}