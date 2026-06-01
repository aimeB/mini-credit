package com.mini.credit.repository.credit;

import com.mini.credit.entity.credit.RemboursementCredit;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RemboursementCreditRepository extends JpaRepository<RemboursementCredit, Long> {
}