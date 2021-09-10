/*
 * Original Author -> 杨海健 (taketoday@foxmail.com) https://taketoday.cn
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

package cn.taketoday.core.support;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Executable;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

import cn.taketoday.asm.ClassReader;
import cn.taketoday.asm.ClassVisitor;
import cn.taketoday.asm.Label;
import cn.taketoday.asm.MethodVisitor;
import cn.taketoday.asm.Opcodes;
import cn.taketoday.asm.Type;
import cn.taketoday.context.ApplicationContextException;
import cn.taketoday.core.Constant;
import cn.taketoday.core.ParameterNameDiscoverer;
import cn.taketoday.util.ClassUtils;
import cn.taketoday.util.ConcurrentCache;
import cn.taketoday.util.ReflectionUtils;

/**
 * @author TODAY 2021/9/10 22:34
 * @since 4.0
 */
public class LocalVariableTableParameterNameDiscoverer extends ParameterNameDiscoverer {
  static final boolean defaultEnableParamNameTypeChecking = Boolean.parseBoolean(
          System.getProperty("ClassUtils.enableParamNameTypeChecking", "true"));
  private boolean enableParamNameTypeChecking = defaultEnableParamNameTypeChecking;

  final ParameterFunction PARAMETER_NAMES_FUNCTION = new ParameterFunction();
  static final ConcurrentCache<Class<?>, Map<Executable, String[]>>
          PARAMETER_NAMES_CACHE = ConcurrentCache.fromSize(64);

  @Override
  public String[] getInternal(Executable executable) {
    return PARAMETER_NAMES_CACHE.get(executable.getDeclaringClass(), PARAMETER_NAMES_FUNCTION).get(executable);
  }

  public void setEnableParamNameTypeChecking(final boolean enableParamNameTypeChecking) {
    this.enableParamNameTypeChecking = enableParamNameTypeChecking;
  }

  final class ParameterFunction implements Function<Class<?>, Map<Executable, String[]>> {

    @Override
    public Map<Executable, String[]> apply(final Class<?> declaringClass) {
      final String classFile = declaringClass.getName()
              .replace(Constant.PACKAGE_SEPARATOR, Constant.PATH_SEPARATOR)
              .concat(ClassUtils.CLASS_FILE_SUFFIX);
      final ClassLoader classLoader = ClassUtils.getClassLoader();
      try (InputStream resourceAsStream = classLoader.getResourceAsStream(classFile)) {
        final ClassNode classVisitor = new ClassNode();
        new ClassReader(resourceAsStream).accept(classVisitor, 0);

        final ArrayList<MethodNode> methodNodes = classVisitor.methodNodes;
        final HashMap<Executable, String[]> map = new HashMap<>(methodNodes.size());
        for (final MethodNode methodNode : methodNodes) {
          final Type[] argumentTypes = Type.getArgumentTypes(methodNode.desc);
          final Class<?>[] argTypes;
          if (argumentTypes.length == 0) {
            argTypes = Constant.EMPTY_CLASS_ARRAY; // fixed @since 3.0.1
          }
          else {
            argTypes = new Class<?>[argumentTypes.length];
            int idx = 0;
            for (final Type argumentType : argumentTypes) {
              argTypes[idx++] = ClassUtils.forName(argumentType.getClassName());
            }
          }
          final Method method = ReflectionUtils.findMethod(declaringClass, methodNode.name, argTypes);
          if (method == null) {
            throw new NoSuchMethodException(
                    "No such method named: '" + methodNode.name + "' argTypes: '"
                            + Arrays.toString(argTypes) + "' in: " + declaringClass);
          }

          final int parameterCount = method.getParameterCount();
          if (parameterCount == 0) {
            map.put(method, Constant.EMPTY_STRING_ARRAY);
            continue;
          }
          final String[] paramNames = new String[parameterCount];
          if (Modifier.isAbstract(method.getModifiers()) || method.isBridge() || method.isSynthetic()) {
            int idx = 0;
            for (final Parameter parameter : method.getParameters()) {
              paramNames[idx++] = parameter.getName();
            }
            map.put(method, paramNames);
            continue;
          }

          final ArrayList<LocalVariable> localVariables = methodNode.localVariables;
          if (localVariables.size() >= parameterCount) {
            int offset = Modifier.isStatic(method.getModifiers()) ? 0 : 1;
            if (enableParamNameTypeChecking) { // enable check params types
              // check params types
              int idx = offset; // localVariable index
              int start = 0; // loop control
              while (start < parameterCount) {
                final Type argument = argumentTypes[start];
                if (!argument.equals(Type.fromDescriptor(localVariables.get(idx++).descriptor))) {
                  idx = ++offset;
                  start = 0; //reset
                }
                else {
                  paramNames[start] = localVariables.get(start + offset).name;
                  start++;
                }
              }
            }
            else {
              for (int idx = 0; idx < parameterCount; idx++) {
                paramNames[idx] = localVariables.get(idx + offset).name;
              }
            }
          }
          // @since 3.0.4 for gc
          localVariables.clear();
          methodNode.localVariables = null;

          map.put(method, paramNames);
        }
        // @since 3.0.4 for gc
        methodNodes.clear();
        classVisitor.methodNodes = null;
        return map;
      }
      catch (IOException | ClassNotFoundException | NoSuchMethodException | IndexOutOfBoundsException e) {
        throw new ApplicationContextException("When visit declaring class: [" + declaringClass.getName() + ']', e);
      }
    }

  }

  static final class ClassNode extends ClassVisitor {
    private ArrayList<MethodNode> methodNodes = new ArrayList<>();

    @Override
    public MethodVisitor visitMethod(int access,
                                     String name,
                                     String descriptor,
                                     String signature,
                                     String[] exceptions) {

      if (isSyntheticOrBridged(access)
              || Constant.CONSTRUCTOR_NAME.equals(name)
              || Constant.STATIC_CLASS_INIT.equals(name)) {
        return null;
      }
      final MethodNode methodNode = new MethodNode(name, descriptor);
      methodNodes.add(methodNode);
      return methodNode;
    }

    private static boolean isSyntheticOrBridged(int access) {
      return (((access & Opcodes.ACC_SYNTHETIC) | (access & Opcodes.ACC_BRIDGE)) > 0);
    }
  }

  static final class MethodNode extends MethodVisitor {
    private final String name;
    private final String desc;
    private ArrayList<LocalVariable> localVariables = new ArrayList<>();

    MethodNode(final String name, final String desc) {
      this.name = name;
      this.desc = desc;
    }

    @Override
    public void visitLocalVariable(String name,
                                   String descriptor,
                                   String signature,
                                   Label start, Label end, int index) {

      localVariables.add(new LocalVariable(name, descriptor));
    }
  }

  static class LocalVariable {
    String name;
    String descriptor;

    LocalVariable(String name, String descriptor) {
      this.name = name;
      this.descriptor = descriptor;
    }
  }

}
