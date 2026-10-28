package fu.ats.service;

import fu.ats.dto.CandidateRequest;
import fu.ats.config.CandidateProperties;
import fu.ats.dto.CandidateResponse;
import fu.ats.entity.CandidateSkillId;
import fu.ats.entity.CandidateSkills;
import fu.ats.entity.CandidateStatus;
import fu.ats.entity.Candidates;
import fu.ats.entity.CvEntity;
import fu.ats.entity.ParseStatus;
import fu.ats.entity.Skills;
import fu.ats.exception.ResourceNotFoundException;
import fu.ats.mapper.CandidateMapper;
import fu.ats.repository.CandidateRepository;
import fu.ats.repository.CandidateSkillsRepository;
import fu.ats.repository.CvEntityRepository;
import fu.ats.repository.SkillRepository;
import fu.ats.shared.commonweb.messaging.rabbitmq.CvParseCommand;
import fu.ats.shared.commonweb.messaging.rabbitmq.RabbitMessagingNames;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CandidateServiceImpl implements CandidateService {
    private final CandidateRepository candidateRepository;
    private final CandidateSkillsRepository candidateSkillsRepository;
    private final CvEntityRepository cvEntityRepository;
    private final SkillRepository skillRepository;
    private final CandidateMapper candidateMapper;
    private final MinioClient minioClient;
    private final CandidateProperties candidateProperties;
    private final RabbitTemplate rabbitTemplate;

    @Override
    @Transactional
    public Candidates save(CandidateRequest request) {
        return saveCandidateAndSkills(request);
    }

    @Transactional(readOnly = true)
    @Override
    public CandidateResponse findById(UUID candidateId) {
        Candidates candidates = candidateRepository.findById(candidateId).orElseThrow(() -> {
                    return new ResourceNotFoundException("Candidate not found with id: " + candidateId);
                }
        );
        return candidateMapper.toDto(candidates);
    }

    @Override
    @Transactional
    public CandidateResponse saveWithCv(
            CandidateRequest request,
            MultipartFile cvFile
    ) {
        if (cvFile == null || cvFile.isEmpty()) {
            throw new IllegalArgumentException("CV file is required");
        }

        Candidates candidate = saveCandidateAndSkills(request);

        String objectKey = "candidates/"
                + candidate.getId()
                + "/"
                + UUID.randomUUID()
                + "-"
                + cvFile.getOriginalFilename();

        CvEntity cv = CvEntity.builder()
                .candidate(candidate)
                .filePath(cvFile.getOriginalFilename())
                .fileType(cvFile.getContentType())
                .sizeBytes(cvFile.getSize())
                .objectKey(objectKey)
                .parseStatus(ParseStatus.PENDING)
                .uploadedAt(OffsetDateTime.now())
                .build();

        CvEntity savedCv = cvEntityRepository.save(cv);

        try {
            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(candidateProperties.minio().bucket())
                            .object(objectKey)
                            .stream(
                                    cvFile.getInputStream(),
                                    cvFile.getSize(),
                                    -1
                            )
                            .contentType(cvFile.getContentType())
                            .build()
            );

            CvParseCommand command = CvParseCommand.create(
                    UUID.randomUUID().toString(),
                    savedCv.getId(),
                    candidate.getId(),
                    objectKey,
                    cvFile.getContentType()
            );

           // publish
            rabbitTemplate.convertAndSend(RabbitMessagingNames.CV_RESULT_ROUTING_KEY, command);

            return candidateMapper.toDto(candidate);

        } catch (Exception exception) {
            savedCv.setParseStatus(ParseStatus.FAILED);
            cvEntityRepository.save(savedCv);

            throw new IllegalStateException(
                    "Cannot upload CV",
                    exception
            );
        }
    }

    private Candidates saveCandidateAndSkills(CandidateRequest request) {
        Candidates savedCandidate = candidateRepository.save(toEntity(request));

        List<UUID> requestedSkillIds = request.getSkillIds() == null
                ? List.of()
                : request.getSkillIds();

        Set<UUID> uniqueSkillIds = new LinkedHashSet<>(requestedSkillIds);
        if (uniqueSkillIds.isEmpty()) {
            return savedCandidate;
        }

        List<Skills> skills = skillRepository.findAllById(uniqueSkillIds);
        if (skills.size() != uniqueSkillIds.size()) {
            throw new IllegalArgumentException("One or more skill IDs do not exist");
        }

        List<CandidateSkills> candidateSkills = skills.stream()
                .map(skill -> CandidateSkills.builder()
                        .candidateSkillId(new CandidateSkillId(savedCandidate, skill))
                        .date(LocalDate.now())
                        .build())
                .toList();

        candidateSkillsRepository.saveAll(candidateSkills);
        return savedCandidate;
    }

    private Candidates toEntity(CandidateRequest request) {
        Candidates candidate = Candidates.builder()
                .email(request.getEmail())
                .fullName(request.getFullName())
                .phone(request.getPhone())
                .source(request.getSource())
                .utmSource(request.getUtmSource())
                .utmCampaign(request.getUtmCampaign())
                .utmMedium(request.getUtmMedium())
                .status(CandidateStatus.ACTIVE)
                .isDuplicate(false)
                .build();

        return candidate;
    }
}
