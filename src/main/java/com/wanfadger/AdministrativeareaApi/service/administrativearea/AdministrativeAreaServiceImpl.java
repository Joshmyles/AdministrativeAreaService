package com.wanfadger.AdministrativeareaApi.service.administrativearea;

import com.wanfadger.AdministrativeareaApi.dto.*;
import com.wanfadger.AdministrativeareaApi.dto.uniqueDtos.*;
import com.wanfadger.AdministrativeareaApi.entity.*;
import com.wanfadger.AdministrativeareaApi.service.county.DbCountyService;
import com.wanfadger.AdministrativeareaApi.service.localgovernment.DbLocalGovernmentService;
import com.wanfadger.AdministrativeareaApi.service.parish.DbParishService;
import com.wanfadger.AdministrativeareaApi.service.region.DbRegionService;
import com.wanfadger.AdministrativeareaApi.service.subRegion.DbSubRegionService;
import com.wanfadger.AdministrativeareaApi.service.subcounty.DbSubCountyService;
import com.wanfadger.AdministrativeareaApi.shared.administrativeareaexceptions.AlreadyExistsException;
import com.wanfadger.AdministrativeareaApi.shared.administrativeareaexceptions.InvalidException;
import com.wanfadger.AdministrativeareaApi.shared.administrativeareaexceptions.MissingDataException;
import com.wanfadger.AdministrativeareaApi.shared.administrativeareaexceptions.NotFoundException;
import com.wanfadger.AdministrativeareaApi.shared.reponses.AdministrativeAreaResponseDto;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AdministrativeAreaServiceImpl implements AdministrativeAreaService {
    private final DbRegionService dbRegionService;
    private final DbSubRegionService dbSubRegionService;
    private final DbLocalGovernmentService dbLocalGovernmentService;
    private final DbCountyService dbCountyService;
    private final DbSubCountyService dbSubCountyService;
    private final DbParishService dbParishService;

    private boolean notNullEmpty(String value) {
        return value != null && !value.isEmpty();
    }

    private boolean nullEmpty(String value) {
        return null == value;
    }


    private String generateCode(AdministrativeAreaType administrativeAreaType) {
        String code;
        return switch (administrativeAreaType) {
            case REGION -> {
                do {
                    code = UUID.randomUUID().toString();
                } while (dbRegionService.dbByCode(code).isPresent());
                yield code;
            }

            case SUBREGION -> {
                do {
                    code = UUID.randomUUID().toString();
                } while (dbSubRegionService.dbByCode(code).isPresent());
                yield code;
            }

            case LOCALGOVERNMENT -> {
                do {
                    code = UUID.randomUUID().toString();
                } while (dbLocalGovernmentService.dbByCode(code).isPresent());

                yield code;
            }

            case COUNTY -> {
                do {
                    code = UUID.randomUUID().toString();
                } while (dbCountyService.dbByCode(code).isPresent());
                yield code;
            }

            case SUBCOUNTY -> {
                do {
                    code = UUID.randomUUID().toString();
                } while (dbSubCountyService.dbByCode(code).isPresent());
                yield code;
            }

            case PARISH -> {
                do {
                    code = UUID.randomUUID().toString();
                } while (dbParishService.dbByCode(code).isPresent());
                yield code;
            }

        };
    }

    @Override
    public ResponseEntity<AdministrativeAreaResponseDto<String>> newOne(Map<String, String> queryMap, NewAdministrativeAreaDto dto) {
        Optional<AdministrativeAreaType> optionalAdministrativeAreaType = AdministrativeAreaType.administrativeAreaTypeStr(queryMap.get("type"));
        if (optionalAdministrativeAreaType.isEmpty()) {
            throw new MissingDataException("Missing Administrative Area Type");
        }

        AdministrativeAreaType administrativeAreaType = optionalAdministrativeAreaType.get();

        return switch (administrativeAreaType) {
            case REGION -> {

                if (dbRegionService.dbByName(dto.getName()).isPresent()) {
                    throw new AlreadyExistsException("Administrative Area Already Exists");
                }


                Region region = convertDtoRegion(dto, administrativeAreaType);

                dbRegionService.dbNew(region);

                yield new ResponseEntity<>(new AdministrativeAreaResponseDto<>("success", "success"), HttpStatus.CREATED);
            }
            case SUBREGION -> {
                if (nullEmpty(dto.getPartOfCode())) {
                    throw new MissingDataException("Missing PartOfCode");
                }

                Region region = dbRegionService.dbByCode(dto.getPartOfCode()).orElseThrow(() -> new InvalidException("Invalid PartOfCode"));
                if (dbSubRegionService.dbByName_RegionCode(dto.getName(), dto.getPartOfCode()).isPresent()) {
                    throw new AlreadyExistsException("Administrative Area Already Exists");
                }

                SubRegion subRegion = convertDtoSubRegion(dto, administrativeAreaType);
                subRegion.setRegion(region);

                dbSubRegionService.dbNew(subRegion);
                yield new ResponseEntity<>(new AdministrativeAreaResponseDto<>("success", "success"), HttpStatus.CREATED);
            }
            case LOCALGOVERNMENT -> {
                if (nullEmpty(dto.getPartOfCode())) {
                    throw new MissingDataException("Missing PartOfCode");
                }

                SubRegion subRegion = dbSubRegionService.dbByCode(dto.getPartOfCode()).orElseThrow(() -> new InvalidException("Invalid PartOfCode"));

                if (dbLocalGovernmentService.dbByName_SubRegionCode(dto.getName(), dto.getPartOfCode()).isPresent()) {
                    throw new AlreadyExistsException("Administrative Area Already Exists");
                }

                LocalGovernment localGovernment = convertDtoLocalGovernment(dto, administrativeAreaType);
                localGovernment.setSubRegion(subRegion);

                dbLocalGovernmentService.dbNew(localGovernment);
                yield new ResponseEntity<>(new AdministrativeAreaResponseDto<>("success", "success"), HttpStatus.CREATED);
            }

            case COUNTY -> {
                if (nullEmpty(dto.getPartOfCode())) {
                    throw new MissingDataException("Missing PartOfCode");
                }

                LocalGovernment localGovernment = dbLocalGovernmentService.dbByCode(dto.getPartOfCode()).orElseThrow(() -> new InvalidException("Invalid PartOfCode"));

                if (dbCountyService.dbByName_LocalGovernment_Code(dto.getName(), dto.getPartOfCode()).isPresent()) {
                    throw new AlreadyExistsException("Administrative Area Already Exists");
                }

                County county = convertDtoCounty(dto, administrativeAreaType);
                county.setLocalGovernment(localGovernment);

                dbCountyService.dbNew(county);
                yield new ResponseEntity<>(new AdministrativeAreaResponseDto<>("success", "success"), HttpStatus.CREATED);
            }

            case SUBCOUNTY -> {
                if (nullEmpty(dto.getPartOfCode())) {
                    throw new MissingDataException("Missing PartOfCode");
                }

                County county = dbCountyService.dbByCode(dto.getPartOfCode()).orElseThrow(() -> new InvalidException("Invalid PartOfCode"));

                if (dbSubCountyService.dbByName_CountyCode(dto.getName(), dto.getPartOfCode()).isPresent()) {
                    throw new AlreadyExistsException("Administrative Area Already Exists");
                }

                SubCounty subCounty = convertDtoSubCounty(dto, administrativeAreaType);
                subCounty.setCounty(county);

                dbSubCountyService.dbNew(subCounty);
                yield new ResponseEntity<>(new AdministrativeAreaResponseDto<>("success", "success"), HttpStatus.CREATED);
            }

            case PARISH -> {
                if (nullEmpty(dto.getPartOfCode())) {
                    throw new MissingDataException("Missing PartOfCode");
                }

                SubCounty subCounty = dbSubCountyService.dbByCode(dto.getPartOfCode()).orElseThrow(() -> new InvalidException("Invalid PartOfCode"));

                if (dbParishService.dbByName_SubCountyCode(dto.getName(), dto.getPartOfCode()).isPresent()) {
                    throw new AlreadyExistsException("Administrative Area Already Exists");
                }

                Parish parish = convertDtoParish(dto, administrativeAreaType);
                parish.setSubCounty(subCounty);

                dbParishService.dbNew(parish);
                yield new ResponseEntity<>(new AdministrativeAreaResponseDto<>("success", "success"), HttpStatus.CREATED);
            }
        };
    }

    private Parish convertDtoParish(NewAdministrativeAreaDto dto, AdministrativeAreaType administrativeAreaType) {
        Parish parish = new Parish();
        parish.setName(dto.getName());
        parish.setLatitude(dto.getLatitude() != null ? Double.valueOf(dto.getLatitude()) : null);
        parish.setLongitude(dto.getLongitude() != null ? Double.valueOf(dto.getLongitude()) : null);
        parish.setCode(generateCode(administrativeAreaType));
        return parish;
    }

    private SubCounty convertDtoSubCounty(NewAdministrativeAreaDto dto, AdministrativeAreaType administrativeAreaType) {
        SubCounty subCounty = new SubCounty();
        subCounty.setName(dto.getName());
        subCounty.setLatitude(dto.getLatitude() != null ? Double.valueOf(dto.getLatitude()) : null);
        subCounty.setLongitude(dto.getLongitude() != null ? Double.valueOf(dto.getLongitude()) : null);
        subCounty.setCode(generateCode(administrativeAreaType));
        return subCounty;
    }

    private County convertDtoCounty(NewAdministrativeAreaDto dto, AdministrativeAreaType administrativeAreaType) {
        County county = new County();
        county.setName(dto.getName());
        county.setLatitude(dto.getLatitude() != null ? Double.valueOf(dto.getLatitude()) : null);
        county.setLongitude(dto.getLongitude() != null ? Double.valueOf(dto.getLongitude()) : null);
        county.setCode(generateCode(administrativeAreaType));
        return county;
    }

    private LocalGovernment convertDtoLocalGovernment(NewAdministrativeAreaDto dto, AdministrativeAreaType administrativeAreaType) {
        LocalGovernment localGovernment = new LocalGovernment();
        localGovernment.setName(dto.getName());
        localGovernment.setLatitude(dto.getLatitude() != null ? Double.valueOf(dto.getLatitude()) : null);
        localGovernment.setLongitude(dto.getLongitude() != null ? Double.valueOf(dto.getLongitude()) : null);
        localGovernment.setCode(generateCode(administrativeAreaType));
        return localGovernment;
    }

    private SubRegion convertDtoSubRegion(NewAdministrativeAreaDto dto, AdministrativeAreaType administrativeAreaType) {
        SubRegion subRegion = new SubRegion();
        subRegion.setName(dto.getName());
        subRegion.setLatitude(notNullEmpty(dto.getLatitude()) ? Double.valueOf(dto.getLatitude()) : null);
        subRegion.setLongitude(notNullEmpty(dto.getLongitude()) ? Double.valueOf(dto.getLongitude()) : null);
        subRegion.setCode(generateCode(administrativeAreaType));
        return subRegion;
    }

    private Region convertDtoRegion(NewAdministrativeAreaDto dto, AdministrativeAreaType administrativeAreaType) {
        Region region = new Region();
        region.setName(dto.getName());
        region.setDescription(dto.getDescription());
        region.setLatitude(dto.getLatitude() != null ? Double.valueOf(dto.getLatitude()) : null);
        region.setLongitude(dto.getLongitude() != null ? Double.valueOf(dto.getLongitude()) : null);
        region.setCode(generateCode(administrativeAreaType));
        return region;
    }

    @Override
    public ResponseEntity<AdministrativeAreaResponseDto<String>> newList(Map<String, String> queryMap, List<NewAdministrativeAreaDto> dtos) {
        Optional<AdministrativeAreaType> optionalAdministrativeAreaType = AdministrativeAreaType.administrativeAreaTypeStr(queryMap.get("type"));
        if (optionalAdministrativeAreaType.isEmpty()) {
            throw new MissingDataException("Missing Administrative Area Type");
        }

        AdministrativeAreaType administrativeAreaType = optionalAdministrativeAreaType.get();

        return switch (administrativeAreaType) {
            case REGION -> {
                // exclude existing ones
                List<Region> regions = dtos.parallelStream().filter(dto -> dbRegionService.dbByName(dto.getName()).isEmpty()).map(dto -> convertDtoRegion(dto, administrativeAreaType)).toList();


                dbRegionService.dbNew(regions);
                yield new ResponseEntity<>(new AdministrativeAreaResponseDto<>("success", "successfully added " + regions.size() + " administrative areas"), HttpStatus.CREATED);

            }
            case SUBREGION -> {

                // check if all have PartOfCode
                if (dtos.parallelStream().anyMatch(dto -> !notNullEmpty(dto.getPartOfCode()))) {
                    throw new MissingDataException("Found Administrative Area without PartOfCoce");
                }

                // exclude existing ones
                List<SubRegion> subRegions = dtos.parallelStream().filter(dto -> dbSubRegionService.dbByName_RegionCode(dto.getName(), dto.getPartOfCode()).isEmpty()).map(dto -> convertDtoSubRegion(dto, administrativeAreaType)).toList();


                dbSubRegionService.dbNew(subRegions);
                yield new ResponseEntity<>(new AdministrativeAreaResponseDto<>("success", "successfully added " + subRegions.size() + " administrative areas"), HttpStatus.CREATED);
            }
            case LOCALGOVERNMENT -> {

                // check if all have PartOfCode
                if (dtos.parallelStream().anyMatch(dto -> !notNullEmpty(dto.getPartOfCode()))) {
                    throw new MissingDataException("Found Administrative Area without PartOfCoce");
                }

                // exclude existing ones
                List<LocalGovernment> localGovernments = dtos.parallelStream().filter(dto -> dbLocalGovernmentService.dbByName_SubRegionCode(dto.getName(), dto.getPartOfCode()).isEmpty()).map(dto -> convertDtoLocalGovernment(dto, administrativeAreaType)).toList();


                dbLocalGovernmentService.dbNew(localGovernments);
                yield new ResponseEntity<>(new AdministrativeAreaResponseDto<>("success", "successfully added " + localGovernments.size() + " administrative areas"), HttpStatus.CREATED);
            }

            case COUNTY -> {

                // check if all have PartOfCode
                if (dtos.parallelStream().anyMatch(dto -> !notNullEmpty(dto.getPartOfCode()))) {
                    throw new MissingDataException("Found Administrative Area without PartOfCoce");
                }

                // exclude existing ones
                List<County> counties = dtos.parallelStream().filter(dto -> dbCountyService.dbByName_LocalGovernment_Code(dto.getName(), dto.getPartOfCode()).isEmpty()).map(dto -> convertDtoCounty(dto, administrativeAreaType)).toList();


                dbCountyService.dbNew(counties);
                yield new ResponseEntity<>(new AdministrativeAreaResponseDto<>("success", "successfully added " + counties.size() + " administrative areas"), HttpStatus.CREATED);
            }

            case SUBCOUNTY -> {

                // check if all have PartOfCode
                if (dtos.parallelStream().anyMatch(dto -> !notNullEmpty(dto.getPartOfCode()))) {
                    throw new MissingDataException("Found Administrative Area without PartOfCoce");
                }

                // exclude existing ones
                List<SubCounty> subCounties = dtos.parallelStream().filter(dto -> dbSubCountyService.dbByName_CountyCode(dto.getName(), dto.getPartOfCode()).isEmpty()).map(dto -> convertDtoSubCounty(dto, administrativeAreaType)).toList();


                dbSubCountyService.dbNew(subCounties);
                yield new ResponseEntity<>(new AdministrativeAreaResponseDto<>("success", "successfully added " + subCounties.size() + " administrative areas"), HttpStatus.CREATED);
            }

            case PARISH -> {

                // check if all have PartOfCode
                if (dtos.parallelStream().anyMatch(dto -> !notNullEmpty(dto.getPartOfCode()))) {
                    throw new MissingDataException("Found Administrative Area without PartOfCoce");
                }

                // exclude existing ones
                List<Parish> parishes = dtos.parallelStream().filter(dto -> dbParishService.dbByName_SubCountyCode(dto.getName(), dto.getPartOfCode()).isEmpty()).map(dto -> convertDtoParish(dto, administrativeAreaType)).toList();


                dbParishService.dbNew(parishes);
                yield new ResponseEntity<>(new AdministrativeAreaResponseDto<>("success", "successfully added " + parishes.size() + " administrative areas"), HttpStatus.CREATED);

            }
        };

    }

    @Override
    public AdministrativeAreaResponseDto<CodeNameDto> filterOne(Map<String, String> queryMap) {
        String type = queryMap.get("type");
        String code = queryMap.get("code");

        if (!notNullEmpty(type)) {
            throw new MissingDataException("Missing Administrative Area Type");
        }


        Optional<AdministrativeAreaType> optionalAdministrativeAreaType = AdministrativeAreaType.administrativeAreaTypeStr(type);
        if (optionalAdministrativeAreaType.isEmpty()) {
            throw new MissingDataException("Missing Administrative Area Type");
        }

        AdministrativeAreaType administrativeAreaType = optionalAdministrativeAreaType.get();

        return switch (administrativeAreaType) {
            case REGION -> {
                if (!notNullEmpty(code)) {
                    throw new MissingDataException("Missing Administrative Area Code");
                }
                Region region = dbRegionService.dbByCode(code).orElseThrow(() -> new NotFoundException("Region not found"));
                yield new AdministrativeAreaResponseDto<>(new CodeNameDto(region.getCode() , region.getName()));
            }
            case SUBREGION -> {
                if (!notNullEmpty(code)) {
                    throw new MissingDataException("Missing Administrative Area Code");
                }
                SubRegion subRegion = dbSubRegionService.dbByCode(code).orElseThrow(() -> new NotFoundException("SubRegion not found"));
                yield new AdministrativeAreaResponseDto<>(new CodeNameDto(subRegion.getCode() , subRegion.getName()));
            }
            case LOCALGOVERNMENT -> {
                if (!notNullEmpty(code)) {
                    throw new MissingDataException("Missing Administrative Area Code");
                }
                LocalGovernment localGovernment = dbLocalGovernmentService.dbByCode(code).orElseThrow(() -> new NotFoundException("LocalGovernment not found"));
                yield new AdministrativeAreaResponseDto<>(new CodeNameDto(localGovernment.getCode() , localGovernment.getName()));
            }
            case COUNTY -> {
                if (!notNullEmpty(code)) {
                    throw new MissingDataException("Missing Administrative Area Code");
                }
                County county = dbCountyService.dbByCode(code).orElseThrow(() -> new NotFoundException("County not found"));
                yield new AdministrativeAreaResponseDto<>(new CodeNameDto(county.getCode() , county.getName()));
            }
            case SUBCOUNTY -> {
                if (!notNullEmpty(code)) {
                    throw new MissingDataException("Missing Administrative Area Code");
                }
                SubCounty subCounty = dbSubCountyService.dbByCode(code).orElseThrow(() -> new NotFoundException("County not found"));
                yield new AdministrativeAreaResponseDto<>(new CodeNameDto(subCounty.getCode() , subCounty.getName()));
            }
            case PARISH -> {
                if (!notNullEmpty(code)) {
                    throw new MissingDataException("Missing Administrative Area Code");
                }
                Parish parish = dbParishService.dbByCode(code).orElseThrow(() -> new NotFoundException("County not found"));
                yield new AdministrativeAreaResponseDto<>(new CodeNameDto(parish.getCode() , parish.getName()));
            }
        };

    }

    @Override
    public AdministrativeAreaResponseDto<List<CodeNameDto>> filterList(Map<String, String> queryMap) {
        String type = queryMap.get("type");
        String partOf = queryMap.get("partOf");


        if (!notNullEmpty(type)) {
            throw new MissingDataException("Missing Administrative Area Type");
        }


        Optional<AdministrativeAreaType> optionalAdministrativeAreaType = AdministrativeAreaType.administrativeAreaTypeStr(type);
        if (optionalAdministrativeAreaType.isEmpty()) {
            throw new MissingDataException("Missing Administrative Area Type");
        }

        AdministrativeAreaType administrativeAreaType = optionalAdministrativeAreaType.get();

        return switch (administrativeAreaType) {
            case REGION -> {
                List<CodeNameDto> codeNameDtoList = dbRegionService.dbList().parallelStream()
                        .map(region -> new CodeNameDto(region.getCode() , region.getName()))
                        .sorted(Comparator.comparing(CodeNameDto::getCode))
                        .toList();
                yield new AdministrativeAreaResponseDto<>(codeNameDtoList);
            }

            case SUBREGION -> {

                if (!notNullEmpty(partOf)) {
                    throw new MissingDataException("Missing Administrative Area partOf");
                }

                List<CodeNameDto> codeNameDtoList = dbSubRegionService.dbByRegionCode(partOf).parallelStream()
                        .map(subRegion -> new CodeNameDto(subRegion.getCode() , subRegion.getName()))
                        .sorted(Comparator.comparing(CodeNameDto::getCode)).toList();
                yield new AdministrativeAreaResponseDto<>(codeNameDtoList);
            }

            case LOCALGOVERNMENT -> {
                if (!notNullEmpty(partOf)) {
                    throw new MissingDataException("Missing Administrative Area partOf");
                }

                List<CodeNameDto> codeNameDtoList = dbLocalGovernmentService.dbBySubRegionCode(partOf).parallelStream()
                        .map(localGovernment -> new CodeNameDto(localGovernment.getCode() , localGovernment.getName()))
                        .sorted(Comparator.comparing(CodeNameDto::getCode)).toList();
                yield new AdministrativeAreaResponseDto<>(codeNameDtoList);
            }

            case COUNTY -> {
                if (!notNullEmpty(partOf)) {
                    throw new MissingDataException("Missing Administrative Area partOf");
                }

                List<CodeNameDto> codeNameDtoList = dbCountyService.dbAllByLocalGovernmentCode(partOf).parallelStream()
                        .map(localGovernment -> new CodeNameDto(localGovernment.getCode() , localGovernment.getName()))
                        .sorted(Comparator.comparing(CodeNameDto::getCode)).toList();
                yield new AdministrativeAreaResponseDto<>(codeNameDtoList);
            }

            case SUBCOUNTY -> {
                if (!notNullEmpty(partOf)) {
                    throw new MissingDataException("Missing Administrative Area partOf");
                }

                List<CodeNameDto> codeNameDtoList = dbSubCountyService.dbByCountyCode(partOf).parallelStream()
                        .map(subCounty -> new CodeNameDto(subCounty.getCode() , subCounty.getName()))
                        .sorted(Comparator.comparing(CodeNameDto::getCode)).toList();
                yield new AdministrativeAreaResponseDto<>(codeNameDtoList);
            }

            case PARISH -> {
                if (!notNullEmpty(partOf)) {
                    throw new MissingDataException("Missing Administrative Area partOf");
                }

                List<CodeNameDto> codeNameDtoList = dbParishService.dbBySubCountyCode(partOf).parallelStream()
                        .map(parish -> new CodeNameDto(parish.getCode() , parish.getName()))
                        .sorted(Comparator.comparing(CodeNameDto::getCode)).toList();
                yield new AdministrativeAreaResponseDto<>(codeNameDtoList);
            }
        };

    }

    @Override
    public AdministrativeAreaResponseDto<List<CodeNameDto>> getParishByPartOf(Map<String, String> queryMap) {
        String type = queryMap.get("type");
        String partOfCode = queryMap.get("partOfCode");


        if (!notNullEmpty(type)) {
            throw new MissingDataException("Missing Administrative Area Type");
        }


        Optional<AdministrativeAreaType> optionalAdministrativeAreaType = AdministrativeAreaType.administrativeAreaTypeStr(type);
        if (optionalAdministrativeAreaType.isEmpty()) {
            throw new MissingDataException("Missing Administrative Area Type");
        }

        AdministrativeAreaType administrativeAreaType = optionalAdministrativeAreaType.get();

        return switch (administrativeAreaType) {
            case REGION -> {
                // get reg subRegs
                // get sub Lgs
                // get lgs counties
                // get counties subcounties
                // get subcounties parishes
                List<String> subRegionCodes = dbSubRegionService.dbByRegionCode(partOfCode).parallelStream().map(subRegion -> subRegion.getCode()).distinct().toList();
                List<String> lgCodes = dbLocalGovernmentService.dbBySubRegionCodes(subRegionCodes).parallelStream().map(localGovernment -> localGovernment.getCode()).distinct().toList();
                List<String> countyCodes = dbCountyService.dbAllByLocalGovernmentCodes(lgCodes).parallelStream().map(county -> county.getCode()).distinct().toList();
                List<String> subCounties = dbSubCountyService.dbByCountyCodes(countyCodes).parallelStream().map(subCounty -> subCounty.getCode()).distinct().toList();
                List<CodeNameDto> codeNameDtoList = dbParishService.dbBySubCountyCodes(subCounties).parallelStream().map(parish -> new CodeNameDto(parish.getCode() , parish.getName())).distinct().toList();

                yield new AdministrativeAreaResponseDto<>(codeNameDtoList);
            }

            case SUBREGION -> {

                if (!notNullEmpty(partOfCode)) {
                    throw new MissingDataException("Missing Administrative Area partOf");
                }
                // get sub Lgs
                // get lgs counties
                // get counties subcounties
                // get subcounties parishes
                List<String> lgCodes = dbLocalGovernmentService.dbBySubRegionCode(partOfCode).parallelStream().map(localGovernment -> localGovernment.getCode()).distinct().toList();
                List<String> countyCodes = dbCountyService.dbAllByLocalGovernmentCodes(lgCodes).parallelStream().map(county -> county.getCode()).distinct().toList();
                List<String> subCounties = dbSubCountyService.dbByCountyCodes(countyCodes).parallelStream().map(subCounty -> subCounty.getCode()).distinct().toList();
                List<CodeNameDto> codeNameDtoList = dbParishService.dbBySubCountyCodes(subCounties).parallelStream().map(parish -> new CodeNameDto(parish.getCode() , parish.getName()))
                        .sorted(Comparator.comparing(CodeNameDto::getCode))
                        .toList();
                yield new AdministrativeAreaResponseDto<>(codeNameDtoList);
            }

            case LOCALGOVERNMENT -> {
                if (!notNullEmpty(partOfCode)) {
                    throw new MissingDataException("Missing Administrative Area partOf");
                }
                // get lg counties
                // get counties subcouties
                // get subcounties parishes
                List<String> countyCodes = dbCountyService.dbAllByLocalGovernmentCode(partOfCode).parallelStream().map(county -> county.getCode()).distinct().toList();
                List<String> subCountyCodes = dbSubCountyService.dbByCountyCodes(countyCodes).parallelStream().map(subCounty -> subCounty.getCode()).distinct().toList();

                List<CodeNameDto> codeNameDtoList = dbParishService.dbBySubCountyCodes(subCountyCodes).parallelStream().map(parish -> new CodeNameDto(parish.getCode() , parish.getName()))
                        .sorted(Comparator.comparing(CodeNameDto::getCode))
                        .toList();
                yield new AdministrativeAreaResponseDto<>(codeNameDtoList);
            }

            case COUNTY -> {
                if (!notNullEmpty(partOfCode)) {
                    throw new MissingDataException("Missing Administrative Area partOf");
                }
                // get county subcounty
                // get subCounties parishes
                List<String> subCountyCodes = dbSubCountyService.dbByCountyCode(partOfCode).parallelStream().map(subCounty -> subCounty.getCode()).distinct().toList();

                List<CodeNameDto> codeNameDtoList = dbParishService.dbBySubCountyCodes(subCountyCodes).parallelStream().map(parish -> new CodeNameDto(parish.getCode() , parish.getName()))
                        .sorted(Comparator.comparing(CodeNameDto::getCode))
                        .toList();
                yield new AdministrativeAreaResponseDto<>(codeNameDtoList);
            }

            case SUBCOUNTY -> {
                if (!notNullEmpty(partOfCode)) {
                    throw new MissingDataException("Missing Administrative Area partOf");
                }

                List<CodeNameDto> codeNameDtoList = dbParishService.dbBySubCountyCode(partOfCode).parallelStream().map(parish -> new CodeNameDto(parish.getCode() , parish.getName()))
                        .sorted(Comparator.comparing(CodeNameDto::getCode))
                        .toList();
                yield new AdministrativeAreaResponseDto<>(codeNameDtoList);
            }

            case PARISH -> new AdministrativeAreaResponseDto<>(Collections.emptyList());
        };

    }

    @Override
    public AdministrativeAreaResponseDto<List<? extends AdministrativeAreaDto>> searchList(Map<String, String> queryMap) {
        String type = queryMap.get("type");
        String partOf = queryMap.get("partOf");


        Optional<AdministrativeAreaType> optionalAdministrativeAreaType = AdministrativeAreaType.administrativeAreaTypeStr(type);
        if (optionalAdministrativeAreaType.isEmpty()) {
            throw new MissingDataException("Missing Administrative Area Type");
        }

        AdministrativeAreaType administrativeAreaType = optionalAdministrativeAreaType.get();

        return switch (administrativeAreaType) {
            case REGION -> {
                List<RegionDto> regionDtos = dbRegionService.dbList().parallelStream().map(AdministrativeAreaServiceImpl::convertRegionDto).sorted(Comparator.comparing(RegionDto::getCode)).toList();
                yield new AdministrativeAreaResponseDto<>(regionDtos);
            }
            case SUBREGION -> {

                List<SubRegionDto> subRegionDtos;
                if (notNullEmpty(partOf)) {
                    subRegionDtos = dbSubRegionService.dbByRegionCode(partOf).parallelStream().map(AdministrativeAreaServiceImpl::convertSubRegionDto).sorted(Comparator.comparing(SubRegionDto::getCode)).toList();
                } else {
                    subRegionDtos = dbSubRegionService.dbList()
                            .parallelStream().map(AdministrativeAreaServiceImpl::convertSubRegionDto).sorted(Comparator.comparing(SubRegionDto::getCode)).toList();
                }
                yield new AdministrativeAreaResponseDto<>(subRegionDtos);

            }
            case LOCALGOVERNMENT -> {
                List<LocalGovernmentDto> localGovernmentDtos;
                if (notNullEmpty(partOf)) {
                    localGovernmentDtos = dbLocalGovernmentService.dbBySubRegionCode(partOf).parallelStream().map(this::convertLocalGovernmentDto).sorted(Comparator.comparing(LocalGovernmentDto::getCode)).toList();
                } else {
                    localGovernmentDtos = dbLocalGovernmentService.dbList()
                            .parallelStream().map(this::convertLocalGovernmentDto).sorted(Comparator.comparing(LocalGovernmentDto::getCode)).toList();
                }
                yield new AdministrativeAreaResponseDto<>(localGovernmentDtos);
            }
            case COUNTY -> {
                List<CountyDto> countyDtos;
                if (notNullEmpty(partOf)) {
                    countyDtos = dbCountyService.dbAllByLocalGovernmentCode(partOf).parallelStream().map(this::convertCountyDto).sorted(Comparator.comparing(CountyDto::getCode)).toList();
                } else {
                    countyDtos = dbCountyService.dbList()
                            .parallelStream().map(this::convertCountyDto).sorted(Comparator.comparing(CountyDto::getCode)).toList();
                }
                yield new AdministrativeAreaResponseDto<>(countyDtos);
            }
            case SUBCOUNTY -> {
                List<SubCountyDto> subCountyDtos;
                if (notNullEmpty(partOf)) {
                    subCountyDtos = dbSubCountyService.dbByCountyCode(partOf).parallelStream().map(this::convertSubCountyDto).sorted(Comparator.comparing(SubCountyDto::getCode)).toList();
                } else {
                    subCountyDtos = dbSubCountyService.dbList()
                            .parallelStream().map(this::convertSubCountyDto).sorted(Comparator.comparing(SubCountyDto::getCode)).toList();
                }
                yield new AdministrativeAreaResponseDto<>(subCountyDtos);
            }
            case PARISH -> {
                List<ParishDto> parishDtos;
                if (notNullEmpty(partOf)) {
                    parishDtos = dbParishService.dbBySubCountyCode(partOf).parallelStream().map(this::convertParishDto).sorted(Comparator.comparing(ParishDto::getCode)).toList();
                } else {
                    parishDtos = dbParishService.dbList()
                            .parallelStream().map(this::convertParishDto).sorted(Comparator.comparing(ParishDto::getCode)).toList();
                }
                yield new AdministrativeAreaResponseDto<>(parishDtos);
            }
        };

    }

    @Override
    public AdministrativeAreaResponseDto<? extends AdministrativeAreaDto> searchOne(Map<String, String> queryMap) {
        String type = queryMap.get("type");
        String code = queryMap.get("code");

        if (code == null){
            throw new MissingDataException("Missing required data");
        }

        Optional<AdministrativeAreaType> optionalAdministrativeAreaType = AdministrativeAreaType.administrativeAreaTypeStr(type);
        if (optionalAdministrativeAreaType.isEmpty()) {
            throw new MissingDataException("Missing Administrative Area Type");
        }

        AdministrativeAreaType administrativeAreaType = optionalAdministrativeAreaType.get();

        return switch (administrativeAreaType) {
            case REGION -> {
                Region region = dbRegionService.dbByCode(code).orElseThrow(() -> new NotFoundException("Region  not found"));
                yield new AdministrativeAreaResponseDto<>(convertRegionDto(region));
            }
            case SUBREGION -> {
                SubRegion subRegion = dbSubRegionService.dbByCode(code).orElseThrow(() -> new NotFoundException("subRegion  not found"));
                yield new AdministrativeAreaResponseDto<>(convertSubRegionDto(subRegion));

            }
            case LOCALGOVERNMENT -> {
                LocalGovernment localGovernment = dbLocalGovernmentService.dbByCode(code).orElseThrow(() -> new NotFoundException("localGovernment  not found"));
                yield new AdministrativeAreaResponseDto<>(convertLocalGovernmentDto(localGovernment));
            }
            case COUNTY -> {
                County county = dbCountyService.dbByCode(code).orElseThrow(() -> new NotFoundException("county  not found"));
                yield new AdministrativeAreaResponseDto<>(convertCountyDto(county));
            }
            case SUBCOUNTY -> {
                SubCounty subCounty = dbSubCountyService.dbByCode(code).orElseThrow(() -> new NotFoundException("subCounty  not found"));
                yield new AdministrativeAreaResponseDto<>(convertSubCountyDto(subCounty));
            }
            case PARISH -> {
                Parish parish = dbParishService.dbByCode(code).orElseThrow(() -> new NotFoundException("parish  not found"));
                yield new AdministrativeAreaResponseDto<>(convertParishDto(parish));
            }
        };

    }

    @Override
    public AdministrativeAreaResponseDto<String> upload(List<AdministrativeAreaExcelDto> dtoList) {
//        System.out.println(dtoList);
        uploadAdministrativeAreas(dtoList);
        return new AdministrativeAreaResponseDto<>("Successfully updated " + dtoList.size());
    }

    @Async
    public void uploadAdministrativeAreas(List<AdministrativeAreaExcelDto> dtoList) {
        // REGION
        uploadRegions(dtoList);

//        //LOCALGOVERNMENT
//        uploadLocalGovernment(dtoList);
//
//        //COUNTY
//        uploadCounty(dtoList);
//
//        //SUBCOUNTY
//        uploadSubCounty(dtoList);
//
//        //PARISH
//        uploadParishes(dtoList);
    }

    private void uploadParishes(List<AdministrativeAreaExcelDto> dtoList) {
        List<Parish> dbParishes = dbParishService.dbList();

        Set<UParish> newParishSet = dtoList.parallelStream().filter(dto -> dbParishes.stream().noneMatch(dbParish -> {
            SubCounty subCounty = dbParish.getSubCounty();
            County county = subCounty.getCounty();
            LocalGovernment localGovernment = county.getLocalGovernment();
            SubRegion subRegion = localGovernment.getSubRegion();
            Region region = subRegion.getRegion();
            return (dbParish.getName().equalsIgnoreCase(dto.getParish())
                    && subCounty.getName().equalsIgnoreCase(dto.getSubCounty())
                    && county.getName().equalsIgnoreCase(dto.getCounty())
                    && localGovernment.getName().equalsIgnoreCase(dto.getLocalGovernment())
                    && subRegion.getName().equalsIgnoreCase(dto.getSubRegion())
                    && region.getName().equalsIgnoreCase(dto.getRegion()));
        })).map(dto -> new UParish(dto.getParish(), dto.getDbSubCounty())).collect(Collectors.toSet());


        if (newParishSet.size() > 0) {
            List<Parish> newParishes = newParishSet.stream().map(UP -> {
                        Parish parish = new Parish();
                        parish.setCode(generateCode(AdministrativeAreaType.PARISH));
                        parish.setName(UP.name());
                        parish.setSubCounty(UP.subCounty());
                        return parish;
                    })
                    .toList();
            dbParishService.dbNew(newParishes);
        }

    }

    private void uploadSubCounty(List<AdministrativeAreaExcelDto> dtoList) {
        List<SubCounty> dbSubCounties = dbSubCountyService.dbList();

        Set<USubCounty> newSubCountySet = dtoList.parallelStream().filter(dto -> dbSubCounties.stream().noneMatch(dbSubCounty -> {
            County county = dbSubCounty.getCounty();
            LocalGovernment localGovernment = county.getLocalGovernment();
            SubRegion subRegion = localGovernment.getSubRegion();
            Region region = subRegion.getRegion();
            return (dbSubCounty.getName().equalsIgnoreCase(dto.getSubCounty())
                    && county.getName().equalsIgnoreCase(dto.getCounty())
                    && localGovernment.getName().equalsIgnoreCase(dto.getLocalGovernment())
                    && subRegion.getName().equalsIgnoreCase(dto.getSubRegion())
                    && region.getName().equalsIgnoreCase(dto.getRegion()));
        })).map(dto -> new USubCounty(dto.getSubCounty(), dto.getDbCounty())).collect(Collectors.toSet());


        List<SubCounty> dbSubCounties2;
        if (newSubCountySet.size() > 0) {
            List<SubCounty> newSubCounties = newSubCountySet.stream().map(dto -> {
                SubCounty subCounty = new SubCounty();
                subCounty.setCode(generateCode(AdministrativeAreaType.SUBCOUNTY));
                subCounty.setName(dto.name());
                subCounty.setCounty(dto.county());
                return subCounty;
            }).toList();

            dbSubCountyService.dbNew(newSubCounties);
            dbSubCounties2 = dbSubCountyService.dbList();
        } else {
            dbSubCounties2 = dbSubCounties;
        }


        // ADDING Subcounty TO LIST
        List<SubCounty> finalDbSubCounties = dbSubCounties2;
        List<AdministrativeAreaExcelDto> newDtos = dtoList.parallelStream()
                .flatMap(oldDto -> finalDbSubCounties.stream()
                        .filter(dbSubCounty -> {
                            County county = dbSubCounty.getCounty();
                            LocalGovernment localGovernment = county.getLocalGovernment();
                            SubRegion subRegion = localGovernment.getSubRegion();
                            Region region = subRegion.getRegion();
                            return (dbSubCounty.getName().equalsIgnoreCase(oldDto.getSubCounty())
                                    && county.getName().equalsIgnoreCase(oldDto.getCounty())
                                    && localGovernment.getName().equalsIgnoreCase(oldDto.getLocalGovernment())
                                    && subRegion.getName().equalsIgnoreCase(oldDto.getSubRegion())
                                    && region.getName().equalsIgnoreCase(oldDto.getRegion())
                            );
                        })
                        .map(subCounty -> {
                            oldDto.setDbSubCounty(subCounty);
                            return oldDto;
                        })).toList();


        /// UPLOAD Parishes
        uploadParishes(newDtos);

    }

    private void uploadCounty(List<AdministrativeAreaExcelDto> dtoList) {
        List<County> dbCounties = dbCountyService.dbList();

        Set<UCounty> newCountSet = dtoList.parallelStream().filter(dto -> dbCounties.stream().parallel().noneMatch(dbCounty -> {
            LocalGovernment localGovernment = dbCounty.getLocalGovernment();
            SubRegion subRegion = localGovernment.getSubRegion();
            Region region = subRegion.getRegion();
            return (dbCounty.getName().equalsIgnoreCase(dto.getCounty())
                    && localGovernment.getName().equalsIgnoreCase(dto.getLocalGovernment())
                    && subRegion.getName().equalsIgnoreCase(dto.getSubRegion())
                    && region.getName().equalsIgnoreCase(dto.getRegion()));
        })).map(dto -> new UCounty(dto.getCounty(), dto.getDbLocalGovernment())).collect(Collectors.toSet());

        List<County> dbCounties2;
        if (newCountSet.size() > 0) {
            List<County> newCounties = newCountSet.stream().map(UC -> {
                County county = new County();
                county.setCode(generateCode(AdministrativeAreaType.COUNTY));
                county.setName(UC.name());
                county.setLocalGovernment(UC.localGovernment());
                return county;
            }).toList();
            dbCountyService.dbNew(newCounties);
            dbCounties2 = dbCountyService.dbList();
        } else {
            dbCounties2 = dbCounties;
        }


        // ADDING Count TO LIST


        List<AdministrativeAreaExcelDto> newDtos = dtoList.parallelStream()
                .flatMap(oldDto -> dbCounties2.stream()
                        .filter(dbCounty -> {
                            LocalGovernment localGovernment = dbCounty.getLocalGovernment();
                            SubRegion subRegion = localGovernment.getSubRegion();
                            Region region = subRegion.getRegion();
                            return (dbCounty.getName().equalsIgnoreCase(oldDto.getCounty())
                                    && localGovernment.getName().equalsIgnoreCase(oldDto.getLocalGovernment())
                                    && subRegion.getName().equalsIgnoreCase(oldDto.getSubRegion())
                                    && region.getName().equalsIgnoreCase(oldDto.getRegion()));
                        })
                        .map(county -> {
                            oldDto.setDbCounty(county);
                            return oldDto;
                        })).toList();

        /// UPLOAD SUB COUNTYe());
        uploadSubCounty(newDtos);

    }

    private void uploadLocalGovernment(List<AdministrativeAreaExcelDto> dtoList) {
        List<LocalGovernment> dbLocalGovernments = dbLocalGovernmentService.dbList();

        Set<ULocalGovernment> newLocalGovernmentSet = dtoList.parallelStream().filter(dto -> dbLocalGovernments.stream().noneMatch(dbLocalGovernment -> {
            SubRegion subRegion = dto.getDbSubRegion();
            Region region = dto.getDbRegion();
            return (dbLocalGovernment.getName().equalsIgnoreCase(dto.getLocalGovernment())
                    && subRegion.getName().equalsIgnoreCase(dto.getSubRegion())
                    && region.getName().equalsIgnoreCase(dto.getRegion()));
        })).map(dto -> new ULocalGovernment(dto.getLocalGovernment(), dto.getDbSubRegion())).collect(Collectors.toSet());


        List<LocalGovernment> dbLocalGovernments2;
        if (newLocalGovernmentSet.size() > 0) {
            List<LocalGovernment> newLocalGovernments = newLocalGovernmentSet.stream().map(uL -> {
                LocalGovernment localGovernment = new LocalGovernment();
                localGovernment.setCode(generateCode(AdministrativeAreaType.LOCALGOVERNMENT));
                localGovernment.setName(uL.name());
                localGovernment.setSubRegion(uL.subRegion());
                return localGovernment;
            }).toList();
            dbLocalGovernmentService.dbNew(newLocalGovernments);
            dbLocalGovernments2 = dbLocalGovernmentService.dbList();
        } else {
            dbLocalGovernments2 = dbLocalGovernments;
        }


        // ADDING LOCAL GOVERNMENT TO LIST
        List<AdministrativeAreaExcelDto> newDtos = dtoList.parallelStream()
                .flatMap(oldDto -> dbLocalGovernments2.stream()
                        .filter(dbLocalGovernment -> {
                            SubRegion subRegion = dbLocalGovernment.getSubRegion();
                            Region region = subRegion.getRegion();
                            return (dbLocalGovernment.getName().equalsIgnoreCase(oldDto.getLocalGovernment())
                                    && subRegion.getName().equalsIgnoreCase(oldDto.getSubRegion())
                                    && region.getName().equalsIgnoreCase(oldDto.getRegion()));
                        })
                        .map(localGovernment -> {
                            oldDto.setDbLocalGovernment(localGovernment);
                            return oldDto;
                        })).toList();


        /// UPLOAD COUNTY
        uploadCounty(newDtos);

    }

    private void uploadSubRegions(List<AdministrativeAreaExcelDto> dtoList) {
        List<SubRegion> dbSubRegions = dbSubRegionService.dbList();

        Set<USubRegion> newSubRegionSet = dtoList.stream().filter(dto -> dbSubRegions.stream().noneMatch(dbSubRegion -> {
                    Region region = dto.getDbRegion();
                    return (dbSubRegion.getName().equalsIgnoreCase(dto.getSubRegion()) && region.getName().equalsIgnoreCase(dto.getRegion()));
                })).map(dto -> new USubRegion(dto.getSubRegion(), dto.getDbRegion()))
                .collect(Collectors.toSet());

        List<SubRegion> dbSubRegions2;
        if (newSubRegionSet.size() > 0) {
            List<SubRegion> newSubRegions = newSubRegionSet.stream().map(uSubRegion -> {
                SubRegion subRegion = new SubRegion();
                subRegion.setCode(generateCode(AdministrativeAreaType.SUBREGION));
                subRegion.setName(uSubRegion.name());
                // region
                subRegion.setRegion(uSubRegion.region());
                return subRegion;
            }).toList();
            dbSubRegionService.dbNew(newSubRegions);
            dbSubRegions2 = dbSubRegionService.dbList();
        } else {
            dbSubRegions2 = dbSubRegions;
        }


        //Update list with db SubREGION
        List<AdministrativeAreaExcelDto> newDtos = dtoList.parallelStream()
                .flatMap(oldDto -> dbSubRegions2.stream()
                        .filter(dbSubRegion -> {
                            Region region = dbSubRegion.getRegion();
                            return (dbSubRegion.getName().equalsIgnoreCase(oldDto.getSubRegion()) && region.getName().equalsIgnoreCase(oldDto.getRegion()));
                        })
                        .map(subRegion -> {
                            oldDto.setDbSubRegion(subRegion);
                            return oldDto;
                        })).toList();


        // UPLOAD LOCAL GOVERNMENT
        uploadLocalGovernment(newDtos);

    }

    @Transactional
    void uploadRegions(List<AdministrativeAreaExcelDto> dtoList) {
        List<Region> dbRegions = dbRegionService.dbList();
        // exclude existing regions
        List<Region> newRegions = dtoList.parallelStream().filter(dto -> dbRegions.stream().noneMatch(dbRegion -> (dbRegion.getName().equalsIgnoreCase(dto.getRegion()))))
                .filter(distinctByKey(AdministrativeAreaExcelDto::getRegion))
                .map(dto -> {
                    Region region = new Region();
                    region.setCode(generateCode(AdministrativeAreaType.REGION));
                    region.setName(dto.getRegion());
                    return region;
                })
                .toList();

        List<Region> dbRegions2;
        if (newRegions.size() > 0) {
            dbRegionService.dbNew(newRegions);
            dbRegions2 = dbRegionService.dbList();
        } else {
            dbRegions2 = dbRegions;
        }


        //Update list with db Region
        List<AdministrativeAreaExcelDto> newDtos = dtoList.parallelStream()
                .flatMap(oldDto -> dbRegions2.stream().filter(region -> region.getName().equalsIgnoreCase(oldDto.getRegion()))
                        .map(region -> {
                            oldDto.setDbRegion(region);
                            return oldDto;
                        })).toList();
        uploadSubRegions(newDtos);
    }

    public static <T> Predicate<T> distinctByKey(
            Function<? super T, ?> keyExtractor) {

        Map<Object, Boolean> seen = new ConcurrentHashMap<>();
        return t -> seen.putIfAbsent(keyExtractor.apply(t), Boolean.TRUE) == null;
    }

    @Override
    public AdministrativeAreaResponseDto<String> updateOne(Map<String, String> queryMap, UpdateAdministrativeAreaDto dto) {
        String type = queryMap.get("type");


        Optional<AdministrativeAreaType> optionalAdministrativeAreaType = AdministrativeAreaType.administrativeAreaTypeStr(type);
        if (optionalAdministrativeAreaType.isEmpty()) {
            throw new MissingDataException("Missing Administrative Area Type");
        }

        AdministrativeAreaType administrativeAreaType = optionalAdministrativeAreaType.get();

        return switch (administrativeAreaType) {
            case REGION -> {
                Region region = dbRegionService.dbByCode(dto.getCode()).orElseThrow(() -> new InvalidException("Invalid PartOfCode"));
                if (notNullEmpty(dto.getName())) {
                    region.setName(dto.getName());
                }

                if (nullEmpty(dto.getLongitude())) {
                    region.setLatitude(Double.valueOf(dto.getLatitude()));
                }

                if (nullEmpty(dto.getLongitude())) {
                    region.setLatitude(Double.valueOf(dto.getLongitude()));
                }

                if (notNullEmpty(dto.getDescription())) {
                    region.setDescription(dto.getDescription());
                }

                dbRegionService.dbNew(region);
                yield new AdministrativeAreaResponseDto<>("SUCCESS");
            }
            case SUBREGION -> {
                if (nullEmpty(dto.getPartOfCode())) {
                    throw new MissingDataException("Missing PartOfCode");
                }

                Region region = dbRegionService.dbByCode(dto.getPartOfCode()).orElseThrow(() -> new InvalidException("Invalid PartOfCode"));
                SubRegion subRegion = dbSubRegionService.dbByCode(dto.getCode()).orElseThrow(() -> new NotFoundException("Administrative Area NotFound"));


                if (notNullEmpty(dto.getName())) {
                    subRegion.setName(dto.getName());
                }

                if (nullEmpty(dto.getLongitude())) {
                    subRegion.setLatitude(Double.valueOf(dto.getLatitude()));
                }

                if (nullEmpty(dto.getLongitude())) {
                    subRegion.setLatitude(Double.valueOf(dto.getLongitude()));
                }

                if (notNullEmpty(dto.getDescription())) {
                    subRegion.setDescription(dto.getDescription());
                }

                subRegion.setRegion(region);

                dbSubRegionService.dbNew(subRegion);
                yield new AdministrativeAreaResponseDto<>("SUCCESS");
            }
            case LOCALGOVERNMENT -> {
                if (nullEmpty(dto.getPartOfCode())) {
                    throw new MissingDataException("Missing PartOfCode");
                }

                SubRegion subRegion = dbSubRegionService.dbByCode(dto.getPartOfCode()).orElseThrow(() -> new InvalidException("Invalid PartOfCode"));
                LocalGovernment localGovernment = dbLocalGovernmentService.dbByCode(dto.getCode()).orElseThrow(() -> new NotFoundException("Administrative Area NotFound"));


                if (notNullEmpty(dto.getName())) {
                    localGovernment.setName(dto.getName());
                }

                if (nullEmpty(dto.getLongitude())) {
                    localGovernment.setLatitude(Double.valueOf(dto.getLatitude()));
                }

                if (nullEmpty(dto.getLongitude())) {
                    localGovernment.setLatitude(Double.valueOf(dto.getLongitude()));
                }

                if (notNullEmpty(dto.getDescription())) {
                    localGovernment.setDescription(dto.getDescription());
                }

                localGovernment.setSubRegion(subRegion);

                dbLocalGovernmentService.dbNew(localGovernment);
                yield new AdministrativeAreaResponseDto<>("SUCCESS");
            }
            case COUNTY -> {
                if (nullEmpty(dto.getPartOfCode())) {
                    throw new MissingDataException("Missing PartOfCode");
                }

                LocalGovernment localGovernment = dbLocalGovernmentService.dbByCode(dto.getPartOfCode()).orElseThrow(() -> new InvalidException("Invalid PartOfCode"));
                County county = dbCountyService.dbByCode(dto.getCode()).orElseThrow(() -> new NotFoundException("Administrative Area NotFound"));


                if (notNullEmpty(dto.getName())) {
                    county.setName(dto.getName());
                }

                if (nullEmpty(dto.getLongitude())) {
                    county.setLatitude(Double.valueOf(dto.getLatitude()));
                }

                if (nullEmpty(dto.getLongitude())) {
                    county.setLatitude(Double.valueOf(dto.getLongitude()));
                }

                if (notNullEmpty(dto.getDescription())) {
                    county.setDescription(dto.getDescription());
                }

                county.setLocalGovernment(localGovernment);

                dbCountyService.dbNew(county);
                yield new AdministrativeAreaResponseDto<>("SUCCESS");
            }
            case SUBCOUNTY -> {
                if (nullEmpty(dto.getPartOfCode())) {
                    throw new MissingDataException("Missing PartOfCode");
                }

                County county = dbCountyService.dbByCode(dto.getPartOfCode()).orElseThrow(() -> new InvalidException("Invalid PartOfCode"));
                SubCounty subCounty = dbSubCountyService.dbByCode(dto.getCode()).orElseThrow(() -> new NotFoundException("Administrative Area NotFound"));


                if (notNullEmpty(dto.getName())) {
                    subCounty.setName(dto.getName());
                }

                if (nullEmpty(dto.getLongitude())) {
                    subCounty.setLatitude(Double.valueOf(dto.getLatitude()));
                }

                if (nullEmpty(dto.getLongitude())) {
                    subCounty.setLatitude(Double.valueOf(dto.getLongitude()));
                }

                if (notNullEmpty(dto.getDescription())) {
                    subCounty.setDescription(dto.getDescription());
                }

                subCounty.setCounty(county);

                dbSubCountyService.dbNew(subCounty);
                yield new AdministrativeAreaResponseDto<>("SUCCESS");
            }
            case PARISH -> {
                if (nullEmpty(dto.getPartOfCode())) {
                    throw new MissingDataException("Missing PartOfCode");
                }

                SubCounty subCounty = dbSubCountyService.dbByCode(dto.getPartOfCode()).orElseThrow(() -> new InvalidException("Invalid PartOfCode"));
                Parish parish = dbParishService.dbByCode(dto.getCode()).orElseThrow(() -> new NotFoundException("Administrative Area NotFound"));


                if (notNullEmpty(dto.getName())) {
                    parish.setName(dto.getName());
                }

                if (nullEmpty(dto.getLongitude())) {
                    parish.setLatitude(Double.valueOf(dto.getLatitude()));
                }

                if (nullEmpty(dto.getLongitude())) {
                    parish.setLatitude(Double.valueOf(dto.getLongitude()));
                }

                if (notNullEmpty(dto.getDescription())) {
                    parish.setDescription(dto.getDescription());
                }

                parish.setSubCounty(subCounty);

                dbParishService.dbNew(parish);
                yield new AdministrativeAreaResponseDto<>("SUCCESS");
            }
        };

    }



    private ParishDto convertParishDto(Parish parish) {
        ParishDto dto = new ParishDto();
        dto.setCode(parish.getCode());
        dto.setName(parish.getName());
        dto.setLatitude(parish.getLatitude() != null ? String.valueOf(parish.getLatitude()) : "");
        dto.setLongitude(parish.getLongitude() != null ? String.valueOf(parish.getLongitude()) : "");

        dto.setSubCounty(convertSubCountyDto(parish.getSubCounty()));

        return dto;
    }

    private SubCountyDto convertSubCountyDto(SubCounty subCounty) {
        SubCountyDto dto = new SubCountyDto();
        dto.setCode(subCounty.getCode());
        dto.setName(subCounty.getName());
        dto.setLatitude(subCounty.getLatitude() != null ? String.valueOf(subCounty.getLatitude()) : "");
        dto.setLongitude(subCounty.getLongitude() != null ? String.valueOf(subCounty.getLongitude()) : "");

        dto.setCounty(convertCountyDto(subCounty.getCounty()));

        return dto;
    }

    private CountyDto convertCountyDto(County county) {
        CountyDto dto = new CountyDto();
        dto.setCode(county.getCode());
        dto.setName(county.getName());
        dto.setLatitude(county.getLatitude() != null ? String.valueOf(county.getLatitude()) : "");
        dto.setLongitude(county.getLongitude() != null ? String.valueOf(county.getLongitude()) : "");

        dto.setLocalGovernment(convertLocalGovernmentDto(county.getLocalGovernment()));

        return dto;
    }

    private LocalGovernmentDto convertLocalGovernmentDto(LocalGovernment localGovernment) {
        LocalGovernmentDto dto = new LocalGovernmentDto();
        dto.setCode(localGovernment.getCode());
        dto.setName(localGovernment.getName());
        dto.setLatitude(localGovernment.getLatitude() != null ? String.valueOf(localGovernment.getLatitude()) : "");
        dto.setLongitude(localGovernment.getLongitude() != null ? String.valueOf(localGovernment.getLongitude()) : "");

        dto.setSubRegion(convertSubRegionDto(localGovernment.getSubRegion()));

        return dto;
    }

    private static SubRegionDto convertSubRegionDto(SubRegion subRegion) {
        SubRegionDto dto = new SubRegionDto();
        dto.setCode(subRegion.getCode());
        dto.setName(subRegion.getName());
        dto.setLatitude(subRegion.getLatitude() != null ? String.valueOf(subRegion.getLatitude()) : "");
        dto.setLongitude(subRegion.getLongitude() != null ? String.valueOf(subRegion.getLongitude()) : "");

        dto.setRegion(convertRegionDto(subRegion.getRegion()));

        return dto;
    }

    private static RegionDto convertRegionDto(Region region) {
        RegionDto dto = new RegionDto();
        dto.setCode(region.getCode());
        dto.setName(region.getName());
        dto.setLongitude(region.getLongitude() != null ? String.valueOf(region.getLongitude()) : "");
        dto.setLatitude(region.getLatitude() != null ? String.valueOf(region.getLatitude()) : "");
        return dto;
    }


//    private final AdministrativeAreaRepository administrativeAreaRepository;
//
//
//    @Override
//    public ResponseEntity<AdministrativeAreaResponseDto<String>> newOne(AdministrativeAreaDtos.NewAdministrativeAreaDto dto) {
//        Optional<AdministrativeAreaType> optionalAdministrativeAreaType = AdministrativeAreaType.administrativeAreaTypeStr(dto.getAdministrativeAreaType());
//        if (optionalAdministrativeAreaType.isEmpty()) {
//            throw new MissingDataException("Missing Administrative Area Type");
//        }
//        AdministrativeAreaType administrativeAreaType = optionalAdministrativeAreaType.get();
//        Optional<AdministrativeArea> optionalAdministrativeArea = dbByName_Type_PartOf(dto.getName(), administrativeAreaType, dto.getPartOf());
//
//        if (optionalAdministrativeArea.isEmpty()) {
//            return new ResponseEntity<>(new AdministrativeAreaResponseDto<>("success" , "Administrative area already exits") , HttpStatus.ALREADY_REPORTED);
//        }
//
//
//        AdministrativeArea administrativeArea = new AdministrativeArea();
//        administrativeArea.setName(dto.getName());
//        administrativeArea.setPartOf(dto.getPartOf());
//        administrativeArea.setLatitude(dto.getLatitude() != null ? Double.valueOf(dto.getLatitude()) : null);
//        administrativeArea.setLongitude(dto.getLongitude() !=null ? Double.valueOf(dto.getLongitude()) : null);
//
//
////        administrativeArea.setAdministrativeAreaType(administrativeAreaType);
//
//        // generate new code
//        String code = generateCode();
//        administrativeArea.setCode(code);
//
//        dbNew(administrativeArea);
//        return new ResponseEntity<>(new AdministrativeAreaResponseDto<>("success" , "success") , HttpStatus.CREATED);
//    }
//
//    @Override
//    public ResponseEntity<AdministrativeAreaResponseDto<String>> newList(List<AdministrativeAreaDtos.NewAdministrativeAreaDto> dtos) {
//
//        // check if all have type
//        if (dtos.parallelStream().anyMatch(dto -> AdministrativeAreaType.administrativeAreaTypeStr(dto.getAdministrativeAreaType()).isEmpty())) {
//            throw new MissingDataException("Found Administrative Area without Type");
//        }
//
//        // exclude existing ones
//        List<AdministrativeArea> administrativeAreaList = dtos.parallelStream().filter(dto -> {
//            AdministrativeAreaType administrativeAreaType = AdministrativeAreaType.administrativeAreaTypeStr(dto.getAdministrativeAreaType()).get();
//            return dbByName_Type_PartOf(dto.getName(), administrativeAreaType, dto.getPartOf()).isEmpty();
//        }).map(dto -> {
//            AdministrativeArea administrativeArea = new AdministrativeArea();
//            administrativeArea.setName(dto.getName());
//            administrativeArea.setPartOf(dto.getPartOf());
//            administrativeArea.setLatitude(dto.getLatitude() != null ? Double.valueOf(dto.getLatitude()) : null);
//            administrativeArea.setLongitude(dto.getLongitude() != null ? Double.valueOf(dto.getLongitude()) : null);
//
//            AdministrativeAreaType administrativeAreaType = AdministrativeAreaType.administrativeAreaTypeStr(dto.getAdministrativeAreaType()).get();
////            administrativeArea.setAdministrativeAreaType(administrativeAreaType);
//
//            // generate new code
//            String code = generateCode();
//            administrativeArea.setCode(code);
//
//            return administrativeArea;
//        }).toList();
//
//
//        dbNew(administrativeAreaList);
//        return new ResponseEntity<>(new AdministrativeAreaResponseDto<>("success" , "successfully added "+administrativeAreaList.size()+" administrative areas") , HttpStatus.CREATED);
//    }
//
//
//    @Override
//    public AdministrativeAreaResponseDto<AdministrativeAreaDtos.AdministrativeAreaDto> filterOne(Map<String, String> queryMap) {
//        String name = queryMap.get("name");
//        String code = queryMap.get("code");
//
//        if (name != null){
//            AdministrativeArea administrativeArea = dbByName(name).orElseThrow(() -> new NotFoundException("Administrative Area not found"));
//
//            return new AdministrativeAreaResponseDto<>(convertToDto1(administrativeArea));
//        }
//
//        if (code != null){
//            AdministrativeArea administrativeArea = dbByCode(code).orElseThrow(() -> new NotFoundException("Administrative Area not found"));
//            return new AdministrativeAreaResponseDto<>(convertToDto1(administrativeArea));
//        }
//        return null;
//    }
//
//    @Override
//    public AdministrativeAreaResponseDto<List<AdministrativeAreaDtos.AdministrativeAreaDto>> filterList(Map<String, String> queryMap) {
//        String type = queryMap.get("type");
//        String partOf = queryMap.get("partOf");
//        String name = queryMap.get("name");
//        String code = queryMap.get("code");
//
//
//        if (partOf != null) {
//            List<AdministrativeAreaDtos.AdministrativeAreaDto> administrativeAreaDtos = dbAllByPartOf(partOf).parallelStream().map(this::convertToDto1).sorted(Comparator.comparing(AdministrativeAreaDtos.AdministrativeAreaDto::getName)).toList();
//            return new AdministrativeAreaResponseDto<>(administrativeAreaDtos);
//        }
//
//        if (type != null) {
//            AdministrativeAreaType administrativeAreaType = AdministrativeAreaType.administrativeAreaTypeStr(type).orElseThrow(() -> new InvalidException("Invalid type " + type));
//            List<AdministrativeAreaDtos.AdministrativeAreaDto> administrativeAreaDtos = dbAllByAdministrativeType(administrativeAreaType).parallelStream().map(this::convertToDto1).sorted(Comparator.comparing(AdministrativeAreaDtos.AdministrativeAreaDto::getName)).toList();
//            return new AdministrativeAreaResponseDto<>(administrativeAreaDtos);
//        }
//
//
//        if (name != null){
//            List<AdministrativeAreaDtos.AdministrativeAreaDto> administrativeAreaDtos = dbAllByNameLike(name).parallelStream().map(this::convertToDto1).sorted(Comparator.comparing(AdministrativeAreaDtos.AdministrativeAreaDto::getName)).toList();
//            return new AdministrativeAreaResponseDto<>(administrativeAreaDtos);
//        }
//
//        if (code != null){
//            List<AdministrativeAreaDtos.AdministrativeAreaDto> administrativeAreaDtos = dbAllByCodeLike(code).parallelStream().map(this::convertToDto1).sorted(Comparator.comparing(AdministrativeAreaDtos.AdministrativeAreaDto::getName)).toList();
//            return new AdministrativeAreaResponseDto<>(administrativeAreaDtos);
//        }
//
//
//        return new AdministrativeAreaResponseDto<>(Collections.emptyList());
//    }
//
//
//
//    private AdministrativeAreaDtos.AdministrativeAreaDto convertToDto1(AdministrativeArea administrativeArea){
//        AdministrativeAreaDtos.AdministrativeAreaDto dto = new AdministrativeAreaDtos.AdministrativeAreaDto();
//        dto.setCode(administrativeArea.getCode());
//        dto.setPartOf(administrativeArea.getPartOf());
//        dto.setName(administrativeArea.getName());
//        dto.setLongitude(administrativeArea.getLongitude() != null ? String.valueOf(administrativeArea.getLongitude()) : null);
//        dto.setLatitude(administrativeArea.getLatitude() != null ? String.valueOf(administrativeArea.getLatitude()) : null);
//        return dto;
//    }
//
//    @Override
//    public String generateCode() {
//        String code;
//        do{
//            code = UUID.randomUUID().toString();
//        }while (administrativeAreaRepository.findByCodeIgnoreCase(code).isPresent());
//        return code;
//    }
//
//    @Override
//    public AdministrativeArea dbNew(AdministrativeArea administrativeArea) {
//        return administrativeAreaRepository.save(administrativeArea);
//    }
//
//    @Override
//    public List<AdministrativeArea> dbNew(List<AdministrativeArea> administrativeAreas) {
//        return administrativeAreaRepository.saveAll(administrativeAreas);
//    }
//
//    @Override
//    public List<AdministrativeArea> dbAllByPartOf(String partOf) {
//        return administrativeAreaRepository.findByPartOf(partOf);
//    }
//
//    @Override
//    public List<AdministrativeArea> dbAllByAdministrativeType(AdministrativeAreaType administrativeAreaType) {
//        return administrativeAreaRepository.findByAdministrativeAreaType(administrativeAreaType);
//    }
//
//    @Override
//    public List<AdministrativeArea> dbAllByCodeLike(String code) {
//        return administrativeAreaRepository.findAllByCodeLikeIgnoreCase(code);
//    }
//
//    @Override
//    public List<AdministrativeArea> dbAllByNameLike(String name) {
//        return administrativeAreaRepository.findAllByNameLikeIgnoreCase(name);
//    }
//
//    @Override
//    public Optional<AdministrativeArea> dbByName_Type_PartOf(String name, AdministrativeAreaType administrativeAreaType, String partOf) {
//        return administrativeAreaRepository.findByNameIgnoreCaseAndAdministrativeAreaTypeAndPartOf(name , administrativeAreaType , partOf);
//    }
//
//    @Override
//    public Optional<AdministrativeArea> dbByCode(String code) {
//        return administrativeAreaRepository.findByCodeIgnoreCase(code);
//    }
//
//    @Override
//    public Optional<AdministrativeArea> dbByName(String name) {
//        return administrativeAreaRepository.findByNameIgnoreCase(name);
//    }
}
