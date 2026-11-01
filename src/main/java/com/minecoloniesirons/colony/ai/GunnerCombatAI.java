package com.minecoloniesirons.colony.ai;

import com.minecolonies.api.colony.IColony;
import com.minecolonies.api.entity.ai.statemachine.tickratestatemachine.ITickRateStateMachine;
import com.minecolonies.api.research.IResearchEffectManager;
import com.minecolonies.core.entity.ai.workers.guard.AbstractEntityAIGuard;
import com.minecolonies.core.entity.ai.workers.guard.RangeCombatAI;
import com.minecolonies.core.entity.citizen.EntityCitizen;
import com.minecolonies.core.util.citizenutils.CitizenItemUtils;
import com.minecoloniesirons.colony.equipment.ModIronsEquipmentTypes;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

/**
 * Gunner Combat AI: Maintains tactical ranged distance (10-18 blocks),
 * equips and checks for Iron's Artifice firearms (Tier 1: Pistols/Revolvers, Tier 2: Muskets/Blunderbusses, Tier 3: Arquebus/Clockwork),
 * checks colony research unlock tiers, and applies Target Practice damage multipliers.
 */
public class GunnerCombatAI extends RangeCombatAI {

    public static final ResourceLocation EFFECT_GUNNER = ResourceLocation.fromNamespaceAndPath("minecolonies", "effects/gunner");
    public static final ResourceLocation EFFECT_GUNNER_ARMORY_2 = ResourceLocation.fromNamespaceAndPath("minecolonies", "effects/gunner_armory_2");
    public static final ResourceLocation EFFECT_GUNNER_ARMORY_3 = ResourceLocation.fromNamespaceAndPath("minecolonies", "effects/gunner_armory_3");
    public static final ResourceLocation EFFECT_GUNNER_DAMAGE_1 = ResourceLocation.fromNamespaceAndPath("minecolonies", "effects/gunner_damage_1");
    public static final ResourceLocation EFFECT_GUNNER_DAMAGE_2 = ResourceLocation.fromNamespaceAndPath("minecolonies", "effects/gunner_damage_2");
    public static final ResourceLocation EFFECT_GUNNER_DAMAGE_3 = ResourceLocation.fromNamespaceAndPath("minecolonies", "effects/gunner_damage_3");

    private int shotsFiredInVolley = 0;
    private boolean isReloading = false;

    public GunnerCombatAI(EntityCitizen citizen, ITickRateStateMachine stateMachine, AbstractEntityAIGuard parentAI) {
        super(citizen, stateMachine, parentAI);
    }

    @Override
    public boolean canAttack() {
        if (this.user == null || !this.user.isAlive()) {
            return false;
        }

        // Auto-equip firearm according to unlocked research tier
        equipFirearmGear();

        return true;
    }

    private void equipFirearmGear() {
        if (this.user == null) return;

        int maxAllowedTier = getMaxAllowedTier();

        ItemStack main = this.user.getMainHandItem();
        if (!main.isEmpty() && ModIronsEquipmentTypes.isGunItem(main)) {
            int currentTier = ModIronsEquipmentTypes.getGunLevel(main);
            if (currentTier <= maxAllowedTier) {
                return; // Already holding a valid, permitted gun
            }
        }

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
            CitizenItemUtils.setHeldItem(this.user, InteractionHand.MAIN_HAND, bestSlot);
        }
    }

    private int getMaxAllowedTier() {
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
        return mult;
    }

    @Override
    protected double getAttackDistance() {
        return 18.0D;
    }

    @Override
    protected int getAttackDelay() {
        if (this.isReloading) {
            this.isReloading = false;
            return 50; // 2.5s reload delay after full volley
        }

        ItemStack weapon = this.user.getMainHandItem();
        String weaponName = weapon.getItem().toString().toLowerCase();
        if (weaponName.contains("clockwork") || weaponName.contains("revolver") || weaponName.contains("six")) {
            return 15;
        } else if (weaponName.contains("blunderbuss")) {
            return 25;
        }
        return 30; // Standard musket cadence
    }

    @Override
    protected void doAttack(LivingEntity target) {
        if (target == null || !target.isAlive() || this.user == null) {
            return;
        }

        // Face the target
        this.user.getLookControl().setLookAt(target, 40.0F, 40.0F);

        ItemStack weapon = this.user.getMainHandItem();
        String weaponName = weapon.getItem().toString().toLowerCase();

        boolean isBlunderbuss = weaponName.contains("blunderbuss");
        boolean isSniper = weaponName.contains("arquebus") || weaponName.contains("rifle");

        // Swing arm to simulate gun recoil
        this.user.swing(InteractionHand.MAIN_HAND);

        if (this.user.level() instanceof ServerLevel serverLevel) {
            Vec3 citizenEye = this.user.getEyePosition();
            Vec3 targetEye = target.getEyePosition();
            Vec3 direction = targetEye.subtract(citizenEye).normalize();

            // Gunshot sound
            serverLevel.playSound(null, this.user.blockPosition(), SoundEvents.GENERIC_EXPLODE.value(), SoundSource.NEUTRAL, 0.6F, 1.8F);

            // Muzzle flash particle at gun barrel
            serverLevel.sendParticles(
                    ParticleTypes.CAMPFIRE_COSY_SMOKE,
                    citizenEye.x + direction.x * 0.9D,
                    citizenEye.y + direction.y * 0.9D,
                    citizenEye.z + direction.z * 0.9D,
                    6, 0.05D, 0.05D, 0.05D, 0.02D
            );
            serverLevel.sendParticles(
                    ParticleTypes.FLAME,
                    citizenEye.x + direction.x * 0.9D,
                    citizenEye.y + direction.y * 0.9D,
                    citizenEye.z + direction.z * 0.9D,
                    3, 0.02D, 0.02D, 0.02D, 0.01D
            );

            // Calculate base damage depending on weapon tier
            float baseDamage = 8.0F;
            if (isBlunderbuss) {
                baseDamage = 11.0F;
                serverLevel.sendParticles(ParticleTypes.CRIT, targetEye.x, targetEye.y, targetEye.z, 15, 0.6D, 0.6D, 0.6D, 0.1D);
            } else if (isSniper) {
                baseDamage = 15.0F;
                serverLevel.sendParticles(ParticleTypes.CRIT, targetEye.x, targetEye.y, targetEye.z, 8, 0.2D, 0.2D, 0.2D, 0.2D);
            } else {
                serverLevel.sendParticles(ParticleTypes.CRIT, targetEye.x, targetEye.y, targetEye.z, 6, 0.3D, 0.3D, 0.3D, 0.1D);
            }

            // Apply Target Practice research damage multiplier
            float totalDamage = baseDamage * getDamageMultiplier();

            // Apply bullet damage
            target.hurt(this.user.damageSources().mobAttack(this.user), totalDamage);

            // Push target back (bullet impact knockback)
            target.knockback(0.35D, -direction.x, -direction.z);
        }

        this.shotsFiredInVolley++;
        if (this.shotsFiredInVolley >= 4) {
            // Full reload cycle triggered for next shot
            this.isReloading = true;
            this.shotsFiredInVolley = 0;
            if (this.user.level() instanceof ServerLevel sl) {
                sl.playSound(null, this.user.blockPosition(), SoundEvents.IRON_GOLEM_REPAIR, SoundSource.NEUTRAL, 0.5F, 1.4F);
            }
        }
    }
}
