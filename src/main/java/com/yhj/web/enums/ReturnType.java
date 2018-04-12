package com.yhj.web.enums;

public enum ReturnType {

	JSON("Json") ,STRING("String");
	
	private String returnType;
	
	private ReturnType(String type){
		returnType = type;
	}

	public String getReturnType() {
		return returnType;
	}

	public void setReturnType(String returnType) {
		this.returnType = returnType;
	}
	
}
