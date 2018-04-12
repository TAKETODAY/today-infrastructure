package com.yhj.web.utils;

import java.io.Serializable;

public final class Json implements Serializable {

	private static final long	serialVersionUID	= -5925945582314435750L;

	private boolean				success;

	private String				msg;

	private Object				data;
	

	public Json() {

	}

	public Json(boolean success, String msg) {
		this.success = success;
		this.msg = msg;
	}

	public Json(boolean success, String msg, Object obj) {
		this.success = success;
		this.msg = msg;
		data = obj;
	}

	public boolean isSuccess() {
		return success;
	}

	public void setSuccess(boolean success) {
		this.success = success;
	}

	public String getMsg() {
		return msg;
	}

	public void setMsg(String msg) {
		this.msg = msg;
	}

	public Object getData() {
		return data;
	}

	public void setData(Object data) {
		this.data = data;
	}
}
