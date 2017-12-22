package com.dump.service.repositories;

import com.dump.service.Enumerations;
import com.dump.service.objects.Dump;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.stereotype.Repository;

import java.util.Date;

/**
 * Spring Repository to hold Dumps
 */
@Repository
public interface DumpRepository extends PagingAndSortingRepository<Dump, Long> {

    /**
     * Finds a Dump by searching for the given public ID
     * @param publicId  Dump public ID to find
     * @return  Dump
     */
    Dump findByPublicId(String publicId);


    /**
     * Finds the first 10 Dumps matching a given exposure sorted by ID in descending order
     * @param exposure  Exposure type
     * @return  Dump array
     */
    Dump[] findFirst10ByExposureOrderByIdDesc(Enumerations.Exposure exposure);


    /**
     * Finds the first 10 Dumps by a specified user sorted by ID in descending order
     * @param username  username to search
     * @return  Dump array
     */
    Dump[] findFirst10ByUsernameIgnoreCaseOrderByIdDesc(String username);


    /**
     * Finds the first 100 Dumps by a specified user and Exposure, sorted by ID in descending order
     * @param username  Username to search
     * @param exposure  Exposure to search
     * @return  Dump array
     */
    Dump[] findFirst100ByUsernameIgnoreCaseAndExposureOrderByIdDesc(String username, Enumerations.Exposure exposure);


    /**
     * Finds the first 100 Dumps by a specified user sorted by ID in descending order
     * @param username  Username to search
     * @return Dump array
     */
    Dump[] findFirst100ByUsernameIgnoreCaseOrderByIdDesc(String username);


    /**
     * Finds Dumps with a specified Exposure sorted by ID in descending order
     * @param pageable  Page information
     * @param exposure  Exposure type
     * @return  Page object containing Dump array
     */
    Page<Dump[]> findByExposureAndTitleContainsAndContentsContainsOrderByIdDesc(Pageable pageable, Enumerations.Exposure exposure, String title, String contents);


    /**
     * Finds Dumps with a specified Exposure and Type, sorted by ID in descending order
     * @param pageable  Page information
     * @param exposure  Exposure type
     * @param type      Post type
     * @return  Page object containing Dump array
     */
    Page<Dump[]> findByExposureAndTypeAndTitleContainsAndContentsContainsOrderByIdDesc(Pageable pageable, Enumerations.Exposure exposure, String type, String title, String contents);


    /**
     * Finds Dumps whose expiration is between two given dates
     * @param after Date to find after
     * @param before    Date to find before
     * @return  Dump array
     */
    Dump[] findByExpirationIsAfterAndExpirationIsBefore(Date after, Date before);

}
