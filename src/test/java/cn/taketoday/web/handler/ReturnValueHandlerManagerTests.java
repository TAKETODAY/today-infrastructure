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

package cn.taketoday.web.handler;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.nio.charset.StandardCharsets;
import java.util.List;

import cn.taketoday.http.HttpStatus;
import cn.taketoday.http.converter.StringHttpMessageConverter;
import cn.taketoday.web.handler.method.ActionMappingAnnotationHandler;
import cn.taketoday.web.handler.method.HandlerMethod;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/2/12 19:34
 */
class ReturnValueHandlerManagerTests {

  @Test
  void returnValueHandlerManager() {
    ReturnValueHandlerManager manager = new ReturnValueHandlerManager();
    assertThat(manager.getMessageConverters()).isNotEmpty().hasSize(3);

    ReturnValueHandlerManager manager1 = new ReturnValueHandlerManager(manager.getMessageConverters());

    assertThat(manager1.getMessageConverters()).isNotEmpty().hasSize(3);
    assertThat(manager1).isNotEqualTo(manager);

    manager.setMessageConverters(List.of(new StringHttpMessageConverter(StandardCharsets.US_ASCII)));
    assertThat(manager.getMessageConverters()).isNotEmpty().hasSize(1);

    assertThat(manager.toString()).isNotEmpty();
    manager.hashCode();
  }

  @Test
  void addHandler() {
    ReturnValueHandlerManager manager = new ReturnValueHandlerManager();
    HttpStatusReturnValueHandler returnValueHandler = new HttpStatusReturnValueHandler();
    manager.addHandlers(returnValueHandler);
    assertThat(manager.getHandlers()).hasSize(1);

    HttpStatusReturnValueHandler highestValueHandler = new HttpStatusReturnValueHandler();
    manager.addHandlers(List.of(highestValueHandler));

    assertThat(manager.getHandlers()).hasSize(2);

    assertThat(manager.getByReturnValue(HttpStatus.OK)).isNotNull();

    assertThat(manager.getByReturnValue("")).isNull();
    assertThat(manager.getByReturnValue(null)).isNull();

    assertThat(manager.getHandler("")).isNull();

    // getHandler(handler)

    HandlerMethod handler = Mockito.mock(HandlerMethod.class);
    Mockito.when(handler.isReturn(HttpStatus.class))
            .thenReturn(true);

    ActionMappingAnnotationHandler annotationHandler = new ActionMappingAnnotationHandler(handler, null) {
      @Override
      protected Object getHandlerBean() {
        return null;
      }
    };

    assertThat(manager.getHandler(annotationHandler))
            .isNotNull();

    // obtainHandler

    assertThat(manager.obtainHandler(annotationHandler))
            .isEqualTo(returnValueHandler)
            .isNotNull();

    // sort

    returnValueHandler.setOrder(2);
    highestValueHandler.setOrder(1);

    manager.sort();
    assertThat(manager.obtainHandler(annotationHandler))
            .isEqualTo(highestValueHandler)
            .isNotNull();

    returnValueHandler.setOrder(1);
    highestValueHandler.setOrder(2);

    manager.sort(manager.getHandlers());
    assertThat(manager.obtainHandler(annotationHandler))
            .isEqualTo(returnValueHandler)
            .isNotNull();

    assertThatThrownBy(() -> manager.obtainHandler(""))
            .isInstanceOf(ReturnValueHandlerNotFoundException.class)
            .hasMessageStartingWith("No ReturnValueHandler for handler");

  }

  @Test
  void contains() {
    ReturnValueHandlerManager manager = new ReturnValueHandlerManager();
    HttpStatusReturnValueHandler returnValueHandler = new HttpStatusReturnValueHandler();
    manager.addHandlers(returnValueHandler);

    assertThat(manager.contains(null)).isFalse();
    assertThat(manager.contains(String.class)).isFalse();
    assertThat(manager.contains(HttpStatusReturnValueHandler.class)).isTrue();

    assertThat(manager.get(HttpStatusReturnValueHandler.class)).isNotNull();

  }

}
