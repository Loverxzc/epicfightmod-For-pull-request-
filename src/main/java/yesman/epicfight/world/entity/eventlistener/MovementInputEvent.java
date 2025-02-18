package yesman.epicfight.world.entity.eventlistener;

import net.minecraft.client.player.Input;
import yesman.epicfight.client.world.capabilites.entitypatch.player.LocalPlayerPatch;

public class MovementInputEvent extends PlayerEvent<LocalPlayerPatch> {
	private Input movementInput;
	
	public MovementInputEvent(LocalPlayerPatch playerpatch, Input movementInput) {
		super(playerpatch, false);
	}
	
	public Input getMovementInput() {
		return this.movementInput;
	}
}