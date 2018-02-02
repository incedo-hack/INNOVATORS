package com.innovators.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Component;

import com.innovators.Entity.Candidate;

@Component
public interface CandidateJPARepository extends JpaRepository<Candidate, Integer>{

	List<Candidate> findByRole(String role);	

}