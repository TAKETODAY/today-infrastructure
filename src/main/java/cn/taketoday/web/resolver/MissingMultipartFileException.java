package cn.taketoday.web.resolver;

import cn.taketoday.web.handler.MethodParameter;

/**
 * Raised when the part of a "multipart/form-data" request identified by its
 * name cannot be found.
 *
 * <p>This may be because the request is not a multipart/form-data request,
 * because the part is not present in the request.
 *
 * @author TODAY
 * @date 2021/1/17 10:30
 * @since 3.0
 */
public class MissingMultipartFileException extends MissingParameterException {

  public MissingMultipartFileException(MethodParameter parameter) {
    super("MultipartFile", parameter);
  }

}
