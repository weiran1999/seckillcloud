package com.weiran.uaa.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;


@Data
@ApiModel(description = "状态表")
public class Status implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    @ApiModelProperty("主键")
    private Long id;

    @ApiModelProperty("用户id")
    private Long userId;

    @ApiModelProperty("身份证号")
    private String identityCardId;

    @ApiModelProperty("贷款逾期记录(拒绝3年内逾期2次以上，金额小于 1000 元，3 天内还清除外)")
    private Integer exceedRecord;

    @ApiModelProperty("客户工作状态(拒绝无业、失业)")
    private String workStatus;

    @ApiModelProperty("客户是否被列入失信名单(拒绝被列入名单)")
    private Integer dishonest;

    @ApiModelProperty("客户年龄(拒绝小于18岁)")
    private Integer age;

    @ApiModelProperty("创建时间")
    private LocalDateTime createdAt;

    @ApiModelProperty("更新时间")
    private LocalDateTime updatedAt;


}