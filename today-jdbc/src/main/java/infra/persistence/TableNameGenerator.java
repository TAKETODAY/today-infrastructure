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

package infra.persistence;

import java.lang.annotation.Annotation;

import infra.core.annotation.MergedAnnotation;
import infra.core.annotation.MergedAnnotations;
import infra.lang.Assert;
import infra.lang.Nullable;
import infra.util.StringUtils;

/**
 * An interface for generating table names based on entity classes. It provides
 * methods to generate table names dynamically and supports chaining of multiple
 * generators for flexible resolution strategies.
 *
 * <p>Usage examples:
 *
 * <pre>{@code
 *   // Create a custom TableNameGenerator
 *   TableNameGenerator customGenerator = new TableNameGenerator() {
 *     @Nullable
 *     @Override
 *     public String generateTableName(Class<?> entityClass) {
 *       if (entityClass == UserModel.class) {
 *         return "t_user";
 *       }
 *       return null;
 *     }
 *   };
 *
 *   // Use the default strategy
 *   TableNameGenerator defaultGenerator = TableNameGenerator.defaultStrategy();
 *   String tableName = defaultGenerator.generateTableName(UserModel.class);
 *
 *   // Chain multiple generators
 *   TableNameGenerator chainedGenerator = customGenerator.and(defaultGenerator);
 *   String resolvedTableName = chainedGenerator.generateTableName(UserModel.class);
 * }</pre>
 *
 * <p>The {@link #generateTableName(Class)} method is the core of this interface,
 * responsible for resolving the table name from the given entity class. Implementations
 * can use various strategies, such as annotations, naming conventions, or external
 * configurations.
 *
 * <p>Static factory methods like {@link #forTableAnnotation()} and
 * {@link #forAnnotation(Class)} provide convenient ways to create annotation-based
 * generators.
 *
 * <p><b>Note:</b> The {@link Nullable} annotation indicates that the return value
 * of {@link #generateTableName(Class)} can be {@code null} if no table name can
 * be resolved.
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see DefaultTableNameGenerator
 * @since 4.0 2022/8/16 21:19
 */
public interface TableNameGenerator {

  /**
   * Generates a table name for the given entity class.
   *
   * <p>This method is used to dynamically resolve the table name based on the provided
   * entity class. The resolution logic may vary depending on the implementation of
   * {@link TableNameGenerator}. For example, it could use annotations like
   * {@link Table#name()} or other strategies to determine the table name.</p>
   *
   * <p>Example usage:</p>
   * <pre>{@code
   *   TableNameGenerator generator = TableNameGenerator.defaultStrategy();
   *   String tableName = generator.generateTableName(User.class);
   *   System.out.println("Generated table name: " + tableName);
   * }</pre>
   *
   * @param entityClass the entity class for which the table name is to be generated;
   * must not be null
   * @return the generated table name as a {@link String}, or {@code null} if the table
   * name cannot be resolved for the given entity class
   */
  @Nullable
  String generateTableName(Class<?> entityClass);

  /**
   * Combines this {@link TableNameGenerator} with another generator to create a composite
   * table name generation strategy. The returned generator will first attempt to generate
   * a table name using the current generator. If the current generator returns {@code null},
   * the next generator in the chain will be invoked to resolve the table name.
   *
   * <p>This method is useful for creating fallback strategies or combining multiple table
   * name resolution logics. For example, you can use it to prioritize annotations over
   * default naming conventions.</p>
   *
   * <p>Example usage:</p>
   * <pre>{@code
   *   TableNameGenerator annotationBased = TableNameGenerator.forTableAnnotation();
   *   TableNameGenerator defaultStrategy = TableNameGenerator.defaultStrategy();
   *
   *   TableNameGenerator combined = annotationBased.and(defaultStrategy);
   *   String tableName = combined.generateTableName(User.class);
   *
   *   System.out.println("Generated table name: " + tableName);
   * }</pre>
   *
   * @param next the next {@link TableNameGenerator} to invoke if the current generator
   * returns {@code null}; must not be null
   * @return a new {@link TableNameGenerator} that combines the current generator with
   * the provided {@code next} generator
   */
  default TableNameGenerator and(TableNameGenerator next) {
    return entityClass -> {
      String name = generateTableName(entityClass);
      if (name == null) {
        return next.generateTableName(entityClass);
      }
      return name;
    };
  }

  // Static Factory Methods

  /**
   * Returns the default table name generation strategy.
   *
   * <p>This method provides a pre-configured {@link DefaultTableNameGenerator} instance
   * that applies common conventions for generating table names. It uses a combination of
   * annotation-based resolution and default naming rules, such as converting camelCase
   * to snake_case and appending prefixes or removing suffixes.</p>
   *
   * <p>Example usage:</p>
   * <pre>{@code
   *   // Obtain the default strategy
   *   TableNameGenerator generator = TableNameGenerator.defaultStrategy();
   *
   *   // Generate a table name for an entity class
   *   String tableName = generator.generateTableName(User.class);
   *
   *   // Output the generated table name
   *   System.out.println("Generated table name: " + tableName);
   * }</pre>
   *
   * <p>The default strategy can also be customized or combined with other strategies
   * using the {@link TableNameGenerator#and(TableNameGenerator)} method.</p>
   *
   * @return a new instance of {@link DefaultTableNameGenerator} configured with
   * default table name generation rules
   */
  static DefaultTableNameGenerator defaultStrategy() {
    return new DefaultTableNameGenerator();
  }

  /**
   * Returns a {@link TableNameGenerator} that resolves table names based on the
   * {@link Table} annotation. This method is a convenience wrapper for
   * {@link #forAnnotation(Class)} specifically targeting the {@link Table} annotation.
   *
   * <p>This generator extracts the table name from the {@code name} or {@code value}
   * attribute of the {@link Table} annotation. If neither attribute is specified,
   * the default behavior of the annotation (e.g., using the entity name) applies.</p>
   *
   * <p>Example usage:</p>
   * <pre>{@code
   *   TableNameGenerator generator = TableNameGenerator.forTableAnnotation();
   *   String tableName = generator.generateTableName(User.class);
   *
   *   System.out.println("Generated table name: " + tableName);
   * }</pre>
   *
   * <p>In the example above, if the {@code User} class is annotated with
   * {@code @Table(name = "t_user")}, the generated table name will be {@code "t_user"}.</p>
   *
   * @return a {@link TableNameGenerator} that uses the {@link Table} annotation to
   * resolve table names
   * @see Table#name()
   * @see Table#value()
   */
  static TableNameGenerator forTableAnnotation() {
    return forAnnotation(Table.class);
  }

  /**
   * Creates a {@link TableNameGenerator} that resolves table names based on the specified
   * annotation type. This method uses the default attribute name {@code "value"} to extract
   * the table name from the annotation.
   *
   * <p>This method is a convenience wrapper for
   * {@link #forAnnotation(Class, String)} with the attribute name set to
   * {@link MergedAnnotation#VALUE}. It is useful when the annotation's table name is stored
   * in its {@code value} attribute.</p>
   *
   * <p>Example usage:</p>
   * <pre>{@code
   *   // Define a custom annotation
   *   @Retention(RetentionPolicy.RUNTIME)
   *   @Target(ElementType.TYPE)
   *   public @interface CustomTable {
   *       String value();
   *   }
   *
   *   // Annotate an entity class
   *   @CustomTable("custom_table_name")
   *   public class User {
   *   }
   *
   *   // Create a generator for the custom annotation
   *   TableNameGenerator generator = TableNameGenerator.forAnnotation(CustomTable.class);
   *
   *   // Generate the table name
   *   String tableName = generator.generateTableName(User.class);
   *
   *   System.out.println("Generated table name: " + tableName); // Output: custom_table_name
   * }</pre>
   *
   * @param annotationType the type of the annotation used to resolve the table name; must not be null
   * @return a {@link TableNameGenerator} that uses the specified annotation type and its
   * {@code value} attribute to resolve table names
   * @see #forAnnotation(Class, String)
   * @see MergedAnnotation#VALUE
   */
  static TableNameGenerator forAnnotation(Class<? extends Annotation> annotationType) {
    return forAnnotation(annotationType, MergedAnnotation.VALUE);
  }

  /**
   * Creates a {@link TableNameGenerator} that resolves table names based on the specified
   * annotation type and attribute name. This method allows customization of the attribute
   * used to extract the table name from the annotation.
   *
   * <p>This generator first checks if the given annotation is present on the entity class.
   * If the annotation exists and the specified attribute has a non-empty value, the value
   * is returned as the table name. If the annotation or attribute is not present, the
   * generator falls back to checking for an {@link EntityRef} annotation. If an
   * {@link EntityRef} is found, the generator recursively resolves the table name for
   * the referenced class.</p>
   *
   * <p>Example usage:</p>
   * <pre>{@code
   *   // Define a custom annotation
   *   @Retention(RetentionPolicy.RUNTIME)
   *   @Target(ElementType.TYPE)
   *   public @interface CustomTable {
   *       String tableName();
   *   }
   *
   *   // Annotate an entity class
   *   @CustomTable(tableName = "custom_table_name")
   *   public class User {
   *   }
   *
   *   // Create a generator for the custom annotation and attribute
   *   TableNameGenerator generator = TableNameGenerator.forAnnotation(
   *       CustomTable.class, "tableName"
   *   );
   *
   *   // Generate the table name
   *   String tableName = generator.generateTableName(User.class);
   *
   *   System.out.println("Generated table name: " + tableName); // Output: custom_table_name
   * }</pre>
   *
   * @param annotationType the type of the annotation used to resolve the table name; must not be null
   * @param attributeName the name of the attribute within the annotation that holds the table name; must not be null
   * @return a {@link TableNameGenerator} that uses the specified annotation type and attribute name to resolve table names
   */
  static TableNameGenerator forAnnotation(Class<? extends Annotation> annotationType, String attributeName) {
    Assert.notNull(attributeName, "attributeName is required");
    Assert.notNull(annotationType, "annotationType is required");

    class ForAnnotation implements TableNameGenerator {

      @Nullable
      @Override
      public String generateTableName(Class<?> entityClass) {
        MergedAnnotations annotations = MergedAnnotations.from(entityClass);
        var annotation = annotations.get(annotationType);
        if (annotation.isPresent()) {
          String name = annotation.getString(attributeName);
          if (StringUtils.hasText(name)) {
            return name;
          }
        }

        var ref = annotations.get(EntityRef.class);
        if (ref.isPresent()) {
          Class<?> classValue = ref.getClassValue();
          return generateTableName(classValue);
        }
        return null;
      }
    }

    return new ForAnnotation();
  }
}
