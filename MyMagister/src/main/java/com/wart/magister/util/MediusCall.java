package com.wart.magister.util;

import android.net.Uri;
import android.util.Log;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.cookie.BasicClientCookie;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.protocol.BasicHttpContext;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.SocketTimeoutException;
import java.util.UUID;

public class MediusCall {
    public static final int HTTP_CONNECT_TIMEOUT = 5000;
    public static final int HTTP_TIMEOUT = 30000;
    private static final String EncTable = "QWERTYUIOPASDFGHJKLZXCVBNMabcdefghijklmnopqrstuvwxyz0123456789";
    private static final String TAG = "MediusCall";
    public static long CacheMinimumFilesize = 0L;
    public static boolean IsOffline;
    public static boolean Offline = false;
    public static boolean UseCaching;
    public static String cacheKey;
    public static UUID clientGUID = UUID.randomUUID();
    public static byte[] clientID = new byte[0];
    public static TeaEncryption crypto;
    public static String error;
    public static String foutmelding;
    static Serializer reader = new Serializer();
    private static HttpClient authClient;
    private static ByteArrayEntity authrequest;
    private static HttpClient httpClient;
    private static BasicHttpContext httpContext;
    private static BasicCookieStore httpCookies;
    private static byte[] readbuffer = new byte[512];
    public Object DataObject = null;
    public String MethodeName = "";
    public String Name = "MediusCall";
    public String ROInterface;
    public String ROMethod;
    public byte[] request;
    public byte[] response;
    protected Serializer writer = new Serializer(4096);

    public MediusCall() {
        Name = "MediusCall";
        DataObject = null;
        MethodeName = "";
        writer = new Serializer(4096);
        if (MediusCall.clientID.length < 1) {
            MediusCall.clientID = toByteArray(MediusCall.clientGUID);
        }
    }

    private static HttpClient getHttpClient() {
        if (MediusCall.httpClient == null) {
            final BasicHttpParams basicHttpParams = new BasicHttpParams();
            HttpConnectionParams.setConnectionTimeout(basicHttpParams, 5000);
            HttpConnectionParams.setSoTimeout(basicHttpParams, 30000);
            HttpConnectionParams.setStaleCheckingEnabled(basicHttpParams, false);
            HttpConnectionParams.setLinger(basicHttpParams, 30);
            HttpConnectionParams.setTcpNoDelay(basicHttpParams, true);
            MediusCall.httpClient = new DefaultHttpClient(basicHttpParams);
            final BasicHttpParams basicHttpParams2 = new BasicHttpParams();
            HttpConnectionParams.setConnectionTimeout(basicHttpParams2, 10000);
            HttpConnectionParams.setSoTimeout(basicHttpParams2, 5000);
            HttpConnectionParams.setStaleCheckingEnabled(basicHttpParams2, false);
            HttpConnectionParams.setLinger(basicHttpParams2, 10);
            HttpConnectionParams.setTcpNoDelay(basicHttpParams2, true);
            MediusCall.authClient = new DefaultHttpClient(basicHttpParams2);
            MediusCall.httpContext = new BasicHttpContext();
            MediusCall.httpCookies = new BasicCookieStore();
            final BasicClientCookie basicClientCookie = new BasicClientCookie("M5.Client.ID", MediusCall.clientGUID.toString());
            basicClientCookie.setDomain(Uri.parse(Data.getString(Data.MEDIUSURL)).getHost());
            basicClientCookie.setPath("/");
            MediusCall.httpCookies.addCookie(basicClientCookie);
            MediusCall.httpContext.setAttribute("http.cookie-store", MediusCall.httpCookies);
        }
        return MediusCall.httpClient;
    }

    public static void setLicense(String license) {
        if (Global.isNullOrEmpty(license)) crypto = null;
        else {
            crypto = new TeaEncryption(license);
            Log.i(TAG, "Set crypto passfrase to " + license);
        }
    }

    public static String makeSimpleKey(UUID p11) {
        String v1 = "";
        int v3 = 1;
        while (v3 <= (int) (Math.random() * 10.0) + 5) {
            v1 = new StringBuilder(String.valueOf(v1)).append(EncTable.charAt((int) (Math.random() * EncTable.length()))).toString();
            v3 = v3 + 1;
        }
        return getSimpleKey(p11, v1) + v1;
    }

    public static String getSimpleKey(UUID var0, String var1) {
        byte[] var2 = toByteArray(var0);
        char[] var3 = new char[136 + EncTable.length()];
        System.arraycopy(EncTable.toCharArray(), 0, var3, 0, EncTable.length());
        int var4 = 0;

        while (var4 < var1.length()) {
            char var5 = var1.charAt(var4);
            int var6 = 0;

            while (true) {
                if (var6 < var3.length) {
                    if (var3[var6] != var5) {
                        var6++;
                        continue;
                    }

                    char var7 = var3[0];
                    var3[0] = var5;
                    var3[var6] = var7;
                }
                ++var4;
                break;
            }
        }

        char[] var8 = new char[var2.length];
        int var9 = EncTable.length();

        for (int var10 = 0; var10 < var2.length; ++var10) {
            var8[var10] = var3[(3 * (255 & var2[var10]) + var1.length()) % var9];
            System.arraycopy(var8, 0, var3, var9, var10 + 1);
            var9 += var10 + 1;
        }

        return new String(var8);
    }

    static byte[] toByteArray(UUID paramUUID) {
        long l1 = paramUUID.getMostSignificantBits();
        long l2 = paramUUID.getLeastSignificantBits();
        byte[] arrayOfByte = new byte[16];
        arrayOfByte[0] = (byte) (int) (l1 >>> 32);
        arrayOfByte[1] = (byte) (int) (l1 >>> 40);
        arrayOfByte[2] = (byte) (int) (l1 >>> 48);
        arrayOfByte[3] = (byte) (int) (l1 >>> 56);
        arrayOfByte[4] = (byte) (int) (l1 >>> 16);
        arrayOfByte[5] = (byte) (int) (l1 >>> 24);
        arrayOfByte[6] = (byte) (int) l1;
        arrayOfByte[7] = (byte) (int) (l1 >>> 8);
        arrayOfByte[8] = (byte) (int) (l2 >>> 48);
        arrayOfByte[9] = (byte) (int) (l2 >>> 56);
        arrayOfByte[10] = (byte) (int) l2;
        arrayOfByte[11] = (byte) (int) (l2 >>> 8);
        arrayOfByte[12] = (byte) (int) (l2 >>> 16);
        arrayOfByte[13] = (byte) (int) (l2 >>> 24);
        arrayOfByte[14] = (byte) (int) (l2 >>> 32);
        arrayOfByte[15] = (byte) (int) (l2 >>> 40);
        return arrayOfByte;
    }

    static boolean containsAndreError(byte[] p4) {
        if (p4 != null && p4.length >= 10) {

            if (p4[0] != 60 || (p4[1] & 95) != 66 || p4[2] != 62) {
                return false;
            }
        } else {
            return false;
        }
        return true;

    }

    static boolean containsHTML(byte[] paramArrayOfByte) {
        if (paramArrayOfByte != null && paramArrayOfByte.length >= 10) {
            int arrayLength = -6 + paramArrayOfByte.length;
            if (arrayLength > 200) {
                arrayLength = 194;
            }

            for (int i = 0; i < arrayLength; ++i) {
                if (paramArrayOfByte[i] == 60 && (95 & paramArrayOfByte[i + 1]) == 72 && (95 & paramArrayOfByte[i + 2]) == 84 && (95 & paramArrayOfByte[i + 3]) == 77 && (95 & paramArrayOfByte[i + 4]) == 76) {
                    return true;
                }
            }
        }
        return false;
    }

    static boolean containsROMessage(byte[] paramArrayOfByte) {
        if (paramArrayOfByte != null && paramArrayOfByte.length >= 10) {
            if (paramArrayOfByte[0] != 82 || paramArrayOfByte[1] != 79 || paramArrayOfByte[2] != 49 || paramArrayOfByte[3] != 48) {
                return false;
            } else {
                return false;
            }
        }
        return true;
    }

    public static boolean ChallengeLogin(final String s) {
        final MediusCall caller = getCaller("Login", "challengelogin", false);
        caller.writer.writeString(s);
        caller.response = null;
        if (caller.MakeTheCall(MediusCall.clientID, caller.ROInterface, caller.ROMethod, caller.writer.getBuffer(), caller.writer.pos)) {
            try {
                return new Serializer(caller.response).readROBoolean();
            } catch (IOException ex) {
                ex.printStackTrace();
                return false;
            }
        }
        return false;
    }

    private static MediusCall getCaller(String roInterface, String roMethod) {
        return getCaller(roInterface, roMethod, true);
    }

    private static MediusCall getCaller(String roInterface, String roMethod, boolean withCredentials) {
        final MediusCall mediusCall = new MediusCall();
        mediusCall.ROInterface = roInterface;
        mediusCall.ROMethod = roMethod;
        mediusCall.writer.writeROHeader(MediusCall.clientID, roInterface, roMethod);
        if (withCredentials) {
            mediusCall.writer.writeInteger(Data.getInt(Data.USERID));
            mediusCall.writer.writeString(Data.getString(Data.ROLE));
            mediusCall.writer.writeString(Data.getString(Data.KEY));
        }
        return mediusCall;
    }

    public static boolean Login(final String name, final String password) {
        final MediusCall caller = getCaller("Login", "M5Login", false);
        caller.writer.writeString(name);
        caller.writer.writeString(password);
        caller.writer.writeString("");
        caller.writer.writeString("");
        caller.writer.writeInteger(0);
        caller.response = null;
        if (caller.MakeTheCall(MediusCall.clientID, caller.ROInterface, caller.ROMethod, caller.writer.getBuffer(), caller.writer.pos)) {
            try {
                return new Serializer(caller.response).readROBoolean();
            } catch (IOException ex) {
                Log.e(TAG, "Login", ex);
                return false;
            }
        }
        return false;
    }

    // Server API's
    public static int getLoggedInState() {
        final MediusCall caller = getCaller("Login", "loggedinstate", false);
        caller.response = null;

        if (caller.MakeTheCall(MediusCall.clientID, caller.ROInterface, caller.ROMethod, caller.writer.getBuffer(), caller.writer.pos)) {
            try {
                return new Serializer(caller.response).readInteger();
            } catch (IOException ex) {
                Log.e(TAG, "LoggedInState", ex);
                return 0;
            }
        }
        return 0;
    }

    public static String getResultStr() {
        final MediusCall caller = getCaller("Login", "resultstr", false);
        caller.response = null;
        if (caller.MakeTheCall(MediusCall.clientID, caller.ROInterface, caller.ROMethod, caller.writer.getBuffer(), caller.writer.pos)) {
            try {
                return new Serializer(caller.response).readString();
            } catch (IOException ex) {
                Log.e(TAG, "ResultStr", ex);
            }
        }
        return "Er is een fout opgetreden.";
    }

    public static String getSchoolName() {
        MediusCall mediusCall = new MediusCall();
        mediusCall.writer.writeROHeader(clientID, "Login", "GetSchoolName");
        mediusCall.ROInterface = "Login";
        mediusCall.ROMethod = "GetSchoolName";
        mediusCall.response = null;
        mediusCall.MakeTheCall(clientID, "Login", "GetSchoolName", mediusCall.writer.getBuffer(), mediusCall.writer.pos);

        if (mediusCall.response != null) {
            mediusCall.writer.setBuffer(mediusCall.response);
            try {
                return mediusCall.writer.readString();
            } catch (IOException e) {
                Log.e(TAG, "getSchoolName", e);
            }
        }
        return null;
    }

    public static String getMediusVersion() {
        MediusCall mediusCall = new MediusCall();
        mediusCall.writer.writeROHeader(clientID, "Global", "GetAppVersionForDB");
        mediusCall.writer.writeString("");
        mediusCall.writer.writeString("");
        mediusCall.MakeTheCall(clientID, "Global", "GetAppVersionForDB", mediusCall.writer.getBuffer(), mediusCall.writer.pos);
        if (mediusCall.response != null) {
            mediusCall.writer.setBuffer(mediusCall.response);
            try {
                return mediusCall.writer.readString();
            } catch (IOException ex) {
                Log.e(TAG, "Error in getMediusVersion", ex);
            }
        }
        return null;
    }

    public static MediusCall RegisterDevice(final DataTable dataTable) {
        final MediusCall caller = getCaller("MaestroLogin", "RegisterDevice", false);
        caller.writer.writeROBinaryWithObjects(dataTable);
        caller.response = null;
        if (caller.MakeTheCall(MediusCall.clientID, caller.ROInterface, caller.ROMethod, caller.writer.getBuffer(), caller.writer.pos))
            return caller;
        return null;
    }

    public static MediusCall updateDeviceInfo() {
        final MediusCall caller = getCaller("MaestroLogin", "MaestroCall", false);
        caller.writer.writeInteger(Data.getInt(Data.USERID));
        caller.writer.writeString(Data.getString(Data.ROLE));
        caller.writer.writeString(Data.getString(Data.KEY));
        caller.writer.writeString("UpdateDeviceInfo");
        caller.writer.writeByte((byte) 1);
        caller.writer.writeInt32(0);
        final int pos = caller.writer.pos;
        caller.writer.writeVariant(2);
        caller.writer.writeVariant(Integer.parseInt(Data.getString(Data.DEVICECODE)));
        caller.writer.writeVariant(String.format("Android %s(Model: %s)", Global.Device.OSVersion, Global.Device.Model));
        caller.writer.writeVariant("1.0.21");
        caller.writer.writePlaceholderWithSize(pos);
        caller.response = null;
        if (caller.MakeTheCall(MediusCall.clientID, caller.ROInterface, caller.ROMethod, caller.writer.getBuffer(), caller.writer.pos))
            return caller;
        return null;
    }

    public static DataTable[] synchViaDD(final String s, final Object... array) {
        final MediusCall mediusCall = getCaller("MaestroLogin", "MaestroCall");
        mediusCall.writer.writeString("SynchViaDD");
        mediusCall.writer.writeByte((byte) 1);
        mediusCall.writer.writeInt32(0);
        final int pos = mediusCall.writer.pos;
        mediusCall.writer.writeVariant(s);
        for (Object o : array)
            mediusCall.writer.writeVariant(o);

        mediusCall.writer.writePlaceholderWithSize(pos);
        mediusCall.response = null;
        if (mediusCall.MakeTheCall(MediusCall.clientID, mediusCall.ROInterface, mediusCall.ROMethod, mediusCall.writer.getBuffer(), mediusCall.writer.pos)) {
            try {
                final Serializer serializer = new Serializer(mediusCall.response);
                if (serializer.readROBoolean()) {
                    serializer.SkipROBinary();
                    return Global.processDataTableResponse(serializer, -1);
                }
            } catch (IOException ex) {
                Log.e(TAG, "synchViaDD Error" + ex);
            }
        }
        return null;
    }

    private void AuthenticateInitialize() {
        Log.v(TAG, "AuthenticateInitialize()");
        getHttpClient();
        final UUID randomUUID = UUID.randomUUID();
        MediusCall.clientID = new byte[0];

        final MediusCall mediusCall = new MediusCall();
        mediusCall.writer.writeROHeader(MediusCall.clientID, "Global", "Authenticate");
        if (Global.isNullOrEmpty(Data.getString(Data.USERNAME)))
            mediusCall.writer.writeString(Data.getString(Data.APPNAME));
        else
            mediusCall.writer.writeString(String.format("%s:%s", Data.getString(Data.USERNAME), Data.getString(Data.APPNAME)));
        mediusCall.writer.writeString(randomUUID.toString());
        mediusCall.writer.writeString("");
        mediusCall.writer.writeString(makeSimpleKey(MediusCall.clientGUID));
        byte[] encryptData = new byte[mediusCall.writer.pos];
        System.arraycopy(mediusCall.writer.getBuffer(), 0, encryptData, 0, encryptData.length);
        if (MediusCall.crypto != null) encryptData = MediusCall.crypto.encryptData(encryptData);
        MediusCall.authrequest = new ByteArrayEntity(encryptData);
    }

    public boolean MakeTheCall(byte[] clientID, String roInterface, String roMethod, byte[] request) {
        return MakeTheCall(clientID, roInterface, roMethod, request, request.length);
    }

    public boolean MakeTheCall(byte[] clientID, String roInterface, String roMethod, byte[] request, int size) {
        boolean connected = false;
        ByteArrayOutputStream content = null;
        boolean didServerCall = false;
        boolean datablobCanBeSaved = false;
        error = null;
        byte[] cache = getDataBlob(cacheKey, true);
        if (Global.doMediusCallToServer || !UseCaching || cache == null) {
            didServerCall = true;
            HttpClient httpClient = getHttpClient();
            if (Authenticate()) {
                ByteArrayEntity payload = null;
                HttpEntity rawResponse = null;

                System.arraycopy(clientID, 0, request, 12, clientID.length);
                if (size != request.length) {
                    byte[] r = new byte[size];
                    System.arraycopy(request, 0, r, 0, size);

                }
                if (crypto != null) request = crypto.encryptData(request);
                payload = new ByteArrayEntity(request);

                int retries = 5;
                int idx = 0;
                Offline = true;
                while (idx < retries) { // Try it 5 times
                    Log.v(TAG, "MakeTheCall to " + roInterface + "." + roMethod + " attempt #" + idx);
                    HttpPost post = new HttpPost(Data.getString(Data.MEDIUSURL));
                    post.setEntity(payload);
                    try {
                        HttpResponse httpResponse = httpClient.execute(post, httpContext);
                        rawResponse = httpResponse.getEntity();
                        if (rawResponse != null) {
                            BufferedInputStream bis = new BufferedInputStream(rawResponse.getContent(), (int) rawResponse.getContentLength());
                            content = new ByteArrayOutputStream();

                            for (int readBytes = 0; readBytes != -1; readBytes = bis.read(readbuffer))
                                content.write(readbuffer, 0, readBytes);

                            response = content.toByteArray();
                            if (crypto == null) {
                                if (containsAndreError(response) || containsHTML(response)) {
                                    if (!containsAndreError(response) && !containsHTML(response))
                                        idx++;
                                    else
                                        Log.i(TAG, "Invalid response:" + new String(response, 0, response.length));
                                }
                            } else {
                                if (!containsROMessage(response) && !containsAndreError(response) && !containsHTML(response)) {
                                    response = crypto.decryptData(response);
                                    if (containsAndreError(response) || containsHTML(response))
                                        Log.i(TAG, "Invalid response(cluster):" + new String(response, 0, response.length));
                                } else if (containsAndreError(response) || containsHTML(response))
                                    Log.i(TAG, "Invalid response(extern):" + new String(response, 0, response.length));
                            }
                            connected = true;
                            if (Global.doMediusCallToServer || (didServerCall == false || connected != false) && (!UseCaching || cache == null)) {
                                MediusCall.Offline = false;
                                datablobCanBeSaved = true;
                            } else {
                                String infoStr;
                                if (cache == null) infoStr = "Not connected.";
                                else infoStr = "Loading cached data.";

                                Log.i(TAG, infoStr);
                                if (didServerCall && !connected) Offline = true;

                                response = null;
                                if (cache != null) {
                                    if (!containsROMessage(cache))
                                        response = crypto.decryptData(cache);
                                    else response = cache;
                                }
                            }
                            if (Global.doMediusCallToServer) Global.doMediusCallToServer = false;

                            if (response != null) {
                                Serializer serializer = new Serializer(response);
                                if (serializer.readROHeader(clientID, roInterface, roMethod)) {
                                    if (response.length <= reader.pos) response = null;
                                    else {
                                        response = new byte[response.length - serializer.pos];
                                        System.arraycopy(serializer.getBuffer(), serializer.pos, response, 0, response.length);
                                    }
                                }
                            }
                            if (datablobCanBeSaved && UseCaching && content != null && !Global.isNullOrEmpty(cacheKey))
                                SaveDataBlob(cacheKey, content.toByteArray());

                            cacheKey = "";
                            return !Offline;
                        }
                    } catch (ROException ex) {
                        ex.printStackTrace();
                    } catch (SocketTimeoutException ex) {
                        ex.printStackTrace();
                    } catch (ClientProtocolException ex) {
                        ex.printStackTrace();
                    } catch (IOException ex) {
                        ex.printStackTrace();
                    }
                }
            }
        }
        return false;
    }

    private byte[] getDataBlob(String filename, boolean anyBlob) {
        if (filename != null && filename.length() > 0) {
            File blobFile = new File(String.valueOf(Data.getString(Data.APPFOLDER)) + "/" + filename + ".blob");
            if (!blobFile.exists()) return null;
            FileInputStream fis = null;
            try {
                fis = new FileInputStream(blobFile);
                byte[] array = new byte[512];
                ByteArrayOutputStream content = new ByteArrayOutputStream();

                for (int readBytes = 0; readBytes != -1; readBytes = fis.read(array))
                    content.write(array, 0, readBytes);

                fis.close();
                return content.toByteArray();
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
        return null;
    }

    private void SaveDataBlob(final String s, final byte[] array) {
        try {
            File file = new File(String.valueOf(Data.getString(Data.APPFOLDER)) + "/" + String.format("%s.blob", s));
            if (!file.exists()) {
                new File(Data.getString(Data.APPFOLDER)).mkdirs();
                file.createNewFile();
            }
            FileOutputStream fos = new FileOutputStream(file);
            fos.write(array);
            fos.flush();
            fos.close();
        } catch (Exception ex) {
            Log.i(TAG, "Cache file not saved: Error: " + ex.getMessage());
        }
    }

    public boolean Authenticate() {
        if (authrequest == null) AuthenticateInitialize();

        boolean containsAndreError = false;
        ByteArrayOutputStream content = null;

        for (int i = 0; i < 5; i++) {
            final HttpPost post = new HttpPost(Data.getString(Data.MEDIUSURL));
            post.setEntity(authrequest);
            try {
                HttpEntity rawResponse = MediusCall.authClient.execute(post, MediusCall.httpContext).getEntity();
                if (rawResponse != null) {
                    final BufferedInputStream bis = new BufferedInputStream(rawResponse.getContent(), (int) rawResponse.getContentLength());
                    content = new ByteArrayOutputStream();

                    for (int readBytes = 0; readBytes != -1; readBytes = bis.read(readbuffer))
                        content.write(readbuffer, 0, readBytes);

                    byte[] result = content.toByteArray();

                    if (crypto != null) {
                        if (!containsROMessage(result) && !containsAndreError(result) && !containsHTML(result)) {
                            result = crypto.decryptData(result);
                            containsAndreError = containsAndreError(result);
                            if (containsAndreError || containsHTML(result))
                                Log.e(TAG, "Invalid response(cluster):" + new String(result, 0, result.length));

                        } else {
                            containsAndreError = containsAndreError(result);
                            Log.e(TAG, "Invalid response(extern):" + new String(result, 0, result.length));
                        }
                    } else {
                        containsAndreError = containsAndreError(result);
                        if (containsAndreError || containsHTML(result)) {
                            Log.e(TAG, "Invalid response:" + new String(result, 0, result.length));
                            continue;
                        }
                    }
                    if (containsAndreError) error = "U heeft geen toegang tot het Meta-cluster.";
                    return result.length > 62 && result[62] == 1;
                }
            } catch (Exception ex) {
                Log.e(TAG, "Authenticate", ex);
            }

        }
        if (containsAndreError) MediusCall.error = "U heeft geen toegang tot het Meta-cluster.";

        return false;
    }
}
