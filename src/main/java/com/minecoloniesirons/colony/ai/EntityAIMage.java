package com.minecoloniesirons.colony.ai;

import com.google.common.collect.ImmutableList;
import com.google.common.reflect.TypeToken;
import com.minecolonies.api.colony.IColony;
import com.minecolonies.api.colony.requestsystem.request.IRequest;
import com.minecolonies.api.colony.requestsystem.requestable.Tool;
import com.minecolonies.api.entity.ai.JobStatus;
import com.minecolonies.api.entity.ai.statemachine.states.AIWorkerState;
import com.minecolonies.api.equipment.registry.EquipmentTypeEntry;
import com.minecolonies.api.research.IResearchEffectManager;
import com.minecolonies.api.util.BlockPosUtil;
import com.minecolonies.api.util.InventoryUtils;
import com.minecolonies.api.util.ItemStackUtils;
import com.minecolonies.core.colony.buildings.AbstractBuildingGuards;
import com.minecolonies.core.entity.ai.workers.guard.AbstractEntityAIGuard;
import com.minecolonies.core.entity.citizen.EntityCitizen;
import com.minecolonies.core.entity.pathfinding.navigation.MinecoloniesAdvancedPathNavigate;
import com.minecolonies.core.entity.pathfinding.pathjobs.PathJobWalkRandomEdge;
import com.minecoloniesirons.colony.equipment.ModIronsEquipmentTypes;
import com.minecoloniesirons.colony.jobs.JobMage;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;

public class EntityAIMage extends AbstractEntityAIGuard<JobMage, AbstractBuildingGuards> {

    private MageCombatAI combatAI;

    public EntityAIMage(JobMage job) {
        super(job);

        // Register required equipment type
        this.toolsNeeded.add(ModIronsEquipmentTypes.SPELL_FOCUS.get());

        if (this.worker instanceof EntityCitizen entityCitizen) {
            this.combatAI = new MageCombatAI(entityCitizen, this.getStateAI(), this);
        }
    }

    /**
     * Determines maximum allowed spell focus tier (1 to 5) based on colony's unlocked research.
     */
    public int getMaxAllowedTier() {
        if (this.worker == null) return 1;
        IColony colony = this.worker.getCitizenColonyHandler().getColonyOrRegister();
        if (colony == null) return 1;

        IResearchEffectManager rem = colony.getResearchManager().getResearchEffects();
        if (rem.getEffectStrength(MageCombatAI.EFFECT_MAGE_ARMORY_5) > 0) return 5;
        if (rem.getEffectStrength(MageCombatAI.EFFECT_MAGE_ARMORY_4) > 0) return 4;
        if (rem.getEffectStrength(MageCombatAI.EFFECT_MAGE_ARMORY_3) > 0) return 3;
        if (rem.getEffectStrength(MageCombatAI.EFFECT_MAGE_ARMORY_2) > 0) return 2;
        return 1;
    }

    @Override
    public boolean hasTool() {
        if (this.worker == null) return false;
        int maxTier = getMaxAllowedTier();
        return InventoryUtils.hasItemHandlerEquipmentWithLevel(
                this.worker.getInventoryCitizen(), ModIronsEquipmentTypes.SPELL_FOCUS.get(), 1, maxTier
        );
    }

    @Override
    public boolean checkForToolOrWeapon(final EquipmentTypeEntry tool) {
        return checkForToolOrWeapon(tool, 1);
    }

    @Override
    protected boolean checkForToolOrWeapon(final EquipmentTypeEntry tool, final int minLevel) {
        if (this.worker == null || this.building == null) return false;

        int maxTier = getMaxAllowedTier();
        int effectiveMin = Math.max(1, minLevel);

        // 1. Check if citizen already carries an allowed spell focus
        if (InventoryUtils.hasItemHandlerEquipmentWithLevel(this.worker.getInventoryCitizen(), tool, effectiveMin, maxTier)) {
            this.worker.getCitizenData().setJobStatus(JobStatus.WORKING);
            return false;
        }

        // 2. Try to retrieve a valid spell focus from building chest
        if (this.walkToBuilding()) {
            if (InventoryUtils.transferXOfFirstSlotInProviderWithIntoNextFreeSlotInItemHandler(
                    this.building, stack -> ItemStackUtils.hasEquipmentLevel(stack, tool, effectiveMin, maxTier), 1, this.worker.getInventoryCitizen())) {
                this.worker.getCitizenData().setJobStatus(JobStatus.WORKING);
                return false;
            }
        }

        // 3. Check if open requests already exist
        final var requests =
                this.building.getOpenRequestsOfTypeFiltered(this.worker.getCitizenData(), TypeToken.of(Tool.class),
                        r -> r.getRequest().getEquipmentType().equals(tool));
        final var completedRequests =
                this.building.getCompletedRequestsOfTypeFiltered(this.worker.getCitizenData(), TypeToken.of(Tool.class),
                        r -> r.getRequest().getEquipmentType().equals(tool));

        if (requests.isEmpty() && completedRequests.isEmpty()) {
            // Request spell focus restricted strictly to unlocked research tier (maxTier) with clear example item
            final Tool requestable = new Tool(tool, effectiveMin, maxTier, ModIronsEquipmentTypes.getSpellFocusRequestStack());
            this.worker.getCitizenData().createRequest(requestable);
        }

        this.worker.getCitizenData().setJobStatus(JobStatus.STUCK);
        return true;
    }

    @Override
    protected void updateRenderMetaData() {
        String s = this.getState() == AIWorkerState.IDLE ? "" : "working";
        this.worker.setRenderMetadata(s);
        if (this.combatAI != null) {
            this.combatAI.equipMageGear();
        }
    }

    @Override
    protected void atBuildingActions() {
        super.atBuildingActions();

        if (this.worker != null && this.building != null && getMaxAllowedTier() >= 2) {
            // Retrieve robes for empty or upgradeable armor slots from tower chest
            for (EquipmentSlot slot : new EquipmentSlot[]{EquipmentSlot.CHEST, EquipmentSlot.HEAD, EquipmentSlot.LEGS, EquipmentSlot.FEET}) {
                ItemStack current = this.worker.getItemBySlot(slot);
                int currentTier = ModIronsEquipmentTypes.isRobeItemForSlot(current, slot) ? ModIronsEquipmentTypes.getRobeTier(current) : 0;
                int maxTier = getMaxAllowedTier();
                if (currentTier < maxTier) {
                    InventoryUtils.transferXOfFirstSlotInProviderWithIntoNextFreeSlotInItemHandler(
                            this.building,
                            stack -> ModIronsEquipmentTypes.isRobeItemForSlot(stack, slot)
                                    && ModIronsEquipmentTypes.getRobeTier(stack) <= maxTier
                                    && ModIronsEquipmentTypes.getRobeTier(stack) > currentTier,
                            1,
                            this.worker.getInventoryCitizen()
                    );
                }
            }
        }

        if (this.combatAI != null) {
            this.combatAI.equipMageGear();
        }
    }

    @Override
    public void equipInventoryArmor() {
        super.equipInventoryArmor();
        if (this.combatAI != null) {
            this.combatAI.equipMageRobes();
        }
    }

    @Override
    public void cleanVisibleSlots() {
        super.cleanVisibleSlots();
        if (this.combatAI != null) {
            this.combatAI.equipMageGear();
        }
    }

    @Override
    public void guardMovement() {
        if (this.worker == null) return;

        // Ensure staff is held in main hand and spellbook in off hand
        if (this.combatAI != null) {
            this.combatAI.equipMageGear();
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
        if (BlockPosUtil.dist(guardPos, this.worker.blockPosition()) > 10.0D) {
            this.walkToSafePos(guardPos);
        } else {
            // Patrol along edges/ledges of the guard tower rather than freezing in one spot
            ((MinecoloniesAdvancedPathNavigate) this.worker.getNavigation()).setPathJob(
                    new PathJobWalkRandomEdge(this.world, guardPos, 10, this.worker),
                    null, 1.0D, true
            );
        }
    }
}
