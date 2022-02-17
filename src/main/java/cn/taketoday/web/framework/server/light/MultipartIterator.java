/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
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

package cn.taketoday.web.framework.server.light;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import cn.taketoday.http.ContentDisposition;
import cn.taketoday.http.FileSizeExceededException;
import cn.taketoday.http.HttpHeaders;
import cn.taketoday.http.MediaType;
import cn.taketoday.util.DataSize;
import cn.taketoday.util.StringUtils;
import cn.taketoday.web.multipart.MultipartConfiguration;
import cn.taketoday.web.resolver.MultipartParsingException;
import cn.taketoday.web.resolver.NotMultipartRequestException;

/**
 * The {@code MultipartIterator} iterates over the parts of a multipart/form-data request.
 * <p>
 * For example, to support file upload from a web browser:
 * <ol>
 * <li>Create an HTML form which includes an input field of type "file", attributes
 *     method="post" and enctype="multipart/form-data", and an action URL of your choice,
 *     for example action="/upload". This form can be served normally like any other
 *     resource, e.g. from an HTML file on disk.
 * <li>In the context handler implementation, construct a {@code MultipartIterator} from
 *     the client {@code HttpRequest}.
 * <li>Iterate over the form {@link RequestPart}s, processing each named field as appropriate -
 *     for the file input field, read the uploaded file using the body input stream.
 * </ol>
 *
 * @author TODAY 2021/4/13 10:53
 */
public class MultipartIterator {

  protected final MultipartInputStream inputStream;
  protected boolean hasNext;

  /**
   * Creates a new MultipartIterator from the given request.
   *
   * @param req the multipart/form-data request
   */
  public MultipartIterator(final HttpRequest req) {
    final HttpHeaders headers = req.getHeaders();
    final MediaType contentType = headers.getContentType();
    if (!contentType.isCompatibleWith(MediaType.MULTIPART_FORM_DATA)) {
      throw new NotMultipartRequestException("Content-Type is not multipart/form-data");
    }
    final String boundary = contentType.getParameter("boundary"); // should be US-ASCII
    if (boundary == null) {
      throw new MultipartParsingException("Content-Type is missing boundary");
    }
    inputStream = new MultipartInputStream(req.getBody(), boundary.getBytes()); // todo charset
  }

  public boolean hasNext() throws IOException {
    return hasNext(inputStream);
  }

  public boolean hasNext(MultipartInputStream inputStream) throws IOException {
    return hasNext || (hasNext = inputStream.nextPart());
  }

  public MultipartInputStream getInputStream() {
    return inputStream;
  }

  /**
   * @throws cn.taketoday.web.resolver.NotMultipartRequestException if this request is not of type multipart/form-data
   * @throws cn.taketoday.web.resolver.MultipartParsingException multipart parse failed
   */
  public RequestPart obtainNext(LightHttpConfig config, MultipartConfiguration multipartConfig) throws IOException {
    hasNext = false;
    // 先解析 header
    final String contentDispositionString = Utils.readLine(inputStream); // Content-Disposition
    final String contentType = Utils.readLine(inputStream); // Content-Type

//    final HttpHeaders httpHeaders = Utils.readHeaders(inputStream, config);
    final MultipartInputStream inputStream = this.inputStream;
    final int partSize = inputStream.tail - inputStream.head;
    final DataSize maxFileSize = multipartConfig.getMaxFileSize();

    if (partSize > maxFileSize.toBytes()) {
      throw new FileSizeExceededException(maxFileSize, null)
              .setActual(DataSize.ofBytes(partSize));
    }

    final ContentDisposition contentDisposition = ContentDisposition.parse(contentDispositionString);

//    if (httpHeaders.containsKey(Constant.CONTENT_TYPE)) {
    if (StringUtils.isNotEmpty(contentType)) {
      if (partSize > config.getMaxMultipartInMemSize()) {
        final String tempLocation = multipartConfig.getLocation();
        final File tempFileDir = new File(tempLocation);
        final String randomString = StringUtils.generateRandomString(10);
        final File tempFile = new File(tempFileDir, randomString);
        // save to temp file
        try (final FileOutputStream fileOutput = new FileOutputStream(tempFile)) {
          final int bufferSize = config.getMultipartBufferSize();
          final int readTimes = partSize / bufferSize; // readTimes > 1
          if (readTimes == 0) {
            // part size 太小了 直接一次性读完
            byte[] buffer = Utils.readBytes(inputStream, partSize);
            fileOutput.write(buffer, 0, partSize);
            final LightMultipartFile multipartFile = new LightMultipartFile(tempFile, contentDisposition, contentType, partSize);
            multipartFile.setCachedBytes(buffer);
            return multipartFile;
          }
          else {
            // 分次读取
            byte[] buffer = new byte[bufferSize];
            int bytesRead = 0;
            for (int i = 0; i < readTimes; i++) {
              bytesRead += inputStream.read(buffer, 0, bufferSize);
              fileOutput.write(buffer, 0, bytesRead);
            }
            // 读取剩余字节
            buffer = Utils.readBytes(inputStream, partSize - bytesRead);
            fileOutput.write(buffer);
            return new LightMultipartFile(tempFile, contentDisposition, contentType, partSize);
          }
        }
      }
      else {
        final byte[] bytes = Utils.readBytes(inputStream, partSize);
        return new LightMultipartFile(bytes, contentDisposition, contentType, partSize); // inputStream memory
      }
    }
    else {
      final String name = contentDisposition.getName();
      final byte[] bytes = Utils.readBytes(inputStream, partSize);
      return new FieldRequestPart(bytes, name);
    }
  }

}
