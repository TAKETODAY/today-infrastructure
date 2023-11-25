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

package cn.taketoday.beans.factory.aot;

import java.beans.PropertyDescriptor;
import java.lang.reflect.Method;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.BiPredicate;
import java.util.function.Function;
import java.util.function.Predicate;

import cn.taketoday.aot.generate.GeneratedMethods;
import cn.taketoday.aot.hint.ExecutableMode;
import cn.taketoday.aot.hint.MemberCategory;
import cn.taketoday.aot.hint.RuntimeHints;
import cn.taketoday.aot.hint.TypeReference;
import cn.taketoday.beans.BeanUtils;
import cn.taketoday.beans.PropertyValue;
import cn.taketoday.beans.PropertyValues;
import cn.taketoday.beans.factory.FactoryBean;
import cn.taketoday.beans.factory.config.BeanDefinition;
import cn.taketoday.beans.factory.config.ConstructorArgumentValues;
import cn.taketoday.beans.factory.config.ConstructorArgumentValues.ValueHolder;
import cn.taketoday.beans.factory.support.AbstractBeanDefinition;
import cn.taketoday.beans.factory.support.AutowireCandidateQualifier;
import cn.taketoday.beans.factory.support.InstanceSupplier;
import cn.taketoday.beans.factory.support.RootBeanDefinition;
import cn.taketoday.core.ReactiveStreams;
import cn.taketoday.javapoet.CodeBlock;
import cn.taketoday.javapoet.CodeBlock.Builder;
import cn.taketoday.lang.Nullable;
import cn.taketoday.util.ClassUtils;
import cn.taketoday.util.ObjectUtils;
import cn.taketoday.util.ReflectionUtils;
import cn.taketoday.util.StringUtils;

/**
 * Internal code generator to set {@link RootBeanDefinition} properties.
 *
 * <p>Generates code in the following form:<pre class="code">
 * beanDefinition.setPrimary(true);
 * beanDefinition.setScope(BeanDefinition.SCOPE_PROTOTYPE);
 * ...
 * </pre>
 *
 * <p>The generated code expects the following variables to be available:
 * <ul>
 * <li>{@code beanDefinition}: the {@link RootBeanDefinition} to configure</li>
 * </ul>
 *
 * <p>Note that this generator does <b>not</b> set the {@link InstanceSupplier}.
 *
 * @author Phillip Webb
 * @author Stephane Nicoll
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
class BeanDefinitionPropertiesCodeGenerator {

  private static final RootBeanDefinition DEFAULT_BEAN_DEFINITION = new RootBeanDefinition();

  private static final String BEAN_DEFINITION_VARIABLE = BeanRegistrationCodeFragments.BEAN_DEFINITION_VARIABLE;

  private final RuntimeHints hints;

  private final Predicate<String> attributeFilter;

  private final BeanDefinitionPropertyValueCodeGenerator valueCodeGenerator;

  BeanDefinitionPropertiesCodeGenerator(RuntimeHints hints,
          Predicate<String> attributeFilter, GeneratedMethods generatedMethods,
          BiFunction<String, Object, CodeBlock> customValueCodeGenerator) {

    this.hints = hints;
    this.attributeFilter = attributeFilter;
    this.valueCodeGenerator = new BeanDefinitionPropertyValueCodeGenerator(generatedMethods,
            (object, type) -> customValueCodeGenerator.apply(PropertyNamesStack.peek(), object));
  }

  CodeBlock generateCode(RootBeanDefinition beanDefinition) {
    CodeBlock.Builder code = CodeBlock.builder();
    addStatementForValue(code, beanDefinition, BeanDefinition::isPrimary,
            "$L.setPrimary($L)");
    addStatementForValue(code, beanDefinition, BeanDefinition::getScope,
            this::hasScope, "$L.setScope($S)");
    addStatementForValue(code, beanDefinition, BeanDefinition::getDependsOn,
            this::hasDependsOn, "$L.setDependsOn($L)", this::toStringVarArgs);
    addStatementForValue(code, beanDefinition, BeanDefinition::isAutowireCandidate,
            "$L.setAutowireCandidate($L)");
    addStatementForValue(code, beanDefinition, BeanDefinition::getRole,
            this::hasRole, "$L.setRole($L)", this::toRole);
    addStatementForValue(code, beanDefinition, AbstractBeanDefinition::getLazyInit,
            "$L.setLazyInit($L)");
    addStatementForValue(code, beanDefinition, AbstractBeanDefinition::isSynthetic,
            "$L.setSynthetic($L)");
    addStatementForValue(code, beanDefinition, BeanDefinition::isEnableDependencyInjection,
            "$L.setEnableDependencyInjection($L)");
    addInitDestroyMethods(code, beanDefinition, beanDefinition.getInitMethodNames(),
            "$L.setInitMethodNames($L)");
    addInitDestroyMethods(code, beanDefinition, beanDefinition.getDestroyMethodNames(),
            "$L.setDestroyMethodNames($L)");
    addConstructorArgumentValues(code, beanDefinition);
    addPropertyValues(code, beanDefinition);
    addAttributes(code, beanDefinition);
    addQualifiers(code, beanDefinition);
    return code.build();
  }

  private void addInitDestroyMethods(Builder code, AbstractBeanDefinition beanDefinition,
          @Nullable String[] methodNames, String format) {
    // For Publisher-based destroy methods
    hints.reflection().registerType(TypeReference.of(ReactiveStreams.INDICATOR_CLASS));
    if (!ObjectUtils.isEmpty(methodNames)) {
      Class<?> beanType = ClassUtils.getUserClass(beanDefinition.getResolvableType().toClass());
      Arrays.stream(methodNames).forEach(methodName -> addInitDestroyHint(beanType, methodName));
      CodeBlock arguments = Arrays.stream(methodNames)
              .map(name -> CodeBlock.of("$S", name))
              .collect(CodeBlock.joining(", "));
      code.addStatement(format, BEAN_DEFINITION_VARIABLE, arguments);
    }
  }

  private void addInitDestroyHint(Class<?> beanUserClass, String methodName) {
    Class<?> methodDeclaringClass = beanUserClass;

    // Parse fully-qualified method name if necessary.
    int indexOfDot = methodName.lastIndexOf('.');
    if (indexOfDot > 0) {
      String className = methodName.substring(0, indexOfDot);
      methodName = methodName.substring(indexOfDot + 1);
      if (!beanUserClass.getName().equals(className)) {
        try {
          methodDeclaringClass = ClassUtils.forName(className, beanUserClass.getClassLoader());
        }
        catch (Throwable ex) {
          throw new IllegalStateException("Failed to load Class [" + className +
                  "] from ClassLoader [" + beanUserClass.getClassLoader() + "]", ex);
        }
      }
    }

    Method method = ReflectionUtils.findMethod(methodDeclaringClass, methodName);
    if (method != null) {
      this.hints.reflection().registerMethod(method, ExecutableMode.INVOKE);
    }
  }

  private void addConstructorArgumentValues(CodeBlock.Builder code, BeanDefinition beanDefinition) {
    ConstructorArgumentValues constructorValues = beanDefinition.getConstructorArgumentValues();
    Map<Integer, ValueHolder> indexedValues = constructorValues.getIndexedArgumentValues();
    if (!indexedValues.isEmpty()) {
      indexedValues.forEach((index, valueHolder) -> {
        Object value = valueHolder.getValue();
        CodeBlock valueCode = castIfNecessary(value == null, Object.class,
                generateValue(valueHolder.getName(), value));
        code.addStatement(
                "$L.getConstructorArgumentValues().addIndexedArgumentValue($L, $L)",
                BEAN_DEFINITION_VARIABLE, index, valueCode);
      });
    }
    List<ValueHolder> genericValues = constructorValues.getGenericArgumentValues();
    if (!genericValues.isEmpty()) {
      genericValues.forEach(valueHolder -> {
        String valueName = valueHolder.getName();
        CodeBlock valueCode = generateValue(valueName, valueHolder.getValue());
        if (valueName != null) {
          CodeBlock valueTypeCode = this.valueCodeGenerator.generateCode(valueHolder.getType());
          code.addStatement(
                  "$L.getConstructorArgumentValues().addGenericArgumentValue(new $T($L, $L, $S))",
                  BEAN_DEFINITION_VARIABLE, ValueHolder.class, valueCode, valueTypeCode, valueName);
        }
        else if (valueHolder.getType() != null) {
          code.addStatement("$L.getConstructorArgumentValues().addGenericArgumentValue($L, $S)",
                  BEAN_DEFINITION_VARIABLE, valueCode, valueHolder.getType());

        }
        else {
          code.addStatement("$L.getConstructorArgumentValues().addGenericArgumentValue($L)",
                  BEAN_DEFINITION_VARIABLE, valueCode);
        }
      });
    }
  }

  private void addPropertyValues(CodeBlock.Builder code, RootBeanDefinition beanDefinition) {
    PropertyValues propertyValues = beanDefinition.getPropertyValues();
    if (!propertyValues.isEmpty()) {
      for (PropertyValue propertyValue : propertyValues) {
        String name = propertyValue.getName();
        CodeBlock valueCode = generateValue(name, propertyValue.getValue());
        code.addStatement("$L.getPropertyValues().add($S, $L)",
                BEAN_DEFINITION_VARIABLE, propertyValue.getName(), valueCode);
      }
      Class<?> infrastructureType = getInfrastructureType(beanDefinition);
      if (infrastructureType != Object.class) {
        Map<String, Method> writeMethods = getWriteMethods(infrastructureType);
        for (PropertyValue propertyValue : propertyValues) {
          Method writeMethod = writeMethods.get(propertyValue.getName());
          if (writeMethod != null) {
            this.hints.reflection().registerMethod(writeMethod, ExecutableMode.INVOKE);
            // ReflectionUtils#findField searches recursively in the type hierarchy
            Class<?> searchType = beanDefinition.getTargetType();
            while (searchType != null && searchType != writeMethod.getDeclaringClass()) {
              this.hints.reflection().registerType(searchType, MemberCategory.DECLARED_FIELDS);
              searchType = searchType.getSuperclass();
            }
            this.hints.reflection().registerType(writeMethod.getDeclaringClass(), MemberCategory.DECLARED_FIELDS);
          }
        }
      }
    }
  }

  private void addQualifiers(CodeBlock.Builder code, RootBeanDefinition beanDefinition) {
    Set<AutowireCandidateQualifier> qualifiers = beanDefinition.getQualifiers();
    if (!qualifiers.isEmpty()) {
      for (AutowireCandidateQualifier qualifier : qualifiers) {
        Collection<CodeBlock> arguments = new ArrayList<>();
        arguments.add(CodeBlock.of("$S", qualifier.getTypeName()));
        Object qualifierValue = qualifier.getAttribute(AutowireCandidateQualifier.VALUE_KEY);
        if (qualifierValue != null) {
          arguments.add(generateValue("value", qualifierValue));
        }
        code.addStatement("$L.addQualifier(new $T($L))", BEAN_DEFINITION_VARIABLE,
                AutowireCandidateQualifier.class, CodeBlock.join(arguments, ", "));
      }
    }
  }

  private CodeBlock generateValue(@Nullable String name, @Nullable Object value) {
    try {
      PropertyNamesStack.push(name);
      return this.valueCodeGenerator.generateCode(value);
    }
    finally {
      PropertyNamesStack.pop();
    }
  }

  private Class<?> getInfrastructureType(RootBeanDefinition beanDefinition) {
    if (beanDefinition.hasBeanClass()) {
      Class<?> beanClass = beanDefinition.getBeanClass();
      if (FactoryBean.class.isAssignableFrom(beanClass)) {
        return beanClass;
      }
    }
    return ClassUtils.getUserClass(beanDefinition.getResolvableType().toClass());
  }

  private Map<String, Method> getWriteMethods(Class<?> clazz) {
    Map<String, Method> writeMethods = new HashMap<>();
    for (PropertyDescriptor propertyDescriptor : BeanUtils.getPropertyDescriptors(clazz)) {
      writeMethods.put(propertyDescriptor.getName(), propertyDescriptor.getWriteMethod());
    }
    return Collections.unmodifiableMap(writeMethods);
  }

  private void addAttributes(CodeBlock.Builder code, BeanDefinition beanDefinition) {
    String[] attributeNames = beanDefinition.getAttributeNames();
    if (ObjectUtils.isNotEmpty(attributeNames)) {
      for (String attributeName : attributeNames) {
        if (attributeFilter.test(attributeName)) {
          CodeBlock value = valueCodeGenerator.generateCode(beanDefinition.getAttribute(attributeName));
          code.addStatement("$L.setAttribute($S, $L)",
                  BEAN_DEFINITION_VARIABLE, attributeName, value);
        }
      }
    }
  }

  private boolean hasScope(String defaultValue, String actualValue) {
    return StringUtils.hasText(actualValue)
            && !BeanDefinition.SCOPE_SINGLETON.equals(actualValue);
  }

  private boolean hasDependsOn(String[] defaultValue, String[] actualValue) {
    return ObjectUtils.isNotEmpty(actualValue);
  }

  private boolean hasRole(int defaultValue, int actualValue) {
    return actualValue != BeanDefinition.ROLE_APPLICATION;
  }

  private CodeBlock toStringVarArgs(String[] strings) {
    return Arrays.stream(strings)
            .map(string -> CodeBlock.of("$S", string))
            .collect(CodeBlock.joining(","));
  }

  private Object toRole(int value) {
    return switch (value) {
      case BeanDefinition.ROLE_INFRASTRUCTURE -> CodeBlock.builder().add("$T.ROLE_INFRASTRUCTURE", BeanDefinition.class).build();
      case BeanDefinition.ROLE_SUPPORT -> CodeBlock.builder().add("$T.ROLE_SUPPORT", BeanDefinition.class).build();
      default -> value;
    };
  }

  private <B extends BeanDefinition, T> void addStatementForValue(
          CodeBlock.Builder code, BeanDefinition beanDefinition,
          Function<B, T> getter, String format) {

    addStatementForValue(code, beanDefinition, getter,
            (defaultValue, actualValue) -> !Objects.equals(defaultValue, actualValue), format);
  }

  private <B extends BeanDefinition, T> void addStatementForValue(
          CodeBlock.Builder code, BeanDefinition beanDefinition,
          Function<B, T> getter, BiPredicate<T, T> filter, String format) {

    addStatementForValue(code, beanDefinition, getter, filter, format, actualValue -> actualValue);
  }

  @SuppressWarnings("unchecked")
  private <B extends BeanDefinition, T> void addStatementForValue(
          CodeBlock.Builder code, BeanDefinition beanDefinition,
          Function<B, T> getter, BiPredicate<T, T> filter, String format,
          Function<T, Object> formatter) {

    T defaultValue = getter.apply((B) DEFAULT_BEAN_DEFINITION);
    T actualValue = getter.apply((B) beanDefinition);
    if (filter.test(defaultValue, actualValue)) {
      code.addStatement(format, BEAN_DEFINITION_VARIABLE, formatter.apply(actualValue));
    }
  }

  /**
   * Cast the specified {@code valueCode} to the specified {@code castType} if
   * the {@code castNecessary} is {@code true}. Otherwise return the valueCode
   * as is.
   *
   * @param castNecessary whether a cast is necessary
   * @param castType the type to cast to
   * @param valueCode the code for the value
   * @return the existing value or a form of {@code (CastType) valueCode} if a
   * cast is necessary
   */
  private CodeBlock castIfNecessary(boolean castNecessary, Class<?> castType, CodeBlock valueCode) {
    return (castNecessary ? CodeBlock.of("($T) $L", castType, valueCode) : valueCode);
  }

  static class PropertyNamesStack {

    private static final ThreadLocal<ArrayDeque<String>> threadLocal = ThreadLocal.withInitial(ArrayDeque::new);

    static void push(@Nullable String name) {
      String valueToSet = name != null ? name : "";
      threadLocal.get().push(valueToSet);
    }

    static void pop() {
      threadLocal.get().pop();
    }

    @Nullable
    static String peek() {
      String value = threadLocal.get().peek();
      return ("".equals(value) ? null : value);
    }

  }

}
