package com.homeutilities;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class PlayerData {
    private JsonObject homes = new JsonObject();

    public JsonObject getHomes() {
        return homes;
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
