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

package cn.taketoday.aop.proxy.std;

import java.util.ArrayList;
import java.util.List;

import cn.taketoday.aop.framework.AdvisedSupport;
import cn.taketoday.core.bytecode.Type;
import cn.taketoday.core.bytecode.core.ClassEmitter;

/**
 * @author TODAY 2021/3/7 20:17
 * @since 3.0
 */
public class GeneratorContext {

  final Type targetType;
  final Class<?> targetClass;
  final AdvisedSupport config;
  final ClassEmitter classEmitter;

  final List<String> fields = new ArrayList<>();

  public GeneratorContext(Type targetType, AdvisedSupport config, ClassEmitter classEmitter, Class<?> targetClass) {
    this.targetType = targetType;
    this.config = config;
    this.classEmitter = classEmitter;
    this.targetClass = targetClass;
  }

  public AdvisedSupport getConfig() {
    return config;
  }

  public Type getTargetType() {
    return targetType;
  }

  public Class<?> getTargetClass() {
    return targetClass;
  }

  public ClassEmitter getClassEmitter() {
    return classEmitter;
  }

  public void addField(String field) {
    fields.add(field);
  }

  public List<String> getFields() {
    return fields;
  }
}
