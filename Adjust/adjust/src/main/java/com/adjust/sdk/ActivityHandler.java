//
//  ActivityHandler.java
//  Adjust
//
//  Created by Christian Wellenbrock on 2013-06-25.
//  Copyright (c) 2013 adjust GmbH. All rights reserved.
//  See the file MIT-LICENSE for copying permission.
//

package com.adjust.sdk;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;

import org.json.JSONObject;

import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static com.adjust.sdk.Constants.ACTIVITY_STATE_FILENAME;
import static com.adjust.sdk.Constants.ATTRIBUTION_FILENAME;
import static com.adjust.sdk.Constants.LOGTAG;

public class ActivityHandler extends HandlerThread implements IActivityHandler {

    private static long TIMER_INTERVAL;
    private static long TIMER_START;
    private static long SESSION_INTERVAL;
    private static long SUBSESSION_INTERVAL;
    private static final String TIME_TRAVEL = "Time travel!";
    private static final String ADJUST_PREFIX = "adjust_";
    private static final String ACTIVITY_STATE_NAME = "Activity state";
    private static final String ATTRIBUTION_NAME = "Attribution";

    private SessionHandler sessionHandler;
    private IPackageHandler packageHandler;
    private ActivityState activityState;
    private Logger logger;
    private static ScheduledExecutorService timer;
    private boolean enabled;
    private boolean shouldGetAttribution;
    private boolean offline;

    private DeviceInfo deviceInfo;
    private AdjustConfig adjustConfig; // always valid after construction
    private Attribution attribution;
    private IAttributionHandler attributionHandler;

    private ActivityHandler(AdjustConfig adjustConfig) {
        super(LOGTAG, MIN_PRIORITY);
        setDaemon(true);
        start();

        logger = AdjustFactory.getLogger();
        sessionHandler = new SessionHandler(getLooper(), this);
        enabled = true;
        this.adjustConfig = adjustConfig;

        Message message = Message.obtain();
        message.arg1 = SessionHandler.INIT;
        sessionHandler.sendMessage(message);
    }

    public static ActivityHandler getInstance(AdjustConfig adjustConfig) {
        if (adjustConfig == null) {
            AdjustFactory.getLogger().error("AdjustConfig not initialized correctly");
            return null;
        }

        if (!adjustConfig.isValid()) {
            return null;
        }

        ActivityHandler activityHandler = new ActivityHandler(adjustConfig);
        return activityHandler;
    }

    public void trackSubsessionStart() {
        Message message = Message.obtain();
        message.arg1 = SessionHandler.START;
        sessionHandler.sendMessage(message);
    }

    public void trackSubsessionEnd() {
        Message message = Message.obtain();
        message.arg1 = SessionHandler.END;
        sessionHandler.sendMessage(message);
    }

    public void trackEvent(Event event) {
        Message message = Message.obtain();
        message.arg1 = SessionHandler.EVENT;
        message.obj = event;
        sessionHandler.sendMessage(message);
    }

    public void finishedTrackingActivity(JSONObject jsonResponse) {
        if (jsonResponse == null) {
            return;
        }

        Message message = Message.obtain();
        message.arg1 = SessionHandler.FINISH_TRACKING;
        message.obj = jsonResponse;
        sessionHandler.sendMessage(message);
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
        if (activityState != null) {
            activityState.enabled = enabled;
        }
        if (enabled) {
            trackSubsessionStart();
            logger.info("Resuming package handler to enabled the SDK");
        } else {
            trackSubsessionEnd();
            logger.info("Pausing package handler to disable the SDK");
        }
    }

    public void setOfflineMode(boolean offline) {
        this.offline = offline;
        if (offline) {
            logger.info("Pausing package handler to put in offline mode");
            trackSubsessionEnd();
        } else {
            logger.info("Resuming package handler to put in online mode");
            trackSubsessionStart();
            startTimer();
        }
    }

    public boolean isEnabled() {
        if (activityState != null) {
            return activityState.enabled;
        } else {
            return enabled;
        }
    }

    public void readOpenUrl(Uri url) {
        Message message = Message.obtain();
        message.arg1 = SessionHandler.DEEP_LINK;
        message.obj = url;
        sessionHandler.sendMessage(message);
    }

    public void updateAttribution(Attribution attribution) {
        if (attribution == null) return;

        if (attribution.equals(this.attribution)) {
            return;
        }

        saveAttribution(attribution);
        launchAttributionListener();
    }

    private void saveAttribution(Attribution attribution) {
        this.attribution = attribution;
        writeAttribution();
    }

    private void launchAttributionListener() {
        if (adjustConfig.onAttributionChangedListener == null) {
            return;
        }
        Handler handler = new Handler(adjustConfig.context.getMainLooper());
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                adjustConfig.onAttributionChangedListener.onAttributionChanged(attribution);
            }
        };
        handler.post(runnable);
    }

    public void setReferrer(String referrer) {
        adjustConfig.referrer = referrer;
        sendReferrer(); // send to background queue to make sure that activityState is valid
    }

    public void setAskingAttribution(boolean askingAttribution) {
        activityState.askingAttribution = askingAttribution;
        writeActivityState();
    }

    public ActivityPackage getAttributionPackage() {
        PackageBuilder attributionBuilder = new PackageBuilder(adjustConfig, deviceInfo, activityState);
        return attributionBuilder.buildAttributionPackage();
    }

    private void sendReferrer() {
        Message message = Message.obtain();
        message.arg1 = SessionHandler.SEND_REFERRER;
        sessionHandler.sendMessage(message);
    }

    private class UrlClickTime {
        Uri url;
        long clickTime;
        UrlClickTime(Uri url, long clickTime) {
            this.url = url;
            this.clickTime = clickTime;
        }
    }

    private static final class SessionHandler extends Handler {
        private static final int BASE_ADDRESS = 72630;
        private static final int INIT = BASE_ADDRESS + 1;
        private static final int START = BASE_ADDRESS + 2;
        private static final int END = BASE_ADDRESS + 3;
        private static final int EVENT = BASE_ADDRESS + 4;
        private static final int FINISH_TRACKING = BASE_ADDRESS + 5;
        private static final int DEEP_LINK = BASE_ADDRESS + 6;
        private static final int SEND_REFERRER = BASE_ADDRESS + 7;

        private final WeakReference<ActivityHandler> sessionHandlerReference;

        protected SessionHandler(Looper looper, ActivityHandler sessionHandler) {
            super(looper);
            this.sessionHandlerReference = new WeakReference<ActivityHandler>(sessionHandler);
        }

        @Override
        public void handleMessage(Message message) {
            super.handleMessage(message);

            ActivityHandler sessionHandler = sessionHandlerReference.get();
            if (sessionHandler == null) {
                return;
            }

            switch (message.arg1) {
                case INIT:
                    sessionHandler.initInternal();
                    break;
                case START:
                    sessionHandler.startInternal();
                    break;
                case END:
                    sessionHandler.endInternal();
                    break;
                case EVENT:
                    Event event = (Event) message.obj;
                    sessionHandler.trackEventInternal(event);
                    break;
                case DEEP_LINK:
                    Uri url = (Uri) message.obj;
                    sessionHandler.readOpenUrlInternal(url);
                    break;
                case SEND_REFERRER:
                    sessionHandler.sendReferrerInternal();
                    break;
                case FINISH_TRACKING:
                    JSONObject jsonResponse = (JSONObject) message.obj;
                    sessionHandler.finishedTrackingActivityInternal(jsonResponse);
                    break;
            }
        }
    }

    private void initInternal() {
        TIMER_INTERVAL = AdjustFactory.getTimerInterval();
        TIMER_START = AdjustFactory.getTimerStart();
        SESSION_INTERVAL = AdjustFactory.getSessionInterval();
        SUBSESSION_INTERVAL = AdjustFactory.getSubsessionInterval();

        deviceInfo = new DeviceInfo(adjustConfig.context, adjustConfig.sdkPrefix);

        if (adjustConfig.environment == AdjustConfig.ENVIRONMENT_PRODUCTION) {
            logger.setLogLevel(Logger.LogLevel.ASSERT);
        } else {
            logger.setLogLevel(adjustConfig.logLevel);
        }

        if (adjustConfig.eventBufferingEnabled) {
            logger.info("Event buffering is enabled");
        }

        String playAdId = Util.getPlayAdId(adjustConfig.context);
        if (playAdId == null) {
            logger.info("Unable to get Google Play Services Advertising ID at start time");
        }

        if (adjustConfig.defaultTracker != null) {
            logger.info("Default tracker: '%s'", adjustConfig.defaultTracker);
        }

        if (adjustConfig.referrer != null) {
            sendReferrer(); // send to background queue to make sure that activityState is valid
        }

        readAttribution();
        readActivityState();

        packageHandler = AdjustFactory.getPackageHandler(this, adjustConfig.context);

        shouldGetAttribution = true;

        startInternal();
    }

    private void sendReferrerInternal() {
        PackageBuilder builder = new PackageBuilder(adjustConfig, deviceInfo, activityState);
        ActivityPackage clickPackage = builder.buildClickPackage("referrer");
        packageHandler.sendClickPackage(clickPackage);
    }

    private void startInternal() {
        // it shouldn't start if it was disabled after a first session
        if (activityState != null
                && !activityState.enabled) {
            return;
        }

        if (!offline) {
            packageHandler.resumeSending();
        }

        processSession();

        checkAttributionState();

        startTimer();
    }

    private void processSession() {
        long now = System.currentTimeMillis();

        // very first session
        if (activityState == null) {
            activityState = new ActivityState();
            activityState.sessionCount = 1; // this is the first session
            activityState.createdAt = now;  // starting now

            transferSessionPackage();
            activityState.resetSessionAttributes(now);
            activityState.enabled = this.enabled;
            writeActivityState();
            return;
        }

        long lastInterval = now - activityState.lastActivity;

        if (lastInterval < 0) {
            logger.error(TIME_TRAVEL);
            activityState.lastActivity = now;
            writeActivityState();
            return;
        }

        // new session
        if (lastInterval > SESSION_INTERVAL) {
            activityState.sessionCount++;
            activityState.createdAt = now;
            activityState.lastInterval = lastInterval;

            transferSessionPackage();
            activityState.resetSessionAttributes(now);
            writeActivityState();
            return;
        }

        // new subsession
        if (lastInterval > SUBSESSION_INTERVAL) {
            activityState.subsessionCount++;
            activityState.sessionLength += lastInterval;
            activityState.lastActivity = now;
            writeActivityState();
            logger.info("Started subsession %d of session %d",
                    activityState.subsessionCount,
                    activityState.sessionCount);
        }
    }

    private void checkAttributionState() {
        // if there is no attribution saved, or there is one being asked
        if (attribution == null || activityState.askingAttribution) {
            // and it was triggered the first time as a subsession
            // (prevents sessions that did not save the attribution due to an interruption)
            if (shouldGetAttribution) {
                getAttributionHandler().getAttribution();
            }
        }
    }

    private void endInternal() {
        packageHandler.pauseSending();
        stopTimer();
        if (updateActivityState(System.currentTimeMillis())) {
            writeActivityState();
        }
    }

    private void trackEventInternal(Event event) {
        if (!checkEvent(event)) return;
        if (!activityState.enabled) return;

        long now = System.currentTimeMillis();

        activityState.createdAt = now;
        activityState.eventCount++;
        updateActivityState(now);

        PackageBuilder eventBuilder = new PackageBuilder(adjustConfig, deviceInfo, activityState);
        ActivityPackage eventPackage = eventBuilder.buildEventPackage(event);
        packageHandler.addPackage(eventPackage);

        if (adjustConfig.eventBufferingEnabled) {
            logger.info("Buffered event %s", eventPackage.getSuffix());
        } else {
            packageHandler.sendFirstPackage();
        }

        writeActivityState();
    }

    private void readOpenUrlInternal(Uri url) {
        if (url == null) {
            return;
        }

        String queryString = url.getQuery();
        if (queryString == null) {
            return;
        }

        Map<String, String> deeplinkParameters = new HashMap<String, String>();
        Attribution deeplinkAttribution = new Attribution();
        boolean hasDeeplink = false;

        String[] queryPairs = queryString.split("&");
        for (String pair : queryPairs) {
            if (readDeeplinkQueryString(pair, deeplinkParameters, deeplinkAttribution)) {
                hasDeeplink = true;
            }
        }

        if (!hasDeeplink) {
            return;
        }

        getAttributionHandler().getAttribution();

        // TODO check if createdAt should be updated in click package

        PackageBuilder builder = new PackageBuilder(adjustConfig, deviceInfo, activityState);
        builder.deeplinkParameters = deeplinkParameters;
        builder.deeplinkAttribution = deeplinkAttribution;
        ActivityPackage clickPackage = builder.buildClickPackage("deeplink");
        packageHandler.sendClickPackage(clickPackage);
    }

    private void finishedTrackingActivityInternal(JSONObject jsonResponse) {
        if (jsonResponse == null) {
            return;
        }

        String deeplink = jsonResponse.optString("deeplink", null);
        launchDeeplinkMain(deeplink);
        getAttributionHandler().checkAttribution(jsonResponse);
    }

    private boolean readDeeplinkQueryString(String queryString,
                                            Map<String, String> deeplinkParameters,
                                            Attribution deeplinkAttribution) {
        String[] pairComponents = queryString.split("=");
        if (pairComponents.length != 2) return false;

        String key = pairComponents[0];
        if (!key.startsWith(ADJUST_PREFIX)) return false;

        String value = pairComponents[1];
        if (value.length() == 0) return false;

        String keyWOutPrefix = key.substring(ADJUST_PREFIX.length());
        if (keyWOutPrefix.length() == 0) return false;

        if (!trySetAttributionDeeplink(deeplinkAttribution, keyWOutPrefix, value)) {
            deeplinkParameters.put(keyWOutPrefix, value);
        }

        return true;
    }

    private boolean trySetAttributionDeeplink(Attribution deeplinkAttribution,
                                              String key,
                                              String value) {
        if (key.equals("tracker")) {
            deeplinkAttribution.trackerName = value;
            return true;
        }

        if (key.equals("campaign")) {
            deeplinkAttribution.campaign = value;
            return true;
        }

        if (key.equals("adgroup")) {
            deeplinkAttribution.adgroup = value;
            return true;
        }

        if (key.equals("creative")) {
            deeplinkAttribution.creative = value;
            return true;
        }

        return false;
    }

    private void launchDeeplinkMain(String deeplink) {
        if (deeplink == null) return;

        Uri location = Uri.parse(deeplink);
        Intent mapIntent = new Intent(Intent.ACTION_VIEW, location);
        mapIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

        // Verify it resolves
        PackageManager packageManager = adjustConfig.context.getPackageManager();
        List<ResolveInfo> activities = packageManager.queryIntentActivities(mapIntent, 0);
        boolean isIntentSafe = activities.size() > 0;

        // Start an activity if it's safe
        if (!isIntentSafe) {
            logger.error("Unable to open deep link (%s)", deeplink);
            return;
        }

        logger.info("Open deep link (%s)", deeplink);
        adjustConfig.context.startActivity(mapIntent);
    }

    private boolean updateActivityState(long now) {
        long lastInterval = now - activityState.lastActivity;
        // ignore late updates
        if (lastInterval > SESSION_INTERVAL) {
            return false;
        }
        activityState.lastActivity = now;

        if (lastInterval < 0) {
            logger.error(TIME_TRAVEL);
        } else {
            activityState.sessionLength += lastInterval;
            activityState.timeSpent += lastInterval;
        }
        return true;
    }

    public static boolean deleteActivityState(Context context) {
        return context.deleteFile(ACTIVITY_STATE_FILENAME);
    }

    public static boolean deleteAttribution(Context context) {
        return context.deleteFile(ATTRIBUTION_FILENAME);
    }

    private void transferSessionPackage() {
        PackageBuilder builder = new PackageBuilder(adjustConfig, deviceInfo, activityState);
        ActivityPackage sessionPackage = builder.buildSessionPackage();
        packageHandler.addPackage(sessionPackage);
        packageHandler.sendFirstPackage();
        shouldGetAttribution = false;
    }

    private void startTimer() {
        if (timer != null) {
            stopTimer();
        }
        timer = Executors.newSingleThreadScheduledExecutor();
        timer.scheduleWithFixedDelay(new Runnable() {
            @Override
            public void run() {
                timerFired();
            }
        }, TIMER_START, TIMER_INTERVAL, TimeUnit.MILLISECONDS);
    }

    private void stopTimer() {
        if (timer != null) {
            timer.shutdown();
        }
    }

    private void timerFired() {
        if (!activityState.enabled) {
            return;
        }

        packageHandler.sendFirstPackage();

        if (updateActivityState(System.currentTimeMillis())) {
            writeActivityState();
        }
    }

    private void readActivityState() {
        activityState = Util.readObject(adjustConfig.context, ACTIVITY_STATE_FILENAME, ACTIVITY_STATE_NAME);
    }

    private void readAttribution() {
        attribution = Util.readObject(adjustConfig.context, ATTRIBUTION_FILENAME, ATTRIBUTION_NAME);
    }

    private void writeActivityState() {
        Util.writeObject(activityState, adjustConfig.context, ACTIVITY_STATE_FILENAME, ACTIVITY_STATE_NAME);
    }

    private void writeAttribution() {
        Util.writeObject(attribution, adjustConfig.context, ATTRIBUTION_FILENAME, ATTRIBUTION_NAME);
    }

    private boolean checkEvent(Event event) {
        if (event == null) {
            logger.error("Event missing");
            return false;
        }
        if (!event.isValid()) return false;

        return true;
    }

    // lazy initialization to prevent null activity state before first session
    private IAttributionHandler getAttributionHandler() {
        if (attributionHandler == null) {
            ActivityPackage attributionPackage = getAttributionPackage();
            attributionHandler = AdjustFactory.getAttributionHandler(this, attributionPackage);
        }
        return attributionHandler;
    }
}