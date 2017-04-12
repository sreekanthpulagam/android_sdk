package com.example.testapp;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;

import com.adjust.sdk.AdjustFactory;
import com.adjust.sdk.Constants;
import com.adjust.sdk.UtilNetworking;
import com.adjust.testlibrary.TestLibrary;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import static com.example.testapp.CommandListener.debug;

public class MainActivity extends AppCompatActivity {
    private TestLibrary testLibrary;
    private CommandListener commandListener;
    public static final String TAG = "TestApp";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        commandListener = new CommandListener(this.getApplicationContext());
        String baseUrl = "https://10.0.2.2:8443";
        AdjustFactory.setBaseUrl(baseUrl);
        AdjustFactory.setConnectionOptions(new ConnectionOptions());
        testLibrary = new TestLibrary(baseUrl, commandListener);
        startTestSession();
    }

    private void startTestSession() {
        testLibrary.initTestSession("unity4.10.0@android4.11.1");
    }

    public void onStartTestSession(View v) {
        startTestSession();
    }

    private static class ConnectionOptions implements UtilNetworking.IConnectionOptions {
        @Override
        public void applyConnectionOptions(HttpsURLConnection connection, String clientSdk) {
            connection.setRequestProperty("Client-SDK", clientSdk);
            connection.setConnectTimeout(Constants.ONE_MINUTE);
            connection.setReadTimeout(Constants.ONE_MINUTE);
            // XXX disable ssl checks for tests, temporary!
            try {
                SSLContext sc = SSLContext.getInstance("TLS");
                sc.init(null, new TrustManager[]{
                        new X509TrustManager() {
                            public java.security.cert.X509Certificate[] getAcceptedIssuers() {
                                //getLogger().verbose("getAcceptedIssuers");

                                return null;
                            }
                            public void checkClientTrusted(
                                    java.security.cert.X509Certificate[] certs, String authType) {
                                //getLogger().verbose("checkClientTrusted %s", certs);
                            }
                            public void checkServerTrusted(
                                    java.security.cert.X509Certificate[] certs, String authType) {
                                //getLogger().verbose("checkServerTrusted %s", certs);
                            }
                        }
                }, new java.security.SecureRandom());
                connection.setSSLSocketFactory(sc.getSocketFactory());

                connection.setHostnameVerifier(new HostnameVerifier() {
                    @Override
                    public boolean verify(String hostname, SSLSession session) {
                        //getLogger().verbose("verify hostname %s", hostname);
                        return true;
                    }
                });
            } catch (Exception e) {
                debug("applyConnectionOptions %s", e.getMessage());
            }

        }
    }

}
