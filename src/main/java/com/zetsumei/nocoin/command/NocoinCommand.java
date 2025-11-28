package com.zetsumei.nocoin.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.LongArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.zetsumei.nocoin.capability.NocoinCapabilityProvider;
import com.zetsumei.nocoin.network.NocoinNetworkHandler;
import com.zetsumei.nocoin.shop.ShopItem;
import com.zetsumei.nocoin.shop.ShopManager;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerPlayer;

import java.util.List;

/**
 * Commandes pour gérer les NOCOIN.
 * /nocoin balance - Affiche son solde
 * /nocoin balance <joueur> - Affiche le solde d'un joueur (op)
 * /nocoin pay <joueur> <montant> - Transfère des NOCOIN à un joueur
 * /nocoin add <joueur> <montant> - Ajoute des NOCOIN (op)
 * /nocoin remove <joueur> <montant> - Retire des NOCOIN (op)
 * /nocoin set <joueur> <montant> - Définit le solde (op)
 * /nocoin shop - Ouvre la boutique (envoie les données au client)
 * /nocoin shop list - Affiche la liste des articles disponibles
 * /nocoin shop buy <id> - Achète un article par son ID
 * /nocoin shop admin add <item_id> <prix> [nom] - Ajoute un article (op)
 * /nocoin shop admin remove <id> - Supprime un article (op)
 * /nocoin shop admin modify <id> price <nouveau_prix> - Modifie le prix (op)
 * /nocoin shop admin modify <id> name <nouveau_nom> - Modifie le nom (op)
 * /nocoin shop admin reload - Recharge la boutique depuis le fichier (op)
 * /nocoin shop admin clear - Vide la boutique (op)
 */
public class NocoinCommand {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
            Commands.literal("nocoin")
                // /nocoin balance
                .then(Commands.literal("balance")
                    .executes(NocoinCommand::showOwnBalance)
                    // /nocoin balance <joueur> (op)
                    .then(Commands.argument("target", EntityArgument.player())
                        .requires(source -> source.hasPermission(2))
                        .executes(NocoinCommand::showTargetBalance)
                    )
                )
                // /nocoin pay <joueur> <montant>
                .then(Commands.literal("pay")
                    .then(Commands.argument("target", EntityArgument.player())
                        .then(Commands.argument("amount", LongArgumentType.longArg(1))
                            .executes(NocoinCommand::payPlayer)
                        )
                    )
                )
                // /nocoin add <joueur> <montant> (op)
                .then(Commands.literal("add")
                    .requires(source -> source.hasPermission(2))
                    .then(Commands.argument("target", EntityArgument.player())
                        .then(Commands.argument("amount", LongArgumentType.longArg(1))
                            .executes(NocoinCommand::addBalance)
                        )
                    )
                )
                // /nocoin remove <joueur> <montant> (op)
                .then(Commands.literal("remove")
                    .requires(source -> source.hasPermission(2))
                    .then(Commands.argument("target", EntityArgument.player())
                        .then(Commands.argument("amount", LongArgumentType.longArg(1))
                            .executes(NocoinCommand::removeBalance)
                        )
                    )
                )
                // /nocoin set <joueur> <montant> (op)
                .then(Commands.literal("set")
                    .requires(source -> source.hasPermission(2))
                    .then(Commands.argument("target", EntityArgument.player())
                        .then(Commands.argument("amount", LongArgumentType.longArg(0))
                            .executes(NocoinCommand::setBalance)
                        )
                    )
                )
                // /nocoin shop - Ouvre la boutique
                .then(Commands.literal("shop")
                    .executes(NocoinCommand::openShop)
                    // /nocoin shop list - Liste les articles
                    .then(Commands.literal("list")
                        .executes(NocoinCommand::listShopItems)
                    )
                    // /nocoin shop buy <id> - Achète un article
                    .then(Commands.literal("buy")
                        .then(Commands.argument("itemId", IntegerArgumentType.integer(0))
                            .executes(NocoinCommand::buyShopItem)
                        )
                    )
                    // /nocoin shop admin - Commandes d'administration de la boutique
                    .then(Commands.literal("admin")
                        .requires(source -> source.hasPermission(2))
                        // /nocoin shop admin add <item_id> <prix> [nom]
                        .then(Commands.literal("add")
                            .then(Commands.argument("item_id", StringArgumentType.word())
                                .then(Commands.argument("price", LongArgumentType.longArg(1))
                                    .executes(NocoinCommand::shopAdminAdd)
                                    .then(Commands.argument("display_name", StringArgumentType.greedyString())
                                        .executes(NocoinCommand::shopAdminAddWithName)
                                    )
                                )
                            )
                        )
                        // /nocoin shop admin remove <id>
                        .then(Commands.literal("remove")
                            .then(Commands.argument("shop_item_id", IntegerArgumentType.integer(0))
                                .executes(NocoinCommand::shopAdminRemove)
                            )
                        )
                        // /nocoin shop admin modify <id> price/quantity/name <valeur>
                        .then(Commands.literal("modify")
                            .then(Commands.argument("shop_item_id", IntegerArgumentType.integer(0))
                                .then(Commands.literal("price")
                                    .then(Commands.argument("new_price", LongArgumentType.longArg(1))
                                        .executes(NocoinCommand::shopAdminModifyPrice)
                                    )
                                )
                                
                                .then(Commands.literal("name")
                                    .then(Commands.argument("new_name", StringArgumentType.greedyString())
                                        .executes(NocoinCommand::shopAdminModifyName)
                                    )
                                )
                            )
                        )
                        // /nocoin shop admin reload
                        .then(Commands.literal("reload")
                            .executes(NocoinCommand::shopAdminReload)
                        )
                        // /nocoin shop admin clear
                        .then(Commands.literal("clear")
                            .executes(NocoinCommand::shopAdminClear)
                        )
                    )
                )
                // /nocoin (alias pour balance)
                .executes(NocoinCommand::showOwnBalance)
        );
    }

    private static int showOwnBalance(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();

        player.getCapability(NocoinCapabilityProvider.NOCOIN_CAPABILITY).ifPresent(cap -> {
            player.sendSystemMessage(
                Component.literal("Votre solde: ")
                    .withStyle(ChatFormatting.YELLOW)
                    .append(Component.literal(String.valueOf(cap.getBalance()) + " NOCOIN")
                        .withStyle(ChatFormatting.GOLD))
            );
        });

        return 1;
    }

    private static int showTargetBalance(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer target = EntityArgument.getPlayer(context, "target");

        target.getCapability(NocoinCapabilityProvider.NOCOIN_CAPABILITY).ifPresent(cap -> {
            context.getSource().sendSuccess(() ->
                Component.literal("Solde de " + target.getName().getString() + ": ")
                    .withStyle(ChatFormatting.YELLOW)
                    .append(Component.literal(String.valueOf(cap.getBalance()) + " NOCOIN")
                        .withStyle(ChatFormatting.GOLD)),
                false
            );
        });

        return 1;
    }

    private static int payPlayer(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer sender = context.getSource().getPlayerOrException();
        ServerPlayer target = EntityArgument.getPlayer(context, "target");
        long amount = LongArgumentType.getLong(context, "amount");

        if (sender.equals(target)) {
            sender.sendSystemMessage(
                Component.literal("Vous ne pouvez pas vous payer vous-même!")
                    .withStyle(ChatFormatting.RED)
            );
            return 0;
        }

        final boolean[] success = {false};

        sender.getCapability(NocoinCapabilityProvider.NOCOIN_CAPABILITY).ifPresent(senderCap -> {
            if (!senderCap.hasEnough(amount)) {
                sender.sendSystemMessage(
                    Component.literal("Solde insuffisant!")
                        .withStyle(ChatFormatting.RED)
                );
                return;
            }

            target.getCapability(NocoinCapabilityProvider.NOCOIN_CAPABILITY).ifPresent(targetCap -> {
                senderCap.removeBalance(amount);
                targetCap.addBalance(amount);

                sender.sendSystemMessage(
                    Component.literal("Vous avez envoyé " + amount + " NOCOIN à " + target.getName().getString())
                        .withStyle(ChatFormatting.GREEN)
                );

                target.sendSystemMessage(
                    Component.literal("Vous avez reçu " + amount + " NOCOIN de " + sender.getName().getString())
                        .withStyle(ChatFormatting.GREEN)
                );

                success[0] = true;
            });
        });

        return success[0] ? 1 : 0;
    }

    private static int addBalance(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer target = EntityArgument.getPlayer(context, "target");
        long amount = LongArgumentType.getLong(context, "amount");

        target.getCapability(NocoinCapabilityProvider.NOCOIN_CAPABILITY).ifPresent(cap -> {
            cap.addBalance(amount);

            context.getSource().sendSuccess(() ->
                Component.literal("Ajouté " + amount + " NOCOIN à " + target.getName().getString())
                    .withStyle(ChatFormatting.GREEN),
                true
            );

            target.sendSystemMessage(
                Component.literal("+" + amount + " NOCOIN (admin)")
                    .withStyle(ChatFormatting.GOLD)
            );
        });

        return 1;
    }

    private static int removeBalance(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer target = EntityArgument.getPlayer(context, "target");
        long amount = LongArgumentType.getLong(context, "amount");

        target.getCapability(NocoinCapabilityProvider.NOCOIN_CAPABILITY).ifPresent(cap -> {
            boolean removed = cap.removeBalance(amount);

            if (removed) {
                context.getSource().sendSuccess(() ->
                    Component.literal("Retiré " + amount + " NOCOIN de " + target.getName().getString())
                        .withStyle(ChatFormatting.GREEN),
                    true
                );
            } else {
                context.getSource().sendFailure(
                    Component.literal("Solde insuffisant pour retirer " + amount + " NOCOIN")
                        .withStyle(ChatFormatting.RED)
                );
            }
        });

        return 1;
    }

    private static int setBalance(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer target = EntityArgument.getPlayer(context, "target");
        long amount = LongArgumentType.getLong(context, "amount");

        target.getCapability(NocoinCapabilityProvider.NOCOIN_CAPABILITY).ifPresent(cap -> {
            cap.setBalance(amount);

            context.getSource().sendSuccess(() ->
                Component.literal("Solde de " + target.getName().getString() + " défini à " + amount + " NOCOIN")
                    .withStyle(ChatFormatting.GREEN),
                true
            );
        });

        return 1;
    }

    private static int openShop(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();

        // Envoyer les données de la boutique au client pour ouvrir l'UI
        NocoinNetworkHandler.sendShopItemsToClient(player);

        player.sendSystemMessage(
            Component.literal("Ouverture de la boutique...")
                .withStyle(ChatFormatting.GOLD)
        );

        return 1;
    }

    private static int listShopItems(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        List<ShopItem> items = ShopManager.getInstance().getItems();

        if (items.isEmpty()) {
            player.sendSystemMessage(
                Component.literal("La boutique est vide.")
                    .withStyle(ChatFormatting.YELLOW)
            );
            return 0;
        }

        player.sendSystemMessage(
            Component.literal("=== Boutique NOCOIN ===")
                .withStyle(ChatFormatting.GOLD)
        );

        for (ShopItem item : items) {
            MutableComponent line = Component.literal("  [" + item.getId() + "] ")
                    .withStyle(ChatFormatting.GRAY)
                    .append(item.getDisplayComponent().copy().withStyle(ChatFormatting.WHITE));
            if (item.getQuantity() > 1) {
                line = line.append(Component.literal(" x" + item.getQuantity()).withStyle(ChatFormatting.GRAY));
            }
            line = line.append(Component.literal(" - ").withStyle(ChatFormatting.DARK_GRAY))
                    .append(Component.literal(String.format("%,d", item.getPrice()) + " NC").withStyle(ChatFormatting.GOLD));
            player.sendSystemMessage(line);
        }

        player.sendSystemMessage(
            Component.literal("Utilisez /nocoin shop buy <id> pour acheter")
                .withStyle(ChatFormatting.GRAY)
        );

        return 1;
    }

    private static int buyShopItem(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        int itemId = IntegerArgumentType.getInteger(context, "itemId");

        ShopManager.PurchaseResult result = ShopManager.getInstance().processPurchase(player, itemId);

        if (result.isSuccess()) {
            player.sendSystemMessage(result.getMessageComponent().copy().withStyle(ChatFormatting.GREEN));
        } else {
            player.sendSystemMessage(result.getMessageComponent().copy().withStyle(ChatFormatting.RED));
        }

        return result.isSuccess() ? 1 : 0;
    }

    // ============== Commandes Admin de la Boutique ==============

    private static int shopAdminAdd(CommandContext<CommandSourceStack> context) {
        return shopAdminAddInternal(context, null);
    }

    private static int shopAdminAddWithName(CommandContext<CommandSourceStack> context) {
        String displayName = StringArgumentType.getString(context, "display_name");
        return shopAdminAddInternal(context, displayName);
    }

    private static int shopAdminAddInternal(CommandContext<CommandSourceStack> context, String displayName) {
        String itemId = StringArgumentType.getString(context, "item_id");
        long price = LongArgumentType.getLong(context, "price");

        ShopManager.AddItemResult result = ShopManager.getInstance().addItem(itemId, price, displayName);

        if (result.isSuccess()) {
            ShopItem item = result.getAddedItem();
            context.getSource().sendSuccess(() ->
                Component.literal("Article ajouté à la boutique:")
                    .withStyle(ChatFormatting.GREEN)
                    .append(Component.literal("\n  ID: " + item.getId()).withStyle(ChatFormatting.GRAY))
                    .append(Component.literal("\n  Item: " + item.getItemId()).withStyle(ChatFormatting.WHITE))
                    .append(Component.literal("\n  Prix: " + String.format("%,d", item.getPrice()) + " NC").withStyle(ChatFormatting.GOLD)),
                true
            );
            return 1;
        } else {
            context.getSource().sendFailure(
                Component.literal("Erreur: " + result.getErrorMessage())
                    .withStyle(ChatFormatting.RED)
            );
            return 0;
        }
    }

    private static int shopAdminRemove(CommandContext<CommandSourceStack> context) {
        int shopItemId = IntegerArgumentType.getInteger(context, "shop_item_id");

        ShopManager.RemoveItemResult result = ShopManager.getInstance().removeItem(shopItemId);

        if (result.isSuccess()) {
            ShopItem item = result.getRemovedItem();
            context.getSource().sendSuccess(() ->
                Component.literal("Article supprimé de la boutique: ")
                    .withStyle(ChatFormatting.GREEN)
                    .append(item.getDisplayComponent().copy().withStyle(ChatFormatting.WHITE)),
                true
            );
            return 1;
        } else {
            context.getSource().sendFailure(
                Component.literal("Erreur: " + result.getErrorMessage())
                    .withStyle(ChatFormatting.RED)
            );
            return 0;
        }
    }

    private static int shopAdminModifyPrice(CommandContext<CommandSourceStack> context) {
        int shopItemId = IntegerArgumentType.getInteger(context, "shop_item_id");
        long newPrice = LongArgumentType.getLong(context, "new_price");

        ShopManager.ModifyItemResult result = ShopManager.getInstance().modifyItem(shopItemId, newPrice, null);

        if (result.isSuccess()) {
            ShopItem item = result.getModifiedItem();
            context.getSource().sendSuccess(() ->
                Component.literal("Prix modifié pour ")
                    .withStyle(ChatFormatting.GREEN)
                    .append(item.getDisplayComponent().copy().withStyle(ChatFormatting.WHITE))
                    .append(Component.literal(": " + String.format("%,d", item.getPrice()) + " NC").withStyle(ChatFormatting.GOLD)),
                true
            );
            return 1;
        } else {
            context.getSource().sendFailure(
                Component.literal("Erreur: " + result.getErrorMessage())
                    .withStyle(ChatFormatting.RED)
            );
            return 0;
        }
    }

    

    private static int shopAdminModifyName(CommandContext<CommandSourceStack> context) {
        int shopItemId = IntegerArgumentType.getInteger(context, "shop_item_id");
        String newName = StringArgumentType.getString(context, "new_name");

        ShopManager.ModifyItemResult result = ShopManager.getInstance().modifyItem(shopItemId, null, newName);

        if (result.isSuccess()) {
            ShopItem item = result.getModifiedItem();
            context.getSource().sendSuccess(() ->
                Component.literal("Nom modifié pour l'article #" + shopItemId + ": ")
                    .withStyle(ChatFormatting.GREEN)
                    .append(item.getDisplayComponent().copy().withStyle(ChatFormatting.WHITE)),
                true
            );
            return 1;
        } else {
            context.getSource().sendFailure(
                Component.literal("Erreur: " + result.getErrorMessage())
                    .withStyle(ChatFormatting.RED)
            );
            return 0;
        }
    }

    private static int shopAdminReload(CommandContext<CommandSourceStack> context) {
        ShopManager.getInstance().reloadFromFile();
        int itemCount = ShopManager.getInstance().getItems().size();

        context.getSource().sendSuccess(() ->
            Component.literal("Boutique rechargée: " + itemCount + " article(s)")
                .withStyle(ChatFormatting.GREEN),
            true
        );

        return 1;
    }

    private static int shopAdminClear(CommandContext<CommandSourceStack> context) {
        int removedCount = ShopManager.getInstance().clearShop();

        context.getSource().sendSuccess(() ->
            Component.literal("Boutique vidée: " + removedCount + " article(s) supprimé(s)")
                .withStyle(ChatFormatting.YELLOW),
            true
        );

        return 1;
    }
}
