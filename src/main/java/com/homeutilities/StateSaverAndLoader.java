package com.homeutilities;

import com.google.gson.JsonObject;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.entity.LivingEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.PersistentState;
import net.minecraft.world.PersistentStateManager;
import net.minecraft.world.PersistentStateType;
import net.minecraft.world.World;

import javax.swing.plaf.nimbus.State;
import java.util.*;

public class StateSaverAndLoader extends PersistentState {

    public Map<UUID,PlayerData> players;
    public PublicData publicHomes;

    public StateSaverAndLoader(){
        this.players = new HashMap<>();
        this.publicHomes = new PublicData();
    }

    public StateSaverAndLoader(Map<UUID, PlayerData> players, PublicData publicHomes) {
        this.players = new HashMap<>(players);
        this.publicHomes = publicHomes;
    }
    /*
    @Override
    public NbtCompound writeNbt(NbtCompound nbt){
        NbtCompound playersNbt = new NbtCompound();
        players.forEach(((uuid, playerData) -> {
            NbtCompound playerNbt = new NbtCompound();
            playerNbt.putString("homes",playerData.toString());
            playerNbt.putString("language",playerData.getLanguage());
            playersNbt.put(uuid.toString(),playerNbt);
        }));
        nbt.put("players", playersNbt);
        nbt.putString("publichomes",publicHomes.toString());
        return nbt;
    }

    public static StateSaverAndLoader createFromNbt(NbtCompound tag, RegistryWrapper.WrapperLookup registryLookup) {
        StateSaverAndLoader state = new StateSaverAndLoader();

        NbtCompound playersNbt = tag.getCompoundOrEmpty("players");
        playersNbt.getKeys().forEach(key -> {
            PlayerData playerData = new PlayerData();
            playerData.setHomes(String.valueOf(playersNbt.getCompoundOrEmpty(key).getString("homes")));
            playerData.setLanguage(String.valueOf(playersNbt.getCompoundOrEmpty(key).getString("language")));
            UUID uuid = UUID.fromString(key);
            state.players.put(uuid, playerData);
        });

        // Add null check for publicHomes
        Optional<String> publicHomesString = tag.getString("publichomes");
        if (publicHomesString.isPresent()) {
            state.publicHomes.setHomes(String.valueOf(publicHomesString));
        }

        return state;
    }
    */
    public static final Codec<StateSaverAndLoader> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.unboundedMap(Codec.STRING.xmap(UUID::fromString, UUID::toString),
                    PlayerData.CODEC).fieldOf("players").forGetter(state -> state.players),
            PublicData.CODEC.fieldOf("publicHomes").forGetter(state -> state.publicHomes)
    ).apply(instance, StateSaverAndLoader::new));

    private static final PersistentStateType<StateSaverAndLoader> type = new PersistentStateType<>(
            HomeUtilities.getMOD_ID(),
            StateSaverAndLoader::new,
            CODEC,
            null
    );

    public static StateSaverAndLoader getServerState(MinecraftServer server){
        PersistentStateManager persistentStateManager = Objects.requireNonNull(server.getWorld(World.OVERWORLD)).getPersistentStateManager();

        StateSaverAndLoader state = persistentStateManager.getOrCreate(type);

        state.markDirty();

        return state;
    }

    public static PlayerData getPlayerState(LivingEntity player){
        StateSaverAndLoader serverState = getServerState(Objects.requireNonNull(player.getServer()));
        return serverState.players.computeIfAbsent(player.getUuid(), uuid -> new PlayerData());
    }

    public static void resetPlayerState(MinecraftServer server){
        StateSaverAndLoader serverState = getServerState(server);
        serverState.players.forEach(((uuid, playerData) -> playerData.setLanguage("en")));
        saveState(server);
    }

    public static PublicData getPublicState(LivingEntity player){
        StateSaverAndLoader serverState = getServerState(Objects.requireNonNull(player.getServer()));
        return serverState.publicHomes;
    }

    public static void saveState(MinecraftServer server) {
        PersistentStateManager persistentStateManager = Objects.requireNonNull(server.getWorld(World.OVERWORLD)).getPersistentStateManager();
        persistentStateManager.save();
    }
}
