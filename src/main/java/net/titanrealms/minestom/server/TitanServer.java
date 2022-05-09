package net.titanrealms.minestom.server;

import net.minestom.server.MinecraftServer;
import net.minestom.server.extras.MojangAuth;
import net.minestom.server.extras.velocity.VelocityProxy;
import net.titanrealms.api.client.TitanApi;
import net.titanrealms.api.client.model.server.ServerType;
import net.titanrealms.minestom.server.development.DevelopmentMode;
import net.titanrealms.minestom.server.module.language.LanguageManager;
import net.titanrealms.minestom.server.module.punishments.PunishmentsModule;
import org.jetbrains.annotations.NotNull;

import java.util.Set;

public class TitanServer {
    private final @NotNull MinecraftServer minecraftServer = MinecraftServer.init();

    private final @NotNull TitanApi titanApi = new TitanApi();

    private final @NotNull LanguageManager languageManager = new LanguageManager(this.titanApi, Set.of(ServerType.GLOBAL));

    public TitanServer(boolean development) {
        if (!development)
            VelocityProxy.enable(System.getenv("VELOCITY_SECRET"));
        else
            MojangAuth.init();

        if (development)
            new DevelopmentMode();

        new PunishmentsModule(this);

        this.minecraftServer.start("localhost", 25565);
    }

    public static void start() {
        new TitanServer(false);
    }

    public @NotNull TitanApi getApi() {
        return this.titanApi;
    }

    public @NotNull MinecraftServer getMinecraftServer() {
        return this.minecraftServer;
    }

    public LanguageManager getLanguageManager() {
        return this.languageManager;
    }
}
