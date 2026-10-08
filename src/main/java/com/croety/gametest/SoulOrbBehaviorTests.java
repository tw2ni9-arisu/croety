package com.croety.gametest;

import com.Polarice3.Goety.utils.SEHelper;
import com.croety.content.orb.OrbContent;
import com.croety.content.orb.SoulOrb;
import com.mojang.authlib.GameProfile;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import java.util.UUID;

@GameTestHolder("croety")
@PrefixGameTestTemplate(false)
public class SoulOrbBehaviorTests {
    @GameTest(template = "empty", timeoutTicks = 110, batch = "croety_water")
    public static void waterBuoyancyRaisesOrb(GameTestHelper h) {
        for (int x = 2; x <= 4; x++) for (int z = 2; z <= 4; z++) {
            h.setBlock(new BlockPos(x, 0, z), Blocks.STONE);
            for (int y = 1; y <= 3; y++) h.setBlock(new BlockPos(x, y, z), Blocks.WATER);
        }
        Vec3 start = h.absoluteVec(new Vec3(3.5, 1.2, 3.5));
        SoulOrb orb = new SoulOrb(h.getLevel(), start, 1);
        orb.setDeltaMovement(Vec3.ZERO);
        h.getLevel().addFreshEntity(orb);
        h.runAfterDelay(80, () -> {
            h.assertTrue(!orb.isRemoved() && orb.getY() > start.y + .25, "水中灵魂球应受浮力上升而非沉底");
            h.succeed();
        });
    }

    @GameTest(template = "empty")
    public static void stackPickupAndSavingPreserveEnergy(GameTestHelper h) {
        SoulOrb orb = OrbContent.SOUL_ORB.get().create(h.getLevel());
        CompoundTag saved = new CompoundTag();
        saved.putInt("Value", 7); saved.putInt("Count", 2); saved.putInt("Health", 5); saved.putInt("Age", 99);
        orb.readAdditionalSaveData(saved);
        FakePlayer player = new FakePlayer(h.getLevel(), new GameProfile(UUID.randomUUID(), "orb_test"));
        SEHelper.setSEActive(player, true); SEHelper.setSESouls(player, 100);
        h.assertTrue(orb.getValue() == 14 && orb.getIcon() == 2, "旧存档两份7灵魂应变为14灵魂的大球");
        CompoundTag output = new CompoundTag(); orb.addAdditionalSaveData(output);
        SoulOrb restored = OrbContent.SOUL_ORB.get().create(h.getLevel()); restored.readAdditionalSaveData(output);
        h.assertTrue(restored.getValue() == 14 && restored.getAge() == 99, "保存重载应保留总价值、寿命");
        restored.playerTouch(player);
        h.assertTrue(SEHelper.getSESouls(player) == 114 && restored.isRemoved() && player.totalExperience == 0, "一次拾取必须获取整颗球的14灵魂");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void valueBoundariesSelectElevenIcons(GameTestHelper h) {
        int[] values = {-10, 1, 2, 3, 6, 7, 16, 17, 36, 37, 72, 73, 148, 149, 306, 307, 616, 617, 1236, 1237, 2476, 2477, 100000};
        int[] icons = {0, 0, 0, 1, 1, 2, 2, 3, 3, 4, 4, 5, 5, 6, 6, 7, 7, 8, 8, 9, 9, 10, 10};
        for (int i = 0; i < values.length; i++) {
            SoulOrb orb = new SoulOrb(h.getLevel(), Vec3.ZERO, values[i]);
            h.assertTrue(orb.getIcon() == icons[i], "灵魂球外观档位边界错误：" + values[i]);
            CompoundTag nbt = new CompoundTag(); orb.addAdditionalSaveData(nbt);
            SoulOrb restored = OrbContent.SOUL_ORB.get().create(h.getLevel()); restored.readAdditionalSaveData(nbt);
            h.assertTrue(restored.getValue() == values[i], "存档不能把大数或负数截为short");
        }
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void nearbyAwardMergesWithoutLosingValue(GameTestHelper h) {
        Vec3 center = h.absoluteVec(new Vec3(3, 2, 3));
        for (int i = 0; i < 200; i++) SoulOrb.award(h.getLevel(), center, 1);
        var orbs = h.getLevel().getEntitiesOfClass(SoulOrb.class, AABB.ofSize(center, 2, 2, 2));
        h.assertTrue(orbs.size() == 1, "相邻同价值灵魂球应堆叠");
        h.assertTrue(orbs.stream().mapToLong(SoulOrb::getValue).sum() == 200, "合并必须保留总能量");
        h.assertTrue(h.getLevel().getEntitiesOfClass(net.minecraft.world.entity.ExperienceOrb.class, AABB.ofSize(center, 2, 2, 2)).isEmpty(), "不能混入原版经验球");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void mergeKeepsYoungerAge(GameTestHelper h) {
        Vec3 center = h.absoluteVec(new Vec3(3, 2, 3));
        SoulOrb old = new SoulOrb(h.getLevel(), center, 3);
        SoulOrb young = new SoulOrb(h.getLevel(), center, 3);
        CompoundTag a = new CompoundTag(); a.putInt("Value", 3); a.putInt("Age", 4000); a.putInt("Health", 5);
        CompoundTag b = a.copy(); b.putInt("Age", 100);
        old.readAdditionalSaveData(a); young.readAdditionalSaveData(b);
        h.getLevel().addFreshEntity(old); h.getLevel().addFreshEntity(young);
        old.tickCount = 1;
        old.tick();
        h.assertTrue(young.isRemoved() && old.getValue() == 6 && old.getAge() == 101, "合并应继承更长剩余寿命并保留两份价值");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void environmentalDamageAndAttackPassThrough(GameTestHelper h) {
        SoulOrb orb = new SoulOrb(h.getLevel(), h.absoluteVec(new Vec3(2, 2, 2)), 1);
        h.assertTrue(!orb.isAttackable() && !orb.isPickable(), "近战和弹射物应穿过灵魂球");
        FakePlayer player = new FakePlayer(h.getLevel(), new GameProfile(UUID.randomUUID(), "damage_test"));
        h.assertTrue(!orb.hurt(h.getLevel().damageSources().playerAttack(player), 20), "玩家直接攻击不能伤害球");
        var arrow = EntityType.ARROW.create(h.getLevel());
        h.assertTrue(!orb.hurt(h.getLevel().damageSources().arrow(arrow, player), 20), "弹射物不能伤害球");
        orb.hurt(h.getLevel().damageSources().cactus(), 4);
        h.assertTrue(!orb.isRemoved(), "5点生命值应承受4点伤害");
        orb.hurt(h.getLevel().damageSources().onFire(), 1);
        h.assertTrue(orb.isRemoved(), "环境伤害累计5点应摧毁球");
        h.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 80, batch = "croety_sun")
    public static void exposedSunlightBurnsAndDestroysOrb(GameTestHelper h) {
        h.setDayTime(6000);
        h.getLevel().setWeatherParameters(6000, 0, false, false);
        BlockPos pos = new BlockPos(2, 2, 2);
        h.setBlock(pos.below(), Blocks.STONE);
        // 测试世界会复用旧结构；显式清空上方遮挡，保证测的是日晒而非阴影。
        BlockPos column = h.absolutePos(pos);
        for (int y = column.getY() + 1; y < h.getLevel().getMaxBuildHeight(); y++)
            h.getLevel().setBlock(new BlockPos(column.getX(), y, column.getZ()), Blocks.AIR.defaultBlockState(), 3);
        SoulOrb orb = new SoulOrb(h.getLevel(), h.absoluteVec(Vec3.atCenterOf(pos)), 1);
        orb.setNoGravity(true); orb.setDeltaMovement(Vec3.ZERO);
        h.getLevel().addFreshEntity(orb);
        h.runAfterDelay(10, () -> {
            h.assertTrue(!orb.isRemoved(), "持续日晒不能每刻重置燃烧并造成额外伤害");
            h.assertTrue(orb.isOnFire(), "日晒应点燃灵魂球：day=" + h.getLevel().isDay()
                    + ", sky=" + h.getLevel().canSeeSky(orb.blockPosition()) + ", brightness=" + orb.getLightLevelDependentMagicValue()
                    + ", pos=" + orb.blockPosition() + ", ticks=" + orb.tickCount);
            // 保留真实实体tick的火焰伤害逻辑，加速经过剩余燃烧时间。
            for (int i = 0; i < 120 && !orb.isRemoved(); i++) orb.tick();
            h.assertTrue(orb.isRemoved(), "日晒燃烧必须造成实际伤害");
            h.succeed();
        });
    }
}
