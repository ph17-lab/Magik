package com.magik.item;

import com.magik.client.render.GeoAsset;
import com.magik.client.render.WeaponGeoRenderer;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;
import software.bernie.geckolib.animatable.GeoItem;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.util.GeckoLibUtil;

import javax.annotation.Nullable;
import java.util.List;
import java.util.function.Consumer;

/**
 * Sword-class heavy weapon (warhammers, greatswords): slow, brutal, with an
 * extra damage bonus from Strength and the Armamento Pesado passives.
 * Rendered through GeckoLib (Bedrock geometry).
 */
public class HeavyWeaponItem extends SwordItem implements HeavyWeapon, RpgGear, GeoItem, GeoAsset {

    private final ItemRequirements requirements;
    private final ResourceLocation geoModel;
    private final ResourceLocation geoTexture;
    private final ResourceLocation geoAnimation;
    private final AnimatableInstanceCache geckoCache = GeckoLibUtil.createInstanceCache(this);

    public HeavyWeaponItem(Tier tier, int attackDamage, float attackSpeed,
                           ItemRequirements requirements,
                           ResourceLocation geoModel, ResourceLocation geoTexture,
                           ResourceLocation geoAnimation, Properties properties) {
        super(tier, attackDamage, attackSpeed, properties);
        this.requirements = requirements;
        this.geoModel = geoModel;
        this.geoTexture = geoTexture;
        this.geoAnimation = geoAnimation;
    }

    @Override
    public ItemRequirements getRpgRequirements() {
        return requirements;
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("item.magik.heavy_weapon.tooltip"));
        RpgGear.appendRequirementTooltip(stack, tooltip);
    }

    // --- GeckoLib ---

    @Override
    public ResourceLocation geoModel() {
        return geoModel;
    }

    @Override
    public ResourceLocation geoTexture() {
        return geoTexture;
    }

    @Override
    public ResourceLocation geoAnimation() {
        return geoAnimation;
    }

    @Override
    public void initializeClient(Consumer<IClientItemExtensions> consumer) {
        consumer.accept(new IClientItemExtensions() {
            private WeaponGeoRenderer<HeavyWeaponItem> renderer;

            @Override
            public BlockEntityWithoutLevelRenderer getCustomRenderer() {
                if (this.renderer == null) {
                    this.renderer = new WeaponGeoRenderer<>();
                }
                return this.renderer;
            }
        });
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.geckoCache;
    }
}
