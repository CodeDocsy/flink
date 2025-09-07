package com.codedocsy.common;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class RideEvent {
    private Long time;
    private String cameraId;
    private Integer passCount;




}
