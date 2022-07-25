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
package cn.taketoday.bytecode.util;

import java.util.Arrays;
import java.util.List;

import cn.taketoday.bytecode.ClassVisitor;
import cn.taketoday.bytecode.Label;
import cn.taketoday.bytecode.Opcodes;
import cn.taketoday.bytecode.Type;
import cn.taketoday.bytecode.core.AbstractClassGenerator;
import cn.taketoday.bytecode.core.ClassEmitter;
import cn.taketoday.bytecode.core.CodeEmitter;
import cn.taketoday.bytecode.core.EmitUtils;
import cn.taketoday.bytecode.core.KeyFactory;
import cn.taketoday.bytecode.core.ObjectSwitchCallback;
import cn.taketoday.bytecode.commons.MethodSignature;
import cn.taketoday.util.ReflectionUtils;

import static cn.taketoday.lang.Constant.SOURCE_FILE;

/**
 * This class implements a simple String->int mapping for a fixed set of keys.
 */
@SuppressWarnings({ "rawtypes", "unchecked" })
public abstract class StringSwitcher {

  private static final Type STRING_SWITCHER = Type.fromClass(StringSwitcher.class);
  private static final MethodSignature INT_VALUE = MethodSignature.from("int intValue(String)");
  private static final StringSwitcherKey KEY_FACTORY = KeyFactory.create(StringSwitcherKey.class);

  interface StringSwitcherKey {
    Object newInstance(String[] strings, int[] ints, boolean fixedInput);
  }

  /**
   * Helper method to create a StringSwitcher. For finer control over the
   * generated instance, use a new instance of StringSwitcher.Generator instead of
   * this static method.
   *
   * @param strings the array of String keys; must be the same length as the value
   * array
   * @param ints the array of integer results; must be the same length as the key
   * array
   * @param fixedInput if false, an unknown key will be returned from {@link #intValue}
   * as <code>-1</code>; if true, the result will be undefined, and the
   * resulting code will be faster
   */
  public static StringSwitcher create(String[] strings, int[] ints, boolean fixedInput) {
    Generator gen = new Generator();
    gen.setStrings(strings);
    gen.setInts(ints);
    gen.setFixedInput(fixedInput);
    return gen.create();
  }

  protected StringSwitcher() { }

  /**
   * Return the integer associated with the given key.
   *
   * @param s the key
   * @return the associated integer value, or <code>-1</code> if the key is
   * unknown (unless <code>fixedInput</code> was specified when this
   * <code>StringSwitcher</code> was created, in which case the return
   * value for an unknown key is undefined)
   */
  abstract public int intValue(String s);

  public static class Generator extends AbstractClassGenerator {

    private int[] ints;
    private String[] strings;
    private boolean fixedInput;

    public Generator() {
      super(StringSwitcher.class);
    }

    /**
     * Set the array of recognized Strings.
     *
     * @param strings the array of String keys; must be the same length as the value
     * array
     * @see #setInts
     */
    public void setStrings(String[] strings) {
      this.strings = strings;
    }

    /**
     * Set the array of integer results.
     *
     * @param ints the array of integer results; must be the same length as the key
     * array
     * @see #setStrings
     */
    public void setInts(int[] ints) {
      this.ints = ints;
    }

    /**
     * Configure how unknown String keys will be handled.
     *
     * @param fixedInput if false, an unknown key will be returned from {@link #intValue}
     * as <code>-1</code>; if true, the result will be undefined, and the
     * resulting code will be faster
     */
    public void setFixedInput(boolean fixedInput) {
      this.fixedInput = fixedInput;
    }

    @Override
    protected ClassLoader getDefaultClassLoader() {
      return getClass().getClassLoader();
    }

    /**
     * Generate the <code>StringSwitcher</code>.
     */
    public StringSwitcher create() {
      setNamePrefix(StringSwitcher.class.getName());
      Object key = KEY_FACTORY.newInstance(strings, ints, fixedInput);
      return (StringSwitcher) super.create(key);
    }

    @Override
    public void generateClass(ClassVisitor v) throws Exception {
      final ClassEmitter ce = new ClassEmitter(v);
      ce.beginClass(Opcodes.JAVA_VERSION, Opcodes.ACC_PUBLIC, getClassName(), STRING_SWITCHER, null, SOURCE_FILE);
      EmitUtils.nullConstructor(ce);
      final CodeEmitter e = ce.beginMethod(Opcodes.ACC_PUBLIC, INT_VALUE);
      e.loadArg(0);
      final List<String> stringList = Arrays.asList(strings);
      int style = fixedInput ? Opcodes.SWITCH_STYLE_HASHONLY : Opcodes.SWITCH_STYLE_HASH;
      EmitUtils.stringSwitch(e, strings, style, new ObjectSwitchCallback() {

        @Override
        public void processCase(Object key, Label end) {
          e.push(ints[stringList.indexOf(key)]);
          e.returnValue();
        }

        @Override
        public void processDefault() {
          e.push(-1);
          e.returnValue();
        }
      });
      e.end_method();
      ce.endClass();
    }

    @Override
    protected Object firstInstance(Class type) {
      return ReflectionUtils.newInstance(type);
    }

    @Override
    protected Object nextInstance(Object instance) {
      return instance;
    }
  }
}
