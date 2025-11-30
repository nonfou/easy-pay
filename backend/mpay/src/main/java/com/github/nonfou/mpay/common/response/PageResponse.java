package com.github.nonfou.mpay.common.response;

import java.util.Collections;
import java.util.List;
import lombok.Getter;
import org.springframework.data.domain.Page;

/**
 * 通用分页返回体。
 */
@Getter
public class PageResponse<T> {
    private final long page;
    private final long pageSize;
    private final long total;
    private final List<T> items;

    private PageResponse(long page, long pageSize, long total, List<T> items) {
        this.page = page;
        this.pageSize = pageSize;
        this.total = total;
        this.items = items == null ? Collections.emptyList() : Collections.unmodifiableList(items);
    }

    public static <T> PageResponse<T> of(long page, long pageSize, long total, List<T> items) {
        return new PageResponse<>(page, pageSize, total, items);
    }

    /**
     * 从 Spring Data Page 对象转换
     */
    public static <T> PageResponse<T> of(Page<T> page) {
        return new PageResponse<>(
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getContent()
        );
    }
}
