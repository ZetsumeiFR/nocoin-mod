package com.zetsumei.nocoin.block;

import com.zetsumei.nocoin.item.ModItems;
import com.zetsumei.nocoin.network.NocoinNetworkHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Machine à Gacha - Bloc interactif pour effectuer des tirages.
 * Le joueur doit avoir une Clé Gacha pour utiliser la machine.
 */
public class GachaMachineBlock extends Block {

    public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;

    // Forme du bloc (légèrement plus grande qu'un bloc standard pour le visuel)
    private static final VoxelShape SHAPE = Shapes.or(
            // Base
            Block.box(1, 0, 1, 15, 2, 15),
            // Corps principal
            Block.box(2, 2, 2, 14, 14, 14),
            // Dôme supérieur
            Block.box(3, 14, 3, 13, 16, 13)
    );

    public GachaMachineBlock(Properties properties) {
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

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }

        if (hand != InteractionHand.MAIN_HAND) {
            return InteractionResult.PASS;
        }

        // Vérifier si le joueur a une Clé Gacha
        ItemStack heldItem = player.getItemInHand(hand);
        boolean hasKey = hasGachaKey(player);

        // Ouvrir l'interface de la machine
        if (player instanceof ServerPlayer serverPlayer) {
            NocoinNetworkHandler.sendOpenGachaMachineScreen(serverPlayer, hasKey, countGachaKeys(player));
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
        return false;
    }

    @Override
    public int getLightEmission(BlockState state, BlockGetter level, BlockPos pos) {
        // La machine émet une légère lumière
        return 7;
    }
}
