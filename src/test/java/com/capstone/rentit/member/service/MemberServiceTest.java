package com.capstone.rentit.member.service;

import com.capstone.rentit.file.service.FileStorageService;
import com.capstone.rentit.item.domain.Item;
import com.capstone.rentit.item.dto.ItemBriefResponse;
import com.capstone.rentit.item.status.ItemStatusEnum;
import com.capstone.rentit.member.exception.MemberNotFoundException;
import com.capstone.rentit.member.exception.MemberTypeMismatchException;
import com.capstone.rentit.member.status.GenderEnum;
import com.capstone.rentit.member.status.MemberRoleEnum;
import com.capstone.rentit.member.domain.Company;
import com.capstone.rentit.member.domain.Member;
import com.capstone.rentit.member.domain.Student;
import com.capstone.rentit.member.domain.StudentCouncilMember;
import com.capstone.rentit.member.dto.*;
import com.capstone.rentit.member.repository.MemberRepository;
import com.capstone.rentit.register.exception.EmailAlreadyRegisteredException;
import com.capstone.rentit.rental.domain.Rental;
import com.capstone.rentit.rental.dto.RentalBriefResponse;
import com.capstone.rentit.rental.status.RentalStatusEnum;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MemberServiceTest {

    @Mock
    private MemberRepository memberRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private FileStorageService fileStorageService;

    @InjectMocks
    private MemberService memberService;

    @Captor
    private ArgumentCaptor<Member> memberCaptor;

    private static final String RAW_PW = "rawPassword";
    private static final String ENC_PW = "encPassword";
    private static final Long   ID    = 42L;
    MockMultipartFile mockImage = new MockMultipartFile("img1", "img1.jpg",
            "image/jpeg", "dummy".getBytes());

    @Test @DisplayName("학생 회원 생성 성공")
    void createStudentMember() {
        // given
        when(passwordEncoder.encode(RAW_PW)).thenReturn(ENC_PW);
        StudentCreateForm form = new StudentCreateForm();
        form.setName("stu");
        form.setEmail("stu@test.com");
        form.setPassword(RAW_PW);
        form.setNickname("nick");
        form.setPhone("010-0000-0000");
        form.setUniversity("Uni");
        form.setStudentId("S123");
        form.setGender(GenderEnum.MEN);

        when(memberRepository.save(any(Member.class)))
                .thenAnswer(invocation -> {
                    Student s = invocation.getArgument(0);
                    return Student.builder()
                            .memberId(ID)
                            .name(s.getName())
                            .email(s.getEmail())
                            .password(s.getPassword())
                            .nickname(s.getNickname())
                            .phone(s.getPhone())
                            .university(s.getUniversity())
                            .studentId(s.getStudentId())
                            .gender(s.getGender())
                            .role(s.getRole())
                            .build();
                });

        // when
        Long savedId = memberService.createMember(form);

        // then
        assertThat(savedId).isEqualTo(ID);
        verify(passwordEncoder).encode(RAW_PW);
        verify(memberRepository).save(memberCaptor.capture());

        Member captured = memberCaptor.getValue();
        assertThat(captured).isInstanceOf(Student.class);
        assertThat(captured.getRole()).isEqualTo(MemberRoleEnum.STUDENT);
        assertThat(((Student)captured).getNickname()).isEqualTo("nick");
        assertThat(captured.getPassword()).isEqualTo(ENC_PW);
    }

    @Test @DisplayName("회사 회원 생성 성공")
    void createCompanyMember() {
        // given
        CompanyCreateForm form = new CompanyCreateForm();
        form.setName("comp");
        form.setEmail("comp@test.com");
        form.setPassword(RAW_PW);
        form.setCompanyName("Acme");

        when(memberRepository.save(any(Member.class)))
                .thenAnswer(invocation -> {
                    Company c = (Company) invocation.getArgument(0);
                    return Company.builder()
                            .memberId(ID)
                            .name(c.getName())
                            .email(c.getEmail())
                            .password(c.getPassword())
                            .companyName(c.getCompanyName())
                            .role(c.getRole())
                            .build();
                });

        // when
        Long savedId = memberService.createMember(form);

        // then
        assertThat(savedId).isEqualTo(ID);
        verify(memberRepository).save(memberCaptor.capture());

        Member captured = memberCaptor.getValue();
        assertThat(captured).isInstanceOf(Company.class);
        assertThat(captured.getRole()).isEqualTo(MemberRoleEnum.COMPANY);
        assertThat(((Company)captured).getCompanyName()).isEqualTo("Acme");
    }

    @Test @DisplayName("학생회 회원 생성 성공")
    void createCouncilMember() {
        // given
        StudentCouncilMemberCreateForm form = new StudentCouncilMemberCreateForm();
        form.setName("council");
        form.setEmail("c@test.com");
        form.setPassword(RAW_PW);
        form.setUniversity("Uni");

        when(memberRepository.save(any(Member.class)))
                .thenAnswer(inv -> {
                    StudentCouncilMember m = (StudentCouncilMember) inv.getArgument(0);
                    return StudentCouncilMember.builder()
                            .memberId(ID)
                            .name(m.getName())
                            .email(m.getEmail())
                            .password(m.getPassword())
                            .university(m.getUniversity())
                            .role(m.getRole())
                            .build();
                });

        // when
        Long savedId = memberService.createMember(form);

        // then
        assertThat(savedId).isEqualTo(ID);
        verify(memberRepository).save(memberCaptor.capture());

        Member captured = memberCaptor.getValue();
        assertThat(captured).isInstanceOf(StudentCouncilMember.class);
        assertThat(captured.getRole()).isEqualTo(MemberRoleEnum.COUNCIL);
    }

    @Test @DisplayName("지원하지 않는 CreateForm일 경우 예외")
    void createMember_unsupportedForm() {
        MemberCreateForm bad = new MemberCreateForm() {};
        assertThatThrownBy(() -> memberService.createMember(bad))
                .isInstanceOf(MemberTypeMismatchException.class)
                .hasMessageContaining("지원하지 않는 회원 유형입니다.");
    }

    @Test @DisplayName("ID로 조회 성공")
    void getMemberById_found() {
        Student s = stubStudent();
        when(memberRepository.findById(ID)).thenReturn(Optional.of(s));

        MemberDto dto = memberService.getMemberById(ID);

        assertThat(dto.getMemberId()).isEqualTo(ID);
        assertThat(dto.getRole()).isEqualTo(MemberRoleEnum.STUDENT);
    }

    @Test @DisplayName("ID로 조회 실패 시 MemberNotFoundException")
    void getMemberById_notFound() {
        when(memberRepository.findById(ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> memberService.getMemberById(ID))
                .isInstanceOf(MemberNotFoundException.class)
                .hasMessage("존재하지 않는 사용자 ID 입니다.");
    }

    @Test @DisplayName("Email로 조회 성공")
    void getMemberByEmail_found() {
        Company c = stubCompany();
        when(memberRepository.findByEmail("c@test.com")).thenReturn(Optional.of(c));

        MemberDto dto = memberService.getMemberByEmail("c@test.com");

        assertThat(dto.getMemberId()).isEqualTo(ID);
        assertThat(dto.getRole()).isEqualTo(MemberRoleEnum.COMPANY);
    }

    @Test @DisplayName("Email로 조회 실패 시 MemberNotFoundException")
    void getMemberByEmail_notFound() {
        when(memberRepository.findByEmail("x@test.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> memberService.getMemberByEmail("x@test.com"))
                .isInstanceOf(MemberNotFoundException.class)
                .hasMessage("존재하지 않는 사용자 이메일 입니다.");
    }

    @Test @DisplayName("전체 회원 조회")
    void getAllMembers() {
        Student s1 = stubStudent();
        Company c1 = stubCompany();
        when(memberRepository.findAll()).thenReturn(Arrays.asList(s1, c1));

        List<MemberDto> dtos = memberService.getAllMembers();

        assertThat(dtos).hasSize(2)
                .extracting(MemberDto::getRole)
                .containsExactly(MemberRoleEnum.STUDENT, MemberRoleEnum.COMPANY);
    }

    @Test @DisplayName("회원 정보 업데이트 성공")
    void updateMember_success() {
        Student mock = mock(Student.class);
        when(memberRepository.findById(ID)).thenReturn(Optional.of(mock));
        when(fileStorageService.store(any(MultipartFile.class)))
                .thenReturn("new1");

        StudentUpdateForm form = new StudentUpdateForm();
        form.setName("new");
        form.setNickname("nn");
        form.setPhone("010");

        doNothing().when(mock).update(form);

        memberService.updateMember(ID, form, mockImage);

        verify(mock).update(form);
    }

    @Test @DisplayName("업데이트 시 ID 없으면 MemberNotFoundException")
    void updateMember_notFound() {
        when(memberRepository.findById(ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> memberService.updateMember(ID, new StudentUpdateForm(), mockImage))
                .isInstanceOf(MemberNotFoundException.class)
                .hasMessage("존재하지 않는 사용자 ID 입니다.");
    }

    @Test @DisplayName("업데이트 타입 불일치 시 MemberTypeMismatchException 전파")
    void updateMember_typeMismatch() {
        Student mock = mock(Student.class);
        when(memberRepository.findById(ID)).thenReturn(Optional.of(mock));

        CompanyUpdateForm badForm = new CompanyUpdateForm();
        doThrow(new MemberTypeMismatchException("회원 유형이 일치하지 않습니다."))
                .when(mock).update(any(MemberUpdateForm.class));

        assertThatThrownBy(() -> memberService.updateMember(ID, badForm, mockImage))
                .isInstanceOf(MemberTypeMismatchException.class)
                .hasMessage("회원 유형이 일치하지 않습니다.");
    }

    @Test @DisplayName("회원 삭제 성공")
    void deleteMember_success() {
        Student mock = stubStudent();
        when(memberRepository.findById(ID)).thenReturn(Optional.of(mock));

        memberService.deleteMember(ID);

        verify(memberRepository).delete(mock);
    }

    @Test @DisplayName("삭제 시 ID 없으면 MemberNotFoundException")
    void deleteMember_notFound() {
        when(memberRepository.findById(ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> memberService.deleteMember(ID))
                .isInstanceOf(MemberNotFoundException.class)
                .hasMessage("존재하지 않는 사용자 ID 입니다.");
    }

    @Test @DisplayName("이미 등록된 이메일 확인 성공")
    void ensureEmailNotRegistered_success() {
        // given
        when(memberRepository.findByEmail("new@example.com"))
                .thenReturn(Optional.empty());

        // when / then
        assertDoesNotThrow(() ->
                memberService.ensureEmailNotRegistered("new@example.com")
        );

        verify(memberRepository, times(1)).findByEmail("new@example.com");
    }

    @Test @DisplayName("등록된 이메일이 있으면 EmailAlreadyRegisteredException")
    void ensureEmailNotRegistered_notFound() {
        // given
        when(memberRepository.findByEmail("exist@example.com"))
                .thenReturn(Optional.of(mock(Member.class)));

        // when / then
        EmailAlreadyRegisteredException ex = assertThrows(
                EmailAlreadyRegisteredException.class,
                () -> memberService.ensureEmailNotRegistered("exist@example.com")
        );
        assertEquals("이미 등록된 이메일입니다.", ex.getMessage());

        verify(memberRepository, times(1)).findByEmail("exist@example.com");
    }

    @Test
    @DisplayName("존재하는 회원이면 MyProfileResponse 에 매핑된 DTO 반환")
    void getMyProfile_whenMemberExists_thenReturnMappedDto() {
        // given
        long memberId = 42L;

        // Member 엔티티 준비
        Student member = Student.builder()
                .memberId(memberId)
                .email("foo@bar.com")
                .name("홍길동")
                .password("pw")
                .role(MemberRoleEnum.STUDENT)
                .university("OO대학교")
                .studentId("20250001")
                .build();

        // 1) 내가 등록한 아이템
        Item item = Item.builder()
                .itemId(100L)
                .name("드릴")
                .ownerId(memberId)
                .description("전동 드릴")
                .status(ItemStatusEnum.AVAILABLE)
                .returnPolicy("반납정책")
                .damagedPolicy("파손정책")
                .build();
        member.getItems().add(item);

        // 2) 내가 소유자로서 빌려준 대여
        Rental owned = Rental.builder()
                .rentalId(200L)
                .item(item)            // 연관관계 편의 메서드 없이 직접 세팅
                .ownerMember(member)
                .renterMember(
                        Student.builder()
                                .memberId(43L)
                                .email("bar@baz.com").name("임차인")
                                .password("pw").role(MemberRoleEnum.STUDENT)
                                .university("OO대").studentId("20250002")
                                .build()
                )
                .requestDate(LocalDateTime.now())
                .startDate(LocalDateTime.now())
                .dueDate(LocalDateTime.now())
                .status(RentalStatusEnum.APPROVED)
                .build();
        member.getOwnedRentals().add(owned);

        // 3) 내가 대여자로서 빌린 대여
        Rental rented = Rental.builder()
                .rentalId(300L)
                .item(
                        Item.builder()
                                .itemId(101L)
                                .name("체인톱")
                                .ownerId(43L)
                                .description("목재 절단용")
                                .status(ItemStatusEnum.AVAILABLE)
                                .returnPolicy("반납정책")
                                .damagedPolicy("파손정책")
                                .build()
                )
                .ownerMember(
                        Student.builder()
                                .memberId(44L)
                                .email("baz@foo.com").name("소유자")
                                .password("pw").role(MemberRoleEnum.STUDENT)
                                .university("OO대").studentId("20250003")
                                .build()
                )
                .renterMember(member)
                .requestDate(LocalDateTime.now())
                .startDate(LocalDateTime.now())
                .dueDate(LocalDateTime.now())
                .status(RentalStatusEnum.APPROVED)
                .build();
        member.getRentedRentals().add(rented);

        when(memberRepository.findProfileWithAll(memberId))
                .thenReturn(Optional.of(member));

        // when
        MyProfileResponse dto = memberService.getMyProfile(memberId);

        // then: 기본 필드
        assertThat(dto.getMemberId()).isEqualTo(memberId);
        assertThat(dto.getEmail()).isEqualTo("foo@bar.com");
        assertThat(dto.getName()).isEqualTo("홍길동");

        // then: items 매핑 검증
        List<ItemBriefResponse> items = dto.getItems();
        assertThat(items).hasSize(1);
        assertThat(items.get(0).getItemId()).isEqualTo(100L);
        assertThat(items.get(0).getName()).isEqualTo("드릴");

        // then: owned rentals 매핑 검증
        List<RentalBriefResponse> ownedDtos = dto.getOwnedRentals();
        assertThat(ownedDtos).hasSize(1);
        assertThat(ownedDtos.get(0).getRentalId()).isEqualTo(200L);
        assertThat(ownedDtos.get(0).getItemName()).isEqualTo("드릴");
        assertThat(ownedDtos.get(0).isOwner()).isTrue();

        // then: rented rentals 매핑 검증
        List<RentalBriefResponse> rentedDtos = dto.getRentedRentals();
        assertThat(rentedDtos).hasSize(1);
        assertThat(rentedDtos.get(0).getRentalId()).isEqualTo(300L);
        assertThat(rentedDtos.get(0).getItemName()).isEqualTo("체인톱");
        assertThat(rentedDtos.get(0).isOwner()).isFalse();

        verify(memberRepository).findProfileWithAll(memberId);
    }

    @Test
    @DisplayName("존재하지 않는 회원이면 MemberNotFoundException 발생")
    void getMyProfile_whenNotFound_thenThrow() {
        long memberId = 99L;
        when(memberRepository.findProfileWithAll(memberId))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> memberService.getMyProfile(memberId))
                .isInstanceOf(MemberNotFoundException.class)
                .hasMessage("존재하지 않는 사용자 ID 입니다.");

        verify(memberRepository).findProfileWithAll(memberId);
    }

    // — 헬퍼 메서드 —
    private Student stubStudent() {
        return Student.builder()
                .memberId(ID)
                .name("stu")
                .email("stu@test.com")
                .role(MemberRoleEnum.STUDENT)
                .build();
    }

    private Company stubCompany() {
        return Company.builder()
                .memberId(ID)
                .name("comp")
                .email("c@test.com")
                .role(MemberRoleEnum.COMPANY)
                .build();
    }
}