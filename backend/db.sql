

-- =====================================================
-- 1. ROLES TABLE
-- =====================================================
CREATE TABLE IF NOT EXISTS roles (
    role_name VARCHAR(50) PRIMARY KEY NOT NULL,
    description TEXT,
    INDEX idx_role_name (role_name)
)  ENGINE=INNODB DEFAULT CHARSET=UTF8MB4 COLLATE = UTF8MB4_UNICODE_CI;

-- =====================================================
-- 2. DEPARTMENTS TABLE
-- =====================================================
CREATE TABLE IF NOT EXISTS departments (
    id INT AUTO_INCREMENT PRIMARY KEY,
    title VARCHAR(255) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
)  ENGINE=INNODB DEFAULT CHARSET=UTF8MB4 COLLATE = UTF8MB4_UNICODE_CI;

-- =====================================================
-- 3. USERS TABLE (Mentors, Admins)
-- =====================================================
CREATE TABLE IF NOT EXISTS users (
    id CHAR(36) PRIMARY KEY NOT NULL,
    email VARCHAR(100) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    full_name VARCHAR(100) NOT NULL,
    date_of_birth DATE,
    is_active BOOLEAN DEFAULT TRUE,
    role_name VARCHAR(50) NOT NULL,
    department_id INT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_user_email (email),
    INDEX idx_user_role (role_name),
    INDEX idx_user_department (department_id),
    INDEX idx_user_active (is_active),
    FOREIGN KEY (role_name)
        REFERENCES roles (role_name)
        ON DELETE RESTRICT,
    FOREIGN KEY (department_id)
        REFERENCES departments (id)
        ON DELETE SET NULL
)  ENGINE=INNODB DEFAULT CHARSET=UTF8MB4 COLLATE = UTF8MB4_UNICODE_CI;

-- =====================================================
-- 4. JOB POSITIONS TABLE
-- =====================================================
CREATE TABLE IF NOT EXISTS positions (
    id INT AUTO_INCREMENT PRIMARY KEY,
    title VARCHAR(255) NOT NULL,
    department_id INT,
    description TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_position_title (title),
    INDEX idx_position_department (department_id),
    FOREIGN KEY (department_id)
        REFERENCES departments (id)
        ON DELETE SET NULL
)  ENGINE=INNODB DEFAULT CHARSET=UTF8MB4 COLLATE = UTF8MB4_UNICODE_CI;

-- =====================================================
-- 5. INTERNSHIP BATCHES TABLE
-- =====================================================
CREATE TABLE IF NOT EXISTS internship_batches (
    id INT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    start_date DATE NOT NULL,
    end_date DATE,
    description TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_batch_name (name),
    INDEX idx_batch_dates (start_date , end_date)
)  ENGINE=INNODB DEFAULT CHARSET=UTF8MB4 COLLATE = UTF8MB4_UNICODE_CI;

-- =====================================================
-- 6. INTERNS TABLE
-- =====================================================
CREATE TABLE IF NOT EXISTS interns (
    id INT AUTO_INCREMENT PRIMARY KEY,
    full_name VARCHAR(255) NOT NULL,
    email VARCHAR(255) UNIQUE,
    phone VARCHAR(20),
    position_id INT NOT NULL,
    batch_id INT,
    mentor_id CHAR(36),
    start_date DATE NOT NULL,
    end_date DATE,
    intern_status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    offer_status VARCHAR(20) DEFAULT 'NONE',
    offer_date DATE,
    offer_notes TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_intern_name (full_name),
    INDEX idx_intern_email (email),
    INDEX idx_intern_position (position_id),
    INDEX idx_intern_batch (batch_id),
    INDEX idx_intern_mentor (mentor_id),
    INDEX idx_intern_status (intern_status),
    INDEX idx_intern_offer_status (offer_status),
    FOREIGN KEY (position_id)
        REFERENCES positions (id)
        ON DELETE RESTRICT,
    FOREIGN KEY (batch_id)
        REFERENCES internship_batches (id)
        ON DELETE SET NULL,
    FOREIGN KEY (mentor_id)
        REFERENCES users (id)
        ON DELETE SET NULL,
    CONSTRAINT chk_intern_status CHECK (intern_status IN ('ACTIVE' , 'WARNING', 'COMPLETED', 'DROPPED')),
    CONSTRAINT chk_intern_offer_status CHECK (offer_status IN ('NONE' , 'PROPOSED', 'ACCEPTED', 'REJECTED'))
)  ENGINE=INNODB DEFAULT CHARSET=UTF8MB4 COLLATE = UTF8MB4_UNICODE_CI;
-- =====================================================
-- 7. WEEKLY REPORTS TABLE
-- =====================================================
CREATE TABLE IF NOT EXISTS weekly_reports (
    id INT AUTO_INCREMENT PRIMARY KEY,
    intern_id INT NOT NULL,
    mentor_id CHAR(36) NOT NULL,
    week_number INT,
    week_start_date DATE NOT NULL,
    tasks_assigned TEXT,
    tasks_completed TEXT,
    issues_risks TEXT,
    mentor_overall_comment TEXT,
    status VARCHAR(20) DEFAULT 'submitted',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_report_intern (intern_id),
    INDEX idx_report_mentor (mentor_id),
    INDEX idx_report_week (week_start_date),
    INDEX idx_report_status (status),
    UNIQUE KEY uk_report_intern_week (intern_id , week_start_date),
    FOREIGN KEY (intern_id)
        REFERENCES interns (id)
        ON DELETE CASCADE,
    FOREIGN KEY (mentor_id)
        REFERENCES users (id)
        ON DELETE RESTRICT
)  ENGINE=INNODB DEFAULT CHARSET=UTF8MB4 COLLATE = UTF8MB4_UNICODE_CI;

-- =====================================================
-- 8. EVALUATION CRITERIA TABLE
-- =====================================================
CREATE TABLE IF NOT EXISTS evaluation_criteria (
    id INT AUTO_INCREMENT PRIMARY KEY,
    category VARCHAR(50),
    name VARCHAR(255) NOT NULL,
    description TEXT,
    weight DECIMAL(5 , 2 ) DEFAULT 1.00,
    is_active BOOLEAN DEFAULT TRUE,
    display_order INT DEFAULT 0,
    INDEX idx_criteria_category (category),
    INDEX idx_criteria_active (is_active)
)  ENGINE=INNODB DEFAULT CHARSET=UTF8MB4 COLLATE = UTF8MB4_UNICODE_CI;


-- =====================================================
-- 9. EVALUATION TEMPLATES TABLE
-- =====================================================
CREATE TABLE IF NOT EXISTS evaluation_templates (
    id INT AUTO_INCREMENT PRIMARY KEY,
    position_id INT NOT NULL,
    criteria_id INT NOT NULL,
    is_required BOOLEAN DEFAULT TRUE,
    weight_override DECIMAL(5 , 2 ),
    INDEX idx_template_position (position_id),
    INDEX idx_template_criteria (criteria_id),
    UNIQUE KEY uk_template_position_criteria (position_id , criteria_id),
    FOREIGN KEY (position_id)
        REFERENCES positions (id)
        ON DELETE CASCADE,
    FOREIGN KEY (criteria_id)
        REFERENCES evaluation_criteria (id)
        ON DELETE CASCADE
)  ENGINE=INNODB DEFAULT CHARSET=UTF8MB4 COLLATE = UTF8MB4_UNICODE_CI;

-- =====================================================
-- 10. EVALUATION SESSIONS TABLE
-- =====================================================
CREATE TABLE IF NOT EXISTS evaluation_sessions (
    id INT AUTO_INCREMENT PRIMARY KEY,
    intern_id INT NOT NULL,
    mentor_id CHAR(36) NOT NULL,
    session_type VARCHAR(20) NOT NULL,
    evaluation_date DATE NOT NULL,
    final_score DECIMAL(5 , 2 ),
    level_assessment VARCHAR(255),
    conclusion VARCHAR(50),
    overall_comment TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_session_intern (intern_id),
    INDEX idx_session_mentor (mentor_id),
    INDEX idx_session_type (session_type),
    INDEX idx_session_date (evaluation_date),
    FOREIGN KEY (intern_id)
        REFERENCES interns (id)
        ON DELETE CASCADE,
    FOREIGN KEY (mentor_id)
        REFERENCES users (id)
        ON DELETE RESTRICT,
    CONSTRAINT chk_session_type CHECK (session_type IN ('WEEKLY' , 'MID_TERM', 'FINAL')),
    CONSTRAINT chk_session_conclusion CHECK (conclusion IS NULL
        OR conclusion IN ('PASS' , 'NEED_IMPROVEMENT', 'FAIL'))
)  ENGINE=INNODB DEFAULT CHARSET=UTF8MB4 COLLATE = UTF8MB4_UNICODE_CI;

-- =====================================================
-- 11. EVALUATION SCORES TABLE
-- =====================================================
CREATE TABLE IF NOT EXISTS evaluation_scores (
    id INT AUTO_INCREMENT PRIMARY KEY,
    session_id INT NOT NULL,
    criteria_id INT NOT NULL,
    score TINYINT,
    comment TEXT,
    INDEX idx_score_session (session_id),
    INDEX idx_score_criteria (criteria_id),
    UNIQUE KEY uk_score_session_criteria (session_id , criteria_id),
    FOREIGN KEY (session_id)
        REFERENCES evaluation_sessions (id)
        ON DELETE CASCADE,
    FOREIGN KEY (criteria_id)
        REFERENCES evaluation_criteria (id)
        ON DELETE RESTRICT,
    CONSTRAINT chk_score_range CHECK (score IS NULL
        OR (score >= 1 AND score <= 10))
)  ENGINE=INNODB DEFAULT CHARSET=UTF8MB4 COLLATE = UTF8MB4_UNICODE_CI;

-- =====================================================
-- 12. INTERNSHIP ROADMAPS TABLE
-- =====================================================
CREATE TABLE IF NOT EXISTS internship_roadmaps (
    id INT AUTO_INCREMENT PRIMARY KEY,
    position_id INT NOT NULL,
    stage_name VARCHAR(50) NOT NULL,
    stage_order INT DEFAULT 0,
    description TEXT,
    duration_weeks INT,
    expected_outcomes TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_roadmap_position (position_id),
    INDEX idx_roadmap_stage (stage_name),
    FOREIGN KEY (position_id)
        REFERENCES positions (id)
        ON DELETE CASCADE,
    CONSTRAINT chk_roadmap_stage CHECK (stage_name IN ('ONBOARDING' , 'TRAINING', 'PROJECT', 'EVALUATION'))
)  ENGINE=INNODB DEFAULT CHARSET=UTF8MB4 COLLATE = UTF8MB4_UNICODE_CI;

-- =====================================================
-- 13. INTERN ROADMAP PROGRESS TABLE
-- =====================================================
CREATE TABLE IF NOT EXISTS intern_roadmap_progress (
    id INT AUTO_INCREMENT PRIMARY KEY,
    intern_id INT NOT NULL,
    roadmap_id INT NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    start_date DATE,
    expected_end_date DATE,
    actual_completion_date DATE,
    mentor_notes TEXT,
    INDEX idx_progress_intern (intern_id),
    INDEX idx_progress_roadmap (roadmap_id),
    INDEX idx_progress_status (status),
    UNIQUE KEY uk_progress_intern_roadmap (intern_id , roadmap_id),
    FOREIGN KEY (intern_id)
        REFERENCES interns (id)
        ON DELETE CASCADE,
    FOREIGN KEY (roadmap_id)
        REFERENCES internship_roadmaps (id)
        ON DELETE CASCADE,
    CONSTRAINT chk_progress_status CHECK (status IN ('PENDING' , 'IN_PROGRESS', 'COMPLETED', 'OVERDUE'))
)  ENGINE=INNODB DEFAULT CHARSET=UTF8MB4 COLLATE = UTF8MB4_UNICODE_CI;

-- =====================================================
-- 14. INTERN STATUS HISTORY TABLE
-- =====================================================
CREATE TABLE IF NOT EXISTS intern_status_history (
    id INT AUTO_INCREMENT PRIMARY KEY,
    intern_id INT NOT NULL,
    old_status VARCHAR(20),
    new_status VARCHAR(20) NOT NULL,
    changed_by CHAR(36) NOT NULL,
    reason TEXT,
    changed_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_history_intern (intern_id),
    INDEX idx_history_changed_by (changed_by),
    INDEX idx_history_new_status (new_status),
    FOREIGN KEY (intern_id)
        REFERENCES interns (id)
        ON DELETE CASCADE,
    FOREIGN KEY (changed_by)
        REFERENCES users (id)
        ON DELETE RESTRICT,
    CONSTRAINT chk_history_old_status CHECK (old_status IS NULL
        OR old_status IN ('ACTIVE' , 'WARNING', 'COMPLETED', 'DROPPED')),
    CONSTRAINT chk_history_new_status CHECK (new_status IN ('ACTIVE' , 'WARNING', 'COMPLETED', 'DROPPED'))
)  ENGINE=INNODB DEFAULT CHARSET=UTF8MB4 COLLATE = UTF8MB4_UNICODE_CI;

-- =====================================================
-- 15. BUSINESS METRICS TABLE
-- =====================================================
CREATE TABLE IF NOT EXISTS business_metrics (
    id INT AUTO_INCREMENT PRIMARY KEY,
    intern_id INT NOT NULL,
    metric_name VARCHAR(255) NOT NULL,
    metric_category VARCHAR(100),
    target_value DECIMAL(10 , 2 ),
    actual_value DECIMAL(10 , 2 ),
    unit VARCHAR(50),
    recorded_date DATE NOT NULL,
    notes TEXT,
    created_by CHAR(36),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_metric_intern (intern_id),
    INDEX idx_metric_category (metric_category),
    INDEX idx_metric_date (recorded_date),
    INDEX idx_metric_created_by (created_by),
    FOREIGN KEY (intern_id)
        REFERENCES interns (id)
        ON DELETE CASCADE,
    FOREIGN KEY (created_by)
        REFERENCES users (id)
        ON DELETE SET NULL
)  ENGINE=INNODB DEFAULT CHARSET=UTF8MB4 COLLATE = UTF8MB4_UNICODE_CI;

-- =====================================================
-- 16. SYSTEM ALERTS TABLE
-- =====================================================
CREATE TABLE IF NOT EXISTS system_alerts (
    id INT AUTO_INCREMENT PRIMARY KEY,
    alert_type VARCHAR(50) NOT NULL,
    target_user_id CHAR(36),
    intern_id INT,
    title VARCHAR(255) NOT NULL,
    message TEXT NOT NULL,
    severity VARCHAR(20) DEFAULT 'info',
    is_read BOOLEAN DEFAULT FALSE,
    read_at TIMESTAMP NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_alert_type (alert_type),
    INDEX idx_alert_target_user (target_user_id),
    INDEX idx_alert_intern (intern_id),
    INDEX idx_alert_read (is_read),
    INDEX idx_alert_created (created_at),
    FOREIGN KEY (target_user_id)
        REFERENCES users (id)
        ON DELETE CASCADE,
    FOREIGN KEY (intern_id)
        REFERENCES interns (id)
        ON DELETE CASCADE,
    CONSTRAINT chk_alert_type CHECK (alert_type IN ('INTERN_AT_RISK' , 'MENTOR_OVERLOADED',
        'DEADLINE_APPROACHING',
        'EVALUATION_DUE'))
)  ENGINE=INNODB DEFAULT CHARSET=UTF8MB4 COLLATE = UTF8MB4_UNICODE_CI;

-- =====================================================
-- 17. AUDIT LOGS TABLE
-- =====================================================
CREATE TABLE IF NOT EXISTS audit_logs (
    id INT AUTO_INCREMENT PRIMARY KEY,
    user_id CHAR(36) NOT NULL,
    action VARCHAR(100) NOT NULL,
    entity_type VARCHAR(100),
    entity_id INT,
    old_value TEXT,
    new_value TEXT,
    details TEXT,
    ip_address VARCHAR(45),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_audit_user (user_id),
    INDEX idx_audit_action (action),
    INDEX idx_audit_entity (entity_type , entity_id),
    INDEX idx_audit_created (created_at),
    FOREIGN KEY (user_id)
        REFERENCES users (id)
        ON DELETE RESTRICT
)  ENGINE=INNODB DEFAULT CHARSET=UTF8MB4 COLLATE = UTF8MB4_UNICODE_CI;

-- =====================================================
-- 18. REFRESH TOKENS TABLE
-- =====================================================
CREATE TABLE IF NOT EXISTS refresh_token (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    refresh_token VARCHAR(255) NOT NULL UNIQUE,
    expiry_date TIMESTAMP NOT NULL,
    user_id CHAR(36) NOT NULL,
    INDEX idx_refresh_token (refresh_token),
    INDEX idx_refresh_user (user_id),
    INDEX idx_refresh_expiry (expiry_date),
    FOREIGN KEY (user_id)
        REFERENCES users (id)
        ON DELETE CASCADE
)  ENGINE=INNODB DEFAULT CHARSET=UTF8MB4 COLLATE = UTF8MB4_UNICODE_CI;

-- =====================================================
-- INSERT DEFAULT DATA
-- =====================================================

-- Insert default roles
INSERT INTO roles (role_name) VALUES
                                  ('ADMIN'),
                                  ('MENTOR'),
                                  ('HR');
-- =====================================================
-- UPDATE WEEKLY REPORTS TABLE
-- =====================================================
ALTER TABLE weekly_reports
    ADD COLUMN average_score DECIMAL(4,2) DEFAULT 0 AFTER status;

-- =====================================================
-- CRITERIA SCORE DEFINITIONS TABLE
-- =====================================================
CREATE TABLE IF NOT EXISTS criteria_score_definitions (
    id INT AUTO_INCREMENT PRIMARY KEY,
    criteria_id INT NOT NULL,
    score_label VARCHAR(50),
    description TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (criteria_id)
        REFERENCES evaluation_criteria (id)
        ON DELETE CASCADE,
    INDEX idx_def_criteria (criteria_id)
)  ENGINE=INNODB DEFAULT CHARSET=UTF8MB4 COLLATE = UTF8MB4_UNICODE_CI;

-- =====================================================
-- WEEKLY REPORT DETAILS TABLE
-- =====================================================
CREATE TABLE IF NOT EXISTS weekly_report_details (
    id INT AUTO_INCREMENT PRIMARY KEY,
    weekly_report_id INT NOT NULL,
    criteria_id INT NOT NULL,
    score TINYINT,
    comment TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (weekly_report_id)
        REFERENCES weekly_reports (id)
        ON DELETE CASCADE,
    FOREIGN KEY (criteria_id)
        REFERENCES evaluation_criteria (id)
        ON DELETE RESTRICT,
    UNIQUE KEY uk_report_criteria (weekly_report_id , criteria_id),
    CONSTRAINT chk_detail_score CHECK (score >= 0 AND score <= 10)
)  ENGINE=INNODB DEFAULT CHARSET=UTF8MB4 COLLATE = UTF8MB4_UNICODE_CI;

alter table weekly_reports drop column status;

ALTER TABLE positions DROP FOREIGN KEY positions_ibfk_1;
ALTER TABLE positions DROP INDEX idx_position_department;
ALTER TABLE positions DROP COLUMN department_id;


DROP TABLE IF EXISTS intern_roadmap_progress;

CREATE TABLE IF NOT EXISTS roadmap_nodes(
    id INT AUTO_INCREMENT PRIMARY KEY,
    -- Liên kết với vị trí công việc (VD: Java Backend Intern)
    -- Chỉ cần set ở Node cha cao nhất (Root), các node con có thể null hoặc thừa kế
    position_id INT,
    -- Cấu trúc đệ quy: Node con trỏ về Node cha
    parent_id INT,
    -- Tên bài học / Task
    title VARCHAR(255) NOT NULL,
    -- Phân loại node để AI và Code xử lý logic
    node_type VARCHAR(20) NOT NULL,
    -- Các giá trị: 'PHASE' (Tháng), 'MODULE' (Tuần), 'LESSON' (Bài), 'TASK' (Nhiệm vụ)
    description TEXT,
    -- Điều kiện Pass (Quan trọng cho AI Agent check)
    pass_condition TEXT,
    -- Context cho AI biết mục tiêu đầu ra của node này là gì
    learning_outcome TEXT,
    -- Ước lượng thời gian (giờ)
    estimated_hours DECIMAL(5,2),
    -- Thứ tự sắp xếp (1.1, 1.2...)
    order_index INT DEFAULT 0,
    is_expanded tinyint(1) default '0',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    -- Indexes & FK
    INDEX idx_node_parent (parent_id),
    INDEX idx_node_position (position_id),
    FOREIGN KEY (position_id) REFERENCES positions(id) ON DELETE CASCADE,
    FOREIGN KEY (parent_id) REFERENCES roadmap_nodes(id) ON DELETE CASCADE,
    CONSTRAINT chk_node_type CHECK (node_type IN ('PHASE', 'MODULE', 'LESSON', 'TASK'))
) ENGINE=INNODB DEFAULT CHARSET=UTF8MB4 COLLATE = UTF8MB4_UNICODE_CI;

CREATE TABLE IF NOT EXISTS intern_roadmap_progress (
    id INT AUTO_INCREMENT PRIMARY KEY,
    intern_id INT NOT NULL,
    node_id INT NOT NULL, -- Link tới roadmap_nodes
    -- Trạng thái chi tiết hơn
    status VARCHAR(20) NOT NULL DEFAULT 'LOCKED',
    -- LOCKED: Chưa học tới (do chưa xong bài trước)
    -- OPEN: Đã mở, có thể học
    -- IN_PROGRESS: Đang làm
    -- SUBMITTED: Đã nộp bài (chờ mentor duyệt)
    -- COMPLETED: Đã xong
    submitted_proof TEXT, -- Link Github PR, hoặc câu trả lời
    mentor_feedback TEXT, -- Mentor nhận xét task này
    completed_at DATE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_progress_intern_node (intern_id, node_id),
    INDEX idx_progress_status (status),
    FOREIGN KEY (intern_id) REFERENCES interns(id) ON DELETE CASCADE,
    FOREIGN KEY (node_id) REFERENCES roadmap_nodes(id) ON DELETE CASCADE,
    CONSTRAINT chk_progress_status CHECK (status IN ('LOCKED', 'OPEN', 'IN_PROGRESS', 'SUBMITTED', 'COMPLETED', 'REJECTED'))
) ENGINE=INNODB DEFAULT CHARSET=UTF8MB4 COLLATE = UTF8MB4_UNICODE_CI;

ALTER TABLE roadmap_nodes
ADD COLUMN assessment_method VARCHAR(255) AFTER estimated_hours,
ADD COLUMN difficulty ENUM('BEGINNER','INTERMEDIATE','ADVANCED')
    AFTER assessment_method;
    
ALTER TABLE roadmap_nodes
MODIFY COLUMN node_type 
ENUM('PHASE','MODULE','LESSON','TASK') NOT NULL;

ALTER TABLE roadmap_nodes
ADD INDEX idx_node_difficulty (difficulty),
ADD INDEX idx_node_type (node_type);

ALTER TABLE intern_roadmap_progress
MODIFY COLUMN status
ENUM(
    'LOCKED',
    'OPEN',
    'IN_PROGRESS',
    'SUBMITTED',
    'COMPLETED',
    'REJECTED'
) NOT NULL DEFAULT 'LOCKED';

CREATE TABLE IF NOT EXISTS tags (
    id INT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL UNIQUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
)
ENGINE=InnoDB
DEFAULT CHARSET=utf8mb4
COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS roadmap_node_tags (
    node_id INT NOT NULL,
    tag_id INT NOT NULL,

    PRIMARY KEY (node_id, tag_id),

    CONSTRAINT fk_rnt_node
        FOREIGN KEY (node_id)
        REFERENCES roadmap_nodes(id)
        ON DELETE CASCADE,

    CONSTRAINT fk_rnt_tag
        FOREIGN KEY (tag_id)
        REFERENCES tags(id)
        ON DELETE CASCADE
)
ENGINE=InnoDB
DEFAULT CHARSET=utf8mb4
COLLATE=utf8mb4_unicode_ci;

UPDATE roadmap_nodes
SET difficulty = 'BEGINNER'
WHERE difficulty IS NULL;
UPDATE roadmap_nodes
SET assessment_method = 'Completed roadmap and Admin approved.'
WHERE assessment_method IS NULL;


ALTER TABLE roadmap_nodes
ADD COLUMN batch_id INT;

-- 3. Tạo khóa ngoại liên kết roadmap với batch
ALTER TABLE roadmap_nodes
ADD CONSTRAINT fk_roadmap_batch
FOREIGN KEY (batch_id) REFERENCES internship_batches(id);

UPDATE internship_batches
SET status = 'ONGOING'
WHERE status = 'DRAFT';


ALTER TABLE internship_batches
    MODIFY COLUMN status
    ENUM('ONGOING', 'CANCEL', 'COMPLETED')
    DEFAULT 'ONGOING';
    

DELIMITER //
CREATE PROCEDURE GenerateWeeklyReports(
    IN p_intern_id INT,
    IN p_mentor_id CHAR(36)
)
BEGIN
    -- Khai báo biến
    DECLARE v_current_date DATE DEFAULT '2025-11-01';
    DECLARE v_week_number INT DEFAULT 1;
    DECLARE v_random_score DECIMAL(3, 1);
    
    -- Vòng lặp: Chạy khi ngày hiện tại trong vòng lặp vẫn nhỏ hơn hoặc bằng ngày hôm nay
    WHILE v_current_date <= CURDATE() DO
        
        -- Tạo điểm random từ 7.0 đến 10.0
        -- RAND() trả về 0 -> 1. Nhân với 3 sẽ được 0 -> 3. Cộng 7 sẽ được 7 -> 10.
        SET v_random_score = ROUND(7 + (RAND() * 3), 1);
        
        -- Nếu random ra > 10 (ví dụ 10.0001 làm tròn) thì set cứng về 10
        IF v_random_score > 10 THEN
            SET v_random_score = 10.0;
        END IF;

        -- Insert dữ liệu
        -- Sử dụng INSERT IGNORE để tránh lỗi nếu chạy lại (do trùng key unique intern_id + week_start_date)
        INSERT IGNORE INTO weekly_reports (
            intern_id, 
            mentor_id, 
            week_number, 
            week_start_date, 
            average_score,
            tasks_assigned, -- Điền mẫu để không bị null xấu data
            tasks_completed,
            issues_risks,
            mentor_overall_comment
        ) VALUES (
            p_intern_id,
            p_mentor_id,
            v_week_number,
            v_current_date,
            v_random_score,
            'Auto generated task',
            'Auto generated completion',
            'None',
            'Good performance'
        );

        -- Tăng ngày lên 7 và tăng số tuần lên 1
        SET v_current_date = DATE_ADD(v_current_date, INTERVAL 7 DAY);
        SET v_week_number = v_week_number + 1;
        
    END WHILE;
END //

DELIMITER ;

-- Ví dụ: intern_id = 1, mentor_id = 'user-uuid-123'
CALL GenerateWeeklyReports(46, '2c638c0d-f4bc-4a53-b88d-1a75e9620830');