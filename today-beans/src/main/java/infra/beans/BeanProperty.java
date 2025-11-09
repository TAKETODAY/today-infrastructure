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

package infra.beans;

import org.jspecify.annotations.Nullable;

import java.beans.PropertyDescriptor;
import java.io.Serial;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Optional;

import infra.beans.support.BeanInstantiator;
import infra.core.MethodParameter;
import infra.core.conversion.ConversionService;
import infra.lang.Assert;
import infra.reflect.Property;
import infra.reflect.PropertyAccessor;
import infra.reflect.SetterMethod;
import infra.util.ClassUtils;
import infra.util.ReflectionUtils;

/**
 * Represents a Java bean property with support for reading and writing property values.
 * This class extends {@link Property} and provides additional functionality for
 * instantiating property values, accessing fields or methods, and handling type conversions.
 *
 * <p>Instances of this class are typically created using static factory methods such as
 * {@link #valueOf(Field)}, {@link #valueOf(Class, String)}, or
 * {@link #valueOf(Method, Method)}.</p>
 *
 * <p><b>Key Features:</b></p>
 * <ul>
 *   <li>Supports both field-based and method-based property access.</li>
 *   <li>Provides mechanisms for instantiating complex property types.</li>
 *   <li>Handles optional types like {@link Optional} during value assignment.</li>
 *   <li>Supports type conversion using {@link TypeConverter} or {@link ConversionService}.</li>
 * </ul>
 *
 * <p><b>Usage Examples:</b></p>
 *
 * Example 1: Creating a BeanProperty from a field and setting its value:
 * <pre>{@code
 * Field field = MyClass.class.getDeclaredField("myProperty");
 * BeanProperty property = BeanProperty.valueOf(field);
 *
 * MyClass instance = new MyClass();
 * property.setValue(instance, "newValue");
 * }</pre>
 *
 * Example 2: Creating a BeanProperty from getter and setter methods:
 * <pre>{@code
 * Method readMethod = MyClass.class.getMethod("getMyProperty");
 * Method writeMethod = MyClass.class.getMethod("setMyProperty", String.class);
 * BeanProperty property = BeanProperty.valueOf(readMethod, writeMethod);
 *
 * MyClass instance = new MyClass();
 * property.setValue(instance, "newValue");
 * }</pre>
 *
 * Example 3: Instantiating a property value for a complex type:
 * <pre>{@code
 * BeanProperty property = BeanProperty.valueOf(MyClass.class, "myComplexProperty");
 * Object newInstance = property.instantiate();
 * }</pre>
 *
 * Example 4: Using a custom type converter for value assignment:
 * <pre>{@code
 * MyCustomType value = new MyCustomType();
 * TypeConverter converter = ...; // Custom converter implementation
 *
 * BeanProperty property = BeanProperty.valueOf(MyClass.class, "myCustomProperty");
 * MyClass instance = new MyClass();
 * property.setValue(instance, value, converter);
 * }</pre>
 *
 * <p><b>Thread Safety:</b></p>
 * Instances of this class are not guaranteed to be thread-safe. If shared across threads,
 * proper synchronization must be applied.
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see #isWriteable()
 * @see #isReadable()
 * @since 3.0 2021/1/27 22:28
 */
public class BeanProperty extends Property {

  @Serial
  private static final long serialVersionUID = 1L;

  @Nullable
  private transient PropertyAccessor accessor;

  @Nullable
  private transient BeanInstantiator instantiator;

  protected BeanProperty(Field field, @Nullable Method readMethod, @Nullable Method writeMethod) {
    super(field, readMethod, writeMethod);
  }

  protected BeanProperty(@Nullable String name, @Nullable Method readMethod,
          @Nullable Method writeMethod, @Nullable Class<?> declaringClass) {
    super(name, readMethod, writeMethod, declaringClass);
  }

  protected BeanProperty(PropertyDescriptor descriptor, Class<?> declaringClass) {
    super(descriptor.getName(), descriptor.getReadMethod(), descriptor.getWriteMethod(), declaringClass);
    if (writeMethod != null && descriptor instanceof GenericTypeAwarePropertyDescriptor generic) {
      this.writeMethodParameter = generic.getWriteMethodParameter();
    }
  }

  /**
   * Instantiates a new instance of the property's type using the default constructor.
   * This method is a convenience wrapper for {@link #instantiate(Object[])} with no arguments.
   *
   * <p>Example usage:
   * <pre>{@code
   * BeanProperty property = BeanProperty.valueOf(MyClass.class, "myProperty");
   * Object instance = property.instantiate();
   * System.out.println("Instantiated object: " + instance);
   * }</pre>
   *
   * <p>If the property's type is a simple value type (e.g., primitive types, String, etc.),
   * this method will throw a {@link BeanInstantiationException}.
   *
   * @return a new instance of the property's type
   * @throws BeanInstantiationException if the property's type cannot be instantiated,
   * such as when it is a simple value type or lacks a default constructor
   */
  public Object instantiate() {
    return instantiate(null);
  }

  /**
   * Instantiates a new instance of the property's type using the specified constructor arguments.
   * This method delegates the instantiation process to a {@link BeanInstantiator} instance,
   * which is lazily initialized if not already set.
   *
   * <p>If the property's type is a simple value type (e.g., primitive types, String, etc.),
   * this method will throw a {@link BeanInstantiationException}.
   *
   * <p>Example usage:
   * <pre>{@code
   * BeanProperty property = BeanProperty.valueOf(MyClass.class, "myProperty");
   * Object[] args = { "arg1", 123 };
   * Object instance = property.instantiate(args);
   * System.out.println("Instantiated object: " + instance);
   * }</pre>
   *
   * @param args an array of arguments to pass to the constructor of the property's type;
   * can be {@code null} if the constructor does not require arguments
   * @return a new instance of the property's type, initialized with the provided arguments
   * @throws BeanInstantiationException if the property's type cannot be instantiated,
   * such as when it is a simple value type or lacks a suitable constructor
   */
  public Object instantiate(@Nullable Object @Nullable [] args) {
    BeanInstantiator constructor = this.instantiator;
    if (constructor == null) {
      Class<?> type = getType();
      if (BeanUtils.isSimpleValueType(type)) {
        throw new BeanInstantiationException(type, "Cannot be instantiated a simple type");
      }
      constructor = BeanInstantiator.forConstructor(type);
      this.instantiator = constructor;
    }
    return constructor.instantiate(args);
  }

  /**
   * Retrieves the value of the property from the given target object.
   *
   * <p>This method delegates the actual retrieval process to the {@link PropertyAccessor}
   * obtained via the {@code accessor()} method. The {@code PropertyAccessor} is responsible
   * for accessing the property value using reflection or other mechanisms.
   *
   * <p>Example usage:
   * <pre>{@code
   * BeanProperty property = BeanProperty.valueOf(MyClass.class, "myProperty");
   * MyClass instance = new MyClass();
   * instance.setMyProperty("exampleValue");
   *
   * Object value = property.getValue(instance);
   * System.out.println("Retrieved value: " + value);
   * }</pre>
   *
   * <p>If the property does not exist or cannot be accessed, the behavior depends on the
   * implementation of the {@code PropertyAccessor}.
   *
   * @param object the target object from which the property value is retrieved; can be {@code null}
   * if the property accessor supports null objects
   * @return the value of the property, or {@code null} if the property value is null or
   * if the property cannot be accessed
   */
  @Nullable
  public Object getValue(Object object) {
    return accessor().get(object);
  }

  /**
   * Sets the value of the property on the given target object.
   * If the provided value is {@code null} and the property type is {@link Optional},
   * the value is converted to an empty {@link Optional}.
   *
   * <p>This method delegates the actual setting process to the {@link #setDirectly(Object, Object)}
   * method after handling any special cases for the value (e.g., converting {@code null} to
   * {@link Optional#empty()} if applicable).
   *
   * <p>Example usage:
   * <pre>{@code
   * BeanProperty property = BeanProperty.valueOf(MyClass.class, "myProperty");
   * MyClass instance = new MyClass();
   *
   * // Set a regular value
   * property.setValue(instance, "exampleValue");
   *
   * // Set null for an Optional-typed property
   * property.setValue(instance, null);
   * }</pre>
   *
   * <p>If the property is read-only, this method will throw a
   * {@link NotWritablePropertyException}.
   *
   * @param obj the target object on which the property value is set; must not be {@code null}
   * @param value the value to set for the property; can be {@code null}
   * @throws NotWritablePropertyException if the property is read-only and cannot be modified
   * @see #setDirectly(Object, Object)
   * @see #handleOptional(Object, Class)
   * @see SetterMethod#set(Object, Object)
   */
  public final void setValue(Object obj, @Nullable Object value) {
    value = handleOptional(value, getType());
    setDirectly(obj, value);
  }

  /**
   * Sets the value of the property on the given target object, using a provided {@link TypeConverter}
   * to handle type conversion if necessary. If the provided value is {@code null} and the property
   * type is {@link Optional}, the value is converted to an empty {@link Optional}.
   *
   * <p>This method performs the following steps:
   * <ol>
   *   <li>Determines the property type from the write method parameter or the declared type.</li>
   *   <li>Handles special cases for {@code null} values when the property type is {@link Optional}.</li>
   *   <li>Uses the provided {@link TypeConverter} to convert the value to the required type if it is not assignable.</li>
   *   <li>Sets the processed value directly on the target object using {@link #setDirectly(Object, Object)}.</li>
   * </ol>
   *
   * <p>Example usage:
   * <pre>{@code
   * BeanProperty property = BeanProperty.valueOf(MyClass.class, "myProperty");
   * MyClass instance = new MyClass();
   * TypeConverter converter = new SimpleTypeConverter();
   *
   * // Set a regular value with type conversion
   * property.setValue(instance, "42", converter);
   *
   * // Set null for an Optional-typed property
   * property.setValue(instance, null, converter);
   * }</pre>
   *
   * <p>If the property is read-only, this method will throw a {@link NotWritablePropertyException}.
   *
   * @param obj the target object on which the property value is set; must not be {@code null}
   * @param value the value to set for the property; can be {@code null}
   * @param converter the {@link TypeConverter} used to convert the value to the required type; must not be {@code null}
   * @throws NotWritablePropertyException if the property is read-only and cannot be modified
   * @see #setDirectly(Object, Object)
   * @see #handleOptional(Object, Class)
   * @see SetterMethod#set(Object, Object)
   * @since 4.0
   */
  public final void setValue(Object obj, @Nullable Object value, TypeConverter converter) {
    Class<?> propertyType;
    // write-method parameter type
    MethodParameter writeMethodParameter = getWriteMethodParameter();
    if (writeMethodParameter != null) {
      propertyType = writeMethodParameter.getParameterType();
    }
    else {
      propertyType = getType();
    }
    if (value == null && propertyType == Optional.class) {
      value = Optional.empty();
    }
    else if (!ClassUtils.isAssignableValue(propertyType, value)) {
      Object necessary = converter.convertIfNecessary(value, propertyType, getTypeDescriptor());
      value = handleOptional(necessary, propertyType);
    }
    setDirectly(obj, value);
  }

  /**
   * Sets the value of a property on the given object, performing necessary type conversion
   * if the provided value is not directly assignable to the property's type.
   *
   * <p>This method determines the property type using either the write method parameter type
   * or the generic type of the property. If the value is {@code null} and the property type
   * is {@link Optional}, it assigns an empty {@link Optional}. Otherwise, it uses the provided
   * {@link ConversionService} to convert the value to the appropriate type.
   *
   * <p>Example usage:
   * <pre>{@code
   * MyClass obj = new MyClass();
   * ConversionService conversionService = new DefaultConversionService();
   *
   * // Set a String value to a property of type Integer
   * setValue(obj, "42", conversionService);
   *
   * // Set a null value to a property of type Optional<String>
   * setValue(obj, null, conversionService);
   * }</pre>
   *
   * @param obj the target object on which the property value will be set
   * @param value the value to be assigned to the property; can be {@code null}
   * @param conversionService the service used to perform type conversion if necessary
   * @throws NotWritablePropertyException If this property is read only
   * @see SetterMethod#set(Object, Object)
   * @since 5.0
   */
  public final void setValue(Object obj, @Nullable Object value, ConversionService conversionService) {
    Class<?> propertyType;
    // write-method parameter type
    MethodParameter writeMethodParameter = getWriteMethodParameter();
    if (writeMethodParameter != null) {
      propertyType = writeMethodParameter.getParameterType();
    }
    else {
      propertyType = getType();
    }
    if (value == null && propertyType == Optional.class) {
      value = Optional.empty();
    }
    else if (!ClassUtils.isAssignableValue(propertyType, value)) {
      Object necessary = conversionService.convert(value, getTypeDescriptor());
      value = handleOptional(necessary, propertyType);
    }
    setDirectly(obj, value);
  }

  // @since 4.0
  @Nullable
  static Object handleOptional(@Nullable Object value, Class<?> propertyType) {
    // convertedValue == null
    if (value == null && propertyType == Optional.class) {
      value = Optional.empty();
    }
    return value;
  }

  /**
   * Sets the value of a property directly on the given object using the
   * internal accessor mechanism. This method ensures that the property is
   * writable before attempting to set the value. If the property is not
   * writable, a {@link NotWritablePropertyException} is thrown.
   *
   * <p>Example usage:
   * <pre>{@code
   *   MyClass myObject = new MyClass();
   *
   *   // Set the property value directly
   *   setDirectly(myObject, "newValue");
   *
   *   // Attempting to set a non-writable property will throw an exception
   *   try {
   *     setDirectly(myObject, someValue);
   *   } catch (NotWritablePropertyException e) {
   *     System.err.println("Property is not writable: " + e.getMessage());
   *   }
   * }</pre>
   *
   * @param obj the target object on which the property value will be set.
   * Must not be null.
   * @param value the value to set for the property. Can be null if the
   * property allows null values.
   * @throws NotWritablePropertyException if the property is not writable.
   * @see SetterMethod#set(Object, Object)
   * @since 3.0.2
   */
  public final void setDirectly(Object obj, @Nullable Object value) {
    var accessor = accessor();
    if (!accessor.isWriteable()) {
      throw new NotWritablePropertyException(getDeclaringClass(), getName());
    }
    accessor.set(obj, value);
  }

  // PropertyAccessor

  /**
   * Returns the {@code PropertyAccessor} instance associated with this object.
   * If the accessor has not been initialized yet, it will be created and cached
   * for future use. This method ensures that the same accessor instance is returned
   * on subsequent calls unless it was explicitly reset or cleared.
   *
   * <p>Example usage:
   * <pre>{@code
   *   MyClass myObject = new MyClass();
   *
   *   // Retrieve the accessor instance
   *   PropertyAccessor accessor = accessor();
   *
   *   // Use the accessor to perform operations
   *   accessor.setValue("key", "value");
   *   String value = (String) accessor.getValue("key");
   * }</pre>
   *
   * @return the {@code PropertyAccessor} instance, which may be lazily initialized
   */
  public PropertyAccessor accessor() {
    PropertyAccessor accessor = this.accessor;
    if (accessor == null) {
      accessor = createAccessor();
      this.accessor = accessor;
    }
    return accessor;
  }

  /**
   * @since 3.0.2
   */
  private PropertyAccessor createAccessor() {
    Field field = getField();
    if (field == null) {
      return PropertyAccessor.forMethod(readMethod, writeMethod);
    }
    return PropertyAccessor.forField(field, readMethod, writeMethod);
  }

  @Override
  public boolean isWriteable() {
    return accessor().isWriteable();
  }

  // @since 5.0
  void setField(@Nullable Field field) {
    this.field = field;
  }

  //---------------------------------------------------------------------
  // Override method of Object
  //---------------------------------------------------------------------

  // static

  /**
   * Creates a new {@code BeanProperty} instance based on the provided {@code Field}.
   * This method retrieves the corresponding read and write methods for the field
   * using reflection utilities and constructs a {@code BeanProperty} object.
   *
   * <p>Example usage:
   * <pre>{@code
   * Field field = MyClass.class.getDeclaredField("myField");
   * BeanProperty property = BeanProperty.valueOf(field);
   *
   * // Accessing the field's read and write methods
   * Method readMethod = property.getReadMethod();
   * Method writeMethod = property.getWriteMethod();
   * }</pre>
   *
   * @param field the {@code Field} object representing the bean property;
   * must not be {@code null}
   * @return a new {@code BeanProperty} instance encapsulating the provided field
   * and its associated read and write methods
   * @throws IllegalArgumentException if the provided {@code field} is {@code null}
   * @since 4.0
   */
  public static BeanProperty valueOf(Field field) {
    Assert.notNull(field, "Field is required");
    Method readMethod = ReflectionUtils.getReadMethod(field);
    Method writeMethod = ReflectionUtils.getWriteMethod(field);
    return new BeanProperty(field, readMethod, writeMethod);
  }

  /**
   * Returns a {@code BeanProperty} instance representing the specified field
   * of the given target class. If the field does not exist in the target class,
   * a {@code NoSuchPropertyException} is thrown.
   *
   * <p>Example usage:
   * <pre>{@code
   *   Class<?> clazz = MyClass.class;
   *   String fieldName = "myField";
   *
   *   try {
   *     BeanProperty property = BeanProperty.valueOf(clazz, fieldName);
   *     System.out.println("Field found: " + property.getName());
   *   } catch (NoSuchPropertyException e) {
   *     System.err.println("Field not found: " + e.getMessage());
   *   }
   * }</pre>
   *
   * @param targetClass the class in which the field is declared
   * @param name the name of the field to retrieve
   * @return a {@code BeanProperty} instance representing the field
   * @throws NoSuchPropertyException if no field with the specified name exists
   * in the target class
   */
  public static BeanProperty valueOf(Class<?> targetClass, String name) {
    Field field = ReflectionUtils.findField(targetClass, name);
    if (field == null) {
      throw new NoSuchPropertyException(targetClass, name);
    }

    return valueOf(field);
  }

  /**
   * Creates a new instance of {@code BeanProperty} based on the provided read and write methods.
   * This method is a convenience overload that internally calls the more detailed
   * {@code valueOf} method with a null annotation context.
   *
   * <p>Example usage:
   * <pre>{@code
   * Method readMethod = MyClass.class.getMethod("getPropertyName");
   * Method writeMethod = MyClass.class.getMethod("setPropertyName", String.class);
   *
   * BeanProperty property = BeanProperty.valueOf(readMethod, writeMethod);
   * }</pre>
   *
   * @param readMethod the method used to read the property value. Must not be null.
   * @param writeMethod the method used to write the property value. Can be null if the
   * property is read-only.
   * @return a new instance of {@code BeanProperty} configured with the specified read and
   * write methods.
   */
  public static BeanProperty valueOf(Method readMethod, @Nullable Method writeMethod) {
    return valueOf(readMethod, writeMethod, null);
  }

  /**
   * Returns a {@code BeanProperty} instance representing the property
   * defined by the given read and write methods, along with the declaring class.
   *
   * <p>This method is a convenience overload that internally delegates to
   * another {@code valueOf} method, passing {@code null} as the name parameter.
   *
   * <p>Example usage:
   * <pre>{@code
   *  Method readMethod = MyClass.class.getMethod("getPropertyName");
   *  Method writeMethod = MyClass.class.getMethod("setPropertyName", String.class);
   *
   *  BeanProperty property = BeanProperty.valueOf(readMethod, writeMethod, MyClass.class);
   * }</pre>
   *
   * @param readMethod the method used to read the property value,
   * or {@code null} if the property is write-only
   * @param writeMethod the method used to write the property value,
   * or {@code null} if the property is read-only
   * @param declaringClass the class that declares the property,
   * or {@code null} if not applicable
   * @return a {@code BeanProperty} instance encapsulating the property details
   */
  public static BeanProperty valueOf(@Nullable Method readMethod, @Nullable Method writeMethod, @Nullable Class<?> declaringClass) {
    return valueOf(null, readMethod, writeMethod, declaringClass);
  }

  /**
   * Creates and returns a new {@code BeanProperty} instance based on the provided parameters.
   * This method is typically used to define a property of a JavaBean, including its name,
   * read method, write method, and declaring class.
   *
   * <p>Example usage:
   * <pre>{@code
   * Method readMethod = MyClass.class.getMethod("getPropertyName");
   * Method writeMethod = MyClass.class.getMethod("setPropertyName", String.class);
   *
   * BeanProperty property = BeanProperty.valueOf(
   *     "propertyName",
   *     readMethod,
   *     writeMethod,
   *     MyClass.class
   * );
   * }</pre>
   *
   * @param propertyName the name of the property; may be {@code null} if not applicable
   * @param readMethod the method used to read the property value; may be {@code null}
   * @param writeMethod the method used to write the property value; may be {@code null}
   * @param declaringClass the class that declares the property; may be {@code null}
   * @return a new {@code BeanProperty} instance representing the specified property
   * @throws IllegalStateException if both {@code readMethod} and {@code writeMethod} are {@code null},
   * indicating that the property is neither readable nor writable
   */
  public static BeanProperty valueOf(@Nullable String propertyName, @Nullable Method readMethod,
          @Nullable Method writeMethod, @Nullable Class<?> declaringClass) {
    if (readMethod == null && writeMethod == null) {
      throw new IllegalStateException("Property is neither readable nor writeable");
    }
    return new BeanProperty(propertyName, readMethod, writeMethod, declaringClass);
  }

}
