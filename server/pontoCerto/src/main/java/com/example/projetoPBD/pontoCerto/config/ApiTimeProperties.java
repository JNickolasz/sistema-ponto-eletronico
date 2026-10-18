package com.example.projetoPBD.pontoCerto.config;


import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@ConfigurationProperties(prefix = "api.pairing.code")
@Component
@Getter
@Setter
public class ApiTimeProperties {
    private Long expiration;
}
