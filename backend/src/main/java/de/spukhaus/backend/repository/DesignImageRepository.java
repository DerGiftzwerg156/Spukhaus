package de.spukhaus.backend.repository;

import de.spukhaus.backend.domain.DesignImage;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DesignImageRepository extends JpaRepository<DesignImage, Long> {
}
