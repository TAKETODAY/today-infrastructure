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
package cn.taketoday.core.bytecode.reflect;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.security.ProtectionDomain;
import java.util.Objects;

import cn.taketoday.core.bytecode.ClassVisitor;
import cn.taketoday.core.bytecode.Opcodes;
import cn.taketoday.core.bytecode.Type;
import cn.taketoday.core.bytecode.commons.MethodSignature;
import cn.taketoday.core.bytecode.core.AbstractClassGenerator;
import cn.taketoday.core.bytecode.core.ClassEmitter;
import cn.taketoday.core.bytecode.core.CodeEmitter;
import cn.taketoday.core.bytecode.core.EmitUtils;
import cn.taketoday.core.bytecode.core.KeyFactory;
import cn.taketoday.core.bytecode.core.MethodInfo;
import cn.taketoday.lang.Constant;
import cn.taketoday.util.ReflectionUtils;

// TODO: don't require exact match for return type

/**
 * <b>DOCUMENTATION FROM APACHE AVALON DELEGATE CLASS</b>
 *
 * <p>
 * Delegates are a typesafe pointer to another method. Since Java does not have
 * language support for such a construct, this utility will construct a proxy
 * that forwards method calls to any method with the same signature. This
 * utility is inspired in part by the C# delegate mechanism. We implemented it
 * in a Java-centric manner.
 * </p>
 *
 * <h2>Delegate</h2>
 * <p>
 * Any interface with one method can become the interface for a delegate.
 * Consider the example below:
 * </p>
 *
 * <pre>
 * public interface MainDelegate {
 *     int main(String[] args);
 * }
 * </pre>
 *
 * <p>
 * The interface above is an example of an interface that can become a delegate.
 * It has only one method, and the interface is public. In order to create a
 * delegate for that method, all we have to do is call
 * <code>MethodDelegate.create(this, "alternateMain", MainDelegate.class)</code>.
 * The following program will show how to use it:
 * </p>
 *
 * <pre>
 * public class Main {
 *     public static int main(String[] args) {
 *         Main newMain = new Main();
 *         MainDelegate start = (MainDelegate) MethodDelegate.create(newMain, "alternateMain", MainDelegate.class);
 *         return start.main(args);
 *     }
 *
 *     public int alternateMain(String[] args) {
 *         for (int i = 0; i < args.length; i++) {
 *             System.out.println(args[i]);
 *         }
 *         return args.length;
 *     }
 * }
 * </pre>
 *
 * <p>
 * By themselves, delegates don't do much. Their true power lies in the fact
 * that they can be treated like objects, and passed to other methods. In fact
 * that is one of the key building blocks of building Intelligent Agents which
 * in tern are the foundation of artificial intelligence. In the above program,
 * we could have easily created the delegate to match the static
 * <code>main</code> method by substituting the delegate creation call with
 * this:
 * <code>MethodDelegate.createStatic(getClass(), "main", MainDelegate.class)</code>.
 * </p>
 * <p>
 * Another key use for Delegates is to register event listeners. It is much
 * easier to have all the code for your events separated out into methods
 * instead of individual classes. One of the ways Java gets around that is to
 * create anonymous classes. They are particularly troublesome because many
 * Debuggers do not know what to do with them. Anonymous classes tend to
 * duplicate alot of code as well. We can use any interface with one declared
 * method to forward events to any method that matches the signature (although
 * the method name can be different).
 * </p>
 *
 * <h3>Equality</h3> The criteria that we use to test if two delegates are equal
 * are:
 * <ul>
 * <li>They both refer to the same instance. That is, the <code>instance</code>
 * parameter passed to the newDelegate method was the same for both. The
 * instances are compared with the identity equality operator, <code>==</code>.
 * </li>
 * <li>They refer to the same method as resolved by
 * <code>Method.equals</code>.</li>
 * </ul>
 *
 * @version $Id: MethodDelegate.java,v 1.25 2006/03/05 02:43:19 herbyderby Exp $
 */
@SuppressWarnings({ "rawtypes", "unchecked" })
public abstract class MethodDelegate {

  private static final MethodDelegateKey KEY_FACTORY = KeyFactory.create(MethodDelegateKey.class, KeyFactory.CLASS_BY_NAME);

  protected Object target;
  protected String eqMethod;

  interface MethodDelegateKey {
    Object newInstance(Class delegateClass, String methodName, Class iface);
  }

  public static <T> T createStatic(Class targetClass, String methodName, Class<T> iface) {
    Generator gen = new Generator();
    gen.setTargetClass(targetClass);
    gen.setMethodName(methodName);
    gen.setInterface(iface);
    return (T) gen.create();
  }

  public static <T> T create(Object target, String methodName, Class<T> iface) {
    Generator gen = new Generator();
    gen.setTarget(target);
    gen.setMethodName(methodName);
    gen.setInterface(iface);
    return (T) gen.create();
  }

  @Override
  public boolean equals(Object o) {
    if (this == o)
      return true;
    if (!(o instanceof final MethodDelegate that))
      return false;
    return Objects.equals(target, that.target) && Objects.equals(eqMethod, that.eqMethod);
  }

  @Override
  public int hashCode() {
    return target.hashCode() ^ eqMethod.hashCode();
  }

  public Object getTarget() {
    return target;
  }

  abstract public MethodDelegate newInstance(Object target);

  public static class Generator extends AbstractClassGenerator {

    private static final Type METHOD_DELEGATE = Type.fromClass(MethodDelegate.class);
    private static final MethodSignature NEW_INSTANCE = new MethodSignature(METHOD_DELEGATE, "newInstance", Type.TYPE_OBJECT);

    private Object target;
    private Class targetClass;
    private String methodName;
    private Class iface;

    public Generator() {
      super(MethodDelegate.class);
    }

    public void setTarget(Object target) {
      this.target = target;
      setTargetClass(target.getClass());
    }

    public void setTargetClass(Class targetClass) {
      this.targetClass = targetClass;
      setNeighbor(targetClass);
    }

    public void setMethodName(String methodName) {
      this.methodName = methodName;
    }

    public void setInterface(Class iface) {
      this.iface = iface;
    }

    @Override
    protected ClassLoader getDefaultClassLoader() {
      return targetClass.getClassLoader();
    }

    @Override
    protected ProtectionDomain getProtectionDomain() {
      return ReflectionUtils.getProtectionDomain(targetClass);
    }

    public MethodDelegate create() {
      setNamePrefix(targetClass.getName());
      Object key = KEY_FACTORY.newInstance(targetClass, methodName, iface);
      return (MethodDelegate) super.create(key);
    }

    @Override
    protected Object firstInstance(Class type) {
      return ((MethodDelegate) ReflectionUtils.newInstance(type)).newInstance(target);
    }

    @Override
    protected Object nextInstance(Object instance) {
      return ((MethodDelegate) instance).newInstance(target);
    }

    @Override
    public void generateClass(ClassVisitor v) throws NoSuchMethodException {

      final Method proxy = ReflectionUtils.findFunctionalInterfaceMethod(iface);
      final Method method = targetClass.getMethod(methodName, proxy.getParameterTypes());

      if (!proxy.getReturnType().isAssignableFrom(method.getReturnType())) {
        throw new IllegalArgumentException("incompatible return types");
      }

      final MethodInfo methodInfo = MethodInfo.from(method);

      boolean isStatic = Modifier.isStatic(methodInfo.getModifiers());
      if ((target == null) ^ isStatic) {
        throw new IllegalArgumentException("Static method " + (isStatic ? "not " : Constant.BLANK) + "expected");
      }

      ClassEmitter ce = new ClassEmitter(v);
      CodeEmitter e;

      ce.beginClass(Opcodes.JAVA_VERSION, Opcodes.ACC_PUBLIC, getClassName(), METHOD_DELEGATE,
                    Type.array(Type.fromClass(iface)), Constant.SOURCE_FILE);

      ce.declare_field(Opcodes.PRIVATE_FINAL_STATIC, "eqMethod", Type.TYPE_STRING, null);
      EmitUtils.nullConstructor(ce);

      // generate proxied method
      MethodInfo proxied = MethodInfo.from(iface.getDeclaredMethods()[0]);
      int modifiers = Opcodes.ACC_PUBLIC;
      if ((proxied.getModifiers() & Opcodes.ACC_VARARGS) == Opcodes.ACC_VARARGS) {
        modifiers |= Opcodes.ACC_VARARGS;
      }
      e = EmitUtils.beginMethod(ce, proxied, modifiers);
      e.loadThis();
      e.super_getfield("target", Type.TYPE_OBJECT);
      e.checkCast(methodInfo.getClassInfo().getType());
      e.loadArgs();
      e.invoke(methodInfo);
      e.returnValue();
      e.end_method();

      // newInstance
      e = ce.beginMethod(Opcodes.ACC_PUBLIC, NEW_INSTANCE);
      e.new_instance_this();
      e.dup();
      e.dup2();
      e.invoke_constructor_this();
      e.getField("eqMethod");
      e.super_putfield("eqMethod", Type.TYPE_STRING);
      e.loadArg(0);
      e.super_putfield("target", Type.TYPE_OBJECT);
      e.returnValue();
      e.end_method();

      // static initializer
      e = ce.begin_static();
      e.push(methodInfo.getSignature().toString());
      e.putField("eqMethod");
      e.returnValue();
      e.end_method();

      ce.endClass();
    }
  }
}
