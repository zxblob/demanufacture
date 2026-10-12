package com.zxblob.demanufacture.client.screen;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/**
 * Stands in for a real minigame screen while we're proving out the interception pipeline.
 * Replaced per-block once each minigame is built.
 */
public class PlaceholderCookScreen extends Screen
{
    public PlaceholderCookScreen()
    {
        super(Component.translatable("screen.demanufacture.placeholder_cook"));
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick)
    {
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        guiGraphics.drawCenteredString(this.font, this.title, this.width / 2, this.height / 2, 0xFFFFFF);
    }

    @Override
    public boolean isPauseScreen()
    {
        return false;
    }
}
