package com.pager.repository;

import com.pager.entity.BotState;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BotStateRepository extends JpaRepository<BotState, String> {
}
