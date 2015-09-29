package com.example.android.activityscenetransitionbasic;

import org.json.JSONException;
import org.json.JSONObject;

public class Item2 {
    private String id;
    private int people;
    private int attendant;
    private String image;
    private String createtime; // RCF 3339 format "yyyy-MM-dd'T'HH:mm:ss.SSSSSSZ"
    private String location; // latitude,longitude in format "[+-]DDD.DDDDD"

    private static final String LARGE_BASE_URL = "http://testgcsserver.appspot.com.storage.googleapis.com/testgcs/large/";
    private static final String THUMB_BASE_URL = "http://testgcsserver.appspot.com.storage.googleapis.com/testgcs/thumbs/";

    // Default constructor
    Item2() {
        //
    }

    // Constructor
    Item2 (String _id,
           int _people,
           int _attendant,
           String _image,
           String _createTime,
           String _location) {
        id = _id;
        people = _people;
        attendant = _attendant;
        image = _image;
        createtime = _createTime;
        location = _location;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
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

    public String getImage() {
        return image;
    }

    public void setImage(String image) {
        this.image = image;
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
        j.put("people", people);
        j.put("attendant", attendant);
        j.put("image", image);
        // Never send CreateTime to the server because it's determined by the server.
        return j;
    }

    @Override
    public String toString() {
        return "Item2{" +
                "id='" + id + '\'' +
                ", people=" + people +
                ", attendant=" + attendant +
                ", image='" + image + '\'' +
                ", createtime=" + createtime +
                '}';
    }
}
