/*
 * Copyright 2017 - 2025 the original author or authors.
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

package infra.beans.support;

import org.jspecify.annotations.Nullable;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Supplier;

import infra.beans.BeanInstantiationException;
import infra.beans.BeanUtils;
import infra.core.ConstructorNotFoundException;
import infra.lang.Assert;
import infra.reflect.MethodAccessor;
import infra.reflect.MethodInvoker;
import infra.reflect.ReflectionException;
import infra.util.ReflectionUtils;

/**
 * Abstract base class for instantiating objects using various strategies.
 * Provides a flexible mechanism to create objects either through constructors,
 * factory methods, or other custom instantiation logic.
 *
 * <p>This class is designed to be extended by specific implementations that
 * define the {@link #doInstantiate(Object[])} method to handle the actual
 * object creation process.
 *
 * <h3>Usage Examples</h3>
 *
 * <p>1. Using a {@link Supplier} to instantiate an object:
 * <pre>{@code
 * BeanInstantiator instantiator = BeanInstantiator.forSupplier(() -> new MyClass());
 * MyClass instance = (MyClass) instantiator.instantiate();
 * }</pre>
 *
 * <p>2. Instantiating a collection type:
 * <pre>{@code
 * BeanInstantiator instantiator = BeanInstantiator.forConstructor(ArrayList.class);
 * List<?> list = (List<?>) instantiator.instantiate();
 * }</pre>
 *
 * <p>3. Using a factory method to create an instance:
 * <pre>{@code
 * Method factoryMethod = MyClass.class.getDeclaredMethod("createInstance");
 * BeanInstantiator instantiator = BeanInstantiator.forStaticMethod(factoryMethod);
 * MyClass instance = (MyClass) instantiator.instantiate();
 * }</pre>
 *
 * <p>4. Creating an array type:
 * <pre>{@code
 * BeanInstantiator instantiator = BeanInstantiator.forConstructor(String[].class);
 * String[] array = (String[]) instantiator.instantiate();
 * }</pre>
 *
 * <h3>Key Features</h3>
 * <ul>
 *   <li>Supports instantiation via constructors, factory methods, and suppliers.</li>
 *   <li>Handles special cases like arrays, collections, and maps.</li>
 *   <li>Provides fallback mechanisms for inaccessible constructors.</li>
 *   <li>Allows customization through subclassing and overriding {@link #doInstantiate(Object[])}.</li>
 * </ul>
 *
 * <h3>Static Factory Methods</h3>
 * <p>This class provides several static factory methods to create instances of
 * {@link BeanInstantiator} for different use cases:
 * <ul>
 *   <li>{@link #forConstructor(Constructor)}: Creates an instantiator for a specific constructor.</li>
 *   <li>{@link #forMethod(Method, Object)}: Creates an instantiator for a factory method.</li>
 *   <li>{@link #forClass(Class)}: Automatically finds a suitable constructor for the target class.</li>
 *   <li>{@link #forSerialization(Class)}: Instantiates objects without calling their constructors.</li>
 * </ul>
 *
 * <h3>Error Handling</h3>
 * <p>If instantiation fails due to missing constructors or other issues, appropriate exceptions
 * such as {@link BeanInstantiationException} or {@link ConstructorNotFoundException} are thrown.
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see #instantiate()
 * @see #instantiate(Object[])
 * @see #doInstantiate(Object[])
 * @see java.lang.reflect.Constructor
 * @since 2020-08-13 19:31
 */
public abstract class BeanInstantiator {

  /**
   * Returns the {@link Constructor} associated with this {@code BeanInstantiator}.
   *
   * <p>This method may return {@code null} if no constructor is explicitly set or available.
   *
   * <p><b>Usage Example:</b>
   * <pre>{@code
   * BeanInstantiator instantiator = BeanInstantiator.forClass(MyClass.class);
   * Constructor<?> constructor = instantiator.getConstructor();
   * if (constructor != null) {
   *   System.out.println("Constructor found: " + constructor.getName());
   * }
   * else {
   *   System.out.println("No constructor is associated with this BeanInstantiator.");
   * }
   * }</pre>
   *
   * @return the {@link Constructor} associated with this {@code BeanInstantiator},
   * or {@code null} if no constructor is available
   */
  @Nullable
  public Constructor<?> getConstructor() {
    return null;
  }

  /**
   * Instantiates an object using the default constructor or factory method
   * associated with this {@code BeanInstantiator}. This method is a convenience
   * overload for {@link #instantiate(Object[])} and invokes it with no arguments.
   *
   * <p>This method internally delegates to the abstract {@link #doInstantiate(Object[])}
   * method, which performs the actual instantiation logic. If the instantiation fails,
   * a {@link BeanInstantiationException} is thrown.
   *
   * <p><b>Usage Example:</b>
   * <pre>{@code
   * BeanInstantiator instantiator = BeanInstantiator.forClass(MyClass.class);
   * try {
   *   Object instance = instantiator.instantiate();
   *   System.out.println("Instance created: " + instance);
   * }
   * catch (BeanInstantiationException e) {
   *   System.err.println("Failed to create instance: " + e.getMessage());
   * }
   * }</pre>
   *
   * @return the newly instantiated object
   * @throws BeanInstantiationException if the instantiation process fails
   */
  public Object instantiate() {
    return instantiate(null);
  }

  /**
   * Instantiates an object using the provided arguments. This method delegates
   * the actual instantiation logic to the abstract {@link #doInstantiate(Object[])}
   * method. If the instantiation fails, a {@link BeanInstantiationException} is thrown.
   *
   * <p>This method is useful when you need to pass arguments to a constructor or
   * factory method during object creation. If no arguments are required, consider
   * using the parameterless overload of {@link #instantiate()}.
   *
   * <p><b>Usage Example:</b>
   * <pre>{@code
   * BeanInstantiator instantiator = BeanInstantiator.forClass(MyClass.class);
   * Object[] args = new Object[] { "arg1", 123 };
   * try {
   *   Object instance = instantiator.instantiate(args);
   *   System.out.println("Instance created: " + instance);
   * }
   * catch (BeanInstantiationException e) {
   *   System.err.println("Failed to create instance: " + e.getMessage());
   * }
   * }</pre>
   *
   * @param args the arguments to pass to the constructor or factory method;
   * may be {@code null} if no arguments are required
   * @return the newly instantiated object
   * @throws BeanInstantiationException if the instantiation process fails
   */
  public final Object instantiate(@Nullable Object @Nullable [] args) {
    try {
      return doInstantiate(args);
    }
    catch (BeanInstantiationException e) {
      throw e;
    }
    catch (Throwable e) {
      throw new BeanInstantiationException(this + " cannot instantiate a bean", e);
    }
  }

  // internal new-instance impl @since 4.0
  protected abstract Object doInstantiate(@Nullable Object @Nullable [] args)
          throws Throwable;

  //---------------------------------------------------------------------
  // Static Factory Methods
  //---------------------------------------------------------------------

  /**
   * Creates a {@link BeanInstantiator} for the given constructor. This method is useful when you
   * need to instantiate objects using a specific constructor via reflection.
   *
   * <p>This method internally generates a {@link BeanInstantiator} implementation tailored to the
   * provided constructor. The generated instantiator can then be used to create instances of the
   * class associated with the constructor.
   *
   * <p><b>Usage Example:</b>
   * <pre>{@code
   * Constructor<MyClass> constructor = MyClass.class.getConstructor(String.class, int.class);
   * BeanInstantiator instantiator = BeanInstantiator.forConstructor(constructor);
   *
   * Object[] args = new Object[] { "example", 42 };
   * try {
   *   Object instance = instantiator.instantiate(args);
   *   System.out.println("Instance created: " + instance);
   * }
   * catch (BeanInstantiationException e) {
   *   System.err.println("Failed to create instance: " + e.getMessage());
   * }
   * }</pre>
   *
   * @param constructor the {@link Constructor} to be used for instantiation; must not be null
   * @return a {@link BeanInstantiator} capable of creating instances using the specified constructor
   * @throws IllegalArgumentException if the provided constructor is null
   */
  public static BeanInstantiator forConstructor(Constructor<?> constructor) {
    return new BeanInstantiatorGenerator(constructor).create();
  }

  /**
   * Creates a {@link BeanInstantiator} for invoking an instance method as a factory method.
   * This is useful when you want to use a specific method on a given object to create instances
   * of a target class. The returned instantiator will use the provided {@link MethodAccessor}
   * and object to invoke the method during instantiation.
   *
   * <p><b>Usage Example:</b>
   * <pre>{@code
   * // Assume MyClass has a method `createInstance()` that acts as a factory method
   * Method method = MyClass.class.getMethod("createInstance");
   * Object myClassInstance = new MyClass();
   *
   * MethodAccessor accessor = new SomeMethodAccessorImpl(method);
   * BeanInstantiator instantiator = BeanInstantiator.forMethod(accessor, myClassInstance);
   *
   * try {
   *   Object instance = instantiator.instantiate();
   *   System.out.println("Instance created: " + instance);
   * } catch (BeanInstantiationException e) {
   *   System.err.println("Failed to create instance: " + e.getMessage());
   * }
   * }</pre>
   *
   * @param accessor the {@link MethodAccessor} representing the factory method to be invoked;
   * must not be null
   * @param obj the object on which the method will be invoked; must not be null
   * @return a {@link BeanInstantiator} capable of creating instances using the specified
   * method and object
   * @throws IllegalArgumentException if either the accessor or the object is null
   */
  public static BeanInstantiator forMethod(MethodAccessor accessor, Object obj) {
    return new MethodAccessorBeanInstantiator(accessor, obj);
  }

  /**
   * Creates a {@link BeanInstantiator} for invoking a specific method on a given object as a factory method.
   * This is useful when you want to use a method of an object to create instances of a target class.
   *
   * <p>This method internally delegates to {@link #forMethod(MethodAccessor, Object)} by wrapping the provided
   * {@link Method} with a {@link MethodInvoker}. The returned instantiator will invoke the specified method
   * on the provided object during instantiation.
   *
   * <p><b>Usage Example:</b>
   * <pre>{@code
   * // Assume MyClass has a method `createInstance()` that acts as a factory method
   * Method method = MyClass.class.getMethod("createInstance");
   * MyClass myClassInstance = new MyClass();
   *
   * BeanInstantiator instantiator = BeanInstantiator.forMethod(method, myClassInstance);
   *
   * try {
   *   Object instance = instantiator.instantiate();
   *   System.out.println("Instance created: " + instance);
   * }
   * catch (BeanInstantiationException e) {
   *   System.err.println("Failed to create instance: " + e.getMessage());
   * }
   * }</pre>
   *
   * @param method the {@link Method} to be invoked as a factory method; must not be null
   * @param obj the object on which the method will be invoked; must not be null
   * @return a {@link BeanInstantiator} capable of creating instances using the specified method and object
   * @throws IllegalArgumentException if either the method or the object is null
   */
  public static BeanInstantiator forMethod(Method method, Object obj) {
    return forMethod(MethodInvoker.forMethod(method), obj);
  }

  /**
   * Creates a {@code BeanInstantiator} instance using the provided {@code MethodAccessor}
   * and an object supplier. This method is typically used to dynamically instantiate beans
   * by invoking a method represented by the {@code MethodAccessor}.
   *
   * <p>Example usage:
   * <pre>{@code
   *   MethodAccessor accessor = ...; // Obtain a MethodAccessor instance
   *   Supplier<Object> supplier = () -> new MyClass();
   *
   *   BeanInstantiator instantiator = forMethod(accessor, supplier);
   *   Object result = instantiator.instantiate();
   * }</pre>
   *
   * @param accessor the {@code MethodAccessor} representing the method to be invoked.
   * It must not be null.
   * @param obj a {@code Supplier} that provides the target object on which the
   * method will be invoked. It must not be null.
   * @return a {@code BeanInstantiator} instance capable of invoking the
   * specified method on the provided object.
   */
  public static BeanInstantiator forMethod(MethodAccessor accessor, Supplier<Object> obj) {
    return new MethodAccessorBeanInstantiator(accessor, obj);
  }

  /**
   * Creates a {@code BeanInstantiator} for the given method using a
   * {@code MethodInvoker} and an object supplier. This method simplifies
   * the instantiation process by wrapping the provided method and object
   * supplier into a reusable {@code BeanInstantiator}.
   *
   * <p>Example usage:
   * <pre>{@code
   * Method method = MyClass.class.getMethod("myMethod", String.class);
   * Supplier<Object> objSupplier = () -> new MyClass();
   *
   * BeanInstantiator instantiator = forMethod(method, objSupplier);
   * Object result = instantiator.invoke("exampleArgument");
   * }</pre>
   *
   * @param method the {@code Method} to be invoked by the
   * {@code BeanInstantiator}. Must not be null.
   * @param obj a {@code Supplier<Object>} providing the target object
   * on which the method will be invoked. Must not be null.
   * @return a {@code BeanInstantiator} instance that can invoke the
   * specified method on the supplied object.
   */
  public static BeanInstantiator forMethod(Method method, Supplier<Object> obj) {
    return forMethod(MethodInvoker.forMethod(method), obj);
  }

  /**
   * Creates a {@code BeanInstantiator} for a static method using the provided
   * {@link MethodAccessor}. This method is typically used when you need to
   * instantiate beans via static factory methods.
   *
   * <p>Example usage:
   * <pre>{@code
   * MethodAccessor accessor = ...; // Obtain a MethodAccessor instance
   * BeanInstantiator instantiator = BeanInstantiator.forStaticMethod(accessor);
   *
   * // Use the instantiator to create bean instances
   * Object beanInstance = instantiator.newInstance();
   * }</pre>
   *
   * @param accessor the {@link MethodAccessor} that provides access to the
   * static method used for bean instantiation; must not be null
   * @return a {@code BeanInstantiator} capable of creating bean instances
   * using the specified static method
   * @throws IllegalArgumentException if the provided {@code accessor} is null
   */
  public static BeanInstantiator forStaticMethod(MethodAccessor accessor) {
    Assert.notNull(accessor, "MethodAccessor is required");
    return new StaticMethodAccessorBeanInstantiator(accessor);
  }

  /**
   * Creates a {@code BeanInstantiator} for a given static method using a {@link Method} object.
   * This method internally delegates to another {@code forStaticMethod} variant that accepts
   * a {@link MethodInvoker}.
   *
   * <p>Example usage:
   * <pre>{@code
   * Method method = MyClass.class.getDeclaredMethod("myStaticMethod");
   * BeanInstantiator instantiator = BeanInstantiator.forStaticMethod(method);
   *
   * // Use the instantiator to invoke the static method
   * Object result = instantiator.newInstance();
   * }</pre>
   *
   * @param method the {@link Method} object representing the static method to be used
   * for creating the {@code BeanInstantiator}. Must not be null.
   * @return a {@code BeanInstantiator} instance configured to invoke the specified static method.
   */
  public static BeanInstantiator forStaticMethod(Method method) {
    return forStaticMethod(MethodInvoker.forMethod(method));
  }

  /**
   * Creates and returns a {@code BeanInstantiator} for the given target class.
   * This method attempts to obtain a suitable constructor for the specified
   * class using {@link BeanUtils#obtainConstructor(Class)}. The obtained
   * constructor is then used to create the {@code BeanInstantiator} by calling
   * {@link #forConstructor(Constructor)}.
   *
   * <p>Example usage:
   * <pre>{@code
   *   BeanInstantiator instantiator = BeanInstantiator.forClass(MyBean.class);
   *
   *   // Use the instantiator to create instances of MyBean
   *   MyBean instance = (MyBean) instantiator.newInstance();
   * }</pre>
   *
   * @param targetClass the class for which a {@code BeanInstantiator} is to be created.
   * Must not be null and should have at least one accessible constructor.
   * @return a {@code BeanInstantiator} capable of creating instances of the specified class.
   * @throws IllegalArgumentException if no suitable constructor is found for the target class.
   */
  public static BeanInstantiator forClass(final Class<?> targetClass) {
    Constructor<?> suitableConstructor = BeanUtils.obtainConstructor(targetClass);
    return forConstructor(suitableConstructor);
  }

  /**
   * Creates a {@link BeanInstantiator} instance using the provided function.
   * This method is useful for dynamically instantiating beans based on a
   * function that takes an array of arguments and returns an object.
   *
   * <p>Example usage:
   * <pre>{@code
   * Function<Object[], MyBean> factory = args -> new MyBean((String) args[0]);
   * BeanInstantiator instantiator = BeanInstantiator.forFunction(factory);
   *
   * MyBean bean = (MyBean) instantiator.instantiate(new Object[]{"example"});
   * }</pre>
   *
   * @param function the function to be used for creating bean instances;
   * must not be null. The function accepts an array of objects
   * as arguments and returns an instantiated object.
   * @return a {@link BeanInstantiator} that uses the provided function for
   * instantiating beans.
   * @throws IllegalArgumentException if the provided function is null.
   */
  public static BeanInstantiator forFunction(Function<@Nullable Object[], ?> function) {
    Assert.notNull(function, "instance function is required");
    return new FunctionInstantiator(function);
  }

  /**
   * Creates a {@code BeanInstantiator} instance using the provided {@link Supplier}.
   * This method is useful when you need to instantiate beans dynamically through a
   * supplier function. If the supplier is null, an exception will be thrown.
   *
   * <p>Example usage:
   * <pre>{@code
   * Supplier<MyBean> supplier = MyBean::new;
   * BeanInstantiator instantiator = BeanInstantiator.forSupplier(supplier);
   *
   * MyBean beanInstance = (MyBean) instantiator.instantiate();
   * }</pre>
   *
   * @param supplier the {@link Supplier} that provides the bean instance; must not be null
   * @return a new {@code BeanInstantiator} that uses the given supplier for instantiation
   * @throws IllegalArgumentException if the provided supplier is null
   */
  public static BeanInstantiator forSupplier(Supplier<?> supplier) {
    Assert.notNull(supplier, "instance supplier is required");
    return new SupplierInstantiator(supplier);
  }

  /**
   * Creates a {@code BeanInstantiator} for the given target class by analyzing its type
   * and constructor. This method supports instantiation of arrays, collections, maps,
   * and classes with a default constructor.
   *
   * <p>Usage examples:
   * <pre>{@code
   * // Instantiating a simple class with a default constructor
   * BeanInstantiator<MyClass> instantiator = BeanInstantiator.forConstructor(MyClass.class);
   * MyClass instance = instantiator.newInstance();
   *
   * // Instantiating an array type
   * BeanInstantiator<String[]> arrayInstantiator = BeanInstantiator.forConstructor(String[].class);
   * String[] arrayInstance = arrayInstantiator.newInstance();
   *
   * // Instantiating a collection type
   * BeanInstantiator<List<String>> listInstantiator = BeanInstantiator.forConstructor(ArrayList.class);
   * List<String> listInstance = listInstantiator.newInstance();
   * }</pre>
   *
   * @param <T> the type of the target class to be instantiated
   * @param target the target class for which the {@code BeanInstantiator} is created;
   * must not be null and should either have a default constructor or
   * be an array, collection, or map type
   * @return a {@code BeanInstantiator} instance capable of creating instances of the
   * specified target class
   * @throws IllegalArgumentException if the target class is null
   * @throws ReflectionException if the target class does not have a default constructor
   * and is not an array, collection, or map type
   */
  public static <T> BeanInstantiator forConstructor(final Class<T> target) {
    Assert.notNull(target, "target class is required");
    if (target.isArray()) {
      Class<?> componentType = target.getComponentType();
      return new ArrayInstantiator(componentType);
    }
    else if (Collection.class.isAssignableFrom(target)) {
      return new CollectionInstantiator(target);
    }
    else if (Map.class.isAssignableFrom(target)) {
      return new MapInstantiator(target);
    }

    try {
      final Constructor<T> constructor = target.getDeclaredConstructor();
      return forConstructor(constructor);
    }
    catch (NoSuchMethodException e) {
      throw new ReflectionException(
              "Target class: '%s' has no default constructor".formatted(target), e);
    }
  }

  /**
   * Creates a {@code ConstructorAccessor} for the given constructor using reflective instantiation.
   * This method ensures that the provided constructor is accessible and wraps it in a
   * {@code ReflectiveInstantiator} instance for further use.
   *
   * <p>Example usage:
   * <pre>{@code
   * Constructor<MyClass> constructor = MyClass.class.getDeclaredConstructor(String.class);
   * ConstructorAccessor accessor = forReflective(constructor);
   *
   * MyClass instance = (MyClass) accessor.newInstance("example");
   * }</pre>
   *
   * @param constructor the constructor to be used for reflective instantiation;
   * must not be {@code null}
   * @return a {@code ConstructorAccessor} instance that can be used to create new instances
   * of the class represented by the given constructor
   * @throws IllegalArgumentException if the provided constructor is {@code null}
   */
  public static ConstructorAccessor forReflective(Constructor<?> constructor) {
    Assert.notNull(constructor, "Constructor is required");
    ReflectionUtils.makeAccessible(constructor);
    return new ReflectiveInstantiator(constructor);
  }

  /**
   * Creates a {@code BeanInstantiator} for the given target class,
   * specifically tailored for serialization purposes. This method
   * leverages the Sun Reflection Factory to generate an instantiator
   * capable of creating instances of the specified class without
   * invoking its constructor.
   *
   * <p>Example usage:
   * <pre>{@code
   *   Class<?> targetClass = MySerializableClass.class;
   *   BeanInstantiator instantiator = BeanInstantiator.forSerialization(targetClass);
   *
   *   // Use the instantiator to create an instance of the target class
   *   Object instance = instantiator.newInstance();
   * }</pre>
   *
   * @param target the {@code Class} object representing the target class
   * for which an instantiator is to be created. Must not be null.
   * @return a {@code BeanInstantiator} instance that can create objects
   * of the specified target class without invoking its constructor.
   */
  public static BeanInstantiator forSerialization(final Class<?> target) {
    return new SunReflectionFactoryInstantiator(target);
  }

}
