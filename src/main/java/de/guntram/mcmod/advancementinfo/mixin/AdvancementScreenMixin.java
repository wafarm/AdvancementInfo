/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.guntram.mcmod.advancementinfo.mixin;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import de.guntram.mcmod.advancementinfo.AdvancementInfo;
import de.guntram.mcmod.advancementinfo.AdvancementStep;
import de.guntram.mcmod.advancementinfo.IteratorReceiver;
import de.guntram.mcmod.advancementinfo.accessors.AdvancementScreenAccessor;
import de.guntram.mcmod.advancementinfo.accessors.AdvancementWidgetAccessor;
import net.minecraft.advancements.AdvancementNode;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.advancements.AdvancementTab;
import net.minecraft.client.gui.screens.advancements.AdvancementWidget;
import net.minecraft.client.gui.screens.advancements.AdvancementsScreen;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.multiplayer.ClientAdvancements;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import org.jspecify.annotations.NonNull;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

import static de.guntram.mcmod.advancementinfo.AdvancementInfo.config;

/**
 * @author gbl
 */
@Mixin(AdvancementsScreen.class)
public abstract class AdvancementScreenMixin extends Screen implements ClientAdvancements.Listener, AdvancementScreenAccessor {

    @Shadow
    @Final
    private static Identifier WINDOW_LOCATION;
    @Unique
    private boolean showRawCriteria;
    @Unique
    private Button rawToggle;
    @Unique
    private int scrollPos;
    @Unique
    private int currentInfoWidth = config.infoWidth.calculate(width);
    @Unique
    private EditBox search;
    @Shadow
    @Final
    private ClientAdvancements advancements;

    public AdvancementScreenMixin(Component title) {
        super(title);
    }

    @Shadow
    protected abstract AdvancementTab getTab(AdvancementNode advancement);

    @ModifyConstant(method = "extractRenderState", constant = @Constant(intValue = 252), require = 1)
    private int getRenderLeft(int orig) {
        return width - config.marginX * 2;
    }

    @ModifyConstant(method = "extractRenderState", constant = @Constant(intValue = 140), require = 1)
    private int getRenderTop(int orig) {
        return height - config.marginY * 2;
    }

    @ModifyConstant(method = "mouseClicked", constant = @Constant(intValue = 252), require = 1)
    private int getMouseLeft(int orig) {
        return width - config.marginX * 2;
    }

    @ModifyConstant(method = "mouseClicked", constant = @Constant(intValue = 140), require = 1)
    private int getMouseTop(int orig) {
        return height - config.marginY * 2;
    }

    @ModifyConstant(method = "extractInside", constant = @Constant(intValue = 234), require = 1)
    private int getAdvTreeXSize(int orig) {
        return width - config.marginX * 2 - 2 * 9 - currentInfoWidth;
    }

    @ModifyConstant(method = "extractInside", constant = @Constant(intValue = 113), require = 1)
    private int getAdvTreeYSize(int orig) {
        return height - config.marginY * 2 - 3 * 9;
    }

    @Redirect(method = "extractWindow", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiGraphicsExtractor;blit(Lcom/mojang/blaze3d/pipeline/RenderPipeline;Lnet/minecraft/resources/Identifier;IIFFIIII)V"))
    public void disableDefaultDraw(GuiGraphicsExtractor instance, RenderPipeline renderPipeline, Identifier texture, int x, int y, float u, float v, int width, int height, int textureWidth, int textureHeight) {
        // do nothing
    }

    @Inject(method = "init", at = @At("HEAD"))
    private void initWidgets(CallbackInfo ci) {
        this.showRawCriteria = config.rawDefault;
        this.search = new EditBox(font, 0, 0, CommonComponents.EMPTY);
        this.rawToggle = Button.builder(Component.translatable("advancementinfo.infopane.rawToggle"), widget -> toggleRaw()).bounds(0, 0, 40, 16).build();
    }

    @Unique
    private void toggleRaw() {
        this.showRawCriteria = !this.showRawCriteria;
    }

    @Unique
    private float getInfoFontScale() {
        return config.infoFontScale / 100f;
    }

    @Unique
    private int getInfoLineHeight() {
        return Math.max(1, Math.round(font.lineHeight * getInfoFontScale()));
    }

    @Unique
    private int getInfoVisibleLineCount() {
        return Math.max(1, (height - 2 * config.marginY - 45) / getInfoLineHeight() - 1);
    }

    @Unique
    private void drawScaledInfoText(GuiGraphicsExtractor context, String text, int maxWidth, int x, int y, int color) {
        float scale = getInfoFontScale();
        String trimmedText = font.plainSubstrByWidth(text == null ? "???" : text, Math.max(1, (int) Math.floor(maxWidth / scale)));
        context.pose().pushMatrix();
        context.pose().scale(scale, scale);
        context.text(font, trimmedText, Math.round(x / scale), Math.round(y / scale), color, false);
        context.pose().popMatrix();
    }

    @Inject(method = "extractRenderState",
        at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/gui/screens/advancements/AdvancementsScreen;extractWindow(Lnet/minecraft/client/gui/GuiGraphicsExtractor;IIII)V"))
    public void renderRightFrameBackground(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        if (currentInfoWidth == 0) return;
        context
            .fill(
                width - config.marginX - currentInfoWidth + 4, config.marginY + 4,
                width - config.marginX - 4, height - config.marginY - 4, 0xffc0c0c0);
    }

    @Inject(method = "extractWindow",
        at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/gui/GuiGraphicsExtractor;blit(Lcom/mojang/blaze3d/pipeline/RenderPipeline;Lnet/minecraft/resources/Identifier;IIFFIIII)V"))
    public void renderFrames(GuiGraphicsExtractor context, int x, int y, int mouseX, int mouseY, CallbackInfo ci) {
        int iw = currentInfoWidth;

        int screenW = 252;
        int screenH = 140;
        // actual size that will be available for the box
        int actualW = width - config.marginX - iw - x;
        int actualH = width - config.marginY - y;
        int halfW = screenW / 2;
        int halfH = screenH / 2;
        // When the screen is less than the default size the corners overlap
        int clipXh = (int) (Math.max(0, screenW - actualW) / 2. + 0.5);
        int clipXl = (int) (Math.max(0, screenW - actualW) / 2.);
        int clipYh = (int) (Math.max(0, screenH - actualH) / 2. + 0.5);
        int clipYl = (int) (Math.max(0, screenH - actualH) / 2.);

        // The base screen has a resolution of 252x140, divided into 4 quadrants this gives:
        // 1 │ 2       x    y        x    y
        // ──┼──    1: 0    0     2: 126  0
        // 3 │ 4    3: 0    70    4: 126  70

        int rightQuadX = width - config.marginX - halfW - iw + clipXh;
        int bottomQuadY = height - config.marginY - halfH + clipYh;

        context.blit(RenderPipelines.GUI_TEXTURED, WINDOW_LOCATION, x, y, 0, 0, halfW - clipXl, halfH - clipYl, 256, 256);
        context.blit(RenderPipelines.GUI_TEXTURED, WINDOW_LOCATION, rightQuadX, y, halfW + clipXh, 0, halfW - clipXh, halfH - clipYl, 256, 256); // top right
        context.blit(RenderPipelines.GUI_TEXTURED, WINDOW_LOCATION, x, bottomQuadY, 0, halfH + clipYh, halfW - clipXl, halfH - clipYh, 256, 256); // bottom left
        context.blit(RenderPipelines.GUI_TEXTURED, WINDOW_LOCATION, rightQuadX, bottomQuadY, halfW + clipXh, halfH + clipYh, halfW - clipXh, halfH - clipYh, 256, 256); // bottom right

        // draw borders
        iterate(x + halfW - clipXl, rightQuadX, 200, (pos, len) -> {
            context.blit(RenderPipelines.GUI_TEXTURED, WINDOW_LOCATION, pos, y, 15, 0, len, halfH, 256, 256); // top
            context.blit(RenderPipelines.GUI_TEXTURED, WINDOW_LOCATION, pos, bottomQuadY, 15, halfH + clipYh, len, halfH - clipYh, 256, 256); // bottom
        });
        iterate(y + halfH - clipYl, bottomQuadY, 100, (pos, len) -> {
            context.blit(RenderPipelines.GUI_TEXTURED, WINDOW_LOCATION, x, pos, 0, 25, halfW, len, 256, 256); // left
            context.blit(RenderPipelines.GUI_TEXTURED, WINDOW_LOCATION, rightQuadX, pos, halfW + clipXh, 25, halfW - clipXh, len, 256, 256); // right
        });

        if (currentInfoWidth == 0) return;

        // draw info corners
        int infoWl = (int) (iw / 2.);
        int infoWh = (int) (iw / 2. + 0.5);
        context.blit(RenderPipelines.GUI_TEXTURED, WINDOW_LOCATION, width - config.marginX - iw, y, 0, 0, infoWh, halfH, 256, 256); //
        context.blit(RenderPipelines.GUI_TEXTURED, WINDOW_LOCATION, width - config.marginX - infoWl, y, screenW - infoWl, 0, infoWl, halfH, 256, 256);
        context.blit(RenderPipelines.GUI_TEXTURED, WINDOW_LOCATION, width - config.marginX - iw, bottomQuadY, 0, halfH, infoWh, halfH, 256, 256);
        context.blit(RenderPipelines.GUI_TEXTURED, WINDOW_LOCATION, width - config.marginX - infoWl, bottomQuadY, screenW - infoWl, halfH, infoWl, halfH, 256, 256);

        // draw info borders
        // Note: If the info box is too wide there would be missing top & bottom borders
        iterate(halfH + config.marginY, bottomQuadY, 100, (pos, len) -> {
            context.blit(RenderPipelines.GUI_TEXTURED, WINDOW_LOCATION, width - config.marginX - iw, pos, 0, 25, iw / 2, len, 256, 256); // left
            context.blit(RenderPipelines.GUI_TEXTURED, WINDOW_LOCATION, width - config.marginX - iw / 2, pos, screenW - iw / 2, 25, iw / 2, len, 256, 256); // right
        });
    }

    @Unique
    private void iterate(int start, int end, int maxstep, IteratorReceiver func) {
        if (start >= end) return;
        int size;
        for (int i = start; i < end; i += maxstep) {
            size = maxstep;
            if (i + size > end) {
                size = end - i;
                if (size <= 0) return;
            }
            func.accept(i, size);
        }
    }

    @Inject(method = "extractWindow", at = @At("HEAD"))
    public void calculateLayout(GuiGraphicsExtractor context, int x, int y, int mouseX, int mouseY, CallbackInfo ci) {
        currentInfoWidth = config.infoWidth.calculate(width);
        search.setX(width - config.marginX - currentInfoWidth + 9);
        search.setY(config.marginY + 18);
        search.setWidth(currentInfoWidth - 18);
        search.setHeight(17);
        rawToggle.setPosition(width - config.marginX - 49, config.marginY + 1);
    }

    @Inject(method = "extractWindow", at = @At("RETURN"))
    public void renderRightFrameTitle(GuiGraphicsExtractor context, int x, int y, int mouseX, int mouseY, CallbackInfo ci) {
        if (currentInfoWidth == 0) return;
        context.text(font, I18n.get("advancementinfo.infopane"), width - config.marginX - currentInfoWidth + 8, y + 6, 0xFF404040, false);
    }

    @Inject(method = "extractRenderState", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screens/advancements/AdvancementsScreen;extractWindow(Lnet/minecraft/client/gui/GuiGraphicsExtractor;IIII)V", shift = At.Shift.AFTER))
    public void renderRightFrameWidgets(GuiGraphicsExtractor context, int mouseX, int mouseY, float deltaTicks, CallbackInfo ci) {
        if (currentInfoWidth == 0) return;
        search.extractWidgetRenderState(context, mouseX, mouseY, deltaTicks);
        rawToggle.extractRenderState(context, mouseX, mouseY, deltaTicks);

        if (AdvancementInfo.mouseClicked != null) {
            renderCriteria(context, AdvancementInfo.mouseClicked);
        } else if (AdvancementInfo.mouseOver != null || AdvancementInfo.cachedClickList != null) {
            renderCriteria(context, AdvancementInfo.mouseOver);
        }
    }

    @Inject(method = "mouseClicked", at = @At("HEAD"), cancellable = true)
    public void rememberClickedWidget(MouseButtonEvent click, boolean doubled, CallbackInfoReturnable<Boolean> cir) {
        if (search.mouseClicked(click, doubled)) {
            search.setFocused(true);
            cir.setReturnValue(true);
            cir.cancel();
        } else {
            search.setFocused(false);
        }

        if (rawToggle.mouseClicked(click, doubled)) {
            cir.setReturnValue(true);
            cir.cancel();
        }

        if (click.x() >= width - config.marginX - currentInfoWidth) {
            // later: handle click on search results here
            return;
        }
        AdvancementInfo.mouseClicked = AdvancementInfo.mouseOver;
        scrollPos = 0;
        if (AdvancementInfo.mouseClicked != null) {
            AdvancementInfo.cachedClickList = AdvancementInfo.getSteps((AdvancementWidgetAccessor) AdvancementInfo.mouseClicked);
            AdvancementInfo.cachedClickListLineCount = AdvancementInfo.getLineCount(AdvancementInfo.cachedClickList);
        } else {
            AdvancementInfo.cachedClickList = null;
            AdvancementInfo.cachedClickListLineCount = 0;
        }
    }

    @Inject(method = "onAddAdvancementRoot", at = @At("HEAD"))
    public void debugRootAdded(AdvancementNode root, CallbackInfo ci) {
        // System.out.println("root added to screen; display="+root.getDisplay()+", id="+root.getId().toString());
    }

    @Inject(method = "mouseScrolled", at = @At("HEAD"), cancellable = true)
    public void mouseScrolled(double X, double Y, double xAmount, double yAmount, CallbackInfoReturnable<Boolean> cir) {
        if (X < search.getX()) {
            return;
        }

        if (yAmount > 0 && scrollPos > 0) {
            scrollPos--;
        } else if (yAmount < 0 && AdvancementInfo.cachedClickList != null
            && scrollPos < AdvancementInfo.cachedClickListLineCount - getInfoVisibleLineCount()) {
            scrollPos++;
        }
        cir.setReturnValue(false);
    }

    @Inject(method = "keyPressed", at = @At("HEAD"), cancellable = true)
    public void redirectKeysToSearch(KeyEvent input, CallbackInfoReturnable<Boolean> cir) {
        if (search.canConsumeInput()) {
            if (input.input() == GLFW.GLFW_KEY_ENTER) {
                AdvancementInfo.setMatchingFrom((AdvancementsScreen) (Object) this, search.getValue());
            }
            search.keyPressed(input);
            // Only let ESCAPE end the screen, we don't want the keybind ('L')
            // to terminate the screen when we're typing text
            if (input.input() != GLFW.GLFW_KEY_ESCAPE) {
                cir.setReturnValue(true);
                cir.cancel();
            }
        }
    }

    @Override
    public boolean charTyped(@NonNull CharacterEvent charInput) {
        if (search.canConsumeInput()) {
            return search.charTyped(charInput);
        }
        return false;
    }


    @Unique
    private void renderCriteria(GuiGraphicsExtractor context, AdvancementWidget widget) {
        int y = search.getY() + search.getHeight() + 4;
        int skip;
        List<AdvancementStep> list;
        if (widget == AdvancementInfo.mouseClicked) {
            list = AdvancementInfo.cachedClickList;
            skip = scrollPos;
        } else {
            list = AdvancementInfo.getSteps((AdvancementWidgetAccessor) widget);
            skip = 0;
        }
        if (list == null) {
            return;
        }
        for (AdvancementStep entry : list) {
            String text = showRawCriteria ? entry.getId() : entry.getName();
            if (text == null) {
                System.out.println("list entry has null name: " + entry);
            }
            if (skip-- <= 0) {
                drawScaledInfoText(context, text, currentInfoWidth - 24, width - config.marginX - currentInfoWidth + 12, y,
                    entry.getObtained() ? (0xFF000000 | (AdvancementInfo.config.colorHave & 0x00FFFFFF)) : (0xFF000000 | (AdvancementInfo.config.colorHaveNot & 0x00FFFFFF)));
                y += getInfoLineHeight();
                if (y > height - config.marginY - getInfoLineHeight() * 2) {
                    return;
                }
            }

            if (entry.getDetails() != null) {
                for (String detail : entry.getDetails()) {
                    if (skip-- <= 0) {
                        drawScaledInfoText(context, detail, currentInfoWidth - 34, width - config.marginX - currentInfoWidth + 22, y, 0xFF000000);
                        y += getInfoLineHeight();
                        if (y > height - config.marginY - getInfoLineHeight() * 2) {
                            return;
                        }
                    }
                }
            }
        }
    }

    @Override
    public ClientAdvancements advancementInfo$getAdvancementHandler() {
        return advancements;
    }

    public AdvancementTab advancementInfo$myGetTab(AdvancementNode advancement) {
        return getTab(advancement);
    }
}
