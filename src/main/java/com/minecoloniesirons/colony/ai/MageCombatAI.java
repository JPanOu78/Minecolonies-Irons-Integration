package com.minecoloniesirons.colony.ai;

import com.minecolonies.api.colony.IColony;
import com.minecolonies.api.entity.ai.statemachine.tickratestatemachine.ITickRateStateMachine;
import com.minecolonies.api.equipment.registry.EquipmentTypeEntry;
import com.minecolonies.api.research.IResearchEffectManager;
import com.minecolonies.core.entity.ai.workers.guard.AbstractEntityAIGuard;
import com.minecolonies.core.entity.ai.workers.guard.RangeCombatAI;
import com.minecolonies.core.entity.citizen.EntityCitizen;
import com.minecolonies.core.util.citizenutils.CitizenItemUtils;
import com.minecoloniesirons.colony.equipment.MageRobeSchool;
import com.minecoloniesirons.colony.equipment.ModIronsEquipmentTypes;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.SmallFireball;
import net.minecraft.world.entity.projectile.Snowball;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

/**
 * Mage Combat AI: Maintains tactical ranged distance (8-16 blocks),
 * equips and checks for Iron's Spells (Spellbooks, Staves, Mage Robes) across Tier 1 to 5,
 * verifies colony research unlock tiers, renders flying projectiles and magic streams in the air,
 * and applies Mana Focus damage multipliers and cast cooldown reductions.
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

    /**
     * Ensures staff is held in main hand and spellbook in offhand at all times.
     * If citizen only carries a spellbook, it is held in the main hand forward.
     */
    public void equipMageGear() {
        if (this.user == null) return;

        // Equip robes if research tier >= 2
        equipMageRobes();

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
        int staffSlot = -1;
        int bookSlot = -1;

        for (int i = 0; i < inv.getSlots(); i++) {
            ItemStack stack = inv.getStackInSlot(i);
            if (stack.isEmpty() || !ModIronsEquipmentTypes.isSpellItem(stack)) continue;

            int itemTier = ModIronsEquipmentTypes.getSpellItemLevel(stack);
            if (itemTier > maxAllowedTier) continue;

            String itemName = stack.getItem().toString().toLowerCase();

            if (staffSlot == -1 && (itemName.contains("staff") || itemName.contains("wand")
                    || itemName.contains("cane") || itemName.contains("rapier") || itemName.contains("flamberge"))) {
                staffSlot = i;
            } else if (bookSlot == -1 && (itemName.contains("spell") || itemName.contains("book")
                    || itemName.contains("tome") || itemName.contains("grimoire")
                    || itemName.contains("codex") || itemName.contains("manual") || itemName.contains("bible")
                    || itemName.contains("journal"))) {
                bookSlot = i;
            }
        }

        // Case 1: Both staff and spellbook available -> Staff in Main Hand, Book in Off Hand
        if (staffSlot != -1 && bookSlot != -1) {
            CitizenItemUtils.setHeldItem(this.user, InteractionHand.MAIN_HAND, staffSlot);
            this.user.setItemSlot(EquipmentSlot.MAINHAND, inv.getStackInSlot(staffSlot));
            CitizenItemUtils.setHeldItem(this.user, InteractionHand.OFF_HAND, bookSlot);
            this.user.setItemSlot(EquipmentSlot.OFFHAND, inv.getStackInSlot(bookSlot));
        }
        // Case 2: Only staff available -> Staff in Main Hand
        else if (staffSlot != -1) {
            CitizenItemUtils.setHeldItem(this.user, InteractionHand.MAIN_HAND, staffSlot);
            this.user.setItemSlot(EquipmentSlot.MAINHAND, inv.getStackInSlot(staffSlot));
        }
        // Case 3: Only spellbook available -> Spellbook in Main Hand so it is visibly held forward
        else if (bookSlot != -1) {
            CitizenItemUtils.setHeldItem(this.user, InteractionHand.MAIN_HAND, bookSlot);
            this.user.setItemSlot(EquipmentSlot.MAINHAND, inv.getStackInSlot(bookSlot));
        }
    }

    /**
     * Equips Iron's Spells magic robes and armor from citizen inventory.
     * Unlocked strictly at Research Tier 2+ (Arcane Repository).
     */
    public void equipMageRobes() {
        if (this.user == null) return;
        int maxTier = getMaxAllowedTier();
        if (maxTier < 2) return; // Strictly unlocked only at Research Tier 2+

        var inv = this.user.getInventoryCitizen();
        EquipmentSlot[] armorSlots = new EquipmentSlot[]{
                EquipmentSlot.HEAD,
                EquipmentSlot.CHEST,
                EquipmentSlot.LEGS,
                EquipmentSlot.FEET
        };

        for (EquipmentSlot slot : armorSlots) {
            ItemStack current = this.user.getItemBySlot(slot);
            int currentRobeTier = ModIronsEquipmentTypes.isRobeItemForSlot(current, slot)
                    ? ModIronsEquipmentTypes.getRobeTier(current) : 0;

            int bestSlot = -1;
            int bestTier = currentRobeTier;

            for (int i = 0; i < inv.getSlots(); i++) {
                ItemStack stack = inv.getStackInSlot(i);
                if (stack.isEmpty()) continue;
                if (!ModIronsEquipmentTypes.isRobeItemForSlot(stack, slot)) continue;

                int tier = ModIronsEquipmentTypes.getRobeTier(stack);
                if (tier <= maxTier && tier > bestTier) {
                    bestTier = tier;
                    bestSlot = i;
                }
            }

            if (bestSlot != -1) {
                ItemStack newArmor = inv.getStackInSlot(bestSlot);
                this.user.setItemSlot(slot, newArmor);
            }
        }
    }

    /**
     * Determines the active magic school based on the worn robes.
     * Chestplate takes precedence, followed by helmet, leggings, and boots.
     */
    public MageRobeSchool getActiveRobeSchool() {
        if (this.user == null || getMaxAllowedTier() < 2) return MageRobeSchool.NONE;

        ItemStack chest = this.user.getItemBySlot(EquipmentSlot.CHEST);
        if (!chest.isEmpty() && ModIronsEquipmentTypes.isRobeItem(chest)) {
            return ModIronsEquipmentTypes.getRobeSchool(chest);
        }
        ItemStack head = this.user.getItemBySlot(EquipmentSlot.HEAD);
        if (!head.isEmpty() && ModIronsEquipmentTypes.isRobeItem(head)) {
            return ModIronsEquipmentTypes.getRobeSchool(head);
        }
        ItemStack legs = this.user.getItemBySlot(EquipmentSlot.LEGS);
        if (!legs.isEmpty() && ModIronsEquipmentTypes.isRobeItem(legs)) {
            return ModIronsEquipmentTypes.getRobeSchool(legs);
        }
        ItemStack feet = this.user.getItemBySlot(EquipmentSlot.FEET);
        if (!feet.isEmpty() && ModIronsEquipmentTypes.isRobeItem(feet)) {
            return ModIronsEquipmentTypes.getRobeSchool(feet);
        }
        return MageRobeSchool.NONE;
    }

    /**
     * Counts how many worn pieces belong to the specified school.
     */
    public int getRobePieceCount(MageRobeSchool school) {
        if (this.user == null || school == MageRobeSchool.NONE) return 0;
        int count = 0;
        for (EquipmentSlot slot : new EquipmentSlot[]{EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET}) {
            ItemStack stack = this.user.getItemBySlot(slot);
            if (!stack.isEmpty() && ModIronsEquipmentTypes.getRobeSchool(stack) == school) {
                count++;
            }
        }
        return count;
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

        // Apply Robe School attack speed / cooldown reductions
        MageRobeSchool school = getActiveRobeSchool();
        if (school == MageRobeSchool.LIGHTNING) {
            // Electromancer rapid casting: up to -30% cooldown
            float reduction = 0.15F + (0.15F * (getRobePieceCount(school) / 4.0F));
            baseDelay = (int) (baseDelay * (1.0F - reduction));
        } else if (school == MageRobeSchool.NETHERITE) {
            // Netherite master: -25% cooldown
            baseDelay = (int) (baseDelay * 0.75F);
        } else if (school == MageRobeSchool.ARCANE) {
            // Arcane novice: -15% cooldown
            baseDelay = (int) (baseDelay * 0.85F);
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

        MageRobeSchool school = getActiveRobeSchool();
        int pieceCount = getRobePieceCount(school);
        boolean hasRobeBonus = school != MageRobeSchool.NONE && getMaxAllowedTier() >= 2;

        float robeDamageMult = 1.0F;

        if (this.user.level() instanceof ServerLevel serverLevel) {
            Vec3 citizenPos = this.user.getEyePosition();
            Vec3 targetPos = target.getEyePosition();
            Vec3 direction = targetPos.subtract(citizenPos).normalize();

            // Apply Robe School specific buffs & effects
            if (hasRobeBonus) {
                switch (school) {
                    case FIRE -> {
                        // Pyromancy: +15% to +35% Fire damage, ignite target, fire resistance
                        robeDamageMult += 0.15F + (0.20F * (pieceCount / 4.0F));
                        target.igniteForSeconds(4 + pieceCount);
                        this.user.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 120, 0, false, false));
                        serverLevel.sendParticles(ParticleTypes.FLAME, citizenPos.x, citizenPos.y + 0.2D, citizenPos.z, 6, 0.2D, 0.2D, 0.2D, 0.02D);
                    }
                    case ICE -> {
                        // Cryomancy: +15% to +30% Ice damage, deep freeze & slowness, damage resistance
                        robeDamageMult += 0.15F + (0.15F * (pieceCount / 4.0F));
                        target.setTicksFrozen(140 + (pieceCount * 30));
                        target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60 + (pieceCount * 15), 1));
                        this.user.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 100, 0, false, false));
                        serverLevel.sendParticles(ParticleTypes.SNOWFLAKE, citizenPos.x, citizenPos.y + 0.2D, citizenPos.z, 6, 0.2D, 0.2D, 0.2D, 0.02D);
                    }
                    case LIGHTNING -> {
                        // Electromancy: +15% to +30% Lightning damage, speed boost
                        robeDamageMult += 0.15F + (0.15F * (pieceCount / 4.0F));
                        this.user.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 100, 0, false, false));
                        serverLevel.sendParticles(ParticleTypes.ELECTRIC_SPARK, citizenPos.x, citizenPos.y + 0.2D, citizenPos.z, 6, 0.2D, 0.2D, 0.2D, 0.05D);
                    }
                    case HOLY -> {
                        // Holy: Smite Undead (+30% to +60%), heal self, regeneration aura
                        if (target.isInvertedHealAndHarm()) {
                            robeDamageMult += 0.30F + (0.30F * (pieceCount / 4.0F));
                            serverLevel.sendParticles(ParticleTypes.FLASH, targetPos.x, targetPos.y + 0.5D, targetPos.z, 1, 0, 0, 0, 0);
                        }
                        float healAmount = 1.0F + (pieceCount * 0.75F);
                        this.user.heal(healAmount);
                        this.user.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 60, 0, false, false));
                        serverLevel.sendParticles(ParticleTypes.HAPPY_VILLAGER, citizenPos.x, citizenPos.y + 0.2D, citizenPos.z, 5, 0.2D, 0.2D, 0.2D, 0.02D);
                    }
                    case BLOOD -> {
                        // Blood: +10% to +25% dark damage, vampiric life leech & weakness
                        robeDamageMult += 0.10F + (0.15F * (pieceCount / 4.0F));
                        target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 60 + (pieceCount * 15), 0));
                        this.user.heal(2.0F + (pieceCount * 0.5F));
                        serverLevel.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, citizenPos.x, citizenPos.y + 0.2D, citizenPos.z, 5, 0.2D, 0.2D, 0.2D, 0.02D);
                    }
                    case EVOCATION -> {
                        // Evocation: +10% to +25% damage, poison toxin
                        robeDamageMult += 0.10F + (0.15F * (pieceCount / 4.0F));
                        target.addEffect(new MobEffectInstance(MobEffects.POISON, 80, 1));
                        serverLevel.sendParticles(ParticleTypes.WITCH, citizenPos.x, citizenPos.y + 0.2D, citizenPos.z, 6, 0.2D, 0.2D, 0.2D, 0.02D);
                    }
                    case ENDER -> {
                        // Ender: +15% to +30% damage, Speed II, tactical teleport dodge
                        robeDamageMult += 0.15F + (0.15F * (pieceCount / 4.0F));
                        this.user.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 100, 1, false, false));
                        serverLevel.sendParticles(ParticleTypes.PORTAL, citizenPos.x, citizenPos.y + 0.2D, citizenPos.z, 8, 0.2D, 0.2D, 0.2D, 0.05D);
                        if (this.user.getHealth() < this.user.getMaxHealth() * 0.5F && this.user.distanceToSqr(target) < 25.0D) {
                            Vec3 away = citizenPos.subtract(targetPos).normalize().scale(5.0D);
                            this.user.teleportTo(citizenPos.x + away.x, citizenPos.y, citizenPos.z + away.z);
                            serverLevel.playSound(null, this.user.blockPosition(), SoundEvents.ENDERMAN_TELEPORT, SoundSource.NEUTRAL, 1.0F, 1.0F);
                        }
                    }
                    case ARCANE -> {
                        // Arcane: +10% to +25% all magic damage, resistance
                        robeDamageMult += 0.10F + (0.15F * (pieceCount / 4.0F));
                        this.user.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 80, 0, false, false));
                        serverLevel.sendParticles(ParticleTypes.ENCHANT, citizenPos.x, citizenPos.y + 0.2D, citizenPos.z, 6, 0.2D, 0.2D, 0.2D, 0.02D);
                    }
                    case NETHERITE -> {
                        // Netherite Master: +35% to +50% ultimate damage, Resistance II, Fire Resistance
                        robeDamageMult += 0.35F + (0.15F * (pieceCount / 4.0F));
                        this.user.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 120, 1, false, false));
                        this.user.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 120, 0, false, false));
                        serverLevel.sendParticles(ParticleTypes.DRAGON_BREATH, citizenPos.x, citizenPos.y + 0.2D, citizenPos.z, 6, 0.2D, 0.2D, 0.2D, 0.02D);
                    }
                    default -> {}
                }
            }

            // Arcane casting burst at hands
            serverLevel.sendParticles(
                    ParticleTypes.ENCHANT,
                    citizenPos.x + direction.x * 0.8D,
                    citizenPos.y + direction.y * 0.8D,
                    citizenPos.z + direction.z * 0.8D,
                    15, 0.2D, 0.2D, 0.2D, 0.1D
            );

            // Execute elemental or eldritch spellcast with VISIBLE projectile trajectory
            if (mainName.contains("blood") || offName.contains("necronomicon") || offName.contains("vampiric")) {
                // Blood / Eldritch Dark Magic: Stream of dark void & soulflame particles towards target
                serverLevel.playSound(null, this.user.blockPosition(), SoundEvents.WITHER_SHOOT, SoundSource.NEUTRAL, 0.9F, 1.3F);
                renderProjectileTrail(serverLevel, citizenPos, targetPos, ParticleTypes.SOUL_FIRE_FLAME, ParticleTypes.WITCH, 0.4D);
                serverLevel.sendParticles(ParticleTypes.DAMAGE_INDICATOR, targetPos.x, targetPos.y + 0.5D, targetPos.z, 15, 0.3D, 0.4D, 0.3D, 0.1D);
                float magicDamage = (10.0F + (weaponTier * 2.0F)) * damageMultiplier * robeDamageMult;
                target.hurt(this.user.damageSources().wither(), magicDamage);
            } else if (mainName.contains("fire") || mainName.contains("pyro") || offName.contains("blaze")) {
                // Fireball / Flame bolt spell: Actual SmallFireball projectile flying + flame streak
                serverLevel.playSound(null, this.user.blockPosition(), SoundEvents.BLAZE_SHOOT, SoundSource.NEUTRAL, 1.0F, 1.0F);
                renderProjectileTrail(serverLevel, citizenPos, targetPos, ParticleTypes.FLAME, ParticleTypes.SMOKE, 0.4D);
                SmallFireball fireball = new SmallFireball(serverLevel, this.user, direction.scale(0.6D));
                fireball.setPos(citizenPos.x + direction.x, citizenPos.y, citizenPos.z + direction.z);
                serverLevel.addFreshEntity(fireball);
                float magicDamage = (9.0F + weaponTier) * damageMultiplier * robeDamageMult;
                target.hurt(this.user.damageSources().inFire(), magicDamage);
            } else if (mainName.contains("ice") || mainName.contains("cryo") || offName.contains("frost")) {
                // Frost / Ice shard spell: Snowball projectile entity + snowflake stream
                serverLevel.playSound(null, this.user.blockPosition(), SoundEvents.GLASS_BREAK, SoundSource.NEUTRAL, 1.0F, 1.4F);
                renderProjectileTrail(serverLevel, citizenPos, targetPos, ParticleTypes.SNOWFLAKE, ParticleTypes.INSTANT_EFFECT, 0.4D);
                Snowball snowball = new Snowball(serverLevel, this.user);
                snowball.setPos(citizenPos.x + direction.x * 0.8D, citizenPos.y, citizenPos.z + direction.z * 0.8D);
                snowball.setDeltaMovement(direction.scale(1.8D));
                serverLevel.addFreshEntity(snowball);
                float magicDamage = (8.0F + weaponTier) * damageMultiplier * robeDamageMult;
                target.hurt(this.user.damageSources().magic(), magicDamage);
                target.setTicksFrozen(120);
            } else if (mainName.contains("lightning") || mainName.contains("electro")) {
                // Lightning bolt spell: Lightning spark stream + flash beam
                serverLevel.playSound(null, this.user.blockPosition(), SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.NEUTRAL, 0.8F, 1.6F);
                renderProjectileTrail(serverLevel, citizenPos, targetPos, ParticleTypes.ELECTRIC_SPARK, ParticleTypes.FLASH, 0.35D);
                float magicDamage = (10.0F + weaponTier) * damageMultiplier * robeDamageMult;
                target.hurt(this.user.damageSources().lightningBolt(), magicDamage);
            } else {
                // Default Arcane Blast: Dense stream of magical runes and enchantment particles
                serverLevel.playSound(null, this.user.blockPosition(), SoundEvents.EVOKER_CAST_SPELL, SoundSource.NEUTRAL, 1.0F, 1.2F);
                renderProjectileTrail(serverLevel, citizenPos, targetPos, ParticleTypes.ENCHANT, ParticleTypes.PORTAL, 0.4D);
                serverLevel.sendParticles(ParticleTypes.WITCH, targetPos.x, targetPos.y + 0.5D, targetPos.z, 20, 0.3D, 0.5D, 0.3D, 0.1D);
                float magicDamage = (6.0F + (weaponTier * 1.5F) + (hasSpellbook ? 2.0F : 0.0F)) * damageMultiplier * robeDamageMult;
                target.hurt(this.user.damageSources().magic(), magicDamage);
            }
        }
    }

    /**
     * Renders a continuous visual projectile stream in the air from start to target.
     */
    public static void renderProjectileTrail(ServerLevel level, Vec3 start, Vec3 end, ParticleOptions primary, ParticleOptions secondary, double stepSize) {
        double dist = start.distanceTo(end);
        int steps = Math.max(2, (int) (dist / stepSize));
        for (int i = 1; i <= steps; i++) {
            double progress = (double) i / steps;
            Vec3 pos = start.lerp(end, progress);
            level.sendParticles(primary, pos.x, pos.y, pos.z, 2, 0.05, 0.05, 0.05, 0.01);
            if (secondary != null && i % 2 == 0) {
                level.sendParticles(secondary, pos.x, pos.y, pos.z, 1, 0.02, 0.02, 0.02, 0.0);
            }
        }
    }
}
