package com.weiran.seckill.redis.key;

public class SeckillKey extends BasePrefix {

	private SeckillKey(String prefix) {
		super(prefix);
	}

	public static SeckillKey seckillCount = new SeckillKey("sc");

	public static SeckillKey isGoodsOver = new SeckillKey("go");

	public static SeckillKey getSeckillPath = new SeckillKey("mp");
}