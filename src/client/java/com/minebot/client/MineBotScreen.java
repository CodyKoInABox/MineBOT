package com.minebot.client;

import com.minebot.core.BlockPos;
import com.minebot.core.DepthSpec;
import com.minebot.core.Ids;
import com.minebot.core.Presets;
import com.minebot.core.Status;
import com.minebot.core.TaskConfig;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/** One dark screen for the whole job. Not a debug dump. */
public final class MineBotScreen extends Screen {
	private static final int PANEL_W = 440;
	private static final int ROW = 22;

	private final TaskConfig working = MineBotClient.config();
	private final EditBox ax = box(12);
	private final EditBox ay = box(12);
	private final EditBox az = box(12);
	private final EditBox bx = box(12);
	private final EditBox by = box(12);
	private final EditBox bz = box(12);
	private final EditBox depthValue = box(12);
	private final EditBox blockIds = box(2048);
	private final EditBox radius = box(6);
	private final EditBox depositIds = box(2048);
	private final EditBox keepIds = box(4096);
	private final EditBox torchSpacing = box(4);
	private final EditBox hunger = box(3);
	private final EditBox presetName = box(48);

	private int scroll;
	private final List<Label> labels = new ArrayList<>();

	public MineBotScreen() {
		super(Component.literal("MineBOT"));
		read(working);
	}

	@Override
	protected void init() {
		layout();
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}

	@Override
	public void removed() {
		writeBack();
		super.removed();
	}

	@Override
	public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
		scroll = Math.max(0, Math.min(maxScroll(), scroll - (int) Math.round(scrollY * 16)));
		layout();
		return true;
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
		graphics.fill(0, 0, this.width, this.height, 0xFF0B0E14);
		int left = panelLeft();
		int right = left + PANEL_W;
		int top = 10;
		int bottom = this.height - 10;
		graphics.fill(left, top, right, bottom, 0xF012161E);
		graphics.fill(left, top, right, top + 2, 0xFFE0A45C);
		graphics.fill(left, bottom - 2, right, bottom, 0xFF2A3344);
		graphics.text(this.font, "MineBOT", left + 16, top + 10, 0xFFE0A45C);
		graphics.text(this.font, "one action per tick", left + 78, top + 10, 0xFF8E98A8);
		for (Label label : labels) {
			if (label.y > top + 24 && label.y < bottom - 28) {
				graphics.text(this.font, label.text, label.x, label.y, label.color);
			}
		}
		String status = MineBotClient.status == null ? Status.STOPPED : MineBotClient.status;
		int color = Status.PAUSED_CHESTS_FULL.equals(status) ? 0xFFE06C75 : 0xFFD5DBE6;
		graphics.text(this.font, status, left + 16, bottom - 16, color);
		super.extractRenderState(graphics, mouseX, mouseY, partialTick);
	}

	private void layout() {
		clearWidgets();
		labels.clear();
		int x = panelLeft() + 14;
		int n = 0;
		label(x, n, "AREA", 0xFFE0A45C);
		n++;
		place(ax, x + 52, n, 48);
		place(ay, x + 104, n, 40);
		place(az, x + 148, n, 48);
		label(x, n, "Corner A", 0xFFD5DBE6);
		addRenderableWidget(button("Use feet", x + 206, n, 90, () -> fillCorner(ax, ay, az, MineBotClient.feet())));
		n++;
		place(bx, x + 52, n, 48);
		place(by, x + 104, n, 40);
		place(bz, x + 148, n, 48);
		label(x, n, "Corner B", 0xFFD5DBE6);
		addRenderableWidget(button("Use feet", x + 206, n, 90, () -> fillCorner(bx, by, bz, MineBotClient.feet())));
		n++;
		label(x, n, "DEPTH", 0xFFE0A45C);
		n++;
		addRenderableWidget(button(depthLabel(), x, n, 120, this::cycleDepth));
		place(depthValue, x + 128, n, 64);
		label(x + 198, n, depthHint(), 0xFF8E98A8);
		n++;
		addRenderableWidget(button(flag("Stair", cfg().stair), x, n, 140, () -> {
			cfg().stair = !cfg().stair;
			layout();
		}));
		n++;
		label(x, n, "MINING", 0xFFE0A45C);
		n++;
		addRenderableWidget(button(flag("Everything", cfg().mine.everything), x, n, 140, () -> {
			cfg().mine.everything = !cfg().mine.everything;
			layout();
		}));
		addRenderableWidget(button(flag("Dig in the way", cfg().mine.digObstructions), x + 148, n, 150, () -> {
			cfg().mine.digObstructions = !cfg().mine.digObstructions;
			layout();
		}));
		n++;
		place(blockIds, x, n, 300);
		label(x + 306, n, "block ids", 0xFF8E98A8);
		n++;
		label(x, n, "CHESTS", 0xFFE0A45C);
		n++;
		place(radius, x + 78, n, 40);
		label(x, n, "Radius", 0xFFD5DBE6);
		addRenderableWidget(button("Mark chest", x + 128, n, 100, this::markChest));
		n++;
		place(depositIds, x, n, 300);
		label(x + 306, n, "deposit", 0xFF8E98A8);
		n++;
		place(keepIds, x, n, 300);
		label(x + 306, n, "keep", 0xFF8E98A8);
		n++;
		label(x, n, "SURVIVAL", 0xFFE0A45C);
		n++;
		addRenderableWidget(button(flag("Eat", cfg().survival.eat), x, n, 70, () -> {
			cfg().survival.eat = !cfg().survival.eat;
			layout();
		}));
		addRenderableWidget(button(flag("Sleep", cfg().survival.sleep), x + 76, n, 80, () -> {
			cfg().survival.sleep = !cfg().survival.sleep;
			layout();
		}));
		addRenderableWidget(button(flag("Torches", cfg().survival.placeTorches), x + 162, n, 90, () -> {
			cfg().survival.placeTorches = !cfg().survival.placeTorches;
			layout();
		}));
		addRenderableWidget(button(flag("Craft picks", cfg().survival.autoCraftPicks), x + 258, n, 110, () -> {
			cfg().survival.autoCraftPicks = !cfg().survival.autoCraftPicks;
			layout();
		}));
		n++;
		place(torchSpacing, x + 78, n, 36);
		place(hunger, x + 210, n, 36);
		label(x, n, "Spacing", 0xFFD5DBE6);
		label(x + 124, n, "Hunger ≤", 0xFFD5DBE6);
		n++;
		label(x, n, "PRESETS", 0xFFE0A45C);
		n++;
		place(presetName, x, n, 150);
		addRenderableWidget(button("Save", x + 156, n, 60, this::savePreset));
		addRenderableWidget(button("Load", x + 220, n, 60, this::loadPreset));
		addRenderableWidget(button("Example quarry", x + 286, n, 120, this::loadExample));
		n++;
		addRenderableWidget(button("Start", x, n, 120, this::start));
		addRenderableWidget(button("Stop", x + 128, n, 120, MineBotClient::stopJob));
	}

	private void start() {
		writeBack();
		MineBotClient.startJob();
	}

	private void savePreset() {
		writeBack();
		String name = presetName.getValue().trim();
		if (name.isEmpty()) {
			MineBotClient.status = "Name the preset first";
			return;
		}
		MineBotClient.savePreset(name, cfg());
		MineBotClient.status = "Saved " + name;
	}

	private void loadPreset() {
		String name = presetName.getValue().trim();
		TaskConfig loaded = MineBotClient.loadPreset(name, MineBotClient.feet());
		if (loaded == null) {
			MineBotClient.status = "No preset named " + name;
			return;
		}
		copyIntoWorking(loaded);
		read(working);
		layout();
		MineBotClient.status = "Loaded " + name;
	}

	private void loadExample() {
		TaskConfig loaded = Presets.exampleQuarry(MineBotClient.feet());
		copyIntoWorking(loaded);
		MineBotClient.savePreset(Presets.EXAMPLE_QUARRY, working);
		read(working);
		presetName.setValue(Presets.EXAMPLE_QUARRY);
		layout();
		MineBotClient.status = "Loaded Example quarry";
	}

	private void markChest() {
		writeBack();
		BlockPos marked = MineBotClient.feet();
		var minecraft = net.minecraft.client.Minecraft.getInstance();
		if (minecraft.player != null && minecraft.level != null) {
			var origin = minecraft.player.blockPosition();
			search:
			for (int dy = -1; dy <= 2; dy++) {
				for (int dz = -2; dz <= 2; dz++) {
					for (int dx = -2; dx <= 2; dx++) {
						var pos = origin.offset(dx, dy, dz);
						String id = ActionPerformer.blockId(minecraft.level.getBlockState(pos));
						if (id.equals("minecraft:chest") || id.equals("minecraft:trapped_chest") || id.equals("minecraft:barrel")) {
							marked = new BlockPos(pos.getX(), pos.getY(), pos.getZ());
							break search;
						}
					}
				}
			}
		}
		cfg().storage.markedChest = marked;
		MineBotClient.replaceConfig(cfg());
		MineBotClient.status = "Marked chest at " + marked;
	}

	private void cycleDepth() {
		cfg().depth.kind = switch (cfg().depth.kind) {
			case BEDROCK -> DepthSpec.Kind.UNTIL_Y;
			case UNTIL_Y -> DepthSpec.Kind.LAYERS;
			case LAYERS -> DepthSpec.Kind.BEDROCK;
		};
		layout();
	}

	private void writeBack() {
		TaskConfig config = cfg();
		config.area.setFirst(corner(ax, ay, az));
		config.area.setSecond(corner(bx, by, bz));
		Integer depth = integer(depthValue);
		if (config.depth.kind == DepthSpec.Kind.UNTIL_Y && depth != null) {
			config.depth.y = depth;
		}
		if (config.depth.kind == DepthSpec.Kind.LAYERS && depth != null) {
			config.depth.layers = Math.max(1, depth);
		}
		config.mine.blockIds = Ids.parseList(blockIds.getValue());
		Integer rad = integer(radius);
		if (rad != null) {
			config.storage.searchRadius = Math.max(1, rad);
		}
		config.storage.depositIds = Ids.parseList(depositIds.getValue());
		config.storage.keepIds = Ids.parseList(keepIds.getValue());
		Integer spacing = integer(torchSpacing);
		if (spacing != null) {
			config.survival.torchSpacing = Math.max(1, spacing);
		}
		Integer food = integer(hunger);
		if (food != null) {
			config.survival.hungerThreshold = food;
		}
		config.name = presetName.getValue().trim();
		MineBotClient.replaceConfig(config);
	}

	private void read(TaskConfig config) {
		if (config.area.first != null) {
			ax.setValue(Integer.toString(config.area.first.x));
			ay.setValue(Integer.toString(config.area.first.y));
			az.setValue(Integer.toString(config.area.first.z));
		}
		if (config.area.second != null) {
			bx.setValue(Integer.toString(config.area.second.x));
			by.setValue(Integer.toString(config.area.second.y));
			bz.setValue(Integer.toString(config.area.second.z));
		}
		depthValue.setValue(switch (config.depth.kind) {
			case UNTIL_Y -> Integer.toString(config.depth.y);
			case LAYERS -> Integer.toString(config.depth.layers);
			case BEDROCK -> "";
		});
		blockIds.setValue(Ids.join(config.mine.blockIds));
		radius.setValue(Integer.toString(config.storage.searchRadius));
		depositIds.setValue(Ids.join(config.storage.depositIds));
		keepIds.setValue(Ids.join(config.storage.keepIds));
		torchSpacing.setValue(Integer.toString(config.survival.torchSpacing));
		hunger.setValue(Integer.toString(config.survival.hungerThreshold));
		presetName.setValue(config.name == null ? "" : config.name);
	}

	private TaskConfig cfg() {
		return working;
	}

	private void copyIntoWorking(TaskConfig loaded) {
		TaskConfig copy = loaded.copy();
		working.name = copy.name;
		working.area = copy.area;
		working.depth = copy.depth;
		working.stair = copy.stair;
		working.mine = copy.mine;
		working.storage = copy.storage;
		working.survival = copy.survival;
	}

	private void fillCorner(EditBox xBox, EditBox yBox, EditBox zBox, BlockPos pos) {
		xBox.setValue(Integer.toString(pos.x));
		yBox.setValue(Integer.toString(pos.y));
		zBox.setValue(Integer.toString(pos.z));
		writeBack();
	}

	private BlockPos corner(EditBox xBox, EditBox yBox, EditBox zBox) {
		Integer x = integer(xBox);
		Integer y = integer(yBox);
		Integer z = integer(zBox);
		if (x == null || y == null || z == null) {
			return null;
		}
		return new BlockPos(x, y, z);
	}

	private static Integer integer(EditBox box) {
		String value = box.getValue().trim();
		if (value.isEmpty() || value.equals("-")) {
			return null;
		}
		try {
			return Integer.parseInt(value);
		} catch (NumberFormatException ignored) {
			return null;
		}
	}

	private String depthLabel() {
		return switch (cfg().depth.kind) {
			case BEDROCK -> "Depth: bedrock";
			case UNTIL_Y -> "Depth: until Y";
			case LAYERS -> "Depth: layers";
		};
	}

	private String depthHint() {
		return switch (cfg().depth.kind) {
			case BEDROCK -> "stops above bedrock";
			case UNTIL_Y -> "inclusive Y";
			case LAYERS -> "layer count";
		};
	}

	private static String flag(String name, boolean on) {
		return name + (on ? "  on" : "  off");
	}

	private EditBox box(int max) {
		EditBox edit = new EditBox(net.minecraft.client.Minecraft.getInstance().font, 0, 0, 40, 18, Component.empty());
		edit.setMaxLength(max);
		edit.setBordered(true);
		return edit;
	}

	private void place(EditBox edit, int x, int row, int width) {
		int y = rowY(row);
		edit.setX(x);
		edit.setY(y);
		edit.setWidth(width);
		if (y > -20 && y < this.height) {
			addRenderableWidget(edit);
		}
	}

	private Button button(String text, int x, int row, int width, Runnable action) {
		return Button.builder(Component.literal(text), button -> action.run())
				.bounds(x, rowY(row), width, 18)
				.build();
	}

	private void label(int x, int row, String text, int color) {
		labels.add(new Label(x, rowY(row) + 4, text, color));
	}

	private int rowY(int row) {
		return 36 + row * ROW - scroll;
	}

	private int panelLeft() {
		return Math.max(8, (this.width - PANEL_W) / 2);
	}

	private int maxScroll() {
		return Math.max(0, 36 + 20 * ROW - (this.height - 40));
	}

	private record Label(int x, int y, String text, int color) {}
}
