package com.rikai.backend.service.intern;

import com.rikai.backend.common.InternStatus;
import com.rikai.backend.common.PageResponse;
import com.rikai.backend.dto.request.intern.InternCreationRequest;
import com.rikai.backend.dto.request.intern.InternUpdateRequest;
import com.rikai.backend.dto.response.intern.InternAnalysisResponse;
import com.rikai.backend.dto.response.intern.InternResponse;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.io.OutputStream;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface IInternService {
    PageResponse<InternResponse> getAllInterns(
            PageRequest pageRequest,
            String keyword,
            String status,
            LocalDate startDate,
            LocalDate endDate,
            Long positionId,
            UUID mentorId);

    InternResponse getInternById(Long id);

    InternResponse createIntern(InternCreationRequest request);

    InternResponse updateIntern(Long id, InternUpdateRequest request);

    void deleteIntern(Long id);

    PageResponse<InternResponse> getInternsByMentor(UUID mentorId, Pageable pageable);

    PageResponse<InternResponse> getInternsByStatus(InternStatus status, Pageable pageable);

    PageResponse<InternResponse> getInternsByPositionId(Long positionId, Pageable pageable);

    PageResponse<InternResponse> getMyIntern(
            Pageable pageable,
            String keyword,
            String status,
            LocalDate startDate,
            LocalDate endDate,
            Long positionId);

    PageResponse<InternResponse> getInternsNotEvaluatedThisWeek(Pageable pageable);

    InternAnalysisResponse getAnalysis();

    PageResponse<InternResponse> getInternsByBatch(Long batchId, Pageable pageable, String keyword, String status);

    PageResponse<InternResponse> findAllInternsByDepartmentOfMentor(Pageable pageable);

    void bulkUpdateInterns(List<Long> internIds, InternUpdateRequest request);

    void bulkDeleteInterns(List<Long> internIds);

    void permanentDeleteIntern(Long id);

    InternResponse updateInternStatus(Long id, InternStatus internStatus);

    StreamingResponseBody exportInterns(String keyword, String internStatus, LocalDate startDate, LocalDate endDate, Long positionId, UUID mentorId) ;
}
