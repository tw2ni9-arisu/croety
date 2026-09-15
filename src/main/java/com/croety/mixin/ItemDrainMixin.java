package com.croety.mixin;

import com.croety.content.fluid.SoulTransfers;
import com.simibubi.create.content.fluids.drain.ItemDrainBlockEntity;
import com.simibubi.create.content.kinetics.belt.transport.TransportedItemStack;
import com.simibubi.create.foundation.blockEntity.behaviour.fluid.SmartFluidTankBehaviour;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = ItemDrainBlockEntity.class, remap = false)
public class ItemDrainMixin {
    @Shadow SmartFluidTankBehaviour internalTank;
    @Shadow TransportedItemStack heldItem;
    @Shadow protected int processingTicks;

    @Inject(method = "continueProcessing", at = @At("HEAD"), cancellable = true)
    private void croety$emptyTotem(CallbackInfoReturnable<Boolean> callback) {
        if (heldItem == null) return;
        ItemDrainBlockEntity drain = (ItemDrainBlockEntity) (Object) this;
        // 原版分液池只在前面的刻模拟容量；提交当刻必须防止玩家刚好把池子灌满。
        if (heldItem.stack.is(com.croety.content.fluid.SoulFluidContent.BUCKET.get())
                && processingTicks == 5 && !drain.getLevel().isClientSide) {
            internalTank.allowInsertion();
            int accepted = internalTank.getPrimaryHandler().fill(new net.minecraftforge.fluids.FluidStack(
                    com.croety.content.fluid.SoulFluidContent.SOUL.get(), 1000), net.minecraftforge.fluids.capability.IFluidHandler.FluidAction.SIMULATE);
            internalTank.forbidInsertion();
            if (accepted != 1000) {
                processingTicks = ItemDrainBlockEntity.FILLING_TIME;
                callback.setReturnValue(true);
            }
            return;
        }
        if (!SoulTransfers.isTotem(heldItem.stack)) return;
        if (drain.getLevel().isClientSide || processingTicks < 5) { callback.setReturnValue(true); return; }
        internalTank.allowInsertion();
        var result = SoulTransfers.emptyTotem(heldItem.stack, internalTank.getPrimaryHandler(), processingTicks != 5);
        internalTank.forbidInsertion();
        if (result.getFirst().isEmpty()) {
            processingTicks = ItemDrainBlockEntity.FILLING_TIME;
        } else if (processingTicks == 5) {
            if (result.getSecond().isEmpty()) heldItem = null;
            else heldItem.stack = result.getSecond();
            drain.notifyUpdate();
        }
        callback.setReturnValue(true);
    }
}
