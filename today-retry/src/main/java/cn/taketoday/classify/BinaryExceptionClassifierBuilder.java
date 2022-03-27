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

import java.util.ArrayList;

import cn.taketoday.lang.Assert;

/**
 * Fluent API for BinaryExceptionClassifier configuration.
 * <p>
 * Can be used in while list style: <pre>{@code
 * BinaryExceptionClassifier.newBuilder()
 * 			.retryOn(IOException.class)
 * 			.retryOn(IllegalArgumentException.class)
 * 			.build();
 * } </pre> or in black list style: <pre>{@code
 * BinaryExceptionClassifier.newBuilder()
 *            .notRetryOn(Error.class)
 *            .build();
 * } </pre>
 * <p>
 * Provides traverseCauses=false by default, and no default rules for exceptions.
 * <p>
 * Not thread safe. Building should be performed in a single thread, publishing of newly
 * created instance should be safe.
 *
 * @author Aleksandr Shamukov
 */
public class BinaryExceptionClassifierBuilder {

  /**
   * Building notation type (white list or black list) - null: has not selected yet -
   * true: white list - false: black list
   */
  private Boolean isWhiteList = null;

  private boolean traverseCauses = false;

  private final ArrayList<Class<? extends Throwable>> exceptionClasses = new ArrayList<>();

  public BinaryExceptionClassifierBuilder retryOn(Class<? extends Throwable> throwable) {
    Assert.isTrue(isWhiteList == null || isWhiteList, "Please use only retryOn() or only notRetryOn()");
    Assert.notNull(throwable, "Exception class can not be null");
    isWhiteList = true;
    exceptionClasses.add(throwable);
    return this;

  }

  public BinaryExceptionClassifierBuilder notRetryOn(Class<? extends Throwable> throwable) {
    Assert.isTrue(isWhiteList == null || !isWhiteList, "Please use only retryOn() or only notRetryOn()");
    Assert.notNull(throwable, "Exception class can not be null");
    isWhiteList = false;
    exceptionClasses.add(throwable);
    return this;
  }

  public BinaryExceptionClassifierBuilder traversingCauses() {
    this.traverseCauses = true;
    return this;
  }

  public BinaryExceptionClassifier build() {
    Assert.isTrue(!exceptionClasses.isEmpty(),
            "Attempt to build classifier with empty rules. To build always true, or always false "
                    + "instance, please use explicit rule for Throwable");
    BinaryExceptionClassifier classifier = new BinaryExceptionClassifier(exceptionClasses, isWhiteList // using
            // white
            // list
            // means
            // classifying
            // provided
            // classes
            // as
            // "true"
            // (is
            // retryable)
    );
    classifier.setTraverseCauses(traverseCauses);
    return classifier;
  }

}
