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
package cn.taketoday.lang;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * A common annotation to declare that annotated elements is <b>Experimental</b>
 * that can change at any time, and has no guarantee of API stability and
 * backward-compatibility. If users want stabilization or signature change of a specific API that
 * is currently annotated {@code @Experimental}, please comment on its tracking issue on GitHub
 * with rationale, use-cases, and so forth, so that may prioritize the process toward
 * stabilization of the API.
 *
 * @author TODAY 2021/9/28 11:24
 * @since 4.0
 */
@Documented
@Retention(RetentionPolicy.SOURCE)
@Target({
        ElementType.METHOD, ElementType.PARAMETER, ElementType.FIELD,
        ElementType.PACKAGE, ElementType.TYPE, ElementType.ANNOTATION_TYPE,
        ElementType.CONSTRUCTOR
})
public @interface Experimental {

  /**
   * description
   */
  String value() default "";
}
