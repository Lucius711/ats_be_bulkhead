package fu.ats.mapper;

import fu.ats.dto.CandidateResponse;
import fu.ats.entity.Candidates;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface CandidateMapper {
    CandidateResponse toDto(Candidates candidate);
}
