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

package cn.taketoday.annotation.config.processor;

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
@SupportedAnnotationTypes({ "cn.taketoday.annotation.config.processor.TestConditionalOnClass",
        "cn.taketoday.annotation.config.processor.TestConditionalOnBean",
        "cn.taketoday.annotation.config.processor.TestConditionalOnSingleCandidate",
        "cn.taketoday.annotation.config.processor.TestConditionalOnWebApplication",
        "cn.taketoday.annotation.config.processor.TestAutoConfigureBefore",
        "cn.taketoday.annotation.config.processor.TestAutoConfigureAfter",
        "cn.taketoday.annotation.config.processor.TestAutoConfigureOrder",
        "cn.taketoday.annotation.config.processor.TestAutoConfiguration" })
public class TestAnnotationConfigAnnotationProcessor extends AnnotationConfigAnnotationProcessor {

  public TestAnnotationConfigAnnotationProcessor() { }

  @Override
  protected List<PropertyGenerator> getPropertyGenerators() {
    List<PropertyGenerator> generators = new ArrayList<>();
    String annotationPackage = "cn.taketoday.annotation.config.processor";
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
