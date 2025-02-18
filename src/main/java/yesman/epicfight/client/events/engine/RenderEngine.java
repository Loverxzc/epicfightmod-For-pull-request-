package yesman.epicfight.client.events.engine;

import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import com.google.common.collect.Maps;
import com.mojang.blaze3d.platform.Window;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Camera;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiComponent;
import net.minecraft.client.gui.components.BossHealthOverlay;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobType;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ShieldItem;
import net.minecraft.world.item.TridentItem;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.EntityViewRenderEvent.CameraSetup;
import net.minecraftforge.client.event.EntityViewRenderEvent.RenderFogEvent;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.client.event.RenderHandEvent;
import net.minecraftforge.client.event.RenderLevelLastEvent;
import net.minecraftforge.client.event.RenderLivingEvent;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModLoader;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;
import yesman.epicfight.api.client.forgeevent.PatchedRenderersEvent;
import yesman.epicfight.api.client.forgeevent.RenderEnderDragonEvent;
import yesman.epicfight.api.utils.math.OpenMatrix4f;
import yesman.epicfight.api.utils.math.Vec3f;
import yesman.epicfight.client.ClientEngine;
import yesman.epicfight.client.gui.BattleModeGui;
import yesman.epicfight.client.gui.EntityIndicator;
import yesman.epicfight.client.gui.screen.overlay.OverlayManager;
import yesman.epicfight.client.input.EpicFightKeyMappings;
import yesman.epicfight.client.renderer.AimHelperRenderer;
import yesman.epicfight.client.renderer.EpicFightRenderTypes;
import yesman.epicfight.client.renderer.FirstPersonRenderer;
import yesman.epicfight.client.renderer.patched.entity.PCreeperRenderer;
import yesman.epicfight.client.renderer.patched.entity.PDrownedRenderer;
import yesman.epicfight.client.renderer.patched.entity.PEnderDragonRenderer;
import yesman.epicfight.client.renderer.patched.entity.PEndermanRenderer;
import yesman.epicfight.client.renderer.patched.entity.PHoglinRenderer;
import yesman.epicfight.client.renderer.patched.entity.PHumanoidRenderer;
import yesman.epicfight.client.renderer.patched.entity.PIllagerRenderer;
import yesman.epicfight.client.renderer.patched.entity.PIronGolemRenderer;
import yesman.epicfight.client.renderer.patched.entity.PPlayerRenderer;
import yesman.epicfight.client.renderer.patched.entity.PRavagerRenderer;
import yesman.epicfight.client.renderer.patched.entity.PSpiderRenderer;
import yesman.epicfight.client.renderer.patched.entity.PStrayRenderer;
import yesman.epicfight.client.renderer.patched.entity.PVexRenderer;
import yesman.epicfight.client.renderer.patched.entity.PVindicatorRenderer;
import yesman.epicfight.client.renderer.patched.entity.PWitchRenderer;
import yesman.epicfight.client.renderer.patched.entity.PWitherRenderer;
import yesman.epicfight.client.renderer.patched.entity.PWitherSkeletonMinionRenderer;
import yesman.epicfight.client.renderer.patched.entity.PZombieVillagerRenderer;
import yesman.epicfight.client.renderer.patched.entity.PatchedEntityRenderer;
import yesman.epicfight.client.renderer.patched.entity.WitherGhostCloneRenderer;
import yesman.epicfight.client.renderer.patched.item.RenderBow;
import yesman.epicfight.client.renderer.patched.item.RenderCrossbow;
import yesman.epicfight.client.renderer.patched.item.RenderItemBase;
import yesman.epicfight.client.renderer.patched.item.RenderKatana;
import yesman.epicfight.client.renderer.patched.item.RenderShield;
import yesman.epicfight.client.renderer.patched.item.RenderTrident;
import yesman.epicfight.client.world.capabilites.entitypatch.player.LocalPlayerPatch;
import yesman.epicfight.main.EpicFightMod;
import yesman.epicfight.skill.SkillContainer;
import yesman.epicfight.world.capabilities.EpicFightCapabilities;
import yesman.epicfight.world.capabilities.entitypatch.LivingEntityPatch;
import yesman.epicfight.world.capabilities.entitypatch.boss.enderdragon.EnderDragonPatch;
import yesman.epicfight.world.capabilities.item.CapabilityItem;
import yesman.epicfight.world.entity.EpicFightEntities;
import yesman.epicfight.world.gamerule.EpicFightGamerules;
import yesman.epicfight.world.item.EpicFightItems;

@SuppressWarnings("rawtypes")
@OnlyIn(Dist.CLIENT)
public class RenderEngine {
	private static final Vec3f AIMING_CORRECTION = new Vec3f(-1.5F, 0.0F, 1.25F);
	
	public AimHelperRenderer aimHelper;
	public BattleModeGui guiSkillBar = new BattleModeGui(Minecraft.getInstance());
	private Minecraft minecraft;
	private Map<EntityType<?>, Supplier<PatchedEntityRenderer>> entityRendererProvider;
	private Map<EntityType<?>, PatchedEntityRenderer> entityRendererCache;
	private Map<Item, RenderItemBase> itemRendererMapByInstance;
	private Map<Class<? extends Item>, RenderItemBase> itemRendererMapByClass;
	private FirstPersonRenderer firstPersonRenderer;
	private OverlayManager overlayManager;
	private boolean aiming;
	private int zoomOutTimer = 0;
	private int zoomCount;
	private int zoomMaxCount = 20;
	
	public RenderEngine() {
		Events.renderEngine = this;
		RenderItemBase.renderEngine = this;
		EntityIndicator.init();
		this.minecraft = Minecraft.getInstance();
		this.entityRendererProvider = Maps.newHashMap();
		this.entityRendererCache = Maps.newHashMap();
		this.itemRendererMapByInstance = Maps.newHashMap();
		this.itemRendererMapByClass = Maps.newHashMap();
		this.firstPersonRenderer = new FirstPersonRenderer();
		this.overlayManager = new OverlayManager();
		this.minecraft.renderBuffers().fixedBuffers.put(EpicFightRenderTypes.enchantedAnimatedArmor(), new BufferBuilder(EpicFightRenderTypes.enchantedAnimatedArmor().bufferSize()));
	}
	
	public void registerRenderer() {
		this.entityRendererProvider.put(EntityType.CREEPER, PCreeperRenderer::new);
		this.entityRendererProvider.put(EntityType.ENDERMAN, PEndermanRenderer::new);
		this.entityRendererProvider.put(EntityType.ZOMBIE, PHumanoidRenderer::new);
		this.entityRendererProvider.put(EntityType.ZOMBIE_VILLAGER, PZombieVillagerRenderer::new);
		this.entityRendererProvider.put(EntityType.ZOMBIFIED_PIGLIN, PHumanoidRenderer::new);
		this.entityRendererProvider.put(EntityType.HUSK, PHumanoidRenderer::new);
		this.entityRendererProvider.put(EntityType.SKELETON, PHumanoidRenderer::new);
		this.entityRendererProvider.put(EntityType.WITHER_SKELETON, PHumanoidRenderer::new);
		this.entityRendererProvider.put(EntityType.STRAY, PStrayRenderer::new);
		this.entityRendererProvider.put(EntityType.PLAYER, PPlayerRenderer::new);
		this.entityRendererProvider.put(EntityType.SPIDER, PSpiderRenderer::new);
		this.entityRendererProvider.put(EntityType.CAVE_SPIDER, PSpiderRenderer::new);
		this.entityRendererProvider.put(EntityType.IRON_GOLEM, PIronGolemRenderer::new);
		this.entityRendererProvider.put(EntityType.VINDICATOR, PVindicatorRenderer::new);
		this.entityRendererProvider.put(EntityType.EVOKER, PIllagerRenderer::new);
		this.entityRendererProvider.put(EntityType.WITCH, PWitchRenderer::new);
		this.entityRendererProvider.put(EntityType.DROWNED, PDrownedRenderer::new);
		this.entityRendererProvider.put(EntityType.PILLAGER, PIllagerRenderer::new);
		this.entityRendererProvider.put(EntityType.RAVAGER, PRavagerRenderer::new);
		this.entityRendererProvider.put(EntityType.VEX, PVexRenderer::new);
		this.entityRendererProvider.put(EntityType.PIGLIN, PHumanoidRenderer::new);
		this.entityRendererProvider.put(EntityType.PIGLIN_BRUTE, PHumanoidRenderer::new);
		this.entityRendererProvider.put(EntityType.HOGLIN, PHoglinRenderer::new);
		this.entityRendererProvider.put(EntityType.ZOGLIN, PHoglinRenderer::new);
		this.entityRendererProvider.put(EntityType.ENDER_DRAGON, PEnderDragonRenderer::new);
		this.entityRendererProvider.put(EntityType.WITHER, PWitherRenderer::new);
		this.entityRendererProvider.put(EpicFightEntities.WITHER_SKELETON_MINION.get(), () -> new PWitherSkeletonMinionRenderer().setOverridingTexture("epicfight:textures/entity/wither_skeleton_minion.png"));
		this.entityRendererProvider.put(EpicFightEntities.WITHER_GHOST_CLONE.get(), WitherGhostCloneRenderer::new);
		
		RenderBow bowRenderer = new RenderBow();
		RenderCrossbow crossbowRenderer = new RenderCrossbow();
		RenderShield shieldRenderer = new RenderShield();
		RenderTrident tridentRenderer = new RenderTrident();
		
		this.itemRendererMapByInstance.clear();
		this.itemRendererMapByInstance.put(Items.AIR, new RenderItemBase());
		this.itemRendererMapByInstance.put(Items.BOW, bowRenderer);
		this.itemRendererMapByInstance.put(Items.SHIELD, shieldRenderer);
		this.itemRendererMapByInstance.put(Items.CROSSBOW, crossbowRenderer);
		this.itemRendererMapByInstance.put(Items.TRIDENT, tridentRenderer);
		this.itemRendererMapByInstance.put(EpicFightItems.KATANA.get(), new RenderKatana());
		this.itemRendererMapByClass.put(BowItem.class, bowRenderer);
		this.itemRendererMapByClass.put(CrossbowItem.class, crossbowRenderer);
		this.itemRendererMapByClass.put(ShieldItem.class, shieldRenderer);
		this.itemRendererMapByClass.put(TridentItem.class, tridentRenderer);
		this.aimHelper = new AimHelperRenderer();
		
		ModLoader.get().postEvent(new PatchedRenderersEvent.Add(this.entityRendererProvider, this.itemRendererMapByInstance));
		
		for (Map.Entry<EntityType<?>, Supplier<PatchedEntityRenderer>> entry : this.entityRendererProvider.entrySet()) {
			this.entityRendererCache.put(entry.getKey(), entry.getValue().get());
		}
		
		ModLoader.get().postEvent(new PatchedRenderersEvent.Modify(this.entityRendererCache));
	}
	
	public void registerCustomEntityRenderer(EntityType<?> entityType, String renderer) {
		if (renderer == "") {
			return;
		}
		
		EntityType<?> presetEntityType = ForgeRegistries.ENTITIES.getValue(new ResourceLocation(renderer));
		
		if (this.entityRendererProvider.containsKey(presetEntityType)) {
			this.entityRendererCache.put(entityType, this.entityRendererProvider.get(presetEntityType).get());
			return;
		}
		
		throw new IllegalArgumentException("Datapack Mob Patch Crash: Invalid Renderer type " + renderer);
	}
	
	public RenderItemBase getItemRenderer(Item item) {
		RenderItemBase renderItem = this.itemRendererMapByInstance.get(item);
		
		if (renderItem == null) {
			renderItem = this.findMatchingRendererByClass(item.getClass());
			
			if (renderItem == null) {
				renderItem = this.itemRendererMapByInstance.get(Items.AIR);
			}
			
			this.itemRendererMapByInstance.put(item, renderItem);
		}
		
		return renderItem;
	}

	private RenderItemBase findMatchingRendererByClass(Class<?> clazz) {
		RenderItemBase renderer = null;
		
		for (; clazz != null && renderer == null; clazz = clazz.getSuperclass()) {
			renderer = this.itemRendererMapByClass.getOrDefault(clazz, null);
		}
		
		return renderer;
	}
	
	@SuppressWarnings("unchecked")
	public void renderEntityArmatureModel(LivingEntity livingEntity, LivingEntityPatch<?> entitypatch, LivingEntityRenderer<? extends Entity, ?> renderer, MultiBufferSource buffer, PoseStack matStack, int packedLightIn, float partialTicks) {
		this.getEntityRenderer(livingEntity).render(livingEntity, entitypatch, renderer, buffer, matStack, packedLightIn, partialTicks);
	}
	
	public PatchedEntityRenderer getEntityRenderer(Entity entity) {
		return this.entityRendererCache.get(entity.getType());
	}
	
	public boolean hasRendererFor(Entity entity) {
		return this.entityRendererCache.computeIfAbsent(entity.getType(), (key) -> this.entityRendererProvider.containsKey(key) ? this.entityRendererProvider.get(entity.getType()).get() : null) != null;
	}
	
	public void clearCustomEntityRenerer() {
		this.entityRendererCache.clear();
	}
	
	public void zoomIn() {
		this.aiming = true;
		this.zoomCount = this.zoomCount == 0 ? 1 : this.zoomCount;
		this.zoomOutTimer = 0;
	}

	public void zoomOut(int timer) {
		this.aiming = false;
		this.zoomOutTimer = timer;
	}
	
	private void updateCameraInfo(CameraSetup event, CameraType pov, double partialTicks) {
		if (ClientEngine.instance.getPlayerPatch() == null) {
			return;
		}
		
		Camera camera = event.getCamera();
		Entity entity = minecraft.getCameraEntity();
		Vec3 vector = camera.getPosition();
		double totalX = vector.x();
		double totalY = vector.y();
		double totalZ = vector.z();
		
		if (pov == CameraType.THIRD_PERSON_BACK && zoomCount > 0) {
			double posX = vector.x();
			double posY = vector.y();
			double posZ = vector.z();
			double entityPosX = entity.xOld + (entity.getX() - entity.xOld) * partialTicks;
			double entityPosY = entity.yOld + (entity.getY() - entity.yOld) * partialTicks + entity.getEyeHeight();
			double entityPosZ = entity.zOld + (entity.getZ() - entity.zOld) * partialTicks;
			float intpol = pov == CameraType.THIRD_PERSON_BACK ? ((float) zoomCount / (float) zoomMaxCount) : 0;
			Vec3f interpolatedCorrection = new Vec3f(AIMING_CORRECTION.x * intpol, AIMING_CORRECTION.y * intpol, AIMING_CORRECTION.z * intpol);
			OpenMatrix4f rotationMatrix = ClientEngine.instance.getPlayerPatch().getMatrix((float)partialTicks);
			Vec3f rotateVec = OpenMatrix4f.transform3v(rotationMatrix, interpolatedCorrection, null);
			double d3 = Math.sqrt((rotateVec.x * rotateVec.x) + (rotateVec.y * rotateVec.y) + (rotateVec.z * rotateVec.z));
			double smallest = d3;
			double d00 = posX + rotateVec.x;
			double d11 = posY - rotateVec.y;
			double d22 = posZ + rotateVec.z;
			
			for (int i = 0; i < 8; ++i) {
				float f = (float) ((i & 1) * 2 - 1);
				float f1 = (float) ((i >> 1 & 1) * 2 - 1);
				float f2 = (float) ((i >> 2 & 1) * 2 - 1);
				f = f * 0.1F;
				f1 = f1 * 0.1F;
				f2 = f2 * 0.1F;
				HitResult raytraceresult = minecraft.level.clip(new ClipContext(new Vec3(entityPosX + f, entityPosY + f1, entityPosZ + f2), new Vec3(d00 + f + f2, d11 + f1, d22 + f2), ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, entity));
				
				if (raytraceresult != null) {
					double d7 = raytraceresult.getLocation().distanceTo(new Vec3(entityPosX, entityPosY, entityPosZ));
					if (d7 < smallest) {
						smallest = d7;
					}
				}
			}
			
			float dist = d3 == 0 ? 0 : (float) (smallest / d3);
			totalX += rotateVec.x * dist;
			totalY -= rotateVec.y * dist;
			totalZ += rotateVec.z * dist;
		}
		
		camera.setPosition(totalX, totalY, totalZ);
	}
	
	public OverlayManager getOverlayManager() {
		return this.overlayManager;
	}
	
	@Mod.EventBusSubscriber(modid = EpicFightMod.MODID, value = Dist.CLIENT)
	public static class Events {
		static RenderEngine renderEngine;
		
		@SubscribeEvent
		public static void renderLivingEvent(RenderLivingEvent.Pre<? extends LivingEntity, ? extends EntityModel<? extends LivingEntity>> event) {
			LivingEntity livingentity = event.getEntity();
			
			if (renderEngine.hasRendererFor(livingentity)) {
				if (livingentity instanceof LocalPlayer && event.getPartialTick() == 1.0F) {
					return;
				}
				
				LivingEntityPatch<?> entitypatch = (LivingEntityPatch<?>) livingentity.getCapability(EpicFightCapabilities.CAPABILITY_ENTITY, null).orElse(null);
				
				if (entitypatch != null && !entitypatch.shouldSkipRender()) {
					event.setCanceled(true);
					renderEngine.renderEntityArmatureModel(livingentity, entitypatch, event.getRenderer(), event.getMultiBufferSource(), event.getPoseStack(), event.getPackedLight(), event.getPartialTick());
				}
			}
			
			if (!renderEngine.minecraft.options.hideGui && !livingentity.level.getGameRules().getBoolean(EpicFightGamerules.DISABLE_ENTITY_UI)) {
				for (EntityIndicator entityIndicator : EntityIndicator.ENTITY_INDICATOR_RENDERERS) {
					if (entityIndicator.shouldDraw(renderEngine.minecraft.player, event.getEntity())) {
						entityIndicator.drawIndicator(event.getEntity(), event.getPoseStack(), event.getMultiBufferSource(), event.getPartialTick());
					}
				}
			}
		}

		@SubscribeEvent
		public static void itemTooltip(ItemTooltipEvent event) {
			if (event.getPlayer() != null) {
				CapabilityItem cap = EpicFightCapabilities.getItemStackCapability(event.getItemStack());
				LocalPlayerPatch playerpatch = (LocalPlayerPatch) event.getPlayer().getCapability(EpicFightCapabilities.CAPABILITY_ENTITY, null).orElse(null);
				
				if (cap != null && playerpatch != null) {
					if (ClientEngine.instance.inputController.isKeyDown(EpicFightKeyMappings.SPECIAL_SKILL_TOOLTIP)) {
						if (cap.getSpecialAttack(playerpatch) != null) {
							event.getToolTip().clear();
							List<Component> skilltooltip = cap.getSpecialAttack(playerpatch).getTooltipOnItem(event.getItemStack(), cap, playerpatch);
							
							for (Component s : skilltooltip) {
								event.getToolTip().add(s);
							}
						}
					} else {
						List<Component> tooltip = event.getToolTip();
						cap.modifyItemTooltip(event.getItemStack(), event.getToolTip(), playerpatch);
						
						for (int i = 0; i < tooltip.size(); i++) {
							Component textComp = tooltip.get(i);
							
							if (textComp.getSiblings().size() > 0) {
								Component sibling = textComp.getSiblings().get(0);
								
								if (sibling instanceof TranslatableComponent) {
									TranslatableComponent translationComponent = (TranslatableComponent)sibling;
									
									if (translationComponent.getArgs().length > 1 && translationComponent.getArgs()[1] instanceof TranslatableComponent) {
										CapabilityItem itemCapability = EpicFightCapabilities.getItemStackCapability(event.getItemStack());
										
										if (((TranslatableComponent)translationComponent.getArgs()[1]).getKey().equals(Attributes.ATTACK_SPEED.getDescriptionId())) {
											float weaponSpeed = (float)playerpatch.getOriginal().getAttribute(Attributes.ATTACK_SPEED).getBaseValue();
											
											for (AttributeModifier modifier : event.getItemStack().getAttributeModifiers(EquipmentSlot.MAINHAND).get(Attributes.ATTACK_SPEED)) {
												weaponSpeed += modifier.getAmount();
											}
											
											if (itemCapability != null) {
												for (AttributeModifier modifier : itemCapability.getAttributeModifiers(EquipmentSlot.MAINHAND, playerpatch).get(Attributes.ATTACK_SPEED)) {
													weaponSpeed += modifier.getAmount();
												}
											}
											
											tooltip.remove(i);
											tooltip.add(i, new TextComponent(String.format(" %.2f ", playerpatch.getAttackSpeed(cap, weaponSpeed))).append(new TranslatableComponent(Attributes.ATTACK_SPEED.getDescriptionId())));
										} else if (((TranslatableComponent)translationComponent.getArgs()[1]).getKey().equals(Attributes.ATTACK_DAMAGE.getDescriptionId())) {
											float weaponDamage = (float)playerpatch.getOriginal().getAttribute(Attributes.ATTACK_DAMAGE).getBaseValue();
											weaponDamage += EnchantmentHelper.getDamageBonus(event.getItemStack(), MobType.UNDEFINED);
											
											for (AttributeModifier modifier : event.getItemStack().getAttributeModifiers(EquipmentSlot.MAINHAND).get(Attributes.ATTACK_DAMAGE)) {
												weaponDamage += modifier.getAmount();
											}
											
											if (itemCapability != null) {
												
												for (AttributeModifier modifier : itemCapability.getAttributeModifiers(EquipmentSlot.MAINHAND, playerpatch).get(Attributes.ATTACK_DAMAGE)) {
													weaponDamage += modifier.getAmount();
												}
											}
											
											tooltip.remove(i);
											tooltip.add(i, new TextComponent(String.format(" %.0f ", playerpatch.getDamageToEntity(null, null, weaponDamage))).append(new TranslatableComponent(Attributes.ATTACK_DAMAGE.getDescriptionId())).withStyle(ChatFormatting.DARK_GREEN));
										}
									}
								}
							}
						}
					}
				}
			}
		}
		
		@SubscribeEvent
		public static void cameraSetupEvent(CameraSetup event) {
			renderEngine.updateCameraInfo(event, renderEngine.minecraft.options.getCameraType(), event.getPartialTicks());
			
			if (renderEngine.zoomCount > 0) {
				if (renderEngine.zoomOutTimer > 0) {
					renderEngine.zoomOutTimer--;
				} else {
					renderEngine.zoomCount = renderEngine.aiming ? renderEngine.zoomCount + 1 : renderEngine.zoomCount - 1;
				}
				
				renderEngine.zoomCount = Math.min(renderEngine.zoomMaxCount, renderEngine.zoomCount);
			}
		}
		
		@SubscribeEvent
		public static void fogEvent(RenderFogEvent event) {
		}
		
		@SubscribeEvent
		public static void renderGameOverlayPre(RenderGameOverlayEvent.Pre event) {
			if (event.getType() == RenderGameOverlayEvent.ElementType.ALL) {
				Window window = Minecraft.getInstance().getWindow();
				LocalPlayerPatch playerpatch = ClientEngine.instance.getPlayerPatch();
				
				if (playerpatch != null) {
					for (SkillContainer skillContainer : playerpatch.getSkillCapability().skillContainers) {
						if (skillContainer.getSkill() != null) {
							skillContainer.getSkill().onScreen(playerpatch, window.getGuiScaledWidth(), window.getGuiScaledHeight());
						}
					}
					
					renderEngine.overlayManager.renderTick(window.getGuiScaledWidth(), window.getGuiScaledHeight());
					
					if (Minecraft.renderNames()) {
						renderEngine.guiSkillBar.renderGui(playerpatch, event.getPartialTicks());
					}
				}
			}
		}
		
		@SubscribeEvent
		public static void renderGameOverlayPost(RenderGameOverlayEvent.BossInfo event) {
			if (event.getBossEvent().getName().getString().equals("Ender Dragon")) {
				if (EnderDragonPatch.INSTANCE_CLIENT != null) {
					EnderDragonPatch dragonpatch = EnderDragonPatch.INSTANCE_CLIENT;
					float stunShield = dragonpatch.getStunShield();
					
					if (stunShield > 0) {
						float progression = stunShield / dragonpatch.getMaxStunShield();
						int x = event.getX();
						int y = event.getY();
						
						RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
			            RenderSystem.setShaderTexture(0, BossHealthOverlay.GUI_BARS_LOCATION);
						GuiComponent.blit(event.getMatrixStack(), x, y + 6, 183, 2, 0, 45.0F, 182, 6, 255, 255);
						GuiComponent.blit(event.getMatrixStack(), x + (int)(183 * progression), y + 6, (int)(183 * (1.0F - progression)), 2, 0, 39.0F, 182, 6, 255, 255);
					}
				}
			}
		}
		
		@SuppressWarnings("unchecked")
		@SubscribeEvent
		public static void renderHand(RenderHandEvent event) {
			LocalPlayerPatch playerpatch = ClientEngine.instance.getPlayerPatch();
			
			if (playerpatch != null) {
				boolean isBattleMode = playerpatch.isBattleMode();
				
				if (isBattleMode || !EpicFightMod.CLIENT_INGAME_CONFIG.filterAnimation.getValue()) {
					if (event.getHand() == InteractionHand.MAIN_HAND) {
						renderEngine.firstPersonRenderer.render(playerpatch.getOriginal(), playerpatch, (LivingEntityRenderer)renderEngine.minecraft.getEntityRenderDispatcher().getRenderer(playerpatch.getOriginal()),
								event.getMultiBufferSource(), event.getPoseStack(), event.getPackedLight(), event.getPartialTicks());
					}
					
					event.setCanceled(true);
				}
			}
		}
		
		@SubscribeEvent
		public static void renderWorldLast(RenderLevelLastEvent event) {
			if (renderEngine.zoomCount > 0 && renderEngine.minecraft.options.getCameraType() == CameraType.THIRD_PERSON_BACK) {
				renderEngine.aimHelper.doRender(event.getPoseStack(), event.getPartialTick());
			}
		}
		
		@SuppressWarnings("unchecked")
		@SubscribeEvent
		public static void renderEnderDragonEvent(RenderEnderDragonEvent event) {
			EnderDragon livingentity = event.getEntity();
			
			if (renderEngine.hasRendererFor(livingentity)) {
				EnderDragonPatch entitypatch = (EnderDragonPatch) livingentity.getCapability(EpicFightCapabilities.CAPABILITY_ENTITY, null).orElse(null);
				
				if (entitypatch != null) {
					event.setCanceled(true);
					renderEngine.getEntityRenderer(livingentity).render(livingentity, entitypatch, event.getRenderer(), event.getBuffers(), event.getPoseStack(), event.getLight(), event.getPartialRenderTick());
				}
			}
		}
	}
}