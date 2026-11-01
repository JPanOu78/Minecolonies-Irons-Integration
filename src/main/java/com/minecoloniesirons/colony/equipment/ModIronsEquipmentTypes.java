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

    // Official Item Tag for Iron's Arms 'n Artifice Firearms
    public static final TagKey<Item> TAG_IRONS_ARTIFICE_GUNS = TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath("irons_artifice", "guns"));

    // Official Item Tags for Iron's Spells 'n Spellbooks
    public static final TagKey<Item> TAG_IRONS_SPELL_BOOKS = TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath("irons_spellbooks", "spell_books"));
    public static final TagKey<Item> TAG_IRONS_STAVES = TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath("irons_spellbooks", "staves"));

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
        // Strictly check Iron's Artifice official weapon tag
        if (stack.is(TAG_IRONS_ARTIFICE_GUNS)) {
            return true;
        }
        // Direct Iron's Artifice item check fallback
        ResourceLocation key = net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(stack.getItem());
        if (!key.getNamespace().equals("irons_artifice")) {
            return false;
        }
        String path = key.getPath();
        return path.contains("pistol") || path.contains("revolver") || path.contains("musket")
                || path.contains("blunderbuss") || path.contains("arquebus") || path.contains("rifle")
                || path.contains("six_shooter");
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
        // Strictly check Iron's Spells official weapon tags
        if (stack.is(TAG_IRONS_SPELL_BOOKS) || stack.is(TAG_IRONS_STAVES)) {
            return true;
        }
        // Direct Iron's Spells item check fallback
        ResourceLocation key = net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(stack.getItem());
        if (!key.getNamespace().equals("irons_spellbooks")) {
            return false;
        }
        String path = key.getPath();
        return path.contains("staff") || path.contains("wand") || path.contains("cane")
                || path.contains("spell") || path.contains("tome") || path.contains("grimoire")
                || path.contains("codex") || path.contains("journal") || path.contains("bible")
                || path.contains("rapier") || path.contains("flamberge");
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
