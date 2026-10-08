package com.croety.gametest;

import com.croety.content.motor.MotorContent;
import com.croety.content.motor.SoulMotorBlock;
import com.croety.content.motor.SoulMotorBlockEntity;
import com.croety.content.motor.SoulMotorData;
import com.mojang.authlib.GameProfile;
import com.simibubi.create.AllItems;
import java.nio.file.Path;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.DimensionDataStorage;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.common.IOUtilities;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder("croety")
@PrefixGameTestTemplate(false)
public class MotorPersistenceTests {
    @GameTest(template = "empty", timeoutTicks = 100)
    public static void savedDataReloadKeepsUnloadedEvictionAndOwner(GameTestHelper helper) throws Exception {
        ServerLevel overworld = helper.getLevel();
        ServerLevel nether = overworld.getServer().getLevel(Level.NETHER);
        helper.assertTrue(nether != null, "测试服务端必须有下界");
        UUID owner = UUID.randomUUID();
        SoulMotorData data = SoulMotorData.get(overworld.getServer());
        BlockPos unloaded = new BlockPos(32768, 200, 32768);
        helper.assertTrue(!nether.hasChunkAt(unloaded), "测试坐标必须保持未加载");
        long oldest = data.add(owner, nether, unloaded);
        long[] ids = new long[4];
        ids[0] = oldest;
        try {
            for (int i = 1; i <= 3; i++) {
                BlockPos pos = helper.absolutePos(new BlockPos(i, 2, 1));
                overworld.setBlock(pos, MotorContent.SOUL_MOTOR.get().defaultBlockState(), 3);
                SoulMotorBlockEntity motor = (SoulMotorBlockEntity) overworld.getBlockEntity(pos);
                ids[i] = data.add(owner, overworld, pos);
                motor.setSummoned(owner, ids[i], overworld.getGameTime() + SoulMotorData.LIFETIME);
                if (i == 1) {
                    CompoundTag motorTag = motor.saveWithFullMetadata(overworld.registryAccess());
                    SoulMotorBlockEntity loadedMotor = new SoulMotorBlockEntity(pos, motor.getBlockState());
                    loadedMotor.loadWithComponents(motorTag, overworld.registryAccess());
                    helper.assertTrue(owner.equals(loadedMotor.getOwner()) && loadedMotor.getRecordId() == ids[i]
                                    && loadedMotor.getExpiresAt() == motor.getExpiresAt(),
                            "方块实体保存加载应保留所属玩家、记录号和到期时间");
                }
            }
            helper.assertTrue(data.activeCount(owner) == 3 && !data.contains(oldest),
                    "跨维度第四台应淘汰下界最早记录");
            data.tick(overworld.getServer());
            helper.assertTrue(!data.contains(oldest), "未加载区块的淘汰记录不能重新激活");

            DimensionDataStorage storage = overworld.getDataStorage();
            storage.save();
            // NeoForge异步写盘，重建读取器前等待本轮保存真正完成。
            IOUtilities.waitUntilIOWorkerComplete();
            Path dataDir = overworld.getServer().getWorldPath(LevelResource.ROOT).resolve("data");
            SoulMotorData fromDisk = new DimensionDataStorage(dataDir.toFile(), overworld.getServer().getFixerUpper(), overworld.registryAccess())
                    .get(SoulMotorData.FACTORY, "croety_soul_motors");
            helper.assertTrue(fromDisk != null && fromDisk.activeCount(owner) == 3 && !fromDisk.contains(oldest),
                    "SavedData 从磁盘重新读取后应保留跨维度上限和淘汰状态");
            CompoundTag reloaded = fromDisk.save(new CompoundTag(), overworld.registryAccess());
            helper.assertTrue(reloaded.getList("Motors", 10).stream().anyMatch(row -> {
                CompoundTag entry = (CompoundTag) row;
                return entry.getLong("Id") == oldest && entry.getBoolean("Removed")
                        && entry.getString("Dimension").equals(Level.NETHER.location().toString())
                        && BlockPos.of(entry.getLong("Pos")).equals(unloaded);
            }), "磁盘数据应保留未加载下界坐标与待清理标记");
            helper.succeed();
        } finally {
            for (long id : ids) if (id != 0) data.forget(id);
            overworld.getDataStorage().save();
        }
    }

    @GameTest(template = "empty")
    public static void fourthMotorInOtherDimensionRemovesFirstLoadedMotor(GameTestHelper helper) {
        ServerLevel overworld = helper.getLevel();
        ServerLevel nether = overworld.getServer().getLevel(Level.NETHER);
        helper.assertTrue(nether != null, "测试服务端必须有下界");
        UUID owner = UUID.randomUUID();
        SoulMotorData data = SoulMotorData.get(overworld.getServer());
        BlockPos netherPos = new BlockPos(0, 200, 0);
        BlockState previous = nether.getBlockState(netherPos);
        nether.setBlock(netherPos, MotorContent.SOUL_MOTOR.get().defaultBlockState(), 3);
        SoulMotorBlockEntity netherMotor = (SoulMotorBlockEntity) nether.getBlockEntity(netherPos);
        long[] ids = new long[4];
        try {
            ids[0] = data.add(owner, nether, netherPos);
            netherMotor.setSummoned(owner, ids[0], overworld.getGameTime() + SoulMotorData.LIFETIME);
            for (int i = 1; i <= 3; i++) {
                BlockPos pos = helper.absolutePos(new BlockPos(i, 2, 1));
                overworld.setBlock(pos, MotorContent.SOUL_MOTOR.get().defaultBlockState(), 3);
                SoulMotorBlockEntity motor = (SoulMotorBlockEntity) overworld.getBlockEntity(pos);
                ids[i] = data.add(owner, overworld, pos);
                motor.setSummoned(owner, ids[i], overworld.getGameTime() + SoulMotorData.LIFETIME);
            }
            data.tick(overworld.getServer());
            helper.assertTrue(data.activeCount(owner) == 3 && nether.getBlockState(netherPos).isAir(),
                    "第四台位于主世界时，应删除下界已加载的最早马达");
            helper.succeed();
        } finally {
            nether.setBlock(netherPos, previous, 3);
            for (long id : ids) if (id != 0) data.forget(id);
        }
    }

    @GameTest(template = "empty")
    public static void wrenchRotateAndReclaimPlusNormalBreakProduceNoDrops(GameTestHelper helper) {
        BlockPos relative = new BlockPos(1, 2, 1);
        BlockPos pos = helper.absolutePos(relative);
        FakePlayer player = new FakePlayer(helper.getLevel(), new GameProfile(UUID.randomUUID(), "motor_wrench"));
        ItemStack wrench = new ItemStack(AllItems.WRENCH.get());
        player.setItemInHand(InteractionHand.MAIN_HAND, wrench);
        helper.setBlock(relative, MotorContent.SOUL_MOTOR.get().defaultBlockState().setValue(SoulMotorBlock.FACING, Direction.NORTH));
        helper.assertTrue(Block.getDrops(helper.getLevel().getBlockState(pos), helper.getLevel(), pos,
                helper.getLevel().getBlockEntity(pos), player, wrench).isEmpty(), "马达掉落表必须为空");
        UseOnContext context = new UseOnContext(helper.getLevel(), player, InteractionHand.MAIN_HAND, wrench,
                new BlockHitResult(Vec3.atCenterOf(pos), Direction.UP, pos, false));
        wrench.useOn(context);
        helper.assertTrue(helper.getLevel().getBlockState(pos).is(MotorContent.SOUL_MOTOR.get())
                        && helper.getLevel().getBlockState(pos).getValue(SoulMotorBlock.FACING) != Direction.NORTH,
                "Create扳手应旋转马达而不拆除");
        player.setShiftKeyDown(true);
        wrench.useOn(context);
        helper.assertTrue(helper.getLevel().getBlockState(pos).isAir(), "潜行扳手应拆除马达");
        helper.assertTrue(player.getInventory().items.stream().noneMatch(stack -> stack.is(MotorContent.SOUL_MOTOR_ITEM.get())),
                "扳手拆除不能将马达放回背包");
        helper.setBlock(relative, MotorContent.SOUL_MOTOR.get());
        helper.getLevel().destroyBlock(pos, true, player, 512);
        helper.assertTrue(helper.getLevel().getBlockState(pos).isAir(), "普通破坏应移除马达");
        helper.assertTrue(helper.getLevel().getEntitiesOfClass(ItemEntity.class, new AABB(pos).inflate(1)).isEmpty(),
                "普通破坏和扳手拆除均不能产生掉落实体");
        helper.succeed();
    }
}
