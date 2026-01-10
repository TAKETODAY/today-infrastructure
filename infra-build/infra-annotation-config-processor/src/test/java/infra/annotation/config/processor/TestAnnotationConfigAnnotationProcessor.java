/*
 * Copyright 2017 - 2026 the TODAY authors.
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

package infra.annotation.config.processor;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.processing.SupportedAnnotationTypes;

/**
 * Version of {@link AnnotationConfigAnnotationProcessor} used for testing.
 *
 * @author Madhura Bhave
 * @author Scott Frederick
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 */
@SupportedAnnotationTypes({ "infra.annotation.config.processor.TestConditionalOnClass",
        "infra.annotation.config.processor.TestConditionalOnBean",
        "infra.annotation.config.processor.TestConditionalOnSingleCandidate",
        "infra.annotation.config.processor.TestConditionalOnWebApplication",
        "infra.annotation.config.processor.TestAutoConfigureBefore",
        "infra.annotation.config.processor.TestAutoConfigureAfter",
        "infra.annotation.config.processor.TestAutoConfigureOrder",
        "infra.annotation.config.processor.TestAutoConfiguration" })
public class TestAnnotationConfigAnnotationProcessor extends AnnotationConfigAnnotationProcessor {

  public TestAnnotationConfigAnnotationProcessor() { }

  @Override
  protected List<PropertyGenerator> getPropertyGenerators() {
    List<PropertyGenerator> generators = new ArrayList<>();
    String annotationPackage = "infra.annotation.config.processor";
    generators.add(PropertyGenerator.of(annotationPackage, "ConditionalOnClass")
            .withAnnotation("TestConditionalOnClass", new OnClassConditionValueExtractor()));
    generators.add(PropertyGenerator.of(annotationPackage, "ConditionalOnBean")
            .withAnnotation("TestConditionalOnBean", new OnBeanConditionValueExtractor()));
    generators.add(PropertyGenerator.of(annotationPackage, "ConditionalOnSingleCandidate")
            .withAnnotation("TestConditionalOnSingleCandidate", new OnBeanConditionValueExtractor()));
    generators.add(PropertyGenerator.of(annotationPackage, "ConditionalOnWebApplication")
            .withAnnotation("TestConditionalOnWebApplication", ValueExtractor.allFrom("type")));
    generators.add(PropertyGenerator.of(annotationPackage, "AutoConfigureBefore", true)
            .withAnnotation("TestAutoConfigureBefore", ValueExtractor.allFrom("value", "name"))
            .withAnnotation("TestAutoConfiguration", ValueExtractor.allFrom("before", "beforeName")));
    generators.add(PropertyGenerator.of(annotationPackage, "AutoConfigureAfter", true)
            .withAnnotation("TestAutoConfigureAfter", ValueExtractor.allFrom("value", "name"))
            .withAnnotation("TestAutoConfiguration", ValueExtractor.allFrom("after", "afterName")));
    generators.add(PropertyGenerator.of(annotationPackage, "AutoConfigureOrder")
            .withAnnotation("TestAutoConfigureOrder", ValueExtractor.allFrom("value")));
    return generators;
  }

}
