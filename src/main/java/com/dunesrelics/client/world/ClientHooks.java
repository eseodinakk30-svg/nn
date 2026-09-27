package com.dunesrelics.client.world;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.BookViewScreen;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

/** Client-only helpers that common code reaches through DistExecutor or client item extensions. */
public final class ClientHooks {
    private static HumanoidModel<LivingEntity> hat;

    private ClientHooks() {}

    public static void openBook(ItemStack stack) {
        Minecraft.getInstance().setScreen(new BookViewScreen(new BookViewScreen.WrittenBookAccess(stack)));
    }

    public static HumanoidModel<LivingEntity> hatModel() {
        if (hat == null) {
            hat = new HumanoidModel<>(Minecraft.getInstance().getEntityModels().bakeLayer(TricornModel.ARMOR_HAT));
        }
        return hat;
    }

    /** Models are rebaked when resource packs reload. */
    public static void resetModels() {
        hat = null;
    }
}
