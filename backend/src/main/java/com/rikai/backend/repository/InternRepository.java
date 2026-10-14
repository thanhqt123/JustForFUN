package com.rikai.backend.repository;

import com.rikai.backend.common.InternStatus;
import com.rikai.backend.model.Intern;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Repository
public interface InternRepository extends JpaRepository<Intern, Long> {
        @EntityGraph(attributePaths = {"position", "mentor"})
        @Query("""
                        SELECT i FROM Intern i
                        WHERE (:keyword IS NULL OR :keyword = '' OR LOWER(i.fullName) LIKE LOWER(CONCAT('%', :keyword, '%')))
                        AND (:internStatus IS NULL OR i.internStatus = :internStatus)
                        AND (:startDate IS NULL OR i.startDate >= :startDate)
                        AND (:endDate IS NULL OR i.startDate <= :endDate)
                        AND (:positionId IS NULL OR i.position.id = :positionId)
                        AND (:mentorId IS NULL OR i.mentor.id = :mentorId)
                         """)
        Page<Intern> getAllInternByKeyword(Pageable pageable,
                        @Param("keyword") String keyword,
                        @Param("internStatus") InternStatus internStatus,
                        @Param("startDate") LocalDate startDate,
                        @Param("endDate") LocalDate endDate,
                        @Param("positionId") Long positionId,
                        @Param("mentorId") UUID mentorId);

        Page<Intern> findByMentor_Id(UUID mentorId, Pageable pageable);

        @Query("""
                        SELECT i FROM Intern i
                        WHERE i.mentor.id = :mentorId
                        AND (:keyword IS NULL OR LOWER(i.fullName) LIKE LOWER(CONCAT('%', :keyword, '%')))
                        """)
        Page<Intern> findByMentor_IdAndKeyword(
                        @org.springframework.data.repository.query.Param("mentorId") UUID mentorId,
                        @org.springframework.data.repository.query.Param("keyword") String keyword,
                        Pageable pageable);

        Page<Intern> findByPosition_Id(Long positionId, Pageable pageable);

        Page<Intern> findByInternStatus(InternStatus status, Pageable pageable);

        Page<Intern> findByInternshipBatch_Id(Long batchId, Pageable pageable);

        @Query("""
                        SELECT i FROM Intern i
                        WHERE i.internshipBatch.id = :batchId
                        AND (:keyword IS NULL OR :keyword = '' OR LOWER(i.fullName) LIKE LOWER(CONCAT('%', :keyword, '%')))
                        AND (:internStatus IS NULL OR i.internStatus = :internStatus)
                        """)
        Page<Intern> findByInternshipBatchWithFilters(@Param("batchId") Long batchId,
                        @Param("keyword") String keyword,
                        @Param("internStatus") InternStatus internStatus,
                        Pageable pageable);

        long countByInternStatus(InternStatus status);

        long countByMentor_Id(UUID mentorId);

        long countByMentor_IdAndInternStatus(UUID mentorId, InternStatus status);

        long countByPosition_Id(Long positionId);

        long countByInternshipBatch_Id(Long batchId);

        @Query("""
                        SELECT i FROM Intern i
                        WHERE i.mentor.id = :mentorId
                        AND i.internStatus != 'DROPPED'
                        AND NOT EXISTS (
                            SELECT 1 FROM WeeklyReport wr
                            WHERE wr.intern.id = i.id
                            AND wr.mentor.id = :mentorId
                            AND wr.weekStartDate = :weekStartDate
                        )
                        ORDER BY i.fullName ASC
                        """)
        Page<Intern> findInternsNotEvaluatedThisWeekByMentor(@Param("mentorId") UUID mentorId,
                        @Param("weekStartDate") LocalDate weekStartDate,
                        Pageable pageable);

        @Query("""
                        SELECT i FROM Intern i
                        WHERE i.internStatus != 'DROPPED'
                        AND NOT EXISTS (
                            SELECT 1 FROM WeeklyReport wr
                            WHERE wr.intern.id = i.id
                            AND wr.mentor.id = i.mentor.id
                            AND wr.weekStartDate = :weekStartDate
                        )
                        ORDER BY i.fullName ASC
                        """)
        Page<Intern> findAllInternsNotEvaluatedThisWeek(@Param("weekStartDate") LocalDate weekStartDate,
                        Pageable pageable);

        /*
         * Find all interns where the mentor's department matches the department of the
         * given mentor ID
         */
        @EntityGraph(attributePaths = {
                        "position",
                        "internshipBatch",
                        "mentor",
                        "mentor.department"
        })
        @Query("""
                        SELECT i
                        FROM Intern i
                        JOIN Users mu ON mu.id = :mentorId
                        WHERE i.mentor.department = mu.department
                        """)
        Page<Intern> findAllInternsByDepartmentOfMentor(@Param("mentorId") UUID mentorId, Pageable pageable);

        /*
         * Check if an intern exists by email
         */
        boolean existsByEmail(String email);

        // Select new intern (Filtered by createdAt)
        List<Intern> findByCreatedAtAfter(Instant date, Pageable pageable);

        // Select updated intern (Filtered by updatedAt)
        @Query("SELECT i FROM Intern i WHERE i.updatedAt > :date AND i.internStatus != 'DROPPED'")
        List<Intern> findRecentlyUpdated(@Param("date") Instant date, Pageable pageable);

        // Select deleted intern (Filtered by updatedAt)
        @Query("SELECT i FROM Intern i WHERE i.updatedAt > :date AND i.internStatus = 'DROPPED'")
        List<Intern> findRecentlyDeleted(@Param("date") Instant date, Pageable pageable);
}
