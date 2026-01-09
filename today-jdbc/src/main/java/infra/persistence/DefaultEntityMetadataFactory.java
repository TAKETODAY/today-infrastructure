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

import java.util.ArrayList;
import java.util.Set;

import infra.beans.BeanMetadata;
import infra.beans.BeanProperty;
import infra.core.annotation.MergedAnnotations;
import infra.jdbc.type.TypeHandler;
import infra.jdbc.type.TypeHandlerManager;
import infra.lang.Assert;
import infra.util.ClassUtils;

/**
 * A factory class responsible for creating {@link EntityMetadata} instances for entity classes.
 * This class provides customizable strategies for property filtering, table name generation,
 * column name discovery, ID property detection, and type handling.
 *
 * <p>The default configuration includes:
 * <ul>
 *   <li>A {@link PropertyFilter} that filters out properties named "class" and those annotated
 *       with a transient annotation.</li>
 *   <li>A {@link TableNameGenerator} using the default strategy.</li>
 *   <li>An {@link IdPropertyDiscover} that detects ID properties based on an annotation or
 *       a default property name.</li>
 *   <li>A {@link ColumnNameDiscover} that uses annotations or converts camelCase to underscore
 *       for column names.</li>
 *   <li>A shared instance of {@link TypeHandlerManager} for resolving type handlers.</li>
 * </ul>
 *
 * <p>This class allows customization of its strategies through setter methods. For example:
 * <pre>{@code
 * DefaultEntityMetadataFactory factory = new DefaultEntityMetadataFactory();
 *
 * // Customize the table name generator
 * factory.setTableNameGenerator(new CustomTableNameGenerator());
 *
 * // Customize the property filter
 * factory.setPropertyFilter(PropertyFilter.filteredNames(Set.of("version")));
 *
 * // Create metadata for an entity class
 * EntityMetadata metadata = factory.createEntityMetadata(MyEntity.class);
 * }</pre>
 *
 * <p>When creating metadata, the factory performs the following steps:
 * <ol>
 *   <li>Determines the table name for the entity class using the configured
 *       {@link TableNameGenerator}.</li>
 *   <li>Iterates over the properties of the entity class, filtering them using the
 *       {@link PropertyFilter}.</li>
 *   <li>Determines the column name for each property using the {@link ColumnNameDiscover}.</li>
 *   <li>Identifies the ID property using the {@link IdPropertyDiscover}.</li>
 *   <li>Creates an {@link EntityMetadata} instance containing all relevant information.</li>
 * </ol>
 *
 * <p>If the table name or column names cannot be determined, or if multiple ID properties are
 * detected, an {@link IllegalEntityException} is thrown.
 *
 * <p><strong>Note:</strong> This implementation supports entities with a single ID property.
 * If no ID property is found, it attempts to resolve one from a referenced metadata (if applicable).
 *
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @see EntityMetadata
 * @see PropertyFilter
 * @see TableNameGenerator
 * @see IdPropertyDiscover
 * @see ColumnNameDiscover
 * @see TypeHandlerManager
 * @since 4.0 2022/9/5 11:38
 */
public class DefaultEntityMetadataFactory extends EntityMetadataFactory {

  private PropertyFilter propertyFilter = PropertyFilter.filteredNames(Set.of("class"))
          .and(PropertyFilter.forTransientAnnotation());

  private TableNameGenerator tableNameGenerator = TableNameGenerator.defaultStrategy();

  private IdPropertyDiscover idPropertyDiscover = IdPropertyDiscover.forIdAnnotation()
          .and(IdPropertyDiscover.forPropertyName(IdPropertyDiscover.DEFAULT_ID_PROPERTY));

  private ColumnNameDiscover columnNameDiscover = ColumnNameDiscover.forColumnAnnotation()
          .and(ColumnNameDiscover.camelCaseToUnderscore());

  private TypeHandlerManager typeHandlerManager = TypeHandlerManager.sharedInstance;

  /**
   * Sets the {@link ColumnNameDiscover} to determine the column name for each property
   * in the entity metadata generation process.
   * <p>
   * The provided {@code ColumnNameDiscover} will be used to resolve the column names
   * for properties. If a property's column name cannot be determined, it may be excluded
   * from the entity metadata.
   * <p>
   * Example usage:
   * <pre>{@code
   * DefaultEntityMetadataFactory factory = new DefaultEntityMetadataFactory();
   *
   * // Use camelCase to underscore naming strategy
   * factory.setColumnNameDiscover(ColumnNameDiscover.camelCaseToUnderscore());
   *
   * // Use property name as column name
   * factory.setColumnNameDiscover(ColumnNameDiscover.forPropertyName());
   *
   * // Combine multiple strategies using a resolving chain
   * ColumnNameDiscover compositeDiscover = ColumnNameDiscover.forColumnAnnotation()
   *         .and(ColumnNameDiscover.camelCaseToUnderscore());
   * factory.setColumnNameDiscover(compositeDiscover);
   * }</pre>
   * <p>
   * Note: The {@code columnNameDiscover} parameter must not be null. If null is passed,
   * an {@link IllegalArgumentException} will be thrown.
   *
   * @param columnNameDiscover the {@link ColumnNameDiscover} to set; must not be null
   * @throws IllegalArgumentException if the provided {@code columnNameDiscover} is null
   */
  public void setColumnNameDiscover(ColumnNameDiscover columnNameDiscover) {
    Assert.notNull(columnNameDiscover, "columnNameDiscover is required");
    this.columnNameDiscover = columnNameDiscover;
  }

  /**
   * Sets the {@link IdPropertyDiscover} to determine which property in the entity
   * should be treated as the ID property during the metadata generation process.
   * <p>
   * The provided {@code IdPropertyDiscover} will be used to identify the ID property
   * for entities. If no property is identified as an ID, the entity metadata may
   * not include an ID column.
   * <p>
   * Example usage:
   * <pre>{@code
   * DefaultEntityMetadataFactory factory = new DefaultEntityMetadataFactory();
   *
   * // Use a specific property name as the ID
   * factory.setIdPropertyDiscover(IdPropertyDiscover.forPropertyName("userId"));
   *
   * // Use the @Id annotation to determine the ID property
   * factory.setIdPropertyDiscover(IdPropertyDiscover.forIdAnnotation());
   *
   * // Combine multiple strategies using a resolving chain
   * IdPropertyDiscover compositeDiscover = IdPropertyDiscover.forIdAnnotation()
   *         .and(IdPropertyDiscover.forPropertyName("customId"));
   * factory.setIdPropertyDiscover(compositeDiscover);
   * }</pre>
   * <p>
   * Note: The {@code idPropertyDiscover} parameter must not be null. If null is passed,
   * an {@link IllegalArgumentException} will be thrown.
   *
   * @param idPropertyDiscover the {@link IdPropertyDiscover} to set; must not be null
   * @throws IllegalArgumentException if the provided {@code idPropertyDiscover} is null
   */
  public void setIdPropertyDiscover(IdPropertyDiscover idPropertyDiscover) {
    Assert.notNull(idPropertyDiscover, "idPropertyDiscover is required");
    this.idPropertyDiscover = idPropertyDiscover;
  }

  /**
   * Sets the PropertyFilter to determine which properties should be excluded
   * from being mapped to database columns.
   * <p>
   * The provided {@code PropertyFilter} will be used to filter out properties
   * that do not map to database columns. If a property is filtered, it will
   * not be included in the entity metadata generated by this factory.
   * <p>
   * Example usage:
   * <pre>{@code
   * DefaultEntityMetadataFactory factory = new DefaultEntityMetadataFactory();
   *
   * // Filter properties annotated with @Transient
   * factory.setPropertyFilter(PropertyFilter.forTransientAnnotation());
   *
   * // Filter specific property names
   * Set<String> filteredNames = Set.of("password", "tempField");
   * factory.setPropertyFilter(PropertyFilter.filteredNames(filteredNames));
   *
   * // Combine multiple filters using a resolving chain
   * PropertyFilter combinedFilter = PropertyFilter.forTransientAnnotation()
   *         .and(PropertyFilter.filteredNames(Set.of("password")));
   * factory.setPropertyFilter(combinedFilter);
   * }</pre>
   *
   * @param propertyFilter the PropertyFilter to set; must not be null
   * @throws IllegalArgumentException if the provided {@code propertyFilter} is null
   */
  public void setPropertyFilter(PropertyFilter propertyFilter) {
    Assert.notNull(propertyFilter, "propertyFilter is required");
    this.propertyFilter = propertyFilter;
  }

  /**
   * set the TableNameGenerator to generate the table name for the class
   *
   * @param tableNameGenerator a TableNameGenerator
   */
  public void setTableNameGenerator(TableNameGenerator tableNameGenerator) {
    Assert.notNull(tableNameGenerator, "tableNameGenerator is required");
    this.tableNameGenerator = tableNameGenerator;
  }

  /**
   * Set the TypeHandlerManager to find {@link TypeHandler} for the {@link BeanProperty}
   *
   * @param typeHandlerManager TypeHandlerManager
   */
  public void setTypeHandlerManager(TypeHandlerManager typeHandlerManager) {
    Assert.notNull(typeHandlerManager, "TypeHandlerManager is required");
    this.typeHandlerManager = typeHandlerManager;
  }

  @Override
  public EntityMetadata createEntityMetadata(Class<?> entityClass) {
    String tableName = tableNameGenerator.generateTableName(entityClass);
    EntityMetadata refMetadata = null;
    if (tableName == null) {
      refMetadata = getRefMetadata(entityClass);
      if (refMetadata != null) {
        tableName = refMetadata.tableName;
      }
    }

    if (tableName == null) {
      throw new IllegalEntityException("Cannot determine table name for entity: " + entityClass);
    }

    BeanMetadata metadata = BeanMetadata.forClass(entityClass);
    ArrayList<String> columnNames = new ArrayList<>();
    ArrayList<BeanProperty> beanProperties = new ArrayList<>();
    ArrayList<EntityProperty> entityProperties = new ArrayList<>();

    EntityProperty idProperty = null;
    EntityProperty refIdProperty = null;
    for (BeanProperty property : metadata) {
      if (isFiltered(property)) {
        continue;
      }

      String columnName = columnNameDiscover.getColumnName(property);
      if (columnName == null) {
        throw new IllegalEntityException("Cannot determine column name for property: %s#%s"
                .formatted(ClassUtils.getShortName(property.getDeclaringClass()), property.getName()));
      }

      columnNames.add(columnName);
      beanProperties.add(property);

      if (idPropertyDiscover.isIdProperty(property)) {
        if (idProperty != null) {
          throw new IllegalEntityException("Only one Id property supported, entity: " + entityClass);
        }
        idProperty = createEntityProperty(property, columnName, true);
        entityProperties.add(idProperty);
      }
      else {
        entityProperties.add(createEntityProperty(property, columnName, false));
      }
    }

    if (idProperty == null) {
      if (refMetadata == null) {
        refMetadata = getRefMetadata(entityClass);
      }
      if (refMetadata != null) {
        refIdProperty = refMetadata.idProperty;
      }
    }

    if (idProperty == null && entityProperties.isEmpty()) {
      throw new IllegalEntityException("Cannot determine properties for entity: " + entityClass);
    }

    return new EntityMetadata(metadata, entityClass, idProperty, tableName,
            refIdProperty, beanProperties, columnNames, entityProperties);
  }

  @Nullable
  private EntityMetadata getRefMetadata(Class<?> entityClass) {
    var ref = MergedAnnotations.from(entityClass).get(EntityRef.class);
    if (ref.isPresent()) {
      return getEntityMetadata(ref.getClassValue());
    }
    return null;
  }

  private EntityProperty createEntityProperty(BeanProperty property, String columnName, boolean isId) {
    return new EntityProperty(property, columnName, typeHandlerManager.getTypeHandler(property), isId);
  }

  private boolean isFiltered(BeanProperty property) {
    return isInnerClass(property) || propertyFilter.isFiltered(property);
  }

  private static boolean isInnerClass(BeanProperty property) {
    return property.getName().equals("this$0");
  }

}
