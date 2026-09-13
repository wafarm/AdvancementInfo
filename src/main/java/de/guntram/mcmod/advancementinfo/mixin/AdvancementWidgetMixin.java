/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.guntram.mcmod.advancementinfo.mixin;

import de.guntram.mcmod.advancementinfo.AdvancementInfo;
import de.guntram.mcmod.advancementinfo.accessors.AdvancementWidgetAccessor;
import net.minecraft.advancements.AdvancementProgress;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.advancements.AdvancementWidget;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * @author gbl
 */

@Mixin(AdvancementWidget.class)
public class AdvancementWidgetMixin implements AdvancementWidgetAccessor {

    @Shadow
    private AdvancementProgress progress;

    @Inject(method = "extractHover", at = @At("HEAD"))
    public void rememberTooltip(GuiGraphicsExtractor graphics, int xo, int yo, float fade, int screenxo, int screenyo, int screenWidth, CallbackInfo ci) {
        AdvancementInfo.mouseOver = (AdvancementWidget) (Object) this;
    }

    @Override
    public AdvancementProgress advancementInfo$getProgress() {
        return this.progress;
    }
}
