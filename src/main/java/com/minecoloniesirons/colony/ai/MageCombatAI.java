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
import net.minecraft.world.entity.projectile.SmallFireball;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

/**
 * Mage Combat AI: Maintains tactical ranged distance (8-16 blocks),
 * equips and checks for Iron's Spells (Spellbooks, Staves, Mage Robes) like a player,
 * and casts spells to defend the colony.
 */
public class MageCombatAI extends RangeCombatAI {

    public MageCombatAI(EntityCitizen citizen, ITickRateStateMachine stateMachine, AbstractEntityAIGuard parentAI) {
        super(citizen, stateMachine, parentAI);
    }

    @Override
    public boolean canAttack() {
        if (this.user == null || !this.user.isAlive()) {
            return false;
        }

        // Auto-equip spellcasting focus or spellbook from inventory if not already holding one
        equipMageGear();

        return true;
    }

    private void equipMageGear() {
        if (this.user == null) return;

        ItemStack main = this.user.getMainHandItem();
        ItemStack off = this.user.getOffhandItem();

        String mainName = main.getItem().toString().toLowerCase();
        String offName = off.getItem().toString().toLowerCase();

        boolean holdingStaff = mainName.contains("staff") || mainName.contains("wand") || mainName.contains("cane");
        boolean holdingBook = offName.contains("spell") || offName.contains("book")
                || mainName.contains("spell") || mainName.contains("book");

        if (holdingStaff && holdingBook) {
            return;
        }

        var inv = this.user.getInventoryCitizen();
        for (int i = 0; i < inv.getSlots(); i++) {
            ItemStack stack = inv.getStackInSlot(i);
            if (stack.isEmpty()) continue;
            String itemName = stack.getItem().toString().toLowerCase();

            // Equip staff or wand in main hand
            if (!holdingStaff && (itemName.contains("staff") || itemName.contains("wand") || itemName.contains("cane"))) {
                CitizenItemUtils.setHeldItem(this.user, InteractionHand.MAIN_HAND, i);
                holdingStaff = true;
            }
            // Equip spellbook in off hand (like player offhand or Curios)
            else if (!holdingBook && (itemName.contains("spell") || itemName.contains("book") || itemName.contains("tome") || itemName.contains("grimoire"))) {
                CitizenItemUtils.setHeldItem(this.user, InteractionHand.OFF_HAND, i);
                holdingBook = true;
            }

            if (holdingStaff && holdingBook) break;
        }
    }

    @Override
    protected double getAttackDistance() {
        return 16.0D;
    }

    @Override
    protected int getAttackDelay() {
        ItemStack main = this.user.getMainHandItem();
        String mainName = main.getItem().toString().toLowerCase();
        if (mainName.contains("wand")) {
            return 20; // Wands cast faster
        } else if (mainName.contains("staff")) {
            return 30; // Staves have higher power, standard cast delay
        }
        return 25;
    }

    @Override
    protected void doAttack(LivingEntity target) {
        if (target == null || !target.isAlive() || this.user == null) {
            return;
        }

        // Face the target
        this.user.getLookControl().setLookAt(target, 30.0F, 30.0F);

        // Check equipped gear (Staves, Wands, Spellbooks in hands)
        ItemStack mainHand = this.user.getMainHandItem();
        ItemStack offHand = this.user.getOffhandItem();
        String mainName = mainHand.getItem().toString().toLowerCase();
        String offName = offHand.getItem().toString().toLowerCase();

        boolean hasSpellbook = offName.contains("spell") || offName.contains("book")
                || mainName.contains("spell") || mainName.contains("book");
        boolean hasStaff = mainName.contains("staff") || mainName.contains("wand")
                || mainName.contains("cane");

        // Swing arm to simulate player spellcasting windup
        this.user.swing(InteractionHand.MAIN_HAND);

        if (this.user.level() instanceof ServerLevel serverLevel) {
            Vec3 citizenPos = this.user.getEyePosition();
            Vec3 targetPos = target.getEyePosition();
            Vec3 direction = targetPos.subtract(citizenPos).normalize();

            // Arcane particles during casting
            serverLevel.sendParticles(
                    ParticleTypes.ENCHANT,
                    citizenPos.x + direction.x * 0.8D,
                    citizenPos.y + direction.y * 0.8D,
                    citizenPos.z + direction.z * 0.8D,
                    15, 0.2D, 0.2D, 0.2D, 0.1D
            );

            // Execute spellcast attack
            if (mainName.contains("fire") || mainName.contains("pyro") || hasSpellbook) {
                // Fireball / Flame bolt spell
                serverLevel.playSound(null, this.user.blockPosition(), SoundEvents.BLAZE_SHOOT, SoundSource.NEUTRAL, 1.0F, 1.0F);
                SmallFireball fireball = new SmallFireball(serverLevel, this.user, direction.scale(0.5D));
                fireball.setPos(citizenPos.x + direction.x, citizenPos.y, citizenPos.z + direction.z);
                serverLevel.addFreshEntity(fireball);
            } else if (mainName.contains("ice") || mainName.contains("cryo")) {
                // Frost / Ice shard spell
                serverLevel.playSound(null, this.user.blockPosition(), SoundEvents.GLASS_BREAK, SoundSource.NEUTRAL, 1.0F, 1.4F);
                serverLevel.sendParticles(ParticleTypes.SNOWFLAKE, targetPos.x, targetPos.y + 0.5D, targetPos.z, 20, 0.4D, 0.4D, 0.4D, 0.05D);
                target.hurt(this.user.damageSources().magic(), 7.0F);
                target.setTicksFrozen(100);
            } else if (mainName.contains("lightning") || mainName.contains("electro")) {
                // Lightning bolt spell
                serverLevel.playSound(null, this.user.blockPosition(), SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.NEUTRAL, 0.8F, 1.6F);
                serverLevel.sendParticles(ParticleTypes.ELECTRIC_SPARK, targetPos.x, targetPos.y + 0.5D, targetPos.z, 25, 0.5D, 0.5D, 0.5D, 0.2D);
                target.hurt(this.user.damageSources().lightningBolt(), 9.0F);
            } else {
                // Default Arcane Blast
                serverLevel.playSound(null, this.user.blockPosition(), SoundEvents.EVOKER_CAST_SPELL, SoundSource.NEUTRAL, 1.0F, 1.2F);
                serverLevel.sendParticles(ParticleTypes.WITCH, targetPos.x, targetPos.y + 0.5D, targetPos.z, 20, 0.3D, 0.5D, 0.3D, 0.1D);
                float magicDamage = 6.0F + (hasStaff ? 3.0F : 0.0F) + (hasSpellbook ? 3.0F : 0.0F);
                target.hurt(this.user.damageSources().magic(), magicDamage);
            }
        }
    }
}
