package fr.games.square.api.heartbeat;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Système", description = "Vérification de l'état de l'API")
@RestController
public class HeartbeatController {
    @Autowired
    private HeartbeatSensor heartbeatSensor;

    @Operation(summary = "Vérifier la disponibilité du server")
    @GetMapping("/heartbeat")
    public int heartbeat() {
        return heartbeatSensor.get();
    };
}
