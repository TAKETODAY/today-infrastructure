/*
 * Copyright 2003,2004 The Apache Software Foundation
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
package cn.taketoday.cglib.core;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.BitSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import cn.taketoday.asm.Label;
import cn.taketoday.asm.Opcodes;
import cn.taketoday.asm.Type;
import cn.taketoday.asm.commons.GeneratorAdapter;
import cn.taketoday.asm.commons.Local;
import cn.taketoday.asm.commons.MethodSignature;
import cn.taketoday.asm.commons.TableSwitchGenerator;
import cn.taketoday.cglib.core.internal.CustomizerRegistry;
import cn.taketoday.core.Constant;
import cn.taketoday.util.CollectionUtils;
import cn.taketoday.util.StringUtils;

@SuppressWarnings({ "rawtypes", "unchecked" })
public abstract class EmitUtils {

  public static final Type TYPE_BIG_INTEGER = Type.fromInternalName("java/math/BigInteger");
  public static final Type TYPE_BIG_DECIMAL = Type.fromInternalName("java/math/BigDecimal");

  private static final MethodSignature CSTRUCT_THROWABLE = MethodSignature.forConstructor("Throwable");

  private static final MethodSignature LENGTH = MethodSignature.from("int length()");
  private static final MethodSignature GET_NAME = MethodSignature.from("String getName()");
  private static final MethodSignature SET_LENGTH = MethodSignature.from("void setLength(int)");
  private static final MethodSignature FOR_NAME = MethodSignature.from("Class forName(String)");
  private static final MethodSignature STRING_CHAR_AT = MethodSignature.from("char charAt(int)");
  private static final MethodSignature APPEND_INT = MethodSignature.from("StringBuffer append(int)");
  private static final MethodSignature APPEND_LONG = MethodSignature.from("StringBuffer append(long)");
  private static final MethodSignature APPEND_CHAR = MethodSignature.from("StringBuffer append(char)");
  private static final MethodSignature APPEND_FLOAT = MethodSignature.from("StringBuffer append(float)");
  private static final MethodSignature APPEND_DOUBLE = MethodSignature.from("StringBuffer append(double)");
  private static final MethodSignature APPEND_STRING = MethodSignature.from("StringBuffer append(String)");
  private static final MethodSignature APPEND_BOOLEAN = MethodSignature.from("StringBuffer append(boolean)");
  private static final MethodSignature FLOAT_TO_INT_BITS = MethodSignature.from("int floatToIntBits(float)");
  private static final MethodSignature DOUBLE_TO_LONG_BITS = MethodSignature.from("long doubleToLongBits(double)");

  private static final MethodSignature GET_DECLARED_METHOD = //
          MethodSignature.from("java.lang.reflect.Method getDeclaredMethod(String, Class[])");

  public static final ArrayDelimiters DEFAULT_DELIMITERS = new ArrayDelimiters("{", ", ", "}");

  public static void factoryMethod(ClassEmitter ce, MethodSignature sig) {

    CodeEmitter e = ce.beginMethod(Opcodes.ACC_PUBLIC, sig);
    e.new_instance_this();
    e.dup();
    e.load_args();
    e.invoke_constructor_this(MethodSignature.forConstructor(sig.getArgumentTypes()));
    e.return_value();
    e.end_method();
  }

  public static void nullConstructor(ClassEmitter ce) {
    CodeEmitter e = ce.beginMethod(Opcodes.ACC_PUBLIC, MethodSignature.EMPTY_CONSTRUCTOR);
    e.load_this();
    e.super_invoke_constructor();
    e.return_value();
    e.end_method();
  }

  /**
   * Process an array on the stack. Assumes the top item on the stack is an array
   * of the specified type. For each element in the array, puts the element on the
   * stack and triggers the callback.
   *
   * @param type
   *         the type of the array (type.isArray() must be true)
   * @param callback
   *         the callback triggered for each element
   */
  public static void processArray(GeneratorAdapter e, Type type, ProcessArrayCallback callback) {
    Type componentType = type.getComponentType();
    Local array = e.newLocal();
    Local loopvar = e.newLocal(Type.INT_TYPE);
    Label loopbody = e.newLabel();
    Label checkloop = e.newLabel();
    e.storeLocal(array);
    e.push(0);
    e.storeLocal(loopvar);
    e.goTo(checkloop);
    e.mark(loopbody);
    e.loadLocal(array);
    e.loadLocal(loopvar);
    e.arrayLoad(componentType);
    callback.processElement(componentType);
    e.iinc(loopvar, 1);

    e.mark(checkloop);
    e.loadLocal(loopvar);
    e.loadLocal(array);
    e.arrayLength();
    e.ifICmp(CodeEmitter.LT, loopbody);
  }

  /**
   * Process two arrays on the stack in parallel. Assumes the top two items on the
   * stack are arrays of the specified class. The arrays must be the same length.
   * For each pair of elements in the arrays, puts the pair on the stack and
   * triggers the callback.
   *
   * @param type
   *         the type of the arrays (type.isArray() must be true)
   * @param callback
   *         the callback triggered for each pair of elements
   */
  public static void processArrays(CodeEmitter e, Type type, ProcessArrayCallback callback) {
    Type componentType = type.getComponentType();
    Local array1 = e.newLocal();
    Local array2 = e.newLocal();
    Local loopvar = e.newLocal(Type.INT_TYPE);
    Label loopbody = e.newLabel();
    Label checkloop = e.newLabel();
    e.storeLocal(array1);
    e.storeLocal(array2);
    e.push(0);
    e.storeLocal(loopvar);
    e.goTo(checkloop);

    e.mark(loopbody);
    e.loadLocal(array1);
    e.loadLocal(loopvar);
    e.arrayLoad(componentType);
    e.loadLocal(array2);
    e.loadLocal(loopvar);
    e.arrayLoad(componentType);
    callback.processElement(componentType);
    e.iinc(loopvar, 1);

    e.mark(checkloop);
    e.loadLocal(loopvar);
    e.loadLocal(array1);
    e.arrayLength();
    e.ifIcmp(CodeEmitter.LT, loopbody);
  }

  public static void stringSwitch(CodeEmitter e, String[] strings, int switchStyle, ObjectSwitchCallback callback) {
    switch (switchStyle) {
      case Opcodes.SWITCH_STYLE_TRIE:
        stringSwitchTrie(e, strings, callback);
        break;
      case Opcodes.SWITCH_STYLE_HASH:
        stringSwitchHash(e, strings, callback, false);
        break;
      case Opcodes.SWITCH_STYLE_HASHONLY:
        stringSwitchHash(e, strings, callback, true);
        break;
      default:
        throw new IllegalArgumentException("unknown switch style " + switchStyle);
    }
  }

  private static void stringSwitchTrie(
          final CodeEmitter e, String[] strings, final ObjectSwitchCallback callback) {
    final Label def = e.newLabel();
    final Label end = e.newLabel();
    final Map<Integer, List<String>> buckets = CollectionUtils.buckets(strings, String::length);

    e.dup();
    e.invokeVirtual(Type.TYPE_STRING, LENGTH);
    e.tableSwitch(getSwitchKeys(buckets), new TableSwitchGenerator() {
      public void generateCase(int key, Label ignore_end) {
        List bucket = buckets.get(key);
        stringSwitchHelper(e, bucket, callback, def, end, 0);
      }

      public void generateDefault() {
        e.goTo(def);
      }
    });
    e.mark(def);
    e.pop();
    callback.processDefault();
    e.mark(end);
  }

  private static void stringSwitchHelper(final CodeEmitter e, List strings, final ObjectSwitchCallback callback,
                                         final Label def, final Label end, final int index) {
    final int len = ((String) strings.get(0)).length();
    final Map buckets = CollectionUtils.buckets(strings, new Function() {
      public Object apply(Object value) {
        return (int) ((String) value).charAt(index);
      }
    });
    e.dup();
    e.push(index);
    e.invokeVirtual(Type.TYPE_STRING, STRING_CHAR_AT);
    e.tableSwitch(getSwitchKeys(buckets), new TableSwitchGenerator() {
      public void generateCase(int key, Label ignore_end) {
        List bucket = (List) buckets.get(key);
        if (index + 1 == len) {
          e.pop();
          callback.processCase(bucket.get(0), end);
        }
        else {
          stringSwitchHelper(e, bucket, callback, def, end, index + 1);
        }
      }

      public void generateDefault() {
        e.goTo(def);
      }
    });
  }

  protected static <T> int[] getSwitchKeys(Map<Integer, List<T>> buckets) {
    final int[] keys = new int[buckets.size()];
    int i = 0;
    for (Integer k : buckets.keySet()) {
      keys[i++] = k;
    }
    Arrays.sort(keys);
    return keys;
  }

  private static void stringSwitchHash(
          final CodeEmitter e, final String[] strings,
          final ObjectSwitchCallback callback, final boolean skipEquals) {

    final Map<Integer, List<String>> buckets = CollectionUtils.buckets(strings, Object::hashCode);

    final Label def = e.newLabel();
    final Label end = e.newLabel();
    e.dup();
    e.invokeVirtual(Type.TYPE_OBJECT, MethodSignature.HASH_CODE);
    e.tableSwitch(getSwitchKeys(buckets), new TableSwitchGenerator() {
      public void generateCase(int key, Label ignore_end) {
        List<String> bucket = buckets.get(key);
        Label next = null;
        if (skipEquals && bucket.size() == 1) {
          e.pop();
          callback.processCase(bucket.get(0), end);
        }
        else {
          for (Iterator<String> it = bucket.iterator(); it.hasNext(); ) {
            final String string = it.next();
            if (next != null) {
              e.mark(next);
            }
            if (it.hasNext()) {
              e.dup();
            }
            e.push(string);
            e.invokeVirtual(Type.TYPE_OBJECT, MethodSignature.EQUALS);
            if (it.hasNext()) {
              e.ifJump(CodeEmitter.EQ, next = e.newLabel());
              e.pop();
            }
            else {
              e.ifJump(CodeEmitter.EQ, def);
            }
            callback.processCase(string, end);
          }
        }
      }

      public void generateDefault() {
        e.pop();
      }
    });
    e.mark(def);
    callback.processDefault();
    e.mark(end);
  }

  public static void loadClassThis(CodeEmitter e) {
    loadClassHelper(e, e.getClassEmitter().getClassType());
  }

  public static void loadClass(GeneratorAdapter e, Type type) {
    if (type.isPrimitive()) {
      if (type == Type.VOID_TYPE) {
        throw new IllegalArgumentException("cannot load void type");
      }
      e.getStatic(type.getBoxedType(), "TYPE", Type.TYPE_CLASS);
    }
    else {
      loadClassHelper((CodeEmitter) e, type);
    }
  }

  private static void loadClassHelper(CodeEmitter e, final Type type) {
    if (e.isStaticHook()) {
      // have to fall back on non-optimized load
      e.push(type.emulateClassGetName());
      e.invokeStatic(Type.TYPE_CLASS, FOR_NAME);
    }
    else {
      ClassEmitter ce = e.getClassEmitter();
      String typeName = type.emulateClassGetName();

      // TODO: can end up with duplicated field names when using chained transformers;
      // incorporate static hook # somehow
      String fieldName = "TODAY$LoadClass$".concat(escapeType(typeName));
      if (!ce.isFieldDeclared(fieldName)) {
        ce.declare_field(Opcodes.PRIVATE_FINAL_STATIC, fieldName, Type.TYPE_CLASS, null);
        CodeEmitter hook = ce.getStaticHook();
        hook.push(typeName);
        hook.invokeStatic(Type.TYPE_CLASS, FOR_NAME);
        hook.putStatic(ce.getClassType(), fieldName, Type.TYPE_CLASS);
      }
      e.getField(fieldName);
    }
  }

  public static void pushArray(GeneratorAdapter e, Object[] array) {
    e.push(array.length);
    e.newArray(Type.fromClass(remapComponentType(array.getClass().getComponentType())));

    for (int i = 0; i < array.length; i++) {
      e.dup();
      e.push(i);
      pushObject(e, array[i]);
      e.aastore();
    }
  }

  private static Class<?> remapComponentType(Class<?> componentType) {
    return componentType.equals(Type.class) ? Class.class : componentType;
  }

  public static void pushObject(GeneratorAdapter e, Object obj) {
    if (obj == null) {
      e.visitInsn(Opcodes.ACONST_NULL);
    }
    else {
      Class type = obj.getClass();
      if (type.isArray()) {
        pushArray(e, (Object[]) obj);
      }
      else if (obj instanceof String) {
        e.push((String) obj);
      }
      else if (obj instanceof Type) {
        loadClass(e, (Type) obj);
      }
      else if (obj instanceof Class) {
        loadClass(e, Type.fromClass((Class) obj));
      }
      else if (obj instanceof BigInteger) {
        e.newInstance(TYPE_BIG_INTEGER);
        e.dup();
        e.push(obj.toString());
        e.invokeConstructor(TYPE_BIG_INTEGER);
      }
      else if (obj instanceof BigDecimal) {
        e.newInstance(TYPE_BIG_DECIMAL);
        e.dup();
        e.push(obj.toString());
        e.invokeConstructor(TYPE_BIG_DECIMAL);
      }
      else {
        throw new IllegalArgumentException("unknown type: " + obj.getClass());
      }
    }
  }

  /**
   * @deprecated use {@link #hashCode(GeneratorAdapter, Type, int, CustomizerRegistry)}
   * instead
   */
  @Deprecated
  public static void hashCode(GeneratorAdapter e, Type type, int multiplier, final Customizer customizer) {
    hashCode(e, type, multiplier, CustomizerRegistry.singleton(customizer));
  }

  public static void hashCode(GeneratorAdapter e, Type type, int multiplier, final CustomizerRegistry registry) {
    if (type.isArray()) {
      hashArray(e, type, multiplier, registry);
    }
    else {
      e.swap(Type.INT_TYPE, type);
      e.push(multiplier);
      e.math(GeneratorAdapter.MUL, Type.INT_TYPE);
      e.swap(type, Type.INT_TYPE);
      if (type.isPrimitive()) {
        hashPrimitive(e, type);
      }
      else {
        hashObject(e, type, registry);
      }
      e.math(GeneratorAdapter.ADD, Type.INT_TYPE);
    }
  }

  private static void hashArray(final GeneratorAdapter e,
                                final Type type,
                                final int multiplier,
                                final CustomizerRegistry registry) //
  {
    Label skip = e.newLabel();
    Label end = e.newLabel();
    e.dup();
    e.ifNull(skip);

    processArray(e, type, (t) -> hashCode(e, t, multiplier, registry));

    e.goTo(end);
    e.mark(skip);
    e.pop();
    e.mark(end);
  }

  private static void hashObject(GeneratorAdapter e, Type type, CustomizerRegistry registry) {
    // (f == null) ? 0 : f.hashCode();
    Label skip = e.newLabel();
    Label end = e.newLabel();
    e.dup();
    e.ifNull(skip);
    boolean customHashCode = false;
    for (HashCodeCustomizer customizer : registry.get(HashCodeCustomizer.class)) {
      if (customizer.customize(e, type)) {
        customHashCode = true;
        break;
      }
    }
    if (!customHashCode) {
      for (Customizer customizer : registry.get(Customizer.class)) {
        customizer.customize(e, type);
      }
      e.invokeVirtual(Type.TYPE_OBJECT, MethodSignature.HASH_CODE);
    }
    e.goTo(end);
    e.mark(skip);
    e.pop();
    e.push(0);
    e.mark(end);
  }

  private static void hashPrimitive(GeneratorAdapter e, Type type) {
    switch (type.getSort()) {
      case Type.BOOLEAN:
        // f ? 0 : 1
        e.push(1);
        e.math(CodeEmitter.XOR, Type.INT_TYPE);
        break;
      case Type.FLOAT:
        // Float.floatToIntBits(f)
        e.invokeStatic(Type.TYPE_FLOAT, FLOAT_TO_INT_BITS);
        break;
      case Type.DOUBLE:
        // Double.doubleToLongBits(f), hash_code(Long.TYPE)
        e.invokeStatic(Type.TYPE_DOUBLE, DOUBLE_TO_LONG_BITS);
        // fall through
      case Type.LONG:
        hashLong(e);
    }
  }

  private static void hashLong(GeneratorAdapter e) {
    // (int)(f ^ (f >>> 32))
    e.dup2();
    e.push(32);
    e.math(GeneratorAdapter.USHR, Type.LONG_TYPE);
    e.math(GeneratorAdapter.XOR, Type.LONG_TYPE);
    e.cast(Type.LONG_TYPE, Type.INT_TYPE);
  }

  //     public static void not_equals(CodeEmitter e, Type type, Label notEquals) {
  //         not_equals(e, type, notEquals, null);
  //     }

  /**
   * @deprecated use
   * {@link #notEquals(CodeEmitter, Type, Label, CustomizerRegistry)}
   * instead
   */
  @Deprecated
  public static void notEquals(CodeEmitter e, Type type, final Label notEquals, final Customizer customizer) {
    notEquals(e, type, notEquals, CustomizerRegistry.singleton(customizer));
  }

  /**
   * Branches to the specified label if the top two items on the stack are not
   * equal. The items must both be of the specified class. Equality is determined
   * by comparing primitive values directly and by invoking the
   * <code>equals</code> method for Objects. Arrays are recursively processed in
   * the same manner.
   */
  public static void notEquals(final CodeEmitter e,
                               final Type type,
                               final Label notEquals,
                               final CustomizerRegistry registry) {
    final ProcessArrayCallback processArrayCallback = new ProcessArrayCallback() {
      public void processElement(Type type) {
        notEqualsHelper(e, type, notEquals, registry, this);
      }
    };
    processArrayCallback.processElement(type);
  }

  private static void notEqualsHelper(final CodeEmitter e,
                                      final Type type,
                                      final Label notEquals,
                                      final CustomizerRegistry registry,
                                      final ProcessArrayCallback callback)//
  {
    if (type.isPrimitive()) {
      e.ifCmp(type, CodeEmitter.NE, notEquals);
    }
    else {
      Label end = e.newLabel();
      nullcmp(e, notEquals, end);
      if (type.isArray()) {
        Label checkContents = e.newLabel();
        e.dup2();
        e.arrayLength();
        e.swap();
        e.arrayLength();
        e.ifIcmp(CodeEmitter.EQ, checkContents);
        e.pop2();
        e.goTo(notEquals);
        e.mark(checkContents);
        EmitUtils.processArrays(e, type, callback);
      }
      else {
        List<Customizer> customizers = registry.get(Customizer.class);
        if (!customizers.isEmpty()) {
          for (Customizer customizer : customizers) {
            customizer.customize(e, type);
          }
          e.swap();
          for (Customizer customizer : customizers) {
            customizer.customize(e, type);
          }
        }
        e.invokeVirtual(Type.TYPE_OBJECT, MethodSignature.EQUALS);
        e.ifJump(CodeEmitter.EQ, notEquals);
      }
      e.mark(end);
    }
  }

  /**
   * If both objects on the top of the stack are non-null, does nothing. If one is
   * null, or both are null, both are popped off and execution branches to the
   * respective label.
   *
   * @param oneNull
   *         label to branch to if only one of the objects is null
   * @param bothNull
   *         label to branch to if both of the objects are null
   */
  private static void nullcmp(CodeEmitter e, Label oneNull, Label bothNull) {
    e.dup2();
    Label nonNull = e.newLabel();
    Label oneNullHelper = e.newLabel();
    Label end = e.newLabel();
    e.ifNonNull(nonNull);
    e.ifNonNull(oneNullHelper);
    e.pop2();
    e.goTo(bothNull);

    e.mark(nonNull);
    e.ifNull(oneNullHelper);
    e.goTo(end);

    e.mark(oneNullHelper);
    e.pop2();
    e.goTo(oneNull);

    e.mark(end);
  }

  /* public static void to_string(CodeEmitter e, Type type, ArrayDelimiters
   * delims, CustomizerRegistry registry) {
   * e.new_instance(Constants.TYPE_STRING_BUFFER); e.dup();
   * e.invoke_constructor(Constants.TYPE_STRING_BUFFER); e.swap();
   * append_string(e, type, delims, registry);
   * e.invoke_virtual(Constants.TYPE_STRING_BUFFER, TO_STRING); } */

  /**
   * @deprecated use
   * {@link #appendString(CodeEmitter, Type, ArrayDelimiters, CustomizerRegistry)}
   * instead
   */
  @Deprecated
  public static void appendString(final CodeEmitter e,
                                  final Type type,
                                  final ArrayDelimiters delims,
                                  final Customizer customizer) {
    appendString(e, type, delims, CustomizerRegistry.singleton(customizer));
  }

  public static void appendString(final CodeEmitter e,
                                  final Type type,
                                  final ArrayDelimiters delims,
                                  final CustomizerRegistry registry) //
  {
    final ArrayDelimiters d = (delims != null) ? delims : DEFAULT_DELIMITERS;
    ProcessArrayCallback callback = new ProcessArrayCallback() {
      public void processElement(Type type) {
        appendStringHelper(e, type, d, registry, this);
        e.push(d.inside);
        e.invokeVirtual(Type.TYPE_STRING_BUFFER, APPEND_STRING);
      }
    };
    appendStringHelper(e, type, d, registry, callback);
  }

  private static void appendStringHelper(final CodeEmitter e,
                                         final Type type,
                                         final ArrayDelimiters delims,
                                         final CustomizerRegistry registry,
                                         final ProcessArrayCallback callback)//
  {
    Label skip = e.newLabel();
    Label end = e.newLabel();
    if (type.isPrimitive()) {
      switch (type.getSort()) {
        case Type.INT:
        case Type.SHORT:
        case Type.BYTE:
          e.invokeVirtual(Type.TYPE_STRING_BUFFER, APPEND_INT);
          break;
        case Type.DOUBLE:
          e.invokeVirtual(Type.TYPE_STRING_BUFFER, APPEND_DOUBLE);
          break;
        case Type.FLOAT:
          e.invokeVirtual(Type.TYPE_STRING_BUFFER, APPEND_FLOAT);
          break;
        case Type.LONG:
          e.invokeVirtual(Type.TYPE_STRING_BUFFER, APPEND_LONG);
          break;
        case Type.BOOLEAN:
          e.invokeVirtual(Type.TYPE_STRING_BUFFER, APPEND_BOOLEAN);
          break;
        case Type.CHAR:
          e.invokeVirtual(Type.TYPE_STRING_BUFFER, APPEND_CHAR);
          break;
      }
    }
    else if (type.isArray()) {
      e.dup();
      e.ifNull(skip);
      e.swap();
      if (delims != null && delims.before != null && !Constant.BLANK.equals(delims.before)) {
        e.push(delims.before);
        e.invokeVirtual(Type.TYPE_STRING_BUFFER, APPEND_STRING);
        e.swap();
      }
      EmitUtils.processArray(e, type, callback);
      shrinkStringBuffer(e, 2);
      if (delims != null && delims.after != null && !"".equals(delims.after)) {
        e.push(delims.after);
        e.invokeVirtual(Type.TYPE_STRING_BUFFER, APPEND_STRING);
      }
    }
    else {
      e.dup();
      e.ifNull(skip);
      for (Customizer customizer : registry.get(Customizer.class)) {
        customizer.customize(e, type);
      }
      e.invokeVirtual(Type.TYPE_OBJECT, MethodSignature.TO_STRING);
      e.invokeVirtual(Type.TYPE_STRING_BUFFER, APPEND_STRING);
    }
    e.goTo(end);
    e.mark(skip);
    e.pop();
    e.push("null");
    e.invokeVirtual(Type.TYPE_STRING_BUFFER, APPEND_STRING);
    e.mark(end);
  }

  private static void shrinkStringBuffer(final CodeEmitter e, final int amt) {
    e.dup();
    e.dup();
    e.invokeVirtual(Type.TYPE_STRING_BUFFER, LENGTH);
    e.push(amt);
    e.math(CodeEmitter.SUB, Type.INT_TYPE);
    e.invokeVirtual(Type.TYPE_STRING_BUFFER, SET_LENGTH);
  }

  static class ArrayDelimiters {
    public final String before;
    public final String inside;
    public final String after;

    public ArrayDelimiters(String before, String inside, String after) {
      this.before = before;
      this.inside = inside;
      this.after = after;
    }
  }

  public static void loadMethod(final CodeEmitter e, final MethodInfo method) {
    loadClass(e, method.getClassInfo().getType());
    e.push(method.getSignature().getName());
    pushObject(e, method.getSignature().getArgumentTypes());
    e.invokeVirtual(Type.TYPE_CLASS, GET_DECLARED_METHOD);
  }

  private interface ParameterTyper {

    Type[] getParameterTypes(MethodInfo member);
  }

  public static void methodSwitch(CodeEmitter e, List methods, ObjectSwitchCallback callback) {
    memberSwitchHelper(e, methods, callback, true);
  }

  public static void constructorSwitch(CodeEmitter e, List constructors, ObjectSwitchCallback callback) {
    memberSwitchHelper(e, constructors, callback, false);
  }

  private static void memberSwitchHelper(final CodeEmitter e, //
                                         final List members,
                                         final ObjectSwitchCallback callback, boolean useName)//
  {
    try {

      final HashMap<MethodInfo, Type[]> cache = new HashMap<>();
      final ParameterTyper cached = (MethodInfo member) -> {
        Type[] types = cache.get(member);
        if (types == null) {
          cache.put(member, types = member.getSignature().getArgumentTypes());
        }
        return types;
      };

      final Label def = e.newLabel();
      final Label end = e.newLabel();
      if (useName) {
        e.swap();
        final Map<String, List<MethodInfo>> buckets = //
                CollectionUtils.buckets(members, (MethodInfo value) -> value.getSignature().getName());

        String[] names = StringUtils.toStringArray(buckets.keySet());
        stringSwitch(e, names, Opcodes.SWITCH_STYLE_HASH, new ObjectSwitchCallback() {

          @Override
          public void processCase(Object key, Label dontUseEnd) {
            memberHelperSize(e, buckets.get(key), callback, cached, def, end);
          }

          @Override
          public void processDefault() {
            e.goTo(def);
          }
        });
      }
      else {
        memberHelperSize(e, members, callback, cached, def, end);
      }
      e.mark(def);
      e.pop();
      callback.processDefault();
      e.mark(end);
    }
    catch (RuntimeException | Error ex) {
      throw ex;
    }
    catch (Exception ex) {
      throw new CodeGenerationException(ex);
    }
  }

  private static void memberHelperSize(final CodeEmitter e,
                                       final List members,
                                       final ObjectSwitchCallback callback,
                                       final ParameterTyper typer,
                                       final Label def, final Label end) //
  {

    final Map<Integer, List<MethodInfo>> buckets = CollectionUtils.buckets(members, (MethodInfo value) -> {
      return typer.getParameterTypes(value).length;
    });

    e.dup();
    e.arrayLength();
    e.tableSwitch(getSwitchKeys(buckets), new TableSwitchGenerator() {

      @Override
      public void generateCase(int key, Label dontUseEnd) {
        memberHelperType(e, buckets.get(key), callback, typer, def, end, new BitSet());
      }

      @Override
      public void generateDefault() {
        e.goTo(def);
      }
    });
  }

  private static void memberHelperType(final CodeEmitter e,
                                       final List<MethodInfo> members,
                                       final ObjectSwitchCallback callback,
                                       final ParameterTyper typer, final Label def,
                                       final Label end, final BitSet checked)  //
  {

    if (members.size() == 1) {
      final MethodInfo member = members.get(0);
      Type[] types = typer.getParameterTypes(member);
      // need to check classes that have not already been checked via switches
      for (int i = 0; i < types.length; i++) {
        if (checked == null || !checked.get(i)) {
          e.dup();
          e.aaload(i);
          e.invokeVirtual(Type.TYPE_CLASS, GET_NAME);
          e.push(types[i].emulateClassGetName());
          e.invokeVirtual(Type.TYPE_OBJECT, MethodSignature.EQUALS);
          e.ifJump(CodeEmitter.EQ, def);
        }
      }
      e.pop();
      callback.processCase(member, end);
    }
    else {
      // choose the index that has the best chance of uniquely identifying member
      Type[] example = typer.getParameterTypes(members.get(0));
      Map<String, List<MethodInfo>> buckets = null;
      int index = -1;
      for (int i = 0; i < example.length; i++) {
        final int j = i;

        final Map<String, List<MethodInfo>> test = CollectionUtils.buckets(members, (MethodInfo value) -> {
          final Type[] parameterTypes = typer.getParameterTypes(value);
          return parameterTypes[j].emulateClassGetName();
        });

        if (buckets == null || test.size() > buckets.size()) {
          buckets = test;
          index = i;
        }
      }
      if (buckets == null || buckets.size() == 1) {
        // TODO: switch by returnType
        // must have two methods with same name, types, and different return types
        e.goTo(def);
      }
      else {
        checked.set(index);

        e.dup();
        e.aaload(index);
        e.invokeVirtual(Type.TYPE_CLASS, GET_NAME);

        final Map<String, List<MethodInfo>> fbuckets = buckets;
        String[] names = StringUtils.toStringArray(buckets.keySet());
        EmitUtils.stringSwitch(e, names, Opcodes.SWITCH_STYLE_HASH, new ObjectSwitchCallback() {

          @Override
          public void processCase(Object key, Label dontUseEnd) {
            memberHelperType(e, fbuckets.get(key), callback, typer, def, end, checked);
          }

          @Override
          public void processDefault() {
            e.goTo(def);
          }
        });
      }
    }
  }

  public static void wrapThrowable(Block block, Type wrapper) {
    CodeEmitter e = block.getCodeEmitter();
    e.catchException(block, Type.TYPE_THROWABLE);
    e.newInstance(wrapper);
    e.dupX1();
    e.swap();
    e.invokeConstructor(wrapper, CSTRUCT_THROWABLE);
    e.throwException();
  }

  public static void addProperties(ClassEmitter ce, String[] names, Type[] types) {
    for (int i = 0; i < names.length; i++) {
      String fieldName = "$today_prop_" + names[i];
      ce.declare_field(Opcodes.ACC_PRIVATE, fieldName, types[i], null);
      EmitUtils.addProperty(ce, names[i], types[i], fieldName);
    }
  }

  public static void addProperty(ClassEmitter ce, String name, Type type, String fieldName) {
    final String property = StringUtils.capitalize(name);
    CodeEmitter e;
    e = ce.beginMethod(Opcodes.ACC_PUBLIC, new MethodSignature(type, "get" + property, Constant.TYPES_EMPTY_ARRAY));
    e.load_this();
    e.getField(fieldName);
    e.return_value();
    e.end_method();

    e = ce.beginMethod(Opcodes.ACC_PUBLIC, new MethodSignature(Type.VOID_TYPE, "set" + property, type));
    e.load_this();
    e.load_arg(0);
    e.putField(fieldName);
    e.return_value();
    e.end_method();
  }

  /* generates: } catch (RuntimeException e) { throw e; } catch (Error e) { throw
   * e; } catch (<DeclaredException> e) { throw e; } catch (Throwable e) { throw
   * new <Wrapper>(e); } */
  public static void wrapUndeclaredThrowable(CodeEmitter e, Block handler, Type[] exceptions, Type wrapper) {
    HashSet<Type> set = new HashSet<>();
    CollectionUtils.addAll(set, exceptions);
    if (set.contains(Type.TYPE_THROWABLE)) {
      return;
    }
    boolean needThrow = exceptions != null;
    if (!set.contains(Type.TYPE_RUNTIME_EXCEPTION)) {
      e.catchException(handler, Type.TYPE_RUNTIME_EXCEPTION);
      needThrow = true;
    }
    if (!set.contains(Type.TYPE_ERROR)) {
      e.catchException(handler, Type.TYPE_ERROR);
      needThrow = true;
    }
    if (exceptions != null) {
      for (final Type exception : exceptions) {
        e.catchException(handler, exception);
      }
    }
    if (needThrow) {
      e.throwException();
    }
    // e -> eo -> oeo -> ooe -> o
    e.catchException(handler, Type.TYPE_THROWABLE);
    e.newInstance(wrapper);
    e.dupX1();
    e.swap();
    e.invokeConstructor(wrapper, CSTRUCT_THROWABLE);
    e.throwException();
  }

  public static CodeEmitter beginMethod(ClassEmitter e, MethodInfo method) {
    return beginMethod(e, method, method.getModifiers());
  }

  public static CodeEmitter beginMethod(ClassEmitter e, MethodInfo method, int access) {
    return e.beginMethod(access, method.getSignature(), method.getExceptionTypes());
  }

  public static void loadEmptyArguments(CodeEmitter codeEmitter) {
    codeEmitter.getStatic(Type.TYPE_CONSTANT, "EMPTY_OBJECT_ARRAY", Type.TYPE_OBJECT_ARRAY);
  }

  // static

  public static String escapeType(String s) {
    StringBuilder sb = new StringBuilder();
    for (int i = 0, len = s.length(); i < len; i++) {
      char c = s.charAt(i);
      switch (c) {
        case '$':
          sb.append("$24");
          break;
        case '.':
          sb.append("$2E");
          break;
        case '[':
          sb.append("$5B");
          break;
        case ';':
          sb.append("$3B");
          break;
        case '(':
          sb.append("$28");
          break;
        case ')':
          sb.append("$29");
          break;
        case '/':
          sb.append("$2F");
          break;
        default:
          sb.append(c);
          break;
      }
    }
    return sb.toString();
  }

}
