package com.my.oj.controller;

import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.my.oj.annotation.AuthCheck;
import com.my.oj.common.BaseResponse;
import com.my.oj.common.DeleteRequest;
import com.my.oj.common.ErrorCode;
import com.my.oj.common.ResultUtils;
import com.my.oj.constant.UserConstant;
import com.my.oj.exception.BusinessException;
import com.my.oj.exception.ThrowUtils;
import com.my.oj.model.dto.question.*;
import com.my.oj.model.entity.Question;
import com.my.oj.model.entity.User;
import com.my.oj.model.vo.QuestionVO;
import com.my.oj.service.QuestionService;
import com.my.oj.service.UserService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.List;

/**
 * 题目
 */
@RestController
@RequestMapping("/question")
@Api("题目模块")
@Slf4j
public class QuestionController {

    @Resource
    private QuestionService questionService;

    @Resource
    private UserService userService;

    // region 增删改查
    /**
     * 创建
     *
     * @param questionAddRequest
     * @param request
     * @return
     */
    @ApiOperation("增加题目")
    @PostMapping("/add")
    public BaseResponse<Long> addQuestion(@RequestBody QuestionAddRequest questionAddRequest, HttpServletRequest request) {
        log.info("增加题目：{}", questionAddRequest);
        //判断前端传来的json数据是否为空
        if (questionAddRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        //创建一个对象，把前端传来的对象进行复制
        Question question = new Question();
        BeanUtils.copyProperties(questionAddRequest, question);
        //把json格式的数据转化成字符串类型
        List<String> tags = questionAddRequest.getTags();
        if (tags != null) {
            question.setTags(JSONUtil.toJsonStr(tags));
        }
        //转化成字符串
        List<JudgeCase> judgeCases = questionAddRequest.getJudgeCase();
        if (judgeCases != null) {
            question.setJudgeCase(JSONUtil.toJsonStr(judgeCases));
        }
        //转换成字符串
        List<JudgeConfig> judgeConfigs = questionAddRequest.getJudgeConfig();
        if (judgeConfigs != null) {
            question.setJudgeConfig(JSONUtil.toJsonStr(judgeConfigs));
        }
        //判断输入的数据是否有效
        questionService.validQuestion(question, true);

        //验证当前登录状态
        User loginUser = userService.getLoginUser(request);
        question.setUserId(loginUser.getId());

        //数据持久化到数据库中
        boolean result = questionService.save(question);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
        long newQuestionId = question.getId();

        return ResultUtils.success(newQuestionId);
    }


    /**
     * 删除
     *
     * @param deleteRequest
     * @param request
     * @return
     */
    @ApiOperation("删除题目")
    @PostMapping("/delete")
    public BaseResponse<Boolean> deleteQuestion(@RequestBody DeleteRequest deleteRequest, HttpServletRequest request) {
        //判断前端传过来的json数据是否为空
        if (deleteRequest == null || deleteRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        //判断当前的登录状态
        User user = userService.getLoginUser(request);
        long id = deleteRequest.getId();

        // 查询数据库中信息是否存在
        Question oldQuestion = questionService.getById(id);
        ThrowUtils.throwIf(oldQuestion == null, ErrorCode.NOT_FOUND_ERROR);

        // 仅本人或管理员可删除
        if (!oldQuestion.getUserId().equals(user.getId()) && !userService.isAdmin(request)) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
        }
        //返回状态
        boolean b = questionService.removeById(id);
        return ResultUtils.success(b);
    }


    /**
     * 更新（仅管理员）
     * @param questionUpdateRequest
     * @return
     */
    @ApiOperation("更新题目")
    @PostMapping("/update")
   // @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Boolean> updateQuestion(@RequestBody QuestionUpdateRequest questionUpdateRequest) {
        log.info("更新题目:{}", questionUpdateRequest);
        //判断是否为空
        if (questionUpdateRequest == null || questionUpdateRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        //拷贝
        Question question = new Question();
        BeanUtils.copyProperties(questionUpdateRequest, question);
        //转换为字符串
        List<String> tags = questionUpdateRequest.getTags();
        if (tags != null) {
            question.setTags(JSONUtil.toJsonStr(tags));
        }
        //转化成字符串
        List<JudgeCase> judgeCases = questionUpdateRequest.getJudgeCase();
        if (judgeCases != null) {
            question.setJudgeCase(JSONUtil.toJsonStr(judgeCases));
        }
        //转换成字符串
        List<JudgeConfig> judgeConfigs = questionUpdateRequest.getJudgeConfig();
        if (judgeConfigs != null) {
            question.setJudgeConfig(JSONUtil.toJsonStr(judgeConfigs));
        }
        //有效性
        // 参数校验
        questionService.validQuestion(question, false);
        long id = questionUpdateRequest.getId();

        // 判断是否存在
        Question oldQuestion = questionService.getById(id);
        ThrowUtils.throwIf(oldQuestion == null, ErrorCode.NOT_FOUND_ERROR);
        //更新
        boolean result = questionService.updateById(question);
        //返回
        return ResultUtils.success(result);
    }

    /**
     * 根据 id 获取
     * @param id
     * @return
     */
    @ApiOperation("根据id获取题目")
    @GetMapping("/get/vo")
    public BaseResponse<QuestionVO> getQuestionVOById(long id, HttpServletRequest request) {
        log.info("根据id获取题目:{},{}", id, request);
        //验证合法性
        if (id <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        //查询
        Question question = questionService.getById(id);
        if (question == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR);
        }
        //返回封装类
        return ResultUtils.success(questionService.getQuestionVO(question, request));
    }

    /**
     * 分页获取列表（仅管理员）
     * @param questionQueryRequest
     * @return
     */
    @ApiOperation("分页获取列表")
    @PostMapping("/list/page")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Page<Question>> listQuestionByPage(@RequestBody QuestionQueryRequest questionQueryRequest) {
        log.info("分页获取列表:{}", questionQueryRequest);
        //获取当前页码和页面大小
        long current = questionQueryRequest.getCurrent();
        long size = questionQueryRequest.getPageSize();
        //分页查询
        Page<Question> questionPage = questionService.page(new Page<>(current, size),
                questionService.getQueryWrapper(questionQueryRequest));
        //返回状态
        return ResultUtils.success(questionPage);
    }

    /**
     * 分页获取列表（封装类）
     *
     * @param questionQueryRequest
     * @param request
     * @return
     */
    @ApiOperation("分页获取列表VO类")
    @PostMapping("/list/page/vo")
    public BaseResponse<Page<QuestionVO>> listQuestionVOByPage(@RequestBody QuestionQueryRequest questionQueryRequest,
                                                               HttpServletRequest request) {
        log.info("分页获取列表VO类:", questionQueryRequest);
        //获取当前页码和页面大小
        long current = questionQueryRequest.getCurrent();
        long size = questionQueryRequest.getPageSize();
        // 限制爬虫
        ThrowUtils.throwIf(size > 20, ErrorCode.PARAMS_ERROR);
        //分页查询
        Page<Question> questionPage = questionService.page(new Page<>(current, size),
                questionService.getQueryWrapper(questionQueryRequest));
        //返回状态
        return ResultUtils.success(questionService.getQuestionVOPage(questionPage, request));
    }

    /**
     * 分页获取当前用户创建的资源列表
     *
     * @param questionQueryRequest
     * @param request
     * @return
     */
    @ApiOperation("分页获取当前用户创建的资源列表")
    @PostMapping("/my/list/page/vo")
    public BaseResponse<Page<QuestionVO>> listMyQuestionVOByPage(@RequestBody QuestionQueryRequest questionQueryRequest,
                                                                 HttpServletRequest request) {
        log.info("分页获取当前用户创建的资源列表:", questionQueryRequest);
        //判断前端请求
        if (questionQueryRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        //获取登录状态
        User loginUser = userService.getLoginUser(request);
        questionQueryRequest.setUserId(loginUser.getId());
        //判断当前页码和页面大小
        long current = questionQueryRequest.getCurrent();
        long size = questionQueryRequest.getPageSize();
        // 限制爬虫
        ThrowUtils.throwIf(size > 20, ErrorCode.PARAMS_ERROR);
        //分页查询
        Page<Question> questionPage = questionService.page(new Page<>(current, size),
                questionService.getQueryWrapper(questionQueryRequest));
        return ResultUtils.success(questionService.getQuestionVOPage(questionPage, request));
    }

    // endregion

    /**
     * 编辑（用户）
     * @param questionEditRequest
     * @param request
     * @return
     */
    @ApiOperation("编辑用户")
    @PostMapping("/edit")
    public BaseResponse<Boolean> editQuestion(@RequestBody QuestionEditRequest questionEditRequest, HttpServletRequest request) {
        log.info("编辑用户:{}", questionEditRequest);

        if (questionEditRequest == null || questionEditRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }

        Question question = new Question();
        BeanUtils.copyProperties(questionEditRequest, question);

        List<String> tags = questionEditRequest.getTags();
        if (tags != null) {
            question.setTags(JSONUtil.toJsonStr(tags));
        }

        // 参数校验
        questionService.validQuestion(question, false);
        User loginUser = userService.getLoginUser(request);

        long id = questionEditRequest.getId();
        // 判断是否存在
        Question oldQuestion = questionService.getById(id);
        ThrowUtils.throwIf(oldQuestion == null, ErrorCode.NOT_FOUND_ERROR);
        // 仅本人或管理员可编辑
        if (!oldQuestion.getUserId().equals(loginUser.getId()) && !userService.isAdmin(loginUser)) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
        }
        boolean result = questionService.updateById(question);
        return ResultUtils.success(result);
    }

}
