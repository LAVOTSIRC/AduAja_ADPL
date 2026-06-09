package com.plr.aduaja.util;

import java.math.BigDecimal;
import java.math.RoundingMode;

public final class DataMaskingUtil {

    private DataMaskingUtil() {}

    public static String maskEmail(String email) {
        if (email == null || email.isBlank()) return "-";
        int atIndex = email.indexOf('@');
        if (atIndex <= 1) return email;
        String local = email.substring(0, atIndex);
        String domain = email.substring(atIndex);
        if (local.length() <= 2) {
            return local.charAt(0) + "***" + domain;
        }
        return local.charAt(0) + "***" + local.charAt(local.length() - 1) + domain;
    }

    public static String maskPhone(String phone) {
        if (phone == null || phone.isBlank()) return "-";
        if (phone.length() <= 4) return phone;
        return phone.substring(0, 2) + "****" + phone.substring(phone.length() - 2);
    }

    public static String maskName(String fullName) {
        if (fullName == null || fullName.isBlank()) return "-";
        String[] parts = fullName.trim().split("\\s+");
        if (parts.length == 1) {
            if (parts[0].length() <= 2) return parts[0];
            return parts[0].charAt(0) + "***";
        }
        StringBuilder masked = new StringBuilder();
        masked.append(parts[0]);
        for (int i = 1; i < parts.length; i++) {
            String p = parts[i];
            if (p.length() <= 2) {
                masked.append(" ").append(p);
            } else {
                masked.append(" ").append(p.charAt(0)).append("***");
            }
        }
        return masked.toString();
    }

    public static String maskNIK(String nik) {
        if (nik == null || nik.isBlank()) return "-";
        if (nik.length() <= 6) return nik;
        return nik.substring(0, 2) + "****" + nik.substring(nik.length() - 2);
    }

    public static BigDecimal roundCoordinate(BigDecimal coord, int decimals) {
        if (coord == null) return null;
        return coord.setScale(decimals, RoundingMode.HALF_UP);
    }
}
