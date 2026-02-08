package kg.freelance.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import kg.freelance.dto.response.CategoryResponse;
import kg.freelance.exception.GlobalExceptionHandler;
import kg.freelance.exception.ResourceNotFoundException;
import kg.freelance.service.CategoryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("CategoryController Tests")
class CategoryControllerTest {

    @Mock
    private CategoryService categoryService;

    @InjectMocks
    private CategoryController categoryController;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(categoryController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    private CategoryResponse buildCategory(Long id, String name, String slug) {
        return CategoryResponse.builder()
                .id(id)
                .name(name)
                .slug(slug)
                .sortOrder(0)
                .build();
    }

    @Nested
    @DisplayName("GET /api/v1/categories")
    class GetAllCategoriesTests {

        @Test
        @DisplayName("Should return all categories")
        void shouldReturnAllCategories() throws Exception {
            CategoryResponse child = buildCategory(2L, "Frontend", "frontend");
            CategoryResponse root = CategoryResponse.builder()
                    .id(1L).name("Web Development").slug("web-development")
                    .children(List.of(child)).sortOrder(0).build();

            when(categoryService.getAllCategories()).thenReturn(List.of(root));

            mockMvc.perform(get("/api/v1/categories"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isArray())
                    .andExpect(jsonPath("$[0].id").value(1))
                    .andExpect(jsonPath("$[0].name").value("Web Development"))
                    .andExpect(jsonPath("$[0].children[0].name").value("Frontend"));
        }

        @Test
        @DisplayName("Should return empty list")
        void shouldReturnEmptyList() throws Exception {
            when(categoryService.getAllCategories()).thenReturn(List.of());

            mockMvc.perform(get("/api/v1/categories"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isEmpty());
        }
    }

    @Nested
    @DisplayName("GET /api/v1/categories/root")
    class GetRootCategoriesTests {

        @Test
        @DisplayName("Should return root categories")
        void shouldReturnRootCategories() throws Exception {
            when(categoryService.getRootCategories())
                    .thenReturn(List.of(buildCategory(1L, "Web", "web")));

            mockMvc.perform(get("/api/v1/categories/root"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].name").value("Web"));
        }
    }

    @Nested
    @DisplayName("GET /api/v1/categories/{id}")
    class GetCategoryByIdTests {

        @Test
        @DisplayName("Should return category by ID")
        void shouldReturnCategoryById() throws Exception {
            when(categoryService.getCategoryById(1L))
                    .thenReturn(buildCategory(1L, "Web Development", "web-development"));

            mockMvc.perform(get("/api/v1/categories/1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(1))
                    .andExpect(jsonPath("$.name").value("Web Development"));
        }

        @Test
        @DisplayName("Should return 404 when category not found")
        void shouldReturn404WhenNotFound() throws Exception {
            when(categoryService.getCategoryById(999L))
                    .thenThrow(new ResourceNotFoundException("Category", "id", 999L));

            mockMvc.perform(get("/api/v1/categories/999"))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("GET /api/v1/categories/slug/{slug}")
    class GetCategoryBySlugTests {

        @Test
        @DisplayName("Should return category by slug")
        void shouldReturnCategoryBySlug() throws Exception {
            when(categoryService.getCategoryBySlug("web-development"))
                    .thenReturn(buildCategory(1L, "Web Development", "web-development"));

            mockMvc.perform(get("/api/v1/categories/slug/web-development"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.slug").value("web-development"));
        }

        @Test
        @DisplayName("Should return 404 when slug not found")
        void shouldReturn404WhenSlugNotFound() throws Exception {
            when(categoryService.getCategoryBySlug("nonexistent"))
                    .thenThrow(new ResourceNotFoundException("Category", "slug", "nonexistent"));

            mockMvc.perform(get("/api/v1/categories/slug/nonexistent"))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("GET /api/v1/categories/{id}/subcategories")
    class GetSubcategoriesTests {

        @Test
        @DisplayName("Should return subcategories")
        void shouldReturnSubcategories() throws Exception {
            when(categoryService.getSubcategories(1L))
                    .thenReturn(List.of(
                            buildCategory(2L, "Frontend", "frontend"),
                            buildCategory(3L, "Backend", "backend")
                    ));

            mockMvc.perform(get("/api/v1/categories/1/subcategories"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isArray())
                    .andExpect(jsonPath("$.length()").value(2));
        }
    }
}
