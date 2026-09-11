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

package infra.persistence.event;

import java.util.EventListener;

/**
 * Marker interface for all persistence listeners that can be registered with an
 * {@link EntityEventRegistry}.
 *
 * <p>Known listener types:
 * <ul>
 *   <li>{@link EntityEventListener} — observes entity lifecycle events
 *   (persist / update / delete).</li>
 *   <li>{@link BatchPersistListener} — observes batch persist operations.</li>
 * </ul>
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see infra.persistence.event.EntityEventRegistry
 * @since 5.0
 */
public interface Listener extends EventListener {
}