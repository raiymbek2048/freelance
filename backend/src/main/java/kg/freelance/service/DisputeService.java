package kg.freelance.service;

import kg.freelance.dto.request.DisputeEvidenceRequest;
import kg.freelance.dto.request.OpenDisputeRequest;
import kg.freelance.dto.request.ResolveDisputeRequest;
import kg.freelance.dto.response.DisputeEvidenceResponse;
import kg.freelance.dto.response.DisputeResponse;
import kg.freelance.dto.response.MessageResponse;
import kg.freelance.dto.response.PageResponse;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface DisputeService {

    // User actions
    DisputeResponse openDispute(Long userId, Long orderId, OpenDisputeRequest request);

    DisputeResponse getDisputeByOrderId(Long orderId, Long userId);

    DisputeResponse getDisputeById(Long disputeId, Long userId);

    DisputeEvidenceResponse addEvidence(Long disputeId, Long userId, DisputeEvidenceRequest request);

    List<DisputeEvidenceResponse> getEvidence(Long disputeId, Long userId);

    // Admin actions
    PageResponse<DisputeResponse> getActiveDisputes(Pageable pageable);

    PageResponse<DisputeResponse> getAllDisputes(String status, Pageable pageable);

    DisputeResponse getDisputeForAdmin(Long disputeId);

    DisputeResponse takeDispute(Long disputeId, Long adminId);

    DisputeResponse addAdminNotes(Long disputeId, Long adminId, String notes);

    DisputeResponse resolveDispute(Long disputeId, Long adminId, ResolveDisputeRequest request);

    PageResponse<MessageResponse> getDisputeMessages(Long disputeId, Pageable pageable);
}
