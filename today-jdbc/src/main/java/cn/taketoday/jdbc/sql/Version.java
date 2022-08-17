/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2022 All Rights Reserved.
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

package cn.taketoday.jdbc.sql;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Specifies the version field or property of an entity class that
 * serves as its optimistic lock value.  The version is used to ensure
 * integrity when performing the merge operation and for optimistic
 * concurrency control.
 *
 * <p> Only a single <code>Version</code> property or field
 * should be used per class; applications that use more than one
 * <code>Version</code> property or field will not be portable.
 *
 * <p> The <code>Version</code> property should be mapped to
 * the primary table for the entity class; applications that
 * map the <code>Version</code> property to a table other than
 * the primary table will not be portable.
 *
 * <p> The following types are supported for version properties:
 * <code>int</code>, <code>Integer</code>, <code>short</code>,
 * <code>Short</code>, <code>long</code>, <code>Long</code>,
 * <code>java.sql.Timestamp</code>.
 *
 * <pre>
 *    Example:
 *
 *    &#064;Version
 *    &#064;Column(name="OPTLOCK")
 *    protected int getVersionNum() { return versionNum; }
 * </pre>
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/8/16 21:07
 */
@Target({ ElementType.METHOD, ElementType.FIELD })
@Retention(RetentionPolicy.RUNTIME)
public @interface Version {

}
