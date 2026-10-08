package com.croety.content.orb;

import com.croety.content.PlayerSouls;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/** 运动沿用经验球算法；相邻同符号灵魂合并为一颗按总价值显示、拾取的球。 */
public class SoulOrb extends Entity {
    private static final EntityDataAccessor<Long> VALUE = SynchedEntityData.defineId(SoulOrb.class, EntityDataSerializers.LONG);
    private int age;
    private int health = 5;
    private Player followingPlayer;

    public SoulOrb(EntityType<? extends SoulOrb> type, Level level) { super(type, level); }

    public SoulOrb(Level level, Vec3 position, long value) {
        this(OrbContent.SOUL_ORB.get(), level);
        setPos(position);
        entityData.set(VALUE, value);
        setYRot(random.nextFloat() * 360);
        setDeltaMovement((random.nextDouble() * .2 - .1) * 2, random.nextDouble() * .4, (random.nextDouble() * .2 - .1) * 2);
    }

    @Override protected void defineSynchedData(SynchedEntityData.Builder builder) { builder.define(VALUE, 1L); }
    public long getValue() { return entityData.get(VALUE); }
    public int getAge() { return age; }
    public int getIcon() {
        int[] thresholds = {3, 7, 17, 37, 73, 149, 307, 617, 1237, 2477};
        int icon = 0;
        while (icon < thresholds.length && getValue() >= thresholds[icon]) icon++;
        return icon;
    }

    @Override public void tick() {
        super.tick();
        xo = getX(); yo = getY(); zo = getZ();
        if (isEyeInFluid(FluidTags.WATER)) {
            Vec3 motion = getDeltaMovement();
            setDeltaMovement(motion.x * .99, Math.min(motion.y + .0005, .06), motion.z * .99);
        } else if (!isNoGravity()) setDeltaMovement(getDeltaMovement().add(0, -.03, 0));
        if (level().getFluidState(blockPosition()).is(FluidTags.LAVA))
            setDeltaMovement((random.nextFloat() - random.nextFloat()) * .2, .2, (random.nextFloat() - random.nextFloat()) * .2);
        if (!level().noCollision(getBoundingBox())) moveTowardsClosestSpace(getX(), (getBoundingBox().minY + getBoundingBox().maxY) / 2, getZ());
        if (tickCount % 20 == 1) {
            if (followingPlayer == null || followingPlayer.distanceToSqr(this) > 64)
                followingPlayer = level().getNearestPlayer(this, 8);
            if (!level().isClientSide) {
                for (SoulOrb other : level().getEntitiesOfClass(SoulOrb.class, getBoundingBox().inflate(.5),
                        other -> other != this && !other.isRemoved())) {
                    if (canMerge(other.getValue())) {
                        entityData.set(VALUE, getValue() + other.getValue());
                        age = Math.min(age, other.age);
                        other.discard();
                    }
                }
            }
        }
        if (followingPlayer != null && (followingPlayer.isSpectator() || followingPlayer.isDeadOrDying())) followingPlayer = null;
        if (followingPlayer != null) {
            Vec3 toward = new Vec3(followingPlayer.getX() - getX(), followingPlayer.getY() + followingPlayer.getEyeHeight() / 2 - getY(), followingPlayer.getZ() - getZ());
            double distance = toward.lengthSqr();
            if (distance < 64) {
                double pull = 1 - Math.sqrt(distance) / 8;
                setDeltaMovement(getDeltaMovement().add(toward.normalize().scale(pull * pull * .1)));
            }
        }
        move(MoverType.SELF, getDeltaMovement());
        float friction = .98F;
        if (onGround()) {
            BlockPos below = getOnPos(.999999F);
            friction *= level().getBlockState(below).getFriction(level(), below, this);
        }
        setDeltaMovement(getDeltaMovement().multiply(friction, .98, friction));
        if (onGround()) setDeltaMovement(getDeltaMovement().multiply(1, -.9, 1));
        if (!level().isClientSide) {
            if (!isOnFire() && level().isDay() && !isInWaterRainOrBubble() && level().canSeeSky(blockPosition())
                    && getLightLevelDependentMagicValue() > .5F) igniteForSeconds(8.0F);
            if (++age >= 6000) discard();
        }
    }

    private boolean canMerge(long amount) {
        long value = getValue();
        return value > 0 && amount > 0 && value <= Long.MAX_VALUE - amount
                || value < 0 && amount < 0 && value >= Long.MIN_VALUE - amount;
    }

    public static void award(ServerLevel level, Vec3 position, int amount) {
        if (amount == 0) return;
        var nearby = level.getEntitiesOfClass(SoulOrb.class, AABB.ofSize(position, 2, 2, 2),
                orb -> !orb.isRemoved() && orb.canMerge(amount));
        if (nearby.isEmpty()) level.addFreshEntity(new SoulOrb(level, position, amount));
        else {
            SoulOrb orb = nearby.get(0);
            orb.entityData.set(VALUE, orb.getValue() + amount);
            orb.age = 0;
        }
    }

    @Override public void playerTouch(Player player) {
        if (level().isClientSide || isRemoved() || player.isSpectator() || player.takeXpDelay != 0) return;
        long value = getValue();
        int requested = (int) Math.max(Integer.MIN_VALUE, Math.min(Integer.MAX_VALUE, value));
        int applied = PlayerSouls.change(player, requested, false);
        if (value != 0 && applied == 0) return;
        player.takeXpDelay = 2;
        player.take(this, 1);
        level().playSound(null, blockPosition(), SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.PLAYERS, .1F, .8F + random.nextFloat() * .4F);
        // 部分拾取直接缩小原球，避免重生成导致碎片、重复合并或重置寿命。
        if (applied != value) entityData.set(VALUE, value - applied);
        else discard();
    }

    @Override public boolean hurt(DamageSource source, float amount) {
        if (level().isClientSide || isRemoved() || isInvulnerableTo(source) || source.is(DamageTypeTags.IS_PROJECTILE)) return false;
        if (source.getDirectEntity() instanceof Player && !source.is(DamageTypeTags.IS_EXPLOSION)) return false;
        markHurt();
        health = (int) (health - amount);
        if (health <= 0) discard();
        return true;
    }
    @Override public void addAdditionalSaveData(CompoundTag tag) {
        tag.putInt("Health", health); tag.putInt("Age", age); tag.putLong("Value", getValue());
    }
    @Override public void readAdditionalSaveData(CompoundTag tag) {
        health = tag.contains("Health") ? tag.getInt("Health") : 5;
        age = tag.getInt("Age");
        long value = tag.contains("Value") ? tag.getLong("Value") : 1;
        // 初版使用int Value和Count；只迁移旧格式，新格式直接保存long总价值。
        if (!tag.contains("Value", net.minecraft.nbt.Tag.TAG_LONG))
            value *= Math.max(tag.getInt("Count"), 1);
        entityData.set(VALUE, value);
    }
    @Override public boolean isAttackable() { return false; }
    @Override protected MovementEmission getMovementEmission() { return MovementEmission.NONE; }
    @Override protected void doWaterSplashEffect() {}
}
