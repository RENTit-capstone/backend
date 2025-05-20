package com.capstone.rentit;

import com.capstone.rentit.file.service.NcpObjectStorageService;
import com.capstone.rentit.locker.message.LockerDeviceProducer;
import com.capstone.rentit.locker.message.LockerDeviceRequestListener;
import com.capstone.rentit.login.provider.JwtTokenProvider;
import com.capstone.rentit.notification.service.FcmService;
import com.capstone.rentit.notification.service.NotificationService;
import com.capstone.rentit.register.service.UnivCertService;
import com.google.firebase.FirebaseApp;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@SpringBootTest
@ActiveProfiles("test")
class RentitApplicationTests {

	@MockitoBean
	private JwtTokenProvider jwtTokenProvider;

	@MockitoBean
	private NcpObjectStorageService ncpObjectStorageService;

	@MockitoBean
	private UnivCertService univCertService;

	@MockitoBean
	private LockerDeviceProducer lockerDeviceProducer;

	@MockitoBean
	private LockerDeviceRequestListener lockerDeviceRequestListener;

	@MockitoBean
	private FirebaseApp firebaseApp;

	@MockitoBean
	private FcmService fcmService;

	@MockitoBean
	private NotificationService notificationService;

	@Test
	void contextLoads() {
	}

}
