package com.croety.content.orb;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class OrbContent {
    private static final DeferredRegister<EntityType<?>> ENTITIES = DeferredRegister.create(Registries.ENTITY_TYPE, "croety");
    public static final DeferredHolder<EntityType<?>, EntityType<SoulOrb>> SOUL_ORB = ENTITIES.register("soul_energy_orb",
            () -> EntityType.Builder.<SoulOrb>of(SoulOrb::new, MobCategory.MISC).sized(0.5F, 0.5F)
                    .clientTrackingRange(6).updateInterval(20).build("croety:soul_energy_orb"));
    public static void register(IEventBus bus) { ENTITIES.register(bus); }
    private OrbContent() {}
}
