/**
 * Original Author -> 杨海健 (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © Today & 2017 - 2018 All Rights Reserved.
 * 
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package cn.taketoday.jdbc.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * @author TODAY <br>
 *         2018-08-22 16:43
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.FIELD, ElementType.TYPE })
public @interface Column {

    /** column name */
    String value() default "";

    /** foreignKey */
    String foreignKey() default "";

    /** not null */
    boolean notNull() default false;

    /** the length of column */
    int length() default 32;

    /** decimal point */
    int decimal() default 0;

    String comment() default "";

    /**
     * The type of database. <b> varchar(6), float(6,5) in Mysql<b>
     */
    String type() default "";

    /** default */
    String defaultValue() default "";

    boolean zerofill() default false;

    boolean unsigned() default false;

    boolean increment() default false;

}
