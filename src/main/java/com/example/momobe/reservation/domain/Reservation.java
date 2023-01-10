package com.example.momobe.reservation.domain;

import com.example.momobe.common.domain.BaseTime;
import com.example.momobe.reservation.domain.enums.ReservationState;
import com.example.momobe.reservation.event.ReservationEvent;
import lombok.*;

import javax.persistence.*;

import java.time.LocalDateTime;
import java.util.Objects;

import static com.example.momobe.common.exception.enums.ErrorCode.*;
import static com.example.momobe.reservation.domain.enums.ReservationState.*;
import static javax.persistence.EnumType.STRING;
import static javax.persistence.GenerationType.*;
import static lombok.AccessLevel.*;

@Entity
@Getter
@Builder
@EqualsAndHashCode(callSuper = false)
@AllArgsConstructor(access = PRIVATE)
@NoArgsConstructor(access = PROTECTED)
public class Reservation extends BaseTime {
    @Id
    @Column(name = "reservation_id")
    @GeneratedValue(strategy = IDENTITY)
    private Long id;

    private Long meetingId;

    @Embedded
    private ReservationDate reservationDate;

    @Embedded
    private Money amount;

    @Embedded
    private ReservedUser reservedUser;

    @Embedded
    private ReservationMemo reservationMemo;

    @Enumerated(STRING)
    private ReservationState reservationState;

    private Long paymentId;

    public Reservation(ReservationDate reservationDate, Money amount, ReservedUser reservedUser, ReservationMemo reservationMemo, Long meetingId) {
        this.reservationDate = reservationDate;
        this.amount = amount;
        this.reservedUser = reservedUser;
        this.reservationMemo = reservationMemo;
        this.meetingId = meetingId;
        this.reservationState = (amount.getWon() <= 0) ? PAYMENT_SUCCESS : PAYMENT_BEFORE;
    }

    public Reservation(ReservationDate reservationDate, Money amount, ReservedUser reservedUser, ReservationMemo reservationMemo, ReservationState reservationState, Long meetingId) {
        this.reservationDate = reservationDate;
        this.amount = amount;
        this.reservedUser = reservedUser;
        this.reservationMemo = reservationMemo;
        this.meetingId = meetingId;
        this.reservationState = reservationState;
    }

    public Boolean isCanceledReservation() {
        return this.reservationState.equals(CANCEL);
    }

    public Boolean isPaymentSucceed() {
        return Objects.equals(this.reservationState, PAYMENT_SUCCESS);
    }

    public void setPaymentId(Long paymentId) { this.paymentId = paymentId; }

    public ReservationEvent.PaymentCancel createCancelEvent(String paymentKey, String reasonMessage) {
        return ReservationEvent.PaymentCancel.builder()
                .paymentKey(paymentKey)
                .reason(reasonMessage)
                .paymentId(this.paymentId)
                .build();
    }

    public Boolean checkAvailabilityOfCancel() {
        return !this.reservationState.equals(ACCEPT) && !this.reservationDate.isBeforeThen(LocalDateTime.now());
    }

    public Boolean matchUserId(Long userId) {
        return this.reservedUser.isEqualTo(userId);
    }

    /*
     * 도메인 이벤트 발생 지점 (cancel, accept)
     * 로직 완성 이후 주석 삭제 예정
     * Author : yang_eun_chan
     * Date : 2022/01/05
     * */
    public void cancel() {
        this.reservationState = CANCEL;
    }

    public void accept() {
        if (this.reservationState == CANCEL) throw new CanNotChangeReservationStateException(CANCELED_RESERVATION);

        this.reservationState = ACCEPT;
    }
}
