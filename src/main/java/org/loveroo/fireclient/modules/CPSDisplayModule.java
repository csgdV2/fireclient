package org.loveroo.fireclient.modules;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import org.loveroo.fireclient.RooHelper;
import org.loveroo.fireclient.client.FireClientside;
import org.loveroo.fireclient.data.Color;
import org.loveroo.fireclient.data.JsonOption;
import org.loveroo.fireclient.data.ModuleData;
import org.loveroo.fireclient.keybind.Keybind;
import org.loveroo.fireclient.screen.widgets.ToggleButtonWidget;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.tooltip.Tooltip;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.text.Text;

public class CPSDisplayModule extends ModuleBase {

    private static final Color color1 = Color.fromRGB(0xFFD700);
    private static final Color color2 = Color.fromRGB(0xFFA500);

    private static final LinkedList<Long> leftClicks = new LinkedList<>();
    private static final LinkedList<Long> rightClicks = new LinkedList<>();

    @JsonOption(name = "show_cps_text")
    private boolean showCpsText = true;

    @JsonOption(name = "show_brackets")
    private boolean showBrackets = true;

    public CPSDisplayModule() {
        super(new ModuleData("cps_display", "\uD83D\uDDB1", color1));

        getData().setHeight(8);
        getData().setWidth(40);

        getData().setDefaultPosX(590, 640);
        getData().setDefaultPosY(2, 360);

        var toggleBind = new Keybind("toggle_cps_display",
            Text.translatable("fireclient.keybind.generic.toggle.name"),
            Text.translatable("fireclient.keybind.generic.toggle_visibility.description", getData().getShownName()),
            true, null,
            () -> getData().setVisible(!getData().isVisible()), null);

        FireClientside.getKeybindManager().registerKeybind(toggleBind);
    }

    public static void registerClick(boolean leftClick) {
        var now = System.currentTimeMillis();
        if (leftClick) {
            leftClicks.add(now);
        } else {
            rightClicks.add(now);
        }
    }

    private int getCPS(LinkedList<Long> clicks) {
        var now = System.currentTimeMillis();
        while (!clicks.isEmpty() && now - clicks.peek() > 1000) {
            clicks.poll();
        }
        return clicks.size();
    }

    @Override
    public List<ClickableWidget> getConfigScreen(Screen base) {
        var widgets = new ArrayList<ClickableWidget>();

        widgets.add(FireClientside.getKeybindManager().getKeybind("toggle_cps_display").getRebindButton(5, base.height - 25, 120, 20));
        widgets.add(getToggleVisibleButton(base.width / 2 - 60, base.height / 2 - 10));

        widgets.add(new ToggleButtonWidget.ToggleButtonBuilder(Text.translatable("fireclient.module.cps_display.show_cps_text.name"))
            .getValue(() -> { return showCpsText; })
            .setValue((value) -> { showCpsText = value; })
            .position(base.width / 2 - 60, base.height / 2 + 20)
            .tooltip(Tooltip.of(Text.translatable("fireclient.module.cps_display.show_cps_text.tooltip")))
            .build());

        widgets.add(new ToggleButtonWidget.ToggleButtonBuilder(Text.translatable("fireclient.module.cps_display.show_brackets.name"))
            .getValue(() -> { return showBrackets; })
            .setValue((value) -> { showBrackets = value; })
            .position(base.width / 2 - 60, base.height / 2 + 50)
            .tooltip(Tooltip.of(Text.translatable("fireclient.module.cps_display.show_brackets.tooltip")))
            .build());

        return widgets;
    }

    @Override
    public void draw(DrawContext context, RenderTickCounter ticks) {
        if (!canDraw()) {
            return;
        }

        transform(context.getMatrices());

        var client = MinecraftClient.getInstance();
        var text = client.textRenderer;

        var leftCps = getCPS(leftClicks);
        var rightCps = getCPS(rightClicks);

        var sb = new StringBuilder();
        if (showBrackets) sb.append("[");
        sb.append(leftCps).append(" | ").append(rightCps);
        if (showCpsText) sb.append(" CPS");
        if (showBrackets) sb.append("]");

        var msg = sb.toString();
        var cpsText = RooHelper.gradientText(msg, color1, color2);

        getData().setWidth(text.getWidth(cpsText));

        context.drawText(text, cpsText, 0, 0, 0xFFFFFFFF, true);

        endTransform(context.getMatrices());
    }
}

