package com.wart.magister;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Intent;
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

import com.loopj.android.http.BinaryHttpResponseHandler;
import com.loopj.android.http.RequestHandle;
import com.loopj.android.http.TextHttpResponseHandler;
import com.wart.magister.util.Data;
import com.wart.magister.util.Global;
import com.wart.magister.util.MediusCall;
import com.wart.magister.util.Serializer;

import org.apache.http.Header;
import org.apache.http.entity.ByteArrayEntity;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import java.io.StringReader;
import java.util.ArrayList;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

public class SelectSchoolActivity extends Activity {
    private static final String TAG = "SelectSchoolActivity";
    private static ArrayList<School> mRetrievedSchools = new ArrayList<School>();
    private ListView mSchoolsList = null;
    private LinearLayout mSearchLayout;

    private RequestHandle mRetrieveSchoolsRequest = null;
    private RequestHandle mTestMediusRequest = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_select_school);
        // Display the Actionbar arrow
        getActionBar().setDisplayHomeAsUpEnabled(false);

        ((EditText) findViewById(R.id.select_school_edittext)).addTextChangedListener(new TextWatcher() {
            @Override
            public void afterTextChanged(Editable e) {
                if (e.length() > 0) {
                    showProgress(true);
                    if (mRetrieveSchoolsRequest != null && !mRetrieveSchoolsRequest.isFinished())
                        mRetrieveSchoolsRequest.cancel(true);
                    mRetrieveSchoolsRequest = Global.AsyncHttpClient.get("http://app.schoolmaster.nl/schoolLicentieService.asmx/Search?term=" + e.toString().replace(" ", "%20").toLowerCase().trim(), new TextHttpResponseHandler() {
                        @Override
                        public void onSuccess(int statusCode, Header[] headers, String responseBody) {
                            try {
                                SAXParserFactory factory = SAXParserFactory.newInstance();
                                SAXParser saxParser = factory.newSAXParser();

                                mRetrievedSchools.clear();
                                DefaultHandler handler = new DefaultHandler() {
                                    private String currentValue;
                                    private School currentSchool;

                                    @Override
                                    public void characters(char ch[], int start, int length) throws SAXException {
                                        currentValue = new String(ch, start, length);
                                    }

                                    @Override
                                    public void endElement(String uri, String localName, String qName) throws SAXException {
                                        if (qName.equalsIgnoreCase("medius"))
                                            currentSchool.URL = currentValue;
                                        else if (qName.equalsIgnoreCase("licentie"))
                                            currentSchool.License = currentValue;
                                    }

                                    @Override
                                    public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
                                        if (qName.equalsIgnoreCase("school")) {
                                            currentSchool = new School();
                                            mRetrievedSchools.add(currentSchool);
                                        }
                                    }
                                };
                                saxParser.parse(new InputSource(new StringReader(responseBody)), handler);

                                String[] names = new String[mRetrievedSchools.size()];
                                for (int i = 0; i < mRetrievedSchools.size(); i++)
                                    names[i] = mRetrievedSchools.get(i).License;

                                mSchoolsList.setAdapter(new ArrayAdapter<String>(SelectSchoolActivity.this, android.R.layout.simple_list_item_1, names));
                            } catch (Exception e) {
                                Log.e(TAG, "Error in onSuccess", e);
                            }
                        }

                        @Override
                        public void onFailure(String responseBody, Throwable error) {
                            mSchoolsList.setAdapter(null);
                            Log.e(TAG, "Retrieving schools failed", error);
                        }

                        @Override
                        public void onFinish() {
                            showProgress(false);
                        }
                    });
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
                if (mTestMediusRequest != null && !mTestMediusRequest.isFinished())
                    mTestMediusRequest.cancel(true);
                ((TextView) findViewById(R.id.select_school_status_message)).setText("Testing school...");
                showProgress(true);

                Data.set(Data.MEDIUSURL, Data.buildMediusUrl(mRetrievedSchools.get(position).URL));
                Log.v(TAG, "posting to " + Data.getString(Data.MEDIUSURL));
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

                Log.i(TAG, "Testing the url...");
                mTestMediusRequest = Global.AsyncHttpClient.post(SelectSchoolActivity.this, Data.getString(Data.MEDIUSURL), new ByteArrayEntity(request), "text/html", new BinaryHttpResponseHandler(new String[]{"text/html"}) {
                    @Override
                    public void onSuccess(byte[] binaryData) {

                        if (binaryData != null) {
                            Log.i(TAG, "Received " + binaryData.length + " bytes of data");
                            Serializer s = new Serializer(binaryData);
                            try {
                                if (s.readROHeader(null, "login", "getschoolname")) {
                                    Data.set(Data.LICENSE, s.readString());
                                    MediusCall.setLicense(Data.getString(Data.LICENSE));
                                } else Log.e(TAG, "Geblokkeerd door webserver");
                            } catch (Exception e) {
                                Log.e(TAG, "Error in TestMedius.onSuccess", e);
                            }
                            Log.i(TAG, "Succesfully tested the medius");
                            SelectSchoolActivity.this.startActivity(new Intent(SelectSchoolActivity.this, LoginActivity.class));
                        } else Log.e(TAG, "TestMedius timed out");
                    }

                    @Override
                    public void onFailure(int statusCode, Header[] headers, byte[] binaryData, Throwable error) {
                        Log.e(TAG, "Error in TestMedius.onFaillure", error);
                    }

                    @Override
                    public void onFinish() {
                        showProgress(false);
                    }
                });
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

    private class School {
        String License;
        String URL;
    }

}
