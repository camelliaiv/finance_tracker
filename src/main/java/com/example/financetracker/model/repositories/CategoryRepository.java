package com.example.financetracker.model.repositories;

import com.example.financetracker.model.DTOs.CategoryDTO;
import com.example.financetracker.model.entities.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CategoryRepository extends JpaRepository<Category, Integer> {
    List<Category> findByName(String name);
}
