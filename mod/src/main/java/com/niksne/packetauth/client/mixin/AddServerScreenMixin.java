package com.niksne.packetauth.client.mixin;

import com.niksne.packetauth.client.PacketAuth;
import net.minecraft.client.gui.Element;
import net.minecraft.client.gui.screen.AddServerScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.CyclingButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.network.ServerInfo;
import net.minecraft.client.util.math.MatrixStack;
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
    @Shadow @Final private ServerInfo server;

    @Shadow protected abstract void init();

    @Unique
    private static final Text ENTER_TOKEN_TEXT = Text.translatable("addServer.enterToken");
    @Unique
    private TextFieldWidget tokenField;
    @Unique
    private String string3;

    protected AddServerScreenMixin() {

        super(null);
    }

    @Inject(at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/widget/CyclingButtonWidget;builder(Ljava/util/function/Function;)Lnet/minecraft/client/gui/widget/CyclingButtonWidget$Builder;"), method = "init")
    private void init1(CallbackInfo ci) {
        this.tokenField = new TextFieldWidget(this.textRenderer, this.width / 2 - 100, 146, 200, 20, Text.translatable("addServer.enterToken"));
        this.tokenField.setMaxLength(4096);
        String ip = this.server.address;
        if (!ip.contains(":")) ip += ":25565";
        this.tokenField.setText(PacketAuth.getConfig().getString(ip).replace(";", ""));
        this.addSelectableChild(this.tokenField);
    }

    @Inject(at = @At("TAIL"), method = "init")
    private void init2(CallbackInfo ci) {
        for (Element element : this.children()) {
            if (element instanceof CyclingButtonWidget<?> button) {
                button.y += 18 + 24;
            }
            if (element instanceof ButtonWidget button) {
                button.y += 18 + 24;
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
        PacketAuth.getConfig().putString(ip, this.tokenField.getText().replace(";", ""));
    }

    @Inject(at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/widget/TextFieldWidget;render(Lnet/minecraft/client/util/math/MatrixStack;IIF)V"), method = "render")
    public void render1(MatrixStack matrices, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        drawTextWithShadow(matrices, this.textRenderer, ENTER_TOKEN_TEXT, this.width / 2 - 100, 135, 10526880);
    }

    @Inject(at = @At("TAIL"), method = "render")
    public void render2(MatrixStack matrices, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        this.tokenField.render(matrices, mouseX, mouseY, delta);
    }
}