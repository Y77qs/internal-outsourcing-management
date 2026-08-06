package com.pta.outsourcing.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

@Data
@TableName("sys_user_role")
public class SysUserRole {

    private Long id;
    private Long userId;
    private Long roleId;
    private LocalDateTime createdAt;
}
