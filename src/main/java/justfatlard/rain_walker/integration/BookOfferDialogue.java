package justfatlard.rain_walker.integration;

import java.util.List;
import justfatlard.village_quests.api.DialogueRegistry;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.ItemEnchantments;

/**
 * A librarian who has the book and will part with it for a price.
 *
 * <p>Lava Walker is a treasure enchantment, which means the game will never put
 * it on an enchanting table and a player can do everything right for a hundred
 * hours without seeing one. The only routes are fishing it out of a lake and
 * finding it in a chest, and neither is a plan.
 *
 * <p>A librarian selling a book is the oldest idiom in the game for exactly this
 * problem, and it is a shop rather than a quest because there is nothing to do:
 * you have emeralds, they have the book, and at sixty reputation they have
 * decided you are the sort of person who should have it.
 */
public final class BookOfferDialogue {
	private BookOfferDialogue() {}

	private static final String OPTION_ID = "rain-walker:book";

	/** Treasure, so it costs more than a boat and asks for more than a boat did. */
	private static final int MIN_REPUTATION = 60;
	private static final int PRICE = 32;

	private static final ResourceKey<Enchantment> RAIN_WALKER = ResourceKey.create(
		Registries.ENCHANTMENT, Identifier.fromNamespaceAndPath("rain-walker", "rain_walker"));

	public static void register() {
		DialogueRegistry.registerProfessionDialogue("librarian", (villager, player, reputation) -> {
			// Only while it is actually coming down. The provider is asked every time
			// a conversation opens, so the sky is free to change its mind.
			if (!villager.level().isRaining()) return List.of();

			return List.of(new DialogueRegistry.DialogueOption(
				OPTION_ID,
				Component.literal("Is there anything to be done about all this rain?"),
				MIN_REPUTATION, Integer.MAX_VALUE));
		});

		DialogueRegistry.registerDialogueHandler(OPTION_ID, BookOfferDialogue::sell);
	}

	private static Component sell(net.minecraft.world.entity.npc.villager.Villager villager,
			ServerPlayer player, String optionId) {
		if (countEmeralds(player) < PRICE) {
			return Component.literal("Funny you should ask, on today of all days. There is a book for it. " + PRICE
				+ " emeralds, and only while I am in this mood about the weather.");
		}

		ItemStack book = enchantedBook(player);
		if (book == null) {
			return Component.literal("...I had it here somewhere. Come back.");
		}

		takeEmeralds(player);
		if (!player.getInventory().add(book)) {
			player.drop(book, false, net.minecraft.util.Prediction.SERVER_ONLY);
		}

		return Component.literal(
			"Boots. Go on then, out you go - it only makes sense while it is raining, "
			+ "and you will not believe me about it until you have seen it.");
	}

	/** Null when the enchantment is not in this world's registry, rather than a crash. */
	private static ItemStack enchantedBook(ServerPlayer player) {
		return player.level().registryAccess()
			.lookup(Registries.ENCHANTMENT)
			.flatMap(registry -> registry.get(RAIN_WALKER))
			.map(BookOfferDialogue::bookOf)
			.orElse(null);
	}

	private static ItemStack bookOf(Holder.Reference<Enchantment> enchantment) {
		ItemStack book = new ItemStack(Items.ENCHANTED_BOOK);
		ItemEnchantments.Mutable stored = new ItemEnchantments.Mutable(ItemEnchantments.EMPTY);
		stored.set(enchantment, 1);
		book.set(net.minecraft.core.component.DataComponents.STORED_ENCHANTMENTS, stored.toImmutable());
		return book;
	}

	private static int countEmeralds(ServerPlayer player) {
		int found = 0;
		for (ItemStack stack : player.getInventory().getNonEquipmentItems()) {
			if (stack.is(Items.EMERALD)) found += stack.getCount();
		}
		return found;
	}

	private static void takeEmeralds(ServerPlayer player) {
		int remaining = PRICE;
		for (ItemStack stack : player.getInventory().getNonEquipmentItems()) {
			if (remaining <= 0) return;
			if (!stack.is(Items.EMERALD)) continue;

			int taken = Math.min(remaining, stack.getCount());
			stack.shrink(taken);
			remaining -= taken;
		}
	}
}
