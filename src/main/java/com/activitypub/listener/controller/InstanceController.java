package com.activitypub.listener.controller;

import com.activitypub.listener.dto.ApiResponse;
import com.activitypub.listener.dto.CreateFediverseInstanceDTO;
import com.activitypub.listener.dto.FediverseInstanceDTO;
import com.activitypub.listener.dto.PaginationResponse;
import com.activitypub.listener.service.InstanceService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/instances")
@RequiredArgsConstructor
public class InstanceController {

    private final InstanceService instanceService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<FediverseInstanceDTO>>> list(
            @RequestParam(defaultValue = "false") boolean paged,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int perPage) {
        if (paged) {
            Pageable pageable = PageRequest.of(page - 1, Math.min(perPage, 100));
            var p = instanceService.list(pageable);
            PaginationResponse<FediverseInstanceDTO> pr = PaginationResponse.<FediverseInstanceDTO>builder()
                    .data(p.getContent())
                    .pagination(PaginationResponse.PaginationInfo.builder()
                            .page(page)
                            .perPage(perPage)
                            .total(p.getTotalElements())
                            .totalPages(p.getTotalPages())
                            .build())
                    .build();
            return ResponseEntity.ok(ApiResponse.<PaginationResponse<FediverseInstanceDTO>>builder()
                    .data(pr)
                    .build());
        }
        List<FediverseInstanceDTO> data = instanceService.listAll();
        return ResponseEntity.ok(ApiResponse.<List<FediverseInstanceDTO>>builder().data(data).build());
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<FediverseInstanceDTO>> get(@PathVariable String id) {
        FediverseInstanceDTO dto = instanceService.getById(id);
        return ResponseEntity.ok(ApiResponse.<FediverseInstanceDTO>builder().data(dto).build());
    }

    @PostMapping
    public ResponseEntity<ApiResponse<FediverseInstanceDTO>> create(
            @Valid @RequestBody CreateFediverseInstanceDTO dto) {
        FediverseInstanceDTO created = instanceService.create(dto);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.<FediverseInstanceDTO>builder()
                        .data(created)
                        .message("Instance created successfully")
                        .build());
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<FediverseInstanceDTO>> update(
            @PathVariable String id,
            @Valid @RequestBody CreateFediverseInstanceDTO dto) {
        FediverseInstanceDTO updated = instanceService.update(id, dto);
        return ResponseEntity.ok(ApiResponse.<FediverseInstanceDTO>builder()
                .data(updated)
                .message("Instance updated successfully")
                .build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable String id) {
        instanceService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/health")
    public ResponseEntity<ApiResponse<Map<String, Object>>> health(@PathVariable String id) {
        Map<String, Object> health = instanceService.healthCheck(id);
        return ResponseEntity.ok(ApiResponse.<Map<String, Object>>builder().data(health).build());
    }
}
