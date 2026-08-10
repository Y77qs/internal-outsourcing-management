package com.pta.outsourcing.common;

public record PageQuery(long pageNo, long pageSize) {

    public static final long DEFAULT_PAGE_NO = 1;
    public static final long DEFAULT_PAGE_SIZE = 10;
    public static final long MAX_PAGE_SIZE = 100;

    /**
     * 统一分页入参，避免超大 pageSize 直接进入数据库分页插件。
     *
     * @param pageNo 原始页码。
     * @param pageSize 原始每页数量。
     * @return 已归一的分页参数。
     */
    public static PageQuery of(long pageNo, long pageSize) {
        long normalizedPageNo = pageNo < DEFAULT_PAGE_NO ? DEFAULT_PAGE_NO : pageNo;
        long normalizedPageSize = pageSize < 1 ? DEFAULT_PAGE_SIZE : Math.min(pageSize, MAX_PAGE_SIZE);
        return new PageQuery(normalizedPageNo, normalizedPageSize);
    }
}
