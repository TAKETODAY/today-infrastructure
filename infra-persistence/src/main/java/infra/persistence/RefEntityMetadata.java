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
import infra.persistence.annotation.EntityRef;
import infra.persistence.sql.OrderSpec;

/**
 * {@link EntityMetadata} for an entity annotated with {@link EntityRef}, mapping it to
 * the primary table of another entity.
 *
 * <p>The referenced entity's metadata is exposed as {@link #refMetadata}. When the
 * annotated entity declares an ID property of its own, that property is used for
 * ID-based operations; otherwise {@link #findIdProperty()} falls back to the ID
 * property of the referenced entity, letting a partial view or update model share the
 * primary key and its type handler with the base entity.
 *
 * <p>Ordering behaves the same way: {@link #getOrderSpec()} falls back to the
 * referenced entity's declarative ordering when this entity declares none.
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see EntityRef
 * @see #findIdProperty()
 * @since 5.0
 */
public class RefEntityMetadata extends EntityMetadata {

  /** The metadata of the referenced entity whose primary table is shared. */
  private final EntityMetadata refMetadata;

  protected RefEntityMetadata(EntityMetadata refMetadata, BeanMetadata root, Class<?> entityClass,
          Identifier tableName, @Nullable EntityProperty idProperty, @Nullable EntityProperty versionProperty,
          List<Identifier> columnNames, List<EntityProperty> entityProperties) {
    super(root, entityClass, tableName, idProperty, versionProperty, columnNames, entityProperties);
    this.refMetadata = refMetadata;
  }

  /**
   * Return the effective ID property: this entity's own ID property when declared,
   * otherwise the ID property of the referenced entity.
   *
   * @return this entity's own ID property, or the referenced entity's ID property, or
   * {@code null} if neither declares one
   */
  @Override
  public @Nullable EntityProperty findIdProperty() {
    EntityProperty idProperty = getIdProperty();
    return idProperty != null ? idProperty : refMetadata.findIdProperty();
  }

  /**
   * Resolve ordering with a fallback: this entity's own declarative ordering when
   * present, otherwise the referenced entity's ordering.
   *
   * @return the effective ordering spec, or {@code null} if neither declares one
   */
  @Override
  protected @Nullable OrderSpec resolveOrderSpec() {
    OrderSpec orderSpec = super.resolveOrderSpec();
    return orderSpec != null ? orderSpec : refMetadata.getOrderSpec();
  }

  @Override
  public boolean equals(@Nullable Object o) {
    return this == o
            || (o instanceof RefEntityMetadata that
            && Objects.equals(refMetadata, that.refMetadata)
            && super.equals(that));
  }

  @Override
  public int hashCode() {
    return 31 * super.hashCode() + Objects.hashCode(refMetadata);
  }

}
