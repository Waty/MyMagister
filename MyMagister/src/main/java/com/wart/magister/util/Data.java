package com.wart.magister.util;

import android.content.Context;
import android.net.Uri;

public class Data {
    public static final String APPFOLDER = "appfolder";
    public static final String APPLICATIONNAME = "Meta 2011-2012";
    public static final String APPNAME = "Meta";
    public static final String DEVICECODE = "devicecode";
    public static final String EMPLOYEEID = "employeeid";
    public static final String FULLNAME = "fullname";
    public static final String IDTYPE = "idtype";
    public static final String KEY = "key";
    public static final String LICENSE = "license";
    public static final String MAGISTER_SUITE = "magistersuite";
    public static final String MEDIUS_VERSION = "mediusversion";
    public static final String MEDIUSFORWARDER = "/mwp/mobile/meta.medius.axd";
    public static final String MEDIUSURL = "medius";
    public static final String ROLE = "role";
    public static final String SCHOOLID = "schoolid";
    public static final String STUDENTID = "studendid";
    public static final String USERID = "userid";
    public static final String USERNAME = "username";

    public static String buildMediusUrl(final String url) {
        final String formattedUrl = formatMediusUrl(url);
        if (formattedUrl.length() > 1 && !formattedUrl.endsWith(MEDIUSFORWARDER))
            return String.valueOf(formattedUrl) + MEDIUSFORWARDER;
        return formattedUrl;
    }

    public static void clear() {
        Global.getSharedPreferences().edit().clear().commit();
    }

    public static String formatMediusUrl(String medius) {
        if (medius == null || medius.length() < 1) return "";

        if (!medius.startsWith("http")) medius = "https://" + medius;
        else if (medius.startsWith("http://")) medius = medius.replaceFirst("http", "https");

        final Uri url = Uri.parse(medius);
        final String authority = url.getAuthority();
        if (authority != null) {
            String scheme = url.getScheme();
            if (!scheme.equalsIgnoreCase("https")) scheme = "https";
            return String.format("%s://%s", scheme, authority);
        }
        return "";
    }

    public static int getInt(String key) {
        return Global.getSharedPreferences().getInt(key, -1);
    }

    public static String getString(String key) {
        return Global.getSharedPreferences().getString(key, "");
    }

    public static void initializeApp(Context context) {
        Data.set(Data.APPFOLDER, context.getApplicationInfo().dataDir);
    }

    public static void set(String key, Object value) {
        Global.setSharedPreferenceValues(key, value);
    }

}
