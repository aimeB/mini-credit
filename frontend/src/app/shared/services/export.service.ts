import { Injectable } from '@angular/core';
import * as XLSX from 'xlsx';
import jsPDF from 'jspdf';
import 'jspdf-autotable';
import { RecetteTerrainResponse } from '../../features/recettes/models';
import { CollecteTerrainResponse, CollecteMembreLigneResponse } from '../../features/recettes/models/collecte-terrain.model';

export interface JsPDFWithAutoTable extends jsPDF {
  autoTable: (options: any) => JsPDFWithAutoTable;
}

@Injectable({
  providedIn: 'root'
})
export class ExportService {

  private writeWorkbook(workbook: XLSX.WorkBook, filename: string): void {
    XLSX.writeFile(workbook, filename);
  }

  exportCollectesDetailByMembreExcel(collectes: CollecteTerrainResponse[], filename: string = 'collectes-detail-par-membre.xlsx'): void {
    if (!collectes || collectes.length === 0) {
      alert('Aucune donnée à exporter');
      return;
    }

    const rows: Array<Record<string, string | number>> = collectes.reduce((acc, c) => {
      const lignes = c.lignes || [];
      if (lignes.length === 0) {
        acc.push({
          'Date': new Date(c.dateCollecte).toLocaleDateString('fr-FR'),
          'Agent': c.agentTerrainNom || c.agentTerrainId,
          'Site': c.siteNom || c.siteId,
          'Antenne': c.antenneId,
          'Statut': c.statut,
          'Membre': '-',
          'Type Ligne': 'N/A',
          'Montant': 0,
          'Quantité': 0,
          'Total Ligne': 0,
          'Espèces Remises': c.especesRemises,
          'Écart Trésorerie': c.ecartTresorerie,
          'Observation': c.observations || ''
        });
        return acc;
      }

      lignes.forEach((l: CollecteMembreLigneResponse) => {
        acc.push({
          'Date': new Date(c.dateCollecte).toLocaleDateString('fr-FR'),
          'Agent': c.agentTerrainNom || c.agentTerrainId,
          'Site': c.siteNom || c.siteId,
          'Antenne': c.antenneId,
          'Statut': c.statut,
          'Membre': l.membreNom || l.membreCode || l.membreId,
          'Type Ligne': l.typeLigne,
          'Montant': l.montant || 0,
          'Quantité': l.quantite || 0,
          'Total Ligne': l.totalLigne ?? ((l.montant || 0) * (l.quantite || 0)),
          'Espèces Remises': c.especesRemises,
          'Écart Trésorerie': c.ecartTresorerie,
          'Observation': c.observations || ''
        });
      });
      return acc;
    }, [] as Array<Record<string, string | number>>);

    const worksheet: XLSX.WorkSheet = XLSX.utils.json_to_sheet(rows);
    const workbook: XLSX.WorkBook = {
      Sheets: { 'CollectesDetailMembres': worksheet },
      SheetNames: ['CollectesDetailMembres']
    };
    this.writeWorkbook(workbook, filename.replace('.xlsx', `-${this.getTodayDate()}.xlsx`));
  }

  exportCollectesResumeJournalierParAgentExcel(collectes: CollecteTerrainResponse[], filename: string = 'collectes-resume-journalier-agent.xlsx'): void {
    if (!collectes || collectes.length === 0) {
      alert('Aucune donnée à exporter');
      return;
    }

    type ResumeRow = {
      date: string;
      agent: string;
      site: string;
      antenne: number;
      collectesCount: number;
      membresVisites: number;
      carnets: number;
      demandesCredit: number;
      totalEpargne: number;
      totalRemboursements: number;
      totalFrais: number;
      totalGeneral: number;
      especesRemises: number;
      ecartTresorerie: number;
    };

    const grouped = new Map<string, ResumeRow>();
    collectes.forEach(c => {
      const key = `${c.dateCollecte}|${c.agentTerrainNom || c.agentTerrainId}|${c.siteNom || c.siteId}|${c.antenneId}`;
      const lignes = c.lignes || [];
      const membres = new Set(lignes.map(l => l.membreId)).size;
      const carnets = lignes.filter(l => l.typeLigne === 'CARNET').reduce((s, l) => s + (l.quantite || 0), 0);
      const demandes = lignes.filter(l => l.typeLigne === 'DEMANDE_CREDIT').length;

      const current = grouped.get(key) || {
        date: new Date(c.dateCollecte).toLocaleDateString('fr-FR'),
        agent: String(c.agentTerrainNom || c.agentTerrainId),
        site: String(c.siteNom || c.siteId),
        antenne: c.antenneId,
        collectesCount: 0,
        membresVisites: 0,
        carnets: 0,
        demandesCredit: 0,
        totalEpargne: 0,
        totalRemboursements: 0,
        totalFrais: 0,
        totalGeneral: 0,
        especesRemises: 0,
        ecartTresorerie: 0,
      };

      current.collectesCount += 1;
      current.membresVisites += membres;
      current.carnets += carnets;
      current.demandesCredit += demandes;
      current.totalEpargne += c.totalEpargneCalcule || 0;
      current.totalRemboursements += c.totalRemboursementsCalcule || 0;
      current.totalFrais += c.totalFraisCalcule || 0;
      current.totalGeneral += c.totalGeneralCalcule || 0;
      current.especesRemises += c.especesRemises || 0;
      current.ecartTresorerie += c.ecartTresorerie || 0;

      grouped.set(key, current);
    });

    const rows = Array.from(grouped.values()).map(r => ({
      'Date': r.date,
      'Agent': r.agent,
      'Site': r.site,
      'Antenne': r.antenne,
      'Nb Collectes': r.collectesCount,
      'Membres Visités': r.membresVisites,
      'Carnets': r.carnets,
      'Demandes Crédit': r.demandesCredit,
      'Total Épargne': r.totalEpargne,
      'Total Remboursements': r.totalRemboursements,
      'Total Frais': r.totalFrais,
      'Total Général': r.totalGeneral,
      'Espèces Remises': r.especesRemises,
      'Écart Trésorerie': r.ecartTresorerie,
    }));

    const worksheet: XLSX.WorkSheet = XLSX.utils.json_to_sheet(rows);
    const workbook: XLSX.WorkBook = {
      Sheets: { 'CollectesResumeJournalier': worksheet },
      SheetNames: ['CollectesResumeJournalier']
    };
    this.writeWorkbook(workbook, filename.replace('.xlsx', `-${this.getTodayDate()}.xlsx`));
  }

  exportCollectesEcartsTresorerieExcel(collectes: CollecteTerrainResponse[], filename: string = 'collectes-ecarts-tresorerie.xlsx'): void {
    if (!collectes || collectes.length === 0) {
      alert('Aucune donnée à exporter');
      return;
    }

    const rows = collectes.map(c => ({
      'Date': new Date(c.dateCollecte).toLocaleDateString('fr-FR'),
      'Statut': c.statut,
      'Agent': c.agentTerrainNom || c.agentTerrainId,
      'Site': c.siteNom || c.siteId,
      'Antenne': c.antenneId,
      'Total Général Attendu': c.totalGeneralCalcule,
      'Espèces Remises': c.especesRemises,
      'Écart Trésorerie': c.ecartTresorerie,
      'Observation': c.observations || ''
    }));

    const worksheet: XLSX.WorkSheet = XLSX.utils.json_to_sheet(rows);
    const workbook: XLSX.WorkBook = {
      Sheets: { 'EcartsTresorerieCollecte': worksheet },
      SheetNames: ['EcartsTresorerieCollecte']
    };
    this.writeWorkbook(workbook, filename.replace('.xlsx', `-${this.getTodayDate()}.xlsx`));
  }

  /**
   * Export recettes to Excel file
   */
  exportToExcel(recettes: RecetteTerrainResponse[], filename: string = 'recettes-terrain.xlsx'): void {
    if (!recettes || recettes.length === 0) {
      alert('Aucune donnée à exporter');
      return;
    }

    // Préparer les données
    const data = recettes.map(r => ({
      'Date': new Date(r.dateRecette).toLocaleDateString('fr-FR'),
      'Agent': r.agentTerrainNom || '',
      'Site': r.siteNom || '',
      'Statut': this.getStatusLabel(r.statut),
      'Membres Visités': r.membresVisites || 0,
      'Nouveaux Membres': r.nouveauxMembres || 0,
      'Carnets Distribués': r.carnetDistribues || 0,
      'Épargne Collectée': r.epargneCollectee || 0,
      'Remboursements Collectés': r.remboursementsCreditCollectes || 0,
      'Frais Collectés': r.fraisCollectes || 0,
      'Espèces Remises': r.especesRemises || 0,
      'Espèces Émises': r.especesEmises || 0,
      'Observations': r.observations || ''
    }));

    // Créer workbook
    const worksheet: XLSX.WorkSheet = XLSX.utils.json_to_sheet(data);
    const workbook: XLSX.WorkBook = {
      Sheets: { 'Recettes': worksheet },
      SheetNames: ['Recettes']
    };

    // Formatage basique
    worksheet['!cols'] = [
      { wch: 12 }, // Date
      { wch: 20 }, // Agent
      { wch: 20 }, // Site
      { wch: 12 }, // Statut
      { wch: 15 }, // Membres Visités
      { wch: 15 }, // Nouveaux Membres
      { wch: 15 }, // Carnets
      { wch: 18 }, // Épargne
      { wch: 20 }, // Remboursements
      { wch: 15 }, // Frais
      { wch: 15 }, // Espèces Remises
      { wch: 18 }, // Écart
      { wch: 30 }  // Observations
    ];

    // Export
    const filename_with_date = filename.replace('.xlsx', `-${this.getTodayDate()}.xlsx`);
    this.writeWorkbook(workbook, filename_with_date);
  }

  /**
   * Export recettes to PDF file
   */
  exportToPdf(
    recettes: RecetteTerrainResponse[],
    stats: any,
    filtres: any,
    filename: string = 'recettes-terrain.pdf'
  ): void {
    if (!recettes || recettes.length === 0) {
      alert('Aucune donnée à exporter');
      return;
    }

    const doc = new jsPDF() as JsPDFWithAutoTable;
    const pageWidth = doc.internal.pageSize.getWidth();
    const pageHeight = doc.internal.pageSize.getHeight();
    let yPosition = 15;

    // Titre
    doc.setFontSize(16);
    doc.text('Rapport Recettes Terrain', pageWidth / 2, yPosition, { align: 'center' });
    yPosition += 10;

    // Période et filtres
    doc.setFontSize(10);
    doc.text(`Généré le: ${new Date().toLocaleDateString('fr-FR')} à ${new Date().toLocaleTimeString('fr-FR')}`, 15, yPosition);
    yPosition += 6;

    if (filtres.statut) {
      doc.text(`Filtre Statut: ${this.getStatusLabel(filtres.statut)}`, 15, yPosition);
      yPosition += 6;
    }
    if (filtres.dateDebut || filtres.dateFin) {
      const dateRange = `${filtres.dateDebut ? new Date(filtres.dateDebut).toLocaleDateString('fr-FR') : '...'} - ${filtres.dateFin ? new Date(filtres.dateFin).toLocaleDateString('fr-FR') : '...'}`;
      doc.text(`Période: ${dateRange}`, 15, yPosition);
      yPosition += 6;
    }

    yPosition += 4;

    // KPI Summary
    if (stats) {
      doc.setFontSize(11);
      doc.text('Résumé Statistiques:', 15, yPosition);
      yPosition += 6;
      
      doc.setFontSize(9);
      const kpiText = [
        `Total Recettes: ${stats.totalRecettes || recettes.length}`,
        `Brouillon: ${stats.brouillon || 0} | Soumises: ${stats.soumises || 0} | Validées: ${stats.validees || 0} | Rejetées: ${stats.rejetees || 0}`,
        `Total Collecté: ${this.formatCurrency(stats.totalCollecte || 0)} | Montant Moyen: ${this.formatCurrency(stats.montantMoyen || 0)}`
      ];
      
      kpiText.forEach(text => {
        doc.text(text, 15, yPosition);
        yPosition += 5;
      });
    }

    yPosition += 5;

    // Tableau
    const tableData = recettes.map(r => [
      new Date(r.dateRecette).toLocaleDateString('fr-FR'),
      r.agentTerrainNom || '',
      r.siteNom || '',
      this.getStatusLabel(r.statut),
      r.membresVisites || 0,
      r.nouveauxMembres || 0,
      r.carnetDistribues || 0,
      this.formatCurrency(r.epargneCollectee),
      this.formatCurrency(r.remboursementsCreditCollectes),
      this.formatCurrency(r.fraisCollectes),
      this.formatCurrency(r.especesRemises),
      this.formatCurrency(r.especesEmises),
      r.observations?.substring(0, 20) || ''
    ]);

    (doc as any).autoTable({
      head: [[
        'Date', 'Agent', 'Site', 'Statut', 
        'Membres', 'Nouveaux', 'Carnets',
        'Épargne', 'Remb.', 'Frais', 'Esp. Remises', 'Esp. Émises', 'Notes'
      ]],
      body: tableData,
      startY: yPosition,
      margin: { top: 15, right: 15, bottom: 15, left: 15 },
      headStyles: { 
        fillColor: [66, 139, 202],
        textColor: 255,
        fontSize: 8,
        fontStyle: 'bold'
      },
      bodyStyles: {
        fontSize: 8,
        textColor: 0
      },
      alternateRowStyles: {
        fillColor: [245, 245, 245]
      },
      columnStyles: {
        3: { halign: 'center' }, // Statut centered
        4: { halign: 'right' },
        5: { halign: 'right' },
        6: { halign: 'right' },
        7: { halign: 'right' },
        8: { halign: 'right' },
        9: { halign: 'right' },
        10: { halign: 'right' },
        11: { halign: 'right' },
        12: { halign: 'left' } // Notes left-aligned
      },
      didDrawPage: (data: any) => {
        // Footer
        const pageCount = (doc as any).internal.pages.length - 1;
        const pageSize = doc.internal.pageSize;
        doc.setFontSize(8);
        doc.text(
          `Page ${(doc as any).internal.getNumberOfPages()}`,
          pageSize.getWidth() / 2,
          pageSize.getHeight() - 10,
          { align: 'center' }
        );
      }
    });

    // Total en bas
    const finalYPosition = (doc as any).lastAutoTable.finalY || yPosition;
    doc.setFontSize(11);
    doc.setFont('helvetica', 'bold');
    doc.text(
      `TOTAL: ${this.formatCurrency(recettes.reduce((sum, r) => sum + (r.totalCollecte || 0), 0))}`,
      15,
      finalYPosition + 10
    );
    doc.setFont('helvetica', 'normal');

    // Export
    const filename_with_date = filename.replace('.pdf', `-${this.getTodayDate()}.pdf`);
    doc.save(filename_with_date);
  }

  /**
   * Helper: Format currency
   */
  private formatCurrency(value: number): string {
    if (!value) return '0 CDF';
    return new Intl.NumberFormat('fr-FR', {
      style: 'currency',
      currency: 'CDF',
      minimumFractionDigits: 0,
      maximumFractionDigits: 0
    }).format(value);
  }

  /**
   * Helper: Get today date in YYYY-MM-DD format
   */
  private getTodayDate(): string {
    const today = new Date();
    return `${today.getFullYear()}-${String(today.getMonth() + 1).padStart(2, '0')}-${String(today.getDate()).padStart(2, '0')}`;
  }

  /**
   * Helper: Get status label in French
   */
  private getStatusLabel(statut: string): string {
    const labels: Record<string, string> = {
      'BROUILLON': 'Brouillon',
      'SOUMISE': 'Soumise',
      'VALIDEE': 'Validée',
      'REJETEE': 'Rejetée'
    };
    return labels[statut] || statut;
  }
}
