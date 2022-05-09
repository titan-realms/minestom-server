package net.titanrealms.minestom.server.development;

import net.minestom.server.MinecraftServer;
import net.minestom.server.coordinate.Point;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.entity.GameMode;
import net.minestom.server.event.GlobalEventHandler;
import net.minestom.server.event.player.PlayerLoginEvent;
import net.minestom.server.event.player.PlayerSpawnEvent;
import net.minestom.server.instance.Instance;
import net.minestom.server.instance.block.Block;

public class DevelopmentMode {
    private final Instance instance = MinecraftServer.getInstanceManager().createInstanceContainer();
    private final Pos respawnPoint = new Pos(0, 40, 0);

    public DevelopmentMode() {
        this.setupInstance();
        GlobalEventHandler eventHandler = MinecraftServer.getGlobalEventHandler();

        eventHandler.addListener(PlayerLoginEvent.class, event -> {
            event.setSpawningInstance(this.instance);
            event.getPlayer().setRespawnPoint(this.respawnPoint);
        });
        eventHandler.addListener(PlayerSpawnEvent.class, event -> {
            event.getPlayer().setGameMode(GameMode.CREATIVE);
        });
    }

    private void setupInstance() {
        this.instance.setGenerator(unit -> {
            final Point start = unit.absoluteStart();
            final Point size = unit.size();
            for (int x = 0; x < size.blockX(); x++) {
                for (int z = 0; z < size.blockZ(); z++) {
                    for (int y = 0; y < Math.min(40 - start.blockY(), size.blockY()); y++) {
                        unit.modifier().setBlock(start.add(x, y, z), Block.STONE);
                    }
                }
            }
        });
    }
}
