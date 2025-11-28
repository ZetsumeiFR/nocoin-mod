package com.zetsumei.nocoin.block;

import com.zetsumei.nocoin.block.entity.LeaderboardBlockEntity;
import com.zetsumei.nocoin.block.entity.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import javax.annotation.Nullable;

/**
 * Bloc Leaderboard - Affiche le classement des joueurs en 3D dans le monde.
 * Parfait pour être placé au spawn pour que les joueurs puissent admirer le classement.
 * 
 * Caractéristiques:
 * - Affiche un panneau holographique avec le top 10 des joueurs
 * - Se met à jour automatiquement toutes les 5 secondes
 * - Animation de particules dorées pour le style
 * - Interaction pour ouvrir l'écran détaillé du leaderboard
 */
public class LeaderboardBlock extends BaseEntityBlock {

    public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;

    // Forme du bloc: panneau vertical sur un piédestal
    private static final VoxelShape SHAPE_NORTH_SOUTH = Shapes.or(
            // Piédestal (base)
            Block.box(2, 0, 2, 14, 2, 14),
            // Colonne
            Block.box(6, 2, 6, 10, 8, 10),
            // Panneau d'affichage (plus large que haut)
            Block.box(0, 8, 6, 16, 24, 10)
    );

    private static final VoxelShape SHAPE_EAST_WEST = Shapes.or(
            // Piédestal (base)
            Block.box(2, 0, 2, 14, 2, 14),
            // Colonne
            Block.box(6, 2, 6, 10, 8, 10),
            // Panneau d'affichage (tourné)
            Block.box(6, 8, 0, 10, 24, 16)
    );

    public LeaderboardBlock(Properties properties) {
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
        Direction facing = state.getValue(FACING);
        return (facing == Direction.NORTH || facing == Direction.SOUTH) ? SHAPE_NORTH_SOUTH : SHAPE_EAST_WEST;
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        // ENTITYBLOCK_ANIMATED permet au BlockEntityRenderer de tout dessiner
        return RenderShape.MODEL;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new LeaderboardBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (!level.isClientSide) {
            return createTickerHelper(type, ModBlockEntities.LEADERBOARD.get(), LeaderboardBlockEntity::serverTick);
        }
        return createTickerHelper(type, ModBlockEntities.LEADERBOARD.get(), LeaderboardBlockEntity::clientTick);
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }

        if (hand != InteractionHand.MAIN_HAND) {
            return InteractionResult.PASS;
        }

        // Ouvrir l'écran du leaderboard détaillé
        if (player instanceof ServerPlayer serverPlayer) {
            // Utiliser le système existant pour ouvrir l'écran du leaderboard
            com.zetsumei.nocoin.network.NocoinNetworkHandler.requestLeaderboardForPlayer(serverPlayer);
        }

        return InteractionResult.CONSUME;
    }

    @Override
    public int getLightEmission(BlockState state, BlockGetter level, BlockPos pos) {
        // Le leaderboard émet une lumière dorée ambiante
        return 10;
    }

    @Override
    public boolean hasAnalogOutputSignal(BlockState state) {
        return false;
    }
}
