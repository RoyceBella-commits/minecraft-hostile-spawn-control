package com.hostilespawncontrol.client;

import com.hostilespawncontrol.registry.HostileMobRegistry;
import com.hostilespawncontrol.rule.SpawnRuleStore;
import com.hostilespawncontrol.rule.SpawnSource;
import java.util.List;
import java.util.Locale;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.ContainerObjectSelectionList;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.MultiLineTextWidget;
import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.layouts.HeaderAndFooterLayout;
import net.minecraft.client.gui.layouts.LayoutSettings;
import net.minecraft.client.gui.layouts.LinearLayout;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EntityType;
import org.jspecify.annotations.Nullable;

/**
 * V1 settings UI: a vanilla-style list with one natural-spawn ON/OFF switch per hostile mob.
 * Rules live in the shared {@link SpawnRuleStore}; in singleplayer the integrated server reads the same instance.
 */
public class SpawnControlScreen extends Screen {
	private static final Component TITLE = Component.translatable("hostile_spawn_control.screen.title");
	private static final Component SUBTITLE = Component.translatable("hostile_spawn_control.screen.subtitle").withColor(0xFFA0A0A0);
	private static final Component READ_ONLY = Component.translatable("hostile_spawn_control.screen.read_only").withColor(0xFFFF7070);
	private static final Component SEARCH = Component.translatable("hostile_spawn_control.screen.search").withStyle(EditBox.SEARCH_HINT_STYLE);

	private final @Nullable Screen lastScreen;
	private final HeaderAndFooterLayout layout = new HeaderAndFooterLayout(this, 61, 33);
	private final boolean editable;
	private @Nullable MobList mobList;
	private EditBox searchBox;
	private boolean dirty;

	public SpawnControlScreen(@Nullable Screen lastScreen) {
		super(TITLE);
		this.lastScreen = lastScreen;
		Minecraft minecraft = Minecraft.getInstance();
		// Rules are enforced by the server. Only a local (integrated) server shares this JVM's store.
		this.editable = minecraft.level == null || minecraft.hasSingleplayerServer();
	}

	@Override
	protected void init() {
		LinearLayout header = this.layout.addToHeader(LinearLayout.vertical().spacing(6));
		this.mobList = new MobList();
		int rowWidth = this.mobList.getRowWidth();

		LinearLayout titleRow = LinearLayout.horizontal().spacing(8);
		titleRow.addChild(new StringWidget(TITLE, this.font), titleRow.newCellSettings().alignVerticallyMiddle());
		this.searchBox = new EditBox(this.font, 0, 0, rowWidth / 2, 20, this.searchBox, SEARCH);
		this.searchBox.setHint(SEARCH);
		this.searchBox.setResponder(value -> this.mobList.updateSearch(value));
		titleRow.addChild(this.searchBox);
		header.addChild(titleRow, LayoutSettings::alignHorizontallyCenter);
		header.addChild(
			new MultiLineTextWidget(this.editable ? SUBTITLE : READ_ONLY, this.font).setMaxWidth(rowWidth).setCentered(true),
			LayoutSettings::alignHorizontallyCenter
		);

		this.layout.addToContents(this.mobList);

		LinearLayout footer = this.layout.addToFooter(LinearLayout.horizontal().spacing(8));
		Button enableAll = Button.builder(Component.translatable("hostile_spawn_control.screen.enable_all"), b -> this.setAll(true)).width(100).build();
		Button disableAll = Button.builder(Component.translatable("hostile_spawn_control.screen.disable_all"), b -> this.setAll(false)).width(100).build();
		enableAll.active = this.editable;
		disableAll.active = this.editable;
		footer.addChild(enableAll);
		footer.addChild(disableAll);
		footer.addChild(Button.builder(CommonComponents.GUI_DONE, b -> this.onClose()).width(100).build());

		this.layout.visitWidgets(this::addRenderableWidget);
		this.repositionElements();
	}

	@Override
	protected void setInitialFocus() {
		this.setInitialFocus(this.searchBox);
	}

	@Override
	protected void repositionElements() {
		this.layout.arrangeElements();
		if (this.mobList != null) {
			this.mobList.updateSize(this.width, this.layout);
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

	private void setAll(boolean allowed) {
		SpawnRuleStore store = SpawnRuleStore.get();
		for (Identifier id : HostileMobRegistry.ids()) {
			store.set(id, SpawnSource.NATURAL, allowed);
		}
		this.dirty = true;
		if (this.mobList != null) {
			this.mobList.refreshEntries();
		}
	}

	private class MobList extends ContainerObjectSelectionList<MobEntry> {
		MobList() {
			super(
				Minecraft.getInstance(),
				SpawnControlScreen.this.width,
				SpawnControlScreen.this.layout.getContentHeight(),
				SpawnControlScreen.this.layout.getHeaderHeight(),
				22
			);
			this.updateSearch("");
		}

		@Override
		public int getRowWidth() {
			return 340;
		}

		void refreshEntries() {
			this.children().forEach(MobEntry::refresh);
		}

		void updateSearch(String query) {
			String q = query.trim().toLowerCase(Locale.ROOT);
			this.clearEntries();
			for (EntityType<?> type : HostileMobRegistry.all()) {
				Identifier id = HostileMobRegistry.id(type);
				String name = type.getDescription().getString().toLowerCase(Locale.ROOT);
				if (q.isEmpty() || name.contains(q) || id.toString().contains(q)) {
					this.addEntry(new MobEntry(type, id));
				}
			}
			this.refreshScrollAmount();
		}
	}

	private class MobEntry extends ContainerObjectSelectionList.Entry<MobEntry> {
		private final EntityType<?> type;
		private final Identifier id;
		private final Component idText;
		private final CycleButton<Boolean> toggle;

		MobEntry(EntityType<?> type, Identifier id) {
			this.type = type;
			this.id = id;
			this.idText = Component.literal(id.toString()).withColor(0xFF808080);
			this.toggle = CycleButton.onOffBuilder(SpawnRuleStore.get().isAllowed(id, SpawnSource.NATURAL))
				.displayOnlyValue()
				.create(0, 0, 60, 20, type.getDescription(), (button, value) -> {
					SpawnRuleStore.get().set(this.id, SpawnSource.NATURAL, value);
					SpawnControlScreen.this.dirty = true;
				});
			this.toggle.active = SpawnControlScreen.this.editable;
		}

		void refresh() {
			this.toggle.setValue(SpawnRuleStore.get().isAllowed(this.id, SpawnSource.NATURAL));
		}

		@Override
		public void extractContent(GuiGraphicsExtractor graphics, int mouseX, int mouseY, boolean hovered, float a) {
			int x = this.getContentX();
			int y = this.getContentY();
			graphics.text(SpawnControlScreen.this.font, this.type.getDescription(), x, y + 6, 0xFFFFFFFF);
			int nameWidth = SpawnControlScreen.this.font.width(this.type.getDescription());
			graphics.text(SpawnControlScreen.this.font, this.idText, x + nameWidth + 8, y + 6, 0xFF808080);
			this.toggle.setX(x + this.getContentWidth() - this.toggle.getWidth());
			this.toggle.setY(y);
			this.toggle.extractRenderState(graphics, mouseX, mouseY, a);
		}

		@Override
		public List<? extends GuiEventListener> children() {
			return List.of(this.toggle);
		}

		@Override
		public List<? extends NarratableEntry> narratables() {
			return List.of(this.toggle);
		}
	}
}
