package org.squiddev.cctweaks.core.network;

import com.google.common.base.Preconditions;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import org.squiddev.cctweaks.api.IWorldPosition;
import org.squiddev.cctweaks.api.network.INetworkNodeProvider;
import org.squiddev.cctweaks.api.network.INetworkRegistry;
import org.squiddev.cctweaks.api.network.IWorldNetworkNode;
import org.squiddev.cctweaks.api.network.IWorldNetworkNodeHost;
import org.squiddev.cctweaks.core.utils.DebugLogger;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.HashSet;
import java.util.Set;

/**
 * Registry for network components
 */
public final class NetworkRegistry implements INetworkRegistry {
	private final Set<INetworkNodeProvider> providers = new HashSet<INetworkNodeProvider>();

	@Override
	public void addNodeProvider(@Nonnull INetworkNodeProvider provider) {
		Preconditions.checkNotNull(provider, "provider cannot be null");
		providers.add(provider);
	}

	@Override
	public boolean isNode(@Nonnull IBlockAccess world, @Nonnull BlockPos position) {
		Preconditions.checkNotNull(world, "world cannot be null");
		Preconditions.checkNotNull(position, "position cannot be null");
		return position.getY() >= 0 && isNode(world.getTileEntity(position));
	}

	@Override
	public boolean isNode(@Nullable TileEntity tile) {
		if (tile == null) return false;

		if (tile instanceof IWorldNetworkNode || tile instanceof IWorldNetworkNodeHost) return true;

		for (INetworkNodeProvider provider : providers) {
			try {
				if (provider.isNode(tile)) return true;
			} catch (Exception e) {
				DebugLogger.debug("Node provider " + provider + " threw exception", e);
			}
		}

		return false;
	}

	@Override
	public boolean isNode(@Nonnull IWorldPosition position) {
		return isNode(position.getBlockAccess(), position.getPosition());
	}


	@Override
	public IWorldNetworkNode getNode(@Nonnull IBlockAccess world, @Nonnull BlockPos position) {
		return position.getY() >= 0 ? getNode(world.getTileEntity(position)) : null;
	}

	@Override
	public IWorldNetworkNode getNode(@Nullable TileEntity tile) {
		if (tile == null) return null;

		if (tile instanceof IWorldNetworkNode) return (IWorldNetworkNode) tile;
		if (tile instanceof IWorldNetworkNodeHost) return ((IWorldNetworkNodeHost) tile).getNode();

		for (INetworkNodeProvider provider : providers) {
			try {
				IWorldNetworkNode node = provider.getNode(tile);
				if (node != null) return node;
			} catch (Exception e) {
				DebugLogger.debug("Node provider " + provider + " threw exception", e);
			}
		}

		return null;
	}

	@Override
	public IWorldNetworkNode getNode(@Nonnull IWorldPosition position) {
		return getNode(position.getBlockAccess(), position.getPosition());
	}
}
