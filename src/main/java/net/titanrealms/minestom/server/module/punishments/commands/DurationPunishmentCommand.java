package net.titanrealms.minestom.server.module.punishments.commands;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.minestom.server.MinecraftServer;
import net.minestom.server.command.CommandSender;
import net.minestom.server.command.builder.Command;
import net.minestom.server.command.builder.CommandContext;
import net.minestom.server.command.builder.arguments.ArgumentString;
import net.minestom.server.command.builder.arguments.ArgumentWord;
import net.minestom.server.command.builder.arguments.minecraft.ArgumentTime;
import net.minestom.server.command.builder.suggestion.Suggestion;
import net.minestom.server.command.builder.suggestion.SuggestionEntry;
import net.minestom.server.entity.Player;
import net.titanrealms.api.client.TitanApi;
import net.titanrealms.api.client.model.punishment.Punishment;
import net.titanrealms.api.client.model.punishment.PunishmentType;
import net.titanrealms.api.client.model.server.ServerType;
import net.titanrealms.api.client.model.spring.Page;
import net.titanrealms.api.client.model.spring.Pageable;
import net.titanrealms.api.client.modules.playerdata.PlayerDataApi;
import net.titanrealms.api.client.modules.punishments.PunishmentApi;
import net.titanrealms.minestom.server.module.language.LanguageManager;
import net.titanrealms.minestom.server.module.punishments.DisconnectScreenUtils;
import net.titanrealms.minestom.server.utils.DurationFormatter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.Locale;
import java.util.UUID;

public class DurationPunishmentCommand extends Command {
    public static final Instant MAX_INSTANT = new Date(7255353600000L).toInstant();
    private static final Logger LOGGER = LoggerFactory.getLogger(DurationPunishmentCommand.class);

    private final ArgumentWord playerArgument;
    private final ArgumentTime durationArgument = new ArgumentTime("duration");

    private final PunishmentType punishmentType;
    private final PunishmentApi punishmentApi;
    private final PlayerDataApi playerDataApi;
    private final LanguageManager languageManager;

    private final boolean timeOption;
    private final String langPrefix;

    public DurationPunishmentCommand(TitanApi api, LanguageManager languageManager, PunishmentType punishmentType, String command, boolean timeOption) {
        super(command);
        this.punishmentType = punishmentType;
        this.punishmentApi = api.getPunishmentApi();
        this.playerDataApi = api.getPlayerDataApi();
        this.languageManager = languageManager;

        this.playerArgument = new ArgumentWord("target");
        this.playerArgument.setSuggestionCallback(this::suggestPlayer);

        this.timeOption = timeOption;
        this.langPrefix = "command-" + command + "-";

        ArgumentString reasonArgument = new ArgumentString("reason");

        this.setDefaultExecutor(this::helpCommand);
        this.addSyntax(this::execute, this.playerArgument, reasonArgument);
        if (timeOption) this.addSyntax(this::execute, this.playerArgument, this.durationArgument, reasonArgument);
    }

    private void suggestPlayer(@NotNull CommandSender sender, @NotNull CommandContext context, @NotNull Suggestion suggestion) {
        String input = context.getRaw(this.playerArgument);

        MinecraftServer.getConnectionManager().getOnlinePlayers().stream().map(Player::getUsername)
                .filter(username -> input == null || username.toLowerCase(Locale.ROOT).contains(input.toLowerCase(Locale.ROOT))).map(SuggestionEntry::new).forEach(suggestion::addEntry);

        if (input == null)
            return; // we don't want to query the DB if no name is specified, but online players are cheap.

        // todo what if there's a high response time?
        this.playerDataApi.searchPlayerDataByUsername(input, Pageable.of(0, 10)).thenApply(Page::content).thenAccept(list -> {
            list.forEach(playerData -> suggestion.addEntry(new SuggestionEntry(playerData.username())));
        }).join();
    }

    private void helpCommand(CommandSender sender, CommandContext context) {
        sender.sendMessage(this.languageManager.get(ServerType.GLOBAL, this.langPrefix + "help")
                .append(Component.newline())
                .append(this.languageManager.get(ServerType.GLOBAL, this.langPrefix + "example")));
    }

    // todo logic for overwriting existing punishment
    // todo the timestamp field is unnecessary, use the ID.
    private void execute(CommandSender sender, CommandContext context) {
        String targetUsername = context.get(this.playerArgument);

        Player onlineTarget = MinecraftServer.getConnectionManager().getPlayer(targetUsername);
        UUID targetId;
        if (onlineTarget == null) { // get from api if not online
            targetId = this.playerDataApi.retrievePlayerDataByUsername(targetUsername).thenApply(playerData -> playerData == null ? null : playerData.id()).join();
            if (targetId == null) {
                sender.sendMessage(this.languageManager.get(ServerType.GLOBAL, "player-not-found", Placeholder.component("name", Component.text(targetUsername))));
                return;
            }
        } else {
            targetId = onlineTarget.getUuid();
        }

        String reason = context.get("reason");

        Instant nowInstant = Instant.now();
        Duration duration = context.get(this.durationArgument);
        Instant endTime = duration == null ? MAX_INSTANT : nowInstant.plusMillis(duration.toMillis()); // We use LocalDateTime.MAX because Instant.MAX is greater than the max supported Date.

        Punishment punishment = new Punishment(this.punishmentType, nowInstant, endTime, null, targetId, reason, onlineTarget != null);
        this.punishmentApi.createPunishment(punishment)
                .whenComplete((punishment1, throwable) -> this.onCompletion(sender, punishment1, throwable, targetUsername, onlineTarget));
    }

    private void onCompletion(@NotNull CommandSender sender, Punishment punishment, Throwable throwable, String name, @Nullable Player onlineTarget) {
        if (throwable != null) {
            LOGGER.error("Unknown error occurred creating Punishment: ", throwable);
            sender.sendMessage(this.languageManager.get(ServerType.GLOBAL, "unknown-error-occurred"));
            return;
        }
        Component confirmationMessage;
        if (this.timeOption && punishment.getExpiry().equals(MAX_INSTANT)) {
            confirmationMessage = this.languageManager.get(ServerType.GLOBAL, this.langPrefix + "success-permanent",
                    Placeholder.component("name", Component.text(name)),
                    Placeholder.component("reason", Component.text(punishment.getReason())));
        } else if (this.timeOption) {
            confirmationMessage = this.languageManager.get(ServerType.GLOBAL, this.langPrefix + "success",
                    Placeholder.component("duration", Component.text(DurationFormatter.toGreatestUnit(Duration.between(punishment.getTimestamp(), punishment.getExpiry())))),
                    Placeholder.component("name", Component.text(name)),
                    Placeholder.component("reason", Component.text(punishment.getReason()))); // todo format
        } else {
            confirmationMessage = this.languageManager.get(ServerType.GLOBAL, this.langPrefix + "success",
                    Placeholder.component("name", Component.text(name)),
                    Placeholder.component("reason", Component.text(punishment.getReason())));
        }
        sender.sendMessage(confirmationMessage);

        if (onlineTarget != null) {
            switch (this.punishmentType) {
                case BAN, KICK -> onlineTarget.kick(DisconnectScreenUtils.create(this.languageManager, punishment));
                case MUTE, WARNING -> onlineTarget.sendMessage(""); // todo
            }
        }
    }
}
