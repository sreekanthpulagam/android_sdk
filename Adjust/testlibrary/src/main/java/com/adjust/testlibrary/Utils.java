package com.adjust.testlibrary;

import android.util.Log;

import java.io.IOException;
import java.util.Arrays;
import java.util.Locale;

import javax.net.ssl.HttpsURLConnection;

import static com.adjust.testlibrary.Constants.LOGTAG;
import static com.adjust.testlibrary.UtilsNetworking.connectionOptions;
import static com.adjust.testlibrary.UtilsNetworking.createPOSTHttpsURLConnection;
import static com.adjust.testlibrary.UtilsNetworking.readHttpResponse;

/**
 * Created by nonelse on 11.03.17.
 */

public class Utils {


    public static UtilsNetworking.HttpResponse sendPostI(String path) {
        return sendPostI(path, null);
    }
    public static UtilsNetworking.HttpResponse sendPostI(String path, String clientSdk) {
        String targetURL = TestLibrary.baseUrl + path;
        debug("targetURL: %s", targetURL);

        try {
            if (clientSdk != null) {
                connectionOptions.clientSdk = clientSdk;
            }
            HttpsURLConnection connection = createPOSTHttpsURLConnection(
                    targetURL, null, connectionOptions);
            UtilsNetworking.HttpResponse httpResponse = readHttpResponse(connection);
            debug("Response: %s", httpResponse.response);

            httpResponse.headerFields= connection.getHeaderFields();
            debug("Headers: %s", httpResponse.headerFields);

            return httpResponse;
        } catch (IOException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public static void debug(String message, Object... parameters) {
        try {
            Log.d(LOGTAG, String.format(Locale.US, message, parameters));
        } catch (Exception e) {
            Log.e(LOGTAG, String.format(Locale.US, "Error formating log message: %s, with params: %s"
                    , message, Arrays.toString(parameters)));
        }
    }

    public static void error(String message, Object... parameters) {
        try {
            Log.d(LOGTAG, String.format(Locale.US, message, parameters));
        } catch (Exception e) {
            Log.e(LOGTAG, String.format(Locale.US, "Error formating log message: %s, with params: %s"
                    , message, Arrays.toString(parameters)));
        }
    }

    public static String appendBasePath(String basePath, String path) {
        if (basePath == null) {
            return path;
        }
        return basePath + path;
    }
}
