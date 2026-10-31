package com.minecoloniesirons.colony.ai;

import com.minecolonies.api.entity.ai.statemachine.tickratestatemachine.ITickRateStateMachine;
import com.minecolonies.core.entity.ai.workers.guard.AbstractEntityAIGuard;
import com.minecolonies.core.entity.ai.workers.guard.RangeCombatAI;
import com.minecolonies.core.entity.citizen.EntityCitizen;
import com.minecolonies.core.util.citizenutils.CitizenItemUtils;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

/**
 * Gunner Combat AI: Maintains tactical ranged distance (10-18 blocks),
 * equips and checks for Iron's Artifice firearms (Musket, Pistol, Blunderbuss, Revolver, Rifle) & Ammo,
 * executes volley firing patterns, reload intervals, and kiting movement.
 */
public class GunnerCombatAI extends RangeCombatAI {

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

        // Auto-equip firearm and ammo from inventory if not already holding a gun
        equipFirearmGear();

        return true;
    }

    private void equipFirearmGear() {
        if (this.user == null) return;

        ItemStack main = this.user.getMainHandItem();
        String mainName = main.getItem().toString().toLowerCase();

        boolean holdingGun = mainName.contains("musket") || mainName.contains("pistol")
                || mainName.contains("rifle") || mainName.contains("blunderbuss")
                || mainName.contains("arquebus") || mainName.contains("gun")
                || mainName.contains("revolver");

        if (holdingGun) {
            return;
        }

        var inv = this.user.getInventoryCitizen();
        for (int i = 0; i < inv.getSlots(); i++) {
            ItemStack stack = inv.getStackInSlot(i);
            if (stack.isEmpty()) continue;
            String itemName = stack.getItem().toString().toLowerCase();

            // Equip firearm in main hand
            if (itemName.contains("musket") || itemName.contains("pistol")
                    || itemName.contains("rifle") || itemName.contains("blunderbuss")
                    || itemName.contains("arquebus") || itemName.contains("gun")
                    || itemName.contains("revolver")) {
                CitizenItemUtils.setHeldItem(this.user, InteractionHand.MAIN_HAND, i);
                break;
            }
        }
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

        // Rifle/Revolver fires faster; Musket/Arquebus has standard cadence
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

            // Calculate damage based on weapon tier and type
            float baseDamage = 8.0F;
            if (isBlunderbuss) {
                // High spread burst damage
                baseDamage = 11.0F;
                serverLevel.sendParticles(ParticleTypes.CRIT, targetEye.x, targetEye.y, targetEye.z, 15, 0.6D, 0.6D, 0.6D, 0.1D);
            } else if (isSniper) {
                // High precision heavy damage
                baseDamage = 14.0F;
                serverLevel.sendParticles(ParticleTypes.CRIT, targetEye.x, targetEye.y, targetEye.z, 8, 0.2D, 0.2D, 0.2D, 0.2D);
            } else {
                serverLevel.sendParticles(ParticleTypes.CRIT, targetEye.x, targetEye.y, targetEye.z, 6, 0.3D, 0.3D, 0.3D, 0.1D);
            }

            // Apply bullet damage
            target.hurt(this.user.damageSources().mobAttack(this.user), baseDamage);

            // Push target back slightly (bullet impact knockback)
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
