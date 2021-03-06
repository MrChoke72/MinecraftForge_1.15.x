package net.minecraft.pathfinding;

import com.google.common.collect.ImmutableSet;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.Entity;
import net.minecraft.entity.MobEntity;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.ai.attributes.IAttributeInstance;
import net.minecraft.network.DebugPacketSender;
import net.minecraft.util.Util;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.Region;
import net.minecraft.world.World;

public abstract class PathNavigator {
   protected final MobEntity entity;
   protected final World world;
   @Nullable
   protected Path currentPath;
   protected double speed;

   //AH CHANGE REFACTOR
   private final IAttributeInstance followRange;
   //private final IAttributeInstance field_226333_p_;

   protected int totalTicks;
   protected int ticksAtLastPos;
   protected Vec3d lastPosCheck = Vec3d.ZERO;
   protected Vec3d timeoutCachedNode = Vec3d.ZERO;
   protected long timeoutTimer;
   protected long lastTimeoutCheck;
   protected double timeoutLimit;
   protected float maxDistanceToWaypoint = 0.5F;
   protected boolean tryUpdatePath;
   protected long lastTimeUpdated;
   protected NodeProcessor nodeProcessor;
   private BlockPos targetPos;

   //AH REFACTOR
   private int keepDist;
   //private int field_225468_r;

   //AH CHANGE REFACTOR
   private float iterMaxMult = 1.0F;
   //private float field_226334_s_ = 1.0F;

   private final PathFinder pathFinder;

   public PathNavigator(MobEntity entityIn, World worldIn) {
      this.entity = entityIn;
      this.world = worldIn;
      this.followRange = entityIn.getAttribute(SharedMonsterAttributes.FOLLOW_RANGE);
      int i = MathHelper.floor(this.followRange.getValue() * 16.0D);
      this.pathFinder = this.getPathFinder(i);
   }

   //AH CHANGE REFACTOR
   public void setDefaultIterMaxMult() {
   //public void func_226336_g_() {
      this.iterMaxMult = 1.0F;
   }

   public void setIterMaxMult(float p_226335_1_) {
      this.iterMaxMult = p_226335_1_;
   }

   public BlockPos getTargetPos() {
      return this.targetPos;
   }

   protected abstract PathFinder getPathFinder(int followRangeMult16);

   public void setSpeed(double speedIn) {
      this.speed = speedIn;
   }

   public boolean canUpdatePathOnTimeout() {
      return this.tryUpdatePath;
   }

   public void updatePath() {
      if (this.world.getGameTime() - this.lastTimeUpdated > 20L) {
         if (this.targetPos != null) {
            this.currentPath = null;
            this.currentPath = this.getPathToPos(this.targetPos, this.keepDist);
            this.lastTimeUpdated = this.world.getGameTime();
            this.tryUpdatePath = false;
         }
      } else {
         this.tryUpdatePath = true;
      }

   }

   @Nullable
   //AH REFACTOR
   public final Path getPathToPos(double x, double y, double z, int keepDist) {
   //public final Path func_225466_a(double p_225466_1_, double p_225466_3_, double p_225466_5_, int p_225466_7_) {
      return this.getPathToPos(new BlockPos(x, y, z), keepDist);
   }

   @Nullable
   //AH CHANGE REFACTOR
   public Path findPath(Stream<BlockPos> posStream, int keepDist) {
   //public Path func_225463_a(Stream<BlockPos> p_225463_1_, int p_225463_2_) {
      return this.findPath(posStream.collect(Collectors.toSet()), 8, false, keepDist);
   }

   @Nullable
   //AH CHANGE REFACTOR
   public Path getPathToPos(BlockPos pos, int keepDist) {
   //public Path getPathToPos(BlockPos pos, int p_179680_2_) {
      return this.findPath(ImmutableSet.of(pos), 8, false, keepDist);
   }

   @Nullable
   public Path getPathToEntity(Entity entityIn, int keepDist) {
      return this.findPath(ImmutableSet.of(new BlockPos(entityIn)), 16, true, keepDist);
   }

   @Nullable
   //AH CHANGE REFACTOR
   protected Path findPath(Set<BlockPos> finalTgtSet, int followRangeExtend, boolean startUp1Block, int keepDist) {
   //protected Path func_225464_a(Set<BlockPos> p_225464_1_, int p_225464_2_, boolean p_225464_3_, int p_225464_4_) {
      if (finalTgtSet.isEmpty()) {
         return null;
      } else if (this.entity.getPosY() < 0.0D) {
         return null;
      } else if (!this.canNavigate()) {
         return null;
      } else if (this.currentPath != null && !this.currentPath.isFinished() && finalTgtSet.contains(this.targetPos)) {
         return this.currentPath;
      } else {
         this.world.getProfiler().startSection("pathfind");
         float f = (float)this.followRange.getValue();
         BlockPos blockpos = startUp1Block ? (new BlockPos(this.entity)).up() : new BlockPos(this.entity);
         int i = (int)(f + (float)followRangeExtend);
         Region region = new Region(this.world, blockpos.add(-i, -i, -i), blockpos.add(i, i, i));
         Path path = this.pathFinder.findPath(region, this.entity, finalTgtSet, f, keepDist, this.iterMaxMult);
         this.world.getProfiler().endSection();
         if (path != null && path.getTargetPos() != null) {
            this.targetPos = path.getTargetPos();
            this.keepDist = keepDist;
         }

         return path;
      }
   }

   public boolean tryMoveToXYZ(double x, double y, double z, double speedIn) {
      return this.setPath(this.getPathToPos(x, y, z, 1), speedIn);
   }

   public boolean tryMoveToEntityLiving(Entity entityIn, double speedIn) {
      Path path = this.getPathToEntity(entityIn, 1);
      return path != null && this.setPath(path, speedIn);
   }

   public boolean setPath(@Nullable Path pathentityIn, double speedIn) {
      if (pathentityIn == null) {

         //AH CHANGE DEBUG OFF
         /*
         if(this.entity instanceof AbstractVillagerEntity && this.entity.getCustomName() != null) // && this.entity.getCustomName().getString().equals("Chuck"))
         {
            //System.out.println("BREAK 2");
            System.out.println("in setPath to null.");
         }
          */

         this.currentPath = null;
         return false;
      } else {
         if (!pathentityIn.isSamePath(this.currentPath)) {
            this.currentPath = pathentityIn;
         }

         if (this.noPath()) {
            return false;
         } else {
            this.trimPath();
            if (this.currentPath.getCurrentPathLength() <= 0) {
               return false;
            } else {
               this.speed = speedIn;
               Vec3d vec3d = this.getEntityPosition();
               this.ticksAtLastPos = this.totalTicks;
               this.lastPosCheck = vec3d;
               return true;
            }
         }
      }
   }

   @Nullable
   public Path getPath() {
      return this.currentPath;
   }

   public void tick() {
      ++this.totalTicks;
      if (this.tryUpdatePath) {
         this.updatePath();
      }

      if (!this.noPath()) {
         if (this.canNavigate()) {
            this.pathFollow();
         } else if (this.currentPath != null && this.currentPath.getCurrentPathIndex() < this.currentPath.getCurrentPathLength()) {
            Vec3d vec3d = this.getEntityPosition();
            Vec3d vec3d1 = this.currentPath.getVectorFromIndex(this.entity, this.currentPath.getCurrentPathIndex());
            if (vec3d.y > vec3d1.y && !this.entity.onGround && MathHelper.floor(vec3d.x) == MathHelper.floor(vec3d1.x) && MathHelper.floor(vec3d.z) == MathHelper.floor(vec3d1.z)) {
               this.currentPath.setCurrentPathIndex(this.currentPath.getCurrentPathIndex() + 1);
            }
         }

         DebugPacketSender.sendPath(this.world, this.entity, this.currentPath, this.maxDistanceToWaypoint);
         if (!this.noPath()) {
            Vec3d vec3d2 = this.currentPath.getPosition(this.entity);
            BlockPos blockpos = new BlockPos(vec3d2);

            //AH CHANGE DEBUG OFF
            /*
            if(this.entity.getCustomName() != null) // && this.entity.getCustomName().getString().equals("Chuck"))
            {
               System.out.println("in navigator tick, currPathPos=" + vec3d2.toString() + "currPathIdx=" + currentPath.getCurrentPathIndex() + ", pathSize=" + this.currentPath.getCurrentPathLength() + ", lastPoint=" + getPath().getFinalPathPoint());
            }
             */

            this.entity.getMoveHelper().setMoveTo(vec3d2.x, this.world.getBlockState(blockpos.down()).isAir() ? vec3d2.y : WalkNodeProcessor.getGroundY(this.world, blockpos), vec3d2.z, this.speed);
         }
         //AH CHANGE DEBUG OFF
         /*
         else
         {
            if(this.entity.getCustomName() != null) // && this.entity.getCustomName().getString().equals("Chuck"))
            {
               System.out.println("in navigator tick, noPath");
            }
         }
          */

      }
   }

   protected void pathFollow() {
      Vec3d vec3d = this.getEntityPosition();
      this.maxDistanceToWaypoint = this.entity.getWidth() > 0.75F ? this.entity.getWidth() / 2.0F : 0.75F - this.entity.getWidth() / 2.0F;
      Vec3d vec3d1 = this.currentPath.getCurrentPos();
      if (Math.abs(this.entity.getPosX() - (vec3d1.x + 0.5D)) < (double)this.maxDistanceToWaypoint && Math.abs(this.entity.getPosZ() - (vec3d1.z + 0.5D)) < (double)this.maxDistanceToWaypoint && Math.abs(this.entity.getPosY() - vec3d1.y) < 1.0D) {
         this.currentPath.setCurrentPathIndex(this.currentPath.getCurrentPathIndex() + 1);
      }

      this.checkForStuck(vec3d);
   }

   protected void checkForStuck(Vec3d positionVec3) {
      if (this.totalTicks - this.ticksAtLastPos > 100) {
         if (positionVec3.squareDistanceTo(this.lastPosCheck) < 2.25D) {

            //AH CHANGE DEBUG OFF
            /*
            if(this.entity.getCustomName() != null)
            {
               System.out.println("checkForStuck 1, calling clearPath.  entPos=" + this.entity.getPosition() + ", posTryingToReach=" + this.currentPath.getCurrentPos());
            }
             */

            this.clearPath();
         }

         this.ticksAtLastPos = this.totalTicks;
         this.lastPosCheck = positionVec3;
      }

      if (this.currentPath != null && !this.currentPath.isFinished()) {
         Vec3d vec3d = this.currentPath.getCurrentPos();
         if (vec3d.equals(this.timeoutCachedNode)) {
            this.timeoutTimer += Util.milliTime() - this.lastTimeoutCheck;
         } else {
            this.timeoutCachedNode = vec3d;
            double d0 = positionVec3.distanceTo(this.timeoutCachedNode);
            this.timeoutLimit = this.entity.getAIMoveSpeed() > 0.0F ? d0 / (double)this.entity.getAIMoveSpeed() * 1000.0D : 0.0D;
         }

         //AH DEBUG OFF - Stop time-based check for stuck when I am debugging!!
         if (this.timeoutLimit > 0.0D && (double)this.timeoutTimer > this.timeoutLimit * 3.0D) {
            this.timeoutCachedNode = Vec3d.ZERO;
            this.timeoutTimer = 0L;
            this.timeoutLimit = 0.0D;

            //AH CHANGE DEBUG OFF
            /*
            if(this.entity.getCustomName() != null)
            {
               System.out.println("checkForStuck 2, calling clearPath.  entPos=" + this.entity.getPosition() + ", posTryingToReach=" + this.currentPath.getCurrentPos()););
            }
            */

            this.clearPath();
         }

         this.lastTimeoutCheck = Util.milliTime();
      }

   }

   public boolean noPath() {
      return this.currentPath == null || this.currentPath.isFinished();
   }

   public boolean hasPath() {
      return !this.noPath();
   }

   public void clearPath() {

      //AH CHANGE DEBUG OFF
      /*
      if(this.entity.getCustomName() != null) // && this.entity.getCustomName().getString().equals("Chuck"))
      {
         //System.out.println("BREAK 2");
         System.out.println("in clearPath to null.");
      }
       */

      this.currentPath = null;
   }

   protected abstract Vec3d getEntityPosition();

   protected abstract boolean canNavigate();

   protected boolean isInLiquid() {
      return this.entity.isInWaterOrBubbleColumn() || this.entity.isInLava();
   }

   protected void trimPath() {
      if (this.currentPath != null) {
         for(int i = 0; i < this.currentPath.getCurrentPathLength(); ++i) {
            PathPoint pathpoint = this.currentPath.getPathPointFromIndex(i);
            PathPoint pathpoint1 = i + 1 < this.currentPath.getCurrentPathLength() ? this.currentPath.getPathPointFromIndex(i + 1) : null;
            BlockState blockstate = this.world.getBlockState(new BlockPos(pathpoint.x, pathpoint.y, pathpoint.z));
            Block block = blockstate.getBlock();
            if (block == Blocks.CAULDRON) {
               this.currentPath.setPoint(i, pathpoint.cloneMove(pathpoint.x, pathpoint.y + 1, pathpoint.z));
               if (pathpoint1 != null && pathpoint.y >= pathpoint1.y) {
                  this.currentPath.setPoint(i + 1, pathpoint1.cloneMove(pathpoint1.x, pathpoint.y + 1, pathpoint1.z));
               }
            }
         }

      }
   }

   protected abstract boolean isDirectPathBetweenPoints(Vec3d posVec31, Vec3d posVec32, int sizeX, int sizeY, int sizeZ);

   public boolean canEntityStandOnPos(BlockPos pos) {
      BlockPos blockpos = pos.down();
      return this.world.getBlockState(blockpos).isOpaqueCube(this.world, blockpos);
   }

   public NodeProcessor getNodeProcessor() {
      return this.nodeProcessor;
   }

   public void setCanSwim(boolean canSwim) {
      this.nodeProcessor.setCanSwim(canSwim);
   }

   public boolean getCanSwim() {
      return this.nodeProcessor.getCanSwim();
   }

   public void func_220970_c(BlockPos p_220970_1_) {
      if (this.currentPath != null && !this.currentPath.isFinished() && this.currentPath.getCurrentPathLength() != 0) {
         PathPoint pathpoint = this.currentPath.getFinalPathPoint();
         Vec3d vec3d = new Vec3d(((double)pathpoint.x + this.entity.getPosX()) / 2.0D, ((double)pathpoint.y + this.entity.getPosY()) / 2.0D, ((double)pathpoint.z + this.entity.getPosZ()) / 2.0D);
         if (p_220970_1_.withinDistance(vec3d, (double)(this.currentPath.getCurrentPathLength() - this.currentPath.getCurrentPathIndex()))) {
            this.updatePath();
         }

      }
   }
}
