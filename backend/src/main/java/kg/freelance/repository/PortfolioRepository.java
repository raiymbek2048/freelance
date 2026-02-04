package kg.freelance.repository;

import kg.freelance.entity.Portfolio;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PortfolioRepository extends JpaRepository<Portfolio, Long> {

    List<Portfolio> findByExecutorIdOrderBySortOrder(Long executorId);

    long countByExecutorId(Long executorId);
}
