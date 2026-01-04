package justfatlard.rain_walker;

import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import eu.pb4.polymer.resourcepack.api.PolymerResourcePackUtils;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.ShapeContext;
import net.minecraft.entity.LivingEntity;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;

public class RainWalker implements ModInitializer {
	public static final String MOD_ID = "rain-walker";

	public static final RegistryKey<net.minecraft.enchantment.Enchantment> RAIN_WALKER =
		RegistryKey.of(RegistryKeys.ENCHANTMENT, Identifier.of(MOD_ID, "rain_walker"));

	// Track ice platforms for removal (position -> removal tick)
	private static final Map<BlockPos, Long> icePlatforms = new ConcurrentHashMap<>();

	@Override
	public void onInitialize() {
		// Register mod assets (lang files) with Polymer for vanilla clients
		PolymerResourcePackUtils.addModAssets(MOD_ID);
		PolymerResourcePackUtils.markAsRequired();

		System.out.println("[rain-walker] Rain Walker enchantment loaded");

		// Register tick event to remove expired ice platforms
		ServerTickEvents.END_SERVER_TICK.register(server -> {
			long currentTick = server.getOverworld().getTime();
			Iterator<Map.Entry<BlockPos, Long>> iterator = icePlatforms.entrySet().iterator();

			while (iterator.hasNext()) {
				Map.Entry<BlockPos, Long> entry = iterator.next();
				if (currentTick >= entry.getValue()) {
					BlockPos pos = entry.getKey();
					// Remove ice from all dimensions (check overworld, nether, end)
					for (ServerWorld world : server.getWorlds()) {
						if (world.getBlockState(pos).isOf(Blocks.ICE)) {
							world.setBlockState(pos, Blocks.AIR.getDefaultState());
							break;
						}
					}
					iterator.remove();
				}
			}
		});
	}

	/**
	 * Creates an ice platform under the entity when falling in rain
	 * Returns true if ice was placed
	 */
	public static boolean createIcePlatform(LivingEntity entity, World world, int level) {
		BlockPos entityPos = entity.getBlockPos();
		BlockPos belowPos = entityPos.down();

		// Check if it's raining at the entity's position (must be exposed to sky and raining)
		if (!world.hasRain(entityPos)) {
			return false;
		}

		BlockState currentBelow = world.getBlockState(belowPos);

		// Don't place if there's already a solid block
		if (!currentBelow.isAir() && !currentBelow.isLiquid()) {
			return false;
		}

		// Use regular ice (not frosted ice which melts to water)
		BlockState ice = Blocks.ICE.getDefaultState();

		if (ice.canPlaceAt(world, belowPos) && world.canPlace(ice, belowPos, ShapeContext.absent())) {
			world.setBlockState(belowPos, ice);
			// Schedule removal after 1-2 seconds (20-40 ticks)
			long removalTick = world.getTime() + MathHelper.nextInt(entity.getRandom(), 20, 40);
			icePlatforms.put(belowPos.toImmutable(), removalTick);
			return true;
		}

		return false;
	}
}
