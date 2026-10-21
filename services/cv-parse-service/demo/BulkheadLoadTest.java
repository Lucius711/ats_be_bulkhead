import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Ban N request phan tich CV CUNG LUC, sau 1s goi /quick-list (HR xem danh sach) va do thoi gian.
 *
 * Chay (Java 21, khong can build):
 *   java demo/BulkheadLoadTest.java without 15   -> khong Bulkhead: quick-list bi treo ~7s
 *   java demo/BulkheadLoadTest.java with 15      -> co Bulkhead:   3 x 200, 12 x 503, quick-list nhanh
 */
public class BulkheadLoadTest {

    static final String BASE = "http://localhost:8085/api/v1/cvparses";
    static final HttpClient CLIENT = HttpClient.newBuilder().version(HttpClient.Version.HTTP_1_1).build();

    public static void main(String[] args) throws Exception {
        boolean with = args.length == 0 || !args[0].equals("without");
        int n = args.length > 1 ? Integer.parseInt(args[1]) : 15;
        String path = with ? "/analyze/" : "/analyze-no-bulkhead/";
        System.out.printf("=== %d request phan tich CV cung luc, %s Bulkhead ===%n", n, with ? "CO" : "KHONG");

        long t0 = System.nanoTime();
        List<CompletableFuture<String>> calls = new ArrayList<>();
        int[] ok = {0}, rejected = {0};
        for (int i = 1; i <= n; i++) {
            String cvId = String.format("CV-%02d", i);
            calls.add(CLIENT.sendAsync(get(BASE + path + cvId), HttpResponse.BodyHandlers.ofString())
                    .thenApply(r -> {
                        synchronized (ok) { if (r.statusCode() == 200) ok[0]++; else rejected[0]++; }
                        return String.format("%s -> %d sau %5d ms", cvId, r.statusCode(), ms(t0));
                    }));
        }

        Thread.sleep(1000); // de cac request CV chiem cho truoc
        long t1 = System.nanoTime();
        HttpResponse<String> light = CLIENT.send(get(BASE + "/quick-list"), HttpResponse.BodyHandlers.ofString());
        System.out.printf("[HR quick-list] -> %d sau %d ms  <== chuc nang khong lien quan%n", light.statusCode(), ms(t1));

        calls.forEach(c -> System.out.println(c.join()));
        System.out.printf("Tong ket: %d thanh cong, %d bi tu choi, tong %d ms%n", ok[0], rejected[0], ms(t0));
    }

    static HttpRequest get(String url) {
        return HttpRequest.newBuilder(URI.create(url)).timeout(Duration.ofSeconds(60)).GET().build();
    }

    static long ms(long start) {
        return (System.nanoTime() - start) / 1_000_000;
    }
}
