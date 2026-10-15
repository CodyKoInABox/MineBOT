package com.minebot.client.mixin;

import com.minebot.client.MineBotClient;
import net.minecraft.client.player.ClientInput;
import net.minecraft.client.player.KeyboardInput;
import net.minecraft.world.entity.player.Input;
import net.minecraft.world.phys.Vec2;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Vanilla rebuilds key state in {@code tick}. Put our walk back after that, still one step. */
@Mixin(KeyboardInput.class)
public class KeyboardInputMixin extends ClientInput {
	@Inject(method = "tick", at = @At("RETURN"))
	private void minebot$drive(CallbackInfo info) {
		if (!MineBotClient.drive) {
			return;
		}
		this.keyPresses = new Input(true, false, false, false, MineBotClient.jump, false, false);
		this.moveVector = new Vec2(0.0F, 1.0F);
	}
}
