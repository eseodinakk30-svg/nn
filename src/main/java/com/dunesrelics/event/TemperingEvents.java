package com.dunesrelics.event;

import com.dunesrelics.DunesRelics;
import com.dunesrelics.item.Tempering;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.DiggerItem;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.ItemAttributeModifierEvent;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.UUID;

/** The bonuses of tempered gear (see {@link Tempering}). */
@Mod.EventBusSubscriber(modid = DunesRelics.MODID)
public final class TemperingEvents {
    private static final UUID DAMAGE = UUID.fromString("5b4e1f0e-6a54-4a8e-9b1d-7c6f0b9d3a01");
    private static final UUID[] ARMOR = {
            UUID.fromString("5b4e1f0e-6a54-4a8e-9b1d-7c6f0b9d3a11"), UUID.fromString("5b4e1f0e-6a54-4a8e-9b1d-7c6f0b9d3a12"),
            UUID.fromString("5b4e1f0e-6a54-4a8e-9b1d-7c6f0b9d3a13"), UUID.fromString("5b4e1f0e-6a54-4a8e-9b1d-7c6f0b9d3a14")};
    private static final UUID[] TOUGHNESS = {
            UUID.fromString("5b4e1f0e-6a54-4a8e-9b1d-7c6f0b9d3a21"), UUID.fromString("5b4e1f0e-6a54-4a8e-9b1d-7c6f0b9d3a22"),
            UUID.fromString("5b4e1f0e-6a54-4a8e-9b1d-7c6f0b9d3a23"), UUID.fromString("5b4e1f0e-6a54-4a8e-9b1d-7c6f0b9d3a24")};

    private TemperingEvents() {}

    @SubscribeEvent
    public static void onAttributes(ItemAttributeModifierEvent event) {
        ItemStack stack = event.getItemStack();
        if (!Tempering.isTempered(stack)) {
            return;
        }
        EquipmentSlot slot = event.getSlotType();
        if (stack.getItem() instanceof ArmorItem armor) {
            if (slot == armor.getEquipmentSlot()) {
                int index = slot.getIndex();
                event.addModifier(Attributes.ARMOR, new AttributeModifier(ARMOR[index], "Tempered armor",
                        Tempering.ARMOR_BONUS, AttributeModifier.Operation.ADDITION));
                event.addModifier(Attributes.ARMOR_TOUGHNESS, new AttributeModifier(TOUGHNESS[index], "Tempered armor",
                        Tempering.TOUGHNESS_BONUS, AttributeModifier.Operation.ADDITION));
            }
        } else if (slot == EquipmentSlot.MAINHAND) {
            event.addModifier(Attributes.ATTACK_DAMAGE, new AttributeModifier(DAMAGE, "Tempered weapon",
                    Tempering.DAMAGE_BONUS, AttributeModifier.Operation.ADDITION));
        }
    }

    @SubscribeEvent
    public static void onBreakSpeed(PlayerEvent.BreakSpeed event) {
        ItemStack held = event.getEntity().getMainHandItem();
        if (held.getItem() instanceof DiggerItem && Tempering.isTempered(held)) {
            event.setNewSpeed(event.getNewSpeed() * Tempering.SPEED_BONUS);
        }
    }

    @SubscribeEvent
    public static void onTooltip(ItemTooltipEvent event) {
        if (Tempering.isTempered(event.getItemStack()) && !event.getToolTip().isEmpty()) {
            event.getToolTip().add(1, Component.translatable("item.dunesrelics.tempered").withStyle(ChatFormatting.GOLD));
        }
    }
}
