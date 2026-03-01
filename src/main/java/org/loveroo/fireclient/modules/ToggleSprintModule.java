package org.loveroo.fireclient.modules;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.tooltip.Tooltip;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.text.Text;
import org.loveroo.fireclient.client.FireClientside;
import org.loveroo.fireclient.data.Color;
import org.loveroo.fireclient.data.JsonOption;
import org.loveroo.fireclient.data.ModuleData;
import org.loveroo.fireclient.keybind.Keybind;
import org.loveroo.fireclient.mixin.modules.scrollclick.BoundKeyAccessor;
import org.loveroo.fireclient.screen.widgets.ToggleButtonWidget;
import net.minecraft.client.gui.widget.SliderWidget;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

public class ToggleSprintModule extends ModuleBase {

    private static final Color color = Color.fromRGB(0x4A7D64);

    private static final long MIN_CLICK_MS = 40;

    private static boolean isSprinting = false;
    private static boolean waitingForRelease = false;
    private boolean sprintKeyDown = false;
    private boolean keyHeld = false;
    private long sprintPressTime = 0;

    @JsonOption(name = "show_indicator_text")
    private boolean showIndicatorText = true;

    @JsonOption(name = "tap_threshold")
    private int tapThreshold = 200;

    public ToggleSprintModule() {
        super(new ModuleData("toggle_toggle_sprint", "\uD83C\uDFC3", color));

        getData().setHeight(8);
        getData().setWidth(60);

        getData().setDefaultPosX(280, 640);
        getData().setDefaultPosY(12, 360);

        var toggleBind = new Keybind("toggle_toggle_sprint_visibility",
                Text.translatable("fireclient.keybind.generic.toggle.name"),
                Text.translatable("fireclient.keybind.generic.toggle_visibility.description", getData().getShownName()),
                true, null,
                () -> getData().setVisible(!getData().isVisible()), null);

        FireClientside.getKeybindManager().registerKeybind(toggleBind);
    }

    @Override
    public void update(MinecraftClient client) {
        if (!getData().isEnabled() || client.player == null) {
            return;
        }

        var window = client.getWindow().getHandle();
        var boundKey = ((BoundKeyAccessor) client.options.sprintKey).getBoundKey();
        var sprintPressed = GLFW.glfwGetKey(window, boundKey.getCode()) == GLFW.GLFW_PRESS;

        if (sprintPressed && !sprintKeyDown) {
            sprintKeyDown = true;
            keyHeld = false;
            waitingForRelease = true;
            sprintPressTime = System.currentTimeMillis();
            client.options.sprintKey.setPressed(true);
        }

        if (sprintPressed && sprintKeyDown) {
            var heldDuration = System.currentTimeMillis() - sprintPressTime;
            if (heldDuration > tapThreshold) {
                keyHeld = true;
                waitingForRelease = false;
            }
        }

        if (!sprintPressed && sprintKeyDown) {
            sprintKeyDown = false;
            var heldDuration = System.currentTimeMillis() - sprintPressTime;

            if (heldDuration >= MIN_CLICK_MS && heldDuration <= tapThreshold && !keyHeld) {
                isSprinting = !isSprinting;
            }

            waitingForRelease = false;

            if (!isSprinting) {
                client.options.sprintKey.setPressed(false);
            }

            keyHeld = false;
        }

        if (isSprinting || waitingForRelease) {
            client.options.sprintKey.setPressed(true);
        }
    }

    public static boolean isToggleSprinting() {
        return isSprinting;
    }

    public static boolean isWaitingForRelease() {
        return waitingForRelease;
    }

    @Override
    public List<ClickableWidget> getConfigScreen(Screen base) {
        var widgets = new ArrayList<ClickableWidget>();

        widgets.add(FireClientside.getKeybindManager().getKeybind("toggle_toggle_sprint_visibility").getRebindButton(5, base.height - 25, 120, 20));
        widgets.add(getToggleVisibleButton(base.width / 2 - 60, base.height / 2 - 10));

        widgets.add(new ToggleButtonWidget.ToggleButtonBuilder(Text.translatable("fireclient.module.toggle_toggle_sprint.show_indicator_text.name"))
            .getValue(() -> showIndicatorText)
            .setValue((value) -> showIndicatorText = value)
            .position(base.width / 2 - 60, base.height / 2 + 20)
            .tooltip(Tooltip.of(Text.translatable("fireclient.module.toggle_toggle_sprint.show_indicator_text.tooltip")))
            .build());

        var slider = new SliderWidget(base.width / 2 - 60, base.height / 2 + 50, 120, 20, getThresholdText(), (tapThreshold - 100) / 400.0) {
            @Override
            protected void updateMessage() {
                setMessage(getThresholdText());
            }

            @Override
            protected void applyValue() {
                tapThreshold = (int) (value * 400) + 100;
            }
        };
        slider.setTooltip(Tooltip.of(Text.translatable("fireclient.module.toggle_toggle_sprint.tap_threshold.tooltip")));
        widgets.add(slider);

        return widgets;
    }

    private Text getThresholdText() {
        return Text.translatable("fireclient.module.toggle_toggle_sprint.tap_threshold.name", tapThreshold + "ms");
    }

    @Override
    public void draw(DrawContext context, RenderTickCounter ticks) {
        if (!canDraw()) {
            return;
        }

        if (!isSprinting && !keyHeld) {
            return;
        }

        if (!showIndicatorText) {
            return;
        }

        transform(context.getMatrices());

        var client = MinecraftClient.getInstance();
        var text = client.textRenderer;

        String msg;

        if (keyHeld) {
            msg = "Sprinting (Key Held)";
        } else {
            msg = "Sprinting (Toggle)";
        }

        var displayText = Text.literal(msg).withColor(0xFFFFFF);

        getData().setWidth(text.getWidth(displayText));

        context.drawText(text, displayText, 0, 0, 0xFFFFFFFF, true);

        endTransform(context.getMatrices());
    }
}

