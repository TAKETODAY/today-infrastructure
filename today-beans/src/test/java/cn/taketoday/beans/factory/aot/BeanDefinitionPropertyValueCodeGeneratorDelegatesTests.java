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

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Supplier;

import javax.lang.model.element.Modifier;

import cn.taketoday.aot.generate.GeneratedClass;
import cn.taketoday.aot.generate.ValueCodeGenerator;
import cn.taketoday.aot.generate.ValueCodeGeneratorDelegates;
import cn.taketoday.aot.test.generate.TestGenerationContext;
import cn.taketoday.beans.factory.config.BeanReference;
import cn.taketoday.beans.factory.config.RuntimeBeanNameReference;
import cn.taketoday.beans.factory.config.RuntimeBeanReference;
import cn.taketoday.beans.factory.support.ManagedList;
import cn.taketoday.beans.factory.support.ManagedMap;
import cn.taketoday.beans.factory.support.ManagedSet;
import cn.taketoday.beans.testfixture.beans.factory.aot.DeferredTypeBuilder;
import cn.taketoday.core.ResolvableType;
import cn.taketoday.core.test.tools.Compiled;
import cn.taketoday.core.test.tools.TestCompiler;
import cn.taketoday.core.testfixture.aot.generate.value.EnumWithClassBody;
import cn.taketoday.core.testfixture.aot.generate.value.ExampleClass;
import cn.taketoday.core.testfixture.aot.generate.value.ExampleClass$$GeneratedBy;
import cn.taketoday.javapoet.CodeBlock;
import cn.taketoday.javapoet.MethodSpec;
import cn.taketoday.javapoet.ParameterizedTypeName;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link BeanDefinitionPropertyValueCodeGenerator}.
 *
 * @author Stephane Nicoll
 * @author Phillip Webb
 * @author Sebastien Deleuze
 * @see BeanDefinitionPropertyValueCodeGeneratorDelegatesTests
 * @since 4.0
 */
class BeanDefinitionPropertyValueCodeGeneratorDelegatesTests {

  private static ValueCodeGenerator createValueCodeGenerator(GeneratedClass generatedClass) {
    return ValueCodeGenerator.with(BeanDefinitionPropertyValueCodeGeneratorDelegates.INSTANCES)
            .add(ValueCodeGeneratorDelegates.INSTANCES)
            .scoped(generatedClass.getMethods());
  }

  private void compile(Object value, BiConsumer<Object, Compiled> result) {
    TestGenerationContext generationContext = new TestGenerationContext();
    DeferredTypeBuilder typeBuilder = new DeferredTypeBuilder();
    GeneratedClass generatedClass = generationContext.getGeneratedClasses().addForFeature("TestCode", typeBuilder);
    CodeBlock generatedCode = createValueCodeGenerator(generatedClass).generateCode(value);
    typeBuilder.set(type -> {
      type.addModifiers(Modifier.PUBLIC);
      type.addSuperinterface(
              ParameterizedTypeName.get(Supplier.class, Object.class));
      type.addMethod(MethodSpec.methodBuilder("get").addModifiers(Modifier.PUBLIC)
              .returns(Object.class).addStatement("return $L", generatedCode).build());
    });
    generationContext.writeGeneratedContent();
    TestCompiler.forSystem().with(generationContext).compile(compiled ->
            result.accept(compiled.getInstance(Supplier.class).get(), compiled));
  }

  @Nested
  class NullTests {

    @Test
    void generateWhenNull() {
      compile(null, (instance, compiled) -> assertThat(instance).isNull());
    }

  }

  @Nested
  class PrimitiveTests {

    @Test
    void generateWhenBoolean() {
      compile(true, (instance, compiled) ->
              assertThat(instance).isEqualTo(Boolean.TRUE));
    }

    @Test
    void generateWhenByte() {
      compile((byte) 2, (instance, compiled) ->
              assertThat(instance).isEqualTo((byte) 2));
    }

    @Test
    void generateWhenShort() {
      compile((short) 3, (instance, compiled) ->
              assertThat(instance).isEqualTo((short) 3));
    }

    @Test
    void generateWhenInt() {
      compile(4, (instance, compiled) ->
              assertThat(instance).isEqualTo(4));
    }

    @Test
    void generateWhenLong() {
      compile(5L, (instance, compiled) ->
              assertThat(instance).isEqualTo(5L));
    }

    @Test
    void generateWhenFloat() {
      compile(0.1F, (instance, compiled) ->
              assertThat(instance).isEqualTo(0.1F));
    }

    @Test
    void generateWhenDouble() {
      compile(0.2, (instance, compiled) ->
              assertThat(instance).isEqualTo(0.2));
    }

    @Test
    void generateWhenChar() {
      compile('a', (instance, compiled) ->
              assertThat(instance).isEqualTo('a'));
    }

    @Test
    void generateWhenSimpleEscapedCharReturnsEscaped() {
      testEscaped('\b');
      testEscaped('\t');
      testEscaped('\n');
      testEscaped('\f');
      testEscaped('\r');
      testEscaped('\"');
      testEscaped('\'');
      testEscaped('\\');
    }

    @Test
    void generatedWhenUnicodeEscapedCharReturnsEscaped() {
      testEscaped('\u007f');
    }

    private void testEscaped(char value) {
      compile(value, (instance, compiled) ->
              assertThat(instance).isEqualTo(value));
    }

  }

  @Nested
  class StringTests {

    @Test
    void generateWhenString() {
      compile("test\n", (instance, compiled) ->
              assertThat(instance).isEqualTo("test\n"));
    }

  }

  @Nested
  class CharsetTests {

    @Test
    void generateWhenCharset() {
      compile(StandardCharsets.UTF_8, (instance, compiled) ->
              assertThat(instance).isEqualTo(Charset.forName("UTF-8")));
    }

  }

  @Nested
  class EnumTests {

    @Test
    void generateWhenEnum() {
      compile(ChronoUnit.DAYS, (instance, compiled) ->
              assertThat(instance).isEqualTo(ChronoUnit.DAYS));
    }

    @Test
    void generateWhenEnumWithClassBody() {
      compile(EnumWithClassBody.TWO, (instance, compiled) ->
              assertThat(instance).isEqualTo(EnumWithClassBody.TWO));
    }

  }

  @Nested
  class ClassTests {

    @Test
    void generateWhenClass() {
      compile(InputStream.class, (instance, compiled) -> assertThat(instance)
              .isEqualTo(InputStream.class));
    }

    @Test
    void generateWhenCglibClass() {
      compile(ExampleClass$$GeneratedBy.class, (instance,
              compiled) -> assertThat(instance).isEqualTo(ExampleClass.class));
    }

  }

  @Nested
  class ResolvableTypeTests {

    @Test
    void generateWhenSimpleResolvableType() {
      ResolvableType resolvableType = ResolvableType.forClass(String.class);
      compile(resolvableType, (instance, compiled) -> assertThat(instance)
              .isEqualTo(resolvableType));
    }

    @Test
    void generateWhenNoneResolvableType() {
      ResolvableType resolvableType = ResolvableType.NONE;
      compile(resolvableType, (instance, compiled) ->
              assertThat(instance).isEqualTo(resolvableType));
    }

    @Test
    void generateWhenGenericResolvableType() {
      ResolvableType resolvableType = ResolvableType
              .forClassWithGenerics(List.class, String.class);
      compile(resolvableType, (instance, compiled) ->
              assertThat(instance).isEqualTo(resolvableType));
    }

    @Test
    void generateWhenNestedGenericResolvableType() {
      ResolvableType stringList = ResolvableType.forClassWithGenerics(List.class,
              String.class);
      ResolvableType resolvableType = ResolvableType.forClassWithGenerics(Map.class,
              ResolvableType.forClass(Integer.class), stringList);
      compile(resolvableType, (instance, compiled) -> assertThat(instance)
              .isEqualTo(resolvableType));
    }

  }

  @Nested
  class ArrayTests {

    @Test
    void generateWhenPrimitiveArray() {
      byte[] bytes = { 0, 1, 2 };
      compile(bytes, (instance, compiler) ->
              assertThat(instance).isEqualTo(bytes));
    }

    @Test
    void generateWhenWrapperArray() {
      Byte[] bytes = { 0, 1, 2 };
      compile(bytes, (instance, compiler) ->
              assertThat(instance).isEqualTo(bytes));
    }

    @Test
    void generateWhenClassArray() {
      Class<?>[] classes = new Class<?>[] { InputStream.class, OutputStream.class };
      compile(classes, (instance, compiler) ->
              assertThat(instance).isEqualTo(classes));
    }

  }

  @Nested
  class ManagedListTests {

    @Test
    void generateWhenStringManagedList() {
      ManagedList<String> list = new ManagedList<>();
      list.add("a");
      list.add("b");
      list.add("c");
      compile(list, (instance, compiler) -> assertThat(instance).isEqualTo(list)
              .isInstanceOf(ManagedList.class));
    }

    @Test
    void generateWhenEmptyManagedList() {
      ManagedList<String> list = new ManagedList<>();
      compile(list, (instance, compiler) -> assertThat(instance).isEqualTo(list)
              .isInstanceOf(ManagedList.class));
    }

  }

  @Nested
  class ManagedSetTests {

    @Test
    void generateWhenStringManagedSet() {
      ManagedSet<String> set = new ManagedSet<>();
      set.add("a");
      set.add("b");
      set.add("c");
      compile(set, (instance, compiler) -> assertThat(instance).isEqualTo(set)
              .isInstanceOf(ManagedSet.class));
    }

    @Test
    void generateWhenEmptyManagedSet() {
      ManagedSet<String> set = new ManagedSet<>();
      compile(set, (instance, compiler) -> assertThat(instance).isEqualTo(set)
              .isInstanceOf(ManagedSet.class));
    }

  }

  @Nested
  class ManagedMapTests {

    @Test
    void generateWhenManagedMap() {
      ManagedMap<String, String> map = new ManagedMap<>();
      map.put("k1", "v1");
      map.put("k2", "v2");
      compile(map, (instance, compiler) -> assertThat(instance).isEqualTo(map)
              .isInstanceOf(ManagedMap.class));
    }

    @Test
    void generateWhenEmptyManagedMap() {
      ManagedMap<String, String> map = new ManagedMap<>();
      compile(map, (instance, compiler) -> assertThat(instance).isEqualTo(map)
              .isInstanceOf(ManagedMap.class));
    }

  }

  @Nested
  class ListTests {

    @Test
    void generateWhenStringList() {
      List<String> list = List.of("a", "b", "c");
      compile(list, (instance, compiler) -> assertThat(instance).isEqualTo(list)
              .isNotInstanceOf(ManagedList.class));
    }

    @Test
    void generateWhenEmptyList() {
      List<String> list = List.of();
      compile(list, (instance, compiler) -> assertThat(instance).isEqualTo(list));
    }

  }

  @Nested
  class SetTests {

    @Test
    void generateWhenStringSet() {
      Set<String> set = Set.of("a", "b", "c");
      compile(set, (instance, compiler) -> assertThat(instance).isEqualTo(set)
              .isNotInstanceOf(ManagedSet.class));
    }

    @Test
    void generateWhenEmptySet() {
      Set<String> set = Set.of();
      compile(set, (instance, compiler) -> assertThat(instance).isEqualTo(set));
    }

    @Test
    void generateWhenLinkedHashSet() {
      Set<String> set = new LinkedHashSet<>(List.of("a", "b", "c"));
      compile(set, (instance, compiler) ->
              assertThat(instance).isEqualTo(set).isInstanceOf(LinkedHashSet.class));
    }

    @Test
    void generateWhenSetOfClass() {
      Set<Class<?>> set = Set.of(String.class, Integer.class, Long.class);
      compile(set, (instance, compiler) -> assertThat(instance).isEqualTo(set));
    }

  }

  @Nested
  class MapTests {

    @Test
    void generateWhenSmallMap() {
      Map<String, String> map = Map.of("k1", "v1", "k2", "v2");
      compile(map, (instance, compiler) ->
              assertThat(instance).isEqualTo(map));
    }

    @Test
    void generateWhenMapWithOverTenElements() {
      Map<String, String> map = new HashMap<>();
      for (int i = 1; i <= 11; i++) {
        map.put("k" + i, "v" + i);
      }
      compile(map, (instance, compiler) -> assertThat(instance).isEqualTo(map));
    }

    @Test
    void generateWhenLinkedHashMap() {
      Map<String, String> map = new LinkedHashMap<>();
      map.put("a", "A");
      map.put("b", "B");
      map.put("c", "C");
      compile(map, (instance, compiler) -> {
        assertThat(instance).isEqualTo(map).isInstanceOf(LinkedHashMap.class);
        assertThat(compiler.getSourceFile()).contains("getMap()");
      });
    }

  }

  @Nested
  class BeanReferenceTests {

    @Test
    void generatedWhenBeanNameReference() {
      RuntimeBeanNameReference beanReference = new RuntimeBeanNameReference("test");
      compile(beanReference, (instance, compiler) -> {
        RuntimeBeanReference actual = (RuntimeBeanReference) instance;
        assertThat(actual.getBeanName()).isEqualTo(beanReference.getBeanName());
      });
    }

    @Test
    void generatedWhenBeanReferenceByName() {
      RuntimeBeanReference beanReference = new RuntimeBeanReference("test");
      compile(beanReference, (instance, compiler) -> {
        RuntimeBeanReference actual = (RuntimeBeanReference) instance;
        assertThat(actual.getBeanName()).isEqualTo(beanReference.getBeanName());
        assertThat(actual.getBeanType()).isEqualTo(beanReference.getBeanType());
      });
    }

    @Test
    void generatedWhenBeanReferenceByType() {
      BeanReference beanReference = new RuntimeBeanReference(String.class);
      compile(beanReference, (instance, compiler) -> {
        RuntimeBeanReference actual = (RuntimeBeanReference) instance;
        assertThat(actual.getBeanType()).isEqualTo(String.class);
      });
    }

  }

}
