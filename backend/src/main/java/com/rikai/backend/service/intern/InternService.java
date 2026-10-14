package com.rikai.backend.service.intern;

import com.rikai.backend.common.CsvExportUtil;
import com.rikai.backend.common.ErrorCode;
import com.rikai.backend.common.InternStatus;
import com.rikai.backend.common.PageResponse;
import com.rikai.backend.dto.request.intern.InternCreationRequest;
import com.rikai.backend.dto.request.intern.InternUpdateRequest;
import com.rikai.backend.dto.response.intern.InternAnalysisResponse;
import com.rikai.backend.dto.response.intern.InternResponse;
import com.rikai.backend.exception.AppException;
import com.rikai.backend.mapper.InternMapper;
import com.rikai.backend.model.Intern;
import com.rikai.backend.model.InternshipBatch;
import com.rikai.backend.model.Position;
import com.rikai.backend.model.Users;
import com.rikai.backend.repository.InternRepository;
import com.rikai.backend.repository.InternshipBatchRepository;
import com.rikai.backend.repository.PositionRepository;
import com.rikai.backend.repository.UsersRepository;
import com.rikai.backend.event.InternCreatedEvent;
import com.rikai.backend.event.InternDeletedEvent;
import com.rikai.backend.event.InternUpdatedEvent;
import com.rikai.backend.service.auth.AuthenticationService;
import com.rikai.backend.validation.AutoGenerateEmail;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.text.Normalizer;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjusters;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class InternService implements IInternService {
    InternRepository internRepository;
    PositionRepository positionRepository;
    InternshipBatchRepository internshipBatchRepository;
    UsersRepository usersRepository;
    InternMapper internMapper;
    AuthenticationService authenticationService;
    ApplicationEventPublisher eventPublisher;

    @Override
    @Transactional(readOnly = true)
    public PageResponse<InternResponse> getAllInterns(
            PageRequest pageRequest,
            String keyword,
            String status,
            LocalDate startDate,
            LocalDate endDate,
            Long positionId,
            UUID mentorId) {
        String keywordValue = (keyword != null && !keyword.trim().isEmpty()) ? keyword.trim() : null;
        InternStatus internStatus = null;
        if (status != null && !status.trim().isEmpty()) {
            try {
                internStatus = InternStatus.valueOf(status.trim().toUpperCase());
            } catch (IllegalArgumentException e) {
                throw new AppException(ErrorCode.INVALID_INTERN_STATUS);
            }
        }
        Page<Intern> internPage = internRepository
                .getAllInternByKeyword(pageRequest, keywordValue, internStatus, startDate, endDate, positionId,
                        mentorId);
        return PageResponse.fromPage(internPage.map(internMapper::toInternResponse));
    }

    @Override
    @Transactional(readOnly = true)
    public InternResponse getInternById(Long id) {
        Intern intern = internRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.INTERN_NOT_EXISTED));
        return internMapper.toInternResponse(intern);
    }

    @Override
    @Transactional
    public InternResponse createIntern(InternCreationRequest request) {
        validateDateRange(request.getStartDate(), request.getEndDate());
        Position position = getPosition(request.getPositionId());
        Users mentor = getMentor(request.getMentorId());
        InternshipBatch internshipBatch = getBatch(request.getInternshipBatchId());
        Intern intern = internMapper.toIntern(request);
        String uniqueEmail = AutoGenerateEmail.generateUniqueEmail(
                request.getFullName(),
                internRepository::existsByEmail);
        intern.setEmail(uniqueEmail);
        intern.setEmail(uniqueEmail);
        intern.setPosition(position);
        intern.setMentor(mentor);
        intern.setInternshipBatch(internshipBatch);
        Intern saved = internRepository.save(intern);
        return internMapper.toInternResponse(saved);
    }

    @Override
    @Transactional
    public InternResponse updateIntern(Long id, InternUpdateRequest request) {
        Intern intern = internRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.INTERN_NOT_EXISTED));
        validateDateRange(request.getStartDate(), request.getEndDate());
        Position position = getPosition(request.getPositionId());
        Users mentor = getMentor(request.getMentorId());
        if (request.getInternShipBatchId() != null) {
            InternshipBatch batch = getBatch(request.getInternShipBatchId());
            intern.setInternshipBatch(batch);
        }
        intern.setFullName(request.getFullName());
        intern.setPosition(position);
        intern.setMentor(mentor);
        intern.setStartDate(request.getStartDate());
        intern.setEndDate(request.getEndDate());
        intern.setInternStatus(request.getInternStatus());
        Intern saved = internRepository.save(intern);
        eventPublisher.publishEvent(new InternUpdatedEvent(this, saved));
        return internMapper.toInternResponse(saved);
    }

    @Override
    @Transactional
    public void deleteIntern(Long id) {
        Intern intern = internRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.INTERN_NOT_EXISTED));
        Long internId = intern.getId();
        String internName = intern.getFullName();
        intern.setInternStatus(InternStatus.DROPPED);
        internRepository.save(intern);
        eventPublisher.publishEvent(new InternDeletedEvent(this, internId, internName));
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<InternResponse> getInternsByMentor(UUID mentorId, Pageable pageable) {
        if (!usersRepository.existsById(mentorId)) {
            throw new AppException(ErrorCode.MENTOR_NOT_EXISTED);
        }
        Page<Intern> internPage = internRepository.findByMentor_Id(mentorId, pageable);
        return PageResponse.fromPage(internPage.map(internMapper::toInternResponse));
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<InternResponse> getInternsByPositionId(Long positionId, Pageable pageable) {
        if (!positionRepository.existsById(positionId)) {
            throw new AppException(ErrorCode.POSITION_NOT_EXISTED);
        }
        Page<Intern> internPage = internRepository.findByPosition_Id(positionId, pageable);
        return PageResponse.fromPage(internPage.map(internMapper::toInternResponse));
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<InternResponse> getMyIntern(
            Pageable pageable,
            String keyword,
            String status,
            LocalDate startDate,
            LocalDate endDate,
            Long positionId) {
        Users users = authenticationService.getCurrentUser();
        var mentorId = users.getId();
        if (!usersRepository.existsById(mentorId)) {
            throw new AppException(ErrorCode.MENTOR_NOT_EXISTED);
        }
        String keywordValue = (keyword != null && !keyword.trim().isEmpty()) ? keyword.trim() : null;
        InternStatus internStatus = null;
        if (status != null && !status.trim().isEmpty()) {
            try {
                internStatus = InternStatus.valueOf(status.trim().toUpperCase());
            } catch (IllegalArgumentException e) {
                throw new AppException(ErrorCode.INVALID_INTERN_STATUS);
            }
        }

        Page<Intern> internPage = internRepository
                .getAllInternByKeyword(pageable, keywordValue, internStatus, startDate, endDate, positionId, mentorId);

        return PageResponse.fromPage(internPage.map(internMapper::toInternResponse));
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<InternResponse> getInternsByStatus(InternStatus status, Pageable pageable) {
        Page<Intern> internPage = internRepository.findByInternStatus(status, pageable);
        return PageResponse.fromPage(internPage.map(internMapper::toInternResponse));
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<InternResponse> getInternsNotEvaluatedThisWeek(Pageable pageable) {
        Users currentUser = authenticationService.getCurrentUser();
        if (currentUser == null) {
            throw new AppException(ErrorCode.UNAUTHENTICATED);
        }
        LocalDate today = LocalDate.now();
        LocalDate weekStartDate = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        Page<Intern> internPage;
        if ("ADMIN".equals(currentUser.getRole().getRoleName())) {
            internPage = internRepository.findAllInternsNotEvaluatedThisWeek(weekStartDate, pageable);
        } else {
            UUID mentorId = currentUser.getId();
            internPage = internRepository.findInternsNotEvaluatedThisWeekByMentor(mentorId, weekStartDate, pageable);
        }
        return PageResponse.fromPage(internPage.map(internMapper::toInternResponse));
    }

    @Override
    @Transactional(readOnly = true)
    public InternAnalysisResponse getAnalysis() {
        long totalInterns = internRepository.count();
        long totalMentors = usersRepository.countByRole_RoleName("MENTOR");
        long activeInterns = internRepository.countByInternStatus(InternStatus.ACTIVE);
        long warningInterns = internRepository.countByInternStatus(InternStatus.WARNING);
        long droppedInterns = internRepository.countByInternStatus(InternStatus.DROPPED);
        long completedInterns = internRepository.countByInternStatus(InternStatus.COMPLETED);
        return InternAnalysisResponse.builder()
                .totalInterns(totalInterns)
                .totalMentors(totalMentors)
                .activeInterns(activeInterns)
                .warningInterns(warningInterns)
                .droppedInterns(droppedInterns)
                .completedInterns(completedInterns)
                .build();
    }

    private void validateDateRange(LocalDate startDate, LocalDate endDate) {
        if (startDate != null && endDate != null && endDate.isBefore(startDate)) {
            throw new AppException(ErrorCode.INVALID_DATE_RANGE);
        }
    }

    private Position getPosition(Long positionId) {
        return positionRepository.findById(positionId)
                .orElseThrow(() -> new AppException(ErrorCode.POSITION_NOT_EXISTED));
    }

    private Users getMentor(UUID mentorId) {
        return usersRepository.findById(mentorId)
                .orElseThrow(() -> new AppException(ErrorCode.MENTOR_NOT_EXISTED));
    }

    private InternshipBatch getBatch(Long batchId) {
        return internshipBatchRepository.findById(batchId)
                .orElseThrow(() -> new AppException(ErrorCode.BATCH_NOT_EXISTED));
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<InternResponse> getInternsByBatch(Long batchId, Pageable pageable, String keyword,
            String status) {
        if (!internshipBatchRepository.existsById(batchId)) {
            throw new AppException(ErrorCode.BATCH_NOT_EXISTED);
        }

        String keywordValue = (keyword != null && !keyword.trim().isEmpty()) ? keyword.trim() : null;
        InternStatus internStatus = null;
        if (status != null && !status.trim().isEmpty()) {
            try {
                internStatus = InternStatus.valueOf(status.trim().toUpperCase());
            } catch (IllegalArgumentException e) {
                throw new AppException(ErrorCode.INVALID_INTERN_STATUS);
            }
        }

        Page<Intern> internPage = internRepository.findByInternshipBatchWithFilters(
                batchId,
                keywordValue,
                internStatus,
                pageable);
        return PageResponse.fromPage(internPage.map(internMapper::toInternResponse));
    }

    @Override
    public PageResponse<InternResponse> findAllInternsByDepartmentOfMentor(Pageable pageable) {
        Users users = authenticationService.getCurrentUser();
        var mentorId = users.getId();
        if (!usersRepository.existsById(mentorId)) {
            throw new AppException(ErrorCode.MENTOR_NOT_EXISTED);
        }
        Page<Intern> internPage = internRepository.findAllInternsByDepartmentOfMentor(mentorId, pageable);
        return PageResponse.fromPage(internPage.map(internMapper::toInternResponse));
    }

    @Override
    @Transactional
    public void bulkUpdateInterns(List<Long> internIds, InternUpdateRequest request) {
        if (internIds == null || internIds.isEmpty()) {
            return;
        }
        validateDateRange(request.getStartDate(), request.getEndDate());
        Position position = getPosition(request.getPositionId());
        Users mentor = getMentor(request.getMentorId());
        InternshipBatch batch = getBatch(request.getInternShipBatchId());
        List<Intern> interns = internRepository.findAllById(internIds);
        if (interns.isEmpty()) {
            throw new AppException(ErrorCode.INTERN_NOT_EXISTED);
        }
        for (Intern intern : interns) {
            intern.setFullName(request.getFullName());
            intern.setPosition(position);
            intern.setMentor(mentor);
            intern.setInternshipBatch(batch);
            intern.setStartDate(request.getStartDate());
            intern.setEndDate(request.getEndDate());
            intern.setInternStatus(request.getInternStatus());
        }
        internRepository.saveAll(interns);
    }

    @Override
    @Transactional
    public void bulkDeleteInterns(List<Long> internIds) {
        if (internIds == null || internIds.isEmpty()) {
            return;
        }
        List<Intern> interns = internRepository.findAllById(internIds);
        if (interns.isEmpty()) {
            throw new AppException(ErrorCode.INTERN_NOT_EXISTED);
        }
        for (Intern intern : interns) {
            intern.setInternStatus(InternStatus.DROPPED);
        }
        internRepository.saveAll(interns);
    }

    @Override
    @Transactional
    public void permanentDeleteIntern(Long id) {
        Intern intern = internRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.INTERN_NOT_EXISTED));
        internRepository.delete(intern);
    }

    @Override
    @Transactional
    public InternResponse updateInternStatus(Long id, InternStatus internStatus) {
        Intern intern = internRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.INTERN_NOT_EXISTED));

        intern.setInternStatus(internStatus);
        Intern saved = internRepository.save(intern);

        eventPublisher.publishEvent(new InternUpdatedEvent(this, saved));
        return internMapper.toInternResponse(saved);
    }
    private InternStatus parseInternStatus(String status){
        InternStatus internStatus = null;
        if(status!=null && !status.trim().isEmpty()){
            try {
                internStatus = InternStatus.valueOf(status.trim().toUpperCase());
            } catch (IllegalArgumentException e) {
                throw new AppException(ErrorCode.INVALID_INTERN_STATUS);
            }
        }
        return  internStatus;
    }

    @Override
    @Transactional(readOnly = true)
    public StreamingResponseBody exportInterns(String keyword, String status, LocalDate startDate, LocalDate endDate, Long positionId, UUID mentorId) {
        DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        return outputStream -> {
            InternStatus internStatus = parseInternStatus(status);
            int a=1;
            try (Writer writer = new OutputStreamWriter(outputStream, StandardCharsets.UTF_8)) {
                // Ghi BOM cho Excel
                writer.write(CsvExportUtil.BYTE_ORDER_MARK);

                // Ghi Header theo đúng thứ tự màn hình Admin
                List<String> headers = Arrays.asList(
                        "Full name", "Position", "Mentor", "Start date",
                        "End date", "Status", "ID", "Email", "Phone", "Batch"
                );
                CsvExportUtil.writeLine(writer, headers);

                int page = 0;
                int size = 500;
                Page<Intern> internPage;

                do {

                    Pageable pageable = PageRequest.of(page, size, Sort.by("id").ascending());

                    internPage = internRepository.getAllInternByKeyword(
                            pageable, keyword, internStatus, startDate, endDate, positionId, mentorId
                    );

                    for (Intern intern : internPage.getContent()) {
                        String formattedStartDate = intern.getStartDate() != null ? "'"+intern.getStartDate().format(DATE_FORMATTER) : "";
                        String formattedEndDate = intern.getEndDate() != null ? "'"+intern.getEndDate().format(DATE_FORMATTER) : "";
                        List<String> row = Arrays.asList(
                                intern.getFullName(),
                                intern.getPosition() != null ? intern.getPosition().getTitle(): "",
                                intern.getMentor() != null ? intern.getMentor().getFullName() : "",
                                formattedStartDate,
                                formattedEndDate,
                                intern.getInternStatus() != null ? intern.getInternStatus().name() : "",
                                intern.getId() != null ? intern.getId().toString() : "",
                                intern.getEmail(),
                                intern.getPhone(),
                                intern.getInternshipBatch() != null ? intern.getInternshipBatch().getName() : ""
                        );
                        CsvExportUtil.writeLine(writer, row);
                    }

                    writer.flush();
                    page++;

                } while (internPage.hasNext());

            } catch (Exception e) {
                e.printStackTrace();
            }
        };
    }
}
