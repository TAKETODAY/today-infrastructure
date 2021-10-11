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
package cn.taketoday.context;

import java.lang.reflect.AnnotatedElement;

import cn.taketoday.context.annotation.Profile;
import cn.taketoday.context.loader.ConditionEvaluationContext;
import cn.taketoday.core.AnnotationAttributes;
import cn.taketoday.lang.Constant;
import cn.taketoday.core.annotation.AnnotationUtils;
import cn.taketoday.core.env.Environment;

/**
 * Resolve {@link Profile} {@link Condition}
 *
 * @author TODAY 2018-11-14 18:52
 */
public class ProfileCondition implements Condition {

  @Override
  public boolean matches(ConditionEvaluationContext context, AnnotatedElement annotated) {
    Environment environment = context.getEnvironment();
    AnnotationAttributes[] attributesArray = AnnotationUtils.getAttributesArray(annotated, Profile.class);
    for (AnnotationAttributes attributes : attributesArray) {
      if (environment.acceptsProfiles(attributes.getStringArray(Constant.VALUE))) {
        return true;
      }
    }
    return false;
  }

}
