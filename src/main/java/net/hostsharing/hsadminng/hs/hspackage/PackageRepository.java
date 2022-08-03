package net.hostsharing.hsadminng.hs.hspackage;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface PackageRepository extends JpaRepository<PackageEntity, UUID> {

}
