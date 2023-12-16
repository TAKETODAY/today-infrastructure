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
 * along with this program. If not, see [https://www.gnu.org/licenses/]
 */

package cn.taketoday.beans.factory.aot;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import cn.taketoday.aot.generate.GeneratedMethod;
import cn.taketoday.aot.generate.GeneratedMethods;
import cn.taketoday.aot.generate.ValueCodeGenerator;
import cn.taketoday.aot.generate.ValueCodeGenerator.Delegate;
import cn.taketoday.aot.generate.ValueCodeGeneratorDelegates;
import cn.taketoday.aot.generate.ValueCodeGeneratorDelegates.CollectionDelegate;
import cn.taketoday.aot.generate.ValueCodeGeneratorDelegates.MapDelegate;
import cn.taketoday.beans.factory.config.BeanReference;
import cn.taketoday.beans.factory.config.RuntimeBeanReference;
import cn.taketoday.beans.factory.config.TypedStringValue;
import cn.taketoday.beans.factory.support.ManagedList;
import cn.taketoday.beans.factory.support.ManagedMap;
import cn.taketoday.beans.factory.support.ManagedSet;
import cn.taketoday.javapoet.AnnotationSpec;
import cn.taketoday.javapoet.CodeBlock;

/**
 * Code generator {@link Delegate} for common bean definition property values.
 *
 * @author Stephane Nicoll
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
abstract class BeanDefinitionPropertyValueCodeGeneratorDelegates {

  /**
   * Return the {@link Delegate} implementations for common bean definition
   * property value types. These are:
   * <ul>
   * <li>{@link ManagedList},</li>
   * <li>{@link ManagedSet},</li>
   * <li>{@link ManagedMap},</li>
   * <li>{@link LinkedHashMap},</li>
   * <li>{@link BeanReference},</li>
   * <li>{@link TypedStringValue}.</li>
   * </ul>
   * When combined with {@linkplain ValueCodeGeneratorDelegates#INSTANCES the
   * delegates for common value types}, this should be added first as they have
   * special handling for list, set, and map.
   */
  public static final List<Delegate> INSTANCES = List.of(
          new ManagedListDelegate(),
          new ManagedSetDelegate(),
          new ManagedMapDelegate(),
          new LinkedHashMapDelegate(),
          new BeanReferenceDelegate(),
          new TypedStringValueDelegate()
  );

  /**
   * {@link Delegate} for {@link ManagedList} types.
   */
  private static class ManagedListDelegate extends CollectionDelegate<ManagedList<?>> {

    public ManagedListDelegate() {
      super(ManagedList.class, CodeBlock.of("new $T()", ManagedList.class));
    }
  }

  /**
   * {@link Delegate} for {@link ManagedSet} types.
   */
  private static class ManagedSetDelegate extends CollectionDelegate<ManagedSet<?>> {

    public ManagedSetDelegate() {
      super(ManagedSet.class, CodeBlock.of("new $T()", ManagedSet.class));
    }
  }

  /**
   * {@link Delegate} for {@link ManagedMap} types.
   */
  private static class ManagedMapDelegate implements Delegate {

    private static final CodeBlock EMPTY_RESULT = CodeBlock.of("$T.ofEntries()", ManagedMap.class);

    @Override
    public CodeBlock generateCode(ValueCodeGenerator valueCodeGenerator, Object value) {
      if (value instanceof ManagedMap<?, ?> managedMap) {
        return generateManagedMapCode(valueCodeGenerator, managedMap);
      }
      return null;
    }

    private <K, V> CodeBlock generateManagedMapCode(ValueCodeGenerator valueCodeGenerator,
            ManagedMap<K, V> managedMap) {
      if (managedMap.isEmpty()) {
        return EMPTY_RESULT;
      }
      CodeBlock.Builder code = CodeBlock.builder();
      code.add("$T.ofEntries(", ManagedMap.class);
      Iterator<Entry<K, V>> iterator = managedMap.entrySet().iterator();
      while (iterator.hasNext()) {
        Entry<?, ?> entry = iterator.next();
        code.add("$T.entry($L,$L)", Map.class,
                valueCodeGenerator.generateCode(entry.getKey()),
                valueCodeGenerator.generateCode(entry.getValue()));
        if (iterator.hasNext()) {
          code.add(", ");
        }
      }
      code.add(")");
      return code.build();
    }
  }

  /**
   * {@link Delegate} for {@link Map} types.
   */
  private static class LinkedHashMapDelegate extends MapDelegate {

    @Override
    protected CodeBlock generateMapCode(ValueCodeGenerator valueCodeGenerator, Map<?, ?> map) {
      GeneratedMethods generatedMethods = valueCodeGenerator.getGeneratedMethods();
      if (map instanceof LinkedHashMap<?, ?> && generatedMethods != null) {
        return generateLinkedHashMapCode(valueCodeGenerator, generatedMethods, map);
      }
      return super.generateMapCode(valueCodeGenerator, map);
    }

    private CodeBlock generateLinkedHashMapCode(ValueCodeGenerator valueCodeGenerator,
            GeneratedMethods generatedMethods, Map<?, ?> map) {

      GeneratedMethod generatedMethod = generatedMethods.add("getMap", method -> {
        method.addAnnotation(AnnotationSpec
                .builder(SuppressWarnings.class)
                .addMember("value", "{\"rawtypes\", \"unchecked\"}")
                .build());
        method.returns(Map.class);
        method.addStatement("$T map = new $T($L)", Map.class,
                LinkedHashMap.class, map.size());
        map.forEach((key, value) -> method.addStatement("map.put($L, $L)",
                valueCodeGenerator.generateCode(key),
                valueCodeGenerator.generateCode(value)));
        method.addStatement("return map");
      });
      return CodeBlock.of("$L()", generatedMethod.getName());
    }
  }

  /**
   * {@link Delegate} for {@link BeanReference} types.
   */
  private static class BeanReferenceDelegate implements Delegate {

    @Override
    public CodeBlock generateCode(ValueCodeGenerator valueCodeGenerator, Object value) {
      if (value instanceof RuntimeBeanReference runtimeBeanReference
              && runtimeBeanReference.getBeanType() != null) {
        return CodeBlock.of("new $T($T.class)", RuntimeBeanReference.class,
                runtimeBeanReference.getBeanType());
      }
      else if (value instanceof BeanReference beanReference) {
        return CodeBlock.of("new $T($S)", RuntimeBeanReference.class,
                beanReference.getBeanName());
      }
      return null;
    }
  }

  /**
   * {@link Delegate} for {@link TypedStringValue} types.
   */
  private static class TypedStringValueDelegate implements Delegate {

    @Override
    public CodeBlock generateCode(ValueCodeGenerator valueCodeGenerator, Object value) {
      if (value instanceof TypedStringValue typedStringValue) {
        return generateTypeStringValueCode(valueCodeGenerator, typedStringValue);
      }
      return null;
    }

    private CodeBlock generateTypeStringValueCode(ValueCodeGenerator valueCodeGenerator, TypedStringValue typedStringValue) {
      String value = typedStringValue.getValue();
      if (typedStringValue.hasTargetType()) {
        return CodeBlock.of("new $T($S, $L)", TypedStringValue.class, value,
                valueCodeGenerator.generateCode(typedStringValue.getTargetType()));
      }
      return valueCodeGenerator.generateCode(value);
    }
  }
}
