package com.croety.content.spell;

import com.Polarice3.Goety.api.magic.SpellType;
import com.Polarice3.Goety.common.magic.Spell;
import com.Polarice3.Goety.common.magic.SpellStat;
import com.croety.content.motor.MotorContent;
import com.croety.content.motor.SoulMotorBlock;
import com.croety.content.motor.SoulMotorBlockEntity;
import com.croety.content.motor.SoulMotorData;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.ForgeEventFactory;
import net.minecraftforge.common.util.BlockSnapshot;

public class WavingSpell extends Spell {
    @Override
    public int defaultSoulCost() {
        return 1024;
    }

    @Override
    public int defaultCastDuration() {
        return 0;
    }

    @Override
    public int defaultSpellCooldown() {
        return 20 * 60;
    }

    @Override
    public SpellType getSpellType() {
        return SpellType.NONE;
    }

    @Override
    public List<Enchantment> acceptedEnchantments() {
        return List.of();
    }

    @Override
    public void SpellResult(ServerLevel level, LivingEntity caster, ItemStack staff, SpellStat stats) {
        if (!(caster instanceof Player player)) return;
        BlockHitResult hit = blockResult(level, caster, 32);
        if (hit == null || hit.getType() != net.minecraft.world.phys.HitResult.Type.BLOCK) return;
        BlockPos pos = hit.getBlockPos().relative(hit.getDirection());
        Direction facing = hit.getDirection();
        BlockState existing = level.getBlockState(pos);
        BlockPlaceContext context = new BlockPlaceContext(level, player, player.getUsedItemHand(), ItemStack.EMPTY,
                new BlockHitResult(Vec3.atCenterOf(pos), facing, pos, false));
        if (!level.isInWorldBounds(pos) || !level.hasChunkAt(pos) || !existing.canBeReplaced(context)
                || !existing.getFluidState().isEmpty() || !player.mayInteract(level, pos)
                || !player.mayUseItemAt(pos, facing, ItemStack.EMPTY)) return;
        BlockState motorState = MotorContent.SOUL_MOTOR.get().defaultBlockState().setValue(SoulMotorBlock.FACING, facing);
        if (!level.isUnobstructed(motorState, pos, net.minecraft.world.phys.shapes.CollisionContext.of(player))) return;
        BlockSnapshot snapshot = BlockSnapshot.create(level.dimension(), level, pos);
        if (!level.setBlock(pos, motorState, 3)) return;
        if (ForgeEventFactory.onBlockPlace(player, snapshot, facing)) {
            snapshot.restore();
            return;
        }
        if (level.getBlockEntity(pos) instanceof SoulMotorBlockEntity motor) {
            SoulMotorData data = SoulMotorData.get(level.getServer());
            long id = data.add(player.getUUID(), level, pos);
            motor.setSummoned(player.getUUID(), id, level.getServer().overworld().getGameTime() + SoulMotorData.LIFETIME);
            data.tick(level.getServer());
        }
    }
}
