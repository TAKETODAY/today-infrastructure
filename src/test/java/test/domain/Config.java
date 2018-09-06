package test.domain;

import java.io.Serializable;

import cn.taketoday.context.annotation.Prototype;
import cn.taketoday.context.annotation.Singleton;
import cn.taketoday.context.annotation.Value;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
@Singleton
@AllArgsConstructor
@Prototype("prototype_config")
public final class Config implements Serializable {

	private static final long	serialVersionUID	= 2021083013784359309L;

	private Integer				id;

	@Value("#{site.cdn}")
	private String				cdn;

	@Value("#{site.icp}")
	private String				icp;

	@Value("#{site.host}")
	private String				host;

	@Value("#{site.index}")
	private String				index;

	@Value("#{site.upload}")
	private String				upload;

	@Value("#{site.keywords}")
	private String				keywords;

	@Value("#{site.name}")
	private String				siteName;

	@Value("#{site.copyright}")
	private String				copyright;

	@Value("#{site.baiduCode}")
	private String				baiduCode;

	@Value("#{site.server.path}")
	private String				serverPath;

	@Value("#{site.description}")
	private String				description;

	@Value("#{site.otherFooterInfo}")
	private String				otherFooterInfo;

	public Config() {

	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("{\n\t\"id\":\"").append(id).append("\",\n\t\"cdn\":\"").append(cdn).append("\",\n\t\"icp\":\"")
				.append(icp).append("\",\n\t\"host\":\"").append(host).append("\",\n\t\"index\":\"").append(index)
				.append("\",\n\t\"upload\":\"").append(upload).append("\",\n\t\"keywords\":\"").append(keywords)
				.append("\",\n\t\"siteName\":\"").append(siteName).append("\",\n\t\"copyright\":\"").append(copyright)
				.append("\",\n\t\"baiduCode\":\"").append(baiduCode).append("\",\n\t\"serverPath\":\"")
				.append(serverPath).append("\",\n\t\"description\":\"").append(description)
				.append("\",\n\t\"otherFooterInfo\":\"").append(otherFooterInfo).append("\"\n}");
		return builder.toString();
	}
	
	

}
