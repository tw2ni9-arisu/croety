package com.croety.gametest;

import com.Polarice3.Goety.common.items.ModItems;
import com.Polarice3.Goety.common.items.handler.SoulUsingItemHandler;
import com.Polarice3.Goety.common.items.magic.DarkWand;
import com.Polarice3.Goety.utils.SEHelper;
import com.croety.content.motor.MotorContent;
import com.croety.content.spell.WavingSpell;
import com.mojang.authlib.GameProfile;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import java.util.UUID;

@GameTestHolder("croety")
@PrefixGameTestTemplate(false)
public class WavingSpellTests {
    @GameTest(template = "empty")
    public static void wandCastsInstantlyAndEnforcesCooldown(GameTestHelper h) {
        FakePlayer player = new FakePlayer(h.getLevel(), new GameProfile(UUID.randomUUID(), "waving_test"));
        player.setPos(h.absoluteVec(new Vec3(2.5, 1, 1.5)));
        player.setYRot(0); player.setXRot(0);
        h.setBlock(new BlockPos(2, 2, 5), Blocks.STONE);
        SEHelper.setSEActive(player, true); SEHelper.setSESouls(player, 10000);
        ItemStack staff = new ItemStack(ModItems.DARK_WAND.get());
        ItemStack focus = new ItemStack(MotorContent.WAVING_FOCUS.get());
        SoulUsingItemHandler.get(staff).insertItem(focus);
        player.setItemInHand(InteractionHand.MAIN_HAND, staff);
        DarkWand wand = (DarkWand) staff.getItem();
        wand.inventoryTick(staff, h.getLevel(), player, 0, true);
        int cost = wand.SoulUse(player, staff);
        h.assertTrue(cost > 0, "法杖必须读取聚晶灵魂消耗");
        wand.use(h.getLevel(), player, InteractionHand.MAIN_HAND);
        h.assertTrue(h.getBlockState(new BlockPos(2, 2, 4)).is(MotorContent.SOUL_MOTOR.get()), "法杖右键应瞬发产生马达");
        h.assertTrue(SEHelper.getSESouls(player) == 10000 - cost, "必须通过原生法杖扣除灵魂");
        h.assertTrue(SEHelper.isOnCooldown(player, focus), "施法后必须进入聚晶冷却");
        int after = SEHelper.getSESouls(player);
        wand.use(h.getLevel(), player, InteractionHand.MAIN_HAND);
        h.assertTrue(SEHelper.getSESouls(player) == after, "冷却中再次使用不能重复扣除或施法");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void fourthSpellImmediatelyRemovesFirstMotor(GameTestHelper h) {
        FakePlayer player = new FakePlayer(h.getLevel(), new GameProfile(UUID.randomUUID(), "limit_test"));
        WavingSpell spell = new WavingSpell();
        for (int x = 1; x <= 4; x++) {
            player.setPos(h.absoluteVec(new Vec3(x + .5, 1, 1.5)));
            player.setYRot(0); player.setXRot(0);
            h.setBlock(new BlockPos(x, 2, 5), Blocks.STONE);
            spell.SpellResult(h.getLevel(), player, ItemStack.EMPTY, spell.defaultStats());
            h.assertTrue(h.getBlockState(new BlockPos(x, 2, 4)).is(MotorContent.SOUL_MOTOR.get()), "每次施法都应放置马达");
        }
        h.assertTrue(h.getBlockState(new BlockPos(1, 2, 4)).isAir(), "第四次施法结束时第一台应已消失");
        h.succeed();
    }
}
