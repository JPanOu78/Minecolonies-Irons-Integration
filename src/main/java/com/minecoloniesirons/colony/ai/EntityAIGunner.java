package com.minecoloniesirons.colony.ai;

import com.minecolonies.api.entity.citizen.AbstractEntityCitizen;
import com.minecolonies.core.colony.buildings.AbstractBuildingGuards;
import com.minecolonies.core.entity.ai.workers.guard.AbstractEntityAIGuard;
import com.minecolonies.core.entity.citizen.EntityCitizen;
import com.minecoloniesirons.colony.equipment.ModIronsEquipmentTypes;
import com.minecoloniesirons.colony.jobs.JobGunner;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;

public class EntityAIGunner extends AbstractEntityAIGuard<JobGunner, AbstractBuildingGuards> {

    private GunnerCombatAI combatAI;

    public EntityAIGunner(JobGunner job) {
        super(job);

        // Require firearm tool for this job (triggers requests in colony when missing)
        this.toolsNeeded.add(ModIronsEquipmentTypes.GUN.get());

        if (this.worker instanceof EntityCitizen entityCitizen) {
            this.combatAI = new GunnerCombatAI(entityCitizen, this.getStateAI(), this);
        }
    }

    @Override
    public void guardMovement() {
        if (this.worker == null) return;

        // Ensure firearm is visibly held in main hand during guard patrol
        if (this.combatAI != null) {
            this.combatAI.equipFirearmGear();
        }
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
