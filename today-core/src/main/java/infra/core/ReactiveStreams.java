/*
 * Copyright 2017 - 2026 the TODAY authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package infra.core;

import static infra.util.ClassUtils.isPresent;

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
