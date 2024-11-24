/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2021 All Rights Reserved.
 *
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER
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
