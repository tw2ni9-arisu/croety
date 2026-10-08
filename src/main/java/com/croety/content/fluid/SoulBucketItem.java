package com.croety.content.fluid;

import com.croety.content.PlayerSouls;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;

public class SoulBucketItem extends BucketItem {
    public SoulBucketItem(Properties properties) { super(SoulFluidContent.SOUL.get(), properties); }
    @Override public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!player.isShiftKeyDown()) return InteractionResultHolder.pass(stack);
        if (PlayerSouls.change(player, 1000, true) != 1000) return InteractionResultHolder.fail(stack);
        if (!level.isClientSide) {
            PlayerSouls.change(player, 1000, false);
            player.setItemInHand(hand, new ItemStack(Items.BUCKET));
        }
        return InteractionResultHolder.sidedSuccess(player.getItemInHand(hand), level.isClientSide);
    }
    @Override public boolean emptyContents(Player player, Level level, BlockPos pos, BlockHitResult hit) { return false; }
    @Override public boolean emptyContents(Player player, Level level, BlockPos pos, BlockHitResult hit, ItemStack stack) { return false; }
}
