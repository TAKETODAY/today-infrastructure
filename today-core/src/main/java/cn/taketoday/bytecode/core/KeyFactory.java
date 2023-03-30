/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2022 All Rights Reserved.
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

package cn.taketoday.bytecode.core;

import java.lang.reflect.Method;
import java.security.ProtectionDomain;
import java.util.Collections;
import java.util.List;

import cn.taketoday.bytecode.ClassVisitor;
import cn.taketoday.bytecode.Label;
import cn.taketoday.bytecode.Opcodes;
import cn.taketoday.bytecode.Type;
import cn.taketoday.bytecode.commons.GeneratorAdapter;
import cn.taketoday.bytecode.commons.MethodSignature;
import cn.taketoday.lang.Constant;
import cn.taketoday.util.CollectionUtils;
import cn.taketoday.util.ReflectionUtils;

/**
 * Generates classes to handle multi-valued keys, for use in things such as Maps
 * and Sets. Code for <code>equals</code> and <code>hashCode</code> methods
 * follow the the rules laid out in <i>Effective Java</i> by Joshua Bloch.
 * <p>
 * To generate a <code>KeyFactory</code>, you need to supply an interface which
 * describes the structure of the key. The interface should have a single method
 * named <code>newInstance</code>, which returns an <code>Object</code>. The
 * arguments array can be <i>anything</i>--Objects, primitive values, or single
 * or multi-dimension arrays of either. For example:
 * <p>
 *
 * <pre>
 * private interface IntStringKey {
 *     public Object newInstance(int i, String s);
 * }
 * </pre>
 * <p>
 * Once you have made a <code>KeyFactory</code>, you generate a new key by
 * calling the <code>newInstance</code> method defined by your interface.
 * <p>
 *
 * <pre>
 * IntStringKey factory = (IntStringKey) KeyFactory.create(IntStringKey.class);
 * Object key1 = factory.newInstance(4, "Hello");
 * Object key2 = factory.newInstance(4, "World");
 * </pre>
 * <p>
 * <b>Note:</b> <code>hashCode</code> equality between two keys
 * <code>key1</code> and <code>key2</code> is only guaranteed if
 * <code>key1.equals(key2)</code> <i>and</i> the keys were produced by the same
 * factory.
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 */
@SuppressWarnings({ "rawtypes", "unchecked" })
public abstract class KeyFactory {

  private static final Type KEY_FACTORY = Type.fromClass(KeyFactory.class);
  private static final MethodSignature GET_SORT = MethodSignature.from("int getSort()");
  private static final MethodSignature GET_NAME = MethodSignature.from("String getName()");
  private static final MethodSignature APPEND_STRING = MethodSignature.from("StringBuffer append(String)");

  // generated numbers:
  private final static int[] PRIMES = {
          11, 73, 179, 331, 521, 787, 1213, 1823,
          2609, 3691, 5189, 7247, 10037, 13931, 19289,
          26627, 36683, 50441, 69403, 95401, 131129,
          180179, 247501, 340057, 467063, 641371,
          880603, 1209107, 1660097, 2279161, 3129011,
          4295723, 5897291, 8095873, 11114263, 15257791,
          20946017, 28754629, 39474179, 54189869, 74391461,
          102123817, 140194277, 192456917, 264202273, 362693231,
          497900099, 683510293, 938313161, 1288102441, 1768288259
  };

  public static final Customizer CLASS_BY_NAME = (GeneratorAdapter e, Type type) -> {
    if (type.equals(Type.TYPE_CLASS)) {
      e.invokeVirtual(Type.TYPE_CLASS, GET_NAME);
    }
  };

  /**
   * {@link Type#hashCode()} is very expensive as it traverses full descriptor to
   * calculate hash code. This customizer uses {@link Type#getSort()} as a hash
   * code.
   */
  public static final HashCodeCustomizer HASH_ASM_TYPE = (GeneratorAdapter e, Type type) -> {
    if (Type.TYPE_TYPE.equals(type)) {
      e.invokeVirtual(type, GET_SORT);
      return true;
    }
    return false;
  };

  protected KeyFactory() { }

  public static <T> T create(Class<T> keyInterface) {
    return create(keyInterface, null);
  }

  public static <T> T create(Class<T> keyInterface, Customizer customizer) {
    return create(keyInterface.getClassLoader(), keyInterface, customizer);
  }

  public static <T> T create(Class<T> keyInterface, KeyFactoryCustomizer first, List<KeyFactoryCustomizer> next) {
    return create(keyInterface.getClassLoader(), keyInterface, first, next);
  }

  public static <T> T create(ClassLoader loader, Class<T> keyInterface, Customizer customizer) {
    return create(loader, keyInterface, customizer, Collections.emptyList());
  }

  public static <T> T create(
          ClassLoader loader, Class<T> keyInterface, KeyFactoryCustomizer customizer, List<KeyFactoryCustomizer> next) //
  {
    Generator gen = new Generator();
    gen.setInterface(keyInterface);
    gen.setNeighbor(keyInterface);// @since 4.0

    if (customizer != null) {
      gen.addCustomizer(customizer);
    }

    if (CollectionUtils.isNotEmpty(next)) {
      for (KeyFactoryCustomizer keyFactoryCustomizer : next) {
        gen.addCustomizer(keyFactoryCustomizer);
      }
    }
    gen.setClassLoader(loader);
    return (T) gen.create();
  }

  public static class Generator extends AbstractClassGenerator {

    private static final Class[] KNOWN_CUSTOMIZER_TYPES = new Class[] { Customizer.class, FieldTypeCustomizer.class };

    private Class keyInterface;
    private final CustomizerRegistry customizers = new CustomizerRegistry(KNOWN_CUSTOMIZER_TYPES);
    private int constant;
    private int multiplier;

    public Generator() {
      super(KeyFactory.class);
    }

    protected ClassLoader getDefaultClassLoader() {
      return keyInterface.getClassLoader();
    }

    protected ProtectionDomain getProtectionDomain() {
      return ReflectionUtils.getProtectionDomain(keyInterface);
    }

    public void addCustomizer(KeyFactoryCustomizer customizer) {
      customizers.add(customizer);
    }

    public <T> List<T> getCustomizers(Class<T> klass) {
      return customizers.get(klass);
    }

    public void setInterface(Class keyInterface) {
      this.keyInterface = keyInterface;
    }

    public KeyFactory create() {
      setNamePrefix(keyInterface.getName());
      return (KeyFactory) super.create(keyInterface.getName());
    }

    public void setHashConstant(int constant) {
      this.constant = constant;
    }

    public void setHashMultiplier(int multiplier) {
      this.multiplier = multiplier;
    }

    protected Object firstInstance(Class type) {
      return ReflectionUtils.newInstance(type);
    }

    protected Object nextInstance(Object instance) {
      return instance;
    }

    public void generateClass(ClassVisitor v) {
      ClassEmitter ce = new ClassEmitter(v);

      Method newInstance = CglibReflectUtils.findNewInstance(keyInterface);
      if (!newInstance.getReturnType().equals(Object.class)) {
        throw new IllegalArgumentException("newInstance method must return Object");
      }

      Type[] parameterTypes = Type.getTypes(newInstance.getParameterTypes());
      ce.beginClass(Opcodes.JAVA_VERSION, //
              Opcodes.ACC_PUBLIC, //
              getClassName(), //
              KEY_FACTORY, //
              Type.array(Type.fromClass(keyInterface)), //
              Constant.SOURCE_FILE//
      );

      EmitUtils.nullConstructor(ce);
      EmitUtils.factoryMethod(ce, MethodSignature.from(newInstance));

      int seed = 0;
      CodeEmitter e = ce.beginMethod(Opcodes.ACC_PUBLIC, MethodSignature.forConstructor(parameterTypes));
      e.loadThis();
      e.super_invoke_constructor();
      e.loadThis();
      List<FieldTypeCustomizer> fieldTypeCustomizers = getCustomizers(FieldTypeCustomizer.class);
      for (int i = 0; i < parameterTypes.length; i++) {
        Type parameterType = parameterTypes[i];
        Type fieldType = parameterType;
        for (FieldTypeCustomizer customizer : fieldTypeCustomizers) {
          fieldType = customizer.getOutType(i, fieldType);
        }
        seed += fieldType.hashCode();
        ce.declare_field(Opcodes.ACC_PRIVATE | Opcodes.ACC_FINAL, getFieldName(i), fieldType, null);
        e.dup();
        e.loadArg(i);
        for (FieldTypeCustomizer customizer : fieldTypeCustomizers) {
          customizer.customize(e, i, parameterType);
        }
        e.putField(getFieldName(i));
      }
      e.returnValue();
      e.end_method();

      // hash code
      e = ce.beginMethod(Opcodes.ACC_PUBLIC, MethodSignature.HASH_CODE);
      int hc = (constant != 0) ? constant : PRIMES[Math.abs(seed) % PRIMES.length];
      int hm = (multiplier != 0) ? multiplier : PRIMES[Math.abs(seed * 13) % PRIMES.length];
      e.push(hc);
      for (int i = 0; i < parameterTypes.length; i++) {
        e.loadThis();
        e.getField(getFieldName(i));
        EmitUtils.hashCode(e, parameterTypes[i], hm, customizers);
      }
      e.returnValue();
      e.end_method();

      // equals
      e = ce.beginMethod(Opcodes.ACC_PUBLIC, MethodSignature.EQUALS);
      Label fail = e.newLabel();
      e.loadArg(0);
      e.instance_of_this();
      e.ifJump(CodeEmitter.EQ, fail);
      for (int i = 0; i < parameterTypes.length; i++) {
        e.loadThis();
        e.getField(getFieldName(i));
        e.loadArg(0);
        e.checkcast_this();
        e.getField(getFieldName(i));
        EmitUtils.notEquals(e, parameterTypes[i], fail, customizers);
      }
      e.push(1);
      e.returnValue();
      e.mark(fail);
      e.push(0);
      e.returnValue();
      e.end_method();

      // toString
      e = ce.beginMethod(Opcodes.ACC_PUBLIC, MethodSignature.TO_STRING);
      e.newInstance(Type.TYPE_STRING_BUFFER);
      e.dup();
      e.invokeConstructor(Type.TYPE_STRING_BUFFER);
      for (int i = 0; i < parameterTypes.length; i++) {
        if (i > 0) {
          e.push(", ");
          e.invokeVirtual(Type.TYPE_STRING_BUFFER, APPEND_STRING);
        }
        e.loadThis();
        e.getField(getFieldName(i));
        EmitUtils.appendString(e, parameterTypes[i], EmitUtils.DEFAULT_DELIMITERS, customizers);
      }
      e.invokeVirtual(Type.TYPE_STRING_BUFFER, MethodSignature.TO_STRING);
      e.returnValue();
      e.end_method();

      ce.endClass();
    }

    private String getFieldName(int arg) {
      return "today$field_" + arg;
    }
  }
}
