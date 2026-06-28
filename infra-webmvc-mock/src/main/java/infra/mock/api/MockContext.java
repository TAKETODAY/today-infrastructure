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

import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Enumeration;
import java.util.Set;

/**
 * Defines a set of methods that a servlet uses to communicate with its servlet container, for example, to get the MIME
 * type of a file, dispatch requests, or write to a log file.
 *
 * <p>
 * There is one context per "web application" per Java Virtual Machine. (A "web application" is a collection of servlets
 * and content installed under a specific subset of the server's URL namespace such as <code>/catalog</code> and
 * possibly installed via a <code>.war</code> file.)
 *
 * <p>
 * In the case of a web application marked "distributed" in its deployment descriptor, there will be one context
 * instance for each virtual machine. In this situation, the context cannot be used as a location to share global
 * information (because the information won't be truly global). Use an external resource like a database instead.
 */
public interface MockContext {

  /**
   * Returns a directory-like listing of all the paths to resources within the web application whose longest sub-path
   * matches the supplied path argument.
   *
   * <p>
   * Paths indicating subdirectory paths end with a <tt>/</tt>.
   *
   * <p>
   * The returned paths are all relative to the root of the web application, or relative to the
   * <tt>/META-INF/resources</tt> directory of a JAR file inside the web application's <tt>/WEB-INF/lib</tt> directory,
   * and have a leading <tt>/</tt>.
   *
   * <p>
   * The returned set is not backed by the {@code MockContext} object, so changes in the returned set are not reflected
   * in the {@code MockContext} object, and vice-versa.
   * </p>
   *
   * <p>
   * For example, for a web application containing:
   *
   * <pre>
   * {@code
   *   /welcome.html
   *   /catalog/index.html
   *   /catalog/products.html
   *   /catalog/offers/books.html
   *   /catalog/offers/music.html
   *   /customer/login.jsp
   *   /WEB-INF/web.xml
   *   /WEB-INF/classes/com.acme.OrderServlet.class
   *   /WEB-INF/lib/catalog.jar!/META-INF/resources/catalog/moreOffers/books.html
   * }
   * </pre>
   *
   * <tt>getResourcePaths("/")</tt> would return <tt>{"/welcome.html", "/catalog/", "/customer/", "/WEB-INF/"}</tt>, and
   * <tt>getResourcePaths("/catalog/")</tt> would return <tt>{"/catalog/index.html", "/catalog/products.html",
   * "/catalog/offers/", "/catalog/moreOffers/"}</tt>.
   *
   * @param path the partial path used to match the resources, which must start with a <tt>/</tt>
   * @return a Set containing the directory listing, or null if there are no resources in the web application whose path
   * begins with the supplied path.
   */
  Set<String> getResourcePaths(String path);

  /**
   * Returns a URL to the resource that is mapped to the given path.
   *
   * <p>
   * The path must begin with a <tt>/</tt> and is interpreted as relative to the current context root, or relative to the
   * <tt>/META-INF/resources</tt> directory of a JAR file inside the web application's <tt>/WEB-INF/lib</tt> directory.
   * This method will first search the document root of the web application for the requested resource, before searching
   * any of the JAR files inside <tt>/WEB-INF/lib</tt>. The order in which the JAR files inside <tt>/WEB-INF/lib</tt> are
   * searched is undefined.
   *
   * <p>
   * This method allows the servlet container to make a resource available to servlets from any source. Resources can be
   * located on a local or remote file system, in a database, or in a <code>.war</code> file.
   *
   * <p>
   * The servlet container must implement the URL handlers and <code>URLConnection</code> objects that are necessary to
   * access the resource.
   *
   * <p>
   * This method returns <code>null</code> if no resource is mapped to the pathname.
   *
   * <p>
   * Some containers may allow writing to the URL returned by this method using the methods of the URL class.
   *
   * <p>
   * The resource content is returned directly, so be aware that requesting a <code>.jsp</code> page returns the JSP
   * source code. Use a <code>RequestDispatcher</code> instead to include results of an execution.
   *
   * <p>
   * This method has a different purpose than <code>java.lang.Class.getResource</code>, which looks up resources based on
   * a class loader. This method does not use class loaders.
   *
   * <p>
   * This method bypasses both implicit (no direct access to WEB-INF or META-INF) and explicit (defined by the web
   * application) security constraints. Care should be taken both when constructing the path (e.g. avoid unsanitized user
   * provided data) and when using the result not to create a security vulnerability in the application.
   *
   * @param path a <code>String</code> specifying the path to the resource
   * @return the resource located at the named path, or <code>null</code> if there is no resource at that path
   * @throws MalformedURLException if the pathname is not given in the correct form
   */
  URL getResource(String path) throws MalformedURLException;

  /**
   * Returns the resource located at the named path as an <code>InputStream</code> object.
   *
   * <p>
   * The data in the <code>InputStream</code> can be of any type or length. The path must be specified according to the
   * rules given in <code>getResource</code>. This method returns <code>null</code> if no resource exists at the specified
   * path.
   *
   * <p>
   * Meta-information such as content length and content type that is available via <code>getResource</code> method is
   * lost when using this method.
   *
   * <p>
   * The servlet container must implement the URL handlers and <code>URLConnection</code> objects necessary to access the
   * resource.
   *
   * <p>
   * This method is different from <code>java.lang.Class.getResourceAsStream</code>, which uses a class loader. This
   * method allows servlet containers to make a resource available to a servlet from any location, without using a class
   * loader.
   *
   * <p>
   * This method bypasses both implicit (no direct access to WEB-INF or META-INF) and explicit (defined by the web
   * application) security constraints. Care should be taken both when constructing the path (e.g. avoid unsanitized user
   * provided data) and when using the result not to create a security vulnerability in the application.
   *
   * @param path a <code>String</code> specifying the path to the resource
   * @return the <code>InputStream</code> returned to the servlet, or <code>null</code> if no resource exists at the
   * specified path
   */
  InputStream getResourceAsStream(String path);

  /**
   * Gets the <i>real</i> path corresponding to the given <i>virtual</i> path.
   *
   * <p>
   * The path should begin with a <tt>/</tt> and is interpreted as relative to the current context root. If the path does
   * not begin with a <tt>/</tt>, the container will behave as if the method was called with <tt>/</tt> appended to the
   * beginning of the provided path.
   *
   * <p>
   * For example, if <tt>path</tt> is equal to <tt>/index.html</tt>, this method will return the absolute file path on the
   * server's filesystem to which a request of the form
   * <tt>http://&lt;host&gt;:&lt;port&gt;/&lt;contextPath&gt;/index.html</tt> would be mapped, where
   * <tt>&lt;contextPath&gt;</tt> corresponds to the context path of this MockContext.
   *
   * <p>
   * The real path returned will be in a form appropriate to the computer and operating system on which the servlet
   * container is running, including the proper path separators.
   *
   * <p>
   * Resources inside the <tt>/META-INF/resources</tt> directories of JAR files bundled in the application's
   * <tt>/WEB-INF/lib</tt> directory must be considered only if the container has unpacked them from their containing JAR
   * file, in which case the path to the unpacked location must be returned.
   *
   * <p>
   * This method returns <code>null</code> if the servlet container is unable to translate the given <i>virtual</i> path
   * to a <i>real</i> path.
   *
   * @param path the <i>virtual</i> path to be translated to a <i>real</i> path
   * @return the <i>real</i> path, or <tt>null</tt> if the translation cannot be performed
   */
  String getRealPath(String path);

  /**
   * Returns the servlet container attribute with the given name, or <code>null</code> if there is no attribute by that
   * name.
   *
   * <p>
   * An attribute allows a servlet container to give the servlet additional information not already provided by this
   * interface. See your server documentation for information about its attributes. A list of supported attributes can be
   * retrieved using <code>getAttributeNames</code>.
   *
   * <p>
   * The attribute is returned as a <code>java.lang.Object</code> or some subclass.
   *
   * <p>
   * Attribute names should follow the same convention as package names. The Jakarta Servlet specification reserves names
   * matching <code>java.*</code>, <code>javax.*</code>, and <code>sun.*</code>.
   *
   * @param name a <code>String</code> specifying the name of the attribute
   * @return an <code>Object</code> containing the value of the attribute, or <code>null</code> if no attribute exists
   * matching the given name.
   * @throws NullPointerException if the argument {@code name} is {@code null}
   * @see MockContext#getAttributeNames
   */
  Object getAttribute(String name);

  /**
   * Returns an <code>Enumeration</code> containing the attribute names available within this MockContext.
   *
   * <p>
   * Use the {@link #getAttribute} method with an attribute name to get the value of an attribute.
   *
   * @return an <code>Enumeration</code> of attribute names
   * @see #getAttribute
   */
  Enumeration<String> getAttributeNames();

  /**
   * Binds an object to a given attribute name in this MockContext. If the name specified is already used for an
   * attribute, this method will replace the attribute with the new to the new attribute.
   * <p>
   * If listeners are configured on the <code>MockContext</code> the container notifies them accordingly.
   * <p>
   * If a null value is passed, the effect is the same as calling <code>removeAttribute()</code>.
   *
   * <p>
   * Attribute names should follow the same convention as package names. The Jakarta Servlet specification reserves names
   * matching <code>java.*</code>, <code>javax.*</code>, and <code>sun.*</code>.
   *
   * @param name a <code>String</code> specifying the name of the attribute
   * @param object an <code>Object</code> representing the attribute to be bound
   * @throws NullPointerException if the name parameter is {@code null}
   */
  void setAttribute(String name, Object object);

  /**
   * Removes the attribute with the given name from this MockContext. After removal, subsequent calls to
   * {@link #getAttribute} to retrieve the attribute's value will return <code>null</code>.
   *
   * <p>
   * If listeners are configured on the <code>MockContext</code> the container notifies them accordingly.
   *
   * @param name a <code>String</code> specifying the name of the attribute to be removed
   */
  void removeAttribute(String name);

}
