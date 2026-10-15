package com.minebot.core;

/** One configured mining job. Edited from the screen, then handed to the state machine. */
public final class TaskConfig {
	public String name = "";
	public Area area = new Area();
	public DepthSpec depth = DepthSpec.bedrock();
	public boolean stair;
	public MineFilter mine = MineFilter.everything();
	public StorageSpec storage = StorageSpec.withDefaults();
	public SurvivalSpec survival = SurvivalSpec.defaults();

	public TaskConfig() {}

	public TaskConfig copy() {
		TaskConfig copy = new TaskConfig();
		copy.name = name;
		copy.area = area.copy();
		copy.depth = depth.copy();
		copy.stair = stair;
		copy.mine = mine.copy();
		copy.storage = storage.copy();
		copy.survival = survival.copy();
		return copy;
	}
}
