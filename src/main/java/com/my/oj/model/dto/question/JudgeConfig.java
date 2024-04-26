package com.my.oj.model.dto.question;

import lombok.Data;

/**
 * @Author yqq
 * @Date 2024/4/5 21:41
 * @Description 题目配置
 * @Version 1.0
 */
@Data
public class JudgeConfig {
    /**
     * 时间限制 ms
     */
    private Long timeLimit;
    /**
     * 内存限制 kb
     */
    private Long memoryLimit;
    /**
     * 堆栈限制 kb
     */
    private Long stackLimit;



}
