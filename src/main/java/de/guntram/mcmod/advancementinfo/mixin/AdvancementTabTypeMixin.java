/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.guntram.mcmod.advancementinfo.mixin;

import de.guntram.mcmod.advancementinfo.AdvancementInfo;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.advancements.AdvancementTabType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

/**
 * @author gbl
 */

@Mixin(AdvancementTabType.class)
public class AdvancementTabTypeMixin {
    @ModifyConstant(method = "getX", constant = @Constant(intValue = 248), require = 1)
    public int getAdjustedTabX(int orig) {
        //noinspection DataFlowIssue
        return Minecraft.getInstance().screen.width - AdvancementInfo.config.marginX * 2 - 4;
    }

    @ModifyConstant(method = "getY", constant = @Constant(intValue = 136), require = 1)
    public int getAdjustedTabY(int orig) {
        //noinspection DataFlowIssue
        return Minecraft.getInstance().screen.height - AdvancementInfo.config.marginY * 2 - 4;
    }
}
