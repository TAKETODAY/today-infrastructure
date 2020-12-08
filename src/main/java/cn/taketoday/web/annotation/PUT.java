/**
 * Original Author -> 杨海健 (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2020 All Rights Reserved.
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
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package cn.taketoday.web.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import cn.taketoday.context.Constant;
import cn.taketoday.web.RequestMethod;

/**
 * @author TODAY <br>
 * 2018-07-01 14:07:11 2018-08-23 10:24 change add
 * <b>@ActionMapping(method = RequestMethod.PUT)
 */
@Retention(RetentionPolicy.RUNTIME)
@ActionMapping(method = RequestMethod.PUT)
@Target({ ElementType.METHOD, ElementType.TYPE })
public @interface PUT {

  /** urls */
  String[] value() default Constant.BLANK;

  /** Exclude url on class */
  boolean exclude() default false;

}
