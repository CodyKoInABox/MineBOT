package com.minebot.client;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.minebot.core.JobProgress;
import com.minebot.core.PresetStore;
import com.minebot.core.TaskConfig;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import net.fabricmc.loader.api.FabricLoader;

/** Presets and the live job, so a restart can resume. */
public final class SessionStore {
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

	private final Path file;

	public SessionStore() {
		this.file = FabricLoader.getInstance().getConfigDir().resolve("minebot").resolve("session.json");
	}

	public void save(PresetStore store) {
		Disk disk = new Disk();
		disk.active = store.active();
		disk.progress = store.progress();
		disk.presets = new LinkedHashMap<>();
		store.snapshot().forEach((name, config) -> disk.presets.put(name, config));
		try {
			Files.createDirectories(file.getParent());
			try (Writer writer = Files.newBufferedWriter(file)) {
				GSON.toJson(disk, writer);
			}
		} catch (IOException ignored) {
			// The in-memory job still runs. The next save can land.
		}
	}

	public void load(PresetStore store) {
		if (!Files.isRegularFile(file)) {
			return;
		}
		try (Reader reader = Files.newBufferedReader(file)) {
			Disk disk = GSON.fromJson(reader, Disk.class);
			if (disk == null) {
				return;
			}
			if (disk.presets != null) {
				disk.presets.forEach(store::save);
			}
			if (disk.active != null) {
				store.rememberActive(disk.active);
			}
			if (disk.progress != null) {
				store.rememberProgress(disk.progress);
			}
		} catch (IOException ignored) {
			// Start with an empty session rather than crashing the client.
		}
	}

	private static final class Disk {
		TaskConfig active;
		JobProgress progress;
		Map<String, TaskConfig> presets;
	}
}
