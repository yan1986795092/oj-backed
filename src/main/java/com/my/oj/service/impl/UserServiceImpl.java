package com.my.oj.service.impl;

import cn.hutool.core.collection.CollUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.my.oj.common.ErrorCode;
import com.my.oj.constant.CommonConstant;
import com.my.oj.exception.BusinessException;
import com.my.oj.mapper.UserMapper;
import com.my.oj.model.dto.user.UserQueryRequest;
import com.my.oj.model.entity.User;
import com.my.oj.model.enums.UserRoleEnum;
import com.my.oj.model.vo.LoginUserVO;
import com.my.oj.model.vo.UserVO;
import com.my.oj.service.UserService;
import com.my.oj.utils.SqlUtils;
import lombok.extern.slf4j.Slf4j;
import me.chanjar.weixin.common.bean.WxOAuth2UserInfo;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;

import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static com.my.oj.constant.UserConstant.USER_LOGIN_STATE;

/**
 * @author 86178
 * @description 针对表【user(用户)】的数据库操作Service实现
 * @createDate 2024-04-06 17:04:22
 */
@Service
@Slf4j
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {
    /**
     * 盐值，混淆密码
     */
    public static final String SALT = "yqq";

    @Override
    public long userRegister(String userAccount, String userPassword, String checkPassword) {
        // 1. 校验
        if (StringUtils.isAnyBlank(userAccount, userPassword, checkPassword)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "参数为空");
        }
        if (userAccount.length() < 4) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户账号过短");
        }
        if (userPassword.length() < 8 || checkPassword.length() < 8) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户密码过短");
        }
        // 密码和校验密码相同
        if (!userPassword.equals(checkPassword)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "两次输入的密码不一致");
        }
        synchronized (userAccount.intern()) {
            // 账户不能重复
            QueryWrapper<com.my.oj.model.entity.User> queryWrapper = new QueryWrapper<>();
            queryWrapper.eq("userAccount", userAccount);
            long count = this.baseMapper.selectCount(queryWrapper);
            if (count > 0) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "账号重复");
            }
            // 2. 加密
            String encryptPassword = DigestUtils.md5DigestAsHex((SALT + userPassword).getBytes());
            // 3. 插入数据
            com.my.oj.model.entity.User user = new com.my.oj.model.entity.User();
            user.setUserAccount(userAccount);
            user.setUserPassword(encryptPassword);
            boolean saveResult = this.save(user);
            if (!saveResult) {
                throw new BusinessException(ErrorCode.SYSTEM_ERROR, "注册失败，数据库错误");
            }
            return user.getId();
        }
    }

    @Override
    public LoginUserVO userLogin(String userAccount, String userPassword, HttpServletRequest request) {
        // 1. 校验
        if (StringUtils.isAnyBlank(userAccount, userPassword)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "参数为空");
        }

        if (userAccount.length() < 4) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "账号错误");
        }

        if (userPassword.length() < 8) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "密码错误");
        }

        // 2. 加密
        String encryptPassword = DigestUtils.md5DigestAsHex((SALT + userPassword).getBytes());
        // 查询用户是否存在
        QueryWrapper<com.my.oj.model.entity.User> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("userAccount", userAccount);
        queryWrapper.eq("userPassword", encryptPassword);
        com.my.oj.model.entity.User user = this.baseMapper.selectOne(queryWrapper);
        // 用户不存在

        if (user == null) {
            log.info("user login failed, userAccount cannot match userPassword");
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户不存在或密码错误");
        }
        // 3. 记录用户的登录态
        request.getSession().setAttribute(USER_LOGIN_STATE, user);
        return this.getLoginUserVO(user);
    }

    /**
     *微信登录
     * @param wxOAuth2UserInfo 从微信获取的用户信息
     * @param request
     * @return
     */
    @Override
    public LoginUserVO userLoginByMpOpen(WxOAuth2UserInfo wxOAuth2UserInfo, HttpServletRequest request) {
        String unionId = wxOAuth2UserInfo.getUnionId();
        String mpOpenId = wxOAuth2UserInfo.getOpenid();
        // 单机锁
        synchronized (unionId.intern()) {
            // 查询用户是否已存在
            QueryWrapper<com.my.oj.model.entity.User> queryWrapper = new QueryWrapper<>();
            queryWrapper.eq("unionId", unionId);
            com.my.oj.model.entity.User user = this.getOne(queryWrapper);
            // 被封号，禁止登录
            if (user != null && UserRoleEnum.BAN.getValue().equals(user.getUserRole())) {
                throw new BusinessException(ErrorCode.FORBIDDEN_ERROR, "该用户已被封，禁止登录");
            }
            // 用户不存在则创建
            if (user == null) {
                user = new com.my.oj.model.entity.User();
                user.setUnionId(unionId);
                user.setMpOpenId(mpOpenId);
                user.setUserAvatar(wxOAuth2UserInfo.getHeadImgUrl());
                user.setUserName(wxOAuth2UserInfo.getNickname());
                boolean result = this.save(user);
                if (!result) {
                    throw new BusinessException(ErrorCode.SYSTEM_ERROR, "登录失败");
                }
            }
            // 记录用户的登录态
            request.getSession().setAttribute(USER_LOGIN_STATE, user);
            return getLoginUserVO(user);
        }
    }

    /**
     * 获取当前登录用户
     * @param request
     * @return
     */
    @Override
    public com.my.oj.model.entity.User getLoginUser(HttpServletRequest request) {
        // 先判断是否已登录
        Object userObj = request.getSession().getAttribute(USER_LOGIN_STATE);
        com.my.oj.model.entity.User currentUser = (com.my.oj.model.entity.User) userObj;

        if (currentUser == null || currentUser.getId() == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR);
        }

        //todo
        // 从数据库查询（追求性能的话可以注释，直接走缓存）
        long userId = currentUser.getId();
        currentUser = this.getById(userId);
        if (currentUser == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR);
        }

        return currentUser;
    }

    /**
     * 获取当前登录用户（允许未登录）
     *
     * @param request
     * @return
     */
    @Override
    public com.my.oj.model.entity.User getLoginUserPermitNull(HttpServletRequest request) {
        // 先判断是否已登录
        Object userObj = request.getSession().getAttribute(USER_LOGIN_STATE);
        com.my.oj.model.entity.User currentUser = (com.my.oj.model.entity.User) userObj;
        if (currentUser == null || currentUser.getId() == null) {
            return null;
        }
        // 从数据库查询（追求性能的话可以注释，直接走缓存）
        long userId = currentUser.getId();
        return this.getById(userId);
    }

    /**
     * 是否为管理员
     * @param request
     * @return
     */
    @Override
    public boolean isAdmin(HttpServletRequest request) {
        // 仅管理员可查询
        Object userObj = request.getSession().getAttribute(USER_LOGIN_STATE);
        com.my.oj.model.entity.User user = (com.my.oj.model.entity.User) userObj;
        return isAdmin(user);
    }

    @Override
    public boolean isAdmin(com.my.oj.model.entity.User user) {
        return user != null && UserRoleEnum.ADMIN.getValue().equals(user.getUserRole());
    }

    /**
     * 用户注销
     * @param request
     */
    @Override
    public boolean userLogout(HttpServletRequest request) {
        if (request.getSession().getAttribute(USER_LOGIN_STATE) == null) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "未登录");
        }
        // 移除登录态
        request.getSession().removeAttribute(USER_LOGIN_STATE);
        return true;
    }

    @Override
    public LoginUserVO getLoginUserVO(com.my.oj.model.entity.User user) {
        if (user == null) {
            return null;
        }
        LoginUserVO loginUserVO = new LoginUserVO();
        BeanUtils.copyProperties(user, loginUserVO);
        return loginUserVO;
    }

    @Override
    public UserVO getUserVO(com.my.oj.model.entity.User user) {
        if (user == null) {
            return null;
        }
        UserVO userVO = new UserVO();
        BeanUtils.copyProperties(user, userVO);
        return userVO;
    }

    @Override
    public List<UserVO> getUserVO(List<com.my.oj.model.entity.User> userList) {
        if (CollUtil.isEmpty(userList)) {
            return new ArrayList<>();
        }
        return userList.stream().map(this::getUserVO).collect(Collectors.toList());
    }

    @Override
    public QueryWrapper<com.my.oj.model.entity.User> getQueryWrapper(UserQueryRequest userQueryRequest) {
        // 检查传入的请求参数是否为空
        if (userQueryRequest == null) {
            // 如果请求参数为空，则抛出参数错误的业务异常
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "请求参数为空");
        }

        // 从请求对象中获取各种查询条件
        Long id = userQueryRequest.getId();                 // 用户ID
        String unionId = userQueryRequest.getUnionId();     // 用户Union ID
        String mpOpenId = userQueryRequest.getMpOpenId();   // 用户小程序Open ID
        String userName = userQueryRequest.getUserName();   // 用户名
        String userProfile = userQueryRequest.getUserProfile(); // 用户简介
        String userRole = userQueryRequest.getUserRole();   // 用户角色
        String sortField = userQueryRequest.getSortField(); // 排序字段
        String sortOrder = userQueryRequest.getSortOrder(); // 排序方式（升序或降序）

        // 创建查询条件对象
        QueryWrapper<com.my.oj.model.entity.User> queryWrapper = new QueryWrapper<>();

        // 根据各种条件构建查询条件
        queryWrapper.eq(id != null, "id", id);                      // 根据用户ID相等条件查询
        queryWrapper.eq(StringUtils.isNotBlank(unionId), "unionId", unionId); // 根据Union ID相等条件查询
        queryWrapper.eq(StringUtils.isNotBlank(mpOpenId), "mpOpenId", mpOpenId); // 根据小程序Open ID相等条件查询
        queryWrapper.eq(StringUtils.isNotBlank(userRole), "userRole", userRole); // 根据用户角色相等条件查询
        queryWrapper.like(StringUtils.isNotBlank(userProfile), "userProfile", userProfile); // 根据用户简介模糊查询
        queryWrapper.like(StringUtils.isNotBlank(userName), "userName", userName); // 根据用户名模糊查询

        // 根据排序字段和排序方式对查询结果进行排序
        queryWrapper.orderBy(SqlUtils.validSortField(sortField), sortOrder.equals(CommonConstant.SORT_ORDER_ASC),
                sortField);

        // 返回构建好的查询条件对象
        return queryWrapper;
    }
}




