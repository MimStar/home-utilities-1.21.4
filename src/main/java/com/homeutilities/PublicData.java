package com.homeutilities;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class PublicData {
    private JsonObject homes = new JsonObject();

    public synchronized JsonObject getHomes() {
        return homes;
    }

    public synchronized void setHomes(String homes) {
        JsonObject parsedHomes = JsonParser.parseString(homes).getAsJsonObject();
        if (parsedHomes != null) {
            this.homes = parsedHomes;
        }
    }

    public synchronized String toString() {
        return homes.toString();
    }
}