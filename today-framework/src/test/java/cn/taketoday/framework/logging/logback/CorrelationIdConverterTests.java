/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © Harry Yang & 2017 - 2023 All Rights Reserved.
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

package cn.taketoday.framework.logging.logback;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.LoggingEvent;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2023/7/2 16:34
 */
class CorrelationIdConverterTests {

  private final CorrelationIdConverter converter;

  private final LoggingEvent event = new LoggingEvent();

  CorrelationIdConverterTests() {
    this.converter = new CorrelationIdConverter();
    this.converter.setContext(new LoggerContext());
  }

  @Test
  void defaultPattern() {
    addMdcProperties(this.event);
    this.converter.start();
    String converted = this.converter.convert(this.event);
    this.converter.stop();
    assertThat(converted).isEqualTo("[01234567890123456789012345678901-0123456789012345] ");
  }

  @Test
  void customPattern() {
    this.converter.setOptionList(List.of("traceId(0)", "spanId(0)"));
    addMdcProperties(this.event);
    this.converter.start();
    String converted = this.converter.convert(this.event);
    this.converter.stop();
    assertThat(converted).isEqualTo("[01234567890123456789012345678901-0123456789012345] ");
  }

  private void addMdcProperties(LoggingEvent event) {
    event.setMDCPropertyMap(Map.of("traceId", "01234567890123456789012345678901", "spanId", "0123456789012345"));
  }

}