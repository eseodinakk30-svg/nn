package com.dunesrelics;

import com.mojang.logging.LogUtils;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.fml.common.Mod;
import org.slf4j.Logger;
import terrablender.api.Regions;

@Mod(DunesRelics.MODID)
public class DunesRelics {
    public static final String MODID = "dunesrelics";
    public static final Logger LOGGER = LogUtils.getLogger();

    public DunesRelics() {
        LOGGER.info("Dunes & Relics loading, TerraBlender present: {}", Regions.class.getName());
    }

    public static ResourceLocation id(String path) {
        return new ResourceLocation(MODID, path);
    }
}
