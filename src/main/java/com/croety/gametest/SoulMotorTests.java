package com.croety.gametest;

import com.croety.content.motor.MotorContent;
import com.croety.content.motor.SoulMotorBlock;
import com.croety.content.motor.SoulMotorBlockEntity;
import com.croety.content.motor.SoulMotorData;
import com.Polarice3.Goety.common.crafting.RitualRecipe;
import com.simibubi.create.api.stress.BlockStressValues;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import java.util.List;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import net.minecraftforge.registries.ForgeRegistries;

@GameTestHolder("croety")
@PrefixGameTestTemplate(false)
public class SoulMotorTests {
    @GameTest(template = "empty")
    public static void motorAndFocusAreRegistered(GameTestHelper helper) {
        helper.assertTrue(ForgeRegistries.BLOCKS.containsKey(new ResourceLocation("croety", "soul_motor")), "灵魂马达必须注册");
        helper.assertTrue(ForgeRegistries.ITEMS.containsKey(new ResourceLocation("croety", "waving_focus")), "涌动聚晶必须注册");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void sixDirectionsProduce128RpmAnd64StressPerRpm(GameTestHelper helper) {
        BlockPos pos = new BlockPos(1, 2, 1);
        helper.assertTrue(BlockStressValues.getCapacity(MotorContent.SOUL_MOTOR.get()) == 64,
                "马达在每RPM应提供64SU");
        for (Direction direction : Direction.values()) {
            helper.setBlock(pos, MotorContent.SOUL_MOTOR.get().defaultBlockState().setValue(SoulMotorBlock.FACING, direction));
            SoulMotorBlockEntity motor = (SoulMotorBlockEntity) helper.getLevel().getBlockEntity(helper.absolutePos(pos));
            helper.assertTrue(motor != null && Math.abs(motor.getGeneratedSpeed()) == 128,
                    "六个方向均应输出128RPM");
            helper.assertTrue(helper.getLevel().getBlockState(helper.absolutePos(pos)).getLightEmission() == 4,
                    "马达光照应为4");
        }
        helper.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 100)
    public static void motorDrivesShaftWith8192StressCapacity(GameTestHelper helper) {
        BlockPos motorPos = new BlockPos(1, 2, 1);
        BlockPos shaftPos = new BlockPos(2, 2, 1);
        helper.setBlock(motorPos, MotorContent.SOUL_MOTOR.get().defaultBlockState().setValue(SoulMotorBlock.FACING, Direction.EAST));
        helper.setBlock(shaftPos, AllBlocks.SHAFT.getDefaultState().setValue(RotatedPillarBlock.AXIS, Direction.Axis.X));
        helper.runAfterDelay(10, () -> {
            SoulMotorBlockEntity motor = (SoulMotorBlockEntity) helper.getLevel().getBlockEntity(helper.absolutePos(motorPos));
            KineticBlockEntity shaft = (KineticBlockEntity) helper.getLevel().getBlockEntity(helper.absolutePos(shaftPos));
            helper.assertTrue(motor != null && shaft != null && Math.abs(motor.getSpeed()) == 128
                            && Math.abs(shaft.getSpeed()) == 128, "输出轴应实际以128RPM带动相邻传动轴");
            helper.assertTrue(motor.getOrCreateNetwork().calculateCapacity() == 8192,
                    "动力网络实际应有8192SU容量");
            helper.succeed();
        });
    }

    @GameTest(template = "empty")
    public static void fourthSummonedMotorEvictsOldestAndKeepsPermanentMotor(GameTestHelper helper) {
        UUID owner = UUID.randomUUID();
        SoulMotorData data = SoulMotorData.get(helper.getLevel().getServer());
        BlockPos[] positions = {new BlockPos(1, 2, 1), new BlockPos(2, 2, 1),
                new BlockPos(1, 2, 2), new BlockPos(2, 2, 2)};
        for (BlockPos pos : positions) {
            helper.setBlock(pos, MotorContent.SOUL_MOTOR.get());
            SoulMotorBlockEntity motor = (SoulMotorBlockEntity) helper.getLevel().getBlockEntity(helper.absolutePos(pos));
            long id = data.add(owner, helper.getLevel(), helper.absolutePos(pos));
            motor.setSummoned(owner, id, helper.getLevel().getServer().overworld().getGameTime() + SoulMotorData.LIFETIME);
        }
        helper.assertTrue(data.activeCount(owner) == 3, "第四台应淘汰最早一台");
        CompoundTag saved = data.save(new CompoundTag());
        helper.assertTrue(saved.getList("Motors", 10).stream().anyMatch(tag -> ((CompoundTag) tag).getBoolean("Removed")),
                "淘汰标记必须持久化，供区块重新加载后清除");
        data.tick(helper.getLevel().getServer());
        helper.assertTrue(helper.getLevel().getBlockState(helper.absolutePos(positions[0])).isAir(),
                "已加载的最早马达应消失");
        for (int i = 1; i < positions.length; i++)
            helper.assertTrue(helper.getLevel().getBlockState(helper.absolutePos(positions[i])).is(MotorContent.SOUL_MOTOR.get()),
                    "其余三台应保留");
        BlockPos permanent = new BlockPos(1, 2, 3);
        helper.setBlock(permanent, MotorContent.SOUL_MOTOR.get());
        data.tick(helper.getLevel().getServer());
        helper.assertTrue(helper.getLevel().getBlockState(helper.absolutePos(permanent)).is(MotorContent.SOUL_MOTOR.get()),
                "非施法放置的马达不应计入或受生命周期限制");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void forgeRitualUsesWaterWheelAndAllSailVariants(GameTestHelper helper) {
        var recipe = helper.getLevel().getRecipeManager().byKey(new ResourceLocation("croety", "waving_focus")).orElse(null);
        helper.assertTrue(recipe instanceof RitualRecipe, "涌动聚晶锻造仪式必须载入");
        RitualRecipe ritual = (RitualRecipe) recipe;
        helper.assertTrue("forge".equals(ritual.getCraftType()) && ritual.getSoulCost() == 256 && ritual.getDuration() == 16,
                "仪式需每秒256灵魂并持续16秒");
        helper.assertTrue(ritual.getActivationItem().test(new ItemStack(ForgeRegistries.ITEMS.getValue(new ResourceLocation("create", "large_water_wheel")))),
                "中心物品必须是大型水车");
        List<String> colors = List.of("white", "orange", "magenta", "light_blue", "yellow", "lime", "pink", "gray",
                "light_gray", "cyan", "purple", "blue", "brown", "green", "red", "black");
        for (String color : colors) {
            var sail = ForgeRegistries.BLOCKS.getValue(new ResourceLocation("create", color + "_sail"));
            helper.assertTrue(ritual.getIngredients().stream().anyMatch(ingredient -> ingredient.test(
                    new ItemStack(sail.asItem()))), "仪式必须接受" + color + "风帆对应的物品");
        }
        helper.assertTrue(ritual.getIngredients().stream().anyMatch(ingredient -> ingredient.test(
                new ItemStack(ForgeRegistries.ITEMS.getValue(new ResourceLocation("create", "sail_frame"))))),
                "仪式必须接受风帆框架");
        for (var sail : ForgeRegistries.BLOCKS.tags().getTag(com.simibubi.create.AllTags.AllBlockTags.WINDMILL_SAILS.tag)) {
            helper.assertTrue(ritual.getIngredients().stream().anyMatch(ingredient -> ingredient.test(new ItemStack(sail.asItem()))),
                    "仪式物品标签必须覆盖Create本体风帆方块标签对应的物品");
        }
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void expiredMotorCannotReenergizeOnReload(GameTestHelper helper) {
        BlockPos pos = new BlockPos(1, 2, 1);
        helper.setBlock(pos, MotorContent.SOUL_MOTOR.get());
        SoulMotorBlockEntity motor = (SoulMotorBlockEntity) helper.getLevel().getBlockEntity(helper.absolutePos(pos));
        UUID owner = UUID.randomUUID();
        long id = SoulMotorData.get(helper.getLevel().getServer()).add(owner, helper.getLevel(), helper.absolutePos(pos));
        motor.setSummoned(owner, id, helper.getLevel().getServer().overworld().getGameTime() - 1);
        helper.assertTrue(motor.getGeneratedSpeed() == 0, "过期马达重新加载前不能输出转速");
        motor.initialize();
        helper.assertTrue(helper.getLevel().getBlockState(helper.absolutePos(pos)).isAir(), "过期马达重新加载应立即清除");
        helper.succeed();
    }
}
