package net.titanrealms.minestom.server.module.punishments.listener;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import net.minestom.server.MinecraftServer;
import net.minestom.server.entity.Player;
import net.minestom.server.event.player.PlayerSpawnEvent;
import net.titanrealms.api.client.model.punishment.Punishment;
import net.titanrealms.api.client.model.punishment.PunishmentType;
import net.titanrealms.api.client.model.server.ServerType;
import net.titanrealms.api.client.modules.punishments.PunishmentApi;
import net.titanrealms.minestom.server.TitanServer;
import net.titanrealms.minestom.server.module.language.LanguageManager;
import net.titanrealms.minestom.server.utils.DurationFormatter;
import org.jetbrains.annotations.NotNull;

import java.time.Duration;
import java.time.Instant;

public class PunishmentJoinListener {
    private final LanguageManager languageManager;

    public PunishmentJoinListener(TitanServer titanServer) {
        this.languageManager = titanServer.getLanguageManager();

        PunishmentApi punishmentApi = titanServer.getApi().getPunishmentApi();
        MinecraftServer.getGlobalEventHandler().addListener(PlayerSpawnEvent.class, event -> {
            Player player = event.getPlayer();
            punishmentApi.retrievePlayerNonNotifiedPunishments(player.getUuid()).thenAccept(punishments -> {
                if (punishments.isEmpty()) return;

                TextComponent.Builder builder = Component.text();
                builder.append(this.languageManager.get(ServerType.GLOBAL, "join-punishments-notification"));

                for (Punishment punishment : punishments) {
                    builder.append(Component.newline()).append(this.createLine(punishment));
                }
                player.sendMessage(builder.build());
            });
        });
    }

    private Component createLine(@NotNull Punishment punishment) {
        PunishmentType type = punishment.getPunishmentType();

        TagResolver.Single timeSince = Placeholder.component("time_since", Component.text(DurationFormatter.toGreatestUnit(Duration.between(punishment.getTimestamp(), Instant.now()))));
        TagResolver.Single reason = Placeholder.component("reason", Component.text(punishment.getReason()));
        return switch (type) {
            case MUTE -> this.languageManager.get(ServerType.GLOBAL, "join-punishments-notification-mute", timeSince, reason,
                    Placeholder.component("duration", Component.text(DurationFormatter.toGreatestUnit(Duration.between(punishment.getTimestamp(), punishment.getExpiry())))));
            case WARNING -> this.languageManager.get(ServerType.GLOBAL, "join-punishments-notification-warning", timeSince, reason);
            default -> Component.empty();
        };
    }
}
