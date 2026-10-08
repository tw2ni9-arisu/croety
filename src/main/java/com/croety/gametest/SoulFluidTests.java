package com.croety.gametest;

import com.Polarice3.Goety.api.items.magic.ITotem;
import com.Polarice3.Goety.common.blocks.ModBlocks;
import com.Polarice3.Goety.common.blocks.entities.CursedCageBlockEntity;
import com.Polarice3.Goety.common.items.ModItems;
import com.Polarice3.Goety.config.MainConfig;
import com.croety.content.fluid.SoulTransfers;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder("croety")
@PrefixGameTestTemplate(false)
public class SoulFluidTests {
    @GameTest(template = "empty")
    public static void drainPartiallyEmptiesTotem(GameTestHelper helper) {
        ResourceLocation id = ResourceLocation.fromNamespaceAndPath("croety", "fluid_soul");
        helper.assertTrue(BuiltInRegistries.FLUID.containsKey(id), "液态灵魂必须注册");
        helper.setBlock(new BlockPos(2, 1, 2), com.simibubi.create.AllBlocks.ITEM_DRAIN.get());
        var drain = (com.simibubi.create.content.fluids.drain.ItemDrainBlockEntity) helper.getBlockEntity(new BlockPos(2, 1, 2));
        var tank = drain.getBehaviour(com.simibubi.create.foundation.blockEntity.behaviour.fluid.SmartFluidTankBehaviour.TYPE);
        tank.getPrimaryHandler().setFluid(new FluidStack(BuiltInRegistries.FLUID.get(id), 1200));
        ItemStack totem = new ItemStack(ModItems.TOTEM_OF_SOULS.get());
        ITotem.setMaxSoulAmount(totem, 10000);
        ITotem.setSoulsamount(totem, 10000);
        ITotem.updateTag(totem, tag -> tag.putString("demo_marker", "preserve"));
        var transported = new com.simibubi.create.content.kinetics.belt.transport.TransportedItemStack(totem);
        transported.beltPosition = .49F;
        drain.setHeldItem(transported, net.minecraft.core.Direction.NORTH);
        for (int tick = 0; tick < 17; tick++) drain.tick();
        helper.assertTrue(tank.getPrimaryHandler().getFluidAmount() == 1500, "只应抽取剩余容量300mB");
        ItemStack remaining = drain.getHeldItemStack();
        helper.assertTrue(ITotem.currentSouls(remaining) == 9700, "图腾应剩9700灵魂");
        helper.assertTrue(ITotem.tag(remaining).getString("demo_marker").equals("preserve"), "分液必须保留其他物品数据");
        helper.succeed();
    }

    // 防止满图腾吞液、模拟操作扣液以及超过剩余容量充值。
    @GameTest(template = "empty")
    public static void cageAcceptsOnlyRemainingCapacity(GameTestHelper helper) {
        ResourceLocation id = ResourceLocation.fromNamespaceAndPath("croety", "fluid_soul");
        helper.assertTrue(BuiltInRegistries.FLUID.containsKey(id), "液态灵魂必须注册");
        helper.setBlock(new BlockPos(1, 1, 1), ModBlocks.CURSED_CAGE_BLOCK.get());
        CursedCageBlockEntity cage = (CursedCageBlockEntity) helper.getBlockEntity(new BlockPos(1, 1, 1));
        ItemStack totem = new ItemStack(ModItems.TOTEM_OF_SOULS.get());
        ITotem.setMaxSoulAmount(totem, 10000);
        ITotem.setSoulsamount(totem, 9970);
        cage.setItem(totem);
        var handler = helper.getLevel().getCapability(Capabilities.FluidHandler.BLOCK, cage.getBlockPos(), null);
        helper.assertTrue(handler != null, "诅咒之笼应可接收灵魂流体");
        FluidStack input = new FluidStack(BuiltInRegistries.FLUID.get(id), 100);
        helper.assertTrue(handler.fill(input, FluidAction.SIMULATE) == 30, "模拟只接受剩余30");
        helper.assertTrue(ITotem.currentSouls(totem) == 9970, "模拟不能改变图腾");
        helper.assertTrue(handler.fill(input, FluidAction.EXECUTE) == 30, "实际只消耗30mB");
        helper.assertTrue(ITotem.currentSouls(totem) == 10000, "应恰好充满图腾");
        helper.assertTrue(handler.fill(input, FluidAction.EXECUTE) == 0, "充满后不得吞液");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void spentTotemPreservesCustomComponents(GameTestHelper helper) {
        ItemStack totem = new ItemStack(ModItems.TOTEM_OF_SOULS.get());
        ITotem.setMaxSoulAmount(totem, 10000);
        ITotem.setSoulsamount(totem, 200);
        ITotem.updateTag(totem, tag -> tag.putString("demo_marker", "preserve"));
        totem.set(DataComponents.CUSTOM_NAME, Component.literal("Migration marker"));
        FluidTank tank = new FluidTank(1500);

        var simulated = SoulTransfers.emptyTotem(totem, tank, true);
        helper.assertTrue(simulated.getFirst().getAmount() == 200 && tank.isEmpty()
                        && ITotem.currentSouls(totem) == 200 && ITotem.maximumSouls(totem) == 10000,
                "模拟耗尽图腾不能改变输入灵魂或储罐");
        helper.assertTrue(Component.literal("Migration marker").equals(totem.get(DataComponents.CUSTOM_NAME))
                        && ITotem.tag(totem).getString("demo_marker").equals("preserve"),
                "模拟不能改变输入名称或自定义数据");

        var result = SoulTransfers.emptyTotem(totem, tank, false);
        ItemStack spent = result.getSecond();
        var tag = spent.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        helper.assertTrue(spent.is(ModItems.SPENT_TOTEM.get()) && tank.getFluidAmount() == 200,
                "耗尽灵魂图腾应产出200mB并变为耗尽图腾");
        helper.assertTrue(Component.literal("Migration marker").equals(spent.get(DataComponents.CUSTOM_NAME))
                        && tag.getString("demo_marker").equals("preserve"),
                "耗尽转换必须保留名称组件与自定义数据");
        helper.assertTrue(!tag.contains(ITotem.SOULS_AMOUNT) && !tag.contains(ITotem.MAX_SOUL_AMOUNT),
                "耗尽图腾只移除灵魂计数字段");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void totemWithoutStoredMaximumUsesItemCapacity(GameTestHelper helper) {
        int previousMaximum = MainConfig.MaxSouls.get();
        try {
            MainConfig.MaxSouls.set(10000);
            ItemStack roots = new ItemStack(ModItems.TOTEM_OF_ROOTS.get());
            ITotem.setSoulsamount(roots, 0);
            ITotem.updateTag(roots, tag -> tag.remove(ITotem.MAX_SOUL_AMOUNT));
            helper.assertTrue(SoulTransfers.fillTotem(roots, 200, true) == 100,
                    "缺少保存上限时，根图腾应按物品容量只接受100");
            helper.assertTrue(ITotem.currentSouls(roots) == 0 && !ITotem.tag(roots).contains(ITotem.MAX_SOUL_AMOUNT),
                    "模拟不能写入灵魂或补建容量字段");
            helper.assertTrue(SoulTransfers.fillTotem(roots, 200, false) == 100
                            && ITotem.currentSouls(roots) == 100 && ITotem.maximumSouls(roots) == 100,
                    "实际注液按物品容量充满并只消耗100mB");
            helper.succeed();
        } finally {
            MainConfig.MaxSouls.set(previousMaximum);
        }
    }
}
