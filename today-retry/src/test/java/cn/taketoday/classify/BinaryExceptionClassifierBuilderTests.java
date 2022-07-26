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

import org.junit.jupiter.api.Test;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.StreamCorruptedException;
import java.util.concurrent.TimeoutException;

import cn.taketoday.retry.support.RetryTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

/**
 * @author Aleksandr Shamukov
 */
public class BinaryExceptionClassifierBuilderTests {

  @Test
  public void testWhiteList() {
    RetryTemplate.builder().infiniteRetry().retryOn(IOException.class).uniformRandomBackoff(1000, 3000).build();

    BinaryExceptionClassifier classifier = BinaryExceptionClassifier.builder().retryOn(IOException.class)
            .retryOn(TimeoutException.class).build();

    assertThat(classifier.classify(new IOException())).isTrue();
    // should not retry due to traverseCauses=fasle
    assertThat(classifier.classify(new RuntimeException(new IOException()))).isFalse();
    assertThat(classifier.classify(new StreamCorruptedException())).isTrue();
    assertThat(classifier.classify(new OutOfMemoryError())).isFalse();
  }

  @Test
  public void testWhiteListWithTraverseCauses() {
    BinaryExceptionClassifier classifier = BinaryExceptionClassifier.builder().retryOn(IOException.class)
            .retryOn(TimeoutException.class).traversingCauses().build();

    assertThat(classifier.classify(new IOException())).isTrue();
    // should retry due to traverseCauses=true
    assertThat(classifier.classify(new RuntimeException(new IOException()))).isTrue();
    assertThat(classifier.classify(new StreamCorruptedException())).isTrue();
    // should retry due to FileNotFoundException is a subclass of TimeoutException
    assertThat(classifier.classify(new FileNotFoundException())).isTrue();
    assertThat(classifier.classify(new RuntimeException())).isFalse();
  }

  @Test
  public void testBlackList() {
    BinaryExceptionClassifier classifier = BinaryExceptionClassifier.builder().notRetryOn(Error.class)
            .notRetryOn(InterruptedException.class).traversingCauses().build();

    // should not retry due to OutOfMemoryError is a subclass of Error
    assertThat(classifier.classify(new OutOfMemoryError())).isFalse();
    assertThat(classifier.classify(new InterruptedException())).isFalse();
    assertThat(classifier.classify(new Throwable())).isTrue();
    // should retry due to traverseCauses=true
    assertThat(classifier.classify(new RuntimeException(new InterruptedException()))).isFalse();
  }

  @Test
  public void testFailOnNotationMix() {
    assertThatIllegalArgumentException().isThrownBy(() -> BinaryExceptionClassifier.builder()
            .retryOn(IOException.class).notRetryOn(OutOfMemoryError.class));
  }

}
