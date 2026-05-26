package chillerguard.controller;

import chillerguard.entity.Building;
import chillerguard.repository.BuildingRepository;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

//building crud endpoints
@RestController
@RequestMapping("/api/v1/buildings")
public class BuildingController {

    private static final Logger logger = LoggerFactory.getLogger(BuildingController.class);

    private final BuildingRepository buildingRepository;

    public BuildingController(BuildingRepository buildingRepository) {
        this.buildingRepository = buildingRepository;
    }

    //get all buildings
    @GetMapping
    public ResponseEntity<List<Building>> getAllBuildings() {
        logger.debug("Fetching all buildings");
        return ResponseEntity.ok(buildingRepository.findAll());
    }

    //get building by id
    @GetMapping("/{id}")
    public ResponseEntity<Building> getBuildingById(@PathVariable UUID id) {
        logger.debug("Fetching building by ID: {}", id);
        Optional<Building> building = buildingRepository.findById(id);
        return building.map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    //get building by external id
    @GetMapping("/external/{externalId}")
    public ResponseEntity<Building> getBuildingByExternalId(@PathVariable String externalId) {
        logger.debug("Fetching building by external ID: {}", externalId);
        Optional<Building> building = buildingRepository.findByExternalId(externalId);
        return building.map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    //create building
    @PostMapping
    public ResponseEntity<Building> createBuilding(@Valid @RequestBody Building building) {
        logger.info("Creating new building: {}", building.getName());

        if (buildingRepository.existsByExternalId(building.getExternalId())) {
            return ResponseEntity.status(HttpStatus.CONFLICT).build();
        }

        Building saved = buildingRepository.save(building);
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    //update building
    @PutMapping("/{id}")
    public ResponseEntity<Building> updateBuilding(@PathVariable UUID id, @Valid @RequestBody Building building) {
        logger.info("Updating building: {}", id);

        if (!buildingRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }

        building.setId(id);
        Building updated = buildingRepository.save(building);
        return ResponseEntity.ok(updated);
    }

    //delete building
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteBuilding(@PathVariable UUID id) {
        logger.info("Deleting building: {}", id);

        if (!buildingRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }

        buildingRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    //filter by status
    @GetMapping("/status/{status}")
    public ResponseEntity<List<Building>> getBuildingsByStatus(@PathVariable Building.BuildingStatus status) {
        logger.debug("Fetching buildings by status: {}", status);
        return ResponseEntity.ok(buildingRepository.findByStatus(status));
    }

    //search by name
    @GetMapping("/search")
    public ResponseEntity<List<Building>> searchBuildings(@RequestParam String query) {
        logger.debug("Searching buildings with query: {}", query);
        return ResponseEntity.ok(buildingRepository.searchByName(query));
    }
}
