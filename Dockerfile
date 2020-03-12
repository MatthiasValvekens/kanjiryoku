FROM gradle:jdk11-slim as build
USER root

RUN apt-get update && apt-get install -y swig git build-essential 
RUN git clone https://github.com/taku910/zinnia /zinnia
WORKDIR /zinnia/zinnia/

RUN echo "Building Zinnia..." && ./configure --enable-static=no && make

COPY . /kanjiryoku/

RUN cp /zinnia/zinnia/.libs/libzinnia.so.0 /kanjiryoku/zinnia-swig/
WORKDIR /kanjiryoku/server/
RUN gradle build

FROM openjdk:11-jre-slim

RUN mkdir /kanjiryoku
COPY --from=build /kanjiryoku/server/build/libs/kanjiryoku-server-*.jar /kanjiryoku/server.jar
COPY --from=build /kanjiryoku/zinnia-swig/build/libjzinnia-*.so  \
                  /kanjiryoku/zinnia-swig/libzinnia.so.0 \
                  /kanjiryoku/

ENV LD_LIBRARY_PATH=/kanjiryoku/
ENTRYPOINT ["java", "-Djava.library.path=/kanjiryoku", "-jar", "/kanjiryoku/server.jar"]
CMD ["/kanjiryoku-data/kanjiryoku.cfg"]
