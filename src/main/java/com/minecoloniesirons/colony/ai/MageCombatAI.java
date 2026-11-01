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
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.registry.AttributeRegistry;
import io.redspace.ironsspellbooks.api.registry.SpellRegistry;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.api.spells.CastSource;
import io.redspace.ironsspellbooks.api.spells.ISpellContainer;
import io.redspace.ironsspellbooks.api.spells.SpellSlot;
import io.redspace.ironsspellbooks.capabilities.magic.TargetEntityCastData;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * Mage Combat AI: Maintains tactical ranged distance (8-16 blocks),
 * equips and visibly holds Iron's Spells gear (Staff in Main Hand, Spellbook in Offhand),
 * equips Iron's Spells Robes at Research Tier 2+ with school-specific buffs,
 * maintains individual Citizen Mana and Mana Regen using Iron's Spells AttributeRegistry,
 * strictly casts spells inscribed in the equipped Spell Book (defaulting to Magic Missile LV1 if empty),
 * and executes spells natively using Iron's Spells system with real projectile entities and effects.
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

    private int spellCycleIndex = 0;
    private long lastTickTime = 0;

    public MageCombatAI(EntityCitizen citizen, ITickRateStateMachine stateMachine, AbstractEntityAIGuard parentAI) {
        super(citizen, stateMachine, parentAI);
    }

    @Override
    public EquipmentTypeEntry getWeaponType() {
        return ModIronsEquipmentTypes.SPELL_FOCUS.get();
    }

    /**
     * Finds the equipped Spell Book in offhand or mainhand.
     */
    public ItemStack getEquippedSpellBook() {
        if (this.user == null) return ItemStack.EMPTY;
        ItemStack off = this.user.getOffhandItem();
        if (ModIronsEquipmentTypes.isSpellBook(off)) {
            return off;
        }
        ItemStack main = this.user.getMainHandItem();
        if (ModIronsEquipmentTypes.isSpellBook(main)) {
            return main;
        }
        return ItemStack.EMPTY;
    }

    @Override
    public boolean canAttack() {
        if (this.user == null || !this.user.isAlive()) {
            return false;
        }

        // Auto-equip staff and spellbook according to unlocked research tier
        equipMageGear();

        // Mage must hold a Spell Book to cast spells
        ItemStack spellBook = getEquippedSpellBook();
        if (spellBook.isEmpty()) {
            return false;
        }

        // Check if citizen has enough mana for at least baseline casting
        updateMana(10);
        MagicData magicData = MagicData.getPlayerMagicData(this.user);
        return magicData.getMana() >= 8.0F;
    }

    /**
     * Updates and regenerates the citizen's individual Mana pool using Iron's Spells attributes.
     * Items, staves, robes, curios, and potion effects naturally modify MAX_MANA and MANA_REGEN.
     */
    public void updateMana(int elapsedTicks) {
        if (this.user == null) return;
        MagicData magicData = MagicData.getPlayerMagicData(this.user);
        float maxMana = (float) this.user.getAttributeValue(AttributeRegistry.MAX_MANA);
        float regen = (float) this.user.getAttributeValue(AttributeRegistry.MANA_REGEN);

        if (maxMana <= 0.0F) {
            maxMana = 100.0F;
        }

        if (magicData.getMana() <= 0.0F) {
            magicData.setMana(maxMana);
        } else {
            // Mana regen formula: maxMana * regen * 0.01 per minute (1200 ticks), scaled by elapsed ticks
            float regenAmount = (maxMana * regen * 0.01F) * ((float) elapsedTicks / 20.0F);
            float newMana = Math.min(maxMana, magicData.getMana() + regenAmount);
            magicData.setMana(newMana);
        }

        // Tick active spell cooldowns
        if (magicData.getPlayerCooldowns() != null) {
            magicData.getPlayerCooldowns().tick(elapsedTicks);
        }
    }

    /**
     * Equips Staff in Main Hand and Spellbook in Offhand at all times.
     * If citizen only carries a Spellbook, it is held in Main Hand forward.
     * Dynamically updates held slots and item slots so they are immediately visible.
     */
    public void equipMageGear() {
        if (this.user == null) return;

        // Equip robes if research tier >= 2
        equipMageRobes();

        int maxAllowedTier = getMaxAllowedTier();
        var inv = this.user.getInventoryCitizen();

        int bestStaffSlot = -1;
        int bestStaffTier = 0;
        int bestBookSlot = -1;
        int bestBookTier = 0;

        for (int i = 0; i < inv.getSlots(); i++) {
            ItemStack stack = inv.getStackInSlot(i);
            if (stack.isEmpty() || !ModIronsEquipmentTypes.isSpellItem(stack)) continue;

            int itemTier = ModIronsEquipmentTypes.getSpellItemLevel(stack);
            if (itemTier > maxAllowedTier) continue;

            if (ModIronsEquipmentTypes.isStaff(stack) && itemTier > bestStaffTier) {
                bestStaffTier = itemTier;
                bestStaffSlot = i;
            }

            if (ModIronsEquipmentTypes.isSpellBook(stack) && itemTier > bestBookTier) {
                bestBookTier = itemTier;
                bestBookSlot = i;
            }
        }

        ItemStack currentMain = this.user.getMainHandItem();
        ItemStack currentOff = this.user.getOffhandItem();

        // Case 1: Both staff and spellbook available -> Staff in Main Hand, Book in Offhand
        if (bestStaffSlot != -1 && bestBookSlot != -1) {
            ItemStack staffStack = inv.getStackInSlot(bestStaffSlot);
            ItemStack bookStack = inv.getStackInSlot(bestBookSlot);

            if (currentMain != staffStack && !ItemStack.isSameItemSameComponents(currentMain, staffStack)) {
                CitizenItemUtils.setHeldItem(this.user, InteractionHand.MAIN_HAND, bestStaffSlot);
                this.user.setItemSlot(EquipmentSlot.MAINHAND, staffStack);
            }
            if (currentOff != bookStack && !ItemStack.isSameItemSameComponents(currentOff, bookStack)) {
                CitizenItemUtils.setHeldItem(this.user, InteractionHand.OFF_HAND, bestBookSlot);
                this.user.setItemSlot(EquipmentSlot.OFFHAND, bookStack);
            }
        }
        // Case 2: Only spellbook available -> Spellbook in Main Hand forward
        else if (bestBookSlot != -1) {
            ItemStack bookStack = inv.getStackInSlot(bestBookSlot);
            if (currentMain != bookStack && !ItemStack.isSameItemSameComponents(currentMain, bookStack)) {
                CitizenItemUtils.setHeldItem(this.user, InteractionHand.MAIN_HAND, bestBookSlot);
                this.user.setItemSlot(EquipmentSlot.MAINHAND, bookStack);
            }
        }
        // Case 3: Only staff available -> Staff in Main Hand
        else if (bestStaffSlot != -1) {
            ItemStack staffStack = inv.getStackInSlot(bestStaffSlot);
            if (currentMain != staffStack && !ItemStack.isSameItemSameComponents(currentMain, staffStack)) {
                CitizenItemUtils.setHeldItem(this.user, InteractionHand.MAIN_HAND, bestStaffSlot);
                this.user.setItemSlot(EquipmentSlot.MAINHAND, staffStack);
            }
        }
    }

    /**
     * Equips Iron's Spells magic robes and armor from citizen inventory.
     * Unlocked strictly at Research Tier 2+ (Arcane Repository).
     */
    public void equipMageRobes() {
        if (this.user == null) return;
        int maxTier = getMaxAllowedTier();
        if (maxTier < 2) return;

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
            baseDelay = 20;
        } else if (mainName.contains("staff") || mainName.contains("blood_staff")) {
            baseDelay = 30;
        }

        if (this.user != null) {
            IColony colony = this.user.getCitizenColonyHandler().getColonyOrRegister();
            if (colony != null && colony.getResearchManager().getResearchEffects().getEffectStrength(EFFECT_MAGE_DAMAGE_4) > 0) {
                baseDelay = (int) (baseDelay * 0.85F);
            }
        }

        MageRobeSchool school = getActiveRobeSchool();
        if (school == MageRobeSchool.LIGHTNING) {
            float reduction = 0.15F + (0.15F * (getRobePieceCount(school) / 4.0F));
            baseDelay = (int) (baseDelay * (1.0F - reduction));
        } else if (school == MageRobeSchool.NETHERITE) {
            baseDelay = (int) (baseDelay * 0.75F);
        } else if (school == MageRobeSchool.ARCANE) {
            baseDelay = (int) (baseDelay * 0.85F);
        }

        return Math.max(10, baseDelay);
    }

    @Override
    protected void doAttack(LivingEntity target) {
        if (target == null || !target.isAlive() || this.user == null) {
            return;
        }

        // 1. Equip gear and verify spellbook is equipped
        equipMageGear();
        ItemStack spellBook = getEquippedSpellBook();
        if (spellBook.isEmpty()) {
            return; // Cannot cast without a Spell Book!
        }

        // 2. Align rotation directly facing target eye position
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

        // 3. Update individual citizen mana and active cooldowns
        long now = this.user.level().getGameTime();
        int elapsed = (int) Math.max(1, Math.min(60, now - this.lastTickTime));
        this.lastTickTime = now;
        updateMana(elapsed);

        MagicData magicData = MagicData.getPlayerMagicData(this.user);

        // 4. Select spell: strictly from inscribed spells in equipped Spell Book
        ISpellContainer container = ISpellContainer.get(spellBook);
        List<SpellSlot> activeSpells = (container != null) ? container.getActiveSpells() : List.of();

        AbstractSpell spellToCast;
        int spellLevel;

        if (activeSpells.isEmpty()) {
            // Baseline spell: Magic Missile LV1 (castable by any Mage with a Spell Book)
            spellToCast = SpellRegistry.MAGIC_MISSILE_SPELL.get();
            spellLevel = 1;
        } else {
            // Select inscribed spell from book (skip spells currently on cooldown if possible)
            SpellSlot chosenSlot = null;
            for (int i = 0; i < activeSpells.size(); i++) {
                SpellSlot slot = activeSpells.get((this.spellCycleIndex + i) % activeSpells.size());
                if (!magicData.getPlayerCooldowns().isOnCooldown(slot.getSpell())) {
                    chosenSlot = slot;
                    this.spellCycleIndex = (this.spellCycleIndex + i + 1) % activeSpells.size();
                    break;
                }
            }
            if (chosenSlot == null) {
                chosenSlot = activeSpells.get(this.spellCycleIndex % activeSpells.size());
                this.spellCycleIndex = (this.spellCycleIndex + 1) % activeSpells.size();
            }
            spellToCast = chosenSlot.getSpell();
            spellLevel = chosenSlot.getLevel();
        }

        // 5. Mana verification
        int manaCost = spellToCast.getManaCost(spellLevel);
        if (magicData.getMana() < manaCost) {
            // Insufficient mana! Wait for mana to regenerate naturally
            return;
        }

        // 6. Swing arm for player-like casting animation
        this.user.swing(InteractionHand.MAIN_HAND);

        // 7. Consume mana
        magicData.addMana(-manaCost);

        // 8. Set targeting data for target-directed spells
        magicData.setAdditionalCastData(new TargetEntityCastData(target));

        // 9. Execute native Iron's Spells cast: spawns actual projectiles, plays native sounds & particle packets
        if (this.user.level() instanceof ServerLevel serverLevel) {
            spellToCast.onCast(serverLevel, spellLevel, this.user, CastSource.SPELLBOOK, magicData);

            // Record cooldown
            int baseCooldown = spellToCast.getSpellCooldown();
            double cdr = this.user.getAttributeValue(AttributeRegistry.COOLDOWN_REDUCTION);
            int effectiveCooldown = (int) (baseCooldown * Math.max(0.2D, 1.0D - (cdr * 0.01D)));
            magicData.getPlayerCooldowns().addCooldown(spellToCast, effectiveCooldown);

            // Apply Robe School specific bonuses
            applyRobeSchoolBonuses(serverLevel, target);
        }
    }

    /**
     * Applies school-specific robe buffs unlocked at Tier 2+.
     */
    private void applyRobeSchoolBonuses(ServerLevel serverLevel, LivingEntity target) {
        MageRobeSchool school = getActiveRobeSchool();
        if (school == MageRobeSchool.NONE || getMaxAllowedTier() < 2) return;

        int pieceCount = getRobePieceCount(school);
        Vec3 citizenPos = this.user.getEyePosition();
        Vec3 targetPos = target.getEyePosition();

        switch (school) {
            case FIRE -> {
                target.igniteForSeconds(4 + pieceCount);
                this.user.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 120, 0, false, false));
                serverLevel.sendParticles(ParticleTypes.FLAME, citizenPos.x, citizenPos.y + 0.2D, citizenPos.z, 6, 0.2D, 0.2D, 0.2D, 0.02D);
            }
            case ICE -> {
                target.setTicksFrozen(140 + (pieceCount * 30));
                target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60 + (pieceCount * 15), 1));
                this.user.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 100, 0, false, false));
                serverLevel.sendParticles(ParticleTypes.SNOWFLAKE, citizenPos.x, citizenPos.y + 0.2D, citizenPos.z, 6, 0.2D, 0.2D, 0.2D, 0.02D);
            }
            case LIGHTNING -> {
                this.user.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 100, 0, false, false));
                serverLevel.sendParticles(ParticleTypes.ELECTRIC_SPARK, citizenPos.x, citizenPos.y + 0.2D, citizenPos.z, 6, 0.2D, 0.2D, 0.2D, 0.05D);
            }
            case HOLY -> {
                if (target.isInvertedHealAndHarm()) {
                    serverLevel.sendParticles(ParticleTypes.FLASH, targetPos.x, targetPos.y + 0.5D, targetPos.z, 1, 0, 0, 0, 0);
                }
                this.user.heal(1.0F + (pieceCount * 0.75F));
                this.user.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 60, 0, false, false));
                serverLevel.sendParticles(ParticleTypes.HAPPY_VILLAGER, citizenPos.x, citizenPos.y + 0.2D, citizenPos.z, 5, 0.2D, 0.2D, 0.2D, 0.02D);
            }
            case BLOOD -> {
                target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 60 + (pieceCount * 15), 0));
                this.user.heal(2.0F + (pieceCount * 0.5F));
                serverLevel.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, citizenPos.x, citizenPos.y + 0.2D, citizenPos.z, 5, 0.2D, 0.2D, 0.2D, 0.02D);
            }
            case EVOCATION -> {
                target.addEffect(new MobEffectInstance(MobEffects.POISON, 80, 1));
                serverLevel.sendParticles(ParticleTypes.WITCH, citizenPos.x, citizenPos.y + 0.2D, citizenPos.z, 6, 0.2D, 0.2D, 0.2D, 0.02D);
            }
            case ENDER -> {
                this.user.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 100, 1, false, false));
                serverLevel.sendParticles(ParticleTypes.PORTAL, citizenPos.x, citizenPos.y + 0.2D, citizenPos.z, 8, 0.2D, 0.2D, 0.2D, 0.05D);
                if (this.user.getHealth() < this.user.getMaxHealth() * 0.5F && this.user.distanceToSqr(target) < 25.0D) {
                    Vec3 away = citizenPos.subtract(targetPos).normalize().scale(5.0D);
                    this.user.teleportTo(citizenPos.x + away.x, citizenPos.y, citizenPos.z + away.z);
                    serverLevel.playSound(null, this.user.blockPosition(), SoundEvents.ENDERMAN_TELEPORT, SoundSource.NEUTRAL, 1.0F, 1.0F);
                }
            }
            case ARCANE -> {
                this.user.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 80, 0, false, false));
                serverLevel.sendParticles(ParticleTypes.ENCHANT, citizenPos.x, citizenPos.y + 0.2D, citizenPos.z, 6, 0.2D, 0.2D, 0.2D, 0.02D);
            }
            case NETHERITE -> {
                this.user.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 120, 1, false, false));
                this.user.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 120, 0, false, false));
                serverLevel.sendParticles(ParticleTypes.DRAGON_BREATH, citizenPos.x, citizenPos.y + 0.2D, citizenPos.z, 6, 0.2D, 0.2D, 0.2D, 0.02D);
            }
            default -> {}
        }
    }
}
