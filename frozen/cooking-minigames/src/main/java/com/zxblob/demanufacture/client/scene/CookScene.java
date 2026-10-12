package com.zxblob.demanufacture.client.scene;

import java.util.List;

import org.joml.Matrix4f;
import org.joml.Vector4f;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.zxblob.demanufacture.Demanufacture;

import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.client.event.ViewportEvent;

/**
 * The in-world cooking view: while a cook minigame screen is open, the camera sits behind and a
 * little above the player's head looking at the pot, the player is turned to face the pot, the
 * HUD is hidden, and the pot's contents are drawn as 3D items inside it. The screen on top only
 * handles the mouse and the basket / liquid panels.
 * <p>
 * All state is static because there is only ever one local player in one scene at a time.
 */
@EventBusSubscriber(modid = Demanufacture.MODID, value = Dist.CLIENT)
public final class CookScene
{
    // TUNE: camera framing, in blocks.
    private static final double CAMERA_BACK = 0.25;
    private static final double CAMERA_UP = 1.15;
    /** Direction of the spoon spin relative to the exported rotation: flip to -1 if it turns against the stir. */
    private static final float SPOON_DIRECTION = -1f;
    private static final double ORBIT_RADIUS = 0.2;
    // top of the FD pot is 10/16 high; ingredients float just above the liquid line
    private static final double CONTENTS_HEIGHT = 0.6;

    private static boolean active;
    private static BlockPos potPos;
    private static Vec3 focus = Vec3.ZERO;

    private static CameraType savedCameraType;
    private static boolean savedHideGui;

    private static double spoonPhase;
    private static List<ItemStack> contents = List.of();
    private static double rotation;

    private static final Matrix4f PROJECTION = new Matrix4f();
    private static final Matrix4f MODELVIEW = new Matrix4f();
    private static Vec3 cameraPos = Vec3.ZERO;
    private static float cameraYaw;
    private static boolean haveMatrices;

    private CookScene()
    {
    }

    public static void begin(BlockPos pos)
    {
        Minecraft mc = Minecraft.getInstance();
        potPos = pos;
        focus = Vec3.atCenterOf(pos).add(0, 0.1, 0);
        savedCameraType = mc.options.getCameraType();
        savedHideGui = mc.options.hideGui;
        mc.options.setCameraType(CameraType.THIRD_PERSON_BACK);
        mc.options.hideGui = true;
        active = true;
        haveMatrices = false;
        markPotDirty();
    }

    private static void markPotDirty()
    {
        Minecraft mc = Minecraft.getInstance();
        if (mc.levelRenderer != null && potPos != null)
            mc.levelRenderer.setBlocksDirty(potPos.getX(), potPos.getY(), potPos.getZ(), potPos.getX(), potPos.getY(), potPos.getZ());
    }

    /** True while the cook scene is on and {@code pos} is the pot being cooked at. */
    public static boolean isCookingAt(BlockPos pos)
    {
        return active && pos.equals(potPos);
    }

    public static void end()
    {
        if (!active)
            return;
        Minecraft mc = Minecraft.getInstance();
        active = false;
        if (savedCameraType != null)
            mc.options.setCameraType(savedCameraType);
        mc.options.hideGui = savedHideGui;
        contents = List.of();
        markPotDirty();
    }

    public static boolean isActive()
    {
        return active;
    }

    /** Called by the screen every frame: what is in the pot and how far the stir has turned. */
    public static void setContents(List<ItemStack> stacks, double stirRotation, double animationPhase)
    {
        contents = stacks;
        rotation = stirRotation;
        // the spoon shares the arm's phase, so the two can never drift apart
        spoonPhase = animationPhase;
    }

    private static Vec3 forward()
    {
        double f = Math.toRadians(cameraYaw);
        return new Vec3(-Math.sin(f), 0, Math.cos(f));
    }

    private static Vec3 right()
    {
        double f = Math.toRadians(cameraYaw);
        return new Vec3(-Math.cos(f), 0, -Math.sin(f));
    }

    /** World position of ingredient {@code index}, orbiting with the stir. */
    private static Vec3 contentPos(int index)
    {
        double angle = rotation + index * 2.39996;
        // screen angle -> world: screen right = camera right, screen down = towards the camera
        Vec3 offset = right().scale(Math.cos(angle) * ORBIT_RADIUS).add(forward().scale(-Math.sin(angle) * ORBIT_RADIUS));
        return new Vec3(potPos.getX() + 0.5, potPos.getY() + CONTENTS_HEIGHT, potPos.getZ() + 0.5).add(offset);
    }

    /**
     * Screen position (in gui-scaled pixels) of the ingredient at {@code index}, or null when it
     * is behind the camera or no frame has been rendered yet.
     */
    public static double[] screenPosOfContent(int index, int guiWidth, int guiHeight)
    {
        if (!haveMatrices)
            return null;
        Vec3 p = contentPos(index).subtract(cameraPos);
        Vector4f v = new Vector4f((float) p.x, (float) p.y, (float) p.z, 1f);
        v.mul(MODELVIEW);
        v.mul(PROJECTION);
        if (v.w <= 0.0001f)
            return null;
        double nx = v.x / v.w;
        double ny = v.y / v.w;
        return new double[] {(nx * 0.5 + 0.5) * guiWidth, (1 - (ny * 0.5 + 0.5)) * guiHeight};
    }

    @SubscribeEvent
    public static void onCameraAngles(ViewportEvent.ComputeCameraAngles event)
    {
        if (!active)
            return;
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null)
            return;

        Vec3 feet = player.getPosition((float) event.getPartialTick());
        Vec3 eye = feet.add(0, player.getEyeHeight(), 0);
        double dx = focus.x - feet.x;
        double dz = focus.z - feet.z;
        double len = Math.max(1.0E-4, Math.sqrt(dx * dx + dz * dz));
        Vec3 dir = new Vec3(dx / len, 0, dz / len);

        Vec3 cam = eye.subtract(dir.scale(CAMERA_BACK)).add(0, CAMERA_UP, 0);
        Vec3 look = focus.subtract(cam);
        double horizontal = Math.sqrt(look.x * look.x + look.z * look.z);
        float yaw = (float) (Mth.atan2(-look.x, look.z) * 180.0 / Math.PI);
        float pitch = (float) (-Mth.atan2(look.y, horizontal) * 180.0 / Math.PI);

        event.getCamera().setPosition(cam);
        event.setYaw(yaw);
        event.setPitch(pitch);
        event.setRoll(0);
        cameraYaw = yaw;
        cameraPos = cam;
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event)
    {
        if (!active)
            return;
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null)
            return;
        // face the pot so the animated arms line up with it
        double dx = focus.x - player.getX();
        double dz = focus.z - player.getZ();
        float yaw = (float) (Mth.atan2(-dx, dz) * 180.0 / Math.PI);
        player.setYRot(yaw);
        player.yRotO = yaw;
        player.setYHeadRot(yaw);
        player.yHeadRotO = yaw;
        player.yBodyRot = yaw;
        player.yBodyRotO = yaw;
        player.setXRot(20f);
        player.xRotO = 20f;
    }

    private static void renderSpoon(Minecraft mc, PoseStack pose, Vec3 cam, MultiBufferSource.BufferSource buffers)
    {
        BlockState state = mc.level.getBlockState(potPos);
        if (state.getBlock() != vectorwing.farmersdelight.common.registry.ModBlocks.COOKING_POT.get()
                || !SpoonProp.available())
            return;

        BakedModel model = mc.getModelManager().getModel(SpoonModels.SPOON);
        // The rig was authored with the player on the +z side of the pot, the spoon leaning to
        // their right. The pot's own facing has nothing to do with where the player stands, so the
        // spoon is turned to follow the player, not the block: rig +z -> the direction pot -> player.
        double toPlayerX = mc.player.getX() - (potPos.getX() + 0.5);
        double toPlayerZ = mc.player.getZ() - (potPos.getZ() + 0.5);
        float sideYaw = (float) Math.toDegrees(Math.atan2(toPlayerX, toPlayerZ));

        pose.pushPose();
        pose.translate(potPos.getX() - cam.x + 0.5, potPos.getY() - cam.y, potPos.getZ() - cam.z + 0.5);
        pose.mulPose(Axis.YP.rotationDegrees(sideYaw));
        pose.mulPose(Axis.YP.rotationDegrees(SPOON_DIRECTION * SpoonProp.yawAt(spoonPhase)));
        pose.translate(-0.5, 0, -0.5);
        int light = LevelRenderer.getLightColor(mc.level, potPos.above());
        mc.getBlockRenderer().getModelRenderer().renderModel(pose.last(), buffers.getBuffer(RenderType.cutout()),
                state, model, 1f, 1f, 1f, light, OverlayTexture.NO_OVERLAY);
        pose.popPose();
    }

    @SubscribeEvent
    public static void onRenderLevel(RenderLevelStageEvent event)
    {
        if (!active || event.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES)
            return;

        PROJECTION.set(event.getProjectionMatrix());
        MODELVIEW.set(event.getModelViewMatrix());
        haveMatrices = true;

        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null)
            return;

        PoseStack pose = event.getPoseStack();
        Vec3 cam = event.getCamera().getPosition();
        MultiBufferSource.BufferSource buffers = mc.renderBuffers().bufferSource();
        renderSpoon(mc, pose, cam, buffers);
        if (contents.isEmpty())
            return;
        for (int i = 0; i < contents.size(); i++)
        {
            ItemStack stack = contents.get(i);
            if (stack.isEmpty())
                continue;
            Vec3 p = contentPos(i);
            pose.pushPose();
            pose.translate(p.x - cam.x, p.y - cam.y, p.z - cam.z);
            pose.mulPose(Axis.YP.rotation((float) -rotation + i));
            pose.scale(0.5f, 0.5f, 0.5f);
            int light = LevelRenderer.getLightColor(mc.level, BlockPos.containing(p));
            mc.getItemRenderer().renderStatic(stack, ItemDisplayContext.GROUND, light, OverlayTexture.NO_OVERLAY,
                    pose, buffers, mc.level, i);
            pose.popPose();
        }
        buffers.endBatch();
    }
}
