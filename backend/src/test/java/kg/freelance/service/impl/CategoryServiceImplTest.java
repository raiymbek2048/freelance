package kg.freelance.service.impl;

import kg.freelance.dto.response.CategoryResponse;
import kg.freelance.entity.Category;
import kg.freelance.exception.ResourceNotFoundException;
import kg.freelance.repository.CategoryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("CategoryService Tests")
class CategoryServiceImplTest {

    @Mock
    private CategoryRepository categoryRepository;

    @InjectMocks
    private CategoryServiceImpl categoryService;

    private Category rootCategory;
    private Category childCategory1;
    private Category childCategory2;
    private Category inactiveChild;

    @BeforeEach
    void setUp() {
        rootCategory = Category.builder()
                .id(1L)
                .name("Web Development")
                .slug("web-development")
                .description("Web dev services")
                .iconUrl("/icons/web.png")
                .sortOrder(0)
                .active(true)
                .children(new ArrayList<>())
                .build();

        childCategory1 = Category.builder()
                .id(2L)
                .name("Frontend")
                .slug("frontend")
                .description("Frontend development")
                .parent(rootCategory)
                .sortOrder(0)
                .active(true)
                .children(new ArrayList<>())
                .build();

        childCategory2 = Category.builder()
                .id(3L)
                .name("Backend")
                .slug("backend")
                .description("Backend development")
                .parent(rootCategory)
                .sortOrder(1)
                .active(true)
                .children(new ArrayList<>())
                .build();

        inactiveChild = Category.builder()
                .id(4L)
                .name("Inactive Category")
                .slug("inactive")
                .parent(rootCategory)
                .sortOrder(2)
                .active(false)
                .children(new ArrayList<>())
                .build();

        rootCategory.getChildren().addAll(List.of(childCategory1, childCategory2, inactiveChild));
    }

    @Nested
    @DisplayName("Get All Categories Tests")
    class GetAllCategoriesTests {

        @Test
        @DisplayName("Should return all root categories with active children")
        void shouldReturnAllRootCategoriesWithActiveChildren() {
            // Given
            when(categoryRepository.findByParentIsNullAndActiveTrueOrderBySortOrder())
                    .thenReturn(List.of(rootCategory));

            // When
            List<CategoryResponse> result = categoryService.getAllCategories();

            // Then
            assertThat(result).hasSize(1);
            CategoryResponse root = result.get(0);
            assertThat(root.getId()).isEqualTo(1L);
            assertThat(root.getName()).isEqualTo("Web Development");
            assertThat(root.getSlug()).isEqualTo("web-development");
            assertThat(root.getChildren()).hasSize(2);
            assertThat(root.getChildren()).extracting(CategoryResponse::getName)
                    .containsExactly("Frontend", "Backend");
        }

        @Test
        @DisplayName("Should filter out inactive children")
        void shouldFilterOutInactiveChildren() {
            // Given
            when(categoryRepository.findByParentIsNullAndActiveTrueOrderBySortOrder())
                    .thenReturn(List.of(rootCategory));

            // When
            List<CategoryResponse> result = categoryService.getAllCategories();

            // Then
            CategoryResponse root = result.get(0);
            assertThat(root.getChildren()).hasSize(2);
            assertThat(root.getChildren()).extracting(CategoryResponse::getName)
                    .doesNotContain("Inactive Category");
        }

        @Test
        @DisplayName("Should return empty list when no categories")
        void shouldReturnEmptyListWhenNoCategories() {
            // Given
            when(categoryRepository.findByParentIsNullAndActiveTrueOrderBySortOrder())
                    .thenReturn(List.of());

            // When
            List<CategoryResponse> result = categoryService.getAllCategories();

            // Then
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("Should set children to null when no active children")
        void shouldSetChildrenToNullWhenNoActiveChildren() {
            // Given
            Category categoryWithOnlyInactiveChildren = Category.builder()
                    .id(5L)
                    .name("Empty Category")
                    .slug("empty")
                    .sortOrder(0)
                    .active(true)
                    .children(new ArrayList<>())
                    .build();
            Category inactiveOnly = Category.builder()
                    .id(6L)
                    .name("Inactive")
                    .slug("inactive-only")
                    .parent(categoryWithOnlyInactiveChildren)
                    .active(false)
                    .children(new ArrayList<>())
                    .build();
            categoryWithOnlyInactiveChildren.getChildren().add(inactiveOnly);

            when(categoryRepository.findByParentIsNullAndActiveTrueOrderBySortOrder())
                    .thenReturn(List.of(categoryWithOnlyInactiveChildren));

            // When
            List<CategoryResponse> result = categoryService.getAllCategories();

            // Then
            assertThat(result).hasSize(1);
            assertThat(result.get(0).getChildren()).isNull();
        }
    }

    @Nested
    @DisplayName("Get Root Categories Tests")
    class GetRootCategoriesTests {

        @Test
        @DisplayName("Should return root categories without children")
        void shouldReturnRootCategoriesWithoutChildren() {
            // Given
            when(categoryRepository.findByParentIsNullAndActiveTrueOrderBySortOrder())
                    .thenReturn(List.of(rootCategory));

            // When
            List<CategoryResponse> result = categoryService.getRootCategories();

            // Then
            assertThat(result).hasSize(1);
            CategoryResponse root = result.get(0);
            assertThat(root.getId()).isEqualTo(1L);
            assertThat(root.getName()).isEqualTo("Web Development");
            assertThat(root.getParentId()).isNull();
            assertThat(root.getChildren()).isNull();
        }
    }

    @Nested
    @DisplayName("Get Category By ID Tests")
    class GetCategoryByIdTests {

        @Test
        @DisplayName("Should return category with children by ID")
        void shouldReturnCategoryWithChildrenById() {
            // Given
            when(categoryRepository.findById(1L)).thenReturn(Optional.of(rootCategory));

            // When
            CategoryResponse result = categoryService.getCategoryById(1L);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(1L);
            assertThat(result.getName()).isEqualTo("Web Development");
            assertThat(result.getChildren()).hasSize(2);
        }

        @Test
        @DisplayName("Should throw exception when category not found by ID")
        void shouldThrowExceptionWhenCategoryNotFoundById() {
            // Given
            when(categoryRepository.findById(999L)).thenReturn(Optional.empty());

            // When/Then
            assertThatThrownBy(() -> categoryService.getCategoryById(999L))
                    .isInstanceOf(ResourceNotFoundException.class);
        }

        @Test
        @DisplayName("Should set parentId for child category")
        void shouldSetParentIdForChildCategory() {
            // Given
            when(categoryRepository.findById(2L)).thenReturn(Optional.of(childCategory1));

            // When
            CategoryResponse result = categoryService.getCategoryById(2L);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getParentId()).isEqualTo(1L);
        }
    }

    @Nested
    @DisplayName("Get Category By Slug Tests")
    class GetCategoryBySlugTests {

        @Test
        @DisplayName("Should return category by slug")
        void shouldReturnCategoryBySlug() {
            // Given
            when(categoryRepository.findBySlug("web-development"))
                    .thenReturn(Optional.of(rootCategory));

            // When
            CategoryResponse result = categoryService.getCategoryBySlug("web-development");

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getSlug()).isEqualTo("web-development");
            assertThat(result.getName()).isEqualTo("Web Development");
        }

        @Test
        @DisplayName("Should throw exception when category not found by slug")
        void shouldThrowExceptionWhenCategoryNotFoundBySlug() {
            // Given
            when(categoryRepository.findBySlug("nonexistent")).thenReturn(Optional.empty());

            // When/Then
            assertThatThrownBy(() -> categoryService.getCategoryBySlug("nonexistent"))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("Get Subcategories Tests")
    class GetSubcategoriesTests {

        @Test
        @DisplayName("Should return subcategories by parent ID")
        void shouldReturnSubcategoriesByParentId() {
            // Given
            when(categoryRepository.findByParentIdAndActiveTrueOrderBySortOrder(1L))
                    .thenReturn(List.of(childCategory1, childCategory2));

            // When
            List<CategoryResponse> result = categoryService.getSubcategories(1L);

            // Then
            assertThat(result).hasSize(2);
            assertThat(result).extracting(CategoryResponse::getName)
                    .containsExactly("Frontend", "Backend");
            assertThat(result).extracting(CategoryResponse::getParentId)
                    .containsOnly(1L);
        }

        @Test
        @DisplayName("Should return empty list when no subcategories")
        void shouldReturnEmptyListWhenNoSubcategories() {
            // Given
            when(categoryRepository.findByParentIdAndActiveTrueOrderBySortOrder(999L))
                    .thenReturn(List.of());

            // When
            List<CategoryResponse> result = categoryService.getSubcategories(999L);

            // Then
            assertThat(result).isEmpty();
        }
    }
}
