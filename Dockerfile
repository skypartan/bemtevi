#FROM rust:1.54.0-slim

#COPY src ./src
#COPY res ./res
#COPY Cargo.toml ./
#COPY Cargo.lock ./

#RUN cargo build --release
#RUN mv target/release/bemtevi ./

FROM ubuntu:latest

#RUN apt-get install build-essential autoconf automake libtool m4
RUN apt-get update && apt-get install libopus-dev -y

COPY res/* /app/res/
COPY target/release/bemtevi /app/

WORKDIR /app
CMD [ "./bemtevi" ]
