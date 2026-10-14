package com.example;

import com.example.model.Reservation;
import com.example.service.ReservationService;
import com.example.service.ReservationServiceImpl;

import javax.persistence.OptimisticLockException;
import java.time.LocalDateTime;

public class ConcurrentReservationSimulator {

    public static void main(String[] args) {
        ReservationService service = new ReservationServiceImpl();

        Reservation reservation = new Reservation("Salle A",
                LocalDateTime.now().plusDays(1),
                LocalDateTime.now().plusDays(1).plusHours(2));
        reservation = service.creerReservation(reservation);
        System.out.println("Réservation créée : " + reservation);

        Reservation tx1 = service.trouverParId(reservation.getId());
        Reservation tx2 = service.trouverParId(reservation.getId());
        System.out.println("Transaction 1 a lu : " + tx1);
        System.out.println("Transaction 2 a lu : " + tx2);

        tx1.setSalle("Salle B");
        service.modifierReservation(tx1);
        System.out.println("Transaction 1 : modification réussie.");

        try {
            tx2.setSalle("Salle C");
            service.modifierReservation(tx2);
            System.out.println("Transaction 2 : modification réussie.");
        } catch (OptimisticLockException e) {
            System.out.println("Transaction 2 : conflit détecté - " + e.getMessage());
        }

        System.out.println("État final en base : " + service.trouverParId(reservation.getId()));
    }
}
