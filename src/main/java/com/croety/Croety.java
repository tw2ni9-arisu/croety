package com.croety;

import com.croety.content.DemoTab;
import com.croety.content.fluid.SoulFluidContent;
import com.croety.content.motor.MotorContent;
import com.croety.content.orb.OrbContent;
import com.croety.integration.CreateIntegration;
import com.croety.integration.GoetyIntegration;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod(Croety.MODID)
public class Croety
{
    public static final String MODID = "croety";

    public Croety(FMLJavaModLoadingContext context)
    {
        IEventBus modEventBus = context.getModEventBus();
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




