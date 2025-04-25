package com.capstone.rentit.item.repository;

import com.capstone.rentit.item.domain.Item;
import com.capstone.rentit.item.dto.ItemSearchForm;
import org.springframework.stereotype.Repository;

import java.util.List;

public interface CustomItemRepository {
    List<Item> search(ItemSearchForm form);
}
