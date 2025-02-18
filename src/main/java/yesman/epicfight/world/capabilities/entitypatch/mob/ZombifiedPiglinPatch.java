package yesman.epicfight.world.capabilities.entitypatch.mob;

import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.monster.ZombifiedPiglin;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import yesman.epicfight.api.animation.LivingMotion;
import yesman.epicfight.api.client.animation.ClientAnimator;
import yesman.epicfight.api.model.Model;
import yesman.epicfight.api.utils.game.AttackResult;
import yesman.epicfight.gameasset.Animations;
import yesman.epicfight.gameasset.Models;
import yesman.epicfight.world.capabilities.entitypatch.Faction;
import yesman.epicfight.world.capabilities.entitypatch.HumanoidMobPatch;
import yesman.epicfight.world.entity.ai.goal.AnimatedAttackGoal;
import yesman.epicfight.world.entity.ai.goal.CombatBehaviors;
import yesman.epicfight.world.entity.ai.goal.TargetChasingGoal;

public class ZombifiedPiglinPatch extends HumanoidMobPatch<ZombifiedPiglin> {
	public ZombifiedPiglinPatch() {
		super(Faction.NEUTRAL);
	}
	
	@OnlyIn(Dist.CLIENT)
	@Override
	public void initAnimator(ClientAnimator clientAnimator) {
		clientAnimator.addLivingAnimation(LivingMotion.IDLE, Animations.PIGLIN_ZOMBIFIED_IDLE);
		clientAnimator.addLivingAnimation(LivingMotion.WALK, Animations.PIGLIN_ZOMBIFIED_WALK);
		clientAnimator.addLivingAnimation(LivingMotion.CHASE, Animations.PIGLIN_ZOMBIFIED_CHASE);
		clientAnimator.addLivingAnimation(LivingMotion.FALL, Animations.BIPED_FALL);
		clientAnimator.addLivingAnimation(LivingMotion.MOUNT, Animations.BIPED_MOUNT);
		clientAnimator.addLivingAnimation(LivingMotion.DEATH, Animations.PIGLIN_DEATH);
		clientAnimator.setCurrentMotionsAsDefault();
	}
	
	@Override
	public void updateMotion(boolean considerInaction) {
		super.commonAggressiveMobUpdateMotion(considerInaction);
	}
	
	@Override
	public void setAIAsInfantry(boolean holdingRanedWeapon) {
		CombatBehaviors.Builder<HumanoidMobPatch<?>> builder = this.getHoldingItemWeaponMotionBuilder();
		
		if (builder != null) {
			this.original.goalSelector.addGoal(1, new AnimatedAttackGoal<>(this, builder.build(this)));
			this.original.goalSelector.addGoal(1, new TargetChasingGoal(this, this.getOriginal(), 1.2D, true));
		}
	}
	
	@Override
	public AttackResult tryHurt(DamageSource damageSource, float amount) {
		if (damageSource.getEntity() instanceof ZombifiedPiglin) {
			return new AttackResult(AttackResult.ResultType.FAILED, amount);
		}
		
		return super.tryHurt(damageSource, amount);
	}
	
	@Override
	public <M extends Model> M getEntityModel(Models<M> modelDB) {
		return modelDB.piglin;
	}
}