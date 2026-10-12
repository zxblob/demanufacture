package com.zxblob.demanufacture.client.screen;

import java.util.List;

import com.zxblob.demanufacture.Demanufacture;
import com.zxblob.demanufacture.network.ToggleCookModePayload;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.PageButton;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * Opened by right-clicking a placed cookbook. Styled on vanilla's own written-book screen
 * conventions (same 192x192 canvas, same page-turn arrows - those are built-in sprites, no new
 * art needed for them) rather than inventing a layout from scratch. BOOK_TEXTURE is vanilla's
 * own book background as a placeholder; swap in real art at the same path/size whenever ready.
 * <p>
 * Page 0 is the cook-mode toggle. Page 1 logs every dish this player has served so far - just a
 * flat count for now (there's only one dish type), becomes the real per-dish discovery record
 * once dishes actually vary.
 */
public class CookbookMenuScreen extends Screen
{
    private static final ResourceLocation BOOK_TEXTURE = ResourceLocation.withDefaultNamespace("textures/gui/book.png");
    private static final int IMAGE_WIDTH = 192;
    private static final int IMAGE_HEIGHT = 192;
    private static final int PAGE_COUNT = 2;

    private int currentPage = 0;
    private Button cookModeButton;
    private PageButton forwardButton;
    private PageButton backButton;

    public CookbookMenuScreen()
    {
        super(Component.translatable("screen.demanufacture.cookbook_menu"));
    }

    private int bookLeft()
    {
        return (this.width - IMAGE_WIDTH) / 2;
    }

    private int bookTop()
    {
        return (this.height - IMAGE_HEIGHT) / 2;
    }

    @Override
    protected void init()
    {
        super.init();
        int left = bookLeft();
        int top = bookTop();

        cookModeButton = this.addRenderableWidget(Button.builder(cookModeLabel(), button ->
                        PacketDistributor.sendToServer(new ToggleCookModePayload()))
                .bounds(left + 20, top + IMAGE_HEIGHT - 34, 108, 20)
                .build());

        forwardButton = this.addRenderableWidget(new PageButton(left + 116, top + 157, true, b -> turnPage(1), true));
        backButton = this.addRenderableWidget(new PageButton(left + 43, top + 157, false, b -> turnPage(-1), true));
        updatePageWidgets();
    }

    private void turnPage(int direction)
    {
        currentPage = Math.max(0, Math.min(PAGE_COUNT - 1, currentPage + direction));
        updatePageWidgets();
    }

    private void updatePageWidgets()
    {
        forwardButton.visible = currentPage < PAGE_COUNT - 1;
        backButton.visible = currentPage > 0;
        cookModeButton.visible = currentPage == 0;
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick)
    {
        int left = bookLeft();
        int top = bookTop();

        guiGraphics.blit(BOOK_TEXTURE, left, top, 0, 0, IMAGE_WIDTH, IMAGE_HEIGHT);

        cookModeButton.setMessage(cookModeLabel());
        super.render(guiGraphics, mouseX, mouseY, partialTick);

        if (currentPage == 0)
        {
            guiGraphics.drawString(this.font, Component.translatable("page.demanufacture.cookbook_menu.settings"), left + 36, top + 30, 0x000000, false);
            guiGraphics.drawWordWrap(this.font, Component.translatable("page.demanufacture.cookbook_menu.settings_body"), left + 36, top + 45, 114, 0x000000);
        }
        else
        {
            renderRecipesPage(guiGraphics, left, top);
        }

        Component pageIndicator = Component.translatable("book.pageIndicator", currentPage + 1, PAGE_COUNT);
        guiGraphics.drawString(this.font, pageIndicator, left + IMAGE_WIDTH - 44 - this.font.width(pageIndicator), top + 16, 0x000000, false);
    }

    private void renderRecipesPage(GuiGraphics guiGraphics, int left, int top)
    {
        List<ItemStack> served = Minecraft.getInstance().player != null
                ? Minecraft.getInstance().player.getData(Demanufacture.SERVED_DISHES)
                : List.of();

        guiGraphics.drawString(this.font, Component.translatable("page.demanufacture.cookbook_menu.recipes"), left + 36, top + 30, 0x000000, false);

        if (served.isEmpty())
        {
            guiGraphics.drawWordWrap(this.font, Component.translatable("page.demanufacture.cookbook_menu.recipes_body"), left + 36, top + 45, 114, 0x000000);
            return;
        }

        ItemStack sample = served.get(served.size() - 1);
        guiGraphics.renderItem(sample, left + 36, top + 48);
        guiGraphics.drawString(this.font, sample.getHoverName(), left + 58, top + 50, 0x000000, false);
        guiGraphics.drawString(this.font, Component.translatable("page.demanufacture.cookbook_menu.served_count", served.size()), left + 58, top + 62, 0x404040, false);
    }

    private Component cookModeLabel()
    {
        boolean on = Minecraft.getInstance().player != null && Minecraft.getInstance().player.getData(Demanufacture.COOK_MODE);
        return Component.translatable(on ? "button.demanufacture.cook_mode_on" : "button.demanufacture.cook_mode_off");
    }

    @Override
    public boolean isPauseScreen()
    {
        return false;
    }
}
