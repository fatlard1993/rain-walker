package justfatlard.rain_walker.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import justfatlard.rain_walker.RainWalker;

@Mixin(LivingEntity.class)
public class LivingEntityMixin {

	@Unique
	private int rainWalkCooldown = 0;

	@Inject(method = "tickMovement", at = @At("TAIL"))
	private void onTickMovement(CallbackInfo ci) {
		LivingEntity self = (LivingEntity)(Object)this;
		World world = self.getEntityWorld();

		if (world.isClient()) return;

		// Decrease cooldown
		if (rainWalkCooldown > 0) {
			rainWalkCooldown--;
			return;
		}

		// Only trigger when falling (not on ground and moving downward)
		if (self.isOnGround()) return;

		Vec3d velocity = self.getVelocity();
		if (velocity.y >= 0) return; // Not falling

		ItemStack boots = self.getEquippedStack(EquipmentSlot.FEET);
		if (boots.isEmpty()) return;

		// Get the enchantment from registry
		var enchantmentRegistry = world.getRegistryManager().getOrThrow(RegistryKeys.ENCHANTMENT);
		var rainWalkerOpt = enchantmentRegistry.getOptional(RainWalker.RAIN_WALKER);

		if (rainWalkerOpt.isEmpty()) return;

		int level = EnchantmentHelper.getLevel(rainWalkerOpt.get(), boots);

		if (level > 0) {
			if (RainWalker.createIcePlatform(self, world, level)) {
				// Short cooldown for smooth rain running
				rainWalkCooldown = 1;

				// Reset fall distance since we landed on ice
				self.fallDistance = 0;
			}
		}
	}
}
