package com.homeutilities;

import com.google.gson.JsonObject;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.fabricmc.api.ModInitializer;

import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
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

import javax.swing.plaf.nimbus.State;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class HomeUtilities implements ModInitializer {
	private static final String MOD_ID = "home-utilities";
	private static final ConcurrentHashMap<UUID, LinkedList<JsonObject>> shareHomeMap = new ConcurrentHashMap<>();

	// This logger is used to write text to the console and the log file.
	// It is considered best practice to use your mod id as the logger's name.
	// That way, it's clear which mod wrote info, warnings, and errors.
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	public static String getMOD_ID(){
		return MOD_ID;
	}

	@Override
	public void onInitialize() {
		// This code runs as soon as Minecraft is in a mod-load-ready state.
		// However, some things (like resources) may still be uninitialized.
		// Proceed with mild caution.

		CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
			dispatcher.register(CommandManager.literal("sethome")
					.then(CommandManager.argument("name", StringArgumentType.string())
							.executes(this::sethomeExecute)));
		});
		CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
			dispatcher.register(CommandManager.literal("delhome")
					.then(CommandManager.argument("name", StringArgumentType.string())
							.suggests(new HomesSuggestionProvider())
								.executes(this::delhomeExecute)));
		});
		CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
			dispatcher.register(CommandManager.literal("home")
					.then(CommandManager.argument("name", StringArgumentType.string())
							.suggests(new HomesSuggestionProvider())
								.executes(this::homeExecute)));
		});
		CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
			dispatcher.register(CommandManager.literal("homes")
					.executes(this::homesExecute));
		});
		CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
			dispatcher.register(CommandManager.literal("sharehome")
					.then(CommandManager.argument("name", StringArgumentType.string())
							.suggests(new HomesSuggestionProvider())
								.then(CommandManager.argument("player", EntityArgumentType.player())
									.executes(this::sharehomeExecute))));
		});
		CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
			dispatcher.register(CommandManager.literal("accepthome")
					.then(CommandManager.argument("name", StringArgumentType.string())
							.executes(this::accepthomeExecute)));
		});
		CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
			dispatcher.register(CommandManager.literal("psethome")
					.then(CommandManager.argument("name", StringArgumentType.string())
							.executes(this::psethomeExecute)));
		});
		CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
			dispatcher.register(CommandManager.literal("pdelhome")
					.then(CommandManager.argument("name", StringArgumentType.string())
							.suggests(new PublicHomesSuggestionProvider())
							.executes(this::pdelhomeExecute)));
		});
		CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
			dispatcher.register(CommandManager.literal("phome")
					.then(CommandManager.argument("name", StringArgumentType.string())
							.suggests(new PublicHomesSuggestionProvider())
							.executes(this::phomeExecute)));
		});
		CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
			dispatcher.register(CommandManager.literal("phomes")
					.executes(this::phomesExecute));
		});
		LOGGER.info("HOME Utilities has been loaded successfully!");
	}

	private int sethomeExecute(CommandContext<ServerCommandSource> context){
		String home_name = StringArgumentType.getString(context, "name");
		ServerPlayerEntity player = context.getSource().getPlayer();
        assert player != null;
        JsonHandler.addLocation(player, home_name, player.getX(), player.getY(), player.getZ(),player.getServerWorld());
		context.getSource().sendFeedback(() -> Text.literal("Your home has been set!").formatted(Formatting.GREEN), false);
		player.playSoundToPlayer(SoundEvents.ENTITY_PLAYER_LEVELUP, SoundCategory.MASTER, 1.0f, 1.0f);
		return 1;
	}

	private int psethomeExecute(CommandContext<ServerCommandSource> context){
		String home_name = StringArgumentType.getString(context, "name");
		ServerPlayerEntity player = context.getSource().getPlayer();
		assert player != null;
		String home_finalname = player.getName().getString() + "-" + home_name;
		JsonHandler.addPublicLocation(player, home_finalname, player.getX(), player.getY(), player.getZ(),player.getServerWorld());
		context.getSource().sendFeedback(() -> Text.literal("Your public home has been set!").formatted(Formatting.GREEN), false);
		player.playSoundToPlayer(SoundEvents.ENTITY_PLAYER_LEVELUP, SoundCategory.MASTER, 1.0f, 1.0f);
		return 1;
	}

	private int delhomeExecute(CommandContext<ServerCommandSource> context){
		String home_name = StringArgumentType.getString(context, "name");
		ServerPlayerEntity player = context.getSource().getPlayer();
		assert player != null;
		if (JsonHandler.removeLocation(player, home_name)){
			context.getSource().sendFeedback(() -> Text.literal("Your home has been deleted!").formatted(Formatting.GREEN), false);
			player.playSoundToPlayer(SoundEvents.ENTITY_PLAYER_LEVELUP, SoundCategory.MASTER, 1.0f, 1.0f);
		}
		else{
			context.getSource().sendFeedback(() -> Text.literal("Error : The home don't exist.").formatted(Formatting.RED), false);
			player.playSoundToPlayer(SoundEvents.ENTITY_VILLAGER_NO, SoundCategory.MASTER, 1.0f, 1.0f);
		}
		return 1;
	}

	private int pdelhomeExecute(CommandContext<ServerCommandSource> context){
		String home_name = StringArgumentType.getString(context, "name");
		ServerPlayerEntity player = context.getSource().getPlayer();
		assert player != null;
		if (JsonHandler.removePublicLocation(player, home_name)){
			context.getSource().sendFeedback(() -> Text.literal("Your public home has been deleted!").formatted(Formatting.GREEN), false);
			player.playSoundToPlayer(SoundEvents.ENTITY_PLAYER_LEVELUP, SoundCategory.MASTER, 1.0f, 1.0f);
		}
		else{
			context.getSource().sendFeedback(() -> Text.literal("Error : The public home don't exist or you're not the owner.").formatted(Formatting.RED), false);
			player.playSoundToPlayer(SoundEvents.ENTITY_VILLAGER_NO, SoundCategory.MASTER, 1.0f, 1.0f);
		}
		return 1;
	}

	private int homeExecute(CommandContext<ServerCommandSource> context){
		String home_name = StringArgumentType.getString(context, "name");
		ServerPlayerEntity player = context.getSource().getPlayer();
		assert player != null;
		JsonObject location = JsonHandler.getLocation(player,home_name);
		if (location != null){
			Vec3d pos = new Vec3d(location.get("x").getAsDouble(),location.get("y").getAsDouble(),location.get("z").getAsDouble());
			Identifier identifier = Identifier.of(location.get("world").getAsString());
			RegistryKey<World> worldRegistryKey = RegistryKey.of(RegistryKeys.WORLD,identifier);
			ServerWorld world = context.getSource().getServer().getWorld(worldRegistryKey);
			assert world != null;
			TeleportTarget teleport_target = new TeleportTarget(world, pos, player.getVelocity(),player.getYaw(),player.getPitch(),TeleportTarget.ADD_PORTAL_CHUNK_TICKET);
			player.teleportTo(teleport_target);
			context.getSource().sendFeedback(() -> Text.literal("You have been teleported to your home!").formatted(Formatting.GREEN), false);
		}
		else{
			context.getSource().sendFeedback(() -> Text.literal("Error : The home don't exist.").formatted(Formatting.RED), false);
			player.playSoundToPlayer(SoundEvents.ENTITY_VILLAGER_NO, SoundCategory.MASTER, 1.0f, 1.0f);
		}
		return 1;
	}

	private int phomeExecute(CommandContext<ServerCommandSource> context){
		String home_name = StringArgumentType.getString(context, "name");
		ServerPlayerEntity player = context.getSource().getPlayer();
		assert player != null;
		JsonObject location = JsonHandler.getPublicLocation(player,home_name);
		if (location != null){
			Vec3d pos = new Vec3d(location.get("x").getAsDouble(),location.get("y").getAsDouble(),location.get("z").getAsDouble());
			Identifier identifier = Identifier.of(location.get("world").getAsString());
			RegistryKey<World> worldRegistryKey = RegistryKey.of(RegistryKeys.WORLD,identifier);
			ServerWorld world = context.getSource().getServer().getWorld(worldRegistryKey);
			assert world != null;
			TeleportTarget teleport_target = new TeleportTarget(world, pos, player.getVelocity(),player.getYaw(),player.getPitch(),TeleportTarget.ADD_PORTAL_CHUNK_TICKET);
			player.teleportTo(teleport_target);
			context.getSource().sendFeedback(() -> Text.literal("You have been teleported to the public home!").formatted(Formatting.GREEN), false);
		}
		else{
			context.getSource().sendFeedback(() -> Text.literal("Error : The public home don't exist.").formatted(Formatting.RED), false);
			player.playSoundToPlayer(SoundEvents.ENTITY_VILLAGER_NO, SoundCategory.MASTER, 1.0f, 1.0f);
		}
		return 1;
	}

	private int homesExecute(CommandContext<ServerCommandSource> context){
		ServerPlayerEntity player = context.getSource().getPlayer();
		assert player != null;
		List<String> homesList = JsonHandler.listLocations(player);
        if (homesList == null){
			context.getSource().sendFeedback(() -> Text.literal("Error : You don't have any home.").formatted(Formatting.RED), false);
			player.playSoundToPlayer(SoundEvents.ENTITY_VILLAGER_NO, SoundCategory.MASTER, 1.0f, 1.0f);
		}
		else{
			context.getSource().sendFeedback(() -> Text.literal("Your homes (You can click on them to teleport):").formatted(Formatting.DARK_GREEN), false);
			for (String home : homesList){
				context.getSource().sendFeedback(() -> Text.literal("- " + home).formatted(Formatting.GOLD).styled(style -> style.withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/home " + home))), false);
			}
		}
		return 1;
	}

	private int phomesExecute(CommandContext<ServerCommandSource> context){
		ServerPlayerEntity player = context.getSource().getPlayer();
		assert player != null;
		List<String> homesList = JsonHandler.listPublicLocations(player);
		if (homesList == null){
			context.getSource().sendFeedback(() -> Text.literal("Error : The server don't have any public home.").formatted(Formatting.RED), false);
			player.playSoundToPlayer(SoundEvents.ENTITY_VILLAGER_NO, SoundCategory.MASTER, 1.0f, 1.0f);
		}
		else{
			context.getSource().sendFeedback(() -> Text.literal("Public homes (You can click on them to teleport):").formatted(Formatting.DARK_GREEN), false);
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
		if (player_target.getName().getString().equals(player.getName().getString())){
			context.getSource().sendFeedback(() -> Text.literal("Error : You can't share with yourself.").formatted(Formatting.RED), false);
			player.playSoundToPlayer(SoundEvents.ENTITY_VILLAGER_NO, SoundCategory.MASTER, 1.0f, 1.0f);
			return 1;
		}
		JsonObject location = JsonHandler.getLocation(player, home_name);
		if (location == null){
			context.getSource().sendFeedback(() -> Text.literal("Error : The home don't exist.").formatted(Formatting.RED), false);
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
			player_target.sendMessage(Text.literal(String.format("%s wants to share a home with you! To accept it click on this message.", player.getName().getString())).formatted(Formatting.GOLD).styled(style -> style.withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/accepthome " + home_finalname))));
		}
		return 1;
	}

	private int accepthomeExecute(CommandContext<ServerCommandSource> context){
		String home_name = StringArgumentType.getString(context, "name");
		ServerPlayerEntity player = context.getSource().getPlayer();
        assert player != null;
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
					context.getSource().sendFeedback(() -> Text.literal("The home has been transferred! Run /homes to find it.").formatted(Formatting.GREEN).styled(style -> style.withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/homes"))), false);
					player.playSoundToPlayer(SoundEvents.ENTITY_PLAYER_LEVELUP, SoundCategory.MASTER, 1.0f, 1.0f);
					return 1;
				}
			}
			context.getSource().sendFeedback(() -> Text.literal("Error : The home don't exist.").formatted(Formatting.RED), false);
			player.playSoundToPlayer(SoundEvents.ENTITY_VILLAGER_NO, SoundCategory.MASTER, 1.0f, 1.0f);
		}
		else{
			context.getSource().sendFeedback(() -> Text.literal("Error : There is no home to accept.").formatted(Formatting.RED), false);
			player.playSoundToPlayer(SoundEvents.ENTITY_VILLAGER_NO, SoundCategory.MASTER, 1.0f, 1.0f);
		}
		return 1;
	}

}