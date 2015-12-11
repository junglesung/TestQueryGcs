package com.vernonsung.testquerygcs;

import org.json.JSONException;
import org.json.JSONObject;

public class Item2 {
    private String id;          // Datastore ID of item kind
    private String image;       // Google Cloud Storage file URL
    private int people;         // Satisfied people number
    private int attendant;      // Delta people number
    private double latitude;    // Format "[+-]DDD.DDDDD"
    private double longitude;   // Format "[+-]DDD.DDDDD"
    private String createtime;  // RCF 3339 format "yyyy-MM-dd'T'HH:mm:ss.SSSSSSZ"

    private static final String LARGE_BASE_URL = "http://aliza-1148.appspot.com.storage.googleapis.com/testgcs/large/";
    private static final String THUMB_BASE_URL = "http://aliza-1148.appspot.com.storage.googleapis.com/testgcs/thumbs/";

    // Default constructor
    Item2() {
        //
    }

    // Constructor
    Item2 (String _id,
           String _image,
           int _people,
           int _attendant,
           double _latitude,
           double _longitude,
           String _createTime) {
        id = _id;
        image = _image;
        people = _people;
        attendant = _attendant;
        latitude = _latitude;
        longitude = _longitude;
        createtime = _createTime;
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

    public String getPhotoUrl() {
//        return LARGE_BASE_URL + image;
        return image;
    }

    public String getThumbnailUrl() {
//        return THUMB_BASE_URL + image;
        return image;
    }

    // Android org.json is not as powerful as standard JAVA org.json.
    // There is no parser constructor for class object in JSONObject.
    // So create a transform function.
    public JSONObject toJSONObject() throws JSONException {
        JSONObject j = new JSONObject();
        // ID and CreateTime are determined by server. So just put other properties.
        j.put("id", id);
        j.put("image", image);
        j.put("people", people);
        j.put("attendant", attendant);
        j.put("latitude", latitude);
        j.put("longitude", longitude);
        // Never send CreateTime to the server because it's determined by the server.
        return j;
    }

    @Override
    public String toString() {
        return "Item2{" +
                "id='" + id + '\'' +
                ", image='" + image + '\'' +
                ", people=" + people +
                ", attendant=" + attendant +
                ", latitude=" + latitude +
                ", longitude=" + longitude +
                ", createtime=" + createtime +
                '}';
    }
}
