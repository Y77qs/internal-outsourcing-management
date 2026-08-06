package com.pta.outsourcing.common;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

@Schema(description = "分页响应体")
public record PageVO<T>(
        @Schema(description = "当前页数据列表")
        List<T> records,
        @Schema(description = "总记录数", example = "100")
        long total,
        @Schema(description = "当前页码", example = "1")
        long pageNo,
        @Schema(description = "每页记录数", example = "10")
        long pageSize
) {
}
