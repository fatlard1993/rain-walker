package justfatlard.rain_walker;

import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import justfatlard.pandorical.api.PandoricalApi;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
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
	/** Where the ice went, what it stood in for, and when it goes again. */
	private record Platform(BlockState replaced, long removeAt) {}

	private static final Map<GlobalPos, Platform> icePlatforms = new ConcurrentHashMap<>();

	@Override
	public void onInitialize() {
		// Guarded class load: BookOfferDialogue names village-quests types.
		if (net.fabricmc.loader.api.FabricLoader.getInstance().isModLoaded("village-quests-justfatlard")) {
			justfatlard.rain_walker.integration.BookOfferDialogue.register();
		}

		if (PandoricalApi.isAvailable()) {
			PandoricalApi.content().registerModAssets(MOD_ID);
		}

		System.out.println("[rain-walker] Rain Walker enchantment loaded");

		// Sweep expired ice platforms each server tick
		ServerTickEvents.END_SERVER_TICK.register(server -> {
			long currentTick = server.overworld().getGameTime();
			Iterator<Map.Entry<GlobalPos, Platform>> iterator = icePlatforms.entrySet().iterator();

			while (iterator.hasNext()) {
				Map.Entry<GlobalPos, Platform> entry = iterator.next();
				if (currentTick < entry.getValue().removeAt()) continue;
				iterator.remove();

				ServerLevel world = server.getLevel(entry.getKey().dimension());
				BlockPos pos = entry.getKey().pos();
				// Back to what it stood in for, water and lava included: a source it had frozen over
				// is still a source, not a hole in the lake.
				if (world != null && world.getBlockState(pos).is(Blocks.ICE)) {
					world.setBlock(pos, entry.getValue().replaced(), 3);
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
			icePlatforms.put(GlobalPos.of(world.dimension(), belowPos.immutable()), new Platform(currentBelow, removalTick));
			return true;
		}

		return false;
	}
}
