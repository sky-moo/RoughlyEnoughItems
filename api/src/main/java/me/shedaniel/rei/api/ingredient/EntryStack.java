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

package me.shedaniel.rei.api.ingredient;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.serialization.Dynamic;
import com.mojang.serialization.JsonOps;
import me.shedaniel.architectury.platform.Platform;
import me.shedaniel.rei.api.gui.Renderer;
import me.shedaniel.rei.api.ingredient.entry.*;
import me.shedaniel.rei.api.ingredient.util.EntryStacks;
import me.shedaniel.rei.api.util.TextRepresentable;
import me.shedaniel.rei.impl.Internals;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.TagParser;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.minecraft.util.Unit;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;

@Environment(EnvType.CLIENT)
public interface EntryStack<T> extends TextRepresentable, Renderer {
    static EntryStack<Unit> empty() {
        return Internals.getEntryStackProvider().empty();
    }
    
    static <T> EntryStack<T> of(EntryDefinition<T> definition, T value) {
        return Internals.getEntryStackProvider().of(definition, value);
    }
    
    static <T> EntryStack<T> of(EntryType<T> type, T value) {
        return of(type.getDefinition(), value);
    }
    
    @ApiStatus.Internal
    static EntryStack<?> readFromJson(JsonElement jsonElement) {
        try {
            JsonObject obj = jsonElement.getAsJsonObject();
            EntryType<Object> type = EntryType.deferred(new ResourceLocation(GsonHelper.getAsString(obj, "type")));
            EntrySerializer<Object> serializer = type.getDefinition().getSerializer();
            if (serializer != null && serializer.supportReading()) {
                Object o = serializer.read(TagParser.parseTag(obj.toString()));
                return EntryStack.of(type, o);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return EntryStack.empty();
    }
    
    @ApiStatus.Internal
    @Nullable
    default JsonElement toJson() {
        try {
            EntrySerializer<T> serializer = getDefinition().getSerializer();
            if (serializer != null && serializer.supportSaving()) {
                JsonObject object = Dynamic.convert(NbtOps.INSTANCE, JsonOps.INSTANCE, serializer.save(this, getValue())).getAsJsonObject();
                object.addProperty("type", getType().getId().toString());
                return object;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
    
    EntryDefinition<T> getDefinition();
    
    default EntryType<T> getType() {
        return getDefinition().getType();
    }
    
    default Class<T> getValueType() {
        return getDefinition().getValueType();
    }
    
    default EntryRenderer<T> getRenderer() {
        return getDefinition().getRenderer();
    }
    
    Optional<ResourceLocation> getIdentifier();
    
    boolean isEmpty();
    
    EntryStack<T> copy();
    
    @ApiStatus.Internal
    default EntryStack<T> rewrap() {
        return copy();
    }
    
    EntryStack<T> normalize();
    
    @Deprecated
    int hashCode();
    
    int hash(ComparisonContext context);
    
    boolean equals(EntryStack<T> other, ComparisonContext context);
    
    @Deprecated
    boolean equals(Object o);
    
    T getValue();
    
    <R> EntryStack<T> setting(Settings<R> settings, R value);
    
    <R> EntryStack<T> removeSetting(Settings<R> settings);
    
    EntryStack<T> clearSettings();
    
    <R> R get(Settings<R> settings);
    
    class Settings<R> {
        @ApiStatus.Internal
        private static final List<Settings<?>> SETTINGS = new ArrayList<>();
        
        public static final Supplier<Boolean> TRUE = () -> true;
        public static final Supplier<Boolean> FALSE = () -> false;
        public static final Settings<Supplier<Boolean>> RENDER = new Settings<>(TRUE);
        @Deprecated
        public static final Settings<Supplier<Boolean>> CHECK_TAGS = new Settings<>(FALSE);
        @Deprecated
        public static final Settings<Supplier<Boolean>> CHECK_AMOUNT = new Settings<>(FALSE);
        @Deprecated
        public static final Settings<Supplier<Boolean>> TOOLTIP_ENABLED = new Settings<>(TRUE);
        @Deprecated
        public static final Settings<Supplier<Boolean>> TOOLTIP_APPEND_MOD = new Settings<>(TRUE);
        public static final Settings<Supplier<Boolean>> RENDER_COUNTS = new Settings<>(TRUE);
        @Deprecated
        public static final Settings<Function<EntryStack<?>, List<Component>>> TOOLTIP_APPEND_EXTRA = new Settings<>(stack -> Collections.emptyList());
        @Deprecated
        public static final Settings<Function<EntryStack<?>, String>> COUNTS = new Settings<>(stack -> null);
        
        private static short nextId;
        private R defaultValue;
        private short id;
        
        @ApiStatus.Internal
        public Settings(R defaultValue) {
            this.defaultValue = defaultValue;
            this.id = nextId++;
            SETTINGS.add(this);
        }
        
        @ApiStatus.Internal
        public static <R> Settings<R> getById(short id) {
            return (Settings<R>) SETTINGS.get(id);
        }
        
        public R getDefaultValue() {
            return defaultValue;
        }
        
        @ApiStatus.Internal
        public short getId() {
            return id;
        }
        
        public static class Fluid {
            private static final String FLUID_AMOUNT = Platform.isForge() ? "tooltip.rei.fluid_amount.forge" : "tooltip.rei.fluid_amount";
            // Return null to disable
            public static final Settings<Function<EntryStack<?>, String>> AMOUNT_TOOLTIP = new Settings<>(stack -> I18n.get(FLUID_AMOUNT, EntryStacks.simplifyAmount(stack.cast()).getValue().getAmount()));
            
            private Fluid() {
            }
        }
        
    }
    
    @ApiStatus.NonExtendable
    default <O> EntryStack<O> cast() {
        return (EntryStack<O>) this;
    }
}
