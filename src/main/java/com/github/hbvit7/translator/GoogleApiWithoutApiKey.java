package com.github.hbvit7.translator;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.zip.GZIPInputStream;

import static com.github.hbvit7.translator.Constants.GOOGLE_TRANSLATE_URLS;
import static com.github.hbvit7.translator.Constants.MARK_BEG;
import static com.github.hbvit7.translator.Constants.MARK_END;
import static com.github.hbvit7.translator.Constants.RE_HTML;
import static com.github.hbvit7.translator.Constants.RE_UNICODE;
import static com.github.hbvit7.translator.Constants.USER_AGENTS;

public class GoogleApiWithoutApiKey implements Api {

    private final Map<String, String> params = new HashMap<>();
    {
        params.put("dt", "t");
        params.put("dj", "1");
    }

    public final Map<String, String> headers = new HashMap<>();
    {
        headers.put("Accept", "text/html,application/xhtml+xm…ml;q=0.9,image/webp,*/*;q=0.8");
        headers.put("Accept-Encoding", "gzip, deflate, br");
        headers.put("Connection", "keep-alive");
        headers.put("Cookie", "BL_D_PROV=; BL_T_PROV=Google");
        headers.put("Host", "translate.googleapis.com");
        headers.put("Referer", "https://translate.google.com/");
        headers.put("TE", "Trailers");
        headers.put("Upgrade-Insecure-Requests", "1");
    }

    private final Random rnd = new Random();
    private final Set<String> failures = new HashSet<>();

    @Override
    public String translate(String sourceLang, String targetLang1, String text) throws IOException {
        String trText = text.length() > 5000 ? text.substring(0, 4997) + "..." : text;

        String targetLang = changeIfChinese(targetLang1);

        params.put("sl", sourceLang);
        params.put("tl", targetLang);
        params.put("q", trText);

        headers.put("User-Agent", USER_AGENTS.get(rnd.nextInt(USER_AGENTS.size())));

        byte[] answer = null;

        int size = GOOGLE_TRANSLATE_URLS.size();
        while (failures.size() != size && answer == null) {
            String url = GOOGLE_TRANSLATE_URLS.get(rnd.nextInt(size));
//            System.out.println(url);
            try {
                answer = getURLasByteArray(url, params, headers);
            } catch (IOException e) {
//                System.out.println("Exception " + e.getMessage() +" with url=" + url);
                failures.add(url);
                GOOGLE_TRANSLATE_URLS.remove(url);
            }
        }
        if (failures.size() == size) {
            return null;
        }
        GZIPInputStream gis = new GZIPInputStream(new ByteArrayInputStream(answer));
        BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(gis, "UTF-8"));
        StringBuilder outStr = new StringBuilder();
        String line;
        while ((line = bufferedReader.readLine()) != null) {
            outStr.append(line);
        }
//        System.out.println("outStr = " + outStr);
        String v = outStr.toString();
//        logger.debug("outStr={}", v);
        while (true) {
            Matcher m = RE_UNICODE.matcher(v);
            if (!m.find()) {
                break;
            }
            String g = m.group();
            char c = (char) Integer.parseInt(m.group(1), 16);
            v = v.replace(g, Character.toString(c));
        }
        v = v.replace("\\n", "\n");
        v = v.replace("\\\"", "\"");
        while (true) {
            Matcher m = RE_HTML.matcher(v);
            if (!m.find()) {
                break;
            }
            String g = m.group();
            char c = (char) Integer.parseInt(m.group(1));
            v = v.replace(g, Character.toString(c));
        }

        List<String> items = new ArrayList<>();
        int begin;
        int end = -1;

        do {
            begin = StringUtils.indexOf(v, MARK_BEG, end) + MARK_BEG.length();
            end = StringUtils.indexOf(v, MARK_END, begin);
//            logger.debug("begin={}, end={}", begin, end);

//            System.out.println(v);
//            System.out.println(begin);
//            System.out.println(end);
            String tr;
            if (begin < end) {
                tr = v.substring(begin, end);
            } else {
                tr = v.substring(2, v.length() - 2);
            }
            items.add(tr);
        } while (begin != StringUtils.lastIndexOf(v, MARK_BEG) + MARK_BEG.length());

        String result = StringUtils.join(items, "");
        return result;
    }

    /**
     * Differentiate in target between simplified and traditional Chinese.
     *
     * @param targetLang -
     * @return
     */
    private String changeIfChinese(String targetLang) {
        // TODO - check if needed
        if (targetLang.compareToIgnoreCase("zh-cn") == 0) {
            targetLang = "zh-CN";
        } else if (targetLang.compareToIgnoreCase("zh-tw") == 0) {
            targetLang = "zh-TW";
        } else if (targetLang.compareToIgnoreCase("zh-hk") == 0) {
            targetLang = "zh-HK";
        }
        return targetLang;
    }

    public static byte[] getURLasByteArray(String address, Map<String, String> params,
                                           Map<String, String> additionalHeaders) throws IOException {
        StringBuilder s = new StringBuilder(address);
        boolean next = false;
        if (!address.contains("?")) {
            s.append('?');
        } else {
            next = true;
        }

        for (Map.Entry<String, String> p : params.entrySet()) {
            if (next) {
                s.append('&');
            } else {
                next = true;
            }
            s.append(p.getKey());
            s.append('=');
            s.append(URLEncoder.encode(p.getValue(), StandardCharsets.UTF_8.name()));
        }
        String url = s.toString();

        HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
        try {
            conn.setRequestMethod("GET");
            if (additionalHeaders != null) {
                for (Map.Entry<String, String> en : additionalHeaders.entrySet()) {
                    conn.setRequestProperty(en.getKey(), en.getValue());
                }
            }
            return IOUtils.toByteArray(conn.getInputStream());
        } finally {
            conn.disconnect();
        }
    }

}
