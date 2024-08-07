package com.wanfadger.AdministrativeareaApi.repository;


import com.wanfadger.AdministrativeareaApi.entity.Parish;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ParishRepository extends JpaRepository<Parish, String> {

    @Override
    @EntityGraph(attributePaths = {"subCounty.county.localGovernment.subRegion.region"} , type = EntityGraph.EntityGraphType.FETCH)
    List<Parish> findAll();

    @Override
    @EntityGraph(attributePaths = {"subCounty.county.localGovernment.subRegion.region"} , type = EntityGraph.EntityGraphType.FETCH)
    Optional<Parish> findById(String id);

    Optional<Parish> findByNameIgnoreCaseAndSubCounty_Code(String name , String countyCode);

    List<Parish> findAllBySubCounty_Code(String subCountyCode);

    @Query("SELECT P FROM Parish P WHERE P.subCounty.code IN :subCountyCodes")
    List<Parish> findAllBySubCountyCodes(List<String> subCountyCodes);

    Optional<Parish> findByCodeIgnoreCase(String code);


}
