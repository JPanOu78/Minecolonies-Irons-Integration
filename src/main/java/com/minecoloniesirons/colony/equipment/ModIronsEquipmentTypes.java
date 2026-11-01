package com.minecoloniesirons.colony.equipment;

import com.minecolonies.api.equipment.registry.EquipmentTypeEntry;
import com.minecolonies.apiimp.CommonMinecoloniesAPIImpl;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModIronsEquipmentTypes {

    public static final String MODID = "minecolonies_irons";

    // Item Tags for Firearms
    public static final TagKey<Item> TAG_IRONS_ARTIFICE_GUNS = TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath("irons_artifice", "guns"));
    public static final TagKey<Item> TAG_COMMON_GUNS = TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath("c", "guns"));
    public static final TagKey<Item> TAG_MINECOLONIES_GUNS = TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath(MODID, "guns"));

    // Item Tags for Magic / Spell Focus
    public static final TagKey<Item> TAG_IRONS_SPELL_BOOKS = TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath("irons_spellbooks", "spell_books"));
    public static final TagKey<Item> TAG_IRONS_STAVES = TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath("irons_spellbooks", "staves"));
    public static final TagKey<Item> TAG_CURIO_SPELLBOOK = TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath("curios", "spellbook"));
    public static final TagKey<Item> TAG_COMMON_SPELL_FOCUS = TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath("c", "spell_focus"));
    public static final TagKey<Item> TAG_MINECOLONIES_SPELL_FOCUS = TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath(MODID, "spell_focus"));

    public static final DeferredRegister<EquipmentTypeEntry> DEFERRED_REGISTER =
            DeferredRegister.create(CommonMinecoloniesAPIImpl.EQUIPMENT_TYPES, MODID);

    public static final DeferredHolder<EquipmentTypeEntry, EquipmentTypeEntry> GUN =
            DEFERRED_REGISTER.register("gun", () ->
                    new EquipmentTypeEntry.Builder()
                            .setRegistryName(ResourceLocation.fromNamespaceAndPath(MODID, "gun"))
                            .setDisplayName(Component.translatable("com.minecolonies_irons.equipment.gun"))
                            .setIsEquipment((stack, entry) -> isGunItem(stack))
                            .setEquipmentLevel((stack, entry) -> getGunLevel(stack))
                            .build()
            );

    public static final DeferredHolder<EquipmentTypeEntry, EquipmentTypeEntry> SPELL_FOCUS =
            DEFERRED_REGISTER.register("spell_focus", () ->
                    new EquipmentTypeEntry.Builder()
                            .setRegistryName(ResourceLocation.fromNamespaceAndPath(MODID, "spell_focus"))
                            .setDisplayName(Component.translatable("com.minecolonies_irons.equipment.spell_focus"))
                            .setIsEquipment((stack, entry) -> isSpellItem(stack))
                            .setEquipmentLevel((stack, entry) -> getSpellItemLevel(stack))
                            .build()
            );

    public static boolean isGunItem(ItemStack stack) {
        if (stack.isEmpty()) return false;
        // Priority 1: Check standard & custom Item Tags
        if (stack.is(TAG_IRONS_ARTIFICE_GUNS) || stack.is(TAG_COMMON_GUNS) || stack.is(TAG_MINECOLONIES_GUNS)) {
            return true;
        }
        // Priority 2: Fallback check by item name / registry path
        String id = stack.getItem().toString().toLowerCase();
        return id.contains("pistol") || id.contains("revolver") || id.contains("musket")
                || id.contains("blunderbuss") || id.contains("arquebus") || id.contains("rifle")
                || id.contains("six_shooter") || id.contains("artifice");
    }

    public static int getGunLevel(ItemStack stack) {
        String id = stack.getItem().toString().toLowerCase();
        // Tier 3: Arquebus, Clockwork Rifle
        if (id.contains("arquebus") || id.contains("clockwork") || id.contains("rifle")) return 3;
        // Tier 2: Musket, Blunderbuss
        if (id.contains("musket") || id.contains("blunderbuss")) return 2;
        // Tier 1: Flintlock Pistol, Blackpowder Revolver, Six Shooter
        return 1;
    }

    public static boolean isSpellItem(ItemStack stack) {
        if (stack.isEmpty()) return false;
        // Priority 1: Check standard & custom Item Tags
        if (stack.is(TAG_IRONS_SPELL_BOOKS) || stack.is(TAG_IRONS_STAVES)
                || stack.is(TAG_CURIO_SPELLBOOK) || stack.is(TAG_COMMON_SPELL_FOCUS)
                || stack.is(TAG_MINECOLONIES_SPELL_FOCUS)) {
            return true;
        }
        // Priority 2: Fallback check by item name / registry path
        String id = stack.getItem().toString().toLowerCase();
        return id.contains("staff") || id.contains("wand") || id.contains("cane")
                || id.contains("spell") || id.contains("tome") || id.contains("grimoire")
                || id.contains("codex") || id.contains("journal") || id.contains("bible")
                || id.contains("rapier") || id.contains("flamberge")
                || id.contains("irons_spellbooks");
    }

    public static int getSpellItemLevel(ItemStack stack) {
        String id = stack.getItem().toString().toLowerCase();
        // Tier 5: Ancient Codex, Necronomicon, The Blood Staff, Decrepit Flamberge
        if (id.contains("ancient") || id.contains("codex") || id.contains("necronomicon")
                || id.contains("blood_staff") || id.contains("flamberge")) return 5;
        // Tier 4: Villager Bible, Grimoire of Evokation, Dragonskin Book, Vampiric Book, Hither-Thither Wand, Spellbreaker
        if (id.contains("bible") || id.contains("evokation") || id.contains("dragonskin")
                || id.contains("vampiric") || id.contains("hither") || id.contains("spellbreaker")) return 4;
        // Tier 3: Enchanted Spell Book, Blaze Manual, Frostbranded Book, Druidic Tome, Ice Staff, Rod o' Lightning, Pyrium Staff
        if (id.contains("enchanted") || id.contains("blaze") || id.contains("frost")
                || id.contains("druid") || id.contains("lightning") || id.contains("pyrium")) return 3;
        // Tier 2: Apprentice's Spell Book, Ironbound Tome, Artificer's Cane, Amethyst Rapier
        if (id.contains("apprentice") || id.contains("ironbound") || id.contains("artificer")
                || id.contains("rapier")) return 2;
        // Tier 1: Flimsy Journal, Rotten Spell Book, Graybeard Staff, Wooden/Basic Wands
        return 1;
    }

    private ModIronsEquipmentTypes() {}
}
