package com.niksne.packetauth.client.mixin;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.Element;
import net.minecraft.client.gui.screen.AddServerScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.CyclingButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.network.ServerInfo;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import static com.niksne.packetauth.client.Utils.getToken;
import static com.niksne.packetauth.client.Utils.saveToken;

@Mixin(AddServerScreen.class)
public abstract class AddServerScreenMixin extends Screen {
    @Shadow @Final private ServerInfo server;

    @Shadow protected abstract void init();

    private static final Text ENTER_TOKEN_TEXT = Text.translatable("addServer.enterToken");
    private TextFieldWidget tokenField;
    private String string3;

    protected AddServerScreenMixin() {

        super(null);
    }

    @Inject(at = @At("TAIL"), method = "tick")
    public void tick(CallbackInfo ci) {
        this.tokenField.tick();
    }

    @Inject(at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/widget/CyclingButtonWidget;builder(Ljava/util/function/Function;)Lnet/minecraft/client/gui/widget/CyclingButtonWidget$Builder;"), method = "init")
    private void init1(CallbackInfo ci) {
        this.tokenField = new TextFieldWidget(this.textRenderer, this.width / 2 - 100, 146, 200, 20, Text.translatable("addServer.enterToken"));
        this.tokenField.setMaxLength(4096);
        this.tokenField.setText(getToken(this.server.address));
        this.addSelectableChild(this.tokenField);
    }

    @Inject(at = @At("TAIL"), method = "init")
    private void onAddServerScreenInit(CallbackInfo ci) {
        for (Element element : this.children()) {
            if (element instanceof CyclingButtonWidget<?> button) {
                button.setY(button.getY() + 18 + 24);
            }
            if (element instanceof ButtonWidget button) {
                button.setY(button.getY() + 18 + 24);
            }
        }
    }

    @Inject(at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screen/AddServerScreen;init(Lnet/minecraft/client/MinecraftClient;II)V"), method = "resize")
    public void resize1(CallbackInfo ci) {
        string3 = this.tokenField.getText();
    }

    @Inject(at = @At("TAIL"), method = "resize")
    public void resize2(CallbackInfo ci) {
        this.tokenField.setText(string3);
    }

    @Inject(at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/widget/TextFieldWidget;getText()Ljava/lang/String;"), method = "addAndClose")
    private void save(CallbackInfo ci) {
        String ip = this.server.address;
        if (!ip.contains(":")) ip += ":25565";
        saveToken(ip, this.tokenField.getText());
    }

    @Inject(at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/DrawContext;drawCenteredTextWithShadow(Lnet/minecraft/client/font/TextRenderer;Lnet/minecraft/text/Text;III)V"), method = "render")
    public void render1(DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        context.drawTextWithShadow(this.textRenderer, ENTER_TOKEN_TEXT, this.width / 2 - 100, 135, 10526880);
    }

    @Inject(at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screen/Screen;render(Lnet/minecraft/client/gui/DrawContext;IIF)V"), method = "render")
    public void render2(DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        this.tokenField.render(context, mouseX, mouseY, delta);
    }
}