FROM gradle:7.4.2-jdk17

ENV APP_ENV=dev

WORKDIR /app

COPY /app .

RUN gradle installDist

CMD ./build/install/app/bin/app
