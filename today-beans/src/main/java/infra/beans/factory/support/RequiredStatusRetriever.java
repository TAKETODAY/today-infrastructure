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

package infra.beans.factory.support;

import java.lang.annotation.Annotation;
import java.lang.reflect.AccessibleObject;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;

import infra.beans.factory.annotation.Autowired;
import infra.core.annotation.MergedAnnotation;
import infra.core.annotation.MergedAnnotations;
import infra.lang.TodayStrategies;

/**
 * Required Status Retriever
 *
 * <p>
 * detect AccessibleObject's Annotation to determine the 'required' status
 * </p>
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see Autowired
 * @since 4.0 2022/1/1 12:13
 */
public class RequiredStatusRetriever {

  private String requiredParameterName = "required";

  private boolean requiredParameterValue = true;

  private final LinkedHashSet<String> requiredAnnotationType = new LinkedHashSet<>(2);

  public RequiredStatusRetriever() {
    requiredAnnotationType.add(Autowired.class.getName());
    List<String> types = TodayStrategies.findNames(RequiredStatusRetriever.class.getName());
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
