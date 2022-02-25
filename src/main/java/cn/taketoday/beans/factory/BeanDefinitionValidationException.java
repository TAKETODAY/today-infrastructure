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
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package cn.taketoday.beans.factory;

import cn.taketoday.beans.FatalBeanException;
import cn.taketoday.beans.factory.support.BeanDefinition;

/**
 * Exception thrown when the validation of a bean definition failed.
 *
 * @author TODAY 2021/9/29 10:42
 * @see BeanDefinition#validate()
 * @since 4.0
 */
@SuppressWarnings("serial")
public class BeanDefinitionValidationException extends FatalBeanException {

  public BeanDefinitionValidationException(String msg) {
    super(msg);
  }

  public BeanDefinitionValidationException(String msg, Throwable cause) {
    super(msg, cause);
  }

}
