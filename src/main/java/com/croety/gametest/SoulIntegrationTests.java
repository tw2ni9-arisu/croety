package com.croety.gametest;

import com.Polarice3.Goety.api.items.magic.ITotem;
import com.Polarice3.Goety.common.blocks.ModBlocks;
import com.Polarice3.Goety.common.blocks.entities.ArcaBlockEntity;
import com.Polarice3.Goety.common.items.ModItems;
import com.Polarice3.Goety.config.MainConfig;
import com.Polarice3.Goety.utils.SEHelper;
import com.croety.content.fluid.SoulFluidContent;
import com.croety.content.fluid.SoulTransfers;
import com.croety.content.motor.MotorContent;
import com.croety.content.motor.SoulMotorBlock;
import com.croety.content.orb.OrbContent;
import com.croety.content.orb.SoulOrb;
import com.mojang.authlib.GameProfile;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.content.fluids.OpenEndedPipe;
import com.simibubi.create.content.fluids.pump.PumpBlock;
import com.simibubi.create.content.fluids.pump.PumpBlockEntity;
import com.simibubi.create.content.fluids.pipes.FluidPipeBlock;
import com.simibubi.create.content.kinetics.base.RotatedPillarKineticBlock;
import net.createmod.catnip.math.BlockFace;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.FluidUtil;
import net.minecraftforge.fluids.capability.IFluidHandler.FluidAction;
import net.minecraftforge.fluids.capability.templates.FluidTank;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import java.util.UUID;

@GameTestHolder("croety")
@PrefixGameTestTemplate(false)
public class SoulIntegrationTests {
    @GameTest(template = "empty")
    public static void lastMomentPlayerFillCannotDestroyProcessingBucket(GameTestHelper h) {
        BlockPos pos = new BlockPos(2, 1, 2);
        h.setBlock(pos, AllBlocks.ITEM_DRAIN.get());
        var drain = (com.simibubi.create.content.fluids.drain.ItemDrainBlockEntity) h.getBlockEntity(pos);
        var tank = drain.getBehaviour(com.simibubi.create.foundation.blockEntity.behaviour.fluid.SmartFluidTankBehaviour.TYPE).getPrimaryHandler();
        tank.setFluid(new FluidStack(SoulFluidContent.SOUL.get(), 500));
        var moving = new com.simibubi.create.content.kinetics.belt.transport.TransportedItemStack(new ItemStack(SoulFluidContent.BUCKET.get()));
        moving.beltPosition = .49F; drain.setHeldItem(moving, Direction.NORTH);
        for (int i = 0; i < 15; i++) drain.tick();
        FakePlayer player = new FakePlayer(h.getLevel(), new GameProfile(UUID.randomUUID(), "drain_fill_test"));
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(SoulFluidContent.BUCKET.get()));
        h.useBlock(pos, player);
        h.assertTrue(tank.getFluidAmount() == 1500 && player.getMainHandItem().is(Items.BUCKET), "玩家应能在处理中向分液池补满");
        drain.tick();
        h.assertTrue(drain.getHeldItemStack().is(SoulFluidContent.BUCKET.get()) && tank.getFluidAmount() == 1500,
                "最后一刻池子变满时，正在处理的桶必须保留");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void bucketRoundTripAndNoWorldPlacement(GameTestHelper h) {
        FluidTank tank = new FluidTank(1500);
        tank.fill(new FluidStack(SoulFluidContent.SOUL.get(), 1000), FluidAction.EXECUTE);
        var filled = FluidUtil.tryFillContainer(new ItemStack(Items.BUCKET), tank, 1000, null, true);
        h.assertTrue(filled.isSuccess() && filled.getResult().is(SoulFluidContent.BUCKET.get()), "恰好1000mB应装满一桶");
        h.assertTrue(tank.isEmpty(), "装桶必须移走1000mB");
        FluidTank small = new FluidTank(999);
        h.assertTrue(!FluidUtil.tryEmptyContainer(filled.getResult(), small, 1000, null, true).isSuccess(), "空间不足不能部分排空桶");
        h.assertTrue(small.isEmpty(), "失败不能留下部分液体");
        var emptied = FluidUtil.tryEmptyContainer(filled.getResult(), tank, 1000, null, true);
        h.assertTrue(emptied.isSuccess() && emptied.getResult().is(Items.BUCKET) && tank.getFluidAmount() == 1000, "分液应返还空桶且守恒");
        h.assertTrue(SoulFluidContent.SOUL.get().defaultFluidState().createLegacyBlock().isAir(), "液态灵魂不能有世界方块");
        var bucket = (net.minecraft.world.item.BucketItem) SoulFluidContent.BUCKET.get();
        h.assertTrue(!bucket.emptyContents(null, h.getLevel(), h.absolutePos(new BlockPos(1, 1, 1)), null), "桶不能向世界倒出流体");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void drinkingBucketReplacesItAndAddsSouls(GameTestHelper h) {
        FakePlayer player = new FakePlayer(h.getLevel(), new GameProfile(UUID.randomUUID(), "bucket_test"));
        SEHelper.setSEActive(player, true);
        SEHelper.setSESouls(player, 40);
        player.setShiftKeyDown(true);
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(SoulFluidContent.BUCKET.get()));
        SoulFluidContent.BUCKET.get().use(h.getLevel(), player, InteractionHand.MAIN_HAND);
        h.assertTrue(SEHelper.getSESouls(player) == 1040, "饮用应补充1000灵魂");
        h.assertTrue(player.getMainHandItem().is(Items.BUCKET), "饮用后必须留下空桶");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void drinkingCannotWasteOverflow(GameTestHelper h) {
        FakePlayer player = new FakePlayer(h.getLevel(), new GameProfile(UUID.randomUUID(), "bucket_limit_test"));
        SEHelper.setSEActive(player, true);
        int before = MainConfig.MaxArcaSouls.get() - 10;
        SEHelper.setSESouls(player, before);
        player.setShiftKeyDown(true);
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(SoulFluidContent.BUCKET.get()));
        SoulFluidContent.BUCKET.get().use(h.getLevel(), player, InteractionHand.MAIN_HAND);
        h.assertTrue(player.getMainHandItem().is(SoulFluidContent.BUCKET.get()) && SEHelper.getSESouls(player) == before,
                "不足1000容量时应保持整桶与现有灵魂不变");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void rootsPartialThenConsumedAndSoulTotemSpent(GameTestHelper h) {
        ItemStack roots = new ItemStack(ModItems.TOTEM_OF_ROOTS.get());
        ITotem.setMaxSoulAmount(roots, 100); ITotem.setSoulsAmount(roots, 100);
        FluidTank tank = new FluidTank(1500);
        tank.fill(new FluidStack(SoulFluidContent.SOUL.get(), 1460), FluidAction.EXECUTE);
        var simulated = SoulTransfers.emptyTotem(roots, tank, true);
        h.assertTrue(simulated.getFirst().getAmount() == 40 && ITotem.currentSouls(roots) == 100 && tank.getFluidAmount() == 1460, "模拟不得修改任一端");
        var partial = SoulTransfers.emptyTotem(roots, tank, false);
        h.assertTrue(ITotem.currentSouls(partial.getSecond()) == 60 && tank.getFluidAmount() == 1500, "根图腾应只减少40");
        tank.drain(100, FluidAction.EXECUTE);
        var consumed = SoulTransfers.emptyTotem(partial.getSecond(), tank, false);
        h.assertTrue(consumed.getSecond().isEmpty() && consumed.getFirst().getAmount() == 60, "根图腾耗尽应消失");
        ItemStack soul = new ItemStack(ModItems.TOTEM_OF_SOULS.get());
        ITotem.setMaxSoulAmount(soul, 10000); ITotem.setSoulsAmount(soul, 200);
        FluidTank empty = new FluidTank(1500);
        var spent = SoulTransfers.emptyTotem(soul, empty, false);
        h.assertTrue(spent.getSecond().is(ModItems.SPENT_TOTEM.get()) && empty.getFluidAmount() == 200, "灵魂图腾耗尽应变为耗尽图腾");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void otherFluidAndFullTankDoNotConsumeTotem(GameTestHelper h) {
        ItemStack roots = new ItemStack(ModItems.TOTEM_OF_ROOTS.get());
        ITotem.setSoulsAmount(roots, 100);
        FluidTank tank = new FluidTank(1500);
        tank.fill(new FluidStack(Fluids.WATER, 100), FluidAction.EXECUTE);
        h.assertTrue(SoulTransfers.emptyTotem(roots, tank, false).getFirst().isEmpty(), "异种流体不能混入");
        h.assertTrue(ITotem.currentSouls(roots) == 100 && tank.getFluidAmount() == 100, "失败不能消耗图腾");
        tank.setFluid(new FluidStack(SoulFluidContent.SOUL.get(), 1500));
        h.assertTrue(SoulTransfers.emptyTotem(roots, tank, false).getFirst().isEmpty() && ITotem.currentSouls(roots) == 100, "满池不能消耗图腾");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void arcaFillsOnlyConnectedPlayersRemainingCapacity(GameTestHelper h) {
        BlockPos pos = new BlockPos(1, 2, 1);
        h.setBlock(pos, ModBlocks.ARCA_BLOCK.get());
        ArcaBlockEntity arca = (ArcaBlockEntity) h.getBlockEntity(pos);
        FakePlayer player = new FakePlayer(h.getLevel(), new GameProfile(UUID.randomUUID(), "arca_test"));
        player.setPos(h.absoluteVec(new Vec3(3, 2, 3)));
        h.getLevel().addNewPlayer(player);
        try {
            arca.setOwner(player);
            SEHelper.setSEActive(player, true);
            SEHelper.setSESouls(player, MainConfig.MaxArcaSouls.get() - 35);
            var capability = SEHelper.getCapability(player);
            capability.setArcaBlock(h.absolutePos(pos)); capability.setArcaBlockDimension(h.getLevel().dimension());
            var receiver = arca.getCapability(ForgeCapabilities.FLUID_HANDLER).orElseThrow(IllegalStateException::new);
            var offered = new FluidStack(SoulFluidContent.SOUL.get(), 100);
            h.assertTrue(receiver.fill(offered, FluidAction.SIMULATE) == 35, "方舟模拟只接受剩余35");
            h.assertTrue(SEHelper.getSESouls(player) == MainConfig.MaxArcaSouls.get() - 35, "模拟不充值");
            h.assertTrue(receiver.fill(offered, FluidAction.EXECUTE) == 35 && SEHelper.getSESouls(player) == MainConfig.MaxArcaSouls.get(), "方舟应按实际容量扣液充值");
            h.assertTrue(receiver.fill(offered, FluidAction.EXECUTE) == 0, "满方舟不能吞液");
            SEHelper.setSESouls(player, 0); capability.setArcaBlock(h.absolutePos(pos.above()));
            h.assertTrue(receiver.fill(offered, FluidAction.EXECUTE) == 0, "已解绑方舟不能充值");
        } finally { h.getLevel().removePlayerImmediately(player, Entity.RemovalReason.DISCARDED); }
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void openPipeSimulationAndDischargeConserveSouls(GameTestHelper h) {
        BlockPos pos = h.absolutePos(new BlockPos(2, 2, 2));
        OpenEndedPipe pipe = new OpenEndedPipe(new BlockFace(pos, Direction.EAST));
        pipe.manageSource(h.getLevel());
        var handler = pipe.provideHandler().orElseThrow(IllegalStateException::new);
        FluidStack offered = new FluidStack(SoulFluidContent.SOUL.get(), 100);
        int total = 0;
        AABB area = new AABB(pos).inflate(4);
        for (int i = 0; i < 25; i++) {
            handler.fill(offered, FluidAction.SIMULATE);
            long before = h.getLevel().getEntitiesOfClass(SoulOrb.class, area).stream().mapToLong(SoulOrb::getValue).sum();
            h.assertTrue(before == total, "模拟管口排放不能生成灵魂");
            total += handler.fill(offered, FluidAction.EXECUTE);
        }
        long emitted = h.getLevel().getEntitiesOfClass(SoulOrb.class, area).stream().mapToLong(SoulOrb::getValue).sum();
        h.assertTrue(total > 0 && total == emitted, "管口实际扣除量必须等于灵魂球总价值");
        h.assertTrue(h.getLevel().getBlockState(pos.east()).isAir(), "排放不能产生流体方块");
        h.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 180)
    public static void realPumpNeedsPowerAndDischargesSouls(GameTestHelper h) {
        h.setNight();
        for (int x = 0; x < 8; x++) for (int z = 0; z < 8; z++) h.setBlock(new BlockPos(x, 0, z), Blocks.STONE);
        h.setBlock(new BlockPos(1, 2, 2), AllBlocks.FLUID_TANK.get());
        h.setBlock(new BlockPos(2, 2, 2), AllBlocks.MECHANICAL_PUMP.get().defaultBlockState().setValue(PumpBlock.FACING, Direction.EAST));
        h.setBlock(new BlockPos(3, 2, 2), AllBlocks.FLUID_PIPE.get().defaultBlockState()
                .setValue(FluidPipeBlock.WEST, true).setValue(FluidPipeBlock.EAST, true));
        var tank = h.getBlockEntity(new BlockPos(1, 2, 2)).getCapability(ForgeCapabilities.FLUID_HANDLER).orElseThrow(IllegalStateException::new);
        tank.fill(new FluidStack(SoulFluidContent.SOUL.get(), 1000), FluidAction.EXECUTE);
        h.runAtTickTime(20, () -> {
            h.assertTrue(tank.getFluidInTank(0).getAmount() == 1000, "停泵时不能扣液");
            h.setBlock(new BlockPos(2, 3, 2), AllBlocks.COGWHEEL.get().defaultBlockState().setValue(RotatedPillarKineticBlock.AXIS, Direction.Axis.X));
            h.setBlock(new BlockPos(1, 3, 2), MotorContent.SOUL_MOTOR.get().defaultBlockState().setValue(SoulMotorBlock.FACING, Direction.EAST));
        });
        h.runAtTickTime(100, () -> {
            var pump = (PumpBlockEntity) h.getBlockEntity(new BlockPos(2, 2, 2));
            h.assertTrue(Math.abs(pump.getSpeed()) == 128, "实际齿轮连接应驱动泵至128RPM");
            h.assertTrue(tank.getFluidInTank(0).getAmount() < 1000, "有动力的泵应抽液");
            h.assertTrue(!h.getLevel().getEntitiesOfClass(SoulOrb.class, new AABB(h.absolutePos(new BlockPos(3, 1, 2))).inflate(10)).isEmpty(), "真实管网开口应生成灵魂球");
            h.setBlock(new BlockPos(1, 3, 2), Blocks.AIR);
        });
        h.runAtTickTime(140, () -> {
            var pump = (PumpBlockEntity) h.getBlockEntity(new BlockPos(2, 2, 2));
            h.assertTrue(pump.getSpeed() == 0, "移除动力源应停止泵");
            int remaining = tank.getFluidInTank(0).getAmount();
            h.runAfterDelay(20, () -> {
                h.assertTrue(tank.getFluidInTank(0).getAmount() == remaining, "停泵后不应继续消耗储罐流体");
                h.succeed();
            });
        });
    }
}
