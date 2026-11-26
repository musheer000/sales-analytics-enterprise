package com.salesanalytics.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.salesanalytics.dto.CategoryDTO;
import com.salesanalytics.exception.CategoryNotFoundException;
import com.salesanalytics.service.CategoryService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(CategoryController.class)
class CategoryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private CategoryService categoryService;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("GET /api/categories - Should return list of categories with 200 OK")
    void testGetAllCategories() throws Exception {
        CategoryDTO cat1 = new CategoryDTO(1L, "Electronics", "Devices and accessories", 3L);
        CategoryDTO cat2 = new CategoryDTO(2L, "Home & Kitchen", "Appliances", 2L);

        when(categoryService.getAllCategories()).thenReturn(List.of(cat1, cat2));

        mockMvc.perform(get("/api/categories"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.length()").value(2))
                .andExpect(jsonPath("$.data[0].name").value("Electronics"))
                .andExpect(jsonPath("$.data[1].name").value("Home & Kitchen"));
    }

    @Test
    @DisplayName("GET /api/categories/{id} - Should return category when found")
    void testGetCategoryByIdSuccess() throws Exception {
        CategoryDTO cat = new CategoryDTO(1L, "Electronics", "Devices", 5L);
        when(categoryService.getCategoryById(1L)).thenReturn(cat);

        mockMvc.perform(get("/api/categories/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.name").value("Electronics"));
    }

    @Test
    @DisplayName("GET /api/categories/{id} - Should return 404 when category does not exist")
    void testGetCategoryByIdNotFound() throws Exception {
        when(categoryService.getCategoryById(999L))
                .thenThrow(new CategoryNotFoundException(999L));

        mockMvc.perform(get("/api/categories/999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value("Category not found with ID: 999"));
    }

    @Test
    @DisplayName("POST /api/categories - Should create category and return 201 Created")
    void testCreateCategorySuccess() throws Exception {
        CategoryDTO inputDto = new CategoryDTO(null, "Gaming Gear", "Consoles and controllers");
        CategoryDTO savedDto = new CategoryDTO(10L, "Gaming Gear", "Consoles and controllers", 0L);

        when(categoryService.createCategory(any(CategoryDTO.class))).thenReturn(savedDto);

        mockMvc.perform(post("/api/categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(inputDto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(10))
                .andExpect(jsonPath("$.data.name").value("Gaming Gear"));
    }

    @Test
    @DisplayName("POST /api/categories - Should return 400 Bad Request when name is blank")
    void testCreateCategoryValidationFailure() throws Exception {
        CategoryDTO invalidDto = new CategoryDTO(null, "", "Missing name");

        mockMvc.perform(post("/api/categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidDto)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Validation Failed"));
    }

    @Test
    @DisplayName("DELETE /api/categories/{id} - Should delete category and return 200 OK")
    void testDeleteCategorySuccess() throws Exception {
        doNothing().when(categoryService).deleteCategory(1L);

        mockMvc.perform(delete("/api/categories/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Category deleted successfully"));
    }
}
