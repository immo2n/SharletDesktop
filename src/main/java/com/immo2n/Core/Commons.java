package com.immo2n.Core;

import java.util.Random;

public class Commons {
    public static String extractParameterValue(String requestBody, String parameterName) {
        String[] pairs = requestBody.split("&");
        for (String pair : pairs) {
            String[] keyValue = pair.split("=");
            if (keyValue.length == 2 && keyValue[0].equals(parameterName)) {
                return decodeURL(keyValue[1]);
            }
        }
        return null;
    }
    public static String decodeURL(String url) {
        StringBuilder decoded = new StringBuilder();
        char[] chars = url.toCharArray();
        int length = chars.length;
        int i = 0;
        while (i < length) {
            char c = chars[i];
            if (c == '%' && i + 2 < length) {
                char hex1 = Character.toLowerCase(chars[i + 1]);
                char hex2 = Character.toLowerCase(chars[i + 2]);
                int digit1 = Character.digit(hex1, 16);
                int digit2 = Character.digit(hex2, 16);
                if (digit1 != -1 && digit2 != -1) {
                    decoded.append((char) ((digit1 << 4) + digit2));
                    i += 3;
                    continue;
                }
            }
            decoded.append(c);
            i++;
        }
        return decoded.toString();
    }
}
