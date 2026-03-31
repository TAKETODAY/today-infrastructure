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

package infra.test.context.bean.override.convention;

import java.lang.reflect.AnnotatedElement;
import java.util.List;
import java.util.Optional;

import infra.aot.hint.ReflectionHints;
import infra.aot.hint.TypeReference;
import infra.aot.hint.annotation.ReflectiveProcessor;
import infra.core.annotation.MergedAnnotation;
import infra.core.annotation.MergedAnnotations;
import infra.lang.Assert;

import static infra.aot.hint.ExecutableMode.INVOKE;

/**
 * {@link ReflectiveProcessor} that processes {@link TestBean @TestBean} annotations.
 *
 * @author Sam Brannen
 * @since 5.0
 */
class TestBeanReflectiveProcessor implements ReflectiveProcessor {

  @Override
  public void registerReflectionHints(ReflectionHints hints, AnnotatedElement element) {
    Optional.ofNullable(MergedAnnotations.from(element)
                    .get(TestBean.class)
                    .synthesize(MergedAnnotation::isPresent))

            .map(TestBean::methodName)
            .filter(methodName -> methodName.contains("#"))
            .ifPresent(methodName -> {
              int indexOfHash = methodName.lastIndexOf('#');
              String className = methodName.substring(0, indexOfHash).trim();
              Assert.hasText(className, () -> "No class name present in fully-qualified method name: " + methodName);
              String methodNameToUse = methodName.substring(indexOfHash + 1).trim();
              Assert.hasText(methodNameToUse, () -> "No method name present in fully-qualified method name: " + methodName);
              hints.registerType(TypeReference.of(className), builder ->
                      builder.withMethod(methodNameToUse, List.of(), INVOKE));
            });
  }

}
