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

package cn.taketoday.core;

import static cn.taketoday.util.ClassUtils.isPresent;

/**
 * A common delegate for detecting Reactive presence AND its features
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2023/11/25 14:26
 */
public abstract class ReactiveStreams {
  public static final String INDICATOR_CLASS = "org.reactivestreams.Publisher";
  public static final String REACTOR_INDICATOR_CLASS = "reactor.core.publisher.Flux";

  public static final boolean isPresent = isPresent(INDICATOR_CLASS, ReactiveStreams.class);
  public static final boolean reactorPresent = isPresent(REACTOR_INDICATOR_CLASS, ReactiveStreams.class);
  public static final boolean mutinyPresent = isPresent("io.smallrye.mutiny.Multi", ReactiveStreams.class);
  public static final boolean rxjava3Present = isPresent("io.reactivex.rxjava3.core.Flowable", ReactiveStreams.class);

}
