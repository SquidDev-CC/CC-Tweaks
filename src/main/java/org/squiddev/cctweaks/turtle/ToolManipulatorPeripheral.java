package org.squiddev.cctweaks.turtle;

import com.google.common.collect.Multimap;
import dan200.computercraft.api.lua.ILuaContext;
import dan200.computercraft.api.lua.ILuaTask;
import dan200.computercraft.api.lua.LuaException;
import dan200.computercraft.api.peripheral.IComputerAccess;
import dan200.computercraft.api.peripheral.IPeripheral;
import dan200.computercraft.api.turtle.*;
import dan200.computercraft.shared.turtle.core.InteractDirection;
import dan200.computercraft.shared.util.WorldUtil;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.ai.attributes.AttributeModifier;
import net.minecraft.init.Blocks;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ActionResult;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.ForgeEventFactory;
import net.minecraftforge.event.entity.player.PlayerDestroyItemEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import org.apache.commons.lang3.tuple.Pair;
import org.squiddev.cctweaks.api.network.INetworkCompatiblePeripheral;
import org.squiddev.cctweaks.core.turtle.TurtleRegistry;
import org.squiddev.cctweaks.lua.lib.DelayedTask;

import javax.annotation.Nonnull;

public class ToolManipulatorPeripheral implements IPeripheral, INetworkCompatiblePeripheral {
	private final ITurtleAccess access;
	private final ToolHostPlayer player;
	private final TurtleSide side;

	public ToolManipulatorPeripheral(ITurtleAccess access, ToolHostPlayer player, TurtleSide side) {
		this.access = access;
		this.player = player;
		this.side = side;
	}

	@Nonnull
	@Override
	public String getType() {
		return "tool_manipulator";
	}

	@Nonnull
	@Override
	public String[] getMethodNames() {
		return new String[]{
			"use", "useUp", "useDown",
			"canUse", "canUseUp", "canUseDown",
			"swing", "swingUp", "swingDown",
			"canSwing", "canSwingUp", "canSwingDown",
		};
	}

	@Override
	public Object[] callMethod(@Nonnull IComputerAccess computer, @Nonnull ILuaContext context, int method, @Nonnull Object[] args) throws LuaException, InterruptedException {
		switch (method) {
			case 0:
				return use(computer, context, InteractDirection.Forward, args);
			case 1:
				return use(computer, context, InteractDirection.Up, args);
			case 2:
				return use(computer, context, InteractDirection.Down, args);
			case 3:
				return canUse(computer, context, InteractDirection.Forward, args);
			case 4:
				return canUse(computer, context, InteractDirection.Up, args);
			case 5:
				return canUse(computer, context, InteractDirection.Down, args);
			case 6:
				return swing(computer, context, InteractDirection.Forward, args);
			case 7:
				return swing(computer, context, InteractDirection.Up, args);
			case 8:
				return swing(computer, context, InteractDirection.Down, args);
			case 9:
				return canSwing(computer, context, InteractDirection.Forward, args);
			case 10:
				return canSwing(computer, context, InteractDirection.Up, args);
			case 11:
				return canSwing(computer, context, InteractDirection.Down, args);
		}

		return null;
	}

	//region Use
	public Object[] use(final IComputerAccess computer, ILuaContext context, InteractDirection direction, Object[] args) throws LuaException, InterruptedException {
		final int duration;
		final boolean sneak;
		if (args.length <= 0 || args[0] == null) {
			duration = 0;
		} else if (args[0] instanceof Number) {
			duration = ((Number) args[0]).intValue();

			if (duration < 0) throw new LuaException("Duration must be >= 0");
		} else {
			throw new LuaException("Expected number for argument #1");
		}

		if (args.length <= 1 || args[1] == null) {
			sneak = false;
		} else if (args[1] instanceof Boolean) {
			sneak = (Boolean) args[1];
		} else {
			throw new LuaException("Expected boolean for argument #2");
		}

		final EnumFacing dir = direction.toWorldDir(access);

		return new DelayedTask() {
			@Override
			public Object[] execute() throws LuaException {
				return doUse(this, computer, dir, sneak, duration);
			}
		}.execute(computer, context);
	}

	public Object[] doUse(DelayedTask task, IComputerAccess computer, EnumFacing direction, boolean sneak, int duration) throws LuaException {
		player.load(access, direction, sneak);

		RayTraceResult hit = findHit(direction, 0.65);
		ItemStack stack = player.getItem(access);
		World world = player.getEntityWorld();

		player.posY += 1.5;

		try {
			if (!stack.isEmpty()) {
				TurtleCommandResult result = TurtleRegistry.instance.use(access, computer, player, stack, direction, hit);
				if (result != null) return toObjectArray(result);
			}

			if (hit != null) {
				switch (hit.typeOfHit) {
					case ENTITY:
						if (!stack.isEmpty()) {
							EnumActionResult result = player.interactOn(hit.entityHit, EnumHand.MAIN_HAND);
							if (result != EnumActionResult.PASS) {
								return new Object[]{result == EnumActionResult.SUCCESS, "entity", "interact"};
							}
						}
						break;
					case BLOCK: {
						// When right next to a block the hit direction gets inverted. Try both to see if one works.
						Object[] result = tryUseOnBlock(world, hit, stack, hit.sideHit);
						if (result != null) return result;

						result = tryUseOnBlock(world, hit, stack, hit.sideHit.getOpposite());
						if (result != null) return result;
					}
				}
			}

			if (!stack.isEmpty()) {
				if (MinecraftForge.EVENT_BUS.post(new PlayerInteractEvent.RightClickEmpty(player, EnumHand.MAIN_HAND))) {
					return new Object[]{true, "item", "use"};
				}

				player.posX += direction.getFrontOffsetX() * 0.6;
				player.posY += direction.getFrontOffsetY() * 0.6;
				player.posZ += direction.getFrontOffsetZ() * 0.6;

				duration = Math.min(duration, stack.getMaxItemUseDuration());
				ActionResult<ItemStack> result = stack.useItemRightClick(player.getEntityWorld(), player, EnumHand.MAIN_HAND);

				switch (result.getType()) {
					case FAIL:
						return new Object[]{false, "item", "use"};
					case SUCCESS:
						task.delay = duration;
						ItemStack active = player.getActiveItemStack();
						if (!active.isEmpty() && !ForgeEventFactory.onUseItemStop(player, active, duration)) {
							active.onPlayerStoppedUsing(player.getEntityWorld(), player, active.getMaxItemUseDuration() - duration);
							player.resetActiveHand();
						}

						player.inventory.setInventorySlotContents(player.inventory.currentItem, result.getResult());
						return new Object[]{true, "item", "use"};
				}
			}

			return new Object[]{false};
		} finally {
			player.resetActiveHand();
			player.unload(access);
		}
	}

	public Object[] tryUseOnBlock(World world, RayTraceResult hit, @Nonnull ItemStack stack, EnumFacing side) {
		IBlockState state = world.getBlockState(hit.getBlockPos());
		if (!state.getBlock().isAir(state, world, hit.getBlockPos())) {
			if (MinecraftForge.EVENT_BUS.post(new PlayerInteractEvent.RightClickBlock(player, EnumHand.MAIN_HAND, hit.getBlockPos(), side, hit.hitVec))) {
				return new Object[]{true, "block", "interact"};
			}

			Object[] result = onPlayerRightClick(stack, hit.getBlockPos(), side, hit.hitVec);
			if (result != null) return result;
		}

		return null;
	}

	public Object[] onPlayerRightClick(@Nonnull ItemStack stack, BlockPos pos, EnumFacing side, Vec3d look) {
		float xCoord = (float) look.x - (float) pos.getX();
		float yCoord = (float) look.y - (float) pos.getY();
		float zCoord = (float) look.z - (float) pos.getZ();
		World world = player.getEntityWorld();

		if (!stack.isEmpty() && stack.getItem().onItemUseFirst(player, world, pos, side, xCoord, yCoord, zCoord, EnumHand.MAIN_HAND) == EnumActionResult.SUCCESS) {
			return new Object[]{true, "item", "use"};
		}

		if (!player.isSneaking() || stack.isEmpty() || stack.getItem().doesSneakBypassUse(stack, world, pos, player)) {
			IBlockState state = world.getBlockState(pos);
			if (state.getBlock().onBlockActivated(world, pos, state, player, EnumHand.MAIN_HAND, side, xCoord, yCoord, zCoord)) {
				return new Object[]{true, "block", "interact"};
			}
		}

		if (stack.isEmpty()) return null;

		if (stack.getItem() instanceof ItemBlock) {
			ItemBlock itemBlock = (ItemBlock) stack.getItem();

			Block block = world.getBlockState(pos).getBlock();
			BlockPos shiftPos = pos;
			EnumFacing shiftSide = side;
			if (block == Blocks.SNOW_LAYER && block.isReplaceable(world, pos)) {
				shiftSide = EnumFacing.UP;
			} else if (!block.isReplaceable(world, pos)) {
				shiftPos = pos.offset(side);
			}

			if (!world.mayPlace(itemBlock.getBlock(), shiftPos, false, shiftSide, null)) {
				return null;
			}
		}

		if (stack.onItemUse(player, world, pos, EnumHand.MAIN_HAND, side, xCoord, yCoord, zCoord) == EnumActionResult.SUCCESS) {
			if (stack.getCount() <= 0) {
				MinecraftForge.EVENT_BUS.post(new PlayerDestroyItemEvent(player, stack, EnumHand.MAIN_HAND));
			}

			return new Object[]{true, "place"};
		}

		return null;
	}
	//endregion

	//region Swing
	public Object[] swing(final IComputerAccess computer, ILuaContext context, InteractDirection direction, Object[] args) throws LuaException, InterruptedException {
		final boolean sneak;

		if (args.length <= 0 || args[0] == null) {
			sneak = false;
		} else if (args[0] instanceof Boolean) {
			sneak = (Boolean) args[0];
		} else {
			throw new LuaException("Expected boolean");
		}

		final EnumFacing dir = direction.toWorldDir(access);

		return access.executeCommand(context, new ITurtleCommand() {
			@Nonnull
			@Override
			public TurtleCommandResult execute(@Nonnull ITurtleAccess iTurtleAccess) {
				try {
					return doSwing(computer, dir, sneak);
				} catch (LuaException e) {
					return TurtleCommandResult.failure(e.getMessage());
				}
			}
		});
	}

	public TurtleCommandResult doSwing(IComputerAccess computer, EnumFacing direction, boolean sneak) throws LuaException {
		player.load(access, direction, sneak);

		ItemStack stack = player.getItem(access);
		TurtleAnimation animation = side == TurtleSide.Left ? TurtleAnimation.SwingLeftTool : TurtleAnimation.SwingRightTool;
		RayTraceResult hit = findHit(direction, 0.65);

		player.posY += 1.5;

		try {
			if (!stack.isEmpty()) {
				TurtleCommandResult result = TurtleRegistry.instance.swing(access, computer, player, stack, direction, hit);
				if (result != null) {
					if (result.isSuccess()) access.playAnimation(animation);
					return result;
				}
			}

			if (hit != null) {
				switch (hit.typeOfHit) {
					case ENTITY: {
						TurtleCommandResult result = player.attack(access, hit.entityHit);
						if (result.isSuccess()) access.playAnimation(animation);
						return result;
					}
					case BLOCK: {
						TurtleCommandResult result = player.dig(access, direction, hit.getBlockPos());
						if (result.isSuccess()) access.playAnimation(animation);
						return result;
					}
				}
			}
		} finally {
			player.resetActiveHand();
			player.unload(access);
		}

		return TurtleCommandResult.failure("Nothing to do here");
	}
	//endregion

	//region Can use
	public Object[] canUse(final IComputerAccess computer, ILuaContext context, InteractDirection dir, Object[] args) throws LuaException, InterruptedException {
		final EnumFacing direction = dir.toWorldDir(access);
		return context.executeMainThreadTask(new ILuaTask() {
			@Override
			public Object[] execute() throws LuaException {
				player.load(access, direction, false);

				ItemStack stack = player.getHeldItem(EnumHand.MAIN_HAND);
				RayTraceResult hit = findHit(direction, 0.65);

				player.posY += 1.5;

				try {
					if (!stack.isEmpty()) {
						boolean result = TurtleRegistry.instance.canUse(access, player, stack, direction, hit);
						if (result) return new Object[]{true};
					}

					return new Object[]{false};
				} finally {
					player.unload(access);
				}
			}
		});
	}

	public Object[] canSwing(final IComputerAccess computer, ILuaContext context, InteractDirection dir, Object[] args) throws LuaException, InterruptedException {
		final EnumFacing direction = dir.toWorldDir(access);
		return context.executeMainThreadTask(new ILuaTask() {
			@Override
			public Object[] execute() throws LuaException {
				player.load(access, direction, false);

				ItemStack stack = player.getHeldItem(EnumHand.MAIN_HAND);
				RayTraceResult hit = findHit(direction, 0.65);

				player.posY += 1.5;

				try {
					if (!stack.isEmpty()) {
						boolean result = TurtleRegistry.instance.canSwing(access, player, stack, direction, hit);
						if (result) return new Object[]{true};

						if (hit != null && hit.entityHit != null) {
							@SuppressWarnings("unchecked") Multimap<String, AttributeModifier> map = stack.getAttributeModifiers(EntityEquipmentSlot.MAINHAND);
							for (AttributeModifier modifier : map.get(SharedMonsterAttributes.ATTACK_DAMAGE.getName())) {
								if (modifier.getAmount() > 0) {
									return new Object[]{true};
								}
							}
						}
					}

					if (hit != null && hit.typeOfHit == RayTraceResult.Type.BLOCK) {
						Block block = access.getWorld().getBlockState(hit.getBlockPos()).getBlock();
						if (block.canHarvestBlock(access.getWorld(), hit.getBlockPos(), player)) {
							return new Object[]{true};
						}
					}

					return new Object[]{false};
				} finally {
					player.unload(access);
				}
			}
		});
	}
	//endregion

	public RayTraceResult findHit(EnumFacing facing, double range) {
		Vec3d origin = new Vec3d(player.posX, player.posY, player.posZ);
		Vec3d blockCenter = origin.addVector(
			facing.getFrontOffsetX() * 0.51,
			facing.getFrontOffsetY() * 0.51,
			facing.getFrontOffsetZ() * 0.51
		);
		Vec3d target = blockCenter.addVector(
			facing.getFrontOffsetX() * range,
			facing.getFrontOffsetY() * range,
			facing.getFrontOffsetZ() * range
		);

		RayTraceResult hit = player.getEntityWorld().rayTraceBlocks(origin, target);
		Pair<Entity, Vec3d> pair = WorldUtil.rayTraceEntities(player.getEntityWorld(), origin, target, 1.1);
		Entity entity = pair == null ? null : pair.getLeft();

		if (entity instanceof EntityLivingBase && (hit == null || player.getDistanceSq(hit.getBlockPos()) > player.getDistanceSqToEntity(entity))) {
			return new RayTraceResult(entity);
		} else {
			return hit;
		}
	}

	@Override
	public void attach(@Nonnull IComputerAccess computer) {
	}

	@Override
	public void detach(@Nonnull IComputerAccess computer) {
	}

	@Override
	public boolean equals(Object other) {
		return other == this || (other instanceof ToolManipulatorPeripheral && access.equals(((ToolManipulatorPeripheral) other).access));
	}

	@Override
	public int hashCode() {
		return access.hashCode();
	}

	@Override
	public boolean equals(IPeripheral other) {
		return equals((Object) other);
	}

	public static Object[] toObjectArray(TurtleCommandResult result) {
		if (result.isSuccess()) {
			Object[] resultVals = result.getResults();
			if (resultVals == null) {
				return new Object[]{true};
			} else {
				Object[] returnVals = new Object[resultVals.length + 1];
				returnVals[0] = true;
				System.arraycopy(resultVals, 0, returnVals, 1, resultVals.length);
				return returnVals;
			}
		} else {
			String message = result.getErrorMessage();
			return message == null ? new Object[]{false} : new Object[]{false, result.getErrorMessage()};
		}
	}
}
