/*
 * Original Author -> 杨海健 (taketoday@foxmail.com) https://taketoday.cn
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

package cn.taketoday.aop.proxy;

import java.util.HashMap;
import java.util.Map;

import cn.taketoday.aop.intercept.StandardMethodInvocation.Target;

/**
 * @author TODAY
 * 2020/10/30 22:32
 */
public abstract class InvocationRegistry {

  private static final Map<String, Target> targetMap = new HashMap<>();

  public static Target getTarget(String key) {
    return targetMap.get(key);
  }

  public static void putTarget(String key, Target target) {
    targetMap.put(key, target);
  }

}
