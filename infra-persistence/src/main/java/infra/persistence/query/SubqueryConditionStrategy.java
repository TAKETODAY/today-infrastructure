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

package infra.persistence.query;

import org.jspecify.annotations.Nullable;

import infra.core.annotation.MergedAnnotation;
import infra.lang.Constant;
import infra.persistence.EntityMetadata;
import infra.persistence.EntityMetadataFactory;
import infra.persistence.EntityProperty;
import infra.persistence.Identifier;
import infra.persistence.IllegalEntityException;
import infra.persistence.annotation.EntityRef;
import infra.persistence.annotation.Subquery;

/**
 * A {@link PropertyConditionStrategy} that turns a {@link Subquery @Subquery}-annotated
 * property into a {@code column IN (SELECT ...)} predicate for many-to-many lookups
 * through a junction table.
 *
 * <p>The target column is resolved from the referenced entity's metadata — the
 * entity the queried class maps to via {@link EntityRef @EntityRef}, exposed by
 * {@link EntityMetadata#getRefMetadata()}. It is the ID column by default or a
 * {@link Subquery#target() target} column otherwise. The junction table and its
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
  public @Nullable Condition resolve(EntityMetadata metadata, EntityProperty property,
          Object value, ValueNormalizer valueNormalizer) {
    MergedAnnotation<Subquery> subquery = property.getAnnotation(Subquery.class);
    if (subquery.isPresent()) {

      // the queried class must map to a referenced entity via @EntityRef
      EntityMetadata refMetadata = metadata.getRefMetadata();
      if (refMetadata == null) {
        return null;
      }

      Identifier targetColumn = resolveTargetColumn(subquery, refMetadata);
      if (targetColumn == null) {
        return null;
      }

      Identifier tableName = resolveTableName(subquery);
      Identifier sourceColumn = property.getColumnName();

      value = valueNormalizer.normalize(property, value);
      return new SubqueryCondition(targetColumn, subquery.getBoolean("negative"),
              subquery.getString("operator"),
              subquery.getString("select"), tableName, sourceColumn, value, property);
    }
    return null;
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
