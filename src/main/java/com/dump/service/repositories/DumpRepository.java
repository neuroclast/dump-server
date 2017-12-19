package com.dump.service.repositories;

import com.dump.service.Enumerations;
import com.dump.service.objects.Dump;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DumpRepository extends PagingAndSortingRepository<Dump, Long> {

    Dump findByPublicId(String publicId);

    Dump[] findFirst10ByExposureOrderByIdDesc(Enumerations.Exposure exposure);

    Dump[] findFirst10ByUsernameIgnoreCaseOrderByIdDesc(String username);

    Dump[] findFirst100ByUsernameIgnoreCaseAndExposureOrderByIdDesc(String username, Enumerations.Exposure exposure);

    Dump[] findFirst100ByUsernameIgnoreCaseOrderByIdDesc(String username);

    Page<Dump[]> findByExposureOrderByIdDesc(Pageable pageable, Enumerations.Exposure exposure);

    Page<Dump[]> findByExposureAndTypeOrderByIdDesc(Pageable pageable, Enumerations.Exposure exposure, String type);

}
