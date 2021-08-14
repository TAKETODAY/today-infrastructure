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
package cn.taketoday.cglib.reflect;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.security.ProtectionDomain;
import java.util.Objects;

import cn.taketoday.asm.ClassVisitor;
import cn.taketoday.asm.Opcodes;
import cn.taketoday.asm.Type;
import cn.taketoday.cglib.core.AbstractClassGenerator;
import cn.taketoday.cglib.core.CglibReflectUtils;
import cn.taketoday.cglib.core.ClassEmitter;
import cn.taketoday.cglib.core.CodeEmitter;
import cn.taketoday.cglib.core.EmitUtils;
import cn.taketoday.cglib.core.KeyFactory;
import cn.taketoday.cglib.core.MethodInfo;
import cn.taketoday.cglib.core.Signature;
import cn.taketoday.cglib.core.TypeUtils;
import cn.taketoday.core.Constant;

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
@SuppressWarnings("all")
abstract public class MethodDelegate {

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
    if (this == o) return true;
    if (!(o instanceof MethodDelegate)) return false;
    final MethodDelegate that = (MethodDelegate) o;
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

    private static final Type METHOD_DELEGATE = TypeUtils.parseType(MethodDelegate.class);
    private static final Signature NEW_INSTANCE = new Signature("newInstance", METHOD_DELEGATE, new Type[] { Constant.TYPE_OBJECT });

    private Object target;
    private Class targetClass;
    private String methodName;
    private Class iface;

    public Generator() {
      super(MethodDelegate.class);
    }

    public void setTarget(Object target) {
      this.target = target;
      this.targetClass = target.getClass();
    }

    public void setTargetClass(Class targetClass) {
      this.targetClass = targetClass;
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
      return CglibReflectUtils.getProtectionDomain(targetClass);
    }

    public MethodDelegate create() {
      setNamePrefix(targetClass.getName());
      Object key = KEY_FACTORY.newInstance(targetClass, methodName, iface);
      return (MethodDelegate) super.create(key);
    }

    @Override
    protected Object firstInstance(Class type) {
      return ((MethodDelegate) CglibReflectUtils.newInstance(type)).newInstance(target);
    }

    @Override
    protected Object nextInstance(Object instance) {
      return ((MethodDelegate) instance).newInstance(target);
    }

    @Override
    public void generateClass(ClassVisitor v) throws NoSuchMethodException {

      final Method proxy = CglibReflectUtils.findInterfaceMethod(iface);
      final Method method = targetClass.getMethod(methodName, proxy.getParameterTypes());

      if (!proxy.getReturnType().isAssignableFrom(method.getReturnType())) {
        throw new IllegalArgumentException("incompatible return types");
      }

      final MethodInfo methodInfo = CglibReflectUtils.getMethodInfo(method);

      boolean isStatic = Modifier.isStatic(methodInfo.getModifiers());
      if ((target == null) ^ isStatic) {
        throw new IllegalArgumentException("Static method " + (isStatic ? "not " : Constant.BLANK) + "expected");
      }

      ClassEmitter ce = new ClassEmitter(v);
      CodeEmitter e;

      ce.beginClass(Opcodes.JAVA_VERSION, Opcodes.ACC_PUBLIC, getClassName(), METHOD_DELEGATE,
                    Type.array(Type.fromClass(iface)), Constant.SOURCE_FILE);

      ce.declare_field(Constant.PRIVATE_FINAL_STATIC, "eqMethod", Constant.TYPE_STRING, null);
      EmitUtils.nullConstructor(ce);

      // generate proxied method
      MethodInfo proxied = CglibReflectUtils.getMethodInfo(iface.getDeclaredMethods()[0]);
      int modifiers = Opcodes.ACC_PUBLIC;
      if ((proxied.getModifiers() & Opcodes.ACC_VARARGS) == Opcodes.ACC_VARARGS) {
        modifiers |= Opcodes.ACC_VARARGS;
      }
      e = EmitUtils.beginMethod(ce, proxied, modifiers);
      e.load_this();
      e.super_getfield("target", Constant.TYPE_OBJECT);
      e.checkcast(methodInfo.getClassInfo().getType());
      e.load_args();
      e.invoke(methodInfo);
      e.return_value();
      e.end_method();

      // newInstance
      e = ce.beginMethod(Opcodes.ACC_PUBLIC, NEW_INSTANCE);
      e.new_instance_this();
      e.dup();
      e.dup2();
      e.invoke_constructor_this();
      e.getfield("eqMethod");
      e.super_putfield("eqMethod", Constant.TYPE_STRING);
      e.load_arg(0);
      e.super_putfield("target", Constant.TYPE_OBJECT);
      e.return_value();
      e.end_method();

      // static initializer
      e = ce.begin_static();
      e.push(methodInfo.getSignature().toString());
      e.putfield("eqMethod");
      e.return_value();
      e.end_method();

      ce.endClass();
    }
  }
}
