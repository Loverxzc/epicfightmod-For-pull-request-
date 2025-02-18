package yesman.epicfight.world.capabilities.item;

import java.util.ArrayList;
import java.util.List;

import com.mojang.datafixers.util.Pair;

import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.item.Item;
import yesman.epicfight.api.animation.types.StaticAnimation;
import yesman.epicfight.api.collider.Collider;
import yesman.epicfight.gameasset.Animations;
import yesman.epicfight.gameasset.ColliderPreset;
import yesman.epicfight.gameasset.EpicFightSounds;
import yesman.epicfight.particle.EpicFightParticles;
import yesman.epicfight.particle.HitParticleType;
import yesman.epicfight.world.capabilities.entitypatch.LivingEntityPatch;
import yesman.epicfight.world.capabilities.entitypatch.player.PlayerPatch;
import yesman.epicfight.world.entity.ai.attribute.EpicFightAttributes;

public class TridentCapability extends RangedWeaponCapability {
	private static List<StaticAnimation> attackMotion;
	private static List<StaticAnimation> mountAttackMotion;
	
	public TridentCapability(Item item) {
		super(item, null, Animations.BIPED_JAVELIN_AIM, Animations.BIPED_JAVELIN_THROW, WeaponCategory.TRIDENT);
		
		if (attackMotion == null) {
			attackMotion = new ArrayList<StaticAnimation> ();
			attackMotion.add(Animations.SPEAR_ONEHAND_AUTO);
			attackMotion.add(Animations.SPEAR_DASH);
			attackMotion.add(Animations.SPEAR_ONEHAND_AIR_SLASH);
		}
		
		if (mountAttackMotion == null) {
			mountAttackMotion = new ArrayList<StaticAnimation> ();
			mountAttackMotion.add(Animations.SPEAR_MOUNT_ATTACK);
		}
	}
	
	@Override
	public Style getStyle(LivingEntityPatch<?> entitypatch) {
		return Style.ONE_HAND;
	}
	
	@Override
	protected void registerAttribute() {
		this.addStyleAttibute(Style.ONE_HAND, Pair.of(EpicFightAttributes.IMPACT.get(), EpicFightAttributes.getImpactModifier(2.3D)));
	}
	
	@Override
	public SoundEvent getHitSound() {
		return EpicFightSounds.BLADE_HIT;
	}
	
	@Override
	public HitParticleType getHitParticle() {
		return EpicFightParticles.HIT_BLADE.get();
	}
	
	@Override
	public Collider getWeaponCollider() {
		return ColliderPreset.SPEAR;
	}
	
	@Override
	public List<StaticAnimation> getAutoAttckMotion(PlayerPatch<?> playerpatch) {
		return attackMotion;
	}
	
	@Override
	public List<StaticAnimation> getMountAttackMotion() {
		return mountAttackMotion;
	}
}