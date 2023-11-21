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

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import cn.taketoday.core.task.TaskDecorator;
import cn.taketoday.lang.Assert;

/**
 * Composite {@link TaskDecorator} that delegates to other task decorators.
 *
 * @author Tadaya Tsuyukubo
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
public class CompositeTaskDecorator implements TaskDecorator {

  private final List<TaskDecorator> taskDecorators;

  /**
   * Create a new instance.
   *
   * @param taskDecorators the taskDecorators to delegate to
   */
  public CompositeTaskDecorator(Collection<? extends TaskDecorator> taskDecorators) {
    Assert.notNull(taskDecorators, "TaskDecorators is required");
    this.taskDecorators = new ArrayList<>(taskDecorators);
  }

  @Override
  public Runnable decorate(Runnable runnable) {
    Assert.notNull(runnable, "Runnable is required");
    for (TaskDecorator taskDecorator : this.taskDecorators) {
      runnable = taskDecorator.decorate(runnable);
    }
    return runnable;
  }

}
