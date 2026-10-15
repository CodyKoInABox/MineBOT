package com.minebot.client;

import com.minebot.core.BlockPos;
import com.minebot.core.BotAction;
import com.minebot.core.ChestPolicy;
import com.minebot.core.Ids;
import com.minebot.core.JobProgress;
import com.minebot.core.QuarryMachine;
import com.minebot.core.TaskConfig;
import com.minebot.core.TickInput;
import com.minebot.core.WorldQuery;
import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.CraftingMenu;
import net.minecraft.world.inventory.SmithingMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.VoxelShape;

/** Reads the client world, asks the core for one action, then performs it. */
public final class MineBotRuntime {
	private final ActionPerformer performer = new ActionPerformer();
	private QuarryMachine machine;
	private int saveCooldown;

	public void attach(TaskConfig config, JobProgress progress, boolean resume) {
		machine = new QuarryMachine(config, progress);
		int bedrock = bedrockY();
		if (resume) {
			machine.resume(bedrock);
		} else {
			machine.start(bedrock);
		}
	}

	public void stop() {
		if (machine != null) {
			machine.stop();
		}
	}

	public QuarryMachine machine() {
		return machine;
	}

	public void tick(Minecraft minecraft) {
		MineBotClient.drive = false;
		MineBotClient.jump = false;
		if (machine == null) {
			return;
		}
		LocalPlayer player = minecraft.player;
		if (player == null || minecraft.level == null || minecraft.gameMode == null) {
			return;
		}
		if (minecraft.gui.screen() instanceof MineBotScreen) {
			return;
		}
		if (performer.busy()) {
			performer.pump(minecraft);
			remember(minecraft);
			return;
		}
		TickInput input = sample(minecraft, player);
		BotAction action = machine.tick(input);
		MineBotClient.status = action.status == null || action.status.isEmpty() ? machine.progress().status : action.status;
		if (MineBotClient.status != null && !MineBotClient.status.isEmpty()) {
			minecraft.gui.hud.setOverlayMessage(net.minecraft.network.chat.Component.literal(MineBotClient.status), false);
		}
		performer.perform(minecraft, action);
		remember(minecraft);
	}

	private void remember(Minecraft minecraft) {
		if (machine == null) {
			return;
		}
		MineBotClient.store.rememberProgress(machine.progress());
		MineBotClient.store.rememberActive(machine.config());
		if (--saveCooldown <= 0) {
			saveCooldown = 40;
			MineBotClient.sessions.save(MineBotClient.store);
		}
	}

	private static int bedrockY() {
		Minecraft minecraft = Minecraft.getInstance();
		if (minecraft.level == null) {
			return -64;
		}
		return minecraft.level.dimensionType().minY();
	}

	private static TickInput sample(Minecraft minecraft, LocalPlayer player) {
		TickInput input = new TickInput();
		net.minecraft.core.BlockPos feet = player.blockPosition();
		input.playerX = feet.getX();
		input.playerY = feet.getY();
		input.playerZ = feet.getZ();
		input.hunger = player.getFoodData().getFoodLevel();
		input.dayTime = minecraft.level.getOverworldClockTime();
		input.bedrockY = minecraft.level.dimensionType().minY();
		input.inventory = inventory(player);
		input.hasFood = ChestPolicy.foodIn(input.inventory) != null;
		input.toolId = ActionPerformer.itemId(player.getInventory().getSelectedItem());
		input.world = new ClientWorld(minecraft, player);
		input.inventoryScreenOpen = minecraft.gui.screen() instanceof net.minecraft.client.gui.screens.inventory.InventoryScreen;
		input.craftingScreenOpen = player.containerMenu instanceof CraftingMenu;
		input.smithingScreenOpen = player.containerMenu instanceof SmithingMenu;
		input.chestOpen = player.containerMenu instanceof ChestMenu;
		input.bedNearby = findNearby(minecraft, player, input, true);
		findNearby(minecraft, player, input, false);
		input.chestCanAcceptDeposit = chestHasRoom(player, machineDepositId(input));
		BlockPos table = findBlock(minecraft, player, "minecraft:crafting_table", 6);
		if (table != null) {
			input.craftingTablePos = table;
		}
		BlockPos smithing = findBlock(minecraft, player, "minecraft:smithing_table", 6);
		if (smithing != null) {
			input.smithingTablePos = smithing;
		}
		return input;
	}

	private static String machineDepositId(TickInput input) {
		var decision = ChestPolicy.evaluate(
				input.inventory,
				MineBotClient.config().storage.depositIds,
				MineBotClient.config().storage.keepIds,
				true
		);
		return decision.itemId;
	}

	private static boolean chestHasRoom(LocalPlayer player, String itemId) {
		if (!(player.containerMenu instanceof ChestMenu chest)) {
			return true;
		}
		if (itemId == null || itemId.isEmpty()) {
			return true;
		}
		int chestSlots = chest.getRowCount() * 9;
		for (int i = 0; i < chestSlots && i < chest.slots.size(); i++) {
			ItemStack stack = chest.slots.get(i).getItem();
			if (stack.isEmpty()) {
				return true;
			}
			if (itemId.equals(ActionPerformer.itemId(stack)) && stack.isStackable() && stack.getCount() < 64) {
				return true;
			}
		}
		return false;
	}

	private static boolean findNearby(Minecraft minecraft, LocalPlayer player, TickInput input, boolean beds) {
		TaskConfig config = MineBotClient.config();
		int radius = Math.max(2, config.storage.searchRadius);
		net.minecraft.core.BlockPos origin = player.blockPosition();
		BlockPos marked = config.storage.markedChest;
		if (!beds && marked != null) {
			input.chestPos = marked;
			return false;
		}
		int reach = beds ? 4 : radius;
		BlockPos found = null;
		int best = Integer.MAX_VALUE;
		for (int dy = -2; dy <= 2; dy++) {
			for (int dz = -reach; dz <= reach; dz++) {
				for (int dx = -reach; dx <= reach; dx++) {
					int x = origin.getX() + dx;
					int y = origin.getY() + dy;
					int z = origin.getZ() + dz;
					String id = ActionPerformer.blockId(minecraft.level.getBlockState(new net.minecraft.core.BlockPos(x, y, z)));
					boolean match = beds ? id.endsWith("_bed") : isChest(id);
					if (!match) {
						continue;
					}
					int dist = Math.abs(dx) + Math.abs(dy) + Math.abs(dz);
					if (dist < best) {
						best = dist;
						found = new BlockPos(x, y, z);
					}
				}
			}
		}
		if (beds) {
			input.bedNearby = found != null;
			input.bedPos = found;
			return found != null;
		}
		input.chestPos = found;
		return found != null;
	}

	private static BlockPos findBlock(Minecraft minecraft, LocalPlayer player, String blockId, int radius) {
		net.minecraft.core.BlockPos origin = player.blockPosition();
		for (int dy = -1; dy <= 2; dy++) {
			for (int dz = -radius; dz <= radius; dz++) {
				for (int dx = -radius; dx <= radius; dx++) {
					int x = origin.getX() + dx;
					int y = origin.getY() + dy;
					int z = origin.getZ() + dz;
					String id = ActionPerformer.blockId(minecraft.level.getBlockState(new net.minecraft.core.BlockPos(x, y, z)));
					if (blockId.equals(id)) {
						return new BlockPos(x, y, z);
					}
				}
			}
		}
		return null;
	}

	private static boolean isChest(String id) {
		return id.equals("minecraft:chest")
				|| id.equals("minecraft:trapped_chest")
				|| id.equals("minecraft:barrel");
	}

	private static Map<String, Integer> inventory(LocalPlayer player) {
		Map<String, Integer> counts = new LinkedHashMap<>();
		for (ItemStack stack : player.getInventory().getNonEquipmentItems()) {
			if (stack.isEmpty()) {
				continue;
			}
			counts.merge(ActionPerformer.itemId(stack), stack.getCount(), Integer::sum);
		}
		return counts;
	}

	private static final class ClientWorld implements WorldQuery {
		private final Minecraft minecraft;
		private final LocalPlayer player;

		private ClientWorld(Minecraft minecraft, LocalPlayer player) {
			this.minecraft = minecraft;
			this.player = player;
		}

		@Override
		public String blockId(int x, int y, int z) {
			return ActionPerformer.blockId(state(x, y, z));
		}

		@Override
		public float hardness(int x, int y, int z) {
			return state(x, y, z).getDestroySpeed(minecraft.level, new net.minecraft.core.BlockPos(x, y, z));
		}

		@Override
		public boolean solid(int x, int y, int z) {
			VoxelShape shape = state(x, y, z).getCollisionShape(minecraft.level, new net.minecraft.core.BlockPos(x, y, z));
			return shape != null && !shape.isEmpty();
		}

		@Override
		public boolean canHarvest(String toolId, int x, int y, int z) {
			BlockState state = state(x, y, z);
			if (player.hasCorrectToolForDrops(state)) {
				return true;
			}
			if (!Ids.isPickaxe(toolId)) {
				return false;
			}
			String id = blockId(x, y, z);
			if (id.contains("obsidian") || id.contains("ancient_debris") || id.contains("respawn_anchor")) {
				return toolId.contains("diamond") || toolId.contains("netherite");
			}
			return true;
		}

		@Override
		public float toolSpeed(String toolId, int x, int y, int z) {
			return com.minebot.core.BreakTimes.speedFor(toolId);
		}

		private BlockState state(int x, int y, int z) {
			return minecraft.level.getBlockState(new net.minecraft.core.BlockPos(x, y, z));
		}
	}
}
