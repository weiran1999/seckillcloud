# 前言
Jmeter是一款由Java构建的压力测试数据，秒杀项目的相关数据目前由Jmeter测试完成。
- beta内测版本，RabbitMQ的使用上欠缺性能和并发能力。
- 0.0.1版本进行了大规模的RabbitMQ相关代码的重构，但多线程并发容器配置还有问题。
- 0.0.2版本进行了多容器配置的修补，实际功能上，真实QPS提升了。至此，大概有效QPS瓶颈在400-500，真实QPS低大概50左右。

# 测试数据
## RabbitMQ 测试数据
- beta内测版本
10000 8322 1678 (10000 5s秒杀完 有效qps 335)

- beta内测版本，不用MQ 总量、没有卖出、真正卖出
10000 8280 1720 (10000 6s 有效qps 286)

- beta内测版本，预减库存代码优化后 总量、没有卖出、真正卖出
10000 8264 1736 (10000 6s 有效qps 289)

- beta内测版本
109秒，吞吐量定时器限制，每分钟吞吐量6000，每秒100，线程只开一个, 有效QPS 91
10000 84 9916

- 0.0.1版本，更换高可用RabbitMQ代码后
10w 190秒杀完，470s异步处理完，有效QPS 526 真实QPS 212 （basic）
10w 203秒杀完，517s异步处理完，有效QPS492， 真实197  （手动确认队列）

- 0.0.1版本
MQ更换多消费者实例，针对高并发业务失败

- 0.0.1版本，进行内部测试迭代后
10w 157s秒杀完 340s异步处理完 有效qps636 真实qps294（100线程 basic）
10w 140s杀完 313s异步处理完 有效qps714 真实qps313 （200线程 basic）

- 0.0.2版本，解决了多实例容器配置问题的并发问题后。
10w 191s秒杀完 207s异步处理完 有效QPS523 真实QPS483 （200线程 basic）
10w 215s秒杀完 233s异步处理完 有效QPS465 真实QPS429 （500线程 basic）

- 0.0.2版本，多实例 MANUAL
10w 190s秒杀完 206s异步处理完 有效QPS525 真实QPS485 （500线程 MANUAL）
10w 227s秒杀完 267s异步处理完 有效QPS440 真实QPS374 （500线程 MANUAL）

- 0.0.2版本 修改为，只有写订单表和更改库存表的量是需要读数据库，其他操作均读写Redis缓存，需要取消库存表的乐观锁，这里看出一个事情，就是在更改库存这一块，换成Redis缓存并没有增加实际的性能，这里可能并不是性能的瓶颈。后续还试着注释掉了代码中的更改库存表的功能，性能只有有效QPS稍稍提升了点。
10w 203s秒杀完 245s异步处理完 有效QPS492 真实QPS408 

- 0.0.2版本 注释查询订单表看是否有重复订单的代码后，性能确实提升了，选择使用Redis来缓存客户订购商品后，性能与原来相近。
10w 146s秒杀完 160s异步处理完 有效QPS684 真实QPS625

## RocketMQ 测试数据
- 1w 15s秒杀完 18s异步处理完 有效QPS666 真实QPS556
- 10w 237s秒杀完 261s异步处理完 有效QPS421 真实QPS383
- 0.0.7版本测试LUA脚本解决超卖 10w 227s秒杀完 229s异步处理完 有效QPS441 真实QPS436
- 0.0.7版本中测试订单部分使用幂等机制，10w 270s秒杀完 324s异步处理完 有效QP370 真实QPS308 （500线程）
- 0.0.7 10w 10w 150s秒杀完 190s异步处理完 有效QP666 真实QPS526 （200线程 比较理想的情况）
这里在0.0.7版本做一个总结，在项目中的Rocket，需要有对应的调优，即使在如上的相当理想的速度和完全正常的结果下，控制台依旧是相当多的消息队列的失败以及消息积压导致的重复发送，当然这也跟压力测试工具的测试方法有关，开发环境、测试环境、生产环境各有千秋。

# SQL的demo语句
TRUNCATE TABLE order_info;
UPDATE seckill_goods SET stock_count = 100000 WHERE id = 1;