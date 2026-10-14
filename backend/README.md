# Rikai Intern Management - Backend Service

Hệ thống backend quản lý thực tập sinh (Intern Management System) được phát triển bằng **Spring Boot 3 (Java 21)**, tích hợp cơ sở dữ liệu **MySQL**, cơ chế bảo mật **JWT / Spring Security** và phân hệ **AI Agent** tự động xây dựng lộ trình học tập sử dụng **Spring AI (Google Gemini)**.

---

## 1. Công nghệ chính (Tech Stack)

| Thành phần | Công nghệ / Thư viện | Phiên bản |
| :--- | :--- | :--- |
| **Ngôn ngữ** | Java | 21 (LTS) |
| **Framework** | Spring Boot | 3.3.5 |
| **Build Tool** | Gradle | Wrapper đi kèm |
| **Bảo mật** | Spring Security, OAuth2 Resource Server, Nimbus Jose JWT | Tích hợp Spring Boot |
| **Cơ sở dữ liệu** | MySQL 8.x, Spring Data JPA, Hibernate | Driver: `mysql-connector-j` |
| **AI Integration** | Spring AI (`spring-ai-starter-model-openai`) | 1.1.2 |
| **AI Model** | Google Gemini (thông qua OpenAI Compatible Endpoint) | `gemini-2.5-pro`, `gemini-2.5-flash` |
| **Mapping & Tiện ích** | MapStruct, Lombok, `spring-dotenv` | MapStruct 1.5.5.Final |
| **Tài liệu API** | SpringDoc OpenAPI / Swagger UI | 2.6.0 |

---

## 2. Cấu trúc thư mục dự án

```text
backend/
├── ddl/                                # Các script DDL định nghĩa database
├── gradle/                             # Gradle Wrapper
├── src/
│   ├── main/
│   │   ├── java/com/rikai/backend/
│   │   │   ├── ai/                     # Phân hệ AI Agent (Spring AI & Gemini)
│   │   │   │   ├── agent/              # Các tác tử AI xử lý chuyên biệt (Router, Editor)
│   │   │   │   ├── config/             # Cấu hình ChatClient, Model parameters
│   │   │   │   ├── dto/                # Request/Response DTO cho module AI
│   │   │   │   │   ├── request/        # AddNodeRequest, UpdateNodeRequest, ...
│   │   │   │   │   └── response/       # ChatResponseDto, IntentDto, PositionSelectionDto, ...
│   │   │   │   ├── prompt/             # Quản lý System Prompts và Template
│   │   │   │   ├── service/roadmap/    # Quản lý bản nháp lộ trình (DraftRoadmapManager)
│   │   │   │   ├── tool/               # Function Calling Tools cho Agent (RoadmapEditingTools)
│   │   │   │   └── util/               # Validation kết quả xử lý
│   │   │   ├── common/                 # Các tiện ích chung, chuẩn hóa API
│   │   │   │   ├── ApiResponse.java    # Cấu trúc Response chuẩn { code, message, result }
│   │   │   │   ├── PageResponse.java   # Phân trang chuẩn
│   │   │   │   ├── ErrorCode.java      # Danh mục mã lỗi và HTTP Status
│   │   │   │   └── InternStatus.java   # Trạng thái thực tập sinh
│   │   │   ├── config/                 # Cấu hình ứng dụng (CORS, Async, OpenAPI, Init Data)
│   │   │   ├── controller/             # Tầng REST API Endpoints
│   │   │   ├── dto/                    # Data Transfer Objects
│   │   │   │   ├── request/            # DTO đầu vào theo từng Domain
│   │   │   │   └── response/           # DTO trả về theo từng Domain
│   │   │   ├── event/                  # Event-driven listeners và publishers
│   │   │   ├── exception/              # Global Exception Handling & Custom Exceptions
│   │   │   ├── mapper/                 # MapStruct mappers (chuyển đổi Entity <-> DTO)
│   │   │   ├── model/                  # Các JPA Entity ánh xạ bảng MySQL
│   │   │   │   └── Enum/               # Enums: Role, Status, Grade,...
│   │   │   ├── repository/             # Spring Data JPA Repositories
│   │   │   ├── security/               # Cấu hình Security, JWT Decoder & EntryPoint
│   │   │   ├── service/                # Business Logic Layer (chia theo từng domain)
│   │   │   └── validation/             # Custom Annotations & Validators
│   │   └── resources/
│   │       └── application.yml         # File cấu hình chính (Datasource, JWT, AI, JPA)
│   └── test/                           # Unit tests và Integration tests
├── .env.example                        # File mẫu khai báo biến môi trường
├── build.gradle                        # Định nghĩa thư viện và cấu hình build
├── db.sql                              # Toàn bộ database schema và dữ liệu mẫu
├── gradlew & gradlew.bat               # Trình chạy Gradle
└── settings.gradle                     # Tên project
```

---

## 3. Kiến trúc hệ thống (Layered Architecture)

Hệ thống được thiết kế theo mô hình phân tầng chặt chẽ:

```
[ Client: Frontend Vue 3 / Swagger / Postman ]
                     │
                     ▼
       ┌───────────────────────────┐
       │     Security Filter       │  (JWT Authentication, CORS, Role-based Access)
       └─────────────┬─────────────┘
                     │
                     ▼
       ┌───────────────────────────┐
       │     Controller Layer      │  (Validate DTO, định tuyến REST endpoints)
       └─────────────┬─────────────┘
                     │
                     ▼
       ┌───────────────────────────┐      ┌─────────────────────────┐
       │       Service Layer       │ ◄──► │     AI Agent Module     │
       │   (Business Logic Core)   │      │ (Gemini + Tool Calling) │
       └─────────────┬─────────────┘      └─────────────────────────┘
                     │
                     ▼
       ┌───────────────────────────┐
       │     Repository Layer      │  (Spring Data JPA)
       └─────────────┬─────────────┘
                     │
                     ▼
       ┌───────────────────────────┐
       │     Database (MySQL)      │
       └───────────────────────────┘
```

### 3.1. Controller Layer
Xử lý các HTTP request và trả về dữ liệu được bọc trong `ApiResponse<T>`:

| Controller | Tiền tố API | Chức năng chính |
| :--- | :--- | :--- |
| `AuthController` | `/auth` | Đăng nhập, làm mới Token (`/refresh`), xác thực Token (`/introspect`), Logout |
| `UserController` | `/users` | Quản lý tài khoản (Admin, Mentor), cấp quyền, đổi thông tin |
| `InternController` | `/interns` | CRUD thực tập sinh, phân công mentor, đổi trạng thái, quản lý profile |
| `InternshipBatchController` | `/batches` | Quản lý các đợt/kỳ thực tập sinh |
| `DepartmentController` | `/departments` | Quản lý danh mục phòng ban |
| `PositionController` | `/positions` | Quản lý các vị trí thực tập (Frontend, Backend, Tester, ...) |
| `WeeklyReportController` | `/weekly-reports` | Nộp báo cáo tuần, mentor chấm điểm và phản hồi |
| `EvaluationCriteriaController` | `/evaluation-criteria` | Quản lý tiêu chí đánh giá |
| `CriteriaGroupController` | `/criteria-groups` | Quản lý nhóm tiêu chí đánh giá |
| `CriteriaScoreDefinitionController` | `/criteria-score-definitions` | Quản lý thang điểm và mô tả tiêu chí |
| `EvaluationSessionController` | `/evaluation-sessions` | Tạo và điều phối các phiên đánh giá định kỳ |
| `RoadmapController` | `/roadmaps` | Quản lý lộ trình học tập, tích hợp AI tự động sinh & sửa lộ trình |
| `DashboardController` | `/dashboard` | Thống kê KPI, tổng số intern, mentor, tỷ lệ hoàn thành |

### 3.2. Service Layer
Chia theo module độc lập trong package `com.rikai.backend.service.*`:
- `auth`: Xử lý sinh và thẩm định JWT Token.
- `intern`: Xử lý vòng đời thực tập sinh (ứng tuyển -> đang thực tập -> hoàn thành / dừng).
- `user`: Xử lý thông tin người dùng và phân quyền.
- `weeklyreport`: Xử lý luồng nộp và duyệt báo cáo tuần.
- `evaluationsession`: Tổng hợp điểm số và đánh giá performance.
- `criteriagroup`, `evaluationcriteria`, `department`, `position`, `internship_batch`, `dashboard`.

### 3.3. Module AI Agent (`com.rikai.backend.ai`)
Điểm nhấn của hệ thống là khả năng tương tác ngôn ngữ tự nhiên để tạo và chỉnh sửa lộ trình học tập:
- **`IntentRouterAgent`**: Dùng model `gemini-2.5-pro` để phân tích ý định người dùng (ví dụ: tạo mới lộ trình cho Java Backend, thêm node "Spring Security", hoặc xóa node).
- **`RoadmapEditorAgent`**: Dùng model `gemini-2.5-flash` kết hợp **Function Calling** (`RoadmapEditingTools`) để tự động thực thi các thao tác `add_node`, `update_node`, `remove_node` trên lộ trình.
- **`DraftRoadmapManager`**: Lưu trữ và quản lý bản nháp lộ trình trong phiên làm việc của người dùng trước khi lưu xuống cơ sở dữ liệu.

### 3.4. Security Layer (`com.rikai.backend.security`)
- Sử dụng **OAuth2 Resource Server** với JWT.
- Token được trích xuất từ **Authorization Header** (`Bearer <token>`) hoặc **Cookie** (`accessToken`).
- Các endpoint công khai (Public): `/swagger-ui/**`, `/v3/**`, `/auth/**`, `/actuator/**`.
- Phân quyền theo vai trò (Role-based): Hỗ trợ `@PreAuthorize("hasRole('ADMIN')")` hoặc `@PreAuthorize("hasRole('MENTOR')")`.

---

## 4. Cấu hình môi trường (.env & application.yml)

Cấu hình chính nằm tại `src/main/resources/application.yml`. Các giá trị bí mật được đọc thông qua biến môi trường hoặc file `.env`:

```env
# Cấu hình cơ sở dữ liệu MySQL
URL_DATABASE=jdbc:mysql://localhost:3306/intern_system?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true
DB_USERNAME=root
DB_PASSWORD=your_password

# Bảo mật JWT
SIGNER_KEY=your_jwt_secret_key_minimum_32_characters_long

# Tích hợp AI (Google Gemini)
GEMINI_API_KEY=your_google_gemini_api_key
```

Cổng chạy mặc định của dịch vụ: `8081`.

---

## 5. Hướng dẫn khởi chạy dự án

### 5.1. Yêu cầu tiên quyết
- **Java Development Kit (JDK):** Phiên bản 21 trở lên.
- **MySQL Database:** Phiên bản 8.0 trở lên.

### 5.2. Các bước khởi chạy
1. **Khởi tạo cơ sở dữ liệu:**
   - Tạo database `intern_system` trên MySQL.
   - Chạy script [db.sql](file:///D:/Rikai/backend/db.sql) để tạo toàn bộ bảng và dữ liệu khởi tạo.

2. **Cấu hình môi trường:**
   - Sao chép file `.env.example` thành `.env`:
     ```bash
     cp .env.example .env
     ```
   - Cập nhật thông tin kết nối Database, `SIGNER_KEY` và `GEMINI_API_KEY`.

3. **Chạy ứng dụng bằng Gradle Wrapper:**
   - **Windows:**
     ```cmd
     gradlew.bat bootRun
     ```
   - **Linux / macOS:**
     ```bash
     ./gradlew bootRun
     ```

4. **Truy cập tài liệu API (Swagger UI):**
   - Mở trình duyệt tại: `http://localhost:8081/swagger-ui/index.html`
