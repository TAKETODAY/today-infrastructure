/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2021 All Rights Reserved.
 *
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see [http://www.gnu.org/licenses/]
 */

package cn.taketoday.core;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Executable;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

import cn.taketoday.core.bytecode.ClassReader;
import cn.taketoday.core.bytecode.ClassVisitor;
import cn.taketoday.core.bytecode.Label;
import cn.taketoday.core.bytecode.MethodVisitor;
import cn.taketoday.core.bytecode.Opcodes;
import cn.taketoday.core.bytecode.Type;
import cn.taketoday.core.bytecode.commons.MethodSignature;
import cn.taketoday.lang.Nullable;
import cn.taketoday.logging.Logger;
import cn.taketoday.logging.LoggerFactory;
import cn.taketoday.util.ClassUtils;

/**
 * Implementation of {@link ParameterNameDiscoverer} that uses the LocalVariableTable
 * information in the method attributes to discover parameter names. Returns
 * {@code null} if the class file was compiled without debug information.
 *
 * <p>Uses ObjectWeb's ASM library for analyzing class files. Each discoverer instance
 * caches the ASM discovered information for each introspected Class, in a thread-safe
 * manner. It is recommended to reuse ParameterNameDiscoverer instances as far as possible.
 *
 * @author TODAY 2021/9/10 22:34
 * @since 4.0
 */
public class LocalVariableTableParameterNameDiscoverer
        extends ParameterNameDiscoverer implements Function<Class<?>, Map<Executable, String[]>> {
  private static final Logger log = LoggerFactory.getLogger(LocalVariableTableParameterNameDiscoverer.class);

  // marker object for classes that do not have any debug info
  private static final Map<Executable, String[]> NO_DEBUG_INFO_MAP = Collections.emptyMap();

  // the cache uses a nested index (value is a map) to keep the top level cache relatively small in size
  private final ConcurrentHashMap<Class<?>, Map<Executable, String[]>>
          parameterNamesCache = new ConcurrentHashMap<>(32);

  @Override
  public String[] doGet(Executable executable) {
    Class<?> declaringClass = executable.getDeclaringClass();
    Map<Executable, String[]> map = parameterNamesCache.computeIfAbsent(declaringClass, this);
    return map != NO_DEBUG_INFO_MAP ? map.get(executable) : null;
  }

  @Override
  public Map<Executable, String[]> apply(Class<?> clazz) {
    return inspectClass(clazz);
  }

  /**
   * Inspects the target class.
   * <p>Exceptions will be logged, and a marker map returned to indicate the
   * lack of debug information.
   */
  private Map<Executable, String[]> inspectClass(Class<?> clazz) {
    try (InputStream is = clazz.getResourceAsStream(ClassUtils.getClassFileName(clazz))) {
      if (is == null) {
        // We couldn't load the class file, which is not fatal as it
        // simply means this method of discovering parameter names won't work.
        log.debug("Cannot find '.class' file for class [{}] " +
                "- unable to determine constructor/method parameter names", clazz);
        return NO_DEBUG_INFO_MAP;
      }
      ClassReader classReader = new ClassReader(is);
      Map<Executable, String[]> map = new ConcurrentHashMap<>(32);
      classReader.accept(new ParameterNameDiscoveringVisitor(clazz, map), 0);
      return map;
    }
    catch (IOException ex) {
      log.debug("Exception thrown while reading '.class' file for class " +
              "[{}] - unable to determine constructor/method parameter names", clazz, ex);
    }
    catch (IllegalArgumentException ex) {
      log.debug("ASM ClassReader failed to parse class file [{}]," +
              " probably due to a new Java class file version that isn't supported yet " +
              "- unable to determine constructor/method parameter names", clazz, ex);
    }
    // ignore
    return NO_DEBUG_INFO_MAP;
  }

  /**
   * Helper class that inspects all methods and constructors and then
   * attempts to find the parameter names for the given {@link Executable}.
   */
  private static class ParameterNameDiscoveringVisitor extends ClassVisitor {
    private static final String STATIC_CLASS_INIT = MethodSignature.STATIC_CLASS_INIT;

    private final Class<?> clazz;
    private final Map<Executable, String[]> executableMap;

    public ParameterNameDiscoveringVisitor(Class<?> clazz, Map<Executable, String[]> executableMap) {
      this.clazz = clazz;
      this.executableMap = executableMap;
    }

    @Override
    @Nullable
    public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
      // exclude synthetic + bridged && static class initialization
      if (!isSyntheticOrBridged(access) && !STATIC_CLASS_INIT.equals(name)) {
        return new LocalVariableTableVisitor(clazz, executableMap, name, desc, isStatic(access));
      }
      return null;
    }

    private static boolean isSyntheticOrBridged(int access) {
      return ((access & Opcodes.ACC_SYNTHETIC) | (access & Opcodes.ACC_BRIDGE)) > 0;
    }

    private static boolean isStatic(int access) {
      return (access & Opcodes.ACC_STATIC) > 0;
    }
  }

  private static class LocalVariableTableVisitor extends MethodVisitor {
    private static final String CONSTRUCTOR = MethodSignature.CONSTRUCTOR_NAME;

    private final String name;
    private final Type[] args;
    private final Class<?> clazz;
    private final boolean isStatic;
    private final String[] parameterNames;
    private final Map<Executable, String[]> executableMap;

    private boolean hasLvtInfo = false;

    /*
     * The nth entry contains the slot index of the LVT table entry holding the
     * argument name for the nth parameter.
     */
    private final int[] lvtSlotIndex;

    public LocalVariableTableVisitor(
            Class<?> clazz, Map<Executable, String[]> map, String name, String desc, boolean isStatic) {
      this.name = name;
      this.clazz = clazz;
      this.executableMap = map;
      this.isStatic = isStatic;
      this.args = Type.getArgumentTypes(desc);
      this.parameterNames = new String[this.args.length];
      this.lvtSlotIndex = computeLvtSlotIndices(isStatic, this.args);
    }

    @Override
    public void visitLocalVariable(
            String name, String description, String signature, Label start, Label end, int index) {
      this.hasLvtInfo = true;
      for (int i = 0; i < this.lvtSlotIndex.length; i++) {
        if (this.lvtSlotIndex[i] == index) {
          this.parameterNames[i] = name;
        }
      }
    }

    @Override
    public void visitEnd() {
      if (this.hasLvtInfo || (this.isStatic && this.parameterNames.length == 0)) {
        // visitLocalVariable will never be called for static no args methods
        // which doesn't use any local variables.
        // This means that hasLvtInfo could be false for that kind of methods
        // even if the class has local variable info.
        this.executableMap.put(resolveExecutable(), this.parameterNames);
      }
    }

    private Executable resolveExecutable() {
      final Type[] args = this.args;
      ClassLoader loader = this.clazz.getClassLoader();
      Class<?>[] argTypes = new Class<?>[args.length];
      for (int i = 0; i < args.length; i++) {
        argTypes[i] = ClassUtils.resolveClassName(args[i].getClassName(), loader);
      }
      try {
        if (CONSTRUCTOR.equals(this.name)) {
          return this.clazz.getDeclaredConstructor(argTypes);
        }
        return this.clazz.getDeclaredMethod(this.name, argTypes);
      }
      catch (NoSuchMethodException ex) {
        throw new IllegalStateException(
                "Method [" + this.name + "] was discovered in the .class file but cannot be resolved in the class object", ex);
      }
    }

    private static int[] computeLvtSlotIndices(boolean isStatic, Type[] paramTypes) {
      int[] lvtIndex = new int[paramTypes.length];
      int nextIndex = (isStatic ? 0 : 1);
      for (int i = 0; i < paramTypes.length; i++) {
        lvtIndex[i] = nextIndex;
        if (isWideType(paramTypes[i])) {
          nextIndex += 2;
        }
        else {
          nextIndex++;
        }
      }
      return lvtIndex;
    }

    private static boolean isWideType(Type aType) {
      // float is not a wide type
      return (aType == Type.LONG_TYPE || aType == Type.DOUBLE_TYPE);
    }
  }

}
