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

/**
 * A functional interface for extracting and wrapping property values
 * from entities or examples based on specific conditions.
 *
 * <p>This interface is designed to be implemented for custom logic that
 * extracts a value from an entity or example object and optionally wraps
 * the extracted value into a desired type. It is particularly useful in
 * scenarios where property values need transformation or special handling
 * before being used in queries, persistence operations, or other contexts.
 *
 * <h3>Usage Example</h3>
 * Below is an example of implementing this interface to handle Base64-encoded
 * values:
 *
 * <pre>{@code
 * static class Base64ValueExtractor implements ConditionPropertyExtractor<String> {
 *
 *   @Nullable
 *   @Override
 *   public Object extract(Object entityOrExample, EntityProperty property, @Nullable Object value) {
 *     if (value instanceof Base64Value base64Value) {
 *       return base64Value.value;
 *     }
 *     return value;
 *   }
 *
 *   @Override
 *   public Object wrap(String extracted) {
 *     return new Base64Value(extracted);
 *   }
 * }
 * }</pre>
 *
 * <p>In this example:
 * <ul>
 *   <li>The {@code extract} method checks if the input value is an instance of
 *       {@code Base64Value}. If so, it extracts the raw value; otherwise, it
 *       returns the value as-is.</li>
 *   <li>The {@code wrap} method wraps the extracted string value back into a
 *       {@code Base64Value} object.</li>
 * </ul>
 *
 * <h3>Method Details</h3>
 * <ul>
 *   <li>{@link #extract(Object, EntityProperty, Object)}: Extracts a value from
 *       the given entity or example object. The method may return {@code null}
 *       if the value is not applicable or cannot be extracted.</li>
 *   <li>{@link #wrap(Object)}: Wraps the extracted value into a desired type.
 *       This method is typically used to transform the extracted value into a
 *       format suitable for further processing or storage.</li>
 * </ul>
 *
 * @param <T> the type of the extracted value after wrapping
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 5.0 2025/1/25 22:51
 */
public interface ConditionPropertyExtractor<T> {

  /**
   * Extracts a value from the given entity or example object based on the specified property.
   *
   * <p>This method is designed to handle custom extraction logic for property values. It can
   * be used to transform, filter, or directly extract values from an entity or example object.
   * If the value cannot be extracted or is not applicable, the method may return {@code null}.
   *
   * <p><strong>Example Usage:</strong>
   * <pre>{@code
   * ConditionPropertyExtractor<String> extractor = new ConditionPropertyExtractor<>() {
   *   @Nullable
   *   @Override
   *   public Object extract(Object entityOrExample, EntityProperty property, @Nullable Object value) {
   *     if (property.getName().equals("status") && value instanceof String status) {
   *       return "active".equalsIgnoreCase(status) ? 1 : 0;
   *     }
   *     return value;
   *   }
   *
   *   @Override
   *   public Object wrap(String extracted) {
   *     return extracted.toUpperCase();
   *   }
   * };
   *
   * Object result = extractor.extract(entity, property, "active");
   * }</pre>
   *
   * <p>In this example:
   * <ul>
   *   <li>The {@code extract} method checks if the property name is "status" and if the value
   *       is a string. It then converts the status "active" to {@code 1} and others to {@code 0}.</li>
   *   <li>The {@code wrap} method (not shown in the example usage) transforms the extracted value
   *       into uppercase.</li>
   * </ul>
   *
   * @param entityOrExample the entity or example object from which the value is extracted
   * @param property the property associated with the value to be extracted
   * @param value the raw value associated with the property, which may be {@code null}
   * @return the extracted value, or {@code null} if the value cannot be extracted or is not applicable
   */
  @Nullable
  Object extract(Object entityOrExample, EntityProperty property, @Nullable Object value);

  /**
   * Wraps the extracted value into a desired format or type.
   *
   * <p>This method is typically used to apply transformations or additional processing
   * to the value extracted by the {@code extract} method. For example, it can convert
   * the value to uppercase, format it, or wrap it in a custom object.
   *
   * <p><strong>Example Usage:</strong>
   * <pre>{@code
   * ConditionPropertyExtractor<String> extractor = new ConditionPropertyExtractor<>() {
   *   @Override
   *   public Object extract(Object entityOrExample, EntityProperty property, @Nullable Object value) {
   *     if (property.getName().equals("status") && value instanceof String status) {
   *       return "active".equalsIgnoreCase(status) ? 1 : 0;
   *     }
   *     return value;
   *   }
   *
   *   @Override
   *   public Object wrap(String extracted) {
   *     return extracted.toUpperCase();
   *   }
   * };
   *
   * String extractedValue = "example";
   * Object wrappedValue = extractor.wrap(extractedValue);
   * System.out.println(wrappedValue); // Output: EXAMPLE
   * }</pre>
   *
   * <p>In this example:
   * <ul>
   *   <li>The {@code wrap} method transforms the extracted string into uppercase.</li>
   * </ul>
   *
   * @param extracted the value extracted by the {@code extract} method
   * @return the wrapped or transformed value
   */
  Object wrap(T extracted);

}
