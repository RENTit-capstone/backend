package com.capstone.rentit.item.repository;

import com.capstone.rentit.item.domain.Item;
import com.capstone.rentit.item.dto.ItemSearchForm;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

public interface CustomItemRepository {
    public Page<Item> search(ItemSearchForm form, Pageable pageable);
    public Optional<Item> findWithOwnerByItemId(Long itemId);
}
