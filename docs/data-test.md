CANDIDATE_ID="ID_CANDIDATE_ACTIVE"

for i in $(seq 1 10); do
JOB_ID=$(uuidgen | tr '[:upper:]' '[:lower:]')

curl -sS -o /dev/null \
-w "req=$i status=%{http_code} time=%{time_total}s\n" \
-X POST "http://localhost:8080/api/v1/applications" \
-H "Content-Type: application/json" \
-H "X-Correlation-Id: lb-test-$i" \
-d "{\"candidateId\":\"$CANDIDATE_ID\",\"jobId\":\"$JOB_ID\",\"cvUrl\":\"https://example.com/cv/$i.pdf\"}" &
done
wait