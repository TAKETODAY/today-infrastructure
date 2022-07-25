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

package cn.taketoday.beans.factory.support;

import java.lang.annotation.Annotation;
import java.lang.reflect.AccessibleObject;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;

import cn.taketoday.beans.factory.annotation.Autowired;
import cn.taketoday.core.annotation.MergedAnnotation;
import cn.taketoday.core.annotation.MergedAnnotations;
import cn.taketoday.lang.TodayStrategies;

/**
 * Required Status Retriever
 *
 * <p>
 * detect AccessibleObject's Annotation to determine the 'required' status
 * </p>
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see cn.taketoday.beans.factory.annotation.Autowired
 * @since 4.0 2022/1/1 12:13
 */
public class RequiredStatusRetriever {

  private String requiredParameterName = "required";

  private boolean requiredParameterValue = true;

  private final LinkedHashSet<String> requiredAnnotationType = new LinkedHashSet<>(2);

  public RequiredStatusRetriever() {
    requiredAnnotationType.add(Autowired.class.getName());
    List<String> types = TodayStrategies.get(RequiredStatusRetriever.class.getName());
    requiredAnnotationType.addAll(types);
  }

  public RequiredStatusRetriever(Class<?> annotationType) {
    requiredAnnotationType.add(annotationType.getName());
  }

  /**
   * Set the name of an attribute of the annotation that specifies whether it is required.
   *
   * @see #setRequiredParameterValue(boolean)
   */
  public void setRequiredParameterName(String requiredParameterName) {
    this.requiredParameterName = requiredParameterName;
  }

  /**
   * Set the boolean value that marks a dependency as required.
   * <p>For example if using 'required=true' (the default), this value should be
   * {@code true}; but if using 'optional=false', this value should be {@code false}.
   *
   * @see #setRequiredParameterName(String)
   */
  public void setRequiredParameterValue(boolean requiredParameterValue) {
    this.requiredParameterValue = requiredParameterValue;
  }

  /**
   * retrieve the 'required' status, detect AccessibleObject's Annotation
   */
  public boolean retrieve(AccessibleObject accessible) {
    MergedAnnotations annotations = MergedAnnotations.from(accessible);
    for (String annotationType : requiredAnnotationType) {
      MergedAnnotation<? extends Annotation> source = annotations.get(annotationType);
      if (source.isPresent()) {
        Optional<Boolean> optional = source.getValue(requiredParameterName, Boolean.class);
        if (optional.isPresent()) {
          if (optional.get() != requiredParameterValue) {
            return false; // is not required
          }
        }
      }
    }
    // default is required
    return true;
  }

}
