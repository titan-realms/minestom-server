package net.titanrealms.minestom.server.module.language;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import net.titanrealms.api.client.TitanApi;
import net.titanrealms.api.client.model.language.Language;
import net.titanrealms.api.client.model.server.ServerType;
import net.titanrealms.api.client.modules.language.LanguageApi;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;

public class LanguageManager {
    private static final MiniMessage MINI_MESSAGE = MiniMessage.builder()
            .tags(TagResolver.standard()) // This is the default, but we're being implicit.
            .build();

    private final Map<ServerType, Map<Language, Map<String, String>>> languageStrings = new ConcurrentHashMap<>();

    private final Collection<ServerType> serverTypes;
    private final LanguageApi languageApi;

    public Map<ServerType, Map<Language, Map<String, String>>> getLanguageStrings() {
        return this.languageStrings;
    }

    public LanguageManager(TitanApi api, Collection<ServerType> serverTypes) {
        this.serverTypes = Collections.unmodifiableCollection(serverTypes);
        this.languageApi = api.getLanguageApi();

        for (ServerType serverType : this.serverTypes) {
            this.subscribeForUpdates(serverType);
            this.languageApi.retrieveLanguagePack(serverType).thenAccept(data -> {
                this.languageStrings.put(serverType, data);
            });
        }
    }

    private void subscribeForUpdates(ServerType serverType) {
        this.languageApi.subscribeToLanguageUpdates(serverType, data -> this.languageStrings.put(serverType, data));
    }

    public Component get(ServerType serverType, Language language, String key, TagResolver.Single... placeholders) {
        return MINI_MESSAGE.deserialize(this.languageStrings.get(serverType).get(language).get(key), TagResolver.resolver(placeholders));
    }

    // todo this is only temporary as we don't have a lang storage system yet.
    public Component get(ServerType serverType, String key, TagResolver.Single... placeholders) {
        return MINI_MESSAGE.deserialize(this.languageStrings.get(serverType).get(Language.EN_UK).get(key), TagResolver.resolver(placeholders));
    }
}
