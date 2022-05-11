package net.titanrealms.minestom.server.utils.argument;

import net.minestom.server.MinecraftServer;
import net.minestom.server.command.CommandSender;
import net.minestom.server.command.builder.CommandContext;
import net.minestom.server.command.builder.NodeMaker;
import net.minestom.server.command.builder.arguments.Argument;
import net.minestom.server.command.builder.exception.ArgumentSyntaxException;
import net.minestom.server.command.builder.suggestion.Suggestion;
import net.minestom.server.command.builder.suggestion.SuggestionEntry;
import net.minestom.server.entity.Player;
import net.minestom.server.network.packet.server.play.DeclareCommandsPacket;
import net.minestom.server.utils.StringUtils;
import net.minestom.server.utils.binary.BinaryWriter;
import net.titanrealms.api.client.model.playerdata.PlayerData;
import net.titanrealms.api.client.model.spring.Page;
import net.titanrealms.api.client.model.spring.Pageable;
import net.titanrealms.api.client.modules.playerdata.PlayerDataApi;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.CompletableFuture;

public class ArgumentApiPlayer extends Argument<CompletableFuture<PlayerData>> {
    public static final int SPACE_ERROR = 1;

    private final @NotNull PlayerDataApi api;

    public ArgumentApiPlayer(@NotNull String id, @NotNull PlayerDataApi api) {
        super(id);
        this.api = api;

        this.setSuggestionCallback(this::suggestPlayer);
    }

    @Override
    public @NotNull CompletableFuture<PlayerData> parse(@NotNull String input) throws ArgumentSyntaxException {
        if (input.contains(StringUtils.SPACE))
            throw new ArgumentSyntaxException("Word cannot contain space character", input, SPACE_ERROR);

        return this.api.retrievePlayerDataByUsername(input);
    }

    @Override
    public void processNodes(@NotNull NodeMaker nodeMaker, boolean executable) {
        // Can be any word, add only one argument node
        DeclareCommandsPacket.Node argumentNode = simpleArgumentNode(this, executable, false, false);
        argumentNode.parser = "brigadier:string";
        argumentNode.properties = BinaryWriter.makeArray(packetWriter -> {
            packetWriter.writeVarInt(0); // Single word
        });
        nodeMaker.addNodes(new DeclareCommandsPacket.Node[]{argumentNode});
    }

    private void suggestPlayer(@NotNull CommandSender sender, @NotNull CommandContext context, @NotNull Suggestion suggestion) {
        String input = context.getRaw(this);

        if (input == null) { // we don't want to query the DB if no name is specified, but online players are cheap.
            MinecraftServer.getConnectionManager().getOnlinePlayers().stream()
                    .map(Player::getUsername)
                    .map(SuggestionEntry::new)
                    .forEach(suggestion::addEntry);
            return;
        }
        // We don't combine the online usernames here as they should be returned anyway.
        // However, we could prioritise online players in the future, would be hard tho.

        // todo what if there's a high response time?
        this.api.searchPlayerDataByUsername(input, Pageable.of(0, 10)).thenApply(Page::content).thenAccept(list -> {
            list.forEach(playerData -> suggestion.addEntry(new SuggestionEntry(playerData.username())));
        }).join();
    }
}
