package cn.taketoday.test.domain;

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

}
