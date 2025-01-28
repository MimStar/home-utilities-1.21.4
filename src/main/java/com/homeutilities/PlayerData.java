package com.homeutilities;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class PlayerData {
    private JsonObject homes = new JsonObject();
    private String language = "en";

    public JsonObject getHomes() {
        return homes;
    }

    public String getLanguage() {
        return language;
    }

    public void setLanguage(String language) {
        this.language = language;
    }

    public void setHomes(String homes) {
        JsonObject parsedHomes = JsonParser.parseString(homes).getAsJsonObject();
        if (parsedHomes != null) {
            this.homes = parsedHomes;
        }
    }

    public String toString(){
        return homes.toString();
    }
}
