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

package cn.taketoday.validation;

import org.hibernate.validator.messageinterpolation.ResourceBundleMessageInterpolator;
import org.junit.jupiter.api.Test;

import cn.taketoday.context.MessageSource;
import cn.taketoday.util.ReflectionTestUtils;
import jakarta.validation.MessageInterpolator;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/2/19 21:56
 */
class MessageInterpolatorFactoryTests {

  @Test
  void getObjectShouldReturnResourceBundleMessageInterpolator() {
    MessageInterpolator interpolator = new MessageInterpolatorFactory().get();
    assertThat(interpolator).isInstanceOf(ResourceBundleMessageInterpolator.class);
  }

  @Test
  void getObjectShouldReturnMessageSourceMessageInterpolatorDelegateWithResourceBundleMessageInterpolator() {
    MessageSource messageSource = mock(MessageSource.class);
    MessageInterpolatorFactory interpolatorFactory = new MessageInterpolatorFactory(messageSource);
    MessageInterpolator interpolator = interpolatorFactory.get();
    assertThat(interpolator).isInstanceOf(MessageSourceMessageInterpolator.class);
    assertThat(interpolator).hasFieldOrPropertyWithValue("messageSource", messageSource);
    assertThat(ReflectionTestUtils.getField(interpolator, "messageInterpolator"))
            .isInstanceOf(ResourceBundleMessageInterpolator.class);
  }

}
