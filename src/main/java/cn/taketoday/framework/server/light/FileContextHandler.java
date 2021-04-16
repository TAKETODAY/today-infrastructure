/*
 * Original Author -> 杨海健 (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2021 All Rights Reserved.
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

package cn.taketoday.framework.server.light;

/**
 * The {@code FileContextHandler} services a context by mapping it
 * to a file or folder (recursively) on disk.
 *
 * @author TODAY 2021/4/13 10:49
 */
//@Deprecated
//public class FileContextHandler implements ContextHandler {
//
//  protected final File base;
//
//  public FileContextHandler(File dir) throws IOException {
//    this.base = dir.getCanonicalFile();
//  }
//
//  public int serve(LightRequest req, Response resp) throws IOException {
//    return serveFile(base, req.getContext().getPath(), req, resp);
//  }
//
//  @Override
//  public int serve(RequestContext context) throws IOException {
//    return 0;
//  }
//
//  /**
//   * Serves a context's contents from a file based resource.
//   * <p>
//   * The file is located by stripping the given context prefix from
//   * the request's path, and appending the result to the given base directory.
//   * <p>
//   * Missing, forbidden and otherwise invalid files return the appropriate
//   * error response. Directories are served as an HTML index page if the
//   * virtual host allows one, or a forbidden error otherwise. Files are
//   * sent with their corresponding content types, and handle conditional
//   * and partial retrievals according to the RFC.
//   *
//   * @param base
//   *         the base directory to which the context is mapped
//   * @param context
//   *         the context which is mapped to the base directory
//   * @param req
//   *         the request
//   * @param resp
//   *         the response into which the content is written
//   *
//   * @return the HTTP status code to return, or 0 if a response was sent
//   *
//   * @throws IOException
//   *         if an error occurs
//   */
//  public static int serveFile(File base, String context,
//                              LightRequest req, Response resp) throws IOException {
//    String relativePath = req.getPath().substring(context.length());
//    File file = new File(base, relativePath).getCanonicalFile();
//    if (!file.exists() || file.isHidden() || file.getName().startsWith(".")) {
//      return 404;
//    }
//    else if (!file.canRead() || !file.getPath().startsWith(base.getPath())) { // validate
//      return 403;
//    }
//    else if (file.isDirectory()) {
//      if (relativePath.endsWith("/")) {
//        if (!req.getVirtualHost().isAllowGeneratedIndex())
//          return 403;
//        resp.send(200, createIndex(file, req.getPath()));
//      }
//      else { // redirect to the normalized directory URL ending with '/'
//        resp.redirect(req.getBaseURL() + req.getPath() + "/", true);
//      }
//    }
//    else if (relativePath.endsWith("/")) {
//      return 404; // non-directory ending with slash (File constructor removed it)
//    }
//    else {
//      serveFileContent(file, req, resp);
//    }
//    return 0;
//  }
//
//  /**
//   * Serves the contents of a file, with its corresponding content type,
//   * last modification time, etc. conditional and partial retrievals are
//   * handled according to the RFC.
//   *
//   * @param file
//   *         the existing and readable file whose contents are served
//   * @param req
//   *         the request
//   * @param resp
//   *         the response into which the content is written
//   *
//   * @throws IOException
//   *         if an error occurs
//   */
//  public static void serveFileContent(File file, LightRequest req, Response resp) throws IOException {
//    long len = file.length();
//    long lastModified = file.lastModified();
//    String etag = "W/\"" + lastModified + "\""; // a weak tag based on date
//    int status = 200;
//    // handle range or conditional request
//    long[] range = req.getRange(len);
//    if (range == null || len == 0) {
//      status = Utils.getConditionalStatus(req, lastModified, etag);
//    }
//    else {
//      String ifRange = req.getHeaders().get("If-Range");
//      if (ifRange == null) {
//        if (range[0] >= len)
//          status = 416; // unsatisfiable range
//        else
//          status = Utils.getConditionalStatus(req, lastModified, etag);
//      }
//      else if (range[0] >= len) {
//        // RFC2616#14.16, 10.4.17: invalid If-Range gets everything
//        range = null;
//      }
//      else { // send either range or everything
//        if (!ifRange.startsWith("\"") && !ifRange.startsWith("W/")) {
//          Date date = req.getHeaders().getDate("If-Range");
//          if (date != null && lastModified > date.getTime())
//            range = null; // modified - send everything
//        }
//        else if (!ifRange.equals(etag)) {
//          range = null; // modified - send everything
//        }
//      }
//    }
//    // send the response
//    Headers respHeaders = resp.getHeaders();
//    switch (status) {
//      case 304: // no other headers or body allowed
//        respHeaders.add("ETag", etag);
//        respHeaders.add("Vary", "Accept-Encoding");
//        respHeaders.add("Last-Modified", Utils.formatDate(lastModified));
//        resp.sendHeaders(304);
//        break;
//      case 412:
//        resp.sendHeaders(412);
//        break;
//      case 416:
//        respHeaders.add("Content-Range", "bytes */" + len);
//        resp.sendHeaders(416);
//        break;
//      case 200:
//        // send OK response
//        resp.sendHeaders(200, len, lastModified, etag,
//                         Utils.getContentType(file.getName(), "application/octet-stream"), range);
//        // send body
//        InputStream in = new FileInputStream(file);
//        try {
//          resp.sendBody(in, len, range);
//        }
//        finally {
//          in.close();
//        }
//        break;
//      default:
//        resp.sendHeaders(500); // should never happen
//        break;
//    }
//  }
//
//  /**
//   * Serves the contents of a directory as an HTML file index.
//   *
//   * @param dir
//   *         the existing and readable directory whose contents are served
//   * @param path
//   *         the displayed base path corresponding to dir
//   *
//   * @return an HTML string containing the file index for the directory
//   */
//  public static String createIndex(File dir, String path) {
//    if (!path.endsWith("/"))
//      path += "/";
//    // calculate name column width
//    int w = 21; // minimum width
//    for (String name : dir.list())
//      if (name.length() > w)
//        w = name.length();
//    w += 2; // with room for added slash and space
//    // note: we use apache's format, for consistent user experience
//    Formatter f = new Formatter(Locale.US);
//    f.format("<!DOCTYPE html>%n" +
//                     "<html><head><title>Index of %s</title></head>%n" +
//                     "<body><h1>Index of %s</h1>%n" +
//                     "<pre> Name%" + (w - 5) + "s Last modified      Size<hr>",
//             path, path, "");
//    if (path.length() > 1) // add parent link if not root path
//      f.format(" <a href=\"%s/\">Parent Directory</a>%"
//                       + (w + 5) + "s-%n", Utils.getParentPath(path), "");
//    for (File file : dir.listFiles()) {
//      try {
//        String name = file.getName() + (file.isDirectory() ? "/" : "");
//        String size = file.isDirectory() ? "- " : Utils.toSizeApproxString(file.length());
//        // properly url-encode the link
//        String link = new URI(null, path + name, null).toASCIIString();
//        if (!file.isHidden() && !name.startsWith("."))
//          f.format(" <a href=\"%s\">%s</a>%-" + (w - name.length()) +
//                           "s&#8206;%td-%<tb-%<tY %<tR%6s%n",
//                   link, name, "", file.lastModified(), size);
//      }
//      catch (URISyntaxException ignore) {}
//    }
//    f.format("</pre></body></html>");
//    return f.toString();
//  }
//}
