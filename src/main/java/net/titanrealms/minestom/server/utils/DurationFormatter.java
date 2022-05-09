package net.titanrealms.minestom.server.utils;

import org.jetbrains.annotations.NotNull;

import java.time.Duration;

public class DurationFormatter {

    public static @NotNull String formatFull(@NotNull Duration duration) {
        long days = duration.toDays();
        long hours = duration.toHoursPart();
        long minutes = duration.toMinutesPart();
        long seconds = duration.toSecondsPart();

        if (days > 0) {
            return String.format("%sd %sh %sm %ss", days, hours, minutes, seconds);
        }
        if (hours > 0) {
            return String.format("%sh %sm %ss", hours, minutes, seconds);
        }
        if (minutes > 0) {
            return String.format("%sm %ss", minutes, seconds);
        }
        return String.format("%ss", seconds);
    }

    public static @NotNull String toGreatestUnit(@NotNull Duration duration) {
        long days = duration.toDays();
        if (days > 0)
            return days + "d";

        long hours = duration.toHours();
        if (hours > 0)
            return hours + "h";

        long minutes = duration.toMinutes();
        if (minutes > 0)
            return minutes + "m";

        long seconds = duration.toSeconds();
            return seconds + "s";
    }
}
