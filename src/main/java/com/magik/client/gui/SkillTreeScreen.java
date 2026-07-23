package com.magik.client.gui;

import com.magik.client.ClientRpgData;
import com.magik.client.KeyBindings;
import com.magik.client.SkillIcons;
import com.magik.network.AssignSkillSlotPacket;
import com.magik.network.MagikNetwork;
import com.magik.network.RespecPacket;
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
 * The redesigned skill tree: a themed, branching talent tree. Each tree has a
 * glowing root emblem that its branches flow out of, circular nodes with soft
 * accent glows, tapered connectors that animate a pulse of energy along
 * unlocked paths, and a drifting particle background tinted per tree. Supports
 * pan/zoom with auto-fit, animated unlocks, rich tooltips, hotkey slot binding
 * and a one-click respec.
 */
public class SkillTreeScreen extends Screen {

    // --- Canvas layout (world coordinates, scaled by zoom) ---
    private static final int NODE_SIZE = 30;
    private static final int BRANCH_OFFSET_X = 78;
    private static final int TIER_SPACING_Y = 82;
    private static final double ROOT_Y = -TIER_SPACING_Y * 0.9D;

    // --- Palette ---
    private static final int COLOR_TEXT = 0xFFEDEDF2;
    private static final int COLOR_MUTED = 0xFF9A9AA5;

    /** Accent color of each tree. */
    private static int accent(SkillTrees.Tree tree) {
        return switch (tree) {
            case ARCANE -> 0xFFB13BD8;
            case SWORDSMAN -> 0xFFD84A4A;
            case ARCHER -> 0xFF5BBF3F;
            case HEAVY -> 0xFFC9862E;
            case DEFENSE -> 0xFFE3C55A;
            case ADVANCED_ARCANE -> 0xFF8A2BE2;
            case DAGGER -> 0xFF9B59D0;
        };
    }

    private SkillTrees.Tree currentTree = SkillTrees.Tree.ARCANE;

    private double panX;
    private double panY;
    private double zoom = 1.0D;
    private boolean panInitialized;
    private double dragDistance;

    private final Set<String> knownUnlocked = new HashSet<>();
    private final Map<String, Long> unlockAnimations = new HashMap<>();
    private Button respecButton;

    public SkillTreeScreen() {
        super(Component.translatable("screen.magik.skills"));
    }

    @Override
    protected void init() {
        knownUnlocked.clear();
        knownUnlocked.addAll(ClientRpgData.get().getUnlockedSkills());

        int backWidth = 46;
        addRenderableWidget(Button.builder(Component.translatable("screen.magik.back"),
                        button -> minecraft.setScreen(new CharacterScreen()))
                .bounds(6, 6, backWidth, 18)
                .build());

        int trees = SkillTrees.Tree.values().length;
        int tabsLeft = 6 + backWidth + 6;
        int tabsArea = width - tabsLeft - 6;
        int gap = 2;
        int tabWidth = Math.max(28, (tabsArea - (trees - 1) * gap) / trees);
        int x = tabsLeft;
        for (SkillTrees.Tree tree : SkillTrees.Tree.values()) {
            SkillTrees.Tree tabTree = tree;
            addRenderableWidget(Button.builder(tree.getDisplayName(), button -> {
                        currentTree = tabTree;
                        panInitialized = false;
                    })
                    .bounds(x, 6, tabWidth, 18)
                    .build());
            x += tabWidth + gap;
        }

        // Respec button in the footer.
        respecButton = Button.builder(Component.translatable("screen.magik.respec"),
                        button -> MagikNetwork.CHANNEL.sendToServer(new RespecPacket()))
                .bounds(width - 92, height - 22, 86, 18)
                .build();
        addRenderableWidget(respecButton);
    }

    private int canvasTop() {
        return 30;
    }

    private int canvasBottom() {
        return height - 26;
    }

    private void ensurePan() {
        if (panInitialized) {
            return;
        }
        panInitialized = true;

        List<Skill> skills = SkillTrees.byTree(currentTree);
        double minX = -BRANCH_OFFSET_X, maxX = BRANCH_OFFSET_X;
        double minY = ROOT_Y, maxY = 0;
        for (Skill skill : skills) {
            double nx = nodeWorldX(skill);
            double ny = nodeWorldY(skill);
            minX = Math.min(minX, nx);
            maxX = Math.max(maxX, nx + NODE_SIZE);
            minY = Math.min(minY, ny);
            maxY = Math.max(maxY, ny + NODE_SIZE);
        }
        double treeW = (maxX - minX) + 60.0D;
        double treeH = (maxY - minY) + 60.0D;
        zoom = Mth.clamp(Math.min(width / treeW, (canvasBottom() - canvasTop()) / treeH), 0.5D, 1.25D);
        double centerX = (minX + maxX) / 2.0D;
        double centerY = (minY + maxY) / 2.0D;
        panX = width / 2.0D - centerX * zoom;
        panY = (canvasTop() + canvasBottom()) / 2.0D - centerY * zoom;
    }

    private double nodeWorldX(Skill skill) {
        int branches = SkillTrees.branchCount(skill.getTree());
        double spacing = BRANCH_OFFSET_X * 2.0D;
        return (skill.getBranch() - (branches - 1) / 2.0D) * spacing - NODE_SIZE / 2.0D;
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
        PlayerRpg rpg = ClientRpgData.get();
        trackUnlockAnimations(rpg);
        int accent = accent(currentTree);

        renderThemedBackground(graphics, accent);

        List<Skill> skills = SkillTrees.byTree(currentTree);
        graphics.enableScissor(0, canvasTop(), width, canvasBottom());
        graphics.pose().pushPose();
        graphics.pose().translate(panX, panY, 0);
        graphics.pose().scale((float) zoom, (float) zoom, 1.0F);

        renderRoot(graphics, skills, rpg, accent);
        renderConnections(graphics, skills, rpg, accent);
        Skill hovered = null;
        for (Skill skill : skills) {
            if (renderNode(graphics, skill, rpg, accent, mouseX, mouseY)) {
                hovered = skill;
            }
        }

        graphics.pose().popPose();
        graphics.disableScissor();

        renderHeader(graphics, rpg, accent);
        renderFooter(graphics, rpg);
        if (respecButton != null) {
            respecButton.active = !rpg.getUnlockedSkills().isEmpty();
        }
        super.render(graphics, mouseX, mouseY, partialTick);

        if (hovered != null) {
            renderSkillTooltip(graphics, hovered, rpg, mouseX, mouseY);
        }
    }

    /** A dark accent-tinted gradient with slow drifting motes. */
    private void renderThemedBackground(GuiGraphics graphics, int accent) {
        int top = blend(0xFF0B0B12, accent, 0.10F);
        int bottom = 0xFF07070C;
        graphics.fillGradient(0, canvasTop(), width, canvasBottom(), top, bottom);

        long t = System.currentTimeMillis();
        int h = canvasBottom() - canvasTop();
        for (int i = 0; i < 55; i++) {
            long seed = i * 2654435761L;
            int sx = (int) (Math.abs(seed >> 8) % width);
            double speed = 6.0D + (Math.abs(seed >> 3) % 10);
            int sy = canvasTop() + (int) (((Math.abs(seed) % h) + t / (140.0D - speed)) % h);
            int alpha = 0x22 + (int) (0x33 * (0.5 + 0.5 * Math.sin(t / 500.0D + i)));
            int size = (i % 7 == 0) ? 2 : 1;
            graphics.fill(sx, sy, sx + size, sy + size, withAlpha(accent, alpha));
        }
        // Framing lines.
        graphics.fill(0, canvasTop() - 1, width, canvasTop(), withAlpha(accent, 0x80));
        graphics.fill(0, canvasBottom(), width, canvasBottom() + 1, withAlpha(accent, 0x80));
    }

    /** The glowing tree emblem every branch grows from. */
    private void renderRoot(GuiGraphics graphics, List<Skill> skills, PlayerRpg rpg, int accent) {
        int cx = 0;
        int cy = (int) (ROOT_Y + NODE_SIZE / 2.0D);
        int unlocked = SkillTrees.unlockedInTree(rpg, currentTree);
        boolean any = unlocked > 0;
        glow(graphics, cx, cy, 22, withAlpha(accent, any ? 0x60 : 0x28));
        fillDisc(graphics, cx, cy, 15, 0xFF12121C);
        ring(graphics, cx, cy, 15, withAlpha(accent, 0xFF), 2);
        // Emblem: the tree's initial.
        String letter = currentTree.getDisplayName().getString().substring(0, 1).toUpperCase();
        graphics.pose().pushPose();
        graphics.pose().translate(cx, cy - 4, 0);
        graphics.pose().scale(1.6F, 1.6F, 1.0F);
        graphics.drawCenteredString(font, letter, 0, 0, accent);
        graphics.pose().popPose();

        // Connect the root to every branch's first node.
        for (Skill skill : skills) {
            if (skill.getTier() == 0) {
                int x2 = (int) (nodeWorldX(skill) + NODE_SIZE / 2.0D);
                int y2 = (int) nodeWorldY(skill);
                boolean lit = rpg.hasSkill(skill.getId());
                connector(graphics, cx, cy + 15, x2, y2, accent, lit || any, lit);
            }
        }
    }

    /** Tapered connectors with an animated energy pulse on unlocked links. */
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
            boolean lit = rpg.hasSkill(skill.getId());
            boolean reachable = rpg.hasSkill(parent.getId());
            connector(graphics, x1, y1, x2, y2, accent, lit || reachable, lit);
        }
    }

    /** Draws a connector, optionally lit, with a moving pulse when unlocked. */
    private void connector(GuiGraphics graphics, int x1, int y1, int x2, int y2,
                           int accent, boolean reachable, boolean lit) {
        int color = lit ? accent : reachable ? withAlpha(accent, 0x88) : 0xFF2C2C38;
        thickLine(graphics, x1, y1, x2, y2, color, lit ? 3 : 2);
        if (lit) {
            double p = (System.currentTimeMillis() % 1400L) / 1400.0D;
            int px = (int) (x1 + (x2 - x1) * p);
            int py = (int) (y1 + (y2 - y1) * p);
            fillDisc(graphics, px, py, 3, 0xFFFFFFFF);
            fillDisc(graphics, px, py, 5, withAlpha(0xFFFFFFFF, 0x55));
        }
    }

    /** Renders one node; returns true when the mouse hovers it. */
    private boolean renderNode(GuiGraphics graphics, Skill skill, PlayerRpg rpg,
                               int accent, int mouseX, int mouseY) {
        int cx = (int) (nodeWorldX(skill) + NODE_SIZE / 2.0D);
        int cy = (int) (nodeWorldY(skill) + NODE_SIZE / 2.0D);
        boolean unlocked = rpg.hasSkill(skill.getId());
        boolean unlockable = SkillTrees.canUnlock(rpg, skill);
        boolean hovered = isNodeHovered(skill, mouseX, mouseY);
        int r = NODE_SIZE / 2;

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
            float scale = 1.0F + 0.3F * anim;
            graphics.pose().translate(cx, cy, 0);
            graphics.pose().scale(scale, scale, 1.0F);
            graphics.pose().translate(-cx, -cy, 0);
        }

        // Glow: strong for unlocked, pulsing for unlockable.
        if (unlocked) {
            glow(graphics, cx, cy, r + 8, withAlpha(accent, 0x66));
        } else if (unlockable) {
            glow(graphics, cx, cy, r + 6, withAlpha(0xFFFFFFFF, pulseAlpha() / 2));
        }

        // Body + ring.
        fillDisc(graphics, cx, cy, r + 2, 0xFF0C0C14);
        fillDisc(graphics, cx, cy, r, 0xFF15151F);
        int ringColor = unlocked ? accent
                : unlockable ? (hovered ? 0xFFFFFFFF : withAlpha(0xFFFFFFFF, pulseAlpha()))
                : 0xFF33333F;
        ring(graphics, cx, cy, r, ringColor, 2);

        // Icon.
        int is = NODE_SIZE - 8;
        graphics.blit(SkillIcons.get(skill.getId()), cx - is / 2, cy - is / 2, is, is,
                0.0F, 0.0F, SkillIcons.SIZE, SkillIcons.SIZE, SkillIcons.SIZE, SkillIcons.SIZE);
        if (!unlocked) {
            fillDisc(graphics, cx, cy, r - 1, unlockable ? 0x33000000 : 0xB0000000);
        }
        // Supreme skills get a small crown mark.
        graphics.pose().popPose();
        return hovered;
    }

    private void renderHeader(GuiGraphics graphics, PlayerRpg rpg, int accent) {
        // Big tree name + unlocked count, centered above the canvas.
        Component title = currentTree.getDisplayName();
        int unlocked = SkillTrees.unlockedInTree(rpg, currentTree);
        int total = SkillTrees.byTree(currentTree).size();
        graphics.pose().pushPose();
        graphics.pose().translate(width / 2.0F, canvasTop() + 3, 0);
        graphics.pose().scale(1.3F, 1.3F, 1.0F);
        graphics.drawCenteredString(font, title, 0, 0, accent);
        graphics.pose().popPose();
        graphics.drawCenteredString(font, unlocked + " / " + total,
                width / 2, canvasTop() + 16, COLOR_MUTED);
    }

    private void renderFooter(GuiGraphics graphics, PlayerRpg rpg) {
        graphics.drawString(font,
                Component.translatable("screen.magik.skill_points", rpg.getSkillPoints()),
                8, height - 18, COLOR_TEXT);

        int slotSize = 18;
        int x = width / 2 - (PlayerRpg.SKILL_SLOTS * (slotSize + 2)) / 2;
        int y = height - slotSize - 4;
        for (int slot = 0; slot < PlayerRpg.SKILL_SLOTS; slot++) {
            int sx = x + slot * (slotSize + 2);
            graphics.fill(sx - 1, y - 1, sx + slotSize + 1, y + slotSize + 1, 0xFF3A3A4A);
            graphics.fill(sx, y, sx + slotSize, y + slotSize, 0xFF15151E);
            String skillId = rpg.getSkillSlots()[slot];
            if (skillId != null) {
                graphics.blit(SkillIcons.get(skillId), sx + 1, y + 1, slotSize - 2, slotSize - 2,
                        0.0F, 0.0F, SkillIcons.SIZE, SkillIcons.SIZE, SkillIcons.SIZE, SkillIcons.SIZE);
            }
            graphics.drawString(font, String.valueOf(slot + 1), sx + 2, y - 9, COLOR_MUTED);
        }
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
        if (skill.getTree() == SkillTrees.Tree.ADVANCED_ARCANE) {
            boolean intMet = rpg.getAttribute(com.magik.player.RpgAttribute.INTELLIGENCE)
                    >= SkillTrees.ADVANCED_ARCANE_INTELLIGENCE;
            lines.add(Component.translatable("tooltip.magik.requires_intelligence",
                            SkillTrees.ADVANCED_ARCANE_INTELLIGENCE)
                    .withStyle(intMet ? ChatFormatting.DARK_GREEN : ChatFormatting.RED));
            lines.add(Component.translatable("tooltip.magik.requires_staff")
                    .withStyle(ChatFormatting.DARK_PURPLE));
        }
        if (skill.getTree() == SkillTrees.Tree.DAGGER) {
            lines.add(Component.translatable("tooltip.magik.dagger_dual").withStyle(ChatFormatting.DARK_PURPLE));
        }
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
    // Input
    // ------------------------------------------------------------------

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (mouseY < canvasTop() || mouseY > canvasBottom()) {
            return super.mouseScrolled(mouseX, mouseY, delta);
        }
        double factor = delta > 0 ? 1.15D : 1.0D / 1.15D;
        double newZoom = Mth.clamp(zoom * factor, 0.5D, 2.5D);
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
    // Draw helpers
    // ------------------------------------------------------------------

    private boolean isNodeHovered(Skill skill, double mouseX, double mouseY) {
        if (mouseY < canvasTop() || mouseY > canvasBottom()) {
            return false;
        }
        double worldX = (mouseX - panX) / zoom;
        double worldY = (mouseY - panY) / zoom;
        double cx = nodeWorldX(skill) + NODE_SIZE / 2.0D;
        double cy = nodeWorldY(skill) + NODE_SIZE / 2.0D;
        double dx = worldX - cx, dy = worldY - cy;
        return dx * dx + dy * dy <= (NODE_SIZE / 2.0D + 1) * (NODE_SIZE / 2.0D + 1);
    }

    private void fillDisc(GuiGraphics graphics, int cx, int cy, int r, int color) {
        for (int dy = -r; dy <= r; dy++) {
            int dx = (int) Math.sqrt((double) r * r - dy * dy);
            graphics.fill(cx - dx, cy + dy, cx + dx + 1, cy + dy + 1, color);
        }
    }

    private void ring(GuiGraphics graphics, int cx, int cy, int r, int color, int thickness) {
        for (int dy = -r; dy <= r; dy++) {
            int outer = (int) Math.sqrt((double) r * r - dy * dy);
            int innerR = Math.max(0, r - thickness);
            int inner = dy >= -innerR && dy <= innerR
                    ? (int) Math.sqrt((double) innerR * innerR - dy * dy) : -1;
            if (inner < 0) {
                graphics.fill(cx - outer, cy + dy, cx + outer + 1, cy + dy + 1, color);
            } else {
                graphics.fill(cx - outer, cy + dy, cx - inner, cy + dy + 1, color);
                graphics.fill(cx + inner + 1, cy + dy, cx + outer + 1, cy + dy + 1, color);
            }
        }
    }

    /** Soft glow: a few translucent discs of decreasing size. */
    private void glow(GuiGraphics graphics, int cx, int cy, int r, int color) {
        int a = (color >>> 24) & 0xFF;
        for (int i = 3; i >= 1; i--) {
            int rr = r * i / 3;
            fillDisc(graphics, cx, cy, rr, withAlpha(color, a / (4 - i)));
        }
    }

    private void thickLine(GuiGraphics graphics, int x1, int y1, int x2, int y2, int color, int thickness) {
        int dx = Math.abs(x2 - x1), dy = Math.abs(y2 - y1);
        int sx = x1 < x2 ? 1 : -1, sy = y1 < y2 ? 1 : -1;
        int err = dx - dy;
        int half = thickness / 2;
        while (true) {
            graphics.fill(x1 - half, y1 - half, x1 + half + 1, y1 + half + 1, color);
            if (x1 == x2 && y1 == y2) {
                break;
            }
            int e2 = 2 * err;
            if (e2 > -dy) {
                err -= dy;
                x1 += sx;
            }
            if (e2 < dx) {
                err += dx;
                y1 += sy;
            }
        }
    }

    private void trackUnlockAnimations(PlayerRpg rpg) {
        for (String skillId : rpg.getUnlockedSkills()) {
            if (knownUnlocked.add(skillId)) {
                unlockAnimations.put(skillId, System.currentTimeMillis());
            }
        }
        knownUnlocked.retainAll(rpg.getUnlockedSkills()); // Reset after a respec.
    }

    private int pulseAlpha() {
        return 0x70 + (int) (0x60 * Math.sin(System.currentTimeMillis() / 300.0D));
    }

    private static int withAlpha(int color, int alpha) {
        return (Mth.clamp(alpha, 0, 255) << 24) | (color & 0x00FFFFFF);
    }

    /** Linear blend of two ARGB colors toward b by t (keeps a's alpha). */
    private static int blend(int a, int b, float t) {
        int aa = (a >>> 24) & 0xFF;
        int ar = (a >> 16) & 0xFF, ag = (a >> 8) & 0xFF, ab = a & 0xFF;
        int br = (b >> 16) & 0xFF, bg = (b >> 8) & 0xFF, bb = b & 0xFF;
        int rr = (int) (ar + (br - ar) * t);
        int rg = (int) (ag + (bg - ag) * t);
        int rb = (int) (ab + (bb - ab) * t);
        return (aa << 24) | (rr << 16) | (rg << 8) | rb;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
