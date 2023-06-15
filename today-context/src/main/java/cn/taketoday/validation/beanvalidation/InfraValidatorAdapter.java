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

import java.io.Serial;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import cn.taketoday.beans.NotReadablePropertyException;
import cn.taketoday.context.MessageSourceResolvable;
import cn.taketoday.context.support.DefaultMessageSourceResolvable;
import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;
import cn.taketoday.util.ClassUtils;
import cn.taketoday.util.StringUtils;
import cn.taketoday.validation.BindingResult;
import cn.taketoday.validation.Errors;
import cn.taketoday.validation.FieldError;
import cn.taketoday.validation.ObjectError;
import cn.taketoday.validation.SmartValidator;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ElementKind;
import jakarta.validation.Path;
import jakarta.validation.ValidationException;
import jakarta.validation.Validator;
import jakarta.validation.executable.ExecutableValidator;
import jakarta.validation.metadata.BeanDescriptor;
import jakarta.validation.metadata.ConstraintDescriptor;

/**
 * Adapter that takes a JSR-303 {@code javax.validator.Validator} and
 * exposes it as a Framework {@link cn.taketoday.validation.Validator}
 * while also exposing the original JSR-303 Validator interface itself.
 *
 * <p>Can be used as a programmatic wrapper. Also serves as base class for
 * {@link CustomValidatorBean} and {@link LocalValidatorFactoryBean},
 * and as the primary implementation of the {@link SmartValidator} interface.
 *
 * <p>this adapter is fully compatible with
 * Bean Validation 1.1 as well as 2.0.
 *
 * @author Juergen Hoeller
 * @author Sam Brannen
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see SmartValidator
 * @see CustomValidatorBean
 * @see LocalValidatorFactoryBean
 * @since 4.0
 */
public class InfraValidatorAdapter implements SmartValidator, jakarta.validation.Validator {

  private static final Set<String> internalAnnotationAttributes = Set.of("message", "groups", "payload");

  @Nullable
  private Validator targetValidator;

  /**
   * Create a new ContextValidatorAdapter for the given JSR-303 Validator.
   *
   * @param targetValidator the JSR-303 Validator to wrap
   */
  public InfraValidatorAdapter(jakarta.validation.Validator targetValidator) {
    Assert.notNull(targetValidator, "Target Validator must not be null");
    this.targetValidator = targetValidator;
  }

  InfraValidatorAdapter() { }

  void setTargetValidator(jakarta.validation.Validator targetValidator) {
    this.targetValidator = targetValidator;
  }

  //---------------------------------------------------------------------
  // Implementation of Framework Validator interface
  //---------------------------------------------------------------------

  @Override
  public boolean supports(Class<?> clazz) {
    return (this.targetValidator != null);
  }

  @Override
  public void validate(Object target, Errors errors) {
    if (this.targetValidator != null) {
      processConstraintViolations(this.targetValidator.validate(target), errors);
    }
  }

  @Override
  public void validate(Object target, Errors errors, Object... validationHints) {
    if (this.targetValidator != null) {
      processConstraintViolations(
              this.targetValidator.validate(target, asValidationGroups(validationHints)), errors);
    }
  }

  @SuppressWarnings({ "unchecked", "rawtypes" })
  @Override
  public void validateValue(Class<?> targetType, String fieldName,
          @Nullable Object value, Errors errors, Object... validationHints) {

    if (this.targetValidator != null) {
      processConstraintViolations(this.targetValidator.validateValue(
              (Class) targetType, fieldName, value, asValidationGroups(validationHints)), errors);
    }
  }

  /**
   * Turn the specified validation hints into JSR-303 validation groups.
   */
  private Class<?>[] asValidationGroups(Object... validationHints) {
    Set<Class<?>> groups = new LinkedHashSet<>(4);
    for (Object hint : validationHints) {
      if (hint instanceof Class<?> clazz) {
        groups.add(clazz);
      }
    }
    return ClassUtils.toClassArray(groups);
  }

  /**
   * Process the given JSR-303 ConstraintViolations, adding corresponding errors to
   * the provided Framework {@link Errors} object.
   *
   * @param violations the JSR-303 ConstraintViolation results
   * @param errors the Framework errors object to register to
   */
  protected void processConstraintViolations(Set<ConstraintViolation<Object>> violations, Errors errors) {
    for (ConstraintViolation<Object> violation : violations) {
      String field = determineField(violation);
      FieldError fieldError = errors.getFieldError(field);
      if (fieldError == null || !fieldError.isBindingFailure()) {
        try {
          ConstraintDescriptor<?> cd = violation.getConstraintDescriptor();
          String errorCode = determineErrorCode(cd);
          Object[] errorArgs = getArgumentsForConstraint(errors.getObjectName(), field, cd);
          if (errors instanceof BindingResult bindingResult) {
            // Can do custom FieldError registration with invalid value from ConstraintViolation,
            // as necessary for Hibernate Validator compatibility (non-indexed set path in field)
            String nestedField = bindingResult.getNestedPath() + field;
            if (nestedField.isEmpty()) {
              String[] errorCodes = bindingResult.resolveMessageCodes(errorCode);
              ObjectError error = new ViolationObjectError(
                      errors.getObjectName(), errorCodes, errorArgs, violation, this);
              bindingResult.addError(error);
            }
            else {
              Object rejectedValue = getRejectedValue(field, violation, bindingResult);
              String[] errorCodes = bindingResult.resolveMessageCodes(errorCode, field);
              FieldError error = new ViolationFieldError(errors.getObjectName(), nestedField,
                      rejectedValue, errorCodes, errorArgs, violation, this);
              bindingResult.addError(error);
            }
          }
          else {
            // Got no BindingResult - can only do standard rejectValue call
            // with automatic extraction of the current field value
            errors.rejectValue(field, errorCode, errorArgs, violation.getMessage());
          }
        }
        catch (NotReadablePropertyException ex) {
          throw new IllegalStateException("JSR-303 validated property '" + field +
                  "' does not have a corresponding accessor for Framework data binding - " +
                  "check your DataBinder's configuration (bean property versus direct field access)", ex);
        }
      }
    }
  }

  /**
   * Determine a field for the given constraint violation.
   * <p>The default implementation returns the stringified property path.
   *
   * @param violation the current JSR-303 ConstraintViolation
   * @return the Framework-reported field (for use with {@link Errors})
   * @see ConstraintViolation#getPropertyPath()
   * @see FieldError#getField()
   */
  protected String determineField(ConstraintViolation<Object> violation) {
    Path path = violation.getPropertyPath();
    StringBuilder sb = new StringBuilder();
    boolean first = true;
    for (Path.Node node : path) {
      if (node.isInIterable() && !first) {
        sb.append('[');
        Object index = node.getIndex();
        if (index == null) {
          index = node.getKey();
        }
        if (index != null) {
          sb.append(index);
        }
        sb.append(']');
      }
      String name = node.getName();
      if (name != null && node.getKind() == ElementKind.PROPERTY && !name.startsWith("<")) {
        if (!first) {
          sb.append('.');
        }
        first = false;
        sb.append(name);
      }
    }
    return sb.toString();
  }

  /**
   * Determine a Framework-reported error code for the given constraint descriptor.
   * <p>The default implementation returns the simple class name of the descriptor's
   * annotation type. Note that the configured
   * {@link cn.taketoday.validation.MessageCodesResolver} will automatically
   * generate error code variations which include the object name and the field name.
   *
   * @param descriptor the JSR-303 ConstraintDescriptor for the current violation
   * @return a corresponding error code (for use with {@link Errors})
   * @see ConstraintDescriptor#getAnnotation()
   * @see cn.taketoday.validation.MessageCodesResolver
   */
  protected String determineErrorCode(ConstraintDescriptor<?> descriptor) {
    return descriptor.getAnnotation().annotationType().getSimpleName();
  }

  /**
   * Return FieldError arguments for a validation error on the given field.
   * Invoked for each violated constraint.
   * <p>The default implementation returns a first argument indicating the field name
   * (see {@link #getResolvableField}). Afterwards, it adds all actual constraint
   * annotation attributes (i.e. excluding "message", "groups" and "payload") in
   * alphabetical order of their attribute names.
   * <p>Can be overridden to e.g. add further attributes from the constraint descriptor.
   *
   * @param objectName the name of the target object
   * @param field the field that caused the binding error
   * @param descriptor the JSR-303 constraint descriptor
   * @return the Object array that represents the FieldError arguments
   * @see FieldError#getArguments
   * @see cn.taketoday.context.support.DefaultMessageSourceResolvable
   * @see cn.taketoday.validation.DefaultBindingErrorProcessor#getArgumentsForBindError
   */
  protected Object[] getArgumentsForConstraint(
          String objectName, String field, ConstraintDescriptor<?> descriptor) {
    ArrayList<Object> arguments = new ArrayList<>();
    arguments.add(getResolvableField(objectName, field));
    // Using a TreeMap for alphabetical ordering of attribute names
    TreeMap<String, Object> attributesToExpose = new TreeMap<>();

    for (Map.Entry<String, Object> entry : descriptor.getAttributes().entrySet()) {
      String attributeName = entry.getKey();
      Object attributeValue = entry.getValue();
      if (!internalAnnotationAttributes.contains(attributeName)) {
        if (attributeValue instanceof String str) {
          attributeValue = new ResolvableAttribute(str);
        }
        attributesToExpose.put(attributeName, attributeValue);
      }
    }

    arguments.addAll(attributesToExpose.values());
    return arguments.toArray();
  }

  /**
   * Build a resolvable wrapper for the specified field, allowing to resolve the field's
   * name in a {@code MessageSource}.
   * <p>The default implementation returns a first argument indicating the field:
   * of type {@code DefaultMessageSourceResolvable}, with "objectName.field" and "field"
   * as codes, and with the plain field name as default message.
   *
   * @param objectName the name of the target object
   * @param field the field that caused the binding error
   * @return a corresponding {@code MessageSourceResolvable} for the specified field
   * @see #getArgumentsForConstraint
   */
  protected MessageSourceResolvable getResolvableField(String objectName, String field) {
    String[] codes = StringUtils.hasText(field)
                     ? new String[] { objectName + Errors.NESTED_PATH_SEPARATOR + field, field }
                     : new String[] { objectName };
    return new DefaultMessageSourceResolvable(codes, field);
  }

  /**
   * Extract the rejected value behind the given constraint violation,
   * for exposure through the Framework errors representation.
   *
   * @param field the field that caused the binding error
   * @param violation the corresponding JSR-303 ConstraintViolation
   * @param bindingResult a Framework BindingResult for the backing object
   * which contains the current field's value
   * @return the invalid value to expose as part of the field error
   * @see ConstraintViolation#getInvalidValue()
   * @see FieldError#getRejectedValue()
   */
  @Nullable
  protected Object getRejectedValue(
          String field, ConstraintViolation<Object> violation, BindingResult bindingResult) {
    Object invalidValue = violation.getInvalidValue();
    if (!field.isEmpty() && !field.contains("[]") &&
            (invalidValue == violation.getLeafBean() || field.contains("[") || field.contains("."))) {
      // Possibly a bean constraint with property path: retrieve the actual property value.
      // However, explicitly avoid this for "address[]" style paths that we can't handle.
      invalidValue = bindingResult.getRawFieldValue(field);
    }
    return invalidValue;
  }

  /**
   * Indicate whether this violation's interpolated message has remaining
   * placeholders and therefore requires {@link java.text.MessageFormat}
   * to be applied to it. Called for a Bean Validation defined message
   * (coming out {@code ValidationMessages.properties}) when rendered
   * as the default message in Framework's MessageSource.
   * <p>The default implementation considers a Framework-style "{0}" placeholder
   * for the field name as an indication for {@link java.text.MessageFormat}.
   * Any other placeholder or escape syntax occurrences are typically a
   * mismatch, coming out of regex pattern values or the like. Note that
   * standard Bean Validation does not support "{0}" style placeholders at all;
   * this is a feature typically used in Framework MessageSource resource bundles.
   *
   * @param violation the Bean Validation constraint violation, including
   * BV-defined interpolation of named attribute references in its message
   * @return {@code true} if {@code java.text.MessageFormat} is to be applied,
   * or {@code false} if the violation's message should be used as-is
   * @see #getArgumentsForConstraint
   */
  protected boolean requiresMessageFormat(ConstraintViolation<?> violation) {
    return containsInfraStylePlaceholder(violation.getMessage());
  }

  private static boolean containsInfraStylePlaceholder(@Nullable String message) {
    return message != null && message.contains("{0}");
  }

  //---------------------------------------------------------------------
  // Implementation of JSR-303 Validator interface
  //---------------------------------------------------------------------

  @Override
  public <T> Set<ConstraintViolation<T>> validate(T object, Class<?>... groups) {
    return targetValidator().validate(object, groups);
  }

  @Override
  public <T> Set<ConstraintViolation<T>> validateProperty(T object, String propertyName, Class<?>... groups) {
    return targetValidator().validateProperty(object, propertyName, groups);
  }

  @Override
  public <T> Set<ConstraintViolation<T>> validateValue(
          Class<T> beanType, String propertyName, Object value, Class<?>... groups) {

    return targetValidator().validateValue(beanType, propertyName, value, groups);
  }

  @Override
  public BeanDescriptor getConstraintsForClass(Class<?> clazz) {
    return targetValidator().getConstraintsForClass(clazz);
  }

  @Override
  @SuppressWarnings("unchecked")
  public <T> T unwrap(@Nullable Class<T> type) {
    Validator targetValidator = targetValidator();
    try {
      return type != null ? targetValidator.unwrap(type) : (T) targetValidator;
    }
    catch (ValidationException ex) {
      // Ignore if just being asked for plain JSR-303 Validator
      if (jakarta.validation.Validator.class == type) {
        return (T) targetValidator;
      }
      throw ex;
    }
  }

  @Override
  public ExecutableValidator forExecutables() {
    return targetValidator().forExecutables();
  }

  private Validator targetValidator() {
    Validator validator = targetValidator;
    Assert.state(validator != null, "No target Validator set");
    return validator;
  }

  /**
   * Wrapper for a String attribute which can be resolved via a {@code MessageSource},
   * falling back to the original attribute as a default value otherwise.
   */
  private record ResolvableAttribute(String resolvableString) implements MessageSourceResolvable, Serializable {

    @Override
    public String[] getCodes() {
      return new String[] { this.resolvableString };
    }

    @Override
    public String getDefaultMessage() {
      return this.resolvableString;
    }

    @Override
    public String toString() {
      return this.resolvableString;
    }
  }

  /**
   * Subclass of {@code ObjectError} with Infra-style default message rendering.
   */
  private static class ViolationObjectError extends ObjectError implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Nullable
    private final transient InfraValidatorAdapter adapter;

    @Nullable
    private final transient ConstraintViolation<?> violation;

    public ViolationObjectError(String objectName, String[] codes, Object[] arguments,
            ConstraintViolation<?> violation, InfraValidatorAdapter adapter) {

      super(objectName, codes, arguments, violation.getMessage());
      this.adapter = adapter;
      this.violation = violation;
      wrap(violation);
    }

    @Override
    public boolean shouldRenderDefaultMessage() {
      return (this.adapter != null && this.violation != null ?
              this.adapter.requiresMessageFormat(this.violation) :
              containsInfraStylePlaceholder(getDefaultMessage()));
    }
  }

  /**
   * Subclass of {@code FieldError} with Framework-style default message rendering.
   */
  private static class ViolationFieldError extends FieldError implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Nullable
    private final transient InfraValidatorAdapter adapter;

    @Nullable
    private final transient ConstraintViolation<?> violation;

    public ViolationFieldError(String objectName, String field,
            @Nullable Object rejectedValue, String[] codes,
            Object[] arguments, ConstraintViolation<?> violation, InfraValidatorAdapter adapter) {

      super(objectName, field, rejectedValue, false, codes, arguments, violation.getMessage());
      this.adapter = adapter;
      this.violation = violation;
      wrap(violation);
    }

    @Override
    public boolean shouldRenderDefaultMessage() {
      return this.adapter != null && this.violation != null
             ? this.adapter.requiresMessageFormat(this.violation)
             : containsInfraStylePlaceholder(getDefaultMessage());
    }
  }

}
