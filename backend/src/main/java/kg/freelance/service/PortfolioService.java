package kg.freelance.service;

import kg.freelance.dto.request.PortfolioRequest;
import kg.freelance.dto.response.PortfolioResponse;

import java.util.List;

public interface PortfolioService {

    List<PortfolioResponse> getMyPortfolio(Long userId);

    PortfolioResponse addPortfolioItem(Long userId, PortfolioRequest request);

    PortfolioResponse updatePortfolioItem(Long userId, Long itemId, PortfolioRequest request);

    void deletePortfolioItem(Long userId, Long itemId);

    void reorderPortfolio(Long userId, List<Long> itemIds);
}
