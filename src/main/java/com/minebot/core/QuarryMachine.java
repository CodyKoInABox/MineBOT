package com.minebot.core;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * One action per tick. Break actions repeat for the vanilla destroy duration.
 * Survival, crafting, and a full chest outrank digging.
 */
public final class QuarryMachine {
	private static final double REACH_SQ = 4.5 * 4.5;

	private final TaskConfig config;
	private final JobProgress progress;
	private int bedrockY = -64;
	private int surface;
	private int floor;
	private Map<Long, Integer> stairTops = Map.of();
	private boolean planned;

	public QuarryMachine(TaskConfig config, JobProgress progress) {
		this.config = config;
		this.progress = progress == null ? new JobProgress() : progress;
	}

	public TaskConfig config() {
		return config;
	}

	public JobProgress progress() {
		return progress;
	}

	public void start(int bedrockY) {
		this.bedrockY = bedrockY;
		rebuildPlan();
		progress.craftClicks = new ArrayList<>();
		progress.craftIndex = 0;
		progress.breaking = false;
		progress.placeTorchNext = false;
		if (!config.area.complete()) {
			progress.running = false;
			progress.initialized = false;
			progress.status = Status.NEED_AREA;
			return;
		}
		progress.running = true;
		progress.initialized = true;
		progress.x = config.area.minX();
		progress.y = surface;
		progress.z = config.area.minZ();
		progress.status = Status.RUNNING;
	}

	public void resume(int bedrockY) {
		this.bedrockY = bedrockY;
		rebuildPlan();
		if (!config.area.complete()) {
			progress.running = false;
			progress.status = Status.NEED_AREA;
			return;
		}
		progress.running = true;
		if (!progress.initialized) {
			progress.x = config.area.minX();
			progress.y = surface;
			progress.z = config.area.minZ();
			progress.initialized = true;
		}
		if (progress.status == null || progress.status.isBlank() || Status.STOPPED.equals(progress.status)) {
			progress.status = Status.RUNNING;
		}
	}

	public void stop() {
		progress.running = false;
		progress.breaking = false;
		progress.breakTicksLeft = 0;
		progress.eatTicksLeft = 0;
		progress.craftClicks = new ArrayList<>();
		progress.craftIndex = 0;
		progress.status = Status.STOPPED;
	}

	public BotAction tick(TickInput in) {
		if (in == null) {
			return BotAction.idle(progress.status);
		}
		if (!planned) {
			this.bedrockY = in.bedrockY;
			rebuildPlan();
		}
		if (!progress.running) {
			return BotAction.idle(progress.status);
		}
		if (!config.area.complete()) {
			progress.running = false;
			progress.status = Status.NEED_AREA;
			return BotAction.idle(progress.status);
		}
		if (progress.breaking) {
			return continueBreak(in);
		}

		if (progress.sleepCooldown > 0) {
			progress.sleepCooldown--;
		} else if (SurvivalPolicy.shouldSleep(config.survival.sleep, in.bedNearby, in.dayTime)) {
			progress.sleepCooldown = 80;
			progress.status = Status.SLEEPING;
			BlockPos bed = in.bedPos == null ? new BlockPos(in.playerX, in.playerY, in.playerZ) : in.bedPos;
			return BotAction.sleep(bed.x, bed.y, bed.z, progress.status);
		}
		if (progress.eatTicksLeft > 0) {
			progress.eatTicksLeft--;
			progress.status = Status.EATING;
			String food = ChestPolicy.foodIn(in.inventory);
			return BotAction.eat(food == null ? "minecraft:bread" : food, progress.status);
		}
		if (SurvivalPolicy.shouldEat(in.hunger, config.survival.hungerThreshold, config.survival.eat, in.hasFood())) {
			progress.eatTicksLeft = SurvivalPolicy.EAT_TICKS - 1;
			progress.status = Status.EATING;
			String food = ChestPolicy.foodIn(in.inventory);
			return BotAction.eat(food == null ? "minecraft:bread" : food, progress.status);
		}

		BotAction craft = craft(in);
		if (craft != null) {
			return craft;
		}

		ChestDecision chest = ChestPolicy.evaluate(
				in.inventory,
				config.storage.depositIds,
				config.storage.keepIds,
				in.chestCanAcceptDeposit
		);
		if (chest.type == ChestDecision.Type.PAUSE) {
			progress.status = Status.PAUSED_CHESTS_FULL;
			return BotAction.pause(progress.status);
		}
		if (chest.type == ChestDecision.Type.DEPOSIT) {
			return deposit(in, chest);
		}

		if (progress.placeTorchNext) {
			progress.placeTorchNext = false;
			progress.status = Status.RUNNING;
			return BotAction.place(progress.torchX, progress.torchY, progress.torchZ, "minecraft:torch", "Placing torch");
		}

		return mine(in);
	}

	private BotAction craft(TickInput in) {
		if (!config.survival.autoCraftPicks) {
			return null;
		}
		if (progress.craftClicks != null && progress.craftIndex < progress.craftClicks.size()) {
			CraftClick click = progress.craftClicks.get(progress.craftIndex++);
			progress.status = Status.CRAFTING;
			if (click.kind == CraftClick.Kind.PLACE_TABLE && in.craftingTablePos == null) {
				BlockPos spot = placeSpot(in);
				click = CraftClick.placeTable(spot.x, spot.y - 1, spot.z);
			}
			return BotAction.craft(click, progress.status);
		}
		BlockPos next = peekMine(in);
		if (next == null) {
			return null;
		}
		String block = in.world.blockId(next.x, next.y, next.z);
		ToolKind kind = ToolRules.kindFor(block);
		if (kind == null) {
			return null;
		}
		List<CraftStep> steps = CraftPlan.plan(in.inventory, kind, ToolRules.minimumTier(block), in.toolUses);
		if (steps.isEmpty()) {
			return null;
		}
		CraftStep step = steps.get(0);
		List<CraftClick> clicks = new ArrayList<>();
		if ("crafting_table".equals(step.station)) {
			if (!in.craftingScreenOpen) {
				if (in.craftingTablePos == null) {
					BlockPos spot = placeSpot(in);
					clicks.add(CraftClick.placeTable(spot.x, spot.y - 1, spot.z));
				} else {
					clicks.add(CraftClick.openTable(in.craftingTablePos.x, in.craftingTablePos.y, in.craftingTablePos.z));
				}
			}
		} else if ("smithing_table".equals(step.station)) {
			if (!in.smithingScreenOpen && in.smithingTablePos != null) {
				clicks.add(CraftClick.openSmithing(in.smithingTablePos.x, in.smithingTablePos.y, in.smithingTablePos.z));
			}
		} else if (!in.inventoryScreenOpen) {
			clicks.add(CraftClick.openInventory());
		}
		clicks.addAll(CraftClicks.expand(step));
		progress.craftClicks = clicks;
		progress.craftIndex = 0;
		if (clicks.isEmpty()) {
			return null;
		}
		CraftClick click = clicks.get(progress.craftIndex++);
		progress.status = Status.CRAFTING;
		return BotAction.craft(click, progress.status);
	}

	private BotAction deposit(TickInput in, ChestDecision chest) {
		progress.status = Status.DEPOSITING + " " + chest.itemId;
		if (in.chestPos == null) {
			return BotAction.idle(Status.NO_CHEST);
		}
		if (!inReach(in, in.chestPos.x, in.chestPos.y, in.chestPos.z)) {
			return moveToward(in, standNear(in, in.chestPos.x, in.chestPos.y, in.chestPos.z));
		}
		if (!in.chestOpen) {
			Aim aim = Aim.atFeet(in.playerX, in.playerY, in.playerZ, in.chestPos.x, in.chestPos.y, in.chestPos.z);
			BotAction use = BotAction.use(in.chestPos.x, in.chestPos.y, in.chestPos.z, "minecraft:chest", progress.status);
		return use.withAim(aim.yaw, aim.pitch);
	}
		return BotAction.clickChest(in.chestPos.x, in.chestPos.y, in.chestPos.z, chest.itemId, progress.status);
	}

	private BotAction mine(TickInput in) {
		if (progress.breaking) {
			return continueBreak(in);
		}
		int guard = 0;
		int limit = config.area.sizeX() * config.area.sizeZ() * (surface - floor + 1) + 2;
		while (guard++ < limit) {
			if (progress.y < floor) {
				progress.running = false;
				progress.status = Status.DONE;
				return BotAction.done(progress.status);
			}
			int x = progress.x;
			int y = progress.y;
			int z = progress.z;
			if (wantsBreak(in, x, y, z)) {
				if (!inReach(in, x, y, z)) {
					return moveToward(in, standNear(in, x, y, z));
				}
				return beginBreak(in, x, y, z);
			}
			advance();
		}
		progress.running = false;
		progress.status = Status.DONE;
		return BotAction.done(progress.status);
	}

	private boolean wantsBreak(TickInput in, int x, int y, int z) {
		if (SpiralStair.protectedColumn(stairTops, x, y, z, bedrockY)) {
			return false;
		}
		String id = in.world.blockId(x, y, z);
		if (Ids.isAir(id) || Ids.isBedrock(id)) {
			return false;
		}
		float hardness = in.world.hardness(x, y, z);
		if (hardness < 0f) {
			return false;
		}
		boolean listed = config.mine.everything || config.mine.lists(id);
		if (!config.mine.shouldMine(id, !listed)) {
			return false;
		}
		return true;
	}

	private BotAction beginBreak(TickInput in, int x, int y, int z) {
		String block = in.world.blockId(x, y, z);
		String tool = ToolRules.choose(block, in.inventory, in.toolId);
		float hardness = in.world.hardness(x, y, z);
		boolean harvest = ToolRules.canHarvest(tool, block);
		int ticks = BreakTimes.ticks(hardness, ToolRules.speedAgainst(tool, block), harvest);
		if (ticks < 0) {
			advance();
			return BotAction.idle(progress.status);
		}
		progress.breaking = true;
		progress.breakStartPending = false;
		progress.breakTicksLeft = ticks - 1;
		progress.breakX = x;
		progress.breakY = y;
		progress.breakZ = z;
		progress.breakItem = tool == null ? "" : tool;
		progress.status = Status.RUNNING;
		Aim aim = Aim.atFeet(in.playerX, in.playerY, in.playerZ, x, y, z);
		if (ticks == 1) {
			progress.breaking = false;
			afterBreak(x, y, z);
		}
		return BotAction.breakBlock(x, y, z, aim.yaw, aim.pitch, true, progress.breakItem, progress.status);
	}

	private BotAction continueBreak(TickInput in) {
		int x = progress.breakX;
		int y = progress.breakY;
		int z = progress.breakZ;
		Aim aim = Aim.atFeet(in.playerX, in.playerY, in.playerZ, x, y, z);
		progress.breakTicksLeft--;
		boolean finished = progress.breakTicksLeft <= 0;
		if (finished) {
			progress.breaking = false;
			afterBreak(x, y, z);
		}
		String tool = progress.breakItem == null ? "" : progress.breakItem;
		return BotAction.breakBlock(x, y, z, aim.yaw, aim.pitch, false, tool, progress.status);
	}

	private BlockPos peekMine(TickInput in) {
		int x = progress.x;
		int y = progress.y;
		int z = progress.z;
		int guard = 0;
		int limit = config.area.sizeX() * config.area.sizeZ() * (surface - floor + 1) + 2;
		while (guard++ < limit) {
			if (y < floor) {
				return null;
			}
			if (wantsBreak(in, x, y, z)) {
				return new BlockPos(x, y, z);
			}
			if (x < config.area.maxX()) {
				x++;
				continue;
			}
			x = config.area.minX();
			if (z < config.area.maxZ()) {
				z++;
				continue;
			}
			z = config.area.minZ();
			y--;
		}
		return null;
	}

	private void afterBreak(int x, int y, int z) {
		advance();
		if (config.survival.placeTorches
				&& y == floor
				&& TorchGrid.isAnchor(x, z, config.area.minX(), config.area.minZ(), config.area.maxX(), config.area.maxZ(), config.survival.torchSpacing)
				&& !SpiralStair.protectedColumn(stairTops, x, y, z, bedrockY)) {
			progress.placeTorchNext = true;
			progress.torchX = x;
			progress.torchY = y;
			progress.torchZ = z;
		}
	}

	private void advance() {
		if (progress.x < config.area.maxX()) {
			progress.x++;
			return;
		}
		progress.x = config.area.minX();
		if (progress.z < config.area.maxZ()) {
			progress.z++;
			return;
		}
		progress.z = config.area.minZ();
		progress.y--;
	}

	private BotAction moveToward(TickInput in, BlockPos goal) {
		if (goal == null) {
			progress.status = Status.NO_PATH;
			return BotAction.idle(progress.status);
		}
		if (in.playerX == goal.x && in.playerY == goal.y && in.playerZ == goal.z) {
			Aim aim = Aim.atFeet(in.playerX, in.playerY, in.playerZ, goal.x, goal.y, goal.z);
			return BotAction.look(aim.yaw, aim.pitch, progress.status);
		}
		List<Pathfinder.Move> path = Pathfinder.find(
				in.playerX, in.playerY, in.playerZ,
				goal.x, goal.y, goal.z,
				in.world,
				2048
		);
		Pathfinder.Move step;
		if (!path.isEmpty()) {
			step = path.get(0);
		} else {
			step = greedy(in, goal);
		}
		if (step == null) {
			progress.status = Status.NO_PATH;
			return BotAction.idle(progress.status);
		}
		Aim aim = Aim.atFeet(in.playerX, in.playerY, in.playerZ, step.x(), step.y(), step.z());
		progress.status = Status.RUNNING;
		if (step.kind() == Pathfinder.Kind.JUMP) {
			return BotAction.jump(step.x(), step.y(), step.z(), aim.yaw, aim.pitch, progress.status);
		}
		return BotAction.walk(step.x(), step.y(), step.z(), aim.yaw, aim.pitch, progress.status);
	}

	private Pathfinder.Move greedy(TickInput in, BlockPos goal) {
		int dx = Integer.compare(goal.x, in.playerX);
		int dz = Integer.compare(goal.z, in.playerZ);
		int sx = dx != 0 ? in.playerX + dx : in.playerX;
		int sz = dx != 0 ? in.playerZ : in.playerZ + dz;
		if (in.world.canStand(sx, in.playerY, sz)) {
			return new Pathfinder.Move(sx, in.playerY, sz, Pathfinder.Kind.WALK);
		}
		if (in.world.canStand(sx, in.playerY + 1, sz)) {
			return new Pathfinder.Move(sx, in.playerY + 1, sz, Pathfinder.Kind.JUMP);
		}
		if (in.world.canStand(sx, in.playerY - 1, sz)) {
			return new Pathfinder.Move(sx, in.playerY - 1, sz, Pathfinder.Kind.DROP);
		}
		return null;
	}

	private BlockPos standNear(TickInput in, int x, int y, int z) {
		int[][] around = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}, {1, 1}, {1, -1}, {-1, 1}, {-1, -1}};
		BlockPos best = null;
		int bestDist = Integer.MAX_VALUE;
		for (int dy = 1; dy >= -2; dy--) {
			for (int[] delta : around) {
				int sx = x + delta[0];
				int sy = y + dy;
				int sz = z + delta[1];
				if (!in.world.canStand(sx, sy, sz)) {
					continue;
				}
				int dist = Math.abs(sx - in.playerX) + Math.abs(sy - in.playerY) + Math.abs(sz - in.playerZ);
				if (dist < bestDist) {
					bestDist = dist;
					best = new BlockPos(sx, sy, sz);
				}
			}
		}
		if (best != null) {
			return best;
		}
		if (in.world.canStand(x, y + 1, z)) {
			return new BlockPos(x, y + 1, z);
		}
		return new BlockPos(in.playerX, in.playerY, in.playerZ);
	}

	private BlockPos placeSpot(TickInput in) {
		int[][] around = {{0, 0}, {1, 0}, {-1, 0}, {0, 1}, {0, -1}};
		for (int[] delta : around) {
			int x = in.playerX + delta[0];
			int z = in.playerZ + delta[1];
			if (in.world.solid(x, in.playerY - 1, z) && !in.world.solid(x, in.playerY, z)) {
				return new BlockPos(x, in.playerY, z);
			}
		}
		return new BlockPos(in.playerX, in.playerY, in.playerZ);
	}

	private boolean inReach(TickInput in, int x, int y, int z) {
		double dx = (x + 0.5) - (in.playerX + 0.5);
		double dy = (y + 0.5) - (in.playerY + 1.62);
		double dz = (z + 0.5) - (in.playerZ + 0.5);
		return dx * dx + dy * dy + dz * dz <= REACH_SQ;
	}

	private void rebuildPlan() {
		if (!config.area.complete()) {
			planned = true;
			stairTops = Map.of();
			return;
		}
		surface = config.area.surfaceY();
		floor = config.depth.floorInclusive(surface, bedrockY);
		stairTops = QuarryVolume.stairs(config, surface, floor);
		planned = true;
	}
}
