package com.zetsumei.nocoin.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.zetsumei.nocoin.gacha.GachaManager;
import com.zetsumei.nocoin.gacha.GachaRarity;
import com.zetsumei.nocoin.gacha.GachaReward;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.ResourceLocationArgument;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Commandes d'administration pour le système Gacha.
 * 
 * /gacha list - Liste toutes les récompenses
 * /gacha add <item> <rarity> <displayName> [weight] - Ajoute une récompense
 * /gacha remove <item> - Retire une récompense
 * /gacha setweight <item> <weight> - Modifie le poids d'une récompense
 * /gacha setrarity <item> <rarity> - Modifie la rareté d'une récompense
 * /gacha rates - Affiche les probabilités de rareté
 * /gacha setrates <5star%> <4star%> <3star%> - Modifie les probabilités
 * /gacha reload - Recharge la configuration depuis le fichier
 * /gacha info <item> - Affiche les détails d'une récompense
 */
public class GachaCommand {

    // Suggestions pour les raretés
    private static final SuggestionProvider<CommandSourceStack> RARITY_SUGGESTIONS = (context, builder) ->
            SharedSuggestionProvider.suggest(
                    Arrays.stream(GachaRarity.values()).map(r -> r.name().toLowerCase()),
                    builder
            );

    // Suggestions pour les items du gacha existants
    private static final SuggestionProvider<CommandSourceStack> GACHA_ITEM_SUGGESTIONS = (context, builder) ->
            SharedSuggestionProvider.suggest(
                    GachaManager.getInstance().getAllRewards().stream().map(GachaReward::getItemId),
                    builder
            );

    // Suggestions pour tous les items Minecraft
    private static final SuggestionProvider<CommandSourceStack> ALL_ITEMS_SUGGESTIONS = (context, builder) ->
            SharedSuggestionProvider.suggestResource(ForgeRegistries.ITEMS.getKeys(), builder);

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
            Commands.literal("gacha")
                .requires(source -> source.hasPermission(2)) // Niveau op 2 requis
                
                // /gacha list [rarity]
                .then(Commands.literal("list")
                    .executes(GachaCommand::listAllRewards)
                    .then(Commands.argument("rarity", StringArgumentType.word())
                        .suggests(RARITY_SUGGESTIONS)
                        .executes(GachaCommand::listRewardsByRarity)
                    )
                )
                
                // /gacha add <item> <rarity> <displayName> [weight]
                .then(Commands.literal("add")
                    .then(Commands.argument("item", ResourceLocationArgument.id())
                        .suggests(ALL_ITEMS_SUGGESTIONS)
                        .then(Commands.argument("rarity", StringArgumentType.word())
                            .suggests(RARITY_SUGGESTIONS)
                            .then(Commands.argument("displayName", StringArgumentType.greedyString())
                                .executes(ctx -> addReward(ctx, 1.0))
                            )
                        )
                    )
                )
                
                // /gacha addweight <item> <rarity> <weight> <displayName>
                .then(Commands.literal("addweight")
                    .then(Commands.argument("item", ResourceLocationArgument.id())
                        .suggests(ALL_ITEMS_SUGGESTIONS)
                        .then(Commands.argument("rarity", StringArgumentType.word())
                            .suggests(RARITY_SUGGESTIONS)
                            .then(Commands.argument("weight", DoubleArgumentType.doubleArg(0.01, 1000.0))
                                .then(Commands.argument("displayName", StringArgumentType.greedyString())
                                    .executes(GachaCommand::addRewardWithWeight)
                                )
                            )
                        )
                    )
                )
                
                // /gacha remove <item>
                .then(Commands.literal("remove")
                    .then(Commands.argument("item", StringArgumentType.string())
                        .suggests(GACHA_ITEM_SUGGESTIONS)
                        .executes(GachaCommand::removeReward)
                    )
                )
                
                // /gacha setweight <item> <weight>
                .then(Commands.literal("setweight")
                    .then(Commands.argument("item", StringArgumentType.string())
                        .suggests(GACHA_ITEM_SUGGESTIONS)
                        .then(Commands.argument("weight", DoubleArgumentType.doubleArg(0.01, 1000.0))
                            .executes(GachaCommand::setWeight)
                        )
                    )
                )
                
                // /gacha setrarity <item> <rarity>
                .then(Commands.literal("setrarity")
                    .then(Commands.argument("item", StringArgumentType.string())
                        .suggests(GACHA_ITEM_SUGGESTIONS)
                        .then(Commands.argument("rarity", StringArgumentType.word())
                            .suggests(RARITY_SUGGESTIONS)
                            .executes(GachaCommand::setRarity)
                        )
                    )
                )
                
                // /gacha rates
                .then(Commands.literal("rates")
                    .executes(GachaCommand::showRates)
                )
                
                // /gacha setrates <5star> <4star> <3star>
                .then(Commands.literal("setrates")
                    .then(Commands.argument("fiveStar", DoubleArgumentType.doubleArg(0.0, 100.0))
                        .then(Commands.argument("fourStar", DoubleArgumentType.doubleArg(0.0, 100.0))
                            .then(Commands.argument("threeStar", DoubleArgumentType.doubleArg(0.0, 100.0))
                                .executes(GachaCommand::setRates)
                            )
                        )
                    )
                )
                
                // /gacha reload
                .then(Commands.literal("reload")
                    .executes(GachaCommand::reloadConfig)
                )

                // /gacha clear - Vide toutes les récompenses
                .then(Commands.literal("clear")
                    .executes(GachaCommand::clearAllRewards)
                )
                
                // /gacha info <item>
                .then(Commands.literal("info")
                    .then(Commands.argument("item", StringArgumentType.string())
                        .suggests(GACHA_ITEM_SUGGESTIONS)
                        .executes(GachaCommand::showItemInfo)
                    )
                )
                
                // /gacha (aide par défaut)
                .executes(GachaCommand::showHelp)
        );
    }

    private static int showHelp(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        
        source.sendSuccess(() -> Component.literal("═══ Commandes Gacha Admin ═══").withStyle(ChatFormatting.GOLD), false);
        source.sendSuccess(() -> Component.literal("/gacha list [rarity]").withStyle(ChatFormatting.YELLOW)
                .append(Component.literal(" - Liste les récompenses").withStyle(ChatFormatting.GRAY)), false);
        source.sendSuccess(() -> Component.literal("/gacha add <item> <rarity> <nom>").withStyle(ChatFormatting.YELLOW)
                .append(Component.literal(" - Ajoute une récompense").withStyle(ChatFormatting.GRAY)), false);
        source.sendSuccess(() -> Component.literal("/gacha addweight <item> <rarity> <poids> <nom>").withStyle(ChatFormatting.YELLOW)
                .append(Component.literal(" - Ajoute avec poids").withStyle(ChatFormatting.GRAY)), false);
        source.sendSuccess(() -> Component.literal("/gacha remove <item>").withStyle(ChatFormatting.YELLOW)
                .append(Component.literal(" - Retire une récompense").withStyle(ChatFormatting.GRAY)), false);
        source.sendSuccess(() -> Component.literal("/gacha setweight <item> <poids>").withStyle(ChatFormatting.YELLOW)
                .append(Component.literal(" - Modifie le poids").withStyle(ChatFormatting.GRAY)), false);
        source.sendSuccess(() -> Component.literal("/gacha setrarity <item> <rarity>").withStyle(ChatFormatting.YELLOW)
                .append(Component.literal(" - Modifie la rareté").withStyle(ChatFormatting.GRAY)), false);
        source.sendSuccess(() -> Component.literal("/gacha rates").withStyle(ChatFormatting.YELLOW)
                .append(Component.literal(" - Affiche les probabilités").withStyle(ChatFormatting.GRAY)), false);
        source.sendSuccess(() -> Component.literal("/gacha setrates <5★%> <4★%> <3★%>").withStyle(ChatFormatting.YELLOW)
                .append(Component.literal(" - Modifie les probabilités").withStyle(ChatFormatting.GRAY)), false);
        source.sendSuccess(() -> Component.literal("/gacha reload").withStyle(ChatFormatting.YELLOW)
                .append(Component.literal(" - Recharge le fichier config").withStyle(ChatFormatting.GRAY)), false);
        source.sendSuccess(() -> Component.literal("/gacha clear").withStyle(ChatFormatting.YELLOW)
                .append(Component.literal(" - Vide toutes les récompenses").withStyle(ChatFormatting.GRAY)), false);
        source.sendSuccess(() -> Component.literal("/gacha info <item>").withStyle(ChatFormatting.YELLOW)
                .append(Component.literal(" - Détails d'une récompense").withStyle(ChatFormatting.GRAY)), false);
        source.sendSuccess(() -> Component.literal("Raretés: three_star, four_star, five_star").withStyle(ChatFormatting.AQUA), false);
        
        return 1;
    }

    private static int listAllRewards(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        GachaManager manager = GachaManager.getInstance();
        List<GachaReward> rewards = manager.getAllRewards();

        if (rewards.isEmpty()) {
            source.sendSuccess(() -> Component.literal("Aucune récompense configurée dans le gacha.")
                    .withStyle(ChatFormatting.YELLOW), false);
            return 0;
        }

        source.sendSuccess(() -> Component.literal("═══ Récompenses Gacha (" + rewards.size() + ") ═══")
                .withStyle(ChatFormatting.GOLD), false);

        // Grouper par rareté
        for (GachaRarity rarity : GachaRarity.values()) {
            List<GachaReward> rarityRewards = manager.getRewardsByRarity(rarity);
            if (!rarityRewards.isEmpty()) {
                source.sendSuccess(() -> Component.literal(rarity.getDisplayStars() + " (" + rarityRewards.size() + ")")
                        .withStyle(rarity.getColor()), false);
                
                for (GachaReward reward : rarityRewards) {
                    source.sendSuccess(() -> formatRewardLine(reward), false);
                }
            }
        }

        return 1;
    }

    private static int listRewardsByRarity(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        String rarityStr = StringArgumentType.getString(context, "rarity").toUpperCase();
        
        GachaRarity rarity;
        try {
            rarity = GachaRarity.valueOf(rarityStr);
        } catch (IllegalArgumentException e) {
            source.sendFailure(Component.literal("Rareté invalide: " + rarityStr + ". Utilisez: three_star, four_star, five_star")
                    .withStyle(ChatFormatting.RED));
            return 0;
        }

        List<GachaReward> rewards = GachaManager.getInstance().getRewardsByRarity(rarity);

        if (rewards.isEmpty()) {
            source.sendSuccess(() -> Component.literal("Aucune récompense " + rarity.getDisplayStars() + " configurée.")
                    .withStyle(ChatFormatting.YELLOW), false);
            return 0;
        }

        source.sendSuccess(() -> Component.literal("═══ " + rarity.getDisplayStars() + " (" + rewards.size() + ") ═══")
                .withStyle(rarity.getColor()), false);

        for (GachaReward reward : rewards) {
            source.sendSuccess(() -> formatRewardLine(reward), false);
        }

        return 1;
    }

    private static Component formatRewardLine(GachaReward reward) {
        return Component.literal("  ")
                .append(Component.literal(reward.getDisplayName()).withStyle(ChatFormatting.WHITE))
                .append(Component.literal(" [").withStyle(ChatFormatting.DARK_GRAY))
                .append(Component.literal(reward.getItemId()).withStyle(ChatFormatting.GRAY))
                .append(Component.literal("] ").withStyle(ChatFormatting.DARK_GRAY))
                .append(Component.literal("w=" + String.format("%.2f", reward.getWeight())).withStyle(ChatFormatting.AQUA));
    }

    private static int addReward(CommandContext<CommandSourceStack> context, double weight) {
        CommandSourceStack source = context.getSource();
        ResourceLocation itemId = ResourceLocationArgument.getId(context, "item");
        String rarityStr = StringArgumentType.getString(context, "rarity").toUpperCase();
        String displayName = StringArgumentType.getString(context, "displayName");

        // Valider la rareté
        GachaRarity rarity;
        try {
            rarity = GachaRarity.valueOf(rarityStr);
        } catch (IllegalArgumentException e) {
            source.sendFailure(Component.literal("Rareté invalide: " + rarityStr)
                    .withStyle(ChatFormatting.RED));
            return 0;
        }

        // Vérifier que l'item existe
        Item item = ForgeRegistries.ITEMS.getValue(itemId);
        if (item == null || item == Items.AIR) {
            source.sendFailure(Component.literal("Item invalide: " + itemId)
                    .withStyle(ChatFormatting.RED));
            return 0;
        }

        // Ajouter la récompense
        boolean success = GachaManager.getInstance().addReward(itemId.toString(), rarity, displayName, weight);

        if (success) {
            source.sendSuccess(() -> Component.literal("✓ Récompense ajoutée: ")
                    .withStyle(ChatFormatting.GREEN)
                    .append(Component.literal(displayName).withStyle(rarity.getColor()))
                    .append(Component.literal(" " + rarity.getDisplayStars()).withStyle(rarity.getColor())), true);
            return 1;
        } else {
            source.sendFailure(Component.literal("Échec de l'ajout. L'item existe peut-être déjà dans le gacha.")
                    .withStyle(ChatFormatting.RED));
            return 0;
        }
    }

    private static int addRewardWithWeight(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        ResourceLocation itemId = ResourceLocationArgument.getId(context, "item");
        String rarityStr = StringArgumentType.getString(context, "rarity").toUpperCase();
        double weight = DoubleArgumentType.getDouble(context, "weight");
        String displayName = StringArgumentType.getString(context, "displayName");

        // Valider la rareté
        GachaRarity rarity;
        try {
            rarity = GachaRarity.valueOf(rarityStr);
        } catch (IllegalArgumentException e) {
            source.sendFailure(Component.literal("Rareté invalide: " + rarityStr)
                    .withStyle(ChatFormatting.RED));
            return 0;
        }

        // Vérifier que l'item existe
        Item item = ForgeRegistries.ITEMS.getValue(itemId);
        if (item == null || item == Items.AIR) {
            source.sendFailure(Component.literal("Item invalide: " + itemId)
                    .withStyle(ChatFormatting.RED));
            return 0;
        }

        // Ajouter la récompense
        boolean success = GachaManager.getInstance().addReward(itemId.toString(), rarity, displayName, weight);

        if (success) {
            source.sendSuccess(() -> Component.literal("✓ Récompense ajoutée: ")
                    .withStyle(ChatFormatting.GREEN)
                    .append(Component.literal(displayName).withStyle(rarity.getColor()))
                    .append(Component.literal(" " + rarity.getDisplayStars()).withStyle(rarity.getColor()))
                    .append(Component.literal(" (poids: " + weight + ")").withStyle(ChatFormatting.AQUA)), true);
            return 1;
        } else {
            source.sendFailure(Component.literal("Échec de l'ajout. L'item existe peut-être déjà dans le gacha.")
                    .withStyle(ChatFormatting.RED));
            return 0;
        }
    }

    private static int removeReward(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        String itemId = StringArgumentType.getString(context, "item");

        Optional<GachaReward> existing = GachaManager.getInstance().findRewardByItemId(itemId);
        if (existing.isEmpty()) {
            source.sendFailure(Component.literal("Récompense non trouvée: " + itemId)
                    .withStyle(ChatFormatting.RED));
            return 0;
        }

        boolean success = GachaManager.getInstance().removeReward(itemId);

        if (success) {
            GachaReward reward = existing.get();
            source.sendSuccess(() -> Component.literal("✓ Récompense retirée: ")
                    .withStyle(ChatFormatting.GREEN)
                    .append(Component.literal(reward.getDisplayName()).withStyle(reward.getRarity().getColor())), true);
            return 1;
        } else {
            source.sendFailure(Component.literal("Échec du retrait de la récompense.")
                    .withStyle(ChatFormatting.RED));
            return 0;
        }
    }

    private static int setWeight(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        String itemId = StringArgumentType.getString(context, "item");
        double weight = DoubleArgumentType.getDouble(context, "weight");

        Optional<GachaReward> existing = GachaManager.getInstance().findRewardByItemId(itemId);
        if (existing.isEmpty()) {
            source.sendFailure(Component.literal("Récompense non trouvée: " + itemId)
                    .withStyle(ChatFormatting.RED));
            return 0;
        }

        boolean success = GachaManager.getInstance().setRewardWeight(itemId, weight);

        if (success) {
            GachaReward reward = existing.get();
            source.sendSuccess(() -> Component.literal("✓ Poids modifié pour ")
                    .withStyle(ChatFormatting.GREEN)
                    .append(Component.literal(reward.getDisplayName()).withStyle(reward.getRarity().getColor()))
                    .append(Component.literal(": " + String.format("%.2f", weight)).withStyle(ChatFormatting.AQUA)), true);
            return 1;
        } else {
            source.sendFailure(Component.literal("Échec de la modification du poids.")
                    .withStyle(ChatFormatting.RED));
            return 0;
        }
    }

    private static int setRarity(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        String itemId = StringArgumentType.getString(context, "item");
        String rarityStr = StringArgumentType.getString(context, "rarity").toUpperCase();

        // Valider la rareté
        GachaRarity rarity;
        try {
            rarity = GachaRarity.valueOf(rarityStr);
        } catch (IllegalArgumentException e) {
            source.sendFailure(Component.literal("Rareté invalide: " + rarityStr)
                    .withStyle(ChatFormatting.RED));
            return 0;
        }

        Optional<GachaReward> existing = GachaManager.getInstance().findRewardByItemId(itemId);
        if (existing.isEmpty()) {
            source.sendFailure(Component.literal("Récompense non trouvée: " + itemId)
                    .withStyle(ChatFormatting.RED));
            return 0;
        }

        boolean success = GachaManager.getInstance().setRewardRarity(itemId, rarity);

        if (success) {
            GachaReward reward = existing.get();
            source.sendSuccess(() -> Component.literal("✓ Rareté modifiée pour ")
                    .withStyle(ChatFormatting.GREEN)
                    .append(Component.literal(reward.getDisplayName()).withStyle(rarity.getColor()))
                    .append(Component.literal(": " + rarity.getDisplayStars()).withStyle(rarity.getColor())), true);
            return 1;
        } else {
            source.sendFailure(Component.literal("Échec de la modification de la rareté.")
                    .withStyle(ChatFormatting.RED));
            return 0;
        }
    }

    private static int showRates(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();

        source.sendSuccess(() -> Component.literal("═══ Probabilités Gacha ═══").withStyle(ChatFormatting.GOLD), false);
        source.sendSuccess(() -> Component.literal("★★★★★ 5-Star: ")
                .withStyle(GachaRarity.FIVE_STAR.getColor())
                .append(Component.literal(String.format("%.2f%%", GachaManager.getFiveStarRate())).withStyle(ChatFormatting.WHITE)), false);
        source.sendSuccess(() -> Component.literal("★★★★ 4-Star: ")
                .withStyle(GachaRarity.FOUR_STAR.getColor())
                .append(Component.literal(String.format("%.2f%%", GachaManager.getFourStarRate())).withStyle(ChatFormatting.WHITE)), false);
        source.sendSuccess(() -> Component.literal("★★★ 3-Star: ")
                .withStyle(GachaRarity.THREE_STAR.getColor())
                .append(Component.literal(String.format("%.2f%%", GachaManager.getThreeStarRate())).withStyle(ChatFormatting.WHITE)), false);

        double total = GachaManager.getFiveStarRate() + GachaManager.getFourStarRate() + GachaManager.getThreeStarRate();
        if (Math.abs(total - 100.0) > 0.01) {
            source.sendSuccess(() -> Component.literal("⚠ Attention: Total = " + String.format("%.2f%%", total) + " (devrait être 100%)")
                    .withStyle(ChatFormatting.YELLOW), false);
        }

        return 1;
    }

    private static int setRates(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        double fiveStar = DoubleArgumentType.getDouble(context, "fiveStar");
        double fourStar = DoubleArgumentType.getDouble(context, "fourStar");
        double threeStar = DoubleArgumentType.getDouble(context, "threeStar");

        double total = fiveStar + fourStar + threeStar;
        if (Math.abs(total - 100.0) > 0.01) {
            source.sendFailure(Component.literal("Les probabilités doivent totaliser 100%. Total actuel: " + String.format("%.2f%%", total))
                    .withStyle(ChatFormatting.RED));
            return 0;
        }

        GachaManager.getInstance().setRarityRates(fiveStar, fourStar, threeStar);

        source.sendSuccess(() -> Component.literal("✓ Probabilités mises à jour: ")
                .withStyle(ChatFormatting.GREEN)
                .append(Component.literal("5★=" + fiveStar + "% ").withStyle(GachaRarity.FIVE_STAR.getColor()))
                .append(Component.literal("4★=" + fourStar + "% ").withStyle(GachaRarity.FOUR_STAR.getColor()))
                .append(Component.literal("3★=" + threeStar + "%").withStyle(GachaRarity.THREE_STAR.getColor())), true);

        return 1;
    }

    private static int reloadConfig(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();

        GachaManager.getInstance().reload();
        int count = GachaManager.getInstance().getRewardCount();

        source.sendSuccess(() -> Component.literal("✓ Configuration gacha rechargée: " + count + " récompenses")
                .withStyle(ChatFormatting.GREEN), true);

        return 1;
    }

    private static int clearAllRewards(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();

        int removedCount = GachaManager.getInstance().clearAllRewards();

        source.sendSuccess(() -> Component.literal("✓ Gacha vidé: " + removedCount + " récompenses supprimées")
                .withStyle(ChatFormatting.GREEN), true);

        return 1;
    }

    private static int showItemInfo(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        String itemId = StringArgumentType.getString(context, "item");

        Optional<GachaReward> optReward = GachaManager.getInstance().findRewardByItemId(itemId);
        if (optReward.isEmpty()) {
            source.sendFailure(Component.literal("Récompense non trouvée: " + itemId)
                    .withStyle(ChatFormatting.RED));
            return 0;
        }

        GachaReward reward = optReward.get();

        source.sendSuccess(() -> Component.literal("═══ Détails Récompense ═══").withStyle(ChatFormatting.GOLD), false);
        source.sendSuccess(() -> Component.literal("Nom: ").withStyle(ChatFormatting.GRAY)
                .append(Component.literal(reward.getDisplayName()).withStyle(reward.getRarity().getColor())), false);
        source.sendSuccess(() -> Component.literal("Item ID: ").withStyle(ChatFormatting.GRAY)
                .append(Component.literal(reward.getItemId()).withStyle(ChatFormatting.WHITE)), false);
        source.sendSuccess(() -> Component.literal("Rareté: ").withStyle(ChatFormatting.GRAY)
                .append(Component.literal(reward.getRarity().getDisplayStars()).withStyle(reward.getRarity().getColor())), false);
        source.sendSuccess(() -> Component.literal("Poids: ").withStyle(ChatFormatting.GRAY)
                .append(Component.literal(String.format("%.2f", reward.getWeight())).withStyle(ChatFormatting.AQUA)), false);
        source.sendSuccess(() -> Component.literal("Valide: ").withStyle(ChatFormatting.GRAY)
                .append(Component.literal(reward.isValid() ? "✓ Oui" : "✗ Non")
                        .withStyle(reward.isValid() ? ChatFormatting.GREEN : ChatFormatting.RED)), false);

        // Calculer la probabilité effective
        GachaRarity rarity = reward.getRarity();
        double rarityRate = switch (rarity) {
            case FIVE_STAR -> GachaManager.getFiveStarRate();
            case FOUR_STAR -> GachaManager.getFourStarRate();
            case THREE_STAR -> GachaManager.getThreeStarRate();
        };
        
        List<GachaReward> sameRarity = GachaManager.getInstance().getRewardsByRarity(rarity);
        double totalWeight = sameRarity.stream().mapToDouble(GachaReward::getWeight).sum();
        double effectiveChance = (rarityRate / 100.0) * (reward.getWeight() / totalWeight) * 100.0;

        source.sendSuccess(() -> Component.literal("Probabilité effective: ").withStyle(ChatFormatting.GRAY)
                .append(Component.literal(String.format("%.4f%%", effectiveChance)).withStyle(ChatFormatting.YELLOW)), false);

        return 1;
    }
}
