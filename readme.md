Create Package

mvn clean package

Copy in docker :

docker cp edf_clean.csv hadoop-master:/tmp/edf_clean.csv
docker cp target/hadoop-edf.jar hadoop-master:/tmp/edf-job.jar

mvn clean package && docker cp target/hadoop-edf.jar hadoop-master:/tmp/edf-job.jar


In docker :

hdfs dfs -mkdir -p /input/edf
hdfs dfs -put /tmp/edf_clean.csv /input/edf/
hdfs dfs -rm -r /output/indispo_centrale

Launch :

hadoop jar /tmp/edf-job.jar org.example.IndisponibiliteDriver /input/edf/edf_clean.csv /output/indispo_centrale
hdfs dfs -cat /output/indispo_centrale/part-r-00000
