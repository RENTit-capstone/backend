package com.capstone.rentit.item.service;

import com.capstone.rentit.common.CommonResponse;
import com.capstone.rentit.file.service.FileStorageService;
import com.capstone.rentit.item.domain.Item;
import com.capstone.rentit.item.dto.*;
import com.capstone.rentit.item.exception.ItemImageMissingException;
import com.capstone.rentit.item.exception.ItemNotFoundException;
import com.capstone.rentit.item.exception.ItemUnauthorizedException;
import com.capstone.rentit.item.repository.ItemRepository;
import com.capstone.rentit.item.status.ItemStatusEnum;
import com.capstone.rentit.member.dto.MemberDto;
import com.capstone.rentit.rental.domain.Rental;
import com.capstone.rentit.rental.repository.RentalRepository;
import com.capstone.rentit.rental.status.RentalStatusEnum;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
@RequiredArgsConstructor
public class ItemService {

    private final ItemRepository itemRepository;
    private final RentalRepository rentalRepository;
    private final FileStorageService fileStorageService;

    public Long createItem(Long memberId, ItemCreateForm form) {
        assertItemImage(form.getImageKeys());

        Item savedItem = itemRepository.save(Item.createItem(memberId, form));

        return savedItem.getItemId();
    }

    @Transactional(readOnly = true)
    public Page<ItemSearchResponse> getAllItems(ItemSearchForm searchForm, Pageable pageable) {
        Page<Item> page = itemRepository.search(searchForm, pageable);
        return page.map(item ->
                ItemSearchResponse.fromEntity(
                item,
                item.getImageKeys().stream().map(fileStorageService::generatePresignedUrl).toList(),
                fileStorageService.generatePresignedUrl(item.getOwner().getProfileImg()),
                null));
    }

    @Transactional(readOnly = true)
    public ItemSearchResponse getItem(Long itemId) {
        Item item = findItem(itemId);

        LocalDateTime rentalEndAt = null;
        if (item.getStatus() == ItemStatusEnum.OUT) {
            rentalEndAt = rentalRepository
                    .findTopByItemIdAndStatus(itemId, RentalStatusEnum.PICKED_UP)
                    .map(Rental::getDueDate)
                    .orElse(null);
        }
        return ItemSearchResponse.fromEntity(
                item,
                item.getImageKeys().stream().map(fileStorageService::generatePresignedUrl).toList(),
                fileStorageService.generatePresignedUrl(item.getOwner().getProfileImg()),
                rentalEndAt);
    }

    public void updateItem(MemberDto loginMember, Long itemId, ItemUpdateForm form) {
        Item item = findItem(itemId);
        assertOwner(item, loginMember.getMemberId());

        item.updateItem(form);
    }

    public void deleteItem(MemberDto loginMember, Long itemId) {
        Item item = findItem(itemId);
        assertOwner(item, loginMember.getMemberId());

        itemRepository.deleteById(itemId);
    }

    private Item findItem(Long itemId) {
        return itemRepository.findWithOwnerByItemId(itemId)
                .orElseThrow(() ->
                        new ItemNotFoundException("존재하지 않는 물품입니다.")
                );
    }

    private void assertOwner(Item item, Long userId) {
        if (!item.getOwnerId().equals(userId)) {
            throw new ItemUnauthorizedException("자신의 소유 물품이 아닙니다.");
        }
    }

    private void assertItemImage(List<String> images) {
        if (images == null || images.isEmpty()) {
            throw new ItemImageMissingException("물품 이미지가 없습니다.");
        }
    }
}
