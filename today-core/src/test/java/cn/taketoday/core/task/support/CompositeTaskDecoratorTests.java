/*
 * Copyright 2017 - 2023 the original author or authors.
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

package cn.taketoday.core.task.support;

import org.junit.jupiter.api.Test;
import org.mockito.InOrder;

import java.util.List;

import cn.taketoday.core.task.TaskDecorator;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2023/8/26 17:21
 */
class CompositeTaskDecoratorTests {

  @Test
  void createWithNullCollection() {
    assertThatIllegalArgumentException().isThrownBy(() -> new CompositeTaskDecorator(null))
            .withMessage("TaskDecorators must not be null");
  }

  @Test
  void decorateWithNullRunnable() {
    CompositeTaskDecorator taskDecorator = new CompositeTaskDecorator(List.of());
    assertThatIllegalArgumentException().isThrownBy(() -> taskDecorator.decorate(null))
            .withMessage("Runnable must not be null");
  }

  @Test
  void decorate() {
    TaskDecorator first = mockNoOpTaskDecorator();
    TaskDecorator second = mockNoOpTaskDecorator();
    TaskDecorator third = mockNoOpTaskDecorator();
    CompositeTaskDecorator taskDecorator = new CompositeTaskDecorator(List.of(first, second, third));
    Runnable runnable = mock();
    taskDecorator.decorate(runnable);
    InOrder ordered = inOrder(first, second, third);
    ordered.verify(first).decorate(runnable);
    ordered.verify(second).decorate(runnable);
    ordered.verify(third).decorate(runnable);
  }

  @Test
  void decorateReusesResultOfPreviousRun() {
    Runnable original = mock();
    Runnable firstDecorated = mock();
    TaskDecorator first = mock();
    given(first.decorate(original)).willReturn(firstDecorated);
    Runnable secondDecorated = mock();
    TaskDecorator second = mock();
    given(second.decorate(firstDecorated)).willReturn(secondDecorated);
    Runnable result = new CompositeTaskDecorator(List.of(first, second)).decorate(original);
    assertThat(result).isSameAs(secondDecorated);
    verify(first).decorate(original);
    verify(second).decorate(firstDecorated);
  }

  private TaskDecorator mockNoOpTaskDecorator() {
    TaskDecorator mock = mock();
    given(mock.decorate(any())).willAnswer(invocation -> invocation.getArguments()[0]);
    return mock;
  }

}