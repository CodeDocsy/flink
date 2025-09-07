package com.codedocsy.common;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class QueueCheckResult {
    private Long startTime;
    private Long endTime;
    private Integer count;
}
