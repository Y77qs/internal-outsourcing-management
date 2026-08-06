package com.pta.outsourcing.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

@Data
@TableName("sys_role_permission")
public class SysRolePermission {

    private Long id;
    private Long roleId;
    private Long permissionId;
    private LocalDateTime createdAt;
}
