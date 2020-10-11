/**
 * Original Author -> 杨海健 (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2020 All Rights Reserved.
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
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package cn.taketoday.web;

import java.io.BufferedReader;
import java.io.Flushable;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.HttpCookie;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;

import cn.taketoday.context.io.Readable;
import cn.taketoday.context.io.Writable;
import cn.taketoday.web.RequestContextHolder.ApplicationNotStartedContext;
import cn.taketoday.web.annotation.PathVariable;
import cn.taketoday.web.multipart.MultipartFile;
import cn.taketoday.web.ui.Model;
import cn.taketoday.web.ui.ModelAndView;
import cn.taketoday.web.ui.RedirectModel;

/**
 * Context holder for request-specific state.
 *
 * @author TODAY <br>
 *         2019-06-22 15:48
 * @since 2.3.7
 */
public interface RequestContext extends Readable, Writable, Model, HttpHeaders, Flushable {

  HttpCookie[] EMPTY_COOKIES = {};

  // --- request

  /**
   * Returns the portion of the request URI that indicates the context of the
   * request. The context path always comes first in a request URI. The path
   * starts with a "" character but does not end with a "" character. The
   * container does not decode this string.
   *
   * @return a <code>String</code> specifying the portion of the request URI that
   *         indicates the context of the request
   */
  String contextPath();

  /**
   * Returns the part of this request's URL from the protocol name up to the query
   * string in the first line of the HTTP request. The web container does not
   * decode this String. For example:
   *
   * <table summary="Examples of Returned Values">
   * <tr align=left>
   * <th>First line of HTTP request</th>
   * <th>Returned Value</th>
   * <tr>
   * <td>POST /some/path.html HTTP/1.1
   * <td>
   * <td>/some/path.html
   * <tr>
   * <td>GET http://foo.bar/a.html HTTP/1.0
   * <td>
   * <td>/a.html
   * <tr>
   * <td>HEAD /xyz?a=b HTTP/1.1
   * <td>
   * <td>/xyz
   * </table>
   *
   * @return a <code>String</code> containing the part of the URL from the
   *         protocol name up to the query string
   */
  String requestURI();

  /**
   * The returned URL contains a protocol, server name, port number, and server
   * path, but it does not include query string parameters.
   *
   * @return A URL
   */
  String requestURL();

  /**
   * Returns the query string that is contained in the request URL after the path.
   * This method returns <code>null</code> if the URL does not have a query
   * string. Same as the value of the CGI variable QUERY_STRING.
   *
   * @return a <code>String</code> containing the query string or
   *         <code>null</code> if the URL contains no query string. The value is
   *         not decoded by the container.
   */
  String queryString();

  /**
   * Returns an array containing all of the <code>Cookie</code> objects the client
   * sent with this request. This method returns <code>null</code> if no cookies
   * were sent.
   *
   * @return an array of all the <code>Cookies</code> included with this request,
   *         or {@link #EMPTY_COOKIES} if the request has no cookies
   */
  HttpCookie[] cookies();

  /**
   * Returns a {@link HttpCookie} object the client sent with this request. This
   * method returns <code>null</code> if no target cookie were sent.
   *
   * @param name
   *            Cookie name
   * @return a {@link HttpCookie} object the client sent with this request. This
   *         method returns <code>null</code> if no target cookie were sent.
   * @since 2.3.7
   */
  HttpCookie cookie(String name);

  /**
   * Adds the specified cookie to the response. This method can be called multiple
   * times to set more than one cookie.
   *
   * @param cookie
   *            the Cookie to return to the client
   */
  RequestContext addCookie(HttpCookie cookie);

  /**
   * Returns a java.util.Map of the parameters of this request.
   *
   * <p>
   * Request parameters are extra information sent with the request. Parameters
   * are contained in the query string or posted form data.
   *
   * @return java.util.Map containing parameter names as keys and parameter values
   *         as map values. The keys in the parameter map are of type String. The
   *         values in the parameter map are of type String array.
   */
  Map<String, String[]> parameters();

  /**
   * Returns an <code>Enumeration</code> of <code>String</code> objects containing
   * the names of the parameters contained in this request. If the request has no
   * parameters, the method returns an empty <code>Enumeration</code>.
   *
   * @return an <code>Enumeration</code> of <code>String</code> objects, each
   *         <code>String</code> containing the name of a request parameter; or an
   *         empty <code>Enumeration</code> if the request has no parameters
   */
  Enumeration<String> parameterNames();

  /**
   * Returns an array of <code>String</code> objects containing all of the values
   * the given request parameter has, or <code>null</code> if the parameter does
   * not exist.
   *
   * <p>
   * If the parameter has a single value, the array has a length of 1.
   *
   * @param name
   *            a <code>String</code> containing the name of the parameter whose
   *            value is requested
   *
   * @return an array of <code>String</code> objects containing the parameter's
   *         values
   *
   * @see #parameters()
   */
  String[] parameters(String name);

  /**
   * Returns the value of a request parameter as a <code>String</code>, or
   * <code>null</code> if the parameter does not exist. Request parameters are
   * extra information sent with the request. Parameters are contained in the
   * query string or posted form data.
   *
   * <p>
   * You should only use this method when you are sure the parameter has only one
   * value. If the parameter might have more than one value, use
   * {@link #parameters(String)}.
   *
   * <p>
   * If you use this method with a multivalued parameter, the value returned is
   * equal to the first value in the array returned by
   * <code>parameters(String)</code>.
   *
   * <p>
   * If the parameter data was sent in the request body, such as occurs with an
   * HTTP POST request, then reading the body directly via {@link #getInputStream}
   * or {@link #getReader} can interfere with the execution of this method.
   *
   * @param name
   *            a <code>String</code> specifying the name of the parameter
   *
   * @return a <code>String</code> representing the single value of the parameter
   *
   * @see #parameters(String)
   */
  String parameter(String name);

  /**
   * Returns the name of the HTTP method with which this request was made, for
   * example, GET, POST, or PUT.
   *
   * @return a <code>String</code> specifying the name of the method with which
   *         this request was made
   */
  String method();

  /**
   * Returns the Internet Protocol (IP) address of the client or last proxy that
   * sent the request.
   *
   * @return a <code>String</code> containing the IP address of the client that
   *         sent the request
   */
  String remoteAddress();

  /**
   * Returns the length, in bytes, of the request body and made available by the
   * input stream, or -1 if the length is not known.
   *
   * @return a long containing the length of the request body or -1L if the length
   *         is not known
   */
  long contentLength();

  /**
   * Retrieves the body of the request as binary data using a {@link InputStream}.
   * Either this method or {@link #getReader} may be called to read the body, not
   * both.
   *
   * @return a {@link InputStream} object containing the body of the request
   *
   * @exception IllegalStateException
   *                For Servlet Environment if the {@link #getReader} method has
   *                already been called for this request
   *
   * @exception IOException
   *                if an input or output exception occurred
   */
  @Override
  InputStream getInputStream() throws IOException;

  /**
   * Retrieves the body of the request as character data using a
   * <code>BufferedReader</code>. The reader translates the character data
   * according to the character encoding used on the body. Either this method or
   * {@link #getInputStream} may be called to read the body, not both.
   *
   * @return a <code>BufferedReader</code> containing the body of the request
   *
   * @exception IllegalStateException
   *                For Servlet Environment if {@link #getInputStream} method has
   *                been called on this request
   * @exception IOException
   *                if an input or output exception occurred
   *
   * @see #getInputStream
   */
  @Override
  BufferedReader getReader() throws IOException;

  // ------------------

  /**
   * Request body object
   */
  Object requestBody();

  /**
   * Apply request body object
   *
   * @param body
   *            Target request body object
   * @return Request body object
   */
  Object requestBody(Object body);

  String[] pathVariables();

  /**
   * set current {@link PathVariable}s
   *
   * @param variables
   *            {@link PathVariable}s
   * @return input variables
   */
  String[] pathVariables(String[] variables);

  RedirectModel redirectModel();

  RedirectModel redirectModel(RedirectModel redirectModel);

  /**
   * Get all {@link MultipartFile}s from current request
   *
   * @throws IOException
   *             if an I/O error occurred during the retrieval of the Part
   *             components of this request
   */
  Map<String, List<MultipartFile>> multipartFiles() throws IOException;

  // ---------------- response

  /**
   * Get a {@link ModelAndView}
   * <p>
   * If there isn't a {@link ModelAndView} in this {@link RequestContext},
   * <b>Create One</b>
   *
   * @return Returns {@link ModelAndView}, never be null but except
   *         {@link ApplicationNotStartedContext}
   */
  ModelAndView modelAndView();

  /**
   * Sets the length of the content body in the response , this method sets the
   * HTTP Content-Length header.
   *
   * @param length
   *            an long specifying the length of the content being returned to the
   *            client; sets the Content-Length header
   */
  RequestContext contentLength(long length);

  /**
   * Returns a boolean indicating if the response has been committed. A committed
   * response has already had its status code and headers written.
   *
   * @return a boolean indicating if the response has been committed
   *
   * @see #reset
   */
  boolean committed();

  /**
   * Clears any data that exists in the buffer as well as the status code,
   * headers. The state of calling {@link #getWriter} or {@link #getOutputStream}
   * is also cleared. It is legal, for instance, to call {@link #getWriter},
   * {@link #reset} and then {@link #getOutputStream}. If {@link #getWriter} or
   * {@link #getOutputStream} have been called before this method, then the
   * corrresponding returned Writer or OutputStream will be staled and the
   * behavior of using the stale object is undefined. If the response has been
   * committed, this method throws an <code>IllegalStateException</code>.
   *
   * @exception IllegalStateException
   *                if the response has already been committed
   */
  RequestContext reset();

  /**
   * Sends a temporary redirect response to the client using the specified
   * redirect location URL and clears the buffer. The buffer will be replaced with
   * the data set by this method. Calling this method sets the status code to 302
   * (Found). This method can accept relative URLs;the servlet container must
   * convert the relative URL to an absolute URL before sending the response to
   * the client. If the location is relative without a leading '/' the container
   * interprets it as relative to the current request URI. If the location is
   * relative with a leading '/' the container interprets it as relative to the
   * servlet container root. If the location is relative with two leading '/' the
   * container interprets it as a network-path reference (see
   * <a href="http://www.ietf.org/rfc/rfc3986.txt"> RFC 3986: Uniform Resource
   * Identifier (URI): Generic Syntax</a>, section 4.2 &quot;Relative
   * Reference&quot;).
   *
   * <p>
   * If the response has already been committed, this method throws an
   * IllegalStateException. After using this method, the response should be
   * considered to be committed and should not be written to.
   *
   * @param location
   *            the redirect location URL
   * @exception IOException
   *                If an input or output exception occurs
   * @exception IllegalStateException
   *                If the response was committed or if a partial URL is given and
   *                cannot be converted into a valid URL
   */
  RequestContext redirect(String location) throws IOException;

  /**
   * Sets the status code for this response.
   *
   * <p>
   * This method is used to set the return status code when there is no error .
   * <p>
   * This method preserves any cookies and other response headers.
   * <p>
   * Valid status codes are those in the 2XX, 3XX, 4XX, and 5XX ranges. Other
   * status codes are treated as container specific.
   *
   * @param sc
   *            the status code
   */
  RequestContext status(int sc);

  /**
   * Gets the current status code of this response.
   *
   * @return the current status code of this response
   */
  int status();

  /**
   * Sends an error response to the client using the specified status code and
   * clears the buffer.
   *
   * The server will preserve cookies and may clear or update any headers needed
   * to serve the error page as a valid response.
   *
   * If an error-page declaration has been made for the web application
   * corresponding to the status code passed in, it will be served back the error
   * page
   *
   * <p>
   * If the response has already been committed, this method throws an
   * IllegalStateException. After using this method, the response should be
   * considered to be committed and should not be written to.
   *
   * @param sc
   *            the error status code
   * @exception IOException
   *                If an input or output exception occurs
   * @exception IllegalStateException
   *                If the response was committed before this method call
   */
  RequestContext sendError(int sc) throws IOException;

  /**
   * <p>
   * Sends an error response to the client using the specified status and clears
   * the buffer. The server defaults to creating the response to look like an
   * HTML-formatted server error page containing the specified message, setting
   * the content type to "text/html". The caller is <strong>not</strong>
   * responsible for escaping or re-encoding the message to ensure it is safe with
   * respect to the current response encoding and content type. This aspect of
   * safety is the responsibility of the container, as it is generating the error
   * page containing the message. The server will preserve cookies and may clear
   * or update any headers needed to serve the error page as a valid response.
   * </p>
   *
   * <p>
   * If an error-page declaration has been made for the web application
   * corresponding to the status code passed in, it will be served back in
   * preference to the suggested msg parameter and the msg parameter will be
   * ignored.
   * </p>
   *
   * <p>
   * If the response has already been committed, this method throws an
   * IllegalStateException. After using this method, the response should be
   * considered to be committed and should not be written to.
   *
   * @param sc
   *            the error status code
   * @param msg
   *            the descriptive message
   * @exception IOException
   *                If an input or output exception occurs
   * @exception IllegalStateException
   *                If the response was committed
   */
  RequestContext sendError(int sc, String msg) throws IOException;

  /**
   * Returns a {@link OutputStream} suitable for writing binary data in the
   * response. The Server container does not encode the binary data.
   * <p>
   * Calling flush() on the {@link OutputStream} commits the response.
   *
   * Either this method or {@link #getWriter} may be called to write the body, not
   * both, except when {@link #reset} has been called.
   *
   * @return a {@link OutputStream} for writing binary data
   *
   * @exception IllegalStateException
   *                For Servlet Environment if the <code>getWriter</code> method
   *                has been called on this response
   * @exception IOException
   *                if an input or output exception occurred
   * @see #getWriter
   * @see #reset
   */
  @Override
  OutputStream getOutputStream() throws IOException;

  /**
   * Returns a <code>PrintWriter</code> object that can send character text to the
   * client. The <code>PrintWriter</code> uses the UTF-8 character encoding.
   * <p>
   * Calling flush() on the <code>PrintWriter</code> commits the response.
   * <p>
   * Either this method or {@link #getOutputStream} may be called to write the
   * body, not both, except when {@link #reset} has been called.
   *
   * @return a <code>PrintWriter</code> object that can return character data to
   *         the client
   *
   * @exception IOException
   *                if an input or output exception occurred
   *
   * @exception IllegalStateException
   *                For Servlet Environment if the <code>getOutputStream</code>
   *                method has already been called for this response object
   * @see #getOutputStream
   * @see #reset
   */
  @Override
  PrintWriter getWriter() throws IOException;

  // ----------------------

  <T> T nativeSession();

  <T> T nativeSession(Class<T> sessionClass);

  <T> T nativeRequest();

  <T> T nativeRequest(Class<T> requestClass);

  <T> T nativeResponse();

  <T> T nativeResponse(Class<T> responseClass);

}
