package com.minecoloniesirons.colony.jobs;

import com.minecolonies.api.colony.ICitizenData;
import com.minecolonies.core.colony.buildings.AbstractBuildingGuards;
import com.minecolonies.core.colony.jobs.AbstractJobGuard;
import com.minecolonies.core.entity.ai.workers.guard.AbstractEntityAIGuard;
import com.minecoloniesirons.colony.ai.EntityAIGunner;

public class JobGunner extends AbstractJobGuard<JobGunner> {

    public JobGunner(ICitizenData citizenData) {
        super(citizenData);
    }

    @Override
    protected AbstractEntityAIGuard<JobGunner, ? extends AbstractBuildingGuards> generateGuardAI() {
        return new EntityAIGunner(this);
    }
}
