package com.croety.gametest;

import com.Polarice3.Goety.utils.SEHelper;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import net.minecraftforge.registries.ForgeRegistries;

@GameTestHolder("croety")
@PrefixGameTestTemplate(false)
public class SoulOrbTests {
    // 防止未注册、负值丢失、拾取错误增加经验以及寿命未恢复。
    @GameTest(template = "empty")
    public static void negativeOrbConsumesSouls(GameTestHelper helper) {
        var type = ForgeRegistries.ENTITY_TYPES.getValue(new ResourceLocation("croety", "soul_energy_orb"));
        helper.assertTrue(ForgeRegistries.ENTITY_TYPES.containsKey(new ResourceLocation("croety", "soul_energy_orb")), "灵魂球必须注册");
        Entity orb = type.create(helper.getLevel());
        CompoundTag tag = new CompoundTag();
        tag.putInt("Value", -7);
        tag.putInt("Health", 5);
        tag.putInt("Count", 1);
        orb.load(tag);
        Player player = net.minecraftforge.common.util.FakePlayerFactory.getMinecraft(helper.getLevel());
        SEHelper.setSEActive(player, true);
        SEHelper.setSESouls(player, 30);
        orb.playerTouch(player);
        helper.assertTrue(SEHelper.getSESouls(player) == 23, "负球应精确扣除7灵魂");
        helper.assertTrue(player.totalExperience == 0, "灵魂球不能增加原版经验");
        helper.assertTrue(orb.isRemoved(), "拾取完成应移除实体");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void loadedAgeExpires(GameTestHelper helper) {
        var type = ForgeRegistries.ENTITY_TYPES.getValue(new ResourceLocation("croety", "soul_energy_orb"));
        helper.assertTrue(ForgeRegistries.ENTITY_TYPES.containsKey(new ResourceLocation("croety", "soul_energy_orb")), "灵魂球必须注册");
        Entity orb = type.create(helper.getLevel());
        CompoundTag tag = new CompoundTag();
        tag.putInt("Age", 5999);
        tag.putInt("Value", 1);
        tag.putInt("Health", 5);
        orb.load(tag);
        orb.setPos(helper.absoluteVec(net.minecraft.world.phys.Vec3.atCenterOf(new net.minecraft.core.BlockPos(1, 2, 1))));
        orb.tick();
        helper.assertTrue(orb.isRemoved(), "加载的寿命应在6000刻到期");
        helper.succeed();
    }
}

