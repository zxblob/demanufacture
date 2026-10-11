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
 * First real minigame, for the Stockpot (KC) / Cooking Pot (FD) pair. Stirring, the basket UI,
 * the Liquid Flask, and the doneness meter. Dish naming/output (FoodState) still doesn't exist -
 * finishing a cook right now just flips raw ingredients to their vanilla-smelting-recipe result.
 */
public class StirringMinigameScreen extends Screen
{
    // TUNE: still first-guess numbers.
    private static final double MIN_GOOD_SPEED = 0.6; // radians/second
    private static final double MAX_GOOD_SPEED = 6.0; // radians/second
    private static final int POT_RADIUS = 70;
    private static final int INGREDIENT_ORBIT_RADIUS = 40;

    private static final double METER_DRAIN_PER_SECOND = 18.0;
    private static final double METER_RECOVER_PER_SECOND = 10.0;
    private static final double SPEED_SMOOTHING = 0.15;

    // Doneness only accrues while pace is at least this good - staying in full control of the
    // stir is what cooks the dish, not just time passing.
    private static final double MIN_PACE_FOR_DONENESS = 90.0;

    private static final int BASKET_CELL_SIZE = 24;
    private static final int BASKET_COLUMNS = 3;
    private static final int BASKET_VISIBLE_ROWS = 5;
    private static final int BASKET_PANEL_LEFT = 20;

    private static final int LIQUID_ICON_SIZE = 20;

    private final BlockPos potPos;

    private boolean hasLastAngle = false;
    private double lastAngle;
    private double displayRotation;
    private double previousDisplayRotation;
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

    public StirringMinigameScreen(BlockPos potPos, List<BasketScanner.BasketEntry> basketItems)
    {
        super(Component.translatable("screen.demanufacture.stirring"));
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

        double centerX = this.width / 2.0;
        double centerY = this.height / 2.0;
        double dx = mouseX - centerX;
        double dy = mouseY - centerY;

        if (dx * dx + dy * dy < 16 * 16)
        {
            hasLastAngle = false;
            return;
        }

        double angle = Math.atan2(dy, dx);
        if (hasLastAngle)
        {
            double delta = angle - lastAngle;
            while (delta > Math.PI) delta -= 2 * Math.PI;
            while (delta <= -Math.PI) delta += 2 * Math.PI;
            displayRotation += delta;
        }
        lastAngle = angle;
        hasLastAngle = true;
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

    /**
     * @return index into the pot's ingredient list for the item under the cursor, or -1. Mirrors
     * the layout math in render() exactly - same center, same golden-angle offsets per index.
     */
    private int ingredientIndexAt(double mouseX, double mouseY)
    {
        List<ItemStack> ingredients = currentIngredients();
        int centerX = this.width / 2;
        int centerY = this.height / 2;
        for (int i = 0; i < ingredients.size(); i++)
        {
            double itemAngle = displayRotation + i * 2.39996;
            double ix = centerX + INGREDIENT_ORBIT_RADIUS * Math.cos(itemAngle);
            double iy = centerY + INGREDIENT_ORBIT_RADIUS * Math.sin(itemAngle);
            if (Math.abs(mouseX - ix) <= 8 && Math.abs(mouseY - iy) <= 8)
                return i;
        }
        return -1;
    }

    /** @return index into solidItems for the cell under the cursor, or -1 if none. */
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

        double rawSpeed = elapsedSeconds > 0 ? (displayRotation - previousDisplayRotation) / elapsedSeconds : 0;
        previousDisplayRotation = displayRotation;
        smoothedSpeed += (rawSpeed - smoothedSpeed) * SPEED_SMOOTHING;

        boolean onPace = hasLastAngle && Math.abs(smoothedSpeed) >= MIN_GOOD_SPEED && Math.abs(smoothedSpeed) <= MAX_GOOD_SPEED;
        if (onPace)
            paceMeter = Math.min(100.0, paceMeter + METER_RECOVER_PER_SECOND * elapsedSeconds);
        else
            paceMeter = Math.max(0.0, paceMeter - METER_DRAIN_PER_SECOND * elapsedSeconds);

        List<ItemStack> ingredientsNow = currentIngredients();
        boolean ingredientsEmptyNow = ingredientsNow.isEmpty();
        if (ingredientsEmptyNow && !wasIngredientsEmpty)
        {
            // Fresh start: whatever cooked, burned, or got taken back out, a clean pot means a
            // clean slate for the next batch rather than carrying over stale progress.
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

        guiGraphics.fill(centerX - POT_RADIUS, centerY - POT_RADIUS, centerX + POT_RADIUS, centerY + POT_RADIUS, 0xFF2B2B2B);

        if (ingredientsEmptyNow)
        {
            int ix = centerX + (int) (INGREDIENT_ORBIT_RADIUS * Math.cos(displayRotation));
            int iy = centerY + (int) (INGREDIENT_ORBIT_RADIUS * Math.sin(displayRotation));
            guiGraphics.fill(ix - 4, iy - 4, ix + 4, iy + 4, 0xFF808080);
        }
        else
        {
            // Golden-angle spacing per item gives each one a fixed, non-repeating offset from
            // the others so a handful of ingredients look scattered around the pot rather than
            // stacked on top of each other, while still all turning together with the stir.
            for (int i = 0; i < ingredientsNow.size(); i++)
            {
                double itemAngle = displayRotation + i * 2.39996; // golden angle, radians
                int ix = centerX + (int) (INGREDIENT_ORBIT_RADIUS * Math.cos(itemAngle));
                int iy = centerY + (int) (INGREDIENT_ORBIT_RADIUS * Math.sin(itemAngle));
                guiGraphics.renderItem(ingredientsNow.get(i), ix - 8, iy - 8);
            }
        }

        guiGraphics.drawCenteredString(this.font, this.title, centerX, centerY - POT_RADIUS - 20, 0xFFFFFF);

        renderPaceMeter(guiGraphics, centerX, centerY + POT_RADIUS + 14);
        renderDonenessMeter(guiGraphics, centerX, centerY + POT_RADIUS + 34);
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

    /** 0..1. Only advances while pace stays at/above {@link #MIN_PACE_FOR_DONENESS}. */
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

    /** {x, y, width, height} - shared between click-detection and rendering so they can't drift apart. */
    private int[] serveButtonBounds()
    {
        int width = 50;
        int height = 16;
        int x = this.width / 2 - width / 2;
        int y = this.height / 2 + POT_RADIUS + 54;
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
