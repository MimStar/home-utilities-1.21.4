package com.homeutilities;

import com.google.gson.JsonObject;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.fabricmc.api.ModInitializer;

import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.block.Blocks;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.ClickEvent;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.TeleportTarget;
import net.minecraft.util.Identifier;
import net.minecraft.world.World;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import me.lucko.fabric.api.permissions.v0.Permissions;

import javax.swing.plaf.nimbus.State;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class HomeUtilities implements ModInitializer {
	private static final String MOD_ID = "home-utilities";
	private static final ConcurrentHashMap<UUID, LinkedList<JsonObject>> shareHomeMap = new ConcurrentHashMap<>();
	private static JsonObject translations = new JsonObject();
	// This logger is used to write text to the console and the log file.
	// It is considered best practice to use your mod id as the logger's name.
	// That way, it's clear which mod wrote info, warnings, and errors.
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	public static String getMOD_ID(){
		return MOD_ID;
	}

	public static Set<String> listTranslations(){
		return translations.keySet();
	}

	public static String getTranslation(String language, String sentence){
		if (!translations.has(language)) return "(Error : there is a problem in the tpa_translations.json, please delete the file and restart the server or correct your translation)";
		return translations.get(language).getAsJsonObject().get(sentence).getAsString();
	}

	@Override
	public void onInitialize() {
		// This code runs as soon as Minecraft is in a mod-load-ready state.
		// However, some things (like resources) may still be uninitialized.
		// Proceed with mild caution.

		CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
			dispatcher.register(CommandManager.literal("sethome")
					.requires(Permissions.require("homeutilities.command.sethome", true))
					.then(CommandManager.argument("name", StringArgumentType.string())
							.executes(this::sethomeExecute)));
		});
		CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
			dispatcher.register(CommandManager.literal("delhome")
					.requires(Permissions.require("homeutilities.command.delhome", true))
					.then(CommandManager.argument("name", StringArgumentType.string())
							.suggests(new HomesSuggestionProvider())
								.executes(this::delhomeExecute)));
		});
		CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
			dispatcher.register(CommandManager.literal("home")
					.requires(Permissions.require("homeutilities.command.home", true))
					.then(CommandManager.argument("name", StringArgumentType.string())
							.suggests(new HomesSuggestionProvider())
								.executes(this::homeExecute)));
		});
		CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
			dispatcher.register(CommandManager.literal("homes")
					.requires(Permissions.require("homeutilities.command.homes", true))
					.executes(this::homesExecute));
		});
		CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
			dispatcher.register(CommandManager.literal("sharehome")
					.requires(Permissions.require("homeutilities.command.sharehome", true))
					.then(CommandManager.argument("name", StringArgumentType.string())
							.suggests(new HomesSuggestionProvider())
								.then(CommandManager.argument("player", EntityArgumentType.player())
									.executes(this::sharehomeExecute))));
		});
		CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
			dispatcher.register(CommandManager.literal("accepthome")
					.requires(Permissions.require("homeutilities.command.accepthome", true))
					.then(CommandManager.argument("name", StringArgumentType.string())
							.executes(this::accepthomeExecute)));
		});
		CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
			dispatcher.register(CommandManager.literal("psethome")
					.requires(Permissions.require("homeutilities.command.psethome", true))
					.then(CommandManager.argument("name", StringArgumentType.string())
							.executes(this::psethomeExecute)));
		});
		CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
			dispatcher.register(CommandManager.literal("pdelhome")
					.requires(Permissions.require("homeutilities.command.pdelhome", true))
					.then(CommandManager.argument("name", StringArgumentType.string())
							.suggests(new PublicHomesSuggestionProvider())
							.executes(this::pdelhomeExecute)));
		});
		CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
			dispatcher.register(CommandManager.literal("phome")
					.requires(Permissions.require("homeutilities.command.phome", true))
					.then(CommandManager.argument("name", StringArgumentType.string())
							.suggests(new PublicHomesSuggestionProvider())
							.executes(this::phomeExecute)));
		});
		CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
			dispatcher.register(CommandManager.literal("phomes")
					.requires(Permissions.require("homeutilities.command.phomes", true))
					.executes(this::phomesExecute));
		});
		CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
			dispatcher.register(CommandManager.literal("homelanguage")
					.requires(Permissions.require("homeutilities.command.homelanguage", true))
					.then(CommandManager.argument("language", StringArgumentType.string())
							.suggests(new LanguageSuggestionProvider())
							.executes(this::homelanguageExecute)));
		});
		CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
			dispatcher.register(CommandManager.literal("homeslimit")
					.requires(source -> source.hasPermissionLevel(4))
					.then(CommandManager.argument("limit", IntegerArgumentType.integer())
							.executes(this::homeslimitExecute)));
		});
		CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
			dispatcher.register(CommandManager.literal("phomeslimit")
					.requires(source -> source.hasPermissionLevel(4))
					.then(CommandManager.argument("limit", IntegerArgumentType.integer())
							.executes(this::phomeslimitExecute)));
		});
		ServerLifecycleEvents.SERVER_STARTED.register(server -> {
			translations = JsonHandler.loadTranslations(server);
		});
		LOGGER.info("HOME Utilities has been loaded successfully!");
	}

	private int sethomeExecute(CommandContext<ServerCommandSource> context){
		String home_name = StringArgumentType.getString(context, "name");
		ServerPlayerEntity player = context.getSource().getPlayer();
		assert player != null;
		String player_language = StateSaverAndLoader.getPlayerState(player).getLanguage();
		SettingsData settings = StateSaverAndLoader.getSettingsState(player);
		if (StateSaverAndLoader.getPlayerState(player).getHomes().size() >= settings.getHomeslimit()){
			context.getSource().sendFeedback(() -> Text.literal(String.format(getTranslation(player_language,"sethome_limit"),settings.getHomeslimit())).formatted(Formatting.RED), false);
			player.playSoundToPlayer(SoundEvents.ENTITY_VILLAGER_NO, SoundCategory.MASTER, 1.0f, 1.0f);
		}
		else {
			JsonHandler.addLocation(player, home_name, player.getX(), player.getY(), player.getZ(), player.getServerWorld());
			context.getSource().sendFeedback(() -> Text.literal(getTranslation(player_language, "sethome_success")).formatted(Formatting.GREEN), false);
			player.playSoundToPlayer(SoundEvents.ENTITY_PLAYER_LEVELUP, SoundCategory.MASTER, 1.0f, 1.0f);
		}
		return 1;
	}

	private int psethomeExecute(CommandContext<ServerCommandSource> context){
		String home_name = StringArgumentType.getString(context, "name");
		ServerPlayerEntity player = context.getSource().getPlayer();
		assert player != null;
		String player_language = StateSaverAndLoader.getPlayerState(player).getLanguage();
		SettingsData settings = StateSaverAndLoader.getSettingsState(player);
		List<String> phomes = JsonHandler.listPublicLocations(player);
		int number_of_phomes = 0;
		if (phomes != null) {
			for (String phomeName : phomes) {
				JsonObject phome = JsonHandler.getPublicLocation(player, phomeName);
				if (phome != null && Objects.equals(phome.get("owner").getAsString(), player.getUuidAsString())) {
					number_of_phomes++;
				}
			}
		}

		if (number_of_phomes >= settings.getPhomeslimit()){
			context.getSource().sendFeedback(() -> Text.literal(String.format(getTranslation(player_language,"psethome_limit"),settings.getPhomeslimit())).formatted(Formatting.RED), false);
			player.playSoundToPlayer(SoundEvents.ENTITY_VILLAGER_NO, SoundCategory.MASTER, 1.0f, 1.0f);
		}
		else {
			String home_finalname = player.getName().getString() + "-" + home_name;
			JsonHandler.addPublicLocation(player, home_finalname, player.getX(), player.getY(), player.getZ(), player.getServerWorld());
			context.getSource().sendFeedback(() -> Text.literal(getTranslation(player_language, "psethome_success")).formatted(Formatting.GREEN), false);
			player.playSoundToPlayer(SoundEvents.ENTITY_PLAYER_LEVELUP, SoundCategory.MASTER, 1.0f, 1.0f);
		}
		return 1;
	}

	private int delhomeExecute(CommandContext<ServerCommandSource> context){
		String home_name = StringArgumentType.getString(context, "name");
		ServerPlayerEntity player = context.getSource().getPlayer();
		assert player != null;
		String player_language = StateSaverAndLoader.getPlayerState(player).getLanguage();
		if (JsonHandler.removeLocation(player, home_name)){
			context.getSource().sendFeedback(() -> Text.literal(getTranslation(player_language,"delhome_success")).formatted(Formatting.GREEN), false);
			player.playSoundToPlayer(SoundEvents.ENTITY_PLAYER_LEVELUP, SoundCategory.MASTER, 1.0f, 1.0f);
		}
		else{
			context.getSource().sendFeedback(() -> Text.literal(getTranslation(player_language,"delhome_failure")).formatted(Formatting.RED), false);
			player.playSoundToPlayer(SoundEvents.ENTITY_VILLAGER_NO, SoundCategory.MASTER, 1.0f, 1.0f);
		}
		return 1;
	}

	private int pdelhomeExecute(CommandContext<ServerCommandSource> context){
		String home_name = StringArgumentType.getString(context, "name");
		ServerPlayerEntity player = context.getSource().getPlayer();
		assert player != null;
		String player_language = StateSaverAndLoader.getPlayerState(player).getLanguage();
		if (JsonHandler.removePublicLocation(player, home_name)){
			context.getSource().sendFeedback(() -> Text.literal(getTranslation(player_language,"pdelhome_success")).formatted(Formatting.GREEN), false);
			player.playSoundToPlayer(SoundEvents.ENTITY_PLAYER_LEVELUP, SoundCategory.MASTER, 1.0f, 1.0f);
		}
		else{
			context.getSource().sendFeedback(() -> Text.literal(getTranslation(player_language,"pdelhome_failure")).formatted(Formatting.RED), false);
			player.playSoundToPlayer(SoundEvents.ENTITY_VILLAGER_NO, SoundCategory.MASTER, 1.0f, 1.0f);
		}
		return 1;
	}

	private int homeExecute(CommandContext<ServerCommandSource> context){
		String home_name = StringArgumentType.getString(context, "name");
		ServerPlayerEntity player = context.getSource().getPlayer();
		assert player != null;
		String player_language = StateSaverAndLoader.getPlayerState(player).getLanguage();
		JsonObject location = JsonHandler.getLocation(player,home_name);
		if (location != null){
			Vec3d pos = new Vec3d(location.get("x").getAsDouble(),location.get("y").getAsDouble(),location.get("z").getAsDouble());
			Identifier identifier = Identifier.of(location.get("world").getAsString());
			RegistryKey<World> worldRegistryKey = RegistryKey.of(RegistryKeys.WORLD,identifier);
			ServerWorld world = context.getSource().getServer().getWorld(worldRegistryKey);
			assert world != null;
			TeleportTarget teleport_target = new TeleportTarget(world, pos, player.getVelocity(),player.getYaw(),player.getPitch(),TeleportTarget.ADD_PORTAL_CHUNK_TICKET);
			player.teleportTo(teleport_target);
			context.getSource().sendFeedback(() -> Text.literal(getTranslation(player_language,"home_success")).formatted(Formatting.GREEN), false);
		}
		else{
			context.getSource().sendFeedback(() -> Text.literal(getTranslation(player_language,"home_failure")).formatted(Formatting.RED), false);
			player.playSoundToPlayer(SoundEvents.ENTITY_VILLAGER_NO, SoundCategory.MASTER, 1.0f, 1.0f);
		}
		return 1;
	}

	private int phomeExecute(CommandContext<ServerCommandSource> context){
		String home_name = StringArgumentType.getString(context, "name");
		ServerPlayerEntity player = context.getSource().getPlayer();
		assert player != null;
		String player_language = StateSaverAndLoader.getPlayerState(player).getLanguage();
		JsonObject location = JsonHandler.getPublicLocation(player,home_name);
		if (location != null){
			Vec3d pos = new Vec3d(location.get("x").getAsDouble(),location.get("y").getAsDouble(),location.get("z").getAsDouble());
			Identifier identifier = Identifier.of(location.get("world").getAsString());
			RegistryKey<World> worldRegistryKey = RegistryKey.of(RegistryKeys.WORLD,identifier);
			ServerWorld world = context.getSource().getServer().getWorld(worldRegistryKey);
			assert world != null;
			TeleportTarget teleport_target = new TeleportTarget(world, pos, player.getVelocity(),player.getYaw(),player.getPitch(),TeleportTarget.ADD_PORTAL_CHUNK_TICKET);
			player.teleportTo(teleport_target);
			context.getSource().sendFeedback(() -> Text.literal(getTranslation(player_language,"phome_success")).formatted(Formatting.GREEN), false);
		}
		else{
			context.getSource().sendFeedback(() -> Text.literal(getTranslation(player_language,"phome_failure")).formatted(Formatting.RED), false);
			player.playSoundToPlayer(SoundEvents.ENTITY_VILLAGER_NO, SoundCategory.MASTER, 1.0f, 1.0f);
		}
		return 1;
	}

	private int homesExecute(CommandContext<ServerCommandSource> context){
		ServerPlayerEntity player = context.getSource().getPlayer();
		assert player != null;
		String player_language = StateSaverAndLoader.getPlayerState(player).getLanguage();
		List<String> homesList = JsonHandler.listLocations(player);
        if (homesList == null){
			context.getSource().sendFeedback(() -> Text.literal(getTranslation(player_language,"homes_failure")).formatted(Formatting.RED), false);
			player.playSoundToPlayer(SoundEvents.ENTITY_VILLAGER_NO, SoundCategory.MASTER, 1.0f, 1.0f);
		}
		else{
			context.getSource().sendFeedback(() -> Text.literal(getTranslation(player_language,"homes_success")).formatted(Formatting.DARK_GREEN), false);
			for (String home : homesList){
				context.getSource().sendFeedback(() -> Text.literal("- " + home).formatted(Formatting.GOLD).styled(style -> style.withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/home " + home))), false);
			}
		}
		return 1;
	}

	private int phomesExecute(CommandContext<ServerCommandSource> context){
		ServerPlayerEntity player = context.getSource().getPlayer();
		assert player != null;
		String player_language = StateSaverAndLoader.getPlayerState(player).getLanguage();
		List<String> homesList = JsonHandler.listPublicLocations(player);
		if (homesList == null){
			context.getSource().sendFeedback(() -> Text.literal(getTranslation(player_language,"phomes_failure")).formatted(Formatting.RED), false);
			player.playSoundToPlayer(SoundEvents.ENTITY_VILLAGER_NO, SoundCategory.MASTER, 1.0f, 1.0f);
		}
		else{
			context.getSource().sendFeedback(() -> Text.literal(getTranslation(player_language,"phomes_success")).formatted(Formatting.DARK_GREEN), false);
			for (String home : homesList){
				context.getSource().sendFeedback(() -> Text.literal("- " + home).formatted(Formatting.GOLD).styled(style -> style.withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/phome " + home))), false);
			}
		}
		return 1;
	}

	private int sharehomeExecute(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
		String home_name = StringArgumentType.getString(context, "name");
		ServerPlayerEntity player_target = EntityArgumentType.getPlayer(context, "player");
		ServerPlayerEntity player = context.getSource().getPlayer();
		assert player_target != null;
		assert player != null;
		String player_language = StateSaverAndLoader.getPlayerState(player).getLanguage();
		String target_language = StateSaverAndLoader.getPlayerState(player_target).getLanguage();
		if (player_target.getName().getString().equals(player.getName().getString())){
			context.getSource().sendFeedback(() -> Text.literal(getTranslation(player_language,"sharehome_yourself")).formatted(Formatting.RED), false);
			player.playSoundToPlayer(SoundEvents.ENTITY_VILLAGER_NO, SoundCategory.MASTER, 1.0f, 1.0f);
			return 1;
		}
		JsonObject location = JsonHandler.getLocation(player, home_name);
		if (location == null){
			context.getSource().sendFeedback(() -> Text.literal(getTranslation(player_language,"sharehome_failure")).formatted(Formatting.RED), false);
			player.playSoundToPlayer(SoundEvents.ENTITY_VILLAGER_NO, SoundCategory.MASTER, 1.0f, 1.0f);
		}
		else{
			String home_finalname = player.getName().getString() + "-" + home_name;
			JsonObject new_location = new JsonObject();
			location.addProperty("name",home_finalname);
			location.addProperty("x", location.get("x").getAsDouble());
			location.addProperty("y", location.get("y").getAsDouble());
			location.addProperty("z", location.get("z").getAsDouble());
			location.addProperty("world", location.get("world").getAsString());
			if (shareHomeMap.containsKey(player_target.getUuid())){
                shareHomeMap.get(player_target.getUuid()).removeIf(check -> check.has("name") && check.get("name").getAsString().equals(home_finalname));
				shareHomeMap.get(player_target.getUuid()).add(location);
			}
			else{
				LinkedList<JsonObject> new_list = new LinkedList<>();
				new_list.add(location);
				shareHomeMap.put(player_target.getUuid(), new_list);
			}
			player_target.sendMessage(Text.literal(String.format(getTranslation(target_language,"sharehome_success"), player.getName().getString())).formatted(Formatting.GOLD).styled(style -> style.withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/accepthome " + home_finalname))));
		}
		return 1;
	}

	private int accepthomeExecute(CommandContext<ServerCommandSource> context){
		String home_name = StringArgumentType.getString(context, "name");
		ServerPlayerEntity player = context.getSource().getPlayer();
        assert player != null;
		String player_language = StateSaverAndLoader.getPlayerState(player).getLanguage();
        if (shareHomeMap.containsKey(player.getUuid())) {
			for (JsonObject location : shareHomeMap.get(player.getUuid())){
				if (location.has("name") && location.get("name").getAsString().equals(home_name)){
					Identifier identifier = Identifier.of(location.get("world").getAsString());
					RegistryKey<World> worldRegistryKey = RegistryKey.of(RegistryKeys.WORLD,identifier);
					ServerWorld world = context.getSource().getServer().getWorld(worldRegistryKey);
                    assert world != null;
                    JsonHandler.addLocation(player, location.get("name").getAsString(), location.get("x").getAsDouble(), location.get("y").getAsDouble(), location.get("z").getAsDouble(), world);
					if (shareHomeMap.get(player.getUuid()).isEmpty()) {
						shareHomeMap.remove(player.getUuid());
					}
					shareHomeMap.get(player.getUuid()).remove(location);
					context.getSource().sendFeedback(() -> Text.literal(getTranslation(player_language,"accepthome_success")).formatted(Formatting.GREEN).styled(style -> style.withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/homes"))), false);
					player.playSoundToPlayer(SoundEvents.ENTITY_PLAYER_LEVELUP, SoundCategory.MASTER, 1.0f, 1.0f);
					return 1;
				}
			}
			context.getSource().sendFeedback(() -> Text.literal(getTranslation(player_language,"accepthome_failure")).formatted(Formatting.RED), false);
			player.playSoundToPlayer(SoundEvents.ENTITY_VILLAGER_NO, SoundCategory.MASTER, 1.0f, 1.0f);
		}
		else{
			context.getSource().sendFeedback(() -> Text.literal(getTranslation(player_language,"accepthome_empty")).formatted(Formatting.RED), false);
			player.playSoundToPlayer(SoundEvents.ENTITY_VILLAGER_NO, SoundCategory.MASTER, 1.0f, 1.0f);
		}
		return 1;
	}

	private int homelanguageExecute(CommandContext<ServerCommandSource> context){
		PlayerData playerData = StateSaverAndLoader.getPlayerState(Objects.requireNonNull(context.getSource().getPlayer()));
		String new_language = StringArgumentType.getString(context,"language");
		if (translations.has(new_language)){
			playerData.setLanguage(new_language);
			StateSaverAndLoader.saveState(Objects.requireNonNull(context.getSource().getServer()));
			context.getSource().sendFeedback(() -> Text.literal(getTranslation(playerData.getLanguage(),"homelanguage_success")).formatted(Formatting.GREEN), false);
			context.getSource().getPlayer().playSoundToPlayer(SoundEvents.ENTITY_PLAYER_LEVELUP, SoundCategory.MASTER, 1.0f, 1.0f);
		}
		else{
			context.getSource().sendFeedback(() -> Text.literal(getTranslation(playerData.getLanguage(),"homelanguage_failure")).formatted(Formatting.RED), false);
			context.getSource().getPlayer().playSoundToPlayer(SoundEvents.ENTITY_VILLAGER_NO, SoundCategory.MASTER, 1.0f, 1.0f);
		}
		return 1;
	}

	private int homeslimitExecute(CommandContext<ServerCommandSource> context){
		int limit = IntegerArgumentType.getInteger(context, "limit");
		ServerPlayerEntity player = context.getSource().getPlayer();
		assert player != null;
		String player_language = StateSaverAndLoader.getPlayerState(player).getLanguage();
		if (limit >= 0){
			SettingsData settings = StateSaverAndLoader.getSettingsState(Objects.requireNonNull(context.getSource().getPlayer()));
			settings.setHomeslimit(limit);
			StateSaverAndLoader.saveState(Objects.requireNonNull(context.getSource().getServer()));
			context.getSource().sendFeedback(() -> Text.literal(getTranslation(player_language,"homeslimit_success")).formatted(Formatting.GREEN), false);
			player.playSoundToPlayer(SoundEvents.ENTITY_PLAYER_LEVELUP, SoundCategory.MASTER, 1.0f, 1.0f);
		}
		else{
			context.getSource().sendFeedback(() -> Text.literal(getTranslation(player_language,"homeslimit_failure")).formatted(Formatting.RED), false);
			player.playSoundToPlayer(SoundEvents.ENTITY_VILLAGER_NO, SoundCategory.MASTER, 1.0f, 1.0f);
		}
		return 1;
	}

	private int phomeslimitExecute(CommandContext<ServerCommandSource> context){
		int limit = IntegerArgumentType.getInteger(context, "limit");
		ServerPlayerEntity player = context.getSource().getPlayer();
		assert player != null;
		String player_language = StateSaverAndLoader.getPlayerState(player).getLanguage();
		if (limit >= 0){
			SettingsData settings = StateSaverAndLoader.getSettingsState(Objects.requireNonNull(context.getSource().getPlayer()));
			settings.setPhomeslimit(limit);
			StateSaverAndLoader.saveState(Objects.requireNonNull(context.getSource().getServer()));
			context.getSource().sendFeedback(() -> Text.literal(getTranslation(player_language,"phomeslimit_success")).formatted(Formatting.GREEN), false);
			player.playSoundToPlayer(SoundEvents.ENTITY_PLAYER_LEVELUP, SoundCategory.MASTER, 1.0f, 1.0f);
		}
		else{
			context.getSource().sendFeedback(() -> Text.literal(getTranslation(player_language,"phomeslimit_failure")).formatted(Formatting.RED), false);
			player.playSoundToPlayer(SoundEvents.ENTITY_VILLAGER_NO, SoundCategory.MASTER, 1.0f, 1.0f);
		}
		return 1;
	}

}