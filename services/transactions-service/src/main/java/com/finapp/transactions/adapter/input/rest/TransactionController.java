package com.finapp.transactions.adapter.input.rest;

import com.finapp.transactions.adapter.input.rest.dto.TransactionRequestDTO;
import com.finapp.transactions.adapter.input.rest.dto.TransactionResponseDTO;
import com.finapp.transactions.adapter.input.rest.mapper.TransactionDtoMapper;
import com.finapp.transactions.application.usecase.CreateTransaction;
import com.finapp.transactions.application.usecase.GetTransactionHistory;
import com.finapp.transactions.application.usecase.ReverseTransaction;
import com.finapp.transactions.domain.entity.Transaction;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * REST Controller — exposes the Transactions API endpoints.
 *
 * <p>Responsibilities:</p>
 * <ul>
 *   <li>Receives incoming DTOs from the HTTP layer.</li>
 *   <li>Extracts the authenticated {@code userId} from the request context
 *       (injected by {@link com.finapp.transactions.adapter.input.filter.JwtAuthenticationFilter}).</li>
 *   <li>Delegates business logic to the application-layer interactors.</li>
 *   <li>Transforms domain results (or exceptions) into proper HTTP responses.</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/v1/transactions")
public class TransactionController {

    private final CreateTransaction createTransaction;
    private final ReverseTransaction reverseTransaction;
    private final GetTransactionHistory getTransactionHistory;
    private final TransactionDtoMapper mapper;

    public TransactionController(CreateTransaction createTransaction,
                                 ReverseTransaction reverseTransaction,
                                 GetTransactionHistory getTransactionHistory,
                                 TransactionDtoMapper mapper) {
        this.createTransaction = createTransaction;
        this.reverseTransaction = reverseTransaction;
        this.getTransactionHistory = getTransactionHistory;
        this.mapper = mapper;
    }

    /**
     * POST /api/v1/transactions — Creates a new financial transaction.
     *
     * @return 201 Created with the persisted transaction
     */
    @PostMapping
    public ResponseEntity<TransactionResponseDTO> create(
            @RequestBody TransactionRequestDTO requestDTO,
            HttpServletRequest request) {

        String userId = extractUserId(request);
        CreateTransaction.Command command = mapper.toCommand(requestDTO, userId);
        Transaction created = createTransaction.execute(command);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(mapper.toResponseDTO(created));
    }

    /**
     * POST /api/v1/transactions/{id}/reverse — Reverses (annuls) a transaction.
     *
     * @return 201 Created with the compensating REVERSAL transaction
     */
    @PostMapping("/{id}/reverse")
    public ResponseEntity<TransactionResponseDTO> reverse(
            @PathVariable UUID id,
            HttpServletRequest request) {

        String userId = extractUserId(request);
        ReverseTransaction.Command command = new ReverseTransaction.Command(id, userId);
        Transaction reversal = reverseTransaction.execute(command);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(mapper.toResponseDTO(reversal));
    }

    /**
     * GET /api/v1/transactions — Returns the full transaction history for the
     * authenticated user.
     *
     * @return 200 OK with the list of transactions
     */
    @GetMapping
    public ResponseEntity<List<TransactionResponseDTO>> history(HttpServletRequest request) {
        String userId = extractUserId(request);
        GetTransactionHistory.Query query = new GetTransactionHistory.Query(userId);
        List<Transaction> transactions = getTransactionHistory.execute(query);

        return ResponseEntity.ok(mapper.toResponseDTOList(transactions));
    }

    /* ────────────────────────────────────────────────────────
       Exception handlers
       ──────────────────────────────────────────────────────── */

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> handleBadRequest(IllegalArgumentException ex) {
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(Map.of("error", ex.getMessage()));
    }

    @ExceptionHandler(SecurityException.class)
    public ResponseEntity<Map<String, String>> handleForbidden(SecurityException ex) {
        return ResponseEntity
                .status(HttpStatus.FORBIDDEN)
                .body(Map.of("error", ex.getMessage()));
    }

    /* ────────────────────────────────────────────────────────
       Private helpers
       ──────────────────────────────────────────────────────── */

    /**
     * Extracts the userId placed into the request attributes by the
     * {@link com.finapp.transactions.adapter.input.filter.JwtAuthenticationFilter}.
     */
    private String extractUserId(HttpServletRequest request) {
        Object userId = request.getAttribute("userId");
        if (userId == null) {
            throw new SecurityException("Missing authenticated userId in request context");
        }
        return userId.toString();
    }
}
