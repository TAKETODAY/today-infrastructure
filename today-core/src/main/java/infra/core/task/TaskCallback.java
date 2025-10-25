/*
 * Copyright 2017 - 2025 the original author or authors.
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

package infra.core.task;

import java.util.concurrent.Callable;

/**
 * Variant of {@link Callable} with a flexible exception signature
 * that can be adapted in the {@link SyncTaskExecutor#execute(TaskCallback)}
 * method signature for propagating specific exception types only.
 *
 * <p>An implementation of this interface can also be passed into any
 * {@code Callback}-based method such as {@link AsyncTaskExecutor#submit(Callable)}
 * or {@link AsyncTaskExecutor#submitCompletable(Callable)}. It is just capable
 * of adapting to flexible exception propagation in caller signatures as well.
 *
 * @param <V> the returned value type, if any
 * @param <E> the exception propagated, if any
 * @author Juergen Hoeller
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @see SyncTaskExecutor#execute(TaskCallback)
 * @since 5.0
 */
public interface TaskCallback<V, E extends Exception> extends Callable<V> {

  @Override
  V call() throws E;

}
