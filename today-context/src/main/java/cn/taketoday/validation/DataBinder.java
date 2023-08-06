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

package cn.taketoday.validation;

import java.beans.PropertyEditor;
import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;

import cn.taketoday.beans.BeanInstantiationException;
import cn.taketoday.beans.BeanUtils;
import cn.taketoday.beans.ConfigurablePropertyAccessor;
import cn.taketoday.beans.PropertyAccessException;
import cn.taketoday.beans.PropertyAccessorUtils;
import cn.taketoday.beans.PropertyBatchUpdateException;
import cn.taketoday.beans.PropertyEditorRegistrar;
import cn.taketoday.beans.PropertyEditorRegistry;
import cn.taketoday.beans.PropertyValue;
import cn.taketoday.beans.PropertyValues;
import cn.taketoday.beans.SimpleTypeConverter;
import cn.taketoday.beans.TypeConverter;
import cn.taketoday.beans.TypeMismatchException;
import cn.taketoday.core.MethodParameter;
import cn.taketoday.core.ResolvableType;
import cn.taketoday.core.TypeDescriptor;
import cn.taketoday.core.conversion.ConversionService;
import cn.taketoday.format.Formatter;
import cn.taketoday.format.support.FormatterPropertyEditorAdapter;
import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;
import cn.taketoday.logging.Logger;
import cn.taketoday.logging.LoggerFactory;
import cn.taketoday.util.ObjectUtils;
import cn.taketoday.util.StringUtils;
import cn.taketoday.validation.annotation.ValidationAnnotationUtils;

/**
 * Binder that allows for setting property values on a target object, including
 * support for validation and binding result analysis.
 *
 * <p>The binding process can be customized by specifying allowed field patterns,
 * required fields, custom editors, etc.
 *
 * <p><strong>WARNING</strong>: Data binding can lead to security issues by exposing
 * parts of the object graph that are not meant to be accessed or modified by
 * external clients. Therefore the design and use of data binding should be considered
 * carefully with regard to security. For more details, please refer to the dedicated
 * sections on data binding for
 * <a href="https://docs.spring.io/spring-framework/docs/current/reference/html/web.html#mvc-ann-initbinder-model-design">Infra Web MVC</a> and
 * <a href="https://docs.spring.io/spring-framework/docs/current/reference/html/web-reactive.html#webflux-ann-initbinder-model-design">Infra WebFlux</a>
 * in the reference manual.
 *
 * <p>The binding results can be examined via the {@link BindingResult} interface,
 * extending the {@link Errors} interface: see the {@link #getBindingResult()} method.
 * Missing fields and property access exceptions will be converted to {@link FieldError FieldErrors},
 * collected in the Errors instance, using the following error codes:
 *
 * <ul>
 * <li>Missing field error: "required"
 * <li>Type mismatch error: "typeMismatch"
 * <li>Method invocation error: "methodInvocation"
 * </ul>
 *
 * <p>By default, binding errors get resolved through the {@link BindingErrorProcessor}
 * strategy, processing for missing fields and property access exceptions: see the
 * {@link #setBindingErrorProcessor} method. You can override the default strategy
 * if needed, for example to generate different error codes.
 *
 * <p>Custom validation errors can be added afterwards. You will typically want to resolve
 * such error codes into proper user-visible error messages; this can be achieved through
 * resolving each error via a {@link cn.taketoday.context.MessageSource}, which is
 * able to resolve an {@link ObjectError}/{@link FieldError} through its
 * {@link cn.taketoday.context.MessageSource#getMessage(cn.taketoday.context.MessageSourceResolvable, java.util.Locale)}
 * method. The list of message codes can be customized through the {@link MessageCodesResolver}
 * strategy: see the {@link #setMessageCodesResolver} method. {@link DefaultMessageCodesResolver}'s
 * javadoc states details on the default resolution rules.
 *
 * <p>This generic data binder can be used in any kind of environment.
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @author Rob Harrop
 * @author Stephane Nicoll
 * @author Kazuki Shimizu
 * @author Sam Brannen
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see #setAllowedFields
 * @see #setRequiredFields
 * @see #registerCustomEditor
 * @see #setMessageCodesResolver
 * @see #setBindingErrorProcessor
 * @see #bind
 * @see #getBindingResult
 * @see DefaultMessageCodesResolver
 * @see DefaultBindingErrorProcessor
 * @see cn.taketoday.context.MessageSource
 */
public class DataBinder implements PropertyEditorRegistry, TypeConverter {

  /** Default object name used for binding: "target". */
  public static final String DEFAULT_OBJECT_NAME = "target";

  /** Default limit for array and collection growing: 256. */
  public static final int DEFAULT_AUTO_GROW_COLLECTION_LIMIT = 256;

  /**
   * We'll create a lot of DataBinder instances: Let's use a static logger.
   */
  protected static final Logger logger = LoggerFactory.getLogger(DataBinder.class);

  @Nullable
  private Object target;

  @Nullable
  ResolvableType targetType;

  private final String objectName;

  @Nullable
  private AbstractPropertyBindingResult bindingResult;

  private boolean directFieldAccess = false;

  @Nullable
  private ExtendedTypeConverter typeConverter;

  private boolean declarativeBinding = false;

  private boolean ignoreUnknownFields = true;

  private boolean ignoreInvalidFields = false;

  private boolean autoGrowNestedPaths = true;

  private int autoGrowCollectionLimit = DEFAULT_AUTO_GROW_COLLECTION_LIMIT;

  @Nullable
  private String[] allowedFields;

  @Nullable
  private String[] disallowedFields;

  @Nullable
  private String[] requiredFields;

  @Nullable
  private NameResolver nameResolver;

  @Nullable
  private ConversionService conversionService;

  @Nullable
  private MessageCodesResolver messageCodesResolver;

  private BindingErrorProcessor bindingErrorProcessor = new DefaultBindingErrorProcessor();

  private final ArrayList<Validator> validators = new ArrayList<>();

  @Nullable
  private Predicate<Validator> excludedValidators;

  /**
   * Create a new DataBinder instance, with default object name.
   *
   * @param target the target object to bind onto (or {@code null}
   * if the binder is just used to convert a plain parameter value)
   * @see #DEFAULT_OBJECT_NAME
   */
  public DataBinder(@Nullable Object target) {
    this(target, DEFAULT_OBJECT_NAME);
  }

  /**
   * Create a new DataBinder instance.
   *
   * @param target the target object to bind onto (or {@code null}
   * if the binder is just used to convert a plain parameter value)
   * @param objectName the name of the target object
   */
  public DataBinder(@Nullable Object target, String objectName) {
    this.target = ObjectUtils.unwrapOptional(target);
    this.objectName = objectName;
  }

  /**
   * Return the wrapped target object.
   * <p>If the target object is {@code null} and {@link #getTargetType()} is set,
   * then {@link #construct(ValueResolver)} may be called to create the target.
   */
  @Nullable
  public Object getTarget() {
    return this.target;
  }

  /**
   * Return the name of the bound object.
   */
  public String getObjectName() {
    return this.objectName;
  }

  /**
   * Set the type for the target object. When the target is {@code null},
   * setting the targetType allows using {@link #construct(ValueResolver)} to
   * create the target.
   *
   * @param targetType the type of the target object
   */
  public void setTargetType(ResolvableType targetType) {
    Assert.state(this.target == null, "targetType is used to for target creation, but target is already set");
    this.targetType = targetType;
  }

  /**
   * Return the {@link #setTargetType configured} type for the target object.
   */
  @Nullable
  public ResolvableType getTargetType() {
    return this.targetType;
  }

  /**
   * Set whether this binder should attempt to "auto-grow" a nested path that contains a null value.
   * <p>If "true", a null path location will be populated with a default object value and traversed
   * instead of resulting in an exception. This flag also enables auto-growth of collection elements
   * when accessing an out-of-bounds index.
   * <p>Default is "true" on a standard DataBinder. Note that since 4.0 this feature is supported
   * for bean property access (DataBinder's default mode) and field access.
   * <p>Used for setter/field injection via {@link #bind(PropertyValues)}, and not
   * applicable to constructor initialization via {@link #construct(ValueResolver)}.
   *
   * @see #initBeanPropertyAccess()
   * @see cn.taketoday.beans.BeanWrapper#setAutoGrowNestedPaths
   */
  public void setAutoGrowNestedPaths(boolean autoGrowNestedPaths) {
    Assert.state(this.bindingResult == null,
            "DataBinder is already initialized - call setAutoGrowNestedPaths before other configuration methods");
    this.autoGrowNestedPaths = autoGrowNestedPaths;
  }

  /**
   * Return whether "auto-growing" of nested paths has been activated.
   */
  public boolean isAutoGrowNestedPaths() {
    return this.autoGrowNestedPaths;
  }

  /**
   * Specify the limit for array and collection auto-growing.
   * <p>Default is 256, preventing OutOfMemoryErrors in case of large indexes.
   * Raise this limit if your auto-growing needs are unusually high.
   * <p>Used for setter/field injection via {@link #bind(PropertyValues)}, and not
   * applicable to constructor initialization via {@link #construct(ValueResolver)}.
   *
   * @see #initBeanPropertyAccess()
   * @see cn.taketoday.beans.BeanWrapper#setAutoGrowCollectionLimit
   */
  public void setAutoGrowCollectionLimit(int autoGrowCollectionLimit) {
    Assert.state(this.bindingResult == null,
            "DataBinder is already initialized - call setAutoGrowCollectionLimit before other configuration methods");
    this.autoGrowCollectionLimit = autoGrowCollectionLimit;
  }

  /**
   * Return the current limit for array and collection auto-growing.
   */
  public int getAutoGrowCollectionLimit() {
    return this.autoGrowCollectionLimit;
  }

  /**
   * Initialize standard JavaBean property access for this DataBinder.
   * <p>This is the default; an explicit call just leads to eager initialization.
   *
   * @see #initDirectFieldAccess()
   * @see #createBeanPropertyBindingResult()
   */
  public void initBeanPropertyAccess() {
    Assert.state(this.bindingResult == null,
            "DataBinder is already initialized - call initBeanPropertyAccess before other configuration methods");
    this.directFieldAccess = false;
  }

  /**
   * Create the {@link AbstractPropertyBindingResult} instance using standard
   * JavaBean property access.
   */
  protected AbstractPropertyBindingResult createBeanPropertyBindingResult() {
    BeanPropertyBindingResult result = new BeanPropertyBindingResult(getTarget(),
            getObjectName(), isAutoGrowNestedPaths(), getAutoGrowCollectionLimit());

    if (this.conversionService != null) {
      result.initConversion(this.conversionService);
    }
    if (this.messageCodesResolver != null) {
      result.setMessageCodesResolver(this.messageCodesResolver);
    }

    return result;
  }

  /**
   * Initialize direct field access for this DataBinder,
   * as alternative to the default bean property access.
   *
   * @see #initBeanPropertyAccess()
   * @see #createDirectFieldBindingResult()
   */
  public void initDirectFieldAccess() {
    Assert.state(this.bindingResult == null,
            "DataBinder is already initialized - call initDirectFieldAccess before other configuration methods");
    this.directFieldAccess = true;
  }

  /**
   * Create the {@link AbstractPropertyBindingResult} instance using direct
   * field access.
   */
  protected AbstractPropertyBindingResult createDirectFieldBindingResult() {
    DirectFieldBindingResult result = new DirectFieldBindingResult(getTarget(),
            getObjectName(), isAutoGrowNestedPaths());

    if (this.conversionService != null) {
      result.initConversion(this.conversionService);
    }
    if (this.messageCodesResolver != null) {
      result.setMessageCodesResolver(this.messageCodesResolver);
    }

    return result;
  }

  /**
   * Return the internal BindingResult held by this DataBinder,
   * as an AbstractPropertyBindingResult.
   */
  protected AbstractPropertyBindingResult getInternalBindingResult() {
    AbstractPropertyBindingResult bindingResult = this.bindingResult;
    if (bindingResult == null) {
      bindingResult = directFieldAccess ? createDirectFieldBindingResult() : createBeanPropertyBindingResult();
      this.bindingResult = bindingResult;
    }
    return this.bindingResult;
  }

  /**
   * Return the underlying PropertyAccessor of this binder's BindingResult.
   */
  protected ConfigurablePropertyAccessor getPropertyAccessor() {
    return getInternalBindingResult().getPropertyAccessor();
  }

  /**
   * Return this binder's underlying SimpleTypeConverter.
   */
  protected SimpleTypeConverter getSimpleTypeConverter() {
    if (this.typeConverter == null) {
      this.typeConverter = new ExtendedTypeConverter();
      if (this.conversionService != null) {
        this.typeConverter.setConversionService(this.conversionService);
      }
    }
    return this.typeConverter;
  }

  /**
   * Return the underlying TypeConverter of this binder's BindingResult.
   */
  protected PropertyEditorRegistry getPropertyEditorRegistry() {
    if (getTarget() != null) {
      return getInternalBindingResult().getPropertyAccessor();
    }
    else {
      return getSimpleTypeConverter();
    }
  }

  /**
   * Return the underlying TypeConverter of this binder's BindingResult.
   */
  protected TypeConverter getTypeConverter() {
    if (getTarget() != null) {
      return getInternalBindingResult().getPropertyAccessor();
    }
    else {
      return getSimpleTypeConverter();
    }
  }

  /**
   * Return the BindingResult instance created by this DataBinder.
   * This allows for convenient access to the binding results after
   * a bind operation.
   *
   * @return the BindingResult instance, to be treated as BindingResult
   * or as Errors instance (Errors is a super-interface of BindingResult)
   * @see Errors
   * @see #bind
   */
  public BindingResult getBindingResult() {
    return getInternalBindingResult();
  }

  /**
   * Set whether to bind only fields explicitly intended for binding including:
   * <ul>
   * <li>Constructor binding via {@link #construct}.
   * <li>Property binding with configured
   * {@link #setAllowedFields(String...) allowedFields}.
   * </ul>
   * <p>Default is "false". Turn this on to limit binding to constructor
   * parameters and allowed fields.
   */
  public void setDeclarativeBinding(boolean declarativeBinding) {
    this.declarativeBinding = declarativeBinding;
  }

  /**
   * Return whether to bind only fields intended for binding.
   */
  public boolean isDeclarativeBinding() {
    return this.declarativeBinding;
  }

  /**
   * Set whether to ignore unknown fields, that is, whether to ignore bind
   * parameters that do not have corresponding fields in the target object.
   * <p>Default is "true". Turn this off to enforce that all bind parameters
   * must have a matching field in the target object.
   * <p>Note that this setting only applies to <i>binding</i> operations
   * on this DataBinder, not to <i>retrieving</i> values via its
   * {@link #getBindingResult() BindingResult}.
   * <p>Used for setter/field inject via {@link #bind(PropertyValues)}, and not
   * applicable to constructor initialization via {@link #construct(ValueResolver)},
   * which uses only the values it needs.
   *
   * @see #bind
   */
  public void setIgnoreUnknownFields(boolean ignoreUnknownFields) {
    this.ignoreUnknownFields = ignoreUnknownFields;
  }

  /**
   * Return whether to ignore unknown fields when binding.
   */
  public boolean isIgnoreUnknownFields() {
    return this.ignoreUnknownFields;
  }

  /**
   * Set whether to ignore invalid fields, that is, whether to ignore bind
   * parameters that have corresponding fields in the target object which are
   * not accessible (for example because of null values in the nested path).
   * <p>Default is "false". Turn this on to ignore bind parameters for
   * nested objects in non-existing parts of the target object graph.
   * <p>Note that this setting only applies to <i>binding</i> operations
   * on this DataBinder, not to <i>retrieving</i> values via its
   * {@link #getBindingResult() BindingResult}.
   * <p>Used for setter/field inject via {@link #bind(PropertyValues)}, and not
   * applicable to constructor initialization via {@link #construct(ValueResolver)},
   * which uses only the values it needs.
   *
   * @see #bind
   */
  public void setIgnoreInvalidFields(boolean ignoreInvalidFields) {
    this.ignoreInvalidFields = ignoreInvalidFields;
  }

  /**
   * Return whether to ignore invalid fields when binding.
   */
  public boolean isIgnoreInvalidFields() {
    return this.ignoreInvalidFields;
  }

  /**
   * Register field patterns that should be allowed for binding.
   * <p>Default is all fields.
   * <p>Restrict this for example to avoid unwanted modifications by malicious
   * users when binding HTTP request parameters.
   * <p>Supports {@code "xxx*"}, {@code "*xxx"}, {@code "*xxx*"}, and
   * {@code "xxx*yyy"} matches (with an arbitrary number of pattern parts), as
   * well as direct equality.
   * <p>The default implementation of this method stores allowed field patterns
   * in {@linkplain PropertyAccessorUtils#canonicalPropertyName(String) canonical}
   * form. Subclasses which override this method must therefore take this into
   * account.
   * <p>More sophisticated matching can be implemented by overriding the
   * {@link #isAllowed} method.
   * <p>Alternatively, specify a list of <i>disallowed</i> field patterns.
   * <p>Used for setter/field inject via {@link #bind(PropertyValues)}, and not
   * applicable to constructor initialization via {@link #construct(ValueResolver)},
   * which uses only the values it needs.
   *
   * @param allowedFields array of allowed field patterns
   * @see #setDisallowedFields
   * @see #isAllowed(String)
   */
  public void setAllowedFields(@Nullable String... allowedFields) {
    this.allowedFields = PropertyAccessorUtils.canonicalPropertyNames(allowedFields);
  }

  /**
   * Return the field patterns that should be allowed for binding.
   *
   * @return array of allowed field patterns
   * @see #setAllowedFields(String...)
   */
  @Nullable
  public String[] getAllowedFields() {
    return this.allowedFields;
  }

  /**
   * Register field patterns that should <i>not</i> be allowed for binding.
   * <p>Default is none.
   * <p>Mark fields as disallowed, for example to avoid unwanted
   * modifications by malicious users when binding HTTP request parameters.
   * <p>Supports {@code "xxx*"}, {@code "*xxx"}, {@code "*xxx*"}, and
   * {@code "xxx*yyy"} matches (with an arbitrary number of pattern parts), as
   * well as direct equality.
   * <p>The default implementation of this method stores disallowed field patterns
   * in {@linkplain PropertyAccessorUtils#canonicalPropertyName(String) canonical}
   * form. the default implementation also transforms
   * disallowed field patterns to {@linkplain String#toLowerCase() lowercase} to
   * support case-insensitive pattern matching in {@link #isAllowed}. Subclasses
   * which override this method must therefore take both of these transformations
   * into account.
   * <p>More sophisticated matching can be implemented by overriding the
   * {@link #isAllowed} method.
   * <p>Alternatively, specify a list of <i>allowed</i> field patterns.
   * <p>Used for setter/field inject via {@link #bind(PropertyValues)}, and not
   * applicable to constructor initialization via {@link #construct(ValueResolver)},
   * which uses only the values it needs.
   *
   * @param disallowedFields array of disallowed field patterns
   * @see #setAllowedFields
   * @see #isAllowed(String)
   */
  public void setDisallowedFields(@Nullable String... disallowedFields) {
    if (disallowedFields == null) {
      this.disallowedFields = null;
    }
    else {
      String[] fieldPatterns = new String[disallowedFields.length];
      for (int i = 0; i < fieldPatterns.length; i++) {
        fieldPatterns[i] = PropertyAccessorUtils.canonicalPropertyName(disallowedFields[i]).toLowerCase();
      }
      this.disallowedFields = fieldPatterns;
    }
  }

  /**
   * Return the field patterns that should <i>not</i> be allowed for binding.
   *
   * @return array of disallowed field patterns
   * @see #setDisallowedFields(String...)
   */
  @Nullable
  public String[] getDisallowedFields() {
    return this.disallowedFields;
  }

  /**
   * Register fields that are required for each binding process.
   * <p>If one of the specified fields is not contained in the list of
   * incoming property values, a corresponding "missing field" error
   * will be created, with error code "required" (by the default
   * binding error processor).
   * <p>Used for setter/field inject via {@link #bind(PropertyValues)}, and not
   * applicable to constructor initialization via {@link #construct(ValueResolver)},
   * which uses only the values it needs.
   *
   * @param requiredFields array of field names
   * @see #setBindingErrorProcessor
   * @see DefaultBindingErrorProcessor#MISSING_FIELD_ERROR_CODE
   */
  public void setRequiredFields(@Nullable String... requiredFields) {
    this.requiredFields = PropertyAccessorUtils.canonicalPropertyNames(requiredFields);
    if (logger.isDebugEnabled()) {
      logger.debug("DataBinder requires binding of required fields [{}]",
              StringUtils.arrayToCommaDelimitedString(requiredFields));
    }
  }

  /**
   * Return the fields that are required for each binding process.
   *
   * @return array of field names
   */
  @Nullable
  public String[] getRequiredFields() {
    return this.requiredFields;
  }

  /**
   * Configure a resolver to determine the name of the value to bind to a
   * constructor parameter in {@link #construct}.
   * <p>If not configured, or if the name cannot be resolved, by default
   * {@link cn.taketoday.core.DefaultParameterNameDiscoverer} is used.
   *
   * @param nameResolver the resolver to use
   */
  public void setNameResolver(NameResolver nameResolver) {
    this.nameResolver = nameResolver;
  }

  /**
   * Return the {@link #setNameResolver configured} name resolver for
   * constructor parameters.
   */
  @Nullable
  public NameResolver getNameResolver() {
    return this.nameResolver;
  }

  /**
   * Set the strategy to use for resolving errors into message codes.
   * Applies the given strategy to the underlying errors holder.
   * <p>Default is a DefaultMessageCodesResolver.
   *
   * @see BeanPropertyBindingResult#setMessageCodesResolver
   * @see DefaultMessageCodesResolver
   */
  public void setMessageCodesResolver(@Nullable MessageCodesResolver messageCodesResolver) {
    Assert.state(this.messageCodesResolver == null, "DataBinder is already initialized with MessageCodesResolver");
    this.messageCodesResolver = messageCodesResolver;
    if (this.bindingResult != null && messageCodesResolver != null) {
      this.bindingResult.setMessageCodesResolver(messageCodesResolver);
    }
  }

  /**
   * Set the strategy to use for processing binding errors, that is,
   * required field errors and {@code PropertyAccessException}s.
   * <p>Default is a DefaultBindingErrorProcessor.
   *
   * @see DefaultBindingErrorProcessor
   */
  public void setBindingErrorProcessor(BindingErrorProcessor bindingErrorProcessor) {
    Assert.notNull(bindingErrorProcessor, "BindingErrorProcessor must not be null");
    this.bindingErrorProcessor = bindingErrorProcessor;
  }

  /**
   * Return the strategy for processing binding errors.
   */
  public BindingErrorProcessor getBindingErrorProcessor() {
    return this.bindingErrorProcessor;
  }

  /**
   * Set the Validator to apply after each binding step.
   *
   * @see #addValidators(Validator...)
   * @see #replaceValidators(Validator...)
   */
  public void setValidator(@Nullable Validator validator) {
    assertValidators(validator);
    this.validators.clear();
    if (validator != null) {
      this.validators.add(validator);
    }
  }

  private void assertValidators(Validator... validators) {
    Object target = getTarget();
    for (Validator validator : validators) {
      if (validator != null && (target != null && !validator.supports(target.getClass()))) {
        throw new IllegalStateException("Invalid target for Validator [" + validator + "]: " + target);
      }
    }
  }

  /**
   * Configure a predicate to exclude validators.
   */
  public void setExcludedValidators(Predicate<Validator> predicate) {
    this.excludedValidators = predicate;
  }

  /**
   * Add Validators to apply after each binding step.
   *
   * @see #setValidator(Validator)
   * @see #replaceValidators(Validator...)
   */
  public void addValidators(Validator... validators) {
    assertValidators(validators);
    this.validators.addAll(Arrays.asList(validators));
  }

  /**
   * Replace the Validators to apply after each binding step.
   *
   * @see #setValidator(Validator)
   * @see #addValidators(Validator...)
   */
  public void replaceValidators(Validator... validators) {
    assertValidators(validators);
    this.validators.clear();
    this.validators.addAll(Arrays.asList(validators));
  }

  /**
   * Return the primary Validator to apply after each binding step, if any.
   */
  @Nullable
  public Validator getValidator() {
    return (!this.validators.isEmpty() ? this.validators.get(0) : null);
  }

  /**
   * Return the Validators to apply after data binding.
   */
  public List<Validator> getValidators() {
    return Collections.unmodifiableList(this.validators);
  }

  /**
   * Return the Validators to apply after data binding. This includes the
   * configured {@link #getValidators() validators} filtered by the
   * {@link #setExcludedValidators(Predicate) exclude predicate}.
   */
  public List<Validator> getValidatorsToApply() {
    return (this.excludedValidators != null ?
            this.validators.stream().filter(validator -> !this.excludedValidators.test(validator)).toList() :
            Collections.unmodifiableList(this.validators));
  }

  //---------------------------------------------------------------------
  // Implementation of PropertyEditorRegistry/TypeConverter interface
  //---------------------------------------------------------------------

  /**
   * Specify a {@link ConversionService} to use for converting
   * property values, as an alternative to JavaBeans PropertyEditors.
   */
  public void setConversionService(@Nullable ConversionService conversionService) {
    Assert.state(this.conversionService == null, "DataBinder is already initialized with ConversionService");
    this.conversionService = conversionService;
    if (this.bindingResult != null && conversionService != null) {
      this.bindingResult.initConversion(conversionService);
    }
  }

  /**
   * Return the associated ConversionService, if any.
   */
  @Nullable
  public ConversionService getConversionService() {
    return this.conversionService;
  }

  /**
   * Add a custom formatter, applying it to all fields matching the
   * {@link Formatter}-declared type.
   * <p>Registers a corresponding {@link PropertyEditor} adapter underneath the covers.
   *
   * @param formatter the formatter to add, generically declared for a specific type
   * @see #registerCustomEditor(Class, PropertyEditor)
   */
  public void addCustomFormatter(Formatter<?> formatter) {
    FormatterPropertyEditorAdapter adapter = new FormatterPropertyEditorAdapter(formatter);
    getPropertyEditorRegistry().registerCustomEditor(adapter.getFieldType(), adapter);
  }

  /**
   * Add a custom formatter for the field type specified in {@link Formatter} class,
   * applying it to the specified fields only, if any, or otherwise to all fields.
   * <p>Registers a corresponding {@link PropertyEditor} adapter underneath the covers.
   *
   * @param formatter the formatter to add, generically declared for a specific type
   * @param fields the fields to apply the formatter to, or none if to be applied to all
   * @see #registerCustomEditor(Class, String, PropertyEditor)
   */
  public void addCustomFormatter(Formatter<?> formatter, String... fields) {
    FormatterPropertyEditorAdapter adapter = new FormatterPropertyEditorAdapter(formatter);
    Class<?> fieldType = adapter.getFieldType();
    PropertyEditorRegistry registry = getPropertyEditorRegistry();
    if (ObjectUtils.isEmpty(fields)) {
      registry.registerCustomEditor(fieldType, adapter);
    }
    else {
      for (String field : fields) {
        registry.registerCustomEditor(fieldType, field, adapter);
      }
    }
  }

  /**
   * Add a custom formatter, applying it to the specified field types only, if any,
   * or otherwise to all fields matching the {@link Formatter}-declared type.
   * <p>Registers a corresponding {@link PropertyEditor} adapter underneath the covers.
   *
   * @param formatter the formatter to add (does not need to generically declare a
   * field type if field types are explicitly specified as parameters)
   * @param fieldTypes the field types to apply the formatter to, or none if to be
   * derived from the given {@link Formatter} implementation class
   * @see #registerCustomEditor(Class, PropertyEditor)
   */
  public void addCustomFormatter(Formatter<?> formatter, Class<?>... fieldTypes) {
    FormatterPropertyEditorAdapter adapter = new FormatterPropertyEditorAdapter(formatter);
    PropertyEditorRegistry editorRegistry = getPropertyEditorRegistry();
    if (ObjectUtils.isEmpty(fieldTypes)) {
      editorRegistry.registerCustomEditor(adapter.getFieldType(), adapter);
    }
    else {
      for (Class<?> fieldType : fieldTypes) {
        editorRegistry.registerCustomEditor(fieldType, adapter);
      }
    }
  }

  @Override
  public void registerCustomEditor(Class<?> requiredType, PropertyEditor propertyEditor) {
    getPropertyEditorRegistry().registerCustomEditor(requiredType, propertyEditor);
  }

  @Override
  public void registerCustomEditor(@Nullable Class<?> requiredType, @Nullable String field, PropertyEditor propertyEditor) {
    getPropertyEditorRegistry().registerCustomEditor(requiredType, field, propertyEditor);
  }

  @Override
  @Nullable
  public PropertyEditor findCustomEditor(@Nullable Class<?> requiredType, @Nullable String propertyPath) {
    return getPropertyEditorRegistry().findCustomEditor(requiredType, propertyPath);
  }

  @Override
  @Nullable
  public <T> T convertIfNecessary(@Nullable Object value, @Nullable Class<T> requiredType) throws TypeMismatchException {
    return getTypeConverter().convertIfNecessary(value, requiredType);
  }

  @Override
  @Nullable
  public <T> T convertIfNecessary(@Nullable Object value, @Nullable Class<T> requiredType,
          @Nullable MethodParameter methodParam) throws TypeMismatchException {

    return getTypeConverter().convertIfNecessary(value, requiredType, methodParam);
  }

  @Override
  @Nullable
  public <T> T convertIfNecessary(@Nullable Object value, @Nullable Class<T> requiredType, @Nullable Field field)
          throws TypeMismatchException {

    return getTypeConverter().convertIfNecessary(value, requiredType, field);
  }

  @Nullable
  @Override
  public <T> T convertIfNecessary(@Nullable Object value, @Nullable Class<T> requiredType,
          @Nullable TypeDescriptor typeDescriptor) throws TypeMismatchException {

    return getTypeConverter().convertIfNecessary(value, requiredType, typeDescriptor);
  }

  /**
   * Create the target with constructor injection of values. It is expected that
   * {@link #setTargetType(ResolvableType)} was previously called and that
   * {@link #getTarget()} is {@code null}.
   * <p>Uses a public, no-arg constructor if available in the target object type,
   * also supporting a "primary constructor" approach for data classes as follows:
   * It understands the JavaBeans {@code ConstructorProperties} annotation as
   * well as runtime-retained parameter names in the bytecode, associating
   * input values with constructor arguments by name. If no such constructor is
   * found, the default constructor will be used (even if not public), assuming
   * subsequent bean property bindings through setter methods.
   * <p>After the call, use {@link #getBindingResult()} to check for failures
   * to bind to, and/or validate constructor arguments. If there are no errors,
   * the target is set, and {@link #doBind(PropertyValues)} can be used
   * for further initialization via setters.
   *
   * @param valueResolver to resolve constructor argument values with
   * @throws BeanInstantiationException in case of constructor failure
   */
  public final void construct(ValueResolver valueResolver) {
    Assert.state(this.target == null, "Target instance already available");
    Assert.state(this.targetType != null, "Target type not set");

    this.target = createObject(this.targetType, "", valueResolver);

    if (!getBindingResult().hasErrors()) {
      this.bindingResult = null;
      if (this.typeConverter != null) {
        this.typeConverter.registerCustomEditors(getPropertyAccessor());
      }
    }
  }

  @Nullable
  private Object createObject(ResolvableType objectType, String nestedPath, ValueResolver valueResolver) {
    Class<?> clazz = objectType.resolve();
    boolean isOptional = (clazz == Optional.class);
    clazz = (isOptional ? objectType.resolveGeneric(0) : clazz);
    if (clazz == null) {
      throw new IllegalStateException(
              "Insufficient type information to create instance of " + objectType);
    }

    Object result = null;
    Constructor<?> ctor = BeanUtils.obtainConstructor(clazz);

    if (ctor.getParameterCount() == 0) {
      // A single default constructor -> clearly a standard JavaBeans arrangement.
      result = BeanUtils.newInstance(ctor);
    }
    else {
      // A single data class constructor -> resolve constructor arguments from request parameters.
      String[] paramNames = BeanUtils.getParameterNames(ctor);
      Class<?>[] paramTypes = ctor.getParameterTypes();
      Object[] args = new Object[paramTypes.length];
      HashSet<String> failedParamNames = new HashSet<>(4);

      for (int i = 0; i < paramNames.length; i++) {
        MethodParameter param = MethodParameter.forFieldAwareConstructor(ctor, i, paramNames[i]);
        String lookupName = null;
        if (this.nameResolver != null) {
          lookupName = this.nameResolver.resolveName(param);
        }
        if (lookupName == null) {
          lookupName = paramNames[i];
        }

        String paramPath = nestedPath + lookupName;
        Class<?> paramType = paramTypes[i];
        Object value = valueResolver.resolveValue(paramPath, paramType);

        if (value == null && !BeanUtils.isSimpleValueType(param.nestedIfOptional().getNestedParameterType())) {
          ResolvableType type = ResolvableType.forMethodParameter(param);
          args[i] = createObject(type, paramPath + ".", valueResolver);
        }
        else {
          try {
            if (value == null && (param.isOptional() || getBindingResult().hasErrors())) {
              args[i] = (param.getParameterType() == Optional.class ? Optional.empty() : null);
            }
            else {
              args[i] = convertIfNecessary(value, paramType, param);
            }
          }
          catch (TypeMismatchException ex) {
            ex.initPropertyName(paramPath);
            args[i] = null;
            failedParamNames.add(paramPath);
            getBindingResult().recordFieldValue(paramPath, paramType, value);
            getBindingErrorProcessor().processPropertyAccessException(ex, getBindingResult());
          }
        }
      }

      if (getBindingResult().hasErrors()) {
        for (int i = 0; i < paramNames.length; i++) {
          String paramPath = nestedPath + paramNames[i];
          if (!failedParamNames.contains(paramPath)) {
            Object value = args[i];
            getBindingResult().recordFieldValue(paramPath, paramTypes[i], value);
            validateConstructorArgument(ctor.getDeclaringClass(), nestedPath, paramNames[i], value);
          }
        }
        if (!(objectType.getSource() instanceof MethodParameter param && param.isOptional())) {
          try {
            result = BeanUtils.newInstance(ctor, args);
          }
          catch (BeanInstantiationException ex) {
            // swallow and proceed without target instance
          }
        }
      }
      else {
        result = BeanUtils.newInstance(ctor, args);
      }
    }

    return (isOptional && !nestedPath.isEmpty() ? Optional.ofNullable(result) : result);
  }

  private void validateConstructorArgument(
          Class<?> constructorClass, String nestedPath, String name, @Nullable Object value) {

    Object[] hints = null;
    if (this.targetType.getSource() instanceof MethodParameter parameter) {
      for (Annotation ann : parameter.getParameterAnnotations()) {
        hints = ValidationAnnotationUtils.determineValidationHints(ann);
        if (hints != null) {
          break;
        }
      }
    }
    if (hints == null) {
      return;
    }
    for (Validator validator : getValidatorsToApply()) {
      if (validator instanceof SmartValidator smartValidator) {
        boolean isNested = !nestedPath.isEmpty();
        if (isNested) {
          getBindingResult().pushNestedPath(nestedPath.substring(0, nestedPath.length() - 1));
        }
        try {
          smartValidator.validateValue(constructorClass, name, value, getBindingResult(), hints);
        }
        catch (IllegalArgumentException ex) {
          // No corresponding field on the target class...
        }
        if (isNested) {
          getBindingResult().popNestedPath();
        }
      }
    }
  }

  /**
   * Bind the given property values to this binder's target.
   * <p>This call can create field errors, representing basic binding
   * errors like a required field (code "required"), or type mismatch
   * between value and bean property (code "typeMismatch").
   * <p>Note that the given PropertyValues should be a throwaway instance:
   * For efficiency, it will be modified to just contain allowed fields if it
   * implements the PropertyValues interface; else, an internal mutable
   * copy will be created for this purpose. Pass in a copy of the PropertyValues
   * if you want your original instance to stay unmodified in any case.
   *
   * @param pvs property values to bind
   * @see #doBind(cn.taketoday.beans.PropertyValues)
   */
  public void bind(PropertyValues pvs) {
    if (shouldNotBindPropertyValues()) {
      return;
    }
    doBind(pvs);
  }

  /**
   * Whether to not bind parameters to properties. Returns "true" if
   * {@link #isDeclarativeBinding()} is on, and
   * {@link #setAllowedFields(String...) allowedFields} are not configured.
   */
  protected boolean shouldNotBindPropertyValues() {
    return isDeclarativeBinding() && ObjectUtils.isEmpty(this.allowedFields);
  }

  /**
   * Actual implementation of the binding process, working with the
   * passed-in PropertyValues instance.
   *
   * @param values the property values to bind,
   * as PropertyValues instance
   * @see #checkAllowedFields
   * @see #checkRequiredFields
   * @see #applyPropertyValues
   */
  protected void doBind(PropertyValues values) {
    checkAllowedFields(values);
    checkRequiredFields(values);
    applyPropertyValues(values);
  }

  /**
   * Check the given property values against the allowed fields,
   * removing values for fields that are not allowed.
   *
   * @param mpvs the property values to be bound (can be modified)
   * @see #getAllowedFields
   * @see #isAllowed(String)
   */
  protected void checkAllowedFields(PropertyValues mpvs) {
    for (PropertyValue pv : mpvs.toArray()) {
      String field = PropertyAccessorUtils.canonicalPropertyName(pv.getName());
      if (!isAllowed(field)) {
        mpvs.remove(pv);
        getBindingResult().recordSuppressedField(field);
        if (logger.isDebugEnabled()) {
          logger.debug("Field [{}] has been removed from PropertyValues " +
                  "and will not be bound, because it has not been found in the list of allowed fields", field);
        }
      }
    }
  }

  /**
   * Determine if the given field is allowed for binding.
   * <p>Invoked for each passed-in property value.
   * <p>Checks for {@code "xxx*"}, {@code "*xxx"}, {@code "*xxx*"}, and
   * {@code "xxx*yyy"} matches (with an arbitrary number of pattern parts), as
   * well as direct equality, in the configured lists of allowed field patterns
   * and disallowed field patterns.
   * <p>Matching against allowed field patterns is case-sensitive; whereas,
   * matching against disallowed field patterns is case-insensitive.
   * <p>A field matching a disallowed pattern will not be accepted even if it
   * also happens to match a pattern in the allowed list.
   * <p>Can be overridden in subclasses, but care must be taken to honor the
   * aforementioned contract.
   *
   * @param field the field to check
   * @return {@code true} if the field is allowed
   * @see #setAllowedFields
   * @see #setDisallowedFields
   * @see cn.taketoday.util.StringUtils#simpleMatch(String, String)
   */
  protected boolean isAllowed(String field) {
    String[] allowed = getAllowedFields();
    String[] disallowed = getDisallowedFields();
    return (ObjectUtils.isEmpty(allowed) || StringUtils.simpleMatch(allowed, field))
            && (ObjectUtils.isEmpty(disallowed) || !StringUtils.simpleMatch(disallowed, field.toLowerCase()));
  }

  /**
   * Check the given property values against the required fields,
   * generating missing field errors where appropriate.
   *
   * @param mpvs the property values to be bound (can be modified)
   * @see #getRequiredFields
   * @see #getBindingErrorProcessor
   * @see BindingErrorProcessor#processMissingFieldError
   */
  protected void checkRequiredFields(PropertyValues mpvs) {
    String[] requiredFields = getRequiredFields();
    if (ObjectUtils.isNotEmpty(requiredFields)) {
      HashMap<String, PropertyValue> propertyValues = new HashMap<>();
      for (PropertyValue pv : mpvs) {
        String canonicalName = PropertyAccessorUtils.canonicalPropertyName(pv.getName());
        propertyValues.put(canonicalName, pv);
      }

      BindingErrorProcessor bindingErrorProcessor = getBindingErrorProcessor();
      for (String field : requiredFields) {
        PropertyValue pv = propertyValues.get(field);
        boolean empty = pv == null || pv.getValue() == null;
        if (!empty) {
          if (pv.getValue() instanceof String text) {
            empty = StringUtils.isBlank(text);
          }
          else if (pv.getValue() instanceof String[] values) {
            empty = values.length == 0 || StringUtils.isBlank(values[0]);
          }
        }
        if (empty) {
          // Use bind error processor to create FieldError.
          bindingErrorProcessor.processMissingFieldError(field, getInternalBindingResult());
          // Remove property from property values to bind:
          // It has already caused a field error with a rejected value.
          if (pv != null) {
            mpvs.remove(pv);
            propertyValues.remove(field);
          }
        }
      }
    }
  }

  /**
   * Apply given property values to the target object.
   * <p>Default implementation applies all the supplied property
   * values as bean property values. By default, unknown fields will
   * be ignored.
   *
   * @param mpvs the property values to be bound (can be modified)
   * @see #getTarget
   * @see #getPropertyAccessor
   * @see #isIgnoreUnknownFields
   * @see #getBindingErrorProcessor
   * @see BindingErrorProcessor#processPropertyAccessException
   */
  protected void applyPropertyValues(PropertyValues mpvs) {
    try {
      // Bind request parameters onto target object.
      getPropertyAccessor().setPropertyValues(mpvs, isIgnoreUnknownFields(), isIgnoreInvalidFields());
    }
    catch (PropertyBatchUpdateException ex) {
      // Use bind error processor to create FieldErrors.
      for (PropertyAccessException pae : ex.getPropertyAccessExceptions()) {
        getBindingErrorProcessor().processPropertyAccessException(pae, getInternalBindingResult());
      }
    }
  }

  /**
   * Invoke the specified Validators, if any.
   *
   * @see #setValidator(Validator)
   * @see #getBindingResult()
   */
  public void validate() {
    Object target = getTarget();
    Assert.state(target != null, "No target to validate");
    BindingResult bindingResult = getBindingResult();
    // Call each validator with the same binding result
    for (Validator validator : getValidatorsToApply()) {
      validator.validate(target, bindingResult);
    }
  }

  /**
   * Invoke the specified Validators, if any, with the given validation hints.
   * <p>Note: Validation hints may get ignored by the actual target Validator.
   *
   * @param validationHints one or more hint objects to be passed to a {@link SmartValidator}
   * @see #setValidator(Validator)
   * @see SmartValidator#validate(Object, Errors, Object...)
   */
  public void validate(Object... validationHints) {
    Object target = getTarget();
    Assert.state(target != null, "No target to validate");
    BindingResult bindingResult = getBindingResult();
    // Call each validator with the same binding result
    for (Validator validator : getValidatorsToApply()) {
      if (!ObjectUtils.isEmpty(validationHints) && validator instanceof SmartValidator smartValidator) {
        smartValidator.validate(target, bindingResult, validationHints);
      }
      else if (validator != null) {
        validator.validate(target, bindingResult);
      }
    }
  }

  /**
   * Close this DataBinder, which may result in throwing
   * a BindException if it encountered any errors.
   *
   * @return the model Map, containing target object and Errors instance
   * @throws BindException if there were any errors in the bind operation
   * @see BindingResult#getModel()
   */
  public Map<?, ?> close() throws BindException {
    if (getBindingResult().hasErrors()) {
      throw new BindException(getBindingResult());
    }
    return getBindingResult().getModel();
  }

  /**
   * Strategy to determine the name of the value to bind to a method parameter.
   * Supported on constructor parameters with {@link #construct constructor
   * binding} which performs lookups via {@link ValueResolver#resolveValue}.
   */
  public interface NameResolver {

    /**
     * Return the name to use for the given method parameter, or {@code null}
     * if unresolved. For constructor parameters, the name is determined via
     * {@link cn.taketoday.core.DefaultParameterNameDiscoverer} if
     * unresolved.
     */
    @Nullable
    String resolveName(MethodParameter parameter);

  }

  /**
   * Strategy for {@link #construct constructor binding} to look up the values
   * to bind to a given constructor parameter.
   */
  @FunctionalInterface
  public interface ValueResolver {

    /**
     * Resolve the value for the given name and target parameter type.
     *
     * @param name the name to use for the lookup, possibly a nested path
     * for constructor parameters on nested objects
     * @param type the target type, based on the constructor parameter type
     * @return the resolved value, possibly {@code null} if none found
     */
    @Nullable
    Object resolveValue(String name, Class<?> type);

  }

  /**
   * {@link SimpleTypeConverter} that is also {@link PropertyEditorRegistrar}.
   */
  private static class ExtendedTypeConverter
          extends SimpleTypeConverter implements PropertyEditorRegistrar {

    @Override
    public void registerCustomEditors(PropertyEditorRegistry registry) {
      copyCustomEditorsTo(registry, null);
    }
  }

}
