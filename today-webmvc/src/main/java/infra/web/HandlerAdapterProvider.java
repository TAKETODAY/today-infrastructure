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

package infra.web;

/**
 * <p>
 * <b>Note:</b> This framework allows hander use
 * {@link HandlerAdapterProvider HandlerAdapterCapable}
 * to specific a HandlerAdapter at startup time
 *
 * @author TODAY <br>
 * 2019-12-28 14:12
 */
@FunctionalInterface
public interface HandlerAdapterProvider {

  /**
   * Get {@link HandlerAdapter}
   *
   * @return Never be null
   */
  HandlerAdapter getHandlerAdapter();
}
