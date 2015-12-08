package com.vernonsung.testquerygcs;

// Send to APP server to update registration token
// Since it's going to convert to JSON format with GSON library parser, use lowercase property names.
public class UserRegistration {
    private String instanceid;
    private String registrationtoken;

    // Default constructor
    public UserRegistration() {
    }

    // Constructor
    public UserRegistration(String instanceid,
                            String registrationtoken) {
        this.instanceid = instanceid;
        this.registrationtoken = registrationtoken;
    }

    // Getter and setter
    public String getRegistrationtoken() {
        return registrationtoken;
    }

    public void setRegistrationtoken(String registrationtoken) {
        this.registrationtoken = registrationtoken;
    }

    public String getInstanceid() {
        return instanceid;
    }

    public void setInstanceid(String instanceid) {
        this.instanceid = instanceid;
    }

    @Override
    public String toString() {
        return "UserRegistration{" +
                "instanceid='" + instanceid + '\'' +
                ", registrationtoken='" + registrationtoken + '\'' +
                '}';
    }
}
