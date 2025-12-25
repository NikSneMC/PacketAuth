package ru.niksne.packetauth.fabric.client.mixin;

import org.jetbrains.annotations.NotNull;
import ru.niksne.packetauth.fabric.client.PacketAuth;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.Element;
import net.minecraft.client.gui.screen.multiplayer.AddServerScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.CyclingButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.network.ServerInfo;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AddServerScreen.class)
public abstract class AddServerScreenMixin extends Screen {
    @Shadow
    @Final
    private ServerInfo server;

    @Shadow
    protected abstract void init();

    @Unique
    @NotNull
    private static final Text ENTER_TOKEN_TEXT = Text.translatable("addServer.enterToken");
    @Unique
    private TextFieldWidget packetAuth$tokenField;
    @Unique
    private String packetAuth$tokenFieldText;

    protected AddServerScreenMixin() {
        super(null);
    }

    @Inject(at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/widget/CyclingButtonWidget;builder(Ljava/util/function/Function;)Lnet/minecraft/client/gui/widget/CyclingButtonWidget$Builder;"), method = "init")
    private void init1(
        @NotNull
        CallbackInfo ci
    ) {
        this.packetAuth$tokenField = new TextFieldWidget(this.textRenderer, this.width / 2 - 100, 146, 200, 20, Text.translatable("addServer.enterToken"));
        this.packetAuth$tokenField.setMaxLength(4096);
        String ip = this.server.address;
        if (!ip.contains(":")) ip += ":25565";
        this.packetAuth$tokenField.setText(PacketAuth.getTokenStorage().getTokenFor(ip).replace(";", ""));
        this.addSelectableChild(this.packetAuth$tokenField);
    }

    @Inject(at = @At("TAIL"), method = "init")
    private void init2(
        @NotNull
        CallbackInfo ci
    ) {
        for (Element element : this.children()) {
            if (element instanceof CyclingButtonWidget<?> button) {
                button.setY(button.getY() + 18 + 24);
            }
            if (element instanceof ButtonWidget button) {
                button.setY(button.getY() + 18 + 24);
            }
        }
    }

    @Inject(at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screen/multiplayer/AddServerScreen;init(Lnet/minecraft/client/MinecraftClient;II)V"), method = "resize")
    public void resize1(
        @NotNull
        CallbackInfo ci
    ) {
        packetAuth$tokenFieldText = this.packetAuth$tokenField.getText();
    }

    @Inject(at = @At("TAIL"), method = "resize")
    public void resize2(
        @NotNull
        CallbackInfo ci
    ) {
        this.packetAuth$tokenField.setText(packetAuth$tokenFieldText);
    }

    @Inject(at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/widget/TextFieldWidget;getText()Ljava/lang/String;"), method = "addAndClose")
    private void save(
        @NotNull
        CallbackInfo ci
    ) {
        String ip = this.server.address;
        if (!ip.contains(":")) ip += ":25565";
        PacketAuth.getTokenStorage().saveTokenFor(ip, this.packetAuth$tokenField.getText().replace(";", ""));
        PacketAuth.getTokenStorageManager().saveStorage();
    }

    @Inject(at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/widget/TextFieldWidget;render(Lnet/minecraft/client/gui/DrawContext;IIF)V"), method = "render")
    public void render1(
        @NotNull
        DrawContext context,
        int mouseX,
        int mouseY,
        float delta,
        @NotNull
        CallbackInfo ci
    ) {
        context.drawTextWithShadow(this.textRenderer, ENTER_TOKEN_TEXT, this.width / 2 - 100, 135, 10526880);
    }

    @Inject(at = @At("TAIL"), method = "render")
    public void render2(
        @NotNull
        DrawContext context,
        int mouseX,
        int mouseY,
        float delta,
        @NotNull
        CallbackInfo ci
    ) {
        this.packetAuth$tokenField.render(context, mouseX, mouseY, delta);
    }
}