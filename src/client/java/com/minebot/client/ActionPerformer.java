package com.minebot.client;

import com.minebot.core.ActionType;
import com.minebot.core.BotAction;
import com.minebot.core.CraftClick;
import com.minebot.core.Ids;
import java.util.ArrayDeque;
import java.util.Deque;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.network.protocol.game.ServerboundPunchPacket;
import net.minecraft.network.protocol.game.ServerboundSetCarriedItemPacket;
import net.minecraft.resources.Identifier;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.CraftingMenu;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.inventory.SmithingMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

/**
 * Turns one core action into one real client interaction. Craft grid clicks are split across ticks.
 * A break stays on one block until vanilla's destroy progress finishes. The attack key is not held,
 * so the normal tick would abort the break; the client mixin skips that abort while {@code breaking} is set.
 */
public final class ActionPerformer {
	private final Deque<Click> pending = new ArrayDeque<>();
	/** Set after a block actually finishes, so the next start drains vanilla's 5-tick destroy delay. */
	private boolean vanillaDelayPending;

	public boolean busy() {
		return !pending.isEmpty();
	}

	public void pump(Minecraft minecraft) {
		Click click = pending.poll();
		if (click != null) {
			click(minecraft, click);
		}
	}

	public void perform(Minecraft minecraft, BotAction action) {
		LocalPlayer player = minecraft.player;
		MultiPlayerGameMode gameMode = minecraft.gameMode;
		if (player == null || gameMode == null || action == null) {
			return;
		}
		switch (action.type) {
			case LOOK -> look(player, action.yaw, action.pitch);
			case WALK, JUMP -> {
				look(player, action.yaw, action.pitch);
				MineBotClient.drive = true;
				MineBotClient.jump = action.type == ActionType.JUMP;
			}
			case BREAK -> breakBlock(minecraft, player, gameMode, action);
			case PLACE -> {
				selectItem(player, action.itemId);
				BlockPos support = new BlockPos(action.x, action.y - 1, action.z);
				useOn(gameMode, player, support, Direction.UP);
			}
			case USE, SLEEP -> {
				if (!action.itemId.isEmpty() && !action.itemId.equals("minecraft:chest") && !action.itemId.equals("minecraft:red_bed")) {
					selectItem(player, action.itemId);
				}
				BlockPos pos = new BlockPos(action.x, action.y, action.z);
				useOn(gameMode, player, pos, faceToward(player, pos));
			}
			case EAT -> {
				selectItem(player, action.itemId);
				if (!player.isUsingItem()) {
					gameMode.useItem(player, InteractionHand.MAIN_HAND);
				}
			}
			case CLICK_CHEST -> shiftClick(player, gameMode, action.itemId);
			case CRAFT -> craft(minecraft, player, action.craft);
			case PAUSE, IDLE, DONE -> {
				// Status is shown by the HUD. Nothing to click.
			}
		}
	}

	private void breakBlock(Minecraft minecraft, LocalPlayer player, MultiPlayerGameMode gameMode, BotAction action) {
		if (action.itemId == null || action.itemId.isEmpty()) {
			selectEmptyHand(player);
		} else {
			selectItem(player, action.itemId);
		}
		BlockPos pos = new BlockPos(action.x, action.y, action.z);
		Direction face = faceToward(player, pos);
		look(player, action.yaw, action.pitch);
		player.connection.send(new ServerboundMovePlayerPacket.Rot(
				player.getYRot(),
				player.getXRot(),
				player.onGround(),
				player.horizontalCollision
		));
		Vec3 hit = Vec3.atCenterOf(pos).add(face.getStepX() * 0.5, face.getStepY() * 0.5, face.getStepZ() * 0.5);
		minecraft.hitResult = new BlockHitResult(hit, face, pos, false);
		MineBotClient.breaking = true;
		if (action.breakStart) {
			if (vanillaDelayPending) {
				for (int i = 0; i < 5; i++) {
					gameMode.continueDestroyBlock(pos, face);
				}
				vanillaDelayPending = false;
			}
			gameMode.startDestroyBlock(pos, face);
			// startDestroyBlock only arms the break. Progress is added by continueDestroyBlock.
			gameMode.continueDestroyBlock(pos, face);
		} else {
			gameMode.continueDestroyBlock(pos, face);
		}
		if (!gameMode.isDestroying()) {
			vanillaDelayPending = true;
		}
		player.swing(InteractionHand.MAIN_HAND, player.getMainHandItem().getAttackAnimation(), false);
		player.connection.send(ServerboundPunchPacket.INSTANCE);
	}

	private void craft(Minecraft minecraft, LocalPlayer player, CraftClick click) {
		if (click == null) {
			return;
		}
		switch (click.kind) {
			case OPEN_INVENTORY -> minecraft.gui.setScreen(new InventoryScreen(player));
			case PLACE_TABLE -> {
				selectItem(player, "minecraft:crafting_table");
				useOn(minecraft.gameMode, player, new BlockPos(click.x, click.y, click.z), Direction.UP);
			}
			case OPEN_TABLE, OPEN_SMITHING -> useOn(
					minecraft.gameMode,
					player,
					new BlockPos(click.x, click.y, click.z),
					faceToward(player, new BlockPos(click.x, click.y, click.z))
			);
			case GRID -> enqueueGrid(player, click.itemId, click.gridIndex);
			case TAKE_RESULT -> {
				int result = player.containerMenu instanceof SmithingMenu ? 3 : 0;
				pending.add(new Click(result, 0, ContainerInput.QUICK_MOVE));
				pump(minecraft);
			}
		}
	}

	private void enqueueGrid(LocalPlayer player, String itemId, int gridIndex) {
		AbstractContainerMenu menu = player.containerMenu;
		int menuSlot = gridMenuSlot(menu, gridIndex);
		int source = findSourceSlot(menu, itemId);
		if (source < 0 || menuSlot < 0) {
			return;
		}
		ItemStack stack = menu.getSlot(source).getItem();
		pending.add(new Click(source, 0, ContainerInput.PICKUP));
		if (stack.getCount() <= 1) {
			pending.add(new Click(menuSlot, 0, ContainerInput.PICKUP));
		} else {
			pending.add(new Click(menuSlot, 1, ContainerInput.PICKUP));
			pending.add(new Click(source, 0, ContainerInput.PICKUP));
		}
		pump(Minecraft.getInstance());
	}

	private static int gridMenuSlot(AbstractContainerMenu menu, int gridIndex) {
		if (menu instanceof SmithingMenu) {
			return gridIndex;
		}
		if (menu instanceof CraftingMenu || menu instanceof InventoryMenu) {
			return gridIndex + 1;
		}
		return gridIndex + 1;
	}

	private static int findSourceSlot(AbstractContainerMenu menu, String itemId) {
		int reserved = reservedSlots(menu);
		for (int i = reserved; i < menu.slots.size(); i++) {
			ItemStack stack = menu.slots.get(i).getItem();
			if (!stack.isEmpty() && itemId.equals(itemId(stack))) {
				return i;
			}
		}
		return -1;
	}

	private static int reservedSlots(AbstractContainerMenu menu) {
		if (menu instanceof SmithingMenu) {
			return 4;
		}
		if (menu instanceof CraftingMenu) {
			return 10;
		}
		return 5;
	}

	private void shiftClick(LocalPlayer player, MultiPlayerGameMode gameMode, String itemId) {
		AbstractContainerMenu menu = player.containerMenu;
		for (int i = 0; i < menu.slots.size(); i++) {
			Slot slot = menu.slots.get(i);
			ItemStack stack = slot.getItem();
			if (stack.isEmpty() || !itemId.equals(itemId(stack))) {
				continue;
			}
			if (slot.container == player.getInventory()) {
				gameMode.handleContainerInput(menu.containerId, i, 0, ContainerInput.QUICK_MOVE, player);
				return;
			}
		}
	}

	private void click(Minecraft minecraft, Click click) {
		LocalPlayer player = minecraft.player;
		MultiPlayerGameMode gameMode = minecraft.gameMode;
		if (player == null || gameMode == null) {
			pending.clear();
			return;
		}
		AbstractContainerMenu menu = player.containerMenu;
		gameMode.handleContainerInput(menu.containerId, click.slot, click.button, click.type, player);
	}

	private static void useOn(MultiPlayerGameMode gameMode, LocalPlayer player, BlockPos pos, Direction face) {
		Vec3 hit = Vec3.atCenterOf(pos).add(face.getStepX() * 0.5, face.getStepY() * 0.5, face.getStepZ() * 0.5);
		gameMode.useItemOn(player, InteractionHand.MAIN_HAND, new BlockHitResult(hit, face, pos, false));
	}

	private static void look(LocalPlayer player, float yaw, float pitch) {
		player.setYRot(yaw);
		player.setXRot(pitch);
		player.setYHeadRot(yaw);
	}

	static boolean selectItem(LocalPlayer player, String itemId) {
		if (itemId == null || itemId.isEmpty()) {
			return false;
		}
		String want = Ids.normalize(itemId);
		var items = player.getInventory().getNonEquipmentItems();
		int found = -1;
		int bestUses = -1;
		for (int i = 0; i < items.size(); i++) {
			ItemStack stack = items.get(i);
			if (!want.equals(itemId(stack))) {
				continue;
			}
			int uses = stack.isDamageableItem() ? stack.getMaxDamage() - stack.getDamageValue() : Integer.MAX_VALUE;
			if (uses > bestUses) {
				bestUses = uses;
				found = i;
			}
		}
		if (found < 0) {
			return false;
		}
		int hotbar = net.minecraft.world.entity.player.Inventory.getSelectionSize();
		if (found < hotbar) {
			carry(player, found);
			return true;
		}
		int swapInto = player.getInventory().getSuitableHotbarSlot();
		int menuSlot = inventoryMenuSlot(found);
		player.inventoryMenu.getSlot(menuSlot);
		Minecraft minecraft = Minecraft.getInstance();
		if (minecraft.gameMode != null) {
			minecraft.gameMode.handleContainerInput(
					player.inventoryMenu.containerId,
					menuSlot,
					swapInto,
					ContainerInput.SWAP,
					player
			);
		}
		carry(player, swapInto);
		return true;
	}

	private static int inventoryMenuSlot(int nonEquipmentIndex) {
		int hotbar = net.minecraft.world.entity.player.Inventory.getSelectionSize();
		if (nonEquipmentIndex < hotbar) {
			return 36 + nonEquipmentIndex;
		}
		return nonEquipmentIndex;
	}

	private static void selectEmptyHand(LocalPlayer player) {
		String selected = itemId(player.getMainHandItem());
		if (!com.minebot.core.ToolItems.isTool(selected)) {
			return;
		}
		var items = player.getInventory().getNonEquipmentItems();
		int hotbar = net.minecraft.world.entity.player.Inventory.getSelectionSize();
		for (int i = 0; i < hotbar && i < items.size(); i++) {
			if (items.get(i).isEmpty()) {
				carry(player, i);
				return;
			}
		}
	}

	private static void carry(LocalPlayer player, int hotbarSlot) {
		player.getInventory().setSelectedSlot(hotbarSlot);
		player.connection.send(new ServerboundSetCarriedItemPacket(hotbarSlot));
	}

	static String itemId(ItemStack stack) {
		if (stack == null || stack.isEmpty()) {
			return "";
		}
		Identifier id = BuiltInRegistries.ITEM.getKey(stack.getItem());
		return id == null ? "" : id.toString();
	}

	static String blockId(net.minecraft.world.level.block.state.BlockState state) {
		Identifier id = BuiltInRegistries.BLOCK.getKey(state.getBlock());
		return id == null ? "minecraft:air" : id.toString();
	}

	private static Direction faceToward(LocalPlayer player, BlockPos pos) {
		double dx = player.getX() - (pos.getX() + 0.5);
		double dy = player.getEyeY() - (pos.getY() + 0.5);
		double dz = player.getZ() - (pos.getZ() + 0.5);
		double ax = Math.abs(dx);
		double ay = Math.abs(dy);
		double az = Math.abs(dz);
		if (ay >= ax && ay >= az) {
			return dy > 0 ? Direction.UP : Direction.DOWN;
		}
		if (ax >= az) {
			return dx > 0 ? Direction.EAST : Direction.WEST;
		}
		return dz > 0 ? Direction.SOUTH : Direction.NORTH;
	}

	private record Click(int slot, int button, ContainerInput type) {}
}
