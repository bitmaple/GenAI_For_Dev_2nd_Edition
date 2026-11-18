FROM eclipse-temurin:21-jdk-jammy AS build
WORKDIR /src
COPY SkillGenie.java SkillGenieLab.java ./
RUN javac --release 21 -d /out SkillGenie.java SkillGenieLab.java

FROM eclipse-temurin:21-jre-jammy
WORKDIR /app
COPY --from=build /out ./classes
USER 10001:10001
ENTRYPOINT ["java", "-cp", "/app/classes", "SkillGenieLab"]
CMD ["--demo"]
