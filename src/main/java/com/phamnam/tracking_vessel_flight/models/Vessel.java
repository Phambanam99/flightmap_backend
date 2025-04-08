
 package com.phamnam.tracking_vessel_flight.models;

 import jakarta.persistence.*;
 import lombok.*;

 @Entity
 @Table(name = "vessel")
 @Data
 @Getter
 @Setter
 @AllArgsConstructor
 @NoArgsConstructor
 @Builder
 public class Vessel extends BaseEntity {
     @Id
     @GeneratedValue(strategy = GenerationType.IDENTITY)
     private Long id;

     private String name;

     private String type;

     private String registrationNumber;

     private String owner;
 }