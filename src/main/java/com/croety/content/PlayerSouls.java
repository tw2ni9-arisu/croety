package com.croety.content;

import com.Polarice3.Goety.api.items.magic.ITotem;
import com.Polarice3.Goety.config.MainConfig;
import com.Polarice3.Goety.utils.SEHelper;
import com.Polarice3.Goety.utils.TotemFinder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/** 物质转换按等价数量记账，不套用法术消耗折扣。 */
public final class PlayerSouls {
    private PlayerSouls() {}

    public static int change(Player player, int delta, boolean simulate) {
        if (SEHelper.getSEActive(player)) {
            int before = SEHelper.getSESouls(player);
            int after = before + accepted(before, MainConfig.MaxArcaSouls.get(), delta);
            if (!simulate && after != before) {
                SEHelper.setSESouls(player, after);
                SEHelper.sendSEUpdatePacket(player);
            }
            return after - before;
        }
        ItemStack totem = TotemFinder.FindTotem(player);
        if (!(totem.getItem() instanceof ITotem type)) return 0;
        int before = ITotem.currentSouls(totem);
        int maximum = totem.hasTag() && totem.getTag().contains(ITotem.MAX_SOUL_AMOUNT)
                ? ITotem.maximumSouls(totem) : type.getMaxSouls();
        int after = before + accepted(before, maximum, delta);
        if (!simulate && after != before) {
            ITotem.setMaxSoulAmount(totem, maximum);
            ITotem.setSoulsAmount(totem, after);
            player.getInventory().setChanged();
        }
        return after - before;
    }

    private static int accepted(int current, int maximum, int requested) {
        // 降低配置上限后，充值不能顺带裁剪旧余额；扣除也只按实际请求量执行。
        if (requested > 0) return (int) Math.min(requested, Math.max(0L, (long) maximum - current));
        if (requested < 0) return (int) -Math.min(-(long) requested, Math.max(0, current));
        return 0;
    }
}
