package mewsicbridge.client;

import mewsicbridge.MewsicBridgeMod;
import mewsicbridge.config.ModConfig;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;

public class MewsicPortScreen extends Screen {
    private final Screen parentScreen;
    private EditBox portInput;
    private Button saveBtn;
    private String errorMessage = "";

    public MewsicPortScreen(Screen parentScreen) {
        super(Component.literal("Mewsic WebSocket Port"));
        this.parentScreen = parentScreen;
    }

    @Override
    protected void init() {
        int centerX = this.width / 2;
        int centerY = this.height / 2;

        this.portInput = new EditBox(this.font, centerX - 100, centerY - 20, 200, 20, Component.literal("Port"));
        this.portInput.setMaxLength(5);
        this.portInput.setValue(String.valueOf(ModConfig.get().port));
        this.portInput.setResponder(this::validatePort);
        this.portInput.setTooltip(Tooltip.create(Component.literal("Enter the WebSocket port (default: 3012). Must be between 1024 and 65535.")));
        this.addRenderableWidget(this.portInput);
        this.setInitialFocus(this.portInput);

        this.saveBtn = Button.builder(Component.literal("Save & Reconnect"), btn -> saveAndClose())
            .bounds(centerX - 102, centerY + 24, 100, 20)
            .tooltip(Tooltip.create(Component.literal("Save the new port and restart the WebSocket bridge server.")))
            .build();
        this.addRenderableWidget(this.saveBtn);

        Button cancelBtn = Button.builder(CommonComponents.GUI_CANCEL, btn -> onClose())
            .bounds(centerX + 2, centerY + 24, 100, 20)
            .tooltip(Tooltip.create(Component.literal("Discard changes and return to the previous menu.")))
            .build();
        this.addRenderableWidget(cancelBtn);

        validatePort(this.portInput.getValue());
    }

    private void validatePort(String text) {
        try {
            int port = Integer.parseInt(text.trim());
            if (port >= 1024 && port <= 65535) {
                this.errorMessage = "";
                if (this.saveBtn != null) {
                    this.saveBtn.active = true;
                }
                return;
            }
        } catch (NumberFormatException ignored) {
        }
        this.errorMessage = "Port must be a number between 1024 and 65535";
        if (this.saveBtn != null) {
            this.saveBtn.active = false;
        }
    }

    private void saveAndClose() {
        try {
            int port = Integer.parseInt(this.portInput.getValue().trim());
            if (port >= 1024 && port <= 65535) {
                ModConfig.get().port = port;
                ModConfig.save();
                MewsicBridgeMod.restartServer();
            }
        } catch (Exception ignored) {
        }
        if (this.minecraft != null) {
            this.minecraft.setScreenAndShow(this.parentScreen);
        }
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor context, int mouseX, int mouseY, float tickDelta) {
        super.extractRenderState(context, mouseX, mouseY, tickDelta);
        int centerX = this.width / 2;
        int centerY = this.height / 2;
        context.centeredText(this.font, this.title, centerX, centerY - 60, -1);
        context.centeredText(this.font, "Enter WebSocket server port (default 3012):", centerX, centerY - 42, -5592406);
        if (!this.errorMessage.isEmpty()) {
            context.centeredText(this.font, this.errorMessage, centerX, centerY + 6, -65536);
        }
    }

    @Override
    public void onClose() {
        if (this.minecraft != null) {
            this.minecraft.setScreenAndShow(this.parentScreen);
        }
    }
}
