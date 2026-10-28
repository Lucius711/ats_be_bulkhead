package fu.ats.dto;

import fu.ats.entity.ParseStatus;

import java.util.List;
import java.util.Map;

/** Danh sách CV mới nhất + số lượng CV theo từng trạng thái parse (trên toàn bộ dữ liệu). */
public record CvListResponse(
        Map<ParseStatus, Long> counts,
        List<CvSummaryResponse> items
) {
}
