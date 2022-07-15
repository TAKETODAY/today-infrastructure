package cn.taketoday.http.converter.json;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.Map;

import cn.taketoday.http.ProblemDetail;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_EMPTY;

/**
 * An interface to associate Jackson annotations with
 * {@link ProblemDetail} to avoid a hard dependency on
 * the Jackson library.
 *
 * <p>The annotations ensure the {@link ProblemDetail#getProperties() properties}
 * map is unwrapped and rendered as top level JSON properties, and likewise that
 * the {@code properties} map contains unknown properties from the JSON.
 *
 * <p>{@link Jackson2ObjectMapperBuilder} automatically registers this as a
 * "mix-in" for {@link ProblemDetail}, which means it always applies, unless
 * an {@code ObjectMapper} is instantiated directly and configured for use.
 *
 * @author Rossen Stoyanchev
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/7/14 9:58
 */
@JsonInclude(NON_EMPTY)
public interface ProblemDetailJacksonMixin {

  @JsonAnySetter
  void setProperty(String name, Object value);

  @JsonAnyGetter
  Map<String, Object> getProperties();

}
