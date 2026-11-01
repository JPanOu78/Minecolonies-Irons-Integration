package com.minecoloniesirons.colony.ai;

import com.minecolonies.api.colony.IColony;
import com.minecolonies.api.entity.ai.statemachine.tickratestatemachine.ITickRateStateMachine;
import com.minecolonies.api.equipment.registry.EquipmentTypeEntry;
import com.minecolonies.api.research.IResearchEffectManager;
import com.minecolonies.core.entity.ai.workers.guard.AbstractEntityAIGuard;
import com.minecolonies.core.entity.ai.workers.guard.RangeCombatAI;
import com.minecolonies.core.entity.citizen.EntityCitizen;
import com.minecolonies.core.util.citizenutils.CitizenItemUtils;
import com.minecoloniesirons.colony.equipment.ModIronsEquipmentTypes;
import io.redspace.irons_artifice.item.FireOutcome;
import io.redspace.irons_artifice.item.GunItem;
import io.redspace.irons_artifice.item.GunplayManager;
import io.redspace.irons_artifice.item.MagazineContents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

/**
 * Gunner Combat AI: Maintains tactical ranged distance (10-18 blocks),
 * equips and checks for Iron's Artifice firearms across Tier 1 to 3 according to research unlocks,
 * requires and consumes ammunition (bullets),
 * fires native Iron's Artifice bullets with true physical projectile trajectories, sounds, and animations,
 * applies Recoil Compensation and Tactical Speedloader from colony research,
 * completely free of environmental explosions and fire damage to preserve colony architecture.
 */
public class GunnerCombatAI extends RangeCombatAI {

    public static final ResourceLocation EFFECT_GUNNER = ResourceLocation.fromNamespaceAndPath("minecolonies", "effects/gunner");
    public static final ResourceLocation EFFECT_GUNNER_ARMORY_2 = ResourceLocation.fromNamespaceAndPath("minecolonies", "effects/gunner_armory_2");
    public static final ResourceLocation EFFECT_GUNNER_ARMORY_3 = ResourceLocation.fromNamespaceAndPath("minecolonies", "effects/gunner_armory_3");
    public static final ResourceLocation EFFECT_GUNNER_ARMORY_4 = ResourceLocation.fromNamespaceAndPath("minecolonies", "effects/gunner_armory_4");
    public static final ResourceLocation EFFECT_GUNNER_ARMORY_5 = ResourceLocation.fromNamespaceAndPath("minecolonies", "effects/gunner_armory_5");
    public static final ResourceLocation EFFECT_GUNNER_DAMAGE_1 = ResourceLocation.fromNamespaceAndPath("minecolonies", "effects/gunner_damage_1");
    public static final ResourceLocation EFFECT_GUNNER_DAMAGE_2 = ResourceLocation.fromNamespaceAndPath("minecolonies", "effects/gunner_damage_2");
    public static final ResourceLocation EFFECT_GUNNER_DAMAGE_3 = ResourceLocation.fromNamespaceAndPath("minecolonies", "effects/gunner_damage_3");
    public static final ResourceLocation EFFECT_GUNNER_DAMAGE_4 = ResourceLocation.fromNamespaceAndPath("minecolonies", "effects/gunner_damage_4");

    private int shotsFiredInVolley = 0;
    private boolean isReloading = false;

    public GunnerCombatAI(EntityCitizen citizen, ITickRateStateMachine stateMachine, AbstractEntityAIGuard parentAI) {
        super(citizen, stateMachine, parentAI);
    }

    @Override
    public EquipmentTypeEntry getWeaponType() {
        return ModIronsEquipmentTypes.GUN.get();
    }

    @Override
    public boolean canAttack() {
        if (this.user == null || !this.user.isAlive()) {
            return false;
        }

        // Auto-equip highest permitted firearm from inventory
        equipFirearmGear();

        ItemStack main = this.user.getMainHandItem();
        if (main.isEmpty() || !ModIronsEquipmentTypes.isGunItem(main)) {
            return false;
        }

        // Must have bullets in magazine or inventory
        int magCount = MagazineContents.get(main).count();
        return magCount > 0 || hasAmmo();
    }

    /**
     * Checks if citizen currently carries firearm ammunition in their inventory.
     */
    public boolean hasAmmo() {
        if (this.user == null) return false;
        var inv = this.user.getInventoryCitizen();
        for (int i = 0; i < inv.getSlots(); i++) {
            ItemStack stack = inv.getStackInSlot(i);
            if (!stack.isEmpty() && ModIronsEquipmentTypes.isAmmoItem(stack)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Consumes 1 bullet from citizen inventory when loaded into a firearm.
     */
    public boolean consumeBullet() {
        if (this.user == null) return false;
        var inv = this.user.getInventoryCitizen();
        for (int i = 0; i < inv.getSlots(); i++) {
            ItemStack stack = inv.getStackInSlot(i);
            if (!stack.isEmpty() && ModIronsEquipmentTypes.isAmmoItem(stack)) {
                stack.shrink(1);
                return true;
            }
        }
        return false;
    }

    /**
     * Scans inventory for the best available firearm matching unlocked research tier,
     * and equips it directly into MAIN_HAND, dynamically re-rendering the held weapon.
     */
    public void equipFirearmGear() {
        if (this.user == null) return;

        int maxAllowedTier = getMaxAllowedTier();
        var inv = this.user.getInventoryCitizen();

        int bestSlot = -1;
        int bestTier = 0;

        for (int i = 0; i < inv.getSlots(); i++) {
            ItemStack stack = inv.getStackInSlot(i);
            if (stack.isEmpty() || !ModIronsEquipmentTypes.isGunItem(stack)) continue;

            int tier = ModIronsEquipmentTypes.getGunLevel(stack);
            if (tier <= maxAllowedTier && tier > bestTier) {
                bestTier = tier;
                bestSlot = i;
            }
        }

        if (bestSlot != -1) {
            ItemStack bestStack = inv.getStackInSlot(bestSlot);
            ItemStack currentHeld = this.user.getMainHandItem();
            if (currentHeld != bestStack && (currentHeld.isEmpty() || !ItemStack.isSameItemSameComponents(currentHeld, bestStack))) {
                CitizenItemUtils.setHeldItem(this.user, InteractionHand.MAIN_HAND, bestSlot);
                this.user.setItemSlot(EquipmentSlot.MAINHAND, bestStack);
            }
        }
    }

    public int getMaxAllowedTier() {
        if (this.user == null) return 1;
        IColony colony = this.user.getCitizenColonyHandler().getColonyOrRegister();
        if (colony == null) return 1;

        IResearchEffectManager rem = colony.getResearchManager().getResearchEffects();
        if (rem.getEffectStrength(EFFECT_GUNNER_ARMORY_3) > 0) return 3;
        if (rem.getEffectStrength(EFFECT_GUNNER_ARMORY_2) > 0) return 2;
        return 1;
    }

    private float getDamageMultiplier() {
        float mult = 1.0F;
        if (this.user == null) return mult;
        IColony colony = this.user.getCitizenColonyHandler().getColonyOrRegister();
        if (colony == null) return mult;

        IResearchEffectManager rem = colony.getResearchManager().getResearchEffects();
        if (rem.getEffectStrength(EFFECT_GUNNER_DAMAGE_1) > 0) mult += 0.10F;
        if (rem.getEffectStrength(EFFECT_GUNNER_DAMAGE_2) > 0) mult += 0.10F;
        if (rem.getEffectStrength(EFFECT_GUNNER_DAMAGE_3) > 0) mult += 0.10F;
        if (rem.getEffectStrength(EFFECT_GUNNER_DAMAGE_4) > 0) mult += 0.10F;
        return mult;
    }

    private boolean hasRecoilCompensation() {
        if (this.user == null) return false;
        IColony colony = this.user.getCitizenColonyHandler().getColonyOrRegister();
        if (colony == null) return false;
        return colony.getResearchManager().getResearchEffects().getEffectStrength(EFFECT_GUNNER_ARMORY_4) > 0;
    }

    private boolean hasSpeedloader() {
        if (this.user == null) return false;
        IColony colony = this.user.getCitizenColonyHandler().getColonyOrRegister();
        if (colony == null) return false;
        return colony.getResearchManager().getResearchEffects().getEffectStrength(EFFECT_GUNNER_ARMORY_5) > 0;
    }

    @Override
    protected double getAttackDistance() {
        return 18.0D;
    }

    @Override
    protected int getAttackDelay() {
        if (this.isReloading) {
            this.isReloading = false;

            int reloadDelay = 50; // Base reload delay (2.5 seconds)

            // LV 5 Tactical Speedloader halves reload time
            if (hasSpeedloader()) {
                reloadDelay = 25;
            }

            if (this.user != null) {
                IColony colony = this.user.getCitizenColonyHandler().getColonyOrRegister();
                if (colony != null && colony.getResearchManager().getResearchEffects().getEffectStrength(EFFECT_GUNNER_DAMAGE_4) > 0) {
                    reloadDelay = (int) (reloadDelay * 0.8F);
                }
            }

            return Math.max(15, reloadDelay);
        }

        ItemStack weapon = this.user.getMainHandItem();
        String weaponName = weapon.getItem().toString().toLowerCase();
        int cadence = 30; // Standard musket cadence
        if (weaponName.contains("clockwork") || weaponName.contains("revolver") || weaponName.contains("six")) {
            cadence = 15;
        } else if (weaponName.contains("blunderbuss")) {
            cadence = 25;
        }

        // LV 4 Recoil Compensation reduces cadence delay between shots by 25%
        if (hasRecoilCompensation()) {
            cadence = (int) (cadence * 0.75F);
        }

        return Math.max(8, cadence);
    }

    @Override
    protected void doAttack(LivingEntity target) {
        if (target == null || !target.isAlive() || this.user == null) {
            return;
        }

        // Re-check and equip firearm
        equipFirearmGear();

        ItemStack weapon = this.user.getMainHandItem();
        if (weapon.isEmpty() || !ModIronsEquipmentTypes.isGunItem(weapon)) {
            return;
        }

        // Align facing directly at target eye position
        this.user.getLookControl().setLookAt(target, 45.0F, 45.0F);
        Vec3 diff = target.getEyePosition().subtract(this.user.getEyePosition());
        double dx = diff.x;
        double dy = diff.y;
        double dz = diff.z;
        double horizDist = Math.sqrt(dx * dx + dz * dz);
        float yaw = (float) (Mth.atan2(dz, dx) * (180.0D / Math.PI)) - 90.0F;
        float pitch = (float) (-(Mth.atan2(dy, horizDist) * (180.0D / Math.PI)));
        this.user.setYRot(yaw);
        this.user.setYHeadRot(yaw);
        this.user.setYBodyRot(yaw);
        this.user.setXRot(pitch);

        // Check gun magazine: replenish from citizen inventory if needed
        int currentMag = MagazineContents.get(weapon).count();
        if (currentMag <= 0) {
            if (!hasAmmo()) {
                return; // Out of ammunition!
            }
            int capacity = (weapon.getItem() instanceof GunItem gunItem) ? gunItem.magazineCapacity() : 1;
            int loaded = 0;
            while (loaded < capacity && consumeBullet()) {
                loaded++;
            }
            if (loaded > 0) {
                MagazineContents.set(weapon, new MagazineContents(loaded));
                if (this.user.level() instanceof ServerLevel sl) {
                    sl.playSound(null, this.user.blockPosition(), SoundEvents.IRON_GOLEM_REPAIR, SoundSource.NEUTRAL, 0.5F, 1.4F);
                }
            } else {
                return;
            }
        }

        // Swing arm to trigger player-like visual gunshot animation
        this.user.swing(InteractionHand.MAIN_HAND);

        // Native Iron's Artifice gunshot: spawns Bullet projectile entity with sound, muzzle flash & recoil
        Vec3 targetEye = target.getEyePosition();
        FireOutcome outcome = GunplayManager.tryFire(this.user, targetEye);

        if (!outcome.fired()) {
            if (outcome == FireOutcome.EMPTY_MAGAZINE && consumeBullet()) {
                MagazineContents.set(weapon, new MagazineContents(1));
                GunplayManager.tryFire(this.user, targetEye);
            }
        }

        this.shotsFiredInVolley++;
        int volleyCapacity = hasSpeedloader() ? 6 : 4;
        if (this.shotsFiredInVolley >= volleyCapacity) {
            this.isReloading = true;
            this.shotsFiredInVolley = 0;
        }
    }
}
