/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2022 All Rights Reserved.
 *
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER
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

package cn.taketoday.web.handler.mvc;

import cn.taketoday.http.HttpHeaders;
import cn.taketoday.http.HttpMethod;
import cn.taketoday.lang.Nullable;
import cn.taketoday.web.RequestContext;
import cn.taketoday.web.RequestContextUtils;
import cn.taketoday.web.WebContentGenerator;
import cn.taketoday.web.session.WebSession;
import cn.taketoday.web.util.WebUtils;
import cn.taketoday.web.view.ModelAndView;

/**
 * Convenient superclass for controller implementations, using the Template Method
 * design pattern.
 *
 * <p><b>Workflow
 * (<a href="Controller.html#workflow">and that defined by interface</a>):</b><br>
 * <ol>
 * <li>{@link #handleRequest(RequestContext) handleRequest()}
 * will be called by the DispatcherServlet</li>
 * <li>Inspection of supported methods (ServletException if request method
 * is not support)</li>
 * <li>If session is required, try to get it (ServletException if not found)</li>
 * <li>Set caching headers if needed according to the cacheSeconds property</li>
 * <li>Call abstract method
 * {@link #handleRequestInternal(RequestContext) handleRequestInternal()}
 * (optionally synchronizing around the call on the HttpSession),
 * which should be implemented by extending classes to provide actual
 * functionality to return {@link cn.taketoday.web.view.ModelAndView ModelAndView} objects.</li>
 * </ol>
 *
 * <p><b><a name="config">Exposed configuration properties</a>
 * (<a href="Controller.html#config">and those defined by interface</a>):</b><br>
 * <table border="1">
 * <tr>
 * <td><b>name</b></td>
 * <td><b>default</b></td>
 * <td><b>description</b></td>
 * </tr>
 * <tr>
 * <td>supportedMethods</td>
 * <td>GET,POST</td>
 * <td>comma-separated (CSV) list of methods supported by this controller,
 * such as GET, POST and PUT</td>
 * </tr>
 * <tr>
 * <td>requireSession</td>
 * <td>false</td>
 * <td>whether a session should be required for requests to be able to
 * be handled by this controller. This ensures that derived controller
 * can - without fear of null pointers - call request.getSession() to
 * retrieve a session. If no session can be found while processing
 * the request, a ServletException will be thrown</td>
 * </tr>
 * <tr>
 * <td>cacheSeconds</td>
 * <td>-1</td>
 * <td>indicates the amount of seconds to include in the cache header
 * for the response following on this request. 0 (zero) will include
 * headers for no caching at all, -1 (the default) will not generate
 * <i>any headers</i> and any positive number will generate headers
 * that state the amount indicated as seconds to cache the content</td>
 * </tr>
 * <tr>
 * <td>synchronizeOnSession</td>
 * <td>false</td>
 * <td>whether the call to {@code handleRequestInternal} should be
 * synchronized around the HttpSession, to serialize invocations
 * from the same client. No effect if there is no HttpSession.
 * </td>
 * </tr>
 * </table>
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @author Rossen Stoyanchev
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see WebContentInterceptor
 * @since 4.0 2022/2/8 15:47
 */
public abstract class AbstractController extends WebContentGenerator implements Controller {

  private boolean synchronizeOnSession = false;

  /**
   * Create a new AbstractController which supports
   * HTTP methods GET, HEAD and POST by default.
   */
  public AbstractController() {
    this(true);
  }

  /**
   * Create a new AbstractController.
   *
   * @param restrictDefaultSupportedMethods {@code true} if this
   * controller should support HTTP methods GET, HEAD and POST by default,
   * or {@code false} if it should be unrestricted
   */
  public AbstractController(boolean restrictDefaultSupportedMethods) {
    super(restrictDefaultSupportedMethods);
  }

  /**
   * Set if controller execution should be synchronized on the session,
   * to serialize parallel invocations from the same client.
   * <p>More specifically, the execution of the {@code handleRequestInternal}
   * method will get synchronized if this flag is "true". The best available
   * session mutex will be used for the synchronization; ideally, this will
   * be a mutex exposed by HttpSessionMutexListener.
   * <p>The session mutex is guaranteed to be the same object during
   * the entire lifetime of the session, available under the key defined
   * by the {@code SESSION_MUTEX_ATTRIBUTE} constant. It serves as a
   * safe reference to synchronize on for locking on the current session.
   * <p>In many cases, the HttpSession reference itself is a safe mutex
   * as well, since it will always be the same object reference for the
   * same active logical session. However, this is not guaranteed across
   * different servlet containers; the only 100% safe way is a session mutex.
   *
   * @see AbstractController#handleRequestInternal
   * @see cn.taketoday.web.util.WebUtils#getSessionMutex(WebSession)
   */
  public final void setSynchronizeOnSession(boolean synchronizeOnSession) {
    this.synchronizeOnSession = synchronizeOnSession;
  }

  /**
   * Return whether controller execution should be synchronized on the session.
   */
  public final boolean isSynchronizeOnSession() {
    return this.synchronizeOnSession;
  }

  @Override
  @Nullable
  public ModelAndView handleRequest(RequestContext request) throws Exception {

    if (HttpMethod.OPTIONS.matches(request.getMethodValue())) {
      request.responseHeaders().set(HttpHeaders.ALLOW, getAllowHeader());
      return null;
    }

    // Delegate to WebContentGenerator for checking and preparing.
    checkRequest(request);
    prepareResponse(request);

    // Execute handleRequestInternal in synchronized block if required.
    if (this.synchronizeOnSession) {
      WebSession session = RequestContextUtils.getSession(request, false);
      if (session != null) {
        Object mutex = WebUtils.getSessionMutex(session);
        synchronized(mutex) {
          return handleRequestInternal(request);
        }
      }
    }

    return handleRequestInternal(request);
  }

  /**
   * Template method. Subclasses must implement this.
   * The contract is the same as for {@code handleRequest}.
   *
   * @see #handleRequest
   */
  @Nullable
  protected abstract ModelAndView handleRequestInternal(RequestContext request)
          throws Exception;

}
