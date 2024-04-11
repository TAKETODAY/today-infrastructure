/*
 * Copyright 2017 - 2024 the original author or authors.
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

package cn.taketoday.persistence;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Specifies the primary key of an entity.
 * The field or property to which the <code>GeneratedId</code> annotation
 * is applied should be one of the following types: any Java primitive type;
 * any primitive wrapper type;
 * <code>java.math.BigDecimal</code>;
 * <code>java.math.BigInteger</code>.
 *
 * <p>The mapped column for the primary key of the entity is assumed
 * to be the primary key of the primary table. If no <code>Column</code> annotation
 * is specified, the primary key column name is assumed to be the name
 * of the primary key property or field.
 *
 * <pre>{@code
 *   // Example:
 *
 *   @GeneratedId
 *   public Long getId() {
 *     return id;
 *   }
 * }</pre>
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 1.0 2024/2/14 21:11
 */
@Id
@Documented
@Target({ ElementType.ANNOTATION_TYPE, ElementType.FIELD, ElementType.METHOD })
@Retention(RetentionPolicy.RUNTIME)
public @interface GeneratedId {

}
