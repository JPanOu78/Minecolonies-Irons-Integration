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
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

/**
 * Gunner Combat AI: Maintains tactical ranged distance (10-18 blocks),
 * equips and checks for Iron's Artifice firearms across Tier 1 to 3 according to research unlocks,
 * requires and consumes ammunition (bullets),
 * applies Recoil Compensation (faster cadence & heavier knockback),
 * Tactical Speedloader (extended 6-round volleys & rapid reload),
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

        // Auto-equip firearm according to unlocked research tier
        equipFirearmGear();

        // Must have ammunition to engage in ranged combat
        return hasAmmo();
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
     * Consumes 1 bullet from citizen inventory when a shot is fired.
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

    public void equipFirearmGear() {
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
            this.user.setItemSlot(EquipmentSlot.MAINHAND, inv.getStackInSlot(bestSlot));
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
        if (rem.getEffectStrength(EFFECT_GUNNER_DAMAGE_4) > 0) mult += 0.10F; // LV 5: total +40%
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

            // LV 5 Tactical Speedloader halves reload time to 25 ticks
            if (hasSpeedloader()) {
                reloadDelay = 25;
            }

            // LV 5 Master Marksman reduces reload delay by an additional 20%
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

        // Must consume ammunition to fire
        if (!consumeBullet()) {
            return;
        }

        // Face the target
        this.user.getLookControl().setLookAt(target, 40.0F, 40.0F);

        ItemStack weapon = this.user.getMainHandItem();
        String weaponName = weapon.getItem().toString().toLowerCase();

        boolean isBlunderbuss = weaponName.contains("blunderbuss");
        boolean isSniper = weaponName.contains("arquebus") || weaponName.contains("rifle");
        boolean recoilReduced = hasRecoilCompensation();

        // Swing arm to simulate gun recoil
        this.user.swing(InteractionHand.MAIN_HAND);

        if (this.user.level() instanceof ServerLevel serverLevel) {
            Vec3 citizenEye = this.user.getEyePosition();
            Vec3 targetEye = target.getEyePosition();
            Vec3 direction = targetEye.subtract(citizenEye).normalize();

            // Gunshot sound
            serverLevel.playSound(null, this.user.blockPosition(), SoundEvents.GENERIC_EXPLODE.value(), SoundSource.NEUTRAL, 0.6F, 1.8F);

            // Muzzle smoke and flash
            Vec3 muzzlePos = citizenEye.add(direction.scale(0.85D));
            serverLevel.sendParticles(
                    ParticleTypes.CAMPFIRE_COSY_SMOKE,
                    muzzlePos.x, muzzlePos.y, muzzlePos.z,
                    6, 0.05D, 0.05D, 0.05D, 0.02D
            );
            serverLevel.sendParticles(
                    ParticleTypes.FLAME,
                    muzzlePos.x, muzzlePos.y, muzzlePos.z,
                    3, 0.02D, 0.02D, 0.02D, 0.01D
            );

            // Render visible bullet flight trajectory from muzzle to target
            renderBulletFlight(serverLevel, muzzlePos, targetEye, isBlunderbuss, isSniper);

            // Calculate base damage depending on weapon tier
            float baseDamage = 8.0F;
            if (isBlunderbuss) {
                baseDamage = 11.0F;
                serverLevel.sendParticles(ParticleTypes.CRIT, targetEye.x, targetEye.y, targetEye.z, 15, 0.5D, 0.5D, 0.5D, 0.15D);
            } else if (isSniper) {
                baseDamage = 15.0F;
                serverLevel.sendParticles(ParticleTypes.CRIT, targetEye.x, targetEye.y, targetEye.z, 10, 0.2D, 0.2D, 0.2D, 0.2D);
                serverLevel.sendParticles(ParticleTypes.ELECTRIC_SPARK, targetEye.x, targetEye.y, targetEye.z, 8, 0.2D, 0.2D, 0.2D, 0.1D);
            } else {
                serverLevel.sendParticles(ParticleTypes.CRIT, targetEye.x, targetEye.y, targetEye.z, 8, 0.25D, 0.25D, 0.25D, 0.1D);
            }
            serverLevel.sendParticles(ParticleTypes.POOF, targetEye.x, targetEye.y, targetEye.z, 3, 0.1D, 0.1D, 0.1D, 0.02D);

            // Apply Target Practice research damage multiplier
            float totalDamage = baseDamage * getDamageMultiplier();

            // Apply bullet damage directly to target (100% safe, no terrain or AoE damage)
            target.hurt(this.user.damageSources().mobAttack(this.user), totalDamage);

            // Knockback on target (Recoil Compensation increases stopping power)
            double knockbackPower = recoilReduced ? 0.55D : 0.35D;
            target.knockback(knockbackPower, -direction.x, -direction.z);
        }

        this.shotsFiredInVolley++;

        // Volley capacity: 6 rounds if Tactical Speedloader unlocked, else 4 rounds
        int volleyCapacity = hasSpeedloader() ? 6 : 4;

        if (this.shotsFiredInVolley >= volleyCapacity) {
            // Full reload cycle triggered for next shot
            this.isReloading = true;
            this.shotsFiredInVolley = 0;
            if (this.user.level() instanceof ServerLevel sl) {
                sl.playSound(null, this.user.blockPosition(), SoundEvents.IRON_GOLEM_REPAIR, SoundSource.NEUTRAL, 0.5F, 1.4F);
            }
        }
    }

    /**
     * Renders a visible, supersonic bullet flight trajectory from gun muzzle to target.
     * Supports scattershot pellet cone for Blunderbuss, piercing sparks for rifles, and smoke tracers.
     */
    public static void renderBulletFlight(ServerLevel level, Vec3 start, Vec3 end, boolean isBlunderbuss, boolean isSniper) {
        if (isBlunderbuss) {
            // Shotgun scattershot: 5 distinct pellet trajectories spreading outward
            RandomSource rnd = level.getRandom();
            for (int p = 0; p < 5; p++) {
                double spreadX = (rnd.nextDouble() - 0.5D) * 1.5D;
                double spreadY = (rnd.nextDouble() - 0.5D) * 1.5D;
                double spreadZ = (rnd.nextDouble() - 0.5D) * 1.5D;
                Vec3 spreadEnd = end.add(spreadX, spreadY, spreadZ);

                double dist = start.distanceTo(spreadEnd);
                int steps = Math.max(3, (int) (dist / 0.5D));
                for (int i = 1; i <= steps; i++) {
                    double progress = (double) i / steps;
                    Vec3 pos = start.lerp(spreadEnd, progress);
                    level.sendParticles(ParticleTypes.CRIT, pos.x, pos.y, pos.z, 1, 0.02D, 0.02D, 0.02D, 0.0D);
                    if (i % 3 == 0) {
                        level.sendParticles(ParticleTypes.SMOKE, pos.x, pos.y, pos.z, 1, 0.01D, 0.01D, 0.01D, 0.0D);
                    }
                }
            }
            return;
        }

        // Standard or Sniper supersonic tracer line
        double dist = start.distanceTo(end);
        int steps = Math.max(4, (int) (dist / 0.35D));
        for (int i = 1; i <= steps; i++) {
            double progress = (double) i / steps;
            Vec3 pos = start.lerp(end, progress);

            // Dense supersonic crit spark trail
            level.sendParticles(ParticleTypes.CRIT, pos.x, pos.y, pos.z, 2, 0.02D, 0.02D, 0.02D, 0.01D);

            // Whisp of gunsmoke along the path
            if (i % 2 == 0) {
                level.sendParticles(ParticleTypes.SMOKE, pos.x, pos.y, pos.z, 1, 0.01D, 0.01D, 0.01D, 0.0D);
            }

            // Sniper rifle electric tracer streak
            if (isSniper && i % 3 == 0) {
                level.sendParticles(ParticleTypes.ELECTRIC_SPARK, pos.x, pos.y, pos.z, 1, 0.02D, 0.02D, 0.02D, 0.0D);
            }
        }
    }
}
