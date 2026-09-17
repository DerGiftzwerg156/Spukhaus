package de.spukhaus.backend.repository;

import de.spukhaus.backend.domain.Design;
import de.spukhaus.backend.domain.User;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface DesignRepository extends JpaRepository<Design, Long> {

    List<Design> findByOwner(User owner);

    @Query("""
            select d from Design d
            where d.currentPublishedVersion is not null
              and lower(d.currentPublishedVersion.name) like lower(concat('%', :query, '%'))
            """)
    Page<Design> searchPublished(@Param("query") String query, Pageable pageable);

    @Query("select d from Design d where d.currentPublishedVersion is not null")
    Page<Design> findAllPublished(Pageable pageable);

    List<Design> findByForkedFrom(Design forkedFrom);
}
