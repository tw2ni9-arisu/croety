package com.croety.content;

import com.croety.content.fluid.SoulFluidContent;
import com.croety.content.motor.MotorContent;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class DemoTab {
    private static final DeferredRegister<CreativeModeTab> TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, "croety");
    static {
        TABS.register("demo", () -> CreativeModeTab.builder().title(Component.translatable("itemGroup.croety.demo"))
                .icon(() -> MotorContent.SOUL_MOTOR_ITEM.get().getDefaultInstance())
                .displayItems((parameters, output) -> {
                    output.accept(MotorContent.SOUL_MOTOR_ITEM.get());
                    output.accept(MotorContent.WAVING_FOCUS.get());
                    output.accept(SoulFluidContent.BUCKET.get());
                }).build());
    }
    public static void register(IEventBus bus) { TABS.register(bus); }
    private DemoTab() {}
}
