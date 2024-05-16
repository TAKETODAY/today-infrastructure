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

package cn.taketoday.beans.factory;

import cn.taketoday.beans.FatalBeanException;

/**
 * Exception thrown when the validation of a bean definition failed.
 *
 * @author TODAY 2021/9/29 10:42
 * @since 4.0
 */
public class BeanDefinitionValidationException extends FatalBeanException {

  public BeanDefinitionValidationException(String msg) {
    super(msg);
  }

}
