package org.squiddev.cctweaks.api.network;

import net.minecraft.tileentity.TileEntity;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Provide a custom way to get a node.
 *
 * This can be used for blocks that do not implement {@link INetworkNode}. They should be registered
 * with {@link INetworkRegistry#addNodeProvider(INetworkNodeProvider)}.
 */
public interface INetworkNodeProvider {
	/**
	 * Get the network node for the specific TileEntity
	 *
	 * @param tile The entity to get the node for
	 * @return The node or {@code null} if it cannot be converted
	 */
	@Nullable
	IWorldNetworkNode getNode(@Nonnull TileEntity tile);

	/**
	 * Checks if this TileEntity is a network node
	 *
	 * @param tile The entity to check
	 * @return True if this can be converted into a node
	 */
	boolean isNode(@Nonnull TileEntity tile);
}
