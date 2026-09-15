package com.croety.gametest;

import com.Polarice3.Goety.api.items.magic.ITotem;
import com.Polarice3.Goety.common.blocks.ModBlocks;
import com.Polarice3.Goety.common.blocks.entities.CursedCageBlockEntity;
import com.Polarice3.Goety.common.items.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler.FluidAction;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import net.minecraftforge.registries.ForgeRegistries;

@GameTestHolder("croety")
@PrefixGameTestTemplate(false)
public class SoulFluidTests {
    @GameTest(template = "empty")
    public static void drainPartiallyEmptiesTotem(GameTestHelper helper) {
        ResourceLocation id = new ResourceLocation("croety", "fluid_soul");
        helper.assertTrue(ForgeRegistries.FLUIDS.containsKey(id), "液态灵魂必须注册");
        helper.setBlock(new BlockPos(2, 1, 2), com.simibubi.create.AllBlocks.ITEM_DRAIN.get());
        var drain = (com.simibubi.create.content.fluids.drain.ItemDrainBlockEntity) helper.getBlockEntity(new BlockPos(2, 1, 2));
        var tank = drain.getBehaviour(com.simibubi.create.foundation.blockEntity.behaviour.fluid.SmartFluidTankBehaviour.TYPE);
        tank.getPrimaryHandler().setFluid(new FluidStack(ForgeRegistries.FLUIDS.getValue(id), 1200));
        ItemStack totem = new ItemStack(ModItems.TOTEM_OF_SOULS.get());
        ITotem.setMaxSoulAmount(totem, 10000);
        ITotem.setSoulsAmount(totem, 10000);
        totem.getOrCreateTag().putString("demo_marker", "preserve");
        var transported = new com.simibubi.create.content.kinetics.belt.transport.TransportedItemStack(totem);
        transported.beltPosition = .49F;
        drain.setHeldItem(transported, net.minecraft.core.Direction.NORTH);
        for (int tick = 0; tick < 17; tick++) drain.tick();
        helper.assertTrue(tank.getPrimaryHandler().getFluidAmount() == 1500, "只应抽取剩余容量300mB");
        ItemStack remaining = drain.getHeldItemStack();
        helper.assertTrue(ITotem.currentSouls(remaining) == 9700, "图腾应剩9700灵魂");
        helper.assertTrue(remaining.getTag().getString("demo_marker").equals("preserve"), "分液必须保留其他NBT");
        helper.succeed();
    }

    // 防止满图腾吞液、模拟操作扣液以及超过剩余容量充值。
    @GameTest(template = "empty")
    public static void cageAcceptsOnlyRemainingCapacity(GameTestHelper helper) {
        ResourceLocation id = new ResourceLocation("croety", "fluid_soul");
        helper.assertTrue(ForgeRegistries.FLUIDS.containsKey(id), "液态灵魂必须注册");
        helper.setBlock(new BlockPos(1, 1, 1), ModBlocks.CURSED_CAGE_BLOCK.get());
        CursedCageBlockEntity cage = (CursedCageBlockEntity) helper.getBlockEntity(new BlockPos(1, 1, 1));
        ItemStack totem = new ItemStack(ModItems.TOTEM_OF_SOULS.get());
        ITotem.setMaxSoulAmount(totem, 10000);
        ITotem.setSoulsAmount(totem, 9970);
        cage.setItem(totem);
        var handler = cage.getCapability(ForgeCapabilities.FLUID_HANDLER).orElse(null);
        helper.assertTrue(handler != null, "诅咒之笼应可接收灵魂流体");
        FluidStack input = new FluidStack(ForgeRegistries.FLUIDS.getValue(id), 100);
        helper.assertTrue(handler.fill(input, FluidAction.SIMULATE) == 30, "模拟只接受剩余30");
        helper.assertTrue(ITotem.currentSouls(totem) == 9970, "模拟不能改变图腾");
        helper.assertTrue(handler.fill(input, FluidAction.EXECUTE) == 30, "实际只消耗30mB");
        helper.assertTrue(ITotem.currentSouls(totem) == 10000, "应恰好充满图腾");
        helper.assertTrue(handler.fill(input, FluidAction.EXECUTE) == 0, "充满后不得吞液");
        helper.succeed();
    }
}

