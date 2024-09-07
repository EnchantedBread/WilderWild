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

package net.frozenblock.wilderwild.entity.render.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import it.unimi.dsi.fastutil.objects.Object2ObjectLinkedOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectMap;
import java.util.function.Supplier;
import net.frozenblock.wilderwild.WWConstants;
import net.frozenblock.wilderwild.entity.Firefly;
import net.frozenblock.wilderwild.entity.render.renderer.state.FireflyRenderState;
import net.frozenblock.wilderwild.entity.variant.FireflyColor;
import net.frozenblock.wilderwild.registry.WilderWildRegistries;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.NotNull;
import org.joml.Quaternionf;

public class FireflyRenderer extends EntityRenderer<Firefly, FireflyRenderState> {
	//CREDIT TO magistermaks ON GITHUB!!

	public static final Object2ObjectMap<ResourceLocation, RenderType> LAYERS = new Object2ObjectLinkedOpenHashMap<>() {{
		Object2ObjectMap<ResourceLocation, ResourceLocation> colors = new Object2ObjectLinkedOpenHashMap<>();
		WilderWildRegistries.FIREFLY_COLOR.forEach(color -> colors.put(color.key(), color.texture()));
		colors.forEach((colorKey, texture) -> put(colorKey, RenderType.entityTranslucentEmissive(texture)));
	}};
	private static final ResourceLocation TEXTURE = WWConstants.id("textures/entity/firefly/firefly_off.png");
	private static final RenderType LAYER = RenderType.entityTranslucent(TEXTURE);
	private static final RenderType NECTAR_LAYER = RenderType.entityTranslucent(WWConstants.id("textures/entity/firefly/nectar.png"));
	private static final RenderType NECTAR_FLAP_LAYER = RenderType.entityTranslucent(WWConstants.id("textures/entity/firefly/nectar_wings_down.png"));
	private static final RenderType NECTAR_OVERLAY = RenderType.entityTranslucentEmissive(WWConstants.id("textures/entity/firefly/nectar_overlay.png"), true);
	private static final float Y_OFFSET = 0.155F;
	private static final Quaternionf QUAT_180 = Axis.YP.rotationDegrees(180F);

	public FireflyRenderer(EntityRendererProvider.Context ctx) {
		super(ctx);
	}

	public static void renderFirefly(@NotNull PoseStack poseStack, @NotNull MultiBufferSource buffer, int packedLight, boolean nectar, int overlay, int age, float calcColor, boolean flickers, FireflyColor color, float scale, float xOffset, float yOffset, float zOffset, Quaternionf rotation) {
		poseStack.pushPose();
		poseStack.scale(scale, scale, scale);
		poseStack.translate(xOffset, yOffset, zOffset);
		poseStack.mulPose(rotation);
		poseStack.mulPose(QUAT_180);

		PoseStack.Pose pose = poseStack.last();
		Supplier<RenderType> nectarLayer = () -> age % 2 == 0 ? NECTAR_LAYER : NECTAR_FLAP_LAYER;
		VertexConsumer vertexConsumer = buffer.getBuffer(nectar ? nectarLayer.get() : LAYER);

		vertexConsumer
			.addVertex(pose, -0.5F, -0.5F, 0F)
			.setColor(1F, 1F, 1F, 1F)
			.setUv(0, 1)
			.setOverlay(overlay)
			.setLight(packedLight)
			.setNormal(pose, 0F, 1F, 0F);
		vertexConsumer
			.addVertex(pose, 0.5F, -0.5F, 0F)
			.setColor(1F, 1F, 1F, 1F)
			.setUv(1, 1)
			.setOverlay(overlay)
			.setLight(packedLight)
			.setNormal(pose, 0F, 1F, 0F);
		vertexConsumer
			.addVertex(pose, 0.5F, 0.5F, 0F)
			.setColor(1F, 1F, 1F, 1F)
			.setUv(1, 0)
			.setOverlay(overlay)
			.setLight(packedLight)
			.setNormal(pose, 0F, 1F, 0F);
		vertexConsumer
			.addVertex(pose, -0.5F, 0.5F, 0F)
			.setColor(1F, 1F, 1F, 1F)
			.setUv(0, 0)
			.setOverlay(overlay)
			.setLight(packedLight)
			.setNormal(pose, 0F, 1F, 0F);

		if (color != null && LAYERS.get(color.key()) != null) {
			RenderType layer = nectar ? NECTAR_OVERLAY : LAYERS.get(color.key());
			vertexConsumer = buffer.getBuffer(layer);
		} else {
			vertexConsumer = buffer.getBuffer(LAYERS.get(FireflyColor.ON.key()));
		}

		vertexConsumer
			.addVertex(pose, -0.5F, -0.5F, 0F)
			.setColor(calcColor, calcColor, calcColor, calcColor)
			.setUv(0, 1)
			.setOverlay(overlay)
			.setLight(packedLight)
			.setNormal(pose, 0F, 1F, 0F);
		vertexConsumer
			.addVertex(pose, 0.5F, -0.5F, 0F)
			.setColor(calcColor, calcColor, calcColor, calcColor)
			.setUv(1, 1)
			.setOverlay(overlay)
			.setLight(packedLight)
			.setNormal(pose, 0F, 1F, 0F);
		vertexConsumer
			.addVertex(pose, 0.5F, 0.5F, 0F)
			.setColor(calcColor, calcColor, calcColor, calcColor)
			.setUv(1, 0)
			.setOverlay(overlay)
			.setLight(packedLight)
			.setNormal(pose, 0F, 1F, 0F);
		vertexConsumer
			.addVertex(pose, -0.5F, 0.5F, 0F)
			.setColor(calcColor, calcColor, calcColor, calcColor)
			.setUv(0, 0)
			.setOverlay(overlay)
			.setLight(packedLight)
			.setNormal(pose, 0F, 1F, 0F);

		poseStack.popPose();
	}

	public static int getOverlay(@NotNull Firefly entity, float whiteOverlayProgress) {
		return OverlayTexture.pack(OverlayTexture.u(whiteOverlayProgress), OverlayTexture.v(entity.hurtTime > 0 || entity.deathTime > 0));
	}

	@Override
	public void render(FireflyRenderState renderState, PoseStack poseStack, MultiBufferSource buffer, int light) {
		boolean nectar = false;

		Component component = renderState.customName;
		if (component != null) {
			nectar = component.getString().toLowerCase().contains("nectar");
		}

		float scale = renderState.animScale;

		int overlay = renderState.overlay;

		int age = renderState.flickerAge;
		boolean flickers = renderState.flickers;


		poseStack.pushPose();
		float f = renderState.scale;
		poseStack.scale(f, f, f);
		renderFirefly(poseStack, buffer, light, nectar, overlay, age, renderState.calcColor, flickers, renderState.color, scale, 0F, Y_OFFSET, 0F, this.entityRenderDispatcher.cameraOrientation());

		if (renderState.shouldShowName) {
			this.renderNameTag(renderState, renderState.nameTag, poseStack, buffer, light);
		}
		poseStack.popPose();
	}

	@Override
	@NotNull
	public ResourceLocation getTextureLocation(@NotNull FireflyRenderState renderState) {
		return TEXTURE;
	}

	@Override
	@NotNull
	public FireflyRenderState createRenderState() {
		return new FireflyRenderState();
	}

	@Override
	public void extractRenderState(Firefly entity, FireflyRenderState renderState, float partialTick) {
		super.extractRenderState(entity, renderState, partialTick);

		renderState.overlay = getOverlay(entity, 0);
		renderState.flickerAge = entity.getFlickerAge();
		renderState.flickers = entity.flickers();

		float prevAnimScale = entity.getPrevAnimScale();
		renderState.animScale = prevAnimScale + (partialTick * (entity.getAnimScale() - prevAnimScale));
		renderState.color = entity.getColor();
		renderState.calcColor = (float) (renderState.flickers ?
			(((renderState.flickerAge + partialTick) * Mth.PI) * -4F) / 255F :
			Math.max(((Math.cos(((renderState.flickerAge + prevAnimScale) * Mth.PI) * 0.05F))), 0F)
		);

		renderState.shouldShowName = this.shouldShowName(entity, renderState.distanceToCameraSq);
	}
}
