package com.croety.content.fluid;

import com.Polarice3.Goety.common.blocks.entities.ArcaBlockEntity;
import com.Polarice3.Goety.common.blocks.entities.CursedCageBlockEntity;
import com.Polarice3.Goety.utils.SEHelper;
import com.croety.content.PlayerSouls;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = "croety")
public final class SoulReceiver implements IFluidHandler, ICapabilityProvider {
    private final BlockEntity target;
    private final LazyOptional<IFluidHandler> capability = LazyOptional.of(() -> this);
    private SoulReceiver(BlockEntity target) { this.target = target; }

    @SubscribeEvent public static void attach(AttachCapabilitiesEvent<BlockEntity> event) {
        if (event.getObject() instanceof CursedCageBlockEntity || event.getObject() instanceof ArcaBlockEntity) {
            SoulReceiver receiver = new SoulReceiver(event.getObject());
            event.addCapability(new ResourceLocation("croety", "soul_receiver"), receiver);
            event.addListener(receiver.capability::invalidate);
        }
    }
    @Override public <T> LazyOptional<T> getCapability(Capability<T> cap, Direction side) {
        return cap == ForgeCapabilities.FLUID_HANDLER ? capability.cast() : LazyOptional.empty();
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
