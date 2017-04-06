package com.example.testapp;

import android.content.Context;
import android.util.Log;

import com.adjust.sdk.Adjust;
import com.adjust.sdk.AdjustConfig;
import com.adjust.sdk.AdjustEvent;
import com.adjust.sdk.AdjustFactory;
import com.adjust.sdk.LogLevel;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.example.testapp.MainActivity.TAG;

/**
 * Created by nonelse on 10.03.17.
 */

public class AdjustCommandExecutor extends ICommandExecutor {
    Context context;
    String basePath;
    private static final String DefaultConfigName = "defaultConfig";
    private static final String DefaultEventName = "defaultEvent";
    private Map<String, Object> savedInstances = new HashMap<String, Object>();


    public AdjustCommandExecutor(Context context) {
        this.context = context;
    }

    @Override
    public void executeCommand(Command command) {
        super.executeCommand(command);

        try {
            switch (command.methodName) {
                case "config": config(); break;
                case "start": start(); break;
                case "event": event(); break;
                case "trackEvent": trackEvent(); break;
                case "resume": resume(); break;
                case "pause": pause(); break;
                case "setEnabled": setEnabled(); break;
                case "setReferrer": setReferrer(); break;
                case "setOfflineMode": setOfflineMode(); break;
                case "sendFirstPackages": sendFirstPackages(); break;
                case "addSessionCallbackParameter": addSessionCallbackParameter(); break;
                case "addSessionPartnerParameter": addSessionPartnerParameter(); break;
                case "removeSessionCallbackParameter": removeSessionCallbackParameter(); break;
                case "removeSessionPartnerParameter": removeSessionPartnerParameter(); break;
                case "resetSessionCallbackParameters": resetSessionCallbackParameters(); break;
                case "resetSessionPartnerParameters": resetSessionPartnerParameters(); break;
                case "setPushToken": setPushToken(); break;
                case "teardown": teardown(); break;
            }
        } catch (NullPointerException ex) {
            ex.printStackTrace();
            Log.e(TAG, "executeCommand: failed to parse command. Check commands' syntax");
        }
    }

    public void setBasePath(String basePath) {
        this.basePath = basePath;
    }

    private void config() {
        String configName = null;
        if (command.parameters.containsKey("configName")) {
            configName = command.getFirstParameterValue("configName");
        } else {
            configName = DefaultConfigName;
        }

        AdjustConfig adjustConfig = null;
        if (savedInstances.containsKey(configName)) {
            adjustConfig = (AdjustConfig)savedInstances.get(configName);
        } else {
            String environment = command.getFirstParameterValue("environment");
            String appToken = command.getFirstParameterValue("appToken");
            Context context = this.context;
            if ("null".equalsIgnoreCase(command.getFirstParameterValue("context"))) {
                context = null;
            }
            adjustConfig = new AdjustConfig(context, appToken, environment);
            savedInstances.put(configName, adjustConfig);
        }

        if (command.containsParameter("logLevel")) {
            String logLevelS = command.getFirstParameterValue("logLevel");
            LogLevel logLevel = null;
            switch (logLevelS) {
                case "verbose": logLevel = LogLevel.VERBOSE;
                    break;
                case "debug": logLevel = LogLevel.DEBUG;
                    break;
                case "info": logLevel = LogLevel.INFO;
                    break;
                case "warn": logLevel = LogLevel.WARN;
                    break;
                case "error": logLevel = LogLevel.ERROR;
                    break;
                case "assert": logLevel = LogLevel.ASSERT;
                    break;
                case "suppress": logLevel = LogLevel.SUPRESS;
                    break;
            }
            adjustConfig.setLogLevel(logLevel);
        }

        if (command.containsParameter("defaultTracker")) {
            String defaultTracker = command.getFirstParameterValue("defaultTracker");
            adjustConfig.setDefaultTracker(defaultTracker);
        }

        if (command.containsParameter("delayStart")) {
            String delayStartS = command.getFirstParameterValue("delayStart");
            double delayStart = Double.parseDouble(delayStartS);
            adjustConfig.setDelayStart(delayStart);
        }

        if (command.containsParameter("deviceKnown")) {
            String deviceKnownS = command.getFirstParameterValue("deviceKnown");
            boolean deviceKnown = "true".equals(deviceKnownS);
            adjustConfig.setDeviceKnown(deviceKnown);
        }

        if (command.containsParameter("eventBufferingEnabled")) {
            String eventBufferingEnabledS = command.getFirstParameterValue("eventBufferingEnabled");
            boolean eventBufferingEnabled = "true".equals(eventBufferingEnabledS);
            adjustConfig.setEventBufferingEnabled(eventBufferingEnabled);
        }

        if (command.containsParameter("sendInBackground")) {
            String sendInBackgroundS = command.getFirstParameterValue("sendInBackground");
            boolean sendInBackground = "true".equals(sendInBackgroundS);
            adjustConfig.setSendInBackground(sendInBackground);
        }

        if (command.containsParameter("userAgent")) {
            String userAgent = command.getFirstParameterValue("userAgent");
            adjustConfig.setUserAgent(userAgent);
        }
        // XXX add listeners
    }

    private void start() {
        config();
        String configName = null;
        if (command.parameters.containsKey("configName")) {
            configName = command.getFirstParameterValue("configName");
        } else {
            configName = DefaultConfigName;
        }

        AdjustConfig adjustConfig = (AdjustConfig)savedInstances.get(configName);

        adjustConfig.setBasePath(basePath);
        Adjust.onCreate(adjustConfig);
    }

    private void event() throws NullPointerException {
        String eventName = null;
        if (command.parameters.containsKey("eventName")) {
            eventName = command.getFirstParameterValue("eventName");
        } else {
            eventName = DefaultEventName;
        }

        AdjustEvent adjustEvent = null;
        if (savedInstances.containsKey(eventName)) {
            adjustEvent = (AdjustEvent)savedInstances.get(eventName);
        } else {
            String eventToken = command.getFirstParameterValue("eventToken");
            adjustEvent = new AdjustEvent(eventToken);
            savedInstances.put(eventName, adjustEvent);
        }

        if (command.parameters.containsKey("revenue")) {
            List<String> revenueParams = command.parameters.get("revenue");
            String currency = revenueParams.get(0);
            double revenue = Double.parseDouble(revenueParams.get(1));
            adjustEvent.setRevenue(revenue, currency);
        }

        if (command.parameters.containsKey("callbackParams")) {
            List<String> callbackParams = command.parameters.get("callbackParams");
            for (int i = 0; i < callbackParams.size(); i = i + 2) {
                String key = callbackParams.get(i);
                String value = callbackParams.get(i + 1);
                adjustEvent.addCallbackParameter(key, value);
            }
        }
        if (command.parameters.containsKey("partnerParams")) {
            List<String> partnerParams = command.parameters.get("partnerParams");
            for (int i = 0; i < partnerParams.size(); i = i + 2) {
                String key = partnerParams.get(i);
                String value = partnerParams.get(i + 1);
                adjustEvent.addPartnerParameter(key, value);
            }
        }
        if (command.parameters.containsKey("orderId")) {
            String orderId = command.getFirstParameterValue("orderId");
            adjustEvent.setOrderId(orderId);
        }

        Adjust.trackEvent(adjustEvent);
    }

    private void trackEvent() {
        event();
        String eventName = null;
        if (command.parameters.containsKey("eventName")) {
            eventName = command.getFirstParameterValue("eventName");
        } else {
            eventName = DefaultConfigName;
        }
        AdjustEvent adjustEvent = (AdjustEvent)savedInstances.get(eventName);
        Adjust.trackEvent(adjustEvent);
    }

    private void setReferrer() {
        String referrer = command.getFirstParameterValue("referrer");
        Adjust.setReferrer(referrer);
    }

    private void pause() {
        Adjust.onPause();
    }

    private void resume() {
        Adjust.onResume();
    }

    private void setEnabled() {
        Boolean enabled = Boolean.valueOf(command.getFirstParameterValue("enabled"));
        Adjust.setEnabled(enabled);
    }

    private void setOfflineMode() {
        Boolean enabled = Boolean.valueOf(command.getFirstParameterValue("enabled"));
        Adjust.setOfflineMode(enabled);
    }

    private void sendFirstPackages() {
        Adjust.sendFirstPackages();
    }

    private void addSessionCallbackParameter() {
        for (List<String> keyValuePairs: command.parameters.values()) {
            String key = keyValuePairs.get(0);
            String value = keyValuePairs.get(1);
            Adjust.addSessionCallbackParameter(key, value);
        }
    }

    private void addSessionPartnerParameter() {
        for (List<String> keyValuePairs: command.parameters.values()) {
            String key = keyValuePairs.get(0);
            String value = keyValuePairs.get(1);
            Adjust.addSessionPartnerParameter(key, value);
        }
    }

    private void removeSessionCallbackParameter() {
        String key = command.getFirstParameterValue("key");
        Adjust.removeSessionCallbackParameter(key);
    }

    private void removeSessionPartnerParameter() {
        String key = command.getFirstParameterValue("key");

        Adjust.removeSessionPartnerParameter(key);
    }

    private void resetSessionCallbackParameters() {
        Adjust.resetSessionCallbackParameters();
    }

    private void resetSessionPartnerParameters() {
        Adjust.resetSessionPartnerParameters();
    }

    private void setPushToken() {
        String token = command.getFirstParameterValue("pushToken");

        Adjust.setPushToken(token);
    }

    private void teardown() throws NullPointerException {
        String deleteStateString = command.getFirstParameterValue("deleteState");
        boolean deleteState = Boolean.parseBoolean(deleteStateString);

        AdjustFactory.teardown(deleteState);
    }
}
