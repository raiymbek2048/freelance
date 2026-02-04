package kg.freelance.repository;

import kg.freelance.entity.ChatRoom;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ChatRoomRepository extends JpaRepository<ChatRoom, Long> {

    Optional<ChatRoom> findByOrderId(Long orderId);

    Optional<ChatRoom> findByOrderIdAndExecutorId(Long orderId, Long executorId);

    @Query("""
            SELECT c FROM ChatRoom c
            WHERE c.client.id = :userId OR c.executor.id = :userId
            ORDER BY c.lastMessageAt DESC NULLS LAST
            """)
    Page<ChatRoom> findByUserId(@Param("userId") Long userId, Pageable pageable);

    @Query("""
            SELECT CASE WHEN COUNT(c) > 0 THEN true ELSE false END
            FROM ChatRoom c
            WHERE c.id = :chatRoomId
            AND (c.client.id = :userId OR c.executor.id = :userId)
            """)
    boolean isUserParticipant(@Param("chatRoomId") Long chatRoomId, @Param("userId") Long userId);
}
