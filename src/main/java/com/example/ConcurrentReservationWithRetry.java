package com.example;

import com.example.model.Reservation;
import com.example.service.ReservationService;
import com.example.service.ReservationServiceImpl;

import java.time.LocalDateTime;

public class ConcurrentReservationWithRetry {

    public static void main(String[] args) {
        ReservationService service = new ReservationServiceImpl();
        ReservationRetryHandler retryHandler = new ReservationRetryHandler(service);

        Reservation reservation = new Reservation("Salle A",
                LocalDateTime.now().plusDays(1),
                LocalDateTime.now().plusDays(1).plusHours(2));
        reservation = service.creerReservation(reservation);

        Reservation tx1 = service.trouverParId(reservation.getId());
        Reservation tx2 = service.trouverParId(reservation.getId());

        tx1.setSalle("Salle B");
        service.modifierReservation(tx1);
        System.out.println("Transaction 1 : modification réussie.");

        try {
            Reservation resultat = retryHandler.modifierAvecRetry(tx2, r -> r.setSalle("Salle C"));
            System.out.println("Transaction 2 : modification réussie après retry. " + resultat);
        } catch (Exception e) {
            System.err.println("Transaction 2 : échec après retries - " + e.getMessage());
        }

        System.out.println("État final en base : " + service.trouverParId(reservation.getId()));
    }
}
