package com.weiran.seckill.exception;


import com.weiran.seckill.common.obj.CodeMsg;

/**
 * 全局异常类
 */
public class GlobalException extends RuntimeException {

	private static final long serialVersionUID = 1L;
	
	private CodeMsg codeMsg;
	
	public GlobalException(CodeMsg codeMsg) {
		super(codeMsg.toString());
		this.codeMsg = codeMsg;
	}

	public CodeMsg getCodeMsg() {
		return codeMsg;
	}

}