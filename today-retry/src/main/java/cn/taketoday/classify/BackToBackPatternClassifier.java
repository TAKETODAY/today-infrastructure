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

import java.util.Map;

/**
 * A special purpose {@link Classifier} with easy configuration options for mapping from
 * one arbitrary type of object to another via a pattern matcher.
 *
 * @param <C> the type of thing to classify
 * @param <T> the output of the classifier
 * @author Dave Syer
 */
@SuppressWarnings("serial")
public class BackToBackPatternClassifier<C, T> implements Classifier<C, T> {

  private Classifier<C, String> router;

  private Classifier<String, T> matcher;

  /**
   * Default constructor, provided as a convenience for people using setter injection.
   */
  public BackToBackPatternClassifier() {
  }

  /**
   * Set up a classifier with input to the router and output from the matcher.
   *
   * @param router see {@link #setRouterDelegate(Object)}
   * @param matcher see {@link #setMatcherMap(Map)}
   */
  public BackToBackPatternClassifier(Classifier<C, String> router, Classifier<String, T> matcher) {
    super();
    this.router = router;
    this.matcher = matcher;
  }

  /**
   * A convenience method for creating a pattern matching classifier for the matcher
   * component.
   *
   * @param map maps pattern keys with wildcards to output values
   */
  public void setMatcherMap(Map<String, T> map) {
    this.matcher = new PatternMatchingClassifier<>(map);
  }

  /**
   * A convenience method of creating a router classifier based on a plain old Java
   * Object. The object provided must have precisely one public method that either has
   * the <code>@Classifier</code> annotation or accepts a single argument and outputs a
   * String. This will be used to create an input classifier for the router component.
   *
   * @param delegate the delegate object used to create a router classifier
   */
  public void setRouterDelegate(Object delegate) {
    this.router = new ClassifierAdapter<>(delegate);
  }

  /**
   * Classify the input and map to a String, then take that and put it into a pattern
   * matcher to match to an output value.
   */
  @Override
  public T classify(C classifiable) {
    return this.matcher.classify(this.router.classify(classifiable));
  }

}
