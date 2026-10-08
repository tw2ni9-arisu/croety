package com.croety.content.motor;

import com.simibubi.create.content.kinetics.base.GeneratingKineticBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import java.util.UUID;
import java.util.List;
import java.util.Locale;

public class SoulMotorBlockEntity extends GeneratingKineticBlockEntity {
    private UUID owner;
    private long recordId;
    private long expiresAt;

    public SoulMotorBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    public SoulMotorBlockEntity(BlockPos pos, BlockState state) {
        this(MotorContent.MOTOR_BE.get(), pos, state);
    }

    @Override
    public void initialize() {
        if (invalidSummoning()) {
            dissolve();
            return;
        }
        super.initialize();
        if (!hasSource() || getGeneratedSpeed() > getTheoreticalSpeed())
            updateGeneratedRotation();
    }

    @Override
    public float getGeneratedSpeed() {
        if (!getBlockState().is(MotorContent.SOUL_MOTOR.get()) || invalidSummoning())
            return 0;
        return convertToDirection(128, getBlockState().getValue(SoulMotorBlock.FACING));
    }

    private boolean invalidSummoning() {
        if (owner == null || !(level instanceof ServerLevel serverLevel)) return false;
        return serverLevel.getServer().overworld().getGameTime() >= expiresAt
                || !SoulMotorData.get(serverLevel.getServer()).contains(recordId);
    }

    public void setSummoned(UUID owner, long recordId, long expiresAt) {
        this.owner = owner;
        this.recordId = recordId;
        this.expiresAt = expiresAt;
        notifyUpdate();
        if (level instanceof ServerLevel server) {
            server.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, worldPosition.getX() + .5, worldPosition.getY() + .5,
                    worldPosition.getZ() + .5, 24, .35, .35, .35, .02);
            server.sendParticles(ParticleTypes.SOUL, worldPosition.getX() + .5, worldPosition.getY() + .5,
                    worldPosition.getZ() + .5, 12, .3, .3, .3, .015);
        }
    }

    public void dissolve() {
        if (level instanceof ServerLevel server && level.getBlockEntity(worldPosition) == this) {
            server.sendParticles(ParticleTypes.SOUL, worldPosition.getX() + .5, worldPosition.getY() + .5,
                    worldPosition.getZ() + .5, 24, .4, .4, .4, .035);
            level.setBlock(worldPosition, Blocks.AIR.defaultBlockState(), 3);
        }
    }

    public Component getLifetimeText() {
        if (!isSummoned()) return Component.translatable("croety.goggles.lifetime.permanent");
        long now = level instanceof ServerLevel server ? server.getServer().overworld().getGameTime()
                : level == null ? 0 : level.getGameTime();
        long seconds = (Math.max(0, expiresAt - now) + 19) / 20;
        return Component.translatable("croety.goggles.lifetime.remaining", seconds / 60,
                String.format(Locale.ROOT, "%02d", seconds % 60));
    }

    @Override
    public boolean addToGoggleTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
        super.addToGoggleTooltip(tooltip, isPlayerSneaking);
        tooltip.add(Component.empty());
        tooltip.add(Component.literal("    ").append(getLifetimeText()).withStyle(ChatFormatting.AQUA));
        return true;
    }

    public boolean isSummoned() {
        return owner != null;
    }

    public UUID getOwner() {
        return owner;
    }

    public long getRecordId() {
        return recordId;
    }

    public long getExpiresAt() {
        return expiresAt;
    }

    @Override
    protected void write(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.write(tag, registries, clientPacket);
        if (owner != null) {
            tag.putUUID("MotorOwner", owner);
            tag.putLong("MotorRecord", recordId);
            tag.putLong("MotorExpires", expiresAt);
        }
    }

    @Override
    protected void read(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.read(tag, registries, clientPacket);
        owner = tag.hasUUID("MotorOwner") ? tag.getUUID("MotorOwner") : null;
        recordId = tag.getLong("MotorRecord");
        expiresAt = tag.getLong("MotorExpires");
    }
}
