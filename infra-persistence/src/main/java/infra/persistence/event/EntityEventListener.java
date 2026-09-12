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

/**
 * Base contract for all entity lifecycle listeners.
 *
 * <p>Each lifecycle concern is modelled as a dedicated listener interface that
 * extends this contract and declares its own callbacks:
 * <ul>
 *   <li>{@link PersistingEventListener} — persist (insert) operations.</li>
 *   <li>{@link UpdatingEventListener} — update operations.</li>
 *   <li>{@link DeletingEventListener} — delete operations.</li>
 *   <li>{@link PostLoadEventListener} — entity load operations.</li>
 * </ul>
 *
 * <p>The entity type a listener observes is declared by its generic type
 * parameter, e.g. {@code UpdatingEventListener<ProjectProcess>} receives only
 * {@code ProjectProcess} events. A listener whose generic type cannot be resolved
 * observes every entity.
 *
 * <p>Listeners are invoked <strong>synchronously</strong> by the
 * {@link infra.persistence.EntityManager}; an exception thrown by a listener
 * therefore propagates to the caller.
 *
 * @param <T> the entity type to observe
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see PersistingEventListener
 * @see UpdatingEventListener
 * @see DeletingEventListener
 * @see PostLoadEventListener
 * @see EntityEventRegistry
 * @since 5.0
 */
public interface EntityEventListener<T> extends Listener {

}