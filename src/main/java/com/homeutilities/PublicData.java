package com.homeutilities;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

public class PublicData {
    private JsonObject homes;

    public PublicData() {
        homes = new JsonObject();
    }

    public PublicData(String homes) {
        setHomes(homes);
    }

    public synchronized JsonObject getHomes() {
        return homes;
    }

    public synchronized void setHomes(String homes) {
        JsonObject parsedHomes = JsonParser.parseString(homes).getAsJsonObject();
        if (parsedHomes != null) {
            this.homes = parsedHomes;
        }
    }

    public static final Codec<PublicData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.STRING.fieldOf("homes").forGetter(PublicData::toString)
    ).apply(instance, PublicData::new));

    public synchronized String toString() {
        return homes.toString();
    }
}