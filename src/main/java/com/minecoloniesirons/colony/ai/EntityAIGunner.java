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
import com.minecoloniesirons.colony.jobs.JobGunner;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;

import java.util.function.Predicate;

public class EntityAIGunner extends AbstractEntityAIGuard<JobGunner, AbstractBuildingGuards> {

    private GunnerCombatAI combatAI;

    public EntityAIGunner(JobGunner job) {
        super(job);

        // Register required equipment type
        this.toolsNeeded.add(ModIronsEquipmentTypes.GUN.get());

        if (this.worker instanceof EntityCitizen entityCitizen) {
            this.combatAI = new GunnerCombatAI(entityCitizen, this.getStateAI(), this);
        }
    }

    /**
     * Determines maximum allowed gun tier (1 to 3) based on colony's unlocked research.
     */
    public int getMaxAllowedTier() {
        if (this.worker == null) return 1;
        IColony colony = this.worker.getCitizenColonyHandler().getColonyOrRegister();
        if (colony == null) return 1;

        IResearchEffectManager rem = colony.getResearchManager().getResearchEffects();
        if (rem.getEffectStrength(GunnerCombatAI.EFFECT_GUNNER_ARMORY_3) > 0) return 3;
        if (rem.getEffectStrength(GunnerCombatAI.EFFECT_GUNNER_ARMORY_2) > 0) return 2;
        return 1;
    }

    @Override
    public boolean hasTool() {
        if (this.worker == null) return false;
        int maxTier = getMaxAllowedTier();
        return InventoryUtils.hasItemHandlerEquipmentWithLevel(
                this.worker.getInventoryCitizen(), ModIronsEquipmentTypes.GUN.get(), 1, maxTier
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

        // 1. Check if citizen already carries an allowed gun
        if (InventoryUtils.hasItemHandlerEquipmentWithLevel(this.worker.getInventoryCitizen(), tool, effectiveMin, maxTier)) {
            this.worker.getCitizenData().setJobStatus(JobStatus.WORKING);
            return false;
        }

        // 2. Try to retrieve a valid firearm from building chest
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
            // Request firearm restricted strictly to unlocked research tier (maxTier) with clear example item
            final Tool requestable = new Tool(tool, effectiveMin, maxTier, ModIronsEquipmentTypes.getGunRequestStack());
            this.worker.getCitizenData().createRequest(requestable);
        }

        this.worker.getCitizenData().setJobStatus(JobStatus.STUCK);
        return true;
    }

    @Override
    protected void atBuildingActions() {
        super.atBuildingActions();

        if (this.combatAI != null) {
            this.combatAI.equipFirearmGear();
        }

        if (this.worker == null || this.building == null) return;

        Predicate<ItemStack> isAmmo = ModIronsEquipmentTypes::isAmmoItem;

        // Transfer bullets from building storage if available
        InventoryUtils.transferXOfFirstSlotInProviderWithIntoNextFreeSlotInItemHandler(
                this.building, isAmmo, 64, this.worker.getInventoryCitizen()
        );

        // If citizen has fewer than 16 bullets, create a request for 64 bullets
        if (InventoryUtils.getItemCountInItemHandler(this.worker.getInventoryCitizen(), isAmmo) < 16) {
            ItemStack bulletStack = ModIronsEquipmentTypes.getBulletRequestStack();
            if (!bulletStack.isEmpty()) {
                checkIfRequestForItemExistOrCreateAsync(bulletStack, 64, 16);
            }
        }
    }

    public boolean hasAmmoInInventory() {
        if (this.worker == null) return false;
        return InventoryUtils.getItemCountInItemHandler(this.worker.getInventoryCitizen(), ModIronsEquipmentTypes::isAmmoItem) > 0;
    }

    @Override
    protected void updateRenderMetaData() {
        String s = this.getState() == AIWorkerState.IDLE ? "" : "working";
        if (hasAmmoInInventory()) {
            s += " bullet";
        }
        this.worker.setRenderMetadata(s);
        if (this.combatAI != null) {
            this.combatAI.equipFirearmGear();
        }
    }

    @Override
    public void cleanVisibleSlots() {
        super.cleanVisibleSlots();
        if (this.combatAI != null) {
            this.combatAI.equipFirearmGear();
        }
    }

    @Override
    public void guardMovement() {
        if (this.worker == null) return;

        // Ensure firearm is visibly held in main hand
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
