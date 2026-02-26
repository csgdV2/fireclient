package org.loveroo.fireclient.screen.widgets;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.narration.NarrationMessageBuilder;
import net.minecraft.client.gui.screen.narration.NarrationPart;
import net.minecraft.client.gui.tooltip.Tooltip;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.client.input.MouseInput;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.loveroo.fireclient.FireClient;
import org.loveroo.fireclient.client.FireClientside;
import org.loveroo.fireclient.modules.ModuleBase;

public class ModuleCardWidget extends ClickableWidget {

    private static final int CARD_BG_COLOR = 0xCC2A2A2A;
    private static final int CARD_BG_HOVER_COLOR = 0xCC3A3A3A;
    private static final int CARD_BORDER_COLOR = 0xFF555555;
    private static final int CARD_BORDER_HOVER_COLOR = 0xFF888888;

    private static final int ENABLED_COLOR = 0xFF4CAF50;
    private static final int ENABLED_HOVER_COLOR = 0xFF388E3C;
    private static final int DISABLED_COLOR = 0xFFE57373;
    private static final int DISABLED_HOVER_COLOR = 0xFFD32F2F;

    private static final Text ENABLED_TEXT = Text.literal("ENABLED");
    private static final Text DISABLED_TEXT = Text.literal("DISABLED");

    private static final int TOGGLE_HEIGHT = 16;
    private static final int ICON_SIZE = 32;

    private final ModuleBase module;
    private final Identifier iconTexture;

    public ModuleCardWidget(ModuleBase module, int x, int y, int width, int height) {
        super(x, y, width, height, module.getData().getName());

        this.module = module;
        this.iconTexture = Identifier.of(FireClient.MOD_ID, "textures/gui/modules/" + module.getData().getId() + ".png");
        setTooltip(Tooltip.of(module.getData().getDescription()));
    }

    @Override
    protected void renderWidget(DrawContext context, int mouseX, int mouseY, float deltaTicks) {
        var textRenderer = MinecraftClient.getInstance().textRenderer;
        int x = getX();
        int y = getY();
        int w = getWidth();
        int h = getHeight();

        boolean hovered = isHovered();

        // Card background
        context.fill(x, y, x + w, y + h, hovered ? CARD_BG_HOVER_COLOR : CARD_BG_COLOR);
        // Card border
        drawBorder(context, x, y, w, h, hovered ? CARD_BORDER_HOVER_COLOR : CARD_BORDER_COLOR);

        // === PNG icon (centered in top portion) ===
        int contentHeight = h - TOGGLE_HEIGHT - 2; // space above toggle
        int iconY = y + (contentHeight - ICON_SIZE - 12) / 2 + 2; // center icon, leaving room for name
        int iconX = x + (w - ICON_SIZE) / 2;

        context.drawTexture(RenderPipelines.GUI_TEXTURED, iconTexture, iconX, iconY, 0, 0, ICON_SIZE, ICON_SIZE, ICON_SIZE, ICON_SIZE, 0xFFFFFFFF);

        // === Module name (centered below icon) ===
        var name = module.getData().getName();
        int nameY = iconY + ICON_SIZE + 4;
        context.drawCenteredTextWithShadow(textRenderer, name, x + w / 2, nameY, 0xFFFFFFFF);


        // === Enabled/Disabled toggle bar at bottom ===
        int toggleY = y + h - TOGGLE_HEIGHT - 2;
        boolean enabled = module.getData().isGuiElement() ? module.getData().isVisible() : module.getData().isEnabled();
        boolean toggleHovered = isInsideRect(mouseX, mouseY, x + 2, toggleY, w - 4, TOGGLE_HEIGHT);

        int toggleColor;
        if (enabled) {
            toggleColor = toggleHovered ? ENABLED_HOVER_COLOR : ENABLED_COLOR;
        } else {
            toggleColor = toggleHovered ? DISABLED_HOVER_COLOR : DISABLED_COLOR;
        }

        context.fill(x + 2, toggleY, x + w - 2, toggleY + TOGGLE_HEIGHT, toggleColor);
        var toggleText = enabled ? ENABLED_TEXT : DISABLED_TEXT;
        context.drawCenteredTextWithShadow(textRenderer, toggleText,
                x + w / 2, toggleY + 4, 0xFFFFFFFF);
    }

    private void drawBorder(DrawContext context, int x, int y, int w, int h, int color) {
        context.fill(x, y, x + w, y + 1, color);           // top
        context.fill(x, y + h - 1, x + w, y + h, color);   // bottom
        context.fill(x, y, x + 1, y + h, color);            // left
        context.fill(x + w - 1, y, x + w, y + h, color);   // right
    }

    private boolean isInsideRect(int mx, int my, int rx, int ry, int rw, int rh) {
        return mx >= rx && mx < rx + rw && my >= ry && my < ry + rh;
    }

    @Override
    public void onClick(Click click, boolean doubled) {
        int mx = (int) click.x();
        int my = (int) click.y();
        int x = getX();
        int y = getY();
        int w = getWidth();
        int h = getHeight();

        // Check toggle bar click
        int toggleY = y + h - TOGGLE_HEIGHT - 2;
        if (isInsideRect(mx, my, x + 2, toggleY, w - 4, TOGGLE_HEIGHT)) {
            if (module.getData().isGuiElement()) {
                module.getData().setVisible(!module.getData().isVisible());
            } else {
                module.getData().setEnabled(!module.getData().isEnabled());
            }
            FireClientside.saveConfig();
            return;
        }

        // Clicking anywhere else on the card opens config
        module.moduleConfigPressed(null);
    }

    @Override
    protected boolean isValidClickButton(MouseInput input) {
        return input.button() == 0;
    }

    @Override
    protected void appendClickableNarrations(NarrationMessageBuilder builder) {
        builder.put(NarrationPart.TITLE, module.getData().getName());
    }
}
