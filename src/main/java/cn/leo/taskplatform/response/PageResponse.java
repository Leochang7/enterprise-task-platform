package cn.leo.taskplatform.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Collections;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PageResponse<T> {

    private long pageNo;
    private long pageSize;
    private long total;
    @Builder.Default
    private List<T> records = Collections.emptyList();
}
