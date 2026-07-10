package com.yourapp.chess;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import java.util.TimeZone;

@SpringBootApplication
public class ChessApplication {

    public static void main(String[] args) {
        // Fix for PostgreSQL timezone compatibility issue (legacy 'Asia/Calcutta' vs 'Asia/Kolkata')
        TimeZone.setDefault(TimeZone.getTimeZone("Asia/Kolkata"));

        SpringApplication.run(ChessApplication.class, args);
    }
}