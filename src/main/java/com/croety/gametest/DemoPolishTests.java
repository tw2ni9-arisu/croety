package com.croety.gametest;

import com.Polarice3.Goety.config.MainConfig;
import com.Polarice3.Goety.utils.SEHelper;
import com.croety.content.motor.MotorContent;
import com.croety.content.motor.SoulMotorBlockEntity;
import com.croety.content.motor.SoulMotorData;
import com.croety.content.orb.SoulOrb;
import com.mojang.authlib.GameProfile;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import java.util.UUID;

@GameTestHolder("croety")
@PrefixGameTestTemplate(false)
public class DemoPolishTests {
    @GameTest(template = "empty")
    public static void negativePartialPickupPreservesRemainder(GameTestHelper h) {
        SoulOrb orb = new SoulOrb(h.getLevel(), h.absoluteVec(new Vec3(3, 2, 3)), -7);
        FakePlayer player = new FakePlayer(h.getLevel(), new GameProfile(UUID.randomUUID(), "negative_remainder"));
        SEHelper.setSEActive(player, true); SEHelper.setSESouls(player, 3);
        orb.playerTouch(player);
        h.assertTrue(!orb.isRemoved() && orb.getValue() == -4 && SEHelper.getSESouls(player) == 0, "负球只扣到0时必须保留剩余负价值");
        player.takeXpDelay = 0; orb.playerTouch(player);
        h.assertTrue(!orb.isRemoved() && orb.getValue() == -4, "没有可扣灵魂时不得消耗负球");
        SEHelper.setSESouls(player, 10); player.takeXpDelay = 0; orb.playerTouch(player);
        h.assertTrue(orb.isRemoved() && SEHelper.getSESouls(player) == 6, "再次有足够灵魂时精确扣除剩余4");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void loweringCapacityCannotMakePositivePickupDrainPlayer(GameTestHelper h) {
        int previousMaximum = MainConfig.MaxArcaSouls.get();
        try {
            MainConfig.MaxArcaSouls.set(10000);
            FakePlayer player = new FakePlayer(h.getLevel(), new GameProfile(UUID.randomUUID(), "lowered_capacity"));
            SEHelper.setSEActive(player, true); SEHelper.setSESouls(player, 12000);
            SoulOrb orb = new SoulOrb(h.getLevel(), h.absoluteVec(new Vec3(3, 2, 3)), 50);
            orb.playerTouch(player);
            h.assertTrue(SEHelper.getSESouls(player) == 12000 && orb.getValue() == 50 && !orb.isRemoved(), "降低上限后拾取正球不得扣能量或增大球");
            com.croety.content.PlayerSouls.change(player, -500, false);
            h.assertTrue(SEHelper.getSESouls(player) == 11500, "负向交易只扣请求量，不顺带裁掉超过配置上限的余额");
        } finally { MainConfig.MaxArcaSouls.set(previousMaximum); }
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void differentValuesMergeAndPartialPickupKeepsRemainder(GameTestHelper h) {
        Vec3 pos = h.absoluteVec(new Vec3(3, 2, 3));
        SoulOrb first = new SoulOrb(h.getLevel(), pos, 7);
        SoulOrb second = new SoulOrb(h.getLevel(), pos, 17);
        h.getLevel().addFreshEntity(first); h.getLevel().addFreshEntity(second);
        first.tickCount = 1; first.tick();
        h.assertTrue(second.isRemoved() && first.getValue() == 24 && first.getIcon() == 3, "不同价值的小球应合并为24灵魂大球");
        FakePlayer player = new FakePlayer(h.getLevel(), new GameProfile(UUID.randomUUID(), "orb_capacity"));
        SEHelper.setSEActive(player, true); SEHelper.setSESouls(player, MainConfig.MaxArcaSouls.get() - 10);
        first.playerTouch(player);
        h.assertTrue(first.getValue() == 14 && !first.isRemoved() && SEHelper.getSESouls(player) == MainConfig.MaxArcaSouls.get(), "只能容纳10时，原球保留14，不拆成小球或丢失");
        SEHelper.setSESouls(player, 0); player.takeXpDelay = 0;
        first.playerTouch(player);
        h.assertTrue(first.isRemoved() && SEHelper.getSESouls(player) == 14, "容量足够时一次拿走全部剩余灵魂");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void oppositeSignedOrbsStaySeparate(GameTestHelper h) {
        Vec3 pos = h.absoluteVec(new Vec3(3, 2, 3));
        SoulOrb positive = new SoulOrb(h.getLevel(), pos, 17);
        SoulOrb negative = new SoulOrb(h.getLevel(), pos, -7);
        h.getLevel().addFreshEntity(positive); h.getLevel().addFreshEntity(negative);
        positive.tickCount = 1; positive.tick();
        h.assertTrue(!negative.isRemoved() && positive.getValue() == 17 && negative.getValue() == -7, "正负球不得互相抵消");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void gogglesShowLifetimeAndRoundUpSeconds(GameTestHelper h) {
        BlockPos pos = new BlockPos(2, 2, 2);
        h.setBlock(pos, MotorContent.SOUL_MOTOR.get());
        SoulMotorBlockEntity motor = (SoulMotorBlockEntity) h.getBlockEntity(pos);

        h.assertTrue(motor.getLifetimeText().getString().contains("Permanent"), "永久马达应显示永久而非假倒计时");
        UUID owner = UUID.randomUUID();
        var data = SoulMotorData.get(h.getLevel().getServer());
        long id = data.add(owner, h.getLevel(), h.absolutePos(pos));
        long now = h.getLevel().getServer().overworld().getGameTime();
        motor.setSummoned(owner, id, now + 2500);
        h.assertTrue(motor.getLifetimeText().getString().contains("2:05"), "剩余2500刻应显示2:05");
        motor.setSummoned(owner, id, now + 1);
        h.assertTrue(motor.getLifetimeText().getString().contains("0:01"), "最后一刻应向上取整显示0:01");
        data.forget(id);
        h.succeed();
    }
}
