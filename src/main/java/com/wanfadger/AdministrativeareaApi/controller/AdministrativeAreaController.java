package com.wanfadger.AdministrativeareaApi.controller;

import com.wanfadger.AdministrativeareaApi.dto.*;
import com.wanfadger.AdministrativeareaApi.repository.projections.CodeNameProjection;
import com.wanfadger.AdministrativeareaApi.service.administrativearea.AdministrativeAreaService;
import com.wanfadger.AdministrativeareaApi.shared.reponses.AdministrativeAreaResponseDto;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@CrossOrigin()
@RequestMapping("/AdministrativeAreas")
public class AdministrativeAreaController {

    private final AdministrativeAreaService administrativeAreaService;

    @PostMapping("/one")
    private ResponseEntity<AdministrativeAreaResponseDto<String>> newOne(@RequestParam Map<String , String> queryMap , @RequestBody NewAdministrativeAreaDto dto){
        return administrativeAreaService.newOne(queryMap,dto);
    }


    @PutMapping("/one")
    private AdministrativeAreaResponseDto<String> updateOne(@RequestParam Map<String , String> queryMap , @RequestBody UpdateAdministrativeAreaDto dto){
        return administrativeAreaService.updateOne(queryMap,dto);
    }


    @PostMapping("/list")
    private ResponseEntity<AdministrativeAreaResponseDto<String>> newList(@RequestParam Map<String , String> queryMap , @RequestBody List<NewAdministrativeAreaDto> dtos){
        return administrativeAreaService.newList(queryMap , dtos);
    }

    @GetMapping("/filterOne")
    private AdministrativeAreaResponseDto<CodeNameProjection> filterOne(@RequestParam Map<String , String> queryMap){
        return administrativeAreaService.filterOne(queryMap);
    }



    @GetMapping("/filterList")
    private AdministrativeAreaResponseDto<List<CodeNameProjection>> filterList(@RequestParam Map<String , String> queryMap){
        return administrativeAreaService.filterList(queryMap);
    }

    @GetMapping("/parishListByPartOf")
    private AdministrativeAreaResponseDto<List<CodeNameDto>> getParishByPartOf(@RequestParam Map<String , String> queryMap){
        return administrativeAreaService.getParishByPartOf(queryMap);
    }


    @GetMapping("/searchList")
    private AdministrativeAreaResponseDto<List<? extends AdministrativeAreaDto>> searchList(@RequestParam Map<String , String> queryMap){
        return administrativeAreaService.searchList(queryMap);
    }

    @GetMapping("/searchOne")
    private AdministrativeAreaResponseDto<? extends AdministrativeAreaDto> searchOne(@RequestParam Map<String , String> queryMap){
        return administrativeAreaService.searchOne(queryMap);
    }



    @PostMapping("/upload")
    private AdministrativeAreaResponseDto<String> upload(@RequestBody List<AdministrativeAreaExcelDto> administrativeAreaExcelDtos){
        return administrativeAreaService.upload(administrativeAreaExcelDtos);
    }




}
