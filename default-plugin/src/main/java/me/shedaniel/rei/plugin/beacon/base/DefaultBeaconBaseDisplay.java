/*
 * This file is licensed under the MIT License, part of Roughly Enough Items.
 * Copyright (c) 2018, 2019, 2020 shedaniel
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

package me.shedaniel.rei.plugin.beacon.base;

import me.shedaniel.rei.api.ingredient.EntryStack;
import me.shedaniel.rei.api.registry.display.Display;
import me.shedaniel.rei.api.ingredient.util.EntryStacks;
import me.shedaniel.rei.plugin.DefaultPlugin;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.List;

@Environment(EnvType.CLIENT)
public class DefaultBeaconBaseDisplay implements Display {
    private List<? extends EntryStack<?>> entries;
    
    public DefaultBeaconBaseDisplay(List<ItemStack> entries) {
        this.entries = EntryStacks.ofItemStacks(entries);
    }
    
    @Override
    public @NotNull List<? extends List<? extends EntryStack<?>>> getInputEntries() {
        return Collections.singletonList(entries);
    }
    
    public List<? extends EntryStack<?>> getEntries() {
        return entries;
    }
    
    @Override
    public @NotNull List<? extends List<? extends EntryStack<?>>> getResultingEntries() {
        return Collections.emptyList();
    }
    
    @Override
    public @NotNull ResourceLocation getRecipeCategory() {
        return DefaultPlugin.BEACON;
    }
}