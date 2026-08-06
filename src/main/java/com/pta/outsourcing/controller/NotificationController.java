package com.pta.outsourcing.controller;

import com.pta.outsourcing.annotation.OperationLog;
import com.pta.outsourcing.common.PageVO;
import com.pta.outsourcing.common.ResultVO;
import com.pta.outsourcing.service.NotificationService;
import com.pta.outsourcing.vo.NotificationMessageVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "异步通知")
@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    /**
     * 查询通知消息；管理员和领导可看全部，普通用户只能看自己的通知。
     *
     * @param pageNo 当前页码，从 1 开始。
     * @param pageSize 每页记录数。
     * @return 通知消息分页数据。
     */
    @Operation(summary = "查询通知消息")
    @OperationLog(moduleName = "异步通知", operationType = "查询通知")
    @GetMapping
    @PreAuthorize("hasAuthority('notification:read')")
    public ResultVO<PageVO<NotificationMessageVO>> pageMine(
            @Parameter(description = "页码，从 1 开始", example = "1")
            @RequestParam(defaultValue = "1") long pageNo,
            @Parameter(description = "每页记录数", example = "10")
            @RequestParam(defaultValue = "10") long pageSize
    ) {
        return ResultVO.success(notificationService.pageMine(pageNo, pageSize));
    }
}
