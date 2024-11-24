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

package infra.context.testfixture.context.annotation;

import infra.context.EnvironmentAware;
import infra.context.annotation.Configuration;
import infra.context.annotation.ImportAware;
import infra.core.env.Environment;
import infra.core.type.AnnotationMetadata;

@Configuration(proxyBeanMethods = false)
@SuppressWarnings("unused")
public class ImportAwareConfiguration implements ImportAware, EnvironmentAware {

  private AnnotationMetadata annotationMetadata;

  @Override
  public void setImportMetadata(AnnotationMetadata importMetadata) {
    this.annotationMetadata = importMetadata;
  }

  @Override
  public void setEnvironment(Environment environment) {

  }

}
