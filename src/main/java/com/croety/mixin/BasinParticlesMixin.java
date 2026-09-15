package com.croety.mixin;

import com.croety.content.fluid.SoulFluidContent;
import com.simibubi.create.content.processing.basin.BasinBlockEntity;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.Mth;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** 只在客户端为可见液面添加粒子，不进行服务端扫描或发送常驻粒子包。 */
@Mixin(value = BasinBlockEntity.class, remap = false)
public class BasinParticlesMixin {
    @Inject(method = "createFluidParticles", at = @At("HEAD"))
    private void croety$soulParticles(CallbackInfo callback) {
        BasinBlockEntity basin = (BasinBlockEntity) (Object) this;
        var level = basin.getLevel();
        if (level == null || !level.isClientSide || level.random.nextInt(10) != 0) return;
        for (var behaviour : basin.getTanks()) {
            if (behaviour == null) continue;
            for (var segment : behaviour.getTanks()) {
                var fluid = segment.getRenderedFluid();
                if (segment.isEmpty(1) || fluid.isEmpty() || !fluid.getFluid().isSame(SoulFluidContent.SOUL.get())) continue;
                float fraction = Mth.clamp(basin.getTotalFluidUnits(1) / 2000, 0, 1);
                float surface = .125F + .75F * (1 - (1 - fraction) * (1 - fraction));
                var pos = basin.getBlockPos();
                level.addParticle(ParticleTypes.SOUL, pos.getX() + .2 + level.random.nextFloat() * .6,
                        pos.getY() + surface + .03, pos.getZ() + .2 + level.random.nextFloat() * .6, 0, .025, 0);
                return;
            }
        }
    }
}
