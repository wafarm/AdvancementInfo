/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.guntram.mcmod.advancementinfo.mixin;

import de.guntram.mcmod.advancementinfo.AdvancementInfo;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.advancements.AdvancementTab;
import net.minecraft.client.gui.screens.advancements.AdvancementsScreen;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import static de.guntram.mcmod.advancementinfo.AdvancementInfo.config;

/**
 * @author gbl
 */
@Mixin(AdvancementTab.class)
public class AdvancementTabMixin {

    @Shadow
    @Final
    private AdvancementsScreen screen;
    @Shadow
    private boolean centered;

    @Unique
    private int currentInfoWidth;
    @Unique
    private int screenWidth = -1;
    @Unique
    private int screenHeight = -1;
    @Unique
    private int contentWidth = -1;
    @Unique
    private int contentHeight = -1;

    @Inject(method = "extractContents", at = @At("HEAD"))
    private void updateLayout(GuiGraphicsExtractor context, int x, int y, CallbackInfo ci) {
        if (screen != null) {
            if (screen.width != screenWidth || screen.height != screenHeight) {
                centered = false; // make tab recalculate origin

                currentInfoWidth = config.infoWidth.calculate(screen.width);
                screenWidth = screen.width;
                screenHeight = screen.height;
                contentWidth = screen.width - config.marginX * 2 - 2 * 9 - currentInfoWidth;
                contentHeight = screen.height - config.marginY * 2 - 3 * 9;
            }
        }
    }

    // space of the whole internal advancements widget
    @ModifyConstant(method = "extractContents", constant = @Constant(intValue = 234), require = 1)
    private int getAdvTreeXSize(int orig) {
        return contentWidth;
    }

    @ModifyConstant(method = "extractContents", constant = @Constant(intValue = 113), require = 1)
    private int getAdvTreeYSize(int orig) {
        return contentHeight;
    }

    // origin of the shown tree within the scrollable space

    @ModifyConstant(method = "extractContents", constant = @Constant(intValue = 117), require = 1)
    private int getAdvTreeXOrig(int orig) {
        return contentWidth / 2;
    }

    @ModifyConstant(method = "extractContents", constant = @Constant(intValue = 56), require = 1)
    private int getAdvTreeYOrig(int orig) {
        return contentHeight / 2;
    }

    @ModifyConstant(method = "scroll", constant = @Constant(intValue = 234), require = 1)
    private int getMoveXCenter(int orig) {
        return contentWidth;
    }

    @ModifyConstant(method = "scroll", constant = @Constant(intValue = 113), require = 1)
    private int getMoveYCenter(int orig) {
        return contentHeight;
    }

    @ModifyConstant(method = "canScrollHorizontally", constant = @Constant(intValue = 234), require = 1)
    private int getScrollableWidth(int orig) {
        return contentWidth;
    }

    @ModifyConstant(method = "canScrollVertically", constant = @Constant(intValue = 113), require = 1)
    private int getScrollableHeight(int orig) {
        return contentHeight;
    }

    // need to repeat the texture inside the scrollable space more

    @ModifyConstant(method = "extractContents", constant = @Constant(intValue = 15), require = 1)
    private int getXTextureRepeats(int orig) {
        return (screen.width - config.marginX * 2 - currentInfoWidth) / 16 + 1;
    }

    @ModifyConstant(method = "extractContents", constant = @Constant(intValue = 8), require = 1)
    private int getYTextureRepeats(int orig) {
        return (screen.height - config.marginY * 2) / 16 + 1;
    }

    // area that can show a tooltip
    @ModifyConstant(method = "extractTooltips", constant = @Constant(intValue = 234), require = 2)
    private int getTooltipXSize(int orig) {
        return contentWidth;
    }

    @ModifyConstant(method = "extractTooltips", constant = @Constant(intValue = 113), require = 2)
    private int getTooltipYSize(int orig) {
        return contentHeight;
    }

    @Inject(method = "extractTooltips", at = @At("HEAD"))
    private void forgetMouseOver(GuiGraphicsExtractor context, int mouseX, int mouseY, int x, int y, CallbackInfo ci) {
        AdvancementInfo.mouseOver = null;
    }
}
