# Getting Started

### Übersicht
Die Anwendung soll es ermöglichen ein Mapping von Produktdaten aufgrund einer Konfig zu machen, welche aus einer Oberfläche stammen soll.
Dadurch kann der Kunde ohne Programmierkenntnisse die Webseite pflegen. Ergänzt werden sollte noch eine ähnliche Anwendung, die auf Basis der gemappten Daten
eine Webseite generiert.
Alle Kundenspezifischen Daten sollten aus dem Adapter kommen. Hier wird eine einheitliche Datanstruktur verwendet, die für alle Kunden gelten soll.

### Stand
Dies ist nur ein POC. Entsprechend ist hier vieles vereinfacht oder nur angedeutet. Die Produktdaten sollen aus dem Adapter kommen, aber wohl nicht durch einen Reequest,
sondern über eine Datenbank/Kafka etc.
Noch offen:
- Pflegeoberfläche (nur grob andeuten, da OD das vermutlich besser kann)
- Modulieren der Daten unabhängig von UKEYs, wie zum Beispiel ein DeliveryScope etc.
- Königsklasse: Ein Weg für die technischen Daten.
- Kategorienmapping
- Mehrere Ukeys in eine Liste mappen
- Templateanwendung (nur ganz grobe Skizze, da OD das vermutlich besser kann), aber mal eine Stage darstellen sollte gehen
