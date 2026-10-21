package fu.ats.dto;

import lombok.*;

import java.time.LocalDate;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
@Builder
public class JobView {
    private UUID jobId;
    private String title;
    private String status;
    private LocalDate applicationDeadline;
}
