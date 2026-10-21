package fu.ats.api.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record JobSearchRequest(
        @Size(max = 255) String keyword,
        @Size(max = 255) String location,
        String employmentType,
        String workMode,
        String status,
        UUID skillId,
        @Min(0) Integer page,
        @Min(1) @Max(100) Integer size,
        String sort
) {
    public JobSearchRequest {
        page = page == null ? 0 : page;
        size = size == null ? 20 : size;
        sort = sort == null || sort.isBlank() ? "createdAt,desc" : sort;
        status = status == null || status.isBlank() ? "OPEN" : status;
    }
}
