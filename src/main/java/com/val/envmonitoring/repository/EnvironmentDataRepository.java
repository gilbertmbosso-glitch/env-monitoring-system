package com.val.envmonitoring.repository;

import com.val.envmonitoring.model.EnvironmentData;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface EnvironmentDataRepository extends JpaRepository<EnvironmentData, Long> {

    Optional<EnvironmentData> findTopByCityOrderByTimestampDesc(String city);

    List<EnvironmentData> findTop24ByCityOrderByTimestampDesc(String city);

    List<EnvironmentData> findTop48ByCityOrderByTimestampDesc(String city);

    List<EnvironmentData> findByCityOrderByTimestampDesc(String city);

    List<EnvironmentData> findByCityOrderByTimestampAsc(String city);
}
