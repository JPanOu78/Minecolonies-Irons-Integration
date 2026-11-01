package com.minecoloniesirons.colony.jobs;

import com.minecolonies.api.colony.jobs.registry.JobEntry;
import com.minecolonies.apiimp.CommonMinecoloniesAPIImpl;
import com.minecolonies.core.colony.jobs.views.DefaultJobView;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModIronsJobs {

    public static final String MODID = "minecolonies_irons";

    public static final ResourceLocation MAGE_ID = ResourceLocation.fromNamespaceAndPath(MODID, "mage");
    public static final ResourceLocation GUNNER_ID = ResourceLocation.fromNamespaceAndPath(MODID, "gunner");

    public static final DeferredRegister<JobEntry> DEFERRED_REGISTER =
            DeferredRegister.create(CommonMinecoloniesAPIImpl.JOBS, MODID);

    public static final DeferredHolder<JobEntry, JobEntry> MAGE = DEFERRED_REGISTER.register("mage", () ->
            new JobEntry.Builder()
                    .setRegistryName(MAGE_ID)
                    .setJobProducer(JobMage::new)
                    .setJobViewProducer(() -> DefaultJobView::new)
                    .createJobEntry()
    );

    public static final DeferredHolder<JobEntry, JobEntry> GUNNER = DEFERRED_REGISTER.register("gunner", () ->
            new JobEntry.Builder()
                    .setRegistryName(GUNNER_ID)
                    .setJobProducer(JobGunner::new)
                    .setJobViewProducer(() -> DefaultJobView::new)
                    .createJobEntry()
    );

    private ModIronsJobs() {}
}

