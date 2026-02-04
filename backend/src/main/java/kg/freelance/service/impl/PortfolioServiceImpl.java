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
import kg.freelance.service.PortfolioService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PortfolioServiceImpl implements PortfolioService {

    private final PortfolioRepository portfolioRepository;
    private final ExecutorProfileRepository executorProfileRepository;
    private final CategoryRepository categoryRepository;

    @Override
    @Transactional(readOnly = true)
    public List<PortfolioResponse> getMyPortfolio(Long userId) {
        return portfolioRepository.findByExecutorIdOrderBySortOrder(userId).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public PortfolioResponse addPortfolioItem(Long userId, PortfolioRequest request) {
        ExecutorProfile executor = executorProfileRepository.findById(userId)
                .orElseThrow(() -> new BadRequestException("You need to create an executor profile first"));

        Category category = null;
        if (request.getCategoryId() != null) {
            category = categoryRepository.findById(request.getCategoryId())
                    .orElseThrow(() -> new ResourceNotFoundException("Category", "id", request.getCategoryId()));
        }

        int maxSortOrder = (int) portfolioRepository.countByExecutorId(userId);

        Portfolio portfolio = Portfolio.builder()
                .executor(executor)
                .title(request.getTitle())
                .description(request.getDescription())
                .category(category)
                .images(request.getImages())
                .externalLink(request.getExternalLink())
                .sortOrder(maxSortOrder)
                .build();

        portfolio = portfolioRepository.save(portfolio);
        return mapToResponse(portfolio);
    }

    @Override
    @Transactional
    public PortfolioResponse updatePortfolioItem(Long userId, Long itemId, PortfolioRequest request) {
        Portfolio portfolio = portfolioRepository.findById(itemId)
                .orElseThrow(() -> new ResourceNotFoundException("Portfolio item", "id", itemId));

        if (!portfolio.getExecutor().getId().equals(userId)) {
            throw new ForbiddenException("You can only update your own portfolio items");
        }

        if (request.getTitle() != null) {
            portfolio.setTitle(request.getTitle());
        }
        if (request.getDescription() != null) {
            portfolio.setDescription(request.getDescription());
        }
        if (request.getCategoryId() != null) {
            Category category = categoryRepository.findById(request.getCategoryId())
                    .orElseThrow(() -> new ResourceNotFoundException("Category", "id", request.getCategoryId()));
            portfolio.setCategory(category);
        }
        if (request.getImages() != null) {
            portfolio.setImages(request.getImages());
        }
        if (request.getExternalLink() != null) {
            portfolio.setExternalLink(request.getExternalLink());
        }

        portfolio = portfolioRepository.save(portfolio);
        return mapToResponse(portfolio);
    }

    @Override
    @Transactional
    public void deletePortfolioItem(Long userId, Long itemId) {
        Portfolio portfolio = portfolioRepository.findById(itemId)
                .orElseThrow(() -> new ResourceNotFoundException("Portfolio item", "id", itemId));

        if (!portfolio.getExecutor().getId().equals(userId)) {
            throw new ForbiddenException("You can only delete your own portfolio items");
        }

        portfolioRepository.delete(portfolio);
    }

    @Override
    @Transactional
    public void reorderPortfolio(Long userId, List<Long> itemIds) {
        List<Portfolio> items = portfolioRepository.findByExecutorIdOrderBySortOrder(userId);

        for (int i = 0; i < itemIds.size(); i++) {
            Long itemId = itemIds.get(i);
            for (Portfolio item : items) {
                if (item.getId().equals(itemId)) {
                    item.setSortOrder(i);
                    break;
                }
            }
        }

        portfolioRepository.saveAll(items);
    }

    private PortfolioResponse mapToResponse(Portfolio portfolio) {
        return PortfolioResponse.builder()
                .id(portfolio.getId())
                .title(portfolio.getTitle())
                .description(portfolio.getDescription())
                .categoryId(portfolio.getCategory() != null ? portfolio.getCategory().getId() : null)
                .categoryName(portfolio.getCategory() != null ? portfolio.getCategory().getName() : null)
                .images(portfolio.getImages())
                .externalLink(portfolio.getExternalLink())
                .sortOrder(portfolio.getSortOrder())
                .createdAt(portfolio.getCreatedAt())
                .build();
    }
}
