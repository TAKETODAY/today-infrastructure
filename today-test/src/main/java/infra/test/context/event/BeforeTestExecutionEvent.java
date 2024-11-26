/*
 * Copyright 2017 - 2024 the original author or authors.
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
 * along with this program. If not, see [https://www.gnu.org/licenses/]
 */

package infra.test.context.event;

import infra.test.context.TestContext;
import infra.test.context.TestExecutionListener;
import infra.test.context.event.annotation.BeforeTestExecution;

/**
 * {@link TestContextEvent} published by the {@link EventPublishingTestExecutionListener} when
 * {@link TestExecutionListener#beforeTestExecution(TestContext)}
 * is invoked.
 *
 * @author Frank Scheffler
 * @see BeforeTestExecution @BeforeTestExecution
 * @since 4.0
 */
@SuppressWarnings("serial")
public class BeforeTestExecutionEvent extends TestContextEvent {

  public BeforeTestExecutionEvent(TestContext source) {
    super(source);
  }

}
