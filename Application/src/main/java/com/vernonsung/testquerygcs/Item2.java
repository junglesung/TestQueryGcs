package com.vernonsung.testquerygcs;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Arrays;


public class Item2 {
    public class ItemMember {
        // Property names must be lowercase because they are received and sent in JSON format
        private String userkey;      // Datastore ID of an user
        private int    attendant;    // How many the user attends
        private String phonenumber;  // User's phone number
        private String skypeid;      // User's Skype ID

        // Default constructor
        public ItemMember() {
        }

        // Constructor
        public ItemMember(String userkey, int attendant, String phonenumber, String skypeid) {
            this.userkey = userkey;
            this.attendant = attendant;
            this.phonenumber = phonenumber;
            this.skypeid = skypeid;
        }

        public String getUserkey() {
            return userkey;
        }

        public void setUserkey(String userkey) {
            this.userkey = userkey;
        }

        public int getAttendant() {
            return attendant;
        }

        public void setAttendant(int attendant) {
            this.attendant = attendant;
        }

        public String getPhonenumber() {
            return phonenumber;
        }

        public void setPhonenumber(String phonenumber) {
            this.phonenumber = phonenumber;
        }

        public String getSkypeid() {
            return skypeid;
        }

        public void setSkypeid(String skypeid) {
            this.skypeid = skypeid;
        }

        @Override
        public String toString() {
            return "ItemMember{" +
                    "userkey='" + userkey + '\'' +
                    ", attendant=" + attendant +
                    ", phonenumber='" + phonenumber + '\'' +
                    ", skypeid='" + skypeid + '\'' +
                    '}';
        }
    }

    // Property names must be lowercase because they are received and sent in JSON format
    private String id;               // Datastore ID of item kind
    private String image;            // Google Cloud Storage file URL
    private String thumbnail;        // Google Cloud Storage file URL
    private int people;              // Satisfied people number
    private int attendant;           // Delta people number
    private double latitude;         // Format "[+-]DDD.DDDDD"
    private double longitude;        // Format "[+-]DDD.DDDDD"
    private String createtime;       // RCF 3339 format "yyyy-MM-dd'T'HH:mm:ss.SSSSSSZ"
    private ItemMember members[];    // The users who attended this item
    private String gcmgroupname;  // Google Cloud Messaging unique group ID
    private String gcmgroupkey;  // Google Cloud Messaging unique group ID

    // Default constructor
    Item2() {
        //
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getImage() {
        return image;
    }

    public void setImage(String image) {
        this.image = image;
    }

    public String getThumbnail() {
        return thumbnail;
    }

    public void setThumbnail(String thumbnail) {
        this.thumbnail = thumbnail;
    }

    public int getPeople() {
        return people;
    }

    public void setPeople(int people) {
        this.people = people;
    }

    public int getAttendant() {
        return attendant;
    }

    public void setAttendant(int attendant) {
        this.attendant = attendant;
    }

    public double getLatitude() {
        return latitude;
    }

    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }

    public String getCreatetime() {
        return createtime;
    }

    public void setCreatetime(String createtime) {
        this.createtime = createtime;
    }

    public ItemMember[] getMembers() {
        return members;
    }

    public void setMembers(ItemMember[] members) {
        this.members = members;
    }

    public String getGcmgroupname() {
        return gcmgroupname;
    }

    public void setGcmgroupname(String gcmgroupname) {
        this.gcmgroupname = gcmgroupname;
    }

    public String getGcmgroupkey() {
        return gcmgroupkey;
    }

    public void setGcmgroupkey(String gcmgroupkey) {
        this.gcmgroupkey = gcmgroupkey;
    }

    // Android org.json is not as powerful as standard JAVA org.json.
    // There is no parser constructor for class object in JSONObject.
    // So create a transform function.
    public JSONObject toJSONObject() throws JSONException {
        JSONObject j = new JSONObject();
        // ID, CreateTime, NotificationKey are determined by server. So just put other properties.
        j.put("image", image);
        j.put("thumbnail", thumbnail);
        j.put("people", people);
        j.put("attendant", attendant);
        j.put("latitude", latitude);
        j.put("longitude", longitude);
        JSONObject m = new JSONObject();
        m.put("attendant", members[0].attendant);
        m.put("phonenumber", members[0].phonenumber);
        m.put("skypeid", members[0].skypeid);
        JSONArray a = new JSONArray();
        a.put(m);
        j.put("members", a);
        // Never send CreateTime to the server because it's determined by the server.
        return j;
    }

    @Override
    public String toString() {
        return "Item2{" +
                "id='" + id + '\'' +
                ", image='" + image + '\'' +
                ", thumbnail='" + thumbnail + '\'' +
                ", people=" + people +
                ", attendant=" + attendant +
                ", latitude=" + latitude +
                ", longitude=" + longitude +
                ", createtime='" + createtime + '\'' +
                ", members=" + Arrays.toString(members) +
                ", gcmgroupname='" + gcmgroupname + '\'' +
                ", gcmgroupkey='" + gcmgroupkey + '\'' +
                '}';
    }
}
