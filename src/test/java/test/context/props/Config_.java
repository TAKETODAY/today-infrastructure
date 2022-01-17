package test.context.props;

import cn.taketoday.context.annotation.Props;
import cn.taketoday.lang.Singleton;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Setter
@Getter
@Singleton
@NoArgsConstructor
@Props(prefix = "site.")
public class Config_ {

  private String cdn;
  private String icp;
  private String host;
  private String index;

  @Override
  public String toString() {
    return new StringBuilder()//
            .append("{\n\t\"cdn\":\"").append(cdn)//
            .append("\",\n\t\"icp\":\"").append(icp)//
            .append("\",\n\t\"host\":\"").append(host)//
            .append("\",\n\t\"index\":\"").append(index)//
            .append("\"\n}")//
            .toString();
  }
}
