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

import java.util.HashMap;
import java.util.Map;

/**
 * A {@link Classifier} that maps from String patterns with wildcards to a set of values
 * of a given type. An input String is matched with the most specific pattern possible to
 * the corresponding value in an input map. A default value should be specified with a
 * pattern key of "*".
 *
 * @param <T> the output of the classifier
 * @author Dave Syer
 */
@SuppressWarnings("serial")
public class PatternMatchingClassifier<T> implements Classifier<String, T> {

  private PatternMatcher<T> values;

  /**
   * Default constructor. Use the setter or the other constructor to create a sensible
   * classifier, otherwise all inputs will cause an exception.
   */
  public PatternMatchingClassifier() {
    this(new HashMap<>());
  }

  /**
   * Create a classifier from the provided map. The keys are patterns, using '?' as a
   * single character and '*' as multi-character wildcard.
   *
   * @param values the values to use in the {@link PatternMatcher}
   */
  public PatternMatchingClassifier(Map<String, T> values) {
    super();
    this.values = new PatternMatcher<T>(values);
  }

  /**
   * A map from pattern to value
   *
   * @param values the pattern map to set
   */
  public void setPatternMap(Map<String, T> values) {
    this.values = new PatternMatcher<>(values);
  }

  /**
   * Classify the input by matching it against the patterns provided in
   * {@link #setPatternMap(Map)}. The most specific pattern that matches will be used to
   * locate a value.
   *
   * @return the value matching the most specific pattern possible
   * @throws IllegalStateException if no matching value is found.
   */
  @Override
  public T classify(String classifiable) {
    return values.match(classifiable);
  }

}
