package com.minecoloniesirons.colony.ai;

import com.minecolonies.api.entity.citizen.AbstractEntityCitizen;
import com.minecolonies.core.colony.buildings.AbstractBuildingGuards;
import com.minecolonies.core.entity.ai.workers.guard.AbstractEntityAIGuard;
import com.minecolonies.core.entity.citizen.EntityCitizen;
import com.minecoloniesirons.colony.jobs.JobMage;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;

public class EntityAIMage extends AbstractEntityAIGuard<JobMage, AbstractBuildingGuards> {

    public EntityAIMage(JobMage job) {
        super(job);
        if (this.worker instanceof EntityCitizen entityCitizen) {
            new MageCombatAI(entityCitizen, this.getStateAI(), this);
        }
    }

    @Override
    public void guardMovement() {
        if (this.worker == null) return;
        RandomSource random = this.worker.getRandom();

        if (random.nextInt(30) < 1) {
            BlockPos guardPos = this.buildingGuards.getGuardPos(this.worker);
            this.walkToUnSafePos(guardPos, 5);
            return;
        }

        if (!this.worker.getNavigation().isDone()) {
            return;
        }

        BlockPos guardPos = this.buildingGuards.getGuardPos(this.worker);
        this.walkToSafePos(guardPos);
    }
}
