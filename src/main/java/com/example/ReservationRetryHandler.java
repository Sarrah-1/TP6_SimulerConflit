package com.example;

import com.example.model.Reservation;
import com.example.service.ReservationService;

import javax.persistence.OptimisticLockException;
import java.util.function.Consumer;

public class ReservationRetryHandler {

    private static final int MAX_RETRIES = 3;
    private final ReservationService service;

    public ReservationRetryHandler(ReservationService service) {
        this.service = service;
    }

    public Reservation modifierAvecRetry(Reservation reservation, Consumer<Reservation> modification) {
        Reservation courante = reservation;
        for (int tentative = 1; tentative <= MAX_RETRIES; tentative++) {
            try {
                modification.accept(courante);
                return service.modifierReservation(courante);
            } catch (OptimisticLockException e) {
                System.out.println("Conflit détecté, tentative de retry " + tentative);
                courante = service.trouverParId(courante.getId());
            }
        }
        throw new OptimisticLockException("Échec après " + MAX_RETRIES + " tentatives");
    }
}
