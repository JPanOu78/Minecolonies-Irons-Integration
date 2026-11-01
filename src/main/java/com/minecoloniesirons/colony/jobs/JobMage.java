package com.minecoloniesirons.colony.jobs;

import com.minecolonies.api.client.render.modeltype.ModModelTypes;
import com.minecolonies.api.colony.ICitizenData;
import com.minecolonies.core.colony.buildings.AbstractBuildingGuards;
import com.minecolonies.core.colony.jobs.AbstractJobGuard;
import com.minecolonies.core.entity.ai.workers.guard.AbstractEntityAIGuard;
import com.minecoloniesirons.colony.ai.EntityAIMage;
import net.minecraft.resources.ResourceLocation;

public class JobMage extends AbstractJobGuard<JobMage> {

    public JobMage(ICitizenData citizenData) {
        super(citizenData);
    }

    @Override
    protected AbstractEntityAIGuard<JobMage, ? extends AbstractBuildingGuards> generateGuardAI() {
        return new EntityAIMage(this);
    }

    @Override
    public ResourceLocation getModel() {
        // Cleric / Healer uniform for standalone Minecolonies without MCA
        return ModModelTypes.HEALER_ID;
    }

    public com.minecolonies.api.equipment.registry.EquipmentTypeEntry getEquipmentType() {
        return ModIronsEquipmentTypes.SPELL_FOCUS.get();
    }
}
