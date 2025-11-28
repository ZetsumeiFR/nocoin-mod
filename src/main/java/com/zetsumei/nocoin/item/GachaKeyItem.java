package com.zetsumei.nocoin.item;

import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

/**
 * Cle Gacha - Item qui permet d'acceder a la machine Gacha.
 * S'achete aupres du PNJ Vendeur Gacha contre des NOCOIN.
 */
public class GachaKeyItem extends Item {

    public GachaKeyItem(Properties properties) {
        super(properties);
    }

    @Override
    public void appendHoverText(
        ItemStack stack,
        @Nullable Level level,
        List<Component> tooltipComponents,
        TooltipFlag isAdvanced
    ) {
        super.appendHoverText(stack, level, tooltipComponents, isAdvanced);

        tooltipComponents.add(
            Component.literal("Utilisez dans la Machine Gacha").withStyle(
                ChatFormatting.GRAY
            )
        );
        tooltipComponents.add(
            Component.literal("Tentez votre chance au Gacha !").withStyle(
                ChatFormatting.GOLD
            )
        );
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        // Effet brillant pour indiquer que c'est un item spécial
        return true;
    }
}
