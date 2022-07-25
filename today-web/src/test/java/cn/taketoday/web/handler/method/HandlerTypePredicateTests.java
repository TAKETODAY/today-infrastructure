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

package cn.taketoday.web.handler.method;

import org.junit.jupiter.api.Test;

import java.util.function.Predicate;

import cn.taketoday.stereotype.Controller;
import cn.taketoday.web.annotation.RestController;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/1/28 10:30
 */
class HandlerTypePredicateTests {

  @Test
  public void forAnnotation() {

    Predicate<Class<?>> predicate = HandlerTypePredicate.forAnnotation(Controller.class);

    assertThat(predicate.test(HtmlController.class)).isTrue();
    assertThat(predicate.test(ApiController.class)).isTrue();
    assertThat(predicate.test(AnotherApiController.class)).isTrue();
  }

  @Test
  public void forAnnotationWithException() {

    Predicate<Class<?>> predicate = HandlerTypePredicate.forAnnotation(Controller.class)
            .and(HandlerTypePredicate.forAssignableType(Special.class));

    assertThat(predicate.test(HtmlController.class)).isFalse();
    assertThat(predicate.test(ApiController.class)).isFalse();
    assertThat(predicate.test(AnotherApiController.class)).isTrue();
  }

  @Controller
  private static class HtmlController { }

  @RestController
  private static class ApiController { }

  @RestController
  private static class AnotherApiController implements Special { }

  interface Special { }

}
