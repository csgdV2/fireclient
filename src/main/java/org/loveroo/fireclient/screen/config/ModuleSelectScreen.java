package org.loveroo.fireclient.screen.config;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.tooltip.Tooltip;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;

import org.loveroo.fireclient.FireClient;
import org.loveroo.fireclient.client.FireClientside;
import org.loveroo.fireclient.screen.base.ConfigScreenBase;
import org.loveroo.fireclient.screen.base.ScrollableWidget;
import org.loveroo.fireclient.screen.widgets.ModuleCardWidget;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

public class ModuleSelectScreen extends ConfigScreenBase {

    private ScrollableWidget modulesWidget;
    private ButtonWidget backButton;
    private TextFieldWidget searchBar;

    private String search = "";

    private final HashMap<String, ModuleCardWidget> moduleCards = new HashMap<>();

    private static final int CARD_WIDTH = 120;
    private static final int CARD_HEIGHT = 100;
    private static final int CARD_SPACING = 10;
    private static final int COLUMNS = 3;

    private final int moduleSelectWidth = (CARD_WIDTH + CARD_SPACING) * COLUMNS + 30;
    private final int moduleSelectHeight = 220;

    private static double scroll = 0.0;

    public ModuleSelectScreen() {
        super(Text.translatable("fireclient.screen.module_select.title"));
    }

    @Override
    public void init() {
        for(var module : FireClientside.getModules()) {
            var card = new ModuleCardWidget(module, 0, 0, CARD_WIDTH, CARD_HEIGHT);
            moduleCards.put(module.getData().getId(), card);
        }

        backButton = ButtonWidget.builder(Text.translatable("fireclient.screen.settings.back.name"), this::backButtonPressed)
            .dimensions(width/2 - 40, height/2 + moduleSelectHeight/2 + 20, 80, 20)
            .tooltip(Tooltip.of(Text.translatable("fireclient.screen.settings.back.tooltip")))
            .build();
            
        addDrawableChild(backButton);
            
        modulesWidget = new ScrollableWidget(this, moduleSelectWidth, moduleSelectHeight, 0, CARD_HEIGHT + CARD_SPACING, List.of());
        modulesWidget.setPosition(width/2 - (moduleSelectWidth/2), height/2 - (moduleSelectHeight/2));
        modulesWidget.setScrollY(scroll);
        
        addDrawableChild(modulesWidget);
        
        var barWidth = moduleSelectWidth - 120;
        searchBar = new TextFieldWidget(client.textRenderer, barWidth, 15, Text.literal(""));
        searchBar.setPosition(width/2 - (barWidth/2), height/2 - (moduleSelectHeight/2) - 20);
        
        searchBar.setChangedListener(this::refreshSearch);
        searchBar.setText(search);

        addDrawableChild(searchBar);
        setFocused(searchBar);
    }

    private void filterModuleButtons() {
        var filter = search.toLowerCase().trim();
        var sortedModules = new ArrayList<>(FireClientside.getModules().stream()
        .filter((module) -> {
            var nameSplit = module.getData().getName().getString().split(" ");

            for(var name : nameSplit) {
                if(name.toLowerCase().startsWith(filter)) {
                    return true;
                }
            }
            
            return false;
        })
        .collect(Collectors.toList()));
        
        sortedModules.sort((module1, module2) -> {
            var favorited1 = module1.getData().isFavorited();
            var favorited2 = module2.getData().isFavorited();

            if((favorited1 && favorited2) || (!favorited1 && !favorited2)) {
                return module1.getData().getId().compareTo(module2.getData().getId());
            }
            else if(favorited1 &&! favorited2) {
                return -1;
            }
            else if(!favorited1 && favorited2) {
                return 1;
            }

            return 0;
        });

        var modules = sortedModules.stream().map((module) -> module.getData().getId()).toList();
        var cards = new ArrayList<ModuleCardWidget>();

        var skips = 0;
        for(var i = 0; i < modules.size(); i++) {
            var module = FireClientside.getModule(modules.get(i));

            if(module.getData().isSkip()) {
                skips++;
                continue;
            }

            var index = i - skips;
            var col = index % COLUMNS;

            int totalGridWidth = COLUMNS * CARD_WIDTH + (COLUMNS - 1) * CARD_SPACING;
            int startX = width / 2 - totalGridWidth / 2;
            int cardX = startX + col * (CARD_WIDTH + CARD_SPACING);

            var card = moduleCards.get(module.getData().getId());
            card.setPosition(cardX, 0);

            cards.add(card);
        }

        var entries = new ArrayList<ScrollableWidget.ElementEntry>();
        
        var size = cards.size();
        var lineCount = (int)Math.ceil(size / (double) COLUMNS);

        for(int i = 0; i < lineCount; i++) {
            var entryWidgets = new ArrayList<ClickableWidget>();

            var cardEntryIndex = (i * COLUMNS);
            var cardEntryCount = Math.min(COLUMNS, size - cardEntryIndex);

            for(int k = 0; k < cardEntryCount; k++) {
                entryWidgets.add(cards.get(cardEntryIndex + k));
            }

            var entry = new ScrollableWidget.ElementEntry(entryWidgets);
            entries.add(entry);
        }

        modulesWidget.setEntries(entries);
        if(modules.size() == moduleCards.size()) {
            modulesWidget.setScrollY(scroll);
        }
        else {
            modulesWidget.setScrollY(0);
        }
    }

    private void refreshSearch(String input) {
        search = input;
        filterModuleButtons();
    }

    @Override
    public void exitOnInventory() { }

    private void backButtonPressed(ButtonWidget button) {
        MinecraftClient.getInstance().setScreen(new MainConfigScreen());
    }

    @Override
    public void tick() {
        scroll = modulesWidget.getScrollY();
    }

    @Override
    protected boolean escapePressed() {
        MinecraftClient.getInstance().setScreen(new MainConfigScreen());
        return true;
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);
        var text = MinecraftClient.getInstance().textRenderer;

        context.drawCenteredTextWithShadow(text, Text.translatable("fireclient.screen.module_select.header"), width/2, height/2 - (moduleSelectHeight/2 + 30), 0xFFFFFFFF);
    }

    public static void resetScroll() {
        scroll = 0;
    }
}
