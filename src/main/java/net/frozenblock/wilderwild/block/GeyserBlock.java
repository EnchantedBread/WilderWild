/*
 * Copyright 2023-2024 FrozenBlock
 * This file is part of Wilder Wild.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, see <https://www.gnu.org/licenses/>.
 */

package net.frozenblock.wilderwild.block;

import com.mojang.serialization.MapCodec;
import net.frozenblock.wilderwild.block.entity.GeyserBlockEntity;
import net.frozenblock.wilderwild.block.impl.GeyserStage;
import net.frozenblock.wilderwild.block.impl.GeyserType;
import net.frozenblock.wilderwild.misc.client.ClientMethodInteractionHandler;
import net.frozenblock.wilderwild.registry.RegisterBlockEntities;
import net.frozenblock.wilderwild.registry.RegisterProperties;
import net.frozenblock.wilderwild.registry.RegisterSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.SupportType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class GeyserBlock extends BaseEntityBlock {
	public static final EnumProperty<GeyserType> GEYSER_TYPE = RegisterProperties.GEYSER_TYPE;
	public static final EnumProperty<GeyserStage> GEYSER_STAGE = RegisterProperties.GEYSER_STAGE;
	public static final DirectionProperty FACING = BlockStateProperties.FACING;
	public static final BooleanProperty NATURAL = RegisterProperties.NATURAL;
	public static final MapCodec<GeyserBlock> CODEC = simpleCodec(GeyserBlock::new);

	public GeyserBlock(@NotNull Properties settings) {
		super(settings);
		this.registerDefaultState(this.getStateDefinition().any()
			.setValue(GEYSER_TYPE, GeyserType.NONE)
			.setValue(GEYSER_STAGE, GeyserStage.DORMANT)
			.setValue(FACING, Direction.UP)
			.setValue(NATURAL, true)
		);
	}

	@NotNull
	@Override
	protected MapCodec<? extends GeyserBlock> codec() {
		return CODEC;
	}

	@Nullable
	@Override
	public BlockEntity newBlockEntity(@NotNull BlockPos pos, @NotNull BlockState state) {
		return new GeyserBlockEntity(pos, state);
	}

	@Override
	protected void createBlockStateDefinition(@NotNull StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(GEYSER_TYPE, GEYSER_STAGE, FACING, NATURAL);
	}

	@Override
	public BlockState getStateForPlacement(@NotNull BlockPlaceContext context) {
		Level level = context.getLevel();
		BlockPos pos = context.getClickedPos();
		Direction direction = context.getClickedFace();
		GeyserType geyserType = getGeyserTypeForPos(level, direction, pos);
		boolean canErupt = context.getLevel().hasNeighborSignal(context.getClickedPos()) && !isInactive(geyserType);

		return this.defaultBlockState()
			.setValue(GEYSER_STAGE, canErupt ? GeyserStage.ERUPTING : GeyserStage.DORMANT)
			.setValue(GEYSER_TYPE, geyserType)
			.setValue(FACING, direction)
			.setValue(NATURAL, false);
	}

	@Override
	protected void neighborChanged(BlockState blockState, @NotNull Level level, BlockPos blockPos, Block block, BlockPos blockPos2, boolean bl) {
		if (!level.isClientSide) {
			boolean erupting = blockState.getValue(GEYSER_STAGE) == GeyserStage.ERUPTING;
			if (erupting != level.hasNeighborSignal(blockPos) && !isInactive(blockState.getValue(GEYSER_TYPE))) {
				if (!erupting) {
					level.setBlock(blockPos, blockState.setValue(GEYSER_STAGE, GeyserStage.ERUPTING), Block.UPDATE_CLIENTS);
				}
			}
		}
	}

	@Override
	@NotNull
	public BlockState updateShape(@NotNull BlockState state, @NotNull Direction direction, @NotNull BlockState neighborState, @NotNull LevelAccessor level, @NotNull BlockPos currentPos, @NotNull BlockPos neighborPos) {
		if (direction.equals(state.getValue(FACING))) {
			GeyserType geyserType = getGeyserTypeForPos(level, state, currentPos);
			if (geyserType != state.getValue(GEYSER_TYPE)) {
				state = state.setValue(GEYSER_TYPE, geyserType);
			}
		}
		return state;
	}

	@Override
	public void onRemove(@NotNull BlockState state, @NotNull Level level, @NotNull BlockPos pos, @NotNull BlockState newState, boolean isMoving) {
		super.onRemove(state, level, pos, newState, isMoving);
	}

	public static GeyserType getGeyserTypeForPos(@NotNull LevelAccessor level, @NotNull BlockState state, @NotNull BlockPos pos) {
		return getGeyserTypeForPos(level, state.getValue(FACING), pos);
	}

	public static GeyserType getGeyserTypeForPos(@NotNull LevelAccessor level, @NotNull Direction direction, @NotNull BlockPos pos) {
		BlockPos checkPos = pos.relative(direction);
		BlockState checkState = level.getBlockState(checkPos);
		if (!checkState.isFaceSturdy(level, checkPos, direction.getOpposite(), SupportType.CENTER)) {
			if (checkState.getFluidState().is(FluidTags.WATER)) {
				return GeyserType.WATER;
			} else if (checkState.getFluidState().is(FluidTags.LAVA)) {
				return GeyserType.LAVA;
			} else {
				return GeyserType.AIR;
			}
		}
		return GeyserType.NONE;
	}

	@Override
	public void tick(@NotNull BlockState state, @NotNull ServerLevel level, @NotNull BlockPos pos, @NotNull RandomSource random) {
		GeyserType geyserType = getGeyserTypeForPos(level, state, pos);
		if (geyserType != state.getValue(GEYSER_TYPE)) {
			level.setBlock(pos, state.setValue(GEYSER_TYPE, geyserType), UPDATE_ALL);
		}
	}

	@Override
	public void animateTick(@NotNull BlockState blockState, @NotNull Level level, @NotNull BlockPos blockPos, @NotNull RandomSource random) {
		GeyserType geyserType = blockState.getValue(GEYSER_TYPE);
		Direction direction = blockState.getValue(FACING);
		boolean natural = blockState.getValue(NATURAL);
		if (!isInactive(geyserType)) {
			ClientMethodInteractionHandler.spawnBaseGeyserParticles(blockPos, direction, natural, random);
			if (natural ? random.nextFloat() <= 0.0085F : random.nextFloat() <= 0.002F) {
				level.playLocalSound(blockPos, RegisterSounds.BLOCK_GEYSER_BOIL, SoundSource.BLOCKS, 0.15F, 0.9F + (random.nextFloat() * 0.2F), false);
			}
		}
	}

	@Override
	@NotNull
	public RenderShape getRenderShape(@NotNull BlockState blockState) {
		return RenderShape.MODEL;
	}

	@Nullable
	public <T extends BlockEntity> BlockEntityTicker<T> getTicker(@NotNull Level level, @NotNull BlockState state, @NotNull BlockEntityType<T> type) {
		return !level.isClientSide ? createTickerHelper(type, RegisterBlockEntities.GEYSER, (worldx, pos, statex, blockEntity) ->
			blockEntity.tickServer(worldx, pos, statex, worldx.random))
			: createTickerHelper(type, RegisterBlockEntities.GEYSER, (worldx, pos, statex, blockEntity) ->
			blockEntity.tickClient(worldx, pos, statex, worldx.random));
	}

	public static boolean isInactive(GeyserType geyserType) {
		return geyserType == GeyserType.NONE;
	}

	public static @NotNull Vec3 getParticleVelocity(@NotNull Direction direction, @NotNull RandomSource random, double min, double max) {
		double difference = max - min;
		double velocity = min + (random.nextDouble() * difference);
		double x = direction.getStepX() * velocity;
		double y = direction.getStepY() * velocity;
		double z = direction.getStepZ() * velocity;
		return new Vec3(x, y, z);
	}

	public static @NotNull Vec3 getVelocityFromDistance(BlockPos pos, Direction direction, @NotNull Vec3 vec3, @NotNull RandomSource random, double max) {
		return vec3.subtract(getParticlePosWithoutRandom(pos, direction, random)).scale(random.nextDouble() * max);
	}

	public static @NotNull Vec3 getParticlePosWithoutRandom(BlockPos pos, Direction direction, RandomSource random) {
		return Vec3.atLowerCornerOf(pos).add(
			getParticleOffsetX(direction, random, false),
			getParticleOffsetY(direction, random, false),
			getParticleOffsetZ(direction, random, false)
		);
	}

	public static @NotNull Vec3 getParticlePos(BlockPos pos, Direction direction, RandomSource random) {
		return Vec3.atLowerCornerOf(pos).add(
			getParticleOffsetX(direction, random, true),
			getParticleOffsetY(direction, random, true),
			getParticleOffsetZ(direction, random, true)
		);
	}

	private static double getRandomParticleOffset(@NotNull RandomSource random) {
		return random.nextDouble() / 3D * (random.nextBoolean() ? 1D : -1D);
	}

	private static double getParticleOffsetX(@NotNull Direction direction, RandomSource random, boolean useRandom) {
		return switch (direction) {
			case UP, DOWN, SOUTH, NORTH -> 0.5D + (useRandom ? getRandomParticleOffset(random) : 0D);
			case EAST -> 1.05D;
			case WEST -> -0.05D;
		};
	}

	private static double getParticleOffsetY(@NotNull Direction direction, RandomSource random, boolean useRandom) {
		return switch (direction) {
			case DOWN -> -0.05D;
			case UP -> 1.05D;
			case NORTH, WEST, EAST, SOUTH -> 0.5D + (useRandom ? getRandomParticleOffset(random) : 0D);
		};
	}

	private static double getParticleOffsetZ(@NotNull Direction direction, RandomSource random, boolean useRandom) {
		return switch (direction) {
			case UP, DOWN, EAST, WEST -> 0.5D + (useRandom ? getRandomParticleOffset(random) : 0D);
			case NORTH -> -0.05D;
			case SOUTH -> 1.05D;
		};
	}
}
