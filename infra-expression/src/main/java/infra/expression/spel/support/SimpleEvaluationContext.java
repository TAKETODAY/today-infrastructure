/*
 * Copyright 2002-present the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

// Modifications Copyright 2017 - 2026 the TODAY authors.

package infra.expression.spel.support;

import org.jspecify.annotations.Nullable;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import infra.core.TypeDescriptor;
import infra.core.conversion.ConversionService;
import infra.core.conversion.Converter;
import infra.core.conversion.support.DefaultConversionService;
import infra.expression.BeanResolver;
import infra.expression.EvaluationContext;
import infra.expression.Expression;
import infra.expression.IndexAccessor;
import infra.expression.MethodResolver;
import infra.expression.OperatorOverloader;
import infra.expression.PropertyAccessor;
import infra.expression.TypeComparator;
import infra.expression.TypeConverter;
import infra.expression.TypeLocator;
import infra.expression.TypedValue;
import infra.expression.spel.SpelEvaluationException;
import infra.expression.spel.SpelMessage;
import infra.util.Assert;
import infra.util.InfraStrategies;

/**
 * A basic implementation of {@link EvaluationContext} that focuses on a subset
 * of essential SpEL features and customization options, targeting simple
 * condition evaluation and in particular data binding scenarios.
 *
 * <p>In many cases, the full extent of the SpEL language is not required and
 * should be meaningfully restricted. Examples include but are not limited to
 * data binding expressions, property-based filters, and others. To that effect,
 * {@code SimpleEvaluationContext} is tailored to support only a subset of the
 * SpEL language syntax, e.g. excluding references to Java types, constructors,
 * and bean references.
 *
 * <p>When creating a {@code SimpleEvaluationContext} you need to choose the level of
 * support that you need for data binding in SpEL expressions:
 * <ul>
 * <li>Data binding for read-only access</li>
 * <li>Data binding for read and write access</li>
 * <li>A custom {@code PropertyAccessor} (typically not reflection-based), potentially
 * combined with a {@link DataBindingPropertyAccessor}</li>
 * </ul>
 *
 * <p>Conveniently, {@link SimpleEvaluationContext#forReadOnlyDataBinding()} enables
 * read-only access to properties via {@link DataBindingPropertyAccessor}. Similarly,
 * {@link SimpleEvaluationContext#forReadWriteDataBinding()} enables read and write access
 * to properties. Alternatively, configure custom accessors via
 * {@link SimpleEvaluationContext#forPropertyAccessors}, potentially
 * {@linkplain Builder#withAssignmentDisabled() disable assignment}, and optionally
 * activate method resolution and/or a type converter through the builder.
 *
 * <p>Note that {@code SimpleEvaluationContext} is typically not configured
 * with a default root object. Instead it is meant to be created once and
 * used repeatedly through {@code getValue} calls on a predefined
 * {@link Expression} with both an
 * {@code EvaluationContext} and a root object as arguments:
 * {@link Expression#getValue(EvaluationContext, Object)}.
 *
 * <p>In addition to support for setting and looking up variables as defined in
 * the {@link EvaluationContext} API, {@code SimpleEvaluationContext} also
 * provides support for {@linkplain #setVariable(String, Object) registering} and
 * {@linkplain #lookupVariable(String) looking up} functions as variables. Since
 * functions share a common namespace with the variables in this evaluation
 * context, care must be taken to ensure that function names and variable names
 * do not overlap.
 *
 * <p>By default, {@code SimpleEvaluationContext} uses a {@link StandardTypeConverter}
 * backed by a restricted {@link DefaultConversionService}. This service excludes
 * converters that reflectively invoke constructors, static factory methods, or other
 * methods on arbitrary types. Common conversions remain available; register dedicated
 * converters for additional types via {@link Builder#withConverter}. Custom conversion
 * services and type converters are used as-is. See {@link Builder#withConversionService}
 * and {@link Builder#withTypeConverter} for details. The previous default can be restored
 * globally via the {@value #REFLECTIVE_CONVERSION_PROPERTY_NAME} property.
 *
 * <p>For more power and flexibility, in particular for internal configuration
 * scenarios, consider using {@link StandardEvaluationContext} instead.
 *
 * <p>Because a parsed {@code Expression} may cache accessor and executor state
 * resolved against a particular {@code EvaluationContext} configuration, a parsed
 * {@code Expression} must never be evaluated first against a
 * {@code StandardEvaluationContext} (or any other, less restrictive,
 * {@code EvaluationContext}) and later against a {@code SimpleEvaluationContext}. If the
 * same expression string must be evaluated under both kinds of contexts, parse it into
 * two distinct {@code Expression} instances instead. See
 * {@link infra.expression.Expression Expression} for further details on
 * this lifecycle contract.
 *
 * @author Rossen Stoyanchev
 * @author Juergen Hoeller
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see #forPropertyAccessors
 * @see #forReadOnlyDataBinding()
 * @see #forReadWriteDataBinding()
 * @see StandardEvaluationContext
 * @see StandardTypeConverter
 * @see DataBindingPropertyAccessor
 * @since 4.0
 */
public final class SimpleEvaluationContext implements EvaluationContext {

  /**
   * Property that restores the standard {@link DefaultConversionService} as the
   * default conversion service for {@code SimpleEvaluationContext}: {@value}.
   * <p>Setting this flag to {@code true} enables reflective type conversion in every
   * context that does not specify its own {@link ConversionService} or {@link TypeConverter}.
   * <p><strong>WARNING</strong>: This has the same security implications as passing a
   * standard {@code DefaultConversionService} to {@link Builder#withConversionService}.
   * Prefer registering dedicated converters via {@link Builder#withConverter}.
   * <p>Can be set as a JVM system property or via {@link InfraStrategies}.
   * @since 5.0
   */
  public static final String REFLECTIVE_CONVERSION_PROPERTY_NAME = "infra.expression.reflectiveConversion";

  private static final TypeLocator typeNotFoundTypeLocator = typeName -> {
    throw new SpelEvaluationException(SpelMessage.TYPE_NOT_FOUND, typeName);
  };

  private static volatile @Nullable DefaultConversionService restrictedConversionService;

  private final TypedValue rootObject;

  private final List<PropertyAccessor> propertyAccessors;

  private final List<IndexAccessor> indexAccessors;

  private final List<MethodResolver> methodResolvers;

  private final TypeConverter typeConverter;

  private final TypeComparator typeComparator = TypeComparator.STANDARD;

  private final OperatorOverloader operatorOverloader = OperatorOverloader.STANDARD;

  private final Map<String, Object> variables = new HashMap<>();

  private final boolean assignmentEnabled;

  private final boolean compilationSupported;

  private SimpleEvaluationContext(List<PropertyAccessor> propertyAccessors, List<IndexAccessor> indexAccessors,
          List<MethodResolver> resolvers, @Nullable TypeConverter converter, @Nullable TypedValue rootObject,
          boolean assignmentEnabled, boolean compilationSupported) {

    this.propertyAccessors = propertyAccessors;
    this.indexAccessors = indexAccessors;
    this.methodResolvers = resolvers;
    this.typeConverter = (converter != null ? converter : new StandardTypeConverter(getDefaultConversionService()));
    this.rootObject = (rootObject != null ? rootObject : TypedValue.NULL);
    this.assignmentEnabled = assignmentEnabled;
    this.compilationSupported = compilationSupported;
  }

  /**
   * Return the specified root object, if any.
   */
  @Override
  public TypedValue getRootObject() {
    return this.rootObject;
  }

  /**
   * Return the specified {@link PropertyAccessor} delegates, if any.
   *
   * @see #forPropertyAccessors
   */
  @Override
  public List<PropertyAccessor> getPropertyAccessors() {
    return this.propertyAccessors;
  }

  /**
   * Return the specified {@link IndexAccessor} delegates, if any.
   *
   * @see Builder#withIndexAccessors(IndexAccessor...)
   */
  @Override
  public List<IndexAccessor> getIndexAccessors() {
    return this.indexAccessors;
  }

  /**
   * Return the specified {@link MethodResolver} delegates, if any.
   *
   * @see Builder#withMethodResolvers
   */
  @Override
  public List<MethodResolver> getMethodResolvers() {
    return this.methodResolvers;
  }

  /**
   * {@code SimpleEvaluationContext} does not support the use of bean references.
   *
   * @return always {@code null}
   */
  @Override
  @Nullable
  public BeanResolver getBeanResolver() {
    return null;
  }

  /**
   * {@code SimpleEvaluationContext} does not support use of type references.
   *
   * @return {@code TypeLocator} implementation that raises a
   * {@link SpelEvaluationException} with {@link SpelMessage#TYPE_NOT_FOUND}
   */
  @Override
  public TypeLocator getTypeLocator() {
    return typeNotFoundTypeLocator;
  }

  /**
   * The configured {@link TypeConverter}.
   * <p>By default this is a {@link StandardTypeConverter} backed by a restricted
   * {@link ConversionService}. See the {@linkplain SimpleEvaluationContext
   * class-level documentation} for details.
   *
   * @see Builder#withConverter
   * @see Builder#withTypeConverter
   * @see Builder#withConversionService
   */
  @Override
  public TypeConverter getTypeConverter() {
    return this.typeConverter;
  }

  /**
   * Return an instance of {@link StandardTypeComparator}.
   */
  @Override
  public TypeComparator getTypeComparator() {
    return this.typeComparator;
  }

  /**
   * Return an instance of {@link StandardOperatorOverloader}.
   */
  @Override
  public OperatorOverloader getOperatorOverloader() {
    return this.operatorOverloader;
  }

  /**
   * {@code SimpleEvaluationContext} does not support variable assignment within
   * expressions.
   *
   * @throws SpelEvaluationException with {@link SpelMessage#VARIABLE_ASSIGNMENT_NOT_SUPPORTED}
   */
  @Override
  public TypedValue assignVariable(String name, Supplier<TypedValue> valueSupplier) {
    throw new SpelEvaluationException(SpelMessage.VARIABLE_ASSIGNMENT_NOT_SUPPORTED, "#" + name);
  }

  /**
   * Set a named variable or function in this evaluation context to the specified
   * value.
   * <p>A function can be registered as a {@link java.lang.reflect.Method} or
   * a {@link java.lang.invoke.MethodHandle}.
   * <p>Note that variables and functions share a common namespace in this
   * evaluation context. See the {@linkplain SimpleEvaluationContext
   * class-level documentation} for details.
   *
   * @param name the name of the variable or function to set
   * @param value the value to be placed in the variable or function
   * @see #lookupVariable(String)
   */
  @Override
  public void setVariable(String name, @Nullable Object value) {
    this.variables.put(name, value);
  }

  /**
   * Look up a named variable or function within this evaluation context.
   * <p>Note that variables and functions share a common namespace in this
   * evaluation context. See the {@linkplain SimpleEvaluationContext
   * class-level documentation} for details.
   *
   * @param name the name of the variable or function to look up
   * @return the value of the variable or function, or {@code null} if not found
   */
  @Override
  @Nullable
  public Object lookupVariable(String name) {
    return this.variables.get(name);
  }

  /**
   * Determine if assignment is enabled within expressions evaluated by this evaluation
   * context.
   * <p>If this method returns {@code false}, the assignment ({@code =}), increment
   * ({@code ++}), and decrement ({@code --}) operators are disabled.
   *
   * @return {@code true} if assignment is enabled; {@code false} otherwise
   * @see #forReadOnlyDataBinding()
   * @see Builder#withAssignmentDisabled()
   */
  @Override
  public boolean isAssignmentEnabled() {
    return this.assignmentEnabled;
  }

  /**
   * By default, expression compilation is not supported in this context.
   *
   * @return whether compilation was explicitly enabled
   * @since 5.0
   */
  @Override
  public boolean isCompilationSupported() {
    return this.compilationSupported;
  }

  /**
   * Create a {@code SimpleEvaluationContext} for the specified {@link PropertyAccessor}
   * delegates: typically a custom {@code PropertyAccessor} specific to a use case &mdash;
   * for example, for attribute resolution in a custom data structure &mdash; potentially
   * combined with a {@link DataBindingPropertyAccessor} if property dereferences are
   * needed as well.
   * <p>By default, assignment is enabled within expressions evaluated by the context
   * created via this factory method; however, assignment can be disabled via
   * {@link Builder#withAssignmentDisabled()}.
   *
   * @param accessors the accessor delegates to use
   * @see DataBindingPropertyAccessor#forReadOnlyAccess()
   * @see DataBindingPropertyAccessor#forReadWriteAccess()
   * @see #isAssignmentEnabled()
   * @see Builder#withAssignmentDisabled()
   */
  public static Builder forPropertyAccessors(PropertyAccessor... accessors) {
    for (PropertyAccessor accessor : accessors) {
      if (accessor.getClass() == ReflectivePropertyAccessor.class) {
        throw new IllegalArgumentException("SimpleEvaluationContext is not designed for use with a plain " +
                "ReflectivePropertyAccessor. Consider using DataBindingPropertyAccessor or a custom subclass.");
      }
    }
    return new Builder(accessors);
  }

  /**
   * Create a {@code SimpleEvaluationContext} for read-only access to
   * public properties via {@link DataBindingPropertyAccessor}.
   * <p>Assignment is disabled within expressions evaluated by the context created via
   * this factory method.
   *
   * @see DataBindingPropertyAccessor#forReadOnlyAccess()
   * @see #forPropertyAccessors
   * @see #isAssignmentEnabled()
   * @see Builder#withAssignmentDisabled()
   */
  public static Builder forReadOnlyDataBinding() {
    return new Builder(DataBindingPropertyAccessor.forReadOnlyAccess()).withAssignmentDisabled();
  }

  /**
   * Create a {@code SimpleEvaluationContext} for read-write access to
   * public properties via {@link DataBindingPropertyAccessor}.
   * <p>By default, assignment is enabled within expressions evaluated by the context
   * created via this factory method. Assignment can be disabled via
   * {@link Builder#withAssignmentDisabled()}; however, it is preferable to use
   * {@link #forReadOnlyDataBinding()} if you desire read-only access.
   *
   * @see DataBindingPropertyAccessor#forReadWriteAccess()
   * @see #forPropertyAccessors
   * @see #isAssignmentEnabled()
   * @see Builder#withAssignmentDisabled()
   */
  public static Builder forReadWriteDataBinding() {
    return new Builder(DataBindingPropertyAccessor.forReadWriteAccess());
  }

  /**
   * Return the shared standard service if the compatibility flag is enabled;
   * otherwise return the shared, lazily initialized restricted service.
   */
  private static DefaultConversionService getDefaultConversionService() {
    if (InfraStrategies.getFlag(REFLECTIVE_CONVERSION_PROPERTY_NAME)) {
      return DefaultConversionService.getSharedInstance();
    }
    DefaultConversionService service = restrictedConversionService;
    if (service == null) {
      synchronized (SimpleEvaluationContext.class) {
        service = restrictedConversionService;
        if (service == null) {
          service = createRestrictedConversionService();
          restrictedConversionService = service;
        }
      }
    }
    return service;
  }

  /** Create a default conversion service without the reflective Object-to-Object converters. */
  private static DefaultConversionService createRestrictedConversionService() {
    DefaultConversionService service = new DefaultConversionService();
    service.removeConvertible(Object.class, Object.class);
    return service;
  }

  /**
   * Builder for {@code SimpleEvaluationContext}.
   */
  public static final class Builder {

    private final List<PropertyAccessor> propertyAccessors;

    private List<IndexAccessor> indexAccessors = Collections.emptyList();

    private List<MethodResolver> resolvers = Collections.emptyList();

    @Nullable
    private TypeConverter typeConverter;

    @Nullable
    private DefaultConversionService conversionService;

    @Nullable
    private TypedValue rootObject;

    private boolean assignmentEnabled = true;

    private boolean compilationSupported;

    private Builder(PropertyAccessor... accessors) {
      this.propertyAccessors = Arrays.asList(accessors);
    }

    /**
     * Disable assignment within expressions evaluated by this evaluation context.
     *
     * @see SimpleEvaluationContext#isAssignmentEnabled()
     * @since 5.0
     */
    public Builder withAssignmentDisabled() {
      this.assignmentEnabled = false;
      return this;
    }

    /**
     * Explicitly permit compilation for expressions evaluated in this context.
     * Opting in for untrusted expressions removes the guards of interpreted mode.
     *
     * @return this builder
     * @since 5.0
     */
    public Builder withCompilationSupported() {
      this.compilationSupported = true;
      return this;
    }

    /**
     * Register the specified {@link IndexAccessor} delegates.
     *
     * @param indexAccessors the index accessors to use
     */
    public Builder withIndexAccessors(IndexAccessor... indexAccessors) {
      this.indexAccessors = Arrays.asList(indexAccessors);
      return this;
    }

    /**
     * Register the specified {@link MethodResolver} delegates for
     * a combination of property access and method resolution.
     *
     * @param resolvers the resolver delegates to use
     * @see #withInstanceMethods()
     * @see SimpleEvaluationContext#forPropertyAccessors
     */
    public Builder withMethodResolvers(MethodResolver... resolvers) {
      for (MethodResolver resolver : resolvers) {
        if (resolver.getClass() == ReflectiveMethodResolver.class) {
          throw new IllegalArgumentException("SimpleEvaluationContext is not designed for use with a plain " +
                  "ReflectiveMethodResolver. Consider using DataBindingMethodResolver or a custom subclass.");
        }
      }
      this.resolvers = Arrays.asList(resolvers);
      return this;
    }

    /**
     * Register a {@link DataBindingMethodResolver} for instance method invocation purposes
     * (i.e. not supporting static methods) in addition to the specified property accessors,
     * typically in combination with a {@link DataBindingPropertyAccessor}.
     *
     * @see #withMethodResolvers
     * @see SimpleEvaluationContext#forReadOnlyDataBinding()
     * @see SimpleEvaluationContext#forReadWriteDataBinding()
     */
    public Builder withInstanceMethods() {
      this.resolvers = Collections.singletonList(DataBindingMethodResolver.forInstanceMethodInvocation());
      return this;
    }

    /**
     * Register a custom {@link ConversionService}.
     * <p>By default a {@link StandardTypeConverter} backed by a restricted
     * {@link DefaultConversionService} is used. It does not convert values by
     * reflectively invoking constructors or methods on arbitrary types.
     * <p><strong>WARNING</strong>: The supplied service is used as-is. If it
     * performs reflective conversion, as a standard {@code DefaultConversionService}
     * does, treat every conversion target with the same care as any object reachable
     * from an expression. Prefer adding dedicated converters via {@link #withConverter}.
     *
     * @see #withConverter
     * @see #withTypeConverter
     * @see StandardTypeConverter#StandardTypeConverter(ConversionService)
     */
    public Builder withConversionService(ConversionService conversionService) {
      this.typeConverter = new StandardTypeConverter(conversionService);
      return this;
    }

    /**
     * Register a custom {@link TypeConverter}.
     * <p>By default a {@link StandardTypeConverter} backed by a restricted
     * {@link DefaultConversionService} is used.
     * <p><strong>WARNING</strong>: The supplied converter is used as-is. For
     * example, a {@link StandardTypeConverter} created with its no-arg constructor
     * delegates to the shared service, which supports reflective conversion. See
     * {@link #withConversionService} for the implications.
     *
     * @see #withConverter
     * @see #withConversionService
     * @see StandardTypeConverter#StandardTypeConverter()
     */
    public Builder withTypeConverter(TypeConverter converter) {
      this.typeConverter = converter;
      return this;
    }

    /**
     * Register a dedicated {@link Converter} in addition to the default converters.
     * <p>Prefer this method for individual conversions not supported by default, for
     * example {@code withConverter(String.class, ZoneId.class, ZoneId::of)}.
     * This method can be called multiple times but cannot be combined with
     * {@link #withConversionService} or {@link #withTypeConverter}; in that case,
     * {@link #build()} throws an {@link IllegalStateException}.
     *
     * @param <S> the source type
     * @param <T> the target type
     * @param sourceType the type to convert from
     * @param targetType the type to convert to
     * @param converter the converter to register
     * @return this builder
     * @see SimpleEvaluationContext#REFLECTIVE_CONVERSION_PROPERTY_NAME
     * @since 5.0
     */
    public <S, T> Builder withConverter(Class<S> sourceType, Class<T> targetType,
            Converter<? super S, ? extends T> converter) {

      DefaultConversionService service = this.conversionService;
      if (service == null) {
        service = (InfraStrategies.getFlag(REFLECTIVE_CONVERSION_PROPERTY_NAME) ?
                new DefaultConversionService() : createRestrictedConversionService());
        this.conversionService = service;
      }
      service.addConverter(sourceType, targetType, converter);
      return this;
    }

    /**
     * Specify a default root object to resolve against.
     * <p>Default is none, expecting an object argument at evaluation time.
     *
     * @see Expression#getValue(EvaluationContext)
     * @see Expression#getValue(EvaluationContext, Object)
     */
    public Builder withRootObject(Object rootObject) {
      this.rootObject = new TypedValue(rootObject);
      return this;
    }

    /**
     * Specify a typed root object to resolve against.
     * <p>Default is none, expecting an object argument at evaluation time.
     *
     * @see Expression#getValue(EvaluationContext)
     * @see Expression#getValue(EvaluationContext, Object)
     */
    public Builder withTypedRootObject(Object rootObject, TypeDescriptor typeDescriptor) {
      this.rootObject = new TypedValue(rootObject, typeDescriptor);
      return this;
    }

    public SimpleEvaluationContext build() {
      TypeConverter converter = this.typeConverter;
      if (this.conversionService != null) {
        Assert.state(converter == null,
                "withConverter() cannot be combined with withConversionService() or withTypeConverter()");
        converter = new StandardTypeConverter(this.conversionService);
      }
      return new SimpleEvaluationContext(this.propertyAccessors, this.indexAccessors,
              this.resolvers, converter, this.rootObject,
              this.assignmentEnabled, this.compilationSupported);
    }

  }

}
