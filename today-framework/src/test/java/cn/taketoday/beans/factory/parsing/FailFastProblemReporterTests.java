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

package cn.taketoday.beans.factory.parsing;

import org.junit.jupiter.api.Test;

import cn.taketoday.core.io.DescriptiveResource;
import cn.taketoday.logging.Logger;

import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * @author Rick Evans
 * @author Juergen Hoeller
 * @author Chris Beams
 */
public class FailFastProblemReporterTests {

  @Test
  public void testError() throws Exception {
    FailFastProblemReporter reporter = new FailFastProblemReporter();
    assertThatExceptionOfType(BeanDefinitionParsingException.class).isThrownBy(() ->
            reporter.error(new Problem("VGER", new Location(new DescriptiveResource("here")),
                    null, new IllegalArgumentException())));
  }

  @Test
  public void testWarn() throws Exception {
    Problem problem = new Problem("VGER", new Location(new DescriptiveResource("here")),
            null, new IllegalArgumentException());

    Logger log = mock(Logger.class);

    FailFastProblemReporter reporter = new FailFastProblemReporter();
    reporter.setLogger(log);
    reporter.warning(problem);

    verify(log).warn(any(), isA(IllegalArgumentException.class));
  }

}
