package net.titanrealms.minestom.server.module.punishments;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.titanrealms.api.client.model.language.Language;
import net.titanrealms.api.client.model.punishment.Punishment;
import net.titanrealms.api.client.model.punishment.PunishmentType;
import net.titanrealms.api.client.model.server.ServerType;
import net.titanrealms.minestom.server.module.language.LanguageManager;
import net.titanrealms.minestom.server.module.punishments.commands.DurationPunishmentCommand;
import net.titanrealms.minestom.server.utils.DurationFormatter;

import java.time.Duration;

public class DisconnectScreenUtils {

    public static Component create(LanguageManager languageManager, Punishment punishment) {
        Component punishmentLine = createPunishmentLine(languageManager, punishment);

        return languageManager.get(ServerType.GLOBAL, "punishment-disconnect-message",
                Placeholder.component("punishment_line", punishmentLine),
                Placeholder.component("id", Component.text(punishment.getId().toHexString())),
                Placeholder.component("reason", Component.text(punishment.getReason())));
    }

    private static Component createPunishmentLine(LanguageManager languageManager, Punishment punishment) {
        Component punishmentLine;
        if (punishment.getPunishmentType() == PunishmentType.BAN) {
            if (punishment.isPermanent()) {
                punishmentLine = languageManager.get(ServerType.GLOBAL, "punishment-disconnect-ban-line-permanent");
            } else {
                punishmentLine = languageManager.get(ServerType.GLOBAL, "punishment-disconnect-ban-line",
                        Placeholder.component("duration", Component.text(DurationFormatter.formatFull(Duration.between(punishment.getTimestamp(), punishment.getExpiry())))));
            }
        } else {
            punishmentLine = languageManager.get(ServerType.GLOBAL, "punishment-disconnect-kick-line");
        }
        return punishmentLine;
    }
}
