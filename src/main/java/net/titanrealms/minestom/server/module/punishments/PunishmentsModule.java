package net.titanrealms.minestom.server.module.punishments;

import net.minestom.server.MinecraftServer;
import net.minestom.server.command.CommandManager;
import net.minestom.server.command.builder.Command;
import net.titanrealms.api.client.TitanApi;
import net.titanrealms.api.client.model.punishment.PunishmentType;
import net.titanrealms.api.client.modules.playerdata.PlayerDataApi;
import net.titanrealms.api.client.modules.punishments.PunishmentApi;
import net.titanrealms.minestom.server.TitanServer;
import net.titanrealms.minestom.server.module.punishments.commands.DurationPunishmentCommand;
import net.titanrealms.minestom.server.module.punishments.listener.PunishmentJoinListener;
import org.jetbrains.annotations.NotNull;

public class PunishmentsModule {

    public PunishmentsModule(TitanServer titanServer) {

        this.registerCommands(
                new DurationPunishmentCommand(titanServer.getApi(), titanServer.getLanguageManager(), PunishmentType.BAN, "ban", true),
                new DurationPunishmentCommand(titanServer.getApi(), titanServer.getLanguageManager(), PunishmentType.MUTE, "mute", true),
                new DurationPunishmentCommand(titanServer.getApi(), titanServer.getLanguageManager(), PunishmentType.KICK, "kick", false),
                new DurationPunishmentCommand(titanServer.getApi(), titanServer.getLanguageManager(), PunishmentType.WARNING, "warn", false)
        );

        new PunishmentJoinListener(titanServer);
    }

    private void registerCommands(@NotNull Command... commands) {
        CommandManager commandManager = MinecraftServer.getCommandManager();
        for (Command command : commands) {
            commandManager.register(command);
        }
    }
}
