package com.minecoloniesirons.colony.guardtype;

import com.minecolonies.api.colony.guardtype.GuardType;
import com.minecolonies.api.entity.citizen.Skill;
import com.minecolonies.apiimp.CommonMinecoloniesAPIImpl;
import com.minecoloniesirons.colony.jobs.JobGunner;
import com.minecoloniesirons.colony.jobs.JobMage;
import com.minecoloniesirons.colony.jobs.ModIronsJobs;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModIronsGuardTypes {

    public static final String MODID = "minecolonies_irons";

    public static final ResourceLocation MAGE_ID = ResourceLocation.fromNamespaceAndPath(MODID, "mage");
    public static final ResourceLocation GUNNER_ID = ResourceLocation.fromNamespaceAndPath(MODID, "gunner");

    public static final DeferredRegister<GuardType> DEFERRED_REGISTER =
            DeferredRegister.create(CommonMinecoloniesAPIImpl.GUARD_TYPES, MODID);

    public static final DeferredHolder<GuardType, GuardType> MAGE = DEFERRED_REGISTER.register("mage", () ->
            new GuardType.Builder()
                    .setRegistryName(MAGE_ID)
                    .setJobEntry(ModIronsJobs.MAGE::get)
                    .setJobTranslationKey("com.minecolonies.job.mage")
                    .setButtonTranslationKey("com.minecolonies.coremod.gui.workerhuts.mage")
                    .setPrimarySkill(Skill.Mana)
                    .setSecondarySkill(Skill.Focus)
                    .setWorkerSoundName("worker.druid")
                    .setClazz(JobMage.class)
                    .createGuardType()
    );

    public static final DeferredHolder<GuardType, GuardType> GUNNER = DEFERRED_REGISTER.register("gunner", () ->
            new GuardType.Builder()
                    .setRegistryName(GUNNER_ID)
                    .setJobEntry(ModIronsJobs.GUNNER::get)
                    .setJobTranslationKey("com.minecolonies.job.gunner")
                    .setButtonTranslationKey("com.minecolonies.coremod.gui.workerhuts.gunner")
                    .setPrimarySkill(Skill.Dexterity)
                    .setSecondarySkill(Skill.Agility)
                    .setWorkerSoundName("worker.archer")
                    .setClazz(JobGunner.class)
                    .createGuardType()
    );

    private ModIronsGuardTypes() {}
}
