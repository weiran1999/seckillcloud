package com.weiran.mission.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.weiran.common.enums.RedisCacheTimeEnum;
import com.weiran.common.enums.CodeMsg;
import com.weiran.common.obj.Result;
import com.weiran.common.redis.key.*;
import com.weiran.common.redis.manager.RedisLua;
import com.weiran.common.redis.manager.RedisService;
import com.weiran.common.utils.AssertUtil;
import com.weiran.common.utils.SM3Util;
import com.weiran.mission.pojo.entity.Order;
import com.weiran.mission.pojo.entity.SeckillGoods;
import com.weiran.mission.manager.OrderManager;
import com.weiran.mission.manager.SeckillGoodsManager;
import com.weiran.common.obj.SeckillMessage;
import com.weiran.mission.rocketmq.MessageSender;
import com.weiran.mission.service.SeckillService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.PostConstruct;
import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
public class SeckillServiceImpl implements SeckillService {

    public static final String SECKILL_TOPIC_TAG = "seckill-topic:tag1";
    private final RedisService redisService;
    private final SeckillGoodsManager seckillGoodsManager;
    private final OrderManager orderManager;
    private final MessageSender messageSender;
    private final RedisLua redisLua;
    final RedisTemplate<String, Object> redisTemplate;

    // 内存标记，减少redis访问，并且为线程安全的集合
    private final Map<Long, Boolean> localOverMap = new ConcurrentHashMap<>();

    /**
     * 系统初始化，把秒杀商品库存剩余加载到Redis缓存中。库存预热。
     */
    @PostConstruct
    public void initSeckillGoodsNumber() {
        List<SeckillGoods> seckillGoodsList = seckillGoodsManager.list();
        if (seckillGoodsList == null) {
            return;
        }
        for (SeckillGoods seckillGoods : seckillGoodsList) {
            // 用商品Id作为key，加载秒杀商品的剩余数量
            redisService.set(SeckillGoodsKey.seckillCount, "" + seckillGoods.getGoodsId(), seckillGoods.getStockCount(), RedisCacheTimeEnum.GOODS_LIST_EXTIME.getValue());
            if (seckillGoods.getStockCount() > 0) {
                localOverMap.put(seckillGoods.getGoodsId(), true);
            } else {
                localOverMap.put(seckillGoods.getGoodsId(), false);
            }
        }
    }

    // 执行秒杀
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<Integer> doSeckill(long goodsId, String path, HttpServletRequest request) {
        long userId = getUserId(request);
        // 验证path
        checkPath(goodsId, path, userId);
        // 若为非，则为商品已经售完
        isCountOver(goodsId);
        // 使用幂等机制，根据用户和商品id生成订单号，防止重复秒杀
        getOrderId(goodsId, userId);
        // LUA脚本判断库存和预减库存
        luaCheckAndReduceStock(goodsId);
        // 入队
        handleMQ(goodsId, userId);
        return Result.success(0); // 排队中
    }

    private void getOrderId(long goodsId, long userId) {
        Long orderId  = goodsId * 1000000 + userId;
        Order order = orderManager.getOne(Wrappers.<Order>lambdaQuery()
                .eq(Order::getId, orderId));
        AssertUtil.seckillInvalid(order != null, CodeMsg.REPEATED_SECKILL);
    }

    private void isCountOver(long goodsId) {
        boolean over = localOverMap.get(goodsId);
        AssertUtil.seckillInvalid(!over, CodeMsg.SECKILL_OVER);
    }

    private void handleMQ(long goodsId, long userId) {
        SeckillMessage seckillMessage = new SeckillMessage();
        seckillMessage.setUserId(userId);
        seckillMessage.setGoodsId(goodsId);
        // 判断库存、判断是否已经秒杀到了和减库存 下订单 写入订单都由消息队列来执行，做到削峰填谷
        messageSender.asyncSend(seckillMessage, SECKILL_TOPIC_TAG); // 这里使用RocketMQ
    }

    private void luaCheckAndReduceStock(long goodsId) {
        Long count = redisLua.judgeStockAndDecrStock(goodsId);
        if (count == -1) {
            localOverMap.put(goodsId, false);
            AssertUtil.seckillInvalid(CodeMsg.SECKILL_OVER);
        }
    }

    private void checkPath(long goodsId, String path, long userId) {
        boolean check = checkPath(userId, goodsId, path);
        AssertUtil.seckillInvalid(!check, CodeMsg.REQUEST_ILLEGAL);
    }

    private long getUserId(HttpServletRequest request) {
        String authInfo = request.getHeader("Authorization");
        String loginToken = authInfo.split("Bearer ")[1];
        return redisService.get(UserKey.getById, loginToken, Long.class);
    }

    // 客户端-前端服务器轮询查询是否下单成功
    @Override
    public Result<Long> seckillResult(long goodsId, HttpServletRequest request) {
        long userId = getUserId(request);
        // resultId 为 orderId：成功; 0：排队中; -1：秒杀失败(秒杀时间已结束)
        long resultId = getResultId(goodsId, userId);
        return Result.success(resultId);
    }

    private long getResultId(long goodsId, long userId) {
        long resultId;
        // 查寻订单
        Order order = orderManager.getOne(Wrappers.<Order>lambdaQuery()
                .eq(Order::getUserId, userId)
                .eq(Order::getGoodsId, goodsId));
        if (order != null) { // 秒杀成功
            resultId = order.getId();
        } else if (getGoodsIsOver(goodsId)) {
            resultId = -1; // 秒杀失败(秒杀时间已结束)
        } else {
            resultId = 0; // 排队中
        }
        return resultId;
    }

    // 返回一个唯一的path的id
    @Override
    public Result<String> getSeckillPath(HttpServletRequest request, long goodsId) {
        long userId = getUserId(request);
        String path = createSeckillPath(userId, goodsId);
        return Result.success(path);
    }

    // 在redis里验证path
    private boolean checkPath(Long userId, long goodsId, String path) {
        if (userId == null || path == null) {
            return false;
        }
        String redis_path = redisService.get(SeckillKey.getSeckillPath, "" + userId + "_"+ goodsId, String.class);
        return path.equals(redis_path);
    }

    // 加盐生成唯一path，构成URl动态化
    private String createSeckillPath(Long userId, Long goodsId) {
        if (userId == null || goodsId == null) {
            return null;
        }
        // 随机返回一个唯一的id，加上123456的盐，然后sm3加密
        String str = SM3Util.sm3(UUID.randomUUID() + "123456");
        redisService.set(SeckillKey.getSeckillPath, "" + userId + "_" + goodsId, str, RedisCacheTimeEnum.GOODS_ID_EXTIME.getValue());
        return str;
    }

    // 查看秒杀商品是否已经结束
    private boolean getGoodsIsOver(long goodsId) {
        return redisService.exists(SeckillKey.isGoodsOver, "" + goodsId);
    }
}
