package com.magik.client.gui;

import com.magik.client.ClientRpgData;
import com.magik.client.KeyBindings;
import com.magik.client.SkillIcons;
import com.magik.network.AssignSkillSlotPacket;
import com.magik.network.MagikNetwork;
import com.magik.network.UnlockSkillPacket;
import com.magik.player.PlayerRpg;
import com.magik.skills.Skill;
import com.magik.skills.SkillTrees;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * The skill tree screen: five tabs (one per tree, each with its own accent
 * color), a pannable and mouse-wheel-zoomable canvas, nodes connected like
 * modern RPG talent trees, animated unlocks, rich tooltips and hotkey slot
 * assignment (hover an unlocked active skill and press 1-4).
 */
public class SkillTreeScreen extends Screen {

    // --- Canvas layout (world coordinates, scaled by zoom) ---
    private static final int NODE_SIZE = 28;
    private static final int BRANCH_OFFSET_X = 70;
    private static final int TIER_SPACING_Y = 74;

    // --- Palette ---
    private static final int COLOR_PANEL = 0xE80D0D14;
    private static final int COLOR_CANVAS = 0xFF15151E;
    private static final int COLOR_BORDER = 0xFF3A3A4A;
    private static final int COLOR_TEXT = 0xFFE0E0E0;
    private static final int COLOR_MUTED = 0xFF9A9AA5;
    private static final int COLOR_LOCKED = 0xB0000000;

    /** Accent color of each tree, used for tabs, connections and node frames. */
    private static int accent(SkillTrees.Tree tree) {
        return switch (tree) {
            case ARCANE -> 0xFFB13BD8;
            case SWORDSMAN -> 0xFFD84A4A;
            case ARCHER -> 0xFF5BBF3F;
            case HEAVY -> 0xFFC9862E;
            case DEFENSE -> 0xFFE3C55A;
        };
    }

    private SkillTrees.Tree currentTree = SkillTrees.Tree.ARCANE;

    // Pan & zoom state.
    private double panX;
    private double panY;
    private double zoom = 1.0D;
    private boolean panInitialized;
    private double dragDistance;

    // Unlock animation bookkeeping.
    private final Set<String> knownUnlocked = new HashSet<>();
    private final Map<String, Long> unlockAnimations = new HashMap<>();

    public SkillTreeScreen() {
        super(Component.translatable("screen.magik.skills"));
    }

    @Override
    protected void init() {
        knownUnlocked.clear();
        knownUnlocked.addAll(ClientRpgData.get().getUnlockedSkills());

        int tabWidth = 76;
        int totalWidth = SkillTrees.Tree.values().length * (tabWidth + 2) - 2;
        int x = (width - totalWidth) / 2;
        for (SkillTrees.Tree tree : SkillTrees.Tree.values()) {
            SkillTrees.Tree tabTree = tree;
            addRenderableWidget(Button.builder(tree.getDisplayName(), button -> {
                        currentTree = tabTree;
                        panInitialized = false;
                    })
                    .bounds(x, 6, tabWidth, 18)
                    .build());
            x += tabWidth + 2;
        }

        addRenderableWidget(Button.builder(Component.translatable("screen.magik.back"),
                        button -> minecraft.setScreen(new CharacterScreen()))
                .bounds(6, 6, 50, 18)
                .build());
    }

    // ------------------------------------------------------------------
    // Coordinate transforms
    // ------------------------------------------------------------------

    private int canvasTop() {
        return 30;
    }

    private int canvasBottom() {
        return height - 26;
    }

    private void ensurePan() {
        if (!panInitialized) {
            panX = width / 2.0D;
            panY = canvasTop() + 40.0D;
            zoom = 1.0D;
            panInitialized = true;
        }
    }

    private double nodeWorldX(Skill skill) {
        return (skill.getBranch() == 0 ? -BRANCH_OFFSET_X : BRANCH_OFFSET_X) - NODE_SIZE / 2.0D;
    }

    private double nodeWorldY(Skill skill) {
        return skill.getTier() * TIER_SPACING_Y;
    }

    // ------------------------------------------------------------------
    // Rendering
    // ------------------------------------------------------------------

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        ensurePan();
        renderBackground(graphics);
        PlayerRpg rpg = ClientRpgData.get();
        trackUnlockAnimations(rpg);

        graphics.fill(0, canvasTop(), width, canvasBottom(), COLOR_CANVAS);
        graphics.fill(0, canvasTop() - 1, width, canvasTop(), COLOR_BORDER);
        graphics.fill(0, canvasBottom(), width, canvasBottom() + 1, COLOR_BORDER);

        int accent = accent(currentTree);
        List<Skill> skills = SkillTrees.byTree(currentTree);

        graphics.enableScissor(0, canvasTop(), width, canvasBottom());
        graphics.pose().pushPose();
        graphics.pose().translate(panX, panY, 0);
        graphics.pose().scale((float) zoom, (float) zoom, 1.0F);

        renderConnections(graphics, skills, rpg, accent);
        Skill hovered = null;
        for (Skill skill : skills) {
            if (renderNode(graphics, skill, rpg, accent, mouseX, mouseY)) {
                hovered = skill;
            }
        }

        graphics.pose().popPose();
        graphics.disableScissor();

        renderFooter(graphics, rpg);
        super.render(graphics, mouseX, mouseY, partialTick);

        if (hovered != null) {
            renderSkillTooltip(graphics, hovered, rpg, mouseX, mouseY);
        }
    }

    /** L-shaped connectors between consecutive nodes, lit when unlocked. */
    private void renderConnections(GuiGraphics graphics, List<Skill> skills, PlayerRpg rpg, int accent) {
        for (Skill skill : skills) {
            if (skill.getPrerequisite() == null) {
                continue;
            }
            Skill parent = SkillTrees.get(skill.getPrerequisite());
            if (parent == null || parent.getTree() != skill.getTree()) {
                continue;
            }
            int x1 = (int) (nodeWorldX(parent) + NODE_SIZE / 2.0D);
            int y1 = (int) (nodeWorldY(parent) + NODE_SIZE);
            int x2 = (int) (nodeWorldX(skill) + NODE_SIZE / 2.0D);
            int y2 = (int) nodeWorldY(skill);
            int color = rpg.hasSkill(skill.getId()) ? accent
                    : rpg.hasSkill(parent.getId()) ? withAlpha(accent, 0x90) : 0xFF34343E;
            graphics.fill(x1 - 1, y1, x1 + 1, (y1 + y2) / 2, color);
            graphics.fill(Math.min(x1, x2) - 1, (y1 + y2) / 2 - 1,
                    Math.max(x1, x2) + 1, (y1 + y2) / 2 + 1, color);
            graphics.fill(x2 - 1, (y1 + y2) / 2, x2 + 1, y2, color);
        }
    }

    /** Renders one node; returns true when the mouse hovers it. */
    private boolean renderNode(GuiGraphics graphics, Skill skill, PlayerRpg rpg,
                               int accent, int mouseX, int mouseY) {
        int x = (int) nodeWorldX(skill);
        int y = (int) nodeWorldY(skill);
        boolean unlocked = rpg.hasSkill(skill.getId());
        boolean unlockable = SkillTrees.canUnlock(rpg, skill);
        boolean hovered = isNodeHovered(skill, mouseX, mouseY);

        // Unlock pop animation: node briefly scales up and emits a ring.
        Long animStart = unlockAnimations.get(skill.getId());
        float anim = 0.0F;
        if (animStart != null) {
            anim = 1.0F - Mth.clamp((System.currentTimeMillis() - animStart) / 600.0F, 0.0F, 1.0F);
            if (anim <= 0.0F) {
                unlockAnimations.remove(skill.getId());
            }
        }

        graphics.pose().pushPose();
        if (anim > 0.0F) {
            float scale = 1.0F + 0.25F * anim;
            graphics.pose().translate(x + NODE_SIZE / 2.0F, y + NODE_SIZE / 2.0F, 0);
            graphics.pose().scale(scale, scale, 1.0F);
            graphics.pose().translate(-(x + NODE_SIZE / 2.0F), -(y + NODE_SIZE / 2.0F), 0);
        }

        int border = unlocked ? accent
                : unlockable ? (hovered ? 0xFFFFFFFF : withAlpha(0xFFFFFFFF, pulseAlpha()))
                : 0xFF2A2A34;
        graphics.fill(x - 2, y - 2, x + NODE_SIZE + 2, y + NODE_SIZE + 2, border);
        graphics.fill(x - 1, y - 1, x + NODE_SIZE + 1, y + NODE_SIZE + 1, 0xFF10101A);
        graphics.blit(SkillIcons.get(skill.getId()), x, y, NODE_SIZE, NODE_SIZE,
                0.0F, 0.0F, SkillIcons.SIZE, SkillIcons.SIZE, SkillIcons.SIZE, SkillIcons.SIZE);
        if (!unlocked) {
            graphics.fill(x, y, x + NODE_SIZE, y + NODE_SIZE, unlockable ? 0x50000000 : COLOR_LOCKED);
        }
        if (anim > 0.0F) {
            int ring = (int) (6 + 14 * (1.0F - anim));
            int alpha = (int) (0xFF * anim);
            int ringColor = withAlpha(accent, alpha);
            graphics.fill(x - ring, y - ring, x + NODE_SIZE + ring, y - ring + 1, ringColor);
            graphics.fill(x - ring, y + NODE_SIZE + ring - 1, x + NODE_SIZE + ring, y + NODE_SIZE + ring, ringColor);
            graphics.fill(x - ring, y - ring, x - ring + 1, y + NODE_SIZE + ring, ringColor);
            graphics.fill(x + NODE_SIZE + ring - 1, y - ring, x + NODE_SIZE + ring, y + NODE_SIZE + ring, ringColor);
        }
        graphics.pose().popPose();
        return hovered;
    }

    private void renderFooter(GuiGraphics graphics, PlayerRpg rpg) {
        graphics.drawString(font,
                Component.translatable("screen.magik.skill_points", rpg.getSkillPoints()),
                8, height - 18, COLOR_TEXT);

        // Slot assignment preview on the right.
        int slotSize = 18;
        int x = width - (PlayerRpg.SKILL_SLOTS * (slotSize + 2)) - 6;
        int y = height - slotSize - 4;
        for (int slot = 0; slot < PlayerRpg.SKILL_SLOTS; slot++) {
            int sx = x + slot * (slotSize + 2);
            graphics.fill(sx, y, sx + slotSize, y + slotSize, COLOR_BORDER);
            graphics.fill(sx + 1, y + 1, sx + slotSize - 1, y + slotSize - 1, 0xFF15151E);
            String skillId = rpg.getSkillSlots()[slot];
            if (skillId != null) {
                graphics.blit(SkillIcons.get(skillId), sx + 1, y + 1, slotSize - 2, slotSize - 2,
                        0.0F, 0.0F, SkillIcons.SIZE, SkillIcons.SIZE, SkillIcons.SIZE, SkillIcons.SIZE);
            }
            graphics.drawString(font, String.valueOf(slot + 1), sx + 2, y - 9, COLOR_MUTED);
        }
        graphics.drawCenteredString(font,
                Component.translatable("screen.magik.zoom_hint"), width / 2, height - 18, 0xFF55555F);
    }

    private void renderSkillTooltip(GuiGraphics graphics, Skill skill, PlayerRpg rpg, int mouseX, int mouseY) {
        List<Component> lines = new ArrayList<>();
        lines.add(skill.getDisplayName().copy().withStyle(style ->
                style.withColor(accent(skill.getTree())).withBold(true)));
        lines.add(Component.translatable("screen.magik.skill_type." + (skill.getType() == Skill.Type.ACTIVE
                ? "active" : "passive")).withStyle(ChatFormatting.GRAY));
        lines.add(skill.getDescription().copy().withStyle(ChatFormatting.WHITE));

        if (skill.getManaCost() > 0) {
            lines.add(Component.translatable("screen.magik.cost_mana", (int) skill.getManaCost())
                    .withStyle(ChatFormatting.BLUE));
        }
        if (skill.getStaminaCost() > 0) {
            lines.add(Component.translatable("screen.magik.cost_stamina", (int) skill.getStaminaCost())
                    .withStyle(ChatFormatting.GREEN));
        }
        if (skill.getCooldownTicks() > 0) {
            lines.add(Component.translatable("screen.magik.cooldown",
                    String.format("%.1f", skill.getCooldownTicks() / 20.0F)).withStyle(ChatFormatting.GRAY));
        }
        boolean levelMet = rpg.getLevel() >= skill.getRequiredLevel();
        lines.add(Component.translatable("tooltip.magik.requires_level", skill.getRequiredLevel())
                .withStyle(levelMet ? ChatFormatting.DARK_GREEN : ChatFormatting.RED));
        if (skill.getPrerequisite() != null) {
            Skill prerequisite = SkillTrees.get(skill.getPrerequisite());
            if (prerequisite != null) {
                boolean met = rpg.hasSkill(prerequisite.getId());
                lines.add(Component.translatable("screen.magik.requires_skill",
                                prerequisite.getDisplayName())
                        .withStyle(met ? ChatFormatting.DARK_GREEN : ChatFormatting.RED));
            }
        }

        if (rpg.hasSkill(skill.getId())) {
            lines.add(Component.translatable("screen.magik.unlocked").withStyle(ChatFormatting.GOLD));
            if (skill.getType() == Skill.Type.ACTIVE) {
                lines.add(Component.translatable("screen.magik.assign_hint").withStyle(ChatFormatting.DARK_GRAY));
            }
        } else if (SkillTrees.canUnlock(rpg, skill)) {
            lines.add(Component.translatable("screen.magik.click_to_unlock").withStyle(ChatFormatting.YELLOW));
        }

        graphics.renderComponentTooltip(font, lines, mouseX, mouseY);
    }

    // ------------------------------------------------------------------
    // Input: zoom, pan, unlock, slot assignment
    // ------------------------------------------------------------------

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (mouseY < canvasTop() || mouseY > canvasBottom()) {
            return super.mouseScrolled(mouseX, mouseY, delta);
        }
        double factor = delta > 0 ? 1.15D : 1.0D / 1.15D;
        double newZoom = Mth.clamp(zoom * factor, 0.5D, 2.5D);
        // Zoom towards the cursor: keep the world point under it fixed.
        double worldX = (mouseX - panX) / zoom;
        double worldY = (mouseY - panY) / zoom;
        zoom = newZoom;
        panX = mouseX - worldX * zoom;
        panY = mouseY - worldY * zoom;
        return true;
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (button == 0 && mouseY >= canvasTop() && mouseY <= canvasBottom()) {
            panX += dragX;
            panY += dragY;
            dragDistance += Math.abs(dragX) + Math.abs(dragY);
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        dragDistance = 0;
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        // A short drag is a click: try to unlock the hovered node.
        if (button == 0 && dragDistance < 4.0D
                && mouseY >= canvasTop() && mouseY <= canvasBottom()) {
            PlayerRpg rpg = ClientRpgData.get();
            for (Skill skill : SkillTrees.byTree(currentTree)) {
                if (isNodeHovered(skill, mouseX, mouseY) && SkillTrees.canUnlock(rpg, skill)) {
                    MagikNetwork.CHANNEL.sendToServer(new UnlockSkillPacket(skill.getId()));
                    return true;
                }
            }
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        // Hover an unlocked active skill and press 1-4 to bind it to a slot.
        if (keyCode >= GLFW.GLFW_KEY_1 && keyCode <= GLFW.GLFW_KEY_4) {
            int slot = keyCode - GLFW.GLFW_KEY_1;
            PlayerRpg rpg = ClientRpgData.get();
            double mouseX = minecraft.mouseHandler.xpos() * width / minecraft.getWindow().getScreenWidth();
            double mouseY = minecraft.mouseHandler.ypos() * height / minecraft.getWindow().getScreenHeight();
            for (Skill skill : SkillTrees.byTree(currentTree)) {
                if (isNodeHovered(skill, mouseX, mouseY)
                        && rpg.hasSkill(skill.getId()) && skill.getType() == Skill.Type.ACTIVE) {
                    MagikNetwork.CHANNEL.sendToServer(new AssignSkillSlotPacket(slot, skill.getId()));
                    return true;
                }
            }
        }
        if (keyCode == GLFW.GLFW_KEY_E || KeyBindings.OPEN_MENU.matches(keyCode, scanCode)) {
            onClose();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    // ------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------

    private boolean isNodeHovered(Skill skill, double mouseX, double mouseY) {
        if (mouseY < canvasTop() || mouseY > canvasBottom()) {
            return false;
        }
        double worldX = (mouseX - panX) / zoom;
        double worldY = (mouseY - panY) / zoom;
        return worldX >= nodeWorldX(skill) && worldX < nodeWorldX(skill) + NODE_SIZE
                && worldY >= nodeWorldY(skill) && worldY < nodeWorldY(skill) + NODE_SIZE;
    }

    private void trackUnlockAnimations(PlayerRpg rpg) {
        for (String skillId : rpg.getUnlockedSkills()) {
            if (knownUnlocked.add(skillId)) {
                unlockAnimations.put(skillId, System.currentTimeMillis());
            }
        }
    }

    private int pulseAlpha() {
        return 0x70 + (int) (0x60 * Math.sin(System.currentTimeMillis() / 300.0D));
    }

    private static int withAlpha(int color, int alpha) {
        return (Mth.clamp(alpha, 0, 255) << 24) | (color & 0x00FFFFFF);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
