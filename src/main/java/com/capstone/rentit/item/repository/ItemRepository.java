package com.capstone.rentit.item.repository;

import com.capstone.rentit.item.domain.Item;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ItemRepository extends JpaRepository<Item, Long> {
}
