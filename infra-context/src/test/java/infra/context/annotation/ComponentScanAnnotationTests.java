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

package infra.context.annotation;

import org.junit.jupiter.api.Test;

import infra.beans.factory.support.DefaultBeanNameGenerator;
import infra.beans.testfixture.beans.TestBean;
import infra.context.annotation.ComponentScan.Filter;
import infra.core.type.filter.TypeFilter;

/**
 * Unit tests for the @ComponentScan annotation.
 *
 * @author Chris Beams
 * @see ComponentScanAnnotationIntegrationTests
 * @since 4.0
 */
public class ComponentScanAnnotationTests {

  @Test
  public void noop() {
    // no-op; the @ComponentScan-annotated MyConfig class below simply exercises
    // available attributes of the annotation.
  }
}

@interface MyAnnotation {
}

@Configuration
@ComponentScan(
        basePackageClasses = TestBean.class,
        nameGenerator = DefaultBeanNameGenerator.class,
//        scopedProxy = ScopedProxyMode.NO,
        scopeResolver = AnnotationScopeMetadataResolver.class,
        resourcePattern = "**/*custom.class",
        useDefaultFilters = false,
        includeFilters = {
                @Filter(type = FilterType.ANNOTATION, value = MyAnnotation.class)
        },
        excludeFilters = {
                @Filter(type = FilterType.CUSTOM, value = TypeFilter.class)
        },
        lazyInit = true
)
class MyConfig {
}

@ComponentScan(basePackageClasses = example.scannable.NamedComponent.class)
class SimpleConfig {
}
