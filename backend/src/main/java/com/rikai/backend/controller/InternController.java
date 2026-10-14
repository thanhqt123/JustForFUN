package com.rikai.backend.controller;

import com.rikai.backend.common.ApiResponse;
import com.rikai.backend.common.InternStatus;
import com.rikai.backend.common.PageResponse;
import com.rikai.backend.common.SuccessCode;
import com.rikai.backend.dto.request.intern.InternCreationRequest;
import com.rikai.backend.dto.request.intern.InternUpdateRequest;
import com.rikai.backend.dto.request.intern.InternStatusUpdateRequest;
import com.rikai.backend.dto.response.intern.InternAnalysisResponse;
import com.rikai.backend.dto.response.intern.InternResponse;
import com.rikai.backend.service.intern.IInternService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/interns")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class InternController {
    IInternService internService;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MENTOR')")
    public ApiResponse<PageResponse<InternResponse>> getAllInterns(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int limit,
            @RequestParam(required = false, name = "keyword") String keyword,
            @RequestParam(required = false, name = "status") String status,
            @RequestParam(required = false, name = "start_date") LocalDate startDate,
            @RequestParam(required = false, name = "end_date") LocalDate endDate,
            @RequestParam(required = false, name = "position_id") Long positionId,
            @RequestParam(required = false, name = "mentor_id") UUID mentorId) {
        PageRequest pageable = PageRequest.of(page, limit);
        return ApiResponse.buildSuccessResponse(
                internService.getAllInterns(pageable, keyword, status, startDate, endDate, positionId, mentorId),
                SuccessCode.GET_ALL_INTERNS_SUCCESSFUL);
    }

    @GetMapping("/my-interns")
    @PreAuthorize("hasRole('MENTOR')")
    public ApiResponse<PageResponse<InternResponse>> getMyIntern(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int limit,
            @RequestParam(required = false, name = "keyword") String keyword,
            @RequestParam(required = false, name = "status") String status,
            @RequestParam(required = false, name = "start_date") LocalDate startDate,
            @RequestParam(required = false, name = "end_date") LocalDate endDate,
            @RequestParam(required = false, name = "position_id") Long positionId) {
        Pageable pageable = PageRequest.of(page, limit);
        return ApiResponse.buildSuccessResponse(
                internService.getMyIntern(pageable, keyword, status, startDate, endDate, positionId),
                SuccessCode.GET_ALL_INTERNS_SUCCESSFUL);
    }

    @GetMapping("/not-evaluated-this-week")
    @PreAuthorize("hasAnyRole('ADMIN', 'MENTOR')")
    public ApiResponse<PageResponse<InternResponse>> getInternsNotEvaluatedThisWeek(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int limit) {
        Pageable pageable = PageRequest.of(page, limit);
        return ApiResponse.buildSuccessResponse(
                internService.getInternsNotEvaluatedThisWeek(pageable),
                SuccessCode.GET_ALL_INTERNS_SUCCESSFUL);
    }

    @GetMapping("/analyze")
    @PreAuthorize("hasAnyRole('ADMIN', 'MENTOR')")
    public ApiResponse<InternAnalysisResponse> getAnalysis() {
        return ApiResponse.buildSuccessResponse(internService.getAnalysis(), SuccessCode.GET_ALL_INTERNS_SUCCESSFUL);
    }

    @GetMapping("/mentor/{mentorId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MENTOR')")
    public ApiResponse<PageResponse<InternResponse>> getInternsByMentor(@PathVariable UUID mentorId,
            Pageable pageable) {
        return ApiResponse.buildSuccessResponse(internService.getInternsByMentor(mentorId, pageable),
                SuccessCode.GET_ALL_INTERNS_SUCCESSFUL);
    }

    @GetMapping("/status/{status}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MENTOR')")
    public ApiResponse<PageResponse<InternResponse>> getInternsByStatus(@PathVariable InternStatus status,
            Pageable pageable) {
        return ApiResponse.buildSuccessResponse(internService.getInternsByStatus(status, pageable),
                SuccessCode.GET_ALL_INTERNS_SUCCESSFUL);
    }

    @GetMapping("/position/{positionId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MENTOR')")
    public ApiResponse<PageResponse<InternResponse>> getInternsByPositionId(@PathVariable Long positionId,
            Pageable pageable) {
        return ApiResponse.buildSuccessResponse(internService.getInternsByPositionId(positionId, pageable),
                SuccessCode.GET_ALL_INTERNS_SUCCESSFUL);
    }

    @GetMapping("/{id:\\d+}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MENTOR')")
    public ApiResponse<InternResponse> getInternById(@PathVariable Long id) {
        return ApiResponse.buildSuccessResponse(internService.getInternById(id), SuccessCode.GET_INTERN_SUCCESSFUL);
    }

    @GetMapping("/department-interns")
    @PreAuthorize("hasRole('MENTOR')")
    public ApiResponse<PageResponse<InternResponse>> getAllInternsByDepartmentOfMentor(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int limit) {
        Pageable pageable = PageRequest.of(page, limit);
        return ApiResponse.buildSuccessResponse(
                internService.findAllInternsByDepartmentOfMentor(pageable),
                SuccessCode.GET_ALL_INTERNS_SUCCESSFUL);
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MENTOR')")
    public ApiResponse<InternResponse> createIntern(@Valid @RequestBody InternCreationRequest request) {
        return ApiResponse.buildSuccessResponse(internService.createIntern(request),
                SuccessCode.CREATE_INTERN_SUCCESSFUL);
    }

    @PutMapping("/{id:\\d+}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MENTOR')")
    public ApiResponse<InternResponse> updateIntern(@PathVariable Long id,
            @Valid @RequestBody InternUpdateRequest request) {
        return ApiResponse.buildSuccessResponse(internService.updateIntern(id, request),
                SuccessCode.UPDATE_INTERN_SUCCESSFUL);
    }

    @DeleteMapping("/{id:\\d+}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MENTOR')")
    public ApiResponse<Void> deleteIntern(@PathVariable Long id) {
        internService.deleteIntern(id);
        return ApiResponse.buildSuccessResponse(null, SuccessCode.DELETE_INTERN_SUCCESSFUL);
    }

    @GetMapping("/batch/{batchId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MENTOR')")
    public ApiResponse<PageResponse<InternResponse>> getInternsByBatch(
            @PathVariable Long batchId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int limit,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String status) {
        Pageable pageable = PageRequest.of(page, limit);
        return ApiResponse.buildSuccessResponse(
                internService.getInternsByBatch(batchId, pageable, keyword, status),
                SuccessCode.GET_ALL_INTERNS_SUCCESSFUL);
    }

    @PostMapping("/bulk-update")
    @PreAuthorize("hasAnyRole('ADMIN', 'HR', 'MENTOR')")
    public ApiResponse<Void> bulkUpdateInterns(
            @RequestParam List<Long> internIds,
            @Valid @RequestBody InternUpdateRequest request) {
        internService.bulkUpdateInterns(internIds, request);
        return ApiResponse.buildSuccessResponse(null, SuccessCode.UPDATE_INTERN_SUCCESSFUL);
    }

    @DeleteMapping("/bulk-delete")
    @PreAuthorize("hasAnyRole('ADMIN', 'HR', 'MENTOR')")
    public ApiResponse<Void> bulkDeleteInterns(@RequestParam List<Long> internIds) {
        internService.bulkDeleteInterns(internIds);
        return ApiResponse.buildSuccessResponse(null, SuccessCode.DELETE_INTERN_SUCCESSFUL);
    }

    @DeleteMapping("/{id:\\d+}/permanent")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<Void> permanentDeleteIntern(@PathVariable Long id) {
        internService.permanentDeleteIntern(id);
        return ApiResponse.buildSuccessResponse(null, SuccessCode.DELETE_INTERN_SUCCESSFUL);
    }

    @PatchMapping("/{id:\\d+}/status")
    @PreAuthorize("hasAnyRole('ADMIN', 'MENTOR')")
    public ApiResponse<InternResponse> updateInternStatus(
            @PathVariable Long id,
            @Valid @RequestBody InternStatusUpdateRequest request) {
        return ApiResponse.buildSuccessResponse(internService.updateInternStatus(id, request.getInternStatus()),
                SuccessCode.UPDATE_INTERN_SUCCESSFUL);
    }

    @GetMapping("/export")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Export interns to CSV")
    public ResponseEntity<StreamingResponseBody> exportInterns(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) LocalDate start_date,
            @RequestParam(required = false) LocalDate end_date,
            @RequestParam(required = false) Long position_id,
            @RequestParam(required = false) UUID mentor_id) {

        StreamingResponseBody stream = internService.exportInterns(
                keyword, status, start_date, end_date, position_id, mentor_id
        );

        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        String filename = "interns_" + timestamp + ".csv";

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .header(HttpHeaders.CONTENT_TYPE, "text/csv; charset=UTF-8")
                .body(stream);
    }
}
