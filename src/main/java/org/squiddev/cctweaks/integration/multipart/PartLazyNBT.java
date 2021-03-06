package org.squiddev.cctweaks.integration.multipart;

import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagCompound;
import org.squiddev.cctweaks.core.McEvents;

/**
 * Multipart equivalent of {@link org.squiddev.cctweaks.blocks.TileLazyNBT}
 */
public abstract class PartLazyNBT extends PartBase {
	private NBTTagCompound lazyTag;

	/**
	 * Lazy load the NBT tag
	 *
	 * @param tag The NBT tag to load
	 */
	public abstract void readLazyNBT(NBTTagCompound tag);

	/**
	 * The fields that the tag stores.
	 *
	 * Used in the rare case that we are saving without having had an update tick
	 *
	 * @return The list of fields to keep
	 */
	public abstract Iterable<String> getFields();

	@Override
	public void onLoaded() {
		super.onLoaded();
		if (!getWorld().isRemote) {
			McEvents.schedule(new Runnable() {
				@Override
				public void run() {
					if (lazyTag != null) {
						readLazyNBT(lazyTag);
						lazyTag = null;
					}
				}
			});
		}
	}

	@Override
	public void readFromNBT(NBTTagCompound tag) {
		super.readFromNBT(tag);
		if (getWorld() == null) {
			lazyTag = tag;
		} else {
			readLazyNBT(tag);
		}
	}

	@Override
	public NBTTagCompound writeToNBT(NBTTagCompound tag) {
		super.writeToNBT(tag);
		if (lazyTag != null) {
			for (String field : getFields()) {
				NBTBase fieldTag = lazyTag.getTag(field);
				if (fieldTag != null) tag.setTag(field, fieldTag);
			}
		}
		return tag;
	}
}
