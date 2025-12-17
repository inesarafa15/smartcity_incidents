package com.smartcity.incident_management.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "historique_feedback")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class HistoriqueFeedback {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "incident_id", nullable = false)
    private Incident incident;

    @Column(name = "feedback_commentaire", columnDefinition = "TEXT")
    private String commentaire;

    @Column(name = "feedback_note")
    private Integer note;

    @Column(name = "feedback_satisfait")
    private Boolean satisfait;

    @Column(name = "date_feedback")
    private LocalDateTime dateFeedback;

    @Column(name = "date_refus")
    private LocalDateTime dateRefus = LocalDateTime.now();
}
