package com.capstone.rentit.payment.dto;

import com.capstone.rentit.payment.config.NhApiProperties;
import lombok.Builder;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.atomic.AtomicInteger;

@Builder
public record NhHeader(
        String ApiNm,      // 호출 API 명 (ex. DrawingTransfer)
        String Tsymd,      // YYYYMMDD
        String Trtm,       // HHMMSS
        String Iscd,       // 기관코드
        String FintechApsno, //핀테크 앱 일련번호
        String ApiSvcCd,   // API 서비스 코드
        String IsTuno,     // 기관 거래고유번호
        String AccessToken // 인증키
) {
    /* 전역 순번(동시성 안전) */
    private static final AtomicInteger SEQ = new AtomicInteger();

    public static NhHeader create(String apiNm, String svcCd, NhApiProperties prop) {

        LocalDateTime now = LocalDateTime.now();
        String ts = now.format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String tm = now.format(DateTimeFormatter.ofPattern("HHmmss"));

        String isTuno = ts + String.format("%010d", SEQ.getAndIncrement());

        return NhHeader.builder()
                .ApiNm(apiNm)
                .Tsymd(ts)
                .Trtm(tm)
                .Iscd(prop.iscd())
                .FintechApsno(prop.fintechApsNo())
                .ApiSvcCd(svcCd)
                .IsTuno(isTuno)
                .AccessToken(prop.accessToken())
                .build();
    }
}
