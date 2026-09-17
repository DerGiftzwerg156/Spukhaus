package de.spukhaus.backend.repository;

import de.spukhaus.backend.domain.Design;
import de.spukhaus.backend.domain.DesignVersion;
import de.spukhaus.backend.domain.VersionStatus;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DesignVersionRepository extends JpaRepository<DesignVersion, Long> {

    Optional<DesignVersion> findFirstByDesignAndStatusInOrderByVersionNumberDesc(
            Design design, List<VersionStatus> statuses);

    List<DesignVersion> findByDesignOrderByVersionNumberDesc(Design design);

    List<DesignVersion> findByStatusOrderBySubmittedAtAsc(VersionStatus status);
}
