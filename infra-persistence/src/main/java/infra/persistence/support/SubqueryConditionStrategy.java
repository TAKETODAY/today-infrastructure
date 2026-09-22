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

package infra.persistence.support;

import org.jspecify.annotations.Nullable;

import infra.core.annotation.MergedAnnotation;
import infra.core.annotation.MergedAnnotations;
import infra.persistence.Condition;
import infra.persistence.EntityMetadata;
import infra.persistence.EntityMetadataFactory;
import infra.persistence.EntityProperty;
import infra.persistence.Identifier;
import infra.persistence.IllegalEntityException;
import infra.persistence.PropertyConditionStrategy;
import infra.persistence.ValueNormalizer;
import infra.persistence.annotation.EntityRef;
import infra.persistence.annotation.Subquery;
import infra.persistence.sql.Restrictions;

/**
 * A {@link PropertyConditionStrategy} that turns a {@link Subquery @Subquery}-annotated
 * property into a {@code column IN (SELECT ...)} predicate for many-to-many lookups
 * through a junction table.
 *
 * <p>The target column is resolved from the referenced entity's ID column via
 * {@link EntityRef @EntityRef} on the declaring class. The junction table and its
 * columns are declared in the {@code @Subquery} annotation.
 *
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 5.0
 */
public class SubqueryConditionStrategy implements PropertyConditionStrategy {

  private final EntityMetadataFactory metadataFactory;

  public SubqueryConditionStrategy(EntityMetadataFactory metadataFactory) {
    this.metadataFactory = metadataFactory;
  }

  @Override
  public @Nullable Condition resolve(EntityProperty entityProperty, Object value, ValueNormalizer valueNormalizer) {
    MergedAnnotation<Subquery> subquery = entityProperty.getAnnotation(Subquery.class);
    if (!subquery.isPresent()) {
      return null;
    }

    value = valueNormalizer.normalize(entityProperty, value);

    // resolve the referenced entity's ID column via @EntityRef
    Class<?> declaringClass = entityProperty.getBeanProperty().getDeclaringClass();
    MergedAnnotation<EntityRef> entityRef = MergedAnnotations.from(declaringClass).get(EntityRef.class);
    if (!entityRef.isPresent()) {
      return null;
    }

    EntityMetadata refMetadata = metadataFactory.getEntityMetadata(entityRef.getClassValue());
    Identifier targetColumn = refMetadata.getIdColumnName();
    if (targetColumn == null) {
      return null;
    }

    Identifier tableName = resolveTableName(subquery);
    String sourceColumn = entityProperty.getColumnName().render();
    String operator = subquery.getBoolean("negative") ? " NOT IN (" : " IN (";
    String sql = targetColumn.render()
            + operator
            + "SELECT " + subquery.getString("select")
            + " FROM " + tableName
            + " WHERE " + sourceColumn + " = ?)";

    return new PropertyCondition(value, Restrictions.plain(sql), entityProperty);
  }

  private Identifier resolveTableName(MergedAnnotation<Subquery> subquery) {
    Class<?> entityClass = subquery.getClass("entity");
    if (entityClass != void.class) {
      EntityMetadata junctionMetadata = metadataFactory.getEntityMetadata(entityClass);
      return junctionMetadata.getTableName();
    }
    String tableName = subquery.getString("table");
    if (tableName.isEmpty()) {
      throw new IllegalEntityException(
              "@Subquery table name is required when 'entity' is not specified");
    }
    return Identifier.parse(tableName);
  }

}