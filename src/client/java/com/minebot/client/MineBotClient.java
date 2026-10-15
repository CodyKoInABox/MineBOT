package com.minebot.client;

import com.minebot.core.BlockPos;
import com.minebot.core.JobProgress;
import com.minebot.core.PresetStore;
import com.minebot.core.Presets;
import com.minebot.core.Status;
import com.minebot.core.TaskConfig;
import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.ClientCommands;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

public final class MineBotClient implements ClientModInitializer {
	public static final String MOD_ID = "minebot";
	public static final KeyMapping OPEN = new KeyMapping(
			"key.minebot.open",
			InputConstants.Type.KEYBOARD,
			InputConstants.KEY_K,
			KeyMapping.Category.MISC
	);

	static final PresetStore store = new PresetStore();
	static final SessionStore sessions = new SessionStore();
	static final MineBotRuntime runtime = new MineBotRuntime();

	public static volatile boolean drive;
	public static volatile boolean jump;
	/** True for the rest of a tick that is continuing one block break, so vanilla does not abort it. */
	public static volatile boolean breaking;
	static String status = Status.STOPPED;

	@Override
	public void onInitializeClient() {
		sessions.load(store);
		KeyMappingHelper.registerKeyMapping(OPEN);
		ClientTickEvents.START_CLIENT_TICK.register(MineBotClient::tick);
		ClientLifecycleEvents.CLIENT_STOPPING.register(client -> sessions.save(store));
		ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> dispatcher.register(
				ClientCommands.literal("minebot")
						.then(ClientCommands.literal("start").executes(context -> {
							startJob();
							context.getSource().sendFeedback(Component.literal("MineBOT: " + status));
							return 1;
						}))
						.then(ClientCommands.literal("stop").executes(context -> {
							stopJob();
							context.getSource().sendFeedback(Component.literal("MineBOT: " + status));
							return 1;
						}))
		));
		HudElementRegistry.addLast(Identifier.fromNamespaceAndPath(MOD_ID, "status"), (graphics, delta) -> {
			if (status == null || status.isEmpty() || Status.STOPPED.equals(status)) {
				return;
			}
			Minecraft minecraft = Minecraft.getInstance();
			int color = Status.PAUSED_CHESTS_FULL.equals(status) ? 0xFFE06C75 : 0xFFE0A45C;
			graphics.text(minecraft.font, status, 8, 8, color);
		});
		JobProgress progress = store.progress();
		if (progress.running) {
			runtime.attach(store.active(), progress, true);
			status = progress.status == null ? Status.RUNNING : progress.status;
		}
	}

	public static TaskConfig config() {
		return store.active();
	}

	public static void replaceConfig(TaskConfig config) {
		store.rememberActive(config);
		sessions.save(store);
	}

	public static void savePreset(String name, TaskConfig config) {
		store.save(name, config);
		store.rememberActive(config);
		sessions.save(store);
	}

	public static TaskConfig loadPreset(String name, BlockPos origin) {
		if (Presets.EXAMPLE_QUARRY.equals(name) && !store.has(name)) {
			TaskConfig created = Presets.exampleQuarry(origin);
			store.save(name, created);
			store.rememberActive(created);
			sessions.save(store);
			return created;
		}
		return store.load(name).map(loaded -> {
			store.rememberActive(loaded);
			sessions.save(store);
			return loaded;
		}).orElse(null);
	}

	public static void startJob() {
		TaskConfig config = store.active();
		runtime.attach(config, new JobProgress(), false);
		status = runtime.machine().progress().status;
		store.rememberProgress(runtime.machine().progress());
		sessions.save(store);
	}

	public static void stopJob() {
		runtime.stop();
		status = Status.STOPPED;
		if (runtime.machine() != null) {
			store.rememberProgress(runtime.machine().progress());
		}
		sessions.save(store);
	}

	public static BlockPos feet() {
		LocalPlayer player = Minecraft.getInstance().player;
		if (player == null) {
			return new BlockPos(0, 64, 0);
		}
		net.minecraft.core.BlockPos pos = player.blockPosition();
		return new BlockPos(pos.getX(), pos.getY(), pos.getZ());
	}

	private static void tick(Minecraft minecraft) {
		while (OPEN.consumeClick()) {
			minecraft.gui.setScreen(new MineBotScreen());
		}
		runtime.tick(minecraft);
	}
}
