package org.example;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileStatus;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.DoubleWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;

public class IndisponibiliteDriver {

    private static final String PG_URL = "jdbc:postgresql://postgres-container:5432/mydatabase";
    private static final String PG_USER = "myuser";
    private static final String PG_PASS = "mypassword";

    public static void main(String[] args) throws Exception {
        if (args.length != 2) {
            System.err.println("Usage: IndisponibiliteDriver <input> <output>");
            System.exit(1);
        }

        Configuration conf = new Configuration();

        Path outputPath = new Path(args[1]);
        FileSystem fs = FileSystem.get(conf);
        if (fs.exists(outputPath)) {
            fs.delete(outputPath, true);
            System.out.println("Répertoire de sortie supprimé : " + args[1]);
        }

        Job job = Job.getInstance(conf, "Indisponibilite par centrale");

        job.setJarByClass(IndisponibiliteDriver.class);
        job.setMapperClass(IndisponibiliteMapper.class);
        job.setReducerClass(IndisponibiliteReducer.class);

        job.setMapOutputKeyClass(Text.class);
        job.setMapOutputValueClass(DoubleWritable.class);

        job.setOutputKeyClass(Text.class);
        job.setOutputValueClass(DoubleWritable.class);

        FileInputFormat.addInputPath(job, new Path(args[0]));
        FileOutputFormat.setOutputPath(job, new Path(args[1]));

        boolean success = job.waitForCompletion(true);

        if (success) {
            exportToPostgres(conf, outputPath);
            fs.delete(outputPath, true);
            System.out.println("Répertoire de sortie HDFS nettoyé.");
        }

        System.exit(success ? 0 : 1);
    }

    private static void exportToPostgres(Configuration conf, Path outputDir) throws Exception {
        Class.forName("org.postgresql.Driver");
        FileSystem fs = FileSystem.get(conf);
        try (Connection conn = DriverManager.getConnection(PG_URL, PG_USER, PG_PASS)) {
            conn.createStatement().execute(
                "CREATE TABLE IF NOT EXISTS indisponibilite_edf(" +
                "  centrale VARCHAR(255) PRIMARY KEY," +
                "  indisponibilite_mw DOUBLE PRECISION," +
                "  created_at TIMESTAMP DEFAULT NOW()," +
                "  updated_at TIMESTAMP DEFAULT NOW()" +
                ")"
            );

            String upsertSql =
                "INSERT INTO indisponibilite_edf (centrale, indisponibilite_mw, created_at, updated_at) " +
                "VALUES (?, ?, NOW(), NOW()) " +
                "ON CONFLICT (centrale) DO UPDATE " +
                "  SET indisponibilite_mw = EXCLUDED.indisponibilite_mw, updated_at = NOW()";

            try (PreparedStatement ps = conn.prepareStatement(upsertSql)) {
                for (FileStatus status : fs.listStatus(outputDir)) {
                    String name = status.getPath().getName();
                    if (!name.startsWith("part-")) continue;

                    try (BufferedReader reader = new BufferedReader(
                            new InputStreamReader(fs.open(status.getPath())))) {
                        String line;
                        while ((line = reader.readLine()) != null) {
                            String[] parts = line.split("\t", 2);
                            if (parts.length != 2) continue;
                            String centrale = parts[0].trim();
                            double indispo = Double.parseDouble(parts[1].trim());
                            ps.setString(1, centrale);
                            ps.setDouble(2, indispo);
                            ps.addBatch();
                        }
                    }
                }
                conn.setAutoCommit(false);
                ps.executeBatch();
                conn.commit();
                System.out.println("Export PostgreSQL terminé.");
            }
        }
    }
}