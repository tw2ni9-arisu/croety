package com.croety.content.fluid;

import com.Polarice3.Goety.common.blocks.entities.ArcaBlockEntity;
import com.Polarice3.Goety.common.blocks.entities.CursedCageBlockEntity;
import com.Polarice3.Goety.common.blocks.entities.ModBlockEntities;
import com.Polarice3.Goety.utils.SEHelper;
import com.croety.content.PlayerSouls;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.wrappers.FluidBucketWrapper;

public final class SoulReceiver implements IFluidHandler {
    private final BlockEntity target;
    private SoulReceiver(BlockEntity target) { this.target = target; }

    public static void registerCapabilities(RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(Capabilities.FluidHandler.BLOCK, ModBlockEntities.CURSED_CAGE.get(),
                (cage, side) -> new SoulReceiver(cage));
        event.registerBlockEntity(Capabilities.FluidHandler.BLOCK, ModBlockEntities.ARCA.get(),
                (arca, side) -> new SoulReceiver(arca));
        event.registerItem(Capabilities.FluidHandler.ITEM, (stack, context) -> new FluidBucketWrapper(stack), SoulFluidContent.BUCKET.get());
    }
    @Override public int getTanks() { return 1; }
    @Override public FluidStack getFluidInTank(int tank) { return FluidStack.EMPTY; }
    @Override public int getTankCapacity(int tank) { return Integer.MAX_VALUE; }
    @Override public boolean isFluidValid(int tank, FluidStack fluid) { return fluid.getFluid().isSame(SoulFluidContent.SOUL.get()); }
    @Override public int fill(FluidStack resource, FluidAction action) {
        if (resource.isEmpty() || !isFluidValid(0, resource) || target.isRemoved()
                || target.getLevel() == null || target.getLevel().isClientSide) return 0;
        int accepted = 0;
        if (target instanceof CursedCageBlockEntity cage) {
            accepted = SoulTransfers.fillTotem(cage.getItem(), resource.getAmount(), action.simulate());
        } else if (target instanceof ArcaBlockEntity arca) {
            var player = arca.getPlayer();
            if (player != null && SEHelper.getSEActive(player) && arca.getBlockPos().equals(SEHelper.getArcaBlock(player))
                    && arca.getLevel().dimension().equals(SEHelper.getArcaDimension(player)))
                accepted = PlayerSouls.change(player, resource.getAmount(), action.simulate());
        }
        if (accepted > 0 && action.execute()) {
            target.setChanged();
            target.getLevel().sendBlockUpdated(target.getBlockPos(), target.getBlockState(), target.getBlockState(), 3);
        }
        return accepted;
    }
    @Override public FluidStack drain(FluidStack resource, FluidAction action) { return FluidStack.EMPTY; }
    @Override public FluidStack drain(int maximum, FluidAction action) { return FluidStack.EMPTY; }
}
