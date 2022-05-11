package net.titanrealms.minestom.server.module.punishments.commands;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.minestom.server.MinecraftServer;
import net.minestom.server.command.CommandSender;
import net.minestom.server.command.builder.Command;
import net.minestom.server.command.builder.CommandContext;
import net.minestom.server.command.builder.arguments.ArgumentString;
import net.minestom.server.command.builder.arguments.minecraft.ArgumentTime;
import net.minestom.server.entity.Player;
import net.titanrealms.api.client.TitanApi;
import net.titanrealms.api.client.model.punishment.Punishment;
import net.titanrealms.api.client.model.punishment.PunishmentType;
import net.titanrealms.api.client.model.server.ServerType;
import net.titanrealms.api.client.modules.playerdata.PlayerDataApi;
import net.titanrealms.api.client.modules.punishments.PunishmentApi;
import net.titanrealms.minestom.server.module.language.LanguageManager;
import net.titanrealms.minestom.server.module.punishments.DisconnectScreenUtils;
import net.titanrealms.minestom.server.utils.DurationFormatter;
import net.titanrealms.minestom.server.utils.argument.ArgumentApiPlayer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.time.Instant;

public class DurationPunishmentCommand extends Command {
    private static final Logger LOGGER = LoggerFactory.getLogger(DurationPunishmentCommand.class);

    private final ArgumentApiPlayer playerArgument;
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

        this.playerArgument = new ArgumentApiPlayer("target", this.playerDataApi);

        this.timeOption = timeOption;
        this.langPrefix = "command-" + command + "-";

        ArgumentString reasonArgument = new ArgumentString("reason");

        this.setDefaultExecutor(this::helpCommand);
        this.addSyntax(this::execute, this.playerArgument, reasonArgument);
        if (timeOption) this.addSyntax(this::execute, this.playerArgument, this.durationArgument, reasonArgument);
    }

    private void helpCommand(CommandSender sender, CommandContext context) {
        sender.sendMessage(this.languageManager.get(ServerType.GLOBAL, this.langPrefix + "help")
                .append(Component.newline())
                .append(this.languageManager.get(ServerType.GLOBAL, this.langPrefix + "example")));
    }

    // todo logic for overwriting existing punishment
    private void execute(CommandSender sender, CommandContext context) {
        String targetUsername = context.getRaw(this.playerArgument);
        context.get(this.playerArgument).thenAccept(playerData -> {
            if (playerData == null) {
                sender.sendMessage(this.languageManager.get(ServerType.GLOBAL, "player-not-found", Placeholder.component("name", Component.text(targetUsername))));
                return;
            }

            Player onlineTarget = MinecraftServer.getConnectionManager().getPlayer(playerData.id());

            String reason = context.get("reason");
            Instant nowInstant = Instant.now();
            Duration duration = context.get(this.durationArgument);
            Instant endTime = duration == null ? null : nowInstant.plusMillis(duration.toMillis());

            Punishment punishment = new Punishment(this.punishmentType, endTime, null, playerData.id(), reason, onlineTarget != null);
            this.punishmentApi.createPunishment(punishment)
                    .whenComplete((punishment1, throwable) -> this.onCompletion(sender, punishment1, throwable, playerData.username(), onlineTarget));
        });
    }

    private void onCompletion(@NotNull CommandSender sender, Punishment punishment, Throwable throwable, String name, @Nullable Player onlineTarget) {
        if (throwable != null) {
            LOGGER.error("Unknown error occurred creating Punishment: ", throwable);
            sender.sendMessage(this.languageManager.get(ServerType.GLOBAL, "unknown-error-occurred"));
            return;
        }
        Component confirmationMessage;
        if (this.timeOption && punishment.isPermanent()) {
            confirmationMessage = this.languageManager.get(ServerType.GLOBAL, this.langPrefix + "success-permanent",
                    Placeholder.component("name", Component.text(name)),
                    Placeholder.component("reason", Component.text(punishment.getReason())));
        } else if (this.timeOption) {
            confirmationMessage = this.languageManager.get(ServerType.GLOBAL, this.langPrefix + "success",
                    Placeholder.component("duration", Component.text(DurationFormatter.toGreatestUnit(Duration.between(punishment.getTimestamp(), punishment.getExpiry())))),
                    Placeholder.component("name", Component.text(name)),
                    Placeholder.component("reason", Component.text(punishment.getReason())));
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
