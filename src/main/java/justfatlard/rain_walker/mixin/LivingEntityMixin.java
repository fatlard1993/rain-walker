package justfatlard.rain_walker.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import justfatlard.rain_walker.RainWalker;

@Mixin(LivingEntity.class)
public class LivingEntityMixin {

	@Unique
	private int rainWalkCooldown = 0;

	@Inject(method = "aiStep", at = @At("TAIL"))
	private void onTickMovement(CallbackInfo ci) {
		LivingEntity self = (LivingEntity)(Object)this;
		Level world = self.level();

		if (world.isClientSide()) return;

		// Decrease cooldown
		if (rainWalkCooldown > 0) {
			rainWalkCooldown--;
			return;
		}

		// Only trigger when falling (not on ground and moving downward)
		if (self.onGround()) return;

		Vec3 velocity = self.getDeltaMovement();
		if (velocity.y >= 0) return; // Not falling

		ItemStack boots = self.getItemBySlot(EquipmentSlot.FEET);
		if (boots.isEmpty()) return;

		// Get the enchantment from registry
		var enchantmentRegistry = world.registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
		var rainWalkerOpt = enchantmentRegistry.get(RainWalker.RAIN_WALKER);

		if (rainWalkerOpt.isEmpty()) return;

		int level = EnchantmentHelper.getItemEnchantmentLevel(rainWalkerOpt.get(), boots);

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
