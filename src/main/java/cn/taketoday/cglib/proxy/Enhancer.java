/*
 * Copyright 2002,2003,2004 The Apache Software Foundation
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
package cn.taketoday.cglib.proxy;

import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.WeakReference;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.security.ProtectionDomain;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import cn.taketoday.asm.ClassReader;
import cn.taketoday.asm.ClassVisitor;
import cn.taketoday.asm.Label;
import cn.taketoday.asm.MethodVisitor;
import cn.taketoday.asm.Opcodes;
import cn.taketoday.asm.Type;
import cn.taketoday.cglib.core.AbstractClassGenerator;
import cn.taketoday.cglib.core.CglibReflectUtils;
import cn.taketoday.cglib.core.ClassEmitter;
import cn.taketoday.cglib.core.CodeEmitter;
import cn.taketoday.cglib.core.CodeGenerationException;
import cn.taketoday.cglib.core.DuplicatesPredicate;
import cn.taketoday.cglib.core.EmitUtils;
import cn.taketoday.cglib.core.GeneratorStrategy;
import cn.taketoday.cglib.core.KeyFactory;
import cn.taketoday.cglib.core.Local;
import cn.taketoday.cglib.core.MethodInfo;
import cn.taketoday.cglib.core.MethodInfoTransformer;
import cn.taketoday.cglib.core.MethodWrapper;
import cn.taketoday.cglib.core.NamingPolicy;
import cn.taketoday.cglib.core.ObjectSwitchCallback;
import cn.taketoday.cglib.core.ProcessSwitchCallback;
import cn.taketoday.cglib.core.RejectModifierPredicate;
import cn.taketoday.cglib.core.Signature;
import cn.taketoday.cglib.core.TypeUtils;
import cn.taketoday.cglib.core.VisibilityPredicate;
import cn.taketoday.cglib.core.WeakCacheKey;
import cn.taketoday.core.Constant;
import cn.taketoday.util.ClassUtils;
import cn.taketoday.util.CollectionUtils;
import cn.taketoday.util.ObjectUtils;
import cn.taketoday.util.ReflectionUtils;

import static cn.taketoday.asm.ClassReader.SKIP_DEBUG;
import static cn.taketoday.asm.ClassReader.SKIP_FRAMES;
import static cn.taketoday.asm.Opcodes.ACC_PRIVATE;
import static cn.taketoday.asm.Opcodes.ACC_PUBLIC;
import static cn.taketoday.asm.Opcodes.ACC_STATIC;
import static cn.taketoday.asm.Opcodes.INVOKEINTERFACE;
import static cn.taketoday.asm.Opcodes.INVOKESPECIAL;
import static cn.taketoday.asm.Type.BOOLEAN_TYPE;
import static cn.taketoday.asm.Type.INT_TYPE;
import static cn.taketoday.asm.Type.LONG_TYPE;
import static cn.taketoday.asm.Type.VOID_TYPE;
import static cn.taketoday.asm.Type.array;
import static cn.taketoday.cglib.core.TypeUtils.parseSignature;
import static cn.taketoday.cglib.core.TypeUtils.parseType;
import static cn.taketoday.core.Constant.PRIVATE_FINAL_STATIC;
import static cn.taketoday.core.Constant.SUID_FIELD_NAME;
import static cn.taketoday.core.Constant.TYPE_OBJECT;

/**
 * Generates dynamic subclasses to enable method interception. This class
 * started as a substitute for the standard Dynamic Proxy support included with
 * JDK 1.3, but one that allowed the proxies to extend a concrete base class, in
 * addition to implementing interfaces. The dynamically generated subclasses
 * override the non-final methods of the superclass and have hooks which
 * callback to user-defined interceptor implementations.
 * <p>
 * The original and most general callback type is the {@link MethodInterceptor},
 * which in AOP terms enables "around advice"--that is, you can invoke custom
 * code both before and after the invocation of the "super" method. In addition
 * you can modify the arguments before calling the super method, or not call it
 * at all.
 * <p>
 * Although <code>MethodInterceptor</code> is generic enough to meet any
 * interception need, it is often overkill. For simplicity and performance,
 * additional specialized callback types, such as {@link LazyLoader} are also
 * available. Often a single callback will be used per enhanced class, but you
 * can control which callback is used on a per-method basis with a
 * {@link CallbackFilter}.
 * <p>
 * The most common uses of this class are embodied in the static helper methods.
 * For advanced needs, such as customizing the <code>ClassLoader</code> to use,
 * you should create a new instance of <code>Enhancer</code>. Other classes
 * within TODAY follow a similar pattern.
 * <p>
 * All enhanced objects implement the {@link Factory} interface, unless
 * {@link #setUseFactory} is used to explicitly disable this feature. The
 * <code>Factory</code> interface provides an API to change the callbacks of an
 * existing object, as well as a faster and easier way to create new instances
 * of the same type.
 * <p>
 * For an almost drop-in replacement for <code>java.lang.reflect.Proxy</code>,
 * see the {@link Proxy} class.
 */
@SuppressWarnings("all")
public class Enhancer extends AbstractClassGenerator<Object> {

  private static final CallbackFilter ALL_ZERO = (m) -> 0;

  private static final EnhancerKey KEY_FACTORY = KeyFactory.create(EnhancerKey.class, KeyFactory.HASH_ASM_TYPE, null);

  private static final String BOUND_FIELD = "today$Bound";
  private static final String CONSTRUCTED_FIELD = "today$Constructed";
  private static final String FACTORY_DATA_FIELD = "today$FactoryData";
  private static final String THREAD_CALLBACKS_FIELD = "today$ThreadCallbacks";
  private static final String STATIC_CALLBACKS_FIELD = "today$StaticCallbacks";
  private static final String SET_THREAD_CALLBACKS_NAME = "today$SetThreadCallbacks";
  private static final String SET_STATIC_CALLBACKS_NAME = "today$SetStaticCallbacks";

  /**
   * {@link cn.taketoday.cglib.core.AbstractClassGenerator.ClassLoaderData#generatedClasses}
   * requires to keep cache key in a good shape (the keys should be up and running
   * if the proxy class is alive), and one of the cache keys is
   * {@link CallbackFilter}. That is why the generated class contains static field
   * that keeps strong reference to the {@link #filter}.
   * <p>
   * This dance achieves two goals: ensures generated class is reusable and
   * available through generatedClasses cache, and it enables to unload
   * classloader and the related {@link CallbackFilter} in case user does not need
   * that
   * </p>
   */
  private static final String CALLBACK_FILTER_FIELD = "today$CallbackFilter";

  private static final Type OBJECT_TYPE = TYPE_OBJECT;
  private static final Type FACTORY = Type.fromClass(Factory.class);
  private static final Type ILLEGAL_STATE_EXCEPTION = parseType("IllegalStateException");
  private static final Type ILLEGAL_ARGUMENT_EXCEPTION = parseType("IllegalArgumentException");
  private static final Type THREAD_LOCAL = parseType("ThreadLocal");
  private static final Type CALLBACK = Type.fromClass(Callback.class);
  private static final Type CALLBACK_ARRAY = Type.fromClass(Callback[].class);

  private static final Signature CSTRUCT_NULL = TypeUtils.parseConstructor(Constant.BLANK);

  private static final Signature SET_THREAD_CALLBACKS = new Signature(SET_THREAD_CALLBACKS_NAME, VOID_TYPE, array(CALLBACK_ARRAY));
  private static final Signature SET_STATIC_CALLBACKS = new Signature(SET_STATIC_CALLBACKS_NAME, VOID_TYPE, array(CALLBACK_ARRAY));
  private static final Signature NEW_INSTANCE = new Signature("newInstance", TYPE_OBJECT, array(CALLBACK_ARRAY));
  private static final Signature MULTIARG_NEW_INSTANCE = new Signature("newInstance",
                                                                       TYPE_OBJECT,
                                                                       array(Constant.TYPE_CLASS_ARRAY,
                                                                             Constant.TYPE_OBJECT_ARRAY,
                                                                             CALLBACK_ARRAY));

  private static final Signature SINGLE_NEW_INSTANCE = new Signature("newInstance", TYPE_OBJECT, array(CALLBACK));
  private static final Signature SET_CALLBACK = new Signature("setCallback", VOID_TYPE, array(INT_TYPE, CALLBACK));
  private static final Signature GET_CALLBACK = new Signature("getCallback", CALLBACK, array(INT_TYPE));
  private static final Signature SET_CALLBACKS = new Signature("setCallbacks", VOID_TYPE, array(CALLBACK_ARRAY));
  private static final Signature GET_CALLBACKS = new Signature("getCallbacks", CALLBACK_ARRAY, new Type[0]);
  private static final Signature THREAD_LOCAL_GET = parseSignature("Object get()");
  private static final Signature THREAD_LOCAL_SET = parseSignature("void set(Object)");
  private static final Signature BIND_CALLBACKS = parseSignature("void today$BindCallbacks(Object)");

  private EnhancerFactoryData currentData;
  private Object currentKey;

  private Class<?>[] interfaces;
  private CallbackFilter filter;
  private Callback[] callbacks;
  private Type[] callbackTypes;
  private boolean validateCallbackTypes;
  private boolean classOnly;
  private Class<?> superclass;
  private Class<?>[] argumentTypes;
  private Object[] arguments;
  private boolean useFactory = true;
  private Long serialVersionUID;
  private boolean interceptDuringConstruction = true;

  /** Internal interface, only public due to ClassLoader issues. */
  public interface EnhancerKey {

    Object newInstance(String type,
                       String[] interfaces,
                       WeakCacheKey<CallbackFilter> filter,
                       Type[] callbackTypes,
                       boolean useFactory,
                       boolean interceptDuringConstruction,
                       Long serialVersionUID);
  }

  /**
   * Create a new <code>Enhancer</code>. A new <code>Enhancer</code> object should
   * be used for each generated object, and should not be shared across threads.
   * To create additional instances of a generated class, use the
   * <code>Factory</code> interface.
   *
   * @see Factory
   */
  public Enhancer() {
    super("Enhance");
  }

  /**
   * Set the class which the generated class will extend. As a convenience, if the
   * supplied superclass is actually an interface, <code>setInterfaces</code> will
   * be called with the appropriate argument instead. A non-interface argument
   * must not be declared as final, and must have an accessible constructor.
   *
   * @param superclass
   *         class to extend or interface to implement
   *
   * @see #setInterfaces(Class[])
   */
  public Enhancer setSuperclass(Class<?> superclass) {
    if (superclass != null && superclass.isInterface()) {
      setInterfaces(superclass);
    }
    else if (superclass != null && superclass.equals(Object.class)) {
      // affects choice of ClassLoader
      this.superclass = null;
    }
    else {
      this.superclass = superclass;
    }
    return this;
  }

  /**
   * Set the interfaces to implement. The <code>Factory</code> interface will
   * always be implemented regardless of what is specified here.
   *
   * @param interfaces
   *         array of interfaces to implement, or null
   *
   * @see Factory
   */
  public Enhancer setInterfaces(Class<?>... interfaces) {
    this.interfaces = interfaces;
    return this;
  }

  /**
   * Set the {@link CallbackFilter} used to map the generated class' methods to a
   * particular callback index. New object instances will always use the same
   * mapping, but may use different actual callback objects.
   *
   * @param filter
   *         the callback filter to use when generating a new class
   *
   * @see #setCallbacks
   */
  public Enhancer setCallbackFilter(CallbackFilter filter) {
    this.filter = filter;
    return this;
  }

  /**
   * Set the single {@link Callback} to use. Ignored if you use
   * {@link #createClass}.
   *
   * @param callback
   *         the callback to use for all methods
   *
   * @see #setCallbacks
   */
  public Enhancer setCallback(final Callback callback) {
    setCallbacks(callback);
    return this;
  }

  /**
   * Set the array of callbacks to use. Ignored if you use {@link #createClass}.
   * You must use a {@link CallbackFilter} to specify the index into this array
   * for each method in the proxied class.
   *
   * @param callbacks
   *         the callback array
   *
   * @see #setCallbackFilter
   * @see #setCallback
   */
  public Enhancer setCallbacks(Callback... callbacks) {
    if (ObjectUtils.isEmpty(callbacks)) {
      throw new IllegalArgumentException("Array cannot be empty");
    }
    this.callbacks = callbacks;
    return this;
  }

  /**
   * Set whether the enhanced object instances should implement the
   * {@link Factory} interface. This was added for tools that need for proxies to
   * be more indistinguishable from their targets. Also, in some cases it may be
   * necessary to disable the <code>Factory</code> interface to prevent code from
   * changing the underlying callbacks.
   *
   * @param useFactory
   *         whether to implement <code>Factory</code>; default is
   *         <code>true</code>
   */
  public Enhancer setUseFactory(boolean useFactory) {
    this.useFactory = useFactory;
    return this;
  }

  /**
   * Set whether methods called from within the proxy's constructer will be
   * intercepted. The default value is true. Unintercepted methods will call the
   * method of the proxy's base class, if it exists.
   *
   * @param interceptDuringConstruction
   *         whether to intercept methods called from the constructor
   */
  public Enhancer setInterceptDuringConstruction(boolean interceptDuringConstruction) {
    this.interceptDuringConstruction = interceptDuringConstruction;
    return this;
  }

  /**
   * Set the single type of {@link Callback} to use. This may be used instead of
   * {@link #setCallback} when calling {@link #createClass}, since it may not be
   * possible to have an array of actual callback instances.
   *
   * @param callbackType
   *         the type of callback to use for all methods
   *
   * @see #setCallbackTypes
   */
  public Enhancer setCallbackType(Class<?> callbackType) {
    setCallbackTypes(callbackType);
    return this;
  }

  /**
   * Set the array of callback types to use. This may be used instead of
   * {@link #setCallbacks} when calling {@link #createClass}, since it may not be
   * possible to have an array of actual callback instances. You must use a
   * {@link CallbackFilter} to specify the index into this array for each method
   * in the proxied class.
   *
   * @param callbackTypes
   *         the array of callback types
   */
  public Enhancer setCallbackTypes(Class<?>... callbackTypes) {
    if (ObjectUtils.isEmpty(callbackTypes)) {
      throw new IllegalArgumentException("Array cannot be empty");
    }
    this.callbackTypes = CallbackInfo.determineTypes(callbackTypes);
    return this;
  }

  // --------------------------

  @Override
  public Enhancer setUseCache(boolean useCache) {
    super.setUseCache(useCache);
    return this;
  }

  @Override
  public Enhancer setAttemptLoad(boolean attemptLoad) {
    super.setAttemptLoad(attemptLoad);
    return this;
  }

  @Override
  public Enhancer setClassLoader(ClassLoader classLoader) {
    super.setClassLoader(classLoader);
    return this;
  }

  @Override
  public Enhancer setNamingPolicy(NamingPolicy namingPolicy) {
    super.setNamingPolicy(namingPolicy);
    return this;
  }

  @Override
  public Enhancer setStrategy(GeneratorStrategy strategy) {
    super.setStrategy(strategy);
    return this;
  }

  // ------------------------------

  /**
   * Generate a new class if necessary and uses the specified callbacks (if any)
   * to create a new object instance. Uses the no-arg constructor of the
   * superclass.
   *
   * @return a new instance
   */
  public Object create() {
    classOnly = false;
    argumentTypes = null;
    return createHelper();
  }

  /**
   * Generate a new class if necessary and uses the specified callbacks (if any)
   * to create a new object instance. Uses the constructor of the superclass
   * matching the <code>argumentTypes</code> parameter, with the given arguments.
   *
   * @param argumentTypes
   *         constructor signature
   * @param arguments
   *         compatible wrapped arguments to pass to constructor
   *
   * @return a new instance
   */
  public Object create(Class<?>[] argumentTypes, Object[] arguments) {
    classOnly = false;
    if (argumentTypes == null || arguments == null || argumentTypes.length != arguments.length) {
      throw new IllegalArgumentException("Arguments must be non-null and of equal length");
    }
    this.argumentTypes = argumentTypes;
    this.arguments = arguments;
    return createHelper();
  }

  /**
   * Generate a new class if necessary and return it without creating a new
   * instance. This ignores any callbacks that have been set. To create a new
   * instance you will have to use reflection, and methods called during the
   * constructor will not be intercepted. To avoid this problem, use the multi-arg
   * <code>create</code> method.
   *
   * @see #create(Class[], Object[])
   */
  public Class<?> createClass() {
    classOnly = true;
    return (Class<?>) createHelper();
  }

  /**
   * Insert a static serialVersionUID field into the generated class.
   *
   * @param sUID
   *         the field value, or null to avoid generating field.
   */
  public Enhancer setSerialVersionUID(Long sUID) {
    this.serialVersionUID = sUID;
    return this;
  }

  private void preValidate() {
    if (callbackTypes == null) {
      callbackTypes = CallbackInfo.determineTypes(callbacks, false);
      validateCallbackTypes = true;
    }
    if (filter == null) {
      if (callbackTypes.length > 1) {
        throw new IllegalStateException("Multiple callback types possible but no filter specified");
      }
      filter = ALL_ZERO;
    }
  }

  private void validate() {
    if (classOnly ^ (callbacks == null)) {
      if (classOnly) {
        throw new IllegalStateException("createClass does not accept callbacks");
      }
      else {
        throw new IllegalStateException("Callbacks are required");
      }
    }
    if (classOnly && (callbackTypes == null)) {
      throw new IllegalStateException("Callback types are required");
    }
    if (validateCallbackTypes) {
      callbackTypes = null;
    }
    if (callbacks != null && callbackTypes != null) {
      if (callbacks.length != callbackTypes.length) {
        throw new IllegalStateException("Lengths of callback and callback types array must be the same");
      }
      Type[] check = CallbackInfo.determineTypes(callbacks);
      for (int i = 0; i < check.length; i++) {
        if (!check[i].equals(callbackTypes[i])) {
          throw new IllegalStateException("Callback " + check[i] + " is not assignable to " + callbackTypes[i]);
        }
      }
    }
    else if (callbacks != null) {
      callbackTypes = CallbackInfo.determineTypes(callbacks);
    }
    if (interfaces != null) {
      for (int i = 0; i < interfaces.length; i++) {
        if (interfaces[i] == null) {
          throw new IllegalStateException("Interfaces cannot be null");
        }
        if (!interfaces[i].isInterface()) {
          throw new IllegalStateException(interfaces[i] + " is not an interface");
        }
      }
    }
  }

  /**
   * The idea of the class is to cache relevant java.lang.reflect instances so
   * proxy-class can be instantiated faster that when using
   * {@link ReflectionUtils#newInstance(Class, Class[], Object[])} and
   * {@link Enhancer#setThreadCallbacks(Class, Callback[])}
   */
  static class EnhancerFactoryData {

    public final Class<?> generatedClass;
    private final Method setThreadCallbacks;
    private final Class<?>[] primaryConstructorArgTypes;
    private final Constructor<?> primaryConstructor;

    /**
     * Creates proxy instance for given argument types, and assigns the callbacks.
     * Ideally, for each proxy class, just one set of argument types should be used,
     * otherwise it would have to spend time on constructor lookup. Technically, it
     * is a re-implementation of {@link Enhancer#createUsingReflection(Class)}, with
     * "cache {@link #setThreadCallbacks} and {@link #primaryConstructor}"
     *
     * @param argumentTypes
     *         constructor argument types
     * @param arguments
     *         constructor arguments
     * @param callbacks
     *         callbacks to set for the new instance
     *
     * @return newly created proxy
     *
     * @see #createUsingReflection(Class)
     */
    public Object newInstance(Class<?>[] argumentTypes, Object[] arguments, Callback[] callbacks) {
      setThreadCallbacks(callbacks);
      try {
        // Explicit reference equality is added here just in case Arrays.equals does not
        // have one
        if (primaryConstructorArgTypes == argumentTypes || Arrays.equals(primaryConstructorArgTypes, argumentTypes)) {
          // If we have relevant Constructor instance at hand, just call it
          // This skips "get constructors" machinery
          return ReflectionUtils.invokeConstructor(primaryConstructor, arguments);
        }
        // Take a slow path if observing unexpected argument types
        return ReflectionUtils.newInstance(generatedClass, argumentTypes, arguments);
      }
      finally {
        // clear thread callbacks to allow them to be gc'd
        setThreadCallbacks(null);
      }

    }

    public EnhancerFactoryData(Class<?> generatedClass, Class<?>[] primaryConstructorArgTypes, boolean classOnly) {
      this.generatedClass = generatedClass;
      try {
        setThreadCallbacks = getCallbacksSetter(generatedClass, SET_THREAD_CALLBACKS_NAME);
        if (classOnly) {
          this.primaryConstructorArgTypes = null;
          this.primaryConstructor = null;
        }
        else {
          this.primaryConstructorArgTypes = primaryConstructorArgTypes;
          this.primaryConstructor = ClassUtils.getConstructor(generatedClass, primaryConstructorArgTypes);
        }
      }
      catch (NoSuchMethodException e) {
        throw new CodeGenerationException(e);
      }
    }

    private void setThreadCallbacks(Callback[] callbacks) {
      try {
        setThreadCallbacks.invoke(generatedClass, (Object) callbacks);
      }
      catch (IllegalAccessException e) {
        throw new CodeGenerationException(e);
      }
      catch (InvocationTargetException e) {
        throw new CodeGenerationException(e.getTargetException());
      }
    }
  }

  private Object createHelper() {
    preValidate();
    Object key = KEY_FACTORY.newInstance(
            (superclass != null) ? superclass.getName() : null, //
            CglibReflectUtils.getNames(interfaces), //
            filter == ALL_ZERO ? null : new WeakCacheKey<>(filter), //
            callbackTypes, //
            useFactory, //
            interceptDuringConstruction, //
            serialVersionUID//
    );

    this.currentKey = key;
    return super.create(key);
  }

  @Override
  protected Class<?> generate(ClassLoaderData data) {
    validate();
    if (superclass != null) {
      setNamePrefix(superclass.getName());
    }
    else if (interfaces != null) {
      setNamePrefix(interfaces[CglibReflectUtils.findPackageProtected(interfaces)].getName());
    }
    return super.generate(data);
  }

  @Override
  protected ClassLoader getDefaultClassLoader() {
    if (superclass != null) {
      return superclass.getClassLoader();
    }
    if (ObjectUtils.isNotEmpty(interfaces)) {
      return interfaces[0].getClassLoader();
    }
    return null;
  }

  @Override
  protected ProtectionDomain getProtectionDomain() {
    if (superclass != null) {
      return CglibReflectUtils.getProtectionDomain(superclass);
    }
    if (ObjectUtils.isNotEmpty(interfaces)) {
      return CglibReflectUtils.getProtectionDomain(interfaces[0]);
    }
    return null;
  }

  private Signature rename(Signature sig, int index) {
    return new Signature("today$" + sig.getName() + '$' + index, sig.getDescriptor());
  }

  /**
   * Finds all of the methods that will be extended by an Enhancer-generated class
   * using the specified superclass and interfaces. This can be useful in building
   * a list of Callback objects. The methods are added to the end of the given
   * list. Due to the subclassing nature of the classes generated by Enhancer, the
   * methods are guaranteed to be non-static, non-final, and non-private. Each
   * method signature will only occur once, even if it occurs in multiple classes.
   *
   * @param superclass
   *         the class that will be extended, or null
   * @param interfaces
   *         the list of interfaces that will be implemented, or null
   * @param methods
   *         the list into which to copy the applicable methods
   */
  public static void getMethods(Class<?> superclass, Class<?>[] interfaces, List<Method> methods) {
    getMethods(superclass, interfaces, methods, null, null);
  }

  private static void getMethods(
          Class<?> superclass,
          Class<?>[] interfaces,
          List<Method> methods,
          List<Method> interfaceMethods,
          Set<Object> forcePublic
  ) {

    CglibReflectUtils.addAllMethods(superclass, methods);

    List<Method> target = (interfaceMethods != null) ? interfaceMethods : methods;
    if (interfaces != null) {
      for (int i = 0; i < interfaces.length; i++) {
        if (interfaces[i] != Factory.class) {
          CglibReflectUtils.addAllMethods(interfaces[i], target);
        }
      }
    }
    if (interfaceMethods != null) {
      if (forcePublic != null) {
        forcePublic.addAll(MethodWrapper.createSet(interfaceMethods));
      }
      methods.addAll(interfaceMethods);
    }
    CollectionUtils.filter(methods, new RejectModifierPredicate(ACC_STATIC));
    CollectionUtils.filter(methods, new VisibilityPredicate(superclass, true));
    CollectionUtils.filter(methods, new DuplicatesPredicate(methods));
    CollectionUtils.filter(methods, new RejectModifierPredicate(Opcodes.ACC_FINAL));
  }

  @Override
  public void generateClass(ClassVisitor v) throws Exception {

    Class sc = (superclass == null) ? Object.class : superclass;

    if (Modifier.isFinal(sc.getModifiers())) {
      throw new IllegalArgumentException("Cannot subclass final class " + sc.getName());
    }

    List constructors = new ArrayList(8);
    Collections.addAll(constructors, sc.getDeclaredConstructors());
    filterConstructors(sc, constructors);

    // Order is very important: must add superclass, then its superclass chain, then
    // each interface and its superinterfaces.

    List actualMethods = new ArrayList();
    List interfaceMethods = new ArrayList();
    final Set forcePublic = new HashSet();
    getMethods(sc, interfaces, actualMethods, interfaceMethods, forcePublic);

    final List<MethodInfo> methods = CollectionUtils.transform(actualMethods, (Method method) -> {

      int modifiers = Opcodes.ACC_FINAL | (method.getModifiers() //
              & ~Opcodes.ACC_ABSTRACT //
              & ~Opcodes.ACC_NATIVE //
              & ~Opcodes.ACC_SYNCHRONIZED//
      );

      if (forcePublic.contains(MethodWrapper.create(method))) {
        modifiers = (modifiers & ~Opcodes.ACC_PROTECTED) | ACC_PUBLIC;
      }
      return CglibReflectUtils.getMethodInfo(method, modifiers);
    });

    final ClassEmitter e = new ClassEmitter(v);
    if (currentData == null) {
      e.beginClass(Opcodes.JAVA_VERSION, //
                   ACC_PUBLIC, //
                   getClassName(), //
                   Type.fromClass(sc), //
                   (useFactory ? TypeUtils.add(TypeUtils.getTypes(interfaces), FACTORY) : TypeUtils.getTypes(interfaces)), //
                   Constant.SOURCE_FILE//
      );
    }
    else {
      e.beginClass(Opcodes.JAVA_VERSION, //
                   ACC_PUBLIC, //
                   getClassName(), //
                   null, //
                   Type.array(FACTORY),
                   Constant.SOURCE_FILE//
      );
    }
    List constructorInfo = CollectionUtils.transform(constructors, MethodInfoTransformer.getInstance());

    e.declare_field(ACC_PRIVATE, BOUND_FIELD, BOOLEAN_TYPE, null);
    e.declare_field(ACC_PUBLIC | ACC_STATIC, FACTORY_DATA_FIELD, OBJECT_TYPE, null);
    if (!interceptDuringConstruction) {
      e.declare_field(ACC_PRIVATE, CONSTRUCTED_FIELD, BOOLEAN_TYPE, null);
    }
    e.declare_field(PRIVATE_FINAL_STATIC, THREAD_CALLBACKS_FIELD, THREAD_LOCAL, null);
    e.declare_field(PRIVATE_FINAL_STATIC, STATIC_CALLBACKS_FIELD, CALLBACK_ARRAY, null);
    if (serialVersionUID != null) {
      e.declare_field(PRIVATE_FINAL_STATIC, SUID_FIELD_NAME, LONG_TYPE, serialVersionUID);
    }

    for (int i = 0; i < callbackTypes.length; i++) {
      e.declare_field(ACC_PRIVATE, getCallbackField(i), callbackTypes[i], null);
    }
    // This is declared private to avoid "public field" pollution
    e.declare_field(ACC_PRIVATE | ACC_STATIC, CALLBACK_FILTER_FIELD, OBJECT_TYPE, null);

    if (currentData == null) {
      emitMethods(e, methods, actualMethods);
      emitConstructors(e, constructorInfo);
    }
    else {
      emitDefaultConstructor(e);
    }
    emitSetThreadCallbacks(e);
    emitSetStaticCallbacks(e);
    emitBindCallbacks(e);

    if (useFactory || currentData != null) {
      int[] keys = getCallbackKeys();
      emitNewInstanceCallbacks(e);
      emitNewInstanceCallback(e);
      emitNewInstanceMultiarg(e, constructorInfo);
      emitGetCallback(e, keys);
      emitSetCallback(e, keys);
      emitGetCallbacks(e);
      emitSetCallbacks(e);
    }

    e.endClass();
  }

  /**
   * Filter the list of constructors from the superclass. The constructors which
   * remain will be included in the generated class. The default implementation is
   * to filter out all private constructors, but subclasses may extend Enhancer to
   * override this behavior.
   *
   * @param sc
   *         the superclass
   * @param constructors
   *         the list of all declared constructors from the superclass
   *
   * @throws IllegalArgumentException
   *         if there are no non-private constructors
   */
  protected void filterConstructors(Class sc, List constructors) {
    CollectionUtils.filter(constructors, new VisibilityPredicate(sc, true));
    if (constructors.size() == 0)
      throw new IllegalArgumentException("No visible constructors in " + sc);
  }

  /**
   * This method should not be called in regular flow. Technically speaking
   * {@link #wrapCachedClass(Class)} uses {@link EnhancerFactoryData} as a cache
   * value, and the latter enables faster instantiation than plain old reflection
   * lookup and invoke. This method is left intact for backward compatibility
   * reasons: just in case it was ever used.
   *
   * @param type
   *         class to instantiate
   *
   * @return newly created proxy instance
   *
   * @throws Exception
   *         if something goes wrong
   */
  protected Object firstInstance(Class type) throws Exception {
    if (classOnly) {
      return type;
    }
    else {
      return createUsingReflection(type);
    }
  }

  protected Object nextInstance(Object instance) {
    EnhancerFactoryData data = (EnhancerFactoryData) instance;

    if (classOnly) {
      return data.generatedClass;
    }

    Class[] argumentTypes = this.argumentTypes;
    Object[] arguments = this.arguments;
    if (argumentTypes == null) {
      argumentTypes = Constant.EMPTY_CLASS_ARRAY;
      arguments = null;
    }
    return data.newInstance(argumentTypes, arguments, callbacks);
  }

  @Override
  protected Object wrapCachedClass(Class klass) {
    Class[] argumentTypes = this.argumentTypes;
    if (argumentTypes == null) {
      argumentTypes = Constant.EMPTY_CLASS_ARRAY;
    }
    EnhancerFactoryData factoryData = new EnhancerFactoryData(klass, argumentTypes, classOnly);
    Field factoryDataField = null;
    try {
      // The subsequent dance is performed just once for each class,
      // so it does not matter much how fast it goes
      factoryDataField = klass.getField(FACTORY_DATA_FIELD);
      factoryDataField.set(null, factoryData);
      Field callbackFilterField = klass.getDeclaredField(CALLBACK_FILTER_FIELD);
      callbackFilterField.setAccessible(true);
      callbackFilterField.set(null, this.filter);
    }
    catch (NoSuchFieldException | IllegalAccessException e) {
      throw new CodeGenerationException(e);
    }
    return new WeakReference<EnhancerFactoryData>(factoryData);
  }

  @Override
  protected Object unwrapCachedValue(Object cached) {
    if (currentKey instanceof EnhancerKey) {
      EnhancerFactoryData data = ((WeakReference<EnhancerFactoryData>) cached).get();
      return data;
    }
    return super.unwrapCachedValue(cached);
  }

  /**
   * Call this method to register the {@link Callback} array to use before
   * creating a new instance of the generated class via reflection. If you are
   * using an instance of <code>Enhancer</code> or the {@link Factory} interface
   * to create new instances, this method is unnecessary. Its primary use is for
   * when you want to cache and reuse a generated class yourself, and the
   * generated class does <i>not</i> implement the {@link Factory} interface.
   * <p>
   * Note that this method only registers the callbacks on the current thread. If
   * you want to register callbacks for instances created by multiple threads, use
   * {@link #registerStaticCallbacks}.
   * <p>
   * The registered callbacks are overwritten and subsequently cleared when
   * calling any of the <code>create</code> methods (such as {@link #create}), or
   * any {@link Factory} <code>newInstance</code> method. Otherwise they are
   * <i>not</i> cleared, and you should be careful to set them back to
   * <code>null</code> after creating new instances via reflection if memory
   * leakage is a concern.
   *
   * @param generatedClass
   *         a class previously created by {@link Enhancer}
   * @param callbacks
   *         the array of callbacks to use when instances of the generated
   *         class are created
   *
   * @see #setUseFactory
   */
  public static void registerCallbacks(Class generatedClass, Callback[] callbacks) {
    setThreadCallbacks(generatedClass, callbacks);
  }

  /**
   * Similar to {@link #registerCallbacks}, but suitable for use when multiple
   * threads will be creating instances of the generated class. The thread-level
   * callbacks will always override the static callbacks. Static callbacks are
   * never cleared.
   *
   * @param generatedClass
   *         a class previously created by {@link Enhancer}
   * @param callbacks
   *         the array of callbacks to use when instances of the generated
   *         class are created
   */
  public static void registerStaticCallbacks(Class generatedClass, Callback[] callbacks) {
    setCallbacksHelper(generatedClass, callbacks, SET_STATIC_CALLBACKS_NAME);
  }

  /**
   * Determine if a class was generated using <code>Enhancer</code>.
   *
   * @param type
   *         any class
   *
   * @return whether the class was generated using <code>Enhancer</code>
   */
  public static boolean isEnhanced(Class type) {
    try {
      getCallbacksSetter(type, SET_THREAD_CALLBACKS_NAME);
      return true;
    }
    catch (NoSuchMethodException e) {
      return false;
    }
  }

  private static void setThreadCallbacks(Class type, Callback[] callbacks) {
    setCallbacksHelper(type, callbacks, SET_THREAD_CALLBACKS_NAME);
  }

  private static void setCallbacksHelper(Class type, Callback[] callbacks, String methodName) {
    // TODO: optimize
    try {
      getCallbacksSetter(type, methodName).invoke(null, new Object[] { callbacks });
    }
    catch (NoSuchMethodException e) {
      throw new IllegalArgumentException(type + " is not an enhanced class");
    }
    catch (IllegalAccessException e) {
      throw new CodeGenerationException(e);
    }
    catch (InvocationTargetException e) {
      throw new CodeGenerationException(e.getTargetException());
    }
  }

  private static Method getCallbacksSetter(Class type, String methodName) throws NoSuchMethodException {
    return type.getDeclaredMethod(methodName, new Class[] { Callback[].class });
  }

  /**
   * Instantiates a proxy instance and assigns callback values. Implementation
   * detail: java.lang.reflect instances are not cached, so this method should not
   * be used on a hot path. This method is used when {@link #setUseCache(boolean)}
   * is set to {@code false}.
   *
   * @param type
   *         class to instantiate
   *
   * @return newly created instance
   */
  private Object createUsingReflection(Class type) {
    setThreadCallbacks(type, callbacks);
    try {

      if (argumentTypes != null) {
        return ReflectionUtils.newInstance(type, argumentTypes, arguments);
      }
      return ReflectionUtils.newInstance(type);
    }
    finally {
      // clear thread callbacks to allow them to be gc'd
      setThreadCallbacks(type, null);
    }
  }

  /**
   * Helper method to create an intercepted object. For finer control over the
   * generated instance, use a new instance of <code>Enhancer</code> instead of
   * this static method.
   *
   * @param type
   *         class to extend or interface to implement
   * @param callback
   *         the callback to use for all methods
   */
  public static Object create(Class type, Callback callback) {
    Enhancer e = new Enhancer();
    e.setSuperclass(type);
    e.setCallback(callback);
    return e.create();
  }

  /**
   * Helper method to create an intercepted object. For finer control over the
   * generated instance, use a new instance of <code>Enhancer</code> instead of
   * this static method.
   *
   * @param superclass
   *         class to extend or interface to implement
   * @param interfaces
   *         array of interfaces to implement, or null
   * @param callback
   *         the callback to use for all methods
   */
  public static Object create(Class superclass, Class[] interfaces, Callback callback) {
    Enhancer e = new Enhancer();
    e.setSuperclass(superclass);
    e.setInterfaces(interfaces);
    e.setCallback(callback);
    return e.create();
  }

  /**
   * Helper method to create an intercepted object. For finer control over the
   * generated instance, use a new instance of <code>Enhancer</code> instead of
   * this static method.
   *
   * @param superclass
   *         class to extend or interface to implement
   * @param interfaces
   *         array of interfaces to implement, or null
   * @param filter
   *         the callback filter to use when generating a new class
   * @param callbacks
   *         callback implementations to use for the enhanced object
   */
  public static Object create(Class superclass, Class[] interfaces, CallbackFilter filter, Callback[] callbacks) {
    Enhancer e = new Enhancer();
    e.setSuperclass(superclass);
    e.setInterfaces(interfaces);
    e.setCallbackFilter(filter);
    e.setCallbacks(callbacks);
    return e.create();
  }

  private void emitDefaultConstructor(ClassEmitter ce) {
    Constructor<Object> declaredConstructor;
    try {
      declaredConstructor = Object.class.getDeclaredConstructor();
    }
    catch (NoSuchMethodException e) {
      throw new IllegalStateException("Object should have default constructor ", e);
    }
    MethodInfo constructor = MethodInfoTransformer.getInstance().apply(declaredConstructor);
    CodeEmitter e = EmitUtils.beginMethod(ce, constructor, ACC_PUBLIC);
    e.load_this();
    e.dup();
    Signature sig = constructor.getSignature();
    e.super_invoke_constructor(sig);
    e.return_value();
    e.end_method();
  }

  private void emitConstructors(ClassEmitter ce, List<MethodInfo> constructors) {
    boolean seenNull = false;

    for (MethodInfo constructor : constructors) {
      if (currentData != null && !"()V".equals(constructor.getSignature().getDescriptor())) {
        continue;
      }
      CodeEmitter e = EmitUtils.beginMethod(ce, constructor, ACC_PUBLIC);
      e.load_this();
      e.dup();
      e.load_args();
      Signature sig = constructor.getSignature();
      seenNull = seenNull || sig.getDescriptor().equals("()V");
      e.super_invoke_constructor(sig);
      if (currentData == null) {
        e.invoke_static_this(BIND_CALLBACKS);
        if (!interceptDuringConstruction) {
          e.load_this();
          e.push(1);
          e.putfield(CONSTRUCTED_FIELD);
        }
      }
      e.return_value();
      e.end_method();
    }
    if (!classOnly && !seenNull && arguments == null) {
      throw new IllegalArgumentException("Superclass has no null constructors but no arguments were given");
    }
  }

  private int[] getCallbackKeys() {
    Type[] types = this.callbackTypes;
    int[] keys = new int[types.length];
    for (int i = 0; i < types.length; i++) {
      keys[i] = i;
    }
    return keys;
  }

  private void emitGetCallback(ClassEmitter ce, int[] keys) {
    final CodeEmitter e = ce.beginMethod(ACC_PUBLIC, GET_CALLBACK);
    e.load_this();
    e.invoke_static_this(BIND_CALLBACKS);
    e.load_this();
    e.load_arg(0);
    e.process_switch(keys, new ProcessSwitchCallback() {
      public void processCase(int key, Label end) {
        e.getfield(getCallbackField(key));
        e.goTo(end);
      }

      public void processDefault() {
        e.pop(); // stack height
        e.aconst_null();
      }
    });
    e.return_value();
    e.end_method();
  }

  private void emitSetCallback(ClassEmitter ce, int[] keys) {
    final CodeEmitter e = ce.beginMethod(ACC_PUBLIC, SET_CALLBACK);
    e.load_arg(0);
    e.process_switch(keys, new ProcessSwitchCallback() {
      public void processCase(int key, Label end) {
        e.load_this();
        e.load_arg(1);
        e.checkcast(callbackTypes[key]);
        e.putfield(getCallbackField(key));
        e.goTo(end);
      }

      public void processDefault() {
        // TODO: error?
      }
    });
    e.return_value();
    e.end_method();
  }

  private void emitSetCallbacks(ClassEmitter ce) {
    CodeEmitter e = ce.beginMethod(ACC_PUBLIC, SET_CALLBACKS);
    e.load_this();
    e.load_arg(0);
    for (int i = 0; i < callbackTypes.length; i++) {
      e.dup2();
      e.aaload(i);
      e.checkcast(callbackTypes[i]);
      e.putfield(getCallbackField(i));
    }
    e.return_value();
    e.end_method();
  }

  private void emitGetCallbacks(ClassEmitter ce) {
    CodeEmitter e = ce.beginMethod(ACC_PUBLIC, GET_CALLBACKS);
    e.load_this();
    e.invoke_static_this(BIND_CALLBACKS);
    e.load_this();
    e.push(callbackTypes.length);
    e.newArray(CALLBACK);
    for (int i = 0; i < callbackTypes.length; i++) {
      e.dup();
      e.push(i);
      e.load_this();
      e.getfield(getCallbackField(i));
      e.aastore();
    }
    e.return_value();
    e.end_method();
  }

  private void emitNewInstanceCallbacks(ClassEmitter ce) {
    CodeEmitter e = ce.beginMethod(ACC_PUBLIC, NEW_INSTANCE);
    Type thisType = getThisType(e);
    e.load_arg(0);
    e.invoke_static(thisType, SET_THREAD_CALLBACKS);
    emitCommonNewInstance(e);
  }

  private Type getThisType(CodeEmitter e) {
    if (currentData == null) {
      return e.getClassEmitter().getClassType();
    }
    else {
      return Type.fromClass(currentData.generatedClass);
    }
  }

  private void emitCommonNewInstance(CodeEmitter e) {
    Type thisType = getThisType(e);
    e.new_instance(thisType);
    e.dup();
    e.invoke_constructor(thisType);
    e.aconst_null();
    e.invoke_static(thisType, SET_THREAD_CALLBACKS);
    e.return_value();
    e.end_method();
  }

  private void emitNewInstanceCallback(ClassEmitter ce) {
    CodeEmitter e = ce.beginMethod(ACC_PUBLIC, SINGLE_NEW_INSTANCE);
    switch (callbackTypes.length) {
      case 0:
        // TODO: make sure Callback is null
        break;
      case 1:
        // for now just make a new array; TODO: optimize
        e.push(1);
        e.newArray(CALLBACK);
        e.dup();
        e.push(0);
        e.load_arg(0);
        e.aastore();
        e.invoke_static(getThisType(e), SET_THREAD_CALLBACKS);
        break;
      default:
        e.throw_exception(ILLEGAL_STATE_EXCEPTION, "More than one callback object required");
    }
    emitCommonNewInstance(e);
  }

  private void emitNewInstanceMultiarg(ClassEmitter ce, List constructors) {
    final CodeEmitter e = ce.beginMethod(ACC_PUBLIC, MULTIARG_NEW_INSTANCE);
    final Type thisType = getThisType(e);
    e.load_arg(2);
    e.invoke_static(thisType, SET_THREAD_CALLBACKS);
    e.new_instance(thisType);
    e.dup();
    e.load_arg(0);
    EmitUtils.constructorSwitch(e, constructors, new ObjectSwitchCallback() {
      @Override
      public void processCase(Object key, Label end) {
        MethodInfo constructor = (MethodInfo) key;
        Type types[] = constructor.getSignature().getArgumentTypes();
        for (int i = 0; i < types.length; i++) {
          e.load_arg(1);
          e.push(i);
          e.aaload();
          e.unbox(types[i]);
        }
        e.invoke_constructor(thisType, constructor.getSignature());
        e.goTo(end);
      }

      @Override
      public void processDefault() {
        e.throw_exception(ILLEGAL_ARGUMENT_EXCEPTION, "Constructor not found");
      }
    });
    e.aconst_null();
    e.invoke_static(thisType, SET_THREAD_CALLBACKS);
    e.return_value();
    e.end_method();
  }

  private void emitMethods(final ClassEmitter ce, List<MethodInfo> methods, List<Method> actualMethods) {

    final Type[] callbackTypes = this.callbackTypes;
    final CallbackGenerator[] generators = CallbackInfo.getGenerators(callbackTypes);

    final HashMap<MethodInfo, Integer> indexes = new HashMap<>();
    final HashMap<MethodInfo, Integer> originalModifiers = new HashMap<>();
    final HashMap<MethodInfo, Integer> positions = getIndexMap(methods);
    final HashMap<Class<?>, Set<Signature>> declToBridge = new HashMap<>();
    final HashMap<CallbackGenerator, List<MethodInfo>> groups = new HashMap<>();

    final Iterator<Method> it2 = (actualMethods != null) ? actualMethods.iterator() : null;

    for (final MethodInfo method : methods) {
      final Method actualMethod = (it2 != null) ? it2.next() : null;
      int index = filter.accept(actualMethod);

      if (index >= callbackTypes.length) {
        throw new IllegalArgumentException("Callback filter returned an index that is too large: " + index);
      }
      originalModifiers.put(method, Integer.valueOf((actualMethod != null) ? actualMethod.getModifiers() : method.getModifiers()));

      indexes.put(method, index);
      List<MethodInfo> group = groups.get(generators[index]);
      if (group == null) {
        groups.put(generators[index], group = new ArrayList(methods.size()));
      }
      group.add(method);

      if (actualMethod.isBridge()) {

        Set<Signature> bridges = declToBridge.get(actualMethod.getDeclaringClass());
        if (bridges == null) {
          declToBridge.put(actualMethod.getDeclaringClass(), bridges = new HashSet<>());
        }
        bridges.add(method.getSignature());
      }
    }

    final Set seenGen = new HashSet<>();
    final CodeEmitter se = ce.getStaticHook();

    se.new_instance(THREAD_LOCAL);
    se.dup();
    se.invoke_constructor(THREAD_LOCAL, CSTRUCT_NULL);
    se.putfield(THREAD_CALLBACKS_FIELD);

    final CallbackGenerator.Context context = new CallbackGenerator.Context() {
      Map<Signature, Signature> bridgeToTarget = null;

      @Override
      public ClassLoader getClassLoader() {
        return Enhancer.this.getClassLoader();
      }

      @Override
      public int getOriginalModifiers(MethodInfo method) {
        return originalModifiers.get(method).intValue();
      }

      @Override
      public int getIndex(MethodInfo method) {
        return indexes.get(method).intValue();
      }

      @Override
      public void emitCallback(CodeEmitter e, int index) {
        emitCurrentCallback(e, index);
      }

      @Override
      public Signature getImplSignature(MethodInfo method) {
        return rename(method.getSignature(), positions.get(method).intValue());
      }

      @Override
      public void emitLoadArgsAndInvoke(CodeEmitter e, MethodInfo method) {
        // If this is a bridge and we know the target was called from invokespecial,
        // then we need to invoke_virtual w/ the bridge target instead of doing
        // a super, because super may itself be using super, which would bypass
        // any proxies on the target.
        if (bridgeToTarget == null) {
          bridgeToTarget = BridgeMethodResolver.resolve(getClassLoader(), declToBridge);
        }
        final Signature bridgeTarget = bridgeToTarget.get(method.getSignature());
        if (bridgeTarget != null) {
          // checkcast each argument against the target's argument types
          final Type[] argumentTypes = bridgeTarget.getArgumentTypes();
          for (int i = 0; i < argumentTypes.length; i++) {
            e.load_arg(i);
            Type target = argumentTypes[i];
            if (!target.equals(method.getSignature().getArgumentTypes()[i])) {
              e.checkcast(target);
            }
          }

          e.invoke_virtual_this(bridgeTarget);

          Type retType = method.getSignature().getReturnType();
          // Not necessary to cast if the target & bridge have
          // the same return type.
          // (This conveniently includes void and primitive types,
          // which would fail if casted. It's not possible to
          // covariant from boxed to unbox (or vice versa), so no having
          // to box/unbox for bridges).
          // TODO: It also isn't necessary to checkcast if the return is
          // assignable from the target. (This would happen if a subclass
          // used covariant returns to narrow the return type within a bridge
          // method.)
          if (!retType.equals(bridgeTarget.getReturnType())) {
            e.checkcast(retType);
          }
        }
        else {
          e.load_args();
          e.super_invoke(method.getSignature());
        }
      }

      @Override
      public CodeEmitter beginMethod(ClassEmitter ce, MethodInfo method) {
        CodeEmitter e = EmitUtils.beginMethod(ce, method);
        if (!interceptDuringConstruction && !Modifier.isAbstract(method.getModifiers())) {
          Label constructed = e.make_label();
          e.load_this();
          e.getfield(CONSTRUCTED_FIELD);
          e.if_jump(CodeEmitter.NE, constructed);
          e.load_this();
          e.load_args();
          e.super_invoke();
          e.return_value();
          e.mark(constructed);
        }
        return e;
      }
    };
    for (int i = 0; i < callbackTypes.length; i++) {
      CallbackGenerator gen = generators[i];
      if (!seenGen.contains(gen)) {
        seenGen.add(gen);
        final List<MethodInfo> fmethods = groups.get(gen);
        if (fmethods != null) {
          try {
            gen.generate(ce, context, fmethods);
            gen.generateStatic(se, context, fmethods);
          }
          catch (RuntimeException x) {
            throw x;
          }
          catch (Exception x) {
            throw new CodeGenerationException(x);
          }
        }
      }
    }
    se.return_value();
    se.end_method();
  }

  static <T> HashMap<T, Integer> getIndexMap(List<T> list) {
    final HashMap<T, Integer> indexes = new HashMap<>(list.size());
    int index = 0;
    for (final T obj : list) {
      indexes.put(obj, index++);
    }
    return indexes;
  }

  private void emitSetThreadCallbacks(ClassEmitter ce) {
    CodeEmitter e = ce.beginMethod(ACC_PUBLIC | ACC_STATIC, SET_THREAD_CALLBACKS);
    e.getfield(THREAD_CALLBACKS_FIELD);
    e.load_arg(0);
    e.invoke_virtual(THREAD_LOCAL, THREAD_LOCAL_SET);
    e.return_value();
    e.end_method();
  }

  private void emitSetStaticCallbacks(ClassEmitter ce) {
    CodeEmitter e = ce.beginMethod(ACC_PUBLIC | ACC_STATIC, SET_STATIC_CALLBACKS);
    e.load_arg(0);
    e.putfield(STATIC_CALLBACKS_FIELD);
    e.return_value();
    e.end_method();
  }

  private void emitCurrentCallback(CodeEmitter e, int index) {
    e.load_this();
    e.getfield(getCallbackField(index));
    e.dup();
    Label end = e.make_label();
    e.ifnonnull(end);
    e.pop(); // stack height
    e.load_this();
    e.invoke_static_this(BIND_CALLBACKS);
    e.load_this();
    e.getfield(getCallbackField(index));
    e.mark(end);
  }

  private void emitBindCallbacks(ClassEmitter ce) {
    CodeEmitter e = ce.beginMethod(PRIVATE_FINAL_STATIC, BIND_CALLBACKS);
    Local me = e.make_local();
    e.load_arg(0);
    e.checkcast_this();
    e.store_local(me);

    Label end = e.make_label();
    e.load_local(me);
    e.getfield(BOUND_FIELD);
    e.if_jump(CodeEmitter.NE, end);
    e.load_local(me);
    e.push(1);
    e.putfield(BOUND_FIELD);

    e.getfield(THREAD_CALLBACKS_FIELD);
    e.invoke_virtual(THREAD_LOCAL, THREAD_LOCAL_GET);
    e.dup();
    Label found_callback = e.make_label();
    e.ifnonnull(found_callback);
    e.pop();

    e.getfield(STATIC_CALLBACKS_FIELD);
    e.dup();
    e.ifnonnull(found_callback);
    e.pop();
    e.goTo(end);

    e.mark(found_callback);
    e.checkcast(CALLBACK_ARRAY);
    e.load_local(me);
    e.swap();
    for (int i = callbackTypes.length - 1; i >= 0; i--) {
      if (i != 0) {
        e.dup2();
      }
      e.aaload(i);
      e.checkcast(callbackTypes[i]);
      e.putfield(getCallbackField(i));
    }

    e.mark(end);
    e.return_value();
    e.end_method();
  }

  private static String getCallbackField(int index) {
    return "today$Callback_" + index;
  }

  /**
   * Uses bytecode reflection to figure out the targets of all bridge methods that
   * use invokespecial and invokeinterface, so that we can later rewrite them to
   * use invokevirtual.
   *
   * <p>
   * For interface bridges, using invokesuper will fail since the method being
   * bridged to is in a superinterface, not a superclass. Starting in Java 8,
   * javac emits default bridge methods in interfaces, which use invokeinterface
   * to bridge to the target method.
   *
   * @author sberlin@gmail.com (Sam Berlin)
   */
  protected static class BridgeMethodResolver {

    /**
     * Finds all bridge methods that are being called with invokespecial & returns
     * them.
     */
    public static Map<Signature, Signature> resolve(
            ClassLoader classLoader, Map<Class<?>, Set<Signature>> declToBridge) //
    {

      final Map<Signature, Signature> resolved = new HashMap<>();

      for (final Entry<Class<?>, Set<Signature>> entry : declToBridge.entrySet()) {
        try {

          final InputStream is = //
                  classLoader.getResourceAsStream(entry.getKey().getName().replace('.', '/') + ".class");
          if (is == null) {
            return resolved;
          }

          try {
            new ClassReader(is).accept(new BridgedFinder(entry.getValue(), resolved), SKIP_FRAMES | SKIP_DEBUG);
          }
          finally {
            is.close();
          }
        }
        catch (IOException ignored) {
        }
      }
      return resolved;
    }

    private static class BridgedFinder extends ClassVisitor {

      private Signature currentMethod = null;

      private final Set<Signature> eligibleMethods;
      private final Map<Signature, Signature> resolved;

      BridgedFinder(Set<Signature> eligibleMethods, Map<Signature, Signature> resolved) {
        this.resolved = resolved;
        this.eligibleMethods = eligibleMethods;
      }

      @Override
      public void visit(int version, int access, String name, //
                        String signature, String superName, String[] interfaces) { }

      @Override
      public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
        final Signature sig = new Signature(name, desc);
        if (!eligibleMethods.remove(sig)) {
          return null;
        }

        final class BridgedFinderMethodVisitor extends MethodVisitor {

          @Override
          public void visitMethodInsn(int opcode, String owner, String name, String desc, boolean itf) {
            if ((opcode == INVOKESPECIAL || (itf && opcode == INVOKEINTERFACE)) && currentMethod != null) {
              final Signature target = new Signature(name, desc);
              // If the target signature is the same as the current,
              // we shouldn't change our bridge becaues invokespecial
              // is the only way to make progress (otherwise we'll
              // get infinite recursion). This would typically
              // only happen when a bridge method is created to widen
              // the visibility of a superclass' method.
              if (!target.equals(currentMethod)) {
                resolved.put(currentMethod, target);
              }
              currentMethod = null;
            }
          }
        }

        currentMethod = sig;
        return new BridgedFinderMethodVisitor();
      }
    }

  }

}
