package com.homeutilities;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.RegistryKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.world.World;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class JsonHandler {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path CONFIG_PATH = FabricLoader.getInstance().getConfigDir().resolve("home_translations.json");

    public static JsonObject loadTranslations(MinecraftServer server) {
        if (!CONFIG_PATH.toFile().exists()) {
            createDefaultConfig(CONFIG_PATH.toFile());
            StateSaverAndLoader.resetPlayerState(server);
            return readConfigFile();
        }

        try (FileReader reader = new FileReader(CONFIG_PATH.toFile())) {
            JsonObject config = JsonParser.parseReader(reader).getAsJsonObject();

            if (config.has("en") && config.getAsJsonObject("en").has("version")) {
                String version = config.getAsJsonObject("en").get("version").getAsString();
                if (version.compareTo("1.3") < 0) {
                    createDefaultConfig(CONFIG_PATH.toFile());
                    StateSaverAndLoader.resetPlayerState(server);
                    return readConfigFile();
                }
            } else {
                createDefaultConfig(CONFIG_PATH.toFile());
                StateSaverAndLoader.resetPlayerState(server);
                return readConfigFile();
            }

            return config;
        } catch (IOException e) {
            e.printStackTrace();
            return new JsonObject();
        }
    }

    private static JsonObject readConfigFile() {
        try (FileReader reader = new FileReader(CONFIG_PATH.toFile())) {
            return JsonParser.parseReader(reader).getAsJsonObject();
        } catch (IOException e) {
            e.printStackTrace();
            return new JsonObject();
        }
    }

    private static void createDefaultConfig(File configFile){
        JsonObject defaultConfig = new JsonObject();
        JsonObject en = new JsonObject();

        en.addProperty("sethome_success","Your home has been set!");
        en.addProperty("sethome_limit","Error : You can only have %d homes.");
        en.addProperty("psethome_success","Your public home has been set!");
        en.addProperty("psethome_limit","Error : You can only have %d public homes.");
        en.addProperty("delhome_success","Your home has been deleted!");
        en.addProperty("delhome_failure","Error : The home don't exist.");
        en.addProperty("pdelhome_success","Your public home has been deleted!");
        en.addProperty("pdelhome_failure","Error : The public home don't exist or you're not the owner.");
        en.addProperty("home_success","You have been teleported to your home!");
        en.addProperty("home_failure","Error : The home don't exist.");
        en.addProperty("phome_success","You have been teleported to the public home!");
        en.addProperty("phome_failure","Error : The public home don't exist.");
        en.addProperty("homes_success","Your homes (You can click on them to teleport):");
        en.addProperty("homes_failure","Error : You don't have any home.");
        en.addProperty("phomes_success","Public homes (You can click on them to teleport):");
        en.addProperty("phomes_failure","Error : The server don't have any public home.");
        en.addProperty("sharehome_success","%s wants to share a home with you! To accept it click on this message.");
        en.addProperty("sharehome_failure","Error : The home don't exist.");
        en.addProperty("sharehome_yourself","Error : You can't share with yourself.");
        en.addProperty("accepthome_success","The home has been transferred! Run /homes to find it.");
        en.addProperty("accepthome_failure","Error : The home don't exist.");
        en.addProperty("accepthome_empty","Error : There is no home to accept.");
        en.addProperty("homelanguage_success","HOME language changed!");
        en.addProperty("homelanguage_failure","Error : The language provided is invalid.");
        en.addProperty("homeslimit_success","The new limit of homes has been set!");
        en.addProperty("homeslimit_failure","Error : Please provide a number that is not negative.");
        en.addProperty("phomeslimit_success","The new limit of public homes has been set!");
        en.addProperty("phomeslimit_failure","Error : Please provide a number that is not negative.");
        en.addProperty("version", "1.3");

        defaultConfig.add("en",en);

        try (FileWriter writer = new FileWriter(configFile)){
            GSON.toJson(defaultConfig,writer);
        } catch (IOException e){
            e.printStackTrace();
        }
    }

    public static void addLocation(ServerPlayerEntity player, String name, double x, double y, double z, ServerWorld world){
        PlayerData playerData = StateSaverAndLoader.getPlayerState(player);
        JsonObject locationObject = new JsonObject();
        locationObject.addProperty("x", x);
        locationObject.addProperty("y", y);
        locationObject.addProperty("z", z);
        RegistryKey<World> registryKey = world.getRegistryKey();
        locationObject.addProperty("world", registryKey.getValue().toString());
        playerData.getHomes().add(name,locationObject);
        StateSaverAndLoader.saveState(Objects.requireNonNull(player.getEntityWorld().getServer()));
    }

    public static void addPublicLocation(ServerPlayerEntity player, String name, double x, double y, double z, ServerWorld world){
        PublicData publicData = StateSaverAndLoader.getPublicState(player);
        JsonObject locationObject = new JsonObject();
        locationObject.addProperty("x", x);
        locationObject.addProperty("y", y);
        locationObject.addProperty("z", z);
        RegistryKey<World> registryKey = world.getRegistryKey();
        locationObject.addProperty("world", registryKey.getValue().toString());
        locationObject.addProperty("owner",player.getUuidAsString());
        publicData.getHomes().add(name,locationObject);
        StateSaverAndLoader.saveState(Objects.requireNonNull(player.getEntityWorld().getServer()));
    }

    public static boolean removeLocation(ServerPlayerEntity player, String name){
        PlayerData playerData = StateSaverAndLoader.getPlayerState(player);
        if(playerData.getHomes().isEmpty() || !playerData.getHomes().has(name)){
            return false;
        }
        else{
            playerData.getHomes().remove(name);
            StateSaverAndLoader.saveState(Objects.requireNonNull(player.getEntityWorld().getServer()));
            return true;
        }
    }

    public static boolean removePublicLocation(ServerPlayerEntity player, String name){
        PublicData publicData = StateSaverAndLoader.getPublicState(player);
        if(publicData.getHomes().isEmpty() || !publicData.getHomes().has(name) || !Objects.equals(publicData.getHomes().get(name).getAsJsonObject().get("owner").getAsString(), player.getUuidAsString())){
            return false;
        }
        else{
            publicData.getHomes().remove(name);
            StateSaverAndLoader.saveState(Objects.requireNonNull(player.getEntityWorld().getServer()));
            return true;
        }
    }

    public static JsonObject getLocation(ServerPlayerEntity player, String name){
        PlayerData playerData = StateSaverAndLoader.getPlayerState(player);
        if (playerData.getHomes().isEmpty() || !playerData.getHomes().has(name)){
            return null;
        }
        else{
            return playerData.getHomes().get(name).getAsJsonObject();
        }
    }

    public static JsonObject getPublicLocation(ServerPlayerEntity player, String name){
        PublicData publicData = StateSaverAndLoader.getPublicState(player);
        if (publicData.getHomes().isEmpty() || !publicData.getHomes().has(name)){
            return null;
        }
        else{
            return publicData.getHomes().get(name).getAsJsonObject();
        }
    }

    public static List<String> listLocations(ServerPlayerEntity player){
        PlayerData playerData = StateSaverAndLoader.getPlayerState(player);
        if (playerData.getHomes().isEmpty()){
            return null;
        }
        else{
            return new ArrayList<>(playerData.getHomes().keySet());
        }
    }

    public static List<String> listPublicLocations(ServerPlayerEntity player){
        PublicData publicData = StateSaverAndLoader.getPublicState(player);
        if (publicData.getHomes().isEmpty()){
            return null;
        }
        else{
            return new ArrayList<>(publicData.getHomes().keySet());
        }
    }
}
