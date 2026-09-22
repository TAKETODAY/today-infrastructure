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
import infra.lang.Constant;
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
  public @Nullable Condition resolve(EntityMetadata entityMetadata, EntityProperty entityProperty,
          Object value, ValueNormalizer valueNormalizer) {
    MergedAnnotation<Subquery> subquery = entityProperty.getAnnotation(Subquery.class);
    if (!subquery.isPresent()) {
      return null;
    }

    // resolve the referenced entity's metadata via @EntityRef
    MergedAnnotation<EntityRef> entityRef = entityMetadata.getAnnotation(EntityRef.class);
    if (!entityRef.isPresent()) {
      return null;
    }

    EntityMetadata refMetadata = metadataFactory.getEntityMetadata(entityRef.getClassValue());
    Identifier targetColumn = resolveTargetColumn(subquery, refMetadata);
    if (targetColumn == null) {
      return null;
    }

    Identifier tableName = resolveTableName(subquery);
    Identifier sourceColumn = entityProperty.getColumnName();

    value = valueNormalizer.normalize(entityProperty, value);
    return new SubqueryCondition(targetColumn, subquery.getBoolean("negative"),
            subquery.getString("select"), tableName, sourceColumn,
            value, entityProperty);
  }

  private @Nullable Identifier resolveTargetColumn(MergedAnnotation<Subquery> subquery, EntityMetadata refMetadata) {
    String target = subquery.getString("target");
    if (Constant.BLANK.equals(target)) {
      return refMetadata.getIdColumnName();
    }
    // resolve the target as a property of the referenced entity, honouring its mapped column
    EntityProperty property = refMetadata.findProperty(target);
    if (property != null) {
      return property.getColumnName();
    }
    return Identifier.parse(target);
  }

  private Identifier resolveTableName(MergedAnnotation<Subquery> subquery) {
    Class<?> entityClass = subquery.getClass("from");
    if (entityClass != void.class) {
      EntityMetadata junctionMetadata = metadataFactory.getEntityMetadata(entityClass);
      return junctionMetadata.getTableName();
    }
    String tableName = subquery.getString("fromTable");
    if (tableName.isEmpty()) {
      throw new IllegalEntityException(
              "@Subquery fromTable is required when 'from' is not specified");
    }
    return Identifier.parse(tableName);
  }

}