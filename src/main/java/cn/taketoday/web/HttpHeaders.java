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
 * along with this program.  If not, see [http://www.gnu.org/licenses/]
 */
package cn.taketoday.web;

import java.util.Collection;
import java.util.Enumeration;

/**
 * @author TODAY <br>
 *         2019-07-10 07:44
 */
public interface HttpHeaders {

    /**
     * Returns the value of the specified request header as a <code>String</code>.
     * If the request did not include a header of the specified name, this method
     * returns <code>null</code>. If there are multiple headers with the same name,
     * this method returns the first head in the request. The header name is case
     * insensitive. You can use this method with any request header.
     *
     * @param name
     *            a <code>String</code> specifying the header name
     *
     * @return a <code>String</code> containing the value of the requested header,
     *         or <code>null</code> if the request does not have a header of that
     *         name
     */
    String requestHeader(String name);

    /**
     * Returns all the values of the specified request header as an
     * <code>Enumeration</code> of <code>String</code> objects.
     *
     * <p>
     * Some headers, such as <code>Accept-Language</code> can be sent by clients as
     * several headers each with a different value rather than sending the header as
     * a comma separated list.
     *
     * <p>
     * If the request did not include any headers of the specified name, this method
     * returns an empty <code>Enumeration</code>. The header name is case
     * insensitive. You can use this method with any request header.
     *
     * @param name
     *            a <code>String</code> specifying the header name
     *
     * @return an <code>Enumeration</code> containing the values of the requested
     *         header. If the request does not have any headers of that name return
     *         an empty enumeration. If the container does not allow access to
     *         header information, return null
     */
    Enumeration<String> requestHeaders(String name);

    /**
     * Returns an enumeration of all the header names this request contains. If the
     * request has no headers, this method returns an empty enumeration.
     *
     * @return an enumeration of all the header names sent with this request; if the
     *         request has no headers, an empty enumeration; if the servlet
     *         container does not allow servlets to use this method,
     *         <code>null</code>
     */
    Enumeration<String> requestHeaderNames();

    /**
     * Returns the value of the specified request header as an <code>int</code>. If
     * the request does not have a header of the specified name, this method returns
     * -1. If the header cannot be converted to an integer, this method throws a
     * <code>NumberFormatException</code>.
     *
     * <p>
     * The header name is case insensitive.
     *
     * @param name
     *            a <code>String</code> specifying the name of a request header
     *
     * @return an integer expressing the value of the request header or -1 if the
     *         request doesn't have a header of this name
     *
     * @exception NumberFormatException
     *                If the header value can't be converted to an <code>int</code>
     */
    int requestIntHeader(String name);

    /**
     * Returns the value of the specified request header as a <code>long</code>
     * value that represents a <code>Date</code> object. Use this method with
     * headers that contain dates, such as <code>If-Modified-Since</code>.
     *
     * <p>
     * The date is returned as the number of milliseconds since January 1, 1970 GMT.
     * The header name is case insensitive.
     *
     * <p>
     * If the request did not have a header of the specified name, this method
     * returns -1. If the header can't be converted to a date, the method throws an
     * <code>IllegalArgumentException</code>.
     *
     * @param name
     *            a <code>String</code> specifying the name of the header
     *
     * @return a <code>long</code> value representing the date specified in the
     *         header expressed as the number of milliseconds since January 1, 1970
     *         GMT, or -1 if the named header was not included with the request
     *
     * @exception IllegalArgumentException
     *                If the header value can't be converted to a date
     */
    long requestDateHeader(String name);

    /**
     * Returns the MIME type of the body of the request, or <code>null</code> if the
     * type is not known.
     *
     * @return a <code>String</code> containing the name of the MIME type of the
     *         request, or null if the type is not known
     */
    String contentType();
    
    
    // ------- response

    /**
     * Gets the value of the response header with the given name.
     * 
     * <p>
     * If a response header with the given name exists and contains multiple values,
     * the value that was added first will be returned.
     *
     * @param name
     *            the name of the response header whose value to return
     *
     * @return the value of the response header with the given name, or
     *         <tt>null</tt> if no header with the given name has been set on this
     *         response
     */
    String responseHeader(String name);

    /**
     * Gets the values of the response header with the given name.
     *
     * <p>
     * Any changes to the returned <code>Collection</code> must not affect this
     * <code>HttpServletResponse</code>.
     *
     * @param name
     *            the name of the response header whose values to return
     *
     * @return a (possibly empty) <code>Collection</code> of the values of the
     *         response header with the given name
     */
    Collection<String> responseHeaders(String name);

    /**
     * Gets the names of the headers of this response.
     *
     * <p>
     * Any changes to the returned <code>Collection</code> must not affect this
     * <code>HttpServletResponse</code>.
     *
     * @return a (possibly empty) <code>Collection</code> of the names of the
     *         headers of this response
     */
    Collection<String> responseHeaderNames();

    /**
     * Sets a response header with the given name and value. If the header had
     * already been set, the new value overwrites the previous one. The
     * <code>containsHeader</code> method can be used to test for the presence of a
     * header before setting its value.
     * 
     * @param name
     *            the name of the header
     * @param value
     *            the header value If it contains octet string, it should be encoded
     *            according to RFC 2047 (http://www.ietf.org/rfc/rfc2047.txt)
     *
     * @see #addResponseHeader(String, String)
     */
    HttpHeaders responseHeader(String name, String value);

    /**
     * Adds a response header with the given name and value. This method allows
     * response headers to have multiple values.
     * 
     * @param name
     *            the name of the header
     * @param value
     *            the additional header value If it contains octet string, it should
     *            be encoded according to RFC 2047
     *            (http://www.ietf.org/rfc/rfc2047.txt)
     *
     * @see #responseHeader(String, String)
     */
    HttpHeaders addResponseHeader(String name, String value);

    /**
     * Sets a response header with the given name and date-value. The date is
     * specified in terms of milliseconds since the epoch. If the header had already
     * been set, the new value overwrites the previous one.
     * 
     * @param name
     *            the name of the header to set
     * @param date
     *            the assigned date value
     * 
     * @see #addResponseDateHeader
     */
    HttpHeaders responseDateHeader(String name, long date);

    /**
     * 
     * Adds a response header with the given name and date-value. The date is
     * specified in terms of milliseconds since the epoch. This method allows
     * response headers to have multiple values.
     * 
     * @param name
     *            the name of the header to set
     * @param date
     *            the additional date value
     * 
     * @see #responseDateHeader
     */
    HttpHeaders addResponseDateHeader(String name, long date);

    /**
     * Sets a response header with the given name and integer value. If the header
     * had already been set, the new value overwrites the previous one. The
     * <code>containsHeader</code> method can be used to test for the presence of a
     * header before setting its value.
     *
     * @param name
     *            the name of the header
     * @param value
     *            the assigned integer value
     *
     * @see #addResponseIntHeader
     */
    HttpHeaders responseIntHeader(String name, int value);

    /**
     * Adds a response header with the given name and integer value. This method
     * allows response headers to have multiple values.
     *
     * @param name
     *            the name of the header
     * @param value
     *            the assigned integer value
     *
     * @see #responseIntHeader
     */
    HttpHeaders addResponseIntHeader(String name, int value);

    /**
     * Sets the content type of the response being sent to the client, if the
     * response has not been committed yet. The given content type may include a
     * character encoding specification, for example,
     * <code>text/html;charset=UTF-8</code>. The response's character encoding is
     * only set from the given content type if this method is called before
     * <code>getWriter</code> is called.
     * <p>
     * This method may be called repeatedly to change content type and character
     * encoding. This method has no effect if called after the response has been
     * committed. It does not set the response's character encoding if it is called
     * after <code>getWriter</code> has been called or after the response has been
     * committed.
     * <p>
     * Containers must communicate the content type and the character encoding used
     * for the servlet response's writer to the client if the protocol provides a
     * way for doing so. In the case of HTTP, the <code>Content-Type</code> header
     * is used.
     *
     * @param contentType
     *            a <code>String</code> specifying the MIME type of the content
     */
    HttpHeaders contentType(String contentType);
}
