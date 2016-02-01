package org.squiddev.cctweaks.core.patch;

import dan200.computercraft.ComputerCraft;
import dan200.computercraft.shared.peripheral.PeripheralType;
import dan200.computercraft.shared.peripheral.common.ItemCable;
import dan200.computercraft.shared.peripheral.modem.TileCable;
import mcmultipart.multipart.IMultipart;
import mcmultipart.multipart.MultipartHelper;
import net.minecraft.block.Block;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.BlockPos;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.Vec3;
import net.minecraft.world.World;
import org.squiddev.cctweaks.integration.multipart.network.PartCable;
import org.squiddev.cctweaks.integration.multipart.network.PartModem;
import org.squiddev.patcher.visitors.MergeVisitor;

public class ItemCable_Patch extends ItemCable {
	public ItemCable_Patch(Block block) {
		super(block);
	}

	public IMultipart newPart(ItemStack stack, EnumFacing facing) {
		switch (getPeripheralType(stack)) {
			case Cable:
				return new PartCable();
			case WiredModem:
				return new PartModem(facing.getOpposite());
		}

		return null;
	}

	@Override
	public boolean onItemUse(ItemStack stack, EntityPlayer player, World world, BlockPos pos, EnumFacing side, float hitX, float hitY, float hitZ) {
		Block block = world.getBlockState(pos).getBlock();

		// We can always place in an air block or a cable block that doesn't have a modem already.
		if (block.isAir(world, pos) && nativePlace(stack, player, world, pos, side, hitX, hitY, hitZ)) {
			return true;
		}
		if (block == ComputerCraft.Blocks.cable) {
			TileCable cable = (TileCable) world.getTileEntity(pos);
			PeripheralType type = cable.getPeripheralType();
			PeripheralType stackType = getPeripheralType(stack);
			if ((type == PeripheralType.Cable && stackType == PeripheralType.WiredModem)
				&& nativePlace(stack, player, world, pos, side, hitX, hitY, hitZ)) {
				return true;
			} else if ((type == PeripheralType.WiredModem && stackType == PeripheralType.Cable)
				&& nativePlace(stack, player, world, pos, side, hitX, hitY, hitZ)) {
				return true;
			}
		}

		Vec3 hit = new Vec3(hitX, hitY, hitZ);
		double depth = (hit.xCoord * 2.0D - 1.0D) * (double) side.getFrontOffsetX() + (hit.yCoord * 2.0D - 1.0D) * (double) side.getFrontOffsetY() + (hit.zCoord * 2.0D - 1.0D) * (double) side.getFrontOffsetZ();
		return (depth < 1.0D && place(stack, player, world, pos, side))
			|| nativePlace(stack, player, world, pos, side, hitX, hitY, hitZ)
			|| place(stack, player, world, pos.offset(side), side);
	}

	/**
	 * The original {@link ItemCable#onItemUse(ItemStack, EntityPlayer, World, BlockPos, EnumFacing, float, float, float)} method
	 * We keep this to ensure cables can be placed in existing modem blocks
	 *
	 * @return Success at placing the cable
	 */
	@MergeVisitor.Stub
	@MergeVisitor.Rename(from = {"onItemUse", "func_180614_a"})
	public boolean nativePlace(ItemStack stack, EntityPlayer player, World world, BlockPos pos, EnumFacing side, float hitX, float hitY, float hitZ) {
		return false;
	}

	/**
	 * Place a multipart into the world
	 *
	 * @return Success at placing the part
	 */
	public boolean place(ItemStack item, EntityPlayer player, World world, BlockPos pos, EnumFacing side) {
		IMultipart part = newPart(item, side);

		if (part == null || !MultipartHelper.canAddPart(world, pos, part)) return false;
		if (!world.isRemote) MultipartHelper.addPart(world, pos, part);
		if (!player.capabilities.isCreativeMode) item.stackSize--;

		return true;
	}
}