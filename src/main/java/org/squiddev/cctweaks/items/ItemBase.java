package org.squiddev.cctweaks.items;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.common.registry.GameRegistry;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.squiddev.cctweaks.CCTweaks;
import org.squiddev.cctweaks.core.registry.IClientModule;
import org.squiddev.cctweaks.core.utils.Helpers;

public abstract class ItemBase extends Item implements IClientModule {
	protected final String name;

	public ItemBase(String itemName, int stackSize) {
		name = itemName;

		setUnlocalizedName(CCTweaks.ID + "." + name);

		setCreativeTab(CCTweaks.getCreativeTab());
		setMaxStackSize(stackSize);
	}

	public ItemBase(String itemName) {
		this(itemName, 64);
	}

	public static NBTTagCompound getTag(ItemStack stack) {
		NBTTagCompound tag = stack.getTagCompound();
		if (tag == null) stack.setTagCompound(tag = new NBTTagCompound());
		return tag;
	}

	@Override
	public boolean canLoad() {
		return true;
	}

	@Override
	public void preInit() {
		GameRegistry.register(this, new ResourceLocation(CCTweaks.ID, name));
	}

	@Override
	public void init() {
	}

	@Override
	public void postInit() {
	}

	@Override
	@SideOnly(Side.CLIENT)
	public void clientPreInit() {
		Helpers.setupModel(this, 0, name);
	}

	@Override
	@SideOnly(Side.CLIENT)
	public void clientInit() {
	}
}
