package com.zxblob.demanufacture.client.screen;

import java.util.ArrayList;
import java.util.List;

import com.zxblob.demanufacture.Demanufacture;
import com.zxblob.demanufacture.cooking.BasketScanner;
import com.zxblob.demanufacture.cooking.LiquidItems;
import com.zxblob.demanufacture.network.FinishCookingPayload;
import com.zxblob.demanufacture.network.RuinPotPayload;
import com.zxblob.demanufacture.network.ServePotPayload;
import com.zxblob.demanufacture.network.SetPotLiquidPayload;
import com.zxblob.demanufacture.network.TakeBasketItemPayload;
import com.zxblob.demanufacture.network.TakeIngredientOutPayload;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * Second minigame, for FD's Skillet. Deliberately a different core feel from the Stockpot/Cooking
 * Pot's circular stir: here it's raw mouse-scribbling speed that matters (like whisking scrambled
 * eggs), not a specific gesture shape. Basket/Liquid Flask/doneness/fail/serve are identical in
 * spirit to StirringMinigameScreen - those systems are already block-agnostic - only the core
 * motion-judging and the per-item visual differ, so this is a parallel sibling, not a subclass of
 * that screen (two data points isn't enough yet to know what the right shared base class is).
 */
public class ScramblingMinigameScreen extends Screen
{
    // TUNE: pixels/second of raw mouse travel, not radians - totally different feel from stirring.
    private static final double MIN_GOOD_SPEED = 400.0;
    private static final double MAX_GOOD_SPEED = 2500.0;
    private static final int PAN_RADIUS = 70;
    private static final int INGREDIENT_BASE_RADIUS = 24;
    private static final double MAX_JITTER_RADIUS = 10.0;

    private static final double METER_DRAIN_PER_SECOND = 18.0;
    private static final double METER_RECOVER_PER_SECOND = 10.0;
    private static final double SPEED_SMOOTHING = 0.15;

    private static final double MIN_PACE_FOR_DONENESS = 90.0;

    private static final int BASKET_CELL_SIZE = 24;
    private static final int BASKET_COLUMNS = 3;
    private static final int BASKET_VISIBLE_ROWS = 5;
    private static final int BASKET_PANEL_LEFT = 20;

    private static final int LIQUID_ICON_SIZE = 20;

    private final BlockPos potPos;

    private boolean hasLastMouse = false;
    private double lastMouseX;
    private double lastMouseY;
    private double pendingDistance = 0;
    private double smoothedSpeed;

    private double paceMeter = 100.0;

    private long lastFrameNanos = -1;

    private final List<BasketScanner.BasketEntry> solidItems;
    private final List<BasketScanner.BasketEntry> liquidItems;
    private int basketScrollRow = 0;
    private int selectedLiquidIndex = 0;

    private double cookProgressSeconds = 0;
    private boolean finishSent = false;
    private boolean ruinSent = false;
    private boolean wasIngredientsEmpty = true;

    public ScramblingMinigameScreen(BlockPos potPos, List<BasketScanner.BasketEntry> basketItems)
    {
        super(Component.translatable("screen.demanufacture.scrambling"));
        this.potPos = potPos;
        this.solidItems = new ArrayList<>();
        this.liquidItems = new ArrayList<>();
        for (BasketScanner.BasketEntry entry : basketItems)
        {
            if (LiquidItems.isLiquidItem(entry.stack()))
                liquidItems.add(entry);
            else
                solidItems.add(entry);
        }
    }

    @Override
    public void mouseMoved(double mouseX, double mouseY)
    {
        super.mouseMoved(mouseX, mouseY);

        if (hasLastMouse)
            pendingDistance += Math.hypot(mouseX - lastMouseX, mouseY - lastMouseY);
        lastMouseX = mouseX;
        lastMouseY = mouseY;
        hasLastMouse = true;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button)
    {
        int ingredientIndex = ingredientIndexAt(mouseX, mouseY);
        if (ingredientIndex >= 0)
        {
            PacketDistributor.sendToServer(new TakeIngredientOutPayload(potPos, ingredientIndex));
            return true;
        }

        int basketIndex = basketSlotAt(mouseX, mouseY);
        if (basketIndex >= 0 && basketIndex < solidItems.size())
        {
            BasketScanner.BasketEntry entry = solidItems.get(basketIndex);
            PacketDistributor.sendToServer(new TakeBasketItemPayload(entry.basketPos(), entry.slot(), potPos));
            return true;
        }

        if (!liquidItems.isEmpty())
        {
            int panelRight = this.width - 20;
            int iconLeft = panelRight - LIQUID_ICON_SIZE;
            int top = liquidPanelTop();

            if (isWithin(mouseX, mouseY, iconLeft - 16, top, 14, LIQUID_ICON_SIZE))
            {
                selectedLiquidIndex = Math.floorMod(selectedLiquidIndex - 1, liquidItems.size());
                return true;
            }
            if (isWithin(mouseX, mouseY, iconLeft + LIQUID_ICON_SIZE + 2, top, 14, LIQUID_ICON_SIZE))
            {
                selectedLiquidIndex = Math.floorMod(selectedLiquidIndex + 1, liquidItems.size());
                return true;
            }
            if (isWithin(mouseX, mouseY, iconLeft, top, LIQUID_ICON_SIZE, LIQUID_ICON_SIZE))
            {
                BasketScanner.BasketEntry entry = liquidItems.get(selectedLiquidIndex);
                PacketDistributor.sendToServer(new SetPotLiquidPayload(entry.basketPos(), entry.slot(), potPos));
                selectedLiquidIndex = Math.min(selectedLiquidIndex, Math.max(0, liquidItems.size() - 2));
                return true;
            }
        }

        int[] serveBounds = serveButtonBounds();
        if (isWithin(mouseX, mouseY, serveBounds[0], serveBounds[1], serveBounds[2], serveBounds[3]))
        {
            PacketDistributor.sendToServer(new ServePotPayload(potPos, paceMeter));
            return true;
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    private static boolean isWithin(double mouseX, double mouseY, int x, int y, int w, int h)
    {
        return mouseX >= x && mouseX < x + w && mouseY >= y && mouseY < y + h;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY)
    {
        int maxScroll = Math.max(0, ceilDiv(solidItems.size(), BASKET_COLUMNS) - BASKET_VISIBLE_ROWS);
        basketScrollRow = Math.max(0, Math.min(maxScroll, basketScrollRow - (int) Math.signum(scrollY)));
        return true;
    }

    private int basketPanelTop()
    {
        return this.height / 2 - (BASKET_CELL_SIZE * BASKET_VISIBLE_ROWS) / 2;
    }

    private int liquidPanelTop()
    {
        return this.height / 2 - LIQUID_ICON_SIZE / 2;
    }

    private int itemX(int centerX, int i)
    {
        double angle = i * 2.39996;
        double jitter = Math.sin(System.nanoTime() / 1.0e8 + i * 1.7) * jitterRadius();
        return centerX + (int) (INGREDIENT_BASE_RADIUS * Math.cos(angle) + jitter);
    }

    private int itemY(int centerY, int i)
    {
        double angle = i * 2.39996;
        double jitter = Math.cos(System.nanoTime() / 1.0e8 + i * 2.3) * jitterRadius();
        return centerY + (int) (INGREDIENT_BASE_RADIUS * Math.sin(angle) + jitter);
    }

    private double jitterRadius()
    {
        return MAX_JITTER_RADIUS * Math.min(1.0, smoothedSpeed / MAX_GOOD_SPEED);
    }

    private int ingredientIndexAt(double mouseX, double mouseY)
    {
        List<ItemStack> ingredients = currentIngredients();
        int centerX = this.width / 2;
        int centerY = this.height / 2;
        for (int i = 0; i < ingredients.size(); i++)
        {
            if (Math.abs(mouseX - itemX(centerX, i)) <= 8 && Math.abs(mouseY - itemY(centerY, i)) <= 8)
                return i;
        }
        return -1;
    }

    private int basketSlotAt(double mouseX, double mouseY)
    {
        int top = basketPanelTop();
        int col = (int) ((mouseX - BASKET_PANEL_LEFT) / BASKET_CELL_SIZE);
        int visibleRow = (int) ((mouseY - top) / BASKET_CELL_SIZE);
        if (col < 0 || col >= BASKET_COLUMNS || visibleRow < 0 || visibleRow >= BASKET_VISIBLE_ROWS)
            return -1;
        if (mouseX < BASKET_PANEL_LEFT || mouseY < top)
            return -1;
        int row = visibleRow + basketScrollRow;
        return row * BASKET_COLUMNS + col;
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick)
    {
        super.render(guiGraphics, mouseX, mouseY, partialTick);

        long now = System.nanoTime();
        if (lastFrameNanos < 0)
            lastFrameNanos = now;
        double elapsedSeconds = (now - lastFrameNanos) / 1_000_000_000.0;
        lastFrameNanos = now;

        double rawSpeed = elapsedSeconds > 0 ? pendingDistance / elapsedSeconds : 0;
        pendingDistance = 0;
        smoothedSpeed += (rawSpeed - smoothedSpeed) * SPEED_SMOOTHING;

        boolean onPace = hasLastMouse && smoothedSpeed >= MIN_GOOD_SPEED && smoothedSpeed <= MAX_GOOD_SPEED;
        if (onPace)
            paceMeter = Math.min(100.0, paceMeter + METER_RECOVER_PER_SECOND * elapsedSeconds);
        else
            paceMeter = Math.max(0.0, paceMeter - METER_DRAIN_PER_SECOND * elapsedSeconds);

        List<ItemStack> ingredientsNow = currentIngredients();
        boolean ingredientsEmptyNow = ingredientsNow.isEmpty();
        if (ingredientsEmptyNow && !wasIngredientsEmpty)
        {
            cookProgressSeconds = 0;
            finishSent = false;
            ruinSent = false;
        }
        wasIngredientsEmpty = ingredientsEmptyNow;

        if (!ruinSent && paceMeter <= 0.0 && !ingredientsEmptyNow)
        {
            ruinSent = true;
            PacketDistributor.sendToServer(new RuinPotPayload(potPos));
        }

        boolean currentlyCookingWell = paceMeter >= MIN_PACE_FOR_DONENESS
                && !ingredientsEmptyNow && !currentLiquid().isEmpty();
        if (currentlyCookingWell)
            cookProgressSeconds = Math.min(Demanufacture.COOK_DURATION_SECONDS, cookProgressSeconds + elapsedSeconds);

        int centerX = this.width / 2;
        int centerY = this.height / 2;

        guiGraphics.fill(centerX - PAN_RADIUS, centerY - PAN_RADIUS, centerX + PAN_RADIUS, centerY + PAN_RADIUS, 0xFF4A4A4A);

        if (ingredientsEmptyNow)
        {
            guiGraphics.fill(itemX(centerX, 0) - 4, itemY(centerY, 0) - 4, itemX(centerX, 0) + 4, itemY(centerY, 0) + 4, 0xFF808080);
        }
        else
        {
            for (int i = 0; i < ingredientsNow.size(); i++)
                guiGraphics.renderItem(ingredientsNow.get(i), itemX(centerX, i) - 8, itemY(centerY, i) - 8);
        }

        guiGraphics.drawCenteredString(this.font, this.title, centerX, centerY - PAN_RADIUS - 20, 0xFFFFFF);

        renderPaceMeter(guiGraphics, centerX, centerY + PAN_RADIUS + 14);
        renderDonenessMeter(guiGraphics, centerX, centerY + PAN_RADIUS + 34);
        renderServeButton(guiGraphics, mouseX, mouseY, !ingredientsEmptyNow);
        renderBasketPanel(guiGraphics, mouseX, mouseY);
        renderLiquidPanel(guiGraphics, mouseX, mouseY);

        checkAndSendFinishCooking();
    }

    private List<ItemStack> currentIngredients()
    {
        BlockEntity be = potBlockEntity();
        return be != null ? be.getData(Demanufacture.POT_INGREDIENTS) : List.of();
    }

    private ItemStack currentLiquid()
    {
        BlockEntity be = potBlockEntity();
        return be != null ? be.getData(Demanufacture.POT_LIQUID) : ItemStack.EMPTY;
    }

    private BlockEntity potBlockEntity()
    {
        Level level = Minecraft.getInstance().level;
        return level != null ? level.getBlockEntity(potPos) : null;
    }

    private double currentDoneness()
    {
        return cookProgressSeconds / Demanufacture.COOK_DURATION_SECONDS;
    }

    private void checkAndSendFinishCooking()
    {
        if (!finishSent && currentDoneness() >= 1.0)
        {
            finishSent = true;
            PacketDistributor.sendToServer(new FinishCookingPayload(potPos));
        }
    }

    private void renderPaceMeter(GuiGraphics guiGraphics, int centerX, int top)
    {
        renderMeter(guiGraphics, centerX, top, paceMeter / 100.0, "Pace");
    }

    private void renderDonenessMeter(GuiGraphics guiGraphics, int centerX, int top)
    {
        renderMeter(guiGraphics, centerX, top, currentDoneness(), "Doneness");
    }

    private void renderMeter(GuiGraphics guiGraphics, int centerX, int top, double fraction, String label)
    {
        int meterWidth = 140;
        int meterHeight = 8;
        int left = centerX - meterWidth / 2;

        guiGraphics.fill(left - 1, top - 1, left + meterWidth + 1, top + meterHeight + 1, 0xFF000000);
        guiGraphics.fill(left, top, left + meterWidth, top + meterHeight, 0xFF3A3A3A);

        int filledWidth = (int) (meterWidth * fraction);
        int red = (int) (255 * (1 - fraction));
        int green = (int) (255 * fraction);
        int color = 0xFF000000 | (red << 16) | (green << 8);
        guiGraphics.fill(left, top, left + filledWidth, top + meterHeight, color);

        guiGraphics.drawString(this.font, label, left + meterWidth + 6, top, 0xFFAAAAAA, false);
    }

    private int[] serveButtonBounds()
    {
        int width = 50;
        int height = 16;
        int x = this.width / 2 - width / 2;
        int y = this.height / 2 + PAN_RADIUS + 54;
        return new int[] {x, y, width, height};
    }

    private void renderServeButton(GuiGraphics guiGraphics, int mouseX, int mouseY, boolean enabled)
    {
        int[] b = serveButtonBounds();
        boolean hovered = enabled && isWithin(mouseX, mouseY, b[0], b[1], b[2], b[3]);
        int bg = !enabled ? 0x40444444 : hovered ? 0xFF5A8F4A : 0xFF3A6B2E;
        int textColor = enabled ? 0xFFFFFF : 0xFF888888;

        guiGraphics.fill(b[0], b[1], b[0] + b[2], b[1] + b[3], bg);
        guiGraphics.drawCenteredString(this.font, "Serve", b[0] + b[2] / 2, b[1] + 4, textColor);
    }

    private void renderBasketPanel(GuiGraphics guiGraphics, int mouseX, int mouseY)
    {
        int top = basketPanelTop();
        int panelWidth = BASKET_CELL_SIZE * BASKET_COLUMNS;
        int panelHeight = BASKET_CELL_SIZE * BASKET_VISIBLE_ROWS;

        guiGraphics.fill(BASKET_PANEL_LEFT - 4, top - 16, BASKET_PANEL_LEFT + panelWidth + 4, top + panelHeight + 4, 0x80000000);
        guiGraphics.drawString(this.font, "Basket", BASKET_PANEL_LEFT, top - 14, 0xFFFFFF, false);

        for (int visibleRow = 0; visibleRow < BASKET_VISIBLE_ROWS; visibleRow++)
        {
            for (int col = 0; col < BASKET_COLUMNS; col++)
            {
                int index = (visibleRow + basketScrollRow) * BASKET_COLUMNS + col;
                if (index >= solidItems.size())
                    continue;

                int x = BASKET_PANEL_LEFT + col * BASKET_CELL_SIZE;
                int y = top + visibleRow * BASKET_CELL_SIZE;

                boolean hovered = basketSlotAt(mouseX, mouseY) == index;
                guiGraphics.fill(x, y, x + BASKET_CELL_SIZE - 2, y + BASKET_CELL_SIZE - 2, hovered ? 0x80FFFFFF : 0x40FFFFFF);

                ItemStack stack = solidItems.get(index).stack();
                guiGraphics.renderItem(stack, x + 4, y + 4);
                guiGraphics.renderItemDecorations(this.font, stack, x + 4, y + 4);
            }
        }

        boolean canScrollMore = ceilDiv(solidItems.size(), BASKET_COLUMNS) > BASKET_VISIBLE_ROWS;
        if (canScrollMore)
            guiGraphics.drawString(this.font, "scroll for more", BASKET_PANEL_LEFT, top + panelHeight + 4, 0xFFAAAAAA, false);
    }

    private void renderLiquidPanel(GuiGraphics guiGraphics, int mouseX, int mouseY)
    {
        int panelRight = this.width - 20;
        int iconLeft = panelRight - LIQUID_ICON_SIZE;
        int top = liquidPanelTop();

        guiGraphics.drawString(this.font, "Liquid Flask", iconLeft - 40, top - 26, 0xFFFFFF, false);

        ItemStack inPot = currentLiquid();
        guiGraphics.drawString(this.font, "In pot:", iconLeft - 40, top - 12, 0xFFAAAAAA, false);
        if (!inPot.isEmpty())
            guiGraphics.renderItem(inPot, iconLeft - 40, top - 12 - 2);

        if (liquidItems.isEmpty())
        {
            guiGraphics.drawString(this.font, "no liquids in basket", iconLeft - 60, top + 4, 0xFF888888, false);
            return;
        }

        selectedLiquidIndex = Math.floorMod(selectedLiquidIndex, liquidItems.size());
        ItemStack candidate = liquidItems.get(selectedLiquidIndex).stack();

        guiGraphics.drawString(this.font, "<", iconLeft - 16, top + 4, 0xFFFFFF, false);
        guiGraphics.drawString(this.font, ">", iconLeft + LIQUID_ICON_SIZE + 4, top + 4, 0xFFFFFF, false);

        boolean hovered = isWithin(mouseX, mouseY, iconLeft, top, LIQUID_ICON_SIZE, LIQUID_ICON_SIZE);
        guiGraphics.fill(iconLeft - 2, top - 2, iconLeft + LIQUID_ICON_SIZE + 2, top + LIQUID_ICON_SIZE + 2, hovered ? 0x80FFFFFF : 0x40FFFFFF);
        guiGraphics.renderItem(candidate, iconLeft + 2, top + 2);
    }

    private static int ceilDiv(int a, int b)
    {
        return (a + b - 1) / b;
    }

    @Override
    public boolean isPauseScreen()
    {
        return false;
    }
}
