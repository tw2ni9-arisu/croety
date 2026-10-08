package com.croety.gametest;

import com.Polarice3.Goety.common.items.ModItems;
import com.croety.content.fluid.SoulFluidContent;
import com.croety.content.motor.MotorContent;
import com.croety.content.motor.SoulMotorBlock;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.content.kinetics.base.RotatedPillarKineticBlock;
import com.simibubi.create.content.processing.basin.BasinBlockEntity;
import com.simibubi.create.content.processing.burner.BlazeBurnerBlock;
import com.simibubi.create.content.processing.burner.BlazeBurnerBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder("croety")
@PrefixGameTestTemplate(false)
public class SoulMixingTests {
    private static BasinBlockEntity machine(GameTestHelper h) {
        h.setBlock(new BlockPos(2, 1, 2), AllBlocks.BASIN.get());
        h.setBlock(new BlockPos(2, 3, 2), AllBlocks.MECHANICAL_MIXER.get());
        h.setBlock(new BlockPos(3, 3, 2), AllBlocks.COGWHEEL.get().defaultBlockState().setValue(RotatedPillarKineticBlock.AXIS, Direction.Axis.Y));
        h.setBlock(new BlockPos(3, 2, 2), MotorContent.SOUL_MOTOR.get().defaultBlockState().setValue(SoulMotorBlock.FACING, Direction.UP));
        return (BasinBlockEntity) h.getBlockEntity(new BlockPos(2, 1, 2));
    }
    private static void heat(GameTestHelper h) {
        h.setBlock(new BlockPos(2, 0, 2), AllBlocks.BLAZE_BURNER.get().defaultBlockState().setValue(BlazeBurnerBlock.HEAT_LEVEL, BlazeBurnerBlock.HeatLevel.KINDLED));
        ((BlazeBurnerBlockEntity) h.getBlockEntity(new BlockPos(2, 0, 2))).isCreative = true;
    }
    private static int outputItems(BasinBlockEntity basin, Item item) {
        int count = 0;
        for (int i = 0; i < basin.getOutputInventory().getSlots(); i++) {
            ItemStack stack = basin.getOutputInventory().getStackInSlot(i);
            if (stack.is(item)) count += stack.getCount();
        }
        return count;
    }
    private static void liquidRecipe(GameTestHelper h, Item ingredient, int expected) {
        BasinBlockEntity basin = machine(h);
        basin.getInputInventory().insertItem(0, new ItemStack(ingredient), false);
        h.runAtTickTime(80, () -> {
            h.assertTrue(basin.getTanks().getSecond().isEmpty(), "未加热时不能生成液态灵魂");
            heat(h);
        });
        h.succeedWhen(() -> h.assertTrue(basin.getTanks().getSecond().getPrimaryHandler().getFluidAmount() == expected
                && basin.getInputInventory().isEmpty(), "实际搅拌应产生正确液量并消耗原料"));
    }
    @GameTest(template = "empty", timeoutTicks = 350)
    public static void ectoplasmMixesIntoFiveMillibuckets(GameTestHelper h) { liquidRecipe(h, ModItems.ECTOPLASM.get(), 5); }
    @GameTest(template = "empty", timeoutTicks = 350)
    public static void soulSandMixesIntoTwentyFiveMillibuckets(GameTestHelper h) { liquidRecipe(h, Items.SOUL_SAND, 25); }
    @GameTest(template = "empty", timeoutTicks = 350)
    public static void soulEmeraldMixesWithoutHeat(GameTestHelper h) {
        BasinBlockEntity basin = machine(h);
        basin.getInputInventory().insertItem(0, new ItemStack(Items.EMERALD, 4), false);
        basin.inputTank.getPrimaryHandler().fill(new FluidStack(SoulFluidContent.SOUL.get(), 25), FluidAction.EXECUTE);
        h.succeedWhen(() -> h.assertTrue(outputItems(basin, ModItems.SOUL_EMERALD.get()) == 4 && basin.inputTank.isEmpty()
                && basin.getInputInventory().isEmpty(), "无加热实际搅拌应消耗25mB与4绿宝石，产出4灵魂绿宝石"));
    }
    @GameTest(template = "empty", timeoutTicks = 350)
    public static void cursedMetalMixesWithHeat(GameTestHelper h) {
        BasinBlockEntity basin = machine(h);
        basin.getInputInventory().insertItem(0, new ItemStack(Items.GOLD_INGOT, 2), false);
        basin.getInputInventory().insertItem(1, new ItemStack(Items.IRON_INGOT, 2), false);
        basin.inputTank.getPrimaryHandler().fill(new FluidStack(SoulFluidContent.SOUL.get(), 25), FluidAction.EXECUTE);
        h.runAtTickTime(80, () -> {
            h.assertTrue(outputItems(basin, ModItems.CURSED_METAL_INGOT.get()) == 0, "未加热不能合成诅咒金属");
            heat(h);
        });
        h.succeedWhen(() -> h.assertTrue(outputItems(basin, ModItems.CURSED_METAL_INGOT.get()) == 4 && basin.inputTank.isEmpty()
                && basin.getInputInventory().isEmpty(), "加热搅拌应消耗25mB与金铁，产出4诅咒金属：input="
                + basin.getInputInventory().serializeNBT(h.getLevel().registryAccess()) + ", output=" + basin.getOutputInventory().serializeNBT(h.getLevel().registryAccess())
                + ", fluid=" + basin.inputTank.getPrimaryHandler().getFluid() + ", mixer="
                + ((com.simibubi.create.content.kinetics.mixer.MechanicalMixerBlockEntity) h.getBlockEntity(new BlockPos(2, 3, 2))).getSpeed()));
    }
}
