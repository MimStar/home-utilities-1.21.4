package com.homeutilities;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

public class SettingsData {
    private int homeslimit;
    private int phomeslimit;

    public SettingsData(){
        homeslimit = 10;
        phomeslimit = 10;
    }

    public SettingsData(int homeslimit, int phomeslimit){
        this.homeslimit = homeslimit;
        this.phomeslimit = phomeslimit;
    }

    public int getHomeslimit() {
        return homeslimit;
    }

    public int getPhomeslimit() {
        return phomeslimit;
    }

    public void setHomeslimit(int homeslimit) {
        this.homeslimit = homeslimit;
    }

    public void setPhomeslimit(int phomeslimit) {
        this.phomeslimit = phomeslimit;
    }

    public static final Codec<SettingsData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.INT.fieldOf("homeslimit").forGetter(SettingsData::getHomeslimit),
            Codec.INT.fieldOf("phomeslimit").forGetter(SettingsData::getPhomeslimit)
    ).apply(instance, SettingsData::new));
}
