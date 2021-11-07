/*
 * This file is licensed under the MIT License, part of Roughly Enough Items.
 * Copyright (c) 2018, 2019, 2020, 2021 shedaniel
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package me.shedaniel.rei.impl.client.gui.widget;

import com.google.common.collect.ImmutableList;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import me.shedaniel.clothconfig2.ClothConfigInitializer;
import me.shedaniel.clothconfig2.api.LazyResettable;
import me.shedaniel.clothconfig2.api.ScissorsHandler;
import me.shedaniel.clothconfig2.api.ScrollingContainer;
import me.shedaniel.math.Point;
import me.shedaniel.math.Rectangle;
import me.shedaniel.math.impl.PointHelper;
import me.shedaniel.rei.api.client.REIRuntime;
import me.shedaniel.rei.api.client.config.ConfigManager;
import me.shedaniel.rei.api.client.config.ConfigObject;
import me.shedaniel.rei.api.client.favorites.FavoriteEntry;
import me.shedaniel.rei.api.client.favorites.FavoriteEntryType;
import me.shedaniel.rei.api.client.gui.AbstractContainerEventHandler;
import me.shedaniel.rei.api.client.gui.drag.DraggableStack;
import me.shedaniel.rei.api.client.gui.drag.DraggableStackProviderWidget;
import me.shedaniel.rei.api.client.gui.drag.DraggingContext;
import me.shedaniel.rei.api.client.gui.widgets.Tooltip;
import me.shedaniel.rei.api.client.gui.widgets.Widget;
import me.shedaniel.rei.api.client.gui.widgets.WidgetWithBounds;
import me.shedaniel.rei.api.client.overlay.OverlayListWidget;
import me.shedaniel.rei.api.client.overlay.ScreenOverlay;
import me.shedaniel.rei.api.client.util.ClientEntryStacks;
import me.shedaniel.rei.api.common.entry.EntryStack;
import me.shedaniel.rei.api.common.util.Animator;
import me.shedaniel.rei.api.common.util.CollectionUtils;
import me.shedaniel.rei.api.common.util.ImmutableTextComponent;
import me.shedaniel.rei.impl.client.config.ConfigManagerImpl;
import me.shedaniel.rei.impl.client.config.ConfigObjectImpl;
import me.shedaniel.rei.impl.client.gui.widget.region.RealRegionEntry;
import me.shedaniel.rei.impl.client.gui.widget.region.RegionDraggableStack;
import me.shedaniel.rei.impl.client.gui.widget.region.RegionListener;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Stream;

import static me.shedaniel.rei.impl.client.gui.widget.EntryListWidget.entrySize;
import static me.shedaniel.rei.impl.client.gui.widget.EntryListWidget.notSteppingOnExclusionZones;

@ApiStatus.Internal
public class FavoritesListWidget extends WidgetWithBounds implements DraggableStackProviderWidget, OverlayListWidget, RegionListener<FavoriteEntry> {
    private Rectangle fullBounds;
    private EntryStacksRegionWidget<FavoriteEntry> region = new EntryStacksRegionWidget<>(this);
    
    public final AddFavoritePanel favoritePanel = new AddFavoritePanel(this);
    public final ToggleAddFavoritePanelButton favoritePanelButton = new ToggleAddFavoritePanelButton(this);
    private List<Widget> children = ImmutableList.of(favoritePanel, favoritePanelButton, region);
    
    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double amount) {
        if (fullBounds.contains(mouseX, mouseY)) {
            if (Screen.hasControlDown()) {
                ConfigObjectImpl config = ConfigManagerImpl.getInstance().getConfig();
                if (config.setEntrySize(config.getEntrySize() + amount * 0.075)) {
                    ConfigManager.getInstance().saveConfig();
                    REIRuntime.getInstance().getOverlay().ifPresent(ScreenOverlay::queueReloadOverlay);
                    return true;
                }
            } else if (favoritePanel.mouseScrolled(mouseX, mouseY, amount)) {
                return true;
            }
        }
        return super.mouseScrolled(mouseX, mouseY, amount);
    }
    
    @Override
    public Rectangle getBounds() {
        return fullBounds;
    }
    
    public EntryStacksRegionWidget<FavoriteEntry> getRegion() {
        return region;
    }
    
    @Override
    public void onDrop(Stream<FavoriteEntry> entries) {
        if (ConfigObject.getInstance().isFavoritesEnabled()) {
            List<FavoriteEntry> favorites = ConfigObject.getInstance().getFavoriteEntries();
            favorites.clear();
            entries.forEach(entry -> {
                favorites.add(entry.copy());
            });
            
            ConfigManager.getInstance().saveConfig();
        }
    }
    
    @Override
    @Nullable
    public FavoriteEntry convertDraggableStack(DraggingContext<Screen> context, DraggableStack stack) {
        return FavoriteEntry.fromEntryStack(stack.getStack().copy());
    }
    
    @Override
    @Nullable
    public DraggableStack getHoveredStack(DraggingContext<Screen> context, double mouseX, double mouseY) {
        DraggableStack stack = region.getHoveredStack(context, mouseX, mouseY);
        if (stack != null) return stack;
        if (favoritePanel.bounds.contains(mouseX, mouseY)) {
            for (AddFavoritePanel.Row row : favoritePanel.rows.get()) {
                if (row instanceof AddFavoritePanel.SectionEntriesRow entriesRow) {
                    for (AddFavoritePanel.SectionEntriesRow.SectionFavoriteWidget widget : entriesRow.widgets) {
                        if (widget.containsMouse(mouseX, mouseY)) {
                            RealRegionEntry<FavoriteEntry> entry = new RealRegionEntry<>(region, widget.entry.copy(), entrySize());
                            entry.size.setAs(entrySize() * 100);
                            return new RegionDraggableStack<>(entry, widget);
                        }
                    }
                }
            }
        }
        return null;
    }
    
    @Override
    public EntryStack<?> getFocusedStack() {
        Point mouse = PointHelper.ofMouse();
        EntryStack<?> stack = region.getFocusedStack();
        if (stack != null && !stack.isEmpty()) return stack;
        if (favoritePanel.bounds.contains(mouse)) {
            for (AddFavoritePanel.Row row : favoritePanel.rows.get()) {
                if (row instanceof AddFavoritePanel.SectionEntriesRow entriesRow) {
                    for (AddFavoritePanel.SectionEntriesRow.SectionFavoriteWidget widget : entriesRow.widgets) {
                        if (widget.containsMouse(mouse)) {
                            return ClientEntryStacks.of(widget.entry.getRenderer(false)).copy();
                        }
                    }
                }
            }
        }
        return EntryStack.empty();
    }
    
    @Override
    public Stream<EntryStack<?>> getEntries() {
        return region.getEntries();
    }
    
    @Override
    public void render(PoseStack matrices, int mouseX, int mouseY, float delta) {
        if (fullBounds.isEmpty())
            return;
        
        if (favoritePanel.getBounds().height > 20)
            region.getBounds().setBounds(this.fullBounds.x, this.fullBounds.y, this.fullBounds.width, this.fullBounds.height - (this.fullBounds.getMaxY() - this.favoritePanel.bounds.y) - 4);
        else region.getBounds().setBounds(this.fullBounds);
        region.render(matrices, mouseX, mouseY, delta);
        renderAddFavorite(matrices, mouseX, mouseY, delta);
    }
    
    private void renderAddFavorite(PoseStack matrices, int mouseX, int mouseY, float delta) {
        this.favoritePanel.render(matrices, mouseX, mouseY, delta);
        this.favoritePanelButton.render(matrices, mouseX, mouseY, delta);
    }
    
    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (containsMouse(PointHelper.ofMouse()))
            for (Widget widget : children())
                if (widget.keyPressed(keyCode, scanCode, modifiers))
                    return true;
        return false;
    }
    
    public void updateFavoritesBounds(@Nullable String searchTerm) {
        this.fullBounds = REIRuntime.getInstance().calculateFavoritesListArea();
    }
    
    public void updateSearch() {
        if (ConfigObject.getInstance().isFavoritesEnabled()) {
            region.setEntries(CollectionUtils.map(ConfigObject.getInstance().getFavoriteEntries(), FavoriteEntry::copy));
        } else region.setEntries(Collections.emptyList());
    }
    
    @Override
    public List<? extends Widget> children() {
        return children;
    }
    
    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (region.mouseClicked(mouseX, mouseY, button))
            return true;
        for (Widget widget : children())
            if (widget.mouseClicked(mouseX, mouseY, button))
                return true;
        return false;
    }
    
    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (containsMouse(mouseX, mouseY)) {
            for (Widget widget : children())
                if (widget.mouseReleased(mouseX, mouseY, button))
                    return true;
        }
        return false;
    }
    
    public static class ToggleAddFavoritePanelButton extends FadingFavoritePanelButton {
        public ToggleAddFavoritePanelButton(FavoritesListWidget widget) {
            super(widget);
        }
        
        @Override
        protected void onClick() {
            widget.favoritePanel.expendState.setTo(widget.favoritePanel.expendState.target() == 1 ? 0 : 1, 1500);
            widget.favoritePanel.resetRows();
        }
        
        @Override
        protected void queueTooltip() {
            Tooltip.create(new TranslatableComponent("text.rei.add_favorite_widget")).queue();
        }
        
        @Override
        protected Rectangle updateArea(Rectangle fullArea) {
            return new Rectangle(fullArea.x + 4, fullArea.getMaxY() - 16 - 4, 16, 16);
        }
        
        @Override
        protected boolean isAvailable(int mouseX, int mouseY) {
            float expendProgress = widget.favoritePanel.expendState.floatValue();
            return widget.fullBounds.contains(mouseX, mouseY) || REIRuntime.getInstance().getOverlay().orElseThrow().getEntryList().containsMouse(new Point(mouseX, mouseY)) || expendProgress > .1f;
        }
        
        @Override
        protected void renderButtonText(PoseStack matrices, MultiBufferSource.BufferSource bufferSource) {
            float expendProgress = widget.favoritePanel.expendState.floatValue();
            if (expendProgress < .9f) {
                int textColor = 0xFFFFFF | (Math.round(0xFF * alpha.floatValue() * (1 - expendProgress)) << 24);
                font.drawInBatch("+", bounds.getCenterX() - 2.5f, bounds.getCenterY() - 3, textColor, false, matrices.last().pose(), bufferSource, false, 0, 15728880);
            }
            if (expendProgress > .1f) {
                int textColor = 0xFFFFFF | (Math.round(0xFF * alpha.floatValue() * expendProgress) << 24);
                font.drawInBatch("-", bounds.getCenterX() - 2.5f, bounds.getCenterY() - 3, textColor, false, matrices.last().pose(), bufferSource, false, 0, 15728880);
            }
        }
    }
    
    public abstract static class FadingFavoritePanelButton extends WidgetWithBounds {
        protected final FavoritesListWidget widget;
        public boolean wasClicked = false;
        public final Animator alpha = new Animator(0);
        
        public final Rectangle bounds = new Rectangle();
        
        public FadingFavoritePanelButton(FavoritesListWidget widget) {
            this.widget = widget;
        }
        
        @Override
        public void render(PoseStack matrices, int mouseX, int mouseY, float delta) {
            this.bounds.setBounds(updateArea(widget.fullBounds));
            boolean hovered = containsMouse(mouseX, mouseY);
            this.alpha.setTo(hovered ? 1f : isAvailable(mouseX, mouseY) ? 0.3f : 0f, 260);
            this.alpha.update(delta);
            int buttonColor = 0xFFFFFF | (Math.round(0x74 * alpha.floatValue()) << 24);
            fillGradient(matrices, bounds.x, bounds.y, bounds.getMaxX(), bounds.getMaxY(), buttonColor, buttonColor);
            if (isVisible()) {
                MultiBufferSource.BufferSource bufferSource = MultiBufferSource.immediate(Tesselator.getInstance().getBuilder());
                renderButtonText(matrices, bufferSource);
                bufferSource.endBatch();
            }
            if (hovered) {
                queueTooltip();
            }
        }
        
        protected abstract boolean isAvailable(int mouseX, int mouseY);
        
        protected abstract void renderButtonText(PoseStack matrices, MultiBufferSource.BufferSource bufferSource);
        
        @Override
        public Rectangle getBounds() {
            return bounds;
        }
        
        public boolean isVisible() {
            return Math.round(0x12 * alpha.floatValue()) > 0;
        }
        
        protected boolean wasClicked() {
            boolean tmp = this.wasClicked;
            this.wasClicked = false;
            return tmp;
        }
        
        @Override
        public List<? extends GuiEventListener> children() {
            return Collections.emptyList();
        }
        
        @Override
        public boolean mouseClicked(double mouseX, double mouseY, int button) {
            if (isVisible() && containsMouse(mouseX, mouseY)) {
                this.wasClicked = true;
            }
            return false;
        }
        
        @Override
        public boolean mouseReleased(double mouseX, double mouseY, int button) {
            if (wasClicked() && isVisible() && containsMouse(mouseX, mouseY)) {
                onClick();
                return true;
            }
            return false;
        }
        
        protected abstract void onClick();
        
        protected abstract void queueTooltip();
        
        protected abstract Rectangle updateArea(Rectangle fullArea);
    }
    
    public static class AddFavoritePanel extends WidgetWithBounds {
        private final FavoritesListWidget widget;
        public final Animator expendState = new Animator(0);
        private final Rectangle bounds = new Rectangle();
        private final Rectangle scrollBounds = new Rectangle();
        private final LazyResettable<List<Row>> rows = new LazyResettable<>(() -> {
            List<Row> rows = new ArrayList<>();
            for (FavoriteEntryType.Section section : FavoriteEntryType.registry().sections()) {
                rows.add(new SectionRow(section.getText(), section.getText().copy().withStyle(style -> style.withUnderlined(true))));
                rows.add(new SectionEntriesRow(CollectionUtils.map(section.getEntries(), FavoriteEntry::copy)));
                rows.add(new SectionSeparatorRow());
            }
            if (!rows.isEmpty()) rows.remove(rows.size() - 1);
            return rows;
        });
        private final ScrollingContainer scroller = new ScrollingContainer() {
            @Override
            public Rectangle getBounds() {
                return scrollBounds;
            }
            
            @Override
            public int getMaxScrollHeight() {
                return Math.max(1, rows.get().stream().mapToInt(Row::getRowHeight).sum());
            }
        };
        
        public AddFavoritePanel(FavoritesListWidget widget) {
            this.widget = widget;
        }
        
        public void resetRows() {
            this.rows.reset();
        }
        
        @Override
        public void render(PoseStack matrices, int mouseX, int mouseY, float delta) {
            this.bounds.setBounds(updatePanelArea(widget.fullBounds));
            this.scrollBounds.setBounds(bounds.x + 4, bounds.y + 4, bounds.width - 8, bounds.height - 20);
            this.expendState.update(delta);
            int buttonColor = 0xFFFFFF | (Math.round(0x34 * Math.min(expendState.floatValue() * 2, 1)) << 24);
            fillGradient(matrices, bounds.x, bounds.y, bounds.getMaxX(), bounds.getMaxY(), buttonColor, buttonColor);
            scroller.updatePosition(delta);
            
            if (expendState.floatValue() > 0.1f) {
                ScissorsHandler.INSTANCE.scissor(scrollBounds);
                matrices.pushPose();
                matrices.translate(0, scroller.scrollAmount, 0);
                int y = scrollBounds.y;
                for (Row row : rows.get()) {
                    row.render(matrices, scrollBounds.x, y, scrollBounds.width, row.getRowHeight(), mouseX, mouseY, delta);
                    y += row.getRowHeight();
                }
                matrices.popPose();
                ScissorsHandler.INSTANCE.removeLastScissor();
            }
        }
        
        private Rectangle updatePanelArea(Rectangle fullArea) {
            int currentWidth = 16 + Math.round(Math.min(expendState.floatValue(), 1) * (fullArea.getWidth() - 16 - 8));
            int currentHeight = 16 + Math.round(expendState.floatValue() * (fullArea.getHeight() * 0.4f - 16 - 8));
            return new Rectangle(fullArea.x + 4, fullArea.getMaxY() - currentHeight - 4, currentWidth, currentHeight);
        }
        
        @Override
        public boolean mouseScrolled(double d, double e, double f) {
            if (scrollBounds.contains(d, e)) {
                scroller.offset(ClothConfigInitializer.getScrollStep() * f, true);
                return true;
            }
            return super.mouseScrolled(d, e, f);
        }
        
        @Override
        public List<? extends GuiEventListener> children() {
            return rows.get();
        }
        
        @Override
        public Rectangle getBounds() {
            return bounds;
        }
        
        private static abstract class Row extends AbstractContainerEventHandler {
            public abstract int getRowHeight();
            
            public abstract void render(PoseStack matrices, int x, int y, int rowWidth, int rowHeight, int mouseX, int mouseY, float delta);
        }
        
        private static class SectionRow extends Row {
            private final Component sectionText;
            private final Component styledText;
            
            public SectionRow(Component sectionText, Component styledText) {
                this.sectionText = sectionText;
                this.styledText = styledText;
            }
            
            @Override
            public int getRowHeight() {
                return 11;
            }
            
            @Override
            public void render(PoseStack matrices, int x, int y, int rowWidth, int rowHeight, int mouseX, int mouseY, float delta) {
                if (mouseX >= x && mouseY >= y && mouseX <= x + rowWidth && mouseY <= y + rowHeight) {
                    Tooltip.create(sectionText).queue();
                }
                Minecraft.getInstance().font.draw(matrices, styledText, x, y + 1, 0xFFFFFFFF);
            }
            
            @Override
            public List<? extends GuiEventListener> children() {
                return Collections.emptyList();
            }
        }
        
        private static class SectionSeparatorRow extends Row {
            @Override
            public int getRowHeight() {
                return 5;
            }
            
            @Override
            public void render(PoseStack matrices, int x, int y, int rowWidth, int rowHeight, int mouseX, int mouseY, float delta) {
                fillGradient(matrices, x, y + 2, x + rowWidth, y + 3, -571806998, -571806998);
            }
            
            @Override
            public List<? extends GuiEventListener> children() {
                return Collections.emptyList();
            }
        }
        
        private class SectionEntriesRow extends Row {
            private final List<FavoriteEntry> entries;
            private final List<SectionFavoriteWidget> widgets;
            private int blockedCount;
            private int lastY;
            
            public SectionEntriesRow(List<FavoriteEntry> entries) {
                this.entries = entries;
                int entrySize = entrySize();
                this.widgets = CollectionUtils.map(this.entries, entry -> new SectionFavoriteWidget(new Point(0, 0), entrySize, entry));
                
                for (SectionFavoriteWidget widget : this.widgets) {
                    widget.size.setTo(entrySize * 100, 300);
                }
                
                this.lastY = scrollBounds.y;
                
                updateEntriesPosition(widget -> false);
            }
            
            @Override
            public int getRowHeight() {
                return Mth.ceil((entries.size() + blockedCount) / (scrollBounds.width / (float) entrySize())) * entrySize();
            }
            
            @Override
            public void render(PoseStack matrices, int x, int y, int rowWidth, int rowHeight, int mouseX, int mouseY, float delta) {
                this.lastY = y;
                int entrySize = entrySize();
                boolean fastEntryRendering = ConfigObject.getInstance().doesFastEntryRendering();
                updateEntriesPosition(entry -> true);
                for (SectionFavoriteWidget widget : widgets) {
                    widget.update(delta);
                    
                    if (widget.getBounds().getMaxY() > lastY && widget.getBounds().getY() <= lastY + rowHeight) {
                        if (widget.getCurrentEntry().isEmpty())
                            continue;
                        widget.render(matrices, mouseX, mouseY, delta);
                    }
                }
            }
            
            @Override
            public List<? extends GuiEventListener> children() {
                return widgets;
            }
            
            private class SectionFavoriteWidget extends EntryListEntryWidget {
                private Animator x = new Animator();
                private Animator y = new Animator();
                private Animator size = new Animator();
                private FavoriteEntry entry;
                
                protected SectionFavoriteWidget(Point point, int entrySize, FavoriteEntry entry) {
                    super(point, entrySize);
                    this.entry = entry;
                    entry(ClientEntryStacks.of(entry.getRenderer(true)));
                    noBackground();
                }
                
                public void moveTo(boolean animated, int xPos, int yPos) {
                    if (animated) {
                        x.setTo(xPos, 200);
                        y.setTo(yPos, 200);
                    } else {
                        x.setAs(xPos);
                        y.setAs(yPos);
                    }
                }
                
                public void update(float delta) {
                    this.size.update(delta);
                    this.x.update(delta);
                    this.y.update(delta);
                    this.getBounds().width = this.getBounds().height = (int) Math.round(this.size.doubleValue() / 100);
                    double offsetSize = (entrySize() - this.size.doubleValue() / 100) / 2;
                    this.getBounds().x = (int) Math.round(x.doubleValue() + offsetSize);
                    this.getBounds().y = (int) Math.round(y.doubleValue() + offsetSize) + lastY;
                }
                
                @Override
                @Nullable
                public Tooltip getCurrentTooltip(Point point) {
                    if (!scrollBounds.contains(point)) return null;
                    Tooltip tooltip = super.getCurrentTooltip(point);
                    if (tooltip != null) {
                        tooltip.add(ImmutableTextComponent.EMPTY);
                        tooltip.add(new TranslatableComponent("tooltip.rei.drag_to_add_favorites"));
                    }
                    return tooltip;
                }
            }
            
            public void updateEntriesPosition(Predicate<SectionFavoriteWidget> animated) {
                int entrySize = entrySize();
                this.blockedCount = 0;
                int width = scrollBounds.width / entrySize;
                int currentX = 0;
                int currentY = 0;
                
                int slotIndex = 0;
                for (SectionFavoriteWidget widget : this.widgets) {
                    while (true) {
                        int xPos = currentX * entrySize + scrollBounds.x - 1;
                        int yPos = currentY * entrySize;
                        
                        currentX++;
                        if (currentX >= width) {
                            currentX = 0;
                            currentY++;
                        }
                        
                        if (notSteppingOnExclusionZones(xPos, yPos + lastY + (int) scroller.scrollAmount, entrySize, entrySize, scrollBounds)) {
                            widget.moveTo(animated.test(widget), xPos, yPos);
                            break;
                        } else {
                            blockedCount++;
                        }
                    }
                }
            }
        }
    }
}
