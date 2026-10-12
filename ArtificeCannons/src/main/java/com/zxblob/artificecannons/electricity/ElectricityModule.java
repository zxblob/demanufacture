package com.zxblob.artificecannons.electricity;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModList;
import net.neoforged.fml.loading.FMLEnvironment;

/**
 * Items ported out of Create: Power Grid so the pack can drop that mod. Every part registers only when the mod it
 * builds on is present, and every class that touches those mods lives in its own file so nothing loads without them.
 * <ul>
 *   <li>{@link ZapperContent}: Electro-Zapper, runs on FE (needs Create).</li>
 *   <li>{@link FactoryLightContent}: Factory Light, a device on Create Electro Energetics wires (needs both).</li>
 * </ul>
 */
public final class ElectricityModule {
    public static final String CREATE = "create";
    public static final String ELECTRO_ENERGETICS = "electroenergetics";

    public static void init(IEventBus modBus) {
        ModList mods = ModList.get();
        if (mods.isLoaded(CREATE)) {
            ZapperContent.init(modBus);
        }
        if (mods.isLoaded(CREATE) && mods.isLoaded(ELECTRO_ENERGETICS)) {
            FactoryLightContent.init(modBus);
        }
    }

    static boolean isClient() {
        return FMLEnvironment.dist.isClient();
    }

    private ElectricityModule() {}
}
