package com.example.app;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;

@Transactional
public interface HelloRepository extends JpaRepository<HelloEntity, Long> {

}
