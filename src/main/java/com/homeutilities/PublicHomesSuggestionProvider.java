package com.homeutilities;

import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.minecraft.server.command.ServerCommandSource;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

public class PublicHomesSuggestionProvider implements SuggestionProvider<ServerCommandSource> {
    @Override
    public CompletableFuture<Suggestions> getSuggestions(CommandContext<ServerCommandSource> context, SuggestionsBuilder builder) throws CommandSyntaxException {
        ServerCommandSource source = context.getSource();
        String input = builder.getRemaining().toLowerCase();

        List<String> homesNames = JsonHandler.listPublicLocations(Objects.requireNonNull(source.getPlayer()));

        if (homesNames != null) {
            for (String homeName : homesNames) {
                if (homeName.toLowerCase().startsWith(input)) {
                    builder.suggest(homeName);
                }
            }
        }

        return builder.buildFuture();
    }
}
