package com.minecoloniesirons;

import com.minecolonies.api.sounds.ModSoundEvents;
import com.minecoloniesirons.colony.equipment.ModIronsEquipmentTypes;
import com.minecoloniesirons.colony.guardtype.ModIronsGuardTypes;
import com.minecoloniesirons.colony.jobs.ModIronsJobs;
import com.minecoloniesirons.colony.module.IronsTowerModules;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Mod(MinecoloniesIronsIntegration.MODID)
public class MinecoloniesIronsIntegration {

    public static final String MODID = "minecolonies_irons";
    private static final Logger LOGGER = LogManager.getLogger(MinecoloniesIronsIntegration.class);

    public MinecoloniesIronsIntegration(IEventBus modEventBus) {
        LOGGER.info("Initializing Minecolonies Iron's Integration mod...");

        // Register custom Minecolonies equipment types, jobs, and guard types
        ModIronsEquipmentTypes.DEFERRED_REGISTER.register(modEventBus);
        ModIronsJobs.DEFERRED_REGISTER.register(modEventBus);
        ModIronsGuardTypes.DEFERRED_REGISTER.register(modEventBus);

        // Register FML setup lifecycle
        modEventBus.addListener(this::onCommonSetup);
    }

    private void onCommonSetup(final FMLCommonSetupEvent event) {
        event.enqueueWork(() -> {
            LOGGER.info("Attaching Mage and Gunner guard modules to Guard Towers...");
            IronsTowerModules.registerTowerModules();

            registerCitizenSoundEvents();
        });
    }

    /**
     * Registers citizen sound event profiles for gunner and mage to prevent NullPointerException
     * in Minecolonies SoundUtils.playSoundAtCitizenWith when citizens play ambient living sounds.
     */
    private void registerCitizenSoundEvents() {
        try {
            var soundEvents = ModSoundEvents.CITIZEN_SOUND_EVENTS;
            if (soundEvents != null) {
                var guardSounds = soundEvents.get("ranger");
                if (guardSounds == null) {
                    guardSounds = soundEvents.get("knight");
                }
                if (guardSounds == null) {
                    guardSounds = soundEvents.get("unemployed");
                }

                var mageSounds = soundEvents.get("druid");
                if (mageSounds == null) {
                    mageSounds = soundEvents.get("cleric");
                }
                if (mageSounds == null) {
                    mageSounds = guardSounds;
                }

                if (guardSounds != null) {
                    soundEvents.putIfAbsent("gunner", guardSounds);
                }
                if (mageSounds != null) {
                    soundEvents.putIfAbsent("mage", mageSounds);
                }
                LOGGER.info("Successfully registered citizen sound profiles for gunner and mage");
            }
        } catch (Exception e) {
            LOGGER.warn("Failed to register citizen sound events: {}", e.getMessage());
        }
    }
}
