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

package me.shedaniel.rei.jeiinternalsworkaround;

import cpw.mods.modlauncher.LaunchPluginHandler;
import cpw.mods.modlauncher.Launcher;
import cpw.mods.modlauncher.serviceapi.ILaunchPluginService;
import me.shedaniel.rei.jeiinternalsworkaround.transformer.InternalsRemapperTransformer;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfig;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.util.*;
import java.util.function.Consumer;

public class JEIInternalsMixinPlugin implements IMixinConfigPlugin {
    private static final List<Consumer<ClassNode>> TRANSFORMERS = new ArrayList<>();
    
    static {
        EnumSet<ILaunchPluginService.Phase> NONE = EnumSet.noneOf(ILaunchPluginService.Phase.class);
        EnumSet<ILaunchPluginService.Phase> AFTER = EnumSet.of(ILaunchPluginService.Phase.AFTER);
        TRANSFORMERS.add(new InternalsRemapperTransformer());
        // committing crime
        LaunchPluginHandler launchPlugins = get(Launcher.INSTANCE, "launchPlugins");
        Map<String, ILaunchPluginService> plugins = get(launchPlugins, "plugins");
        plugins.put("epic_rei_lmao", new ILaunchPluginService() {
            @Override
            public String name() {
                return "epic_rei_lmao";
            }
            
            @Override
            public EnumSet<Phase> handlesClass(Type classType, boolean isEmpty) {
                throw new IllegalStateException("Outdated ModLauncher");
            }
            
            @Override
            public EnumSet<Phase> handlesClass(Type classType, boolean isEmpty, String reason) {
                return isEmpty || Objects.equals(reason, "mixin") || classType.getClassName().contains("jeiinternalsworkaround") ? NONE : AFTER;
            }
            
            @Override
            public int processClassWithFlags(Phase phase, ClassNode classNode, Type classType, String reason) {
                if (phase == Phase.BEFORE && Objects.equals(reason, "mixin")) {
                    return ComputeFlags.NO_REWRITE;
                }
                for (Consumer<ClassNode> transformer : TRANSFORMERS) {
                    transformer.accept(classNode);
                }
                return ComputeFlags.SIMPLE_REWRITE;
            }
        });
       /* for (ILaunchPluginService service : plugins.values()) {
            System.out.println(service.getClass().getSimpleName());
            if (Objects.equals(service.getClass().getSimpleName(), "MixinLaunchPlugin")) {
                List<IClassProcessor> processors = get(service, "processors");
                processors.add(new IClassProcessor() {
                    @Override
                    public EnumSet<ILaunchPluginService.Phase> handlesClass(Type classType, boolean isEmpty, String reason) {
                        return AFTER;
                    }
                    
                    @Override
                    public boolean processClass(ILaunchPluginService.Phase phase, ClassNode classNode, Type classType, String reason) {
                        if (reason.equals("mixin")) return false;
                        System.out.println(classNode.name);
                        for (Consumer<ClassNode> transformer : TRANSFORMERS) {
                            transformer.accept(classNode);
                        }
                        return true;
                    }
                });
            }
        }*/
       /* IMixinTransformer transformer = (IMixinTransformer) MixinEnvironment.getCurrentEnvironment().getActiveTransformer();
        ((Extensions) transformer.getExtensions()).add(new IExtension() {
            @Override
            public boolean checkActive(MixinEnvironment environment) {
                return true;
            }
            
            @Override
            public void preApply(ITargetClassContext context) {
                
            }
            
            @Override
            public void postApply(ITargetClassContext context) {
                System.out.println(context.getClassNode().name);
                for (Consumer<ClassNode> transformer : TRANSFORMERS) {
                    transformer.accept(context.getClassNode());
                }
            }
            
            @Override
            public void export(MixinEnvironment env, String name, boolean force, ClassNode classNode) {
                
            }
        });
        MixinProcessor processor = get(transformer, "processor");
        List<IMixinConfig> configs = get(processor, "configs");
        configs.add(createApplyAnyMixinConfig());*/
    }
    
    private static IMixinConfig createApplyAnyMixinConfig() {
        try {
            Constructor<?> constructor = Class.forName("org.spongepowered.asm.mixin.transformer.MixinConfig").getDeclaredConstructor();
            constructor.setAccessible(true);
            IMixinConfig config = (IMixinConfig) constructor.newInstance();
            set(config, "mixinMapping", new HashMap<String, List<?>>() {
                @Override
                public boolean containsKey(Object key) {
                    return key instanceof String && !((String) key).toLowerCase().contains("mixin");
                }
            });
            set(config, "mixinPackage", "djaikodhjiowadhiuajdhiuwajdhwaiuy8dbhwa");
            return config;
        } catch (Throwable throwable) {
            throw new RuntimeException(throwable);
        }
    }
    
    private static <T> T get(Object obj, String field) {
        try {
            Field declaredField = obj.getClass().getDeclaredField(field);
            declaredField.setAccessible(true);
            return (T) declaredField.get(obj);
        } catch (Throwable throwable) {
            throw new RuntimeException(throwable);
        }
    }
    
    private static void set(Object obj, String field, Object value) {
        try {
            Field declaredField = obj.getClass().getDeclaredField(field);
            declaredField.setAccessible(true);
            declaredField.set(obj, value);
        } catch (Throwable throwable) {
            throw new RuntimeException(throwable);
        }
    }
    
    @Override
    public void onLoad(String mixinPackage) {
        
    }
    
    @Override
    public String getRefMapperConfig() {
        return null;
    }
    
    @Override
    public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
        return false;
    }
    
    @Override
    public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) {
        
    }
    
    @Override
    public List<String> getMixins() {
        return null;
    }
    
    @Override
    public void preApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
        
    }
    
    @Override
    public void postApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
        
    }
}
