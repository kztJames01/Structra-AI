package chillerguard.repository;

import chillerguard.entity.ChillerUnit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

//chiller unit repo
@Repository
public interface ChillerUnitRepository extends JpaRepository<ChillerUnit, UUID> {

    //find by external id
    Optional<ChillerUnit> findByExternalId(String externalId);

    //check external id exists
    boolean existsByExternalId(String externalId);

    //units in a building
    List<ChillerUnit> findByBuildingId(UUID buildingId);

    //filter by status
    List<ChillerUnit> findByStatus(ChillerUnit.UnitStatus status);

    //filter by building and status
    List<ChillerUnit> findByBuildingIdAndStatus(UUID buildingId, ChillerUnit.UnitStatus status);

    //count in building
    long countByBuildingId(UUID buildingId);

    //count by status
    long countByStatus(ChillerUnit.UnitStatus status);

    //search by name
    @Query("SELECT cu FROM ChillerUnit cu WHERE LOWER(cu.name) LIKE LOWER(CONCAT('%', :searchTerm, '%'))")
    List<ChillerUnit> searchByName(@Param("searchTerm") String searchTerm);

    //filter by manufacturer
    List<ChillerUnit> findByManufacturer(String manufacturer);
}
