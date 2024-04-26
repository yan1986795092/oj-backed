package com.my.oj.model.dto.question;

import lombok.Data;

/**
 * @Author yqq
 * @Date 2024/4/5 21:25
 * @Description 题目用例
 * @Version 1.0
 */
@Data
public class JudgeCase {
    /**
     * 输入用例
     */
    private String input;
    /**
     * 输出用例
     */
    private String output;

}
