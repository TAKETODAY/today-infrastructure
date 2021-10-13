/*
 * Copyright 2003 The Apache Software Foundation
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package cn.taketoday.core.bytecode.util;

import java.util.Arrays;
import java.util.List;

import cn.taketoday.core.bytecode.ClassVisitor;
import cn.taketoday.core.bytecode.Label;
import cn.taketoday.core.bytecode.Opcodes;
import cn.taketoday.core.bytecode.Type;
import cn.taketoday.core.bytecode.commons.MethodSignature;
import cn.taketoday.core.bytecode.core.AbstractClassGenerator;
import cn.taketoday.core.bytecode.core.ClassEmitter;
import cn.taketoday.core.bytecode.core.CodeEmitter;
import cn.taketoday.core.bytecode.core.EmitUtils;
import cn.taketoday.core.bytecode.core.KeyFactory;
import cn.taketoday.core.bytecode.core.ObjectSwitchCallback;
import cn.taketoday.util.ReflectionUtils;

import static cn.taketoday.core.bytecode.Opcodes.ACC_PUBLIC;
import static cn.taketoday.core.bytecode.Opcodes.JAVA_VERSION;
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
   * @param strings
   *         the array of String keys; must be the same length as the value
   *         array
   * @param ints
   *         the array of integer results; must be the same length as the key
   *         array
   * @param fixedInput
   *         if false, an unknown key will be returned from {@link #intValue}
   *         as <code>-1</code>; if true, the result will be undefined, and the
   *         resulting code will be faster
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
   * @param s
   *         the key
   *
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
     * @param strings
     *         the array of String keys; must be the same length as the value
     *         array
     *
     * @see #setInts
     */
    public void setStrings(String[] strings) {
      this.strings = strings;
    }

    /**
     * Set the array of integer results.
     *
     * @param ints
     *         the array of integer results; must be the same length as the key
     *         array
     *
     * @see #setStrings
     */
    public void setInts(int[] ints) {
      this.ints = ints;
    }

    /**
     * Configure how unknown String keys will be handled.
     *
     * @param fixedInput
     *         if false, an unknown key will be returned from {@link #intValue}
     *         as <code>-1</code>; if true, the result will be undefined, and the
     *         resulting code will be faster
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
      ce.beginClass(JAVA_VERSION, ACC_PUBLIC, getClassName(), STRING_SWITCHER, null, SOURCE_FILE);
      EmitUtils.nullConstructor(ce);
      final CodeEmitter e = ce.beginMethod(ACC_PUBLIC, INT_VALUE);
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
