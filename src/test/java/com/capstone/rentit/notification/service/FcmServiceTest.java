package com.capstone.rentit.notification.service;

import com.google.firebase.FirebaseApp;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.Message;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.MockedStatic;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;

import static org.assertj.core.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FcmServiceTest {

    @Mock FirebaseApp firebaseApp;

    @Test
    @DisplayName("정상 전송 시 메시지 ID를 반환하고 send()가 호출된다")
    void itReturnsMessageId_andInvokesSend() throws Exception {
        String expectedId = "fake-id";

        // FirebaseMessaging.getInstance(firebaseApp) 가 mockMsg를 리턴하도록
        try (MockedStatic<FirebaseMessaging> firebaseStatic = mockStatic(FirebaseMessaging.class)) {
            FirebaseMessaging mockMsg = mock(FirebaseMessaging.class);
            firebaseStatic
                    .when(() -> FirebaseMessaging.getInstance(firebaseApp))
                    .thenReturn(mockMsg);

            // send() 호출 시 expectedId 반환
            when(mockMsg.send(any(Message.class))).thenReturn(expectedId);

            FcmService sut = new FcmService(firebaseApp);

            // 실제 호출
            String result = sut.sendToToken("tok", "T", "B", Map.of("k", "v"));

            // 반환값 검증
            assertThat(result).isEqualTo(expectedId);

            // send()가 한 번 호출됐는지만 검증
            verify(mockMsg, times(1)).send(any(Message.class));
        }
    }

    @Test
    @DisplayName("FirebaseMessagingException 발생 시 IllegalStateException으로 래핑된다")
    void itWrapsFirebaseExceptionInIllegalState() throws Exception {
        // 가짜 FirebaseMessagingException을 mock으로 생성
        FirebaseMessagingException fakeEx = mock(FirebaseMessagingException.class);

        try (MockedStatic<FirebaseMessaging> firebaseStatic = mockStatic(FirebaseMessaging.class)) {
            FirebaseMessaging mockMsg = mock(FirebaseMessaging.class);
            firebaseStatic
                    .when(() -> FirebaseMessaging.getInstance(firebaseApp))
                    .thenReturn(mockMsg);

            // send() 호출 시 fakeEx 던지기
            when(mockMsg.send(any(Message.class))).thenThrow(fakeEx);

            FcmService sut = new FcmService(firebaseApp);

            // IllegalStateException이 발생하고, 원인은 fakeEx인지 확인
            IllegalStateException ise = assertThrows(
                    IllegalStateException.class,
                    () -> sut.sendToToken("tok", "T", "B", Map.of())
            );
            assertThat(ise).hasCause(fakeEx);
        }
    }
}
