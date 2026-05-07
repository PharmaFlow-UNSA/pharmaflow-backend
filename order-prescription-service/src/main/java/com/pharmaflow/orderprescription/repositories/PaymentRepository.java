package com.pharmaflow.orderprescription.repositories;

import com.pharmaflow.orderprescription.models.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long>,
                                           JpaSpecificationExecutor<Payment> {
}
