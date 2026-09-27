package com.dunesrelics.client.world;

import com.dunesrelics.DunesRelics;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;

/**
 * The pirate's tricorn hat: a crown on a wide brim turned up on three sides. The same shape is worn by pirate
 * illagers (as a layer on their head) and by players (as the Captain's Hat helmet). Texture layout 64 x 64, with the
 * hat below the first 16 rows so the head of an armour model stays see-through.
 */
public final class TricornModel {
    public static final ModelLayerLocation ILLAGER_HAT = new ModelLayerLocation(DunesRelics.id("tricorn"), "illager");
    public static final ModelLayerLocation ARMOR_HAT = new ModelLayerLocation(DunesRelics.id("tricorn"), "armor");

    private TricornModel() {}

    /** Adds the hat to {@code parent}, whose origin is the head pivot; {@code top} is the Y of the top of the head. */
    private static void addHat(PartDefinition parent, float top, boolean feather) {
        PartDefinition hat = parent.addOrReplaceChild("tricorn", CubeListBuilder.create()
                        .texOffs(0, 16).addBox(-4.5F, -4.0F, -4.5F, 9, 3, 9)
                        .texOffs(0, 28).addBox(-6.5F, -1.0F, -6.5F, 13, 1, 13)
                        .texOffs(0, 42).addBox(-6.5F, -4.0F, 5.5F, 13, 3, 1),
                PartPose.offset(0.0F, top + 1.5F, 0.0F));
        hat.addOrReplaceChild("left_flap", CubeListBuilder.create().texOffs(30, 42).addBox(-5.0F, -3.0F, -0.5F, 10, 3, 1),
                PartPose.offsetAndRotation(-3.2F, -1.0F, -2.6F, 0.0F, 0.95F, 0.0F));
        hat.addOrReplaceChild("right_flap", CubeListBuilder.create().texOffs(30, 42).mirror().addBox(-5.0F, -3.0F, -0.5F, 10, 3, 1),
                PartPose.offsetAndRotation(3.2F, -1.0F, -2.6F, 0.0F, -0.95F, 0.0F));
        if (feather) {
            hat.addOrReplaceChild("feather", CubeListBuilder.create().texOffs(54, 16).addBox(-0.5F, -6.0F, -1.5F, 1, 6, 3),
                    PartPose.offsetAndRotation(3.5F, -3.0F, 1.0F, -0.35F, 0.0F, 0.25F));
        }
    }

    /** For illagers: the head is 10 tall, so its top is at -10. */
    public static LayerDefinition createIllagerHat() {
        MeshDefinition mesh = new MeshDefinition();
        addHat(mesh.getRoot(), -10.0F, true);
        return LayerDefinition.create(mesh, 64, 64);
    }

    /** For players: a humanoid armour model whose head carries the hat. */
    public static LayerDefinition createArmorHat() {
        MeshDefinition mesh = HumanoidModel.createMesh(CubeDeformation.NONE, 0.0F);
        addHat(mesh.getRoot().getChild("head"), -8.0F, true);
        return LayerDefinition.create(mesh, 64, 64);
    }
}
