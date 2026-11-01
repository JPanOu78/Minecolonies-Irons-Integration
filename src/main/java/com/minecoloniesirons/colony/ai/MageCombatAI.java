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
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.SmallFireball;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

/**
 * Mage Combat AI: Maintains tactical ranged distance (8-16 blocks),
 * equips and checks for Iron's Spells (Spellbooks, Staves, Mage Robes) across Tier 1 to 5,
 * verifies colony research unlock tiers, and applies Mana Focus damage multipliers and cast cooldown reductions.
 */
public class MageCombatAI extends RangeCombatAI {

    public static final ResourceLocation EFFECT_MAGE = ResourceLocation.fromNamespaceAndPath("minecolonies", "effects/mage");
    public static final ResourceLocation EFFECT_MAGE_ARMORY_2 = ResourceLocation.fromNamespaceAndPath("minecolonies", "effects/mage_armory_2");
    public static final ResourceLocation EFFECT_MAGE_ARMORY_3 = ResourceLocation.fromNamespaceAndPath("minecolonies", "effects/mage_armory_3");
    public static final ResourceLocation EFFECT_MAGE_ARMORY_4 = ResourceLocation.fromNamespaceAndPath("minecolonies", "effects/mage_armory_4");
    public static final ResourceLocation EFFECT_MAGE_ARMORY_5 = ResourceLocation.fromNamespaceAndPath("minecolonies", "effects/mage_armory_5");
    public static final ResourceLocation EFFECT_MAGE_DAMAGE_1 = ResourceLocation.fromNamespaceAndPath("minecolonies", "effects/mage_damage_1");
    public static final ResourceLocation EFFECT_MAGE_DAMAGE_2 = ResourceLocation.fromNamespaceAndPath("minecolonies", "effects/mage_damage_2");
    public static final ResourceLocation EFFECT_MAGE_DAMAGE_3 = ResourceLocation.fromNamespaceAndPath("minecolonies", "effects/mage_damage_3");
    public static final ResourceLocation EFFECT_MAGE_DAMAGE_4 = ResourceLocation.fromNamespaceAndPath("minecolonies", "effects/mage_damage_4");

    public MageCombatAI(EntityCitizen citizen, ITickRateStateMachine stateMachine, AbstractEntityAIGuard parentAI) {
        super(citizen, stateMachine, parentAI);
    }

    @Override
    public EquipmentTypeEntry getWeaponType() {
        return ModIronsEquipmentTypes.SPELL_FOCUS.get();
    }

    @Override
    public boolean canAttack() {
        if (this.user == null || !this.user.isAlive()) {
            return false;
        }

        // Auto-equip spellcasting focus or spellbook from inventory matching unlocked research tier
        equipMageGear();

        ItemStack main = this.user.getMainHandItem();
        ItemStack off = this.user.getOffhandItem();

        return (!main.isEmpty() && ModIronsEquipmentTypes.isSpellItem(main))
                || (!off.isEmpty() && ModIronsEquipmentTypes.isSpellItem(off));
    }

    public void equipMageGear() {
        if (this.user == null) return;

        int maxAllowedTier = getMaxAllowedTier();

        ItemStack main = this.user.getMainHandItem();
        ItemStack off = this.user.getOffhandItem();

        boolean holdingStaff = !main.isEmpty() && ModIronsEquipmentTypes.isSpellItem(main)
                && ModIronsEquipmentTypes.getSpellItemLevel(main) <= maxAllowedTier;
        boolean holdingBook = !off.isEmpty() && ModIronsEquipmentTypes.isSpellItem(off)
                && ModIronsEquipmentTypes.getSpellItemLevel(off) <= maxAllowedTier;

        if (holdingStaff && holdingBook) {
            return;
        }

        var inv = this.user.getInventoryCitizen();
        for (int i = 0; i < inv.getSlots(); i++) {
            ItemStack stack = inv.getStackInSlot(i);
            if (stack.isEmpty() || !ModIronsEquipmentTypes.isSpellItem(stack)) continue;

            int itemTier = ModIronsEquipmentTypes.getSpellItemLevel(stack);
            if (itemTier > maxAllowedTier) continue;

            String itemName = stack.getItem().toString().toLowerCase();

            // Equip staff or wand in main hand
            if (!holdingStaff && (itemName.contains("staff") || itemName.contains("wand")
                    || itemName.contains("cane") || itemName.contains("rapier") || itemName.contains("flamberge"))) {
                CitizenItemUtils.setHeldItem(this.user, InteractionHand.MAIN_HAND, i);
                holdingStaff = true;
            }
            // Equip spellbook in off hand
            else if (!holdingBook && (itemName.contains("spell") || itemName.contains("book")
                    || itemName.contains("tome") || itemName.contains("grimoire")
                    || itemName.contains("codex") || itemName.contains("manual") || itemName.contains("bible"))) {
                CitizenItemUtils.setHeldItem(this.user, InteractionHand.OFF_HAND, i);
                holdingBook = true;
            }

            if (holdingStaff && holdingBook) break;
        }
    }

    public int getMaxAllowedTier() {
        if (this.user == null) return 1;
        IColony colony = this.user.getCitizenColonyHandler().getColonyOrRegister();
        if (colony == null) return 1;

        IResearchEffectManager rem = colony.getResearchManager().getResearchEffects();
        if (rem.getEffectStrength(EFFECT_MAGE_ARMORY_5) > 0) return 5;
        if (rem.getEffectStrength(EFFECT_MAGE_ARMORY_4) > 0) return 4;
        if (rem.getEffectStrength(EFFECT_MAGE_ARMORY_3) > 0) return 3;
        if (rem.getEffectStrength(EFFECT_MAGE_ARMORY_2) > 0) return 2;
        return 1;
    }

    private float getDamageMultiplier() {
        float mult = 1.0F;
        if (this.user == null) return mult;
        IColony colony = this.user.getCitizenColonyHandler().getColonyOrRegister();
        if (colony == null) return mult;

        IResearchEffectManager rem = colony.getResearchManager().getResearchEffects();
        if (rem.getEffectStrength(EFFECT_MAGE_DAMAGE_1) > 0) mult += 0.10F;
        if (rem.getEffectStrength(EFFECT_MAGE_DAMAGE_2) > 0) mult += 0.10F;
        if (rem.getEffectStrength(EFFECT_MAGE_DAMAGE_3) > 0) mult += 0.10F;
        if (rem.getEffectStrength(EFFECT_MAGE_DAMAGE_4) > 0) mult += 0.10F;
        return mult;
    }

    @Override
    protected double getAttackDistance() {
        return 16.0D;
    }

    @Override
    protected int getAttackDelay() {
        ItemStack main = this.user.getMainHandItem();
        String mainName = main.getItem().toString().toLowerCase();
        int baseDelay = 25;
        if (mainName.contains("wand")) {
            baseDelay = 20; // Wands cast faster
        } else if (mainName.contains("staff") || mainName.contains("blood_staff")) {
            baseDelay = 30; // Heavy staves have higher power
        }

        // Archmage Transcendence (LV 5 focus) reduces cast delay by 15%
        if (this.user != null) {
            IColony colony = this.user.getCitizenColonyHandler().getColonyOrRegister();
            if (colony != null && colony.getResearchManager().getResearchEffects().getEffectStrength(EFFECT_MAGE_DAMAGE_4) > 0) {
                baseDelay = (int) (baseDelay * 0.85F);
            }
        }

        return Math.max(10, baseDelay);
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

        boolean hasSpellbook = ModIronsEquipmentTypes.isSpellItem(offHand) || ModIronsEquipmentTypes.isSpellItem(mainHand);
        int weaponTier = Math.max(ModIronsEquipmentTypes.getSpellItemLevel(mainHand), ModIronsEquipmentTypes.getSpellItemLevel(offHand));

        // Swing arm to simulate player spellcasting windup
        this.user.swing(InteractionHand.MAIN_HAND);

        float damageMultiplier = getDamageMultiplier();

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

            // Execute elemental or eldritch spellcast
            if (mainName.contains("blood") || offName.contains("necronomicon") || offName.contains("vampiric")) {
                // Blood / Eldritch Dark Magic
                serverLevel.playSound(null, this.user.blockPosition(), SoundEvents.WITHER_SHOOT, SoundSource.NEUTRAL, 0.9F, 1.3F);
                serverLevel.sendParticles(ParticleTypes.DAMAGE_INDICATOR, targetPos.x, targetPos.y + 0.5D, targetPos.z, 15, 0.3D, 0.4D, 0.3D, 0.1D);
                float magicDamage = (10.0F + (weaponTier * 2.0F)) * damageMultiplier;
                target.hurt(this.user.damageSources().wither(), magicDamage);
            } else if (mainName.contains("fire") || mainName.contains("pyro") || offName.contains("blaze")) {
                // Fireball / Flame bolt spell
                serverLevel.playSound(null, this.user.blockPosition(), SoundEvents.BLAZE_SHOOT, SoundSource.NEUTRAL, 1.0F, 1.0F);
                SmallFireball fireball = new SmallFireball(serverLevel, this.user, direction.scale(0.5D));
                fireball.setPos(citizenPos.x + direction.x, citizenPos.y, citizenPos.z + direction.z);
                serverLevel.addFreshEntity(fireball);
            } else if (mainName.contains("ice") || mainName.contains("cryo") || offName.contains("frost")) {
                // Frost / Ice shard spell
                serverLevel.playSound(null, this.user.blockPosition(), SoundEvents.GLASS_BREAK, SoundSource.NEUTRAL, 1.0F, 1.4F);
                serverLevel.sendParticles(ParticleTypes.SNOWFLAKE, targetPos.x, targetPos.y + 0.5D, targetPos.z, 20, 0.4D, 0.4D, 0.4D, 0.05D);
                float magicDamage = (8.0F + weaponTier) * damageMultiplier;
                target.hurt(this.user.damageSources().magic(), magicDamage);
                target.setTicksFrozen(120);
            } else if (mainName.contains("lightning") || mainName.contains("electro")) {
                // Lightning bolt spell
                serverLevel.playSound(null, this.user.blockPosition(), SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.NEUTRAL, 0.8F, 1.6F);
                serverLevel.sendParticles(ParticleTypes.ELECTRIC_SPARK, targetPos.x, targetPos.y + 0.5D, targetPos.z, 25, 0.5D, 0.5D, 0.5D, 0.2D);
                float magicDamage = (10.0F + weaponTier) * damageMultiplier;
                target.hurt(this.user.damageSources().lightningBolt(), magicDamage);
            } else {
                // Default Arcane Blast
                serverLevel.playSound(null, this.user.blockPosition(), SoundEvents.EVOKER_CAST_SPELL, SoundSource.NEUTRAL, 1.0F, 1.2F);
                serverLevel.sendParticles(ParticleTypes.WITCH, targetPos.x, targetPos.y + 0.5D, targetPos.z, 20, 0.3D, 0.5D, 0.3D, 0.1D);
                float magicDamage = (6.0F + (weaponTier * 1.5F) + (hasSpellbook ? 2.0F : 0.0F)) * damageMultiplier;
                target.hurt(this.user.damageSources().magic(), magicDamage);
            }
        }
    }
}
