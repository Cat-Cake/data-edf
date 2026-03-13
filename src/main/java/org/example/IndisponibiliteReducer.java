package org.example;

import java.io.IOException;
import org.apache.hadoop.io.DoubleWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Reducer;

public class IndisponibiliteReducer extends Reducer<Text, DoubleWritable, Text, DoubleWritable> {

    private final DoubleWritable result = new DoubleWritable();

    @Override
    protected void reduce(Text key, Iterable<DoubleWritable> values, Context context)
            throws IOException, InterruptedException {

        double sum = 0.0;

        for (DoubleWritable value : values) {
            sum += value.get();
        }

        result.set(sum);
        context.write(key, result);
    }
}