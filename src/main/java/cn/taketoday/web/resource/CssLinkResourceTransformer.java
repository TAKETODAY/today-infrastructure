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

package cn.taketoday.web.resource;

import java.io.IOException;
import java.io.StringWriter;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;

import cn.taketoday.core.io.Resource;
import cn.taketoday.lang.Nullable;
import cn.taketoday.logging.Logger;
import cn.taketoday.logging.LoggerFactory;
import cn.taketoday.util.FileCopyUtils;
import cn.taketoday.util.StringUtils;
import cn.taketoday.web.RequestContext;

/**
 * A {@link ResourceTransformer} implementation that modifies links in a CSS
 * file to match the public URL paths that should be exposed to clients (e.g.
 * with an MD5 content-based hash inserted in the URL).
 *
 * <p>The implementation looks for links in CSS {@code @import} statements and
 * also inside CSS {@code url()} functions. All links are then passed through the
 * {@link ResourceResolvingChain} and resolved relative to the location of the
 * containing CSS file. If successfully resolved, the link is modified, otherwise
 * the original link is preserved.
 *
 * @author Rossen Stoyanchev
 * @since 4.0
 */
public class CssLinkResourceTransformer extends ResourceTransformerSupport {

  private static final Charset DEFAULT_CHARSET = StandardCharsets.UTF_8;

  private static final Logger logger = LoggerFactory.getLogger(CssLinkResourceTransformer.class);

  private final List<LinkParser> linkParsers = new ArrayList<>(2);

  public CssLinkResourceTransformer() {
    this.linkParsers.add(new ImportStatementLinkParser());
    this.linkParsers.add(new UrlFunctionLinkParser());
  }

  @Override
  public Resource transform(RequestContext request, Resource resource, ResourceTransformerChain transformerChain)
          throws IOException {

    resource = transformerChain.transform(request, resource);

    String filename = resource.getName();
    if (!"css".equals(StringUtils.getFilenameExtension(filename))
            || resource instanceof EncodedResourceResolver.EncodedResource) {
      return resource;
    }

    byte[] bytes = FileCopyUtils.copyToByteArray(resource.getInputStream());
    String content = new String(bytes, DEFAULT_CHARSET);

    SortedSet<ContentChunkInfo> links = new TreeSet<>();
    for (LinkParser parser : this.linkParsers) {
      parser.parse(content, links);
    }

    if (links.isEmpty()) {
      return resource;
    }

    int index = 0;
    StringWriter writer = new StringWriter();
    for (ContentChunkInfo linkContentChunkInfo : links) {
      writer.write(content.substring(index, linkContentChunkInfo.getStart()));
      String link = content.substring(linkContentChunkInfo.getStart(), linkContentChunkInfo.getEnd());
      String newLink = null;
      if (!hasScheme(link)) {
        String absolutePath = toAbsolutePath(link, request);
        newLink = resolveUrlPath(absolutePath, request, resource, transformerChain);
      }
      writer.write(newLink != null ? newLink : link);
      index = linkContentChunkInfo.getEnd();
    }
    writer.write(content.substring(index));

    return new TransformedResource(resource, writer.toString().getBytes(DEFAULT_CHARSET));
  }

  private boolean hasScheme(String link) {
    int schemeIndex = link.indexOf(':');
    return ((schemeIndex > 0 && !link.substring(0, schemeIndex).contains("/")) || link.indexOf("//") == 0);
  }

  /**
   * Extract content chunks that represent links.
   */
  @FunctionalInterface
  protected interface LinkParser {

    void parse(String content, SortedSet<ContentChunkInfo> result);

  }

  /**
   * Abstract base class for {@link LinkParser} implementations.
   */
  protected abstract static class AbstractLinkParser implements LinkParser {

    /** Return the keyword to use to search for links, e.g. "@import", "url(" */
    protected abstract String getKeyword();

    @Override
    public void parse(String content, SortedSet<ContentChunkInfo> result) {
      int position = 0;
      while (true) {
        position = content.indexOf(getKeyword(), position);
        if (position == -1) {
          return;
        }
        position += getKeyword().length();
        while (Character.isWhitespace(content.charAt(position))) {
          position++;
        }
        if (content.charAt(position) == '\'') {
          position = extractLink(position, "'", content, result);
        }
        else if (content.charAt(position) == '"') {
          position = extractLink(position, "\"", content, result);
        }
        else {
          position = extractLink(position, content, result);
        }
      }
    }

    protected int extractLink(int index, String endKey, String content, SortedSet<ContentChunkInfo> linksToAdd) {
      int start = index + 1;
      int end = content.indexOf(endKey, start);
      linksToAdd.add(new ContentChunkInfo(start, end));
      return end + endKey.length();
    }

    /**
     * Invoked after a keyword match, after whitespace has been removed, and when
     * the next char is neither a single nor double quote.
     */
    protected abstract int extractLink(int index, String content, SortedSet<ContentChunkInfo> linksToAdd);
  }

  private static class ImportStatementLinkParser extends AbstractLinkParser {

    @Override
    protected String getKeyword() {
      return "@import";
    }

    @Override
    protected int extractLink(int index, String content, SortedSet<ContentChunkInfo> linksToAdd) {
      if (content.startsWith("url(", index)) {
        // Ignore: UrlFunctionLinkParser will handle it.
      }
      else if (logger.isTraceEnabled()) {
        logger.trace("Unexpected syntax for @import link at index {}", index);
      }
      return index;
    }
  }

  private static class UrlFunctionLinkParser extends AbstractLinkParser {

    @Override
    protected String getKeyword() {
      return "url(";
    }

    @Override
    protected int extractLink(int index, String content, SortedSet<ContentChunkInfo> linksToAdd) {
      // A url() function without unquoted
      return extractLink(index - 1, ")", content, linksToAdd);
    }
  }

  private static class ContentChunkInfo implements Comparable<ContentChunkInfo> {

    private final int start;

    private final int end;

    ContentChunkInfo(int start, int end) {
      this.start = start;
      this.end = end;
    }

    public int getStart() {
      return this.start;
    }

    public int getEnd() {
      return this.end;
    }

    @Override
    public int compareTo(ContentChunkInfo other) {
      return Integer.compare(this.start, other.start);
    }

    @Override
    public boolean equals(@Nullable Object other) {
      if (this == other) {
        return true;
      }
      if (!(other instanceof ContentChunkInfo otherCci)) {
        return false;
      }
      return (this.start == otherCci.start && this.end == otherCci.end);
    }

    @Override
    public int hashCode() {
      return this.start * 31 + this.end;
    }
  }

}
