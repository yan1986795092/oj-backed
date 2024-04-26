package com.my.oj.model.enums;

import org.apache.commons.lang3.ObjectUtils;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
/**
 * 用户角色枚举
 */
public enum UserRoleEnum {

    // 定义枚举值：用户
    USER("用户", "user"),

    // 定义枚举值：管理员
    ADMIN("管理员", "admin"),

    // 定义枚举值：被封号
    BAN("被封号", "ban");

    private final String text; // 枚举值的显示文本

    private final String value; // 枚举值的实际值

    // 枚举构造函数，用于初始化枚举值的文本和值
    UserRoleEnum(String text, String value) {
        this.text = text;
        this.value = value;
    }

    /**
     * 获取值列表
     *
     * @return 所有枚举值的值列表
     */
    public static List<String> getValues() {
        return Arrays.stream(values()).map(item -> item.value).collect(Collectors.toList());
    }

    /**
     * 根据 value 获取枚举
     *
     * @param value 枚举值的实际值
     * @return 对应的枚举值，如果值为空或者没有找到匹配的枚举值，则返回null
     */
    public static UserRoleEnum getEnumByValue(String value) {
        if (ObjectUtils.isEmpty(value)) {
            return null;
        }
        for (UserRoleEnum anEnum : UserRoleEnum.values()) {
            if (anEnum.value.equals(value)) {
                return anEnum;
            }
        }
        return null;
    }

    // 获取枚举值的实际值
    public String getValue() {
        return value;
    }

    // 获取枚举值的显示文本
    public String getText() {
        return text;
    }
}