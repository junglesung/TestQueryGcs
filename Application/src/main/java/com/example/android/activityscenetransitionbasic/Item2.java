package com.example.android.activityscenetransitionbasic;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Represents an Item in our application. Each item has an id, people number, attendant number, full size image url and
 * thumbnail url.
 */
public class Item2 {
    private String id;
    private int people;
    private int attendant;
    private String image;

    private static final String LARGE_BASE_URL = "http://testgcsserver.appspot.com.storage.googleapis.com/testgcs/large/";
    private static final String THUMB_BASE_URL = "http://testgcsserver.appspot.com.storage.googleapis.com/testgcs/thumbs/";

    Item2 (String _id, int _people, int _attendant, String _image) {
        id = _id;
        people = _people;
        attendant = _attendant;
        image = _image;
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

    public String getPhotoUrl() {
        return LARGE_BASE_URL + image;
    }

    public String getThumbnailUrl() {
        return THUMB_BASE_URL + image;
    }

    // Android org.json is not as powerful as standard JAVA org.json.
    // There is no parser constructor for class object in JSONObject.
    // So create a transform function.
    public JSONObject toJSONObject() throws JSONException {
        JSONObject j = new JSONObject();
        j.put("id", id);
        j.put("people", people);
        j.put("attendant", attendant);
        j.put("image", image);
        return j;
    }
}