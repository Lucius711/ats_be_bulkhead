# Giao tiếp của application-service với các service khác

## 1. Phạm vi

`application-service` chỉ lưu các ID liên kết như `candidateId` và `jobId`, không lưu toàn bộ dữ liệu của service khác và không truy cập trực tiếp vào database của service khác.

Khi cần kiểm tra hoặc hiển thị thông tin candidate hoặc job, `application-service` gọi API qua HTTP, thường được khai báo bằng OpenFeign.

`candidate-service` chịu trách nhiệm tạo và quản lý candidate. `job-service` chịu trách nhiệm tạo và quản lý job. Vì vậy, `application-service` chỉ gọi API đọc dữ liệu cần thiết cho nghiệp vụ application.

Trong luồng tạo application, thứ tự xử lý nên là:

1. Kiểm tra application trùng trong database của application-service.
2. Gọi candidate-service để kiểm tra candidate.
3. Gọi job-service để kiểm tra job.
4. Chỉ lưu application khi candidate và job đều hợp lệ.

## 2. Giao tiếp với candidate-service

### UC-01. Kiểm tra candidate trước khi tạo application

Đây là use case bắt buộc trong luồng `POST /api/v1/applications`.

#### Mục đích

- Xác nhận `candidateId` được gửi lên tồn tại.
- Kiểm tra candidate có trạng thái `ACTIVE` hay không.
- Kiểm tra candidate có bị đánh dấu trùng hay không nếu nghiệp vụ không cho phép ứng tuyển.
- Chỉ tạo application sau khi candidate hợp lệ.

#### Method và endpoint

```http
GET /api/v1/candidates/{candidateId}
```

#### Input

| Thành phần | Kiểu | Bắt buộc | Mô tả |
|---|---|---:|---|
| `candidateId` | `UUID` | Có | ID candidate lấy từ `ApplicationRequest.candidateId` |

Request không có body.

Ví dụ:

```http
GET /api/v1/candidates/7f3c2f47-6a0c-4d4a-9c3c-2f4e2a1b8d10
```

#### Output thành công

HTTP `200 OK`:

```json
{
  "id": "7f3c2f47-6a0c-4d4a-9c3c-2f4e2a1b8d10",
  "fullName": "Nguyen Van A",
  "email": "nguyenvana@example.com",
  "phone": "0901234567",
  "status": "ACTIVE",
  "isDuplicate": false
}
```

`application-service` sử dụng tối thiểu các trường sau:

| Trường | Cách sử dụng |
|---|---|
| `id` | Đối chiếu với `candidateId` trong request |
| `status` | Chỉ tiếp tục tạo application khi giá trị là `ACTIVE` |
| `isDuplicate` | Từ chối nếu nghiệp vụ không cho phép candidate trùng |

Sau khi kiểm tra candidate hợp lệ, `application-service` tiếp tục kiểm tra job, hạn ứng tuyển và lưu application.

#### Các trường hợp lỗi

| HTTP status | Trường hợp | Xử lý tại application-service |
|---|---|---|
| `404 Not Found` | Không tìm thấy candidate với ID đã gửi | Không tạo application, trả lỗi candidate không tồn tại |
| `200 OK`, `status = INACTIVE` | Candidate không còn hoạt động | Không tạo application |
| `200 OK`, `isDuplicate = true` | Candidate bị đánh dấu trùng | Từ chối theo nghiệp vụ |
| Không nhận được phản hồi | candidate-service dừng hoặc lỗi mạng | Ghi log và trả lỗi giao tiếp service |

### UC-02. Hiển thị thông tin candidate cùng application

Use case này áp dụng khi application-service có API xem chi tiết hoặc danh sách application và cần trả thêm thông tin candidate cho recruiter.

#### Mục đích

- Hiển thị tên, email, số điện thoại và trạng thái candidate.
- Không sao chép dữ liệu candidate vào database của application-service.
- Lấy dữ liệu mới nhất từ candidate-service.

#### Method và input

```http
GET /api/v1/applications/{applicationId}
```

`applicationId` là ID của application cần xem. `application-service` đọc `candidateId` trong application rồi gọi nội bộ:

```http
GET /api/v1/candidates/{candidateId}
```

Request không có body.

#### Output

HTTP `200 OK` trả về `ApplicationResponse`, trong đó có thông tin candidate mới nhất:

```json
{
  "id": "application-uuid",
  "candidateId": "7f3c2f47-6a0c-4d4a-9c3c-2f4e2a1b8d10",
  "candidate": {
    "id": "7f3c2f47-6a0c-4d4a-9c3c-2f4e2a1b8d10",
    "fullName": "Nguyen Van A",
    "email": "nguyenvana@example.com",
    "phone": "0901234567",
    "status": "ACTIVE",
    "isDuplicate": false
  },
  "status": "SUBMITTED"
}
```

Nếu candidate không tồn tại hoặc candidate-service không khả dụng, application-service trả lỗi và không tự tạo lại candidate.

### UC-03. Kiểm tra candidate trước các thao tác nghiệp vụ khác

Khi bổ sung các thao tác như chuyển application sang pipeline stage khác, gửi thông báo hoặc xử lý hồ sơ, application-service có thể cần xác nhận candidate vẫn tồn tại và còn hợp lệ.

#### Method và input

```http
GET /api/v1/candidates/{candidateId}
```

`candidateId` lấy từ bản ghi application. Request không có body.

#### Output và cách xử lý

- `200 OK`: đọc `status` và `isDuplicate` để quyết định có tiếp tục nghiệp vụ hay không.
- `404 Not Found`: dừng thao tác và thông báo candidate không tồn tại.
- Lỗi kết nối hoặc lỗi `5xx`: không tự ý thay đổi trạng thái application khi chưa xác nhận được candidate.

Đây là cùng một API kiểm tra candidate, không tạo thêm endpoint riêng cho từng thao tác.

## 3. API candidate-service không thuộc trách nhiệm của application-service

### Tạo candidate

```http
POST /api/v1/candidates
```

API này phục vụ việc đăng ký hoặc tạo hồ sơ candidate. Client chính là frontend, auth-service hoặc một service phụ trách onboarding, không phải application-service.

Input hiện có của `CandidateRequest`:

```json
{
  "fullName": "Nguyen Van A",
  "email": "nguyenvana@example.com",
  "phone": "0901234567",
  "source": "WEBSITE",
  "utmSource": "google",
  "utmMedium": "cpc",
  "utmCampaign": "java-developer",
  "skillIds": [
    "1f3c2f47-6a0c-4d4a-9c3c-2f4e2a1b8d10"
  ]
}
```

Application-service chỉ nhận `candidateId` đã có trong `ApplicationRequest`, không tạo candidate thay cho candidate-service.

## 4. Contract CandidateClient

Trong application-service đã tạo client như sau:

```java
@FeignClient(name = "candidate-service", url = "${candidate-service.url}",
        configuration = CandidateFeignConfig.class)
public interface CandidateClient {

    @GetMapping("/api/v1/candidates/{candidateId}")
    CandidateResponse findById(@PathVariable UUID candidateId);
}
```

Cấu hình URL:

```yaml
candidate-service:
  url: http://localhost:8082
```

## 5. Giao tiếp với job-service

### 5.1. API sử dụng

```http
GET /api/v1/jobs/{jobId}
```

| Thành phần | Kiểu | Bắt buộc | Mô tả |
|---|---|---:|---|
| `jobId` | `UUID` | Có | ID job cần kiểm tra |

Request không có body.

Ví dụ:

```http
GET /api/v1/jobs/f47ac10b-58cc-4372-a567-0e02b2c3d479
```

Response thành công `200 OK` theo `JobApplicationResponse` của job-service và `JobView` của application-service:

```json
{
  "jobId": "f47ac10b-58cc-4372-a567-0e02b2c3d479",
  "title": "Fullstack Java Developer",
  "status": "PUBLISHED",
  "applicationDeadline": "2026-12-31"
}
```

Các trường application-service cần đọc:

| Trường | Kiểu | Cách sử dụng |
|---|---|---|
| `jobId` | `UUID` | Đối chiếu với `jobId` trong request |
| `title` | `String` | Ghi log hoặc hiển thị nếu cần |
| `status` | `String` | Kiểm tra job có cho phép nhận application hay không |
| `applicationDeadline` | `LocalDate` | Không cho ứng tuyển khi đã quá hạn |

### 5.2. Các use case cần giao tiếp

#### UC-J01. Kiểm tra job trước khi tạo application

Đây là use case bắt buộc khi application-service nhận `POST /api/v1/applications`.

- Input: `jobId` lấy từ `ApplicationRequest.jobId`.
- Method: `GET /api/v1/jobs/{jobId}`.
- Output: `jobId`, `status` và `applicationDeadline`.
- Thành công: job tồn tại, đang mở nhận hồ sơ và chưa quá hạn.
- Thất bại: không tạo application nếu job không tồn tại, đã đóng hoặc đã quá hạn.

Luồng kiểm tra:

1. Gọi job-service bằng `jobId`.
2. Kiểm tra `status` theo trạng thái được thống nhất trong job-service.
3. Kiểm tra `applicationDeadline` không trước ngày hiện tại.
4. Lưu application nếu tất cả điều kiện đều hợp lệ.

#### UC-J02. Hiển thị thông tin job cùng application

Use case này áp dụng khi application-service có API xem chi tiết hoặc danh sách application và cần hiển thị thông tin job cho recruiter hoặc candidate.

- Input: `jobId` được lưu trong application.
- Method: `GET /api/v1/jobs/{jobId}`.
- Output: `title`, `status` và `applicationDeadline`; có thể mở rộng thêm các trường trong `JobResponse` khi cần.
- Lưu ý: không lưu bản sao toàn bộ job vào database của application-service.

#### UC-J03. Kiểm tra job trước các thao tác application khác

Khi chuyển pipeline stage, xử lý hồ sơ hoặc gửi thông báo, application-service có thể cần xác nhận job vẫn tồn tại và còn phù hợp với thao tác đang thực hiện.

- Input: `jobId` từ application.
- Method: `GET /api/v1/jobs/{jobId}`.
- Output: `status` và `applicationDeadline`.
- Nếu job đã `CLOSED` hoặc service không phản hồi, cần dừng thao tác hoặc xử lý theo chính sách lỗi đã thống nhất.

### 5.3. Các API không nên gọi từ application-service

Các API sau thuộc trách nhiệm quản lý job của recruiter hoặc job-service:

```http
POST   /api/v1/jobs
PATCH  /api/v1/jobs/{jobId}
DELETE /api/v1/jobs/{jobId}
GET    /api/v1/jobs
```

application-service không tạo, cập nhật, đóng hoặc tìm kiếm danh sách job thay cho job-service. Với luồng tạo application, application-service chỉ cần gọi API lấy job theo ID.

### 5.4. Contract JobClient

`JobClient` hiện đã có trong application-service:

```java
@FeignClient(name = "job-service", url = "${job-service.url}")
public interface JobClient {

    @GetMapping("/api/v1/jobs/{jobId}")
    JobView findById(@PathVariable UUID jobId);
}
```

Cấu hình URL:

```yaml
job-service:
  url: http://localhost:8081
```

## 6. Luồng tạo application hoàn chỉnh

Khi nhận request:

```json
{
  "candidateId": "7f3c2f47-6a0c-4d4a-9c3c-2f4e2a1b8d10",
  "jobId": "f47ac10b-58cc-4372-a567-0e02b2c3d479",
  "cvUrl": "https://example.com/cv/nguyen-van-a.pdf"
}
```

application-service nên xử lý như sau:

1. Kiểm tra đã tồn tại application có cùng `candidateId` và `jobId` chưa.
2. Gọi `GET /api/v1/candidates/{candidateId}`.
3. Gọi `GET /api/v1/jobs/{jobId}`.
4. Kiểm tra candidate đang `ACTIVE`.
5. Kiểm tra job đang nhận hồ sơ và chưa quá `applicationDeadline`.
6. Lưu application với `candidateId` và `jobId`.

## 7. Mã lỗi cần xử lý

| HTTP status | Trường hợp | Xử lý tại application-service |
|---|---|---|
| `200 OK` | Lấy candidate hoặc job thành công | Tiếp tục kiểm tra nghiệp vụ |
| `404 Not Found` | Không tìm thấy candidate hoặc job | Không tạo hoặc không tiếp tục xử lý application |
| `200 OK` với candidate `INACTIVE` | Candidate không hoạt động | Từ chối application |
| `200 OK` với job đã đóng | Job không còn nhận hồ sơ | Từ chối application |
| `200 OK` với deadline đã qua | Job hết hạn nhận hồ sơ | Từ chối application |
| `5xx` hoặc lỗi kết nối | Service phụ thuộc không khả dụng | Ghi log, không lưu dữ liệu dở dang |

## 8. Lưu ý về mã nguồn hiện tại

- `ApplicationServiceImpl` đã gọi `JobClient.findById(jobId)` và kiểm tra `applicationDeadline`.
- `ApplicationServiceImpl` gọi `CandidateClient` để kiểm tra candidate trước khi tạo application.
- `GET /api/v1/applications/{applicationId}` trả application cùng thông tin candidate lấy mới nhất từ `candidate-service`.
- `validateCandidateBeforeOperation` là hàm dùng chung cho các nghiệp vụ application cần kiểm tra candidate trước khi xử lý.
- `JobController` hiện trả về `JobApplicationResponse` gồm `jobId`, `title`, `status` và `applicationDeadline`, phù hợp với `JobView` của application-service.
- Trạng thái job trong mã nguồn hiện tại là `DRAFT`, `PUBLISHED`, `CLOSED`. Cần thống nhất cách gọi trạng thái này trong toàn bộ tài liệu và API.
