package com.minecoloniesirons.colony.equipment;

import com.minecolonies.api.equipment.registry.EquipmentTypeEntry;
import com.minecolonies.apiimp.CommonMinecoloniesAPIImpl;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModIronsEquipmentTypes {

    public static final String MODID = "minecolonies_irons";

    // Item Tags for Iron's Arms 'n Artifice Firearms & Ammunition (strictly Iron's Artifice only)
    public static final TagKey<Item> TAG_IRONS_ARTIFICE_GUNS = TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath("irons_artifice", "guns"));
    public static final TagKey<Item> TAG_IRONS_ARTIFICE_BULLETS = TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath("irons_artifice", "bullets"));

    // Item Tags for Iron's Spells 'n Spellbooks
    public static final TagKey<Item> TAG_IRONS_SPELL_BOOKS = TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath("irons_spellbooks", "spell_books"));
    public static final TagKey<Item> TAG_IRONS_STAVES = TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath("irons_spellbooks", "staves"));
    public static final TagKey<Item> TAG_CURIOS_SPELLBOOK = TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath("curios", "spellbook"));

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

    /**
     * Checks if the stack is a valid firearm from Iron's Arms 'n Artifice.
     */
    public static boolean isGunItem(ItemStack stack) {
        if (stack.isEmpty()) return false;

        // Check official weapon tag
        if (stack.is(TAG_IRONS_ARTIFICE_GUNS)) {
            return true;
        }

        ResourceLocation key = BuiltInRegistries.ITEM.getKey(stack.getItem());
        if (!key.getNamespace().equals("irons_artifice")) {
            return false;
        }

        String path = key.getPath();
        // Exclude non-weapon artifice items
        if (path.contains("bullet") || path.contains("powder") || path.contains("component")
                || path.contains("modifier") || path.contains("hat") || path.contains("box")
                || path.contains("container") || path.contains("sheet") || path.contains("gear")
                || path.contains("ingot") || path.contains("scrap")) {
            return false;
        }

        return path.contains("pistol") || path.contains("revolver") || path.contains("six_shooter")
                || path.contains("musket") || path.contains("blunderbuss")
                || path.contains("arquebus") || path.contains("clockwork") || path.contains("rifle");
    }

    /**
     * Returns weapon tier for firearms (1 to 3).
     */
    public static int getGunLevel(ItemStack stack) {
        String id = stack.getItem().toString().toLowerCase();
        // Tier 3: Arquebus, Clockwork Rifle
        if (id.contains("arquebus") || id.contains("clockwork") || id.contains("rifle")) return 3;
        // Tier 2: Musket, Blunderbuss
        if (id.contains("musket") || id.contains("blunderbuss")) return 2;
        // Tier 1: Flintlock Pistol, Blackpowder Revolver, Six Shooter
        return 1;
    }

    /**
     * Checks if the stack is ammunition for firearms.
     * Strictly restricted exclusively to Iron's Arms 'n Artifice ammunition.
     */
    public static boolean isAmmoItem(ItemStack stack) {
        if (stack.isEmpty()) return false;

        ResourceLocation key = BuiltInRegistries.ITEM.getKey(stack.getItem());
        if (!key.getNamespace().equals("irons_artifice")) {
            return false;
        }

        if (stack.is(TAG_IRONS_ARTIFICE_BULLETS)) {
            return true;
        }

        String path = key.getPath();
        return path.equals("bullet") || path.equals("bullet_box") || path.contains("bullet");
    }

    /**
     * Returns the preferred item stack to request for firearm ammunition.
     * Strictly requests Iron's Artifice bullet.
     */
    public static ItemStack getBulletRequestStack() {
        ResourceLocation bulletId = ResourceLocation.fromNamespaceAndPath("irons_artifice", "bullet");
        Item bulletItem = BuiltInRegistries.ITEM.get(bulletId);
        if (bulletItem != null && bulletItem != Items.AIR) {
            return new ItemStack(bulletItem);
        }
        return ItemStack.EMPTY;
    }

    /**
     * Returns the preferred item stack to request for a firearm.
     * Requests Iron's Artifice flintlock pistol as the standard baseline weapon.
     */
    public static ItemStack getGunRequestStack() {
        ResourceLocation gunId = ResourceLocation.fromNamespaceAndPath("irons_artifice", "flintlock_pistol");
        Item gunItem = BuiltInRegistries.ITEM.get(gunId);
        if (gunItem != null && gunItem != Items.AIR) {
            return new ItemStack(gunItem);
        }
        return ItemStack.EMPTY;
    }

    /**
     * Returns the preferred item stack to request for a spell focus.
     * Requests Iron's Spells flimsy journal as the standard baseline spellbook.
     */
    public static ItemStack getSpellFocusRequestStack() {
        ResourceLocation bookId = ResourceLocation.fromNamespaceAndPath("irons_spellbooks", "flimsy_journal");
        Item bookItem = BuiltInRegistries.ITEM.get(bookId);
        if (bookItem != null && bookItem != Items.AIR) {
            return new ItemStack(bookItem);
        }
        return ItemStack.EMPTY;
    }

    /**
     * Checks if the stack is a valid spellbook, staff, wand, or spellcasting weapon from Iron's Spells 'n Spellbooks.
     * Strictly rejects workstations, anvils, tables, cauldrons, materials, scrolls, and armor.
     */
    public static boolean isSpellItem(ItemStack stack) {
        if (stack.isEmpty()) return false;

        ResourceLocation key = BuiltInRegistries.ITEM.getKey(stack.getItem());
        if (!key.getNamespace().equals("irons_spellbooks")) {
            return false;
        }

        String path = key.getPath();

        // Strictly exclude functional blocks, workstations, scrolls, inks, crafting materials, and armor
        if (path.contains("anvil") || path.contains("forge") || path.contains("table")
                || path.contains("cauldron") || path.contains("scroll") || path.contains("ink")
                || path.contains("rune") || path.contains("essence") || path.contains("ingot")
                || path.contains("scrap") || path.contains("helmet") || path.contains("chestplate")
                || path.contains("leggings") || path.contains("boots") || path.contains("robe")
                || path.contains("curio") || path.contains("ring") || path.contains("necklace")
                || path.contains("upgrade_orb") || path.contains("shriving_stone") || path.contains("cinder_essence")) {
            return false;
        }

        // Priority 1: Check official tags
        if (stack.is(TAG_IRONS_SPELL_BOOKS) || stack.is(TAG_IRONS_STAVES) || stack.is(TAG_CURIOS_SPELLBOOK)) {
            return true;
        }

        // Priority 2: Whitelist exact weapon and spellbook identifiers
        return path.endsWith("_spell_book") || path.endsWith("_staff") || path.endsWith("_wand")
                || path.endsWith("_cane") || path.endsWith("_rapier") || path.endsWith("_flamberge")
                || path.equals("flimsy_journal") || path.equals("villager_bible") || path.equals("ancient_codex")
                || path.equals("spellbreaker") || path.equals("the_blood_staff");
    }

    /**
     * Returns weapon tier for magic items (1 to 5).
     */
    public static int getSpellItemLevel(ItemStack stack) {
        String id = stack.getItem().toString().toLowerCase();
        // Tier 5: Ancient Codex, Necronomicon, The Blood Staff, Decrepit Flamberge
        if (id.contains("ancient") || id.contains("codex") || id.contains("necronomicon")
                || id.contains("blood_staff") || id.contains("flamberge")) return 5;
        // Tier 4: Villager Bible, Evoker Spell Book, Dragonskin Book, Vampiric Book, Hither-Thither Wand, Spellbreaker
        if (id.contains("bible") || id.contains("evoker") || id.contains("dragonskin")
                || id.contains("vampiric") || id.contains("hither") || id.contains("spellbreaker")) return 4;
        // Tier 3: Enchanted Spell Book, Blaze Manual, Frostbranded Book, Druidic Tome, Ice Staff, Lightning Rod, Pyrium Staff
        if (id.contains("enchanted") || id.contains("blaze") || id.contains("frost")
                || id.contains("druid") || id.contains("lightning") || id.contains("pyrium")) return 3;
        // Tier 2: Apprentice's Spell Book, Ironbound Tome, Artificer's Cane, Amethyst Rapier
        if (id.contains("apprentice") || id.contains("ironbound") || id.contains("artificer")
                || id.contains("rapier")) return 2;
        // Tier 1: Flimsy Journal, Rotten Spell Book, Graybeard Staff
        return 1;
    }

    private ModIronsEquipmentTypes() {}
}
