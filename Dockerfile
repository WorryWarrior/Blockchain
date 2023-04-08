FROM ubuntu
RUN mkdir /blockchain
ADD . /blockchain
RUN apt-get update && apt-get install -y openjdk-11-jdk openjdk-11-jre
RUN cd /blockchain &&\
	./gradlew MyFatJar