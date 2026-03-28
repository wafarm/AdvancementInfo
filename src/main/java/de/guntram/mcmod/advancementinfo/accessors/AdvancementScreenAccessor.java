/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.guntram.mcmod.advancementinfo.accessors;

import net.minecraft.advancements.AdvancementNode;
import net.minecraft.client.gui.screens.advancements.AdvancementTab;
import net.minecraft.client.multiplayer.ClientAdvancements;

/**
 * @author gbl
 */
public interface AdvancementScreenAccessor {
    ClientAdvancements advancementInfo$getAdvancementHandler();

    AdvancementTab advancementInfo$myGetTab(AdvancementNode advancement);
}
