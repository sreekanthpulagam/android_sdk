package com.adjust.sdk;

import android.net.Uri;
import android.util.Log;
import com.adobe.fre.*;
import org.json.JSONObject;

import java.lang.*;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by pfms on 31/07/14.
 */
public class AdjustFunction implements FREFunction, 
       OnAttributionChangedListener,
       OnEventTrackingSucceededListener, 
       OnEventTrackingFailedListener,
       OnSessionTrackingSucceededListener,
       OnSessionTrackingFailedListener,
       OnDeeplinkResponseListener {
    private String functionName;
    private Boolean shouldLaunchDeeplink;

    public AdjustFunction(String functionName) {
        this.functionName = functionName;
    }

    @Override
    public FREObject call(FREContext freContext, FREObject[] freObjects) {
        AdjustExtension.context = (AdjustContext) freContext;

        if (functionName == AdjustContext.OnCreate) {
            return OnCreate(freContext, freObjects);
        }

        if (functionName == AdjustContext.TrackEvent) {
            return TrackEvent(freContext, freObjects);
        }

        if (functionName == AdjustContext.SetEnabled) {
            return SetEnabled(freContext, freObjects);
        }

        if (functionName == AdjustContext.IsEnabled) {
            return IsEnabled(freContext, freObjects);
        }

        if (functionName == AdjustContext.OnResume) {
            return OnResume(freContext, freObjects);
        }

        if (functionName == AdjustContext.OnPause) {
            return OnPause(freContext, freObjects);
        }

        if (functionName == AdjustContext.AppWillOpenUrl) {
            return AppWillOpenUrl(freContext, freObjects);
        }

        if (functionName == AdjustContext.SetOfflineMode) {
            return SetOfflineMode(freContext, freObjects);
        }

        if (functionName == AdjustContext.SetReferrer) {
            return SetReferrer(freContext, freObjects);
        }

        if (functionName == AdjustContext.GetGoogleAdId) {
            return GetGoogleAdId(freContext, freObjects);
        }

        if (functionName == AdjustContext.GetIdfa) {
            return GetIdfa(freContext, freObjects);
        }

        if (functionName == AdjustContext.AddSessionCallbackParameter) {
            return AddSessionCallbackParameter(freContext, freObjects);
        }

        if (functionName == AdjustContext.RemoveSessionCallbackParameter) {
            return RemoveSessionCallbackParameter(freContext, freObjects);
        }

        if (functionName == AdjustContext.ResetSessionCallbackParameters) {
            return ResetSessionCallbackParameters(freContext, freObjects);
        }

        if (functionName == AdjustContext.AddSessionPartnerParameter) {
            return AddSessionPartnerParameter(freContext, freObjects);
        }

        if (functionName == AdjustContext.RemoveSessionPartnerParameter) {
            return RemoveSessionPartnerParameter(freContext, freObjects);
        }

        if (functionName == AdjustContext.ResetSessionPartnerParameters) {
            return ResetSessionPartnerParameters(freContext, freObjects);
        }

        if (functionName == AdjustContext.SetDeviceToken) {
            return SetDeviceToken(freContext, freObjects);
        }

        return null;
    }

    private FREObject OnCreate(FREContext freContext, FREObject[] freObjects) {
        try {
            String appToken = null;
            String environment = null;
            String logLevel = null;
            boolean allowSuppressLogLevel = false;

            if (freObjects[0] != null) {
                appToken = freObjects[0].getAsString();
            }

            if (freObjects[1] != null) {
                environment = freObjects[1].getAsString();
            }
            
            if (freObjects[2] != null) {
                logLevel = freObjects[2].getAsString();
            }

           
            if (logLevel != null && logLevel.equals("suppress")) {
                allowSuppressLogLevel = true;
            }

            AdjustConfig adjustConfig = new AdjustConfig(freContext.getActivity(), appToken, environment, allowSuppressLogLevel);

            if (logLevel != null) {
                if (logLevel.equals("verbose")) {
                    adjustConfig.setLogLevel(LogLevel.VERBOSE);
                } else if (logLevel.equals("debug")) {
                    adjustConfig.setLogLevel(LogLevel.DEBUG);
                } else if (logLevel.equals("info")) {
                    adjustConfig.setLogLevel(LogLevel.INFO);
                } else if (logLevel.equals("warn")) {
                    adjustConfig.setLogLevel(LogLevel.WARN);
                } else if (logLevel.equals("error")) {
                    adjustConfig.setLogLevel(LogLevel.ERROR);
                } else if (logLevel.equals("assert")) {
                    adjustConfig.setLogLevel(LogLevel.ASSERT);
                } else if (logLevel.equals("assert")) {
                    adjustConfig.setLogLevel(LogLevel.ASSERT);
                } else if (logLevel.equals("suppress")) {
                    adjustConfig.setLogLevel(LogLevel.SUPPRESS);
                } else {
                    adjustConfig.setLogLevel(LogLevel.INFO);
                }
            }

            if (freObjects[3] != null) {
                Boolean eventBuffering = freObjects[3].getAsBool();
                adjustConfig.setEventBufferingEnabled(eventBuffering);
            }

            if (freObjects[4] != null) {
                Boolean isAttributionCallbackSet = freObjects[4].getAsBool();

                if (isAttributionCallbackSet) {
                    adjustConfig.setOnAttributionChangedListener(this);
                }
            }

            if (freObjects[5] != null) {
                Boolean isCallbackSet = freObjects[5].getAsBool();

                if (isCallbackSet) {
                    adjustConfig.setOnEventTrackingSucceededListener(this);
                }
            }

            if (freObjects[6] != null) {
                Boolean isCallbackSet = freObjects[6].getAsBool();

                if (isCallbackSet) {
                    adjustConfig.setOnEventTrackingFailedListener(this);
                }
            }

            if (freObjects[7] != null) {
                Boolean isCallbackSet = freObjects[7].getAsBool();

                if (isCallbackSet) {
                    adjustConfig.setOnSessionTrackingSucceededListener(this);
                }
            }

            if (freObjects[8] != null) {
                Boolean isCallbackSet = freObjects[8].getAsBool();

                if (isCallbackSet) {
                    adjustConfig.setOnSessionTrackingFailedListener(this);
                }
            }

            if (freObjects[9] != null) {
                Boolean isCallbackSet = freObjects[9].getAsBool();

                if (isCallbackSet) {
                    adjustConfig.setOnDeeplinkResponseListener(this);
                }
            }

            if (freObjects[10] != null) {
                String defaultTracker = freObjects[10].getAsString();

                if (defaultTracker != null) {
                    adjustConfig.setDefaultTracker(defaultTracker);
                }
            }

            if (freObjects[11] != null) {
                String sdkPrefix = freObjects[11].getAsString();

                if (sdkPrefix != null) {
                    adjustConfig.setSdkPrefix(sdkPrefix);
                }
            }

            if (freObjects[12] != null) {
                shouldLaunchDeeplink = freObjects[12].getAsBool();
            }

            if (freObjects[13] != null) {
                String processName = freObjects[13].getAsString();
                adjustConfig.setProcessName(processName);
            }
            if (appToken != null && environment != null) {
                AdjustConfig adjustConfig = new AdjustConfig(freContext.getActivity(), appToken, environment);
                adjustConfig.setLogLevel(LogLevel.VERBOSE);
                adjustConfig.setSdkPrefix("adobe_air4.0.0");

                Adjust.onCreate(adjustConfig);
            }
        } catch (Exception e) {
            Log.e(AdjustExtension.LogTag, e.getMessage());
        }

        return null;
    }

    private FREObject TrackEvent(FREContext freContext, FREObject[] freObjects) {
        try {
            String eventToken = null;
            String currency = null;
            String orderId = null;
            double revenue = 0;

            if (freObjects[0] != null) {
                eventToken = freObjects[0].getAsString();
            }

            AdjustEvent adjustEvent = new AdjustEvent(eventToken);

            if (freObjects[1] != null) {
                currency = freObjects[1].getAsString();

                if (freObjects[2] != null) {
                    revenue = freObjects[2].getAsDouble();
                }

                adjustEvent.setRevenue(revenue, currency);
            }

            if (freObjects[3] != null) {
                for (int i = 0; i < ((FREArray) freObjects[3]).getLength(); i += 2) {
                    adjustEvent.addCallbackParameter(((FREArray) freObjects[3]).getObjectAt(i).getAsString(),
                            ((FREArray) freObjects[3]).getObjectAt(i + 1).getAsString());
                }
            }

            if (freObjects[4] != null) {
                for (int i = 0; i < ((FREArray) freObjects[4]).getLength(); i += 2) {
                    adjustEvent.addCallbackParameter(((FREArray) freObjects[4]).getObjectAt(i).getAsString(),
                            ((FREArray) freObjects[4]).getObjectAt(i + 1).getAsString());
                }
            }

            if (freObjects[5] != null) {
                orderId = freObjects[5].getAsString();
                adjustEvent.setOrderId(orderId);
            }

            Adjust.trackEvent(adjustEvent);
        } catch (Exception e) {
            Log.e(AdjustExtension.LogTag, e.getMessage());
        }

       return null;
    }

    private FREObject SetEnabled(FREContext freContext, FREObject[] freObjects) {
        try {
            Boolean enabled = freObjects[0].getAsBool();

            Adjust.setEnabled(enabled);
        } catch (Exception e) {
            Log.e(AdjustExtension.LogTag, e.getMessage());
        }

        return null;
    }

    private FREObject IsEnabled(FREContext freContext, FREObject[] freObjects) {
        try {
            return FREObject.newObject(Adjust.isEnabled());
        } catch (Exception e) {
            Log.e(AdjustExtension.LogTag, e.getMessage());
        }

        return null;
    }

    private FREObject OnResume(FREContext freContext, FREObject[] freObjects) {
        try {
            Adjust.onResume();
        } catch (Exception e) {
            Log.e(AdjustExtension.LogTag, e.getMessage());
        }

        return null;
    }

    private FREObject OnPause(FREContext freContext, FREObject[] freObjects) {
        try {
            Adjust.onPause();
        } catch (Exception e) {
            Log.e(AdjustExtension.LogTag, e.getMessage());
        }

        return null;
    }

    private FREObject AppWillOpenUrl(FREContext freContext, FREObject[] freObjects) {
        try {
            String url = freObjects[0].getAsString();
            Uri uri = Uri.parse(url);

            Adjust.appWillOpenUrl(uri);
        } catch (Exception e) {
            Log.e(AdjustExtension.LogTag, e.getMessage());
        }

        return null;
    }

    private FREObject SetOfflineMode(FREContext freContext, FREObject[] freObjects) {
        try {
            Boolean isOffline = freObjects[0].getAsBool();

            Adjust.setOfflineMode(isOffline);
        } catch (Exception e) {
            Log.e(AdjustExtension.LogTag, e.getMessage());
        }

        return null;
    }

    private FREObject SetReferrer(FREContext freContext, FREObject[] freObjects) {
        try {
            String referrer = freObjects[0].getAsString();

            Adjust.setReferrer(referrer);
        } catch (Exception e) {
            Log.e(AdjustExtension.LogTag, e.getMessage());
        }

        return null;
    }

    private FREObject SetDeviceToken(FREContext freContext, FREObject[] freObjects) {
        try {
            String token = freObjects[0].getAsString();

            Adjust.setPushToken(token);
        } catch (Exception e) {
            Log.e(AdjustExtension.LogTag, e.getMessage());
        }

        return null;
    }

    private FREObject GetGoogleAdId(final FREContext freContext, FREObject[] freObjects) {
        Adjust.getGoogleAdId(freContext.getActivity(), new OnDeviceIdsRead() {
            @Override
            public void onGoogleAdIdRead(String playAdId) {
                if (playAdId != null) {
                    freContext.dispatchStatusEventAsync("adjust_googleAdId", playAdId);
                } else {
                    freContext.dispatchStatusEventAsync("adjust_googleAdId", "");
                }
            }
        });

        return null;
    }

    private FREObject GetIdfa(FREContext freContext, FREObject[] freObjects) { return null; }

    private FREObject AddSessionCallbackParameter(FREContext freContext, FREObject[] freObjects) {
        try {
            String key = freObjects[0].getAsString();
            String value = freObjects[1].getAsString();

            Adjust.addSessionCallbackParameter(key, value);
        } catch (Exception e) {
            Log.e(AdjustExtension.LogTag, e.getMessage());
        }

        return null;
    }

    private FREObject RemoveSessionCallbackParameter(FREContext freContext, FREObject[] freObjects) {
        try {
            String key = freObjects[0].getAsString();

            Adjust.removeSessionCallbackParameter(key);
        } catch (Exception e) {
            Log.e(AdjustExtension.LogTag, e.getMessage());
        }

        return null;
    }

    private FREObject ResetSessionCallbackParameters(FREContext freContext, FREObject[] freObjects) {
        try {
            Adjust.resetSessionCallbackParameters();
        } catch (Exception e) {
            Log.e(AdjustExtension.LogTag, e.getMessage());
        }

        return null;
    }

    private FREObject AddSessionPartnerParameter(FREContext freContext, FREObject[] freObjects) {
        try {
            String key = freObjects[0].getAsString();
            String value = freObjects[1].getAsString();

            Adjust.addSessionPartnerParameter(key, value);
        } catch (Exception e) {
            Log.e(AdjustExtension.LogTag, e.getMessage());
        }

        return null;
    }

    private FREObject RemoveSessionPartnerParameter(FREContext freContext, FREObject[] freObjects) {
        try {
            String key = freObjects[0].getAsString();

            Adjust.removeSessionPartnerParameter(key);
        } catch (Exception e) {
            Log.e(AdjustExtension.LogTag, e.getMessage());
        }

        return null;
    }

    private FREObject ResetSessionPartnerParameters(FREContext freContext, FREObject[] freObjects) {
        try {
            Adjust.resetSessionPartnerParameters();
        } catch (Exception e) {
            Log.e(AdjustExtension.LogTag, e.getMessage());
        }

        return null;
    }

    @Override
    public void onAttributionChanged(AdjustAttribution attribution) {
        if (attribution == null) {
            return;
        }

        String response = "trackerToken=" + attribution.trackerToken + ","
            + "trackerName=" + attribution.trackerName + ","
            + "campaign=" + attribution.campaign + ","
            + "network=" + attribution.network + ","
            + "creative=" + attribution.creative + ","
            + "adgroup=" + attribution.adgroup + ","
            + "clickLabel=" + attribution.clickLabel;

        AdjustExtension.context.dispatchStatusEventAsync("adjust_attributionData", response);
    }

    @Override
    public void onFinishedEventTrackingSucceeded(AdjustEventSuccess event) {
        if (event == null) {
            return;
        }

        StringBuilder response = new StringBuilder();
        response.append("message=" + event.message + ","
                + "timestamp=" + event.timestamp + ","
                + "adid=" + event.adid + ","
                + "eventToken=" + event.eventToken + ",");

        if (event.jsonResponse != null) {
            response.append("jsonResponse=" + event.jsonResponse.toString());
        }

        AdjustExtension.context.dispatchStatusEventAsync("adjust_eventTrackingSucceeded", response.toString());
    }

    @Override
    public void onFinishedEventTrackingFailed(AdjustEventFailure event) {
        if (event == null) {
            return;
        }

        StringBuilder response = new StringBuilder();
        response.append("message=" + event.message + ","
                + "timestamp=" + event.timestamp + ","
                + "adid=" + event.adid + ","
                + "eventToken=" + event.eventToken + ","
                + "willRetry=" + event.willRetry + ",");

        if (event.jsonResponse != null) {
            response.append("jsonResponse=" + event.jsonResponse.toString());
        }

        AdjustExtension.context.dispatchStatusEventAsync("adjust_eventTrackingFailed", response.toString());
    }

    @Override
    public void onFinishedSessionTrackingSucceeded(AdjustSessionSuccess event) {
        if (event == null) {
            return;
        }

        StringBuilder response = new StringBuilder();
        response.append("message=" + event.message + ","
                + "timestamp=" + event.timestamp + ","
                + "adid=" + event.adid + ",");

        if (event.jsonResponse != null) {
            response.append("jsonResponse=" + event.jsonResponse.toString());
        }

        AdjustExtension.context.dispatchStatusEventAsync("adjust_sessionTrackingSucceeded", response.toString());
    }

    @Override
    public void onFinishedSessionTrackingFailed(AdjustSessionFailure event) {
        if (event == null) {
            return;
        }

        StringBuilder response = new StringBuilder();
        response.append("message=" + event.message + ","
                + "timestamp=" + event.timestamp + ","
                + "adid=" + event.adid + ","
                + "willRetry=" + event.willRetry + ",");

        if (event.jsonResponse != null) {
            response.append("jsonResponse=" + event.jsonResponse.toString());
        }

        AdjustExtension.context.dispatchStatusEventAsync("adjust_sessionTrackingFailed", response.toString());
    }

    @Override
    public boolean launchReceivedDeeplink(Uri deeplink) {
        String response = deeplink.toString();

        AdjustExtension.context.dispatchStatusEventAsync("adjust_deferredDeeplink", response);
        
        return shouldLaunchDeeplink;
    }

    private Map<String, String> getAsMap(FREObject freObject) throws Exception{
        if (freObject == null) {
            return null;
        }
        FREArray parameters = (FREArray) freObject.getProperty("adjust keys");

        if (parameters == null) {
            Log.e(AdjustExtension.LogTag, "getAsMap property 'adjust keys' is null");
            return null;
        }

        int i = 0;
        int length = (int)parameters.getLength();

        Map<String, String> map = new HashMap<String, String>(length);

        while (i < length) {
            String key = parameters.getObjectAt(i).getAsString();
            String value = freObject.getProperty(key).getAsString();
            map.put(key,value);

            i++;
        }

        return map;
    }
}
