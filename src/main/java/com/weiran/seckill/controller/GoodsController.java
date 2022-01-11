package com.weiran.seckill.controller;

import com.weiran.seckill.common.obj.Result;
import com.weiran.seckill.pojo.vo.GoodsBoListVo;
import com.weiran.seckill.pojo.vo.GoodsDetailVo;
import com.weiran.seckill.service.GoodsService;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

/**
 * 商品列表详情控制器
 */
@Controller
@RequiredArgsConstructor
@RequestMapping("/goods")
public class GoodsController {

    final GoodsService goodsService;

    @GetMapping("/list")
    public String listPage() {
        return "/goods_list.html";
    }

    @ApiOperation("获得商品列表")
    @GetMapping("/getGoodsList")
    @ResponseBody
    public Result<GoodsBoListVo> getGoodsList() {
        return goodsService.getGoodsList();
    }

    @ApiOperation("获得具体的商品细节")
    @GetMapping("/getDetail/{goodsId}")
    @ResponseBody
    public Result<GoodsDetailVo> getDetail(@PathVariable("goodsId")long goodsId) {
        return goodsService.getDetail(goodsId);
    }


}
