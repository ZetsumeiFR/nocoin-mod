package com.zetsumei.nocoin.block;

import com.zetsumei.nocoin.block.entity.ModBlockEntities;
import com.zetsumei.nocoin.block.entity.PlayerShopBlockEntity;
import com.zetsumei.nocoin.network.NocoinNetworkHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import javax.annotation.Nullable;

/**
 * Bloc magasin joueur - Permet aux joueurs de créer leurs propres boutiques.
 * Le propriétaire peut définir des offres d'achat et de vente.
 * Les autres joueurs peuvent acheter/vendre selon les offres disponibles.
 */
public class PlayerShopBlock extends BaseEntityBlock {

    public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;

    // Forme du bloc (stand de marché stylisé)
    private static final VoxelShape SHAPE = Shapes.or(
            // Base / Comptoir
            Block.box(0, 0, 0, 16, 12, 16),
            // Présentoir arrière
            Block.box(0, 12, 12, 16, 20, 16),
            // Petites étagères
            Block.box(1, 12, 1, 5, 14, 11),
            Block.box(11, 12, 1, 15, 14, 11)
    );

    public PlayerShopBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return this.defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new PlayerShopBlockEntity(pos, state);
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);

        if (!level.isClientSide && placer instanceof Player player) {
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof PlayerShopBlockEntity shopEntity) {
                shopEntity.setOwner(player);
            }
        }
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }

        if (hand != InteractionHand.MAIN_HAND) {
            return InteractionResult.PASS;
        }

        BlockEntity be = level.getBlockEntity(pos);
        if (!(be instanceof PlayerShopBlockEntity shopEntity)) {
            return InteractionResult.PASS;
        }

        if (player instanceof ServerPlayer serverPlayer) {
            boolean isOwner = shopEntity.isOwner(player);

            if (isOwner) {
                // Ouvrir l'interface de configuration du magasin
                NocoinNetworkHandler.sendOpenPlayerShopOwnerScreen(serverPlayer, pos, shopEntity);
            } else {
                // Ouvrir l'interface d'achat/vente pour les visiteurs
                NocoinNetworkHandler.sendOpenPlayerShopCustomerScreen(serverPlayer, pos, shopEntity);
            }
        }

        return InteractionResult.CONSUME;
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
        if (!state.is(newState.getBlock())) {
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof PlayerShopBlockEntity shopEntity) {
                // Optionnel: rendre les items stockés au propriétaire ou les drop
                // Pour l'instant, on perd simplement les offres
            }
        }
        super.onRemove(state, level, pos, newState, isMoving);
    }

    @Override
    public boolean hasAnalogOutputSignal(BlockState state) {
        return true;
    }

    @Override
    public int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos) {
        BlockEntity be = level.getBlockEntity(pos);
        if (be instanceof PlayerShopBlockEntity shopEntity) {
            // Signal proportionnel au nombre d'offres actives
            int activeOffers = shopEntity.getActiveOffers().size();
            return Math.min(15, activeOffers * 2);
        }
        return 0;
    }

    @Override
    public int getLightEmission(BlockState state, BlockGetter level, BlockPos pos) {
        return 5; // Légère lumière pour indiquer un magasin actif
    }
}
