/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2021 All Rights Reserved.
 *
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER
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
 * along with this program.  If not, see [http://www.gnu.org/licenses/]
 */

package cn.taketoday.web.config.jackson;

import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.KeyDeserializer;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import cn.taketoday.core.annotation.AliasFor;
import cn.taketoday.lang.Component;

/**
 * {@link Component @Component} that provides {@link JsonSerializer},
 * {@link JsonDeserializer} or {@link KeyDeserializer} implementations to be registered
 * with Jackson when {@link JsonComponentModule} is in use. Can be used to annotate
 * implementations directly or a class that contains them as inner-classes. For example:
 * <pre class="code">
 * &#064;JsonComponent
 * public class CustomerJsonComponent {
 *
 *     public static class Serializer extends JsonSerializer&lt;Customer&gt; {
 *
 *         // ...
 *
 *     }
 *
 *     public static class Deserializer extends JsonDeserializer&lt;Customer&gt; {
 *
 *         // ...
 *
 *     }
 *
 * }
 *
 * </pre>
 *
 * @author Phillip Webb
 * @author Paul Aly
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see JsonComponentModule
 * @since 4.0
 */
@Component
@Documented
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface JsonComponent {

  /**
   * The value may indicate a suggestion for a logical component name, to be turned into
   * a bean in case of an autodetected component.
   *
   * @return the component name
   */
  @AliasFor(annotation = Component.class)
  String value() default "";

  /**
   * The types that are handled by the provided serializer/deserializer. This attribute
   * is mandatory for a {@link KeyDeserializer}, as the type cannot be inferred. For a
   * {@link JsonSerializer} or {@link JsonDeserializer} it can be used to limit handling
   * to a subclasses of type inferred from the generic.
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
     * @see JsonSerializer
     * @see JsonDeserializer
     */
    VALUES,

    /**
     * A serializer/deserializer for keys.
     *
     * @see JsonSerializer
     * @see KeyDeserializer
     */
    KEYS

  }

}
