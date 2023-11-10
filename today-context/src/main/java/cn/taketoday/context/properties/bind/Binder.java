/*
 * Copyright 2017 - 2023 the original author or authors.
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

package cn.taketoday.context.properties.bind;

import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Supplier;

import cn.taketoday.beans.PropertyEditorRegistry;
import cn.taketoday.beans.factory.config.ConfigurableBeanFactory;
import cn.taketoday.context.properties.bind.Bindable.BindRestriction;
import cn.taketoday.context.properties.source.ConfigurationProperty;
import cn.taketoday.context.properties.source.ConfigurationPropertyName;
import cn.taketoday.context.properties.source.ConfigurationPropertySource;
import cn.taketoday.context.properties.source.ConfigurationPropertySources;
import cn.taketoday.context.properties.source.ConfigurationPropertyState;
import cn.taketoday.core.conversion.ConversionService;
import cn.taketoday.core.conversion.ConverterNotFoundException;
import cn.taketoday.core.env.ConfigurableEnvironment;
import cn.taketoday.format.support.ApplicationConversionService;
import cn.taketoday.format.support.DefaultFormattingConversionService;
import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;

/**
 * A container object which Binds objects from one or more
 * {@link ConfigurationPropertySource ConfigurationPropertySources}.
 *
 * @author Phillip Webb
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @author Madhura Bhave
 * @since 4.0
 */
public class Binder {

  private static final Set<Class<?>> NON_BEAN_CLASSES = Set.of(Object.class, Class.class);

  private final Iterable<ConfigurationPropertySource> sources;

  private final PlaceholdersResolver placeholdersResolver;

  private final BindConverter bindConverter;

  private final BindHandler defaultBindHandler;

  private final Map<BindMethod, List<DataObjectBinder>> dataObjectBinders;

  /**
   * Create a new {@link Binder} instance for the specified sources. A
   * {@link DefaultFormattingConversionService} will be used for all conversion.
   *
   * @param sources the sources used for binding
   */
  public Binder(ConfigurationPropertySource... sources) {
    this(Arrays.asList(sources), null, null, null);
  }

  /**
   * Create a new {@link Binder} instance for the specified sources. A
   * {@link DefaultFormattingConversionService} will be used for all conversion.
   *
   * @param sources the sources used for binding
   */
  public Binder(Iterable<ConfigurationPropertySource> sources) {
    this(sources, null, null, null);
  }

  /**
   * Create a new {@link Binder} instance for the specified sources.
   *
   * @param sources the sources used for binding
   * @param placeholdersResolver strategy to resolve any property placeholders
   */
  public Binder(Iterable<ConfigurationPropertySource> sources, @Nullable PlaceholdersResolver placeholdersResolver) {
    this(sources, placeholdersResolver, null, null);
  }

  /**
   * Create a new {@link Binder} instance for the specified sources.
   *
   * @param sources the sources used for binding
   * @param placeholdersResolver strategy to resolve any property placeholders
   * @param conversionService the conversion service to convert values (or {@code null}
   * to use {@link ApplicationConversionService})
   */
  public Binder(Iterable<ConfigurationPropertySource> sources, @Nullable PlaceholdersResolver placeholdersResolver,
          @Nullable ConversionService conversionService) {
    this(sources, placeholdersResolver, conversionService, null);
  }

  /**
   * Create a new {@link Binder} instance for the specified sources.
   *
   * @param sources the sources used for binding
   * @param placeholdersResolver strategy to resolve any property placeholders
   * @param conversionService the conversion service to convert values (or {@code null}
   * to use {@link ApplicationConversionService})
   * @param propertyEditorInitializer initializer used to configure the property editors
   * that can convert values (or {@code null} if no initialization is required). Often
   * used to call {@link ConfigurableBeanFactory#copyRegisteredEditorsTo}.
   */
  public Binder(Iterable<ConfigurationPropertySource> sources, @Nullable PlaceholdersResolver placeholdersResolver,
          @Nullable ConversionService conversionService, @Nullable Consumer<PropertyEditorRegistry> propertyEditorInitializer) {
    this(sources, placeholdersResolver, conversionService, propertyEditorInitializer, null);
  }

  /**
   * Create a new {@link Binder} instance for the specified sources.
   *
   * @param sources the sources used for binding
   * @param placeholdersResolver strategy to resolve any property placeholders
   * @param conversionService the conversion service to convert values (or {@code null}
   * to use {@link ApplicationConversionService})
   * @param propertyEditorInitializer initializer used to configure the property editors
   * that can convert values (or {@code null} if no initialization is required). Often
   * used to call {@link ConfigurableBeanFactory#copyRegisteredEditorsTo}.
   * @param defaultBindHandler the default bind handler to use if none is specified when
   * binding
   */
  public Binder(Iterable<ConfigurationPropertySource> sources, @Nullable PlaceholdersResolver placeholdersResolver,
          @Nullable ConversionService conversionService, @Nullable Consumer<PropertyEditorRegistry> propertyEditorInitializer,
          @Nullable BindHandler defaultBindHandler) {
    this(sources, placeholdersResolver, conversionService,
            propertyEditorInitializer, defaultBindHandler, null);
  }

  /**
   * Create a new {@link Binder} instance for the specified sources.
   *
   * @param sources the sources used for binding
   * @param placeholdersResolver strategy to resolve any property placeholders
   * @param conversionService the conversion service to convert values (or {@code null}
   * to use {@link ApplicationConversionService})
   * @param propertyEditorInitializer initializer used to configure the property editors
   * that can convert values (or {@code null} if no initialization is required). Often
   * used to call {@link ConfigurableBeanFactory#copyRegisteredEditorsTo}.
   * @param defaultBindHandler the default bind handler to use if none is specified when
   * binding
   * @param constructorProvider the constructor provider which provides the bind
   * constructor to use when binding
   */
  public Binder(Iterable<ConfigurationPropertySource> sources, @Nullable PlaceholdersResolver placeholdersResolver,
          @Nullable ConversionService conversionService, @Nullable Consumer<PropertyEditorRegistry> propertyEditorInitializer,
          @Nullable BindHandler defaultBindHandler, @Nullable BindConstructorProvider constructorProvider) {
    this(sources, placeholdersResolver, conversionService != null ? Collections.singletonList(conversionService) : null,
            propertyEditorInitializer, defaultBindHandler, constructorProvider);
  }

  /**
   * Create a new {@link Binder} instance for the specified sources.
   *
   * @param sources the sources used for binding
   * @param placeholdersResolver strategy to resolve any property placeholders
   * @param conversionServices the conversion services to convert values (or
   * {@code null} to use {@link ApplicationConversionService})
   * @param propertyEditorInitializer initializer used to configure the property editors
   * that can convert values (or {@code null} if no initialization is required). Often
   * used to call {@link ConfigurableBeanFactory#copyRegisteredEditorsTo}.
   * @param defaultBindHandler the default bind handler to use if none is specified when
   * binding
   * @param constructorProvider the constructor provider which provides the bind
   * constructor to use when binding
   */
  public Binder(Iterable<ConfigurationPropertySource> sources, @Nullable PlaceholdersResolver placeholdersResolver,
          @Nullable List<ConversionService> conversionServices, @Nullable Consumer<PropertyEditorRegistry> propertyEditorInitializer,
          @Nullable BindHandler defaultBindHandler, @Nullable BindConstructorProvider constructorProvider) {
    Assert.notNull(sources, "Sources is required");
    for (ConfigurationPropertySource source : sources) {
      Assert.notNull(source, "Sources must not contain null elements");
    }
    this.sources = sources;
    this.bindConverter = BindConverter.get(conversionServices, propertyEditorInitializer);
    this.defaultBindHandler = defaultBindHandler != null ? defaultBindHandler : BindHandler.DEFAULT;
    this.placeholdersResolver = placeholdersResolver != null ? placeholdersResolver : PlaceholdersResolver.NONE;

    if (constructorProvider == null) {
      constructorProvider = BindConstructorProvider.DEFAULT;
    }
    ValueObjectBinder valueObjectBinder = new ValueObjectBinder(constructorProvider);
    JavaBeanBinder javaBeanBinder = JavaBeanBinder.INSTANCE;
    HashMap<BindMethod, List<DataObjectBinder>> dataObjectBinders = new HashMap<>();
    dataObjectBinders.put(BindMethod.VALUE_OBJECT, List.of(valueObjectBinder));
    dataObjectBinders.put(BindMethod.JAVA_BEAN, List.of(javaBeanBinder));
    dataObjectBinders.put(null, List.of(valueObjectBinder, javaBeanBinder));
    this.dataObjectBinders = Collections.unmodifiableMap(dataObjectBinders);
  }

  /**
   * Bind the specified target {@link Class} using this binder's
   * {@link ConfigurationPropertySource property sources}.
   *
   * @param name the configuration property name to bind
   * @param target the target class
   * @param <T> the bound type
   * @return the binding result (never {@code null})
   * @see #bind(ConfigurationPropertyName, Bindable, BindHandler)
   */
  public <T> BindResult<T> bind(String name, Class<T> target) {
    return bind(name, Bindable.of(target));
  }

  /**
   * Bind the specified target {@link Bindable} using this binder's
   * {@link ConfigurationPropertySource property sources}.
   *
   * @param name the configuration property name to bind
   * @param target the target bindable
   * @param <T> the bound type
   * @return the binding result (never {@code null})
   * @see #bind(ConfigurationPropertyName, Bindable, BindHandler)
   */
  public <T> BindResult<T> bind(String name, Bindable<T> target) {
    return bind(ConfigurationPropertyName.of(name), target, null);
  }

  /**
   * Bind the specified target {@link Bindable} using this binder's
   * {@link ConfigurationPropertySource property sources}.
   *
   * @param name the configuration property name to bind
   * @param target the target bindable
   * @param <T> the bound type
   * @return the binding result (never {@code null})
   * @see #bind(ConfigurationPropertyName, Bindable, BindHandler)
   */
  public <T> BindResult<T> bind(ConfigurationPropertyName name, Bindable<T> target) {
    return bind(name, target, null);
  }

  /**
   * Bind the specified target {@link Bindable} using this binder's
   * {@link ConfigurationPropertySource property sources}.
   *
   * @param name the configuration property name to bind
   * @param target the target bindable
   * @param handler the bind handler (may be {@code null})
   * @param <T> the bound type
   * @return the binding result (never {@code null})
   */
  public <T> BindResult<T> bind(String name, Bindable<T> target, @Nullable BindHandler handler) {
    return bind(ConfigurationPropertyName.of(name), target, handler);
  }

  /**
   * Bind the specified target {@link Bindable} using this binder's
   * {@link ConfigurationPropertySource property sources}.
   *
   * @param name the configuration property name to bind
   * @param target the target bindable
   * @param handler the bind handler (may be {@code null})
   * @param <T> the bound type
   * @return the binding result (never {@code null})
   */
  public <T> BindResult<T> bind(ConfigurationPropertyName name, Bindable<T> target, @Nullable BindHandler handler) {
    T bound = bind(name, target, handler, false);
    return BindResult.of(bound);
  }

  /**
   * Bind the specified target {@link Class} using this binder's
   * {@link ConfigurationPropertySource property sources} or create a new instance using
   * the type of the {@link Bindable} if the result of the binding is {@code null}.
   *
   * @param name the configuration property name to bind
   * @param target the target class
   * @param <T> the bound type
   * @return the bound or created object
   * @see #bind(ConfigurationPropertyName, Bindable, BindHandler)
   */
  public <T> T bindOrCreate(String name, Class<T> target) {
    return bindOrCreate(name, Bindable.of(target));
  }

  /**
   * Bind the specified target {@link Bindable} using this binder's
   * {@link ConfigurationPropertySource property sources} or create a new instance using
   * the type of the {@link Bindable} if the result of the binding is {@code null}.
   *
   * @param name the configuration property name to bind
   * @param target the target bindable
   * @param <T> the bound type
   * @return the bound or created object
   * @see #bindOrCreate(ConfigurationPropertyName, Bindable, BindHandler)
   */
  public <T> T bindOrCreate(String name, Bindable<T> target) {
    return bindOrCreate(ConfigurationPropertyName.of(name), target, null);
  }

  /**
   * Bind the specified target {@link Bindable} using this binder's
   * {@link ConfigurationPropertySource property sources} or create a new instance using
   * the type of the {@link Bindable} if the result of the binding is {@code null}.
   *
   * @param name the configuration property name to bind
   * @param target the target bindable
   * @param handler the bind handler
   * @param <T> the bound type
   * @return the bound or created object
   * @see #bindOrCreate(ConfigurationPropertyName, Bindable, BindHandler)
   */
  public <T> T bindOrCreate(String name, Bindable<T> target, BindHandler handler) {
    return bindOrCreate(ConfigurationPropertyName.of(name), target, handler);
  }

  /**
   * Bind the specified target {@link Bindable} using this binder's
   * {@link ConfigurationPropertySource property sources} or create a new instance using
   * the type of the {@link Bindable} if the result of the binding is {@code null}.
   *
   * @param name the configuration property name to bind
   * @param target the target bindable
   * @param handler the bind handler (may be {@code null})
   * @param <T> the bound or created type
   * @return the bound or created object
   */
  public <T> T bindOrCreate(ConfigurationPropertyName name, Bindable<T> target, @Nullable BindHandler handler) {
    return bind(name, target, handler, true);
  }

  private <T> T bind(ConfigurationPropertyName name, Bindable<T> target, @Nullable BindHandler handler, boolean create) {
    Assert.notNull(name, "Name is required");
    Assert.notNull(target, "Target is required");
    if (handler == null) {
      handler = defaultBindHandler;
    }
    Context context = new Context();
    return bind(name, target, handler, context, false, create);
  }

  private <T> T bind(ConfigurationPropertyName name, Bindable<T> target,
          BindHandler handler, Context context, boolean allowRecursiveBinding, boolean create) {
    try {
      Bindable<T> replacementTarget = handler.onStart(name, target, context);
      if (replacementTarget == null) {
        return handleBindResult(name, target, handler, context, null, create);
      }
      target = replacementTarget;
      Object bound = bindObject(name, target, handler, context, allowRecursiveBinding);
      return handleBindResult(name, target, handler, context, bound, create);
    }
    catch (Exception ex) {
      return handleBindError(name, target, handler, context, ex);
    }
  }

  private <T> T handleBindResult(ConfigurationPropertyName name, Bindable<T> target,
          BindHandler handler, Context context, @Nullable Object result, boolean create) throws Exception {
    if (result != null) {
      result = handler.onSuccess(name, target, context, result);
      result = context.getConverter().convert(result, target);
    }
    if (result == null && create) {
      result = create(target, context);
      result = handler.onCreate(name, target, context, result);
      result = context.getConverter().convert(result, target);
      if (result == null) {
        throw new IllegalStateException("Unable to create instance for " + target.getType());
      }
    }
    handler.onFinish(name, target, context, result);
    return context.getConverter().convert(result, target);
  }

  @Nullable
  private Object create(Bindable<?> target, Context context) {
    for (DataObjectBinder dataObjectBinder : dataObjectBinders.get(target.getBindMethod())) {
      Object instance = dataObjectBinder.create(target, context);
      if (instance != null) {
        return instance;
      }
    }
    return null;
  }

  private <T> T handleBindError(ConfigurationPropertyName name, Bindable<T> target,
          BindHandler handler, Context context, Exception error) {
    try {
      Object result = handler.onFailure(name, target, context, error);
      return context.getConverter().convert(result, target);
    }
    catch (Exception ex) {
      if (ex instanceof BindException) {
        throw (BindException) ex;
      }
      throw new BindException(name, target, context.getConfigurationProperty(), ex);
    }
  }

  @Nullable
  private <T> Object bindObject(ConfigurationPropertyName name, Bindable<T> target, BindHandler handler,
          Context context, boolean allowRecursiveBinding) {
    ConfigurationProperty property = findProperty(name, target, context);
    if (property == null && context.depth != 0 && containsNoDescendantOf(context.getSources(), name)) {
      return null;
    }
    AggregateBinder<?> aggregateBinder = getAggregateBinder(target, context);
    if (aggregateBinder != null) {
      return bindAggregate(name, target, handler, context, aggregateBinder);
    }
    if (property != null) {
      try {
        return bindProperty(target, context, property);
      }
      catch (ConverterNotFoundException ex) {
        // We might still be able to bind it using the recursive binders
        Object instance = bindDataObject(name, target, handler, context, allowRecursiveBinding);
        if (instance != null) {
          return instance;
        }
        throw ex;
      }
    }
    return bindDataObject(name, target, handler, context, allowRecursiveBinding);
  }

  @Nullable
  private AggregateBinder<?> getAggregateBinder(Bindable<?> target, Context context) {
    Class<?> resolvedType = target.getType().resolve(Object.class);
    if (Map.class.isAssignableFrom(resolvedType)) {
      return new MapBinder(context);
    }
    if (Collection.class.isAssignableFrom(resolvedType)) {
      return new CollectionBinder(context);
    }
    if (target.getType().isArray()) {
      return new ArrayBinder(context);
    }
    return null;
  }

  private <T> Object bindAggregate(ConfigurationPropertyName name, Bindable<T> target,
          BindHandler handler, Context context, AggregateBinder<?> aggregateBinder) {

    AggregateElementBinder elementBinder = (itemName, itemTarget, source) -> {
      boolean allowRecursiveBinding = aggregateBinder.isAllowRecursiveBinding(source);
      Supplier<?> supplier = () -> bind(itemName, itemTarget, handler, context, allowRecursiveBinding, false);
      return context.withSource(source, supplier);
    };
    return context.withIncreasedDepth(() -> aggregateBinder.bind(name, target, elementBinder));
  }

  @Nullable
  private <T> ConfigurationProperty findProperty(
          ConfigurationPropertyName name, Bindable<T> target, Context context) {
    if (name.isEmpty() || target.hasBindRestriction(BindRestriction.NO_DIRECT_PROPERTY)) {
      return null;
    }
    for (ConfigurationPropertySource source : context.getSources()) {
      ConfigurationProperty property = source.getConfigurationProperty(name);
      if (property != null) {
        return property;
      }
    }
    return null;
  }

  @Nullable
  private <T> Object bindProperty(Bindable<T> target, Context context, ConfigurationProperty property) {
    context.setConfigurationProperty(property);
    Object result = property.getValue();
    result = this.placeholdersResolver.resolvePlaceholders(result);
    return context.getConverter().convert(result, target);
  }

  @Nullable
  private Object bindDataObject(ConfigurationPropertyName name, Bindable<?> target,
          BindHandler handler, Context context, boolean allowRecursiveBinding) {
    if (isUnbindableBean(name, target, context)) {
      return null;
    }
    Class<?> type = target.getType().resolve(Object.class);
    BindMethod bindMethod = target.getBindMethod();
    if (!allowRecursiveBinding && context.isBindingDataObject(type)) {
      return null;
    }
    DataObjectPropertyBinder propertyBinder = (propertyName, propertyTarget)
            -> bind(name.append(propertyName), propertyTarget, handler, context, false, false);
    return context.withDataObject(type, () -> {
      for (DataObjectBinder dataObjectBinder : dataObjectBinders.get(bindMethod)) {
        Object instance = dataObjectBinder.bind(name, target, context, propertyBinder);
        if (instance != null) {
          return instance;
        }
      }
      return null;
    });
  }

  private boolean isUnbindableBean(ConfigurationPropertyName name, Bindable<?> target, Context context) {
    for (ConfigurationPropertySource source : context.getSources()) {
      if (source.containsDescendantOf(name) == ConfigurationPropertyState.PRESENT) {
        // We know there are properties to bind so we can't bypass anything
        return false;
      }
    }
    Class<?> resolved = target.getType().resolve(Object.class);
    if (resolved.isPrimitive() || NON_BEAN_CLASSES.contains(resolved)) {
      return true;
    }
    return resolved.getName().startsWith("java.");
  }

  private boolean containsNoDescendantOf(
          Iterable<ConfigurationPropertySource> sources, ConfigurationPropertyName name) {
    for (ConfigurationPropertySource source : sources) {
      if (source.containsDescendantOf(name) != ConfigurationPropertyState.ABSENT) {
        return false;
      }
    }
    return true;
  }

  /**
   * Create a new {@link Binder} instance from the specified environment.
   *
   * @param environment the environment source (must have attached
   * {@link ConfigurationPropertySources})
   * @return a {@link Binder} instance
   */
  public static Binder get(ConfigurableEnvironment environment) {
    return get(environment, null);
  }

  /**
   * Create a new {@link Binder} instance from the specified environment.
   *
   * @param environment the environment source (must have attached
   * {@link ConfigurationPropertySources})
   * @param defaultBindHandler the default bind handler to use if none is specified when
   * binding
   * @return a {@link Binder} instance
   */
  public static Binder get(ConfigurableEnvironment environment, @Nullable BindHandler defaultBindHandler) {
    Iterable<ConfigurationPropertySource> sources = ConfigurationPropertySources.get(environment);
    PropertySourcesPlaceholdersResolver placeholdersResolver = new PropertySourcesPlaceholdersResolver(environment);
    return new Binder(sources, placeholdersResolver, null, null, defaultBindHandler);
  }

  /**
   * Context used when binding and the {@link BindContext} implementation.
   */
  final class Context implements BindContext {

    private int depth;

    private final List<ConfigurationPropertySource> source = Arrays.asList((ConfigurationPropertySource) null);

    private int sourcePushCount;

    private final ArrayDeque<Class<?>> dataObjectBindings = new ArrayDeque<>();
    private final ArrayDeque<Class<?>> constructorBindings = new ArrayDeque<>();

    @Nullable
    private ConfigurationProperty configurationProperty;

    private void increaseDepth() {
      this.depth++;
    }

    private void decreaseDepth() {
      this.depth--;
    }

    private <T> T withSource(@Nullable ConfigurationPropertySource source, Supplier<T> supplier) {
      if (source == null) {
        return supplier.get();
      }
      this.source.set(0, source);
      this.sourcePushCount++;
      try {
        return supplier.get();
      }
      finally {
        this.sourcePushCount--;
      }
    }

    private <T> T withDataObject(Class<?> type, Supplier<T> supplier) {
      this.dataObjectBindings.push(type);
      try {
        return withIncreasedDepth(supplier);
      }
      finally {
        this.dataObjectBindings.pop();
      }
    }

    private boolean isBindingDataObject(Class<?> type) {
      return this.dataObjectBindings.contains(type);
    }

    private <T> T withIncreasedDepth(Supplier<T> supplier) {
      increaseDepth();
      try {
        return supplier.get();
      }
      finally {
        decreaseDepth();
      }
    }

    void setConfigurationProperty(ConfigurationProperty configurationProperty) {
      this.configurationProperty = configurationProperty;
    }

    void clearConfigurationProperty() {
      this.configurationProperty = null;
    }

    void pushConstructorBoundTypes(Class<?> value) {
      this.constructorBindings.push(value);
    }

    boolean isNestedConstructorBinding() {
      return !this.constructorBindings.isEmpty();
    }

    void popConstructorBoundTypes() {
      this.constructorBindings.pop();
    }

    PlaceholdersResolver getPlaceholdersResolver() {
      return Binder.this.placeholdersResolver;
    }

    BindConverter getConverter() {
      return Binder.this.bindConverter;
    }

    @Override
    public Binder getBinder() {
      return Binder.this;
    }

    @Override
    public int getDepth() {
      return this.depth;
    }

    @Override
    public Iterable<ConfigurationPropertySource> getSources() {
      if (this.sourcePushCount > 0) {
        return this.source;
      }
      return Binder.this.sources;
    }

    @Override
    @Nullable
    public ConfigurationProperty getConfigurationProperty() {
      return this.configurationProperty;
    }

  }

}
