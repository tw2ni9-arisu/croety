package com.croety.content.motor;

import com.Polarice3.Goety.common.items.magic.MagicFocus;
import com.croety.content.spell.WavingSpell;
import com.simibubi.create.api.stress.BlockStressValues;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class MotorContent {
    private static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, "croety");
    private static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, "croety");
    private static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, "croety");

    public static final RegistryObject<SoulMotorBlock> SOUL_MOTOR = BLOCKS.register("soul_motor",
            () -> new SoulMotorBlock(BlockBehaviour.Properties.copy(Blocks.IRON_BLOCK)
                    .strength(2).noOcclusion().noLootTable().lightLevel(state -> 4)));
    public static final RegistryObject<Item> SOUL_MOTOR_ITEM = ITEMS.register("soul_motor",
            () -> new BlockItem(SOUL_MOTOR.get(), new Item.Properties()));
    public static final RegistryObject<Item> WAVING_FOCUS = ITEMS.register("waving_focus",
            () -> new MagicFocus(new WavingSpell()));
    public static final RegistryObject<BlockEntityType<SoulMotorBlockEntity>> MOTOR_BE = BLOCK_ENTITIES.register("soul_motor",
            () -> BlockEntityType.Builder.of(SoulMotorBlockEntity::new, SOUL_MOTOR.get()).build(null));

    private MotorContent() {}

    public static void register(IEventBus bus) {
        BLOCKS.register(bus);
        ITEMS.register(bus);
        BLOCK_ENTITIES.register(bus);
        MinecraftForge.EVENT_BUS.addListener(MotorContent::onServerTick);
    }

    public static void setup() {
        BlockStressValues.CAPACITIES.register(SOUL_MOTOR.get(), () -> 64.0);
        BlockStressValues.setGeneratorSpeed(128).accept(SOUL_MOTOR.get());
    }

    private static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase == TickEvent.Phase.END && event.getServer().overworld().getGameTime() % 20 == 0)
            SoulMotorData.get(event.getServer()).tick(event.getServer());
    }
}
