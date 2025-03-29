package com.homeutilities;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

public class PlayerData {
    private JsonObject homes;
    private String language;

    public PlayerData(){
        homes = new JsonObject();
        language = "en";
    }

    public PlayerData(String homes, String language){
        this.homes = JsonParser.parseString(homes).getAsJsonObject();
        this.language = language;
    }

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

    public static final Codec<PlayerData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.STRING.fieldOf("homes").forGetter(PlayerData::toString),
            Codec.STRING.fieldOf("language").forGetter(PlayerData::getLanguage)
    ).apply(instance, PlayerData::new));
}
