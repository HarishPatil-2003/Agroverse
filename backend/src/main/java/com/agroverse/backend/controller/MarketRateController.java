package com.agroverse.backend.controller;

import com.agroverse.backend.service.MarketRateService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@CrossOrigin(origins = "*")
@RequestMapping("/api/market")
public class MarketRateController {

    @Autowired
    private MarketRateService marketRateService;

    @GetMapping("/rates")
    public ResponseEntity<List<Map<String, Object>>> getRates() {
        return ResponseEntity.ok(marketRateService.getLiveRates());
    }
}

