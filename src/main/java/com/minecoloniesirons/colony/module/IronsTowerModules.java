package com.minecoloniesirons.colony.module;

import com.minecolonies.api.colony.buildings.ModBuildings;
import com.minecolonies.api.colony.buildings.registry.BuildingEntry;
import com.minecolonies.core.colony.buildings.modules.GuardBuildingModule;
import com.minecolonies.core.colony.buildings.moduleviews.CombinedHiringLimitModuleView;
import com.minecoloniesirons.colony.guardtype.ModIronsGuardTypes;
import net.minecraft.resources.ResourceLocation;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.List;

public final class IronsTowerModules {

    private static final Logger LOGGER = LogManager.getLogger(IronsTowerModules.class);

    public static final ResourceLocation MAGE_RESEARCH_EFFECT =
            ResourceLocation.fromNamespaceAndPath("minecolonies", "effects/mage");
    public static final ResourceLocation GUNNER_RESEARCH_EFFECT =
            ResourceLocation.fromNamespaceAndPath("minecolonies", "effects/gunner");

    public static final BuildingEntry.ModuleProducer<GuardBuildingModule, CombinedHiringLimitModuleView> MAGE_TOWER_WORK =
            new BuildingEntry.ModuleProducer<>(
                    "mage_tower_work",
                    () -> new GuardBuildingModule(
                            ModIronsGuardTypes.MAGE.get(),
                            false,
                            b -> 1,
                            MAGE_RESEARCH_EFFECT
                    ),
                    () -> CombinedHiringLimitModuleView::new
            );

    public static final BuildingEntry.ModuleProducer<GuardBuildingModule, CombinedHiringLimitModuleView> GUNNER_TOWER_WORK =
            new BuildingEntry.ModuleProducer<>(
                    "gunner_tower_work",
                    () -> new GuardBuildingModule(
                            ModIronsGuardTypes.GUNNER.get(),
                            false,
                            b -> 1,
                            GUNNER_RESEARCH_EFFECT
                    ),
                    () -> CombinedHiringLimitModuleView::new
            );

    public static void registerTowerModules() {
        try {
            injectIntoBuilding(ModBuildings.guardTower.get());
        } catch (Exception e) {
            LOGGER.warn("Failed to attach Iron's guard modules to GuardTower: {}", e.getMessage());
        }

        try {
            if (ModBuildings.barracksTower != null && ModBuildings.barracksTower.isBound()) {
                injectIntoBuilding(ModBuildings.barracksTower.get());
            }
        } catch (Exception e) {
            LOGGER.warn("Failed to attach Iron's guard modules to BarracksTower: {}", e.getMessage());
        }
    }

    private static void injectIntoBuilding(BuildingEntry entry) {
        if (entry == null) return;
        List<BuildingEntry.ModuleProducer> producers = entry.getModuleProducers();
        if (producers == null) return;

        if (producers.stream().noneMatch(p -> "mage_tower_work".equals(p.key))) {
            producers.add(MAGE_TOWER_WORK);
            LOGGER.info("Successfully registered Mage Guard module in {}", entry.getRegistryName());
        }

        if (producers.stream().noneMatch(p -> "gunner_tower_work".equals(p.key))) {
            producers.add(GUNNER_TOWER_WORK);
            LOGGER.info("Successfully registered Gunner Guard module in {}", entry.getRegistryName());
        }
    }

    private IronsTowerModules() {}
}
