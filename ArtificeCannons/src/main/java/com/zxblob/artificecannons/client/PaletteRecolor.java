package com.zxblob.artificecannons.client;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Recolours a gun texture by material. Every cube of the model is classified from its own texels:
 * <ul>
 *   <li><b>wood</b>: dark warm browns (stocks, grips). Only a dye changes them; with metal paint alone they stay wood.</li>
 *   <li><b>trim</b>: only on guns that have no wood. Cubes in grip / mag / cog style bones, or clear colour outliers.
 *       A dye changes them; without a dye they follow the metal.</li>
 *   <li><b>metal</b>: everything else (brass, steel, copper, dark barrels, ...). Takes the metal colour.</li>
 * </ul>
 * Cube-level classification means a part is never half recoloured, so no old colour leaks inside a part. Each group is
 * repainted from a shadow-base-highlight ramp (see {@link #ramp}, or one sampled from an ingot texture), anchored so the group's median brightness becomes
 * exactly the new colour; darker and lighter original tones stay darker and lighter. Pure Java so it can be tested
 * without the game.
 */
public final class PaletteRecolor {
    /** A model cube: its bone name and the texel rectangles of its faces as x0,y0,x1,y1 (exclusive end) quadruples. */
    public record Cube(String bone, int[] rects) {}

    private static final Pattern TRIM_BONE = Pattern.compile("(?i).*(grip|handle|stock|mag|cog|wheel|coil|lever|carry).*");

    /** A 3-colour ramp {shadow, base, highlight} (RGB) built from one colour, with a restrained highlight. */
    public static int[] ramp(int rgb) {
        int r = (rgb >> 16) & 255, g = (rgb >> 8) & 255, b = rgb & 255;
        return new int[]{
                ((int) (r * 0.5) << 16) | ((int) (g * 0.5) << 8) | (int) (b * 0.5),
                rgb & 0xFFFFFF,
                ((int) (r + (255 - r) * 0.2) << 16) | ((int) (g + (255 - g) * 0.2) << 8) | (int) (b + (255 - b) * 0.2)};
    }

    /** dyeRamp / metalRamp are {shadow, base, highlight} RGB triples, or null for "not painted". */
    public static int[] recolour(int[] argb, int w, int h, List<Cube> cubes, int[] dyeRamp, int[] metalRamp) {
        int n = w * h;
        int[] owner = new int[n];
        Arrays.fill(owner, -1);
        for (int c = 0; c < cubes.size(); c++) {
            int[] r = cubes.get(c).rects();
            for (int i = 0; i + 3 < r.length; i += 4) {
                for (int y = Math.max(0, r[i + 1]); y < Math.min(h, r[i + 3]); y++) {
                    for (int x = Math.max(0, r[i]); x < Math.min(w, r[i + 2]); x++) {
                        if (owner[y * w + x] < 0) owner[y * w + x] = c;
                    }
                }
            }
        }
        int nc = cubes.size();
        double[][] mean = new double[nc][3];
        int[] count = new int[nc];
        for (int i = 0; i < n; i++) {
            if (owner[i] < 0 || (argb[i] >>> 24) == 0) continue;
            int c = owner[i];
            mean[c][0] += (argb[i] >> 16) & 255; mean[c][1] += (argb[i] >> 8) & 255; mean[c][2] += argb[i] & 255;
            count[c]++;
        }
        int total = 0;
        double[] overall = new double[3];
        for (int c = 0; c < nc; c++) {
            if (count[c] == 0) continue;
            for (int k = 0; k < 3; k++) { overall[k] += mean[c][k]; mean[c][k] /= count[c]; }
            total += count[c];
        }
        if (total == 0) return argb.clone();
        for (int k = 0; k < 3; k++) overall[k] /= total;

        // roles: 0 none, 1 metal, 2 wood, 3 trim
        int[] role = new int[nc];
        boolean anyWood = false;
        for (int c = 0; c < nc; c++) {
            if (count[c] == 0) continue;
            role[c] = isWood(mean[c]) ? 2 : 1;
            anyWood |= role[c] == 2;
        }
        if (!anyWood) {
            boolean any = false;
            for (int c = 0; c < nc; c++) {
                if (count[c] > 0 && TRIM_BONE.matcher(cubes.get(c).bone()).matches()) { role[c] = 3; any = true; }
            }
            if (!any) {
                int best = -1;
                for (int c = 0; c < nc; c++) {
                    if (count[c] == 0) continue;
                    double d = dist(mean[c], overall);
                    if (d > 40 && count[c] <= 0.25 * total) { role[c] = 3; any = true; }
                    if (best < 0 || d > dist(mean[best], overall)) best = c;
                }
                if (!any && best >= 0) role[best] = 3;
            }
        }

        boolean dye = dyeRamp != null, metal = metalRamp != null;
        int[] out = argb.clone();
        // group 1 = metal ramp, group 2 = dye ramp
        int[] group = new int[nc];
        for (int c = 0; c < nc; c++) {
            if (role[c] == 1) group[c] = metal ? 1 : 0;
            else if (role[c] == 2) group[c] = dye ? 2 : 0;
            else if (role[c] == 3) group[c] = dye ? 2 : (metal ? 1 : 0);
        }
        if (metal) repaint(argb, out, owner, group, 1, metalRamp);
        if (dye) repaint(argb, out, owner, group, 2, dyeRamp);
        return out;
    }

    private static void repaint(int[] src, int[] dst, int[] owner, int[] group, int g, int[] ramp) {
        List<Double> lumas = new ArrayList<>();
        for (int i = 0; i < src.length; i++) {
            if (owner[i] < 0 || group[owner[i]] != g || (src[i] >>> 24) == 0) continue;
            lumas.add(luma(src[i]));
        }
        if (lumas.isEmpty()) return;
        Collections.sort(lumas);
        double lmin = lumas.get((int) (lumas.size() * 0.01));
        double lmax = lumas.get(Math.min(lumas.size() - 1, (int) (lumas.size() * 0.99)));
        double lmid = lumas.get(lumas.size() / 2);
        lmid = Math.max(lmin + 1e-3, Math.min(lmax - 1e-3, lmid));
        for (int i = 0; i < src.length; i++) {
            if (owner[i] < 0 || group[owner[i]] != g || (src[i] >>> 24) == 0) continue;
            double l = luma(src[i]);
            double t = l <= lmid ? 0.5 * clamp01((l - lmin) / (lmid - lmin)) : 0.5 + 0.5 * clamp01((l - lmid) / (lmax - lmid));
            int from = t <= 0.5 ? ramp[0] : ramp[1], to = t <= 0.5 ? ramp[1] : ramp[2];
            double f = t <= 0.5 ? t / 0.5 : (t - 0.5) / 0.5;
            double rr = lerp((from >> 16) & 255, (to >> 16) & 255, f);
            double gg = lerp((from >> 8) & 255, (to >> 8) & 255, f);
            double bb = lerp(from & 255, to & 255, f);
            dst[i] = (src[i] & 0xFF000000) | (clamp(rr) << 16) | (clamp(gg) << 8) | clamp(bb);
        }
    }

    private static boolean isWood(double[] c) {
        double r = c[0] / 255, g = c[1] / 255, b = c[2] / 255;
        double max = Math.max(r, Math.max(g, b)), min = Math.min(r, Math.min(g, b)), d = max - min;
        if (d <= 0 || max != r) return false; // red dominant = warm
        double s = d / max;
        double h = ((g - b) / d) % 6 / 6;
        if (h < 0) h += 1;
        if (h > 0.14) return false;
        return (s >= 0.25 && max < 0.5) || (s >= 0.12 && max < 0.3);
    }

    private static double dist(double[] a, double[] b) {
        return Math.sqrt(sq(a[0] - b[0]) + sq(a[1] - b[1]) + sq(a[2] - b[2]));
    }

    private static double lerp(double a, double b, double t) { return a + (b - a) * t; }
    private static double sq(double v) { return v * v; }
    private static double clamp01(double v) { return v < 0 ? 0 : Math.min(v, 1); }
    private static int clamp(double v) { return v < 0 ? 0 : (v > 255 ? 255 : (int) v); }
    private static double luma(int p) { return 0.299 * ((p >> 16) & 255) + 0.587 * ((p >> 8) & 255) + 0.114 * (p & 255); }

    private PaletteRecolor() {}
}
