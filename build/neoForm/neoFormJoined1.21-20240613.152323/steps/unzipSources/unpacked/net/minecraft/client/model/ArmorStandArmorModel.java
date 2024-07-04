package net.minecraft.client.model;

import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class ArmorStandArmorModel extends HumanoidModel<ArmorStand> {
    public ArmorStandArmorModel(ModelPart pRoot) {
        super(pRoot);
    }

    public static LayerDefinition createBodyLayer(CubeDeformation pCubeDeformation) {
        MeshDefinition meshdefinition = HumanoidModel.createMesh(pCubeDeformation, 0.0F);
        PartDefinition partdefinition = meshdefinition.getRoot();
        partdefinition.addOrReplaceChild(
            "head", CubeListBuilder.create().texOffs(0, 0).addBox(-4.0F, -8.0F, -4.0F, 8.0F, 8.0F, 8.0F, pCubeDeformation), PartPose.offset(0.0F, 1.0F, 0.0F)
        );
        partdefinition.addOrReplaceChild(
            "hat",
            CubeListBuilder.create().texOffs(32, 0).addBox(-4.0F, -8.0F, -4.0F, 8.0F, 8.0F, 8.0F, pCubeDeformation.extend(0.5F)),
            PartPose.offset(0.0F, 1.0F, 0.0F)
        );
        partdefinition.addOrReplaceChild(
            "right_leg",
            CubeListBuilder.create().texOffs(0, 16).addBox(-2.0F, 0.0F, -2.0F, 4.0F, 12.0F, 4.0F, pCubeDeformation.extend(-0.1F)),
            PartPose.offset(-1.9F, 11.0F, 0.0F)
        );
        partdefinition.addOrReplaceChild(
            "left_leg",
            CubeListBuilder.create().texOffs(0, 16).mirror().addBox(-2.0F, 0.0F, -2.0F, 4.0F, 12.0F, 4.0F, pCubeDeformation.extend(-0.1F)),
            PartPose.offset(1.9F, 11.0F, 0.0F)
        );
        return LayerDefinition.create(meshdefinition, 64, 32);
    }

    /**
     * Sets this entity's model rotation angles
     */
    public void setupAnim(ArmorStand pEntity, float pLimbSwing, float pLimbSwingAmount, float pAgeInTicks, float pNetHeadYaw, float pHeadPitch) {
        this.head.xRot = (float) (Math.PI / 180.0) * pEntity.getHeadPose().getX();
        this.head.yRot = (float) (Math.PI / 180.0) * pEntity.getHeadPose().getY();
        this.head.zRot = (float) (Math.PI / 180.0) * pEntity.getHeadPose().getZ();
        this.body.xRot = (float) (Math.PI / 180.0) * pEntity.getBodyPose().getX();
        this.body.yRot = (float) (Math.PI / 180.0) * pEntity.getBodyPose().getY();
        this.body.zRot = (float) (Math.PI / 180.0) * pEntity.getBodyPose().getZ();
        this.leftArm.xRot = (float) (Math.PI / 180.0) * pEntity.getLeftArmPose().getX();
        this.leftArm.yRot = (float) (Math.PI / 180.0) * pEntity.getLeftArmPose().getY();
        this.leftArm.zRot = (float) (Math.PI / 180.0) * pEntity.getLeftArmPose().getZ();
        this.rightArm.xRot = (float) (Math.PI / 180.0) * pEntity.getRightArmPose().getX();
        this.rightArm.yRot = (float) (Math.PI / 180.0) * pEntity.getRightArmPose().getY();
        this.rightArm.zRot = (float) (Math.PI / 180.0) * pEntity.getRightArmPose().getZ();
        this.leftLeg.xRot = (float) (Math.PI / 180.0) * pEntity.getLeftLegPose().getX();
        this.leftLeg.yRot = (float) (Math.PI / 180.0) * pEntity.getLeftLegPose().getY();
        this.leftLeg.zRot = (float) (Math.PI / 180.0) * pEntity.getLeftLegPose().getZ();
        this.rightLeg.xRot = (float) (Math.PI / 180.0) * pEntity.getRightLegPose().getX();
        this.rightLeg.yRot = (float) (Math.PI / 180.0) * pEntity.getRightLegPose().getY();
        this.rightLeg.zRot = (float) (Math.PI / 180.0) * pEntity.getRightLegPose().getZ();
        this.hat.copyFrom(this.head);
    }
}
