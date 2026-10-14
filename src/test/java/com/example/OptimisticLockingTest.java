package com.example;

import com.example.model.Reservation;
import com.example.service.ReservationService;
import com.example.service.ReservationServiceImpl;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import javax.persistence.OptimisticLockException;
import java.time.LocalDateTime;

public class OptimisticLockingTest {

    private Reservation nouvelleReservation(ReservationService service) {
        return service.creerReservation(new Reservation("Salle A",
                LocalDateTime.now().plusDays(1),
                LocalDateTime.now().plusDays(1).plusHours(2)));
    }

    @Test
    public void versionIncrementeeApresModification() {
        ReservationService service = new ReservationServiceImpl();
        Reservation r = nouvelleReservation(service);
        Assertions.assertEquals(0, r.getVersion());

        Reservation lue = service.trouverParId(r.getId());
        lue.setSalle("Salle B");
        Reservation modifiee = service.modifierReservation(lue);

        Assertions.assertEquals(1, modifiee.getVersion());
    }

    @Test
    public void conflitDetecteSurVersionPerimee() {
        ReservationService service = new ReservationServiceImpl();
        Reservation r = nouvelleReservation(service);

        Reservation tx1 = service.trouverParId(r.getId());
        Reservation tx2 = service.trouverParId(r.getId());

        tx1.setSalle("Salle B");
        service.modifierReservation(tx1);

        tx2.setSalle("Salle C");
        Assertions.assertThrows(OptimisticLockException.class, () -> service.modifierReservation(tx2));

        Assertions.assertEquals("Salle B", service.trouverParId(r.getId()).getSalle());
    }

    @Test
    public void retryReussitApresConflit() {
        ReservationService service = new ReservationServiceImpl();
        ReservationRetryHandler retryHandler = new ReservationRetryHandler(service);
        Reservation r = nouvelleReservation(service);

        Reservation tx1 = service.trouverParId(r.getId());
        Reservation tx2 = service.trouverParId(r.getId());

        tx1.setSalle("Salle B");
        service.modifierReservation(tx1);

        Reservation resultat = retryHandler.modifierAvecRetry(tx2, x -> x.setSalle("Salle C"));

        Assertions.assertEquals("Salle C", resultat.getSalle());
        Assertions.assertEquals(2, resultat.getVersion());
    }
}
