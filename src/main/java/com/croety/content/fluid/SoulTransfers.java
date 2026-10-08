package com.croety.content.fluid;

import com.Polarice3.Goety.api.items.magic.ITotem;
import com.Polarice3.Goety.common.items.ModItems;
import net.createmod.catnip.data.Pair;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction;

/** 分液和注液都在实际执行时重新计算接受量，不沿用之前模拟的容量。 */
public final class SoulTransfers {
    private SoulTransfers() {}
    public static boolean isTotem(ItemStack stack) {
        return stack.is(ModItems.TOTEM_OF_ROOTS.get()) || stack.is(ModItems.TOTEM_OF_SOULS.get());
    }
    public static int fillTotem(ItemStack stack, int requested, boolean simulate) {
        if (!isTotem(stack) || requested <= 0) return 0;
        ITotem type = (ITotem) stack.getItem();
        int maximum = ITotem.tag(stack).contains(ITotem.MAX_SOUL_AMOUNT) ? ITotem.maximumSouls(stack) : type.getMaxSouls();
        int amount = Math.min(requested, Math.max(0, maximum - ITotem.currentSouls(stack)));
        if (!simulate && amount > 0) {
            ITotem.setMaxSoulAmount(stack, maximum);
            ITotem.setSoulsamount(stack, ITotem.currentSouls(stack) + amount);
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
        ITotem.setSoulsamount(out, souls - amount);
        if (souls == amount) {
            if (stack.is(ModItems.TOTEM_OF_ROOTS.get())) out = ItemStack.EMPTY;
            else {
                out = new ItemStack(ModItems.SPENT_TOTEM.get());
                out.applyComponents(stack.getComponentsPatch());
                CustomData.update(DataComponents.CUSTOM_DATA, out, tag -> {
                    tag.remove("Souls");
                    tag.remove(ITotem.MAX_SOUL_AMOUNT);
                });
            }
        }
        return Pair.of(new FluidStack(SoulFluidContent.SOUL.get(), amount), out);
    }
}
