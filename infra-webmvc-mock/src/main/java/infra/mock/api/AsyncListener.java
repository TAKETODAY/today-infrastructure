/*
 * Copyright (c) 1997, 2023 Oracle and/or its affiliates and others.
 * All rights reserved.
 * Copyright 2004 The Apache Software Foundation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

// Modifications Copyright 2017 - 2026 the TODAY authors.

package infra.mock.api;

import java.io.IOException;
import java.util.EventListener;

/**
 * Listener that will be notified in the event that an asynchronous operation initiated on a ServletRequest to which the
 * listener had been added has completed, timed out, or resulted in an error.
 */
public interface AsyncListener extends EventListener {

  /**
   * Notifies this AsyncListener that an asynchronous operation has been completed.
   *
   * <p>
   * The {@link AsyncContext} corresponding to the asynchronous operation that has been completed may be obtained by
   * calling {@link AsyncEvent#getAsyncContext getAsyncContext} on the given <tt>event</tt>.
   *
   * <p>
   * In addition, if this AsyncListener had been registered via a call to
   * {@link AsyncContext#addListener(AsyncListener, MockRequest, MockResponse)}, the supplied ServletRequest and
   * ServletResponse objects may be retrieved by calling {@link AsyncEvent#getSuppliedRequest getSuppliedRequest} and
   * {@link AsyncEvent#getSuppliedResponse getSuppliedResponse}, respectively, on the given <tt>event</tt>.
   *
   * @param event the AsyncEvent indicating that an asynchronous operation has been completed
   * @throws IOException if an I/O related error has occurred during the processing of the given AsyncEvent
   */
  public void onComplete(AsyncEvent event) throws IOException;

  /**
   * Notifies this AsyncListener that an asynchronous operation has timed out.
   *
   * <p>
   * The {@link AsyncContext} corresponding to the asynchronous operation that has timed out may be obtained by calling
   * {@link AsyncEvent#getAsyncContext getAsyncContext} on the given <tt>event</tt>.
   *
   * <p>
   * In addition, if this AsyncListener had been registered via a call to
   * {@link AsyncContext#addListener(AsyncListener, MockRequest, MockResponse)}, the supplied ServletRequest and
   * ServletResponse objects may be retrieved by calling {@link AsyncEvent#getSuppliedRequest getSuppliedRequest} and
   * {@link AsyncEvent#getSuppliedResponse getSuppliedResponse}, respectively, on the given <tt>event</tt>.
   *
   * @param event the AsyncEvent indicating that an asynchronous operation has timed out
   * @throws IOException if an I/O related error has occurred during the processing of the given AsyncEvent
   */
  public void onTimeout(AsyncEvent event) throws IOException;

  /**
   * Notifies this AsyncListener that an asynchronous operation has failed to complete.
   *
   * <p>
   * The {@link AsyncContext} corresponding to the asynchronous operation that failed to complete may be obtained by
   * calling {@link AsyncEvent#getAsyncContext getAsyncContext} on the given <tt>event</tt>.
   *
   * <p>
   * In addition, if this AsyncListener had been registered via a call to
   * {@link AsyncContext#addListener(AsyncListener, MockRequest, MockResponse)}, the supplied ServletRequest and
   * ServletResponse objects may be retrieved by calling {@link AsyncEvent#getSuppliedRequest getSuppliedRequest} and
   * {@link AsyncEvent#getSuppliedResponse getSuppliedResponse}, respectively, on the given <tt>event</tt>.
   *
   * @param event the AsyncEvent indicating that an asynchronous operation has failed to complete
   * @throws IOException if an I/O related error has occurred during the processing of the given AsyncEvent
   */
  public void onError(AsyncEvent event) throws IOException;

  /**
   * Notifies this AsyncListener that a new asynchronous cycle is being initiated via a call to one of the
   * {@link MockRequest#startAsync} methods.
   *
   * <p>
   * The {@link AsyncContext} corresponding to the asynchronous operation that is being reinitialized may be obtained by
   * calling {@link AsyncEvent#getAsyncContext getAsyncContext} on the given <tt>event</tt>.
   *
   * <p>
   * In addition, if this AsyncListener had been registered via a call to
   * {@link AsyncContext#addListener(AsyncListener, MockRequest, MockResponse)}, the supplied ServletRequest and
   * ServletResponse objects may be retrieved by calling {@link AsyncEvent#getSuppliedRequest getSuppliedRequest} and
   * {@link AsyncEvent#getSuppliedResponse getSuppliedResponse}, respectively, on the given <tt>event</tt>.
   *
   * <p>
   * This AsyncListener will not receive any events related to the new asynchronous cycle unless it registers itself (via
   * a call to {@link AsyncContext#addListener}) with the AsyncContext that is delivered as part of the given AsyncEvent.
   *
   * @param event the AsyncEvent indicating that a new asynchronous cycle is being initiated
   * @throws IOException if an I/O related error has occurred during the processing of the given AsyncEvent
   */
  public void onStartAsync(AsyncEvent event) throws IOException;

}
