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

import org.junit.Assert;
import org.junit.Test;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.StreamCorruptedException;
import java.util.concurrent.TimeoutException;

import cn.taketoday.retry.support.RetryTemplate;

/**
 * @author Aleksandr Shamukov
 */
public class BinaryExceptionClassifierBuilderTests {

  @Test
  public void testWhiteList() {
    RetryTemplate.builder().infiniteRetry().retryOn(IOException.class).uniformRandomBackoff(1000, 3000).build();

    BinaryExceptionClassifier classifier = BinaryExceptionClassifier.builder().retryOn(IOException.class)
            .retryOn(TimeoutException.class).build();

    Assert.assertTrue(classifier.classify(new IOException()));
    // should not retry due to traverseCauses=fasle
    Assert.assertFalse(classifier.classify(new RuntimeException(new IOException())));
    Assert.assertTrue(classifier.classify(new StreamCorruptedException()));
    Assert.assertFalse(classifier.classify(new OutOfMemoryError()));
  }

  @Test
  public void testWhiteListWithTraverseCauses() {
    BinaryExceptionClassifier classifier = BinaryExceptionClassifier.builder().retryOn(IOException.class)
            .retryOn(TimeoutException.class).traversingCauses().build();

    Assert.assertTrue(classifier.classify(new IOException()));
    // should retry due to traverseCauses=true
    Assert.assertTrue(classifier.classify(new RuntimeException(new IOException())));
    Assert.assertTrue(classifier.classify(new StreamCorruptedException()));
    // should retry due to FileNotFoundException is a subclass of TimeoutException
    Assert.assertTrue(classifier.classify(new FileNotFoundException()));
    Assert.assertFalse(classifier.classify(new RuntimeException()));
  }

  @Test
  public void testBlackList() {
    BinaryExceptionClassifier classifier = BinaryExceptionClassifier.builder().notRetryOn(Error.class)
            .notRetryOn(InterruptedException.class).traversingCauses().build();

    // should not retry due to OutOfMemoryError is a subclass of Error
    Assert.assertFalse(classifier.classify(new OutOfMemoryError()));
    Assert.assertFalse(classifier.classify(new InterruptedException()));
    Assert.assertTrue(classifier.classify(new Throwable()));
    // should retry due to traverseCauses=true
    Assert.assertFalse(classifier.classify(new RuntimeException(new InterruptedException())));
  }

  @Test(expected = IllegalArgumentException.class)
  public void testFailOnNotationMix() {
    BinaryExceptionClassifier.builder().retryOn(IOException.class).notRetryOn(OutOfMemoryError.class);
  }

}
