package justfatlard.rain_walker;

import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import justfatlard.pandorical.api.PandoricalApi;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;

public class RainWalker implements ModInitializer {
	public static final String MOD_ID = "rain-walker";

	public static final ResourceKey<Enchantment> RAIN_WALKER =
		ResourceKey.create(Registries.ENCHANTMENT, Identifier.fromNamespaceAndPath(MOD_ID, "rain_walker"));

	// Track ice platforms for removal (position -> removal tick)
	private static final Map<BlockPos, Long> icePlatforms = new ConcurrentHashMap<>();

	@Override
	public void onInitialize() {
		if (PandoricalApi.isAvailable()) {
			PandoricalApi.content().registerModAssets(MOD_ID);
		}

		System.out.println("[rain-walker] Rain Walker enchantment loaded");

		// Sweep expired ice platforms each server tick
		ServerTickEvents.END_SERVER_TICK.register(server -> {
			long currentTick = server.overworld().getGameTime();
			Iterator<Map.Entry<BlockPos, Long>> iterator = icePlatforms.entrySet().iterator();

			while (iterator.hasNext()) {
				Map.Entry<BlockPos, Long> entry = iterator.next();
				if (currentTick >= entry.getValue()) {
					BlockPos pos = entry.getKey();
					// The map doesn't record dimension, so search every level for the ice
					for (ServerLevel world : server.getAllLevels()) {
						if (world.getBlockState(pos).getBlock() == Blocks.ICE) {
							world.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
							break;
						}
					}
					iterator.remove();
				}
			}
		});
	}

	public static boolean createIcePlatform(LivingEntity entity, Level world, int level) {
		BlockPos entityPos = entity.blockPosition();
		BlockPos belowPos = entityPos.below();

		if (!world.isRainingAt(entityPos)) {
			return false;
		}

		BlockState currentBelow = world.getBlockState(belowPos);

		if (!currentBelow.isAir() && !currentBelow.liquid()) {
			return false;
		}

		// Use regular ice (not frosted ice which melts to water)
		BlockState ice = Blocks.ICE.defaultBlockState();

		if (ice.canSurvive(world, belowPos) && world.isUnobstructed(ice, belowPos, CollisionContext.empty())) {
			world.setBlock(belowPos, ice, 3);
			// Schedule removal after 1-2 seconds (20-40 ticks)
			long removalTick = world.getGameTime() + Mth.nextInt(entity.getRandom(), 20, 40);
			icePlatforms.put(belowPos.immutable(), removalTick);
			return true;
		}

		return false;
	}
}
