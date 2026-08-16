package com.multidrive.api.repository;

import com.multidrive.api.entity.DriveOperationItem;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DriveOperationItemRepository extends JpaRepository<DriveOperationItem, Long> {

	List<DriveOperationItem> findAllByJob_IdOrderBySequenceNoAsc(Long jobId);

}
