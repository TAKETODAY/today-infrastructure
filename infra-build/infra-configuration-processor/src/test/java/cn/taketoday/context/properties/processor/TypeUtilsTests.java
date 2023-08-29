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

package cn.taketoday.context.properties.processor;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.function.BiConsumer;

import cn.taketoday.context.properties.processor.test.RoundEnvironmentTester;
import cn.taketoday.context.properties.processor.test.TestableAnnotationProcessor;
import cn.taketoday.context.properties.sample.generic.AbstractGenericProperties;
import cn.taketoday.context.properties.sample.generic.AbstractIntermediateGenericProperties;
import cn.taketoday.context.properties.sample.generic.SimpleGenericProperties;
import cn.taketoday.core.test.tools.SourceFile;
import cn.taketoday.core.test.tools.TestCompiler;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link TypeUtils}.
 *
 * @author Stephane Nicoll
 * @author Scott Frederick
 */
class TypeUtilsTests {

  @Test
  void resolveTypeDescriptorOnConcreteClass() {
    process(SimpleGenericProperties.class, (roundEnv, typeUtils) -> {
      TypeUtils.TypeDescriptor typeDescriptor = typeUtils
              .resolveTypeDescriptor(roundEnv.getRootElement(SimpleGenericProperties.class));
      assertThat(typeDescriptor.getGenerics().keySet().stream().map(Object::toString)).containsOnly("A", "B",
              "C");
      assertThat(typeDescriptor.resolveGeneric("A")).hasToString(String.class.getName());
      assertThat(typeDescriptor.resolveGeneric("B")).hasToString(Integer.class.getName());
      assertThat(typeDescriptor.resolveGeneric("C")).hasToString(Duration.class.getName());

    });
  }

  @Test
  void resolveTypeDescriptorOnIntermediateClass() {
    process(AbstractIntermediateGenericProperties.class, (roundEnv, typeUtils) -> {
      TypeUtils.TypeDescriptor typeDescriptor = typeUtils
              .resolveTypeDescriptor(roundEnv.getRootElement(AbstractIntermediateGenericProperties.class));
      assertThat(typeDescriptor.getGenerics().keySet().stream().map(Object::toString)).containsOnly("A", "B",
              "C");
      assertThat(typeDescriptor.resolveGeneric("A")).hasToString(String.class.getName());
      assertThat(typeDescriptor.resolveGeneric("B")).hasToString(Integer.class.getName());
      assertThat(typeDescriptor.resolveGeneric("C")).hasToString("C");
    });
  }

  @Test
  void resolveTypeDescriptorWithOnlyGenerics() {
    process(AbstractGenericProperties.class, (roundEnv, typeUtils) -> {
      TypeUtils.TypeDescriptor typeDescriptor = typeUtils
              .resolveTypeDescriptor(roundEnv.getRootElement(AbstractGenericProperties.class));
      assertThat(typeDescriptor.getGenerics().keySet().stream().map(Object::toString)).containsOnly("A", "B",
              "C");

    });
  }

  private void process(Class<?> target, BiConsumer<RoundEnvironmentTester, TypeUtils> consumer) {
    TestableAnnotationProcessor<TypeUtils> processor = new TestableAnnotationProcessor<>(consumer, TypeUtils::new);
    TestCompiler compiler = TestCompiler.forSystem()
            .withProcessors(processor)
            .withSources(SourceFile.forTestClass(target));
    compiler.compile((compiled) -> {
    });
  }

}
