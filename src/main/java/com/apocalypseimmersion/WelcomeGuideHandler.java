package com.apocalypseimmersion;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = ApocalypseImmersion.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class WelcomeGuideHandler {

    private static final String GIVEN_TAG = "apocalypseimmersion_welcome_given";

    @SubscribeEvent
    public static void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;

        CompoundTag persistent = player.getPersistentData().getCompound(Player.PERSISTED_NBT_TAG);
        if (persistent.getBoolean(GIVEN_TAG)) return;

        persistent.putBoolean(GIVEN_TAG, true);
        player.getPersistentData().put(Player.PERSISTED_NBT_TAG, persistent);

        player.sendSystemMessage(Component.literal("\u00A76\u00A7l[Apocalypse Mod] \u00A7fThanks for installing Apocalypse Mod! Check your hotbar for the field guide."));

        ItemStack book = new ItemStack(Items.WRITTEN_BOOK);
        CompoundTag tag = book.getOrCreateTag();
        tag.putString("title", "Apocalypse Field Guide");
        tag.putString("author", "Apocalypse Mod");
        tag.putBoolean("resolved", true);

        ListTag pages = new ListTag();
        pages.add(StringTag.valueOf(Component.Serializer.toJson(Component.literal(
            "Thanks for installing Apocalypse Mod!\n\nPress [K] anytime to open the Immersion Settings menu.\n\nUse /reload after editing config files."
        ))));
        pages.add(StringTag.valueOf(Component.Serializer.toJson(Component.literal(
            "Field Features:\n- Equip torches or lanterns in your Curios Belt slot for hands-free light.\n- Flashlights attract zombies in their beam.\n- Acoustic Radar tracks nearby sounds."
        ))));
        tag.put("pages", pages);

        if (!player.getInventory().add(book)) {
            player.drop(book, false);
        }
    }
}