package com.wart.magister;

import android.app.Activity;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.NavUtils;
import android.util.Log;
import android.view.MenuItem;

import com.wart.magister.util.Data;
import com.wart.magister.util.DataRow;
import com.wart.magister.util.DataTable;
import com.wart.magister.util.Global;
import com.wart.magister.util.MediusCall;
import com.wart.magister.util.Serializer;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map.Entry;

public class RegisterActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);
        // Show the Up button in the action bar.
        getActionBar().setDisplayHomeAsUpEnabled(true);
        new RegisterTask().execute();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            NavUtils.navigateUpFromSameTask(this);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    class RegisterTask extends AsyncTask<Void, String, Void> {

        private static final String TAG = "RegisterTask";
        private static final String ERROR = "Error";

        @Override
        protected void onPostExecute(Void voids) {
            Log.i(TAG, "Finished registering this device");
        }

        @Override
        protected void onProgressUpdate(String... strings) {
            // TODO Handle errors on the GUI
            if (strings[0].equals(ERROR)) Log.e(TAG, strings[1]);
            else Log.i(TAG, strings[0]);
        }

        @Override
        protected Void doInBackground(Void... params) {
            if (MediusCall.getLoggedInState() != 0x5) {
                Log.wtf(TAG, "Dude, you're not logged in...");
                return null;
            } else Log.i(TAG, "Confirmed that we are logged in :-)");

            publishProgress(Data.getString(Data.LICENSE));
            try {
                Global.Device.Version = RegisterActivity.this.getPackageManager().getPackageInfo(RegisterActivity.this.getPackageName(), 0).versionName;
                Global.Device.OSVersion = Build.VERSION.RELEASE;
                Global.Device.Model = Build.MODEL;
                Global.Device.HardwareID = Global.getHWID();
            } catch (NameNotFoundException e) {
                Log.e(TAG, "Error in gathering Device info", e);
                return null;
            }

            HashMap<String, String> tableNamesHash = new HashMap<String, String>();
            tableNamesHash.put("gebruiker", "Profiel");
            tableNamesHash.put("lestijden", "Schoolstructuur");
            tableNamesHash.put("persoon", "Personeel");
            tableNamesHash.put("leerling", "Leerlingen");
            tableNamesHash.put("agendaitem", "Agenda");
            DataTable settings = new DataTable("os", "hardwareid", "appname", "appversion", "suite", "rol");
            DataRow srow = settings.newRow();
            srow.put("os", String.format("Android %s(Model: %s)", Global.Device.OSVersion, Global.Device.Model));
            srow.put("appname", Data.getString(Data.APPNAME));
            srow.put("appversion", Global.Device.Version);
            srow.put("hardwareid", Global.Device.HardwareID);
            srow.put("suite", Data.getString(Data.MAGISTER_SUITE));
            srow.put("rol", Data.getString(Data.ROLE));
            settings.add(srow);
            MediusCall call = MediusCall.RegisterDevice(settings);
            if (call == null) publishProgress(ERROR, MediusCall.error);
            else {
                try {
                    publishProgress("Registering device...");
                    Serializer reader = new Serializer(call.response);
                    reader.readROBoolean();
                    if (reader.readByte() != 1) {
                        publishProgress(ERROR, "reader.readByte() was not 1! BufferLength was " + reader.getBufferLength() + ", reader.pos is " + reader.pos);
                        Global.bAuthenticate = false;
                        return null;
                    }

                    int datalen = reader.readInt32();
                    DataTable dt = reader.readDataTable();
                    if (Global.profiles != null && Global.profiles.size() > 0 && dt != null && dt.size() > 0) {
                        Iterator<HashMap<String, String>> iterator = Global.profiles.iterator();
                        while (iterator.hasNext()) {
                            HashMap<String, String> hash = iterator.next();
                            if (Global.toDBString(hash.get("code")).equals(Global.toDBString(dt.get(0x0).get("code"))) && Global.toDBString(hash.get("medius")).equals(Data.getString(Data.MEDIUSURL))) {
                                publishProgress(ERROR, "Dit profiel bestaat al. Deinstalleer de applicatie om je profiel opnieuw aan te maken met een nieuwe uitnodiging. Het personaliseren wordt nu afgebroken.");
                                return null;

                            }
                        }
                    }
                    int prevpos = 0;
                    while (dt != null && prevpos < reader.pos) {
                        prevpos = reader.pos;
                        String status = "";
                        if (!isCancelled()) {
                            if (dt.TableName.equalsIgnoreCase("gebruiker")) {
                                status = "Profiel";
                                DataRow row = dt.get(0);
                                Data.set(Data.USERNAME, Global.toDBString(row.get("loginnaam")));
                                Data.set(Data.USERID, Global.toDBInt(row.get("idgebr")));
                                Data.set(Data.IDTYPE, Global.toDBInt(row.get("idtype")));
                                Data.set(Data.FULLNAME, Global.toDBString(row.get("naam_vol")));
                                Data.set(Data.DEVICECODE, Global.toDBString(row.get("devicecode")));
                                Data.set(Data.EMPLOYEEID, Global.toDBInt(row.get("idpers")));
                                Data.set(Data.STUDENTID, Global.toDBInt(row.get("idleer")));
                                Data.set(Data.KEY, Data.getString(Data.DEVICECODE) + "|" + Global.getVersionFromPackageInfo() + "|" + Global.getMD5Hash());
                            } else if (tableNamesHash.containsKey(dt.TableName.toLowerCase(Locale.ENGLISH).trim()))
                                status = tableNamesHash.get(dt.TableName.toLowerCase().trim());
                            publishProgress(String.format("Downloading %s...", status));
                            // TODO: Fix this: database.AddTable(dt, true);
                            if (reader.pos < datalen) dt = reader.readDataTable();
                            else dt = null;
                        }
                    }
                    publishProgress("Sending personal device info");
                    call = MediusCall.updateDeviceInfo();
                    if (call != null) {
                        Global.setSharedValue("startup", 0x1);
                        reader = new Serializer(call.response);
                        reader.readROBoolean();
                        reader.SkipROBinary();
                        Global.toDBInt(reader.readVariant());
                        Global.toDBBool(reader.readVariant());
                        Global.toDBBool(reader.readVariant());
                        String magisterSuite = Global.toDBString(reader.readVariant());
                        Global.toDBString(reader.readVariant());
                        Global.toDBString(reader.readVariant());
                        DataTable rechtenTable = reader.readDataTable();
                        DataTable betaald = reader.readDataTable();
                        String foutStr = reader.getLastError();
                        if (!Global.isNullOrEmpty(foutStr)) publishProgress(ERROR, foutStr);

                        // TODO: Implement the rechtenTable DataTable
                        if (rechtenTable != null) for (DataRow row : rechtenTable)
                            for (Entry<String, Object> e : row.entrySet())
                                Log.v(TAG, e.getKey() + "=" + e.getValue());

                        // TODO: Implement the betaald DataTable
                        if (betaald != null) for (DataRow row : betaald)
                            for (Entry<String, Object> e : row.entrySet())
                                Log.v(TAG, e.getKey() + "=" + e.getValue());

                        if (magisterSuite.equalsIgnoreCase(Data.getString(Data.MAGISTER_SUITE)) || magisterSuite.equalsIgnoreCase("")) {
                            if (magisterSuite.equalsIgnoreCase("") && Data.getString(Data.MAGISTER_SUITE).equalsIgnoreCase("")) {
                                Data.set(Data.MAGISTER_SUITE, "5.3.7");
                                Data.set(Data.KEY, Data.getString(Data.DEVICECODE) + "|" + Global.getVersionFromPackageInfo());
                                Global.updateCurrentProfile();
                            }
                        } else {
                            Data.set(Data.MAGISTER_SUITE, magisterSuite);
                            Data.set(Data.KEY, Data.getString(Data.DEVICECODE) + "|" + Global.getVersionFromPackageInfo());
                            Global.updateCurrentProfile();
                        }
                    }
                    if (Global.isNullOrEmpty(Data.getString(Data.MAGISTER_SUITE)))
                        publishProgress(ERROR, "Er is een fout opgetreden tijdens het installeren. Onbekende Magistersuite versie.");

                } catch (Exception ex) {
                    publishProgress(ERROR, "Error tijdens personalisatie.");
                    Log.e(TAG, "Exception in RegisterActivity", ex);
                }
            }
            return null;
        }
    }
}
