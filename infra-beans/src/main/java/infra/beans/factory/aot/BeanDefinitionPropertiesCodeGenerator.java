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

package infra.beans.factory.aot;

import org.jspecify.annotations.Nullable;

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

import infra.aot.generate.GeneratedMethods;
import infra.aot.generate.ValueCodeGenerator;
import infra.aot.generate.ValueCodeGenerator.Delegate;
import infra.aot.hint.ExecutableMode;
import infra.aot.hint.MemberCategory;
import infra.aot.hint.RuntimeHints;
import infra.aot.hint.TypeReference;
import infra.beans.BeanUtils;
import infra.beans.PropertyValue;
import infra.beans.PropertyValues;
import infra.beans.factory.FactoryBean;
import infra.beans.factory.config.BeanDefinition;
import infra.beans.factory.config.ConstructorArgumentValues;
import infra.beans.factory.config.ConstructorArgumentValues.ValueHolder;
import infra.beans.factory.support.AbstractBeanDefinition;
import infra.beans.factory.support.AutowireCandidateQualifier;
import infra.beans.factory.support.InstanceSupplier;
import infra.beans.factory.support.LookupOverride;
import infra.beans.factory.support.MethodOverride;
import infra.beans.factory.support.ReplaceOverride;
import infra.beans.factory.support.RootBeanDefinition;
import infra.core.ReactiveStreams;
import infra.javapoet.CodeBlock;
import infra.javapoet.CodeBlock.Builder;
import infra.util.ClassUtils;
import infra.util.ObjectUtils;
import infra.util.ReflectionUtils;
import infra.util.StringUtils;

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

  private final ValueCodeGenerator valueCodeGenerator;

  BeanDefinitionPropertiesCodeGenerator(RuntimeHints hints,
          Predicate<String> attributeFilter, GeneratedMethods generatedMethods,
          List<Delegate> additionalDelegates, BiFunction<String, Object, CodeBlock> customValueCodeGenerator) {

    this.hints = hints;
    this.attributeFilter = attributeFilter;
    ArrayList<Delegate> customDelegates = new ArrayList<>();
    customDelegates.add((valueCodeGenerator, value) ->
            customValueCodeGenerator.apply(PropertyNamesStack.peek(), value));
    customDelegates.addAll(additionalDelegates);
    this.valueCodeGenerator = BeanDefinitionPropertyValueCodeGeneratorDelegates
            .createValueCodeGenerator(generatedMethods, customDelegates);
  }

  @SuppressWarnings("NullAway")
  CodeBlock generateCode(RootBeanDefinition beanDefinition) {
    CodeBlock.Builder code = CodeBlock.builder();

    addStatementForValue(code, beanDefinition, BeanDefinition::getScope, this::hasScope, "$L.setScope($S)");
    addStatementForValue(code, beanDefinition, AbstractBeanDefinition::isBackgroundInit, "$L.setBackgroundInit($L)");
    addStatementForValue(code, beanDefinition, AbstractBeanDefinition::getLazyInit, "$L.setLazyInit($L)");
    addStatementForValue(code, beanDefinition, BeanDefinition::getDependsOn, this::hasDependsOn, "$L.setDependsOn($L)", this::toStringVarArgs);
    addStatementForValue(code, beanDefinition, BeanDefinition::isAutowireCandidate, "$L.setAutowireCandidate($L)");
    addStatementForValue(code, beanDefinition, AbstractBeanDefinition::isDefaultCandidate, "$L.setDefaultCandidate($L)");
    addStatementForValue(code, beanDefinition, BeanDefinition::isPrimary, "$L.setPrimary($L)");
    addStatementForValue(code, beanDefinition, BeanDefinition::isFallback, "$L.setFallback($L)");
    addStatementForValue(code, beanDefinition, AbstractBeanDefinition::isSynthetic, "$L.setSynthetic($L)");
    addStatementForValue(code, beanDefinition, BeanDefinition::getRole, this::hasRole, "$L.setRole($L)", this::toRole);
    addStatementForValue(code, beanDefinition, BeanDefinition::isEnableDependencyInjection, "$L.setEnableDependencyInjection($L)");
    addInitDestroyMethods(code, beanDefinition, beanDefinition.getInitMethodNames(), "$L.setInitMethodNames($L)");
    addInitDestroyMethods(code, beanDefinition, beanDefinition.getDestroyMethodNames(), "$L.setDestroyMethodNames($L)");

    if (beanDefinition.getFactoryBeanName() != null) {
      addStatementForValue(code, beanDefinition, BeanDefinition::getFactoryBeanName,
              "$L.setFactoryBeanName(\"$L\")");
    }
    addConstructorArgumentValues(code, beanDefinition);
    addPropertyValues(code, beanDefinition);
    addAttributes(code, beanDefinition);
    addQualifiers(code, beanDefinition);
    addMethodOverrides(code, beanDefinition);
    return code.build();
  }

  private void addInitDestroyMethods(Builder code, AbstractBeanDefinition beanDefinition,
          String @Nullable [] methodNames, String format) {
    // For Publisher-based destroy methods
    hints.reflection().registerType(TypeReference.of(ReactiveStreams.INDICATOR_CLASS));
    if (ObjectUtils.isNotEmpty(methodNames)) {
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
          throw new IllegalStateException("Failed to load Class [%s] from ClassLoader [%s]"
                  .formatted(className, beanUserClass.getClassLoader()), ex);
        }
      }
    }

    Method method = ReflectionUtils.findMethod(methodDeclaringClass, methodName);
    if (method != null) {
      this.hints.reflection().registerMethod(method, ExecutableMode.INVOKE);
      Method publiclyAccessibleMethod = ReflectionUtils.getPubliclyAccessibleMethodIfPossible(method, beanUserClass);
      if (!publiclyAccessibleMethod.equals(method)) {
        this.hints.reflection().registerMethod(publiclyAccessibleMethod, ExecutableMode.INVOKE);
      }
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
      Class<?> infrastructureType = getInfrastructureType(beanDefinition);
      Map<String, Method> writeMethods = (infrastructureType != Object.class ?
              getWriteMethods(infrastructureType) : Collections.emptyMap());
      for (PropertyValue propertyValue : propertyValues) {
        String name = propertyValue.getName();
        CodeBlock valueCode = generateValue(name, propertyValue.getValue());
        code.addStatement("$L.getPropertyValues().add($S, $L)", BEAN_DEFINITION_VARIABLE, name, valueCode);
        Method writeMethod = writeMethods.get(name);
        if (writeMethod != null) {
          registerReflectionHints(beanDefinition, writeMethod);
        }
      }
    }
  }

  private void registerReflectionHints(RootBeanDefinition beanDefinition, Method writeMethod) {
    this.hints.reflection().registerMethod(writeMethod, ExecutableMode.INVOKE);
    // ReflectionUtils#findField searches recursively in the type hierarchy
    Class<?> searchType = beanDefinition.getTargetType();
    while (searchType != null && searchType != writeMethod.getDeclaringClass()) {
      this.hints.reflection().registerType(searchType, MemberCategory.ACCESS_DECLARED_FIELDS);
      searchType = searchType.getSuperclass();
    }
    this.hints.reflection().registerType(writeMethod.getDeclaringClass(), MemberCategory.ACCESS_DECLARED_FIELDS);
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

  private void addMethodOverrides(CodeBlock.Builder code, RootBeanDefinition beanDefinition) {
    if (beanDefinition.hasMethodOverrides()) {
      for (MethodOverride methodOverride : beanDefinition.getMethodOverrides().getOverrides()) {
        if (methodOverride instanceof LookupOverride lookupOverride) {
          ArrayList<CodeBlock> arguments = new ArrayList<>();
          arguments.add(CodeBlock.of("$S", lookupOverride.getMethodName()));
          arguments.add(CodeBlock.of("$S", lookupOverride.getBeanName()));
          code.addStatement("$L.getMethodOverrides().addOverride(new $T($L))", BEAN_DEFINITION_VARIABLE,
                  LookupOverride.class, CodeBlock.join(arguments, ", "));
        }
        else if (methodOverride instanceof ReplaceOverride replaceOverride) {
          ArrayList<CodeBlock> arguments = new ArrayList<>();
          arguments.add(CodeBlock.of("$S", replaceOverride.getMethodName()));
          arguments.add(CodeBlock.of("$S", replaceOverride.getMethodReplacerBeanName()));
          List<String> typeIdentifiers = replaceOverride.getTypeIdentifiers();
          if (!typeIdentifiers.isEmpty()) {
            arguments.add(CodeBlock.of("java.util.List.of($S)",
                    StringUtils.collectionToDelimitedString(typeIdentifiers, ", ")));
          }
          code.addStatement("$L.getMethodOverrides().addOverride(new $T($L))", BEAN_DEFINITION_VARIABLE,
                  ReplaceOverride.class, CodeBlock.join(arguments, ", "));
        }
        else {
          throw new UnsupportedOperationException("Unexpected MethodOverride subclass: " +
                  methodOverride.getClass().getName());
        }
      }
    }
  }

  private CodeBlock generateValue(@Nullable String name, @Nullable Object value) {
    PropertyNamesStack.push(name);
    try {
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
    return (StringUtils.hasText(actualValue) && !BeanDefinition.SCOPE_SINGLETON.equals(actualValue));
  }

  private boolean hasDependsOn(String[] defaultValue, String[] actualValue) {
    return ObjectUtils.isNotEmpty(actualValue);
  }

  private boolean hasRole(int defaultValue, int actualValue) {
    return actualValue != BeanDefinition.ROLE_APPLICATION;
  }

  private CodeBlock toStringVarArgs(String[] strings) {
    return Arrays.stream(strings).map(string -> CodeBlock.of("$S", string)).collect(CodeBlock.joining(","));
  }

  private Object toRole(int value) {
    return switch (value) {
      case BeanDefinition.ROLE_INFRASTRUCTURE -> CodeBlock.builder().add("$T.ROLE_INFRASTRUCTURE", BeanDefinition.class).build();
      case BeanDefinition.ROLE_SUPPORT -> CodeBlock.builder().add("$T.ROLE_SUPPORT", BeanDefinition.class).build();
      default -> value;
    };
  }

  private <B extends BeanDefinition, T> void addStatementForValue(
          CodeBlock.Builder code, BeanDefinition beanDefinition, Function<B, T> getter, String format) {

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
          CodeBlock.Builder code, BeanDefinition beanDefinition, Function<B, T> getter,
          BiPredicate<T, T> filter, String format, Function<T, Object> formatter) {

    T defaultValue = getter.apply((B) DEFAULT_BEAN_DEFINITION);
    T actualValue = getter.apply((B) beanDefinition);
    if (filter.test(defaultValue, actualValue)) {
      code.addStatement(format, BEAN_DEFINITION_VARIABLE, formatter.apply(actualValue));
    }
  }

  /**
   * Cast the specified {@code valueCode} to the specified {@code castType} if the
   * {@code castNecessary} is {@code true}. Otherwise, return the valueCode as-is.
   *
   * @param castNecessary whether a cast is necessary
   * @param castType the type to cast to
   * @param valueCode the code for the value
   * @return the existing value or a form of {@code (castType) valueCode} if a
   * cast is necessary
   */
  private CodeBlock castIfNecessary(boolean castNecessary, Class<?> castType, CodeBlock valueCode) {
    return (castNecessary ? CodeBlock.of("($T) $L", castType, valueCode) : valueCode);
  }

  static class PropertyNamesStack {

    private static final ThreadLocal<ArrayDeque<String>> threadLocal = ThreadLocal.withInitial(ArrayDeque::new);

    static void push(@Nullable String name) {
      String valueToSet = (name != null ? name : "");
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
