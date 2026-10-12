package com.zxblob.demanufacture.client.anim;

import java.util.HashMap;
import java.util.Map;

import dev.kosmx.playerAnim.api.TransformType;
import dev.kosmx.playerAnim.api.layered.IAnimation;
import dev.kosmx.playerAnim.api.layered.KeyframeAnimationPlayer;
import dev.kosmx.playerAnim.core.data.KeyframeAnimation;
import dev.kosmx.playerAnim.core.util.Vec3f;

/**
 * Plays a looping Player Animator animation by <em>phase</em> instead of by time: the pose is a
 * pure function of {@link #phase}, one full turn = one pass through the animation. The stirring
 * minigame feeds it the mouse's angle, so the arm follows the mouse and stops when it stops.
 * <p>
 * Player Animator's own player can only run forward in whole ticks, so the animation is sampled
 * once up front (4 samples per tick) by stepping a normal player through the loop.
 */
public final class PhaseAnimation implements IAnimation
{
    private static final int SUBSTEPS = 4;
    // Small values on purpose: Player Animator wraps rotations to +-pi, so big probes come back changed
    // and would be mistaken for animated data.
    private static final float[] PROBE_A = {0.11f, 0.22f, 0.33f};
    private static final float[] PROBE_B = {-0.05f, -0.06f, -0.07f};

    /** Turns (1.0 = one full loop). Any value, it wraps. */
    public static volatile double phase;

    private static final Map<KeyframeAnimation, PhaseAnimation> CACHE = new HashMap<>();

    private record Part(float[][] position, float[][] rotation, boolean[] positionAnimated, boolean[] rotationAnimated)
    {
    }

    private final Map<String, Part> parts = new HashMap<>();
    private final int samples;

    private PhaseAnimation(KeyframeAnimation animation)
    {
        int ticks = Math.max(1, animation.endTick + 1);
        samples = ticks * SUBSTEPS;
        KeyframeAnimationPlayer player = new KeyframeAnimationPlayer(animation);
        for (String name : animation.getBodyParts().keySet())
            parts.put(name, new Part(new float[samples][], new float[samples][], new boolean[3], new boolean[3]));

        for (int i = 0; i < samples; i++)
        {
            player.setupAnim((i % SUBSTEPS) / (float) SUBSTEPS);
            for (Map.Entry<String, Part> e : parts.entrySet())
            {
                sample(player, e.getKey(), TransformType.POSITION, e.getValue().position(), e.getValue().positionAnimated(), i);
                sample(player, e.getKey(), TransformType.ROTATION, e.getValue().rotation(), e.getValue().rotationAnimated(), i);
            }
            if (i % SUBSTEPS == SUBSTEPS - 1)
                player.tick();
        }
    }

    private static void sample(KeyframeAnimationPlayer player, String name, TransformType type, float[][] out,
                               boolean[] animated, int index)
    {
        Vec3f a = player.get3DTransform(name, type, 0f, new Vec3f(PROBE_A[0], PROBE_A[1], PROBE_A[2]));
        Vec3f b = player.get3DTransform(name, type, 0f, new Vec3f(PROBE_B[0], PROBE_B[1], PROBE_B[2]));
        float[] av = {a.getX(), a.getY(), a.getZ()};
        float[] bv = {b.getX(), b.getY(), b.getZ()};
        float[] value = new float[3];
        for (int axis = 0; axis < 3; axis++)
        {
            // an axis the animation does not drive just hands the model's own value back
            boolean passthrough = Math.abs(av[axis] - PROBE_A[axis]) < 1.0E-4f && Math.abs(bv[axis] - PROBE_B[axis]) < 1.0E-4f;
            if (!passthrough)
                animated[axis] = true;
            value[axis] = av[axis];
        }
        out[index] = value;
    }

    public static PhaseAnimation of(KeyframeAnimation animation)
    {
        return CACHE.computeIfAbsent(animation, PhaseAnimation::new);
    }

    @Override
    public boolean isActive()
    {
        return true;
    }

    @Override
    public Vec3f get3DTransform(String modelName, TransformType type, float tickDelta, Vec3f value0)
    {
        Part part = parts.get(modelName);
        if (part == null || (type != TransformType.POSITION && type != TransformType.ROTATION))
            return value0;

        boolean rotation = type == TransformType.ROTATION;
        float[][] data = rotation ? part.rotation() : part.position();
        boolean[] animated = rotation ? part.rotationAnimated() : part.positionAnimated();

        double p = phase - Math.floor(phase);
        double at = p * samples;
        int i0 = ((int) Math.floor(at)) % samples;
        int i1 = (i0 + 1) % samples;
        float f = (float) (at - Math.floor(at));

        float x = animated[0] ? data[i0][0] + (data[i1][0] - data[i0][0]) * f : value0.getX();
        float y = animated[1] ? data[i0][1] + (data[i1][1] - data[i0][1]) * f : value0.getY();
        float z = animated[2] ? data[i0][2] + (data[i1][2] - data[i0][2]) * f : value0.getZ();
        return new Vec3f(x, y, z);
    }

    @Override
    public void setupAnim(float tickDelta)
    {
    }
}
