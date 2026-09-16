/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.core.BlockPos
 *  net.minecraft.core.particles.BlockParticleOption
 *  net.minecraft.core.particles.ParticleOptions
 *  net.minecraft.core.particles.ParticleTypes
 *  net.minecraft.nbt.CompoundTag
 *  net.minecraft.network.syncher.SynchedEntityData$Builder
 *  net.minecraft.sounds.SoundEvents
 *  net.minecraft.sounds.SoundSource
 *  net.minecraft.world.entity.Entity
 *  net.minecraft.world.entity.EntityType
 *  net.minecraft.world.level.Level
 *  net.minecraft.world.level.block.state.BlockState
 */
package dev.dedek.greatsage.content;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

public class VenuzdonoaEntity
extends Entity {
    public static final int RISE_TICKS = 44;
    public static final int LIFETIME_TICKS = 240;

    public VenuzdonoaEntity(EntityType<? extends VenuzdonoaEntity> type, Level level) {
        super(type, level);
        this.noPhysics = true;
        this.setNoGravity(true);
    }

    public void tick() {
        super.tick();
        if (this.level().isClientSide()) {
            if (this.tickCount <= 44) {
                this.spawnDebris();
            }
            return;
        }
        if (this.tickCount == 1) {
            this.level().playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.DEEPSLATE_BREAK, SoundSource.PLAYERS, 1.2f, 0.55f);
        }
        if (this.tickCount >= 240) {
            this.discard();
        }
    }

    private void spawnDebris() {
        BlockState state = this.level().getBlockState(BlockPos.containing((double)this.getX(), (double)(this.getY() - 0.1), (double)this.getZ()));
        if (state.isAir()) {
            return;
        }
        for (int i = 0; i < 3; ++i) {
            double angle = this.random.nextDouble() * Math.PI * 2.0;
            double radius = 0.15 + this.random.nextDouble() * 0.35;
            this.level().addParticle((ParticleOptions)new BlockParticleOption(ParticleTypes.BLOCK, state), this.getX() + Math.cos(angle) * radius, this.getY() + 0.05, this.getZ() + Math.sin(angle) * radius, Math.cos(angle) * 0.08, 0.12 + this.random.nextDouble() * 0.1, Math.sin(angle) * 0.08);
        }
    }

    public float riseProgress(float partialTick) {
        return Math.min(1.0f, ((float)this.tickCount + partialTick) / 44.0f);
    }

    public boolean isPickable() {
        return false;
    }

    public boolean canBeCollidedWith() {
        return false;
    }

    public boolean isAttackable() {
        return false;
    }

    protected void defineSynchedData(SynchedEntityData.Builder builder) {
    }

    protected void readAdditionalSaveData(CompoundTag tag) {
    }

    protected void addAdditionalSaveData(CompoundTag tag) {
    }
}

