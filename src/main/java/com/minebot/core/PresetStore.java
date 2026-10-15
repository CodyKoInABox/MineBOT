package com.minebot.core;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/** Named presets plus the in-progress job, so a restart can resume. */
public final class PresetStore {
	private final Map<String, TaskConfig> presets = new LinkedHashMap<>();
	private TaskConfig active = new TaskConfig();
	private JobProgress progress = new JobProgress();

	public void save(String name, TaskConfig config) {
		String key = name == null ? "" : name.trim();
		if (key.isEmpty()) {
			throw new IllegalArgumentException("Preset name is required");
		}
		TaskConfig stored = config.copy();
		stored.name = key;
		presets.put(key, stored);
	}

	public Optional<TaskConfig> load(String name) {
		TaskConfig config = presets.get(name);
		if (config == null) {
			return Optional.empty();
		}
		return Optional.of(config.copy());
	}

	public boolean has(String name) {
		return presets.containsKey(name);
	}

	public Collection<String> names() {
		return presets.keySet();
	}

	public void rememberActive(TaskConfig config) {
		this.active = config.copy();
	}

	public TaskConfig active() {
		return active.copy();
	}

	public void rememberProgress(JobProgress progress) {
		this.progress = progress.copy();
	}

	public JobProgress progress() {
		return progress.copy();
	}

	public Map<String, TaskConfig> snapshot() {
		Map<String, TaskConfig> copy = new LinkedHashMap<>();
		presets.forEach((name, config) -> copy.put(name, config.copy()));
		return copy;
	}
}
