package com.capstone.rentit.member.service;

import com.capstone.rentit.file.service.FileStorageService;
import com.capstone.rentit.item.domain.Item;
import com.capstone.rentit.item.dto.ItemBriefResponse;
import com.capstone.rentit.member.dto.*;
import com.capstone.rentit.member.domain.*;
import com.capstone.rentit.member.exception.*;
import com.capstone.rentit.member.repository.MemberRepository;
import com.capstone.rentit.member.status.MemberRoleEnum;
import com.capstone.rentit.payment.service.PaymentService;
import com.capstone.rentit.register.exception.EmailAlreadyRegisteredException;
import com.capstone.rentit.rental.domain.Rental;
import com.capstone.rentit.rental.dto.RentalBriefResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class MemberService {

    private final MemberRepository memberRepository;
    private final PaymentService paymentService;
    private final PasswordEncoder passwordEncoder;
    private final FileStorageService fileStorageService;

    public Long createMember(MemberCreateForm form) {
        Member member = Member.createEntity(form, passwordEncoder.encode(form.getPassword()));
        Long memberId = memberRepository.save(member).getMemberId();
        paymentService.createWallet(memberId);
        return memberId;
    }

    public Long createAdmin(MemberCreateForm form){
        Member member = Member.createEntity(form, passwordEncoder.encode(form.getPassword()));
        return memberRepository.save(member).getMemberId();
    }

    @Transactional(readOnly = true)
    public MemberDto getMemberById(Long id) {
        Member member = findMemberById(id);
        return MemberDto.fromEntity(member, fileStorageService.generatePresignedUrl(member.getProfileImg()));
    }

    @Transactional(readOnly = true)
    public MemberDto getMemberByEmail(String email) {
        Member member = findMemberByEmail(email);
        return MemberDto.fromEntity(member, fileStorageService.generatePresignedUrl(member.getProfileImg()));
    }

    @Transactional(readOnly = true)
    public List<MemberDto> getAllMembers() {
        return memberRepository.findAll().stream()
                .filter(member -> member.getRole() != MemberRoleEnum.ADMIN)
                .map(member -> MemberDto.fromEntity(member, fileStorageService.generatePresignedUrl(member.getProfileImg())))
                .collect(Collectors.toList());
    }

    public void updateMember(Long id, MemberUpdateForm form, MultipartFile image) {
        Member member = findMemberById(id);
        member.update(form);
        uploadProfileImage(member, image);
    }
    private void uploadProfileImage(Member member, MultipartFile image) {
        if(image != null && !image.isEmpty()) {
            String objectKey = fileStorageService.store(image);
            member.updateProfile(objectKey);
        }
    }

    @Transactional(readOnly = true)
    public MyProfileResponse getMyProfile(Long memberId) {

        Member member = memberRepository.findProfileWithAll(memberId)
                .orElseThrow(() ->
                        new MemberNotFoundException("존재하지 않는 사용자 ID 입니다."));

        List<ItemBriefResponse>    items   = member.getItems()
                .stream()
                .map(this::toItemBrief)
                .toList();

        List<RentalBriefResponse>  owned   = mapRentals(member.getOwnedRentals(),  true);
        List<RentalBriefResponse>  rented  = mapRentals(member.getRentedRentals(), false);

        return MyProfileResponse.fromEntity(member, items, owned, rented);
    }

    /** 6) 회원 삭제 */
    public void deleteMember(Long id) {
        Member member = findMemberById(id);
        memberRepository.delete(member);
    }

    public void ensureEmailNotRegistered(String email) {
        if (memberRepository.findByEmail(email).isPresent()) {
            throw new EmailAlreadyRegisteredException();
        }
    }

    private Member findMemberById(Long id) {
        return memberRepository.findById(id)
                .orElseThrow(() ->
                        new MemberNotFoundException("존재하지 않는 사용자 ID 입니다."));
    }

    private Member findMemberByEmail(String email) {
        return memberRepository.findByEmail(email)
                .orElseThrow(() ->
                        new MemberNotFoundException("존재하지 않는 사용자 이메일 입니다."));
    }

    private ItemBriefResponse toItemBrief(Item item) {
        return ItemBriefResponse.fromEntity(item, firstImageUrl(item));
    }

    private List<RentalBriefResponse> mapRentals(Set<Rental> rentals,
                                                 boolean asOwner) {
        return rentals.stream()
                .map(r -> RentalBriefResponse.fromEntity(
                        r,
                        getReturnImageUrl(r),
                        asOwner))
                .toList();
    }

    private String firstImageUrl(Item item) {
        if (item == null || item.getImageKeys() == null || item.getImageKeys().isEmpty()) {
            return "";
        }
        return fileStorageService.generatePresignedUrl(item.getImageKeys().get(0));
    }

    private String getReturnImageUrl(Rental rental){
        if(rental == null || rental.getReturnImageUrl() == null || rental.getReturnImageUrl().isEmpty()) {
            return "";
        }
        return fileStorageService.generatePresignedUrl(rental.getReturnImageUrl());
    }
}
