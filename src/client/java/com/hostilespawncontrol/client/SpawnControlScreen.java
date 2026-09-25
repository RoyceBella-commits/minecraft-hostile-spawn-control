package com.hostilespawncontrol.client;

import com.hostilespawncontrol.registry.HostileMobRegistry;
import com.hostilespawncontrol.rule.SpawnRuleStore;
import com.hostilespawncontrol.rule.SpawnSource;
import java.util.EnumMap;
import java.util.Locale;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.ObjectSelectionList;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SpawnEggItem;
import org.jspecify.annotations.Nullable;

/**
 * Two-pane spawn manager: the left pane picks a hostile mob (search + list),
 * the right pane shows that mob's per-source spawn switches.
 * Rules live in the shared {@link SpawnRuleStore}; in singleplayer the integrated server reads the same instance.
 */
public class SpawnControlScreen extends Screen {
	private static final Component TITLE = Component.translatable("hostile_spawn_control.screen.title");
	private static final Component READ_ONLY = Component.translatable("hostile_spawn_control.screen.read_only");
	private static final Component SEARCH = Component.translatable("hostile_spawn_control.screen.search").withStyle(EditBox.SEARCH_HINT_STYLE);
	private static final Component RESTRICTED = Component.translatable("hostile_spawn_control.screen.restricted");
	private static final Component NO_MATCH = Component.translatable("hostile_spawn_control.screen.no_match");
	private static final int HEADER = 33;
	private static final int FOOTER = 33;
	private static final int PAD = 8;
	private static final int ROW_HEIGHT = 21;
	private static final int TOGGLE_WIDTH = 60;
	private static final int COLOR_TEXT = 0xFFFFFFFF;
	private static final int COLOR_MUTED = 0xFF808080;
	private static final int COLOR_WARN = 0xFFFFD24A;
	private static final int COLOR_ERROR = 0xFFFF7070;
	private static final int COLOR_DIVIDER = 0x40FFFFFF;

	private final @Nullable Screen lastScreen;
	private final boolean editable;
	private final Map<SpawnSource, CycleButton<Boolean>> toggles = new EnumMap<>(SpawnSource.class);
	private EditBox searchBox;
	private MobList mobList;
	private Button mobAllOn;
	private Button mobAllOff;
	private @Nullable EntityType<?> selectedType;
	private boolean dirty;

	private int leftWidth;
	private int rightX;
	private int rightWidth;

	public SpawnControlScreen(@Nullable Screen lastScreen) {
		super(TITLE);
		this.lastScreen = lastScreen;
		Minecraft minecraft = Minecraft.getInstance();
		// Rules are enforced by the server. Only a local (integrated) server shares this JVM's store.
		this.editable = minecraft.level == null || minecraft.hasSingleplayerServer();
	}

	@Override
	protected void init() {
		this.leftWidth = Math.clamp(this.width * 2 / 5, 150, 240);
		this.rightX = this.leftWidth + PAD * 2;
		this.rightWidth = this.width - this.rightX - PAD;
		int top = HEADER;
		int bottom = this.height - FOOTER;

		// Left pane: search + mob list
		String query = this.searchBox != null ? this.searchBox.getValue() : "";
		this.searchBox = new EditBox(this.font, PAD, top + 4, this.leftWidth - PAD, 20, SEARCH);
		this.searchBox.setHint(SEARCH);
		this.searchBox.setValue(query);
		this.searchBox.setResponder(this::onSearch);
		this.addRenderableWidget(this.searchBox);

		int listTop = top + 28;
		this.mobList = new MobList(this.leftWidth, bottom - listTop - 4, listTop);
		this.addRenderableWidget(this.mobList);

		// Right pane: one switch per spawn source for the selected mob
		this.toggles.clear();
		int rowY = top + 30;
		for (SpawnSource source : SpawnSource.values()) {
			CycleButton<Boolean> toggle = CycleButton.onOffBuilder(true)
				.displayOnlyValue()
				.create(this.rightX + this.rightWidth - TOGGLE_WIDTH, rowY, TOGGLE_WIDTH, 20, Component.translatable(source.translationKey()),
					(button, value) -> this.onToggle(source, value));
			toggle.setTooltip(Tooltip.create(Component.translatable(source.descriptionKey())));
			this.toggles.put(source, toggle);
			this.addRenderableWidget(toggle);
			rowY += ROW_HEIGHT;
		}

		int mobButtonWidth = Math.min(70, (this.rightWidth - TOGGLE_WIDTH - 12) / 2);
		this.mobAllOff = Button.builder(Component.translatable("hostile_spawn_control.screen.mob_all_off"), b -> this.setSelectedMobAll(false))
			.bounds(this.rightX + this.rightWidth - mobButtonWidth, top + 4, mobButtonWidth, 20)
			.build();
		this.mobAllOn = Button.builder(Component.translatable("hostile_spawn_control.screen.mob_all_on"), b -> this.setSelectedMobAll(true))
			.bounds(this.mobAllOff.getX() - mobButtonWidth - 4, top + 4, mobButtonWidth, 20)
			.build();
		this.addRenderableWidget(this.mobAllOn);
		this.addRenderableWidget(this.mobAllOff);

		// Footer: global switches
		int footerY = this.height - FOOTER + 7;
		int footerWidth = 100 * 3 + 8 * 2;
		int footerX = (this.width - footerWidth) / 2;
		Button enableAll = Button.builder(Component.translatable("hostile_spawn_control.screen.enable_all"), b -> this.setEverything(true))
			.bounds(footerX, footerY, 100, 20)
			.build();
		Button disableAll = Button.builder(Component.translatable("hostile_spawn_control.screen.disable_all"), b -> this.setEverything(false))
			.bounds(footerX + 108, footerY, 100, 20)
			.build();
		enableAll.active = this.editable;
		disableAll.active = this.editable;
		this.addRenderableWidget(enableAll);
		this.addRenderableWidget(disableAll);
		this.addRenderableWidget(Button.builder(CommonComponents.GUI_DONE, b -> this.onClose()).bounds(footerX + 216, footerY, 100, 20).build());

		this.mobList.refill(query);
		this.refreshDetail();
	}

	@Override
	protected void setInitialFocus() {
		this.setInitialFocus(this.searchBox);
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
		super.extractRenderState(graphics, mouseX, mouseY, a);
		graphics.centeredText(this.font, this.title, this.width / 2, 12, COLOR_TEXT);
		if (!this.editable) {
			graphics.centeredText(this.font, READ_ONLY, this.width / 2, 22, COLOR_ERROR);
		}

		int top = HEADER;
		graphics.fill(this.leftWidth + PAD, top + 4, this.leftWidth + PAD + 1, this.height - FOOTER - 4, COLOR_DIVIDER);

		if (this.selectedType == null) {
			graphics.text(this.font, NO_MATCH, this.rightX, top + 10, COLOR_MUTED);
			return;
		}

		int nameMaxWidth = this.mobAllOn.getX() - this.rightX - 4;
		graphics.text(this.font, this.font.plainSubstrByWidth(this.selectedType.getDescription().getString(), nameMaxWidth), this.rightX, top + 5, COLOR_TEXT);
		graphics.text(this.font, this.font.plainSubstrByWidth(HostileMobRegistry.id(this.selectedType).toString(), nameMaxWidth), this.rightX, top + 16, COLOR_MUTED);

		for (Map.Entry<SpawnSource, CycleButton<Boolean>> entry : this.toggles.entrySet()) {
			CycleButton<Boolean> toggle = entry.getValue();
			graphics.text(this.font, Component.translatable(entry.getKey().translationKey()), this.rightX, toggle.getY() + 6, COLOR_TEXT);
		}
	}

	@Override
	public void onClose() {
		if (this.dirty) {
			SpawnRuleStore.get().save();
			this.dirty = false;
		}
		this.minecraft.gui.setScreen(this.lastScreen);
	}

	private void onSearch(String query) {
		this.mobList.refill(query);
		this.refreshDetail();
	}

	private void select(@Nullable EntityType<?> type) {
		this.selectedType = type;
		this.refreshDetail();
	}

	private void onToggle(SpawnSource source, boolean allowed) {
		if (this.selectedType == null) {
			return;
		}
		SpawnRuleStore.get().set(HostileMobRegistry.id(this.selectedType), source, allowed);
		this.dirty = true;
	}

	private void setSelectedMobAll(boolean allowed) {
		if (this.selectedType == null) {
			return;
		}
		SpawnRuleStore.get().setMob(HostileMobRegistry.id(this.selectedType), allowed);
		this.dirty = true;
		this.refreshDetail();
	}

	private void setEverything(boolean allowed) {
		SpawnRuleStore.get().setAll(HostileMobRegistry.ids(), null, allowed);
		this.dirty = true;
		this.refreshDetail();
	}

	private void refreshDetail() {
		boolean hasSelection = this.selectedType != null;
		SpawnRuleStore store = SpawnRuleStore.get();
		for (Map.Entry<SpawnSource, CycleButton<Boolean>> entry : this.toggles.entrySet()) {
			CycleButton<Boolean> toggle = entry.getValue();
			toggle.visible = hasSelection;
			toggle.active = hasSelection && this.editable;
			if (hasSelection) {
				toggle.setValue(store.isAllowed(HostileMobRegistry.id(this.selectedType), entry.getKey()));
			}
		}
		this.mobAllOn.visible = hasSelection;
		this.mobAllOff.visible = hasSelection;
		this.mobAllOn.active = hasSelection && this.editable;
		this.mobAllOff.active = hasSelection && this.editable;
	}

	private static boolean matches(EntityType<?> type, String query) {
		if (query.isEmpty()) {
			return true;
		}
		Identifier id = HostileMobRegistry.id(type);
		return type.getDescription().getString().toLowerCase(Locale.ROOT).contains(query)
			|| id.toString().contains(query)
			|| type.toShortString().contains(query);
	}

	private class MobList extends ObjectSelectionList<MobEntry> {
		private final int rowWidth;

		MobList(int width, int height, int y) {
			super(SpawnControlScreen.this.minecraft, width, height, y, 20);
			this.setX(0);
			this.rowWidth = width - PAD - 8;
		}

		@Override
		public int getRowWidth() {
			return this.rowWidth;
		}

		@Override
		public int getRowLeft() {
			return this.getX() + PAD;
		}

		@Override
		protected int scrollBarX() {
			return this.getRowRight() + 2;
		}

		@Override
		public void setSelected(@Nullable MobEntry selected) {
			super.setSelected(selected);
			SpawnControlScreen.this.select(selected == null ? null : selected.type);
		}

		void refill(String rawQuery) {
			String query = rawQuery.trim().toLowerCase(Locale.ROOT);
			EntityType<?> previous = SpawnControlScreen.this.selectedType;
			this.clearEntries();
			MobEntry keep = null;
			for (EntityType<?> type : HostileMobRegistry.all()) {
				if (matches(type, query)) {
					MobEntry entry = new MobEntry(type);
					this.addEntry(entry);
					if (type == previous) {
						keep = entry;
					}
				}
			}
			this.refreshScrollAmount();
			if (keep == null && !this.children().isEmpty()) {
				keep = this.children().getFirst();
			}
			this.setSelected(keep);
		}
	}

	private class MobEntry extends ObjectSelectionList.Entry<MobEntry> {
		private final EntityType<?> type;
		private final Identifier id;
		private final ItemStack icon;

		MobEntry(EntityType<?> type) {
			this.type = type;
			this.id = HostileMobRegistry.id(type);
			this.icon = SpawnEggItem.byId(type).map(ItemStack::new).orElse(ItemStack.EMPTY);
		}

		@Override
		public void extractContent(GuiGraphicsExtractor graphics, int mouseX, int mouseY, boolean hovered, float a) {
			int x = this.getContentX();
			int y = this.getContentY();
			int right = this.getContentRight();
			if (!this.icon.isEmpty()) {
				graphics.item(this.icon, x, this.getContentYMiddle() - 8);
			}

			int textX = x + 20;
			int textY = this.getContentYMiddle() - 4;
			int nameMax = right - textX;
			if (SpawnRuleStore.get().hasRestriction(this.id)) {
				int tagWidth = SpawnControlScreen.this.font.width(RESTRICTED);
				graphics.text(SpawnControlScreen.this.font, RESTRICTED, right - tagWidth, textY, COLOR_WARN);
				nameMax -= tagWidth + 4;
			}
			String name = SpawnControlScreen.this.font.plainSubstrByWidth(this.type.getDescription().getString(), nameMax);
			graphics.text(SpawnControlScreen.this.font, name, textX, textY, COLOR_TEXT);
		}

		@Override
		public Component getNarration() {
			return this.type.getDescription();
		}
	}
}
