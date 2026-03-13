package org.example;

import java.io.IOException;
import org.apache.hadoop.io.DoubleWritable;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Mapper;

public class IndisponibiliteMapper extends Mapper<LongWritable, Text, Text, DoubleWritable> {

    private final Text centraleKey = new Text();
    private final DoubleWritable indispoValue = new DoubleWritable();

    @Override
    protected void map(LongWritable key, Text value, Context context)
            throws IOException, InterruptedException {

        String line = value.toString();

        if (line.startsWith("nom;")) {
            return;
        }

        String[] fields = line.split(";", -1);

        try {
            String nom = fields[0].trim();
            double puissanceMax = Double.parseDouble(fields[1].trim());
            double puissanceDispo = Double.parseDouble(fields[2].trim());

            double indisponibilite = puissanceMax - puissanceDispo;
            String centrale = nom.replaceAll("\\s+\\d+$", "").trim();

            centraleKey.set(centrale);
            indispoValue.set(indisponibilite);

            context.write(centraleKey, indispoValue);
        } catch (Exception e) {
        }
    }
}