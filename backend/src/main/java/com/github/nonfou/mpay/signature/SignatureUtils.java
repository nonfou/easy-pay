package com.github.nonfou.mpay.signature;

import com.github.nonfou.mpay.dto.PublicCreateOrderDTO;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Map;
import java.util.TreeMap;

public final class SignatureUtils {

    private SignatureUtils() {}

    public static String buildSignString(PublicCreateOrderDTO request) {
        Map<String, Object> sorted = new TreeMap<>();
        sorted.put("pid", request.getPid());
        sorted.put("type", request.getType());
        sorted.put("out_trade_no", request.getOutTradeNo());
        sorted.put("name", request.getName());
        sorted.put("money", request.getMoney());
        sorted.put("notify_url", request.getNotifyUrl());
        if (request.getReturnUrl() != null) {
            sorted.put("return_url", request.getReturnUrl());
        }
        sorted.put("clientip", request.getClientIp());
        sorted.put("device", request.getDevice());
        // 防重放参数加入签名
        if (request.getTimestamp() != null) {
            sorted.put("timestamp", request.getTimestamp());
        }
        if (request.getNonce() != null) {
            sorted.put("nonce", request.getNonce());
        }
        if (request.getAttach() != null) {
            sorted.putAll(request.getAttach());
        }
        StringBuilder builder = new StringBuilder();
        sorted.forEach((k, v) -> {
            if (v != null && !"sign".equalsIgnoreCase(k)) {
                builder.append(k).append("=").append(v).append("&");
            }
        });
        if (builder.length() > 0) {
            builder.setLength(builder.length() - 1);
        }
        return builder.toString();
    }

    public static String buildSignString(Map<String, Object> data) {
        Map<String, Object> sorted = new TreeMap<>(data);
        StringBuilder builder = new StringBuilder();
        sorted.forEach((k, v) -> {
            if (v != null && !"sign".equalsIgnoreCase(k)) {
                builder.append(k).append("=").append(v).append("&");
            }
        });
        if (builder.length() > 0) {
            builder.setLength(builder.length() - 1);
        }
        return builder.toString();
    }

    public static String md5(String content) {
        try {
            MessageDigest md5 = MessageDigest.getInstance("MD5");
            byte[] digest = md5.digest(content.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : digest) {
                sb.append(String.format("%02x", b & 0xff));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("MD5 not available", e);
        }
    }
}
