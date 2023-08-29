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

import java.util.function.BiConsumer;

import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.util.ElementFilter;

import cn.taketoday.context.properties.processor.test.ItemMetadataAssert;
import cn.taketoday.context.properties.processor.test.RoundEnvironmentTester;
import cn.taketoday.context.properties.processor.test.TestableAnnotationProcessor;
import cn.taketoday.core.test.tools.SourceFile;
import cn.taketoday.core.test.tools.TestCompiler;

/**
 * Base test infrastructure to test {@link PropertyDescriptor} implementations.
 *
 * @author Stephane Nicoll
 * @author Scott Frederick
 */
public abstract class PropertyDescriptorTests {

  protected String createAccessorMethodName(String prefix, String name) {
    char[] chars = name.toCharArray();
    chars[0] = Character.toUpperCase(chars[0]);
    return prefix + new String(chars, 0, chars.length);
  }

  protected ExecutableElement getMethod(TypeElement element, String name) {
    return ElementFilter.methodsIn(element.getEnclosedElements())
            .stream()
            .filter((method) -> ((Element) method).getSimpleName().toString().equals(name))
            .findFirst()
            .orElse(null);
  }

  protected VariableElement getField(TypeElement element, String name) {
    return ElementFilter.fieldsIn(element.getEnclosedElements())
            .stream()
            .filter((method) -> ((Element) method).getSimpleName().toString().equals(name))
            .findFirst()
            .orElse(null);
  }

  protected ItemMetadataAssert assertItemMetadata(MetadataGenerationEnvironment metadataEnv,
          PropertyDescriptor<?> property) {
    return new ItemMetadataAssert(property.resolveItemMetadata("test", metadataEnv));
  }

  protected void process(Class<?> target,
          BiConsumer<RoundEnvironmentTester, MetadataGenerationEnvironment> consumer) {
    TestableAnnotationProcessor<MetadataGenerationEnvironment> processor = new TestableAnnotationProcessor<>(
            consumer, new MetadataGenerationEnvironmentFactory());
    TestCompiler compiler = TestCompiler.forSystem()
            .withProcessors(processor)
            .withSources(SourceFile.forTestClass(target));
    compiler.compile((compiled) -> {
    });
  }

}
