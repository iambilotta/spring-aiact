/*
 * Copyright 2026 Francesco Bilotta
 * Licensed under the Apache License, Version 2.0 (the "License").
 */
package com.iambilotta.spring.aiact.sample;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/hiring")
public class HiringController {

    private final HiringScreener screener;

    public HiringController(HiringScreener screener) {
        this.screener = screener;
    }

    @PostMapping("/score")
    public ScoringResult score(@RequestBody CandidateApplication application) {
        return screener.score(application);
    }
}
