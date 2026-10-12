package com.zxblob.demanufacture;

import org.slf4j.Logger;

import com.mojang.logging.LogUtils;

import net.neoforged.fml.common.Mod;

/**
 * Demanufacture: the pack harmoniser. Currently an empty shell.
 * <ul>
 *   <li>The cooking minigames are frozen in {@code frozen/cooking-minigames/}.</li>
 *   <li>The Electro-Zapper and Factory Light (electricity module) now live in the ArtificeCannons mod.</li>
 * </ul>
 * New harmoniser modules (machine / recipe-type adapters, shared standards, bridges) go here.
 */
// The value here must match the modId entry in src/main/templates/META-INF/neoforge.mods.toml
@Mod(Demanufacture.MODID)
public class Demanufacture
{
    public static final String MODID = "demanufacture";

    private static final Logger LOGGER = LogUtils.getLogger();

    public Demanufacture()
    {
        LOGGER.info("Demanufacture loading (harmoniser shell, no modules yet)");
    }
}
