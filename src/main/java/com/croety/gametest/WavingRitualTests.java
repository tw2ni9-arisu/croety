package com.croety.gametest;

import com.Polarice3.Goety.api.items.magic.ITotem;
import com.Polarice3.Goety.common.blocks.ModBlocks;
import com.Polarice3.Goety.common.blocks.entities.CursedCageBlockEntity;
import com.Polarice3.Goety.common.blocks.entities.DarkAltarBlockEntity;
import com.Polarice3.Goety.common.blocks.entities.PedestalBlockEntity;
import com.Polarice3.Goety.common.items.ModItems;
import com.croety.content.motor.MotorContent;
import com.mojang.authlib.GameProfile;
import com.simibubi.create.AllBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import java.util.UUID;

@GameTestHolder("croety")
@PrefixGameTestTemplate(false)
public class WavingRitualTests {
    @GameTest(template = "empty", timeoutTicks = 380, batch = "croety_ritual")
    public static void forgeRitualCompletesAndSpends4096Souls(GameTestHelper h) {
        BlockPos altarPos = new BlockPos(3, 2, 3);
        h.setBlock(altarPos.below(), ModBlocks.CURSED_CAGE_BLOCK.get());
        h.setBlock(altarPos, ModBlocks.DARK_ALTAR.get());
        h.setBlock(new BlockPos(1, 1, 1), Blocks.LAVA_CAULDRON);
        h.setBlock(new BlockPos(2, 0, 1), Blocks.STONE);
        h.setBlock(new BlockPos(2, 1, 1), Blocks.ANVIL);
        h.setBlock(new BlockPos(3, 1, 1), Blocks.FURNACE);
        h.setBlock(new BlockPos(4, 1, 1), Blocks.FURNACE);
        BlockPos[] positions = {new BlockPos(1, 2, 3), new BlockPos(5, 2, 3), new BlockPos(3, 2, 1), new BlockPos(3, 2, 5)};
        ItemStack[] ingredients = {new ItemStack(ModBlocks.HAUNTED_JUG.get()), new ItemStack(ModItems.TIDAL_FOCUS.get()),
                new ItemStack(AllBlocks.WINDMILL_BEARING.get()), new ItemStack(Items.BLUE_WOOL)};
        for (int i = 0; i < positions.length; i++) {
            h.setBlock(positions[i], ModBlocks.PEDESTAL.get());
            ((PedestalBlockEntity) h.getBlockEntity(positions[i])).itemStackHandler.insertItem(0, ingredients[i], false);
        }
        ItemStack totem = new ItemStack(ModItems.TOTEM_OF_SOULS.get());
        ITotem.setMaxSoulAmount(totem, 10000); ITotem.setSoulsamount(totem, 10000);
        ((CursedCageBlockEntity) h.getBlockEntity(altarPos.below())).setItem(totem);
        FakePlayer player = new FakePlayer(h.getLevel(), new GameProfile(UUID.randomUUID(), "ritual_test"));
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(AllBlocks.LARGE_WATER_WHEEL.get()));
        DarkAltarBlockEntity altar = (DarkAltarBlockEntity) h.getBlockEntity(altarPos);
        h.assertTrue(altar.activate(h.getLevel(), h.absolutePos(altarPos), player, InteractionHand.MAIN_HAND, Direction.UP)
                && altar.getCurrentRitualRecipe() != null, "原生锻造仪式必须能用大型水车激活");
        h.succeedWhen(() -> {
            ItemStack result = altar.itemStackHandler.getStackInSlot(0);
            h.assertTrue(result.is(MotorContent.WAVING_FOCUS.get()), "实际16秒仪式必须产出涌动聚晶");
            h.assertTrue(ITotem.currentSouls(totem) == 5904, "16秒每秒256应精确消耗4096灵魂");
            for (BlockPos pos : positions)
                h.assertTrue(((PedestalBlockEntity) h.getBlockEntity(pos)).itemStackHandler.getStackInSlot(0).isEmpty(), "周边四项材料必须消耗");
        });
    }
}
