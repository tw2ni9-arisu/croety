package com.croety.content.fluid;

import com.croety.content.orb.SoulOrb;
import com.simibubi.create.api.effect.OpenPipeEffectHandler;
import java.util.function.Consumer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.client.extensions.common.IClientFluidTypeExtensions;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fluids.FluidType;
import net.minecraftforge.fluids.ForgeFlowingFluid;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class SoulFluidContent {
    private static final DeferredRegister<FluidType> TYPES = DeferredRegister.create(ForgeRegistries.Keys.FLUID_TYPES, "croety");
    private static final DeferredRegister<Fluid> FLUIDS = DeferredRegister.create(ForgeRegistries.FLUIDS, "croety");
    private static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, "croety");
    public static final RegistryObject<FluidType> TYPE = TYPES.register("fluid_soul", () -> new FluidType(
            FluidType.Properties.create().descriptionId("fluid.croety.fluid_soul").lightLevel(4).density(1000).viscosity(1000)) {
        @Override public void initializeClient(Consumer<IClientFluidTypeExtensions> consumer) {
            consumer.accept(new IClientFluidTypeExtensions() {
                @Override public ResourceLocation getStillTexture() { return new ResourceLocation("croety", "block/fluid_soul_still"); }
                @Override public ResourceLocation getFlowingTexture() { return getStillTexture(); }
            });
        }
    });
    public static final RegistryObject<ForgeFlowingFluid.Source> SOUL = FLUIDS.register("fluid_soul", () -> new ForgeFlowingFluid.Source(properties()));
    public static final RegistryObject<ForgeFlowingFluid.Flowing> FLOWING = FLUIDS.register("flowing_fluid_soul", () -> new ForgeFlowingFluid.Flowing(properties()));
    public static final RegistryObject<Item> BUCKET = ITEMS.register("fluid_soul_bucket", () -> new SoulBucketItem(
            new Item.Properties().stacksTo(1).craftRemainder(Items.BUCKET)));

    // 不注册LiquidBlock，因此不能放进世界或被喷口生成流体方块。
    private static ForgeFlowingFluid.Properties properties() { return new ForgeFlowingFluid.Properties(TYPE, SOUL, FLOWING).bucket(BUCKET); }
    public static void register(IEventBus bus) { TYPES.register(bus); FLUIDS.register(bus); ITEMS.register(bus); }
    public static void setup() {
        OpenPipeEffectHandler.REGISTRY.register(SOUL.get(), (level, area, fluid) -> {
            if (level instanceof ServerLevel server)
                SoulOrb.award(server, new Vec3(area.getCenter().x, area.maxY - .5, area.getCenter().z), fluid.getAmount());
        });
    }
    private SoulFluidContent() {}
}

