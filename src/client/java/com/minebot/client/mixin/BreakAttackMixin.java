package com.minebot.client.mixin;

import com.minebot.client.MineBotClient;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Vanilla calls this later in the tick. With the attack key up it aborts the break
 * ({@code stopDestroyBlock}) and the crack resets. Mining already called continue itself.
 */
@Mixin(Minecraft.class)
public class BreakAttackMixin {
	@Inject(method = "continueAttack", at = @At("HEAD"), cancellable = true)
	private void minebot$keepBreak(boolean held, CallbackInfo info) {
		if (MineBotClient.breaking) {
			info.cancel();
		}
	}
}
