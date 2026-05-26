package chillerguard.controller;

import chillerguard.entity.ChillerUnit;
import chillerguard.repository.ChillerUnitRepository;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

//chiller unit crud endpoints
@RestController
@RequestMapping("/api/v1/chiller-units")
public class ChillerUnitController {

    private static final Logger logger = LoggerFactory.getLogger(ChillerUnitController.class);

    private final ChillerUnitRepository chillerUnitRepository;

    public ChillerUnitController(ChillerUnitRepository chillerUnitRepository) {
        this.chillerUnitRepository = chillerUnitRepository;
    }

    //get all units
    @GetMapping
    public ResponseEntity<List<ChillerUnit>> getAllChillerUnits() {
        logger.debug("Fetching all chiller units");
        return ResponseEntity.ok(chillerUnitRepository.findAll());
    }

    //get unit by id
    @GetMapping("/{id}")
    public ResponseEntity<ChillerUnit> getChillerUnitById(@PathVariable UUID id) {
        logger.debug("Fetching chiller unit by ID: {}", id);
        Optional<ChillerUnit> unit = chillerUnitRepository.findById(id);
        return unit.map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    //get unit by external id
    @GetMapping("/external/{externalId}")
    public ResponseEntity<ChillerUnit> getChillerUnitByExternalId(@PathVariable String externalId) {
        logger.debug("Fetching chiller unit by external ID: {}", externalId);
        Optional<ChillerUnit> unit = chillerUnitRepository.findByExternalId(externalId);
        return unit.map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    //get units for a building
    @GetMapping("/building/{buildingId}")
    public ResponseEntity<List<ChillerUnit>> getChillerUnitsByBuilding(@PathVariable UUID buildingId) {
        logger.debug("Fetching chiller units for building: {}", buildingId);
        return ResponseEntity.ok(chillerUnitRepository.findByBuildingId(buildingId));
    }

    //create unit
    @PostMapping
    public ResponseEntity<ChillerUnit> createChillerUnit(@Valid @RequestBody ChillerUnit unit) {
        logger.info("Creating new chiller unit: {}", unit.getName());

        if (chillerUnitRepository.existsByExternalId(unit.getExternalId())) {
            return ResponseEntity.status(HttpStatus.CONFLICT).build();
        }

        ChillerUnit saved = chillerUnitRepository.save(unit);
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    //update unit
    @PutMapping("/{id}")
    public ResponseEntity<ChillerUnit> updateChillerUnit(@PathVariable UUID id, @Valid @RequestBody ChillerUnit unit) {
        logger.info("Updating chiller unit: {}", id);

        if (!chillerUnitRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }

        unit.setId(id);
        ChillerUnit updated = chillerUnitRepository.save(unit);
        return ResponseEntity.ok(updated);
    }

    //delete unit
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteChillerUnit(@PathVariable UUID id) {
        logger.info("Deleting chiller unit: {}", id);

        if (!chillerUnitRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }

        chillerUnitRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    //filter by status
    @GetMapping("/status/{status}")
    public ResponseEntity<List<ChillerUnit>> getChillerUnitsByStatus(@PathVariable ChillerUnit.UnitStatus status) {
        logger.debug("Fetching chiller units by status: {}", status);
        return ResponseEntity.ok(chillerUnitRepository.findByStatus(status));
    }

    //filter by building and status
    @GetMapping("/building/{buildingId}/status/{status}")
    public ResponseEntity<List<ChillerUnit>> getChillerUnitsByBuildingAndStatus(
            @PathVariable UUID buildingId,
            @PathVariable ChillerUnit.UnitStatus status) {
        logger.debug("Fetching chiller units for building {} with status {}", buildingId, status);
        return ResponseEntity.ok(chillerUnitRepository.findByBuildingIdAndStatus(buildingId, status));
    }

    //search by name
    @GetMapping("/search")
    public ResponseEntity<List<ChillerUnit>> searchChillerUnits(@RequestParam String query) {
        logger.debug("Searching chiller units with query: {}", query);
        return ResponseEntity.ok(chillerUnitRepository.searchByName(query));
    }
}
