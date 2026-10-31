package com.minecoloniesirons.colony.jobs;

import com.minecolonies.api.colony.ICitizenData;
import com.minecolonies.core.colony.buildings.AbstractBuildingGuards;
import com.minecolonies.core.colony.jobs.AbstractJobGuard;
import com.minecolonies.core.entity.ai.workers.guard.AbstractEntityAIGuard;
import com.minecoloniesirons.colony.ai.EntityAIMage;

public class JobMage extends AbstractJobGuard<JobMage> {

    public JobMage(ICitizenData citizenData) {
        super(citizenData);
    }

    @Override
    protected AbstractEntityAIGuard<JobMage, ? extends AbstractBuildingGuards> generateGuardAI() {
        return new EntityAIMage(this);
    }
}
