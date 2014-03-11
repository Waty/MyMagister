package com.wart.magister;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.annotation.TargetApi;
import android.app.Activity;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.NavUtils;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import java.net.URL;
import java.util.ArrayList;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

/*import com.wart.magister.Data;
import com.wart.magister.MediusCall;
import com.wart.magister.R;
import com.wart.magister.Serializer;*/

public class SelectSchoolActivity extends Activity {
    private static final String TAG = "SelectSchoolActivity";

    private ArrayList<School> mRetrievedSchools = null;
    private RetrieveSchoolsTask mRetrieveTask = null;
    //private TestMediusTask mTestMediusTask = null;

    private EditText mSearchBox = null;
    private ListView mSchoolsList = null;

    private LinearLayout mSearchLayout;

    private class School {
        String License;
        String URL;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_select_school);
        // Display the Actionbar arrow
        getActionBar().setDisplayHomeAsUpEnabled(true);

        mSearchBox = (EditText) findViewById(R.id.select_school_edittext);
        mSearchBox.addTextChangedListener(new TextWatcher() {
            @Override
            public void afterTextChanged(Editable e) {
                if (e.length() > 0) {
                    if (mRetrieveTask != null) mRetrieveTask.cancel(true);
                    ((TextView) findViewById(R.id.select_school_status_message)).setText("Retrieving schools...");
                    showProgress(true);
                    (mRetrieveTask = new RetrieveSchoolsTask()).execute(e.toString());
                } else {
                    if (mRetrieveTask != null) mRetrieveTask.cancel(true);
                    ((ListView) findViewById(R.id.select_school_listview)).setAdapter(null);
                }
            }

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }
        });

        mSchoolsList = (ListView) findViewById(R.id.select_school_listview);
        mSchoolsList.setOnItemClickListener(new OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Log.i(TAG, "Testing some school...");
              /*  if (mTestMediusTask != null) mTestMediusTask.cancel(true);
                ((TextView) findViewById(R.id.select_school_status_message)).setText("Testing school...");
                showProgress(true);
                (mTestMediusTask = new TestMediusTask()).execute(mRetrievedSchools.get(position));*/
            }
        });

        mSearchLayout = (LinearLayout) findViewById(R.id.select_school_status_layout);
    }

    /**
     * Shows the progress UI and hides the login form.
     */
    @TargetApi(Build.VERSION_CODES.HONEYCOMB_MR2)
    private void showProgress(final boolean show) {
        // On Honeycomb MR2 we have the ViewPropertyAnimator APIs, which allow
        // for very easy animations. If available, use these APIs to fade-in
        // the progress spinner.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR2) {
            int shortAnimTime = getResources().getInteger(android.R.integer.config_shortAnimTime);

            mSearchLayout.setVisibility(View.VISIBLE);
            mSearchLayout.animate().setDuration(shortAnimTime).alpha(show ? 1 : 0).setListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    mSearchLayout.setVisibility(show ? View.VISIBLE : View.GONE);
                }
            });

            mSchoolsList.setVisibility(View.VISIBLE);
            mSchoolsList.animate().setDuration(shortAnimTime).alpha(show ? 0 : 1).setListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    mSchoolsList.setVisibility(show ? View.GONE : View.VISIBLE);
                }
            });
        } else {
            // The ViewPropertyAnimator APIs are not available, so simply show
            // and hide the relevant UI components.
            mSearchLayout.setVisibility(show ? View.VISIBLE : View.GONE);
            mSchoolsList.setVisibility(show ? View.GONE : View.VISIBLE);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            NavUtils.navigateUpFromSameTask(this);
            return true;
        } else return super.onOptionsItemSelected(item);
    }

    private class RetrieveSchoolsTask extends AsyncTask<String, Void, ArrayList<School>> {

        private final ArrayList<School> MySchools = new ArrayList<School>();

        @Override
        protected ArrayList<School> doInBackground(String... arg0) {
            try {
                SAXParserFactory factory = SAXParserFactory.newInstance();
                SAXParser saxParser = factory.newSAXParser();

                DefaultHandler handler = new DefaultHandler() {
                    private String currentValue;
                    private School currentSchool;

                    @Override
                    public void characters(char ch[], int start, int length) throws SAXException {
                        currentValue = new String(ch, start, length);
                    }

                    @Override
                    public void endElement(String uri, String localName, String qName) throws SAXException {
                        if (qName.equalsIgnoreCase("medius")) currentSchool.URL = currentValue;
                        else if (qName.equalsIgnoreCase("licentie"))
                            currentSchool.License = currentValue;
                    }

                    @Override
                    public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
                        if (qName.equalsIgnoreCase("school")) {
                            currentSchool = new School();
                            MySchools.add(currentSchool);
                        }
                    }
                };

                URL localURL = new URL("http://app.schoolmaster.nl/schoolLicentieService.asmx/Search?term=" + arg0[0].replace(" ", "%20").toLowerCase().trim());
                saxParser.parse(localURL.openConnection().getInputStream(), handler);
            } catch (Exception e) {
                e.printStackTrace();
            }
            return MySchools;
        }

        @Override
        protected void onPostExecute(ArrayList<School> result) {
            String[] names = new String[result.size()];
            for (int i = 0; i < result.size(); i++)
                names[i] = result.get(i).License;

            ArrayAdapter<String> adapter = new ArrayAdapter<String>(SelectSchoolActivity.this, android.R.layout.simple_list_item_1, names);
            mSchoolsList.setAdapter(adapter);

            mRetrievedSchools = result;
            showProgress(false);
        }
    }

    /*private class TestMediusTask extends AsyncTask<School, Void, Boolean> {

        @Override
        protected Boolean doInBackground(School... params) {
            Data.set(Data.MEDIUSURL, Data.buildMediusUrl(params[0].URL));
            HttpPost post = new HttpPost(Data.getString(Data.MEDIUSURL));
            Log.v(TAG, "posting to " + post.getURI());
            byte[] request = new byte[54];
            request[0] = 0x52;
            request[1] = 0x4f;
            request[2] = 49;
            request[3] = 48;
            request[4] = 55;
            request[12] = 95;
            request[13] = -38;
            request[14] = -109;
            request[15] = 13;
            request[16] = -14;
            request[17] = 92;
            request[18] = 7;
            request[19] = 86;
            request[20] = -77;
            request[21] = -23;
            request[22] = 9;
            request[23] = 22;
            request[24] = 71;
            request[25] = -53;
            request[26] = -81;
            request[27] = 45;
            request[28] = 5;
            request[32] = 76;
            request[33] = 111;
            request[34] = 103;
            request[35] = 105;
            request[36] = 110;
            request[37] = 13;
            request[41] = 71;
            request[42] = 101;
            request[43] = 116;
            request[44] = 83;
            request[45] = 99;
            request[46] = 104;
            request[47] = 111;
            request[48] = 111;
            request[49] = 108;
            request[50] = 78;
            request[51] = 97;
            request[52] = 109;
            request[53] = 101;
            post.setEntity(new ByteArrayEntity(request));

            try { // Try to connect to the MediusUrl
                HttpEntity rawResponse = new DefaultHttpClient().execute(post).getEntity();
                if (rawResponse != null) {
                    BufferedInputStream bis = new BufferedInputStream(rawResponse.getContent(), (int) rawResponse.getContentLength());
                    ByteArrayOutputStream content = new java.io.ByteArrayOutputStream();

                    byte[] readbuffer = new byte[512];
                    for (int readBytes = 0; readBytes != -1; readBytes = bis.read(readbuffer))
                        content.write(readbuffer, 0, readBytes);

                    Serializer serializer = new Serializer(content.toByteArray());
                    if (serializer.readROHeader(null, "login", "getschoolname")) {
                        Data.set(Data.LICENSE, serializer.readString());
                        MediusCall.setLicense(Data.getString(Data.LICENSE));
                        return true;
                    } else Log.e(TAG, "Geblokkeerd door webserver");
                } else Log.e(TAG, "TestMedius timed out");

            } catch (Exception ex) {
                Log.e(TAG, "Error occured in TestMedius", ex);
            }
            return false;
        }

        @Override
        protected void onPostExecute(Boolean result) {
            if (result) {
                showProgress(false);
                SelectSchoolActivity.this.startActivity(new Intent(SelectSchoolActivity.this, LoginActivity.class));
            }
        }
    }*/
}
