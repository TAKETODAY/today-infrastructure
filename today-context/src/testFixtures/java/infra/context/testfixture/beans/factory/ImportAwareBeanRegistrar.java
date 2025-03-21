/*
 * Copyright 2017 - 2025 the original author or authors.
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

package infra.context.testfixture.beans.factory;

import infra.beans.factory.BeanRegistrar;
import infra.beans.factory.BeanRegistry;
import infra.context.annotation.ImportAware;
import infra.core.env.Environment;
import infra.core.type.AnnotationMetadata;
import infra.lang.Nullable;

public class ImportAwareBeanRegistrar implements BeanRegistrar, ImportAware {

  @Nullable
  private AnnotationMetadata importMetadata;

  @Override
  public void register(BeanRegistry registry, Environment env) {
    registry.registerBean(ClassNameHolder.class, spec -> spec.supplier(context ->
            new ClassNameHolder(this.importMetadata == null ? null : this.importMetadata.getClassName())));
  }

  @Override
  public void setImportMetadata(AnnotationMetadata importMetadata) {
    this.importMetadata = importMetadata;
  }

  public @Nullable AnnotationMetadata getImportMetadata() {
    return this.importMetadata;
  }

  public record ClassNameHolder(@Nullable String className) { }
}
