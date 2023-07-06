/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © Harry Yang & 2017 - 2023 All Rights Reserved.
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

package cn.taketoday.validation.beanvalidation;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Supplier;

import cn.taketoday.aop.framework.AopProxyUtils;
import cn.taketoday.aop.support.AopUtils;
import cn.taketoday.context.MessageSourceResolvable;
import cn.taketoday.context.support.DefaultMessageSourceResolvable;
import cn.taketoday.core.BridgeMethodResolver;
import cn.taketoday.core.Conventions;
import cn.taketoday.core.GenericTypeResolver;
import cn.taketoday.core.MethodParameter;
import cn.taketoday.core.ParameterNameDiscoverer;
import cn.taketoday.core.annotation.AnnotationUtils;
import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;
import cn.taketoday.util.ReflectionUtils;
import cn.taketoday.validation.BeanPropertyBindingResult;
import cn.taketoday.validation.BindingResult;
import cn.taketoday.validation.DefaultMessageCodesResolver;
import cn.taketoday.validation.Errors;
import cn.taketoday.validation.MessageCodesResolver;
import cn.taketoday.validation.annotation.Validated;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ElementKind;
import jakarta.validation.Path;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import jakarta.validation.executable.ExecutableValidator;
import jakarta.validation.metadata.ConstraintDescriptor;

/**
 * Assist with applying method-level validation via
 * {@link jakarta.validation.Validator}, adapt each resulting
 * {@link ConstraintViolation} to {@link ParameterValidationResult}, and
 * raise {@link MethodValidationException}.
 *
 * <p>Used by {@link MethodValidationInterceptor}.
 *
 * @author Rossen Stoyanchev
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2023/6/15 22:23
 */
public class MethodValidationAdapter {

  private static final Comparator<ParameterValidationResult> RESULT_COMPARATOR = new ResultComparator();

  private static final MethodValidationResult EMPTY_RESULT = new EmptyMethodValidationResult();

  private final Validator validator;

  private final InfraValidatorAdapter validatorAdapter;

  private MessageCodesResolver messageCodesResolver = new DefaultMessageCodesResolver();

  private ParameterNameDiscoverer parameterNameDiscoverer = ParameterNameDiscoverer.getSharedInstance();

  @Nullable
  private BindingResultNameResolver objectNameResolver;

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
   * Set the ParameterNameDiscoverer to use to resolve method parameter names
   * that is in turn used to create error codes for {@link MessageSourceResolvable}.
   * <p>Default is {@link cn.taketoday.core.DefaultParameterNameDiscoverer}.
   */
  public void setParameterNameDiscoverer(ParameterNameDiscoverer parameterNameDiscoverer) {
    this.parameterNameDiscoverer = parameterNameDiscoverer;
  }

  /**
   * Return the {@link #setParameterNameDiscoverer(ParameterNameDiscoverer) configured}
   * {@code ParameterNameDiscoverer}.
   */
  public ParameterNameDiscoverer getParameterNameDiscoverer() {
    return this.parameterNameDiscoverer;
  }

  /**
   * Configure a resolver for {@link BindingResult} method parameters to match
   * the behavior of the higher level programming model, e.g. how the name of
   * {@code @ModelAttribute} or {@code @RequestBody} is determined in Infra MVC.
   * <p>If this is not configured, then {@link #createBindingResult} will apply
   * default behavior to resolve the name to use.
   * behavior applies.
   *
   * @param nameResolver the resolver to use
   */
  public void setBindingResultNameResolver(BindingResultNameResolver nameResolver) {
    this.objectNameResolver = nameResolver;
  }

  /**
   * Use this method determine the validation groups to pass into
   * {@link #validateMethodArguments(Object, Method, MethodParameter[], Object[], Class[])} and
   * {@link #validateMethodReturnValue(Object, Method, MethodParameter, Object, Class[])}.
   * <p>Default are the validation groups as specified in the {@link Validated}
   * annotation on the method, or on the containing target class of the method,
   * or for an AOP proxy without a target (with all behavior in advisors), also
   * check on proxied interfaces.
   *
   * @param target the target Object
   * @param method the target method
   * @return the applicable validation groups as a {@code Class} array
   */
  public static Class<?>[] determineValidationGroups(Object target, Method method) {
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
    return validatedAnn != null ? validatedAnn.value() : new Class<?>[0];
  }

  /**
   * Validate the given method arguments and return the result of validation.
   *
   * @param target the target Object
   * @param method the target method
   * @param parameters the parameters, if already created and available
   * @param arguments the candidate argument values to validate
   * @param groups groups for validation determined via
   * {@link #determineValidationGroups(Object, Method)}
   * @return a result with {@link ConstraintViolation violations} and
   * {@link ParameterValidationResult validationResults}, both possibly empty
   * in case there are no violations
   */
  public MethodValidationResult validateMethodArguments(Object target, Method method,
          @Nullable MethodParameter[] parameters, Object[] arguments, Class<?>[] groups) {

    ExecutableValidator execVal = validator.forExecutables();
    Set<ConstraintViolation<Object>> result;
    try {
      result = execVal.validateParameters(target, method, arguments, groups);
    }
    catch (IllegalArgumentException ex) {
      // Probably a generic type mismatch between interface and impl as reported in SPR-12237 / HV-1011
      // Let's try to find the bridged method on the implementation class...
      Method mostSpecificMethod = ReflectionUtils.getMostSpecificMethod(method, target.getClass());
      Method bridgedMethod = BridgeMethodResolver.findBridgedMethod(mostSpecificMethod);
      result = execVal.validateParameters(target, bridgedMethod, arguments, groups);
    }
    return result.isEmpty()
           ? EMPTY_RESULT
           : createException(target, method, result,
                   i -> parameters != null ? parameters[i] : new MethodParameter(method, i),
                   i -> arguments[i], false);
  }

  /**
   * Validate the given return value and return the result of validation.
   *
   * @param target the target Object
   * @param method the target method
   * @param returnType the return parameter, if already created and available
   * @param returnValue the return value to validate
   * @param groups groups for validation determined via
   * {@link #determineValidationGroups(Object, Method)}
   * @return a result with {@link ConstraintViolation violations} and
   * {@link ParameterValidationResult validationResults}, both possibly empty
   * in case there are no violations
   */
  public MethodValidationResult validateMethodReturnValue(Object target, Method method,
          @Nullable MethodParameter returnType, @Nullable Object returnValue, Class<?>[] groups) {

    ExecutableValidator execVal = validator.forExecutables();
    Set<ConstraintViolation<Object>> result = execVal.validateReturnValue(target, method, returnValue, groups);
    return (result.isEmpty() ? EMPTY_RESULT :
            createException(target, method, result,
                    i -> returnType != null ? returnType : new MethodParameter(method, -1),
                    i -> returnValue,
                    true));
  }

  private MethodValidationException createException(Object target, Method method,
          Set<ConstraintViolation<Object>> violations, Function<Integer, MethodParameter> parameterFunction,
          Function<Integer, Object> argumentFunction, boolean forReturnValue) {

    var parameterViolations = new LinkedHashMap<MethodParameter, ValueResultBuilder>();
    var cascadedViolations = new LinkedHashMap<Path.Node, BeanResultBuilder>();

    for (ConstraintViolation<Object> violation : violations) {
      Iterator<Path.Node> itr = violation.getPropertyPath().iterator();
      while (itr.hasNext()) {
        Path.Node node = itr.next();

        MethodParameter parameter;
        if (node.getKind().equals(ElementKind.PARAMETER)) {
          int index = node.as(Path.ParameterNode.class).getParameterIndex();
          parameter = parameterFunction.apply(index);
        }
        else if (node.getKind().equals(ElementKind.RETURN_VALUE)) {
          parameter = parameterFunction.apply(-1);
        }
        else {
          continue;
        }
        parameter.initParameterNameDiscovery(this.parameterNameDiscoverer);

        Object argument = argumentFunction.apply(parameter.getParameterIndex());
        if (!itr.hasNext()) {
          parameterViolations
                  .computeIfAbsent(parameter, p -> new ValueResultBuilder(target, parameter, argument))
                  .addViolation(violation);
        }
        else {
          cascadedViolations
                  .computeIfAbsent(node, n -> new BeanResultBuilder(parameter, argument, itr.next()))
                  .addViolation(violation);
        }
        break;
      }
    }

    var validationResultList = new ArrayList<ParameterValidationResult>();

    for (ValueResultBuilder builder : parameterViolations.values()) {
      validationResultList.add(builder.build());
    }

    for (BeanResultBuilder builder : cascadedViolations.values()) {
      validationResultList.add(builder.build());
    }

    validationResultList.sort(RESULT_COMPARATOR);

    return new MethodValidationException(target, method, violations, validationResultList, forReturnValue);
  }

  /**
   * Create a {@link MessageSourceResolvable} for the given violation.
   *
   * @param target target of the method invocation to which validation was applied
   * @param parameter the method parameter associated with the violation
   * @param violation the violation
   * @return the created {@code MessageSourceResolvable}
   */
  private MessageSourceResolvable createMessageSourceResolvable(
          Object target, MethodParameter parameter, ConstraintViolation<Object> violation) {

    String objectName = Conventions.getVariableName(target) + "#" + parameter.getExecutable().getName();
    String paramName = parameter.getParameterName() != null ? parameter.getParameterName() : "";
    Class<?> parameterType = parameter.getParameterType();

    ConstraintDescriptor<?> descriptor = violation.getConstraintDescriptor();
    String code = descriptor.getAnnotation().annotationType().getSimpleName();
    String[] codes = messageCodesResolver.resolveMessageCodes(code, objectName, paramName, parameterType);
    Object[] arguments = validatorAdapter.getArgumentsForConstraint(objectName, paramName, descriptor);

    return new DefaultMessageSourceResolvable(codes, arguments, violation.getMessage());
  }

  /**
   * Select an object name and create a {@link BindingResult} for the argument.
   * You can configure a {@link #setBindingResultNameResolver(BindingResultNameResolver)
   * bindingResultNameResolver} to determine in a way that matches the specific
   * programming model, e.g. {@code @ModelAttribute} or {@code @RequestBody} arguments
   * in Infra MVC.
   * <p>By default, the name is based on the parameter name, or for a return type on
   * {@link Conventions#getVariableNameForReturnType(Method, Class, Object)}.
   * <p>If a name cannot be determined for any reason, e.g. a return value with
   * insufficient type information, then {@code "{methodName}.arg{index}"} is used.
   *
   * @param parameter the method parameter
   * @param argument the argument value
   * @return the determined name
   */
  private BindingResult createBindingResult(MethodParameter parameter, @Nullable Object argument) {
    String objectName = null;
    if (objectNameResolver != null) {
      objectName = objectNameResolver.resolveName(parameter, argument);
    }
    else {
      if (parameter.getParameterIndex() != -1) {
        objectName = parameter.getParameterName();
      }
      else {
        try {
          Method method = parameter.getMethod();
          if (method != null) {
            Class<?> containingClass = parameter.getContainingClass();
            Class<?> resolvedType = GenericTypeResolver.resolveReturnType(method, containingClass);
            objectName = Conventions.getVariableNameForReturnType(method, resolvedType, argument);
          }
        }
        catch (IllegalArgumentException ex) {
          // insufficient type information
        }
      }
    }
    if (objectName == null) {
      int index = parameter.getParameterIndex();
      objectName = (parameter.getExecutable().getName() + (index != -1 ? ".arg" + index : ""));
    }
    BeanPropertyBindingResult result = new BeanPropertyBindingResult(argument, objectName);
    result.setMessageCodesResolver(this.messageCodesResolver);
    return result;
  }

  /**
   * Contract to determine the object name of an {@code @Valid} method parameter.
   */
  public interface BindingResultNameResolver {

    /**
     * Determine the name for the given method parameter.
     *
     * @param parameter the method parameter
     * @param value the argument or return value
     * @return the name to use
     */
    String resolveName(MethodParameter parameter, @Nullable Object value);

  }

  /**
   * Builds a validation result for a value method parameter with constraints
   * declared directly on it.
   */
  private final class ValueResultBuilder {

    private final Object target;

    private final MethodParameter parameter;

    @Nullable
    private final Object argument;

    private final List<MessageSourceResolvable> resolvableErrors = new ArrayList<>();

    private final List<ConstraintViolation<Object>> violations = new ArrayList<>();

    public ValueResultBuilder(Object target, MethodParameter parameter, @Nullable Object argument) {
      this.target = target;
      this.parameter = parameter;
      this.argument = argument;
    }

    public void addViolation(ConstraintViolation<Object> violation) {
      this.resolvableErrors.add(createMessageSourceResolvable(this.target, this.parameter, violation));
      this.violations.add(violation);
    }

    public ParameterValidationResult build() {
      return new ParameterValidationResult(
              this.parameter, this.argument, this.resolvableErrors, this.violations);
    }

  }

  /**
   * Builds a validation result for an {@link jakarta.validation.Valid @Valid}
   * annotated bean method parameter with cascaded constraints.
   */
  private final class BeanResultBuilder {

    private final MethodParameter parameter;

    @Nullable
    private final Object argument;

    @Nullable
    private final Object container;

    @Nullable
    private final Integer containerIndex;

    @Nullable
    private final Object containerKey;

    private final Errors errors;

    private final Set<ConstraintViolation<Object>> violations = new LinkedHashSet<>();

    public BeanResultBuilder(MethodParameter parameter, @Nullable Object argument, Path.Node node) {
      this.parameter = parameter;

      this.containerIndex = node.getIndex();
      this.containerKey = node.getKey();
      if (argument instanceof List<?> list && this.containerIndex != null) {
        this.container = list;
        argument = list.get(this.containerIndex);
      }
      else if (argument instanceof Map<?, ?> map && this.containerKey != null) {
        this.container = map;
        argument = map.get(this.containerKey);
      }
      else {
        this.container = null;
      }

      this.argument = argument;
      this.errors = createBindingResult(parameter, argument);
    }

    public void addViolation(ConstraintViolation<Object> violation) {
      this.violations.add(violation);
    }

    public ParameterErrors build() {
      validatorAdapter.processConstraintViolations(this.violations, this.errors);
      return new ParameterErrors(
              this.parameter, this.argument, this.errors, this.violations,
              this.container, this.containerIndex, this.containerKey);
    }
  }

  /**
   * Comparator for validation results, sorted by method parameter index first,
   * also falling back on container indexes if necessary for cascaded
   * constraints on a List container.
   */
  private final static class ResultComparator implements Comparator<ParameterValidationResult> {

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

  /**
   * {@link MethodValidationResult} to use when there are no violations.
   */
  private static final class EmptyMethodValidationResult implements MethodValidationResult {

    @Override
    public Set<ConstraintViolation<?>> getConstraintViolations() {
      return Collections.emptySet();
    }

    @Override
    public List<ParameterValidationResult> getAllValidationResults() {
      return Collections.emptyList();
    }

    @Override
    public List<ParameterValidationResult> getValueResults() {
      return Collections.emptyList();
    }

    @Override
    public List<ParameterErrors> getBeanResults() {
      return Collections.emptyList();
    }

    @Override
    public void throwIfViolationsPresent() {
    }

    @Override
    public String toString() {
      return "MethodValidationResult (0 violations)";
    }

  }

}

