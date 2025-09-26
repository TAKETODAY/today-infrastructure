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

package infra.validation.beanvalidation;

import org.jspecify.annotations.Nullable;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Supplier;

import infra.aop.framework.AopProxyUtils;
import infra.aop.support.AopUtils;
import infra.context.MessageSourceResolvable;
import infra.context.support.DefaultMessageSourceResolvable;
import infra.core.BridgeMethodResolver;
import infra.core.Conventions;
import infra.core.DefaultParameterNameDiscoverer;
import infra.core.GenericTypeResolver;
import infra.core.MethodParameter;
import infra.core.ParameterNameDiscoverer;
import infra.core.annotation.AnnotationUtils;
import infra.lang.Assert;
import infra.validation.BeanPropertyBindingResult;
import infra.validation.BindingResult;
import infra.validation.DefaultMessageCodesResolver;
import infra.validation.Errors;
import infra.validation.MessageCodesResolver;
import infra.validation.annotation.Validated;
import infra.validation.method.MethodValidationResult;
import infra.validation.method.MethodValidator;
import infra.validation.method.ParameterErrors;
import infra.validation.method.ParameterValidationResult;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ElementKind;
import jakarta.validation.Path;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import jakarta.validation.executable.ExecutableValidator;
import jakarta.validation.metadata.ConstraintDescriptor;

/**
 * {@link MethodValidator} that uses a Bean Validation
 * {@link jakarta.validation.Validator} for validation, and adapts
 * {@link ConstraintViolation}s to {@link MethodValidationResult}.
 *
 * @author Rossen Stoyanchev
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2023/6/15 22:23
 */
public class MethodValidationAdapter implements MethodValidator {

  private static final MethodValidationResult emptyValidationResult = MethodValidationResult.emptyResult();

  private static final ObjectNameResolver defaultObjectNameResolver = new DefaultObjectNameResolver();

  private static final Comparator<ParameterValidationResult> resultComparator = new ResultComparator();

  private final Validator validator;

  private final InfraValidatorAdapter validatorAdapter;

  private MessageCodesResolver messageCodesResolver = new DefaultMessageCodesResolver();

  private ParameterNameDiscoverer parameterNameDiscoverer = ParameterNameDiscoverer.getSharedInstance();

  private ObjectNameResolver objectNameResolver = defaultObjectNameResolver;

  /**
   * Create an instance using a default JSR-303 validator underneath.
   */
  public MethodValidationAdapter() {
    this(Validation.buildDefaultValidatorFactory());
  }

  /**
   * Create an instance using the given JSR-303 ValidatorFactory.
   *
   * @param validatorFactory the JSR-303 ValidatorFactory to use
   */
  public MethodValidationAdapter(ValidatorFactory validatorFactory) {
    this(validatorFactory::getValidator);
  }

  /**
   * Create an instance using the given JSR-303 Validator.
   *
   * @param validator the JSR-303 Validator to use
   */
  public MethodValidationAdapter(Validator validator) {
    Assert.notNull(validator, "Validator is required");
    this.validator = validator;
    this.validatorAdapter = new InfraValidatorAdapter(validator);
  }

  /**
   * Create an instance for the supplied (potentially lazily initialized) Validator.
   *
   * @param validator a Supplier for the Validator to use
   */
  public MethodValidationAdapter(Supplier<Validator> validator) {
    this(new SuppliedValidator(validator));
  }

  /**
   * Set the strategy to use to determine message codes for violations.
   * <p>Default is a DefaultMessageCodesResolver.
   */
  public void setMessageCodesResolver(MessageCodesResolver messageCodesResolver) {
    this.messageCodesResolver = messageCodesResolver;
  }

  /**
   * Return the {@link #setMessageCodesResolver(MessageCodesResolver) configured}
   * {@code MessageCodesResolver}.
   */
  public MessageCodesResolver getMessageCodesResolver() {
    return this.messageCodesResolver;
  }

  /**
   * Set the {@code ParameterNameDiscoverer} to discover method parameter names
   * with to create error codes for {@link MessageSourceResolvable}. Used only
   * when {@link MethodParameter}s are not passed into
   * {@link #validateArguments} or {@link #validateReturnValue}.
   * <p>Default is {@link DefaultParameterNameDiscoverer}.
   */
  public void setParameterNameDiscoverer(ParameterNameDiscoverer parameterNameDiscoverer) {
    this.parameterNameDiscoverer = parameterNameDiscoverer;
  }

  /**
   * Return the {@link #setParameterNameDiscoverer configured}
   * {@code ParameterNameDiscoverer}.
   */
  public ParameterNameDiscoverer getParameterNameDiscoverer() {
    return this.parameterNameDiscoverer;
  }

  /**
   * Configure a resolver to determine the name of an {@code @Valid} method
   * parameter to use for its {@link BindingResult}. This allows aligning with
   * a higher level programming model such as to resolve the name of an
   * {@code @ModelAttribute} method parameter in Infra MVC.
   * <p>By default, the object name is resolved through:
   * <ul>
   * <li>{@link MethodParameter#getParameterName()} for input parameters
   * <li>{@link Conventions#getVariableNameForReturnType(Method, Class, Object)}
   * for a return type
   * </ul>
   * If a name cannot be determined, e.g. a return value with insufficient
   * type information, then it defaults to one of:
   * <ul>
   * <li>{@code "{methodName}.arg{index}"} for input parameters
   * <li>{@code "{methodName}.returnValue"} for a return type
   * </ul>
   */
  public void setObjectNameResolver(ObjectNameResolver nameResolver) {
    this.objectNameResolver = nameResolver;
  }

  /**
   * Return the {@link InfraValidatorAdapter} configured for use.
   *
   * @since 5.0
   */
  public InfraValidatorAdapter getValidatorAdapter() {
    return validatorAdapter;
  }

  /**
   * {@inheritDoc}.
   * <p>Default are the validation groups as specified in the {@link Validated}
   * annotation on the method, or on the containing target class of the method,
   * or for an AOP proxy without a target (with all behavior in advisors), also
   * check on proxied interfaces.
   */
  @Override
  public Class<?>[] determineValidationGroups(Object target, Method method) {
    Validated validatedAnn = AnnotationUtils.findAnnotation(method, Validated.class);
    if (validatedAnn == null) {
      if (AopUtils.isAopProxy(target)) {
        for (Class<?> type : AopProxyUtils.proxiedUserInterfaces(target)) {
          validatedAnn = AnnotationUtils.findAnnotation(type, Validated.class);
          if (validatedAnn != null) {
            break;
          }
        }
      }
      else {
        validatedAnn = AnnotationUtils.findAnnotation(target.getClass(), Validated.class);
      }
    }
    return (validatedAnn != null ? validatedAnn.value() : new Class<?>[0]);
  }

  @Override
  public final MethodValidationResult validateArguments(Object target, Method method,
          @Nullable MethodParameter[] parameters, Object[] arguments, Class<?>[] groups) {

    Set<ConstraintViolation<Object>> violations =
            invokeValidatorForArguments(target, method, arguments, groups);

    if (violations.isEmpty()) {
      return emptyValidationResult;
    }

    return adaptViolations(target, method, violations,
            i -> parameters != null ? parameters[i] : initMethodParameter(method, i),
            i -> arguments[i]);
  }

  /**
   * Invoke the validator, and return the resulting violations.
   */
  public final Set<ConstraintViolation<Object>> invokeValidatorForArguments(
          Object target, Method method, Object[] arguments, Class<?>[] groups) {

    ExecutableValidator execVal = validator.forExecutables();
    Set<ConstraintViolation<Object>> violations;
    try {
      violations = execVal.validateParameters(target, method, arguments, groups);
    }
    catch (IllegalArgumentException ex) {
      // Let's try to find the bridged method on the implementation class...
      Method bridgedMethod = BridgeMethodResolver.getMostSpecificMethod(method, target.getClass());
      violations = execVal.validateParameters(target, bridgedMethod, arguments, groups);
    }
    return violations;
  }

  @Override
  public final MethodValidationResult validateReturnValue(Object target, Method method,
          @Nullable MethodParameter returnType, @Nullable Object returnValue, Class<?>[] groups) {

    Set<ConstraintViolation<Object>> violations =
            invokeValidatorForReturnValue(target, method, returnValue, groups);

    if (violations.isEmpty()) {
      return emptyValidationResult;
    }

    return adaptViolations(target, method, violations,
            i -> returnType != null ? returnType : initMethodParameter(method, -1),
            i -> returnValue);
  }

  /**
   * Invoke the validator, and return the resulting violations.
   */
  public final Set<ConstraintViolation<Object>> invokeValidatorForReturnValue(
          Object target, Method method, @Nullable Object returnValue, Class<?>[] groups) {

    ExecutableValidator execVal = validator.forExecutables();
    return execVal.validateReturnValue(target, method, returnValue, groups);
  }

  private MethodValidationResult adaptViolations(Object target, Method method, Set<ConstraintViolation<Object>> violations,
          Function<Integer, MethodParameter> parameterFunction, Function<Integer, Object> argumentFunction) {

    Map<Path.Node, ParamValidationResultBuilder> paramViolations = new LinkedHashMap<>();
    Map<Path.Node, ParamErrorsBuilder> nestedViolations = new LinkedHashMap<>();
    List<MessageSourceResolvable> crossParamErrors = null;

    for (ConstraintViolation<Object> violation : violations) {
      Iterator<Path.Node> nodes = violation.getPropertyPath().iterator();
      while (nodes.hasNext()) {
        Path.Node node = nodes.next();

        MethodParameter parameter;
        if (node.getKind().equals(ElementKind.PARAMETER)) {
          int index = node.as(Path.ParameterNode.class).getParameterIndex();
          parameter = parameterFunction.apply(index);
        }
        else if (node.getKind().equals(ElementKind.RETURN_VALUE)) {
          parameter = parameterFunction.apply(-1);
        }
        else if (node.getKind().equals(ElementKind.CROSS_PARAMETER)) {
          crossParamErrors = (crossParamErrors != null ? crossParamErrors : new ArrayList<>());
          crossParamErrors.add(createCrossParamError(target, method, violation));
          break;
        }
        else {
          continue;
        }

        Object arg = argumentFunction.apply(parameter.getParameterIndex());

        // If the arg is a container, we need the element, but the only way to extract it
        // is to check for and use a container index or key on the next node:
        // https://github.com/jakartaee/validation/issues/194

        Path.Node parameterNode = node;
        if (nodes.hasNext()) {
          node = nodes.next();
        }

        Object value;
        Object container;
        Integer index = node.getIndex();
        Object key = node.getKey();
        if (index != null && arg instanceof List<?> list) {
          value = list.get(index);
          container = list;
        }
        else if (index != null && arg instanceof Object[] array) {
          value = array[index];
          container = array;
        }
        else if (key != null && arg instanceof Map<?, ?> map) {
          value = map.get(key);
          container = map;
        }
        else if (arg instanceof Iterable<?>) {
          // No index or key, cannot access the specific value
          value = arg;
          container = arg;
        }
        else if (arg instanceof Optional<?> optional) {
          value = optional.orElse(null);
          container = optional;
        }
        else {
          value = arg;
          container = null;
        }

        if (node.getKind().equals(ElementKind.PROPERTY) || node.getKind().equals(ElementKind.BEAN)) {
          nestedViolations.computeIfAbsent(parameterNode, k -> new ParamErrorsBuilder(parameter, value, container, index, key))
                  .addViolation(violation);
        }
        else {
          paramViolations.computeIfAbsent(parameterNode, p -> new ParamValidationResultBuilder(target, parameter, value, container, index, key))
                  .addViolation(violation);
        }

        break;
      }
    }

    List<ParameterValidationResult> resultList = new ArrayList<>();
    paramViolations.forEach((param, builder) -> resultList.add(builder.build()));
    nestedViolations.forEach((key, builder) -> resultList.add(builder.build()));
    resultList.sort(resultComparator);

    return MethodValidationResult.create(target, method, resultList,
            (crossParamErrors != null ? crossParamErrors : Collections.emptyList()));
  }

  private MethodParameter initMethodParameter(Method method, int index) {
    MethodParameter parameter = new MethodParameter(method, index);
    parameter.initParameterNameDiscovery(this.parameterNameDiscoverer);
    return parameter;
  }

  private MessageSourceResolvable createMessageSourceResolvable(
          Object target, MethodParameter parameter, ConstraintViolation<Object> violation) {

    String objectName = Conventions.getVariableName(target) + "#" + parameter.getExecutable().getName();
    String paramName = (parameter.getParameterName() != null ? parameter.getParameterName() : "");
    Class<?> parameterType = parameter.getParameterType();

    ConstraintDescriptor<?> descriptor = violation.getConstraintDescriptor();
    String code = descriptor.getAnnotation().annotationType().getSimpleName();
    String[] codes = messageCodesResolver.resolveMessageCodes(code, objectName, paramName, parameterType);
    Object[] arguments = validatorAdapter.getArgumentsForConstraint(objectName, paramName, descriptor);

    return new ViolationMessageSourceResolvable(codes, arguments, violation.getMessage(), violation);
  }

  private BindingResult createBindingResult(MethodParameter parameter, @Nullable Object argument) {
    String objectName = this.objectNameResolver.resolveName(parameter, argument);
    BeanPropertyBindingResult result = new BeanPropertyBindingResult(argument, objectName);
    result.setMessageCodesResolver(this.messageCodesResolver);
    return result;
  }

  private MessageSourceResolvable createCrossParamError(
          Object target, Method method, ConstraintViolation<Object> violation) {
    String objectName = Conventions.getVariableName(target) + "#" + method.getName();

    ConstraintDescriptor<?> descriptor = violation.getConstraintDescriptor();
    String code = descriptor.getAnnotation().annotationType().getSimpleName();
    String[] codes = this.messageCodesResolver.resolveMessageCodes(code, objectName);
    Object[] arguments = this.validatorAdapter.getArgumentsForConstraint(objectName, "", descriptor);

    return new ViolationMessageSourceResolvable(codes, arguments, violation.getMessage(), violation);
  }

  /**
   * Strategy to resolve the name of an {@code @Valid} method parameter to
   * use for its {@link BindingResult}.
   */
  public interface ObjectNameResolver {

    /**
     * Determine the name for the given method argument.
     *
     * @param parameter the method parameter
     * @param value the argument value or return value
     * @return the name to use
     */
    String resolveName(MethodParameter parameter, @Nullable Object value);

  }

  /**
   * Builds a validation result for a value method parameter with constraints
   * declared directly on it.
   */
  private final class ParamValidationResultBuilder {

    private final Object target;

    private final MethodParameter parameter;

    @Nullable
    private final Object value;

    @Nullable
    private final Object container;

    @Nullable
    private final Integer containerIndex;

    @Nullable
    private final Object containerKey;

    private final List<MessageSourceResolvable> resolvableErrors = new ArrayList<>();

    public ParamValidationResultBuilder(Object target, MethodParameter parameter,
            @Nullable Object value, @Nullable Object container, @Nullable Integer containerIndex, @Nullable Object containerKey) {
      this.target = target;
      this.parameter = parameter;
      this.value = value;
      this.container = container;
      this.containerIndex = containerIndex;
      this.containerKey = containerKey;
    }

    public void addViolation(ConstraintViolation<Object> violation) {
      this.resolvableErrors.add(createMessageSourceResolvable(this.target, this.parameter, violation));
    }

    public ParameterValidationResult build() {
      return new ParameterValidationResult(this.parameter, this.value, this.resolvableErrors,
              this.container, this.containerIndex, this.containerKey, (error, sourceType) -> {
        Assert.isTrue(sourceType.equals(ConstraintViolation.class), "Unexpected source type");
        return ((ViolationMessageSourceResolvable) error).getViolation();
      });
    }

  }

  /**
   * Builds a validation result for an {@link jakarta.validation.Valid @Valid}
   * annotated bean method parameter with cascaded constraints.
   */
  private final class ParamErrorsBuilder {

    private final MethodParameter parameter;

    @Nullable
    private final Object bean;

    @Nullable
    private final Object container;

    @Nullable
    private final Integer containerIndex;

    @Nullable
    private final Object containerKey;

    private final Errors errors;

    private final Set<ConstraintViolation<Object>> violations = new LinkedHashSet<>();

    public ParamErrorsBuilder(MethodParameter param, @Nullable Object bean,
            @Nullable Object container, @Nullable Integer containerIndex, @Nullable Object containerKey) {

      this.parameter = param;
      this.bean = bean;
      this.container = container;
      this.containerIndex = containerIndex;
      this.containerKey = containerKey;
      this.errors = createBindingResult(param, this.bean);
    }

    public void addViolation(ConstraintViolation<Object> violation) {
      this.violations.add(violation);
    }

    public ParameterErrors build() {
      validatorAdapter.processConstraintViolations(this.violations, this.errors);
      return new ParameterErrors(
              this.parameter, this.bean, this.errors, this.container,
              this.containerIndex, this.containerKey);
    }
  }

  @SuppressWarnings("serial")
  private static final class ViolationMessageSourceResolvable extends DefaultMessageSourceResolvable {

    private final transient ConstraintViolation<Object> violation;

    public ViolationMessageSourceResolvable(
            String[] codes, Object[] arguments, String defaultMessage, ConstraintViolation<Object> violation) {

      super(codes, arguments, defaultMessage);
      this.violation = violation;
    }

    public ConstraintViolation<Object> getViolation() {
      return this.violation;
    }
  }

  /**
   * Default algorithm to select an object name, as described in
   * {@link #setObjectNameResolver(ObjectNameResolver)}.
   */
  private static final class DefaultObjectNameResolver implements ObjectNameResolver {

    @Override
    public String resolveName(MethodParameter parameter, @Nullable Object value) {
      String objectName = null;
      if (parameter.getParameterIndex() != -1) {
        objectName = parameter.getParameterName();
      }
      else {
        try {
          Method method = parameter.getMethod();
          if (method != null) {
            Class<?> containingClass = parameter.getContainingClass();
            Class<?> resolvedType = GenericTypeResolver.resolveReturnType(method, containingClass);
            objectName = Conventions.getVariableNameForReturnType(method, resolvedType, value);
          }
        }
        catch (IllegalArgumentException ex) {
          // insufficient type information
        }
      }
      if (objectName == null) {
        int index = parameter.getParameterIndex();
        objectName = (parameter.getExecutable().getName() + (index != -1 ? ".arg" + index : ".returnValue"));
      }
      return objectName;
    }
  }

  /**
   * Comparator for validation results, sorted by method parameter index first,
   * also falling back on container indexes if necessary for cascaded
   * constraints on a List container.
   */
  private static final class ResultComparator implements Comparator<ParameterValidationResult> {

    @Override
    public int compare(ParameterValidationResult result1, ParameterValidationResult result2) {
      int index1 = result1.getMethodParameter().getParameterIndex();
      int index2 = result2.getMethodParameter().getParameterIndex();
      int i = Integer.compare(index1, index2);
      if (i != 0) {
        return i;
      }
      if (result1 instanceof ParameterErrors errors1 && result2 instanceof ParameterErrors errors2) {
        Integer containerIndex1 = errors1.getContainerIndex();
        Integer containerIndex2 = errors2.getContainerIndex();
        if (containerIndex1 != null && containerIndex2 != null) {
          i = Integer.compare(containerIndex1, containerIndex2);
          if (i != 0) {
            return i;
          }
        }
        i = compareKeys(errors1, errors2);
        return i;
      }
      return 0;
    }

    @SuppressWarnings("unchecked")
    private <E> int compareKeys(ParameterErrors errors1, ParameterErrors errors2) {
      Object key1 = errors1.getContainerKey();
      Object key2 = errors2.getContainerKey();
      if (key1 instanceof Comparable<?> && key2 instanceof Comparable<?>) {
        return ((Comparable<E>) key1).compareTo((E) key2);
      }
      return 0;
    }
  }

}

