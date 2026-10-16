package com.airship;

import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.EntityTypeTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Which projectiles pop which balloon.
 * <ul>
 *   <li>Simple Balloon: every projectile (snowballs, eggs, arrows ...).</li>
 *   <li>Balloon: projectiles in the tag {@code airship:pops_balloon} (arrows, tridents, fireballs ...).</li>
 *   <li>Reinforced Balloon: projectiles in the tag {@code airship:pops_reinforced_balloon} (tridents) and
 *       arrows shot from a crossbow with Piercing {@value #REINFORCED_PIERCING} or higher.</li>
 * </ul>
 * Both tags are in the mod's data, so a data pack can change them.
 */
public final class AirshipBalloons {
    public static final TagKey<EntityType<?>> POPS_BALLOON = tag("pops_balloon");
    public static final TagKey<EntityType<?>> POPS_REINFORCED_BALLOON = tag("pops_reinforced_balloon");
    /** Lowest Piercing level of a crossbow whose arrows pop a Reinforced Balloon. */
    public static final int REINFORCED_PIERCING = 3;

    private AirshipBalloons() {}

    private static TagKey<EntityType<?>> tag(String name) {
        return TagKey.create(Registries.ENTITY_TYPE, Identifier.fromNamespaceAndPath(AirshipMod.MOD_ID, name));
    }

    public static boolean isBalloon(BlockState state) {
        return state.is(ModBlocks.AIRSHIP_BALLOON_SIMPLE) || state.is(ModBlocks.AIRSHIP_BALLOON)
                || state.is(ModBlocks.AIRSHIP_BALLOON_REINFORCED);
    }

    /** True if this projectile destroys this balloon. */
    public static boolean pops(BlockState balloon, Projectile projectile) {
        if (balloon.is(ModBlocks.AIRSHIP_BALLOON_SIMPLE)) {
            return true;
        }
        boolean strongest = hasTag(projectile, POPS_REINFORCED_BALLOON)
                || piercingLevel(projectile) >= REINFORCED_PIERCING;
        if (balloon.is(ModBlocks.AIRSHIP_BALLOON)) {
            return strongest || hasTag(projectile, POPS_BALLOON);
        }
        if (balloon.is(ModBlocks.AIRSHIP_BALLOON_REINFORCED)) {
            return strongest;
        }
        return false;
    }

    /** True if the projectile's entity type is in the tag. */
    public static boolean hasTag(Projectile projectile, TagKey<EntityType<?>> tag) {
        EntityType<?> type = projectile.getType();
        for (Holder<EntityType<?>> holder : BuiltInRegistries.ENTITY_TYPE.getTagOrEmpty(tag)) {
            if (holder.value() == type) {
                return true;
            }
        }
        return false;
    }

    /** Piercing level of the crossbow an arrow was shot from (0 for everything else). */
    public static int piercingLevel(Projectile projectile) {
        if (!hasTag(projectile, EntityTypeTags.ARROWS)) {
            return 0;
        }
        ItemStack weapon = projectile.getWeaponItem();
        if (weapon == null || !weapon.is(Items.CROSSBOW)) {
            return 0;
        }
        Holder<Enchantment> piercing = projectile.registryAccess()
                .lookupOrThrow(Registries.ENCHANTMENT)
                .getOrThrow(Enchantments.PIERCING);
        return EnchantmentHelper.getItemEnchantmentLevel(piercing, weapon);
    }
}
