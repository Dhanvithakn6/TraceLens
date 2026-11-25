package com.tracelens.mainbackend.incidents;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface IncidentRepository extends JpaRepository<IncidentEntity, UUID> {
}
