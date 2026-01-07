/*
 * Copyright 2017 - 2026 the original author or authors.
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

package infra.app.jackson;

import com.google.gson.JsonDeserializer;
import com.google.gson.JsonSerializer;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import infra.core.annotation.AliasFor;
import infra.stereotype.Component;
import tools.jackson.databind.KeyDeserializer;
import tools.jackson.databind.ValueDeserializer;
import tools.jackson.databind.ValueSerializer;

/**
 * {@link Component @Component} that provides {@link JsonSerializer},
 * {@link JsonDeserializer} or {@link KeyDeserializer} implementations to be registered
 * with Jackson when {@link JacksonComponentModule} is in use. Can be used to annotate
 * implementations directly or a class that contains them as inner-classes. For example:
 * <pre>{@code
 * @JsonComponent
 * public class CustomerJsonComponent {
 *
 *     public static class Serializer extends JsonSerializer<Customer> {
 *
 *         // ...
 *
 *     }
 *
 *     public static class Deserializer extends JsonDeserializer<Customer> {
 *
 *         // ...
 *
 *     }
 *
 * }
 *
 * }</pre>
 *
 * @author Phillip Webb
 * @author Paul Aly
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see JacksonComponentModule
 * @since 4.0
 */
@Component
@Documented
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface JacksonComponent {

  /**
   * The value may indicate a suggestion for a logical component name, to be turned into
   * a Infra bean in case of an auto-detected component.
   *
   * @return the component name
   */
  @AliasFor(annotation = Component.class)
  String value() default "";

  /**
   * The types that are handled by the provided serializer/deserializer. This attribute
   * is mandatory for a {@link KeyDeserializer}, as the type cannot be inferred. For a
   * {@link ValueSerializer} or {@link ValueDeserializer} it can be used to limit
   * handling to a subclasses of type inferred from the generic.
   *
   * @return the types that should be handled by the component
   */
  Class<?>[] type() default {};

  /**
   * The scope under which the serializer/deserializer should be registered with the
   * module.
   *
   * @return the component's handle type
   */
  Scope scope() default Scope.VALUES;

  /**
   * The various scopes under which a serializer/deserializer can be registered.
   */
  enum Scope {

    /**
     * A serializer/deserializer for regular value content.
     *
     * @see ValueSerializer
     * @see ValueDeserializer
     */
    VALUES,

    /**
     * A serializer/deserializer for keys.
     *
     * @see ValueSerializer
     * @see ValueDeserializer
     */
    KEYS

  }

}
