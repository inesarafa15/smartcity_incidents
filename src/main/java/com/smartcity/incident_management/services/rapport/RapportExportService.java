package com.smartcity.incident_management.services.rapport;

import com.smartcity.incident_management.entities.Incident;
import com.smartcity.incident_management.entities.Utilisateur;
import com.smartcity.incident_management.enums.StatutIncident;
import com.smartcity.incident_management.repository.IncidentRepository;
import com.smartcity.incident_management.services.utilisateur.AdminService;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Transactional
public class RapportExportService {

    @Autowired
    private IncidentRepository incidentRepository;

    @Autowired
    private AdminService adminService;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final DateTimeFormatter DATE_ONLY_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter FILE_DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");

    // ========== EXPORT CSV COMPLET ==========
    
    @Transactional(readOnly = true)
    public ResponseEntity<byte[]> exporterCSV(Utilisateur admin) {
        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            if (admin.getDepartement() == null) {
                throw new IllegalStateException("Admin non assigné à un département");
            }

            // Charger tous les incidents avec toutes les relations
            List<Incident> incidents = incidentRepository.findIncidentsCompletsByDepartementId(admin.getDepartement().getId());
            
            // BOM UTF-8 pour Excel
            outputStream.write(0xEF);
            outputStream.write(0xBB);
            outputStream.write(0xBF);
            
            // En-têtes CSV détaillés
            String headers = "ID;Titre;Description;Statut;Priorité;Département;Quartier;Adresse;Latitude;Longitude;" +
                           "Agent Assigné (ID);Agent Assigné (Nom);Agent Assigné (Email);" +
                           "Citoyen (ID);Citoyen (Nom);Citoyen (Email);" +
                           "Date Création;Date Mise à Jour;Date Résolution;Durée Résolution (jours);" +
                           "Feedback Satisfait;Feedback Note;Feedback Commentaire;Date Feedback;" +
                           "Nombre Photos;Nombre Notifications;Dernier Commentaire Agent\n";
            outputStream.write(headers.getBytes("UTF-8"));

            // Données complètes
            for (Incident incident : incidents) {
                StringBuilder row = new StringBuilder();
                
                // Informations de base
                row.append(escapeCsv(String.valueOf(incident.getId()))).append(";");
                row.append(escapeCsv(incident.getTitre())).append(";");
                row.append(escapeCsv(incident.getDescription())).append(";");
                row.append(escapeCsv(incident.getStatut() != null ? incident.getStatut().name() : "")).append(";");
                row.append(escapeCsv(incident.getPriorite() != null ? incident.getPriorite().name() : "")).append(";");
                
                // Département
                String departementNom = "Non assigné";
                if (incident.getDepartement() != null) {
                    departementNom = incident.getDepartement().getLibelle() != null ? 
                        incident.getDepartement().getLibelle() : 
                        (incident.getDepartement().getNom() != null ? 
                         incident.getDepartement().getNom().name() : "Département ID: " + incident.getDepartement().getId());
                }
                row.append(escapeCsv(departementNom)).append(";");
                
                // Quartier
                row.append(escapeCsv(incident.getQuartier() != null ? incident.getQuartier().getNom() : "")).append(";");
                row.append(escapeCsv(incident.getAdresseTextuelle() != null ? incident.getAdresseTextuelle() : "")).append(";");
                row.append(escapeCsv(incident.getLatitude() != null ? incident.getLatitude().toString() : "")).append(";");
                row.append(escapeCsv(incident.getLongitude() != null ? incident.getLongitude().toString() : "")).append(";");
                
                // Agent assigné
                String agentId = "";
                String agentNom = "Non assigné";
                String agentEmail = "";
                if (incident.getAgentAssigne() != null) {
                    agentId = String.valueOf(incident.getAgentAssigne().getId());
                    agentNom = incident.getAgentAssigne().getPrenom() + " " + incident.getAgentAssigne().getNom();
                    agentEmail = incident.getAgentAssigne().getEmail() != null ? incident.getAgentAssigne().getEmail() : "";
                }
                row.append(escapeCsv(agentId)).append(";");
                row.append(escapeCsv(agentNom)).append(";");
                row.append(escapeCsv(agentEmail)).append(";");
                
                // Citoyen (auteur)
                String citoyenId = "";
                String citoyenNom = "";
                String citoyenEmail = "";
                if (incident.getAuteur() != null) {
                    citoyenId = String.valueOf(incident.getAuteur().getId());
                    citoyenNom = incident.getAuteur().getPrenom() + " " + incident.getAuteur().getNom();
                    citoyenEmail = incident.getAuteur().getEmail() != null ? incident.getAuteur().getEmail() : "";
                }
                row.append(escapeCsv(citoyenId)).append(";");
                row.append(escapeCsv(citoyenNom)).append(";");
                row.append(escapeCsv(citoyenEmail)).append(";");
                
                // Dates
                row.append(escapeCsv(incident.getDateCreation() != null ? 
                    incident.getDateCreation().format(DATE_FORMATTER) : "")).append(";");
                row.append(escapeCsv(incident.getDateDerniereMiseAJour() != null ? 
                    incident.getDateDerniereMiseAJour().format(DATE_FORMATTER) : "")).append(";");
                row.append(escapeCsv(incident.getDateResolution() != null ? 
                    incident.getDateResolution().format(DATE_FORMATTER) : "")).append(";");
                
                // Durée de résolution
                String dureeResolution = "";
                if (incident.getDateCreation() != null && incident.getDateResolution() != null) {
                    long jours = ChronoUnit.DAYS.between(incident.getDateCreation(), incident.getDateResolution());
                    dureeResolution = String.valueOf(jours);
                }
                row.append(escapeCsv(dureeResolution)).append(";");
                
                // Feedback
                row.append(escapeCsv(incident.getFeedbackSatisfait() != null ? 
                    (incident.getFeedbackSatisfait() ? "OUI" : "NON") : "")).append(";");
                row.append(escapeCsv(incident.getFeedbackNote() != null ? 
                    String.valueOf(incident.getFeedbackNote()) : "")).append(";");
                row.append(escapeCsv(incident.getFeedbackCommentaire() != null ? 
                    incident.getFeedbackCommentaire() : "")).append(";");
                row.append(escapeCsv(incident.getDateFeedback() != null ? 
                    incident.getDateFeedback().format(DATE_FORMATTER) : "")).append(";");
                
                // Photos
                int nbPhotos = incident.getPhotos() != null ? incident.getPhotos().size() : 0;
                row.append(escapeCsv(String.valueOf(nbPhotos))).append(";");
                
                // Notifications
                int nbNotifications = incident.getNotificationsIncident() != null ? incident.getNotificationsIncident().size() : 0;
                row.append(escapeCsv(String.valueOf(nbNotifications))).append(";");
                
                // Dernier commentaire agent
                String dernierCommentaire = "";
                if (incident.getNotificationsIncident() != null && !incident.getNotificationsIncident().isEmpty()) {
                    dernierCommentaire = incident.getNotificationsIncident().stream()
                        .filter(n -> n.getMessage() != null && !n.getMessage().isEmpty())
                        .max(Comparator.comparing(n -> n.getDateEnvoi()))
                        .map(n -> n.getMessage())
                        .orElse("");
                }
                row.append(escapeCsv(dernierCommentaire)).append("\n");
                
                outputStream.write(row.toString().getBytes("UTF-8"));
            }

            byte[] csvBytes = outputStream.toByteArray();

            HttpHeaders headersObj = new HttpHeaders();
            headersObj.setContentType(MediaType.parseMediaType("text/csv; charset=UTF-8"));
            headersObj.setContentDispositionFormData("attachment", 
                "rapport_complet_incidents_" + LocalDateTime.now().format(FILE_DATE_FORMATTER) + ".csv");

            return ResponseEntity.ok()
                    .headers(headersObj)
                    .body(csvBytes);

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().build();
        }
    }

    // ========== EXPORT PDF COMPLET ==========
    
    @Transactional(readOnly = true)
    public ResponseEntity<byte[]> exporterPDF(Utilisateur admin) {
        try {
            if (admin.getDepartement() == null) {
                throw new IllegalStateException("Admin non assigné à un département");
            }

            // Charger tous les incidents avec toutes les relations
            List<Incident> incidents = incidentRepository.findIncidentsCompletsByDepartementId(admin.getDepartement().getId());
            Map<String, Object> stats = adminService.getStatistiquesDepartement(admin);

            // Générer un rapport HTML complet
            String htmlContent = generateRapportCompletHTML(incidents, stats, admin);
            byte[] pdfBytes = htmlContent.getBytes("UTF-8");

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.TEXT_HTML);
            headers.setContentDispositionFormData("attachment", 
                "rapport_complet_incidents_" + LocalDateTime.now().format(FILE_DATE_FORMATTER) + ".html");

            return ResponseEntity.ok()
                    .headers(headers)
                    .body(pdfBytes);

        } catch (Exception e) {
            e.printStackTrace();
            return generateRapportSimpleFallback(admin);
        }
    }

    // ========== EXPORT EXCEL COMPLET ==========
    
    @Transactional(readOnly = true)
    public ResponseEntity<byte[]> exporterExcel(Utilisateur admin) {
        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
             Workbook workbook = new XSSFWorkbook()) {
            
            if (admin.getDepartement() == null) {
                throw new IllegalStateException("Admin non assigné à un département");
            }

            // Charger tous les incidents avec toutes les relations
            List<Incident> incidents = incidentRepository.findIncidentsCompletsByDepartementId(admin.getDepartement().getId());
            Map<String, Object> stats = adminService.getStatistiquesDepartement(admin);

            // Feuille 1: Résumé et Statistiques
            Sheet resumeSheet = workbook.createSheet("Résumé");
            createResumeSheet(resumeSheet, stats, admin, incidents);

            // Feuille 2: Détails des incidents
            Sheet detailsSheet = workbook.createSheet("Détails Incidents");
            createDetailsSheet(detailsSheet, incidents);

            // Feuille 3: Analyse par agent
            Sheet agentsSheet = workbook.createSheet("Analyse par Agent");
            createAgentsSheet(agentsSheet, incidents);

            // Feuille 4: Chronologie
            Sheet chronoSheet = workbook.createSheet("Chronologie");
            createChronologieSheet(chronoSheet, incidents);

            workbook.write(outputStream);
            
            byte[] excelBytes = outputStream.toByteArray();

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
            headers.setContentDispositionFormData("attachment", 
                "rapport_complet_incidents_" + LocalDateTime.now().format(FILE_DATE_FORMATTER) + ".xlsx");

            return ResponseEntity.ok()
                    .headers(headers)
                    .body(excelBytes);

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().build();
        }
    }

    // ========== FEUILLE RÉSUMÉ EXCEL ==========
    
    private void createResumeSheet(Sheet sheet, Map<String, Object> stats, Utilisateur admin, List<Incident> incidents) {
        // Style titre
        CellStyle titleStyle = sheet.getWorkbook().createCellStyle();
        Font titleFont = sheet.getWorkbook().createFont();
        titleFont.setBold(true);
        titleFont.setFontHeightInPoints((short) 16);
        titleStyle.setFont(titleFont);
        titleStyle.setAlignment(HorizontalAlignment.CENTER);

        // Style sous-titre
        CellStyle subtitleStyle = sheet.getWorkbook().createCellStyle();
        Font subtitleFont = sheet.getWorkbook().createFont();
        subtitleFont.setBold(true);
        subtitleFont.setFontHeightInPoints((short) 12);
        subtitleStyle.setFont(subtitleFont);

        // Style valeur
        CellStyle valueStyle = sheet.getWorkbook().createCellStyle();
        valueStyle.setAlignment(HorizontalAlignment.RIGHT);

        int rowNum = 0;

        // Titre principal
        Row titleRow = sheet.createRow(rowNum++);
        Cell titleCell = titleRow.createCell(0);
        String departementNom = getDepartementNom(admin);
        titleCell.setCellValue("RAPPORT COMPLET D'INCIDENTS - " + departementNom);
        titleCell.setCellStyle(titleStyle);
        sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, 4));

        rowNum++; // Ligne vide

        // Informations générales
        Row infoRow1 = sheet.createRow(rowNum++);
        infoRow1.createCell(0).setCellValue("Date de génération:");
        infoRow1.createCell(1).setCellValue(LocalDateTime.now().format(DATE_FORMATTER));
        
        Row infoRow2 = sheet.createRow(rowNum++);
        infoRow2.createCell(0).setCellValue("Période couverte:");
        infoRow2.createCell(1).setCellValue(getPeriodeCouverture(incidents));
        
        Row infoRow3 = sheet.createRow(rowNum++);
        infoRow3.createCell(0).setCellValue("Nombre total d'incidents:");
        infoRow3.createCell(1).setCellValue(incidents.size());

        rowNum++; // Ligne vide

        // Statistiques principales
        Row statsTitleRow = sheet.createRow(rowNum++);
        statsTitleRow.createCell(0).setCellValue("STATISTIQUES PRINCIPALES");
        statsTitleRow.getCell(0).setCellStyle(subtitleStyle);
        
        if (stats != null) {
            createStatRow(sheet, rowNum++, "Total Incidents", stats.get("totalIncidents"));
            createStatRow(sheet, rowNum++, "Incidents Résolus", stats.get("incidentsResolus"));
            createStatRow(sheet, rowNum++, "Incidents en Cours", stats.get("incidentsEnCours"));
            createStatRow(sheet, rowNum++, "Incidents en Attente", stats.get("incidentsEnAttente"));
            
            // Calcul du taux de résolution
            long total = ((Number) stats.getOrDefault("totalIncidents", 0)).longValue();
            long resolus = ((Number) stats.getOrDefault("incidentsResolus", 0)).longValue();
            double tauxResolution = total > 0 ? (resolus * 100.0 / total) : 0;
            createStatRow(sheet, rowNum++, "Taux de résolution", String.format("%.1f%%", tauxResolution));
            
            createStatRow(sheet, rowNum++, "Agents Disponibles", stats.get("agentsDisponibles"));
            createStatRow(sheet, rowNum++, "Total Agents", stats.get("totalAgents"));
        }

        rowNum++; // Ligne vide

        // Répartition par statut
        Row repartTitleRow = sheet.createRow(rowNum++);
        repartTitleRow.createCell(0).setCellValue("RÉPARTITION PAR STATUT");
        repartTitleRow.getCell(0).setCellStyle(subtitleStyle);
        
        if (incidents != null && !incidents.isEmpty()) {
            Map<StatutIncident, Long> repartition = incidents.stream()
                .filter(i -> i.getStatut() != null)
                .collect(Collectors.groupingBy(Incident::getStatut, Collectors.counting()));
            
            for (StatutIncident statut : StatutIncident.values()) {
                long count = repartition.getOrDefault(statut, 0L);
                double pourcentage = incidents.size() > 0 ? (count * 100.0 / incidents.size()) : 0;
                Row row = sheet.createRow(rowNum++);
                row.createCell(0).setCellValue(statut.name());
                row.createCell(1).setCellValue(count);
                row.createCell(2).setCellValue(String.format("%.1f%%", pourcentage));
            }
        }

        // Auto-size
        for (int i = 0; i < 5; i++) {
            sheet.autoSizeColumn(i);
        }
    }

    // ========== FEUILLE DÉTAILS INCIDENTS EXCEL ==========
    
    private void createDetailsSheet(Sheet sheet, List<Incident> incidents) {
        // Style en-tête
        CellStyle headerStyle = sheet.getWorkbook().createCellStyle();
        Font headerFont = sheet.getWorkbook().createFont();
        headerFont.setBold(true);
        headerFont.setColor(IndexedColors.WHITE.getIndex());
        headerStyle.setFont(headerFont);
        headerStyle.setFillForegroundColor(IndexedColors.DARK_BLUE.getIndex());
        headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        headerStyle.setAlignment(HorizontalAlignment.CENTER);
        headerStyle.setBorderBottom(BorderStyle.THIN);
        headerStyle.setBorderTop(BorderStyle.THIN);
        headerStyle.setBorderLeft(BorderStyle.THIN);
        headerStyle.setBorderRight(BorderStyle.THIN);

        // En-têtes détaillés
        Row headerRow = sheet.createRow(0);
        String[] headers = {
            "ID", "Titre", "Statut", "Priorité", "Date Création", 
            "Agent Assigné", "Email Agent", "Citoyen", "Email Citoyen",
            "Quartier", "Adresse", "Durée (jours)", "Feedback Note", 
            "Feedback Commentaire", "Photos", "Notifications"
        };
        
        for (int i = 0; i < headers.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(headerStyle);
        }

        // Données détaillées
        if (incidents != null) {
            int rowNum = 1;
            for (Incident incident : incidents) {
                Row row = sheet.createRow(rowNum++);
                
                int col = 0;
                // ID
                row.createCell(col++).setCellValue(incident.getId() != null ? incident.getId() : 0);
                
                // Titre
                row.createCell(col++).setCellValue(incident.getTitre() != null ? incident.getTitre() : "");
                
                // Statut
                row.createCell(col++).setCellValue(incident.getStatut() != null ? incident.getStatut().name() : "");
                
                // Priorité
                row.createCell(col++).setCellValue(incident.getPriorite() != null ? incident.getPriorite().name() : "");
                
                // Date Création
                row.createCell(col++).setCellValue(incident.getDateCreation() != null ? 
                    incident.getDateCreation().format(DATE_ONLY_FORMATTER) : "");
                
                // Agent Assigné
                String agentNom = "Non assigné";
                if (incident.getAgentAssigne() != null) {
                    agentNom = incident.getAgentAssigne().getPrenom() + " " + incident.getAgentAssigne().getNom();
                }
                row.createCell(col++).setCellValue(agentNom);
                
                // Email Agent
                String agentEmail = "";
                if (incident.getAgentAssigne() != null && incident.getAgentAssigne().getEmail() != null) {
                    agentEmail = incident.getAgentAssigne().getEmail();
                }
                row.createCell(col++).setCellValue(agentEmail);
                
                // Citoyen
                String citoyenNom = "";
                if (incident.getAuteur() != null) {
                    citoyenNom = incident.getAuteur().getPrenom() + " " + incident.getAuteur().getNom();
                }
                row.createCell(col++).setCellValue(citoyenNom);
                
                // Email Citoyen
                String citoyenEmail = "";
                if (incident.getAuteur() != null && incident.getAuteur().getEmail() != null) {
                    citoyenEmail = incident.getAuteur().getEmail();
                }
                row.createCell(col++).setCellValue(citoyenEmail);
                
                // Quartier
                row.createCell(col++).setCellValue(incident.getQuartier() != null ? incident.getQuartier().getNom() : "");
                
                // Adresse
                row.createCell(col++).setCellValue(incident.getAdresseTextuelle() != null ? incident.getAdresseTextuelle() : "");
                
                // Durée de résolution
                String duree = "";
                if (incident.getDateCreation() != null && incident.getDateResolution() != null) {
                    long jours = ChronoUnit.DAYS.between(incident.getDateCreation(), incident.getDateResolution());
                    duree = String.valueOf(jours);
                }
                row.createCell(col++).setCellValue(duree);
                
                // Feedback Note
                row.createCell(col++).setCellValue(incident.getFeedbackNote() != null ? incident.getFeedbackNote() : 0);
                
                // Feedback Commentaire
                row.createCell(col++).setCellValue(incident.getFeedbackCommentaire() != null ? 
                    truncateText(incident.getFeedbackCommentaire(), 50) : "");
                
                // Photos
                int nbPhotos = incident.getPhotos() != null ? incident.getPhotos().size() : 0;
                row.createCell(col++).setCellValue(nbPhotos);
                
                // Notifications
                int nbNotifications = incident.getNotificationsIncident() != null ? incident.getNotificationsIncident().size() : 0;
                row.createCell(col++).setCellValue(nbNotifications);
            }
        }

        // Auto-size
        for (int i = 0; i < headers.length; i++) {
            sheet.autoSizeColumn(i);
        }
    }

    // ========== FEUILLE ANALYSE PAR AGENT EXCEL ==========
    
    private void createAgentsSheet(Sheet sheet, List<Incident> incidents) {
        CellStyle headerStyle = createHeaderStyle(sheet.getWorkbook());
        
        // En-têtes
        Row headerRow = sheet.createRow(0);
        String[] headers = {
            "Agent", "Email", "Incidents Assignés", "Incidents Résolus", 
            "Incidents en Cours", "Taux Résolution", "Note Moyenne Feedback",
            "Temps Moyen Résolution (jours)"
        };
        
        for (int i = 0; i < headers.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(headerStyle);
        }
        
        // Grouper par agent
        Map<String, List<Incident>> incidentsParAgent = incidents.stream()
            .filter(i -> i.getAgentAssigne() != null)
            .collect(Collectors.groupingBy(
                i -> i.getAgentAssigne().getId() + "|" + 
                     i.getAgentAssigne().getPrenom() + " " + i.getAgentAssigne().getNom() + "|" +
                     (i.getAgentAssigne().getEmail() != null ? i.getAgentAssigne().getEmail() : "")
            ));
        
        int rowNum = 1;
        for (Map.Entry<String, List<Incident>> entry : incidentsParAgent.entrySet()) {
            String[] agentInfo = entry.getKey().split("\\|");
            String agentId = agentInfo[0];
            String agentNom = agentInfo[1];
            String agentEmail = agentInfo[2];
            List<Incident> incidentsAgent = entry.getValue();
            
            Row row = sheet.createRow(rowNum++);
            
            int col = 0;
            // Agent
            row.createCell(col++).setCellValue(agentNom);
            
            // Email
            row.createCell(col++).setCellValue(agentEmail);
            
            // Incidents Assignés
            long totalAssignes = incidentsAgent.size();
            row.createCell(col++).setCellValue(totalAssignes);
            
            // Incidents Résolus
            long resolus = incidentsAgent.stream()
                .filter(i -> i.getStatut() == StatutIncident.RESOLU || i.getStatut() == StatutIncident.CLOTURE)
                .count();
            row.createCell(col++).setCellValue(resolus);
            
            // Incidents en Cours
            long enCours = incidentsAgent.stream()
                .filter(i -> i.getStatut() == StatutIncident.PRIS_EN_CHARGE || i.getStatut() == StatutIncident.EN_RESOLUTION)
                .count();
            row.createCell(col++).setCellValue(enCours);
            
            // Taux Résolution
            double tauxResolution = totalAssignes > 0 ? (resolus * 100.0 / totalAssignes) : 0;
            row.createCell(col++).setCellValue(String.format("%.1f%%", tauxResolution));
            
            // Note Moyenne Feedback
            double noteMoyenne = incidentsAgent.stream()
                .filter(i -> i.getFeedbackNote() != null)
                .mapToInt(Incident::getFeedbackNote)
                .average()
                .orElse(0);
            row.createCell(col++).setCellValue(String.format("%.1f/5", noteMoyenne));
            
            // Temps Moyen Résolution
            double tempsMoyen = incidentsAgent.stream()
                .filter(i -> i.getDateCreation() != null && i.getDateResolution() != null)
                .mapToLong(i -> ChronoUnit.DAYS.between(i.getDateCreation(), i.getDateResolution()))
                .average()
                .orElse(0);
            row.createCell(col++).setCellValue(String.format("%.1f", tempsMoyen));
        }
        
        // Auto-size
        for (int i = 0; i < headers.length; i++) {
            sheet.autoSizeColumn(i);
        }
    }

    // ========== FEUILLE CHRONOLOGIE EXCEL ==========
    
    private void createChronologieSheet(Sheet sheet, List<Incident> incidents) {
        CellStyle headerStyle = createHeaderStyle(sheet.getWorkbook());
        
        // En-têtes
        Row headerRow = sheet.createRow(0);
        String[] headers = {
            "Date", "ID Incident", "Titre", "Statut", "Agent", "Action"
        };
        
        for (int i = 0; i < headers.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(headerStyle);
        }
        
        // Trier par date de création
        List<Incident> incidentsTries = incidents.stream()
            .sorted(Comparator.comparing(Incident::getDateCreation))
            .collect(Collectors.toList());
        
        int rowNum = 1;
        for (Incident incident : incidentsTries) {
            Row row = sheet.createRow(rowNum++);
            
            // Date
            row.createCell(0).setCellValue(incident.getDateCreation() != null ? 
                incident.getDateCreation().format(DATE_ONLY_FORMATTER) : "");
            
            // ID Incident
            row.createCell(1).setCellValue(incident.getId());
            
            // Titre
            row.createCell(2).setCellValue(incident.getTitre() != null ? incident.getTitre() : "");
            
            // Statut
            row.createCell(3).setCellValue(incident.getStatut() != null ? incident.getStatut().name() : "");
            
            // Agent
            String agentNom = "Non assigné";
            if (incident.getAgentAssigne() != null) {
                agentNom = incident.getAgentAssigne().getPrenom() + " " + incident.getAgentAssigne().getNom();
            }
            row.createCell(4).setCellValue(agentNom);
            
            // Action
            String action = "Créé";
            if (incident.getStatut() == StatutIncident.PRIS_EN_CHARGE) {
                action = "Assigné à " + agentNom;
            } else if (incident.getStatut() == StatutIncident.RESOLU) {
                action = "Résolu";
            } else if (incident.getStatut() == StatutIncident.CLOTURE) {
                action = "Clôturé";
            }
            row.createCell(5).setCellValue(action);
        }
        
        // Auto-size
        for (int i = 0; i < headers.length; i++) {
            sheet.autoSizeColumn(i);
        }
    }

    // ========== RAPPORT HTML COMPLET ==========
    
    private String generateRapportCompletHTML(List<Incident> incidents, Map<String, Object> stats, Utilisateur admin) {
        StringBuilder html = new StringBuilder();
        
        html.append("<!DOCTYPE html>\n");
        html.append("<html lang='fr'>\n");
        html.append("<head>\n");
        html.append("    <meta charset='UTF-8'>\n");
        html.append("    <meta name='viewport' content='width=device-width, initial-scale=1.0'>\n");
        html.append("    <title>Rapport Complet d'Incidents</title>\n");
        html.append("    <style>\n");
        html.append("        body { font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif; margin: 0; padding: 20px; background-color: #f5f7fa; }\n");
        html.append("        .container { max-width: 1200px; margin: 0 auto; background: white; padding: 30px; border-radius: 10px; box-shadow: 0 2px 20px rgba(0,0,0,0.1); }\n");
        html.append("        .header { text-align: center; margin-bottom: 40px; padding-bottom: 20px; border-bottom: 3px solid #3498db; }\n");
        html.append("        .title { font-size: 28px; font-weight: bold; color: #2c3e50; margin-bottom: 10px; }\n");
        html.append("        .subtitle { font-size: 16px; color: #7f8c8d; margin-bottom: 5px; }\n");
        html.append("        .section { margin-bottom: 40px; }\n");
        html.append("        .section-title { font-size: 20px; font-weight: bold; color: #34495e; border-left: 5px solid #3498db; padding-left: 15px; margin-bottom: 20px; }\n");
        html.append("        .stats-grid { display: grid; grid-template-columns: repeat(auto-fit, minmax(250px, 1fr)); gap: 20px; margin-bottom: 30px; }\n");
        html.append("        .stat-card { background: #f8f9fa; padding: 20px; border-radius: 8px; border-left: 4px solid #3498db; }\n");
        html.append("        .stat-number { font-size: 32px; font-weight: bold; color: #2c3e50; margin-bottom: 5px; }\n");
        html.append("        .stat-label { font-size: 14px; color: #7f8c8d; }\n");
        html.append("        table { width: 100%; border-collapse: collapse; margin-bottom: 20px; }\n");
        html.append("        th { background-color: #2c3e50; color: white; padding: 12px 15px; text-align: left; font-weight: 600; }\n");
        html.append("        td { padding: 10px 15px; border-bottom: 1px solid #e0e0e0; }\n");
        html.append("        tr:nth-child(even) { background-color: #f9f9f9; }\n");
        html.append("        tr:hover { background-color: #f0f7ff; }\n");
        html.append("        .badge { display: inline-block; padding: 4px 10px; border-radius: 20px; font-size: 12px; font-weight: 600; margin-right: 5px; }\n");
        html.append("        .badge-urgent { background-color: #e74c3c; color: white; }\n");
        html.append("        .badge-high { background-color: #f39c12; color: white; }\n");
        html.append("        .badge-medium { background-color: #3498db; color: white; }\n");
        html.append("        .badge-low { background-color: #95a5a6; color: white; }\n");
        html.append("        .badge-status { background-color: #2ecc71; color: white; }\n");
        html.append("        .agent-info { background: #e8f4fd; padding: 15px; border-radius: 8px; margin-bottom: 20px; }\n");
        html.append("        .feedback { background: #fef9e7; padding: 15px; border-radius: 8px; margin-bottom: 20px; }\n");
        html.append("        .footer { margin-top: 50px; text-align: center; color: #95a5a6; font-size: 12px; padding-top: 20px; border-top: 1px solid #eee; }\n");
        html.append("        .logo { text-align: center; margin-bottom: 30px; }\n");
        html.append("        .print-btn { display: inline-block; background: #3498db; color: white; padding: 10px 20px; border-radius: 5px; text-decoration: none; margin-top: 20px; }\n");
        html.append("        .print-btn:hover { background: #2980b9; }\n");
        html.append("        @media print { .print-btn { display: none; } }\n");
        html.append("    </style>\n");
        html.append("</head>\n");
        html.append("<body>\n");
        
        html.append("    <div class='container'>\n");
        
        // Logo et en-tête
        html.append("        <div class='logo'>\n");
        html.append("            <h1 style='color: #2c3e50; margin-bottom: 10px;'>🏙️ SMART CITY</h1>\n");
        html.append("            <h3 style='color: #7f8c8d; margin-top: 0;'>Plateforme de Gestion d'Incidents</h3>\n");
        html.append("        </div>\n");
        
        html.append("        <div class='header'>\n");
        html.append("            <div class='title'>RAPPORT COMPLET D'INCIDENTS</div>\n");
        
        String departementNom = getDepartementNom(admin);
        html.append("            <div class='subtitle'>Département: ").append(departementNom).append("</div>\n");
        html.append("            <div class='subtitle'>Généré le: ").append(LocalDateTime.now().format(DATE_FORMATTER)).append("</div>\n");
        html.append("            <div class='subtitle'>Période: ").append(getPeriodeCouverture(incidents)).append("</div>\n");
        html.append("            <a href='javascript:window.print()' class='print-btn'>📄 Imprimer ce rapport</a>\n");
        html.append("        </div>\n");
        
        // Section Statistiques
        html.append("        <div class='section'>\n");
        html.append("            <div class='section-title'>📊 ANALYSE STATISTIQUE</div>\n");
        html.append("            <div class='stats-grid'>\n");
        
        if (stats != null) {
            html.append("                <div class='stat-card'>\n");
            html.append("                    <div class='stat-number'>").append(stats.getOrDefault("totalIncidents", 0)).append("</div>\n");
            html.append("                    <div class='stat-label'>Total Incidents</div>\n");
            html.append("                </div>\n");
            
            html.append("                <div class='stat-card'>\n");
            html.append("                    <div class='stat-number'>").append(stats.getOrDefault("incidentsResolus", 0)).append("</div>\n");
            html.append("                    <div class='stat-label'>Incidents Résolus</div>\n");
            html.append("                </div>\n");
            
            html.append("                <div class='stat-card'>\n");
            long total = ((Number) stats.getOrDefault("totalIncidents", 0)).longValue();
            long resolus = ((Number) stats.getOrDefault("incidentsResolus", 0)).longValue();
            double taux = total > 0 ? (resolus * 100.0 / total) : 0;
            html.append("                    <div class='stat-number'>").append(String.format("%.1f%%", taux)).append("</div>\n");
            html.append("                    <div class='stat-label'>Taux de Résolution</div>\n");
            html.append("                </div>\n");
            
            html.append("                <div class='stat-card'>\n");
            html.append("                    <div class='stat-number'>").append(stats.getOrDefault("incidentsEnAttente", 0)).append("</div>\n");
            html.append("                    <div class='stat-label'>En Attente d'Affectation</div>\n");
            html.append("                </div>\n");
            
            html.append("                <div class='stat-card'>\n");
            html.append("                    <div class='stat-number'>").append(stats.getOrDefault("agentsDisponibles", 0)).append("/").append(stats.getOrDefault("totalAgents", 0)).append("</div>\n");
            html.append("                    <div class='stat-label'>Agents Disponibles / Total</div>\n");
            html.append("                </div>\n");
        }
        
        html.append("            </div>\n");
        html.append("        </div>\n");
        
        // Section Analyse par Agent
        html.append("        <div class='section'>\n");
        html.append("            <div class='section-title'>👥 PERFORMANCE PAR AGENT</div>\n");
        
        Map<String, List<Incident>> incidentsParAgent = incidents.stream()
            .filter(i -> i.getAgentAssigne() != null)
            .collect(Collectors.groupingBy(
                i -> i.getAgentAssigne().getPrenom() + " " + i.getAgentAssigne().getNom()
            ));
        
        if (!incidentsParAgent.isEmpty()) {
            html.append("            <table>\n");
            html.append("                <thead>\n");
            html.append("                    <tr>\n");
            html.append("                        <th>Agent</th>\n");
            html.append("                        <th>Incidents Assignés</th>\n");
            html.append("                        <th>Résolus</th>\n");
            html.append("                        <th>En Cours</th>\n");
            html.append("                        <th>Taux Résolution</th>\n");
            html.append("                        <th>Note Moyenne</th>\n");
            html.append("                    </tr>\n");
            html.append("                </thead>\n");
            html.append("                <tbody>\n");
            
            for (Map.Entry<String, List<Incident>> entry : incidentsParAgent.entrySet()) {
                String agentNom = entry.getKey();
                List<Incident> incidentsAgent = entry.getValue();
                
                long totalAssignes = incidentsAgent.size();
                long resolus = incidentsAgent.stream()
                    .filter(i -> i.getStatut() == StatutIncident.RESOLU || i.getStatut() == StatutIncident.CLOTURE)
                    .count();
                long enCours = incidentsAgent.stream()
                    .filter(i -> i.getStatut() == StatutIncident.PRIS_EN_CHARGE || i.getStatut() == StatutIncident.EN_RESOLUTION)
                    .count();
                double taux = totalAssignes > 0 ? (resolus * 100.0 / totalAssignes) : 0;
                double noteMoyenne = incidentsAgent.stream()
                    .filter(i -> i.getFeedbackNote() != null)
                    .mapToInt(Incident::getFeedbackNote)
                    .average()
                    .orElse(0);
                
                html.append("                    <tr>\n");
                html.append("                        <td><strong>").append(agentNom).append("</strong></td>\n");
                html.append("                        <td>").append(totalAssignes).append("</td>\n");
                html.append("                        <td>").append(resolus).append("</td>\n");
                html.append("                        <td>").append(enCours).append("</td>\n");
                html.append("                        <td>").append(String.format("%.1f%%", taux)).append("</td>\n");
                html.append("                        <td>").append(String.format("%.1f/5", noteMoyenne)).append("</td>\n");
                html.append("                    </tr>\n");
            }
            
            html.append("                </tbody>\n");
            html.append("            </table>\n");
        } else {
            html.append("            <p style='color: #7f8c8d; font-style: italic;'>Aucun agent assigné pour le moment.</p>\n");
        }
        html.append("        </div>\n");
        
        // Section Détail des Incidents
        html.append("        <div class='section'>\n");
        html.append("            <div class='section-title'>📋 DÉTAIL DES INCIDENTS (").append(incidents.size()).append(")</div>\n");
        
        if (!incidents.isEmpty()) {
            html.append("            <table>\n");
            html.append("                <thead>\n");
            html.append("                    <tr>\n");
            html.append("                        <th>ID</th>\n");
            html.append("                        <th>Titre</th>\n");
            html.append("                        <th>Priorité</th>\n");
            html.append("                        <th>Statut</th>\n");
            html.append("                        <th>Agent Assigné</th>\n");
            html.append("                        <th>Citoyen</th>\n");
            html.append("                        <th>Date Création</th>\n");
            html.append("                        <th>Durée</th>\n");
            html.append("                        <th>Feedback</th>\n");
            html.append("                    </tr>\n");
            html.append("                </thead>\n");
            html.append("                <tbody>\n");
            
            for (Incident incident : incidents) {
                // Priorité badge
                String prioriteBadge = "";
                if (incident.getPriorite() != null) {
                    switch (incident.getPriorite()) {
                        case CRITIQUE:
                            prioriteBadge = "<span class='badge badge-urgent'>URGENTE</span>";
                            break;
                        case ELEVEE:
                            prioriteBadge = "<span class='badge badge-high'>HAUTE</span>";
                            break;
                        case MOYENNE:
                            prioriteBadge = "<span class='badge badge-medium'>MOYENNE</span>";
                            break;
                        case FAIBLE:  // <- add this explicitly if it exists
                            prioriteBadge = "<span class='badge badge-low'>BASSE</span>";
                            break;
                    }
                }
                
                // Agent
                String agentNom = "Non assigné";
                if (incident.getAgentAssigne() != null) {
                    agentNom = incident.getAgentAssigne().getPrenom() + " " + incident.getAgentAssigne().getNom();
                }
                
                // Citoyen
                String citoyenNom = "";
                if (incident.getAuteur() != null) {
                    citoyenNom = incident.getAuteur().getPrenom() + " " + incident.getAuteur().getNom();
                }
                
                // Durée
                String duree = "En cours";
                if (incident.getDateCreation() != null && incident.getDateResolution() != null) {
                    long jours = ChronoUnit.DAYS.between(incident.getDateCreation(), incident.getDateResolution());
                    duree = jours + " jours";
                }
                
                // Feedback
                String feedback = "Aucun";
                if (incident.getFeedbackNote() != null) {
                    feedback = incident.getFeedbackNote() + "/5";
                    if (incident.getFeedbackCommentaire() != null && !incident.getFeedbackCommentaire().isEmpty()) {
                        feedback += " <small style='color:#7f8c8d;'>(" + truncateText(incident.getFeedbackCommentaire(), 30) + ")</small>";
                    }
                }
                
                html.append("                    <tr>\n");
                html.append("                        <td><strong>#").append(incident.getId()).append("</strong></td>\n");
                html.append("                        <td>").append(incident.getTitre() != null ? incident.getTitre() : "").append("</td>\n");
                html.append("                        <td>").append(prioriteBadge).append("</td>\n");
                html.append("                        <td><span class='badge badge-status'>").append(incident.getStatut() != null ? incident.getStatut().name() : "").append("</span></td>\n");
                html.append("                        <td>").append(agentNom).append("</td>\n");
                html.append("                        <td>").append(citoyenNom).append("</td>\n");
                html.append("                        <td>").append(incident.getDateCreation() != null ? 
                    incident.getDateCreation().format(DATE_ONLY_FORMATTER) : "").append("</td>\n");
                html.append("                        <td>").append(duree).append("</td>\n");
                html.append("                        <td>").append(feedback).append("</td>\n");
                html.append("                    </tr>\n");
            }
            
            html.append("                </tbody>\n");
            html.append("            </table>\n");
        } else {
            html.append("            <p style='color: #7f8c8d; font-style: italic;'>Aucun incident à afficher.</p>\n");
        }
        html.append("        </div>\n");
        
        // Section Informations Techniques
        html.append("        <div class='section'>\n");
        html.append("            <div class='section-title'>⚙️ INFORMATIONS TECHNIQUES</div>\n");
        
        html.append("            <div class='agent-info'>\n");
        html.append("                <strong>📊 Métriques:</strong><br>\n");
        html.append("                • Nombre total d'incidents: ").append(incidents.size()).append("<br>\n");
        
        if (!incidents.isEmpty()) {
            long avecPhotos = incidents.stream()
                .filter(i -> i.getPhotos() != null && !i.getPhotos().isEmpty())
                .count();
            html.append("                • Incidents avec photos: ").append(avecPhotos).append(" (").append(String.format("%.1f%%", incidents.size() > 0 ? (avecPhotos * 100.0 / incidents.size()) : 0)).append(")<br>\n");
            
            long avecFeedback = incidents.stream()
                .filter(i -> i.getFeedbackNote() != null)
                .count();
            html.append("                • Incidents avec feedback: ").append(avecFeedback).append(" (").append(String.format("%.1f%%", incidents.size() > 0 ? (avecFeedback * 100.0 / incidents.size()) : 0)).append(")<br>\n");
            
            double tempsMoyen = incidents.stream()
                .filter(i -> i.getDateCreation() != null && i.getDateResolution() != null)
                .mapToLong(i -> ChronoUnit.DAYS.between(i.getDateCreation(), i.getDateResolution()))
                .average()
                .orElse(0);
            html.append("                • Temps moyen de résolution: ").append(String.format("%.1f", tempsMoyen)).append(" jours<br>\n");
        }
        html.append("            </div>\n");
        
        html.append("            <div class='feedback'>\n");
        html.append("                <strong>📈 Indicateurs de Performance:</strong><br>\n");
        html.append("                • Rapport généré automatiquement par le système Smart City<br>\n");
        html.append("                • Données actualisées en temps réel<br>\n");
        html.append("                • Compatible avec tous les navigateurs<br>\n");
        html.append("                • Format HTML pour une consultation optimale<br>\n");
        html.append("            </div>\n");
        html.append("        </div>\n");
        
        // Pied de page
        html.append("        <div class='footer'>\n");
        html.append("            <p>--- Document généré automatiquement par Smart City Incident Management ---</p>\n");
        html.append("            <p>Confidentialité: Ce document contient des informations sensibles. Merci de le traiter avec confidentialité.</p>\n");
        html.append("            <p>Pour toute question ou réclamation, contactez l'administration au 01 23 45 67 89 ou par email à admin@smartcity.fr</p>\n");
        html.append("            <p>Page 1 sur 1 | ID de rapport: ").append(LocalDateTime.now().format(FILE_DATE_FORMATTER)).append("</p>\n");
        html.append("        </div>\n");
        
        html.append("    </div>\n"); // Fin container
        
        // Script pour impression
        html.append("    <script>\n");
        html.append("        // Auto-impression optionnelle\n");
        html.append("        // window.onload = function() {\n");
        html.append("        //     setTimeout(function() {\n");
        html.append("        //         window.print();\n");
        html.append("        //     }, 1000);\n");
        html.append("        // };\n");
        html.append("    </script>\n");
        
        html.append("</body>\n");
        html.append("</html>\n");
        
        return html.toString();
    }

    // ========== MÉTHODES UTILITAIRES ==========
    
    private CellStyle createHeaderStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setBold(true);
        font.setColor(IndexedColors.WHITE.getIndex());
        style.setFont(font);
        style.setFillForegroundColor(IndexedColors.DARK_BLUE.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        return style;
    }
    
    private void createStatRow(Sheet sheet, int rowNum, String label, Object value) {
        Row row = sheet.createRow(rowNum);
        row.createCell(0).setCellValue(label);
        
        if (value != null) {
            if (value instanceof Number) {
                row.createCell(1).setCellValue(((Number) value).doubleValue());
            } else {
                row.createCell(1).setCellValue(value.toString());
            }
        } else {
            row.createCell(1).setCellValue(0);
        }
    }
    
    private String getDepartementNom(Utilisateur admin) {
        if (admin == null || admin.getDepartement() == null) {
            return "Non assigné";
        }
        
        try {
            if (admin.getDepartement().getLibelle() != null && !admin.getDepartement().getLibelle().isEmpty()) {
                return admin.getDepartement().getLibelle();
            } else if (admin.getDepartement().getNom() != null) {
                return admin.getDepartement().getNom().name();
            }
        } catch (Exception e) {
            // Ignorer les erreurs de lazy loading
        }
        
        return "Département ID: " + admin.getDepartement().getId();
    }
    
    private String getPeriodeCouverture(List<Incident> incidents) {
        if (incidents == null || incidents.isEmpty()) {
            return "Aucune donnée";
        }
        
        LocalDateTime plusAncien = incidents.stream()
            .map(Incident::getDateCreation)
            .filter(date -> date != null)
            .min(LocalDateTime::compareTo)
            .orElse(LocalDateTime.now());
        
        LocalDateTime plusRecent = incidents.stream()
            .map(Incident::getDateCreation)
            .filter(date -> date != null)
            .max(LocalDateTime::compareTo)
            .orElse(LocalDateTime.now());
        
        return plusAncien.format(DATE_ONLY_FORMATTER) + " - " + plusRecent.format(DATE_ONLY_FORMATTER);
    }
    
    private String truncateText(String text, int maxLength) {
        if (text == null) return "";
        if (text.length() <= maxLength) return text;
        return text.substring(0, maxLength) + "...";
    }
    
    private String escapeCsv(String value) {
        if (value == null) {
            return "";
        }
        value = value.replace("\"", "\"\"");
        if (value.contains(";") || value.contains("\"") || value.contains("\n") || value.contains("\r")) {
            return "\"" + value + "\"";
        }
        return value;
    }
    
    private ResponseEntity<byte[]> generateRapportSimpleFallback(Utilisateur admin) {
        try {
            StringBuilder content = new StringBuilder();
            
            content.append("==================================================\n");
            content.append("RAPPORT D'INCIDENTS - VERSION SIMPLIFIÉE\n");
            content.append("DÉPARTEMENT: ").append(getDepartementNom(admin)).append("\n");
            content.append("==================================================\n\n");
            content.append("Date de génération: ").append(LocalDateTime.now().format(DATE_FORMATTER)).append("\n\n");
            content.append("NOTE: Le rapport complet n'est pas disponible pour le moment.\n");
            content.append("Veuillez contacter l'administrateur système pour plus d'informations.\n\n");
            content.append("==================================================\n");
            content.append("Fin du rapport\n");
            content.append("==================================================\n");
            
            byte[] bytes = content.toString().getBytes("UTF-8");

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.TEXT_PLAIN);
            headers.setContentDispositionFormData("attachment", 
                "rapport_simplifie_" + LocalDateTime.now().format(FILE_DATE_FORMATTER) + ".txt");

            return ResponseEntity.ok()
                    .headers(headers)
                    .body(bytes);

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().build();
        }
    }
}