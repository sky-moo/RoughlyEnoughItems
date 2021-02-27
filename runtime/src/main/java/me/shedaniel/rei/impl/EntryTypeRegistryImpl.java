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

package me.shedaniel.rei.impl;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBasedTable;
import com.google.common.collect.HashBiMap;
import com.google.common.collect.Table;
import me.shedaniel.rei.api.ingredient.entry.EntryDefinition;
import me.shedaniel.rei.api.ingredient.entry.EntryType;
import me.shedaniel.rei.api.ingredient.entry.EntryTypeBridge;
import me.shedaniel.rei.api.ingredient.entry.EntryTypeRegistry;
import me.shedaniel.rei.api.plugins.REIPlugin;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@ApiStatus.Internal
public class EntryTypeRegistryImpl implements EntryTypeRegistry {
    private final BiMap<ResourceLocation, EntryDefinition<?>> entryTypes = HashBiMap.create();
    private final Table<ResourceLocation, ResourceLocation, List<EntryTypeBridge<?, ?>>> typeBridges = HashBasedTable.create();
    
    @Override
    public void acceptPlugin(REIPlugin plugin) {
        plugin.registerEntryTypes(this);
    }
    
    @Override
    public <T> void register(ResourceLocation id, EntryDefinition<T> definition) {
        this.entryTypes.put(id, definition);
    }
    
    @Override
    public <A, B> void registerBridge(EntryType<A> original, EntryType<B> destination, EntryTypeBridge<A, B> bridge) {
        List<EntryTypeBridge<?, ?>> list = this.typeBridges.get(original.getId(), destination.getId());
        if (list == null) {
            this.typeBridges.put(original.getId(), destination.getId(), list = new ArrayList<>());
        }
        list.add(bridge);
    }
    
    @Override
    public EntryDefinition<?> get(ResourceLocation id) {
        return this.entryTypes.get(id);
    }
    
    @Override
    @NotNull
    public <A, B> Iterable<EntryTypeBridge<A, B>> getBridgesFor(EntryType<A> original, EntryType<B> destination) {
        List<? extends EntryTypeBridge<?, ?>> list = this.typeBridges.get(original.getId(), destination.getId());
        if (list == null) {
            return Collections.emptyList();
        }
        return (Iterable<EntryTypeBridge<A, B>>) list;
    }
    
    @Override
    public void startReload() {
        entryTypes.clear();
        typeBridges.clear();
    }
}