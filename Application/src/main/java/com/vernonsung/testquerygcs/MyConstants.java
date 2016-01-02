package com.vernonsung.testquerygcs;

public class MyConstants {
    // Shared preference
    public static final String REGISTRATION_TOKEN = "registrationToken";  // String. Google Cloud Messaging registration token
    public static final String USER_ID = "userId";  // String. Google APP Engine Datastore user ID
    public static final String SENT_TOKEN_TO_SERVER = "sentTokenToServer";  // boolean. Indicate whether the registration token has been sent to Google APP Engine server
    public static final String PHONE_NUMBER = "phoneNumber";  // String. User input phone number

    // Broadcast intent
    public static final String REGISTRATION_COMPLETE = "registrationComplete";
    public static final String UNREGISTRATION_COMPLETE = "unregistrationComplete";
    public static final String SUBSCRIPTION_COMPLETE = "subscriptionComplete";
    public static final String UNSUBSCRIBING_COMPLETE = "unsubscribingComplete";

    // Intent extra data
    public static final String TOPIC = "topic";

    // Intent action
    public static final String ACTION_GET_TOKEN = "actionGetToken";
    public static final String ACTION_DELETE_TOKEN = "actionDeleteToken";
    public static final String ACTION_SUBSCRIBE_TOPIC = "actionSubscribeTopic";
    public static final String ACTION_UNSUBSCRIBE_TOPIC = "actionUnsubscribeTopic";

    // URL
    public static final String APP_SERVER_URL_BASE = "https://aliza-1148.appspot.com/api/0.1";
    public static final String USER_REGISTRATION_URL = APP_SERVER_URL_BASE + "/myself";
    public static final String USER_MESSAGE_URL = APP_SERVER_URL_BASE + "/user-messages";
    public static final String TOPIC_MESSAGE_URL = APP_SERVER_URL_BASE + "/topic-messages";
    public static final String GROUP_MESSAGE_URL = APP_SERVER_URL_BASE + "/group-messages";
    public static final String GROUP_URL = APP_SERVER_URL_BASE + "/groups";

    // HTTP HEADER
    public static final String HTTP_HEADER_INSTANCE_ID = "Instance-Id";

    // Values
    public static final int URL_CONNECTION_READ_TIMEOUT = 10000;  // milliseconds
    public static final int URL_CONNECTION_CONNECT_TIMEOUT = 15000;  // milliseconds

    // Activity and Fragment saves instance state
    public static final String CREATEITEMACTIVITY_MCURRENTPHOTOURI = "CreateItemActivity_mCurrentPhotoUri";
    public static final String CREATEITEMACTIVITY_MGCSPHOTOURL = "CreateItemActivity_mGcsPhotoUrl";
    public static final String CREATEITEMACTIVITY_MPHONENUMBER = "CreateItemActivity_mPhoneNumber";
    public static final String CREATEITEMACTIVITY_MSCREENORIENTATION = "CreateItemActivity_mScreenOrientation";
    public static final String CREATEITEMACTIVITY_MSTATE = "CreateItemActivity_mState";
}
