package com.minecoloniesirons.colony.equipment;

import com.minecolonies.api.equipment.registry.EquipmentTypeEntry;
import com.minecolonies.apiimp.CommonMinecoloniesAPIImpl;
import io.redspace.ironsspellbooks.api.spells.ISpellContainer;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModIronsEquipmentTypes {

    public static final String MODID = "minecolonies_irons";

    // Item Tags for Iron's Arms 'n Artifice Firearms & Ammunition (strictly Iron's Artifice only)
    public static final TagKey<Item> TAG_IRONS_ARTIFICE_GUNS = TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath("irons_artifice", "guns"));
    public static final TagKey<Item> TAG_IRONS_ARTIFICE_BULLETS = TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath("irons_artifice", "ammo"));

    // Item Tags for Iron's Spells 'n Spellbooks
    public static final TagKey<Item> TAG_IRONS_STAFF = TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath("irons_spellbooks", "staff"));
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
                || path.contains("ingot") || path.contains("scrap") || path.contains("egg")
                || path.contains("tricorne")) {
            return false;
        }

        return path.contains("flintlock") || path.contains("pistol") || path.contains("revolver")
                || path.contains("six_shooter") || path.contains("musket") || path.contains("blunderbuss")
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
        // Tier 1: Flintlock, Blackpowder Revolver, Six Shooter
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
     * Requests Iron's Artifice flintlock as the standard baseline weapon.
     */
    public static ItemStack getGunRequestStack() {
        ResourceLocation gunId = ResourceLocation.fromNamespaceAndPath("irons_artifice", "flintlock");
        Item gunItem = BuiltInRegistries.ITEM.get(gunId);
        if (gunItem != null && gunItem != Items.AIR) {
            return new ItemStack(gunItem);
        }
        return ItemStack.EMPTY;
    }

    /**
     * Returns the preferred item stack to request for a spell focus.
     * Requests Iron's Spells copper spell book as the standard baseline spellbook.
     */
    public static ItemStack getSpellFocusRequestStack() {
        ResourceLocation bookId = ResourceLocation.fromNamespaceAndPath("irons_spellbooks", "copper_spell_book");
        Item bookItem = BuiltInRegistries.ITEM.get(bookId);
        if (bookItem != null && bookItem != Items.AIR) {
            return new ItemStack(bookItem);
        }
        return ItemStack.EMPTY;
    }

    /**
     * Checks if the stack is a valid spellbook from Iron's Spells 'n Spellbooks.
     */
    public static boolean isSpellBook(ItemStack stack) {
        if (stack.isEmpty()) return false;
        if (stack.is(TAG_CURIOS_SPELLBOOK)) return true;
        if (ISpellContainer.isSpellContainer(stack)) return true;
        ResourceLocation key = BuiltInRegistries.ITEM.getKey(stack.getItem());
        if (!key.getNamespace().equals("irons_spellbooks")) return false;
        String path = key.getPath();
        return path.endsWith("_spell_book") || path.equals("wimpy_spell_book");
    }

    /**
     * Checks if the stack is a valid staff, wand, or spellcasting weapon.
     */
    public static boolean isStaff(ItemStack stack) {
        if (stack.isEmpty()) return false;
        if (stack.is(TAG_IRONS_STAFF)) return true;
        ResourceLocation key = BuiltInRegistries.ITEM.getKey(stack.getItem());
        if (!key.getNamespace().equals("irons_spellbooks")) return false;
        String path = key.getPath();
        return path.endsWith("_staff") || path.equals("artificer_cane") || path.equals("graybeard_staff")
                || path.equals("lightning_rod") || path.equals("hither_thither_wand")
                || path.equals("magehunter") || path.equals("spellbreaker") || path.equals("amethyst_rapier")
                || path.equals("keeper_flamberge") || path.equals("claymore") || path.equals("misery")
                || path.equals("legionnaire_flamberge") || path.equals("decrepit_scythe") || path.equals("hellrazor")
                || path.equals("boreal_blade") || path.equals("twilight_gale");
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
                || path.contains("upgrade_orb") || path.contains("shriving_stone") || path.contains("cinder_essence")
                || path.contains("bookshelf") || path.contains("raw_") || path.contains("ore")
                || path.contains("salvage") || path.contains("book_stack") || path.contains("logbook")
                || path.contains("bottle") || path.contains("crystal") || path.contains("pouch")) {
            return false;
        }

        return isSpellBook(stack) || isStaff(stack);
    }

    /**
     * Returns weapon tier for magic items (1 to 5).
     */
    public static int getSpellItemLevel(ItemStack stack) {
        String id = stack.getItem().toString().toLowerCase();
        // Tier 5: Netherite Spell Book, Legendary Spell Book, Necronomicon, The Blood Staff, Decrepit Scythe, Twilight Gale, Boreal Blade
        if (id.contains("netherite") || id.contains("legendary") || id.contains("necronomicon")
                || id.contains("blood_staff") || id.contains("scythe") || id.contains("twilight")
                || id.contains("boreal")) return 5;
        // Tier 4: Diamond Spell Book, Dragonskin Book, Villager Book, Evoker Book, Spellbreaker, Hither-Thither Wand, Hellrazor
        if (id.contains("diamond") || id.contains("dragonskin") || id.contains("villager")
                || id.contains("evoker") || id.contains("hither") || id.contains("spellbreaker")
                || id.contains("hellrazor")) return 4;
        // Tier 3: Gold Spell Book, Blaze Book, Druidic Book, Ice Spell Book, Cursed Doll Book, Pyrium Staff, Lightning Rod, Keeper Flamberge, Magehunter
        if (id.contains("gold") || id.contains("blaze") || id.contains("druidic")
                || id.contains("ice_spell_book") || id.contains("ice_staff") || id.contains("cursed_doll")
                || id.contains("lightning") || id.contains("pyrium") || id.contains("keeper")
                || id.contains("magehunter")) return 3;
        // Tier 2: Iron Spell Book, Apprentice, Artificer's Cane, Amethyst Rapier, Misery, Legionnaire Flamberge, Claymore
        if (id.contains("iron_spell_book") || id.contains("artificer") || id.contains("rapier")
                || id.contains("misery") || id.contains("legionnaire") || id.contains("claymore")) return 2;
        // Tier 1: Copper Spell Book, Rotten Spell Book, Wimpy Spell Book, Graybeard Staff
        return 1;
    }

    /**
     * Checks if the stack is a valid magic robe or mage armor piece from Iron's Spells 'n Spellbooks.
     */
    public static boolean isRobeItem(ItemStack stack) {
        if (stack.isEmpty()) return false;
        ResourceLocation key = BuiltInRegistries.ITEM.getKey(stack.getItem());
        if (!key.getNamespace().equals("irons_spellbooks")) return false;
        String path = key.getPath();
        return path.endsWith("_helmet") || path.endsWith("_chestplate")
                || path.endsWith("_leggings") || path.endsWith("_boots")
                || path.endsWith("_hat") || path.endsWith("_crown");
    }

    /**
     * Checks if the stack is a valid robe/armor piece for the specified EquipmentSlot.
     */
    public static boolean isRobeItemForSlot(ItemStack stack, EquipmentSlot slot) {
        if (!isRobeItem(stack)) return false;
        String path = BuiltInRegistries.ITEM.getKey(stack.getItem()).getPath();
        return switch (slot) {
            case HEAD -> path.endsWith("_helmet") || path.endsWith("_hat") || path.endsWith("_crown");
            case CHEST -> path.endsWith("_chestplate");
            case LEGS -> path.endsWith("_leggings");
            case FEET -> path.endsWith("_boots");
            default -> false;
        };
    }

    /**
     * Returns the armor tier of the robe (Tier 2 to 5).
     * Level 2 research unlocks Tier 2 robes.
     */
    public static int getRobeTier(ItemStack stack) {
        if (!isRobeItem(stack)) return 0;
        String path = BuiltInRegistries.ITEM.getKey(stack.getItem()).getPath();
        if (path.contains("netherite")) return 5;
        if (path.contains("cultist") || path.contains("plagued") || path.contains("shadowwalker")
                || path.contains("archevoker") || path.contains("infernal") || path.contains("paladin")) return 4;
        if (path.contains("pyromancer") || path.contains("cryomancer") || path.contains("electromancer")
                || path.contains("priest")) return 3;
        // Tier 2: wandering_magician, wizard, pumpkin, tarnished, speed_boots
        return 2;
    }

    /**
     * Returns the magic school associated with the robe.
     */
    public static MageRobeSchool getRobeSchool(ItemStack stack) {
        if (!isRobeItem(stack)) return MageRobeSchool.NONE;
        String path = BuiltInRegistries.ITEM.getKey(stack.getItem()).getPath();
        if (path.contains("netherite")) return MageRobeSchool.NETHERITE;
        if (path.contains("pyromancer") || path.contains("infernal")) return MageRobeSchool.FIRE;
        if (path.contains("cryomancer")) return MageRobeSchool.ICE;
        if (path.contains("electromancer")) return MageRobeSchool.LIGHTNING;
        if (path.contains("priest") || path.contains("paladin")) return MageRobeSchool.HOLY;
        if (path.contains("cultist")) return MageRobeSchool.BLOOD;
        if (path.contains("archevoker") || path.contains("plagued")) return MageRobeSchool.EVOCATION;
        if (path.contains("shadowwalker")) return MageRobeSchool.ENDER;
        return MageRobeSchool.ARCANE;
    }

    private ModIronsEquipmentTypes() {}
}
