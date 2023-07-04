package com.codechamp.loanApp.model;

import jakarta.persistence.*;
import lombok.Data;

import static jakarta.persistence.GenerationType.IDENTITY;

@Data
@Entity
public class Subscriber {
    @Id
    @GeneratedValue(strategy =IDENTITY)
    private Long id;
    @Column(nullable = false)
    private String msisdn;
}
