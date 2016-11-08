package org.squiddev.cctweaks.client.render;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.VertexBuffer;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import org.lwjgl.opengl.GL11;
import org.squiddev.cctweaks.core.Config;
import org.squiddev.cctweaks.core.registry.IClientModule;
import org.squiddev.cctweaks.core.registry.Module;
import org.squiddev.cctweaks.core.registry.Registry;
import org.squiddev.cctweaks.core.visualiser.VisualisationData;

import java.awt.*;
import java.util.HashSet;
import java.util.Set;

import static org.squiddev.cctweaks.core.visualiser.VisualisationData.Connection;
import static org.squiddev.cctweaks.core.visualiser.VisualisationData.Node;

/**
 * This is a helper to render a network when testing.
 */
public final class RenderNetworkOverlay extends Module implements IClientModule {
	private int ticksInGame;
	public static VisualisationData data;

	@SubscribeEvent
	public void onWorldRenderLast(RenderWorldLastEvent event) {
		++ticksInGame;
		if (data == null) return;

		Minecraft minecraft = Minecraft.getMinecraft();
		ItemStack stack = minecraft.thePlayer.getHeldItemMainhand();
		ItemStack otherStack = minecraft.thePlayer.getHeldItemOffhand();

		if (
			(stack == null || stack.getItem() != Registry.itemDebugger) &&
				(otherStack == null || otherStack.getItem() != Registry.itemDebugger)) {
			return;
		}

		GlStateManager.pushMatrix();
		RenderManager renderManager = minecraft.getRenderManager();
		GlStateManager.translate(-renderManager.viewerPosX, -renderManager.viewerPosY, -renderManager.viewerPosZ);

		GlStateManager.disableDepth();
		GlStateManager.disableTexture2D();
		GlStateManager.enableBlend();
		GlStateManager.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);

		renderNetworkConnections(data, new Color(Color.HSBtoRGB(ticksInGame % 200 / 200F, 0.6F, 1F)), 1f);
		renderNetworkLabels(data);

		GlStateManager.enableDepth();
		GlStateManager.enableTexture2D();
		GlStateManager.disableBlend();
		GlStateManager.popMatrix();
	}

	private void renderNetworkConnections(VisualisationData data, Color color, float thickness) {
		renderConnections(data, color, 1.0f, thickness);
		renderConnections(data, color, 64.0f / 255.0f, thickness * 3);
	}

	private void renderNetworkLabels(VisualisationData data) {
		Set<Node> nodes = new HashSet<Node>();

		// Custom handling for networks with 0 connections (single node networks)
		RayTraceResult position = Minecraft.getMinecraft().objectMouseOver;
		if (position.typeOfHit == RayTraceResult.Type.BLOCK) {
			for (Connection connection : data.connections) {
				Node a = connection.x, b = connection.y;

				// We render a label of all nodes at this point.
				if (position.typeOfHit == RayTraceResult.Type.BLOCK) {
					if (a.position != null) {
						if (a.position.equals(position.getBlockPos())) {
							nodes.add(a);
							if (b.position == null) nodes.add(b);
						}
					}

					if (b.position != null) {
						if (b.position.equals(position.getBlockPos())) {
							if (a.position == null) nodes.add(a);
							nodes.add(b);
						}
					}
				}
			}

			if (data.connections.length == 0) {
				for (Node node : data.nodes) {
					net.minecraft.util.math.BlockPos nodePosition = node.position;
					if (nodePosition != null && nodePosition.equals(position.getBlockPos())) {
						nodes.add(node);
					}
				}
			}

			int counter = 0;
			for (Node node : nodes) {
				BlockPos pos = position.getBlockPos();

				String name = node.position == null ? "\u00a78" + node.name : node.name;
				renderLabel(pos.getX() + 0.5, pos.getY() + 1.5 + (counter++) * 0.4, pos.getZ() + 0.5, name);
			}
		}
	}

	private void renderConnections(VisualisationData data, Color color, float alpha, float thickness) {
		Tessellator tessellator = Tessellator.getInstance();
		VertexBuffer renderer = tessellator.getBuffer();

		GlStateManager.pushMatrix();
		GlStateManager.scale(1, 1, 1);

		GlStateManager.color(color.getRed() / 255.0f, color.getGreen() / 255.0f, color.getBlue() / 255.0f, alpha);
		GL11.glLineWidth(thickness);

		renderer.begin(GL11.GL_LINES, DefaultVertexFormats.POSITION);
		for (Connection connection : data.connections) {
			net.minecraft.util.math.BlockPos a = connection.x.position, b = connection.y.position;

			if (a != null && b != null) {
				renderer.pos(a.getX() + 0.5, a.getY() + 0.5, a.getZ() + 0.5).endVertex();
				renderer.pos(b.getX() + 0.5, b.getY() + 0.5, b.getZ() + 0.5).endVertex();
			}
		}

		tessellator.draw();

		GlStateManager.popMatrix();
	}

	private void renderLabel(double x, double y, double z, String label) {
		RenderManager renderManager = Minecraft.getMinecraft().getRenderManager();
		FontRenderer fontrenderer = renderManager.getFontRenderer();
		if (fontrenderer == null) return;

		GlStateManager.pushMatrix();
		GlStateManager.disableLighting();

		float scale = 0.02666667f;
		GlStateManager.translate(x, y, z);
		GlStateManager.rotate(-renderManager.playerViewY, 0, 1, 0);
		GlStateManager.rotate(renderManager.playerViewX, 1, 0, 0);
		GlStateManager.scale(-scale, -scale, scale);

		// Render label background
		Tessellator tessellator = Tessellator.getInstance();
		VertexBuffer renderer = tessellator.getBuffer();

		int width = fontrenderer.getStringWidth(label);
		int xOffset = width / 2;

		GlStateManager.disableTexture2D();
		GlStateManager.color(0, 0, 0, 65 / 225.0f);

		renderer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION);
		renderer.pos(-xOffset - 1, -1, 0).endVertex();
		renderer.pos(-xOffset - 1, 8, 0).endVertex();
		renderer.pos(xOffset + 1, 8, 0).endVertex();
		renderer.pos(xOffset + 1, -1, 0).endVertex();

		tessellator.draw();
		GlStateManager.enableTexture2D();

		// Render label
		fontrenderer.drawString(label, -width / 2, 0, 0xFFFFFFFF);

		GlStateManager.enableLighting();
		GlStateManager.popMatrix();
	}

	@Override
	public boolean canLoad() {
		return super.canLoad() && Config.Computer.debugWandEnabled;
	}

	@Override
	public void clientInit() {
		MinecraftForge.EVENT_BUS.register(this);
	}
}
