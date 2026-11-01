package com.minecoloniesirons.colony.jobs;

import com.minecolonies.api.client.render.modeltype.ModModelTypes;
import com.minecolonies.api.colony.ICitizenData;
import com.minecolonies.core.colony.buildings.AbstractBuildingGuards;
import com.minecolonies.core.colony.jobs.AbstractJobGuard;
import com.minecolonies.core.entity.ai.workers.guard.AbstractEntityAIGuard;
import com.minecoloniesirons.colony.ai.EntityAIGunner;
import net.minecraft.resources.ResourceLocation;

public class JobGunner extends AbstractJobGuard<JobGunner> {

    public JobGunner(ICitizenData citizenData) {
        super(citizenData);
    }

    @Override
    protected AbstractEntityAIGuard<JobGunner, ? extends AbstractBuildingGuards> generateGuardAI() {
        return new EntityAIGunner(this);
    }

    @Override
    public ResourceLocation getModel() {
        // Guard uniform for standalone Minecolonies without MCA
        return ModModelTypes.ARCHER_GUARD_ID;
    }
}
