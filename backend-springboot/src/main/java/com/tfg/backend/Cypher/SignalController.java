package com.tfg.backend.Cypher;

import com.tfg.backend.Cypher.dto.SignalBootstrapRequestDto;
import com.tfg.backend.Cypher.dto.SignalBootstrapResponseDto;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;


@Controller
@RequestMapping("/api/signal")
public class SignalController {
    private final SignalService signalService;


    @PostMapping("/keys/bootstrap")
        public SignalBootstrapResponseDto postKeysBootstrap(@RequestBody SignalBootstrapRequestDto request) {
            
            return ;
        }
        ("/send")
    
}
