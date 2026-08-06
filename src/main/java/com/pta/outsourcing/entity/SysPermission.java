package com.pta.outsourcing.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

@Data
@TableName("sys_permission")
public class SysPermission {

    private Long id;
    private String permissionCode;
    private String permissionName;
    private String moduleName;
    private String permissionType;
    private String apiPath;
    private String httpMethod;
    private String requestMethod;
    private String path;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
