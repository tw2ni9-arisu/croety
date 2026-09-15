package com.croety.mixin;

import com.Polarice3.Goety.api.items.magic.ITotem;
import com.croety.content.fluid.SoulTransfers;
import com.simibubi.create.content.fluids.transfer.GenericItemEmptying;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = GenericItemEmptying.class, remap = false)
public class GenericItemEmptyingMixin {
    @Inject(method = "canItemBeEmptied", at = @At("HEAD"), cancellable = true)
    private static void croety$totem(Level level, ItemStack stack, CallbackInfoReturnable<Boolean> callback) {
        if (SoulTransfers.isTotem(stack) && ITotem.currentSouls(stack) > 0) callback.setReturnValue(true);
    }
}
