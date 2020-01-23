package net.minecraft.block;

import net.minecraft.block.material.Material;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.BlockItemUseContext;
import net.minecraft.pathfinding.PathType;
import net.minecraft.state.BooleanProperty;
import net.minecraft.state.StateContainer;
import net.minecraft.state.properties.BlockStateProperties;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.ActionResultType;
import net.minecraft.util.Direction;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.BlockRayTraceResult;
import net.minecraft.util.math.shapes.ISelectionContext;
import net.minecraft.util.math.shapes.VoxelShape;
import net.minecraft.util.math.shapes.VoxelShapes;
import net.minecraft.world.IBlockReader;
import net.minecraft.world.IWorld;
import net.minecraft.world.World;

public class FenceGateBlock extends HorizontalBlock {
   public static final BooleanProperty OPEN = BlockStateProperties.OPEN;
   public static final BooleanProperty POWERED = BlockStateProperties.POWERED;
   public static final BooleanProperty IN_WALL = BlockStateProperties.IN_WALL;
   protected static final VoxelShape AABB_HITBOX_ZAXIS = Block.makeCuboidShape(0.0D, 0.0D, 6.0D, 16.0D, 16.0D, 10.0D);
   protected static final VoxelShape AABB_HITBOX_XAXIS = Block.makeCuboidShape(6.0D, 0.0D, 0.0D, 10.0D, 16.0D, 16.0D);
   protected static final VoxelShape AABB_HITBOX_ZAXIS_INWALL = Block.makeCuboidShape(0.0D, 0.0D, 6.0D, 16.0D, 13.0D, 10.0D);
   protected static final VoxelShape AABB_HITBOX_XAXIS_INWALL = Block.makeCuboidShape(6.0D, 0.0D, 0.0D, 10.0D, 13.0D, 16.0D);
   protected static final VoxelShape field_208068_x = Block.makeCuboidShape(0.0D, 0.0D, 6.0D, 16.0D, 24.0D, 10.0D);
   protected static final VoxelShape AABB_COLLISION_BOX_XAXIS = Block.makeCuboidShape(6.0D, 0.0D, 0.0D, 10.0D, 24.0D, 16.0D);
   protected static final VoxelShape field_208069_z = VoxelShapes.or(Block.makeCuboidShape(0.0D, 5.0D, 7.0D, 2.0D, 16.0D, 9.0D), Block.makeCuboidShape(14.0D, 5.0D, 7.0D, 16.0D, 16.0D, 9.0D));
   protected static final VoxelShape AABB_COLLISION_BOX_ZAXIS = VoxelShapes.or(Block.makeCuboidShape(7.0D, 5.0D, 0.0D, 9.0D, 16.0D, 2.0D), Block.makeCuboidShape(7.0D, 5.0D, 14.0D, 9.0D, 16.0D, 16.0D));
   protected static final VoxelShape field_208066_B = VoxelShapes.or(Block.makeCuboidShape(0.0D, 2.0D, 7.0D, 2.0D, 13.0D, 9.0D), Block.makeCuboidShape(14.0D, 2.0D, 7.0D, 16.0D, 13.0D, 9.0D));
   protected static final VoxelShape field_208067_C = VoxelShapes.or(Block.makeCuboidShape(7.0D, 2.0D, 0.0D, 9.0D, 13.0D, 2.0D), Block.makeCuboidShape(7.0D, 2.0D, 14.0D, 9.0D, 13.0D, 16.0D));

   public FenceGateBlock(Block.Properties properties) {
      super(properties);
      this.setDefaultState(this.stateContainer.getBaseState().with(OPEN, Boolean.valueOf(false)).with(POWERED, Boolean.valueOf(false)).with(IN_WALL, Boolean.valueOf(false)));
   }

   public VoxelShape getShape(BlockState state, IBlockReader worldIn, BlockPos pos, ISelectionContext context) {
      if (state.get(IN_WALL)) {
         return state.get(HORIZONTAL_FACING).getAxis() == Direction.Axis.X ? AABB_HITBOX_XAXIS_INWALL : AABB_HITBOX_ZAXIS_INWALL;
      } else {
         return state.get(HORIZONTAL_FACING).getAxis() == Direction.Axis.X ? AABB_HITBOX_XAXIS : AABB_HITBOX_ZAXIS;
      }
   }

   public BlockState updatePostPlacement(BlockState stateIn, Direction facing, BlockState facingState, IWorld worldIn, BlockPos currentPos, BlockPos facingPos) {
      Direction.Axis direction$axis = facing.getAxis();
      if (stateIn.get(HORIZONTAL_FACING).rotateY().getAxis() != direction$axis) {
         return super.updatePostPlacement(stateIn, facing, facingState, worldIn, currentPos, facingPos);
      } else {
         boolean flag = this.isWall(facingState) || this.isWall(worldIn.getBlockState(currentPos.offset(facing.getOpposite())));
         return stateIn.with(IN_WALL, Boolean.valueOf(flag));
      }
   }

   public VoxelShape getCollisionShape(BlockState state, IBlockReader worldIn, BlockPos pos, ISelectionContext context) {
      if (state.get(OPEN)) {
         return VoxelShapes.empty();
      } else {
         return state.get(HORIZONTAL_FACING).getAxis() == Direction.Axis.Z ? field_208068_x : AABB_COLLISION_BOX_XAXIS;
      }
   }

   public VoxelShape getRenderShape(BlockState state, IBlockReader worldIn, BlockPos pos) {
      if (state.get(IN_WALL)) {
         return state.get(HORIZONTAL_FACING).getAxis() == Direction.Axis.X ? field_208067_C : field_208066_B;
      } else {
         return state.get(HORIZONTAL_FACING).getAxis() == Direction.Axis.X ? AABB_COLLISION_BOX_ZAXIS : field_208069_z;
      }
   }

   public boolean allowsMovement(BlockState state, IBlockReader worldIn, BlockPos pos, PathType type) {
      switch(type) {
      case LAND:
         return state.get(OPEN);
      case WATER:
         return false;
      case AIR:
         return state.get(OPEN);
      default:
         return false;
      }
   }

   public BlockState getStateForPlacement(BlockItemUseContext context) {
      World world = context.getWorld();
      BlockPos blockpos = context.getPos();
      boolean flag = world.isBlockPowered(blockpos);
      Direction direction = context.getPlacementHorizontalFacing();
      Direction.Axis direction$axis = direction.getAxis();
      boolean flag1 = direction$axis == Direction.Axis.Z && (this.isWall(world.getBlockState(blockpos.west())) || this.isWall(world.getBlockState(blockpos.east()))) || direction$axis == Direction.Axis.X && (this.isWall(world.getBlockState(blockpos.north())) || this.isWall(world.getBlockState(blockpos.south())));
      return this.getDefaultState().with(HORIZONTAL_FACING, direction).with(OPEN, Boolean.valueOf(flag)).with(POWERED, Boolean.valueOf(flag)).with(IN_WALL, Boolean.valueOf(flag1));
   }

   private boolean isWall(BlockState p_196380_1_) {
      return p_196380_1_.getBlock().isIn(BlockTags.WALLS);
   }

   public ActionResultType func_225533_a_(BlockState p_225533_1_, World p_225533_2_, BlockPos p_225533_3_, PlayerEntity p_225533_4_, Hand p_225533_5_, BlockRayTraceResult p_225533_6_) {
      if (p_225533_1_.get(OPEN)) {
         p_225533_1_ = p_225533_1_.with(OPEN, Boolean.valueOf(false));
         p_225533_2_.setBlockState(p_225533_3_, p_225533_1_, 10);
      } else {
         Direction direction = p_225533_4_.getHorizontalFacing();
         if (p_225533_1_.get(HORIZONTAL_FACING) == direction.getOpposite()) {
            p_225533_1_ = p_225533_1_.with(HORIZONTAL_FACING, direction);
         }

         p_225533_1_ = p_225533_1_.with(OPEN, Boolean.valueOf(true));
         p_225533_2_.setBlockState(p_225533_3_, p_225533_1_, 10);
      }

      p_225533_2_.playEvent(p_225533_4_, p_225533_1_.get(OPEN) ? 1008 : 1014, p_225533_3_, 0);
      return ActionResultType.SUCCESS;
   }

   public void neighborChanged(BlockState state, World worldIn, BlockPos pos, Block blockIn, BlockPos fromPos, boolean isMoving) {
      if (!worldIn.isRemote) {
         boolean flag = worldIn.isBlockPowered(pos);
         if (state.get(POWERED) != flag) {
            worldIn.setBlockState(pos, state.with(POWERED, Boolean.valueOf(flag)).with(OPEN, Boolean.valueOf(flag)), 2);
            if (state.get(OPEN) != flag) {
               worldIn.playEvent((PlayerEntity)null, flag ? 1008 : 1014, pos, 0);
            }
         }

      }
   }

   protected void fillStateContainer(StateContainer.Builder<Block, BlockState> builder) {
      builder.add(HORIZONTAL_FACING, OPEN, POWERED, IN_WALL);
   }

   public static boolean isParallel(BlockState p_220253_0_, Direction p_220253_1_) {
      return p_220253_0_.get(HORIZONTAL_FACING).getAxis() == p_220253_1_.rotateY().getAxis();
   }

   //AH NEW ****
   public void toggleGate(World worldIn, BlockPos pos, boolean open) {
      BlockState blockstate = worldIn.getBlockState(pos);
      if (blockstate.getBlock() == this && blockstate.get(OPEN) != open) {
         worldIn.setBlockState(pos, blockstate.with(OPEN, Boolean.valueOf(open)), 10);
         this.playSound(worldIn, pos, open);
      }
   }

   private void playSound(World world, BlockPos pos, boolean open) {
      world.playEvent((PlayerEntity)null, open ? 1008 : 1014, pos, 0);
   }

   //AH NEW END ****
}
