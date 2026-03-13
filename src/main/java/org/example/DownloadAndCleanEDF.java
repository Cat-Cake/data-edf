package org.example;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.csv.CSVRecord;

import java.io.*;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class DownloadAndCleanEDF {

    private static final String FILE_URL =
            "https://opendata.edf.fr/data-fair/api/v1/datasets/haqk2fpmdz21sjiwhva-0n2x/data-files/Export_toutes_versions.csv";

    public static void main(String[] args) {
        String rawFile = "Export_toutes_versions.csv";
        String cleanFile = "edf_clean.csv";

        try {
            downloadFile(FILE_URL, rawFile);
            cleanCsv(rawFile, cleanFile);
            System.out.println("Fichier nettoyé généré : " + cleanFile);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void downloadFile(String fileUrl, String outputPath) throws IOException {
        try (InputStream in = new URL(fileUrl).openStream();
             FileOutputStream out = new FileOutputStream(outputPath)) {

            byte[] buffer = new byte[8192];
            int bytesRead;

            while ((bytesRead = in.read(buffer)) != -1) {
                out.write(buffer, 0, bytesRead);
            }
        }

        System.out.println("Téléchargement terminé : " + outputPath);
    }

    private static void cleanCsv(String inputFile, String outputFile) throws IOException {
        Path inputPath = Paths.get(inputFile);
        Path outputPath = Paths.get(outputFile);

        Charset charset = Charset.forName("Windows-1252");

        try (
                Reader reader = Files.newBufferedReader(inputPath, charset);
                CSVParser parser = new CSVParser(reader, CSVFormat.DEFAULT
                        .builder()
                        .setDelimiter(';')
                        .setQuote('"')
                        .setHeader()
                        .setSkipHeaderRecord(true)
                        .setIgnoreEmptyLines(true)
                        .build());

                Writer writer = Files.newBufferedWriter(outputPath);
                CSVPrinter printer = new CSVPrinter(writer, CSVFormat.DEFAULT
                        .builder()
                        .setDelimiter(';')
                        .setHeader("nom", "puissance_maximale_mw", "puissance_disponible_mw")
                        .build())
        ) {
            for (CSVRecord record : parser) {
                String nom = record.get("Nom").trim();
                String puissanceMax = record.get("Puissance maximale (MW)").trim();
                String puissanceDispo = record.get("Puissance disponible (MW)").trim();

                if (nom.isEmpty() || puissanceMax.isEmpty() || puissanceDispo.isEmpty()) {
                    continue;
                }

                puissanceMax = puissanceMax.replace(",", ".");
                puissanceDispo = puissanceDispo.replace(",", ".");

                printer.printRecord(nom, puissanceMax, puissanceDispo);
            }
        }
    }
}