package com.homeutilities;

import com.google.gson.JsonObject;
import com.mojang.datafixers.util.Either;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.entity.LivingEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtOps;
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
    public SettingsData settings;

    public StateSaverAndLoader(){
        this.players = new HashMap<>();
        this.publicHomes = new PublicData();
        this.settings = new SettingsData();
    }

    public StateSaverAndLoader(Map<UUID, PlayerData> players, PublicData publicHomes, SettingsData settings) {
        this.players = new HashMap<>(players);
        this.publicHomes = publicHomes;
        this.settings = settings;
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

    public static StateSaverAndLoader createFromNbt(NbtCompound tag, RegistryWrapper.WrapperLookup registryLookup) {
        StateSaverAndLoader state = new StateSaverAndLoader();

        NbtCompound playersNbt = tag.getCompound("players").orElse(new NbtCompound());
        playersNbt.getKeys().forEach(key -> {
            PlayerData playerData = new PlayerData();
            playerData.setHomes(playersNbt.getCompound(key).map(nbt -> nbt.getString("homes").orElse("")).orElse(""));
            playerData.setLanguage(playersNbt.getCompound(key).map(nbt -> nbt.getString("language").orElse("")).orElse(""));
            UUID uuid = UUID.fromString(key);
            state.players.put(uuid, playerData);
        });

        // Add null check for publicHomes
        NbtCompound publicHomesCompound = tag.getCompound("publicHomes").orElse(new NbtCompound());
        String publicHomesString = publicHomesCompound.getString("homes").orElse("");
        if (!publicHomesString.isEmpty()) {
            state.publicHomes.setHomes(publicHomesString);
        }

        String settingsString = tag.getString("settings").orElse("");
        if (!settingsString.isEmpty()) {
            state.settings.setSettings(settingsString);
        }

        // state.markDirty();

        return state;
    }

    private static final Codec<StateSaverAndLoader> CODEC =
            RecordCodecBuilder.create(instance -> instance.group(
                    Codec.unboundedMap(
                            Codec.STRING.xmap(UUID::fromString, UUID::toString),
                            PlayerData.CODEC
                    ).fieldOf("players").forGetter(s -> s.players),
                    PublicData.CODEC.fieldOf("publicHomes").forGetter(s -> s.publicHomes),
                    SettingsData.CODEC.fieldOf("settings").forGetter(s -> s.settings)
            ).apply(instance, StateSaverAndLoader::new));

    private static final Codec<StateSaverAndLoader> FALLBACK_CODEC = new Codec<StateSaverAndLoader>() {
        @Override
        public <T> DataResult<T> encode(StateSaverAndLoader input, DynamicOps<T> ops, T prefix) {
            // Always encode using the new format
            return CODEC.encode(input, ops, prefix);
        }

        @Override
        public <T> DataResult<Pair<StateSaverAndLoader, T>> decode(DynamicOps<T> ops, T input) {
            // First try the new CODEC format
            DataResult<Pair<StateSaverAndLoader, T>> newFormatResult = CODEC.decode(ops, input);

            if (newFormatResult.result().isPresent()) {
                return newFormatResult;
            }

            // If new format fails, try to convert from old NBT format
            if (ops instanceof NbtOps && input instanceof NbtCompound nbt) {
                try {
                    StateSaverAndLoader state = createFromNbt(nbt, null);
                    return DataResult.success(Pair.of(state, input));
                } catch (Exception e) {
                    // Return error or fallback to empty state
                    return DataResult.error(() -> "Failed to parse old NBT format: " + e.getMessage());
                }
            }

            // If neither format works, return the original error
            return newFormatResult;
        }
    };

    private static final PersistentStateType<StateSaverAndLoader> type = new PersistentStateType<>(
            HomeUtilities.getMOD_ID(),
            StateSaverAndLoader::new,
            FALLBACK_CODEC, // Use the fallback CODEC instead
            null
    );

    public static StateSaverAndLoader getServerState(MinecraftServer server){
        PersistentStateManager persistentStateManager = Objects.requireNonNull(server.getWorld(World.OVERWORLD)).getPersistentStateManager();

        return persistentStateManager.getOrCreate(type);
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

    public static SettingsData getSettingsState(MinecraftServer server){
        StateSaverAndLoader serverState = getServerState(Objects.requireNonNull(server));
        return serverState.settings;
    }

    public static void saveState(MinecraftServer server) {
        getServerState(server).markDirty();
    }
}
