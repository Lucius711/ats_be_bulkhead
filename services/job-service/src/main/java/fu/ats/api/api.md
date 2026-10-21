# Job Service API

## 1. Quy ước chung

Base URL:

```text
/api/v1
```

Các API tạo, cập nhật và đóng job yêu cầu recruiter đăng nhập. `recruiterId` được lấy từ claim `sub` của JWT/Keycloak, không nhận từ request body.

### 1.1 Kiểu dữ liệu

| Field | Kiểu | Ghi chú |
|---|---|---|
| `jobId`, `companyId`, `skillIds` | `UUID` | ID của job, công ty và kỹ năng |
| `salaryMin`, `salaryMax` | `number` | Mức lương, dùng cùng đơn vị với `currency` |
| `applicationDeadline` | `date` | Định dạng `yyyy-MM-dd` |
| `createdAt`, `updatedAt` | `date-time` | Định dạng ISO-8601 |

Các giá trị đề xuất:

- `status`: `DRAFT`, `OPEN`, `CLOSED`, `EXPIRED`
- `employmentType`: `FULL_TIME`, `PART_TIME`, `CONTRACT`, `INTERNSHIP`
- `workMode`: `ONSITE`, `HYBRID`, `REMOTE`

## 2. Job APIs

### 2.1 Thêm mới job

| Method | Endpoint |
|---|---|
| `POST` | `/api/v1/jobs` |

Request body tương ứng với `CreateJobRequest`:

```json
{
  "title": "Java Backend Developer",
  "description": "Develop and maintain REST APIs with Spring Boot.",
  "companyId": "b7b3f7c5-3b5d-4c19-9f9f-1e3d7a8a2b10",
  "location": "Ho Chi Minh City",
  "employmentType": "FULL_TIME",
  "workMode": "HYBRID",
  "salaryMin": 1800,
  "salaryMax": 2500,
  "currency": "USD",
  "applicationDeadline": "2026-10-31",
  "skillIds": [
    "0f8fad5b-d9cb-469f-a165-70867728950e"
  ]
}
```

Response `201 Created` (`JobResponse`):

```json
{
  "jobId": "f47ac10b-58cc-4372-a567-0e02b2c3d479",
  "title": "Java Backend Developer",
  "description": "Develop and maintain REST APIs with Spring Boot.",
  "companyId": "b7b3f7c5-3b5d-4c19-9f9f-1e3d7a8a2b10",
  "recruiterId": "d290f1ee-6c54-4b01-90e6-d701748f0851",
  "location": "Ho Chi Minh City",
  "employmentType": "FULL_TIME",
  "workMode": "HYBRID",
  "salaryMin": 1800,
  "salaryMax": 2500,
  "currency": "USD",
  "status": "DRAFT",
  "applicationDeadline": "2026-10-31",
  "skillIds": [
    "0f8fad5b-d9cb-469f-a165-70867728950e"
  ],
  "createdAt": "2026-09-25T10:30:00Z",
  "updatedAt": "2026-09-25T10:30:00Z"
}
```

Job mới được tạo ở trạng thái `DRAFT`. Chỉ recruiter sở hữu job hoặc admin mới được cập nhật, mở hoặc đóng job.

### 2.2 Tìm job theo ID

| Method | Endpoint |
|---|---|
| `GET` | `/api/v1/jobs/{jobId}` |

Path parameter:

| Parameter | Kiểu | Bắt buộc | Mô tả |
|---|---|---|---|
| `jobId` | `UUID` | Có | ID của job cần tìm |

Response `200 OK`: trả về `JobResponse`.

Job không tồn tại trả về `404 Not Found` với mã lỗi `JOB_NOT_FOUND`.

### 2.3 Tìm kiếm và phân trang danh sách job

| Method | Endpoint |
|---|---|
| `GET` | `/api/v1/jobs` |

Query parameters tương ứng với `JobSearchRequest`:

| Parameter | Kiểu | Mặc định | Mô tả |
|---|---|---|---|
| `keyword` | `string` | — | Tìm trong tiêu đề và mô tả |
| `location` | `string` | — | Lọc theo địa điểm |
| `employmentType` | `string` | — | Lọc theo loại hình công việc |
| `workMode` | `string` | — | Lọc theo hình thức làm việc |
| `status` | `string` | `OPEN` | Trạng thái job |
| `skillId` | `UUID` | — | Lọc theo kỹ năng |
| `page` | `integer` | `0` | Số trang, bắt đầu từ `0` |
| `size` | `integer` | `20` | Số phần tử mỗi trang, tối đa `100` |
| `sort` | `string` | `createdAt,desc` | Field và hướng sắp xếp |

Response `200 OK`:

```json
{
  "content": [
    {
      "jobId": "f47ac10b-58cc-4372-a567-0e02b2c3d479",
      "title": "Java Backend Developer",
      "companyId": "b7b3f7c5-3b5d-4c19-9f9f-1e3d7a8a2b10",
      "location": "Ho Chi Minh City",
      "employmentType": "FULL_TIME",
      "workMode": "HYBRID",
      "salaryMin": 1800,
      "salaryMax": 2500,
      "currency": "USD",
      "status": "OPEN",
      "applicationDeadline": "2026-10-31",
      "createdAt": "2026-09-25T10:30:00Z"
    }
  ],
  "page": 0,
  "size": 20,
  "totalElements": 1,
  "totalPages": 1
}
```

Danh sách có thể dùng `JobResponse` cho phần tử `content`; `description`, `recruiterId`, `skillIds` có thể được bỏ qua khi triển khai projection tối ưu cho màn hình danh sách.

### 2.4 Cập nhật thông tin job

| Method | Endpoint |
|---|---|
| `PATCH` | `/api/v1/jobs/{jobId}` |

Request body tương ứng với `UpdateJobRequest`. Chỉ gửi những field cần thay đổi:

```json
{
  "title": "Senior Java Backend Developer",
  "salaryMin": 2200,
  "salaryMax": 3000,
  "applicationDeadline": "2026-11-15"
}
```

Response `200 OK`: trả về job đã cập nhật dưới dạng `JobResponse`.

Không cho phép thay đổi `jobId`, `recruiterId`, `createdAt` hoặc tự ý chuyển job sang trạng thái không hợp lệ.

### 2.5 Đóng job

| Method | Endpoint |
|---|---|
| `DELETE` | `/api/v1/jobs/{jobId}` |

API sử dụng soft delete hoặc chuyển trạng thái sang `CLOSED`, không xóa vật lý để giữ lịch sử ứng tuyển.

Response `204 No Content`.

## 3. Quy ước response và lỗi

Response lỗi thống nhất:

```json
{
  "code": "JOB_NOT_FOUND",
  "message": "Job does not exist",
  "timestamp": "2026-09-25T10:30:00Z",
  "path": "/api/v1/jobs/f47ac10b-58cc-4372-a567-0e02b2c3d479"
}
```

| Status | Trường hợp |
|---|---|
| `200 OK` | Tìm kiếm hoặc cập nhật job thành công |
| `201 Created` | Thêm job thành công |
| `204 No Content` | Đóng job thành công |
| `400 Bad Request` | Dữ liệu request không hợp lệ |
| `401 Unauthorized` | Chưa xác thực |
| `403 Forbidden` | Không có quyền thao tác job |
| `404 Not Found` | Không tìm thấy job hoặc company |
| `409 Conflict` | Job đang ở trạng thái không cho phép thao tác |

Các DTO chính của API nằm trong package `fu.ats.dto`:

- `CreateJobRequest`: request cho API thêm job.
- `UpdateJobRequest`: request cho API cập nhật một phần job.
- `JobResponse`: response chi tiết của job, dùng cho API tìm theo ID.
- `JobSearchRequest`: query parameters cho API tìm kiếm và phân trang.
