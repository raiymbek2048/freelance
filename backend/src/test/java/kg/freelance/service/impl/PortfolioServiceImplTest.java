package kg.freelance.service.impl;

import kg.freelance.dto.request.PortfolioRequest;
import kg.freelance.dto.response.PortfolioResponse;
import kg.freelance.entity.Category;
import kg.freelance.entity.ExecutorProfile;
import kg.freelance.entity.Portfolio;
import kg.freelance.exception.BadRequestException;
import kg.freelance.exception.ForbiddenException;
import kg.freelance.exception.ResourceNotFoundException;
import kg.freelance.repository.CategoryRepository;
import kg.freelance.repository.ExecutorProfileRepository;
import kg.freelance.repository.PortfolioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("PortfolioService Tests")
class PortfolioServiceImplTest {

    @Mock
    private PortfolioRepository portfolioRepository;

    @Mock
    private ExecutorProfileRepository executorProfileRepository;

    @Mock
    private CategoryRepository categoryRepository;

    @InjectMocks
    private PortfolioServiceImpl portfolioService;

    private ExecutorProfile executor;
    private Category category;
    private Portfolio portfolio;

    @BeforeEach
    void setUp() {
        executor = new ExecutorProfile();
        executor.setId(1L);

        category = Category.builder()
                .id(10L)
                .name("Web Development")
                .slug("web-development")
                .build();

        portfolio = Portfolio.builder()
                .id(1L)
                .executor(executor)
                .title("My Project")
                .description("A great project")
                .category(category)
                .images(List.of("/images/1.jpg", "/images/2.jpg"))
                .externalLink("https://example.com")
                .sortOrder(0)
                .createdAt(LocalDateTime.now())
                .build();
    }

    @Nested
    @DisplayName("Get My Portfolio Tests")
    class GetMyPortfolioTests {

        @Test
        @DisplayName("Should return user's portfolio items")
        void shouldReturnUsersPortfolioItems() {
            // Given
            when(portfolioRepository.findByExecutorIdOrderBySortOrder(1L))
                    .thenReturn(List.of(portfolio));

            // When
            List<PortfolioResponse> result = portfolioService.getMyPortfolio(1L);

            // Then
            assertThat(result).hasSize(1);
            PortfolioResponse item = result.get(0);
            assertThat(item.getId()).isEqualTo(1L);
            assertThat(item.getTitle()).isEqualTo("My Project");
            assertThat(item.getDescription()).isEqualTo("A great project");
            assertThat(item.getCategoryId()).isEqualTo(10L);
            assertThat(item.getCategoryName()).isEqualTo("Web Development");
            assertThat(item.getImages()).hasSize(2);
            assertThat(item.getExternalLink()).isEqualTo("https://example.com");
        }

        @Test
        @DisplayName("Should return empty list when no portfolio items")
        void shouldReturnEmptyListWhenNoPortfolioItems() {
            // Given
            when(portfolioRepository.findByExecutorIdOrderBySortOrder(1L))
                    .thenReturn(List.of());

            // When
            List<PortfolioResponse> result = portfolioService.getMyPortfolio(1L);

            // Then
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("Should handle portfolio item with null category")
        void shouldHandlePortfolioItemWithNullCategory() {
            // Given
            Portfolio noCategoryPortfolio = Portfolio.builder()
                    .id(2L)
                    .executor(executor)
                    .title("No Category Project")
                    .category(null)
                    .images(List.of())
                    .sortOrder(0)
                    .createdAt(LocalDateTime.now())
                    .build();

            when(portfolioRepository.findByExecutorIdOrderBySortOrder(1L))
                    .thenReturn(List.of(noCategoryPortfolio));

            // When
            List<PortfolioResponse> result = portfolioService.getMyPortfolio(1L);

            // Then
            assertThat(result).hasSize(1);
            assertThat(result.get(0).getCategoryId()).isNull();
            assertThat(result.get(0).getCategoryName()).isNull();
        }
    }

    @Nested
    @DisplayName("Add Portfolio Item Tests")
    class AddPortfolioItemTests {

        @Test
        @DisplayName("Should add portfolio item with category")
        void shouldAddPortfolioItemWithCategory() {
            // Given
            PortfolioRequest request = new PortfolioRequest();
            request.setTitle("New Project");
            request.setDescription("Description");
            request.setCategoryId(10L);
            request.setImages(List.of("/images/new.jpg"));
            request.setExternalLink("https://new-project.com");

            when(executorProfileRepository.findById(1L)).thenReturn(Optional.of(executor));
            when(categoryRepository.findById(10L)).thenReturn(Optional.of(category));
            when(portfolioRepository.countByExecutorId(1L)).thenReturn(3L);
            when(portfolioRepository.save(any(Portfolio.class))).thenAnswer(inv -> {
                Portfolio p = inv.getArgument(0);
                p.setId(2L);
                p.setCreatedAt(LocalDateTime.now());
                return p;
            });

            // When
            PortfolioResponse result = portfolioService.addPortfolioItem(1L, request);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getTitle()).isEqualTo("New Project");
            assertThat(result.getCategoryId()).isEqualTo(10L);
            verify(portfolioRepository).save(argThat(p ->
                    p.getSortOrder() == 3 && p.getTitle().equals("New Project")));
        }

        @Test
        @DisplayName("Should add portfolio item without category")
        void shouldAddPortfolioItemWithoutCategory() {
            // Given
            PortfolioRequest request = new PortfolioRequest();
            request.setTitle("No Category Project");

            when(executorProfileRepository.findById(1L)).thenReturn(Optional.of(executor));
            when(portfolioRepository.countByExecutorId(1L)).thenReturn(0L);
            when(portfolioRepository.save(any(Portfolio.class))).thenAnswer(inv -> {
                Portfolio p = inv.getArgument(0);
                p.setId(2L);
                p.setCreatedAt(LocalDateTime.now());
                return p;
            });

            // When
            PortfolioResponse result = portfolioService.addPortfolioItem(1L, request);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getCategoryId()).isNull();
            verify(categoryRepository, never()).findById(anyLong());
        }

        @Test
        @DisplayName("Should throw exception when no executor profile")
        void shouldThrowExceptionWhenNoExecutorProfile() {
            // Given
            PortfolioRequest request = new PortfolioRequest();
            request.setTitle("Project");

            when(executorProfileRepository.findById(1L)).thenReturn(Optional.empty());

            // When/Then
            assertThatThrownBy(() -> portfolioService.addPortfolioItem(1L, request))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessage("You need to create an executor profile first");
        }

        @Test
        @DisplayName("Should throw exception when category not found")
        void shouldThrowExceptionWhenCategoryNotFound() {
            // Given
            PortfolioRequest request = new PortfolioRequest();
            request.setTitle("Project");
            request.setCategoryId(999L);

            when(executorProfileRepository.findById(1L)).thenReturn(Optional.of(executor));
            when(categoryRepository.findById(999L)).thenReturn(Optional.empty());

            // When/Then
            assertThatThrownBy(() -> portfolioService.addPortfolioItem(1L, request))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("Update Portfolio Item Tests")
    class UpdatePortfolioItemTests {

        @Test
        @DisplayName("Should update portfolio item successfully")
        void shouldUpdatePortfolioItemSuccessfully() {
            // Given
            PortfolioRequest request = new PortfolioRequest();
            request.setTitle("Updated Title");
            request.setDescription("Updated Description");

            when(portfolioRepository.findById(1L)).thenReturn(Optional.of(portfolio));
            when(portfolioRepository.save(any(Portfolio.class))).thenReturn(portfolio);

            // When
            PortfolioResponse result = portfolioService.updatePortfolioItem(1L, 1L, request);

            // Then
            assertThat(result).isNotNull();
            assertThat(portfolio.getTitle()).isEqualTo("Updated Title");
            assertThat(portfolio.getDescription()).isEqualTo("Updated Description");
            verify(portfolioRepository).save(portfolio);
        }

        @Test
        @DisplayName("Should update only provided fields")
        void shouldUpdateOnlyProvidedFields() {
            // Given
            PortfolioRequest request = new PortfolioRequest();
            request.setTitle("Updated Title");
            // description, categoryId, images, externalLink are null

            when(portfolioRepository.findById(1L)).thenReturn(Optional.of(portfolio));
            when(portfolioRepository.save(any(Portfolio.class))).thenReturn(portfolio);

            // When
            portfolioService.updatePortfolioItem(1L, 1L, request);

            // Then
            assertThat(portfolio.getTitle()).isEqualTo("Updated Title");
            assertThat(portfolio.getDescription()).isEqualTo("A great project"); // unchanged
        }

        @Test
        @DisplayName("Should update category when provided")
        void shouldUpdateCategoryWhenProvided() {
            // Given
            Category newCategory = Category.builder()
                    .id(20L)
                    .name("Design")
                    .slug("design")
                    .build();

            PortfolioRequest request = new PortfolioRequest();
            request.setCategoryId(20L);

            when(portfolioRepository.findById(1L)).thenReturn(Optional.of(portfolio));
            when(categoryRepository.findById(20L)).thenReturn(Optional.of(newCategory));
            when(portfolioRepository.save(any(Portfolio.class))).thenReturn(portfolio);

            // When
            portfolioService.updatePortfolioItem(1L, 1L, request);

            // Then
            assertThat(portfolio.getCategory()).isEqualTo(newCategory);
        }

        @Test
        @DisplayName("Should throw exception when portfolio item not found")
        void shouldThrowExceptionWhenPortfolioItemNotFound() {
            // Given
            PortfolioRequest request = new PortfolioRequest();
            when(portfolioRepository.findById(999L)).thenReturn(Optional.empty());

            // When/Then
            assertThatThrownBy(() -> portfolioService.updatePortfolioItem(1L, 999L, request))
                    .isInstanceOf(ResourceNotFoundException.class);
        }

        @Test
        @DisplayName("Should throw exception when not the owner")
        void shouldThrowExceptionWhenNotTheOwner() {
            // Given
            PortfolioRequest request = new PortfolioRequest();
            request.setTitle("Hacked");

            when(portfolioRepository.findById(1L)).thenReturn(Optional.of(portfolio));

            // When/Then — user 999 is not the owner
            assertThatThrownBy(() -> portfolioService.updatePortfolioItem(999L, 1L, request))
                    .isInstanceOf(ForbiddenException.class)
                    .hasMessage("You can only update your own portfolio items");
        }
    }

    @Nested
    @DisplayName("Delete Portfolio Item Tests")
    class DeletePortfolioItemTests {

        @Test
        @DisplayName("Should delete portfolio item successfully")
        void shouldDeletePortfolioItemSuccessfully() {
            // Given
            when(portfolioRepository.findById(1L)).thenReturn(Optional.of(portfolio));

            // When
            portfolioService.deletePortfolioItem(1L, 1L);

            // Then
            verify(portfolioRepository).delete(portfolio);
        }

        @Test
        @DisplayName("Should throw exception when portfolio item not found")
        void shouldThrowExceptionWhenItemNotFoundForDelete() {
            // Given
            when(portfolioRepository.findById(999L)).thenReturn(Optional.empty());

            // When/Then
            assertThatThrownBy(() -> portfolioService.deletePortfolioItem(1L, 999L))
                    .isInstanceOf(ResourceNotFoundException.class);
        }

        @Test
        @DisplayName("Should throw exception when not the owner for delete")
        void shouldThrowExceptionWhenNotOwnerForDelete() {
            // Given
            when(portfolioRepository.findById(1L)).thenReturn(Optional.of(portfolio));

            // When/Then
            assertThatThrownBy(() -> portfolioService.deletePortfolioItem(999L, 1L))
                    .isInstanceOf(ForbiddenException.class)
                    .hasMessage("You can only delete your own portfolio items");
        }
    }

    @Nested
    @DisplayName("Reorder Portfolio Tests")
    class ReorderPortfolioTests {

        @Test
        @DisplayName("Should reorder portfolio items")
        void shouldReorderPortfolioItems() {
            // Given
            Portfolio item1 = Portfolio.builder().id(1L).executor(executor).sortOrder(0).build();
            Portfolio item2 = Portfolio.builder().id(2L).executor(executor).sortOrder(1).build();
            Portfolio item3 = Portfolio.builder().id(3L).executor(executor).sortOrder(2).build();

            when(portfolioRepository.findByExecutorIdOrderBySortOrder(1L))
                    .thenReturn(new ArrayList<>(List.of(item1, item2, item3)));

            // When — reverse order
            portfolioService.reorderPortfolio(1L, List.of(3L, 1L, 2L));

            // Then
            assertThat(item3.getSortOrder()).isEqualTo(0);
            assertThat(item1.getSortOrder()).isEqualTo(1);
            assertThat(item2.getSortOrder()).isEqualTo(2);
            verify(portfolioRepository).saveAll(anyList());
        }

        @Test
        @DisplayName("Should handle empty reorder list")
        void shouldHandleEmptyReorderList() {
            // Given
            when(portfolioRepository.findByExecutorIdOrderBySortOrder(1L))
                    .thenReturn(List.of());

            // When
            portfolioService.reorderPortfolio(1L, List.of());

            // Then
            verify(portfolioRepository).saveAll(anyList());
        }
    }
}
