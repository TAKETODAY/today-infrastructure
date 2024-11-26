/*
 * Copyright 2017 - 2024 the original author or authors.
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
