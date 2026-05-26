package chillerguard.repository;

import chillerguard.entity.ApiKey;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

//api key repo
@Repository
public interface ApiKeyRepository extends JpaRepository<ApiKey, UUID> {

    //find active key by name
    Optional<ApiKey> findByNameAndIsActiveTrue(String name);

    //find key by prefix before hash check
    Optional<ApiKey> findByKeyPrefixAndIsActiveTrue(String keyPrefix);

    //update last_used_at on auth
    @Modifying
    @Query("UPDATE ApiKey a SET a.lastUsedAt = :now WHERE a.id = :id")
    void updateLastUsedAt(@Param("id") UUID id, @Param("now") Instant now);

    //check if key name already exists
    boolean existsByName(String name);
}
