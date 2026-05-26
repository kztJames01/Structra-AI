package chillerguard.repository;

import chillerguard.entity.Building;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

//building repo
@Repository
public interface BuildingRepository extends JpaRepository<Building, UUID> {

    //find by bms external id
    Optional<Building> findByExternalId(String externalId);

    //check external id exists
    boolean existsByExternalId(String externalId);

    //filter by status
    List<Building> findByStatus(Building.BuildingStatus status);

    //filter by bms type
    List<Building> findByBmsType(String bmsType);

    //search by name
    @Query("SELECT b FROM Building b WHERE LOWER(b.name) LIKE LOWER(CONCAT('%', :searchTerm, '%'))")
    List<Building> searchByName(@Param("searchTerm") String searchTerm);

    //count by status
    long countByStatus(Building.BuildingStatus status);
}
