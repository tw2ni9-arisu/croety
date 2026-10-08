package com.croety.content.fluid;

import com.croety.content.orb.SoulOrb;
import com.simibubi.create.api.effect.OpenPipeEffectHandler;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.fluids.FluidType;
import net.neoforged.neoforge.fluids.BaseFlowingFluid;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

public final class SoulFluidContent {
    private static final DeferredRegister<FluidType> TYPES = DeferredRegister.create(NeoForgeRegistries.Keys.FLUID_TYPES, "croety");
    private static final DeferredRegister<Fluid> FLUIDS = DeferredRegister.create(Registries.FLUID, "croety");
    private static final DeferredRegister<Item> ITEMS = DeferredRegister.create(Registries.ITEM, "croety");
    public static final DeferredHolder<FluidType, FluidType> TYPE = TYPES.register("fluid_soul", () -> new FluidType(
            FluidType.Properties.create().descriptionId("fluid.croety.fluid_soul").lightLevel(4).density(1000).viscosity(1000)));
    public static final DeferredHolder<Fluid, BaseFlowingFluid.Source> SOUL = FLUIDS.register("fluid_soul", () -> new BaseFlowingFluid.Source(properties()));
    public static final DeferredHolder<Fluid, BaseFlowingFluid.Flowing> FLOWING = FLUIDS.register("flowing_fluid_soul", () -> new BaseFlowingFluid.Flowing(properties()));
    public static final DeferredHolder<Item, Item> BUCKET = ITEMS.register("fluid_soul_bucket", () -> new SoulBucketItem(
            new Item.Properties().stacksTo(1).craftRemainder(Items.BUCKET)));

    // 不注册LiquidBlock，因此不能放进世界或被喷口生成流体方块。
    private static BaseFlowingFluid.Properties properties() { return new BaseFlowingFluid.Properties(TYPE, SOUL, FLOWING).bucket(BUCKET); }
    public static void register(IEventBus bus) {
        TYPES.register(bus); FLUIDS.register(bus); ITEMS.register(bus);
        bus.addListener(SoulReceiver::registerCapabilities);
    }
    public static void setup() {
        OpenPipeEffectHandler.REGISTRY.register(SOUL.get(), (level, area, fluid) -> {
            if (level instanceof ServerLevel server)
                SoulOrb.award(server, new Vec3(area.getCenter().x, area.maxY - .5, area.getCenter().z), fluid.getAmount());
        });
    }
    private SoulFluidContent() {}
}
