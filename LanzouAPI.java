import okhttp3.*;
import org.json.JSONObject;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class LanzouAPI {

    private static String generateRandomString(int length) {
        String chars = "abcdefghijklmnopqrstuvwxyz0123456789";
        SecureRandom random = new SecureRandom();
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            int index = random.nextInt(chars.length());
            sb.append(chars.charAt(index));
        }
        return sb.toString();
    }

    public static String fuckLanzou(String src) throws URISyntaxException, IOException {
        URI parsedUri = new URI(src);
        String baseUrl = parsedUri.getHost();
        String filePath = parsedUri.getPath().replaceAll("^/|/$", "");

        Map<String, String> queryParams = new HashMap<>();
        String query = parsedUri.getQuery();
        if (query != null) {
            String[] pairs = query.split("&");
            for (String pair : pairs) {
                int idx = pair.indexOf("=");
                if (idx > 0) {
                    String key = URLDecoder.decode(pair.substring(0, idx), StandardCharsets.UTF_8.name());
                    String value = (idx < pair.length() - 1) ?
                            URLDecoder.decode(pair.substring(idx + 1), StandardCharsets.UTF_8.name()) : "";
                    queryParams.put(key, value);
                }
            }
        }
        String filePass = queryParams.get("pwd");

        OkHttpClient client = new OkHttpClient();
        Request firstGetRequest = new Request.Builder()
                .url("https://" + baseUrl + "/" + filePath)
                .build();

        Response firstResponse = client.newCall(firstGetRequest).execute();
        String html = firstResponse.body().string();

        String file = null;
        Pattern filePattern = Pattern.compile("file=(\\d+)");
        Matcher fileMatcher = filePattern.matcher(html);
        if (fileMatcher.find()) {
            file = fileMatcher.group(1);
        }

        List<String> signMatches = new ArrayList<>();
        Pattern signPattern = Pattern.compile("'sign':'(.+?)'");
        Matcher signMatcher = signPattern.matcher(html);
        while (signMatcher.find()) {
            signMatches.add(signMatcher.group(1));
        }
        String sign = signMatches.size() >= 2 ? signMatches.get(1) : null;

        String randomStr = generateRandomString(16);
        String boundary = "------------------------" + randomStr;
        String postUrl = "https://www.lanzoux.com/ajaxm.php?file=" + file;
        StringBuilder bodyBuilder = new StringBuilder();
        bodyBuilder.append("--------------------------").append(randomStr).append("\n")
                .append("Content-Disposition: form-data; name=\"action\"\n\n")
                .append("downprocess\n")
                .append("--------------------------").append(randomStr).append("\n")
                .append("Content-Disposition: form-data; name=\"sign\"\n\n")
                .append(sign).append("\n")
                .append("--------------------------").append(randomStr).append("\n")
                .append("Content-Disposition: form-data; name=\"p\"\n\n")
                .append(filePass != null ? filePass : "").append("\n")
                .append("--------------------------").append(randomStr).append("\n")
                .append("Content-Disposition: form-data; name=\"kd\"\n\n")
                .append("1\n")
                .append("--------------------------").append(randomStr).append("--");

        RequestBody postBody = RequestBody.create(
                bodyBuilder.toString(),
                MediaType.parse("multipart/form-data; boundary=" + boundary)
        );

        Headers headers = new Headers.Builder()
                .add("Referer", "https://www.lanzoup.com/" + filePath)
                .add("Content-Type", "multipart/form-data; boundary=" + boundary)
                .build();

        Request postRequest = new Request.Builder()
                .url(postUrl)
                .post(postBody)
                .headers(headers)
                .build();

        Response postResponse = client.newCall(postRequest).execute();
        String postResponseBody = postResponse.body().string();
        JSONObject jsonResponse = new JSONObject(postResponseBody);
        String url = jsonResponse.getString("url");

        OkHttpClient noRedirectClient = new OkHttpClient.Builder()
                .followRedirects(false)
                .build();

        Request finalGetRequest = new Request.Builder()
                .url("https://developer-oss.lanrar.com/file/" + url)
                .addHeader("Referer", "https://developer.lanzoug.com")
                .addHeader("Accept-Language", "zh-CN,zh;q=0.9")
                .build();

        Response finalResponse = noRedirectClient.newCall(finalGetRequest).execute();
        return finalResponse.header("Location");
    }

    public static void main(String[] args) {
        String src = "https://wwu.lanzoue.com/xxxxxxxxxx?pwd=xxxx";
        try {
            String link = fuckLanzou(src);
            System.out.println(link);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
