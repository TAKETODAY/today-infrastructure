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

import org.jspecify.annotations.Nullable;

import java.util.Locale;

import infra.util.ObjectUtils;
import infra.util.StringUtils;

/**
 * A default implementation of {@link TableNameGenerator} that generates table names
 * based on entity class names with customizable transformations.
 *
 * <p>This generator applies a series of transformations to the entity class name:
 * <ul>
 *   <li>Appends a configurable prefix (e.g., "t_").</li>
 *   <li>Removes configurable suffixes (e.g., "Model", "Entity").</li>
 *   <li>Converts camelCase names to snake_case if enabled.</li>
 *   <li>Converts the name to lowercase if enabled.</li>
 * </ul>
 *
 * <p><strong>Usage Example:</strong>
 * <pre>{@code
 * DefaultTableNameGenerator generator = new DefaultTableNameGenerator();
 *
 * // Configure the generator
 * generator.setPrefixToAppend("t_");
 * generator.setSuffixArrayToRemove("Model", "Entity");
 * generator.setCamelCaseToUnderscore(true);
 * generator.setLowercase(true);
 *
 * // Generate table name for an entity class
 * String tableName = generator.generateTableName(UserModel.class);
 * System.out.println(tableName); // Output: t_user
 * }</pre>
 *
 * <p><strong>Configuration Options:</strong>
 * <ul>
 *   <li>{@link #setPrefixToAppend(String)}: Sets a prefix to prepend to the table name.</li>
 *   <li>{@link #setSuffixToRemove(String)}: Removes a single suffix from the entity class name.</li>
 *   <li>{@link #setSuffixArrayToRemove(String...)}: Removes multiple suffixes from the entity class name.</li>
 *   <li>{@link #setLowercase(boolean)}: Enables or disables converting the table name to lowercase.</li>
 *   <li>{@link #setCamelCaseToUnderscore(boolean)}: Enables or disables converting camelCase to snake_case.</li>
 * </ul>
 *
 * <p>If the entity class is annotated with a table name annotation, the annotation's value
 * will take precedence over the transformations applied by this generator.
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see TableNameGenerator
 * @since 4.0 2022/8/16 23:09
 */
public class DefaultTableNameGenerator implements TableNameGenerator {

  private final TableNameGenerator annotationGenerator = TableNameGenerator.forTableAnnotation();

  @Nullable
  private String prefixToAppend;

  private String @Nullable []suffixArrayToRemove;

  private boolean lowercase = true;

  private boolean camelCaseToUnderscore = true;

  /**
   * Sets whether the table name should be converted to lowercase.
   * When set to <code>true</code>, the generated table name will be
   * transformed to all lowercase letters. If set to <code>false</code>,
   * the case of the table name will remain unchanged.
   *
   * <p>This setting is particularly useful when working with databases
   * that are case-sensitive or have specific naming conventions.
   *
   * <p>Example usage:
   * <pre>{@code
   *   DefaultTableNameGenerator generator = new DefaultTableNameGenerator();
   *   generator.setLowercase(true);
   *
   *   String tableName = generator.generateTableName(MyEntity.class);
   *   // If the original table name is "MyEntity", it will be converted to "myentity"
   * }</pre>
   *
   * @param lowercase a boolean value indicating whether the table name
   * should be converted to lowercase (<code>true</code>)
   * or left as is (<code>false</code>)
   */
  public void setLowercase(boolean lowercase) {
    this.lowercase = lowercase;
  }

  /**
   * Sets whether the camelCase names should be converted to underscore-separated names.
   * When set to <code>true</code>, any camelCase name will be transformed into an
   * underscore-separated format. If set to <code>false</code>, the name format will
   * remain unchanged.
   *
   * <p>This setting is particularly useful when working with databases or systems
   * that follow the convention of using underscores in identifiers instead of camelCase.
   *
   * <p>Example usage:
   * <pre>{@code
   *   DefaultTableNameGenerator generator = new DefaultTableNameGenerator();
   *   generator.setCamelCaseToUnderscore(true);
   *
   *   String tableName = generator.generateTableName(MyEntity.class);
   *   // If the original name is "myEntity", it will be converted to "my_entity"
   * }</pre>
   *
   * @param camelCaseToUnderscore a boolean value indicating whether camelCase names
   * should be converted to underscore-separated names
   * (<code>true</code>) or left as is (<code>false</code>)
   */
  public void setCamelCaseToUnderscore(boolean camelCaseToUnderscore) {
    this.camelCaseToUnderscore = camelCaseToUnderscore;
  }

  /**
   * Sets the prefix that will be appended to the generated table name.
   * This method allows customization of table names by adding a specific
   * prefix to them. If set to <code>null</code>, no prefix will be appended.
   *
   * <p>This is useful when you need to follow naming conventions or
   * distinguish between different environments (e.g., "test_", "prod_").
   *
   * <p>Example usage:
   * <pre>{@code
   *   DefaultTableNameGenerator generator = new DefaultTableNameGenerator();
   *   generator.setPrefixToAppend("dev_");
   *
   *   String tableName = generator.generateTableName(MyEntity.class);
   *   // If the original table name is "MyEntity", it will become "dev_MyEntity"
   * }</pre>
   *
   * @param prefixToAppend the prefix to append to the generated table name.
   * Can be <code>null</code> if no prefix is required.
   */
  public void setPrefixToAppend(@Nullable String prefixToAppend) {
    this.prefixToAppend = prefixToAppend;
  }

  /**
   * Sets the suffix that should be removed from the generated table name.
   * If the provided suffix is <code>null</code>, no suffix will be removed.
   * Otherwise, the specified suffix will be stored as a single-element array
   * for further processing during table name generation.
   *
   * <p>This method is particularly useful when you need to customize table names
   * by removing specific suffixes from entity class names. For example, if your
   * entity classes are named with a specific suffix like "Entity", you can configure
   * this method to remove it automatically.
   *
   * <p>Example usage:
   * <pre>{@code
   *   DefaultTableNameGenerator generator = new DefaultTableNameGenerator();
   *   generator.setSuffixToRemove("Entity");
   *
   *   String tableName = generator.generateTableName(MyEntity.class);
   *   // If the original class name is "MyEntity", the generated table name
   *   // will be "My" after removing the suffix "Entity".
   * }</pre>
   *
   * @param suffixArrayToRemove the suffix to remove from the generated table name.
   * Can be <code>null</code> if no suffix removal is required.
   */
  public void setSuffixToRemove(@Nullable String suffixArrayToRemove) {
    if (suffixArrayToRemove == null) {
      this.suffixArrayToRemove = null;
    }
    else {
      this.suffixArrayToRemove = new String[] { suffixArrayToRemove };
    }
  }

  /**
   * Sets an array of suffixes that should be removed from the generated table name.
   * If the provided array is <code>null</code>, no suffixes will be removed.
   * Otherwise, the specified suffixes will be stored for further processing
   * during table name generation.
   *
   * <p>This method is particularly useful when you need to customize table names
   * by removing specific suffixes from entity class names. For example, if your
   * entity classes are named with common suffixes like "Entity" or "Table", you
   * can configure this method to remove them automatically.
   *
   * <p>Example usage:
   * <pre>{@code
   *   DefaultTableNameGenerator generator = new DefaultTableNameGenerator();
   *   generator.setSuffixArrayToRemove("Entity", "Table");
   *
   *   String tableName1 = generator.generateTableName(MyEntity.class);
   *   // If the original class name is "MyEntity", the generated table name
   *   // will be "My" after removing the suffix "Entity".
   *
   *   String tableName2 = generator.generateTableName(UserTable.class);
   *   // If the original class name is "UserTable", the generated table name
   *   // will be "User" after removing the suffix "Table".
   * }</pre>
   *
   * @param suffixToRemove an array of suffixes to remove from the generated table name.
   * Can be <code>null</code> if no suffix removal is required.
   */
  public void setSuffixArrayToRemove(String @Nullable ... suffixToRemove) {
    this.suffixArrayToRemove = suffixToRemove;
  }

  @Override
  public String generateTableName(Class<?> entityClass) {
    String name = annotationGenerator.generateTableName(entityClass);
    if (name != null) {
      return name;
    }

    String simpleName = entityClass.getSimpleName();

    // append the prefix like "t_" -> t_user, t_order
    StringBuilder tableName = new StringBuilder();
    if (StringUtils.hasText(prefixToAppend)) {
      tableName.append(prefixToAppend);
    }

    // remove the common suffix like UserModel - Model = User, UserEntity - Entity = User
    if (ObjectUtils.isNotEmpty(suffixArrayToRemove)) {
      for (String suffix : suffixArrayToRemove) {
        if (simpleName.endsWith(suffix)) {
          simpleName = simpleName.substring(0, simpleName.length() - suffix.length());
          break;
        }
      }
    }

    // UserOrder -> user_order
    if (camelCaseToUnderscore) {
      simpleName = StringUtils.camelCaseToUnderscore(simpleName);
    }
    else if (lowercase) {
      // User -> user
      simpleName = simpleName.toLowerCase(Locale.ROOT);
    }

    tableName.append(simpleName);
    return tableName.toString();
  }
}
