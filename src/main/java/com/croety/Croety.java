package com.croety;

import com.croety.content.DemoTab;
import com.croety.content.fluid.SoulFluidContent;
import com.croety.content.motor.MotorContent;
import com.croety.content.orb.OrbContent;
import com.croety.integration.CreateIntegration;
import com.croety.integration.GoetyIntegration;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;

@Mod(Croety.MODID)
public class Croety
{
    public static final String MODID = "croety";

    public Croety(IEventBus modEventBus, ModContainer modContainer)
    {
        OrbContent.register(modEventBus);
        SoulFluidContent.register(modEventBus);
        DemoTab.register(modEventBus);
        MotorContent.register(modEventBus);
        modEventBus.addListener(this::commonSetup);
    }

    private void commonSetup(final FMLCommonSetupEvent event)
    {
        // 注册完成后初始化联动，并检查依赖能否正常调用。
        event.enqueueWork(() -> {
            MotorContent.setup();
            SoulFluidContent.setup();
            CreateIntegration.logEnvironment();
            GoetyIntegration.logEnvironment();
        });
    }
}




