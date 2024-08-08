/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.guntram.mcmod.advancementinfo.mixin;

import de.guntram.mcmod.advancementinfo.AdvancementInfo;
import de.guntram.mcmod.advancementinfo.AdvancementStep;
import de.guntram.mcmod.advancementinfo.IteratorReceiver;
import de.guntram.mcmod.advancementinfo.accessors.AdvancementScreenAccessor;
import de.guntram.mcmod.advancementinfo.accessors.AdvancementWidgetAccessor;
import net.minecraft.advancement.PlacedAdvancement;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.advancement.AdvancementTab;
import net.minecraft.client.gui.screen.advancement.AdvancementWidget;
import net.minecraft.client.gui.screen.advancement.AdvancementsScreen;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.network.ClientAdvancementManager;
import net.minecraft.client.resource.language.I18n;
import net.minecraft.screen.ScreenTexts;
import net.minecraft.util.Identifier;
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
public abstract class AdvancementScreenMixin extends Screen implements AdvancementScreenAccessor {

    @Shadow
    @Final
    private static Identifier WINDOW_TEXTURE;
    @Unique
    private int scrollPos;
    @Unique
    private int currentInfoWidth = config.infoWidth.calculate(width);
    @Unique
    private TextFieldWidget search;
    @Shadow
    @Final
    private ClientAdvancementManager advancementHandler;

    public AdvancementScreenMixin() {
        super(null);
    }

    @Shadow
    protected abstract AdvancementTab getTab(PlacedAdvancement advancement);

    @ModifyConstant(method = "render", constant = @Constant(intValue = 252), require = 1)
    private int getRenderLeft(int orig) {
        return width - config.marginX * 2;
    }

    @ModifyConstant(method = "render", constant = @Constant(intValue = 140), require = 1)
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

    @ModifyConstant(method = "drawAdvancementTree", constant = @Constant(intValue = 234), require = 1)
    private int getAdvTreeXSize(int orig) {
        return width - config.marginX * 2 - 2 * 9 - currentInfoWidth;
    }

    @ModifyConstant(method = "drawAdvancementTree", constant = @Constant(intValue = 113), require = 1)
    private int getAdvTreeYSize(int orig) {
        return height - config.marginY * 2 - 3 * 9;
    }

    @Redirect(method = "drawWindow", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/DrawContext;drawTexture(Lnet/minecraft/util/Identifier;IIIIII)V"))
    public void disableDefaultDraw(DrawContext instance, Identifier texture, int x, int y, int u, int v, int width, int height) {
        // do nothing
    }

    @Inject(method = "init", at = @At("RETURN"))
    private void initSearchField(CallbackInfo ci) {
        this.search = new TextFieldWidget(textRenderer, 0, 0, ScreenTexts.EMPTY);
    }

    @Inject(method = "render",
        at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/gui/screen/advancement/AdvancementsScreen;drawWindow(Lnet/minecraft/client/gui/DrawContext;II)V"))
    public void renderRightFrameBackground(DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        if (currentInfoWidth == 0) return;
        context
            .fill(
                width - config.marginX - currentInfoWidth + 4, config.marginY + 4,
                width - config.marginX - 4, height - config.marginY - 4, 0xffc0c0c0);
    }

    @Inject(method = "drawWindow",
        at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/gui/DrawContext;drawTexture(Lnet/minecraft/util/Identifier;IIIIII)V"))
    public void renderFrames(DrawContext context, int x, int y, CallbackInfo ci) {
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

        context.drawTexture(WINDOW_TEXTURE, x, y, 0, 0, halfW - clipXl, halfH - clipYl);
        context.drawTexture(WINDOW_TEXTURE, rightQuadX, y, halfW + clipXh, 0, halfW - clipXh, halfH - clipYl); // top right
        context.drawTexture(WINDOW_TEXTURE, x, bottomQuadY, 0, halfH + clipYh, halfW - clipXl, halfH - clipYh); // bottom left
        context.drawTexture(WINDOW_TEXTURE, rightQuadX, bottomQuadY, halfW + clipXh, halfH + clipYh, halfW - clipXh, halfH - clipYh); // bottom right

        // draw borders
        iterate(x + halfW - clipXl, rightQuadX, 200, (pos, len) -> {
            context.drawTexture(WINDOW_TEXTURE, pos, y, 15, 0, len, halfH); // top
            context.drawTexture(WINDOW_TEXTURE, pos, bottomQuadY, 15, halfH + clipYh, len, halfH - clipYh); // bottom
        });
        iterate(y + halfH - clipYl, bottomQuadY, 100, (pos, len) -> {
            context.drawTexture(WINDOW_TEXTURE, x, pos, 0, 25, halfW, len); // left
            context.drawTexture(WINDOW_TEXTURE, rightQuadX, pos, halfW + clipXh, 25, halfW - clipXh, len); // right
        });

        if (currentInfoWidth == 0) return;

        // draw info corners
        int infoWl = (int) (iw / 2.);
        int infoWh = (int) (iw / 2. + 0.5);
        context.drawTexture(WINDOW_TEXTURE, width - config.marginX - iw, y, 0, 0, infoWh, halfH); //
        context.drawTexture(WINDOW_TEXTURE, width - config.marginX - infoWl, y, screenW - infoWl, 0, infoWl, halfH);
        context.drawTexture(WINDOW_TEXTURE, width - config.marginX - iw, bottomQuadY, 0, halfH, infoWh, halfH);
        context.drawTexture(WINDOW_TEXTURE, width - config.marginX - infoWl, bottomQuadY, screenW - infoWl, halfH, infoWl, halfH);

        // draw info borders
        // Note: If the info box is too wide there would be missing top & bottom borders
        iterate(halfH + config.marginY, bottomQuadY, 100, (pos, len) -> {
            context.drawTexture(WINDOW_TEXTURE, width - config.marginX - iw, pos, 0, 25, iw / 2, len); // left
            context.drawTexture(WINDOW_TEXTURE, width - config.marginX - iw / 2, pos, screenW - iw / 2, 25, iw / 2, len); // right
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

    @Inject(method = "drawWindow", at = @At("HEAD"))
    public void calculateLayout(DrawContext context, int x, int y, CallbackInfo ci) {
        currentInfoWidth = config.infoWidth.calculate(width);
        search.setX(width - config.marginX - currentInfoWidth + 9);
        search.setY(config.marginY + 18);
        search.setWidth(currentInfoWidth - 18);
        search.setHeight(17);
    }

    @Inject(method = "drawWindow", at = @At("RETURN"))
    public void renderRightFrameTitle(DrawContext context, int x, int y, CallbackInfo ci) {
        if (currentInfoWidth == 0) return;
        context.drawText(textRenderer, I18n.translate("advancementinfo.infopane"), width - config.marginX - currentInfoWidth + 8, y + 6, 4210752, false);
        search.renderWidget(context, x, y, 0);

        if (AdvancementInfo.mouseClicked != null) {
            renderCriteria(context, AdvancementInfo.mouseClicked);
        } else if (AdvancementInfo.mouseOver != null || AdvancementInfo.cachedClickList != null) {
            renderCriteria(context, AdvancementInfo.mouseOver);
        }
    }

    @Inject(method = "mouseClicked", at = @At("HEAD"), cancellable = true)
    public void rememberClickedWidget(double x, double y, int button, CallbackInfoReturnable<Boolean> cir) {
        if (search.mouseClicked(x, y, button)) {
            search.setFocused(true);
            cir.setReturnValue(true);
            cir.cancel();
        } else {
            search.setFocused(false);
        }
        if (x >= width - config.marginX - currentInfoWidth) {
            // later: handle click on search results here
            return;
        }
        AdvancementInfo.mouseClicked = AdvancementInfo.mouseOver;
        scrollPos = 0;
        if (AdvancementInfo.mouseClicked != null) {
            AdvancementInfo.cachedClickList = AdvancementInfo.getSteps((AdvancementWidgetAccessor) AdvancementInfo.mouseClicked);
            AdvancementInfo.cachedClickListLineCount = AdvancementInfo.cachedClickList.size();
        } else {
            AdvancementInfo.cachedClickList = null;
            AdvancementInfo.cachedClickListLineCount = 0;
        }
    }

    @Inject(method = "onRootAdded", at = @At("HEAD"))
    public void debugRootAdded(PlacedAdvancement root, CallbackInfo ci) {
        // System.out.println("root added to screen; display="+root.getDisplay()+", id="+root.getId().toString());
    }

    // @Inject(method="mouseScrolled", at=@At("HEAD"), cancellable = true)
    @Override
    public boolean mouseScrolled(double X, double Y, double xAmount, double yAmount /*, CallbackInfoReturnable cir */) {
        if (yAmount > 0 && scrollPos > 0) {
            scrollPos--;
        } else if (yAmount < 0 && AdvancementInfo.cachedClickList != null
            && scrollPos < AdvancementInfo.cachedClickListLineCount - ((height - 2 * config.marginY - 45) / textRenderer.fontHeight - 1)) {
            scrollPos++;
        }
        // System.out.println("scrollpos is now "+scrollPos+", needed lines "+AdvancementInfo.cachedClickListLineCount+", shown "+((height-2*config.marginY-45)/textRenderer.fontHeight - 1));
        return false;
    }

    @Inject(method = "keyPressed", at = @At("HEAD"), cancellable = true)
    public void redirectKeysToSearch(int keyCode, int scanCode, int modifiers, CallbackInfoReturnable<Boolean> cir) {
        if (search.isActive()) {
            if (keyCode == GLFW.GLFW_KEY_ENTER) {
                AdvancementInfo.setMatchingFrom((AdvancementsScreen) (Object) this, search.getText());
            }
            search.keyPressed(keyCode, scanCode, modifiers);
            // Only let ESCAPE end the screen, we don't want the keybind ('L')
            // to terminate the screen when we're typing text
            if (keyCode != GLFW.GLFW_KEY_ESCAPE) {
                cir.setReturnValue(true);
                cir.cancel();
            }
        }
    }

    @Override
    public boolean charTyped(char chr, int keyCode) {
        if (search.isActive()) {
            return search.charTyped(chr, keyCode);
        }
        return false;
    }

    @Unique
    private void renderCriteria(DrawContext context, AdvancementWidget widget) {
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
            if (entry.getName() == null) {
                System.out.println("list entry has null name: " + entry);
            }
            if (skip-- <= 0) {
                context.drawText(textRenderer,
                    textRenderer.trimToWidth(entry.getName() == null ? "???" : entry.getName(), currentInfoWidth - 24),
                    width - config.marginX - currentInfoWidth + 12, y,
                    entry.getObtained() ? AdvancementInfo.config.colorHave : AdvancementInfo.config.colorHaveNot,
                    false);
                y += textRenderer.fontHeight;
                if (y > height - config.marginY - textRenderer.fontHeight * 2) {
                    return;
                }
            }

            if (entry.getDetails() != null) {
                for (String detail : entry.getDetails()) {
                    if (skip-- <= 0) {
                        context.drawText(textRenderer,
                            textRenderer.trimToWidth(detail, currentInfoWidth - 34),
                            width - config.marginX - currentInfoWidth + 22, y,
                            0x000000, false);
                        y += textRenderer.fontHeight;
                        if (y > height - config.marginY - textRenderer.fontHeight * 2) {
                            return;
                        }
                    }
                }
            }
        }
    }

    @Override
    public ClientAdvancementManager advancementInfo$getAdvancementHandler() {
        return advancementHandler;
    }

    public AdvancementTab advancementInfo$myGetTab(PlacedAdvancement advancement) {
        return getTab(advancement);
    }
}
