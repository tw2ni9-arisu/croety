package com.croety.content.motor;

import com.Polarice3.Goety.common.items.magic.MagicFocus;
import com.croety.content.spell.WavingSpell;
import com.simibubi.create.api.stress.BlockStressValues;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class MotorContent {
    private static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(Registries.BLOCK, "croety");
    private static final DeferredRegister<Item> ITEMS = DeferredRegister.create(Registries.ITEM, "croety");
    private static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, "croety");

    public static final DeferredHolder<Block, SoulMotorBlock> SOUL_MOTOR = BLOCKS.register("soul_motor",
            () -> new SoulMotorBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK)
                    .strength(2).noOcclusion().noLootTable().lightLevel(state -> 4)));
    public static final DeferredHolder<Item, Item> SOUL_MOTOR_ITEM = ITEMS.register("soul_motor",
            () -> new BlockItem(SOUL_MOTOR.get(), new Item.Properties()));
    public static final DeferredHolder<Item, Item> WAVING_FOCUS = ITEMS.register("waving_focus",
            () -> new MagicFocus(new WavingSpell()));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<SoulMotorBlockEntity>> MOTOR_BE = BLOCK_ENTITIES.register("soul_motor",
            () -> BlockEntityType.Builder.of(SoulMotorBlockEntity::new, SOUL_MOTOR.get()).build(null));

    private MotorContent() {}

    public static void register(IEventBus bus) {
        BLOCKS.register(bus);
        ITEMS.register(bus);
        BLOCK_ENTITIES.register(bus);
        NeoForge.EVENT_BUS.addListener(MotorContent::onServerTick);
    }

    public static void setup() {
        BlockStressValues.CAPACITIES.register(SOUL_MOTOR.get(), () -> 64.0);
        BlockStressValues.setGeneratorSpeed(128).accept(SOUL_MOTOR.get());
    }

    private static void onServerTick(ServerTickEvent.Post event) {
        if (event.getServer().overworld().getGameTime() % 20 == 0)
            SoulMotorData.get(event.getServer()).tick(event.getServer());
    }
}
