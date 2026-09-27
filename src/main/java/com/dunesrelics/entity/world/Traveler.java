package com.dunesrelics.entity.world;

import com.dunesrelics.DunesRelics;
import com.dunesrelics.registry.ModBlocks;
import com.dunesrelics.registry.ModItems;
import com.dunesrelics.registry.VolcanicBlocks;
import com.dunesrelics.registry.VolcanicItems;
import com.dunesrelics.registry.WorldBlocks;
import com.dunesrelics.registry.WorldItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.StructureTags;
import net.minecraft.tags.TagKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.npc.VillagerTrades;
import net.minecraft.world.entity.npc.WanderingTrader;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.MapItem;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.MerchantOffers;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.saveddata.maps.MapDecoration;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;
import net.minecraftforge.common.BasicItemListing;
import org.jetbrains.annotations.Nullable;

/**
 * A traveller who walks the roads the world grows around a player's home. He sells maps to far-off places and goods
 * from the other lands of the mod, buys provisions for the road, and moves on after a day.
 */
public class Traveler extends WanderingTrader {
    public static final TagKey<Structure> VOLCANO_MAPS = TagKey.create(Registries.STRUCTURE, DunesRelics.id("on_traveler_maps/volcano"));
    public static final TagKey<Structure> PIRATE_MAPS = TagKey.create(Registries.STRUCTURE, DunesRelics.id("on_traveler_maps/pirate_ship"));

    public Traveler(EntityType<? extends WanderingTrader> type, Level level) {
        super(type, level);
    }

    private static VillagerTrades.ItemListing sell(ItemLike item, int count, int emeralds, int uses) {
        return new BasicItemListing(emeralds, new ItemStack(item, count), uses, 1, 0.05F);
    }

    private static VillagerTrades.ItemListing buy(ItemLike item, int count, int emeralds, int uses) {
        return new BasicItemListing(new ItemStack(item, count), new ItemStack(Items.EMERALD, emeralds), uses, 1, 0.05F);
    }

    private static final VillagerTrades.ItemListing[] GOODS = {
            sell(ModItems.DATES.get(), 6, 1, 8),
            sell(ModItems.AMBER.get(), 2, 3, 6),
            sell(ModBlocks.PALM_SAPLING.get(), 1, 4, 4),
            sell(VolcanicBlocks.EMBER_SAPLING.get(), 1, 6, 4),
            sell(VolcanicItems.FIRE_PEPPER.get(), 3, 2, 8),
            sell(VolcanicItems.SULFUR.get(), 4, 2, 8),
            sell(WorldItems.PEARL.get(), 1, 5, 4),
            sell(WorldBlocks.SEASHELL.get(), 3, 1, 8),
            sell(WorldItems.FLOUR.get(), 6, 1, 8),
            sell(WorldItems.TIDE_CLOCK.get(), 1, 8, 2),
            sell(WorldItems.MESSAGE_IN_A_BOTTLE.get(), 1, 6, 2),
            sell(ModBlocks.DESERT_ROSE.get(), 1, 2, 6),
    };

    private static final VillagerTrades.ItemListing[] PROVISIONS = {
            buy(Items.BREAD, 6, 1, 8),
            buy(Items.COOKED_BEEF, 4, 1, 8),
            buy(Items.APPLE, 6, 1, 8),
            buy(Items.LEATHER, 4, 1, 8),
    };

    private static final VillagerTrades.ItemListing[] MAPS = {
            new StructureMap(VOLCANO_MAPS, "filled_map.dunesrelics.volcano", 14),
            new StructureMap(PIRATE_MAPS, "filled_map.dunesrelics.pirate_ship", 12),
            new StructureMap(StructureTags.VILLAGE, "filled_map.dunesrelics.village", 6),
    };

    @Override
    protected void updateTrades() {
        MerchantOffers offers = this.getOffers();
        this.addOffersFromItemListings(offers, GOODS, 5);
        this.addOffersFromItemListings(offers, PROVISIONS, 2);
        this.addOffersFromItemListings(offers, MAPS, 1);
    }

    /** Sells an explorer map to the nearest structure of a tag, like the cartographer does. */
    public record StructureMap(TagKey<Structure> destination, String nameKey, int emeralds) implements VillagerTrades.ItemListing {
        @Nullable
        @Override
        public MerchantOffer getOffer(Entity trader, RandomSource random) {
            if (!(trader.level() instanceof ServerLevel level)) {
                return null;
            }
            BlockPos pos = level.findNearestMapStructure(this.destination, trader.blockPosition(), 100, true);
            if (pos == null) {
                return null;
            }
            ItemStack map = MapItem.create(level, pos.getX(), pos.getZ(), (byte) 2, true, true);
            MapItem.renderBiomePreviewMap(level, map);
            MapItemSavedData.addTargetDecoration(map, pos, "+", MapDecoration.Type.RED_X);
            map.setHoverName(Component.translatable(this.nameKey));
            return new MerchantOffer(new ItemStack(Items.EMERALD, this.emeralds), new ItemStack(Items.COMPASS), map, 1, 5, 0.2F);
        }
    }
}
