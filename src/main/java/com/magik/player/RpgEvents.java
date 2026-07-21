package com.magik.player;

import com.magik.MagikMod;
import com.magik.combat.RpgAttributeApplier;
import com.magik.item.RpgGear;
import com.magik.item.RpgShieldItem;
import com.magik.network.MagikNetwork;
import com.magik.skills.SkillCasting;
import com.magik.skills.SkillFx;
import com.magik.skills.SkillTrees;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.capabilities.RegisterCapabilitiesEvent;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Lifecycle management for the {@link PlayerRpg} capability:
 * attachment, death persistence, login/respawn synchronization and the
 * per-tick mana/stamina regeneration loop.
 */
@Mod.EventBusSubscriber(modid = MagikMod.MOD_ID)
public final class RpgEvents {

    /** How often (in ticks) changed vitals are pushed to the client. */
    private static final int VITALS_SYNC_INTERVAL = 10;

    private RpgEvents() {
    }

    @Mod.EventBusSubscriber(modid = MagikMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
    public static final class ModBus {
        @SubscribeEvent
        public static void onRegisterCapabilities(RegisterCapabilitiesEvent event) {
            event.register(PlayerRpg.class);
        }
    }

    @SubscribeEvent
    public static void onAttachCapabilities(AttachCapabilitiesEvent<Entity> event) {
        if (event.getObject() instanceof Player) {
            event.addCapability(MagikMod.id("rpg"), new PlayerRpgProvider());
        }
    }

    /** Keeps all RPG progress across death and returning from the End. */
    @SubscribeEvent
    public static void onPlayerClone(PlayerEvent.Clone event) {
        event.getOriginal().reviveCaps();
        PlayerRpgProvider.get(event.getOriginal()).ifPresent(oldData ->
                PlayerRpgProvider.get(event.getEntity()).ifPresent(newData -> {
                    newData.copyFrom(oldData);
                    if (event.isWasDeath()) {
                        // Respawn with full pools and no lingering cooldowns.
                        newData.setMana(RpgStats.maxMana(newData, event.getEntity()));
                        newData.setStamina(RpgStats.maxStamina(newData));
                        newData.getCooldowns().clear();
                    }
                }));
        event.getOriginal().invalidateCaps();
    }

    @SubscribeEvent
    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        syncAndApply(event.getEntity());
    }

    @SubscribeEvent
    public static void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
        syncAndApply(event.getEntity());
    }

    @SubscribeEvent
    public static void onPlayerChangedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        syncAndApply(event.getEntity());
    }

    private static void syncAndApply(Player player) {
        if (player instanceof ServerPlayer serverPlayer) {
            PlayerRpgProvider.get(serverPlayer).ifPresent(rpg -> {
                RpgAttributeApplier.apply(serverPlayer, rpg);
                MagikNetwork.syncFull(serverPlayer, rpg);
            });
        }
    }

    /** Server-side regeneration and passive/aura ticking. */
    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.player instanceof ServerPlayer player)) {
            return;
        }
        PlayerRpgProvider.get(player).ifPresent(rpg -> {
            long gameTime = player.level().getGameTime();

            float maxMana = RpgStats.maxMana(rpg, player);
            if (rpg.getMana() < maxMana) {
                rpg.setMana(Math.min(maxMana, rpg.getMana() + RpgStats.manaRegenPerTick(rpg)));
            } else if (rpg.getMana() > maxMana) {
                rpg.setMana(maxMana);
            }

            float maxStamina = RpgStats.maxStamina(rpg);
            if (rpg.getStamina() < maxStamina
                    && gameTime - rpg.getLastStaminaUse() >= RpgStats.STAMINA_REGEN_DELAY_TICKS) {
                rpg.setStamina(Math.min(maxStamina, rpg.getStamina() + RpgStats.staminaRegenPerTick(rpg)));
            } else if (rpg.getStamina() > maxStamina) {
                rpg.setStamina(maxStamina);
            }

            // Regeneration passive: slow healing while out of combat.
            if (rpg.hasSkill(SkillTrees.REGENERATION)
                    && player.getHealth() < player.getMaxHealth()
                    && gameTime - rpg.getLastCombatTime() >= RpgStats.OUT_OF_COMBAT_TICKS
                    && gameTime % 40 == 0) {
                player.heal(1.0F);
            }

            // Summoned spectral weapons aura (Invocação de Armas).
            if (gameTime < rpg.getWeaponSummonUntil()) {
                SkillCasting.tickWeaponSummon(player, rpg, gameTime);
            }

            // Advanced Arcane auras.
            if (gameTime < rpg.getFloatingSpheresUntil()) {
                SkillCasting.tickFloatingSpheres(player, rpg, gameTime);
            }
            if (gameTime < rpg.getVoidPresenceUntil()) {
                SkillCasting.tickVoidPresence(player, rpg, gameTime);
            }

            // Becoming an Advanced Arcanist (Intelligence 50) grants the
            // purple-and-black Advanced Staff, once.
            if (!rpg.isAdvancedStaffGranted() && gameTime % 40 == 0
                    && rpg.getAttribute(RpgAttribute.INTELLIGENCE)
                    >= SkillTrees.ADVANCED_ARCANE_INTELLIGENCE) {
                rpg.setAdvancedStaffGranted(true);
                ItemStack reward = new ItemStack(com.magik.registry.ModItems.ADVANCED_STAFF.get());
                if (!player.getInventory().add(reward)) {
                    player.drop(reward, false);
                }
                player.displayClientMessage(
                        net.minecraft.network.chat.Component.translatable(
                                "message.magik.advanced_staff_granted"), false);
                player.level().playSound(null, player.blockPosition(),
                        SoundEvents.TOTEM_USE, SoundSource.PLAYERS, 0.8F, 1.4F);
                SkillFx.helix((ServerLevel) player.level(), player,
                        SkillFx.ARCANE_A, SkillFx.ARCANE_B);
            }

            // Active guard: blocking with an RPG shield projects a green
            // barrier and drains stamina; when it empties the guard breaks.
            if (player.isBlocking() && player.getUseItem().getItem() instanceof RpgShieldItem) {
                if (rpg.consumeStamina(0.4F, gameTime)) {
                    if (gameTime % 4 == 0) {
                        SkillFx.guardDome((ServerLevel) player.level(), player);
                    }
                } else {
                    // Guard break: the shield goes on cooldown for 3 seconds.
                    ItemStack shield = player.getUseItem();
                    player.stopUsingItem();
                    player.getCooldowns().addCooldown(shield.getItem(), 60);
                    player.level().playSound(null, player.blockPosition(),
                            SoundEvents.SHIELD_BREAK, SoundSource.PLAYERS, 0.8F, 1.0F);
                }
            }

            // Unequip armor whose level/attribute requirements are not met.
            if (gameTime % 40 == 0) {
                RpgGear.enforceArmorRequirements(player, rpg);
            }

            if (gameTime % VITALS_SYNC_INTERVAL == 0) {
                MagikNetwork.syncVitals(player, rpg);
            }
        });
    }

    /** Ticks the Dimensional Rift zones on every server level tick. */
    @SubscribeEvent
    public static void onLevelTick(TickEvent.LevelTickEvent event) {
        if (event.phase == TickEvent.Phase.END && event.level instanceof ServerLevel serverLevel) {
            SkillCasting.tickRifts(serverLevel);
        }
    }

    /** Restores mana from consumables; clamped to the player's maximum. */
    public static void restoreMana(Player player, float amount) {
        PlayerRpgProvider.get(player).ifPresent(rpg ->
                rpg.setMana(Mth.clamp(rpg.getMana() + amount, 0.0F, RpgStats.maxMana(rpg, player))));
        if (player instanceof ServerPlayer serverPlayer) {
            PlayerRpgProvider.get(serverPlayer).ifPresent(rpg -> MagikNetwork.syncVitals(serverPlayer, rpg));
        }
    }

    /** Restores stamina from consumables; clamped to the player's maximum. */
    public static void restoreStamina(Player player, float amount) {
        PlayerRpgProvider.get(player).ifPresent(rpg ->
                rpg.setStamina(Mth.clamp(rpg.getStamina() + amount, 0.0F, RpgStats.maxStamina(rpg))));
        if (player instanceof ServerPlayer serverPlayer) {
            PlayerRpgProvider.get(serverPlayer).ifPresent(rpg -> MagikNetwork.syncVitals(serverPlayer, rpg));
        }
    }
}
