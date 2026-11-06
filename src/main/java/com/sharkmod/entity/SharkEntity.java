package com.sharkmod.entity;

import com.sharkmod.model.SharkAnimator;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.RandomSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.SmoothSwimmingLookControl;
import net.minecraft.world.entity.ai.control.SmoothSwimmingMoveControl;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.PanicGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.RandomSwimmingGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.ai.navigation.WaterBoundPathNavigation;
import net.minecraft.world.entity.animal.AbstractFish;
import net.minecraft.world.entity.animal.WaterAnimal;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * Tubarão. A mesma classe serve para os dois tipos de entidade:
 *  - ModEntities.SHARK       (dad)  : adulto, ataca peixes e quem o machucar
 *  - ModEntities.BABY_SHARK  (baby) : filhote, pacífico, foge quando se machuca
 */
public class SharkEntity extends WaterAnimal {

    /** Estado da animação (só é atualizado no cliente). */
    public final SharkAnimator animator = new SharkAnimator();

    public SharkEntity(EntityType<? extends SharkEntity> type, Level level) {
        super(type, level);
        this.moveControl = new SmoothSwimmingMoveControl(this, 85, 10, 0.02f, 0.1f, true);
        this.lookControl = new SmoothSwimmingLookControl(this, 10);
    }

    public boolean isBabyVariant() {
        return this.getType() == ModEntities.BABY_SHARK.get();
    }

    @Override
    protected void registerGoals() {
        if (this.isBabyVariant()) {
            this.goalSelector.addGoal(1, new PanicGoal(this, 1.6));
        } else {
            this.goalSelector.addGoal(1, new MeleeAttackGoal(this, 1.3, true));
            this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
            this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, AbstractFish.class, true));
        }
        this.goalSelector.addGoal(4, new RandomSwimmingGoal(this, 1.0, 10));
        this.goalSelector.addGoal(5, new RandomLookAroundGoal(this));
    }

    public static AttributeSupplier.Builder createDadAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 40.0)
                .add(Attributes.MOVEMENT_SPEED, 1.2)
                .add(Attributes.ATTACK_DAMAGE, 6.0);
    }

    public static AttributeSupplier.Builder createBabyAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 8.0)
                .add(Attributes.MOVEMENT_SPEED, 1.1);
    }

    public static boolean canSpawnShark(EntityType<? extends SharkEntity> type, LevelAccessor level,
                                        MobSpawnType reason, BlockPos pos, RandomSource random) {
        return WaterAnimal.checkSurfaceWaterAnimalSpawnRules(type, level, reason, pos, random);
    }

    @Override
    protected PathNavigation createNavigation(Level level) {
        return new WaterBoundPathNavigation(this, level);
    }

    @Override
    public void travel(Vec3 travelVector) {
        if (this.isEffectiveAi() && this.isInWater()) {
            this.moveRelative(this.getSpeed(), travelVector);
            this.move(MoverType.SELF, this.getDeltaMovement());
            this.setDeltaMovement(this.getDeltaMovement().scale(0.9));
            if (this.getTarget() == null) {
                this.setDeltaMovement(this.getDeltaMovement().add(0.0, -0.005, 0.0));
            }
        } else {
            super.travel(travelVector);
        }
    }

    @Override
    public void tick() {
        super.tick();
        if (this.level().isClientSide()) {
            double dx = this.getX() - this.xo;
            double dy = this.getY() - this.yo;
            double dz = this.getZ() - this.zo;
            float speed = (float) Math.sqrt(dx * dx + dy * dy + dz * dz);
            this.animator.tick(speed > 0.03f, speed);
        }
    }

    @Override
    public boolean doHurtTarget(Entity target) {
        boolean hit = target.hurt(this.damageSources().mobAttack(this),
                (float) this.getAttributeValue(Attributes.ATTACK_DAMAGE));
        if (hit) {
            this.doEnchantDamageEffects(this, target);
        }
        // status 4 -> o cliente toca a animação "bite"
        this.level().broadcastEntityEvent(this, (byte) 4);
        return hit;
    }

    @Override
    public void handleEntityEvent(byte status) {
        if (status == 4) {
            this.animator.triggerBite();
        } else {
            super.handleEntityEvent(status);
        }
    }

    /** O modelo é bem maior que a hitbox; sem isso o tubarão some quando a hitbox sai da tela. */
    @Override
    public AABB getBoundingBoxForCulling() {
        return this.getBoundingBox().inflate(this.isBabyVariant() ? 1.0 : 2.5);
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.DOLPHIN_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.DOLPHIN_DEATH;
    }
}
