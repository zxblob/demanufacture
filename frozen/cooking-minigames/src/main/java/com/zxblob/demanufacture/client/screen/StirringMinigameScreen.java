package com.zxblob.demanufacture.client.screen;

import java.util.ArrayList;
import java.util.List;

import com.zxblob.demanufacture.Demanufacture;
import com.zxblob.demanufacture.client.anim.CookAnimationBootstrap;
import com.zxblob.demanufacture.client.scene.CookScene;
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
    // the cook scene aims the camera so the pot sits at the centre of the screen
    private static final String ANIMATION = "fd_cooking_pot.work";
    /** +1 when clockwise mouse motion should play the animation forwards; flip if the arm goes the wrong way. */
    private static final double PHASE_DIRECTION = -1.0;
    /** One mouse circle = one pass through the work loop. */
    private static final double TURN = 2 * Math.PI;

    // Pace feedback is diegetic (steam / smoke over the pot); the meters are only a debug overlay on H.
    private static final double GOOD_PACE_STEAM = 70.0;
    private static final double BAD_PACE_SMOKE = 35.0;

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
    private boolean showMeters = false;
    private double particleClock = 0;
    private boolean doneChimePlayed = false;
    private double sceneSeconds = 0;

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
    protected void init()
    {
        super.init();
        if (!CookScene.isActive())
        {
            CookScene.begin(potPos);
            CookAnimationBootstrap.playPhaseIfPresent(ANIMATION);
        }
    }

    /** No dimming and no blur: the whole point of this screen is seeing the pot. */
    @Override
    public void renderBackground(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick)
    {
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers)
    {
        if (keyCode == org.lwjgl.glfw.GLFW.GLFW_KEY_H)
        {
            showMeters = !showMeters;
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public void removed()
    {
        CookAnimationBootstrap.stopIfPresent();
        CookScene.end();
        super.removed();
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
        if (button == 1)
        {
            if (!currentIngredients().isEmpty())
                PacketDistributor.sendToServer(new ServePotPayload(potPos, paceMeter));
            return true;
        }

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
        if (showMeters && isWithin(mouseX, mouseY, serveBounds[0], serveBounds[1], serveBounds[2], serveBounds[3]))
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
     * @return index into the pot's ingredient list for the ingredient under the cursor, or -1.
     * Uses the same projected positions the 3D items are drawn at.
     */
    private int ingredientIndexAt(double mouseX, double mouseY)
    {
        int count = currentIngredients().size();
        for (int i = 0; i < count; i++)
        {
            double[] screen = CookScene.screenPosOfContent(i, this.width, this.height);
            if (screen != null && Math.abs(mouseX - screen[0]) <= 12 && Math.abs(mouseY - screen[1]) <= 12)
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

        // The pot, the ingredients and the spoon are drawn in the world by CookScene and the arm
        // follows the mouse circle; this screen only keeps the panels (plus optional debug meters).
        double phase = PHASE_DIRECTION * displayRotation / TURN;
        CookScene.setContents(ingredientsNow, displayRotation, phase);
        CookAnimationBootstrap.setPhaseIfPresent(phase);
        sceneSeconds += elapsedSeconds;
        spawnFeedback(elapsedSeconds, ingredientsNow);

        int centerX = this.width / 2;
        if (showMeters)
        {
            renderPaceMeter(guiGraphics, centerX, this.height - 64);
            renderDonenessMeter(guiGraphics, centerX, this.height - 48);
            renderServeButton(guiGraphics, mouseX, mouseY, !ingredientsEmptyNow);
        }
        if (sceneSeconds < 8)
            guiGraphics.drawCenteredString(this.font, "Circle the mouse to stir  -  right-click to serve  -  H: meters",
                    centerX, this.height - 18, 0x99FFFFFF);
        renderBasketPanel(guiGraphics, mouseX, mouseY);
        renderLiquidPanel(guiGraphics, mouseX, mouseY);

        checkAndSendFinishCooking();
    }

    /** Steam when the stir is on pace, smoke when it is about to burn, a chime when done. */
    private void spawnFeedback(double elapsedSeconds, List<ItemStack> ingredients)
    {
        Level level = Minecraft.getInstance().level;
        if (level == null || ingredients.isEmpty())
            return;

        particleClock += elapsedSeconds;
        if (particleClock >= 0.12)
        {
            particleClock = 0;
            double x = potPos.getX() + 0.5 + (level.random.nextDouble() - 0.5) * 0.4;
            double y = potPos.getY() + 0.75;
            double z = potPos.getZ() + 0.5 + (level.random.nextDouble() - 0.5) * 0.4;
            if (paceMeter >= GOOD_PACE_STEAM)
                level.addParticle(net.minecraft.core.particles.ParticleTypes.CAMPFIRE_COSY_SMOKE, x, y, z, 0, 0.02, 0);
            else if (paceMeter <= BAD_PACE_SMOKE)
                level.addParticle(net.minecraft.core.particles.ParticleTypes.SMOKE, x, y, z, 0, 0.03, 0);
        }

        if (currentDoneness() >= 1.0 && !doneChimePlayed)
        {
            doneChimePlayed = true;
            for (int i = 0; i < 6; i++)
                level.addParticle(net.minecraft.core.particles.ParticleTypes.HAPPY_VILLAGER,
                        potPos.getX() + 0.5 + (level.random.nextDouble() - 0.5) * 0.6, potPos.getY() + 0.9,
                        potPos.getZ() + 0.5 + (level.random.nextDouble() - 0.5) * 0.6, 0, 0.05, 0);
            level.playLocalSound(potPos, net.minecraft.sounds.SoundEvents.NOTE_BLOCK_CHIME.value(),
                    net.minecraft.sounds.SoundSource.BLOCKS, 1.0f, 1.2f, false);
        }
        if (currentDoneness() < 0.01)
            doneChimePlayed = false;
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
        int y = this.height - 32;
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
