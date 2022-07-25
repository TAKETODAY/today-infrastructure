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

package cn.taketoday.aop.framework.std;

import java.lang.reflect.Method;

/**
 * @author TODAY 2021/3/7 20:13
 * @since 3.0
 */
public interface ProxyMethodGenerator {

  String FIELD_TARGET = "target";
  String FIELD_CONFIG = "config";
  String FIELD_TARGET_SOURCE = "targetSource";

  /**
   * @return generate status
   */
  boolean generate(Method method, GeneratorContext context);

}
