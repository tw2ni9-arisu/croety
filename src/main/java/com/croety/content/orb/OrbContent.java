package com.croety.content.orb;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class OrbContent {
    private static final DeferredRegister<EntityType<?>> ENTITIES = DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, "croety");
    public static final RegistryObject<EntityType<SoulOrb>> SOUL_ORB = ENTITIES.register("soul_energy_orb",
            () -> EntityType.Builder.<SoulOrb>of(SoulOrb::new, MobCategory.MISC).sized(0.5F, 0.5F)
                    .clientTrackingRange(6).updateInterval(20).build("croety:soul_energy_orb"));
    public static void register(IEventBus bus) { ENTITIES.register(bus); }
    private OrbContent() {}
}
