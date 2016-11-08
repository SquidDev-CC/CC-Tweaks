package org.squiddev.cctweaks.client.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraftforge.fml.client.IModGuiFactory;

import java.util.Set;

public class GuiConfigFactory implements IModGuiFactory {
	@Override
	public void initialize(Minecraft minecraft) {
	}

	@Override
	public Class<? extends GuiScreen> mainConfigGuiClass() {
		return GuiConfigCCTweaks.class;
	}

	@Override
	public Set<RuntimeOptionCategoryElement> runtimeGuiCategories() {
		return null;
	}

	@Override
	@SuppressWarnings("deprecation")
	public RuntimeOptionGuiHandler getHandlerFor(RuntimeOptionCategoryElement runtimeOptionCategoryElement) {
		return null;
	}
}
