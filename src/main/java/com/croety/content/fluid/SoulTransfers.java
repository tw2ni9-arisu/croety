package com.croety.content.fluid;

import com.Polarice3.Goety.api.items.magic.ITotem;
import com.Polarice3.Goety.common.items.ModItems;
import net.createmod.catnip.data.Pair;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fluids.capability.IFluidHandler.FluidAction;

/** 分液和注液都在实际执行时重新计算接受量，不沿用之前模拟的容量。 */
public final class SoulTransfers {
    private SoulTransfers() {}
    public static boolean isTotem(ItemStack stack) {
        return stack.is(ModItems.TOTEM_OF_ROOTS.get()) || stack.is(ModItems.TOTEM_OF_SOULS.get());
    }
    public static int fillTotem(ItemStack stack, int requested, boolean simulate) {
        if (!isTotem(stack) || requested <= 0) return 0;
        ITotem type = (ITotem) stack.getItem();
        int maximum = stack.hasTag() && stack.getTag().contains(ITotem.MAX_SOUL_AMOUNT) ? ITotem.maximumSouls(stack) : type.getMaxSouls();
        int amount = Math.min(requested, Math.max(0, maximum - ITotem.currentSouls(stack)));
        if (!simulate && amount > 0) {
            ITotem.setMaxSoulAmount(stack, maximum);
            ITotem.setSoulsAmount(stack, ITotem.currentSouls(stack) + amount);
        }
        return amount;
    }
    public static Pair<FluidStack, ItemStack> emptyTotem(ItemStack stack, IFluidHandler tank, boolean simulate) {
        if (!isTotem(stack) || stack.getCount() != 1) return Pair.of(FluidStack.EMPTY, stack);
        int souls = Math.max(0, ITotem.currentSouls(stack));
        FluidStack offered = new FluidStack(SoulFluidContent.SOUL.get(), souls);
        int amount = tank.fill(offered, FluidAction.SIMULATE);
        if (amount <= 0) return Pair.of(FluidStack.EMPTY, stack);
        offered.setAmount(amount);
        if (!simulate) amount = tank.fill(offered, FluidAction.EXECUTE);
        if (amount <= 0) return Pair.of(FluidStack.EMPTY, stack);
        ItemStack out = stack.copy();
        ITotem.setSoulsAmount(out, souls - amount);
        if (souls == amount) {
            if (stack.is(ModItems.TOTEM_OF_ROOTS.get())) out = ItemStack.EMPTY;
            else {
                out = new ItemStack(ModItems.SPENT_TOTEM.get());
                if (stack.hasTag()) {
                    out.setTag(stack.getTag().copy());
                    out.getTag().remove("Souls");
                    out.getTag().remove(ITotem.MAX_SOUL_AMOUNT);
                }
            }
        }
        return Pair.of(new FluidStack(SoulFluidContent.SOUL.get(), amount), out);
    }
}
