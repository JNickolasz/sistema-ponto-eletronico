package com.example.projetoPBD.pontoCerto.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Data;

import java.io.Serializable;

@Data
@Entity
@Table(name="employee")
public class Employee implements Serializable {

}
