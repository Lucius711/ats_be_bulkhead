import java.io.RandomAccessFile;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/** Gui N request CUNG LUC qua gateway, dong thoi in log that cua cv-parse-service. Chay: java demo/BulkheadLoadTest.java 15 */
public class BulkheadLoadTest {

    // cv-parse-service ghi log ra file nay (logging.file.name trong application.yml)
    static final String SERVICE_LOG = System.getProperty("user.home") + "/cv-parse-demo.log";

    public static void main(String[] args) throws Exception {
        int n = args.length > 0 ? Integer.parseInt(args[0]) : 15;
        boolean noBulkhead = args.length > 1 && args[1].equals("no-bulkhead");
        String path = noBulkhead ? "/analyze-no-bulkhead/" : "/analyze/";
        HttpClient client = HttpClient.newBuilder().version(HttpClient.Version.HTTP_1_1).build();
        System.out.println("Gui " + n + " request qua gateway, cach nhau 100ms | " + (noBulkhead ? "KHONG co Bulkhead" : "CO Semaphore Bulkhead"));
        System.out.println("CLIENT  = script gui/nhan | SERVICE = log that cua cv-parse-service (" + SERVICE_LOG + ")");

        List<CompletableFuture<Void>> calls = new ArrayList<>();
        Thread tail = new Thread(() -> tailServiceLog(calls));
        tail.setDaemon(true);
        tail.start();
        Thread.sleep(300); // de tail mo file truoc khi gui

        for (int i = 1; i <= n; i++) {
            String cvId = String.format("CV-%02d", i);
            HttpRequest req = HttpRequest.newBuilder(URI.create("http://localhost:8080/api/v1/cvparses" + path + cvId)).build();
            System.out.printf("[%s] CLIENT  -> %s gui request%n", now(), cvId);
            calls.add(client.sendAsync(req, HttpResponse.BodyHandlers.ofString())
                    .thenAccept(r -> System.out.printf("[%s] CLIENT  <- %s %d | %s%n", now(), cvId, r.statusCode(), explain(r)))
                    .exceptionally(e -> {
                        System.out.println(cvId + " LOI KET NOI (gateway chua chay?): " + e.getCause());
                        return null;
                    }));
            Thread.sleep(100); // gui cach nhau 100ms: moi lan lay / tra permit tach rieng, log de doc
        }
        CompletableFuture.allOf(calls.toArray(CompletableFuture[]::new)).join();
        Thread.sleep(500); // cho tail in not dong cuoi
    }

    // Doc phan moi ghi them vao file log cua service, in cac dong demo ([CV-..])
    static void tailServiceLog(List<CompletableFuture<Void>> calls) {
        try (RandomAccessFile f = new RandomAccessFile(SERVICE_LOG, "r")) {
            long pos = f.length(); // bo qua log cu
            while (true) {
                if (f.length() > pos) {
                    f.seek(pos);
                    byte[] buf = new byte[(int) (f.length() - pos)];
                    f.readFully(buf);
                    pos += buf.length;
                    for (String line : new String(buf, StandardCharsets.UTF_8).split("\r?\n")) {
                        int msg = line.indexOf("[CV-");
                        if (msg >= 0) System.out.printf("[%s] SERVICE    %s%n", line.length() > 23 ? line.substring(11, 23) : "", line.substring(msg));
                    }
                }
                Thread.sleep(100);
            }
        } catch (java.io.FileNotFoundException e) {
            System.out.println("KHONG THAY FILE LOG " + SERVICE_LOG + " -> restart cv-parse-service (can logging.file.name trong application.yml)");
        } catch (Exception ignored) {
        }
    }

    static String now() {
        return java.time.LocalTime.now().format(java.time.format.DateTimeFormatter.ofPattern("HH:mm:ss.SSS"));
    }

    // 200 va 503-do-Bulkhead: service da ghi san ly do trong body. Con lai la loi tu gateway/service, khong phai Bulkhead.
    static String explain(HttpResponse<String> r) {
        String body = r.body();
        if (r.statusCode() == 200 || body.startsWith("BULKHEAD")) return body;
        return "KHONG PHAI DO BULKHEAD (request chua toi duoc Bulkhead) - " + (r.statusCode() == 503
                ? "gateway khong tim thay cv-parse-service tren Eureka (service chua chay/chua dang ky): " : "loi: ") + body;
    }
}
