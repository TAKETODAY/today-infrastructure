/*
 * Copyright 2017 - 2024 the original author or authors.
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
 * along with this program. If not, see [https://www.gnu.org/licenses/]
 */

package cn.taketoday.bytecode.core;

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

import cn.taketoday.bytecode.Label;
import cn.taketoday.bytecode.Opcodes;
import cn.taketoday.bytecode.Type;
import cn.taketoday.bytecode.commons.GeneratorAdapter;
import cn.taketoday.bytecode.commons.Local;
import cn.taketoday.bytecode.commons.MethodSignature;
import cn.taketoday.bytecode.commons.TableSwitchGenerator;
import cn.taketoday.util.CollectionUtils;
import cn.taketoday.util.StringUtils;

@SuppressWarnings({ "rawtypes", "unchecked" })
public abstract class EmitUtils {

  private static final MethodSignature CSTRUCT_THROWABLE = MethodSignature.forConstructor("Throwable");

  private static final MethodSignature LENGTH = MethodSignature.from("int length()");
  private static final MethodSignature GET_NAME = MethodSignature.from("String getName()");
  private static final MethodSignature FOR_NAME = MethodSignature.from("Class forName(String)");
  private static final MethodSignature STRING_CHAR_AT = MethodSignature.from("char charAt(int)");

  private static final MethodSignature GET_DECLARED_METHOD = //
          MethodSignature.from("java.lang.reflect.Method getDeclaredMethod(String, Class[])");

  public static void factoryMethod(ClassEmitter ce, MethodSignature sig) {
    CodeEmitter e = ce.beginMethod(Opcodes.ACC_PUBLIC, sig);
    e.new_instance_this();
    e.dup();
    e.loadArgs();
    e.invoke_constructor_this(MethodSignature.forConstructor(sig.getArgumentTypes()));
    e.returnValue();
    e.end_method();
  }

  public static void nullConstructor(ClassEmitter ce) {
    CodeEmitter e = ce.beginMethod(Opcodes.ACC_PUBLIC, MethodSignature.EMPTY_CONSTRUCTOR);
    e.loadThis();
    e.super_invoke_constructor();
    e.returnValue();
    e.end_method();
  }

  /**
   * Process an array on the stack. Assumes the top item on the stack is an array
   * of the specified type. For each element in the array, puts the element on the
   * stack and triggers the callback.
   *
   * @param type the type of the array (type.isArray() must be true)
   * @param callback the callback triggered for each element
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
    e.ifICmp(GeneratorAdapter.LT, loopbody);
  }

  /**
   * Process two arrays on the stack in parallel. Assumes the top two items on the
   * stack are arrays of the specified class. The arrays must be the same length.
   * For each pair of elements in the arrays, puts the pair on the stack and
   * triggers the callback.
   *
   * @param type the type of the arrays (type.isArray() must be true)
   * @param callback the callback triggered for each pair of elements
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
      case Opcodes.SWITCH_STYLE_TRIE -> stringSwitchTrie(e, strings, callback);
      case Opcodes.SWITCH_STYLE_HASH -> stringSwitchHash(e, strings, callback, false);
      case Opcodes.SWITCH_STYLE_HASHONLY -> stringSwitchHash(e, strings, callback, true);
      default -> throw new IllegalArgumentException("unknown switch style " + switchStyle);
    }
  }

  private static void stringSwitchTrie(
          CodeEmitter e, String[] strings, ObjectSwitchCallback callback) {
    Label def = e.newLabel();
    Label end = e.newLabel();
    Map<Integer, List<String>> buckets = CollectionUtils.buckets(strings, String::length);

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

  private static void stringSwitchHelper(CodeEmitter e, List strings,
          ObjectSwitchCallback callback, Label def, Label end, int index) {
    int len = ((String) strings.get(0)).length();
    Map buckets = CollectionUtils.buckets(strings, (Function) value -> (int) ((String) value).charAt(index));
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
    int[] keys = new int[buckets.size()];
    int i = 0;
    for (Integer k : buckets.keySet()) {
      keys[i++] = k;
    }
    Arrays.sort(keys);
    return keys;
  }

  private static void stringSwitchHash(
          CodeEmitter e, String[] strings,
          ObjectSwitchCallback callback, boolean skipEquals) {

    Map<Integer, List<String>> buckets = CollectionUtils.buckets(strings, Object::hashCode);

    Label def = e.newLabel();
    Label end = e.newLabel();
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
            String string = it.next();
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

  public static void loadClass(CodeEmitter e, Type type) {
    if (type.isPrimitive()) {
      if (type == Type.VOID_TYPE) {
        throw new IllegalArgumentException("cannot load void type");
      }
      e.getStatic(type.getBoxedType(), "TYPE", Type.TYPE_CLASS);
    }
    else {
      loadClassHelper(e, type);
    }
  }

  private static void loadClassHelper(CodeEmitter e, Type type) {
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
      String fieldName = "$today$LoadClass$".concat(escapeType(typeName));
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

  public static void pushArray(CodeEmitter e, Object[] array) {
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

  public static void pushObject(CodeEmitter e, Object obj) {
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
        Type typeBigInteger = Type.fromClass(BigInteger.class);
        e.newInstance(typeBigInteger);
        e.dup();
        e.push(obj.toString());
        e.invokeConstructor(typeBigInteger);
      }
      else if (obj instanceof BigDecimal) {
        Type typeBigDecimal = Type.fromClass(BigDecimal.class);
        e.newInstance(typeBigDecimal);
        e.dup();
        e.push(obj.toString());
        e.invokeConstructor(typeBigDecimal);
      }
      else {
        throw new IllegalArgumentException("unknown type: " + obj.getClass());
      }
    }
  }

  public static void loadMethod(CodeEmitter e, MethodInfo method) {
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

  private static void memberSwitchHelper(CodeEmitter e, //
          List members,
          ObjectSwitchCallback callback, boolean useName)//
  {
    try {

      HashMap<MethodInfo, Type[]> cache = new HashMap<>();
      ParameterTyper cached = (MethodInfo member) -> {
        Type[] types = cache.get(member);
        if (types == null) {
          cache.put(member, types = member.getSignature().getArgumentTypes());
        }
        return types;
      };

      Label def = e.newLabel();
      Label end = e.newLabel();
      if (useName) {
        e.swap();
        Map<String, List<MethodInfo>> buckets = //
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

  private static void memberHelperSize(CodeEmitter e,
          List members,
          ObjectSwitchCallback callback,
          ParameterTyper typer,
          Label def, Label end) //
  {

    Map<Integer, List<MethodInfo>> buckets = CollectionUtils.buckets(
            members, (MethodInfo value) -> typer.getParameterTypes(value).length);

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

  private static void memberHelperType(CodeEmitter e,
          List<MethodInfo> members,
          ObjectSwitchCallback callback,
          ParameterTyper typer, Label def,
          Label end, BitSet checked)  //
  {

    if (members.size() == 1) {
      MethodInfo member = members.get(0);
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
        int j = i;

        Map<String, List<MethodInfo>> test = CollectionUtils.buckets(members, (MethodInfo value) -> {
          Type[] parameterTypes = typer.getParameterTypes(value);
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

        Map<String, List<MethodInfo>> fbuckets = buckets;
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
    String property = StringUtils.capitalize(name);
    CodeEmitter e;
    e = ce.beginMethod(Opcodes.ACC_PUBLIC, new MethodSignature(type, "get" + property, Type.EMPTY_ARRAY));
    e.loadThis();
    e.getField(fieldName);
    e.returnValue();
    e.end_method();

    e = ce.beginMethod(Opcodes.ACC_PUBLIC, new MethodSignature(Type.VOID_TYPE, "set" + property, type));
    e.loadThis();
    e.loadArg(0);
    e.putField(fieldName);
    e.returnValue();
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
      for (Type exception : exceptions) {
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
    codeEmitter.getStatic(Type.TYPE_CONSTANT, "EMPTY_OBJECTS", Type.TYPE_OBJECT_ARRAY);
  }

  // static

  public static String escapeType(String s) {
    StringBuilder sb = new StringBuilder();
    for (int i = 0, len = s.length(); i < len; i++) {
      char c = s.charAt(i);
      switch (c) {
        case '$' -> sb.append("$24");
        case '.' -> sb.append("$2E");
        case '[' -> sb.append("$5B");
        case ';' -> sb.append("$3B");
        case '(' -> sb.append("$28");
        case ')' -> sb.append("$29");
        case '/' -> sb.append("$2F");
        default -> sb.append(c);
      }
    }
    return sb.toString();
  }

}
