package com.zetsumei.nocoin.block;

import com.zetsumei.nocoin.block.entity.GachaMachineBlockEntity;
import com.zetsumei.nocoin.item.ModItems;
import com.zetsumei.nocoin.network.NocoinNetworkHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import javax.annotation.Nullable;

/**
 * Machine à Gacha - Bloc interactif pour effectuer des tirages.
 * Le joueur doit avoir une Clé Gacha pour utiliser la machine.
 * Chaque machine possède son propre catalogue de récompenses indépendant.
 *
 * C'est un bloc de 2 blocs de hauteur (comme une porte ou un lit).
 */
public class GachaMachineBlock extends BaseEntityBlock {

    public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;
    public static final EnumProperty<DoubleBlockHalf> HALF = BlockStateProperties.DOUBLE_BLOCK_HALF;

    // Forme de la partie inférieure
    private static final VoxelShape SHAPE_LOWER = Shapes.or(
            // Pied
            Block.box(1, 0, 1, 15, 1, 15),
            // Corps principal inférieur
            Block.box(1, 1, 1, 15, 16, 15)
    );

    // Forme de la partie supérieure
    private static final VoxelShape SHAPE_UPPER = Shapes.or(
            // Corps principal supérieur
            Block.box(1, 0, 0, 15, 13, 15),
            // Couvercle
            Block.box(0, 13, 0, 16, 16, 15)
    );

    public GachaMachineBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any()
                .setValue(FACING, Direction.NORTH)
                .setValue(HALF, DoubleBlockHalf.LOWER));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, HALF);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockPos pos = context.getClickedPos();
        Level level = context.getLevel();

        // Vérifier qu'on peut placer le bloc supérieur
        if (pos.getY() < level.getMaxBuildHeight() - 1 && level.getBlockState(pos.above()).canBeReplaced(context)) {
            return this.defaultBlockState()
                    .setValue(FACING, context.getHorizontalDirection().getOpposite())
                    .setValue(HALF, DoubleBlockHalf.LOWER);
        }
        return null;
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        if (!level.isClientSide()) {
            // Placer la partie supérieure
            level.setBlock(pos.above(), state.setValue(HALF, DoubleBlockHalf.UPPER), 3);
        }
        super.setPlacedBy(level, pos, state, placer, stack);
    }

    @Override
    public boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        if (state.getValue(HALF) == DoubleBlockHalf.UPPER) {
            BlockState below = level.getBlockState(pos.below());
            return below.is(this) && below.getValue(HALF) == DoubleBlockHalf.LOWER;
        }
        return level.getBlockState(pos.above()).canBeReplaced();
    }

    @Override
    public BlockState updateShape(BlockState state, Direction direction, BlockState neighborState, LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
        DoubleBlockHalf half = state.getValue(HALF);

        if (direction.getAxis() == Direction.Axis.Y) {
            boolean valid = true;

            if (half == DoubleBlockHalf.LOWER && direction == Direction.UP) {
                if (!neighborState.is(this) || neighborState.getValue(HALF) != DoubleBlockHalf.UPPER) {
                    valid = false;
                }
            }
            if (half == DoubleBlockHalf.UPPER && direction == Direction.DOWN) {
                if (!neighborState.is(this) || neighborState.getValue(HALF) != DoubleBlockHalf.LOWER) {
                    valid = false;
                }
            }

            if (!valid) {
                return Blocks.AIR.defaultBlockState();
            }
        }

        return super.updateShape(state, direction, neighborState, level, pos, neighborPos);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return state.getValue(HALF) == DoubleBlockHalf.LOWER ? SHAPE_LOWER : SHAPE_UPPER;
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        // Seulement créer un BlockEntity pour la partie inférieure
        if (state.getValue(HALF) == DoubleBlockHalf.LOWER) {
            return new GachaMachineBlockEntity(pos, state);
        }
        return null;
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        // Toujours rediriger vers la partie inférieure
        BlockPos lowerPos = state.getValue(HALF) == DoubleBlockHalf.UPPER ? pos.below() : pos;

        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }

        if (hand != InteractionHand.MAIN_HAND) {
            return InteractionResult.PASS;
        }

        if (player instanceof ServerPlayer serverPlayer) {
            BlockEntity be = level.getBlockEntity(lowerPos);
            if (!(be instanceof GachaMachineBlockEntity gachaBE)) {
                return InteractionResult.FAIL;
            }

            // Shift+clic droit avec permission admin → ouvre l'écran d'administration
            if (player.isShiftKeyDown() && serverPlayer.hasPermissions(2)) {
                NocoinNetworkHandler.sendOpenGachaAdminScreen(serverPlayer, lowerPos);
                return InteractionResult.CONSUME;
            }

            // Comportement normal : ouvrir l'interface de la machine
            boolean hasKey = hasGachaKey(player);
            NocoinNetworkHandler.sendOpenGachaMachineScreen(serverPlayer, lowerPos, hasKey, countGachaKeys(player));
        }

        return InteractionResult.CONSUME;
    }

    /**
     * Vérifie si le joueur possède au moins une Clé Gacha.
     */
    private boolean hasGachaKey(Player player) {
        return countGachaKeys(player) > 0;
    }

    /**
     * Compte le nombre de Clés Gacha dans l'inventaire du joueur.
     */
    private int countGachaKeys(Player player) {
        int count = 0;
        for (ItemStack stack : player.getInventory().items) {
            if (stack.is(ModItems.GACHA_KEY.get())) {
                count += stack.getCount();
            }
        }
        return count;
    }

    @Override
    public boolean hasAnalogOutputSignal(BlockState state) {
        return state.getValue(HALF) == DoubleBlockHalf.LOWER;
    }

    @Override
    public int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos) {
        if (state.getValue(HALF) == DoubleBlockHalf.UPPER) {
            return 0;
        }
        // Peut être étendu pour retourner un signal basé sur le contenu
        return 0;
    }

    @Override
    public int getLightEmission(BlockState state, BlockGetter level, BlockPos pos) {
        // La machine émet une légère lumière
        return 7;
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
        if (state.getValue(HALF) == DoubleBlockHalf.UPPER) {
            // Ne pas traiter la suppression du BlockEntity ici
            return;
        }

        if (!state.is(newState.getBlock())) {
            // Le bloc est détruit, le BlockEntity et son catalogue seront perdus
            super.onRemove(state, level, pos, newState, isMoving);
        }
    }

    @Override
    public void playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        if (!level.isClientSide && (player.isCreative() || !player.hasCorrectToolForDrops(state))) {
            preventCreativeDropFromBottomPart(level, pos, state, player);
        }
        super.playerWillDestroy(level, pos, state, player);
    }

    /**
     * Empêche le drop de l'item quand on casse la partie supérieure en créatif.
     */
    protected static void preventCreativeDropFromBottomPart(Level level, BlockPos pos, BlockState state, Player player) {
        DoubleBlockHalf half = state.getValue(HALF);
        if (half == DoubleBlockHalf.UPPER) {
            BlockPos belowPos = pos.below();
            BlockState belowState = level.getBlockState(belowPos);
            if (belowState.is(state.getBlock()) && belowState.getValue(HALF) == DoubleBlockHalf.LOWER) {
                BlockState newState = belowState.getFluidState().is(Fluids.WATER)
                        ? Blocks.WATER.defaultBlockState()
                        : Blocks.AIR.defaultBlockState();
                level.setBlock(belowPos, newState, 35);
                level.levelEvent(player, 2001, belowPos, Block.getId(belowState));
            }
        }
    }

    @Override
    public long getSeed(BlockState state, BlockPos pos) {
        // Utiliser la position de la partie inférieure pour la seed
        return state.getValue(HALF) == DoubleBlockHalf.LOWER
                ? super.getSeed(state, pos)
                : super.getSeed(state, pos.below());
    }
}
