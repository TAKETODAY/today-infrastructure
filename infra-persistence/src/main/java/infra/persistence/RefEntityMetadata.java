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

package infra.persistence;

import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.Objects;

import infra.beans.BeanMetadata;
import infra.beans.BeanProperty;
import infra.persistence.annotation.EntityRef;

/**
 * {@link EntityMetadata} for an entity annotated with {@link EntityRef}, mapping it to
 * the primary table of another entity.
 *
 * <p>The referenced entity is available as {@link #refMetadata}. When the annotated
 * entity declares an ID property of its own, that property is used for ID-based
 * operations; otherwise the ID property of the referenced entity is reused
 * ({@link #refIdProperty}), letting a partial view or update model share the primary
 * key and its type handler with the base entity.
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see EntityRef
 * @since 5.0
 */
public class RefEntityMetadata extends EntityMetadata {

  /** The metadata of the referenced entity whose primary table is shared. */
  public final EntityMetadata refMetadata;

  /** The ID property of {@link #refMetadata}, reused when this entity has no ID of its own. */
  public final @Nullable EntityProperty refIdProperty;

  protected RefEntityMetadata(EntityMetadata refMetadata, BeanMetadata root, Class<?> entityClass,
          @Nullable EntityProperty idProperty, String tableName, @Nullable EntityProperty versionProperty,
          List<BeanProperty> beanProperties, List<String> columnNames, List<EntityProperty> entityProperties) {
    super(root, entityClass, idProperty, tableName, versionProperty, beanProperties, columnNames, entityProperties);
    this.refMetadata = refMetadata;
    this.refIdProperty = refMetadata.findIdProperty();
  }

  @Override
  public @Nullable EntityProperty findIdProperty() {
    return idProperty != null ? idProperty : refIdProperty;
  }

  @Override
  public boolean equals(@Nullable Object o) {
    return this == o
            || (o instanceof RefEntityMetadata that
            && Objects.equals(refIdProperty, that.refIdProperty)
            && super.equals(that));
  }

  @Override
  public int hashCode() {
    return 31 * super.hashCode() + Objects.hashCode(refIdProperty);
  }

}
