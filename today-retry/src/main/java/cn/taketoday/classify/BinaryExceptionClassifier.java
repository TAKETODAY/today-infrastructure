/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2022 All Rights Reserved.
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
package cn.taketoday.classify;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * A {@link Classifier} for exceptions that has only two classes (true and false).
 * Classifies objects according to their inheritance relation with the supplied types. If
 * the object to be classified is one of the provided types, or is a subclass of one of
 * the types, then the non-default value is returned (usually true).
 *
 * @author Dave Syer
 * @author Gary Russell
 * @see SubclassClassifier
 */
@SuppressWarnings("serial")
public class BinaryExceptionClassifier extends SubclassClassifier<Throwable, Boolean> {

  private boolean traverseCauses;

  public static BinaryExceptionClassifierBuilder builder() {
    return new BinaryExceptionClassifierBuilder();
  }

  public static BinaryExceptionClassifier defaultClassifier() {
    // create new instance for each call due to mutability
    return new BinaryExceptionClassifier(
            Collections.<Class<? extends Throwable>, Boolean>singletonMap(Exception.class, true), false);
  }

  /**
   * Create a binary exception classifier with the provided default value.
   *
   * @param defaultValue defaults to false
   */
  public BinaryExceptionClassifier(boolean defaultValue) {
    super(defaultValue);
  }

  /**
   * Create a binary exception classifier with the provided classes and their
   * subclasses. The mapped value for these exceptions will be the one provided (which
   * will be the opposite of the default).
   *
   * @param exceptionClasses the exceptions to classify among
   * @param value the value to classify
   */
  public BinaryExceptionClassifier(Collection<Class<? extends Throwable>> exceptionClasses, boolean value) {
    this(!value);
    if (exceptionClasses != null) {
      Map<Class<? extends Throwable>, Boolean> map = new HashMap<Class<? extends Throwable>, Boolean>();
      for (Class<? extends Throwable> type : exceptionClasses) {
        map.put(type, !getDefault());
      }
      setTypeMap(map);
    }
  }

  /**
   * Create a binary exception classifier with the default value false and value mapping
   * true for the provided classes and their subclasses.
   *
   * @param exceptionClasses the exception types to throw
   */
  public BinaryExceptionClassifier(Collection<Class<? extends Throwable>> exceptionClasses) {
    this(exceptionClasses, true);
  }

  /**
   * Create a binary exception classifier using the given classification map and a
   * default classification of false.
   *
   * @param typeMap the map of types
   */
  public BinaryExceptionClassifier(Map<Class<? extends Throwable>, Boolean> typeMap) {
    this(typeMap, false);
  }

  /**
   * Create a binary exception classifier using the given classification map and the
   * given value for default class.
   *
   * @param defaultValue the default value to use
   * @param typeMap the map of types to classify
   */
  public BinaryExceptionClassifier(Map<Class<? extends Throwable>, Boolean> typeMap, boolean defaultValue) {
    super(typeMap, defaultValue);
  }

  /**
   * Create a binary exception classifier.
   *
   * @param defaultValue the default value to use
   * @param typeMap the map of types to classify
   * @param traverseCauses if true, throwable's causes will be inspected to find
   * non-default class
   */
  public BinaryExceptionClassifier(Map<Class<? extends Throwable>, Boolean> typeMap, boolean defaultValue,
          boolean traverseCauses) {
    super(typeMap, defaultValue);
    this.traverseCauses = traverseCauses;
  }

  public void setTraverseCauses(boolean traverseCauses) {
    this.traverseCauses = traverseCauses;
  }

  @Override
  public Boolean classify(Throwable classifiable) {
    Boolean classified = super.classify(classifiable);
    if (!this.traverseCauses) {
      return classified;
    }

    /*
     * If the result is the default, we need to find out if it was by default or so
     * configured; if default, try the cause(es).
     */
    if (classified.equals(this.getDefault())) {
      Throwable cause = classifiable;
      do {
        if (this.getClassified().containsKey(cause.getClass())) {
          return classified; // non-default classification
        }
        cause = cause.getCause();
        classified = super.classify(cause);
      }
      while (cause != null && classified.equals(this.getDefault()));
    }

    return classified;
  }

}
