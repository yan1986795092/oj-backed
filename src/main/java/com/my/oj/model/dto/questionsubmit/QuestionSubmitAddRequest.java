package com.my.oj.model.dto.questionsubmit;

import lombok.Data;

import java.io.Serializable;

/**
 * @Author yqq
 * @Date 2024/4/5 21:25
 * @Description 判题信息
 * @Version 1.0
 */
@Data
public class QuestionSubmitAddRequest implements Serializable {

    /**
     * 编程语言
     */
    private String language;

    /**
     * 提交状态
     */
    private Integer status;

    /**
     * 题目 id
     */
    private Long questionId;

    /**
     * 用户代码
     */
    private String code;



    private static final long serialVersionUID = 1L;


}
